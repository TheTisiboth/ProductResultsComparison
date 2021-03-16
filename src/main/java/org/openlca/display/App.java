package org.openlca.display;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.openlca.core.database.Derby;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
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

		var db = Derby.fromDataDir(dbName);
		var productSystem = db.get(ProductSystem.class, "7c16aba1-a7d2-4559-b336-a2208a52a25d");
		// CML-IA non-baseline
//		var impactMethod = new ImpactMethodDao(db).getDescriptorForRefId("46f19b82-ee92-3ff9-b909-d7cab2647b16");
		// Boulay et al 2011(Human Health)
		var impactMethod = new ImpactMethodDao(db).getDescriptorForRefId("3f290cab-a3ac-38af-b940-f31faf74cbe4");
		var setup = new CalculationSetup(productSystem);
		setup.impactMethod = impactMethod;
		var calc = new SystemCalculator(db);
		var fullResult = calc.calculateFull(setup);
		return new myData(productSystem, impactMethod, fullResult, db);
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