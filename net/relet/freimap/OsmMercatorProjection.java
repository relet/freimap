package net.relet.freimap;

/**
 * Represents a Mercator projection of the earth in the way it is used by
 * OpenStreetMap, Google Maps and others.
 * 
 * The result of transforming longtitude and latitude into the
 * mercator projection are pixel coordinates which start at the top left
 * of the map. Geographically this is at the international date line
 * slightly above 85°N.
 * 
 * By definition a zoom level has to be provided. Along with a known
 * number of pixels in a tile earth' circumference and radius in
 * pixels at the given zoom level is calculated.
 * 
 * @author Robert Schuster
 *
 */
public final class OsmMercatorProjection {

	/**
	 * Fixed number of pixels in a tile.
	 */
	final static int TILE_SIZE = 256;

	/**
	 * Denotes earth' radius in pixels.
	 */
	final double radius;
	
	/**
	 * Denotes the number of tiles in the current zoom level.
	 */
	final int tiles;
	
	/**
	 * Earth circumference (at the equator) in pixels.
	 */
	final double circumference;
	
	/**
	 * The zoom level, a value from the interval [0, 17].
	 * 
	 * The upper bound is arbitrarily chosen.
	 */
	final int zoom;
	
	final double falseEasting;
	
	final double falseNorthing;

	public OsmMercatorProjection(int zoom) {
		this.zoom = zoom;
		
		tiles = (int) Math.pow(2, zoom);
		
		circumference = TILE_SIZE * tiles;
		
		radius = circumference / (2 * Math.PI);
		
		falseEasting = -circumference/2.0;
		falseNorthing = circumference/2.0;
	}

	/**
	 * Converts longitude (degrees) into the horizontal pixel
	 * value. 
	 * 
	 * @param longitude
	 * @return
	 */
	int lonToX(double longitude) {
		return (int) ((radius * Math.toRadians(longitude)) - falseEasting);
	}

	/**
	 * Converts the latitude into the vertical pixel value.
	 * 
	 * @param latitude
	 * @return
	 */
	int latToY(double latitude) {
		latitude = Math.toRadians(latitude);

		return (int) (-(radius
				/ 2.0
				* Math.log((1.0 + Math.sin(latitude))
						/ (1.0 - Math.sin(latitude)))) + falseNorthing);
	}
	
	double xToLong(double x)
	{
		x += falseEasting;
		
		double longitude = Math.toDegrees(x/radius);
		
		while (longitude < -180)
			longitude += 180;

		while (longitude > 180)
			longitude -= 180;
	    
	    return longitude;
	}
	
	double yToLat(double y)
	{
	   y = falseNorthing - y;
		
       double latitude = (Math.PI/2) - (2*Math.atan(Math.exp(-1.0 * y / radius)));
       
       return Math.toDegrees(latitude);
	}
	
	double getWorldDistance()
	{
		return Math.PI * 2 * radius;
	}
	
	/**
	 * Returns the number of pixels that make up one degree (at the
	 * equator).
	 * 
	 * @return
	 */
	double getScale()
	{
		return circumference / 360;
	}
	
	/**
	 * Returns the number of pixels that make up one degree at the
	 * given latitude.
	 * 
	 * This value is smallest at the equator and gets bigger towards the
	 * poles.
	 * 
	 * Calculating this value for the poles (latitude of -90° and 90°) should
	 * be avoided because that would lead to a division by zero.
	 * 
	 * @return
	 */
	double getScale(double latitude)
	{
		return getCircumferenceAt(latitude) / 360;
	}
	
	/**
	 * Calculates earth' circumference at a certain latitude.
	 * 
	 * @param latitude
	 * @return
	 */
	double getCircumferenceAt(double latitude)
	{
		return 2 * Math.PI * (Math.cos(Math.toRadians(latitude)) * circumference);
	}
	
	int lonToTileX(double longitude)
	{
		return lonToX(longitude) >>> 8;
	}
	
	int latToTileY(double latitude)
	{
		return latToY(latitude) >>> 8;
	}
	
	public final static int worldToTile(int w)
	{
		return w >>> 8;
	}
	
	public final static int tileToWorld(int t)
	{
		return t << 8;
	}

}
