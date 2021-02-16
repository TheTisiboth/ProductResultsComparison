package org.openlca.display;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

public class ProductDisplay<CategorizedDescriptor> {
	private Shell shell;
	private List<Product<CategorizedDescriptor>> products;
	private Point screenSize;
	private Config config;
	final Point origin;
	final int maxProductResultsAmout;
	final int xMargin;
	final int productHeight;
	final int gapBetweenProduct;
	final int theoreticalScreenHeight;

	public ProductDisplay(Shell shell, final List<Product<CategorizedDescriptor>> products, Config config) {
		this.shell = shell;
		this.products = products;
		this.config = config;
		screenSize = shell.getSize();
		origin = new Point(0, 0);
		maxProductResultsAmout = products.stream().mapToInt(p -> p.getList().size()).max().getAsInt();
		xMargin = 100;
		productHeight = 30;
		gapBetweenProduct = 300;
		theoreticalScreenHeight = xMargin + (productHeight + gapBetweenProduct) * (products.size() - 1);
	}

	/**
	 * Display the products, and draw links between each matching results
	 */
	void display() {
		System.out.println("Display start");
		/**
		 * Composite component
		 */
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));

		/**
		 * Canvas component
		 */
		Canvas canvas = new Canvas(composite, SWT.V_SCROLL);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		/**
		 * VBar component
		 */
		ScrollBar vBar = canvas.getVerticalBar();
		vBar.setMaximum(theoreticalScreenHeight);
		vBar.setMinimum(0);

		addScrollListener(canvas, vBar);
		addResizeEvent(composite, canvas, vBar);
		addPaintListener(composite, canvas);
//		canvas.redraw();
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
				double maxProductWidth = screenSize.x * 0.8; // 80% of the screen width
				Point rectEdge = new Point(origin.x + xMargin, origin.y + xMargin); // Start point of the first product
				// rectangle
				for (int productIndex = 0; productIndex < products.size(); productIndex++) {
					handleProduct(e, maxProductWidth, rectEdge, productIndex);
					rectEdge.y += 300;
				}
				drawLinks(e);
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
			private void handleProduct(PaintEvent e, double maxProductWidth, Point rectEdge, int productIndex) {
				var p = products.get(productIndex);
				final int productResultsAmount = p.getList().size();
				int productWidth = (int) (((double) productResultsAmount / (double) maxProductResultsAmout)
						* maxProductWidth);
				// It is the gap between two results
				double gap = ((double) productWidth / productResultsAmount);
				int chunk = -1, chunkSize = 0;
				boolean drawSeparation = true;
				if (gap < 1.0) {
					// If the gap is to small, we put a certain amount of results in the same
					// chunk
					chunkSize = (int) Math.ceil(1 / gap);
					drawSeparation = false;
				}
				// Draw a rectangle for each product
				e.gc.drawRoundRectangle(rectEdge.x, rectEdge.y, (int) productWidth, productHeight, 30, 30);
				// Draw the product name
				Point textPos = new Point(rectEdge.x + 11, rectEdge.y + 8);
				e.gc.drawText(p.getName(), textPos.x, textPos.y);
				Point prevSubRectEdge = rectEdge; // Coordinate of each result rectangle
				for (int resultIndex = 0; resultIndex < productResultsAmount; resultIndex++) {
					chunk = computeChunk(gap, chunk, chunkSize, drawSeparation, resultIndex);
					var result1 = p.getResult(resultIndex);
					prevSubRectEdge = handleResult(e, productIndex, productResultsAmount, prevSubRectEdge, resultIndex,
							result1, chunk, rectEdge, drawSeparation);
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
					chunk = (int) gap * resultIndex + 1;
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
			private Point handleResult(PaintEvent e, int productIndex, final int productResultsAmount,
					Point prevSubRectEdge, int resultIndex, Result<CategorizedDescriptor> result, int gap,
					Point rectEdge, boolean drawSeparation) {
				// Draw a separator line between the current result, and the next one
				Point sepStart = new Point(rectEdge.x + gap, rectEdge.y);
				Point sepEnd = new Point(sepStart.x, rectEdge.y + productHeight);
				if (drawSeparation && resultIndex != productResultsAmount - 1) {
					e.gc.drawLine(sepStart.x, sepStart.y, sepEnd.x, sepEnd.y);
				}
				if (config.displayResultValue) {
					Point textPos = new Point(prevSubRectEdge.x + 10, (sepEnd.y + sepStart.y) / 2 - 10);
					e.gc.drawText(result.toString(), textPos.x, textPos.y);
				}
				result.setStartPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepEnd.y);
				result.setEndPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepStart.y);
				if (productIndex + 1 < products.size()) { // If there is a next product
					var p2 = products.get(productIndex + 1);
					// We search the first matching result
					var result2 = p2.getList().stream().filter(r2 -> result.equals(r2)).findFirst();
					if (result2.isPresent()) {
						result.setTargetProductResult(result2.get());
					}
				}
				return sepStart;
			}

			/**
			 * Draw the links between each matching results
			 * 
			 * @param e The Paint Event
			 */
			private void drawLinks(PaintEvent e) {
				for (var product : products) {
					for (var result : product.getList()) {
						Point p1 = result.getStartPoint();
						var result2 = result.getTargetProductResult();
						if (result2 != null) {
							Point p2 = result2.getEndPoint();
							drawBezierCurve(e, p1, p2);
						}
					}
				}
			}
		});
	}

	/**
	 * Draw a bezier curve, between 2 points
	 * 
	 * @param e     The Paint Event
	 * @param start The starting point
	 * @param end   The ending point
	 */
	private void drawBezierCurve(PaintEvent e, Point start, Point end) {
		Path p = new Path(e.gc.getDevice());
		p.moveTo(start.x, start.y);
		int offset = 100;
		Point ctrlPoint1 = new Point(start.x + offset, start.y + offset);
		Point ctrlPoint2 = new Point(end.x - offset, end.y - offset);
		p.cubicTo(ctrlPoint1.x, ctrlPoint1.y, ctrlPoint2.x, ctrlPoint2.y, end.x, end.y);
		e.gc.drawPath(p);
	}
}
