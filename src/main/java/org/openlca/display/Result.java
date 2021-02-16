package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

public class Result {

	private Contribution<CategorizedDescriptor> contribution;
	private Point startPoint; // Point from which a links start
	private Point endPoint; // Point to which the links ends
	private Result targetProductResult;

	public Result(Contribution<CategorizedDescriptor> item) {
		contribution = item;
		startPoint = null;
		endPoint = null;
		targetProductResult = null;
	}

	public Contribution<CategorizedDescriptor> getContribution() {
		return contribution;
	}

	public void setContribution(Contribution<CategorizedDescriptor> contribution) {
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
	}

	public boolean equals(Object o, ComparisonCriteria criteria) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof Result)) {
			return false;
		}
		Result r = (Result) o;
		boolean comparison;
		try {
			switch (criteria) {
			case AMOUNT:
				comparison = contribution.amount == r.contribution.amount;
				break;
			case CATEGORY:
				comparison = ((CategorizedDescriptor) r.contribution.item).category
						.equals(((CategorizedDescriptor) contribution.item).category);
				break;
			case LOCATION:
				comparison = ((ProcessDescriptor) r.contribution.item).location
						.equals(((ProcessDescriptor) contribution.item).location);
				break;
			default:
				throw new IllegalArgumentException("Unexpected value: " + criteria);
			}
			return comparison;
		} catch (Exception e) {
			return contribution.item.equals(r.contribution.item);
		}
	}
}
