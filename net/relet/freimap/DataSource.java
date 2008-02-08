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

import java.util.*;

/** The DataSource interface specifies a single source of node, link and related 
    data. It will be called by the Freimap core classes, i.e. {@link VisorFrame} and 
    {@link Visor} in the following general order:
    <ol>
      <li>Usually, a {@link DataSourceListener} is registered first.</li>
      <li>Then, by calling getFirst- and getLastUpdateTime, the initial time interval 
          to be displayed is determined. </li>
      <li>A call to getNodeList should return an initial Vector of {@link Freinode}s.</li>
      <li>Optionally, getNodeAvailability may return an availability value for each node. 
          That's a bit of a hack, and more general node properties will be implemented soon.</li>
    </ol>
    The initiation phase is thus concluded. In the following, the core will repeatedly 
    request getClosestUpdateTime for unix timestamps which it seeks to display. Ususally, 
    this time value will then be used to retrieve the actual link data from getLinks. 
    The core may also request to retrieve a LinkInfo profile (for FreiLinks) or a NodeProfile
    (for FreiNodes) using the methods getLinkProfile and getLinkCountProfile (to be renamed). 
    These methods may directly alter the LinkInfo and NodeInfo objects and set the appropriate 
    status flags.
    At any time, the methods in {@link DataSourceListener} may be called by the DataSource to 
    extend the available time range or add new nodes. 
*/

public interface DataSource {

/** Initialize this data source with its configuration parameters. **/
  public void init(HashMap<String, Object> configuration);

/** @return	A Vector of FreiNodes to be displayed. */
  public Vector<FreiNode> getNodeList();
/** <b>To be deprecated soon.</b>
    @param	time	ignored in current implementations.
    @return	A Hashtable mapping Node IDs to a float value between 0 and 1.
*/
  public Hashtable<String, Float> getNodeAvailability(long time);

/** @return	The first unix timestamp which can be displayed. */
  public long getFirstUpdateTime();
/** @return	The last unix timestamp which can be displayed. */
  public long getLastUpdateTime();
/** This method approximates a requested time with the closest timestamp which can actually be 
    displayed.
    @param	time	A timestamp to be approximated 
    @return	the next best timestamp.
*/
/** @return The first unix timestamp which has been pre-fetched. */
  public long getLastAvailableTime();
/** @return The last unix timestamp which has been pre-fetched. */
  public long getFirstAvailableTime();
  
  public long getClosestUpdateTime(long time);
/** Returns link data for a given timestamp. May return null if the timestamp has not been 
    provided by getClosestUpdateTime.
    @param	time	A timestamp
    @return	All links which should be displayed at this given time.
*/

  public FreiNode getNodeByName(String id);

  public Vector<FreiLink> getLinks(long time);
/** @param 	dsl	A DataSourceListener listening on events from this DataSource */
  public void addDataSourceListener(DataSourceListener dsl);
/** Extendend link information is requested using this method. 
    @param	link	the link on which information is sought
    @param	info	A LinkInfo structure to be filled with this information. 
*/
  public void getLinkProfile(FreiLink link, LinkInfo info); 
/** Extendend node information is requested using this method. 
    @param	node	the node on which information is sought
    @param	info	A NodeInfo structure to be filled with this information. 
*/
  public void getLinkCountProfile(FreiNode node, NodeInfo info); 
}
