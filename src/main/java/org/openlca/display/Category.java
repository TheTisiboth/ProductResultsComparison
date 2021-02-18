package org.openlca.display;

import org.eclipse.swt.graphics.RGB;

public class Category {

	private int startIndex;
	private int endIndex;
	private RGB rgb;
	private Result startingPoint;
	private Result endingPoint;
	
	public Category(int si, RGB r) {
		startIndex = si;
		rgb = r;
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

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}

	public Result getStartingPoint() {
		return startingPoint;
	}

	public void setStartingPoint(Result startingPoint) {
		this.startingPoint = startingPoint;
	}

	public Result getEndingPoint() {
		return endingPoint;
	}

	public void setEndingPoint(Result endingPoint) {
		this.endingPoint = endingPoint;
	}
	
}
