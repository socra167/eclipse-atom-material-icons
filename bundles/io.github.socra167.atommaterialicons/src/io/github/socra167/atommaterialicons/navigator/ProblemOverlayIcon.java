package io.github.socra167.atommaterialicons.navigator;

import java.util.Objects;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;

/**
 * Paints an error or warning overlay in the bottom left corner of an icon, like the Eclipse
 * problem decorations, but first clears the icon under and around the overlay.
 * <p>
 * Material icons fill the whole 16x16 area, so without the cleared margin the overlay merges with
 * the icon and is hard to see. The margin is one pixel at 100% zoom and scales with the zoom.
 * </p>
 */
final class ProblemOverlayIcon extends ImageDescriptor {

	private final ImageDescriptor base;
	private final ImageDescriptor overlay;

	ProblemOverlayIcon(ImageDescriptor base, ImageDescriptor overlay) {
		this.base = Objects.requireNonNull(base);
		this.overlay = Objects.requireNonNull(overlay);
	}

	@Override
	public ImageData getImageData(int zoom) {
		ImageData baseData = base.getImageData(zoom);
		ImageData overlayData = overlay.getImageData(zoom);
		if (baseData == null || overlayData == null) {
			// SWT falls back to scaling the 100% image
			return null;
		}
		return compose(baseData, overlayData, Math.max(1, Math.round(zoom / 100f)));
	}

	/**
	 * @param margin width of the cleared border around the overlay, in pixels
	 */
	static ImageData compose(ImageData baseData, ImageData overlayData, int margin) {
		int width = baseData.width;
		int height = baseData.height;
		ImageData result = new ImageData(width, height, 32, new PaletteData(0xFF0000, 0xFF00, 0xFF));
		result.alphaData = new byte[width * height];
		Alpha baseAlpha = new Alpha(baseData);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				RGB rgb = baseData.palette.getRGB(baseData.getPixel(x, y));
				result.setPixel(x, y, result.palette.getPixel(rgb));
				result.setAlpha(x, y, baseAlpha.at(x, y));
			}
		}

		int left = 0;
		int top = height - overlayData.height;
		Alpha overlayAlpha = new Alpha(overlayData);

		// clear the icon under the overlay and in a margin around it
		for (int oy = 0; oy < overlayData.height; oy++) {
			for (int ox = 0; ox < overlayData.width; ox++) {
				if (overlayAlpha.at(ox, oy) == 0) {
					continue;
				}
				for (int dy = -margin; dy <= margin; dy++) {
					for (int dx = -margin; dx <= margin; dx++) {
						int x = left + ox + dx;
						int y = top + oy + dy;
						if (x >= 0 && y >= 0 && x < width && y < height) {
							result.setAlpha(x, y, 0);
						}
					}
				}
			}
		}

		// paint the overlay
		for (int oy = 0; oy < overlayData.height; oy++) {
			for (int ox = 0; ox < overlayData.width; ox++) {
				int x = left + ox;
				int y = top + oy;
				if (x < 0 || y < 0 || x >= width || y >= height) {
					continue;
				}
				int alpha = overlayAlpha.at(ox, oy);
				if (alpha == 0) {
					continue;
				}
				RGB rgb = overlayData.palette.getRGB(overlayData.getPixel(ox, oy));
				result.setPixel(x, y, result.palette.getPixel(rgb));
				result.setAlpha(x, y, alpha);
			}
		}
		return result;
	}

	/**
	 * Reads the opacity of a pixel whatever kind of transparency the image data uses.
	 */
	private static final class Alpha {

		private final ImageData data;
		private ImageData mask;

		Alpha(ImageData data) {
			this.data = data;
		}

		int at(int x, int y) {
			if (data.alphaData != null) {
				return data.getAlpha(x, y);
			}
			if (data.alpha != -1) {
				return data.alpha;
			}
			if (data.transparentPixel != -1) {
				return data.getPixel(x, y) == data.transparentPixel ? 0 : 255;
			}
			if (data.maskData != null) {
				if (mask == null) {
					mask = data.getTransparencyMask();
				}
				return mask.getPixel(x, y) == 0 ? 0 : 255;
			}
			return 255;
		}
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ProblemOverlayIcon other && base.equals(other.base) && overlay.equals(other.overlay);
	}

	@Override
	public int hashCode() {
		return Objects.hash(base, overlay);
	}
}
