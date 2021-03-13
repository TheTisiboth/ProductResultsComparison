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
	static AggregationCriteria criteria;

	public Result(Contribution<CategorizedDescriptor> item) {
		contribution = item;
		startPoint = null;
		endPoint = null;
	}

	public double getAmount() {
		return contribution.amount;
	}

	public double getValue() {
		try {
			switch (criteria) {
			case CATEGORY:
				return contribution.item.category;
			case LOCATION:
				return ((ProcessDescriptor) contribution.item).location;
			default:
				return contribution.amount;
			}
		} catch (ClassCastException | NullPointerException e) {
			return 0;
		}
	}

	public RGB getRGB(double min, double max) {
		double percentage = 0;
		switch (criteria) {
		case CATEGORY:
			if (contribution.item.category != null) {
				percentage = ((contribution.item.category - min) * 100) / (max - min);
			} else {
				percentage = -1;
			}
			break;
		case LOCATION:
			Long location = ((ProcessDescriptor) contribution.item).location;
			if (location != null) {
				percentage = (location.longValue() * 100) / (max - min);
			} else {
				percentage = -1;
			}
			break;
		default:
			if (contribution.amount != 0.0) {
				percentage = ((contribution.amount - min) * 100) / (max - min);
			} else {
				percentage = -1;
			}
		}
		if (percentage > 100.0) { // It happens because of uncertainty of division
			percentage = 100.0;
		} else if (percentage == -1) {
			return new RGB(192, 192, 192); // Grey color for unfocused values (0 or null)
		}
		java.awt.Color tmpColor = GradientColorHelper.numberToColor((double) percentage);
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

	public boolean isContributionEmpty() {
		boolean contributionEmpty = false;
		switch (criteria) {
		case CATEGORY:
			contributionEmpty = contribution.item.category == null;
			break;
		case LOCATION:
			contributionEmpty = ((ProcessDescriptor) contribution.item).location == null;
			break;
		default:
			contributionEmpty = contribution.amount == 0.0;
		}
		return contributionEmpty;
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
		Result r = (Result) o;
		boolean comparison;
		try {
			switch (criteria) {
			case CATEGORY:
				comparison = ((CategorizedDescriptor) r.contribution.item).category
						.equals(((CategorizedDescriptor) contribution.item).category);
				break;
			case LOCATION:
				comparison = ((ProcessDescriptor) r.contribution.item).location
						.equals(((ProcessDescriptor) contribution.item).location);
				break;
			default:
				comparison = contribution.amount == r.contribution.amount;
			}
			return comparison;
		} catch (NullPointerException e) {
			return false;
		}
	}
}
