/* net.relet.freimap.VisorLayer.java

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

import java.awt.Graphics2D;

public abstract class VisorLayer {
  public static final int VISIBILITY_NOT  = 0;
  public static final int VISIBILITY_FULL = 1;
  public static final int VISIBILITY_DIM  = 2;

  protected Converter converter;

  /**
   * Paints the layer.
   * 
   * @param g, a Graphics2D object.
   */
  public abstract void paint(Graphics2D g);


  /**
   * returns the DataSource of this layer. If the layer is just decorative, returns null.
   * 
   * @return null or DataSource
   */

  public abstract DataSource getSource();

  /**
   * Indiciates whether this VisorLayer instance is transparent. 
   * 
   * @return true or false
   */
  public boolean isTransparent() {
    return false;
  }

  /**
   * Attempts to set transparency to this VisorLayer.
   */

  public void setTransparent(boolean t) {
  }

 /**
   * Sets the scaling converter for this background.
   */

  public void setConverter(Converter conv) {
    converter = conv;
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
 public void setDimension(int w, int h) {}

 /**
  * Sets the <code>VisorLayer</code>s zoom.
  * 
  * <p>This method must be called whenever the zoom changes
  * otherwise calculations will get incorrect and drawing problems
  * may occur.</p>
  * 
  * @param zoom
  */
 public void setZoom(int zoom) {}

 /** retrieves selected layer visibility */
 public int getVisibility() {
   return VISIBILITY_FULL;
 }
 /** toggles visibility between available modes */
 public void toggleVisibility() {}
 /** sets or unsets display filter */
 public static final int FILTER_IP         = 0 ;
 public static final int FILTER_IDENTIFIER = 1 ;
 public void setDisplayFilter(String match, int type, boolean cases, boolean regex) {}
 public void hideUnlocatedNodes(boolean hide) {}

 /**
  * Sets the current point in time to be displayed
  * 
  * @param crtTime, an unix time stamp
  * @return true, if the layer has to be repainted consequently
  */
 public boolean setCurrentTime(long crtTime) {
   return false;
 }

 public void mouseMoved(double lat, double lon) {}
 public void mouseClicked(double lat, double lon, int button) {}

}
