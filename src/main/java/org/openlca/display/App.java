package org.openlca.display;

import java.util.ArrayList;
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
//			String dbNames[] = { "ecoinvent_371_cutoff_unit_20210104", "exiobase3_monetary_20181212", "needs_18",
//					"ideaolcaelemnames_final", "evah_pigment_database_20190314", "usda_1901009" };

			String dbNames[] = { "ecoinvent_371_cutoff_unit_20210104" };
			int impactIndexes[] = { 0, 20, 40, 100, 200, 300 };
			products = getContributionResults(dbNames, impactIndexes, config);
//			products = getHighestContributionResults(dbNames, config);
		} else {
			products = createProducts(5, config);
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

	private static List<Product> getHighestContributionResults(String dbNames[], Config config) {
		var list = new ArrayList<Product>();
		println("Connect to databases ");
		Product.criteria = config.comparisonCriteria;
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

	private static List<Product> getContributionResults(String dbNames[], int impactIndexes[], Config config) {
		var list = new ArrayList<Product>();
		println("Connect to databases ");
		Product.criteria = config.comparisonCriteria;
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
		Random rand = new Random();

		List<Product> products = new ArrayList<>();
		Product.criteria = config.comparisonCriteria;
		for (int i = 0; i < productsAmount; i++) {
			List<Contribution<CategorizedDescriptor>> l = new ArrayList<>();
			for (int j = 0; j < config.NB_Product_Results; j++) {
				Contribution<CategorizedDescriptor> c = new Contribution<>();
				var p = new ProcessDescriptor();
				p.name = String.valueOf(rand.nextInt() % 10);
				c.item = p;
				c.amount = Double.valueOf(p.name);
				l.add(c);
			}
			var p1 = new Product(l, "Product " + i);
			products.add(p1);
		}
		return products;
	}
}