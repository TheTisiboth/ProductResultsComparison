package org.openlca.display;

import java.util.ArrayList;
import java.util.List;

import org.openlca.core.results.Contribution;

public class Product<T> {

	private ArrayList<Result<T>> list;
	private String name;

	public Product() {
		list = new ArrayList<>();
	}

	public Product(List<Contribution<T>> l, String n) {
		name = n;
		list = new ArrayList<>();
		for (Contribution<T> c : l) {
			list.add(new Result<T>(c));
		}
	}

	public String getName() {
		return name;
	}

	public ArrayList<Result<T>> getList() {
		return list;
	}

	public void setList(ArrayList<Result<T>> list) {
		this.list = list;
	}

	public void setResult(int index, Result<T> r) {
		list.set(index, r);
	}

	public Result<T> getResult(int index) {
		return list.get(index);
	}

	@Override
	public String toString() {
		String s = "[ ";
		for (Result<T> r : list) {
			s += r.getContribution().item.toString() + ", ";
		}
		s += " ]";
		return s;
	}
}
