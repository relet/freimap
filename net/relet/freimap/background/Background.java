package net.relet.freimap.background;

import java.awt.Graphics2D;
import java.util.HashMap;

import net.relet.freimap.Configurator;
import net.relet.freimap.ColorScheme;
import net.relet.freimap.Converter;
import net.relet.freimap.VisorLayer;
import net.relet.freimap.DataSource;

/**
 * A <code>Background</code> instance is responsible for painting
 * the background of the application.
 * 
 * <p>An instance keeps track of the section of the world's map the
 * user is looking at and provides a {@link ColorScheme} to be used
 * for nodes and links which fits to the colors used by the background
 * itself.</p>
 * 
 * <p>Additionally the class provides various factory methods to create
 * an instance of a certain <code>Background</code> implementation.</p>
 * 
 * 
 * @author Robert Schuster <robertschuster@fsfe.org>
 * @author Thomas Hirsch <thomas.hirsch gmail com>
 *
 */
public abstract class Background extends VisorLayer {

	protected int zoom, width, height;
  protected int visible = VISIBILITY_FULL;

	/**
	 * Sets the width and height of the section the background is
	 * showing.
	 * 
	 * <p>This method must be called whenever the size changes
	 * otherwise calculations will get incorrect and drawing problems
	 * may occur.</p>
	 * 
	 * @param w
	 * @param h
	 */
	public final void setDimension(int w, int h) {
		width = w;
		height = h;
	}

  public void toggleVisibility() {
    visible = (visible + 1) % 2;
  }
  public int getVisibility() {
    return visible;
  }

  /**
   * returns null
   * 
   * @return null
   */
  public DataSource getSource() { return null; }

	/**
	 * Sets the <code>Background</code>s zoom.
	 * 
	 * <p>This method must be called whenever the zoom changes
	 * otherwise calculations will get incorrect and drawing problems
	 * may occur.</p>
	 * 
	 * @param zoom
	 */
	public final void setZoom(int zoom) {
		this.zoom = zoom;
		zoomUpdated();
	}
	
	/**
	 * This method is called whenever {@link #setZoom(int)}
	 * was called.
	 * 
	 * <p>Subclasses are encouraged to override this method to react
	 * upon changes to the zoom.</p>
	 *
	 */
	protected void zoomUpdated()
	{
		// To be overwritten by subclasses.
	}
	
	/**
	 * Retrieves a {@link ColorScheme} instance which is suitable for displaying
	 * nodes and links on top of this background.
	 * 
	 * <p>The default implementation returns {@link ColorScheme#NO_MAP}.</p>
	 * 
	 * <p>Subclasses should override this method if needed.</p>
	 * 
	 * @return
	 */
	public ColorScheme getColorScheme()
	{
		return ColorScheme.NO_MAP;
	}

	/**
	 * Paints the background.
	 * 
	 * <p>Subclasses must implement this method.</p>
	 * 
	 * @param g
	 */
	public abstract void paint(Graphics2D g);

	/**
	 * Creates a <code>Background</code> which paints nothing.
	 * 
	 * @return
	 */
	public static Background createBlankBackground() {
		return new Background() {
			public void paint(Graphics2D g) {
			};
		};
	}

	/**
	 * Creates a <code>Background</code> which paints 
	 * OpenStreetMap tiles.
	 * 
	 * @return
	 */
	public static Background createOpenStreetMapBackground(HashMap<String, Object> config) {
		return new OpenStreetMapBackground(config);
	}
	
	/**
	 * Creates a <code>Background</code> which paints static images.
	 * 
	 * @return
	 */
	public static Background createImagesBackground(HashMap<String, Object> config)
	{
		return new ImagesBackground(config);
	}
  
  @SuppressWarnings("unchecked")
  public static Background createOtherBackground(HashMap<String, Object> config) {
    try {
      String claz = Configurator.getS("type", config);
      Class<Background> cback=(Class<Background>)Class.forName(claz); //this cast cannot be checked!
      Background back = cback.newInstance();
      return back;
    } catch (Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }
 
  public void init(HashMap<String, Object> config) {
    //not used. may be overridden by subclasses ("other" backgrounds, mostly)
  }

	/**
	 * Evaluates the given <code>String</code> and creates
	 * an instance according to its value.
	 * 
	 * <p>If the value is <code>null</code> or not known the
	 * <code>Background</code> implementation which paints
	 * nothing is chosen.
	 * 
	 * @param type
	 * @return
	 */
	public static Background createBackground(HashMap<String, Object> config) {
    String type = Configurator.getS("type", config);
		if (type == null) {
			System.err.println("warning: no background specified. Defaulting to blank.");
			return createBlankBackground();
		}

		if (type.equalsIgnoreCase("blank"))
			return createBlankBackground();

		if (type.equalsIgnoreCase("images"))
			return createImagesBackground(config);

		if (type.equalsIgnoreCase("openstreetmap"))
			return createOpenStreetMapBackground(config);
      
    Background other = createOtherBackground(config);
    if (other!=null) {
      return other;
    }

		System.err.println("warning: no valid background specified (" + type
				+ "). Defaulting to blank.");
		return createBlankBackground();
	}
 
 //these are ignored.
 public void mouseMoved(double lat, double lon) {}
 public void mouseClicked(double lat, double lon, int button) {}
 public void setDisplayFilter(String match, int type, boolean cases, boolean regex) {}

}
