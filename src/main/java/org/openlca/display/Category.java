package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.openlca.util.Pair;

public class Category {

	private int startPixel;
	private int endPixel;
	private RGB rgb;
	private double value;
	private Pair<Point, Point> endSeparation;
	private Pair<Point, Point> startSeparation;
	private Product product;
	private Point targetStartingPoint;
	private Point targetEndingPoint;
	private boolean isDrawable;
	
	public Point getTargetStartingPoint() {
		return targetStartingPoint;
	}

	public void setTargetStartingPoint(Point startingPoint) {
		this.targetStartingPoint = startingPoint;
	}

	public Point getTargetEndingPoint() {
		return targetEndingPoint;
	}

	public void setTargetEndingPoint(Point endingPoint) {
		this.targetEndingPoint = endingPoint;
	}

	public Category(double value, Product p, double min, double max) {
		isDrawable = true;
		rgb = getRGB(value, min, max);
		product = p;
		this.value = value+Math.abs(min)+1;
	}

	public double getValue() {
		return value;
	}

	private RGB getRGB(double value, double min, double max) {
		double percentage = 0;

		try {
			percentage = ((value - min) * 100) / (max - min);
		} catch (Exception e) {
			percentage = -1;
		}
		if (percentage > 100.0) { // It happens because of uncertainty of division
			percentage = 100.0;
		} else if (percentage == -1 || value == 0.0) {
			isDrawable = false;
			return new RGB(192, 192, 192); // Grey color for unfocused values (0 or null)
		}
		java.awt.Color tmpColor = ColorHelper.numberToColor((double) percentage);
		rgb = new RGB(tmpColor.getRed(), tmpColor.getGreen(), tmpColor.getBlue());
		return rgb;
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}

	public void setStartSeparation(Point sepStart, Point sepEnd) {
		startSeparation = new Pair<Point, Point>(sepStart, sepEnd);
	}

	public Pair<Point, Point> getStartSeparation() {
		return startSeparation;
	}

	public void setEndSeparation(Point sepStart, Point sepEnd) {
		endSeparation = new Pair<Point, Point>(sepStart, sepEnd);
	}

	public boolean isLinkDrawable() {
		return isDrawable;
	}

	public Pair<Point, Point> getEndSeparation() {
		return endSeparation;
	}

	public int getStartPixel() {
		return startPixel;
	}

	public void setStartPixel(int startIndex) {
		this.startPixel = startIndex;
	}

	public int getEndPixel() {
		return endPixel;
	}
	
	public void setEndPixel(int startIndex) {
		this.endPixel = startIndex;
	}

	public String toString() {
		return rgb + " / " + value+" / [ "+startPixel+"; "+endPixel+" ]";
	}

}
