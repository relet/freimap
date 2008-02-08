/* net.relet.freimap.FlowDataSource.java

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

public class FlowDataSource implements DataSource {

  String sNodeSource;
  DataSource nodeSource;

  DataSourceListener listener;

  String host, user, pass, db;
  int port;

  Connection conn, conn2, conn3;

  private LinkedList<Long> updateTimes=new LinkedList<Long>(); 
  long firstUpdate = 1,
       lastUpdate  = 1,
       firstAvailable = 1,
       lastAvailable  = 1;

  public FlowDataSource() {
  }
/** Initialize this data source with its configuration parameters. **/
  public void init(HashMap<String, Object> conf) {
    //reading config
    host = Configurator.getS("host", conf);
    //port = Configurator.getI("port", conf);
    db   = Configurator.getS("db",   conf);
    user = Configurator.getS("user", conf);
    pass = Configurator.getS("pass", conf);

    sNodeSource = Configurator.getS("nodesource", conf);

    if (sNodeSource == null) {
      System.err.println(conf);
      System.err.println("FlowDataSource must have a parameter nodesource");
      System.exit(1);
    }
    /*do more dummy checks here*/

    //init db connection
    try {
      System.out.print("[netflow] connecting.. ");
      String odbcurl="jdbc:mysql://"+host+"/"+db;
      Class.forName ("com.mysql.jdbc.Driver").newInstance();
      conn = DriverManager.getConnection(odbcurl, user, pass);

      conn2 = DriverManager.getConnection(odbcurl, user, pass); //second concurrent connection for background data fetching
      conn3 = DriverManager.getConnection(odbcurl, user, pass); //third concurrent connection for background profile fetching
      System.out.println("done. [/netflow]");

      //initializing background data fetching
      fetchAvailableTimeStamps();

      //collecting base information
      System.out.print("[netflow] fetching basic information.. ");
      Statement s = conn.createStatement();
      ResultSet r = s.executeQuery("select unix_timestamp(min(FLOW_BEGIN)) as first, unix_timestamp(max(FLOW_END)) as last from FLOWS");
      if (r.next()) {
        firstUpdate = r.getLong("first");
        lastUpdate  = r.getLong("last");
      } else {
        System.err.println("No data in flowdb");
      }
    } catch (Exception ex) {
      ex.printStackTrace(); //debug
      System.exit(1);
    }
  }


/** @return A Vector of FreiNodes to be displayed. */
  public Vector<FreiNode> getNodeList() {
    if ((nodeSource == null) && (sNodeSource != null)) {
      nodeSource=Visor.sources.get(sNodeSource);
      sNodeSource = null;
    }
    if (nodeSource!=null) {
      Vector<FreiNode> nodes = nodeSource.getNodeList();
//      for (Enumeration<String> enu = generatedNodes.keys(); enu.hasMoreElements();) {
//        nodes.add(generatedNodes.get(enu.nextElement()));
//      }
//      for (int i=0;i<nodes.size();i++) { 
//        knownNodes.put(nodes.elementAt(i).id, nodes.elementAt(i));
//      }
      return nodes;
    } 
    return null;
  }


/** <b>To be deprecated soon.</b>
    @param  time  ignored in current implementations.
    @return A Hashtable mapping Node IDs to a float value between 0 and 1.
*/
  public Hashtable<String, Float> getNodeAvailability(long time) {
    return null;
  }

/** @return The first unix timestamp which can be displayed. */
  public long getFirstUpdateTime() {
    return firstUpdate;
  }
/** @return The last unix timestamp which can be displayed. */
  public long getLastUpdateTime() {
    return lastUpdate;
  }

/** This method approximates a requested time with the closest timestamp which can actually be 
    displayed.
    @param  time  A timestamp to be approximated 
    @return the next best timestamp.
*/
/** @return The first unix timestamp which has been pre-fetched. */
  public long getLastAvailableTime() {
    return lastAvailable;
  }
/** @return The last unix timestamp which has been pre-fetched. */
  public long getFirstAvailableTime() {
    return firstAvailable;
  }
  public void setAvailableTime(long first, long last) {
    if ((lastAvailable == 1) || (last > lastAvailable)) lastAvailable = last;
    if ((firstAvailable == 1) || (first < firstAvailable)) firstAvailable = first;
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
          return firstUpdate;
        }
      }
    }
  }

