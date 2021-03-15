package org.openlca.display;

import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.results.Contribution;

public class Cell {

	private int startPixel;
	private int endPixel;
	private RGB rgb;
	private Point startingLinksPoint;
	private Point endingLinkPoint;
	private boolean isDrawable;
	static Config config;
	private long minProcessId;
	private long maxProcessId;
	private List<Result> result;
	private double minAmount, maxAmount;

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

	public Cell(List<Contribution<CategorizedDescriptor>> contributions, long min, long max, double minAmount,
			double maxAmount) {
		this.minProcessId = min;
		this.maxProcessId = max;
		this.minAmount = minAmount;
		this.maxAmount = maxAmount;
		result = contributions.stream().map(c -> new Result(c)).collect(Collectors.toList());
		isDrawable = true;
		rgb = computeRGB();
	}

	public List<Result> getResult() {
		return result;
	}

	public double getTargetValue() {
		return result.stream().mapToDouble(r -> r.getValue()).sum();
	}

	public double getNormalizedValue() {
		return result.stream().mapToDouble(r -> r.getValue() + Math.abs(minAmount) + 1).sum();
	}

	public double getAmount() {
		return result.stream().mapToDouble(r -> r.getAmount()).sum();
	}

	public double getNormalizedAmount() {
		return result.stream().mapToDouble(r -> r.getAmount() + Math.abs(minAmount) + 1).sum();
	}

	private RGB computeRGB() {
		double percentage = 0;
		var value = result.get(0).getContribution().item.id;
		try {
			percentage = (((value - minProcessId) * 100) / (maxProcessId - minProcessId)) / 100.0;
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
			java.awt.Color tmpColor = GradientColorHelper.numberToColorPercentage(percentage);
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
		var results = result.stream().map(r -> Double.toString(r.getValue())).collect(Collectors.toList());
		return rgb + " / " + String.join(", ", results) + " / [ " + startPixel + "; " + endPixel + " ]";
	}

}
