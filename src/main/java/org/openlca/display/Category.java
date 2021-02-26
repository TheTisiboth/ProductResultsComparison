package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.openlca.util.Pair;

public class Category {

	private int startIndex;
	private int endIndex;
	private RGB rgb;
	private Result targetResult;
	private Pair<Point, Point> endSeparation;
	private Pair<Point, Point> startSeparation;
	private Product product;

	public Category(int si, RGB r, Product p) {
		startIndex = si;
		rgb = r;
		product = p;
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}

	public Result getTargetResult() {
		return targetResult;
	}

	public void setStartingResult(Result targetResult) {
		this.targetResult = targetResult;
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

	public boolean isSeparationDrawable() {
		if (endSeparation == null || startSeparation == null) {
			return false;
		}
		return (endSeparation.first.x - startSeparation.first.x >= 3);
	}

	public Pair<Point, Point> getEndSeparation() {
		return endSeparation;
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getEndIndex() {
		return endIndex;
	}

	/**
	 * Set the index of the last result of the category. This has to be made AFTER
	 * the start index has been set
	 * 
	 * @param endIndex
	 */
	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
		int resultIndex = 0;
		// We look for the result in the middle of the category
		if (endIndex != startIndex) {
			resultIndex = (startIndex + endIndex) / 2;
		} else {
			resultIndex = startIndex;
		}
		targetResult = product.getList().get(resultIndex);
	}

	public String toString() {
		return rgb + " / " + startIndex + " - " + endIndex;
	}

}
