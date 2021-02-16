package org.openlca.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.matrix.ImpactIndex;
import org.openlca.core.matrix.MatrixData;
import org.openlca.core.matrix.TechIndex;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
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
		Shell shell = new Shell(display);
		shell.setText("Canvas Example");
		shell.setLayout(new FillLayout());
		String dbNames[] = {"ecoinvent_371_apos_unit_20201221","agribalyse_v3_0_1"};
		var products = getContributionResults(dbNames);
//			List<Product<String>> products = createProducts(10, config);
		new ProductDisplay<CategorizedDescriptor>(shell, products, config).display();

		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	

	}

	private static List<Product<CategorizedDescriptor>> getContributionResults(String dbNames[]) {
		var list = new ArrayList<Product<CategorizedDescriptor>>();
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
				var cs = result.getProcessContributions(impact);
				var p = new Product<CategorizedDescriptor>(cs, dbName);
				list.add(p);
			}
		}
		return list;
	}

	private static List<Product<String>> createProducts(int productsAmount, Config config) {
		// Create random numbers, in order to be the product results
		List<String> results = new ArrayList<>();
		Random rand = new Random();
		for (int i = 0; i < config.NB_Product_Results; i++) {
			results.add(String.valueOf(rand.nextLong()));
		}
		List<Product<String>> products = new ArrayList<>();
		for (int i = 0; i < productsAmount; i++) {
			List<String> results2 = new ArrayList<>(results);
			Collections.shuffle(results2);
			List<Contribution<String>> l = new ArrayList<>();
			for (String string : results2) {
				Contribution<String> c = new Contribution<>();
				c.item = string;
				l.add(c);
			}
			var p1 = new Product<String>(l, "Product "+i);
			products.add(p1);
		}
		return products;
	}
}
