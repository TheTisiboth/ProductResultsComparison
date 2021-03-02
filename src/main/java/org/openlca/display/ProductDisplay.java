package org.openlca.display;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public class ProductDisplay {
	private Shell shell;
	private List<Product> products;
	private Point screenSize;
	private Config config;
	private Point origin;
	private final int maxProductResultsAmout;
	private final int xMargin;
	private final int productHeight;
	private final int gapBetweenProduct;
	private final int theoreticalScreenHeight;
	private ComparisonCriteria comparisonCriteria;
	private Canvas canvas;
	private Image cache;
	private Composite composite;
	private double maxCriteriaValue;
	private double minCriteriaValue;
	private List<List<Category>> categoriesList;
	private Combo categoriesValuesSelection;
	private Color chosenColor;

	public ProductDisplay(Shell shell, final List<Product> products, Config config) {
		this.shell = shell;
		this.products = products;
		this.config = config;
		screenSize = shell.getSize();
		origin = new Point(0, 0);
		maxProductResultsAmout = products.stream().mapToInt(p -> p.getList().size()).max().getAsInt();
		xMargin = 200;
		productHeight = 30;
		gapBetweenProduct = 300;
		theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * (products.size() - 1);
		canvas = null;
		composite = null;
		maxCriteriaValue = 0;
		minCriteriaValue = 0;
		comparisonCriteria = config.comparisonCriteria;
		categoriesList = new ArrayList<>();
		sortProducts();
	}

	/**
	 * Display the products, and draw links between each matching results
	 */
	void display() {
		System.out.println("Display start");

		/**
		 * Composite component
		 */
		var composite1 = new Composite(shell, SWT.NONE);
		composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));

		/**
		 * Canvas component
		 */
		canvas = new Canvas(composite, SWT.V_SCROLL);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/**
		 * VBar component
		 */
		var vBar = canvas.getVerticalBar();
		vBar.setMaximum(theoreticalScreenHeight);
		vBar.setMinimum(0);

		addScrollListener(canvas, vBar);
		addResizeEvent(composite, canvas, vBar);

		cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
		cachedPaint(true); // Costly painting method, so we cache it first

		composite1.setLayout(new RowLayout());
		createComparisonCriteriaCombo(composite1);
		createSelectValueCombo(composite1);
		createColorPicker(composite1);
		addPaintListener(canvas); // Once finished, we really paint the cache, so it avoids flickering
	}

	private void createColorPicker(Composite composite) {
		// Start with Celtics green
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
					triggerSelectValueComboSelectionListener(false);
				}
			}
		});
	}

	/**
	 * Create a combo component, in order to change the comparison criteria
	 */
	private void createComparisonCriteriaCombo(Composite composite) {
		final Combo c = new Combo(composite, SWT.READ_ONLY);
		c.setBounds(50, 50, 150, 65);
		var criterias = ComparisonCriteria.valuesToString();
		var indexSelectedCriteria = ArrayUtils.indexOf(criterias, config.comparisonCriteria.toString());
		c.setItems(criterias);
		c.select(indexSelectedCriteria);
		c.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ComparisonCriteria newCriteria = ComparisonCriteria.getCriteria(c.getText());
				if (newCriteria != comparisonCriteria) {
					comparisonCriteria = ComparisonCriteria.getCriteria(c.getText());
					origin = new Point(0, 0);
					// We reset the categories
					categoriesList = new ArrayList<>();
					sortProducts();
					redraw(true);
					triggerSelectValueComboSelectionListener(true);
				}
			}
		});
	}

	private void triggerSelectValueComboSelectionListener(boolean deselect) {
		if (deselect) {
			categoriesValuesSelection.deselectAll();
		}
		Event event = new Event();
		event.widget = categoriesValuesSelection;
		event.display = categoriesValuesSelection.getDisplay();
		event.type = SWT.Selection;
		categoriesValuesSelection.notifyListeners(SWT.Selection, event);
	}

	/**
	 * Create a combo component, in order to change the comparison criteria
	 */
	private void createSelectValueCombo(Composite composite) {
		categoriesValuesSelection = new Combo(composite, SWT.READ_ONLY);
		categoriesValuesSelection.setBounds(30, 50, 150, 65);
		// Get the whole dinstinct categories values
		String[] categoriesValues = categoriesList.stream()
				.flatMap(categories -> categories.stream().map(entry -> entry.getTargetValue() + "")).distinct()
				.sorted().collect(Collectors.toList()).toArray(String[]::new);
		categoriesValuesSelection.setItems(categoriesValues);
		categoriesValuesSelection.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (categoriesValuesSelection.getText().equals("")) {
					// Get the whole dinstinct categories values
					String[] categoriesValues = categoriesList.stream()
							.flatMap(categories -> categories.stream().map(entry -> entry.getTargetValue() + ""))
							.distinct().sorted().collect(Collectors.toList()).toArray(String[]::new);
					categoriesValuesSelection.setItems(categoriesValues);
				} else {
					double selectedValue = Double.valueOf(categoriesValuesSelection.getText()).doubleValue();
					RGB rgb = chosenColor.getRGB();
					// Reset categories colors to default
					categoriesList.stream().forEach(categories -> categories.stream()
							.filter(entry -> entry.getRgb().equals(rgb)).forEach(entry -> entry.resetDefaultRGB()));
					// Set a black color the the chosen categories
					categoriesList.stream()
							.forEach(categories -> categories.stream()
									.filter(entry -> selectedValue == entry.getTargetValue())
									.forEach(entry -> entry.setRgb(rgb)));
					redraw(false);
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

	private void redraw(boolean recompute) {
		screenSize = composite.getSize();
		// Cached image, in which we draw the things, and then display it once it is
		// finished
		cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
		cachedPaint(recompute); // Costly painting, so we cache it; Called one time at the beginning
		canvas.redraw();
	}

	/**
	 * Costly painting method. For each product, it draws links between each
	 * matching results. Since it is costly, it is firstly drawed in an image. Once
	 * it is finished, we paint the image
	 * 
	 * @param recompute
	 * 
	 * @param gc        The GC component
	 * @param composite The parent component
	 */
	private void cachedPaint(boolean recompute) {
		GC gc = new GC(cache);
		if (recompute) {
			categoriesList = new ArrayList<>();
		}
		screenSize = composite.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.8; // 80% of the screen width
		// Start point of the first product rectangle
		Point rectEdge = new Point(0 + xMargin, 0 + xMargin);
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			handleProduct(gc, maxProductWidth, rectEdge, productIndex, recompute);
			rectEdge = new Point(rectEdge.x, rectEdge.y + 300);
		}
		drawLinks(gc);
	}

	/**
	 * Handle the current product. Draw a rectangle, write the product name in it,
	 * and handle the product results
	 * 
	 * @param e               The Paint event
	 * @param maxProductWidth The maximal width for a product
	 * @param rectEdge        The coordinate of the product rectangle
	 * @param productIndex    The index of the current product
	 * @param recompute
	 */
	private void handleProduct(GC gc, double maxProductWidth, Point rectEdge, int productIndex, boolean recompute) {
		var p = products.get(productIndex);
		int productWidth = (int) maxProductWidth;

		p.setBounds(rectEdge, productWidth);
		// Draw a rectangle for each product
		gc.drawRectangle(rectEdge.x, rectEdge.y, productWidth, productHeight);
		// Draw the product name
		Point textPos = new Point(rectEdge.x - xMargin, rectEdge.y + 8);
		gc.drawText(p.getName(), textPos.x, textPos.y);
		handleCategories(gc, rectEdge, productIndex, p, productWidth, recompute);
	}

	@SuppressWarnings("unchecked")
	private void handleCategories(GC gc, Point rectEdge, int productIndex, Product p, int productWidth,
			boolean recompute) {
		// Get all distinct values, create a category for each of them, and insert it
		// into an LinkedHashMap
		List<Category> categoryMap = null;
		if (recompute) {
			categoryMap = p.getList().stream().map(r -> r.getValue()).distinct()
					.map(v -> new Category(v, config, minCriteriaValue, maxCriteriaValue)).collect(Collectors.toList());
			// I also tried to replace LinkedHashMap::new with
			// () -> Collections.synchronizedMap(new LinkedHashMap());
		} else {
			categoryMap = categoriesList.get(productIndex);
		}
//		System.out.println("Product " + productIndex + " : " + categoryMap.size() + " categories");
		// Sum all the distincts values
		double resultsSum = categoryMap.stream().mapToDouble(entry -> Math.abs(entry.getNormalizedValue())).sum();
		Point start = null;
		for (Category category : categoryMap) {
			if (start == null) {
				start = new Point(rectEdge.x + 1, rectEdge.y + 1);
			}
			var value = category.getNormalizedValue();
			int categoryWidth = 0;
			if (value == 0.0) {
				categoryWidth = (int) (productWidth - start.x);
			} else {
				var percentage = value / resultsSum;
				categoryWidth = (int) (productWidth * percentage);
			}
			gc.setBackground(new Color(gc.getDevice(), category.getRgb()));
			gc.fillRectangle(start.x, start.y, categoryWidth, productHeight - 1);
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			var end = new Point(start.x + categoryWidth, start.y);
			var startingPoint = new Point((end.x + start.x) / 2, start.y + productHeight);
			var endingPoint = new Point(startingPoint.x, start.y - 2);
			category.setTargetStartingPoint(startingPoint);
			category.setTargetEndingPoint(endingPoint);
			category.setStartPixel(start.x);
			category.setEndPixel(end.x);
			start = end;
//			drawLine(gc, start, new Point(start.x, start.y + productHeight - 2), SWT.COLOR_WHITE, SWT.COLOR_BLACK);
//			System.out.println(category);

		}
		if (recompute) {
			categoriesList.add(categoryMap);
		}
	}

	/**
	 * Draw a line, with an optional colour
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
			var categoryMap = categoriesList.get(productIndex);
			for (Category category : categoryMap) {
				if (category.isLinkDrawable()) {
					var nextMap = categoriesList.get(productIndex + 1);
					var o = nextMap.stream().filter(next -> next.getRgb().equals(category.getRgb())).findFirst();
					if (o.isPresent()) {
						var linkedCategory = o.get();

						if (linkedCategory != null) {
							var startPoint = category.getTargetStartingPoint();
							var endPoint = linkedCategory.getTargetEndingPoint();
							drawLine(gc, startPoint, endPoint, linkedCategory.getRgb(), SWT.COLOR_BLACK);
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
				redraw(true);
			}
		});
	}

	/**
	 * Add a paint listener to the canvas. This is called whenever the canvas needs
	 * to be redrawed, then it draws the cached image
	 * 
	 * @param composite Parent component of the canvas
	 * @param canvas    A Canvas component
	 */
	private void addPaintListener(Canvas canvas) {
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				e.gc.drawImage(cache, origin.x, origin.y);
			}
		});
	}
}
