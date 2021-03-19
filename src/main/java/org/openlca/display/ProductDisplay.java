package org.openlca.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.ColorDialog;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.openlca.core.database.IDatabase;
import org.openlca.core.database.ImpactCategoryDao;
import org.openlca.core.database.ImpactMethodDao;
import org.openlca.core.database.ProductSystemDao;
import org.openlca.core.math.CalculationSetup;
import org.openlca.core.math.SystemCalculator;
import org.openlca.core.model.Category;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
import org.openlca.core.model.descriptors.ImpactDescriptor;
import org.openlca.core.model.descriptors.ImpactMethodDescriptor;
import org.openlca.core.results.ContributionResult;

public class ProductDisplay {
	private Shell shell;
	private List<Product> products;
	private Point screenSize;
	private Config config;
	private Point origin;
	private final int xMargin;
	private final int productHeight;
	private final int gapBetweenProduct;
	private int theoreticalScreenHeight;
	private ColorCellCriteria colorCellCriteria;
	private Canvas canvas;
	private Map<ColorCellCriteria, Image> cacheMap;
	private Combo selectCategory;
	private Color chosenColor;
	private ScrollBar vBar;
	private ContributionResult contributionResult;
	private int nonCutoffAmount;
	private int cutOffSize;
	private IDatabase db;
	private ProductSystem productSystem;
	private ImpactMethodDescriptor impactMethod;
	private String dbName;
	private Map<String, ImpactDescriptor> impactCategoryMap;
	private List<String> impactCategories;
	private TargetCalculationEnum targetCalculation;
	private Composite impactCategoryTableComposite;
	private Composite composite;

	public ProductDisplay(Shell shell, Config config, myData data, String dbName) {
		this.dbName = dbName;
		this.db = data.db;
		this.shell = shell;
		this.config = config;
		productSystem = data.productSystem;
		impactMethod = data.impactMethod;
		products = new ArrayList<>();
		contributionResult = data.contributionResult;
		origin = new Point(0, 0);
		xMargin = 200;
		productHeight = 30;
		gapBetweenProduct = 300;
		theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * 2;
		canvas = null;
		colorCellCriteria = config.colorCellCriteria;
		cacheMap = new HashMap<>();
		nonCutoffAmount = 100;
		cutOffSize = 25;
		impactCategories = new ArrayList<>();
		targetCalculation = TargetCalculationEnum.IMPACT;
	}

	/**
	 * Entry point of the program. Display the products, and draw links between each
	 * matching results
	 */
	void display() {
		System.out.println("Display start");
		Product.config = config;
		Product.updateComparisonCriteria(colorCellCriteria);
		Cell.config = config;

		/**
		 * Composite component
		 */
		var row1 = new Composite(shell, SWT.NONE);
		var row2 = new Composite(shell, SWT.NONE);
		row2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		row2.setLayout(new GridLayout(1, false));

		/**
		 * Canvas component
		 */
		canvas = new Canvas(row2, SWT.V_SCROLL);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/**
		 * VBar component
		 */
		vBar = canvas.getVerticalBar();
		vBar.setMinimum(0);

		addScrollListener(canvas, vBar);
		addResizeEvent(row2, canvas, vBar);

		row1.setLayout(new RowLayout());

		addPaintListener(canvas); // Once finished, we really paint the cache, so it avoids flickering

		createChooseTarget(row1, row2);
		createChoseImpactCategories(row1, row2);
		createAgregateCombo(row1, row2);
		createColorPicker(row1);
		createSelectedCategory(row1, row2);
		createSelectCutoffSize(row1, row2);
		createSelectAmountVisibleProcess(row1, row2);
		createRunCalculation(row1, row2);

	}

