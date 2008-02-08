package net.relet.freimap.background;

import java.awt.Graphics2D;
import java.awt.Image;
import java.util.HashMap;


import net.relet.freimap.ColorScheme;
import net.relet.freimap.Configurator;

/**
 * A {@link Background} implementation which paints tiles
 * from OpenStreetMap.
 * 
 * @author Robert Schuster <robertschuster@fsfe.org>
 *
 */
class OpenStreetMapBackground extends Background {
	TileCache tileCache;
        HashMap<String, Object> config;

	OpenStreetMapBackground(HashMap<String, Object> config) {
          this.config = config;
          Tile.init(config);

    TilePainter tp = new TilePainter() {
			public void paint(Graphics2D g, Image image, int worldX, int worldY) {
				g.drawImage(image, converter.worldToViewX(worldX), converter.worldToViewY(worldY),
						null);
			}
		};

		tileCache = new TileCache(tp, config);
	}

	public void paint(Graphics2D g) {
    if (visible == 0) return;
		tileCache.paintTiles(g, zoom, converter.offsetX, converter.offsetY, width, height);
	}
	
	protected void zoomUpdated()
	{
		tileCache.setZoom(zoom);
	}

	public ColorScheme getColorScheme()
	{
		String filter = Configurator.getS("filter", config);
		if ((filter!=null)&&(filter.equals("dark"))) {
			return ColorScheme.NO_MAP;
		}
		return ColorScheme.OSM_MAP;
	}
}
