package org.openlca.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Hello world!
 *
 */
public class App {
	final private static int NB_Product_Results = 100;
	public static void main(String[] args) {
		// Create random numbers, in order to be the product results
		ArrayList<String> results = new ArrayList<>();
		Random rand = new Random();
		for (int i = 0; i < NB_Product_Results; i++) {
			results.add(String.valueOf(rand.nextLong()));
		}
		
		// Creation of product 1, with the random results
		Product p1 = new Product(results);
		ArrayList<String> results2 = new ArrayList<>(results);
		Collections.shuffle(results2);
		// Creation of product 2, with the previous results, but shuffled
		Product p2 = new Product(results2);

		ArrayList<Product> list = new ArrayList<>();
		list.add(p1);
		list.add(p2);

		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Canvas Example");
		shell.setLayout(new FillLayout());
		
		new ProductDisplay(shell, list).display();

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	
}
