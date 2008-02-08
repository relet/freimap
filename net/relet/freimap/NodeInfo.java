/* net.relet.freimap.NodeInfo.java

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

import org.jfree.chart.*;
import org.jfree.chart.axis.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.title.*;
import org.jfree.data.xy.*;


public class NodeInfo {
  public static final int CHART_WIDTH = 100;

  public static final int STATUS_FETCHING  = 0;
  public static final int STATUS_AVAILABLE = 100;
  public static final int STATUS_FAILED    = -100;

  //Hashtable<Long,Integer> linkCountProfile;
  JFreeChart linkCountChart;
  public int minLinks=Integer.MAX_VALUE, maxLinks=-1;
  public int status = STATUS_FETCHING;

  public void setLinkCountProfile(LinkedList<LinkCount> lcp) {
    if (lcp.size()==0) {
    	minLinks=0;
	    maxLinks=0;
      return;
    } 

    XYSeries data = new XYSeries("links");
    XYSeries avail = new XYSeries("avail");
    XYSeriesCollection datac = new XYSeriesCollection(data);
    datac.addSeries(avail);
    linkCountChart = ChartFactory.createXYLineChart("average incoming link count\r\nincoming link availability", "time", "count",datac, PlotOrientation.VERTICAL, false, false, false);
    sexupLayout(linkCountChart);

    long first=lcp.getFirst().time,
         last =lcp.getLast().time,
         lastClock = first,
         count = 0,
         maxCount = 0;
    long aggregate = (last-first) / CHART_WIDTH;
    double sum=0;

/* ok, this ain't effective, we do it just to pre-calculate maxCount */
    ListIterator<LinkCount> li = lcp.listIterator();
    while (li.hasNext()) { 
        LinkCount lc = li.next();
        count++;
        if (lc.time - lastClock > aggregate) {
          if (maxCount < count) maxCount = count;
          lastClock=lc.time;
          count=0;
        }
    }

    //reset for second iteration
    count = 0; 
    lastClock = first;

    //iterate again
    li = lcp.listIterator();
    while (li.hasNext()) { 
        LinkCount lc = li.next();
        if (minLinks>lc.count) minLinks=lc.count;
        if (maxLinks<lc.count) maxLinks=lc.count;

        sum += lc.count;
        count++; 
        
        if (aggregate==0) aggregate=1000;//dirty hack
        if (lc.time - lastClock > aggregate) {
          for (long i = lastClock; i<lc.time - aggregate; i+=aggregate) {
            data.add(i * 1000, (i==lastClock)?sum/count:Double.NaN); 
            avail.add(i * 1000, (i==lastClock)?((double)count/maxCount):0);
          }

          count=0; sum=0;
          lastClock=lc.time;
    	}
    }

    status = STATUS_AVAILABLE;
  }

  private void sexupAxis(ValueAxis axis) {
    axis.setLabelFont(VisorFrame.smallerfont);
    axis.setLabelPaint(VisorFrame.fgcolor);
    axis.setTickLabelFont(VisorFrame.smallerfont);
    axis.setTickLabelPaint(VisorFrame.fgcolor);
  }
  private void sexupLayout(JFreeChart chart) {
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(VisorFrame.bgcolor);
    chart.setBorderVisible(false);
    TextTitle title=chart.getTitle();
    title.setFont(VisorFrame.smallerfont);
    title.setPaint(VisorFrame.fgcolor);
    XYPlot plot=chart.getXYPlot();
    plot.setBackgroundPaint(VisorFrame.bgcolor);
    plot.setDomainAxis(new DateAxis());
    sexupAxis(plot.getDomainAxis());
    sexupAxis(plot.getRangeAxis());
  }
}
