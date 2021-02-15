package org.openlca.display;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

public class ProductDisplay {
	private Shell shell;
	private ArrayList<Product> products;
	private Point screenSize;
	private Config config;

	public ProductDisplay(Shell shell, final ArrayList<Product> products, Config config) {
		this.shell = shell;
		this.products = products;
		screenSize = shell.getSize();
		this.config = config;
	}

	/**
	 * Display the products, and draw links between each matching results
	 */
	void display() {
		System.out.println(1);
		Canvas canvas = new Canvas(shell, SWT.V_SCROLL);
		System.out.println(3);
		final int total = products.get(0).getList().size();
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				System.out.println(2);
				screenSize = shell.getSize(); // Responsive behavior
				System.out.println(screenSize);
				Point rectEdge = new Point(100, 100); // Start point of the first product rectangle
				double width = screenSize.x * 0.8;
				int height = 30;
				double gap = width / total;
				for (int productIndex = 0; productIndex < products.size(); productIndex++) {
					Product p = products.get(productIndex);
					// Draw a rectangle for each product
					e.gc.drawRoundRectangle(rectEdge.x, rectEdge.y, (int) width, height, 30, 30);
					Point prevSubRectEdge = rectEdge; // Coordinate of each result rectangle
					for (int resultIndex = 0; resultIndex < total; resultIndex++) {
						Result result1 = p.getResult(resultIndex);
						Point sepStart = new Point(rectEdge.x + (resultIndex + 1) * (int) gap, rectEdge.y);
						Point sepEnd = new Point(sepStart.x, rectEdge.y + height);
						// Draw a separator line between the current result, and the next one
						if (resultIndex != total - 1) {
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
