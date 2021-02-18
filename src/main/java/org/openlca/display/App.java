package org.openlca.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.matrix.ImpactIndex;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.julia.Julia;

public class App {
	private static void println(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws Exception {

		Julia.load();
//		Input input = Arguments.parse(Input.class, args);

		Config config = new Config(); // Contains global parameters
		Display display = new Display();
		Shell shell = new Shell(display, SWT.CLOSE | SWT.TITLE);
		shell.setText("Canvas Example");
		shell.setLayout(new GridLayout());
		List<Product> products;
		if (!config.useFakeResults) {
			String dbNames[] = { "ecoinvent_371_apos_unit_20201221", "agribalyse_v3_0_1" };
			int impactIndexes[] = { 0, 20, 40, 100, 200, 300 };
//			products = getContributionResults(dbNames, impactIndexes);
			products = getHighestContributionResults(dbNames);
		} else {
			products = createProducts(10, config);
		}
		new ProductDisplay(shell, products, config).display();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();

	}

	private static List<Product> getHighestContributionResults(String dbNames[]) {
		var list = new ArrayList<Product>();
		println("Connect to databases ");
		for (String dbName : dbNames) {
			try (var db = DerbyDatabase.fromDataDir(dbName)) {
				var techIndex = TechIndex.unlinkedOf(db);
				var data = MatrixData.of(db, techIndex).withImpacts(ImpactIndex.of(db)).build();
				var result = ContributionResult.of(db, data);
				// select the impact category with the highest result
				ImpactDescriptor impact = null;
				for (int i = 0; i < result.impactIndex.size(); i++) {
					var next = result.impactIndex.at(i);
					if (impact == null) {
						impact = next;
						continue;
					}
					var currentVal = result.getTotalImpactResult(impact);
					var nextVal = result.getTotalImpactResult(next);
					if (nextVal > currentVal) {
						impact = next;
					}
				}
				List<Contribution<CategorizedDescriptor>> cs = result.getProcessContributions(impact);
				var p = new Product(cs, dbName);
				list.add(p);
			}
		}
		return list;
	}

	private static List<Product> getContributionResults(String dbNames[], int impactIndexes[]) {
		var list = new ArrayList<Product>();
		println("Connect to databases ");
		for (String dbName : dbNames) {
			try (var db = DerbyDatabase.fromDataDir(dbName)) {
				var techIndex = TechIndex.unlinkedOf(db);
				var data = MatrixData.of(db, techIndex).withImpacts(ImpactIndex.of(db)).build();
				var result = ContributionResult.of(db, data);
				for (int index : impactIndexes) {
					if (index > 0 && index < result.impactIndex.size()) {
						List<Contribution<CategorizedDescriptor>> cs = result
								.getProcessContributions(result.impactIndex.at(index));
						var p = new Product(cs, dbName);
						list.add(p);
					}
				}
			}
		}
		return list;
	}

	private static List<Product> createProducts(int productsAmount, Config config) {
		// Create random numbers, in order to be the product results
		List<String> results = new ArrayList<>();
		Random rand = new Random();
		for (int i = 0; i < config.NB_Product_Results; i++) {
			results.add(String.valueOf(rand.nextLong()));
		}
		List<Product> products = new ArrayList<>();
		for (int i = 0; i < productsAmount; i++) {
			List<String> results2 = new ArrayList<>(results);
			Collections.shuffle(results2);
			List<Contribution<CategorizedDescriptor>> l = new ArrayList<>();
			for (String string : results2) {
				Contribution<CategorizedDescriptor> c = new Contribution<>();
				var p = new ProcessDescriptor();
				p.name = string;
				c.item = p;
				c.amount = Double.valueOf(string);
				l.add(c);
			}
			var p1 = new Product(l, "Product " + i);
			products.add(p1);
		}
		return products;
	}
}