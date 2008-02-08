/* net.relet.freimap.LinkInfo.java

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
import org.jfree.chart.renderer.xy.*;
import org.jfree.data.xy.*;

public class LinkInfo {
  public static final int CHART_WIDTH = 100;

  public static final int STATUS_FETCHING  = 0;
  public static final int STATUS_AVAILABLE = 100;
  public static final int STATUS_FAILED    = -100;

  JFreeChart linkChart;
  public int status = STATUS_FETCHING;

  public void setLinkProfile(LinkedList<LinkData> lp) {

    XYSeries data = new XYSeries("etx");
    XYSeries avail = new XYSeries("avail");
    XYSeriesCollection datac = new XYSeriesCollection(data);
    datac.addSeries(avail);
    linkChart = ChartFactory.createXYLineChart("average link etx\r\naverage link availability", "time", "etx", datac, PlotOrientation.VERTICAL, false, false, false);
    sexupLayout(linkChart);

    long first=lp.getFirst().time,
         last =lp.getLast().time,
         lastClock = first,
         count = 0,    //number of samplis in aggregation timespan
         maxCount = 0; //max idem
    long aggregate = (last-first) / CHART_WIDTH; //calculate aggregation timespan: divide available timespan in CHART_WIDTH equal chunks
    double sum=0;

/* ok, this ain't effective, we do it just to pre-calculate maxCount */
    ListIterator<LinkData> li = lp.listIterator();
    while (li.hasNext()) { 
        LinkData ld = li.next();
        count++;
        if (ld.time - lastClock > aggregate) {
          if (maxCount < count) maxCount = count;
          lastClock=ld.time;
          count=0;
        }
    }

    //reset for second iteration
    count = 0; 
    lastClock = first;

    //iterate again
    li = lp.listIterator();
    while (li.hasNext()) { 
        LinkData ld = li.next();
        
        sum += ld.quality;
        count++; 
        
        if (aggregate==0) aggregate=1000;//dirty hack
        if (ld.time - lastClock > aggregate) {
          for (long i = lastClock; i<ld.time - aggregate; i+=aggregate) {
            data.add(i * 1000, (i==lastClock)?sum/count:Double.NaN); 
            avail.add(i * 1000, (i==lastClock)?((double)count/maxCount):0);
          }

          count=0; sum=0;
          lastClock=ld.time;
    	}
    }

    status = STATUS_AVAILABLE;
  }

  public void setFlowProfile(LinkedList<FlowData> lp) {

    XYSeries packets = new XYSeries("packets");
    XYSeries bytes   = new XYSeries("bytes");
    XYSeries icmp    = new XYSeries("icmp");
    XYSeries tcp     = new XYSeries("tcp");
    XYSeries udp     = new XYSeries("udp");
    XYSeries other   = new XYSeries("other");

    XYSeriesCollection data1 = new XYSeriesCollection(bytes);
    XYSeriesCollection data2 = new XYSeriesCollection(packets);
    XYSeriesCollection data3 = new XYSeriesCollection(icmp);
    data3.addSeries(tcp);
    data3.addSeries(udp);
    data3.addSeries(other);

    //linkChart = ChartFactory.createXYLineChart("packets, bytes\r\nicmp, tcp, udp other", "time", "count", data1, PlotOrientation.VERTICAL, false, false, false);
    ValueAxis domain = new DateAxis();
    ValueAxis range1 = new NumberAxis();
    ValueAxis range2 = new NumberAxis();
    ValueAxis range3 = new NumberAxis();
    CombinedDomainXYPlot plot = new CombinedDomainXYPlot(domain);
    plot.add(new XYPlot(data2,domain,range1,new XYLineAndShapeRenderer(true, false)));
    plot.add(new XYPlot(data1,domain,range2,new XYLineAndShapeRenderer(true, false)));
    plot.add(new XYPlot(data3,domain,range1,new XYLineAndShapeRenderer(true, false)));
    linkChart = new JFreeChart(plot);
    linkChart.setTitle("");
    sexupLayout(linkChart);

    long min=lp.getFirst().begin,
         max =lp.getLast().end;

    for (float i=0.0f; i<1000.0f; i+=1.0f) {
      long cur = min + (long)((max-min)*(i/1000.0));

      long cpackets = 0;
      long cbytes   = 0;
      long cicmp    = 0; 
      long ctcp     = 0;
      long cudp     = 0;
      long cother   = 0;

      Iterator<FlowData> li=lp.iterator();
      while (li.hasNext()) {
        FlowData data=li.next();
        if (data.begin>cur) break;
        if (data.end<cur) continue;
        cpackets+=data.packets;
        cbytes  +=data.bytes;
        switch (data.protocol) {
          case 1: {
            cicmp += data.packets;
            break;
          }
          case 6: {
            ctcp += data.packets;
            break;
          }
          case 17: {
            cudp += data.packets;
            break;
          }
          default: {
            cother += data.packets;
            break;
          }
        }
      }

      packets.add(cur, cpackets);
      bytes.add  (cur, cbytes);
      icmp.add   (cur, cicmp);
      tcp.add    (cur, ctcp);
      udp.add    (cur, cudp);
      other.add  (cur, cother);
    }

    status = STATUS_AVAILABLE;
  }

  private void sexupAxis(ValueAxis axis) {
    axis.setLabelFont(VisorFrame.smallerfont);
    axis.setLabelPaint(VisorFrame.fgcolor2);
    axis.setTickLabelFont(VisorFrame.smallerfont);
    axis.setTickLabelPaint(VisorFrame.fgcolor2);
  }
  private void sexupPlot(Plot plot) {
    if (plot instanceof CombinedDomainXYPlot) {
      List<Plot> subs = (List<Plot>)(((CombinedDomainXYPlot)plot).getSubplots());
      Iterator<Plot> i = subs.iterator();
      while (i.hasNext()) {
        Plot p = i.next();
        sexupPlot(p);
      }
    } else if (plot instanceof XYPlot) {
      XYPlot xyplot=(XYPlot)plot;
      xyplot.setBackgroundPaint(VisorFrame.bgcolor2);
      xyplot.setDomainAxis(new DateAxis());
      sexupAxis(xyplot.getDomainAxis());
      sexupAxis(xyplot.getRangeAxis());
    }
  }
  private void sexupLayout(JFreeChart chart) {
    chart.setAntiAlias(true);
    chart.setBackgroundPaint(VisorFrame.bgcolor2);
    chart.setBorderVisible(false);
    TextTitle title=chart.getTitle();
    title.setFont(VisorFrame.smallerfont);
    title.setPaint(VisorFrame.fgcolor2);
    Plot plot=chart.getPlot();
    sexupPlot(plot);
  }
}