/** Returns link data for a given timestamp. May return null if the timestamp has not been 
    provided by getClosestUpdateTime.
    @param  time  A timestamp
    @return All links which should be displayed at this given time.
*/

  public FreiNode getNodeByName(String id) {
    if (nodeSource!=null) { 
      return nodeSource.getNodeByName(id);
    }
    else return null;
  }

  public Vector<FreiLink> getLinks(long time) {
    Vector<FreiLink> linkList=new Vector<FreiLink>();
    HashMap<String, HashMap<String, FreiLink>> linkByName = new HashMap<String, HashMap<String, FreiLink>>();
    if ((time<=0) /* || (time>MAX_UNIX_TIME)*/ ) return linkList; //empty
    try {
      Statement s = conn.createStatement();
      ResultSet r = s.executeQuery("SELECT HIGH_PRIORITY * from FLOWS where FLOW_BEGIN<from_unixtime("+time+") and FLOW_END>from_unixtime("+time+")");
      while (r.next()) {
        String src   = r.getString("SOURCE_IP");
        int    sport = r.getInt("SOURCE_PORT");
        String dest= r.getString("DEST_IP");
        int    dport = r.getInt("DEST_PORT");
        int    proto = r.getInt("PROTOCOL");
        long   packs = r.getLong("PACKETS");
        long   bytes = r.getLong("BYTES");
        if (src.equals(dest)) continue;
        Object srcn=nodeSource.getNodeByName(src);
        Object dstn=nodeSource.getNodeByName(dest);
        if (srcn==null) { //enable real-time interpolation
          srcn=new FreiNode(src);
          ((FreifunkMapDataSource)nodeSource).addNode((FreiNode)srcn);
          if (listener!=null) listener.nodeListUpdate((FreiNode)srcn);
        }
        if (dstn==null) {
          dstn=new FreiNode(dest);
          ((FreifunkMapDataSource)nodeSource).addNode((FreiNode)dstn);
          if (listener!=null) listener.nodeListUpdate((FreiNode)dstn);
        }
        if ((srcn!=null) && (dstn!=null)) {
          HashMap<String, FreiLink> temp=linkByName.get(src);
          FreiLink link=(temp==null)?null:temp.get(dest);
          if (link==null) link=new FreiLink((FreiNode)srcn, (FreiNode)dstn, 1);
          link.packets += packs;
          link.bytes   += bytes;
          switch (proto) {
            case 1: {
              link.icmp += packs;
              break;
            }
            case 6: {
              link.tcp += packs;
              break;
            }
            case 17: {
              link.udp += packs;
              break;
            }
            default: {
              link.other += packs;
              break;
            }
          }
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


/** @param  dsl A DataSourceListener listening on events from this DataSource */
  public void addDataSourceListener(DataSourceListener dsl) {
    this.listener = dsl; //todo: allow multiple listeners
  }
/** Extendend link information is requested using this method. 
    @param  link  the link on which information is sought
    @param  info  A LinkInfo structure to be filled with this information. 
*/
  public void getLinkProfile(FreiLink link, LinkInfo info) {
    new LPFetcher(link, info).start();
  }
/** Extendend node information is requested using this method. 
    @param  node  the node on which information is sought
    @param  info  A NodeInfo structure to be filled with this information. 
*/
  public void getLinkCountProfile(FreiNode node, NodeInfo info) {
    return;
  }

  private void fetchAvailableTimeStamps() {
    new TimeStampFetcher();
    try {
      Thread.sleep(100); //sleep to allow a few time stamps to load. this is a hack, indeed.
    } catch (InterruptedException ex) {}
  }

  class LPFetcher extends Thread {
    FreiLink link;
    LinkInfo linkinfo;

    public LPFetcher(FreiLink link, LinkInfo linkinfo) {
      this.link=link;
      this.linkinfo=linkinfo;
    }

    public void run() {
      LinkedList<FlowData> lp=new LinkedList<FlowData>();
      try {
        Statement s = conn3.createStatement();
        ResultSet r = s.executeQuery("select HIGH_PRIORITY unix_timestamp(FLOW_BEGIN) as begin, unix_timestamp(FLOW_END) as end, PACKETS as packets, BYTES as bytes, PROTOCOL as protocol from FLOWS where SOURCE_IP='"+link.from.id+"' and DEST_IP='"+link.to.id+"' ORDER BY FLOW_BEGIN");
        while (r.next()) {
          long begin   = r.getLong("begin");
          long end     = r.getLong("end");
          long packets = r.getLong("packets");
          long bytes   = r.getLong("bytes");
          int  proto   = r.getInt ("protocol");
          lp.add(new FlowData(begin, end, packets, bytes, proto));
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        linkinfo.status=linkinfo.STATUS_FAILED;
        return;
      }
      linkinfo.setFlowProfile(lp);
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
          ResultSet r = s.executeQuery("select unix_timestamp(FLOW_BEGIN) as stamp from FLOWS group by FLOW_BEGIN limit "+OFFSET+" offset "+offset);
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
          setAvailableTime(firstUpdate, stamp);
          offset+=OFFSET;
          Thread.sleep(SLEEP);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
