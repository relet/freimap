package net.relet.freimap;

public final class Converter {

	OSMMercatorProjection projection;

	public int offsetX, offsetY;

	public void setProjection(OSMMercatorProjection p) {
		projection = p;
	}

	public void setWorld(int ofsX, int ofsY) {
		offsetX = ofsX;
		offsetY = ofsY;
	}

	public void setWorldRel(int relX, int relY) {
		offsetX += relX;
		offsetY += relY;
	}

	public int worldToViewX(int x) {
		return x - offsetX;
	}

	public int worldToViewY(int y) {
		return y - offsetY;
	}

	public int viewToWorldX(int x) {
		return x + offsetX;
	}

	public int viewToWorldY(int y) {
		return y + offsetY;
	}

	public int lonToWorld(double lon) {
		return (int) projection.lonToX(lon);
	}

	public int latToWorld(double lat) {
		return (int) projection.latToY(lat);
	}

	public int lonToViewX(double lon) {
		return (int) projection.lonToX(lon) - offsetX;
	}

	public int latToViewY(double lat) {
		return (int) projection.latToY(lat) - offsetY;
	}

	public double viewXToLon(int x) {
		return projection.xToLong(x + offsetX);
	}

	public double viewYToLat(int y) {
		return projection.yToLat(y + offsetY);
	}

	public double getScale() {
		return projection.getScale();
	}

	void initZoom(int zoom, int viewX, int viewY) {
		if (projection == null)
			projection = new OSMMercatorProjection(0);

		// We want to zoom in on the center of our current screen.
		// Therefore we calculate the centers' lon|lat, set up
		// the new projection and then calculate the new
		// offsets.

		double lon = projection.xToLong(offsetX + viewX);
		double lat = projection.yToLat(offsetY + viewY);

		projection = new OSMMercatorProjection(zoom);

		// Geo coordinates -> new view offset
		offsetX = (int) lonToWorld(lon) - viewX;
		offsetY = (int) latToWorld(lat) - viewY;
	}

}
