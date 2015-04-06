package com.groundupworks.partyphotobooth.image;

public interface AndroidImage {

	/** Erode the image. In place. */
	public abstract AndroidImage erode(int erosionLevel);
	
	/**
	 * Morph the current image with another one. In place.
	 */
	public abstract AndroidImage morph(AndroidImage background, int value);

	/**
	 * Check whether the current image is different from the given image.
	 * A pixel is different if pixel_value - threshold < pixel_threshold < pixel_value + threshold
	 * An image is different if the total of different pixels is > than threshold
     * @param background
     * @param pixel_threshold
     * @param threshold the percentage (0...1) of the different pixels to consider different image.
	 */
	public abstract boolean isDifferent(AndroidImage background, int pixel_threshold, 
			float threshold);
	
	/**
	 * Access the low level data of the image. Data layout is
	 * implementation dependent 
	 */
	public abstract byte[] get();

}