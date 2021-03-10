package org.openlca.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
	private ComparisonCriteria comparisonCriteria;
	private Canvas canvas;
	private Map<ComparisonCriteria, Image> cacheMap;
	private double maxCriteriaValue;
	private double minCriteriaValue;
	private Map<ComparisonCriteria, List<List<Cell>>> categoriesMap;
	private Combo categoriesValuesSelection;
	private Combo selectCategory;
	private Color chosenColor;
	private ScrollBar vBar;
	private ContributionResult contributionResult;
	private final String dbName;
	private double cutOff;

	public ProductDisplay(Shell shell, Config config, ContributionResult result, String dbName) {
		this.dbName = dbName;
		this.shell = shell;
		this.config = config;
		products = new ArrayList<>();
		contributionResult = result;
		origin = new Point(0, 0);
		xMargin = 200;
		productHeight = 30;
		gapBetweenProduct = 300;
		theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * 4;
		canvas = null;
		maxCriteriaValue = 0;
		minCriteriaValue = 0;
		comparisonCriteria = config.comparisonCriteria;
		categoriesMap = new HashMap<>();
		categoriesMap.put(comparisonCriteria, new ArrayList<>());
		cacheMap = new HashMap<>();
		cutOff = 0.15;
	}

	/**
	 * Display the products, and draw links between each matching results
	 */
	void display() {
		System.out.println("Display start");

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
		vBar.setMaximum(theoreticalScreenHeight);
		vBar.setMinimum(0);

		addScrollListener(canvas, vBar);
		addResizeEvent(row2, canvas, vBar);

		row1.setLayout(new RowLayout());

		redraw(row2, true);
		addPaintListener(canvas); // Once finished, we really paint the cache, so it avoids flickering
		createChoseImpactCategories(row1, row2);
		createAgregateCombo(row1);
//		createComparisonCriteriaCombo(row1, row2);
//		createSelectValueCombo(row1, row2);
		createColorPicker(row1);
		createSelectedCategory(row1);

	}

	private void createChoseImpactCategories(Composite row1, Composite row2) {
		MultipleSelectionCombo msc = new MultipleSelectionCombo(row1, SWT.BORDER);
		// TODO
		// Handle impact with same names
		var indexMap = contributionResult.impactIndex.content().stream().filter(distinctByKey(index -> index.name))
				.collect(Collectors.toMap(index -> index.name, index -> index));
		contributionResult.impactIndex.content().stream().forEach(i -> msc.add(i.name));
		msc.setSize(500, 500);

		msc.addModifyListener(new ModifyListener() {

			@Override
			public void modifyText(ModifyEvent e) {
				var t = msc.getSelections();
				products = new ArrayList<>();
				Arrays.stream(t).forEach(impactName -> {
					var cs = contributionResult.getProcessContributions(indexMap.get(impactName));
					var p = new Product(cs, impactName);
					products.add(p);
				});

				theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * (products.size());
				sortProducts();
				getCategories();
				triggerSelectValueComboSelectionListener(selectCategory, true);
				redraw(row2, true);
			}
		});

	}

	private void createAgregateCombo(Composite row1) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Agregate : ");
		final Combo c = new Combo(row1, SWT.READ_ONLY);
		c.setBounds(50, 50, 150, 65);

		String values[] = { "", "Category", "Location" };
		c.setItems(values);
	}

	private void createSelectedCategory(Composite row1) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Select Category : ");
		selectCategory = new Combo(row1, SWT.READ_ONLY);
		selectCategory.setBounds(50, 50, 400, 65);
		selectCategory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (selectCategory.getSelectionIndex() == -1) {
					var list = products.stream()
							.flatMap(p -> p.getList().stream().filter(r -> r.getContribution().item.category != null)
									.map(r -> r.getContribution().item.category.toString()).distinct())
							.collect(Collectors.toList());
					list.add(0, "");
					selectCategory.setItems(list.toArray(String[]::new));
				}
			}

		});
	}

	/**
	 * Create a combo component, in order to choose the comparison criteria
	 * 
	 * @param row1 First row, containing the combo components
	 * @param row2 Second row, containing the canvas
	 */
	private void createComparisonCriteriaCombo(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Comparison criteria : ");
		final Combo c = new Combo(row1, SWT.READ_ONLY);
		c.setBounds(50, 50, 150, 65);
		var criterias = ComparisonCriteria.valuesToString();
		var indexSelectedCriteria = ArrayUtils.indexOf(criterias, config.comparisonCriteria.toString());
		c.setItems(criterias);
		c.select(indexSelectedCriteria);
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ComparisonCriteria newCriteria = ComparisonCriteria.getCriteria(c.getText());
				if (newCriteria != comparisonCriteria) {
					if (categoriesValuesSelection.getSelectionIndex() != -1) {
						resetDefaultColorCategories();
					}
					comparisonCriteria = ComparisonCriteria.getCriteria(c.getText());
					origin = new Point(0, 0);
					vBar.setSelection(origin.y);
					// We reset the categories
					sortProducts();
					getCategories();
					triggerSelectValueComboSelectionListener(categoriesValuesSelection, true);
				}
			}
		});
	}

	/**
	 * Trigger a selection event to a combo component
	 * 
	 * @param deselect Indicate if we deselect the selected value of the combo
	 */
	private void triggerSelectValueComboSelectionListener(Combo combo, boolean deselect) {
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
	 * Create a combo component, in order to change the comparison criteria
	 * 
	 * @param row1 First row, containing the combo components
	 * @param row2 Second row, containing the canvas
	 */
	private void createSelectValueCombo(Composite row1, Composite row2) {
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Chosen value : ");
		categoriesValuesSelection = new Combo(row1, SWT.READ_ONLY);
		categoriesValuesSelection.setBounds(30, 50, 150, 65);
		setItemSelectValueCombo(categoriesValuesSelection);
		categoriesValuesSelection.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				var index = categoriesValuesSelection.getSelectionIndex();
				if (index == -1) { // Nothing is selected
					// Update the whole dinstinct categories values
					setItemSelectValueCombo(categoriesValuesSelection);
				} else {
					RGB rgb = chosenColor.getRGB();
					// Reset categories colors to default (just for the one which where changed)
					resetDefaultColorCategories();
					String selectedText = categoriesValuesSelection.getText();
					if (!"".equals(selectedText)) {
						double selectedValue = Double.valueOf(selectedText).doubleValue();
						// Set a chosen color to the chosen categories
						categoriesMap.get(comparisonCriteria).stream()
								.forEach(categories -> categories.stream()
										.filter(category -> selectedValue == category.getTargetValue())
										.forEach(category -> category.setRgb(rgb)));
					}

				}
				redraw(row2, true);
			}
		});
	}

	public void resetDefaultColorCategories() {
		RGB rgb = chosenColor.getRGB();
		// Reset categories colors to default (just for the one which where changed)
		categoriesMap.get(comparisonCriteria).stream().forEach(categories -> categories.stream()
				.filter(category -> category.getRgb().equals(rgb)).forEach(category -> category.resetDefaultRGB()));
	}

	/**
	 * Set the combo item with the different distinct categories values
	 * 
	 * @param combo The Combo component
	 */
	private void setItemSelectValueCombo(Combo combo) {
		List<Double> categoriesValues = categoriesMap.get(comparisonCriteria).stream()
				.flatMap(categories -> categories.stream().map(category -> category.getTargetValue())).distinct()
				.sorted().collect(Collectors.toList());
		List<String> values = categoriesValues.stream().map(e -> e.toString()).collect(Collectors.toList());
		values.add(0, "");
		combo.setItems(values.toArray(String[]::new));
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
//					triggerSelectValueComboSelectionListener(categoriesValuesSelection, false);
				}
			}
		});
	}

	/**
	 * Sort products by descending amount, according to the comparison criteria
	 */
	private void sortProducts() {
		Product.updateComparisonCriteria(comparisonCriteria);
		products.stream().forEach(p -> p.sort());
		maxCriteriaValue = products.stream().mapToDouble(p -> p.max()).max().getAsDouble();
		minCriteriaValue = products.stream().mapToDouble(p -> p.min()).min().getAsDouble();
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
			cacheMap.put(comparisonCriteria, cache);
			cachedPaint(composite, cache); // Costly painting, so we cache it; Called one time at the beginning
		} else {
			// Otherwise, we take a cached Image
			cache = cacheMap.get(comparisonCriteria);
			if (cache == null) {
				cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
				cacheMap.put(comparisonCriteria, cache);
				cachedPaint(composite, cache); // Costly painting, so we cache it; Called one time at the beginning
			}
		}
		canvas.redraw();
	}

	/**
	 * Costly painting method. For each product, it draws links between each
	 * matching results. Since it is costly, it is firstly drawed in an image. Once
	 * it is finished, we paint the image
	 * 
	 * @param recompute Tell if we have to recompute the categories. If false, then
	 *                  we just redraw the whole objects
	 * @param composite The parent component
	 */
	private void cachedPaint(Composite composite, Image cache) {
		GC gc = new GC(cache);
		screenSize = composite.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.8; // 80% of the screen width
		// Start point of the first product rectangle
		Point rectEdge = new Point(0 + xMargin, 0 + xMargin);
		var categories = getCategories();
		var maxAmount = categories.stream().mapToDouble(categoryList -> categoryList.stream()
				.map(c -> c.getNormalizedAmount()).reduce(0.0, (subtotal, amount) -> subtotal + amount)).max();
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			handleProduct(gc, maxProductWidth, rectEdge, productIndex, categories.get(productIndex), maxAmount.getAsDouble());
			rectEdge = new Point(rectEdge.x, rectEdge.y + 300);
		}
		drawLinks(gc);
	}

	private List<List<Cell>> getCategories() {
		// Get all distinct values, create a category for each of them
		var list = categoriesMap.get(comparisonCriteria);
		if (list == null) {
			list = new ArrayList<>();
		}

		list = products.stream()
				.map(p -> p.getList().stream().filter(r -> !r.isContributionEmpty())
						.map(r -> new Cell(r, config, minCriteriaValue, maxCriteriaValue)).collect(Collectors.toList()))
				.collect(Collectors.toList());
		categoriesMap.put(comparisonCriteria, list);

		return list;
	}

	/**
	 * Handle the current product. Draw a rectangle, write the product name in it,
	 * and handle the product results
	 * 
	 * @param gc              The GC component
	 * @param maxProductWidth The maximal width for a product
	 * @param rectEdge        The coordinate of the product rectangle
	 * @param productIndex    The index of the current product
	 * @param categoriesList
	 * @param maxAmount 
	 * @param recompute       Tell if we have to recompute the categories. If false,
	 *                        then we just redraw the whole objects
	 */
	private void handleProduct(GC gc, double maxProductWidth, Point rectEdge, int productIndex,
			List<Cell> categoriesList, double maxAmount) {
		var p = products.get(productIndex);
		int productWidth = (int) maxProductWidth;
		// Draw the product name
		Point textPos = new Point(rectEdge.x - xMargin, rectEdge.y + 8);
		gc.drawText(dbName, textPos.x, textPos.y);
		textPos.y += 25;
		gc.drawText("Impact : " + p.getName(), textPos.x, textPos.y);

		productWidth = handleCategories(gc, rectEdge, productIndex, p, productWidth, categoriesList, maxAmount);

		if (productIndex == 0) { // Draw an arrow to show the way the results are ordered
			Point startPoint = new Point(rectEdge.x, rectEdge.y - 50);
			Point endPoint = new Point(startPoint.x + productWidth, startPoint.y);
			drawLine(gc, startPoint, endPoint, null, null);
			startPoint = new Point(endPoint.x - 15, endPoint.y + 15);
			drawLine(gc, startPoint, endPoint, null, null);
			startPoint = new Point(endPoint.x - 15, endPoint.y - 15);
			drawLine(gc, startPoint, endPoint, null, null);
		}
		p.setBounds(rectEdge, productWidth);
		// Draw a rectangle for each product
		gc.drawRectangle(rectEdge.x, rectEdge.y, productWidth, productHeight);
	}

	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {

		Map<Object, Boolean> seen = new ConcurrentHashMap<>();
		return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
	}

	/**
	 * Handle the categories, which represent a bundle of same values
	 * 
	 * @param gc             The GC component
	 * @param rectEdge       The coordinate of the product rectangle
	 * @param productIndex   The index of the current product
	 * @param p              The current product
	 * @param productWidth   The product width
	 * @param categories
	 * @param maxAmount 
	 * @param recompute      Tell if we have to recompute the categories. If false,
	 *                       then we just redraw the whole objects
	 * @param drawSeparation
	 * @param chunkSize
	 * @param chunk
	 * @param gap
	 * @return
	 */
	private int handleCategories(GC gc, Point rectEdge, int productIndex, Product p, int productWidth,
			List<Cell> categories, double maxAmount) {
		final int categoriesAmount = categories.size();
		double maxAmountCurrentProduct = p.max();
		// Sum all the distincts values
		double resultsSum = categories.stream().mapToDouble(category -> Math.abs(category.getNormalizedValue())).sum();
		productWidth = (int) (productWidth * (resultsSum/maxAmount));
		double gap = ((double) productWidth / categoriesAmount);
		int chunk = -1, chunkSize = 0;
		boolean gapEnoughBig = true;
		var newProductWidth = 0;
		if (gap < 1.0) {
			// If the gap is to small, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(1 / gap);
			gapEnoughBig = false;
		}

		Point start = null;
		var newChunk = 0;
		System.out.println();
		System.out.println("Product" + productIndex + " : " + p.getList().size() + " contribution results");
		System.out.println("Product " + productIndex + " : " + categoriesAmount + " categories");
		for (var categoriesIndex = 0; categoriesIndex < categories.size(); categoriesIndex++) {
			if (!gapEnoughBig) {
				newChunk = computeChunk(gap, chunk, chunkSize, categoriesIndex);
			}
			var category = categories.get(categoriesIndex);
			if (start == null) {
				start = new Point(rectEdge.x + 1, rectEdge.y + 1);
			}
			int categoryWidth = 0;
			if (!gapEnoughBig && chunk != newChunk) {
				// We are on a new chunk, so we draw a category with a width of 1 pixel
				categoryWidth = 1;
			} else if (!gapEnoughBig && chunk == newChunk) {
				// We stay on the same chunk, so we don't draw the category
				categoryWidth = 0;
			} else {
				var value = category.getNormalizedValue();
				var percentage = value / resultsSum;
				categoryWidth = (int) (productWidth * percentage);
			}
			newProductWidth += categoryWidth;
			gc.setBackground(new Color(gc.getDevice(), category.getRgb()));
			gc.fillRectangle(start.x, start.y, categoryWidth, productHeight - 1);
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			var end = computeEndCategory(start, category, categoryWidth);
			if (gapEnoughBig || !gapEnoughBig && chunk != newChunk) {
				// We end the current chunk / category
				start = end;
				chunk = newChunk;
			}
		}
		return newProductWidth;
	}

	private int computeChunk(double gap, int chunk, int chunkSize, int resultIndex) {
		// Every chunkSize, we increment the chunk
		var newChunk = (resultIndex % (int) chunkSize) == 0;
		if (newChunk == true) {
			chunk++;
		}
		return chunk;
	}

	/**
	 * Compute the end of the current category, and set some important information
	 * about the category
	 * 
	 * @param start         The starting point of the category
	 * @param category      The current category
	 * @param categoryWidth The width of the category
	 * @return The end point of the category
	 */
	private Point computeEndCategory(Point start, Cell category, int categoryWidth) {
		var end = new Point(start.x + categoryWidth, start.y);
		var startingPoint = new Point((end.x + start.x) / 2, start.y + productHeight);
		var endingPoint = new Point(startingPoint.x, start.y - 2);
		category.setData(startingPoint, endingPoint, start.x, end.x);
		category.setStartingLinkPoint(startingPoint);
		category.setEndingLinkPoint(endingPoint);
		category.setStartPixel(start.x);
		category.setEndPixel(end.x);
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
	 * @param e The Paint Event
	 */
	private void drawLinks(GC gc) {
		for (int productIndex = 0; productIndex < products.size() - 1; productIndex++) {
			var categoryMap = categoriesMap.get(comparisonCriteria).get(productIndex);
			for (Cell category : categoryMap) {
				if (category.isLinkDrawable()) {
					var nextMap = categoriesMap.get(comparisonCriteria).get(productIndex + 1);
					// We search for a category that has the same process
					var optional = nextMap.stream().filter(next -> next.getResult().getContribution().item
							.equals(category.getResult().getContribution().item)).findFirst();
					if (optional.isPresent()) {
						var linkedCategory = optional.get();
						var startPoint = category.getStartingLinkPoint();
						var endPoint = linkedCategory.getEndingLinkPoint();
						if (config.useBezierCurve) {
							drawBezierCurve(gc, startPoint, endPoint, linkedCategory.getRgb());
						} else {
							drawLine(gc, startPoint, endPoint, linkedCategory.getRgb(), null);
						}
					}
				}
			}
		}
	}

	/**
	 * Draw a bezier curve, between 2 points
	 * 
	 * @param e     The Paint Event
	 * @param start The starting point
	 * @param end   The ending point
	 * @param alpha
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
				e.gc.drawImage(cacheMap.get(comparisonCriteria), origin.x, origin.y);
			}
		});
	}
}