/* net.relet.freimap.DataSource.java

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

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

import org.ho.yaml.*;
import com.thoughtworks.xstream.*;

public class YamlDataSource implements DataSource {

  private final static String[][] nullParms=new String[][]{};

  MysqlDataSource mysqlSource;
  boolean useMysqlSource = false;
  
  String yamlURL;
  long firstUpdateTime=1, lastUpdateTime=1;
  int clockCount;
  LinkedList<YamlState> updateTimes=new LinkedList<YamlState>();
  HashMap<Integer, FreiNode> nodeByID=new HashMap<Integer, FreiNode>();
  HashMap<String, FreiNode> nodeByName=new HashMap<String, FreiNode>();
  Vector<FreiNode> nodes;
  
  DataSourceListener listener;
  
  DateFormat df=new SimpleDateFormat("y-M-D H:m:s");

  XStream xstream=new XStream();//DEBUG
  
  public YamlDataSource() {
  }
  public void init(HashMap<String, Object> conf) {
    yamlURL = Configurator.getS("url", conf);    
    
    if (useMysqlSource) mysqlSource=new MysqlDataSource("localhost", "root", "", "freiberlin", true);
    
    try {
        Object yaml=getYAML(yamlURL+"/uptime/yaml/state/overview/", nullParms);
        ArrayList<HashMap> list = list(yaml);
        firstUpdateTime=getDate(list.get(0), "startdate");
        lastUpdateTime =getDate(list.get(1), "stopdate");
        clockCount     =getI(list.get(2), "count");
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    //fake overview getYAML by reading from a file
/*    try {
      ArrayList<HashMap> list = list(Yaml.load(new File("overview.yaml")));
      firstUpdateTime=getI(list.get(0), "startdate");
      lastUpdateTime =getI(list.get(1), "stopdate");
      clockCount     =getI(list.get(2), "count");
    } catch (Exception ex) {
      ex.printStackTrace();
    }
*/    //end 
    
    fetchAvailableTimeStamps();
  }
  
  @SuppressWarnings("unchecked")
  ArrayList<HashMap> list(Object o) {
    if (!(o instanceof ArrayList)) return null;
    return (ArrayList<HashMap>)o;
  }
  
  String getS(HashMap map, String key) {
    Object o = map.get(key);
    try {
      return (String)o;
    } catch (Exception ex) {
      System.out.println(o);
      ex.printStackTrace();
      System.exit(1);
    }
    return null;
  }

  long getDate(HashMap map, String key) {
    try {
      long timestamp = df.parse(getS(map, key)).getTime();
      return timestamp;
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    return -1;
  }

  int getI(HashMap map, String key) {
    Object o = map.get(key);
    try {
      return ((Integer)o).intValue();
    } catch (Exception ex) {
      System.out.println(o);
      ex.printStackTrace();
      System.exit(1);
    }
    return -1;
  }
  
  Object getYAML(String surl, String[][] parms) { 
    //performs a HTTP POST, gunzips the content, and parses it as YAML file
    try {
      // Construct data
      String data="";
      for(int i = 0; i < parms.length; i++) {
         data += URLEncoder.encode(parms[i][0], "UTF-8") + "=" + URLEncoder.encode(parms[i][1], "UTF-8");
        if (i<parms.length-1) data += "&";
      }
      System.out.println("data: "+data);
 
      // Send data
      URL url = new URL(surl);
      URLConnection conn = url.openConnection();
      conn.addRequestProperty("Accept-encoding","gzip");
      conn.setDoOutput(true);
      OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream()); //where may we close this one?
      wr.write(data);
      wr.flush();
    
      String encoding = conn.getHeaderField("Content-Encoding");
      InputStream in = conn.getInputStream();

      if ((encoding != null) && (encoding.equals("gzip"))) {
        in=new GZIPInputStream(in);
      }
      Object yaml= Yaml.load(in);
      wr.close();
      in.close();
      return yaml;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }
  
  public Vector<FreiNode> getNodeList() {
    if (useMysqlSource) {
      nodes = mysqlSource.getNodeList();
      //for (Enumeration<String> enu = generatedNodes.keys(); enu.hasMoreElements();) {
      //  nodes.add(generatedNodes.get(enu.nextElement()));
      //}
      return nodes;
    } else {
      try {
        //ObjectInputStream ois=new ObjectInputStream(getClass().getResourceAsStream(Configurator.get("olsrd.nodefile")));
        //nodes = (Vector<FreiNode>)ois.readObject();
        //ois.close();
        //for (int i=0;i<nodes.size();i++) { 
        //  knownNodes.put(nodes.elementAt(i).id, nodes.elementAt(i));
        //}
        //return nodes;
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    return null;
  }
  
  private void fetchAvailableTimeStamps() {
    new TimeStampFetcher();
    try {
      Thread.sleep(100); //hack - wait a while, to allow time stamps to preload
    } catch (InterruptedException ex) {}
  }

  
  
  public Hashtable<String, Float> getNodeAvailability(long time) {
    System.err.println("Not available: getNodeAvailability");
    return new Hashtable<String, Float>();
  }
  public long getLastUpdateTime() {
    return lastUpdateTime;
  }
  public long getFirstUpdateTime() {
    return firstUpdateTime;
  }
  public long getFirstAvailableTime() {
    //TODO
    return 1;
  }
  public long getLastAvailableTime() {
    //TODO 
    return 1;
  }
  public FreiNode getNodeByName(String id) {
    //TODO
    return null;
  }

  public long getClosestUpdateTime(long time) {
    long cur=-1, id=-1, closest = Long.MAX_VALUE;
    Iterator<YamlState> ki = updateTimes.iterator();
    while (ki.hasNext()) {
      YamlState state=ki.next();
      cur = state.startdate;
      id  = state.id;
      long d=Math.abs(time-cur);
      if (d<closest) closest=d;
      else break;
    }
    return id; //yet another hack. this should return a time stamp, not an id. but since it is ***currently*** used only in the next line of 
               //visorframe, it won't affect execution. however, your mind will be severely corrupted in the process. (FIXME!)
  }

  @SuppressWarnings("unchecked")
  public Vector<FreiLink> getLinks(long time) { //hack. expects a yamlstate id, not a timestamp. see directly above. (...FIXME)
    Vector <FreiLink> linkList=new Vector<FreiLink>();
    try {
      //Object yaml=getYAML(yamlURL+"/uptime/yaml/state/detail/", new String[][]{{"id",""+time}});
      HashMap<String, Object> yaml=(HashMap<String, Object>)Yaml.load(new File("detail.yaml"));
      
      Iterator<String> entries=yaml.keySet().iterator();
      HashMap<String, Integer> info=(HashMap<String, Integer>)yaml.get(entries.next()); //startdate + stopdate, ignored
      ArrayList<HashMap> conns=(ArrayList<HashMap>)(yaml.get(entries.next())); //connection infos
      for(int i = 0; i < conns.size(); i++) {
        HashMap<String, Object> conn=conns.get(i);
        Integer srcid=(Integer)conn.get("from");
        Integer destid=(Integer)conn.get("to");
        FreiNode srcnode=nodeByID.get(srcid);
        FreiNode destnode=nodeByID.get(destid);
        if (srcnode==null) {
          srcnode = getNode(srcid);
        }
        if (destnode==null) {
          destnode = getNode(destid);
        }
        Double lq = (Double)conn.get("lq");
        Double nlq = (Double)conn.get("nlq");
        FreiLink link=new FreiLink(srcnode, destnode, lq.floatValue(), nlq.floatValue(), false); //TODO HNA?
        linkList.remove(link);
        linkList.add(link);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return linkList;
    
    //System.err.println("Not available: getLinks "+time);
    //return null;
  }
  
  @SuppressWarnings("unchecked")
  public FreiNode getNode(Integer id) {
    //fetches full node info by id.
    //updates nodes' status
    //adds node to nodeByID
    //returns node object
    
    HashMap<String, HashMap> yaml=(HashMap<String, HashMap>)getYAML(yamlURL+"/uptime/yaml/list/", new String[][]{{"id",""+id}, {"fields", "*"}});
    try {
      //HashMap<String, HashMap> yaml=Yaml.load(new File("node.yaml"));
      HashMap<String, Object> nodedata = yaml.get(id.toString());
      String ip=(String)nodedata.get("ip");
      FreiNode node=nodeByName.get(ip);
      //System.out.println("getNode "+id);
      //System.out.println(xstream.toXML(yaml));
      //System.out.println(xstream.toXML(nodedata));
      System.exit(0);
      
      
    } catch (Exception ex) {
      ex.printStackTrace();
    }
 
    return null;
  }
  
  //threaded information fetching
  public void addDataSourceListener(DataSourceListener dsl) {
    listener=dsl;
  }
  //some optional methods
  public void getLinkProfile(FreiLink link, LinkInfo info) {
    System.err.println("Not available: getLinkProfile");
  }
  public void getLinkCountProfile(FreiNode node, NodeInfo info) {
    System.err.println("Not available: getLinkCountProfile");
  }
  
  class TimeStampFetcher implements Runnable { //use an own connection for concurrency!
    private final static int OFFSET = 1000;
    private final static int SLEEP  = 10;
    
    long offsetTime;
 
    public TimeStampFetcher () {
      long realOffset=Math.max(OFFSET, clockCount);
      offsetTime=(lastUpdateTime-firstUpdateTime)*realOffset/clockCount;
      new Thread(this).start();
    }

    @SuppressWarnings("unchecked")
    public void run() {
      long offset = firstUpdateTime;
      try {
        while (true) {
          HashMap<String, HashMap> yaml=(HashMap<String, HashMap>)getYAML(yamlURL+"/uptime/yaml/state/list/", new String[][]{{"timerange",offset+"-"+(offset+offsetTime-1)}});
          //HashMap<String, HashMap> yaml = (HashMap<String, HashMap>)Yaml.load(new File("list.yaml"));          
         
          boolean hasResults=false; 
          long stamp=0;
          Iterator<String> ids=yaml.keySet().iterator();
          while (ids.hasNext()) {
            hasResults=true;
            String yamlid=ids.next();
            HashMap<String, Integer> entry=yaml.get(yamlid);
            stamp=entry.get("startdate").intValue();
            int stop=entry.get("stopdate").intValue();
            Long clock=new Long(stamp);
	          updateTimes.add(new YamlState(Integer.parseInt(yamlid), stamp, stop)); 
            Thread.yield();
          }
          if (!hasResults) break;
          if (listener!=null) listener.timeRangeAvailable(firstUpdateTime, stamp);

          offset+=offsetTime;
          if (offset>lastUpdateTime) break;
          Thread.sleep(SLEEP);          
        }
        
      } catch (Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
