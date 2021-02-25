package org.openlca.display;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.openlca.util.Pair;

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
	private List<Map<RGB, Category>> categories;

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
		categories = new ArrayList<>();
		for (int i = 0; i < products.size(); i++) {
			categories.add(new HashMap<RGB, Category>());
		}
		sortProducts();
	}

	/**
	 * Display the products, and draw links between each matching results
	 */
	void display() {
		System.out.println("Display start");
		createCombo();
		/**
		 * Composite component
		 */
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
		ScrollBar vBar = canvas.getVerticalBar();
		vBar.setMaximum(theoreticalScreenHeight);
		vBar.setMinimum(0);

		addScrollListener(canvas, vBar);
		addResizeEvent(composite, canvas, vBar);

		cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
		cachedPaint(); //
		addPaintListener(composite, canvas); // Once finished, we really paint the cache
	}

	/**
	 * Create a combo component, in order to change the comparison criteria
	 */
	private void createCombo() {
		final Combo c = new Combo(shell, SWT.READ_ONLY);
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
					for (int i = 0; i < categories.size(); i++) {
						categories.set(i, new HashMap<>());
					}
					sortProducts();
					redraw();
				}
			}
		});
	}

	private void sortProducts() {
		// Sort by descending amount
		Product.updateComparisonCriteria(comparisonCriteria);
		products.stream().forEach(p -> p.sort());
		maxCriteriaValue = products.stream().mapToDouble(p -> p.max()).max().getAsDouble();
		minCriteriaValue = products.stream().mapToDouble(p -> p.min()).min().getAsDouble();
		System.out.println();
	}

	/**
	 * Allow to redraw everything
	 */
	private void redraw() {
		screenSize = composite.getSize();
		// Cached image, in which we draw the things, and then display it once it is
		// finished
		cache = new Image(Display.getCurrent(), screenSize.x, theoreticalScreenHeight);
		cachedPaint(); // Costly painting, so we cache it; Called one time at the beginning
		canvas.redraw();
	}

	/**
	 * Costly painting method. For each product, it draws links between each
	 * matching results. Since it is costly, it is firstly drawed in an image. Once
	 * it is finished, we paint the image
	 * 
	 * @param gc        The GC component
	 * @param composite The parent component
	 */
	private void cachedPaint() {
		GC gc = new GC(cache);
		screenSize = composite.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.8; // 80% of the screen width
		// Start point of the first product rectangle
		Point rectEdge = new Point(origin.x + xMargin, origin.y + xMargin);
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			handleProduct(gc, maxProductWidth, rectEdge, productIndex);
			rectEdge.y += 300;
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
	 */
	private void handleProduct(GC gc, double maxProductWidth, Point rectEdge, int productIndex) {
		var p = products.get(productIndex);
		final int productResultsAmount = p.getList().size();
//		int productWidth = (int) (((double) productResultsAmount / (double) maxProductResultsAmout) * maxProductWidth);
		int productWidth = (int) maxProductWidth;
		// It is the gap between two results
		double gap = ((double) productWidth / productResultsAmount);
		int chunk = -1, chunkSize = 0;
		boolean drawSeparation = true;
		if (gap < 3.0) {
			// If the gap is to small, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(1 / gap);
			drawSeparation = false;
			productWidth = (int) (productResultsAmount / chunkSize);
		} else {
			productWidth = (int) gap * productResultsAmount;
		}
		p.setDrawSeparationBetweenResults(drawSeparation);
		// Draw a rectangle for each product
		gc.drawRectangle(rectEdge.x, rectEdge.y, (int) productWidth, productHeight);
		// Draw the product name
		Point textPos = new Point(rectEdge.x - xMargin, rectEdge.y + 8);
		gc.drawText(p.getName(), textPos.x, textPos.y);
		Point prevSubRectEdge = new Point(rectEdge.x, rectEdge.y + 1); // Coordinate of each result rectangle
		Category previousCategory = null;
		var pair = new Pair<>(prevSubRectEdge, previousCategory);
		for (int resultIndex = 0; resultIndex < productResultsAmount; resultIndex++) {
			chunk = computeChunk(gap, chunk, chunkSize, drawSeparation, resultIndex);
			var result = p.getResult(resultIndex);
			pair = handleResult(gc, productIndex, productResultsAmount, pair.first, resultIndex, result, chunk,
					rectEdge, drawSeparation, pair.second);
		}
	}

	/**
	 * Compute the new chunk value, which will indicate the position of the current
	 * result
	 * 
	 * @param gap            The gap between 2 results
	 * @param chunk          The value of the current chunk
	 * @param chunkSize      The size of the chunks
	 * @param drawSeparation Indicate if we have to draw a separation line between
	 *                       the results
	 * @param resultIndex    The index result
	 * @return The new chunk value
	 */
	private int computeChunk(double gap, int chunk, int chunkSize, boolean drawSeparation, int resultIndex) {
		if (!drawSeparation) {
			// Every chunkSize, we increment the chunk
			var newChunk = (resultIndex % (int) chunkSize) == 0;
			if (newChunk == true) {
				chunk++;
			}
		} else {
			chunk = (int) gap * (resultIndex + 1);
		}
		return chunk;
	}

	/**
	 * Handle the current result. Draw a separation line between it and the next
	 * result, display its value, and find a matching result in the next product
	 * 
	 * @param e                    The Paint Event
	 * @param productIndex         Index of the current product
	 * @param productResultsAmount Amount of results for the current product
	 * @param prevSubRectEdge      Separation with the previous result
	 * @param resultIndex          The index result
	 * @param result               THe current result
	 * @param gap                  Gap between 2 products
	 * @param rectEdge             Position of product rectangle
	 * @param drawSeparation
	 * @param previousRGB
	 * @return
	 */
	private Pair<Point, Category> handleResult(GC gc, int productIndex, final int productResultsAmount,
			Point prevSubRectEdge, int resultIndex, Result result, int gap, Point rectEdge, boolean drawSeparation,
			Category previousCategory) {
		// Draw a separator line between the current result, and the next one
		Point sepStart = new Point(rectEdge.x + gap, rectEdge.y + 1);
		Point sepEnd = new Point(sepStart.x, rectEdge.y + productHeight - 1);
		boolean contributionEmpty = result.isContributionEmpty();
		RGB rgb = null;
		rgb = result.getRGB(minCriteriaValue, maxCriteriaValue);
		if (drawSeparation) {
			drawLine(gc, sepStart, sepEnd, null, null);
			gc.setBackground(new Color(gc.getDevice(), rgb));
			gc.fillRectangle(prevSubRectEdge.x + 1, prevSubRectEdge.y, sepStart.x - prevSubRectEdge.x - 1,
					productHeight - 1);
			gc.setBackground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
		} else if (resultIndex != productResultsAmount - 1) {
			drawLine(gc, sepStart, sepEnd, rgb, SWT.COLOR_BLACK);
		}
		if (!contributionEmpty) {
			if (previousCategory == null) {
				previousCategory = new Category(resultIndex, rgb, products.get(productIndex));
				previousCategory.setStartSeparation(sepStart, sepEnd);
			} else {
				if (!previousCategory.getRgb().equals(rgb)) {
					// We are in a new category
					previousCategory.setEndIndex(resultIndex - 1);
					previousCategory.setEndSeparation(sepStart, sepEnd);
					categories.get(productIndex).put(previousCategory.getRgb(), previousCategory);
					Category newCategory = new Category(resultIndex, rgb, products.get(productIndex));
					newCategory.setStartSeparation(sepStart, sepEnd);
					previousCategory = newCategory;
				}
				if (resultIndex == products.get(productIndex).getEffectiveSize() - 1) {
					previousCategory.setEndIndex(resultIndex);
					previousCategory.setEndSeparation(sepStart, sepEnd);
					categories.get(productIndex).put(previousCategory.getRgb(), previousCategory);
				}
			}
		}
		if (config.displayResultValue) {
			Point textPos = new Point(prevSubRectEdge.x + 10, (sepEnd.y + sepStart.y) / 2 - 10);
			gc.drawText(result.toString(), textPos.x, textPos.y);
		}
		result.setStartPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepEnd.y + 2);
		result.setEndPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepStart.y - 2);
		return new Pair<Point, Category>(sepStart, previousCategory);
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
		for (int productIndex = 0; productIndex < categories.size() - 1; productIndex++) {
			var map = categories.get(productIndex);
			for (Map.Entry<RGB, Category> entry : map.entrySet()) {
				var startCategory = entry.getValue();
				if (!products.get(productIndex).getDrawSeparationBetweenResults()) {
					if (startCategory.isSeparationDrawable()) {
						var sepEnd = startCategory.getEndSeparation();
						drawLine(gc, sepEnd.first, sepEnd.second, SWT.COLOR_WHITE, SWT.COLOR_BLACK);
					}
				}
				var nextMap = categories.get(productIndex + 1);
				var linkedCategory = nextMap.get(entry.getKey());
				if (linkedCategory != null) {
					var startPoint = startCategory.getTargetResult().getStartPoint();
					var endPoint = linkedCategory.getTargetResult().getEndPoint();
					drawBezierCurve(gc, startPoint, endPoint, linkedCategory.getRgb());
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
				redraw();
			}
		});
	}

	/**
	 * Add a paint listener to the canvas
	 * 
	 * @param composite Parent composent of the canvas
	 * @param canvas    A Canvas component
	 */
	private void addPaintListener(Composite composite, Canvas canvas) {
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				screenSize = composite.getSize(); // Responsive behavior
				e.gc.drawImage(cache, origin.x, origin.y);
				System.out.println(1);
			}
		});
	}
}
