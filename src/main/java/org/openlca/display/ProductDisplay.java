package org.openlca.display;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
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
import org.openlca.core.model.descriptors.ProcessDescriptor;

public class ProductDisplay {
	private Shell shell;
	private List<Product> products;
	private Point screenSize;
	private Config config;
	private final Point origin;
	private final int maxProductResultsAmout;
	private final int xMargin;
	private final int productHeight;
	private final int gapBetweenProduct;
	private final int theoreticalScreenHeight;
	private ComparisonCriteria comparisonCriteria;
	private Canvas canvas;
	private Image cache;
	private Composite composite;
	private double maxAmount;

	public ProductDisplay(Shell shell, final List<Product> products, Config config) {
		this.shell = shell;
		this.products = products;
		this.config = config;
		screenSize = shell.getSize();
		origin = new Point(0, 0);
		maxProductResultsAmout = products.stream().mapToInt(p -> p.getList().size()).max().getAsInt();
		xMargin = 100;
		productHeight = 30;
		gapBetweenProduct = 300;
		theoreticalScreenHeight = xMargin * 2 + (productHeight + gapBetweenProduct) * (products.size() - 1);
		canvas = null;
		composite = null;
		maxAmount = 1;
		comparisonCriteria = config.comparisonCriteria;
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
				comparisonCriteria = ComparisonCriteria.getCriteria(c.getText());
				// We reset the target product results
				products.stream().forEach(p -> p.resetTargetProductResult());
				sortProducts();
				redraw();
			}
		});
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
		Point rectEdge = new Point(origin.x + xMargin, origin.y + xMargin); // Start point of the first product
		// rectangle
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
		int productWidth = (int) (((double) productResultsAmount / (double) maxProductResultsAmout) * maxProductWidth);
		// It is the gap between two results
		double gap = ((double) productWidth / productResultsAmount);
		int chunk = -1, chunkSize = 0;
		boolean drawSeparation = true;
		if (gap < 1.0) {
			// If the gap is to small, we put a certain amount of results in the same
			// chunk
			chunkSize = (int) Math.ceil(1 / gap);
			drawSeparation = false;
			productWidth = (int) (productResultsAmount / chunkSize);
		} else {
			productWidth = (int) gap * productResultsAmount;
		}
		// Draw a rectangle for each product
		gc.drawRectangle(rectEdge.x, rectEdge.y, (int) productWidth, productHeight);
		// Draw the product name
		Point textPos = new Point(rectEdge.x + 11, rectEdge.y + 8);
		gc.drawText(p.getName(), textPos.x, textPos.y);
		Point prevSubRectEdge = rectEdge; // Coordinate of each result rectangle
		for (int resultIndex = 0; resultIndex < productResultsAmount; resultIndex++) {
			chunk = computeChunk(gap, chunk, chunkSize, drawSeparation, resultIndex);
			var result = p.getResult(resultIndex);
			prevSubRectEdge = handleResult(gc, productIndex, productResultsAmount, prevSubRectEdge, resultIndex, result,
					chunk, rectEdge, drawSeparation);
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
	 * @return
	 */
	private Point handleResult(GC gc, int productIndex, final int productResultsAmount, Point prevSubRectEdge,
			int resultIndex, Result result, int gap, Point rectEdge, boolean drawSeparation) {
		// Draw a separator line between the current result, and the next one
		Point sepStart = new Point(rectEdge.x + gap, rectEdge.y);
		Point sepEnd = new Point(sepStart.x, rectEdge.y + productHeight);
		if (drawSeparation && resultIndex != productResultsAmount - 1) {
			gc.drawLine(sepStart.x, sepStart.y, sepEnd.x, sepEnd.y);
		} else if (!drawSeparation && resultIndex != productResultsAmount - 1) {
			boolean contributionEmpty = false;
			switch (comparisonCriteria) {
			case AMOUNT:
				contributionEmpty = result.getContribution().amount == 0.0;
				break;
			case CATEGORY:
				contributionEmpty = result.getContribution().item.category == null;
				break;
			case LOCATION:
				contributionEmpty = ((ProcessDescriptor) result.getContribution().item).location == null;
				break;
			}
			if (contributionEmpty) {
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
				gc.drawLine(sepStart.x, sepStart.y, sepEnd.x, sepEnd.y);
				gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_BLACK));
			}
		}

		if (config.displayResultValue) {
			Point textPos = new Point(prevSubRectEdge.x + 10, (sepEnd.y + sepStart.y) / 2 - 10);
			gc.drawText(result.toString(), textPos.x, textPos.y);
		}
		result.setStartPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepEnd.y);
		result.setEndPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepStart.y);
		if (comparisonCriteria.equals(ComparisonCriteria.AMOUNT) && result.getContribution().amount != 0.0) {
			if (productIndex + 1 < products.size()) { // If there is a next product
				var p2 = products.get(productIndex + 1);
				// We search the first matching result
				var result2 = p2.getList().stream().filter(r2 -> result.equals(r2, comparisonCriteria)).findFirst();
				if (result2.isPresent()) {
					result.setTargetProductResult(result2.get());
				}
			}
		}
		return sepStart;
	}

	/**
	 * Draw the links between each matching results
	 * 
	 * @param e The Paint Event
	 */
	private void drawLinks(GC gc) {
		for (var product : products) {
			for (var result : product.getList()) {
				int normalizedAmount = (int) (result.getContribution().amount / maxAmount * 255);
				Point p1 = result.getStartPoint();
				var result2 = result.getTargetProductResult();
				if (result2 != null) {
					Point p2 = result2.getEndPoint();
					drawBezierCurve(gc, p1, p2, normalizedAmount);
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
	private void drawBezierCurve(GC gc, Point start, Point end, int alpha) {
		Path p = new Path(gc.getDevice());
		p.moveTo(start.x, start.y);
		int offset = 100;
		Point ctrlPoint1 = new Point(start.x + offset, start.y + offset);
		Point ctrlPoint2 = new Point(end.x - offset, end.y - offset);
		p.cubicTo(ctrlPoint1.x, ctrlPoint1.y, ctrlPoint2.x, ctrlPoint2.y, end.x, end.y);
		gc.drawPath(p);
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
			}
		});
	}

	private void sortProducts() {
		// Sort by descending amount
		products.stream().forEach(p -> p.sort(comparisonCriteria));
		maxAmount = products.stream().mapToDouble(p -> p.getList().get(0).getContribution().amount).max().getAsDouble();
	}
}
