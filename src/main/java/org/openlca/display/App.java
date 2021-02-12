package org.openlca.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

public class App {

	public static void main(String[] args) {
		Config config = new Config(); // Contains global parameters
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Canvas Example");
		shell.setLayout(new FillLayout());
		ArrayList<Product> products = createProducts(3, config);
		new ProductDisplay(shell, products, config).display();

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static ArrayList<Product> createProducts(int productsAmount, Config config) {
		// Create random numbers, in order to be the product results
		List<String> results = new ArrayList<>();
		Random rand = new Random();
		for (int i = 0; i < config.NB_Product_Results; i++) {
			results.add(String.valueOf(rand.nextLong()));
		}
		ArrayList<Product> products = new ArrayList<>();
		for (int i = 0; i < productsAmount; i++) {
			ArrayList<String> results2 = new ArrayList<>(results);
			Collections.shuffle(results2);
			Product p1 = new Product(results2);
			products.add(p1);
		}
		return products;
	}
}
