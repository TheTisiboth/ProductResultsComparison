package org.openlca.display;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.swt.graphics.Point;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

public class Product {

	private ArrayList<Cell> list;
	private String name;
	private boolean drawSeparationBetweenResults = false;
	static ComparisonCriteria criteria;
	static Config config;
	private double width;
	private Point startEdge;
	private long min,max;

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
			list.add(new Cell(contribution, min,max));
		}
	}



	public static void updateComparisonCriteria(ComparisonCriteria c) {
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

//	public void setResult(int index, Result r) {
//		list.set(index, r);
//	}
//
//	public Result getResult(int index) {
//		return list.get(index);
//	}

	public long getCutoffSize(double cutOff) {
		return list.stream().filter(c -> c.getAmount() < cutOff).count();
	}
	/**
	 * Return the size of the product list, without counting the 0 or null values
	 * 
	 * @return The effective size of this product
	 */
	public int getEffectiveSize() {
		try {
			switch (criteria) {
			case AMOUNT:
				return (int) list.stream().filter(r -> r.getResult().getContribution().amount != 0.0).count();
			case CATEGORY:
				return (int) list.stream().filter(r -> r.getResult().getContribution().item.category != null).count();
			case LOCATION:
				return (int) list.stream().filter(r -> ((ProcessDescriptor) r.getResult().getContribution().item).location != null)
						.count();
			}
		} catch (NullPointerException | NoSuchElementException e) {
			return 0;
		}
		return 0;
	}

	public double min() {
		return min;
	}

	public double max() {
		return max;
	}
	

	@Override
	public String toString() {
		String s = "[ ";
		for (Cell r : list) {
			s += r.getResult().getContribution().item.toString() + ", ";
		}
		s += " ]";
		return s;
	}

	/**
	 * Ascending sort of the products results
	 */
	public void sort() {
		switch (criteria) {
		case AMOUNT:
			list.sort((r1, r2) -> {
				double a1 = r1.getResult().getContribution().amount;
				double a2 = r2.getResult().getContribution().amount;
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
			break;
		case CATEGORY:
			list.sort((r1, r2) -> {
				Long c1 = ((ProcessDescriptor) r1.getResult().getContribution().item).category;
				Long c2 = ((ProcessDescriptor) r2.getResult().getContribution().item).category;
				if (c1 == null && c2 == null) {
					return 0;
				} else if (c1 == null && c2 != null) {
					return -1;
				} else if (c1 != null && c2 == null) {
					return 1;
				}
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
					Long l1 = ((ProcessDescriptor) r1.getResult().getContribution().item).location;
					Long l2 = ((ProcessDescriptor) r2.getResult().getContribution().item).location;
					if (l1 == null && l2 == null) {
						return 0;
					} else if (l1 == null && l2 != null) {
						return -1;
					} else if (l1 != null && l2 == null) {
						return 1;
					}
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
			throw new IllegalArgumentException("Unexpected value: " + criteria);
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
