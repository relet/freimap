/* net.relet.freimap.Visor.java

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

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.*;
import javax.swing.*;

import net.relet.freimap.background.Background;

public class Visor extends JFrame implements WindowListener {
  public static Configurator config;
  public static HashMap<String, DataSource> sources;
  public static HashMap<String, Background> backgrounds;

  @SuppressWarnings("unchecked")
  public static void main(String[] args) {
    config=new Configurator();
    
    sources = new HashMap<String, DataSource>();
    try {
      HashMap<String, Object> ds = (HashMap<String, Object>)config.get("datasources");
      HashMap<String, HashMap<String, Object>> ds2subconfig = new HashMap<String, HashMap<String, Object>>();
      Iterator<String> i = ds.keySet().iterator();
      while (i.hasNext()) {
        String id   = i.next();
        HashMap<String, Object> subconfig = (HashMap<String, Object>) ds.get(id);
        String claz = config.getS("class", subconfig);
        Class<DataSource> csource=(Class<DataSource>)Class.forName(claz); //this cast cannot be checked!
        DataSource source = csource.newInstance();
        ds2subconfig.put(id, subconfig);
        sources.put(id, source);
      }
      Iterator<String> j = sources.keySet().iterator();
      while (j.hasNext()) {
        String id = j.next();
        DataSource source = sources.get(id);
        source.init(ds2subconfig.get(id)); //initialize datasource with configuration parameters
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return;
    }

    backgrounds = new HashMap<String, Background>();
    try {
      Object o = config.get("backgrounds");
      if (o != null) {
        HashMap<String, Object> bgs = (HashMap<String, Object>)o;
        Iterator<String> i = bgs.keySet().iterator();
        while (i.hasNext()) {
          String id   = i.next();
          HashMap<String, Object> subconfig = (HashMap<String, Object>) bgs.get(id);
          Background newbg = Background.createBackground(subconfig);
          backgrounds.put(id, newbg);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      return;
    }
   
    new Visor();
  }
  
  VisorFrame viz;

  /* this will be implemented some future time */
  JMenuBar  bar = new JMenuBar();
  JMenu     m_source  = new JMenu("Source");
  JMenuItem mi_open   = new JMenuItem("Open...");
  JMenu     m_back    = new JMenu("Background");
  JMenuItem mi_select = new JMenuItem("Select...");
  JMenu     m_view    = new JMenu("View");
  JMenuItem mi_interp = new JCheckBoxMenuItem("Show interpolated nodes");
  JMenuItem mi_filter = new JMenuItem("Display Filter...");
  JMenu     m_help    = new JMenu("Help");
  JMenuItem mi_about  = new JMenuItem("About");
  

  public Visor() {
    super("http://relet.net/trac/freimap");
    
    initLayout();

    try {
      while (true) {
        Thread.sleep(1);
        viz.nextFrame(); //todo: make this mouse controlled
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }  
  
  void initLayout() {
    viz=new VisorFrame();
    Iterator<String> i = backgrounds.keySet().iterator();
      while (i.hasNext()) {
      String id = i.next();
      viz.addLayer(id, backgrounds.get(id), true);
    }
    i = sources.keySet().iterator();
      while (i.hasNext()) {
      String id = i.next();
      viz.addLayer(id, new NodeLayer(sources.get(id)), true);
    }
    Container c = this.getContentPane();
    
    Color c_menu = Configurator.getC(new String[]{"colorscheme", "menu"});
    if (c_menu==null) {c_menu = new Color(12,64,12);}
    Color c_back = Configurator.getC(new String[]{"colorscheme", "background"});
    if (c_menu==null) {c_back = Color.black;}

    m_source.add(mi_open);
    mi_about.addActionListener(viz);
    m_help.add(mi_about);
    //bar.add(m_source);
    //bar.add(m_back);
    mi_interp.addActionListener(viz);
    mi_interp.setSelected(true);
    mi_filter.addActionListener(viz);
    m_view.add(mi_interp);
    m_view.add(mi_filter);
    bar.add(m_view);
    bar.add(m_help);
    this.setJMenuBar(bar);

    bar.setBackground(c_menu);
    m_view.setForeground(viz.fgcolor);
    m_help.setForeground(viz.fgcolor);

    c.add(viz);
    c.setBackground(c_back); /* this is overridden by the visorframe drawing routines anyway */
    this.pack();
    this.setVisible(true);
    this.addWindowListener(this);
  }
  
  public void windowActivated(WindowEvent e) {}
  public void windowClosed(WindowEvent e) {}
  public void windowClosing(WindowEvent e) {
    System.exit(0);
  }
  public void windowDeactivated(WindowEvent e) {}
  public void windowDeiconified(WindowEvent e) {}
  public void windowIconified(WindowEvent e) {}
  public void windowOpened(WindowEvent e) {}
}
