package org.openlca.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.FlowType;
import org.openlca.core.model.Process;
import org.openlca.core.model.ProductSystem;

public class App {
	private static void println(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws Exception {
		
//		boolean loaded = Julia.load();
//		Input input = Arguments.parse(Input.class, args);
//		println("Connect to databases ");
//		IDatabase db = new DerbyDatabase(Data.getDbDir(input.dbFile));
//		var calc = new SystemCalculator(db, new JuliaSolver());
//		var system = loadSystem(db);
//		List<ImpactDescriptor> impactCategories = new ImpactCategoryDao(db).getDescriptors();
//		println(impactCategories.size() + " impact categories");
//		var results = calc.calculateContributions(new CalculationSetup(system));

		Config config = new Config(); // Contains global parameters
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setText("Canvas Example");
		shell.setLayout(new FillLayout());
		ArrayList<Product> products = createProducts(10, config);
		new ProductDisplay(shell, products, config).display();

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

	private static ProductSystem loadSystem(IDatabase db) {
		System.out.println("Load system");
		ProductSystem system = null;
		for (var d : db.allDescriptorsOf(Process.class)) {
			var process = db.get(Process.class, d.id);
			if (!hasValidQRef(process)) {
				continue;
			}
			system = ProductSystem.of(process);
			system.withoutNetwork = true;
			break;
		}
		if (system == null) {
			System.out.println("the database has no valid processes");
//			error.accept("the database has no valid processes");
		}
		return system;
	}

	private static boolean hasValidQRef(Process p) {
		if (p == null || p.quantitativeReference == null)
			return false;
		var qref = p.quantitativeReference;
		if (qref.amount == 0 || qref.flow == null || qref.isAvoided)
			return false;
		var type = qref.flow.flowType;
		return (type == FlowType.WASTE_FLOW && qref.isInput) || (type == FlowType.PRODUCT_FLOW && !qref.isInput);
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
