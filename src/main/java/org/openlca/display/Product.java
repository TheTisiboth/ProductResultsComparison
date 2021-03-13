package org.openlca.display;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.swt.graphics.Point;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

public class Product {

	private ArrayList<Cell> list;
	private String name;
	private boolean drawSeparationBetweenResults = false;
	static AggregationCriteria criteria;
	static Config config;
	private double width;
	private Point startEdge;
	private long min, max;

	public Product(ComparisonCriteria c) {
		list = new ArrayList<>();
	}

	public Product(List<Contribution<CategorizedDescriptor>> l, String n) {
		name = n;
		list = new ArrayList<>();
		Result.criteria = criteria;
		max = l.stream().mapToLong(c -> c.item.id).max().getAsLong();
		min = l.stream().mapToLong(c -> c.item.id).min().getAsLong();
		for (Contribution<CategorizedDescriptor> contribution : l) {
			List<Contribution<CategorizedDescriptor>> contributions = new ArrayList<>();
			contributions.add(contribution);
			list.add(new Cell(contributions, min, max));
		}
	}

	public static void updateComparisonCriteria(AggregationCriteria c) {
		criteria = c;
		Result.criteria = c;
	}

	public void setDrawSeparationBetweenResults(boolean draw) {
		drawSeparationBetweenResults = draw;
	}

	public boolean getDrawSeparationBetweenResults() {
		return drawSeparationBetweenResults;
	}

	public String getName() {
		return name;
	}

	public ArrayList<Cell> getList() {
		return list;
	}

	public void setList(ArrayList<Cell> list) {
		this.list = list;
	}

	public long getCutoffSize(double cutOff) {
		return list.stream().filter(c -> c.getAmount() < cutOff).count();
	}

	public double min() {
		return min;
	}

	public double normalizedMaxAmount() {
		return list.get(list.size()-1).getNormalizedAmount();
	}
	
	public double max() {
		return max;
	}

	@Override
	public String toString() {
		String s = "[ ";
		for (Cell c : list) {
			var l = c.getResult().stream().map(r -> r.getContribution().item.toString()).collect(Collectors.toList());
			s += String.join(", ", l);
		}
		s += " ]";
		return s;
	}

	/**
	 * Ascending sort of the products results
	 */
	public void sort() {
		switch (criteria) {
		case CATEGORY:
			list.sort((r1, r2) -> {
				Double c1 = r1.getResult().stream().mapToDouble(r -> r.getContribution().item.category).sum();
				Double c2 = r2.getResult().stream().mapToDouble(r -> r.getContribution().item.category).sum();
				long result = c1.longValue() - c2.longValue();
				if (result < 0) {
					return -1;
				}
				if (result > 0) {
					return 1;
				}
				return 0;
			});
			break;
		case LOCATION:
			list.sort((r1, r2) -> {
				try {
					Long l1 = r1.getResult().stream()
							.mapToLong(r -> ((ProcessDescriptor) r.getContribution().item).location).sum();
					Long l2 = r2.getResult().stream()
							.mapToLong(r -> ((ProcessDescriptor) r.getContribution().item).location).sum();
					long result = l1.longValue() - l2.longValue();
					if (result < 0) {
						return -1;
					}
					if (result > 0) {
						return 1;
					}
					return 0;
				} catch (ClassCastException e) {
					// If the item is not a ProcessDescriptor, there is no location field. Hence, we
					// can not sort on this item
					return 0;
				}
			});
			break;
		default:
			list.sort((r1, r2) -> {
				double a1 = r1.getResult().stream().mapToDouble(r -> r.getContribution().amount).sum();
				double a2 = r2.getResult().stream().mapToDouble(r -> r.getContribution().amount).sum();
				if (a1 == 0.0 && a2 != 0.0) {
					return -1;
				} else if (a1 != 0.0 && a2 == 0.0) {
					return 1;
				}
				if (a2 > a1) {
					return -1;
				}
				if (a1 > a2) {
					return 1;
				}
				return 0;
			});
		}
	}

	public void setBounds(Point startEdge, int productWidth) {
		width = productWidth;
		this.startEdge = startEdge;
	}

	public double getWidth() {
		return width;
	}

	public Point getStartEdge() {
		return startEdge;
	}

}
