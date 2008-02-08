package net.relet.freimap.background;

import java.awt.Graphics2D;
import java.awt.Image;

interface TilePainter {
	
	void paint(Graphics2D g, Image image, int worldX, int worldY);

}
