package net.relet.freimap;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * A {@link DataSource} implementation that reads node data from the
 * <a href="http://www.layereight.de/software.php">FreifunkMap</a> plugin 
 * latlon.js file. You will need either a local copy or have it available
 * on a web server.
 * 
 * Since it works with URLs, downloaded data can be used as well.
 *  
 * @author Thomas Hirsch (thomas hirsch gmail com)
 *
 */
public class LatLonJsDataSource implements DataSource {

  Vector<FreiNode> nodes = new Vector<FreiNode>();
  Vector<FreiLink> links = new Vector<FreiLink>();
  HashMap<String, FreiNode> nodeByName = new HashMap<String, FreiNode>();

  long initTime = System.currentTimeMillis()/1000;
  boolean fetchLinks = true;

  public void init(HashMap<String, Object> configuration) {
    String sServerURL = null;
    try {
      sServerURL = Configurator.getS("url", configuration);
      fetchLinks = Configurator.getB("fetchlinks", configuration);
      
      System.out.println("fetching data from URL: " + sServerURL);
      if (!fetchLinks) System.out.println("NOT fetching link information.");
      System.out.print("This may take a while ... ");
      
      BufferedReader in = new BufferedReader(new InputStreamReader(new URL(sServerURL).openStream()));
      while (true) {
        String line=in.readLine();
        if (line==null) break;
        if ((line.length()>4) && (line.substring(0,4).equals("Node"))) {
          StringTokenizer st = new StringTokenizer(line.substring(5,line.length()-2), ",", false);
          String ip = st.nextToken();
          double lat  = Double.parseDouble(st.nextToken());
          double lon  = Double.parseDouble(st.nextToken());
          int    isgateway = Integer.parseInt(st.nextToken());
          String gatewayip = st.nextToken(); 
          String fqid  = st.nextToken();

          ip = stripQuotes(ip); //strip single quotes
          fqid = stripQuotes(fqid);
          gatewayip = stripQuotes(gatewayip);

          // Use ip or coordinates as fqid if tooltip is missing
          if (ip.equals("")) ip=null;
          if (fqid == null) {
            if (ip == null) {
              fqid = lat+","+lon;
            } else {
              fqid = ip;
            }
          }
          if (ip == null) { //we need at least one identifier
            ip = fqid;
          }

          FreiNode nnode;
          if ((lat<-90d)||(lat>90d)||(lon<-180d)||(lon>180d)) { //obviously bogus. some people do that. 
            nnode = new FreiNode(ip, fqid);
          } else {
            nnode = new FreiNode(ip, fqid, lon, lat);
          }
          nodes.add(nnode);
          if (isgateway==1) {
            nnode.attributes.put("Gateway", "SELF");
          } else {
            nnode.attributes.put("Gateway", "OTHER: "+gatewayip);
          }
          nodeByName.put(nnode.id, nnode);

        } else
        if ((fetchLinks) && (line.length()>5) && (line.substring(0,5).equals("PLink"))) {
          StringTokenizer st = new StringTokenizer(line.substring(6,line.length()-2), ",", false);
          String src = st.nextToken();
          String dest = st.nextToken();
          double lq  = Double.parseDouble(st.nextToken());
          double nlq  = Double.parseDouble(st.nextToken());
          double etx  = Double.parseDouble(st.nextToken());

          src = stripQuotes(src);
          dest = stripQuotes(dest);

          FreiNode nsrc = nodeByName.get(src);
          FreiNode ndest = nodeByName.get(dest);

          if (nsrc==null) { 
            System.err.println(src+" not found.");
            continue;
          }
          if (ndest==null) { 
            System.err.println(dest+" not found.");
            continue;
          }

          FreiLink link = new FreiLink(nsrc, ndest, (float)lq, (float)nlq, false);
          links.add(link);
      }
    }

    System.out.println("finished.");
      
    } catch (MalformedURLException mue) {
      System.out.println("failed!");
      throw new IllegalStateException("Invalid server URL: " + sServerURL);
    } catch (IOException ioe) {
      System.out.println("failed! IOException in LatLonJSDataSource");
      ioe.printStackTrace();
    }
  }
  
  private String stripQuotes(String str) {
    if (str.length()<=2) return null;
    return str.substring(1,str.length()-1);
  }

  public void addDataSourceListener(DataSourceListener dsl) {
    // TODO: Implement me.
  }

  public long getClosestUpdateTime(long time) {
    return initTime;
  }

  public long getFirstUpdateTime() {
    return initTime;
  }

  public long getFirstAvailableTime() {
    return initTime;
  }

  public long getLastAvailableTime() {
    return initTime;
  }

  public long getLastUpdateTime() {
    return initTime;
  }

  public FreiNode getNodeByName(String id) {
    return nodeByName.get(id);
  }

  public void addNode(FreiNode node) {
    nodes.remove(node); //just in case
    nodes.add(node);
    nodeByName.put(node.id, node);
  }


  public void getLinkCountProfile(FreiNode node, NodeInfo info) {
    // TODO: Implement me.
    info.setLinkCountProfile(new LinkedList<LinkCount>());
  }

  public void getLinkProfile(FreiLink link, LinkInfo info) {
    // TODO: Implement me.
    info.setLinkProfile(new LinkedList<LinkData>());
  }

  public Vector<FreiLink> getLinks(long time) {
    return links;
  }

  public Hashtable<String, Float> getNodeAvailability(long time) {
    // TODO: Implement me.
    return new Hashtable<String, Float>();
  }

  public Vector<FreiNode> getNodeList() {
    return nodes;
  }

}
