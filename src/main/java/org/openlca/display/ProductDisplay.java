package org.openlca.display;

import java.util.ArrayList;
import java.util.Optional;

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

public class ProductDisplay {
	private Shell shell;
	private ArrayList<Product> products;
	private Point screenSize;
	private Config config;
	final Point origin;

	public ProductDisplay(Shell shell, final ArrayList<Product> products, Config config) {
		this.shell = shell;
		this.products = products;
		screenSize = shell.getSize();
		this.config = config;
		origin = new Point(0, 0);
	}

	/**
	 * Display the products, and draw links between each matching results
	 */
	void display() {
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(new GridLayout(1, false));

		Canvas canvas = new Canvas(composite, SWT.V_SCROLL);
		canvas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		System.out.println(3);
		final int maxProductResultsAmout = products.stream().mapToInt(p -> p.getList().size()).max().getAsInt();
		final ScrollBar vBar = canvas.getVerticalBar();
		
		int theoreticalScreenHeight = 100+330*(products.size()-1);
		vBar.setMaximum(theoreticalScreenHeight);
		vBar.setMinimum(0);
		vBar.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event event) {
				System.out.println("Scroll event");
				int vSelection = vBar.getSelection();
				int destY = -vSelection - origin.y;
				canvas.scroll(0, destY, 0, 0, canvas.getSize().x, canvas.getSize().y, true);
				origin.y = -vSelection;
//                canvas.redraw(0, origin.y,2120, 2000, false);
			}

		});
		canvas.addListener(SWT.Resize, new Listener() {
			@Override
			public void handleEvent(Event e) {
				System.out.println("Resize event");

				Rectangle client = canvas.getClientArea();
//				vBar.setMaximum(2000);

				vBar.setThumb(Math.min(composite.getSize().y, client.height));
				vBar.setPageIncrement(Math.min(composite.getSize().y, client.height));

				vBar.setIncrement(20);
				int vPage = composite.getSize().y - client.height;
				int vSelection = vBar.getSelection();

				if (vSelection >= vPage) {
					if (vPage <= 0)
						vSelection = 0;
					origin.y = -vSelection;
				}
				System.out.println( "origin.x " + origin.x + " origin.y " + origin.y);
//				canvas.redraw(0, origin.y, 2120, 2000, false);
			}
		});
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				System.out.println("Paint event");
				screenSize = composite.getSize(); // Responsive behavior
				System.out.println(screenSize);
				Point rectEdge = new Point(origin.x + 100, origin.y + 100); // Start point of the first product
																			// rectangle
				double width = screenSize.x * 0.8;
				int height = 30;
				for (int productIndex = 0; productIndex < products.size(); productIndex++) {
					Product p = products.get(productIndex);
					final int productResultsAmount = products.get(0).getList().size();
					width = (productResultsAmount / maxProductResultsAmout) * width;
					double gap = width / productResultsAmount;

					// Draw a rectangle for each product
					e.gc.drawRoundRectangle(rectEdge.x, rectEdge.y, (int) width, height, 30, 30);
					Point prevSubRectEdge = rectEdge; // Coordinate of each result rectangle
					for (int resultIndex = 0; resultIndex < productResultsAmount; resultIndex++) {
						Result result1 = p.getResult(resultIndex);
						Point sepStart = new Point(rectEdge.x + (resultIndex + 1) * (int) gap, rectEdge.y);
						Point sepEnd = new Point(sepStart.x, rectEdge.y + height);
						// Draw a separator line between the current result, and the next one
						if (resultIndex != productResultsAmount - 1) {
							e.gc.drawLine(sepStart.x, sepStart.y, sepEnd.x, sepEnd.y);
						}
						if (config.displayResultValue) {
							Point textPos = new Point(prevSubRectEdge.x + 10, (sepEnd.y + sepStart.y) / 2 - 10);
							e.gc.drawText(result1.toString(), textPos.x, textPos.y);
						}
						result1.setStartPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepEnd.y);
						result1.setEndPoint((prevSubRectEdge.x + sepEnd.x) / 2, sepStart.y);
						if (productIndex + 1 < products.size()) { // If there is a next product
							Product p2 = products.get(productIndex + 1);
							Optional<Result> result2 = p2.getList().stream().filter(r2 -> result1.equals(r2))
									.findFirst();// We search for the first match result
							if (result2.isPresent()) {
								result1.setTargetProductResult(result2.get());
							} else {
								System.out.println("ko");
							}
						}
						prevSubRectEdge = sepStart;
					}
					rectEdge.y += 300;
				}
				drawLinks(e);
			}

			/**
			 * Draw the links between each matching results
			 * 
			 * @param e
			 */
			private void drawLinks(PaintEvent e) {
				for (Product product : products) {
					for (Result result : product.getList()) {
						Point p1 = result.getStartPoint();
						Result result2 = result.getTargetProductResult();
						if (result2 != null) {
							Point p2 = result2.getEndPoint();
							drawBezierCurve(e, p1, p2);
							new Path(null);
						}
					}
				}
			}
		});

		canvas.redraw();
		System.out.println();
	}

	/**
	 * Draw a bezier curve, between 2 points
	 * 
	 * @param e
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
