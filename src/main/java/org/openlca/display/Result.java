package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.openlca.core.results.Contribution;

public class Result {

	private Contribution<String> contribution;
	private Point startPoint; // Point from which a links start
	private Point endPoint; // Point to which the links ends
	private Result targetProductResult;

	public Result(String item) {
		contribution = new Contribution<>();
		contribution.item = item;
		startPoint = null;
		endPoint = null;
		targetProductResult = null;
	}

	public Contribution<String> getContribution() {
		return contribution;
	}

	public void setContribution(Contribution<String> contribution) {
		this.contribution = contribution;
	}

	public Result getTargetProductResult() {
		return targetProductResult;
	}

	public void setTargetProductResult(Result targetProductResult) {
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
//		String s = "{ " + contribution.item.toString() + '\n';
//		if (startPoint != null) {
//			s += startPoint.toString() + '\n';
//		}
//		if (targetProductResult != null) {
//			s += targetProductResult.getContribution().item.toString() + " }";
//		}
//		return s;
	}

	@Override
	public boolean equals(Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Result)) {
			return false;
		}
		Result r = (Result) o;
		return contribution.item.equals(r.contribution.item);
	}
}