	private void createChooseTarget(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Target : ");
		final Combo c = new Combo(row1, SWT.READ_ONLY);
		c.setBounds(50, 50, 150, 65);
		String values[] = TargetCalculationEnum.valuesToString();
		c.setItems(values);
		var index = ArrayUtils.indexOf(values, targetCalculation.toString());
		c.select(index);
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				var choice = c.getItem(c.getSelectionIndex());
				TargetCalculationEnum criteria = TargetCalculationEnum.getTarget(choice);
				if (!targetCalculation.equals(criteria)) {
					targetCalculation = criteria;
					createImpactCategoryTable(composite);
				}
			}
		});
	}

	/**
	 * Dropdown menu, allow us to chose different Impact Categories
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void createChoseImpactCategories(Composite row1, Composite row2) {
		var b = new Button(row1, SWT.NONE);
		composite = new Composite(row1, SWT.BORDER);
		var impactCategoryTable = createImpactCategoryTable(composite);
		impactCategoryMap = contributionResult.getImpacts().stream().map(impactCategory -> {
			TableItem item = new TableItem(impactCategoryTable, SWT.BORDER);
			item.setText(impactCategory.id + ": " + impactCategory.name);
			item.setChecked(true);
			impactCategories.add(item.getText());
			return impactCategory;
		}).collect(Collectors.toMap(impactCategory -> impactCategory.id + ": " + impactCategory.name,
				impactCategory -> impactCategory));

		b.setText("Toggle");
		b.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				Arrays.stream(impactCategoryTable.getItems()).forEach(item -> {
					item.setChecked(!item.getChecked());
					if (item.getChecked()) {
						impactCategories.add(item.getText());
					} else {
						impactCategories.remove(item.getText());
					}
				});
			}
		});
		b.pack();
		impactCategoryTable.setSize(300, 100);
		row1.setSize(300, 100);
//		row1.redraw();
	}

	private Table createImpactCategoryTable(Composite composite) {
		int typeButton;
		if (TargetCalculationEnum.IMPACT.equals(targetCalculation)) {
			typeButton = SWT.CHECK;
		} else {
			typeButton = SWT.RADIO;
		}
		var impactCategoryTable = new Table(composite, typeButton | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		impactCategoryTable.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					if (((TableItem) event.item).getChecked()) {
						impactCategories.add(((TableItem) event.item).getText());
					} else {
						impactCategories.remove(((TableItem) event.item).getText());
					}
				} else {
					var checked = ((TableItem) event.item).getChecked();
					((TableItem) event.item).setChecked(!checked);
					if (!checked) {
						impactCategories.add(((TableItem) event.item).getText());
					} else {
						impactCategories.remove(((TableItem) event.item).getText());
					}
				}
			}
		});
		return impactCategoryTable;
	}

	/**
	 * Dropdown menu, allow us to chose by what we want to agregate the contribution
	 * results
	 * 
	 * @param row1 The menu bar
	 */
	private void createAgregateCombo(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Color cells by : ");
		final Combo c = new Combo(row1, SWT.READ_ONLY);
		c.setBounds(50, 50, 150, 65);
		String values[] = ColorCellCriteria.valuesToString();
		c.setItems(values);
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				var choice = c.getItem(c.getSelectionIndex());
				ColorCellCriteria criteria = ColorCellCriteria.getCriteria(choice);
				if (!colorCellCriteria.equals(criteria)) {
					colorCellCriteria = criteria;
					Product.updateComparisonCriteria(criteria);
					products.stream().forEach(p -> p.updateCellsColor());
					triggerComboSelection(selectCategory, true);
					redraw(row2, true);
				}
			}
		});
	}

	/**
	 * Dropdown menu, allow us to chose a specific Process Category to color
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void createSelectedCategory(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Select Product Category : ");
		selectCategory = new Combo(row1, SWT.READ_ONLY);
		selectCategory.setBounds(50, 50, 500, 65);
		var<String, Descriptor> categoryMap = new HashMap<String, Descriptor>();
		selectCategory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (selectCategory.getSelectionIndex() == -1) { // Nothing is selected : initialisation
					resetDefaultColorCells();
					var<String> list = products.stream().flatMap(p -> p.getList().stream().flatMap(results -> results
							.getResult().stream().filter(r -> r.getContribution().item != null).map(r -> {
								var categoryId = r.getContribution().item.category;
								var cat = db.getDescriptor(Category.class, categoryId);
								if (categoryMap.get(cat.name) == null) {
									categoryMap.put(cat.name, cat);
								}
								return cat.name;
							}))).distinct().sorted().collect(Collectors.toList());
					list.add(0, "");
					selectCategory.setItems(list.toArray(String[]::new));
				} else if (selectCategory.getSelectionIndex() == 0) { // Empty value is selected : reset
					resetDefaultColorCells();
					redraw(row2, true);
				} else { // A category is selected : update color
					resetDefaultColorCells();
					var catId = categoryMap.get(selectCategory.getItem(selectCategory.getSelectionIndex())).id;
					products.stream().forEach(p -> p.getList().stream().forEach(c -> {
						if (c.getResult().get(0).getContribution().item.category == catId) {
							c.setRgb(chosenColor.getRGB());
						}
					}));
					redraw(row2, true);
				}
			}
		});
	}

	/**
	 * Trigger a selection event to a combo component
	 * 
	 * @param deselect Indicate if we deselect the selected value of the combo
	 */
	private void triggerComboSelection(Combo combo, boolean deselect) {
		if (deselect) {
			combo.deselectAll();
		}
		Event event = new Event();
		event.widget = combo;
		event.display = combo.getDisplay();
		event.type = SWT.Selection;
		combo.notifyListeners(SWT.Selection, event);
	}

	/**
	 * Reset the default color of the cells
	 */
	public void resetDefaultColorCells() {
		RGB rgb = chosenColor.getRGB();
		// Reset categories colors to default (just for the one which where changed)
		products.stream().forEach(p -> p.getList().stream().filter(cell -> cell.getRgb().equals(rgb))
				.forEach(cell -> cell.resetDefaultRGB()));
	}

	/**
	 * The swt widget that allows to pick a custom color
	 * 
	 * @param composite The parent component
	 */
	private void createColorPicker(Composite composite) {
		// Default color
		chosenColor = new Color(shell.getDisplay(), new RGB(255, 0, 255));
		// Use a label full of spaces to show the color
		final Label colorLabel = new Label(composite, SWT.NONE);
		colorLabel.setText("    ");
		colorLabel.setBackground(chosenColor);
		Button button = new Button(composite, SWT.PUSH);
		button.setText("Color...");
		button.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				// Create the color-change dialog
				ColorDialog dlg = new ColorDialog(shell);
				// Set the selected color in the dialog from
				// user's selected color
				dlg.setRGB(colorLabel.getBackground().getRGB());
				// Change the title bar text
				dlg.setText("Choose a Color");
				// Open the dialog and retrieve the selected color
				RGB rgb = dlg.open();
				if (rgb != null) {
					// Dispose the old color, create the
					// new one, and set into the label
					chosenColor.dispose();
					chosenColor = new Color(composite.getDisplay(), rgb);
					colorLabel.setBackground(chosenColor);
					triggerComboSelection(selectCategory, false);
				}
			}
		});
	}

	/**
	 * Spinner allowing to set the ratio of the cutoff area
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void createSelectCutoffSize(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Select cutoff size (%): ");
		var selectCutoff = new Spinner(row1, SWT.BORDER);
		selectCutoff.setBounds(50, 50, 500, 65);
		selectCutoff.setMinimum(0);
		selectCutoff.setMaximum(100);
		selectCutoff.setSelection(cutOffSize);
		selectCutoff.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event e) {
				if (e.keyCode == 13) { // If we press Enter
					cutOffSize = selectCutoff.getSelection();
					redraw(row2, true);
				}
			}
		});
	}

	/**
	 * Spinner allowing to set the amount of visible process
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void createSelectAmountVisibleProcess(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Select amount non cutoff process: ");
		var selectCutoff = new Spinner(row1, SWT.BORDER);
		selectCutoff.setBounds(50, 50, 500, 65);
		selectCutoff.setMinimum(0);
		selectCutoff.setMaximum(10000);
		selectCutoff.setSelection(nonCutoffAmount);
		selectCutoff.addListener(SWT.KeyDown, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if (arg0.keyCode == 13) { // If we press Enter
					nonCutoffAmount = selectCutoff.getSelection();
					redraw(row2, true);
				}
			}
		});
	}

	private void createRunCalculation(Composite row1, Composite row2) {
		var runCalculation = new Button(row1, SWT.None);
		runCalculation.setText("Run calculation");
		runCalculation.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event e) {
				products = new ArrayList<>();
				if (TargetCalculationEnum.IMPACT.equals(targetCalculation)) {
					impactCategories.stream().forEach(item -> {
						var cs = contributionResult.getProcessContributions(impactCategoryMap.get(item));
						var p = new Product(cs, item, null);
						products.add(p);
					});
				} else {
					new ProductSystemDao(db).getAll().stream().forEach(ps -> {
						var setup = new CalculationSetup(ps);
						setup.impactMethod = impactMethod;
						var calc = new SystemCalculator(db);
						var fullResult = calc.calculateFull(setup);
						var impactCategory = impactCategoryMap.get(impactCategories.get(0));
						var cs = fullResult.getProcessContributions(impactCategory);
						var p = new Product(cs, impactCategory.id + ": " + impactCategory.name, ps.id + ": " + ps.name);
						products.add(p);
					});
				}
				theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * (products.size() - 1);
				vBar.setMaximum(theoreticalScreenHeight);
				sortProducts();
				triggerComboSelection(selectCategory, true);
				redraw(row2, true);
			}
		});
	}

	/**
	 * Sort products by ascending amount, according to the comparison criteria
	 */
	private void sortProducts() {
		Product.updateComparisonCriteria(colorCellCriteria);
		products.stream().forEach(p -> p.sort());
	}

	/**
	 * Redraw everything
	 * 
	 * @param recompute Tell if we have to recompute the categories. If false, then
	 *                  we just redraw the whole objects
	 */
	private void redraw(Composite composite, boolean recompute) {
		screenSize = shell.getSize();
		// Cached image, in which we draw the things, and then display it once it is
		// finished
		Image cache = null;
		if (recompute) { // If we recompute, we draw a brand new Image
			cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
			cacheMap.put(colorCellCriteria, cache);
			cachedPaint(composite, cache); // Costly painting, so we cache it; Called one time at the beginning
		} else {
			// Otherwise, we take a cached Image
			cache = cacheMap.get(colorCellCriteria);
			if (cache == null) {
				cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
				cacheMap.put(colorCellCriteria, cache);
				cachedPaint(composite, cache); // Costly painting, so we cache it; Called one time at the beginning
			}
		}
		canvas.redraw();
		Rectangle client = canvas.getClientArea();
		vBar.setThumb(Math.min(theoreticalScreenHeight, client.height));
		vBar.setPageIncrement(Math.min(theoreticalScreenHeight, client.height));
		vBar.setIncrement(20);
	}

	/**
	 * Costly painting method. For each product, it draws links between each
	 * matching results. Since it is costly, it is firstly drawed in an image. Once
	 * it is finished, we paint the image
	 * 
	 * @param composite The parent component
	 * @param cache     The cached image in which we are drawing
	 */
	private void cachedPaint(Composite composite, Image cache) {
		GC gc = new GC(cache);
		screenSize = composite.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.85; // 90% of the screen width
		// Start point of the first product rectangle
		Point rectEdge = new Point(0 + xMargin, 0 + xMargin);
		Point textPos = new Point(5, 5);
		gc.drawText("Database : " + dbName, textPos.x, textPos.y);
		textPos.y += 30;
		if (TargetCalculationEnum.IMPACT.equals(targetCalculation)) {
			gc.drawText("Product system : " + productSystem.name, textPos.x, textPos.y);
			textPos.y += 30;
		}
		gc.drawText("Impact assessment method : " + impactMethod.name, textPos.x, textPos.y);
		var optional = products.stream()
				.mapToDouble(p -> p.getList().stream().mapToDouble(c -> c.getNormalizedAmount()).sum()).max();
		double maxSumAmount = 0.0;
		if (optional.isPresent()) {
			maxSumAmount = optional.getAsDouble();
		}
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			handleProduct(gc, maxProductWidth, rectEdge, productIndex, maxSumAmount);
			rectEdge = new Point(rectEdge.x, rectEdge.y + 300);
		}
		drawLinks(gc);
	}

	/**
	 * Handle the current product. Draw a rectangle, write the product name in it,
	 * and handle the product results
	 * 
	 * @param gc              The GC component
	 * @param maxProductWidth The maximal width for a product
	 * @param rectEdge        The coordinate of the product rectangle
	 * @param productIndex    The index of the current product
	 * @param maxAmount       The max amounts sum of the products
	 */
	private void handleProduct(GC gc, double maxProductWidth, Point rectEdge, int productIndex, double maxSumAmount) {
		var p = products.get(productIndex);
		int productWidth = (int) maxProductWidth;
		// Draw the product name
		Point textPos = new Point(rectEdge.x - xMargin, rectEdge.y + 8);
		gc.drawText("Contribution result " + productIndex, textPos.x, textPos.y);
		textPos.y += 25;
		if (TargetCalculationEnum.PRODUCT.equals(targetCalculation)) {
			gc.drawText("Product System : " + p.getProductSystemName(), textPos.x, textPos.y);
			textPos.y += 25;
		}
		gc.drawText("Impact : " + p.getImpactCategoryName(), textPos.x, textPos.y);
		productWidth = handleCells(gc, rectEdge, productIndex, p, productWidth, maxSumAmount);

		if (productIndex == 0) { // Draw an arrow to show the way the results are ordered
			Point startPoint = new Point(rectEdge.x, rectEdge.y - 50);
			Point endPoint = new Point((int) (startPoint.x + maxProductWidth), startPoint.y);
			drawLine(gc, startPoint, endPoint, null, null);
			startPoint = new Point(endPoint.x - 15, endPoint.y + 15);
			drawLine(gc, startPoint, endPoint, null, null);
			startPoint = new Point(endPoint.x - 15, endPoint.y - 15);
			drawLine(gc, startPoint, endPoint, null, null);
		}
		// Draw a rectangle for each product
		gc.drawRectangle(rectEdge.x, rectEdge.y, productWidth, productHeight);
	}

	/**
	 * Handle the cells, and display a rectangle for each of them (and merge the
	 * cutoff one in on visual cell)
	 * 
	 * @param gc           The GC component
	 * @param rectEdge     The coordinate of the product rectangle
	 * @param productIndex The index of the current product
	 * @param p            The current product
	 * @param productWidth The product width
	 * @param maxAmount    The max amounts sum of the products
	 * @return The new product width
	 */
	private int handleCells(GC gc, Point rectEdge, int productIndex, Product p, int productWidth, double maxSumAmount) {
		var cells = p.getList();
		// Sum all the distincts values
		double normalizedTotalAmountSum = cells.stream().mapToDouble(cell -> Math.abs(cell.getNormalizedAmount()))
				.sum();
		System.out.println("Product " + productIndex + " : " + normalizedTotalAmountSum + " amounts sum");
		int minimumProductWidth = (int) (0.3 * productWidth);
//		productWidth = (int) (productWidth * (normalizedTotalAmountSum / maxSumAmount));
		if (productWidth < minimumProductWidth) {
			// We set a minimum width, so the rectangle is not too small (in case
			// of big differences )
			productWidth = minimumProductWidth;
		}
		var wrapper = new Object() {
			int width;
		};
		wrapper.width = productWidth;
		long amountCutOff = cells.size() - nonCutoffAmount;
		double totalAmountSumNonCutOff = cells.stream().skip(amountCutOff)
				.mapToDouble(cell -> Math.abs(cell.getNormalizedAmount())).sum();

		double cutoffRectangleSizeRatio = cutOffSize / 100.0;
		double nonCutOffRecangleSizeRatio = 1 - cutoffRectangleSizeRatio;
		long amountNonCutOffBigEnoughContribution = cells.stream().skip(amountCutOff)
				.filter(cell -> Math.abs(cell.getNormalizedAmount()) * totalAmountSumNonCutOff
						* nonCutOffRecangleSizeRatio * wrapper.width > 1)
				.count();
		// Minimum space between each cells
		double gap = (productWidth * cutoffRectangleSizeRatio / amountCutOff);
		int chunk = -1, chunkSize = 0;
		boolean gapEnoughBig = true;
		var newProductWidth = 0.0;
		if (gap < 1.0) {
			// If the gap is to small, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(1 / gap);
			gapEnoughBig = false;
		}
		int minCellWidth = 3;
		Point start = null;
		var newChunk = 0;
		boolean isCutOff = true;
		RGB rgbCutOff = new RGB(192, 192, 192);
		int cutOffSize = 0;
		int nonCutoffSize = 0;
		int nonCutoffBigEnoughSize = 0;
		for (var cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
			if (start == null) {
				start = new Point(rectEdge.x + 1, rectEdge.y + 1);
			}
			if (cellIndex >= amountCutOff) {
				if (isCutOff) {
					isCutOff = false;
					gc.setBackground(new Color(gc.getDevice(), rgbCutOff));
					gc.fillRectangle(rectEdge.x + 1, rectEdge.y + 1, (int) newProductWidth, productHeight - 1);
					cutOffSize = (int) newProductWidth;
					nonCutoffSize = productWidth - cutOffSize;
					nonCutoffBigEnoughSize = (int) (nonCutoffSize
							- (nonCutoffAmount - amountNonCutOffBigEnoughContribution) * minCellWidth);
					gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
					chunk = -1;
					chunkSize = 0;
					gapEnoughBig = true;
					gap = (productWidth * nonCutOffRecangleSizeRatio / (cells.size() - amountCutOff));
					if (gap < 1.0) {
						chunkSize = (int) Math.ceil(1 / gap);
						gapEnoughBig = false;
					}
				}
			}
			if (!gapEnoughBig) {
				newChunk = computeChunk(chunk, chunkSize, cellIndex);
			}
			var cell = cells.get(cellIndex);
			int cellWidth = 0;
			if (!gapEnoughBig && chunk != newChunk) {
				// We are on a new chunk, so we draw a cell with a width of 1 pixel
				cellWidth = 1;
			} else if (!gapEnoughBig && chunk == newChunk) {
				// We stay on the same chunk, so we don't draw the cell
				cellWidth = 0;
			} else {
				var value = cell.getNormalizedAmount();
				if (cellIndex >= amountCutOff) {
					if (cellIndex == cells.size() - 1) {
						cellWidth = (int) (productWidth - newProductWidth);
					} else {
						var percentage = value / totalAmountSumNonCutOff;
						cellWidth = (int) (productWidth * nonCutOffRecangleSizeRatio * percentage);
						if (cellWidth < minCellWidth) {
							cellWidth = minCellWidth;
						} else {
							cellWidth = (int) (nonCutoffBigEnoughSize * percentage);
						}
					}
				} else {
					var percentage = value / normalizedTotalAmountSum;
					cellWidth = (int) (productWidth * percentage);
				}

			}
			newProductWidth += cellWidth;
			if (cellIndex >= amountCutOff) {
				gc.setBackground(new Color(gc.getDevice(), cell.getRgb()));
				gc.fillRectangle(start.x, start.y, cellWidth, productHeight - 1);
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			}
			var end = computeEndCell(start, cell, (int) cellWidth, isCutOff);
			if (gapEnoughBig || !gapEnoughBig && chunk != newChunk) {
				// We end the current chunk / cell
				start = end;
				chunk = newChunk;
			}

		}
		return (int) newProductWidth;
	}

	/**
	 * Tells in which chunk we are
	 * 
	 * @param chunk       Index of the current chunk
	 * @param chunkSize   Amount of cells in a chunk
	 * @param resultIndex The cell index
	 * @return The new chunk index
	 */
	private int computeChunk(int chunk, int chunkSize, int cellIndex) {
		// Every chunkSize, we increment the chunk
		var newChunk = (cellIndex % (int) chunkSize) == 0;
		if (newChunk == true) {
			chunk++;
		}
		return chunk;
	}

	/**
	 * Compute the end of the current cell, and set some important information about
	 * the cell
	 * 
	 * @param start     The starting point of the cell
	 * @param cell      The current cell
	 * @param cellWidth The width of the cell
	 * @return The end point of the cell
	 */
	private Point computeEndCell(Point start, Cell cell, int cellWidth, boolean isCutoff) {
		var end = new Point(start.x + cellWidth, start.y);
		var startingPoint = new Point((end.x + start.x) / 2, start.y + productHeight);
		var endingPoint = new Point(startingPoint.x, start.y - 2);
		cell.setData(startingPoint, endingPoint, start.x, end.x, isCutoff);
		return end;
	}

	/**
	 * Draw a line, with an optional color
	 * 
	 * @param gc          The GC
	 * @param start       The starting point
	 * @param end         The ending point
	 * @param beforeColor The color of the line
	 * @param afterColor  The color to get back after the draw
	 */
	private void drawLine(GC gc, Point start, Point end, Object beforeColor, Integer afterColor) {
		if (beforeColor != null) {
			if (beforeColor instanceof Integer) {
				gc.setForeground(gc.getDevice().getSystemColor((int) beforeColor));
			} else {
				gc.setForeground(new Color(gc.getDevice(), (RGB) beforeColor));
			}
		}
		gc.drawLine(start.x, start.y, end.x, end.y);
		if (afterColor != null) {
			gc.setForeground(gc.getDevice().getSystemColor(afterColor));
		}
	}

	/**
	 * Draw the links between each matching results
	 * 
	 * @param gc The GC component
	 */
	private void drawLinks(GC gc) {
		for (int productIndex = 0; productIndex < products.size() - 1; productIndex++) {
			var cells = products.get(productIndex);
			for (Cell cell : cells.getList()) {
				if (cell.isLinkDrawable()) {
					var nextCells = products.get(productIndex + 1);
					// We search for a cell that has the same process
					var optional = nextCells.getList().stream()
							.filter(next -> next.getResult().get(0).getContribution().item
									.equals(cell.getResult().get(0).getContribution().item))
							.findFirst();
					if (optional.isPresent()) {
						var linkedCell = optional.get();
						var startPoint = cell.getStartingLinkPoint();
						var endPoint = linkedCell.getEndingLinkPoint();
						if (config.useBezierCurve) {
							drawBezierCurve(gc, startPoint, endPoint, cell.getRgb());
						} else {
							drawLine(gc, startPoint, endPoint, cell.getRgb(), null);
						}
					}
				}
			}
		}
	}

	/**
	 * Draw a bezier curve, between 2 points
	 * 
	 * @param gc    The GC component
	 * @param start The starting point
	 * @param end   The ending point
	 * @param rgb   The color of the curve
	 */
	private void drawBezierCurve(GC gc, Point start, Point end, RGB rgb) {
		gc.setForeground(new Color(gc.getDevice(), rgb));
		Path p = new Path(gc.getDevice());
		p.moveTo(start.x, start.y);
		int offset = 100;
		Point ctrlPoint1 = new Point(start.x + offset, start.y + offset);
		Point ctrlPoint2 = new Point(end.x - offset, end.y - offset);
		p.cubicTo(ctrlPoint1.x, ctrlPoint1.y, ctrlPoint2.x, ctrlPoint2.y, end.x, end.y);
		gc.drawPath(p);
		gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
	}

	/**
	 * Add a scroll listener to the canvas
	 * 
	 * @param canvas The canvas component
	 * @param vBar   The scrolling vertical bar
	 */
	private void addScrollListener(Canvas canvas, ScrollBar vBar) {
		vBar.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				int vSelection = vBar.getSelection();
				int destY = -vSelection - origin.y;
				canvas.scroll(0, destY, 0, 0, canvas.getSize().x, canvas.getSize().y, false);
				origin.y = -vSelection;
			}
		});
	}

	/**
	 * Add a resize listener to the canvas
	 * 
	 * @param composite Parent composent of the canvas
	 * @param canvas    The Canvas component
	 * @param vBar      The scrolling vertical bar
	 */
	private void addResizeEvent(Composite composite, Canvas canvas, ScrollBar vBar) {
		canvas.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event e) {
				Rectangle client = canvas.getClientArea();
				vBar.setThumb(Math.min(theoreticalScreenHeight, client.height));
				vBar.setPageIncrement(Math.min(theoreticalScreenHeight, client.height));
				vBar.setIncrement(20);
				int vPage = canvas.getSize().y - client.height;
				int vSelection = vBar.getSelection();
				if (vSelection >= vPage) {
					if (vPage <= 0)
						vSelection = 0;
					origin.y = -vSelection;
				}
				redraw(composite, true);
			}
		});
	}

	/**
	 * Add a paint listener to the canvas. This is called whenever the canvas needs
	 * to be redrawed, then it draws the cached image
	 * 
	 * @param canvas A Canvas component
	 */
	private void addPaintListener(Canvas canvas) {
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(cacheMap.get(colorCellCriteria), origin.x, origin.y);
			}
		});
	}
}