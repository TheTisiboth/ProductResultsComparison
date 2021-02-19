package org.openlca.display;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;

public class Product {

	private ArrayList<Result> list;
	private String name;
	private boolean drawSeparation = false;

	public Product() {
		list = new ArrayList<>();
	}

	public void setDrawSeparation(boolean draw) {
		drawSeparation = draw;
	}

	public boolean getDrawSeparation() {
		return drawSeparation;
	}

	public Product(List<Contribution<CategorizedDescriptor>> l, String n) {
		name = n;
		list = new ArrayList<>();
		for (Contribution<CategorizedDescriptor> c : l) {
			list.add(new Result(c));
		}
	}

	public double min(ComparisonCriteria criteria) {
		try {
			switch (criteria) {
			case AMOUNT:
				return list.stream().mapToDouble(r -> r.getContribution().amount).min().getAsDouble();
			case CATEGORY:
				return list.stream().mapToDouble(r -> r.getContribution().item.category).min().getAsDouble();
			case LOCATION:
				return list.stream().filter(r -> ((ProcessDescriptor) r.getContribution().item).location != null)
						.mapToDouble(r -> ((ProcessDescriptor) r.getContribution().item).location).min().getAsDouble();
			}
		} catch (NullPointerException | NoSuchElementException e) {
			return 0;
		}
		return 0;
	}

	public double max(ComparisonCriteria criteria) {
		try {
			switch (criteria) {
			case AMOUNT:
				return list.stream().mapToDouble(r -> r.getContribution().amount).max().getAsDouble();
			case CATEGORY:
				return list.stream().mapToDouble(r -> r.getContribution().item.category).max().getAsDouble();
			case LOCATION:
				return list.stream().filter(r -> ((ProcessDescriptor) r.getContribution().item).location != null)
						.mapToDouble(r -> ((ProcessDescriptor) r.getContribution().item).location).max().getAsDouble();
			}
		} catch (NullPointerException | NoSuchElementException e) {
			return 0;
		}
		return 0;
	}

	public String getName() {
		return name;
	}

	public ArrayList<Result> getList() {
		return list;
	}

	public void setList(ArrayList<Result> list) {
		this.list = list;
	}

	public void setResult(int index, Result r) {
		list.set(index, r);
	}

	public Result getResult(int index) {
		return list.get(index);
	}

	@Override
	public String toString() {
		String s = "[ ";
		for (Result r : list) {
			s += r.getContribution().item.toString() + ", ";
		}
		s += " ]";
		return s;
	}

	public void sort(ComparisonCriteria criteria) {
		switch (criteria) {
		case AMOUNT:
			list.sort((r1, r2) -> {
				double a1 = r1.getContribution().amount;
				double a2 = r2.getContribution().amount;
				if (a1 == 0.0 && a2 != 0.0) {
					return 1;
				} else if (a1 != 0.0 && a2 == 0.0) {
					return -1;
				}
				if (a2 > a1) {
					return 1;
				}
				if (a1 > a2) {
					return -1;
				}
				return 0;
			});
			break;
		case CATEGORY:
			list.sort((r1, r2) -> {
				Long c1 = ((ProcessDescriptor) r1.getContribution().item).category;
				Long c2 = ((ProcessDescriptor) r2.getContribution().item).category;
				if (c1 == null && c2 == null) {
					return 0;
				} else if (c1 == null && c2 != null) {
					return 1;
				} else if (c1 != null && c2 == null) {
					return -1;
				}
				long result = c1.longValue() - c2.longValue();
				if (result < 0) {
					return 1;
				}
				if (result > 0) {
					return -1;
				}
				return 0;
			});
			break;
		case LOCATION:
			list.sort((r1, r2) -> {
				try {
					Long l1 = ((ProcessDescriptor) r1.getContribution().item).location;
					Long l2 = ((ProcessDescriptor) r2.getContribution().item).location;
					if (l1 == null && l2 == null) {
						return 0;
					} else if (l1 == null && l2 != null) {
						return 1;
					} else if (l1 != null && l2 == null) {
						return -1;
					}
					long result = l1.longValue() - l2.longValue();
					if (result < 0) {
						return 1;
					}
					if (result > 0) {
						return -1;
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

}
