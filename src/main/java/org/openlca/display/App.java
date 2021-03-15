package org.openlca.display;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.derby.DerbyDatabase;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.matrix.ImpactIndex;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.CategorizedDescriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.model.descriptors.ProcessDescriptor;
import org.openlca.core.results.Contribution;
import org.openlca.core.results.ContributionResult;
import org.openlca.julia.Julia;
import org.openlca.util.Pair;

public class App {
	private static void println(String s) {
		System.out.println(s);
	}

	public static void main(String[] args) throws Exception {

		Julia.load();
//		Input input = Arguments.parse(Input.class, args);

		Config config = new Config(); // Contains global parameters
		Display display = new Display();
		Shell shell = new Shell(display, SWT.CLOSE | SWT.TITLE | SWT.RESIZE | SWT.MAX);
		shell.setText("Product comparison GUI");
		shell.setLayout(new GridLayout());
		myData data = null;
		String dbName = "ecoinvent_371_cutoff_unit_20210104";
		if (!config.useFakeResults) {
			data = getContributionResults(dbName, config);
		}
		new ProductDisplay(shell, config, data, dbName).display();
		shell.open();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();

	}

	private static myData getContributionResults(String dbName, Config config) {
		println("Connect to databases ");
		Product.criteria = config.colorCellCriteria;

		var db = DerbyDatabase.fromDataDir(dbName);
		var productSystem = db.get(ProductSystem.class, "7c16aba1-a7d2-4559-b336-a2208a52a25d");
		var impactMethod = new ImpactMethodDao(db).getDescriptorForRefId("3f290cab-a3ac-38af-b940-f31faf74cbe4");
		var setup = new CalculationSetup(productSystem);
		setup.impactMethod = impactMethod;
		var calc = new SystemCalculator(db);
		var fullResult = calc.calculateFull(setup);
		return new myData(productSystem, impactMethod, fullResult, db);
	}

	private static Pair<ImpactIndex, List<Product>> createProducts(int productsAmount, Config config) {
		// Create random numbers, in order to be the product results
		Random rand = new Random();
		var impactIndex = new ImpactIndex();

		List<Product> products = new ArrayList<>();
		Product.criteria = config.colorCellCriteria;
		for (int i = 0; i < productsAmount; i++) {
			var impactDescriptor = new ImpactDescriptor();
			impactDescriptor.id = i;
			impactDescriptor.name = "Impact Descriptor " + i;
			impactIndex.put(impactDescriptor);
			List<Contribution<CategorizedDescriptor>> l = new ArrayList<>();
			for (int j = 0; j < config.NB_Product_Results; j++) {
				Contribution<CategorizedDescriptor> c = new Contribution<>();
				var p = new ProcessDescriptor();
				p.name = String.valueOf(rand.nextInt() % 10);
				c.item = p;
				c.amount = Double.valueOf(p.name);
				l.add(c);
			}
			var p1 = new Product(l, "Product");
			products.add(p1);
		}
		return new Pair<ImpactIndex, List<Product>>(impactIndex, products);
	}
}

class myData {
	ProductSystem productSystem;
	ImpactMethodDescriptor impactMethod;
	ContributionResult contributionResult;
	IDatabase db;

	public myData(ProductSystem ps, ImpactMethodDescriptor im, ContributionResult cr, IDatabase d) {
		productSystem = ps;
		impactMethod = im;
		contributionResult = cr;
		db = d;
	}

}