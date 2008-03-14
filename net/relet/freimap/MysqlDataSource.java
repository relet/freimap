/* net.relet.freimap.MysqlDataSource.java

  This file is part of the freimap software available at freimap.berlios.de

  This software is copyright (c)2007 Thomas Hirsch <thomas hirsch gmail com>

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License with
  the Debian GNU/Linux distribution in file /doc/gpl.txt
  if not, write to the Free Software Foundation, Inc., 59 Temple Place,
  Suite 330, Boston, MA 02111-1307 USA
*/

package net.relet.freimap;

import java.util.*;
import java.sql.*;

public class MysqlDataSource implements DataSource {
  
  private Vector<FreiNode> nodeList=new Vector<FreiNode>();
  private Hashtable<String, FreiNode> nodeByName=new Hashtable<String, FreiNode>(); //fixme: not effective
  private LinkedList<Long> updateTimes=new LinkedList<Long>(); 
  boolean updateClosest = true;
  private Vector<FreiLink> linkList=new Vector<FreiLink>();
  private Hashtable<String, Float> availmap = null;
  private Connection conn, conn2, conn3;
  private long firstUpdateTime = 1,
               lastUpdateTime = 1,
               firstAvailableTime = 1,
               lastAvailableTime = 1;
  private DataSourceListener listener=null;

  private String host, user, pass, db;
  private String TABLE_LINKS;
  private String TABLE_NODES;
  private boolean nodeDataOnly = false;

  DataSource nodeSource;
  String sNodeSource;
  
  public MysqlDataSource() {
    this(null, null, null, null, false);
  }
  public MysqlDataSource(String host, String user, String pass, String db, boolean nodeDataOnly) {
    this.host=host;
    this.user=user;
    this.pass=pass;
    this.db  =db;
    this.nodeDataOnly = nodeDataOnly;
  }

  public void init(HashMap<String, Object> conf) {
    if (host==null) host=Configurator.getS("host", conf);
    if (pass==null) pass=Configurator.getS("pass", conf);
    if (user==null) user=Configurator.getS("user", conf);
    if (db  ==null) db  =Configurator.getS("db", conf);

    TABLE_LINKS = Configurator.getS(new String[]{"tables", "links"}, conf);
    TABLE_NODES = Configurator.getS(new String[]{"tables", "nodes"}, conf);

    sNodeSource = Configurator.getS("nodesource", conf);
    //todo: dummy check input
    try {
      String odbcurl="jdbc:mysql://"+host+"/"+db;
      Class.forName ("com.mysql.jdbc.Driver").newInstance();
      conn = DriverManager.getConnection(odbcurl, user, pass);
      conn3 = DriverManager.getConnection(odbcurl, user, pass); //we like conn-currency :)
      System.out.println("[mysql] retrieving nodes + time stamps");
      updateNodeList();
      if (!nodeDataOnly) {
        conn2 = DriverManager.getConnection(odbcurl, user, pass); //second concurrent connection for background data fetching
        fetchAvailableTimeStamps();
      }
    } catch (Exception ex) {
      ex.printStackTrace(); //debug
      System.exit(1);
    }
  }
  
  private void updateNodeList() throws SQLException{
    if ((nodeSource == null) && (sNodeSource != null)) {
      nodeSource=Visor.sources.get(sNodeSource);
      sNodeSource = null;
    }

    if (nodeSource!=null) {
      Vector<FreiNode> nodev = nodeSource.getNodeList();
      Iterator<FreiNode> nodes=nodev.iterator();
      while (nodes.hasNext()) {
        FreiNode node = nodes.next();
        nodeList.remove(node);
        nodeList.add(node);
        nodeByName.put(node.id, node);
      }
    } else {
      Statement s = conn.createStatement();
      ResultSet r = s.executeQuery("SELECT * from "+TABLE_NODES);
      String ip = null;
      while (r.next()) {
        try {
          ip = r.getString("node");
        } catch (Exception ex) {
          ip = r.getString("ip");
        }
        double lon = r.getDouble("lon"),
              lat = r.getDouble("lat");
        FreiNode node=new FreiNode(ip, lon, lat);
        nodeList.remove(node);
        nodeList.add(node);
        nodeByName.put(node.id, node);
      }
    }
  }
  
