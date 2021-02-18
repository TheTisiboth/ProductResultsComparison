package org.openlca.display;

public abstract class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		double min = -200.0;
		double max = -100;
		double input = -150;
		double percentage = ((input - min) * 100) / (max - min);
		System.out.println(percentage);
	}

}
