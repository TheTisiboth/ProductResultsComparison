package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.openlca.util.Pair;

public class Category {

	private int startIndex;
	private int endIndex;
	private RGB rgb;
	private Result startingResult;
	private Result endingResult;
	private Pair<Point, Point> endSeparation;
	private Pair<Point, Point> startSeparation;

	public Category(int si, RGB r) {
		startIndex = si;
		rgb = r;
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
		if(endSeparation == null || startSeparation == null) {
			System.out.println(1);
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

	public void setEndIndex(int endIndex) {
		this.endIndex = endIndex;
	}

	public RGB getRgb() {
		return rgb;
	}

	public void setRgb(RGB rgb) {
		this.rgb = rgb;
	}

	public Result getStartingResult() {
		return startingResult;
	}

	public void setStartingResult(Result startingResult) {
		this.startingResult = startingResult;
	}

	public Result getEndingResult() {
		return endingResult;
	}

	public void setEndingResult(Result endingPoint) {
		this.endingResult = endingPoint;
	}

}
