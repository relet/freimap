package net.relet.freimap.background;

import java.awt.Graphics2D;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.ImageIcon;

import net.relet.freimap.Configurator;
import net.relet.freimap.OsmMercatorProjection;

/**
 * In a distant future this class will be a fully featured cache for downloaded
 * tiles. It will allow retrieving tiles (based on their zoom, x and y
 * specifier), delayed downloading and automatic discarding tiles (when memory
 * gets sparse).
 * 
 * @author Robert Schuster
 * 
 */
class TileCache extends Thread {

	public static String TILE_SERVER_URL;
        public static String tileServer;
        
	HashMap<Long, Tile> cache = new HashMap<Long, Tile>();

	private ImageIcon REPLACEMENT;

	private LinkedList<Tile> loadQueue = new LinkedList<Tile>();

	private TilePainter tp;
        HashMap<String, Object> config;

	private volatile int zoom;

	TileCache(TilePainter tp, HashMap<String, Object> config) {
		this.tp = tp;
 		this.config = config;

                tileServer = Configurator.getS("tileserver", config);
		if ((tileServer == null)||(tileServer.equals("mapnik"))) {
			TILE_SERVER_URL = "http://tile.openstreetmap.org/mapnik/";
		} else if (tileServer.equals("osmarender")) {
			TILE_SERVER_URL = "http://dev.openstreetmap.org/~ojw/Tiles/tile.php/";
		} else {
			System.out.println("Unknown tile server. Using user provided URL "+tileServer);
			TILE_SERVER_URL = tileServer;
		}
     
		String bgfilter = Configurator.getS("filter", config);
		String resource = (bgfilter != null) && (bgfilter.equals("dark")) ? "gfx/loading_black.png" : "gfx/loading_white.png";
		REPLACEMENT = new ImageIcon(ClassLoader
				.getSystemResource(resource));

		setDaemon(true);

		start();

	}

	public void run() {
		while (true) {
			Tile t;

			synchronized (loadQueue) {
				while (loadQueue.isEmpty()) {
					try {
						loadQueue.wait();
					} catch (InterruptedException _) {
						// Unexpected.
					}
				}

				t = loadQueue.removeFirst();
			}
			
			// Discard loading tiles which do not have the current
			// visible zoom.
			if (t.zoom != zoom)
				continue;

			//System.err.println("fetching image:" + t.url);
			
			t.loadImage();
		}
	}

	private void createTile(int zoom, int tx, int ty) {
    if (zoom>18) return;
		String tileName = zoom + "/" + tx + "/" + ty + ".png";

		try {
			URL url = new URL(TILE_SERVER_URL + tileName);
			Tile e = new Tile(url, zoom, tx, ty);

			cache.put(key(zoom, tx, ty), e);
		} catch (MalformedURLException e) {
			// Unexpected
			System.err.println("invalid url: " + e);
		}
	}

	private long key(long zoom, long tx, long ty) {
		return (zoom << 24) + (ty << 16) + tx;
	}
	
	void setZoom(int z)
	{
		zoom = z;
	}

	void paintTiles(Graphics2D g, int zoom, int wx, int wy, int ww, int wh) {
		int max = (int) Math.pow(2, zoom) - 1;
		int x1 = OsmMercatorProjection.worldToTile(Math.max(wx, 0)); 
		int x2 = Math.min(OsmMercatorProjection.worldToTile(wx + ww), max); 
		int y1 = OsmMercatorProjection.worldToTile(Math.max(wy, 0)); 
		int y2 = Math.min(OsmMercatorProjection.worldToTile(wy + wh), max); 
		
		if (zoom <= 18) {
 		  for (int ty = y1; ty <= y2; ty++) {
			for (int tx = x1; tx <= x2; tx++) {
				Tile tile = cache.get(key(zoom, tx, ty));
				if (tile == null) {
					createTile(zoom, tx, ty);
  					tp.paint(g, REPLACEMENT.getImage(), tx << 8, ty << 8);
				} else if (tile.image == null) {
					// Image is not there.
					
					if (tile.shouldLoad()) {
						
						// Triggers image loading.
						synchronized (loadQueue) {
							loadQueue.addLast(tile);
							loadQueue.notifyAll();
						}
					}
		 			tp.paint(g, REPLACEMENT.getImage(), tx << 8, ty << 8);
					
				} else
					tp.paint(g, tile.image, tx << 8, ty << 8);

			}
		  }
		}
	}

}
