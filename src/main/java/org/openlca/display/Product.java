package org.openlca.display;

import java.util.ArrayList;

public class Product {

	private ArrayList<Result> list;

	public Product() {
		list = new ArrayList<>();
	}

	public Product(ArrayList<String> l) {
		list = new ArrayList<>();
		for (String s : l) {
			list.add( new Result(s));
		}
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

	public String toString() {
		String s = "[ ";
		for (Result r : list) {
			s += r.getContribution().item.toString() + ", ";
		}
		s += " ]";
		return s;
	}
}