  public Vector<FreiNode> getNodeList() {
    return nodeList;
  }
  
  public FreiNode getNodeByName(String name) {
    return nodeByName.get(name);
  }
  public void addNode(FreiNode node) {
    nodeList.remove(node); //just in case
    nodeList.add(node);
    nodeByName.put(node.id, node);
  }
  
  public Hashtable<String, Float> getNodeAvailability(long time) {
    //time is ignored in this implementation
    if (availmap == null) {
      try {
        availmap = new Hashtable<String, Float>();
        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT * from avail_nodes");
        while (r.next()) {
          String id = r.getString("node");
          float avail = r.getFloat("avail");
          availmap.put(id, new Float(avail));
        }
      } catch (Exception ex) {
        System.err.println("Availability table not found or broken. Ignoring.");
        //ex.printStackTrace();
        availmap=null;
      }
    }
    return availmap;
  }

  public long getLastUpdateTime() {
    long newLastUpdateTime=-1;
    try {
      Statement s = conn.createStatement();
      ResultSet r = s.executeQuery("SELECT unix_timestamp(max(clock)) as last from "+TABLE_LINKS+" WHERE clock>from_unixtime("+lastUpdateTime+")");
      if (r.next()) {
        newLastUpdateTime = r.getLong("last");
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    if (newLastUpdateTime > lastUpdateTime) lastUpdateTime = newLastUpdateTime;
    return lastUpdateTime;
  }

  public long getLastAvailableTime() {
    return lastAvailableTime;
  }
  public long getFirstAvailableTime() {
    return firstAvailableTime;
  }
  public void setAvailableTime(long first, long last) {
    if ((lastAvailableTime == 1) || (last > lastAvailableTime)) lastAvailableTime = last;
    if ((firstAvailableTime == 1) || (first < firstAvailableTime)) firstAvailableTime = first;
    if (listener!=null) listener.timeRangeAvailable(first, last);
  }

  public void addDataSourceListener(DataSourceListener dsl) {
    this.listener = dsl; //todo: allow multiple listeners
  }

  private void fetchAvailableTimeStamps() {
      new TimeStampFetcher();
      try {
        Thread.sleep(100); //sleep to allow a few time stamps to load. this is a hack, indeed.
      } catch (InterruptedException ex) {}
  }

  public long getClosestUpdateTime(long time) {
    long closest=-1;
    int tries=0;
    while (true) {
      try {
        ListIterator<Long> li = updateTimes.listIterator(0);
        while(li.hasNext()) {
          long stamp=(li.next()).longValue();
          if (time>stamp) closest=stamp; 
          else break;
        }
        return closest;
      } catch (ConcurrentModificationException ex) {
        //ok, we will have to try again.
        tries++;
        if (tries==10) {
          System.err.println("Too many concurrent modifications in \"MysqlDataSource.getClosestUpdateTime\". Strange things may happen.");
          return firstUpdateTime;
        }
      }
    }
  }
  
  public long getFirstUpdateTime() {
    if (firstUpdateTime == 1) {
      try {
        Statement s = conn.createStatement();
        ResultSet r = s.executeQuery("SELECT unix_timestamp(min(clock)) as last from "+TABLE_LINKS);
        if (r.next()) {
          firstUpdateTime = r.getLong("last");
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    if (firstAvailableTime == 1) firstAvailableTime=firstUpdateTime;
    if (lastAvailableTime == 1) lastAvailableTime=firstUpdateTime;
    return firstUpdateTime;
  }
  
  public Vector<FreiLink> getLinks(long time) {
    linkList=new Vector<FreiLink>();
    if ((time<=0) /* || (time>MAX_UNIX_TIME)*/ ) return linkList; //empty
    try {
      Statement s = conn.createStatement();
      ResultSet r = s.executeQuery("SELECT HIGH_PRIORITY * from "+TABLE_LINKS+" where clock=from_unixtime("+time+")");
      while (r.next()) {
        String src = r.getString("src");
        String dest= r.getString("dest");
        float  q   = r.getFloat("quality");
        //float  q    = r.getFloat("quality");
        Object srcn=nodeByName.get(src);
        Object dstn=nodeByName.get(dest);
	if (srcn==null) { //enable real-time interpolation
	  srcn=new FreiNode(src);
  	  nodeByName.put(src, (FreiNode)srcn);
          if (listener!=null) listener.nodeListUpdate((FreiNode)srcn);
        }
	if (dstn==null) {
	  dstn=new FreiNode(dest);
  	  nodeByName.put(dest, (FreiNode)dstn);
          if (listener!=null) listener.nodeListUpdate((FreiNode)dstn);
        }
        if ((srcn!=null) && (dstn!=null)) {
          FreiLink link=new FreiLink((FreiNode)srcn, (FreiNode)dstn, q);
          linkList.remove(link);
          linkList.add(link);
        }
      }
    } catch (Exception ex) {
      System.out.println("clock = "+time);
      ex.printStackTrace();
    }
    return linkList;
  }

  public void getLinkCountProfile(FreiNode node, NodeInfo nodeinfo) {
    new LCPFetcher(node, nodeinfo).start();
  }
  public void getLinkProfile(FreiLink link, LinkInfo linkinfo) {
    new LPFetcher(link, linkinfo).start();
  }

  class LCPFetcher extends Thread {
    FreiNode node;
    NodeInfo nodeinfo;

    public LCPFetcher(FreiNode node, NodeInfo nodeinfo) {
      this.node=node;
      this.nodeinfo=nodeinfo;
    }

    public void run() {
      LinkedList<LinkCount> lcp=new LinkedList<LinkCount>();
      try {
        Statement s = conn3.createStatement();
        ResultSet r = s.executeQuery("select HIGH_PRIORITY unix_timestamp(clock) as time, count(*) as num_links from "+TABLE_LINKS+" where dest='"+node.id+"' group by clock");
        while (r.next()) {
          long  clock = r.getLong("time");
          int   links = r.getInt("num_links");
	  lcp.add(new LinkCount(clock,links));
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        nodeinfo.status=nodeinfo.STATUS_FAILED;
        return;
      }
      nodeinfo.setLinkCountProfile(lcp);
    }
  }

  class LPFetcher extends Thread {
    FreiLink link;
    LinkInfo linkinfo;

    public LPFetcher(FreiLink link, LinkInfo linkinfo) {
      this.link=link;
      this.linkinfo=linkinfo;
    }

    public void run() {
      LinkedList<LinkData> lp=new LinkedList<LinkData>();
      try {
        Statement s = conn3.createStatement();
        ResultSet r = s.executeQuery("select HIGH_PRIORITY unix_timestamp(clock) as time, quality from "+TABLE_LINKS+" where src='"+link.from.id+"' and dest='"+link.to.id+"'");
        while (r.next()) {
          long  clock   = r.getLong("time");
          float quality = r.getFloat("quality");
	  lp.add(new LinkData(clock,quality));
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        linkinfo.status=linkinfo.STATUS_FAILED;
        return;
      }
      linkinfo.setLinkProfile(lp);
    }
  }

  class TimeStampFetcher implements Runnable { //use an own connection for concurrency!
    private final static int OFFSET = 1000;
    private final static int SLEEP  = 10;
 
    public TimeStampFetcher () {
      new Thread(this).start();
    }

    public void run() {
      long offset = 0;
      try {
        while (true) {
          Statement s = conn2.createStatement();
          ResultSet r = s.executeQuery("select unix_timestamp(clock) as stamp from "+TABLE_LINKS+" group by clock limit "+OFFSET+" offset "+offset);
          boolean hasResults=false; 
          long stamp=0;
          while (r.next()) {
            hasResults=true;
            stamp=r.getLong("stamp");
            Long clock=new Long(stamp);
	    updateTimes.add(clock);
            Thread.yield();
          }
          if (!hasResults) break;
          setAvailableTime(firstUpdateTime, stamp);
          //System.out.println("fetched "+firstUpdateTime+" - "+stamp);
          offset+=OFFSET;
          Thread.sleep(SLEEP);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
