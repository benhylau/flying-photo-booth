package com.groundupworks.partyphotobooth.fragments;

/**
 * Simple Size class.
 *
 * @param <T>
 * @param <V>
 */
public final class SizeCamera<T extends Number,V extends Number> {
	public T mWidth;
	public V mHeight;

	public SizeCamera(T width, V height) {
		mWidth = width;
		mHeight = height;
	}
}