package org.openlca.display;

import java.util.ArrayList;
import java.util.Optional;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Shell;

public class ProductDisplay {
	private Shell shell;
	private ArrayList<Product> products;
	private Point screenSize;

	public ProductDisplay(Shell shell, final ArrayList<Product> products) {
		this.shell = shell;
		this.products = products;
		screenSize = shell.getSize();
	}

	void display() {
		Canvas canvas = new Canvas(shell, SWT.NONE);

		final int total = products.get(0).getList().size();
		canvas.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				screenSize = shell.getSize();
				Point rectEdge = new Point(100, 100);
				double width = screenSize.x * 0.8;
				int height = 30;
				double gap = width / total;
				for (int i = 0; i < products.size(); i++) {
					Product p = products.get(i);
					// Draw a rectangle for each product
					e.gc.drawRoundRectangle(rectEdge.x, rectEdge.y, (int) width, height, 30, 30);
					Point prevRectEdge = rectEdge;
					for (int j = 0; j < total; j++) {
						Result result1 = p.getResult(j);
						Point sepStart = new Point(rectEdge.x + (j + 1) * (int) gap, rectEdge.y);
						Point sepEnd = new Point(sepStart.x, rectEdge.y + height);
						// Draw a separator line between the current result, and the next one
						if (j != total - 1) {
							e.gc.drawLine(sepStart.x, sepStart.y, sepEnd.x, sepEnd.y);
						}
						Point textPos = new Point(prevRectEdge.x+10,(sepEnd.y+sepStart.y)/2-10);
						e.gc.drawText(result1.toString(), textPos.x,textPos.y);
						result1.setStartPoint((prevRectEdge.x + sepEnd.x) / 2, sepEnd.y);
						result1.setEndPoint((prevRectEdge.x + sepEnd.x) / 2, sepStart.y);
						if (i + 1 < products.size()) {
							Product p2 = products.get(i + 1);
							Optional<Result> result2 = p2.getList().stream().filter(r2 -> result1.equals(r2))
									.findFirst();
							if (result2.isPresent()) {
								result1.setTargetProductResult(result2.get());
							} else {
								System.out.println("ko");
							}
						}
						prevRectEdge = sepStart;
					}
					rectEdge.y += 300;
				}
				drawLinks(e);
			}

			private void drawLinks(PaintEvent e) {
				for (Product product : products) {
					for (Result result : product.getList()) {
						Point p1 = result.getStartPoint();
						Result result2 = result.getTargetProductResult();
						if (result2 != null) {
							Point p2 = result2.getEndPoint();
							e.gc.drawLine(p1.x, p1.y, p2.x, p2.y);
						}
					}
				}
			}
		});

		canvas.redraw();
		System.out.println();
	}
}
