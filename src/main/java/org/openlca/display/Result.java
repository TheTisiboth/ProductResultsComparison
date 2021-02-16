package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.openlca.core.results.Contribution;

public class Result<T> {

	private Contribution<T> contribution;
	private Point startPoint; // Point from which a links start
	private Point endPoint; // Point to which the links ends
	private Result<T> targetProductResult;

	public Result(Contribution<T> item) {
		contribution = item;
		startPoint = null;
		endPoint = null;
		targetProductResult = null;
	}

	public Contribution<T> getContribution() {
		return contribution;
	}

	public void setContribution(Contribution<T> contribution) {
		this.contribution = contribution;
	}

	public Result<T> getTargetProductResult() {
		return targetProductResult;
	}

	public void setTargetProductResult(Result<T> targetProductResult) {
		this.targetProductResult = targetProductResult;
	}

	public void setStartPoint(int x, int y) {
		startPoint = new Point(x, y);
	}

	public Point getStartPoint() {
		return startPoint;
	}

	public void setEndPoint(int x, int y) {
		endPoint = new Point(x, y);
	}

	public Point getEndPoint() {
		return endPoint;
	}

	@Override
	public String toString() {
		return contribution.item.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Result)) {
			return false;
		}
		Result<T> r = (Result<T>) o;
		return contribution.item.equals(r.contribution.item);
	}
}
