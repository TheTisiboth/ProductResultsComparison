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

	public String toString() {
		String s = "{ " + contribution.item.toString() + '\n';
		if (startPoint != null) {
			s += startPoint.toString() + '\n';
		}
		if (targetProductResult != null) {
			s += targetProductResult.getContribution().item.toString() + " }";
		}
		return s;
	}

	public boolean equals(Object o) {

		// If the object is compared with itself then return true
		if (o == this) {
			return true;
		}

		/*
		 * Check if o is an instance of Complex or not "null instanceof [type]" also
		 * returns false
		 */
		if (!(o instanceof Result)) {
			return false;
		}

		// typecast o to Complex so that we can compare data members
		Result r = (Result) o;

		// Compare the data members and return accordingly
		return contribution.item.equals(r.contribution.item);
	}
}
