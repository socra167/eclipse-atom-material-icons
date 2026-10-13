package io.github.socra167.atommaterialicons.navigator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.junit.jupiter.api.Test;

class ProblemOverlayIconTest {

	private static final RGB BLUE = new RGB(0, 0, 255);
	private static final RGB RED = new RGB(255, 0, 0);

	@Test
	void clearsAMarginAroundTheOverlay() {
		ImageData base = solid(16, 16, BLUE);
		ImageData overlay = solid(7, 8, RED);

		ImageData result = ProblemOverlayIcon.compose(base, overlay, 1);

		// the overlay sits in the bottom left corner: x 0..6, y 8..15
		assertEquals(RED, color(result, 0, 15));
		assertEquals(RED, color(result, 6, 8));
		assertEquals(255, result.getAlpha(6, 8));
		// one pixel around it is cleared
		assertEquals(0, result.getAlpha(7, 8));
		assertEquals(0, result.getAlpha(6, 7));
		assertEquals(0, result.getAlpha(7, 7));
		// the rest of the icon is untouched
		assertEquals(255, result.getAlpha(8, 8));
		assertEquals(BLUE, color(result, 8, 8));
		assertEquals(255, result.getAlpha(6, 6));
		assertEquals(BLUE, color(result, 15, 0));
	}

	@Test
	void transparentOverlayPixelsKeepTheIcon() {
		ImageData base = solid(16, 16, BLUE);
		ImageData overlay = solid(7, 8, RED);
		overlay.setAlpha(6, 0, 0);
		overlay.setAlpha(5, 0, 0);
		overlay.setAlpha(6, 1, 0);

		ImageData result = ProblemOverlayIcon.compose(base, overlay, 1);

		// the top right corner of the overlay is transparent and away from its opaque pixels
		assertEquals(255, result.getAlpha(7, 7));
		assertEquals(BLUE, color(result, 7, 7));
	}

	private static ImageData solid(int width, int height, RGB rgb) {
		ImageData data = new ImageData(width, height, 32, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		data.alphaData = new byte[width * height];
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				data.setPixel(x, y, data.palette.getPixel(rgb));
				data.setAlpha(x, y, 255);
			}
		}
		return data;
	}

	private static RGB color(ImageData data, int x, int y) {
		return data.palette.getRGB(data.getPixel(x, y));
	}
}
