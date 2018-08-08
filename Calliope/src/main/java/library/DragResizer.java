/*
Code found here:
https://stackoverflow.com/questions/16925612/how-to-resize-component-with-mouse-drag-in-javafx
It was modified to support resizing width
 */

package library;

import javafx.scene.Cursor;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;

/**
 * {@link DragResizer} can be used to add mouse listeners to a {@link Region}
 * and make it resizable by the user by clicking and dragging the border in the
 * same way as a window.
 * <p>
 * Only height resizing is currently implemented. Usage: <pre>DragResizer.makeResizable(myAnchorPane);</pre>
 *
 * @author atill
 *
 */
public class DragResizer {

	/**
	 * The margin around the control that a user can click in to start resizing
	 * the region.
	 */
	private static final int RESIZE_MARGIN = 5;

	private final Region region;

	private double x;

	private boolean initMinWidth;

	private Double minWidth;

	private boolean dragging;

	private DragResizer(Region aRegion) {
		region = aRegion;
	}

	public static void makeResizable(Region region) {
		final DragResizer resizer = new DragResizer(region);

		region.setOnMousePressed(resizer::mousePressed);
		region.setOnMouseDragged(resizer::mouseDragged);
		region.setOnMouseMoved(resizer::mouseOver);
		region.setOnMouseReleased(resizer::mouseReleased);
	}

	private void mouseReleased(MouseEvent event) {
		dragging = false;
		region.setCursor(Cursor.DEFAULT);
	}

	private void mouseOver(MouseEvent event) {
		if(isInDraggableZone(event) || dragging) {
			region.setCursor(Cursor.E_RESIZE);
		}
		else {
			region.setCursor(Cursor.DEFAULT);
		}
	}

	private boolean isInDraggableZone(MouseEvent event) {
		return event.getX() > (region.getWidth() - RESIZE_MARGIN);
	}

	private void mouseDragged(MouseEvent event) {
		if(!dragging) {
			return;
		}

		double mouseX = event.getX();

		double newWidth = region.getMinWidth() + (mouseX - x);

		if (newWidth < minWidth)
			return;

		region.setMinWidth(newWidth);
		region.setMaxWidth(newWidth);

		x = mouseX;
	}

	private void mousePressed(MouseEvent event) {

		// ignore clicks outside of the draggable margin
		if(!isInDraggableZone(event)) {
			return;
		}

		dragging = true;

		// make sure that the minimum width is set to the current width once,
		// setting a min width that is smaller than the current width will
		// have no effect
		if (!initMinWidth) {
			region.setMinWidth(region.getWidth());
			minWidth = region.getWidth();
			initMinWidth = true;
		}

		x = event.getX();
	}
}