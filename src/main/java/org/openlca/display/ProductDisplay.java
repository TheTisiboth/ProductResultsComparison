package org.openlca.display;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
import org.openlca.core.database.IDatabase;
import org.openlca.core.model.Category;
import org.openlca.core.model.ProductSystem;
import org.openlca.core.model.descriptors.Descriptor;
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
	private AggregationCriteria aggregationCriteria;
	private Canvas canvas;
	private Map<AggregationCriteria, Image> cacheMap;
	private Combo selectCategory;
	private Color chosenColor;
	private ScrollBar vBar;
	private ContributionResult contributionResult;
	private long cutOff;
	private IDatabase db;
	private ProductSystem productSystem;
	private ImpactMethodDescriptor impactMethod;
	private String dbName;

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
		aggregationCriteria = config.aggregationCriteria;
		cacheMap = new HashMap<>();
		cutOff = 100;
	}

	/**
	 * Entry point of the program. Display the products, and draw links between each
	 * matching results
	 */
	void display() {
		System.out.println("Display start");
		Product.config = config;
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
		vBar.setMaximum(theoreticalScreenHeight);
		vBar.setMinimum(0);

		addScrollListener(canvas, vBar);
		addResizeEvent(row2, canvas, vBar);

		row1.setLayout(new RowLayout());

		redraw(row2, true);
		addPaintListener(canvas); // Once finished, we really paint the cache, so it avoids flickering
		createChoseImpactCategories(row1, row2);
		createAgregateCombo(row1);
		createColorPicker(row1);
		createSelectedCategory(row1, row2);

	}

	/**
	 * Dropdown menu, allow us to chose different Impact Categories
	 * 
	 * @param row1 The menu bar
	 * @param row2 The canvas
	 */
	private void createChoseImpactCategories(Composite row1, Composite row2) {
		MultipleSelectionCombo msc = new MultipleSelectionCombo(row1, SWT.BORDER);
		var impactCategoryMap = contributionResult.getImpacts().stream().map(impactCategory -> {
			msc.add(impactCategory.id + ": " + impactCategory.name);
			return impactCategory;
		}).collect(Collectors.toMap(impactCategory -> impactCategory.id + ": " + impactCategory.name,
				impactCategory -> impactCategory));
		msc.setSize(500, 500);
		msc.toggleAll();
		msc.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				var selections = msc.getSelections();
				products = new ArrayList<>();
				Arrays.stream(selections).forEach(impactName -> {
					var cs = contributionResult.getProcessContributions(impactCategoryMap.get(impactName));
					var p = new Product(cs, impactName);
					products.add(p);
				});

				theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * (products.size() + 1);
				sortProducts();
				triggerComboSelection(selectCategory, true);
				redraw(row2, true);
			}
		});

	}

	/**
	 * Dropdown menu, allow us to chose by what we want to agregate the contribution
	 * results
	 * 
	 * @param row1 The menu bar
	 */
	private void createAgregateCombo(Composite row1) {
		// TODO
		// Implement the behavior
		final Label l = new Label(row1, SWT.NONE);
		l.setText("Agregate : ");
		final Combo c = new Combo(row1, SWT.READ_ONLY);
		c.setBounds(50, 50, 150, 65);
		String values[] = { "", "Category", "Location" };
		c.setItems(values);
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
		var categoryMap = new HashMap<String, Descriptor>();
		selectCategory.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (selectCategory.getSelectionIndex() == -1) { // Nothing is selected : initialisation
					resetDefaultColorCells();
					var list = products.stream().flatMap(p -> p.getList().stream().flatMap(results -> results
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
	 * Sort products by ascending amount, according to the comparison criteria
	 */
	private void sortProducts() {
		Product.updateComparisonCriteria(aggregationCriteria);
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
			cacheMap.put(aggregationCriteria, cache);
			cachedPaint(composite, cache); // Costly painting, so we cache it; Called one time at the beginning
		} else {
			// Otherwise, we take a cached Image
			cache = cacheMap.get(aggregationCriteria);
			if (cache == null) {
				cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
				cacheMap.put(aggregationCriteria, cache);
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
	 * @param composite The parent component
	 * @param cache     The cached image in which we are drawing
	 */
	private void cachedPaint(Composite composite, Image cache) {
		GC gc = new GC(cache);
		screenSize = composite.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.8; // 80% of the screen width
		// Start point of the first product rectangle
		Point rectEdge = new Point(0 + xMargin, 0 + xMargin);
		Point textPos = new Point(5, 5);
		gc.drawText("Database : " + dbName, textPos.x, textPos.y);
		textPos.y += 30;
		gc.drawText("Product system : " + productSystem.name, textPos.x, textPos.y);
		textPos.y += 30;
		gc.drawText("Impact assessment method : " + impactMethod.name, textPos.x, textPos.y);
		var maxAmount = products.stream().mapToDouble(p -> p.getList().stream().map(c -> c.getNormalizedAmount())
				.reduce(0.0, (subtotal, amount) -> subtotal + amount)).max();
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			handleProduct(gc, maxProductWidth, rectEdge, productIndex, maxAmount.getAsDouble());
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
	private void handleProduct(GC gc, double maxProductWidth, Point rectEdge, int productIndex, double maxAmount) {
		var p = products.get(productIndex);
		int productWidth = (int) maxProductWidth;
		// Draw the product name
		Point textPos = new Point(rectEdge.x - xMargin, rectEdge.y + 8);
		gc.drawText("Contribution result " + productIndex, textPos.x, textPos.y);
		textPos.y += 25;
		gc.drawText("Impact : " + p.getName(), textPos.x, textPos.y);
		var totalAmount = p.getList().stream().mapToDouble(cell -> cell.getAmount()).sum();
		System.out.println("Product " + productIndex + " ; " + totalAmount + " total amount");
		productWidth = handleCells(gc, rectEdge, productIndex, p, productWidth, maxAmount);

		if (productIndex == 0) { // Draw an arrow to show the way the results are ordered
			Point startPoint = new Point(rectEdge.x - 20, rectEdge.y - 50);
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
	private int handleCells(GC gc, Point rectEdge, int productIndex, Product p, int productWidth, double maxAmount) {
		// TODO
		// Fix proportional size of cells
		var cells = p.getList();
		// Sum all the distincts values
		double normalizedTotalAmountSum = cells.stream().mapToDouble(cell -> Math.abs(cell.getNormalizedValue())).sum();
		productWidth = (int) (productWidth * (normalizedTotalAmountSum / maxAmount));
		long amountCutOff = cells.size() - cutOff;
		double normalizedTotalAMountSumNonCutOff = cells.stream().skip(amountCutOff)
				.mapToDouble(cell -> Math.abs(cell.getNormalizedValue())).sum();

		double cutoffRectangleSizeRatio = 1.0 / 4.0;
		double nonCutOffRecangleSizeRation = 1 - cutoffRectangleSizeRatio;
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

		Point start = null;
		var newChunk = 0;
		boolean isCutOff = true;
		RGB rgbCutOff = new RGB(192, 192, 192);
		for (var cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
			if (start == null) {
				start = new Point(rectEdge.x + 1, rectEdge.y + 1);
			}
			if (cellIndex >= amountCutOff) {
				if (isCutOff) {
					isCutOff = false;
					gc.setBackground(new Color(gc.getDevice(), rgbCutOff));
					gc.fillRectangle(rectEdge.x + 1, rectEdge.y + 1, (int) newProductWidth, productHeight - 1);
					gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
					chunk = -1;
					chunkSize = 0;
					gapEnoughBig = true;
					gap = (productWidth * nonCutOffRecangleSizeRation / (cells.size() - amountCutOff));
					if (gap < 1.0) {
						// If the gap is to small, we put a certain amount of results in the same
						// chunk
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
				var value = cell.getNormalizedValue();
				if (cellIndex >= amountCutOff) {
					var percentage = value / normalizedTotalAMountSumNonCutOff;
					cellWidth = (int) (productWidth * nonCutOffRecangleSizeRation * percentage);
				} else {
					var percentage = value / normalizedTotalAmountSum;
					cellWidth = (int) (productWidth * percentage);
				}
			}
			newProductWidth += cellWidth;
			if (cellIndex >= amountCutOff) {
				gc.setBackground(new Color(gc.getDevice(), cell.getRgb()));
				gc.fillRectangle(start.x, start.y, (int) cellWidth, productHeight - 1);
				gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
			}
			var end = computeEndCell(start, cell, (int) cellWidth);
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
	private Point computeEndCell(Point start, Cell cell, int cellWidth) {
		var end = new Point(start.x + cellWidth, start.y);
		var startingPoint = new Point((end.x + start.x) / 2, start.y + productHeight);
		var endingPoint = new Point(startingPoint.x, start.y - 2);
		cell.setData(startingPoint, endingPoint, start.x, end.x);
		cell.setStartingLinkPoint(startingPoint);
		cell.setEndingLinkPoint(endingPoint);
		cell.setStartPixel(start.x);
		cell.setEndPixel(end.x);
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
				e.gc.drawImage(cacheMap.get(aggregationCriteria), origin.x, origin.y);
			}
		});
	}
}