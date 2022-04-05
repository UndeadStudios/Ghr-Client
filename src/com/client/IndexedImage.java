package com.client;
// Decompiled by Jad v1.5.8f. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) 

public final class IndexedImage extends Rasterizer2D {

	public IndexedImage(FileArchive streamLoader, String s, int i) {
		Stream stream = new Stream(streamLoader.getDataForName(s + ".dat"));
		Stream stream_1 = new Stream(streamLoader.getDataForName("index.dat"));
		stream_1.currentPosition = stream.readUnsignedShort();
		resizeWidth = stream_1.readUnsignedShort();
		resizeHeight = stream_1.readUnsignedShort();
		int j = stream_1.readUnsignedByte();
		palette = new int[j];
		for (int k = 0; k < j - 1; k++)
			palette[k + 1] = stream_1.read3Bytes();

		for (int l = 0; l < i; l++) {
			stream_1.currentPosition += 2;
			stream.currentPosition += stream_1.readUnsignedShort()
					* stream_1.readUnsignedShort();
			stream_1.currentPosition++;
		}

		drawOffsetX = stream_1.readUnsignedByte();
		drawOffsetY = stream_1.readUnsignedByte();
		width = stream_1.readUnsignedShort();
		height = stream_1.readUnsignedShort();
		int i1 = stream_1.readUnsignedByte();
		int j1 = width * height;
		palettePixels = new byte[j1];
		if (i1 == 0) {
			for (int k1 = 0; k1 < j1; k1++)
				palettePixels[k1] = stream.readSignedByte();

			return;
		}
		if (i1 == 1) {
			for (int l1 = 0; l1 < width; l1++) {
				for (int i2 = 0; i2 < height; i2++)
					palettePixels[l1 + i2 * width] = stream
							.readSignedByte();

			}

		}
	}

	public void downscale() {
		resizeWidth /= 2;
		resizeHeight /= 2;
		byte raster[] = new byte[resizeWidth * resizeHeight];
		int sourceIndex = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				raster[(x + drawOffsetX >> 1)
						+ (y + drawOffsetY >> 1) * resizeWidth] = raster[sourceIndex++];
			}
		}
		this.palettePixels = raster;
		width = resizeWidth;
		height = resizeHeight;
		drawOffsetX = 0;
		drawOffsetY = 0;
	}

	public void resize() {
		if (width == resizeWidth && height == resizeHeight) {
			return;
		}

		byte raster[] = new byte[resizeWidth * resizeHeight];

		int i = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				raster[x + drawOffsetX + (y + drawOffsetY) * resizeWidth] = raster[i++];
			}
		}
		this.palettePixels = raster;
		width = resizeWidth;
		height = resizeHeight;
		drawOffsetX = 0;
		drawOffsetY = 0;
	}

	public void flipHorizontally() {
		byte raster[] = new byte[width * height];
		int pixel = 0;
		for (int y = 0; y < height; y++) {
			for (int x = width - 1; x >= 0; x--) {
				raster[pixel++] = raster[x + y * width];
			}
		}
		this.palettePixels = raster;
		drawOffsetX = resizeWidth - width - drawOffsetX;
	}

	public void flipVertically() {
		byte raster[] = new byte[width * height];
		int pixel = 0;
		for (int y = height - 1; y >= 0; y--) {
			for (int x = 0; x < width; x++) {
				raster[pixel++] = raster[x + y * width];
			}
		}
		this.palettePixels = raster;
		drawOffsetY = resizeHeight - height - drawOffsetY;
	}

	public void offsetColor(int redOffset, int greenOffset, int blueOffset) {
		for (int index = 0; index < palette.length; index++) {
			int red = palette[index] >> 16 & 0xff;
			red += redOffset;

			if (red < 0) {
				red = 0;
			} else if (red > 255) {
				red = 255;
			}

			int green = palette[index] >> 8 & 0xff;

			green += greenOffset;
			if (green < 0) {
				green = 0;
			} else if (green > 255) {
				green = 255;
			}

			int blue = palette[index] & 0xff;

			blue += blueOffset;
			if (blue < 0) {
				blue = 0;
			} else if (blue > 255) {
				blue = 255;
			}
			palette[index] = (red << 16) + (green << 8) + blue;
		}
	}

	public void draw(int x, int y) {
		x += drawOffsetX;
		y += drawOffsetY;
		int destOffset = x + y * Rasterizer2D.width;
		int sourceOffset = 0;
		int height = this.height;
		int width = this.width;
		int destStep = Rasterizer2D.width - width;
		int sourceStep = 0;

		if (y < Rasterizer2D.clip_top) {
			int dy = Rasterizer2D.clip_top - y;
			height -= dy;
			y = Rasterizer2D.clip_top;
			sourceOffset += dy * width;
			destOffset += dy * Rasterizer2D.width;
		}

		if (y + height > Rasterizer2D.clip_bottom) {
			height -= (y + height) - Rasterizer2D.clip_bottom;
		}

		if (x < Rasterizer2D.clip_left) {
			int k2 = Rasterizer2D.clip_left - x;
			width -= k2;
			x = Rasterizer2D.clip_left;
			sourceOffset += k2;
			destOffset += k2;
			sourceStep += k2;
			destStep += k2;
		}

		if (x + width > Rasterizer2D.clip_right) {
			int dx = (x + width) - Rasterizer2D.clip_right;
			width -= dx;
			sourceStep += dx;
			destStep += dx;
		}

		if (!(width <= 0 || height <= 0)) {
			draw(height, Rasterizer2D.pixels, palettePixels, destStep, destOffset, width, sourceOffset,
					palette, sourceStep);
		}

	}

	private void draw(int i, int raster[], byte image[], int destStep, int destIndex, int width,
					  int sourceIndex, int ai1[], int sourceStep) {
		int minX = -(width >> 2);
		width = -(width & 3);
		for (int y = -i; y < 0; y++) {
			for (int x = minX; x < 0; x++) {

				byte pixel = image[sourceIndex++];

				if (pixel != 0) {
					raster[destIndex++] = ai1[pixel & 0xff];
				} else {
					destIndex++;
				}
				pixel = image[sourceIndex++];
				if (pixel != 0) {
					raster[destIndex++] = ai1[pixel & 0xff];
				} else {
					destIndex++;
				}
				pixel = image[sourceIndex++];
				if (pixel != 0) {
					raster[destIndex++] = ai1[pixel & 0xff];
				} else {
					destIndex++;
				}
				pixel = image[sourceIndex++];
				if (pixel != 0) {
					raster[destIndex++] = ai1[pixel & 0xff];
				} else {
					destIndex++;
				}
			}
			for (int x = width; x < 0; x++) {
				byte pixel = image[sourceIndex++];
				if (pixel != 0) {
					raster[destIndex++] = ai1[pixel & 0xff];
				} else {
					destIndex++;
				}
			}
			destIndex += destStep;
			sourceIndex += sourceStep;
		}
	}

	public void setTransparency(int transRed, int transGreen, int transBlue) {
		for (int index = 0; index < palettePixels.length; index++) {
			int pixel = palette[palettePixels[index]];
			if(pixel == 0xff00ff) {
				palettePixels[index] = 0;
			}
		}
	}
	

	public byte palettePixels[];
	public final int[] palette;
	public int width;
	public int height;
	public int drawOffsetX;
	public int drawOffsetY;
	public int resizeWidth;
	private int resizeHeight;
}
