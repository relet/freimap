/* net.relet.freimap.FreiNode.java

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

import java.util.HashMap;
import java.io.Serializable;

public class FreiNode implements Comparable, Serializable {
  public static double DEFAULT_LAT = 52.520869; //alexanderplatz, berlin, currently.
  public static double DEFAULT_LON = 13.409457;

  public String id;
  public String fqid; //fully qualified identifier - for display only
  public double lon=Double.NaN, lat=Double.NaN;
  public double lonsum=0, latsum=0; //used only for real time interpolation
  public double nc=0;
  public boolean unlocated=false;
  public HashMap<String, Object> attributes = new HashMap<String, Object>();
  
  public FreiNode() {} //serializable

  public FreiNode(String id) {
    this(id, id); // use id as fqid
  }
  public FreiNode(String id, String fqid) {
    this.id=id;
    this.fqid=fqid;
    this.lat = DEFAULT_LAT; //when no coordinates are known, place at default position to allow interpolation
    this.lon = DEFAULT_LON;
    this.unlocated=true; 
  }
  public FreiNode(String id, double lon, double lat) {
    this(id, id, lon, lat);
  }
  public FreiNode(String id, String fqid, double lon, double lat) {
    this.id=id;
    this.fqid=fqid;
    this.lon=lon;
    this.lat=lat;
  }

  public int hashCode() {
    return id.hashCode();
  }

  public int compareTo(Object o) {
    return id.compareTo(((FreiNode)o).id);
  }

  FreiNode eqo;
  public boolean equals(Object o) {
    if (!(o instanceof FreiNode)) return false;
    eqo = (FreiNode)o;
    if (this.id.equals(eqo.id)) return true;
    /*if (this.fqid != null) {
      if (this.fqid.equals(eqo.id)) return true;
      if (eqo.fqid != null) {
        if (this.fqid.equals(eqo.fqid)) return true;
        if (eqo.fqid.equals(this.id)) return true;
      }
    }*/
    return false;
  }
  public String toString() {
    return fqid;
  }
}
