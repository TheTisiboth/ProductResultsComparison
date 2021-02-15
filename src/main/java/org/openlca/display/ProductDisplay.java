package org.openlca.display;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;

public class ProductDisplay {
	private Shell shell;
	private ArrayList<Product> products;
	private Point screenSize;
	private Config config;
	final Point origin;
	final int maxProductResultsAmout;
	final int xMargin;
	final int productHeight;
	final int gapBetweenProduct;
	final int theoreticalScreenHeight;

	public ProductDisplay(Shell shell, final ArrayList<Product> products, Config config) {
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

		Image cache = new Image(Display.getCurrent(), 1904, theoreticalScreenHeight);
		GC gc = new GC(cache);
		paint(gc, composite);
		ImageData data = cache.getImageData();
		addPaintListener(composite, canvas, gc, cache);

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
				System.out.println("Scroll event");
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
				System.out.println("Resize event");
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

	private void paint(GC gc, Composite composite) {
		System.out.println("Paint");
		screenSize = composite.getSize(); // Responsive behavior
		double maxProductWidth = screenSize.x * 0.8; // 80% of the screen width
		Point rectEdge = new Point(origin.x + xMargin, origin.y + xMargin); // Start point of the first product
		// rectangle
		for (int productIndex = 0; productIndex < products.size(); productIndex++) {
			Product p = products.get(productIndex);
			final int productResultsAmount = p.getList().size();
			int productWidth = (int) ((productResultsAmount / maxProductResultsAmout) * maxProductWidth);
			int gap = (int) (productWidth / productResultsAmount);

			// Draw a rectangle for each product
			gc.drawRoundRectangle(rectEdge.x, rectEdge.y, (int) productWidth, productHeight, 30, 30);
			Point prevSubRectEdge = rectEdge; // Coordinate of each result rectangle
			for (int resultIndex = 0; resultIndex < productResultsAmount; resultIndex++) {
				Result result1 = p.getResult(resultIndex);
				prevSubRectEdge = handleResult(gc, productIndex, productResultsAmount, prevSubRectEdge, resultIndex,
						result1, gap, rectEdge);
			}
			rectEdge.y += 300;
		}
		drawLinks(gc);
	}

	private Point handleResult(GC gc, int productIndex, final int productResultsAmount, Point prevSubRectEdge,
			int resultIndex, Result result, int gap, Point rectEdge) {
		// Draw a separator line between the current result, and the next one
		Point sepStart = new Point(rectEdge.x + (resultIndex + 1) * (int) gap, rectEdge.y);
		Point sepEnd = new Point(sepStart.x, rectEdge.y + productHeight);
		if (resultIndex != productResultsAmount - 1) {
			gc.drawLine(sepStart.x, sepStart.y, sepEnd.x, sepEnd.y);
		}
		if (config.displayResultValue) {
			Point textPos = new Point(prevSubRectEdge.x + 10, (sepEnd.y + sepStart.y) / 2 - 10);
			gc.drawText(result.toString(), textPos.x, textPos.y);
		}
		result.setStartPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepEnd.y);
		result.setEndPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepStart.y);
		if (productIndex + 1 < products.size()) { // If there is a next product
			Product p2 = products.get(productIndex + 1);
			// We searh the first matching result
			Optional<Result> result2 = p2.getList().stream().filter(r2 -> result.equals(r2)).findFirst();
			if (result2.isPresent()) {
				result.setTargetProductResult(result2.get());
			}
		}
		return sepStart;
	}

	private void drawLinks(GC gc) {
		for (Product product : products) {
			for (Result result : product.getList()) {
				Point p1 = result.getStartPoint();
				Result result2 = result.getTargetProductResult();
				if (result2 != null) {
					Point p2 = result2.getEndPoint();
					drawBezierCurve(gc, p1, p2);
				}
			}
		}
	}

	/**
	 * Add a paint listener to the canvas
	 * 
	 * @param composite Parent composent of the canvas
	 * @param canvas    A Canvas component
	 */
	private void addPaintListener(Composite composite, Canvas canvas, GC gc, Image cache) {
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				screenSize = composite.getSize(); // Responsive behavior
				System.out.println(screenSize);
				ImageData data = cache.getImageData();
				gc.drawImage(cache, 0, origin.y, screenSize.x, screenSize.y, 0, 0, screenSize.x, screenSize.y);
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
	private void drawBezierCurve(GC gc, Point start, Point end) {
		Path p = new Path(gc.getDevice());
		p.moveTo(start.x, start.y);
		int offset = 100;
		Point ctrlPoint1 = new Point(start.x + offset, start.y + offset);
		Point ctrlPoint2 = new Point(end.x - offset, end.y - offset);
		p.cubicTo(ctrlPoint1.x, ctrlPoint1.y, ctrlPoint2.x, ctrlPoint2.y, end.x, end.y);
		gc.drawPath(p);
	}
}
