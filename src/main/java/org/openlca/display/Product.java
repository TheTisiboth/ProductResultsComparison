package org.openlca.display;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.results.Contribution;

public class Product {

	private ArrayList<Result> list;
	private String name;

	public Product() {
		list = new ArrayList<>();
	}

	public Product(List<Contribution<CategorizedDescriptor>> l, String n) {
		name = n;
		list = new ArrayList<>();
		for (Contribution<CategorizedDescriptor> c : l) {
			list.add(new Result(c));
		}
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
}
