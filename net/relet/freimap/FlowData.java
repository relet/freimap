/* net.relet.freimap.FlowData.java

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

/** FlowData contains the information associated with a single traffic flow.
 */
public class FlowData {
  public long begin, end, packets, bytes;
  public int protocol;

  /** Create one flow information object 
      @param  begin     timestamp of the first packet in this flow
      @param  end       timestamp of the last packet associated with this flow
      @param  packets   number of packets in this flow
      @param  bytes     number of bytes in this flow
      @param  protocol  traffic protocol used in this flow. Values conform to netflow specification. 
  */
  public FlowData (long begin, long end, long packets, long bytes, int protocol) {
    this.begin    = begin;
    this.end      = end;
    this.packets  = packets;
    this.bytes    = bytes;
    this.protocol = protocol;
  }
}
