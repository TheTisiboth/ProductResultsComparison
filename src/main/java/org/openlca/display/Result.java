package org.openlca.display;

import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

public class Result {

	private Contribution<CategorizedDescriptor> contribution;
	private Point startPoint; // Point from which a links start
	private Point endPoint; // Point to which the links ends
	private RGB rgb;

	public Result(Contribution<CategorizedDescriptor> item) {
		contribution = item;
		startPoint = null;
		endPoint = null;
	}

	public RGB createColor(ComparisonCriteria criteria, double min, double max) {
		double percentage = 0;
		switch (criteria) {
		case AMOUNT:
			percentage = ((contribution.amount - min) * 100) / (max - min);
			break;
		case CATEGORY:
			percentage = ((contribution.item.category - min) * 100) / (max - min);
			break;
		case LOCATION:
			percentage = (((ProcessDescriptor) contribution.item).location * 100) / (max - min);
			break;
		}
		if (percentage > 100.0) { // It happens because of uncertainty of division
			percentage = 100.0;
		}
		java.awt.Color tmpColor = ColorHelper.numberToColor((double) percentage);
		rgb = new RGB(tmpColor.getRed(), tmpColor.getGreen(), tmpColor.getBlue());
		return rgb;
	}

	public Contribution<CategorizedDescriptor> getContribution() {
		return contribution;
	}

	public void setContribution(Contribution<CategorizedDescriptor> contribution) {
		this.contribution = contribution;
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
	
	public boolean isContributionEmpty( ComparisonCriteria comparisonCriteria) {
		boolean contributionEmpty = false;
		switch (comparisonCriteria) {
		case AMOUNT:
			contributionEmpty = contribution.amount == 0.0;
			break;
		case CATEGORY:
			contributionEmpty = contribution.item.category == null;
			break;
		case LOCATION:
			contributionEmpty = ((ProcessDescriptor) contribution.item).location == null;
			break;
		}
		return contributionEmpty;
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
		} catch (NullPointerException e) {
			return false;
		}
	}
}
