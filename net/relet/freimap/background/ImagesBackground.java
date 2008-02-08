package net.relet.freimap.background;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.util.*;

import javax.swing.ImageIcon;

import net.relet.freimap.Configurator;

/**
 * A {@link Background} implementation which displays images at certain
 * geographical locations.
 * 
 * <p>The images are specified within the configuration file.</p>
 * 
 */
class ImagesBackground extends Background {

	Vector<Element> bgitems = new Vector<Element>();

  @SuppressWarnings("unchecked")
	ImagesBackground(HashMap<String, Object> config) {
    ArrayList<HashMap<String, Object>> images = (ArrayList<HashMap<String, Object>>)Configurator.get("images", config);
    Iterator<HashMap<String, Object>> i = images.iterator();
    while (i.hasNext()) {
      		HashMap<String, Object> iconf = i.next();
		String iname = Configurator.getS("gfx", iconf);
 		try {
			ImageIcon ii = new ImageIcon(ClassLoader.getSystemResource(iname));
			if (ii!=null) {
				bgitems.addElement(new Element(ii,
              			Configurator.getD("lon", iconf), 
              			Configurator.getD("lat", iconf), 
              			Configurator.getD("scale", iconf)));
			} else {
				System.err.println("Could not create background image: "+ iname);
                        }
		} catch (Exception ex) {
			System.err.println("Could not find background image:" + iname);
                }
	}
    }

	public void paint(Graphics2D g) {
    if (visible == 0) return;
		// draw backgrounds
		for (int i = 0; i < bgitems.size(); i++) {
			Element e = bgitems.elementAt(i);
			Image img = e.gfx.getImage();
			double w2 = img.getWidth(null) / 2;
			double h2 = img.getHeight(null) / 2;
			double rscale = converter.getScale() / e.scale;

			int xc = converter.worldToViewX((int) (converter.lonToWorld(e.lon) - w2
					* rscale));
			int yc = converter.worldToViewY((int) (converter.latToWorld(e.lat) - h2
					* rscale));

			g.drawImage(img, new AffineTransform(rscale, 0d, 0d, rscale, xc, yc), null);
		}
	}

	static class Element {
		  public ImageIcon gfx;
		  public double lon, lat, scale;

		  public Element (ImageIcon gfx, double lon, double lat, double scale) {
		    this.gfx=gfx;
		    this.lon=lon;
		    this.lat=lat;
		    this.scale=scale;
		  }
		  
	}
}
