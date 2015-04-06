package com.groundupworks.partyphotobooth.image;

import android.util.Log;

import com.groundupworks.partyphotobooth.fragments.SizeCamera;

/**
 * Based in Marco Dinacci blog: http://www.intransitione.com/blog/how-to-detect-motion-on-an-android-device/
 * @author davidf
 */
public class AndroidImage_NV21 extends AbstractAndroidImage {

	private static final String TAG = "AndroidImage_NV21";
    private SizeCamera<Integer,Integer> mSize = null;

	public AndroidImage_NV21(byte[] data, SizeCamera<Integer, Integer> size) {
		super(data, size);
        mSize = size;
	}

	@Override
	public boolean isDifferent(AndroidImage other, int pixel_threshold,
			float threshold) {

		if(!assertImage(other)) {
			return false;
		}
		
		byte[] otherData = other.get();
		int totDifferentPixels = 0;
		
		// For the sake of this demo just use a 640x480 image.
		int height = mSize.mHeight;
		int width = mSize.mWidth;
		int size = height*width; // 640x480
		for (int i = 0, ij=0; i < height; i++) {
			for (int j = 0; j < width; j++,ij++) {
				int pix = (0xff & ((int) mData[ij])) - 16;
				int otherPix = (0xff & ((int) otherData[ij])) - 16;
				
				if (pix < 0) pix = 0;
				if (pix > 255) pix = 255;
				if (otherPix < 0) otherPix = 0;
				if (otherPix > 255) otherPix = 255;

				if(Math.abs(pix - otherPix) >= pixel_threshold)
					totDifferentPixels++;
			}
		}
		
		if(totDifferentPixels == 0) totDifferentPixels = 1;
		Log.d("david", "Number of different pixels: " + totDifferentPixels + " -> "
				+ (100 / ( size / totDifferentPixels) ) + "% (total=" + size + ")");
		
		return totDifferentPixels > (threshold * size);
	}
}