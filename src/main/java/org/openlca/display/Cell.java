package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;

public class Cell {

	private int startPixel;
	private int endPixel;
	private RGB rgb;
	private Point startingLinksPoint;
	private Point endingLinkPoint;
	private boolean isDrawable;
	private Config config;
	private double min;
	private double max;
	private Result result;

	public void setData(Point startingLinksPoint, Point endingLinkPoint, int startX, int endx) {
		this.startingLinksPoint = startingLinksPoint;
		this.endingLinkPoint = endingLinkPoint;
		startPixel = startX;
		endPixel = endx;
	}

	public Point getStartingLinkPoint() {
		return startingLinksPoint;
	}

	public void setStartingLinkPoint(Point startingLinksPoint) {
		this.startingLinksPoint = startingLinksPoint;
	}

	public Point getEndingLinkPoint() {
		return endingLinkPoint;
	}

	public void setEndingLinkPoint(Point endingLinkPoint) {
		this.endingLinkPoint = endingLinkPoint;
	}

	public Cell(Result r, Config c, double min, double max) {
		this.min = min;
		this.max = max;
		this.result = r;
		isDrawable = true;
		config = c;
		rgb = computeRGB();
	}
	
	public Result getResult() {
		return result;
	}

	public double getTargetValue() {
		return result.getValue();
	}

	public double getNormalizedValue() {
		return result.getValue() + Math.abs(min) + 1;
	}

	public double getAmount() {
		return result.getAmount();
	}

	public double getNormalizedAmount() {
		return result.getAmount() + Math.abs(min) + 1;
	}

	private RGB computeRGB() {
		double percentage = 0;
		var value = result.getValue();
		try {
			percentage = (((value - min) * 100) / (max - min)) / 100;
		} catch (Exception e) {
			percentage = -1;
		}
		if (percentage > 100.0) { // It happens because of uncertainty of division
			percentage = 100.0;
		} else if (percentage == -1 || value == 0.0) {
			isDrawable = false;
			return new RGB(192, 192, 192); // Grey color for unfocused values (0 or null)
		}
		RGB rgb = null;
		if (config.useGradientColor) {
			java.awt.Color tmpColor = GradientColorHelper.numberToColorPercentage((double) percentage);
			rgb = new RGB(tmpColor.getRed(), tmpColor.getGreen(), tmpColor.getBlue());
		} else {
			rgb = ColorPaletteHelper.getColor(percentage);
		}
		return rgb;
	}

	public void resetDefaultRGB() {
		rgb = computeRGB();
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}

	public boolean isLinkDrawable() {
		return isDrawable;
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
		return rgb + " / " + result.getValue() + " / [ " + startPixel + "; " + endPixel + " ]";
	}

}
