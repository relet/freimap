/* net.relet.freimap.GpsTrackLayer.java

  This file is part of the freimap software available at relet.net/trac/freimap

  This software is copyright (c)2007-2009 Thomas Hirsch <thomas hirsch gmail com>

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
import net.relet.freimap.background.*;

import java.io.*;
import java.net.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.MouseEvent;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.swing.ImageIcon;
import javax.swing.border.LineBorder;

public class GpsTrackLayer extends Background implements DataSourceListener {
  double scale;  // current scaling
  int zoom;       // zoom factor, according to OSM tiling
  int w, h;    //screen width, hight
  int cx, cy; //center of screen
  
  long crtTime = 0;

  Vector<FreiNode> nodes; //vector of known nodes
  Vector<Trackpoint> trackpoints = new Vector<Trackpoint>(); //vector of gps trackpoints
  
  //FIXME the following paragraph is identical and static in VisorFrame. Use these definitions and remove the paragraph.
  public static Font mainfont = new Font("SansSerif", 0, 12),
                     smallerfont = new Font("SansSerif", 0, 9);

  public Color fgcolor = VisorFrame.fgcolor,
               bgcolor = VisorFrame.bgcolor,
               fgcolor2 = VisorFrame.fgcolor2,
               bgcolor2 = VisorFrame.bgcolor2,

               activeblue = Color.cyan,
               activeyellow = Color.yellow,
               activegreen = Color.green,
               activewhite = Color.white,
               transred    = new Color(255,0,0,127),
               transgreen  = new Color(0,255,0,127),
               transyellow = new Color(255,255,0,127),
               transwhite  = new Color(255,255,255,127);
  public int   currentalpha = 255;
  
  DataSource source;
  ImageIcon icon;

  //FreiNode selectedNode;
  //FreiLink selectedLink;
  //double selectedNodeDistance,
  //       selectedLinkDistance;

  private FreiNode uplink = new FreiNode("0.0.0.0/0.0.0.0");

  private boolean transparent = true;

  int visible = VISIBILITY_FULL;

  String  filter=null;
  int     filterType  = 0;
  boolean filterCase  = false;
  boolean filterRegEx = false;

  boolean hideUnlocated = Configurator.getB(new String[]{"display", "hideUnlocated"});

  public GpsTrackLayer() {
  }
  public GpsTrackLayer(DataSource source) {
    this.initDataSource(source);
  }
  public void init(HashMap<String, Object> config) {
    String ssource = Configurator.getS("datasource", config);
    if (ssource!=null) {
      DataSource source = Visor.sources.get(ssource);
      if (source!=null) {
        this.initDataSource(source);
      } else {
        System.err.println("Warning: DataSource not available - "+ssource);
      }
    } else {
      System.out.println("datasource entry not found for GpsTrackLayer.");
    }
    String surl = Configurator.getS("url", config);
    if (surl!=null) {
      loadGpx(surl);
    }
    surl = Configurator.getS("icon", config);
    if (surl!=null) {
      try {
        icon = new ImageIcon(getClass().getResource(surl));
      } catch (Exception ex) {
        ex.printStackTrace();
        System.err.println("Could not load icon for GpxTrackLayer");
      }
    }
  }
  public void initDataSource(DataSource source) {  
    this.source=source;
    this.source.addDataSourceListener(this);

    System.out.println("reading node list.");
    nodes=source.getNodeList();
  }

  /* datasourcelistener */
  public void timeRangeAvailable(long from, long until) {
    //obsolete.
  }
  public void nodeListUpdate(FreiNode node) {
    if (nodes!=null) { //this really should not happen
      nodes.add(node);
    }
  }

  /**
   * returns the DataSource of this layer. If the layer is just decorative, returns null.
   * 
   * @return null or DataSource
   */

  public DataSource getSource() {
    return source;
  }

  /**
   * Paints the layer.
   * 
   * @param g, a Graphics2D object.
   */
  public void paint(Graphics2D g) {
    if (visible == 0) return;

    if (!transparent) {
      g.setColor(Color.black);
      g.fillRect(0,0,w,h);
    } 

    //g.setFont(VisorFrame.mainfont);

    //draw track
    Stroke trackStroke = new BasicStroke((float)(Math.min(2,0.00005 * scale)), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    g.setStroke(trackStroke);
    Trackpoint previous = null;
    Trackpoint next     = null;
    if (trackpoints.size()>0) {
      Trackpoint last = null;
      for(int i = 0; i < trackpoints.size(); i++) {
        Trackpoint tp = trackpoints.elementAt(i);
        if (last!=null) {
          g.setColor(activeblue);
          drawLineSeg(g, last.lon, last.lat, tp.lon, tp.lat, 0.0f, 1.0f);
        }
        if (previous==null) {
          if ((tp.time > crtTime) || (i+1==trackpoints.size())) {
            previous = last;
            next     = tp;
          }
        }
        last = tp;
      }
    }
    
    //draw "live" icon
    if (icon!=null) {
      double iptime =((double)crtTime-previous.time)/(next.time-previous.time);
      double iplat = previous.lat;// + (next.lat-previous.lat) * iptime;
      double iplon = previous.lon;// + (next.lon-previous.lon) * iptime;
      int wi = icon.getIconWidth();
      int hi = icon.getIconHeight();
    
      //draw link to closest node first
      FreiNode closest = getClosestNode(iplon, iplat);
      g.setColor(activeyellow);
      drawLineSeg(g, iplon, iplat, closest.lon, closest.lat, 0.0, 1.0);
      
      g.drawImage(icon.getImage(), converter.lonToViewX(iplon)-wi/2,  converter.latToViewY(iplat)-hi/2, null);
    }
  }

  private void drawLineSeg(Graphics2D g, double lon1, double lat1, double lon2, double lat2, double r1, double r2) {
    int x1 = converter.lonToViewX(lon1 + (lon2-lon1)*r1);
    int x2 = converter.lonToViewX(lon1 + (lon2-lon1)*r2);
    int y1 = converter.latToViewY(lat1 + (lat2-lat1)*r1);
    int y2 = converter.latToViewY(lat1 + (lat2-lat1)*r2);
    g.drawLine(x1,y1,x2,y2);
  }

  public boolean setCurrentTime(long crtTime) { 
    this.crtTime = crtTime; 
    return false;
  }

  public double sqr(double x) { 
    return x*x;
  }
  public double dist (double x1, double x2, double y1, double y2) {
    return Math.sqrt(sqr(x1-x2)+sqr(y1-y2));
  }

  public FreiNode getClosestNode(double lon, double lat) {
    double dmin=Double.POSITIVE_INFINITY;
    FreiNode closest=null, node;
    for (int i=0; i<nodes.size(); i++) {
      node=nodes.elementAt(i);
      double d = Math.abs(node.lon - lon) + Math.abs(node.lat - lat); //no need to sqrt here
      if (d<dmin) {
	      dmin=d;
	      closest=node;
      }
    }
//    selectedNodeDistance = (closest==null)?dmin:dist(closest.lon, lon, closest.lat, lat); //recalculate exact distance
    return closest;
  }


  /**
   * Indiciates whether this VisorLayer instance is transparent. 
   * 
   * @return true
   */

  public boolean isTransparent() {
    return transparent;
  }
  public int getVisibility() {
    return visible;
  }

  /**
   * Attempts to set transparency to this VisorLayer.
   */

  public void setTransparent(boolean t) {
    this.transparent=t;
  }

  public void toggleVisibility() {
    visible = (visible + 1) %3;
    resetColorScheme();
  }

  void resetColorScheme() {
    if (visible == VISIBILITY_DIM) {
      currentalpha = 63;
      fgcolor = new Color(20,200,20,currentalpha);     //used for text, lines etc.
      bgcolor = new Color(64,128,64,currentalpha/2);       //used for transparent backgrounds of most status boxes
      fgcolor2 = new Color(150,150,255,currentalpha);       //used for foreground of link status boxes
      bgcolor2 = new Color(40,40,192,currentalpha/2);       //used for transparent backgrounds of link status boxes
      activeblue = new Color(0,0,255,currentalpha);
      activewhite = new Color(255,255,255,currentalpha);
      activegreen = new Color(0,255,0,currentalpha);
      activeyellow = new Color(255,255,0,currentalpha);
    } else {
      currentalpha = 255;
      fgcolor = new Color(20,200,20);     //used for text, lines etc.
      bgcolor = new Color(64,128,64,196);       //used for transparent backgrounds of most status boxes
      fgcolor2 = new Color(150,150,255);       //used for foreground of link status boxes
      bgcolor2 = new Color(40,40,192,196);       //used for transparent backgrounds of link status boxes
      activeblue = Color.cyan;
      activewhite = Color.white;
      activegreen = Color.green;
      activeyellow = Color.yellow;
    }
  }


 /**
  * Sets the width and height of the section the layer is
  * showing.
  * 
  * <p>This method must be called whenever the size changes
  * otherwise calculations will get incorrect and drawing problems
  * may occur.</p>
  * 
  * @param w
  * @param h
  */
 //public void setDimension(int w, int h) {
 //  this.w=w; this.h=h;
 //  cx = w/2; cy = h/2;
 //}

 /**
  * Sets the <code>VisorLayer</code>s zoom.
  * 
  * <p>This method must be called whenever the zoom changes
  * otherwise calculations will get incorrect and drawing problems
  * may occur.</p>
  * 
  * @param zoom
  */
 public void zoomUpdated() {
   this.scale=converter.getScale();
 }

 public void mouseMoved(double lat, double lon) {
 }
 public void mouseClicked(double lat, double lon, int button) {
 }

 public void setDisplayFilter(String match, int type, boolean cases, boolean regex) {
   filter=match;
   filterType=type;
   filterCase=cases;
   filterRegEx=regex;
 }

 public void hideUnlocatedNodes(boolean hide) {
   hideUnlocated = hide;
 }

 Pattern reTrackpoint = Pattern.compile("<trkpt lat=\"(.*?)\" lon=\"(.*?)\"");
 Pattern reTime       = Pattern.compile("<time>(.*?)</time>");
 SimpleDateFormat df  = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //todo: time zone
 public void loadGpx(String surl) {
   System.out.println("Loading GPX data from file "+surl);
   try {
     InputStream is = new URL(surl).openStream();
     BufferedReader in = new BufferedReader(new InputStreamReader(is));
     Trackpoint tp = new Trackpoint();
     
     while (true) {
       String line = in.readLine();
       if (line==null) {break;}
       Matcher m = reTrackpoint.matcher(line);
       if (m.find()) {
         tp = new Trackpoint(Double.parseDouble(m.group(1)),Double.parseDouble(m.group(2)));
       }
       m = reTime.matcher(line);
       if (m.find()) {
         Date date = df.parse(m.group(1));
         //System.out.println(date + "\t" + date.getTime());
         tp.setDate(date.getTime()/1000);
         trackpoints.addElement(tp);
       }
     }
     in.close();
   } catch (Exception ex) {
     ex.printStackTrace();
     System.err.println("Error: Could not read GPX file.");
   }
 }

 class Trackpoint {
   public double lat, lon;
   public long time; //unix time stamp
   
   public Trackpoint() {
   }  
   public Trackpoint(double lat, double lon) {
     this.lat = lat;
     this.lon = lon;
   }
   public void setDate(long time) {
     this.time = time;
   }
 }

}
