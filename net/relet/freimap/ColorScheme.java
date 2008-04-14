package net.relet.freimap;

import java.awt.Color;
import java.util.HashMap;

public class ColorScheme
{
	static enum Key {
	 MAP_BACKGROUND,
	 NODE_UNLOCATED,
	 NODE_UNAVAILABLE,
	 NODE_UPLINK,
	 NODE_HIGHLIGHT;
	}
	
	public static final ColorScheme NO_MAP = new ColorScheme();
	public static final ColorScheme OSM_MAP = new ColorScheme();
	
	static
	{
		NO_MAP.put(Key.MAP_BACKGROUND, Color.BLACK);
		NO_MAP.put(Key.NODE_UNAVAILABLE, Color.WHITE);
		NO_MAP.put(Key.NODE_UNLOCATED, Color.YELLOW);
		NO_MAP.put(Key.NODE_UPLINK, Color.WHITE);
		NO_MAP.put(Key.NODE_HIGHLIGHT, Color.WHITE);

		OSM_MAP.put(Key.MAP_BACKGROUND, Color.LIGHT_GRAY);
		OSM_MAP.put(Key.NODE_UNAVAILABLE, Color.BLACK);
		OSM_MAP.put(Key.NODE_UNLOCATED, Color.DARK_GRAY);
		OSM_MAP.put(Key.NODE_UPLINK, Color.RED);
		OSM_MAP.put(Key.NODE_HIGHLIGHT, Color.BLACK);
	}
	
	private HashMap<Key, Color> map = new HashMap<Key, Color>();
	
	Color getColor(Key key)
	{
		return map.get(key);
	}
	
	private void put(Key key, Color c)
	{
		map.put(key, c);
	}
}
