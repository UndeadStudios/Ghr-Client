package com.client;


public final class Rasterizer3D extends Rasterizer2D {

    public static boolean saveDepth;
    public static float[] depthBuffer;
    public static int fieldOfView = 512;
    public static double brightness = 0;
    public static boolean world = true;
    private static int mipMapLevel;
    public static int textureAmount = 103;
    static boolean textureOutOfDrawingBounds;
    private static boolean aBoolean1463;
    public static boolean aBoolean1464 = true;
    public static int alpha;
    public static boolean lowMem = false;
    public static int originViewX;
    public static int originViewY;
    public static int textureInt3;
    public static int textureInt4;
    private static int[] anIntArray1468;
    public static final int[] anIntArray1469;
    public static int anIntArray1470[];
    public static int COSINE[];
    public static int scanOffsets[];
    private static int textureCount;
    public static IndexedImage textures[] = new IndexedImage[textureAmount];
    private static boolean[] textureIsTransparant = new boolean[textureAmount];
    private static int[] averageTextureColours = new int[textureAmount];
    private static int textureRequestBufferPointer;
    private static int[][] textureRequestPixelBuffer;
    private static int[][] texturesPixelBuffer = new int[textureAmount][];
    public static int textureLastUsed[] = new int[textureAmount];
    public static int lastTextureRetrievalCount;
    public static int hslToRgb[] = new int[0x10000];
    private static int[][] currentPalette = new int[textureAmount][];
    public static boolean repeatTexture;

    static {
        anIntArray1468 = new int[512];
        anIntArray1469 = new int[2048];
        anIntArray1470 = new int[2048];
        COSINE = new int[2048];
        for (int i = 1; i < 512; i++) {
            anIntArray1468[i] = 32768 / i;
        }
        for (int j = 1; j < 2048; j++) {
            anIntArray1469[j] = 0x10000 / j;
        }
        for (int k = 0; k < 2048; k++) {
            anIntArray1470[k] = (int) (65536D * Math.sin(k * 0.0030679614999999999D));
            COSINE[k] = (int) (65536D * Math.cos(k * 0.0030679614999999999D));
        }
    }
    public static void nullLoader() {
        anIntArray1468 = null;
        anIntArray1468 = null;
        anIntArray1470 = null;
        COSINE = null;
        scanOffsets = null;
        textures = null;
        textureIsTransparant = null;
        averageTextureColours = null;
        textureRequestPixelBuffer = null;
        texturesPixelBuffer = null;
        textureLastUsed = null;
        hslToRgb = null;
        currentPalette = null;
    }
    public static void useViewport() {
        scanOffsets = new int[Rasterizer2D.height];
        for (int j = 0; j < Rasterizer2D.height; j++) {
            scanOffsets[j] = Rasterizer2D.width * j;
        }
        originViewX = Rasterizer2D.width / 2;
        originViewY = Rasterizer2D.height / 2;
    }

    public static void reposition(int width, int height) {
        scanOffsets = new int[height];
        for (int l = 0; l < height; l++) {
            scanOffsets[l] = width * l;
        }
        originViewX = width / 2;
        originViewY = height / 2;
    }

    public static void drawFog(int rgb, int begin, int end) {
        float length = end - begin;// Store as a float for division later
        for (int index = 0; index < Rasterizer2D.pixels.length; index++) {
            float factor = (Rasterizer2D.depthBuffer[index] - begin) / length;
            Rasterizer2D.pixels[index] = blend(Rasterizer2D.pixels[index], rgb, factor);
        }
    }

    private static int blend(int c1, int c2, float factor) {
        if (factor >= 1f) {
            return c2;
        }
        if (factor <= 0f) {
            return c1;
        }

        int r1 = (c1 >> 16) & 0xff;
        int g1 = (c1 >> 8) & 0xff;
        int b1 = (c1) & 0xff;

        int r2 = (c2 >> 16) & 0xff;
        int g2 = (c2 >> 8) & 0xff;
        int b2 = (c2) & 0xff;

        int r3 = r2 - r1;
        int g3 = g2 - g1;
        int b3 = b2 - b1;

        int r = (int) (r1 + (r3 * factor));
        int g = (int) (g1 + (g3 * factor));
        int b = (int) (b1 + (b3 * factor));

        return (r << 16) + (g << 8) + b;
    }

    public static void clearTextureCache() {
        textureRequestPixelBuffer = null;
        for (int j = 0; j < textureAmount; j++) {
            texturesPixelBuffer[j] = null;
        }
    }

    public static void initiateRequestBuffers() {
        if (textureRequestPixelBuffer == null) {
            textureRequestBufferPointer = 20;

            textureRequestPixelBuffer = new int[textureRequestBufferPointer][0x10000];

            for (int i = 0; i < textureAmount; i++) {
                texturesPixelBuffer[i] = null;
            }
        }
    }
    public static void loadTextures(FileArchive archive) {
        textureCount = 0;
        for (int index = 0; index < textureAmount; index++) {
            try {
                textures[index] = new IndexedImage(archive, String.valueOf(index), 0);
                if (lowMem && textures[index].resizeWidth == 128) {
                    textures[index].downscale();
                } else {
                    textures[index].resize();
                    textures[index].setTransparency(255, 0, 255);
                }
                textureCount++;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public static int getOverallColour(int textureId) {
        if (averageTextureColours[textureId] != 0) {
            return averageTextureColours[textureId];
        }
        int totalRed = 0;
        int totalGreen = 0;
        int totalBlue = 0;
        int colourCount = currentPalette[textureId].length;
        for (int ptr = 0; ptr < colourCount; ptr++) {
            totalRed += currentPalette[textureId][ptr] >> 16 & 0xff;
            totalGreen += currentPalette[textureId][ptr] >> 8 & 0xff;
            totalBlue += currentPalette[textureId][ptr] & 0xff;
        }

        int avgPaletteColour = (totalRed / colourCount << 16) + (totalGreen / colourCount << 8)
                + totalBlue / colourCount;
        avgPaletteColour = adjustBrightness(avgPaletteColour, 1.3999999999999999D);
        if (avgPaletteColour == 0) {
            avgPaletteColour = 1;
        }
        averageTextureColours[textureId] = avgPaletteColour;
        return avgPaletteColour;
    }

    public static void requestTextureUpdate(int textureId) {
        if (textureId > 50 && textureId != 59 && textureId != 56 && textureId != 57 && textureId != 96 && textureId != 97 && textureId != 98 && textureId != 99 && textureId != 100 && textureId != 101 && textureId != 102){
            return;
        }
        if (texturesPixelBuffer[textureId] == null) {
            return;
        }
        textureRequestPixelBuffer[textureRequestBufferPointer++] = texturesPixelBuffer[textureId];
        texturesPixelBuffer[textureId] = null;
    }

    public static int[] getTexturePixels(int textureId) {
        textureLastUsed[textureId] = lastTextureRetrievalCount++;
        if (texturesPixelBuffer[textureId] != null) {
            return texturesPixelBuffer[textureId];
        }
        int[] texturePixels;
        if (textureRequestBufferPointer > 0) {
            texturePixels = textureRequestPixelBuffer[--textureRequestBufferPointer];
            textureRequestPixelBuffer[textureRequestBufferPointer] = null;
        } else {
            int lastUsed = 0;
            int target = -1;
            for (int l = 0; l < textureCount; l++) {
                if (texturesPixelBuffer[l] != null && (textureLastUsed[l] < lastUsed || target == -1)) {
                    lastUsed = textureLastUsed[l];
                    target = l;
                }
            }

            texturePixels = texturesPixelBuffer[target];
            texturesPixelBuffer[target] = null;
        }
        texturesPixelBuffer[textureId] = texturePixels;
        IndexedImage background = textures[textureId];
        int[] texturePalette = currentPalette[textureId];

        if (background.width == 64) {
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    texturePixels[y
                            + (x << 7)] = texturePalette[background.palettePixels[(y >> 1) + ((x >> 1) << 6)]];
                }
            }
        } else {
            for (int i = 0; i < 16384; i++) {
                try {
                    texturePixels[i] = texturePalette[background.palettePixels[i]];
                } catch (Exception ignored) {

                }

            }

            textureIsTransparant[textureId] = false;
            for (int i = 0; i < 16384; i++) {
                texturePixels[i] &= 0xf8f8ff;
                int colour = texturePixels[i];
                if (colour == 0) {
                    textureIsTransparant[textureId] = true;
                }
                texturePixels[16384 + i] = colour - (colour >>> 3) & 0xf8f8ff;
                texturePixels[32768 + i] = colour - (colour >>> 2) & 0xf8f8ff;
                texturePixels[49152 + i] = colour - (colour >>> 2) - (colour >>> 3) & 0xf8f8ff;
            }

        }
        return texturePixels;
    }

    public static void setBrightness(double bright) {
        brightness = bright;
        int j = 0;
        for (int k = 0; k < 512; k++) {
            double d1 = k / 8 / 64D + 0.0078125D;
            double d2 = (k & 7) / 8D + 0.0625D;
            for (int k1 = 0; k1 < 128; k1++) {
                double d3 = k1 / 128D;
                double r = d3;
                double g = d3;
                double b = d3;
                if (d2 != 0.0D) {
                    double d7;
                    if (d3 < 0.5D) {
                        d7 = d3 * (1.0D + d2);
                    } else {
                        d7 = (d3 + d2) - d3 * d2;
                    }
                    double d8 = 2D * d3 - d7;
                    double d9 = d1 + 0.33333333333333331D;
                    if (d9 > 1.0D) {
                        d9--;
                    }
                    double d10 = d1;
                    double d11 = d1 - 0.33333333333333331D;
                    if (d11 < 0.0D) {
                        d11++;
                    }
                    if (6D * d9 < 1.0D) {
                        r = d8 + (d7 - d8) * 6D * d9;
                    } else if (2D * d9 < 1.0D) {
                        r = d7;
                    } else if (3D * d9 < 2D) {
                        r = d8 + (d7 - d8) * (0.66666666666666663D - d9) * 6D;
                    } else {
                        r = d8;
                    }
                    if (6D * d10 < 1.0D) {
                        g = d8 + (d7 - d8) * 6D * d10;
                    } else if (2D * d10 < 1.0D) {
                        g = d7;
                    } else if (3D * d10 < 2D) {
                        g = d8 + (d7 - d8) * (0.66666666666666663D - d10) * 6D;
                    } else {
                        g = d8;
                    }
                    if (6D * d11 < 1.0D) {
                        b = d8 + (d7 - d8) * 6D * d11;
                    } else if (2D * d11 < 1.0D) {
                        b = d7;
                    } else if (3D * d11 < 2D) {
                        b = d8 + (d7 - d8) * (0.66666666666666663D - d11) * 6D;
                    } else {
                        b = d8;
                    }
                }
                int byteR = (int) (r * 256D);
                int byteG = (int) (g * 256D);
                int byteB = (int) (b * 256D);
                int rgb = (byteR << 16) + (byteG << 8) + byteB;
                rgb = adjustBrightness(rgb, bright);
                if (rgb == 0) {
                    rgb = 1;
                }
                hslToRgb[j++] = rgb;
            }

        }

        for (int textureId = 0; textureId < textureAmount; textureId++) {
            if (textures[textureId] != null) {
                int[] originalPalette = textures[textureId].palette;
                currentPalette[textureId] = new int[originalPalette.length];
                for (int colourId = 0; colourId < originalPalette.length; colourId++) {
                    currentPalette[textureId][colourId] = adjustBrightness(originalPalette[colourId],
                            bright);
                    if ((currentPalette[textureId][colourId] & 0xf8f8ff) == 0 && colourId != 0) {
                        currentPalette[textureId][colourId] = 1;
                    }
                }

            }
        }

        for (int textureId = 0; textureId < textureAmount; textureId++) {
            requestTextureUpdate(textureId);
        }

    }
    static int adjustBrightness(int color, double amt) {
        double red = (color >> 16) / 256D;
        double green = (color >> 8 & 0xff) / 256D;
        double blue = (color & 0xff) / 256D;
        red = Math.pow(red, amt);
        green = Math.pow(green, amt);
        blue = Math.pow(blue, amt);
        final int red2 = (int) (red * 256D);
        final int green2 = (int) (green * 256D);
        final int blue2 = (int) (blue * 256D);
        return (red2 << 16) + (green2 << 8) + blue2;
    }


    public static void drawShadedTriangle(int y_a, int y_b, int y_c, int x_a, int x_b, int x_c,
                                          int hsl1, int hsl2, int hsl3, float z_a, float z_b, float z_c) {
        if (z_a < 0 || z_b < 0 || z_c < 0) {
            return;
        }
        int rgb1 = hslToRgb[hsl1];
        int rgb2 = hslToRgb[hsl2];
        int rgb3 = hslToRgb[hsl3];
        int r1 = rgb1 >> 16 & 0xff;
        int g1 = rgb1 >> 8 & 0xff;
        int b1 = rgb1 & 0xff;
        int r2 = rgb2 >> 16 & 0xff;
        int g2 = rgb2 >> 8 & 0xff;
        int b2 = rgb2 & 0xff;
        int r3 = rgb3 >> 16 & 0xff;
        int g3 = rgb3 >> 8 & 0xff;
        int b3 = rgb3 & 0xff;
        int a_to_b = 0;
        int dr1 = 0;
        int dg1 = 0;
        int db1 = 0;
        if (y_b != y_a) {
            a_to_b = (x_b - x_a << 16) / (y_b - y_a);
            dr1 = (r2 - r1 << 16) / (y_b - y_a);
            dg1 = (g2 - g1 << 16) / (y_b - y_a);
            db1 = (b2 - b1 << 16) / (y_b - y_a);
        }
        int b_to_c = 0;
        int dr2 = 0;
        int dg2 = 0;
        int db2 = 0;
        if (y_c != y_b) {
            b_to_c = (x_c - x_b << 16) / (y_c - y_b);
            dr2 = (r3 - r2 << 16) / (y_c - y_b);
            dg2 = (g3 - g2 << 16) / (y_c - y_b);
            db2 = (b3 - b2 << 16) / (y_c - y_b);
        }
        int c_to_a = 0;
        int dr3 = 0;
        int dg3 = 0;
        int db3 = 0;
        if (y_c != y_a) {
            c_to_a = (x_a - x_c << 16) / (y_a - y_c);
            dr3 = (r1 - r3 << 16) / (y_a - y_c);
            dg3 = (g1 - g3 << 16) / (y_a - y_c);
            db3 = (b1 - b3 << 16) / (y_a - y_c);
        }
        float b_aX = x_b - x_a;
        float b_aY = y_b - y_a;
        float c_aX = x_c - x_a;
        float c_aY = y_c - y_a;
        float b_aZ = z_b - z_a;
        float c_aZ = z_c - z_a;

        float div = b_aX * c_aY - c_aX * b_aY;
        float depth_slope = (b_aZ * c_aY - c_aZ * b_aY) / div;
        float depth_increment = (c_aZ * b_aX - b_aZ * c_aX) / div;
        if (y_a <= y_b && y_a <= y_c) {
            if (y_a >= Rasterizer2D.clip_bottom) {
                return;
            }
            if (y_b > Rasterizer2D.clip_bottom) {
                y_b = Rasterizer2D.clip_bottom;
            }
            if (y_c > Rasterizer2D.clip_bottom) {
                y_c = Rasterizer2D.clip_bottom;
            }
            z_a = z_a - depth_slope * x_a + depth_slope;
            if (y_b < y_c) {
                x_c = x_a <<= 16;
                r3 = r1 <<= 16;
                g3 = g1 <<= 16;
                b3 = b1 <<= 16;
                if (y_a < 0) {
                    x_c -= c_to_a * y_a;
                    x_a -= a_to_b * y_a;
                    r3 -= dr3 * y_a;
                    g3 -= dg3 * y_a;
                    b3 -= db3 * y_a;
                    r1 -= dr1 * y_a;
                    g1 -= dg1 * y_a;
                    b1 -= db1 * y_a;
                    z_a -= depth_increment * y_a;
                    y_a = 0;
                }
                x_b <<= 16;
                r2 <<= 16;
                g2 <<= 16;
                b2 <<= 16;
                if (y_b < 0) {
                    x_b -= b_to_c * y_b;
                    r2 -= dr2 * y_b;
                    g2 -= dg2 * y_b;
                    b2 -= db2 * y_b;
                    y_b = 0;
                }
                if (y_a != y_b && c_to_a < a_to_b || y_a == y_b && c_to_a > b_to_c) {
                    y_c -= y_b;
                    y_b -= y_a;
                    for (y_a = scanOffsets[y_a]; --y_b >= 0; y_a += Rasterizer2D.width) {
                        drawShadedScanline(Rasterizer2D.pixels, y_a, x_c >> 16, x_a >> 16, r3, g3, b3, r1, g1,
                                b1, z_a, depth_slope);
                        x_c += c_to_a;
                        x_a += a_to_b;
                        r3 += dr3;
                        g3 += dg3;
                        b3 += db3;
                        r1 += dr1;
                        g1 += dg1;
                        b1 += db1;
                        z_a += depth_increment;
                    }
                    while (--y_c >= 0) {
                        drawShadedScanline(Rasterizer2D.pixels, y_a, x_c >> 16, x_b >> 16, r3, g3, b3, r2, g2,
                                b2, z_a, depth_slope);
                        x_c += c_to_a;
                        x_b += b_to_c;
                        r3 += dr3;
                        g3 += dg3;
                        b3 += db3;
                        r2 += dr2;
                        g2 += dg2;
                        b2 += db2;
                        y_a += Rasterizer2D.width;
                        z_a += depth_increment;
                    }
                    return;
                }
                y_c -= y_b;
                y_b -= y_a;
                for (y_a = scanOffsets[y_a]; --y_b >= 0; y_a += Rasterizer2D.width) {
                    drawShadedScanline(Rasterizer2D.pixels, y_a, x_a >> 16, x_c >> 16, r1, g1, b1, r3, g3, b3,
                            z_a, depth_slope);
                    x_c += c_to_a;
                    x_a += a_to_b;
                    r3 += dr3;
                    g3 += dg3;
                    b3 += db3;
                    r1 += dr1;
                    g1 += dg1;
                    b1 += db1;
                    z_a += depth_increment;
                }
                while (--y_c >= 0) {
                    drawShadedScanline(Rasterizer2D.pixels, y_a, x_b >> 16, x_c >> 16, r2, g2, b2, r3, g3, b3,
                            z_a, depth_slope);
                    x_c += c_to_a;
                    x_b += b_to_c;
                    r3 += dr3;
                    g3 += dg3;
                    b3 += db3;
                    r2 += dr2;
                    g2 += dg2;
                    b2 += db2;
                    y_a += Rasterizer2D.width;
                    z_a += depth_increment;
                }
                return;
            }
            x_b = x_a <<= 16;
            r2 = r1 <<= 16;
            g2 = g1 <<= 16;
            b2 = b1 <<= 16;
            if (y_a < 0) {
                x_b -= c_to_a * y_a;
                x_a -= a_to_b * y_a;
                r2 -= dr3 * y_a;
                g2 -= dg3 * y_a;
                b2 -= db3 * y_a;
                r1 -= dr1 * y_a;
                g1 -= dg1 * y_a;
                b1 -= db1 * y_a;
                z_a -= depth_increment * y_a;
                y_a = 0;
            }
            x_c <<= 16;
            r3 <<= 16;
            g3 <<= 16;
            b3 <<= 16;
            if (y_c < 0) {
                x_c -= b_to_c * y_c;
                r3 -= dr2 * y_c;
                g3 -= dg2 * y_c;
                b3 -= db2 * y_c;
                y_c = 0;
            }
            if (y_a != y_c && c_to_a < a_to_b || y_a == y_c && b_to_c > a_to_b) {
                y_b -= y_c;
                y_c -= y_a;
                for (y_a = scanOffsets[y_a]; --y_c >= 0; y_a += Rasterizer2D.width) {
                    drawShadedScanline(Rasterizer2D.pixels, y_a, x_b >> 16, x_a >> 16, r2, g2, b2, r1, g1, b1,
                            z_a, depth_slope);
                    x_b += c_to_a;
                    x_a += a_to_b;
                    r2 += dr3;
                    g2 += dg3;
                    b2 += db3;
                    r1 += dr1;
                    g1 += dg1;
                    b1 += db1;
                    z_a += depth_increment;
                }
                while (--y_b >= 0) {
                    drawShadedScanline(Rasterizer2D.pixels, y_a, x_c >> 16, x_a >> 16, r3, g3, b3, r1, g1, b1,
                            z_a, depth_slope);
                    x_c += b_to_c;
                    x_a += a_to_b;
                    r3 += dr2;
                    g3 += dg2;
                    b3 += db2;
                    r1 += dr1;
                    g1 += dg1;
                    b1 += db1;
                    y_a += Rasterizer2D.width;
                    z_a += depth_increment;
                }
                return;
            }
            y_b -= y_c;
            y_c -= y_a;
            for (y_a = scanOffsets[y_a]; --y_c >= 0; y_a += Rasterizer2D.width) {
                drawShadedScanline(Rasterizer2D.pixels, y_a, x_a >> 16, x_b >> 16, r1, g1, b1, r2, g2, b2,
                        z_a, depth_slope);
                x_b += c_to_a;
                x_a += a_to_b;
                r2 += dr3;
                g2 += dg3;
                b2 += db3;
                r1 += dr1;
                g1 += dg1;
                b1 += db1;
                z_a += depth_increment;
            }
            while (--y_b >= 0) {
                drawShadedScanline(Rasterizer2D.pixels, y_a, x_a >> 16, x_c >> 16, r1, g1, b1, r3, g3, b3,
                        z_a, depth_slope);
                x_c += b_to_c;
                x_a += a_to_b;
                r3 += dr2;
                g3 += dg2;
                b3 += db2;
                r1 += dr1;
                g1 += dg1;
                b1 += db1;
                y_a += Rasterizer2D.width;
                z_a += depth_increment;
            }
            return;
        }
        if (y_b <= y_c) {
            if (y_b >= Rasterizer2D.clip_bottom) {
                return;
            }
            if (y_c > Rasterizer2D.clip_bottom) {
                y_c = Rasterizer2D.clip_bottom;
            }
            if (y_a > Rasterizer2D.clip_bottom) {
                y_a = Rasterizer2D.clip_bottom;
            }
            z_b = z_b - depth_slope * x_b + depth_slope;
            if (y_c < y_a) {
                x_a = x_b <<= 16;
                r1 = r2 <<= 16;
                g1 = g2 <<= 16;
                b1 = b2 <<= 16;
                if (y_b < 0) {
                    x_a -= a_to_b * y_b;
                    x_b -= b_to_c * y_b;
                    r1 -= dr1 * y_b;
                    g1 -= dg1 * y_b;
                    b1 -= db1 * y_b;
                    r2 -= dr2 * y_b;
                    g2 -= dg2 * y_b;
                    b2 -= db2 * y_b;
                    z_b -= depth_increment * y_b;
                    y_b = 0;
                }
                x_c <<= 16;
                r3 <<= 16;
                g3 <<= 16;
                b3 <<= 16;
                if (y_c < 0) {
                    x_c -= c_to_a * y_c;
                    r3 -= dr3 * y_c;
                    g3 -= dg3 * y_c;
                    b3 -= db3 * y_c;
                    y_c = 0;
                }
                if (y_b != y_c && a_to_b < b_to_c || y_b == y_c && a_to_b > c_to_a) {
                    y_a -= y_c;
                    y_c -= y_b;
                    for (y_b = scanOffsets[y_b]; --y_c >= 0; y_b += Rasterizer2D.width) {
                        drawShadedScanline(Rasterizer2D.pixels, y_b, x_a >> 16, x_b >> 16, r1, g1, b1, r2, g2,
                                b2, z_b, depth_slope);
                        x_a += a_to_b;
                        x_b += b_to_c;
                        r1 += dr1;
                        g1 += dg1;
                        b1 += db1;
                        r2 += dr2;
                        g2 += dg2;
                        b2 += db2;
                        z_b += depth_increment;
                    }
                    while (--y_a >= 0) {
                        drawShadedScanline(Rasterizer2D.pixels, y_b, x_a >> 16, x_c >> 16, r1, g1, b1, r3, g3,
                                b3, z_b, depth_slope);
                        x_a += a_to_b;
                        x_c += c_to_a;
                        r1 += dr1;
                        g1 += dg1;
                        b1 += db1;
                        r3 += dr3;
                        g3 += dg3;
                        b3 += db3;
                        y_b += Rasterizer2D.width;
                        z_b += depth_increment;
                    }
                    return;
                }
                y_a -= y_c;
                y_c -= y_b;
                for (y_b = scanOffsets[y_b]; --y_c >= 0; y_b += Rasterizer2D.width) {
                    drawShadedScanline(Rasterizer2D.pixels, y_b, x_b >> 16, x_a >> 16, r2, g2, b2, r1, g1, b1,
                            z_b, depth_slope);
                    x_a += a_to_b;
                    x_b += b_to_c;
                    r1 += dr1;
                    g1 += dg1;
                    b1 += db1;
                    r2 += dr2;
                    g2 += dg2;
                    b2 += db2;
                    z_b += depth_increment;
                }
                while (--y_a >= 0) {
                    drawShadedScanline(Rasterizer2D.pixels, y_b, x_c >> 16, x_a >> 16, r3, g3, b3, r1, g1, b1,
                            z_b, depth_slope);
                    x_a += a_to_b;
                    x_c += c_to_a;
                    r1 += dr1;
                    g1 += dg1;
                    b1 += db1;
                    r3 += dr3;
                    g3 += dg3;
                    b3 += db3;
                    y_b += Rasterizer2D.width;
                    z_b += depth_increment;
                }
                return;
            }
            x_c = x_b <<= 16;
            r3 = r2 <<= 16;
            g3 = g2 <<= 16;
            b3 = b2 <<= 16;
            if (y_b < 0) {
                x_c -= a_to_b * y_b;
                x_b -= b_to_c * y_b;
                r3 -= dr1 * y_b;
                g3 -= dg1 * y_b;
                b3 -= db1 * y_b;
                r2 -= dr2 * y_b;
                g2 -= dg2 * y_b;
                b2 -= db2 * y_b;
                z_b -= depth_increment * y_b;
                y_b = 0;
            }
            x_a <<= 16;
            r1 <<= 16;
            g1 <<= 16;
            b1 <<= 16;
            if (y_a < 0) {
                x_a -= c_to_a * y_a;
                r1 -= dr3 * y_a;
                g1 -= dg3 * y_a;
                b1 -= db3 * y_a;
                y_a = 0;
            }
            if (a_to_b < b_to_c) {
                y_c -= y_a;
                y_a -= y_b;
                for (y_b = scanOffsets[y_b]; --y_a >= 0; y_b += Rasterizer2D.width) {
                    drawShadedScanline(Rasterizer2D.pixels, y_b, x_c >> 16, x_b >> 16, r3, g3, b3, r2, g2, b2,
                            z_b, depth_slope);
                    x_c += a_to_b;
                    x_b += b_to_c;
                    r3 += dr1;
                    g3 += dg1;
                    b3 += db1;
                    r2 += dr2;
                    g2 += dg2;
                    b2 += db2;
                    z_b += depth_increment;
                }
                while (--y_c >= 0) {
                    drawShadedScanline(Rasterizer2D.pixels, y_b, x_a >> 16, x_b >> 16, r1, g1, b1, r2, g2, b2,
                            z_b, depth_slope);
                    x_a += c_to_a;
                    x_b += b_to_c;
                    r1 += dr3;
                    g1 += dg3;
                    b1 += db3;
                    r2 += dr2;
                    g2 += dg2;
                    b2 += db2;
                    y_b += Rasterizer2D.width;
                    z_b += depth_increment;
                }
                return;
            }
            y_c -= y_a;
            y_a -= y_b;
            for (y_b = scanOffsets[y_b]; --y_a >= 0; y_b += Rasterizer2D.width) {
                drawShadedScanline(Rasterizer2D.pixels, y_b, x_b >> 16, x_c >> 16, r2, g2, b2, r3, g3, b3,
                        z_b, depth_slope);
                x_c += a_to_b;
                x_b += b_to_c;
                r3 += dr1;
                g3 += dg1;
                b3 += db1;
                r2 += dr2;
                g2 += dg2;
                b2 += db2;
                z_b += depth_increment;
            }
            while (--y_c >= 0) {
                drawShadedScanline(Rasterizer2D.pixels, y_b, x_b >> 16, x_a >> 16, r2, g2, b2, r1, g1, b1,
                        z_b, depth_slope);
                x_a += c_to_a;
                x_b += b_to_c;
                r1 += dr3;
                g1 += dg3;
                b1 += db3;
                r2 += dr2;
                g2 += dg2;
                b2 += db2;
                y_b += Rasterizer2D.width;
                z_b += depth_increment;
            }
            return;
        }
        if (y_c >= Rasterizer2D.clip_bottom) {
            return;
        }
        if (y_a > Rasterizer2D.clip_bottom) {
            y_a = Rasterizer2D.clip_bottom;
        }
        if (y_b > Rasterizer2D.clip_bottom) {
            y_b = Rasterizer2D.clip_bottom;
        }
        z_c = z_c - depth_slope * x_c + depth_slope;
        if (y_a < y_b) {
            x_b = x_c <<= 16;
            r2 = r3 <<= 16;
            g2 = g3 <<= 16;
            b2 = b3 <<= 16;
            if (y_c < 0) {
                x_b -= b_to_c * y_c;
                x_c -= c_to_a * y_c;
                r2 -= dr2 * y_c;
                g2 -= dg2 * y_c;
                b2 -= db2 * y_c;
                r3 -= dr3 * y_c;
                g3 -= dg3 * y_c;
                b3 -= db3 * y_c;
                z_c -= depth_increment * y_c;
                y_c = 0;
            }
            x_a <<= 16;
            r1 <<= 16;
            g1 <<= 16;
            b1 <<= 16;
            if (y_a < 0) {
                x_a -= a_to_b * y_a;
                r1 -= dr1 * y_a;
                g1 -= dg1 * y_a;
                b1 -= db1 * y_a;
                y_a = 0;
            }
            if (b_to_c < c_to_a) {
                y_b -= y_a;
                y_a -= y_c;
                for (y_c = scanOffsets[y_c]; --y_a >= 0; y_c += Rasterizer2D.width) {
                    drawShadedScanline(Rasterizer2D.pixels, y_c, x_b >> 16, x_c >> 16, r2, g2, b2, r3, g3, b3,
                            z_c, depth_slope);
                    x_b += b_to_c;
                    x_c += c_to_a;
                    r2 += dr2;
                    g2 += dg2;
                    b2 += db2;
                    r3 += dr3;
                    g3 += dg3;
                    b3 += db3;
                    z_c += depth_increment;
                }
                while (--y_b >= 0) {
                    drawShadedScanline(Rasterizer2D.pixels, y_c, x_b >> 16, x_a >> 16, r2, g2, b2, r1, g1, b1,
                            z_c, depth_slope);
                    x_b += b_to_c;
                    x_a += a_to_b;
                    r2 += dr2;
                    g2 += dg2;
                    b2 += db2;
                    r1 += dr1;
                    g1 += dg1;
                    b1 += db1;
                    y_c += Rasterizer2D.width;
                    z_c += depth_increment;
                }
                return;
            }
            y_b -= y_a;
            y_a -= y_c;
            for (y_c = scanOffsets[y_c]; --y_a >= 0; y_c += Rasterizer2D.width) {
                drawShadedScanline(Rasterizer2D.pixels, y_c, x_c >> 16, x_b >> 16, r3, g3, b3, r2, g2, b2,
                        z_c, depth_slope);
                x_b += b_to_c;
                x_c += c_to_a;
                r2 += dr2;
                g2 += dg2;
                b2 += db2;
                r3 += dr3;
                g3 += dg3;
                b3 += db3;
                z_c += depth_increment;
            }
            while (--y_b >= 0) {
                drawShadedScanline(Rasterizer2D.pixels, y_c, x_a >> 16, x_b >> 16, r1, g1, b1, r2, g2, b2,
                        z_c, depth_slope);
                x_b += b_to_c;
                x_a += a_to_b;
                r2 += dr2;
                g2 += dg2;
                b2 += db2;
                r1 += dr1;
                g1 += dg1;
                b1 += db1;
                z_c += depth_increment;
                y_c += Rasterizer2D.width;
            }
            return;
        }
        x_a = x_c <<= 16;
        r1 = r3 <<= 16;
        g1 = g3 <<= 16;
        b1 = b3 <<= 16;
        if (y_c < 0) {
            x_a -= b_to_c * y_c;
            x_c -= c_to_a * y_c;
            r1 -= dr2 * y_c;
            g1 -= dg2 * y_c;
            b1 -= db2 * y_c;
            r3 -= dr3 * y_c;
            g3 -= dg3 * y_c;
            b3 -= db3 * y_c;
            z_c -= depth_increment * y_c;
            y_c = 0;
        }
        x_b <<= 16;
        r2 <<= 16;
        g2 <<= 16;
        b2 <<= 16;
        if (y_b < 0) {
            x_b -= a_to_b * y_b;
            r2 -= dr1 * y_b;
            g2 -= dg1 * y_b;
            b2 -= db1 * y_b;
            y_b = 0;
        }
        if (b_to_c < c_to_a) {
            y_a -= y_b;
            y_b -= y_c;
            for (y_c = scanOffsets[y_c]; --y_b >= 0; y_c += Rasterizer2D.width) {
                drawShadedScanline(Rasterizer2D.pixels, y_c, x_a >> 16, x_c >> 16, r1, g1, b1, r3, g3, b3,
                        z_c, depth_slope);
                x_a += b_to_c;
                x_c += c_to_a;
                r1 += dr2;
                g1 += dg2;
                b1 += db2;
                r3 += dr3;
                g3 += dg3;
                b3 += db3;
                z_c += depth_increment;
            }
            while (--y_a >= 0) {
                drawShadedScanline(Rasterizer2D.pixels, y_c, x_b >> 16, x_c >> 16, r2, g2, b2, r3, g3, b3,
                        z_c, depth_slope);
                x_b += a_to_b;
                x_c += c_to_a;
                r2 += dr1;
                g2 += dg1;
                b2 += db1;
                r3 += dr3;
                g3 += dg3;
                b3 += db3;
                z_c += depth_increment;
                y_c += Rasterizer2D.width;
            }
            return;
        }
        y_a -= y_b;
        y_b -= y_c;
        for (y_c = scanOffsets[y_c]; --y_b >= 0; y_c += Rasterizer2D.width) {
            drawShadedScanline(Rasterizer2D.pixels, y_c, x_c >> 16, x_a >> 16, r3, g3, b3, r1, g1, b1,
                    z_c, depth_slope);
            x_a += b_to_c;
            x_c += c_to_a;
            r1 += dr2;
            g1 += dg2;
            b1 += db2;
            r3 += dr3;
            g3 += dg3;
            b3 += db3;
            z_c += depth_increment;
        }
        while (--y_a >= 0) {
            drawShadedScanline(Rasterizer2D.pixels, y_c, x_c >> 16, x_b >> 16, r3, g3, b3, r2, g2, b2,
                    z_c, depth_slope);
            x_b += a_to_b;
            x_c += c_to_a;
            r2 += dr1;
            g2 += dg1;
            b2 += db1;
            r3 += dr3;
            g3 += dg3;
            b3 += db3;
            y_c += Rasterizer2D.width;
            z_c += depth_increment;
        }
    }

    public static void drawShadedScanline(int[] dest, int offset, int x1, int x2, int r1, int g1,
                                          int b1, int r2, int g2, int b2, float depth, float depth_slope) {
        int n = x2 - x1;
        if (n <= 0) {
            return;
        }
        r2 = (r2 - r1) / n;
        g2 = (g2 - g1) / n;
        b2 = (b2 - b1) / n;
        if (textureOutOfDrawingBounds) {
            if (x2 > Rasterizer2D.lastX) {
                n -= x2 - Rasterizer2D.lastX;
                x2 = Rasterizer2D.lastX;
            }
            if (x1 < 0) {
                n = x2;
                r1 -= x1 * r2;
                g1 -= x1 * g2;
                b1 -= x1 * b2;
                x1 = 0;
            }
        }
        if (x1 < x2) {
            offset += x1;
            depth += depth_slope * x1;
            if (alpha == 0) {
                while (--n >= 0) {
                    if (true) {
                        drawAlpha(
                                dest,
                                offset,
                                (r1 & 0xff0000) | (g1 >> 8 & 0xff00) | (b1 >> 16 & 0xff),
                                255);
                        Rasterizer2D.depthBuffer[offset] = depth;
                    }
                    depth += depth_slope;
                    r1 += r2;
                    g1 += g2;
                    b1 += b2;
                    offset++;
                }
            } else {
                final int a1 = alpha;
                final int a2 = 256 - alpha;
                int rgb;
                int dst;
                while (--n >= 0) {
                    rgb = (r1 & 0xff0000) | (g1 >> 8 & 0xff00) | (b1 >> 16 & 0xff);
                    rgb = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
                    dst = dest[offset];
                    if (true) {
                        drawAlpha(dest, offset, rgb + ((dst & 0xff00ff) * a1 >> 8 & 0xff00ff)
                                + ((dst & 0xff00) * a1 >> 8 & 0xff00), 255);
                        Rasterizer2D.depthBuffer[offset] = depth;
                    }
                    depth += depth_slope;
                    r1 += r2;
                    g1 += g2;
                    b1 += b2;
                    offset++;
                }
            }
        }
    }


    public static void drawFlatTriangle(int y1, int y2, int y3, int x1, int x2, int x3, int rgb, float z1, float z2,
    		float z3) {
        if (!saveDepth) {
            z1 = z2 = z3 = 0;
        }
        int dx1 = 0;
        if (y2 != y1) {
            final int d = (y2 - y1);
            dx1 = (x2 - x1 << 16) / d;
        }
        int dx2 = 0;
        if (y3 != y2) {
            final int d = (y3 - y2);
            dx2 = (x3 - x2 << 16) / d;
        }
        int dx3 = 0;
        if (y3 != y1) {
            final int d = (y1 - y3);
            dx3 = (x1 - x3 << 16) / d;
        }

        float x21 = x2 - x1;
        float y32 = y2 - y1;
        float x31 = x3 - x1;
        float y31 = y3 - y1;
        float z21 = z2 - z1;
        float z31 = z3 - z1;

        float div = x21 * y31 - x31 * y32;
        float depthSlope = (z21 * y31 - z31 * y32) / div;
        float depthScale = (z31 * x21 - z21 * x31) / div;

        if (y1 <= y2 && y1 <= y3) {
            if (y1 >= Rasterizer2D.clip_bottom) {
                return;
            }
            if (y2 > Rasterizer2D.clip_bottom) {
                y2 = Rasterizer2D.clip_bottom;
            }
            if (y3 > Rasterizer2D.clip_bottom) {
                y3 = Rasterizer2D.clip_bottom;
            }
            z1 = z1 - depthSlope * x1 + depthSlope;
            if (y2 < y3) {
                x3 = x1 <<= 16;
                if (y1 < 0) {
                    x3 -= dx3 * y1;
                    x1 -= dx1 * y1;
                    z1 -= depthScale * y1;
                    y1 = 0;
                }
                x2 <<= 16;
                if (y2 < 0) {
                    x2 -= dx2 * y2;
                    y2 = 0;
                }
                if (y1 != y2 && dx3 < dx1 || y1 == y2 && dx3 > dx2) {
                    y3 -= y2;
                    y2 -= y1;
                    for (y1 = scanOffsets[y1]; --y2 >= 0; y1 += Rasterizer2D.width) {
                        drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x3 >> 16, x1 >> 16, z1, depthSlope);
                        z1 += depthScale;
                        x3 += dx3;
                        x1 += dx1;
                    }
                    while (--y3 >= 0) {
                        drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x3 >> 16, x2 >> 16, z1, depthSlope);
                        z1 += depthScale;
                        x3 += dx3;
                        x2 += dx2;
                        y1 += Rasterizer2D.width;
                    }
                    return;
                }
                y3 -= y2;
                y2 -= y1;
                for (y1 = scanOffsets[y1]; --y2 >= 0; y1 += Rasterizer2D.width) {
                    drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x1 >> 16, x3 >> 16, z1, depthSlope);
                    z1 += depthScale;
                    x3 += dx3;
                    x1 += dx1;
                }
                while (--y3 >= 0) {
                    drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x2 >> 16, x3 >> 16, z1, depthSlope);
                    z1 += depthScale;
                    x3 += dx3;
                    x2 += dx2;
                    y1 += Rasterizer2D.width;
                }
                return;
            }
            x2 = x1 <<= 16;
            if (y1 < 0) {
                x2 -= dx3 * y1;
                x1 -= dx1 * y1;
                z1 -= depthScale * y1;
                y1 = 0;
            }
            x3 <<= 16;
            if (y3 < 0) {
                x3 -= dx2 * y3;
                y3 = 0;
            }
            if (y1 != y3 && dx3 < dx1 || y1 == y3 && dx2 > dx1) {
                y2 -= y3;
                y3 -= y1;
                for (y1 = scanOffsets[y1]; --y3 >= 0; y1 += Rasterizer2D.width) {
                    drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x2 >> 16, x1 >> 16, z1, depthSlope);
                    z1 += depthScale;
                    x2 += dx3;
                    x1 += dx1;
                }
                while (--y2 >= 0) {
                    drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x3 >> 16, x1 >> 16, z1, depthSlope);
                    z1 += depthScale;
                    x3 += dx2;
                    x1 += dx1;
                    y1 += Rasterizer2D.width;
                }
                return;
            }
            y2 -= y3;
            y3 -= y1;
            for (y1 = scanOffsets[y1]; --y3 >= 0; y1 += Rasterizer2D.width) {
                drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x1 >> 16, x2 >> 16, z1, depthSlope);
                z1 += depthScale;
                x2 += dx3;
                x1 += dx1;
            }
            while (--y2 >= 0) {
                drawFlatScanline(Rasterizer2D.pixels, y1, rgb, x1 >> 16, x3 >> 16, z1, depthSlope);
                z1 += depthScale;
                x3 += dx2;
                x1 += dx1;
                y1 += Rasterizer2D.width;
            }
            return;
        }
        if (y2 <= y3) {
            if (y2 >= Rasterizer2D.clip_bottom) {
                return;
            }
            if (y3 > Rasterizer2D.clip_bottom) {
                y3 = Rasterizer2D.clip_bottom;
            }
            if (y1 > Rasterizer2D.clip_bottom) {
                y1 = Rasterizer2D.clip_bottom;
            }
            z2 = z2 - depthSlope * x2 + depthSlope;
            if (y3 < y1) {
                x1 = x2 <<= 16;
                if (y2 < 0) {
                    x1 -= dx1 * y2;
                    x2 -= dx2 * y2;
                    z2 -= depthScale * y2;
                    y2 = 0;
                }
                x3 <<= 16;
                if (y3 < 0) {
                    x3 -= dx3 * y3;
                    y3 = 0;
                }
                if (y2 != y3 && dx1 < dx2 || y2 == y3 && dx1 > dx3) {
                    y1 -= y3;
                    y3 -= y2;
                    for (y2 = scanOffsets[y2]; --y3 >= 0; y2 += Rasterizer2D.width) {
                        drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x1 >> 16, x2 >> 16, z2, depthSlope);
                        z2 += depthScale;
                        x1 += dx1;
                        x2 += dx2;
                    }
                    while (--y1 >= 0) {
                        drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x1 >> 16, x3 >> 16, z2, depthSlope);
                        z2 += depthScale;
                        x1 += dx1;
                        x3 += dx3;
                        y2 += Rasterizer2D.width;
                    }
                    return;
                }
                y1 -= y3;
                y3 -= y2;
                for (y2 = scanOffsets[y2]; --y3 >= 0; y2 += Rasterizer2D.width) {
                    drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x2 >> 16, x1 >> 16, z2, depthSlope);
                    z2 += depthScale;
                    x1 += dx1;
                    x2 += dx2;
                }
                while (--y1 >= 0) {
                    drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x3 >> 16, x1 >> 16, z2, depthSlope);
                    z2 += depthScale;
                    x1 += dx1;
                    x3 += dx3;
                    y2 += Rasterizer2D.width;
                }
                return;
            }
            x3 = x2 <<= 16;
            if (y2 < 0) {
                x3 -= dx1 * y2;
                x2 -= dx2 * y2;
                z2 -= depthScale * y2;
                y2 = 0;
            }
            x1 <<= 16;
            if (y1 < 0) {
                x1 -= dx3 * y1;
                y1 = 0;
            }
            if (dx1 < dx2) {
                y3 -= y1;
                y1 -= y2;
                for (y2 = scanOffsets[y2]; --y1 >= 0; y2 += Rasterizer2D.width) {
                    drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x3 >> 16, x2 >> 16, z2, depthSlope);
                    z2 += depthScale;
                    x3 += dx1;
                    x2 += dx2;
                }
                while (--y3 >= 0) {
                    drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x1 >> 16, x2 >> 16, z2, depthSlope);
                    z2 += depthScale;
                    x1 += dx3;
                    x2 += dx2;
                    y2 += Rasterizer2D.width;
                }
                return;
            }
            y3 -= y1;
            y1 -= y2;
            for (y2 = scanOffsets[y2]; --y1 >= 0; y2 += Rasterizer2D.width) {
                drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x2 >> 16, x3 >> 16, z2, depthSlope);
                z2 += depthScale;
                x3 += dx1;
                x2 += dx2;
            }
            while (--y3 >= 0) {
                drawFlatScanline(Rasterizer2D.pixels, y2, rgb, x2 >> 16, x1 >> 16, z2, depthSlope);
                z2 += depthScale;
                x1 += dx3;
                x2 += dx2;
                y2 += Rasterizer2D.width;
            }
            return;
        }
        if (y3 >= Rasterizer2D.clip_bottom) {
            return;
        }
        if (y1 > Rasterizer2D.clip_bottom) {
            y1 = Rasterizer2D.clip_bottom;
        }
        if (y2 > Rasterizer2D.clip_bottom) {
            y2 = Rasterizer2D.clip_bottom;
        }
        z3 = z3 - depthSlope * x3 + depthSlope;
        if (y1 < y2) {
            x2 = x3 <<= 16;
            if (y3 < 0) {
                x2 -= dx2 * y3;
                x3 -= dx3 * y3;
                z3 -= depthScale * y3;
                y3 = 0;
            }
            x1 <<= 16;
            if (y1 < 0) {
                x1 -= dx1 * y1;
                y1 = 0;
            }
            if (dx2 < dx3) {
                y2 -= y1;
                y1 -= y3;
                for (y3 = scanOffsets[y3]; --y1 >= 0; y3 += Rasterizer2D.width) {
                    drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x2 >> 16, x3 >> 16, z3, depthSlope);
                    z3 += depthScale;
                    x2 += dx2;
                    x3 += dx3;
                }
                while (--y2 >= 0) {
                    drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x2 >> 16, x1 >> 16, z3, depthSlope);
                    z3 += depthScale;
                    x2 += dx2;
                    x1 += dx1;
                    y3 += Rasterizer2D.width;
                }
                return;
            }
            y2 -= y1;
            y1 -= y3;
            for (y3 = scanOffsets[y3]; --y1 >= 0; y3 += Rasterizer2D.width) {
                drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x3 >> 16, x2 >> 16, z3, depthSlope);
                z3 += depthScale;
                x2 += dx2;
                x3 += dx3;
            }
            while (--y2 >= 0) {
                drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x1 >> 16, x2 >> 16, z3, depthSlope);
                z3 += depthScale;
                x2 += dx2;
                x1 += dx1;
                y3 += Rasterizer2D.width;
            }
            return;
        }
        x1 = x3 <<= 16;
        if (y3 < 0) {
            x1 -= dx2 * y3;
            x3 -= dx3 * y3;
            z3 -= depthScale * y3;
            y3 = 0;
        }
        x2 <<= 16;
        if (y2 < 0) {
            x2 -= dx1 * y2;
            y2 = 0;
        }
        if (dx2 < dx3) {
            y1 -= y2;
            y2 -= y3;
            for (y3 = scanOffsets[y3]; --y2 >= 0; y3 += Rasterizer2D.width) {
                drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x1 >> 16, x3 >> 16, z3, depthSlope);
                z3 += depthScale;
                x1 += dx2;
                x3 += dx3;
            }
            while (--y1 >= 0) {
                drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x2 >> 16, x3 >> 16, z3, depthSlope);
                z3 += depthScale;
                x2 += dx1;
                x3 += dx3;
                y3 += Rasterizer2D.width;
            }
            return;
        }
        y1 -= y2;
        y2 -= y3;
        for (y3 = scanOffsets[y3]; --y2 >= 0; y3 += Rasterizer2D.width) {
            drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x3 >> 16, x1 >> 16, z3, depthSlope);
            z3 += depthScale;
            x1 += dx2;
            x3 += dx3;
        }
        while (--y1 >= 0) {
            drawFlatScanline(Rasterizer2D.pixels, y3, rgb, x3 >> 16, x2 >> 16, z3, depthSlope);
            z3 += depthScale;
            x2 += dx1;
            x3 += dx3;
            y3 += Rasterizer2D.width;
        }
    }

    private static void drawFlatScanline(int[] dest, int offset, int rgb, int x1, int x2, float z1, float z2) {
        if (x1 >= x2) {
            return;
        }
        if (textureOutOfDrawingBounds) {
            if (x2 > Rasterizer2D.lastX) {
                x2 = Rasterizer2D.lastX;
            }
            if (x1 < 0) {
                x1 = 0;
            }
        }
        if (x1 >= x2) {
            return;
        }
        offset += x1;
        z1 += z2 * x1;
        int n = x2 - x1;
        if (alpha == 0) {
            while (--n >= 0) {
                dest[offset] = rgb;
                if (saveDepth) {
                    depthBuffer[offset] = z1;
                }
                z1 += z2;
                offset++;
            }
        } else {
            final int a1 = alpha;
            final int a2 = 256 - alpha;
            rgb = ((rgb & 0xff00ff) * a2 >> 8 & 0xff00ff) + ((rgb & 0xff00) * a2 >> 8 & 0xff00);
            while (--n >= 0) {
                dest[offset] = rgb + ((dest[offset] & 0xff00ff) * a1 >> 8 & 0xff00ff)
                        + ((dest[offset] & 0xff00) * a1 >> 8 & 0xff00);
                if (saveDepth) {
                    depthBuffer[offset] = z1;
                }
                z1 += z2;
                offset++;
            }
        }
    }

    public static void drawTexturedTriangle(int var0, int var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, int var14, int var15, int var16, int var17, int var18) {
        int[] texturePixels = getTexturePixels(var18);
        int var21;
        aBoolean1463 = !textureIsTransparant[var18];
        var21 = var4 - var3;
        int var26 = var1 - var0;
        int var27 = var5 - var3;
        int var31 = var2 - var0;
        int var28 = var7 - var6;
        int var23 = var8 - var6;
        int var29 = 0;
        if(var1 != var0) {
            var29 = (var4 - var3 << 16) / (var1 - var0);
        }

        int var30 = 0;
        if(var2 != var1) {
            var30 = (var5 - var4 << 16) / (var2 - var1);
        }

        int var22 = 0;
        if(var2 != var0) {
            var22 = (var3 - var5 << 16) / (var0 - var2);
        }

        int var32 = var21 * var31 - var27 * var26;
        if(var32 != 0) {
            int var41 = (var28 * var31 - var23 * var26 << 9) / var32;
            int var20 = (var23 * var21 - var28 * var27 << 9) / var32;
            var10 = var9 - var10;
            var13 = var12 - var13;
            var16 = var15 - var16;
            var11 -= var9;
            var14 -= var12;
            var17 -= var15;
            final int FOV = (aBoolean1464 ? Rasterizer3D.fieldOfView : 512);
            int var24 = var11 * var12 - var14 * var9 << 14;
            int var38 = (int)(((long)(var14 * var15 - var17 * var12) << 3 << 14) / (long)FOV);
            int var25 = (int)(((long)(var17 * var9 - var11 * var15) << 14) / (long)FOV);
            int var36 = var10 * var12 - var13 * var9 << 14;
            int var39 = (int)(((long)(var13 * var15 - var16 * var12) << 3 << 14) / (long)FOV);
            int var37 = (int)(((long)(var16 * var9 - var10 * var15) << 14) / (long)FOV);
            int var33 = var13 * var11 - var10 * var14 << 14;
            int var40 = (int)(((long)(var16 * var14 - var13 * var17) << 3 << 14) / (long)FOV);
            int var34 = (int)(((long)(var10 * var17 - var16 * var11) << 14) / (long)FOV);


            int var35;
            if(var0 <= var1 && var0 <= var2) {
                if(var0 < Rasterizer2D.clip_bottom) {
                    if(var1 > Rasterizer2D.clip_bottom) {
                        var1 = Rasterizer2D.clip_bottom;
                    }

                    if(var2 > Rasterizer2D.clip_bottom) {
                        var2 = Rasterizer2D.clip_bottom;
                    }

                    var6 = (var6 << 9) - var41 * var3 + var41;
                    if(var1 < var2) {
                        var5 = var3 <<= 16;
                        if(var0 < 0) {
                            var5 -= var22 * var0;
                            var3 -= var29 * var0;
                            var6 -= var20 * var0;
                            var0 = 0;
                        }

                        var4 <<= 16;
                        if(var1 < 0) {
                            var4 -= var30 * var1;
                            var1 = 0;
                        }

                        var35 = var0 - originViewY;
                        var24 += var25 * var35;
                        var36 += var37 * var35;
                        var33 += var34 * var35;
                        if((var0 == var1 || var22 >= var29) && (var0 != var1 || var22 <= var30)) {
                            var2 -= var1;
                            var1 -= var0;
                            var0 = scanOffsets[var0];

                            while(true) {
                                --var1;
                                if(var1 < 0) {
                                    while(true) {
                                        --var2;
                                        if(var2 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var4 >> 16, var5 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                        var5 += var22;
                                        var4 += var30;
                                        var6 += var20;
                                        var0 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var3 >> 16, var5 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                var5 += var22;
                                var3 += var29;
                                var6 += var20;
                                var0 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        } else {
                            var2 -= var1;
                            var1 -= var0;
                            var0 = scanOffsets[var0];

                            while(true) {
                                --var1;
                                if(var1 < 0) {
                                    while(true) {
                                        --var2;
                                        if(var2 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var5 >> 16, var4 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                        var5 += var22;
                                        var4 += var30;
                                        var6 += var20;
                                        var0 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var5 >> 16, var3 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                var5 += var22;
                                var3 += var29;
                                var6 += var20;
                                var0 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        }
                    } else {
                        var4 = var3 <<= 16;
                        if(var0 < 0) {
                            var4 -= var22 * var0;
                            var3 -= var29 * var0;
                            var6 -= var20 * var0;
                            var0 = 0;
                        }

                        var5 <<= 16;
                        if(var2 < 0) {
                            var5 -= var30 * var2;
                            var2 = 0;
                        }

                        var35 = var0 - originViewY;
                        var24 += var25 * var35;
                        var36 += var37 * var35;
                        var33 += var34 * var35;
                        if((var0 == var2 || var22 >= var29) && (var0 != var2 || var30 <= var29)) {
                            var1 -= var2;
                            var2 -= var0;
                            var0 = scanOffsets[var0];

                            while(true) {
                                --var2;
                                if(var2 < 0) {
                                    while(true) {
                                        --var1;
                                        if(var1 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var3 >> 16, var5 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                        var5 += var30;
                                        var3 += var29;
                                        var6 += var20;
                                        var0 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var3 >> 16, var4 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                var4 += var22;
                                var3 += var29;
                                var6 += var20;
                                var0 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        } else {
                            var1 -= var2;
                            var2 -= var0;
                            var0 = scanOffsets[var0];

                            while(true) {
                                --var2;
                                if(var2 < 0) {
                                    while(true) {
                                        --var1;
                                        if(var1 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var5 >> 16, var3 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                        var5 += var30;
                                        var3 += var29;
                                        var6 += var20;
                                        var0 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var0, var4 >> 16, var3 >> 16, var6, var41, var24, var36, var33, var38, var39, var40);
                                var4 += var22;
                                var3 += var29;
                                var6 += var20;
                                var0 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        }
                    }
                }
            } else if(var1 <= var2) {
                if(var1 < Rasterizer2D.clip_bottom) {
                    if(var2 > Rasterizer2D.clip_bottom) {
                        var2 = Rasterizer2D.clip_bottom;
                    }

                    if(var0 > Rasterizer2D.clip_bottom) {
                        var0 = Rasterizer2D.clip_bottom;
                    }

                    var7 = (var7 << 9) - var41 * var4 + var41;
                    if(var2 < var0) {
                        var3 = var4 <<= 16;
                        if(var1 < 0) {
                            var3 -= var29 * var1;
                            var4 -= var30 * var1;
                            var7 -= var20 * var1;
                            var1 = 0;
                        }

                        var5 <<= 16;
                        if(var2 < 0) {
                            var5 -= var22 * var2;
                            var2 = 0;
                        }

                        var35 = var1 - originViewY;
                        var24 += var25 * var35;
                        var36 += var37 * var35;
                        var33 += var34 * var35;
                        if((var1 == var2 || var29 >= var30) && (var1 != var2 || var29 <= var22)) {
                            var0 -= var2;
                            var2 -= var1;
                            var1 = scanOffsets[var1];

                            while(true) {
                                --var2;
                                if(var2 < 0) {
                                    while(true) {
                                        --var0;
                                        if(var0 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var5 >> 16, var3 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                        var3 += var29;
                                        var5 += var22;
                                        var7 += var20;
                                        var1 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var4 >> 16, var3 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                var3 += var29;
                                var4 += var30;
                                var7 += var20;
                                var1 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        } else {
                            var0 -= var2;
                            var2 -= var1;
                            var1 = scanOffsets[var1];

                            while(true) {
                                --var2;
                                if(var2 < 0) {
                                    while(true) {
                                        --var0;
                                        if(var0 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var3 >> 16, var5 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                        var3 += var29;
                                        var5 += var22;
                                        var7 += var20;
                                        var1 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var3 >> 16, var4 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                var3 += var29;
                                var4 += var30;
                                var7 += var20;
                                var1 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        }
                    } else {
                        var5 = var4 <<= 16;
                        if(var1 < 0) {
                            var5 -= var29 * var1;
                            var4 -= var30 * var1;
                            var7 -= var20 * var1;
                            var1 = 0;
                        }

                        var3 <<= 16;
                        if(var0 < 0) {
                            var3 -= var22 * var0;
                            var0 = 0;
                        }

                        var35 = var1 - originViewY;
                        var24 += var25 * var35;
                        var36 += var37 * var35;
                        var33 += var34 * var35;
                        if(var29 < var30) {
                            var2 -= var0;
                            var0 -= var1;
                            var1 = scanOffsets[var1];

                            while(true) {
                                --var0;
                                if(var0 < 0) {
                                    while(true) {
                                        --var2;
                                        if(var2 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var3 >> 16, var4 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                        var3 += var22;
                                        var4 += var30;
                                        var7 += var20;
                                        var1 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var5 >> 16, var4 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                var5 += var29;
                                var4 += var30;
                                var7 += var20;
                                var1 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        } else {
                            var2 -= var0;
                            var0 -= var1;
                            var1 = scanOffsets[var1];

                            while(true) {
                                --var0;
                                if(var0 < 0) {
                                    while(true) {
                                        --var2;
                                        if(var2 < 0) {
                                            return;
                                        }

                                        drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var4 >> 16, var3 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                        var3 += var22;
                                        var4 += var30;
                                        var7 += var20;
                                        var1 += Rasterizer2D.width;
                                        var24 += var25;
                                        var36 += var37;
                                        var33 += var34;
                                    }
                                }

                                drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var1, var4 >> 16, var5 >> 16, var7, var41, var24, var36, var33, var38, var39, var40);
                                var5 += var29;
                                var4 += var30;
                                var7 += var20;
                                var1 += Rasterizer2D.width;
                                var24 += var25;
                                var36 += var37;
                                var33 += var34;
                            }
                        }
                    }
                }
            } else if(var2 < Rasterizer2D.clip_bottom) {
                if(var0 > Rasterizer2D.clip_bottom) {
                    var0 = Rasterizer2D.clip_bottom;
                }

                if(var1 > Rasterizer2D.clip_bottom) {
                    var1 = Rasterizer2D.clip_bottom;
                }

                var8 = (var8 << 9) - var41 * var5 + var41;
                if(var0 < var1) {
                    var4 = var5 <<= 16;
                    if(var2 < 0) {
                        var4 -= var30 * var2;
                        var5 -= var22 * var2;
                        var8 -= var20 * var2;
                        var2 = 0;
                    }

                    var3 <<= 16;
                    if(var0 < 0) {
                        var3 -= var29 * var0;
                        var0 = 0;
                    }

                    var35 = var2 - originViewY;
                    var24 += var25 * var35;
                    var36 += var37 * var35;
                    var33 += var34 * var35;
                    if(var30 < var22) {
                        var1 -= var0;
                        var0 -= var2;
                        var2 = scanOffsets[var2];

                        while(true) {
                            --var0;
                            if(var0 < 0) {
                                while(true) {
                                    --var1;
                                    if(var1 < 0) {
                                        return;
                                    }

                                    drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var4 >> 16, var3 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                                    var4 += var30;
                                    var3 += var29;
                                    var8 += var20;
                                    var2 += Rasterizer2D.width;
                                    var24 += var25;
                                    var36 += var37;
                                    var33 += var34;
                                }
                            }

                            drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var4 >> 16, var5 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                            var4 += var30;
                            var5 += var22;
                            var8 += var20;
                            var2 += Rasterizer2D.width;
                            var24 += var25;
                            var36 += var37;
                            var33 += var34;
                        }
                    } else {
                        var1 -= var0;
                        var0 -= var2;
                        var2 = scanOffsets[var2];

                        while(true) {
                            --var0;
                            if(var0 < 0) {
                                while(true) {
                                    --var1;
                                    if(var1 < 0) {
                                        return;
                                    }

                                    drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var3 >> 16, var4 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                                    var4 += var30;
                                    var3 += var29;
                                    var8 += var20;
                                    var2 += Rasterizer2D.width;
                                    var24 += var25;
                                    var36 += var37;
                                    var33 += var34;
                                }
                            }

                            drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var5 >> 16, var4 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                            var4 += var30;
                            var5 += var22;
                            var8 += var20;
                            var2 += Rasterizer2D.width;
                            var24 += var25;
                            var36 += var37;
                            var33 += var34;
                        }
                    }
                } else {
                    var3 = var5 <<= 16;
                    if(var2 < 0) {
                        var3 -= var30 * var2;
                        var5 -= var22 * var2;
                        var8 -= var20 * var2;
                        var2 = 0;
                    }

                    var4 <<= 16;
                    if(var1 < 0) {
                        var4 -= var29 * var1;
                        var1 = 0;
                    }

                    var35 = var2 - originViewY;
                    var24 += var25 * var35;
                    var36 += var37 * var35;
                    var33 += var34 * var35;
                    if(var30 < var22) {
                        var0 -= var1;
                        var1 -= var2;
                        var2 = scanOffsets[var2];

                        while(true) {
                            --var1;
                            if(var1 < 0) {
                                while(true) {
                                    --var0;
                                    if(var0 < 0) {
                                        return;
                                    }

                                    drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var4 >> 16, var5 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                                    var4 += var29;
                                    var5 += var22;
                                    var8 += var20;
                                    var2 += Rasterizer2D.width;
                                    var24 += var25;
                                    var36 += var37;
                                    var33 += var34;
                                }
                            }

                            drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var3 >> 16, var5 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                            var3 += var30;
                            var5 += var22;
                            var8 += var20;
                            var2 += Rasterizer2D.width;
                            var24 += var25;
                            var36 += var37;
                            var33 += var34;
                        }
                    } else {
                        var0 -= var1;
                        var1 -= var2;
                        var2 = scanOffsets[var2];

                        while(true) {
                            --var1;
                            if(var1 < 0) {
                                while(true) {
                                    --var0;
                                    if(var0 < 0) {
                                        return;
                                    }

                                    drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var5 >> 16, var4 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                                    var4 += var29;
                                    var5 += var22;
                                    var8 += var20;
                                    var2 += Rasterizer2D.width;
                                    var24 += var25;
                                    var36 += var37;
                                    var33 += var34;
                                }
                            }

                            drawTexturedLine(Rasterizer2D.pixels, texturePixels, 0, 0, var2, var5 >> 16, var3 >> 16, var8, var41, var24, var36, var33, var38, var39, var40);
                            var3 += var30;
                            var5 += var22;
                            var8 += var20;
                            var2 += Rasterizer2D.width;
                            var24 += var25;
                            var36 += var37;
                            var33 += var34;
                        }
                    }
                }
            }
        }
    }

    static void drawTexturedLine(int[] var0, int[] var1, int var2, int var3, int var4, int var5, int var6, int var7, int var8, int var9, int var10, int var11, int var12, int var13, int var14) {
        if(textureOutOfDrawingBounds) {
            if(var6 > Rasterizer2D.lastX) {
                var6 = Rasterizer2D.lastX;
            }

            if(var5 < 0) {
                var5 = 0;
            }
        }

        if(var5 < var6) {
            var4 += var5;
            var7 += var8 * var5;
            int var17 = var6 - var5;
            int var15;
            int var16;
            int var18;
            int var19;
            int var20;
            int var21;
            int var22;
            int var23;
            if(false) {
                var15 = var5 - originViewX;
                var9 += (var12 >> 3) * var15;
                var10 += (var13 >> 3) * var15;
                var11 += (var14 >> 3) * var15;
                var19 = var11 >> 12;
                if(var19 != 0) {
                    var20 = var9 / var19;
                    var18 = var10 / var19;
                    if(var20 < 0) {
                        var20 = 0;
                    } else if(var20 > 4032) {
                        var20 = 4032;
                    }
                } else {
                    var20 = 0;
                    var18 = 0;
                }

                var9 += var12;
                var10 += var13;
                var11 += var14;
                var19 = var11 >> 12;
                if(var19 != 0) {
                    var22 = var9 / var19;
                    var16 = var10 / var19;
                    if(var22 < 0) {
                        var22 = 0;
                    } else if(var22 > 4032) {
                        var22 = 4032;
                    }
                } else {
                    var22 = 0;
                    var16 = 0;
                }

                var2 = (var20 << 20) + var18;
                var23 = (var22 - var20 >> 3 << 20) + (var16 - var18 >> 3);
                var17 >>= 3;
                var8 <<= 3;
                var21 = var7 >> 8;
                if(aBoolean1463) {
                    if(var17 > 0) {
                        do {
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var20 = var22;
                            var18 = var16;
                            var9 += var12;
                            var10 += var13;
                            var11 += var14;
                            var19 = var11 >> 12;
                            if(var19 != 0) {
                                var22 = var9 / var19;
                                var16 = var10 / var19;
                                if(var22 < 0) {
                                    var22 = 0;
                                } else if(var22 > 4032) {
                                    var22 = 4032;
                                }
                            } else {
                                var22 = 0;
                                var16 = 0;
                            }

                            var2 = (var20 << 20) + var18;
                            var23 = (var22 - var20 >> 3 << 20) + (var16 - var18 >> 3);
                            var7 += var8;
                            var21 = var7 >> 8;
                            --var17;
                        } while(var17 > 0);
                    }

                    var17 = var6 - var5 & 7;
                    if(var17 > 0) {
                        do {
                            var3 = var1[(var2 & 4032) + (var2 >>> 26)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            --var17;
                        } while(var17 > 0);

                    }
                } else {
                    if(var17 > 0) {
                        do {
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var20 = var22;
                            var18 = var16;
                            var9 += var12;
                            var10 += var13;
                            var11 += var14;
                            var19 = var11 >> 12;
                            if(var19 != 0) {
                                var22 = var9 / var19;
                                var16 = var10 / var19;
                                if(var22 < 0) {
                                    var22 = 0;
                                } else if(var22 > 4032) {
                                    var22 = 4032;
                                }
                            } else {
                                var22 = 0;
                                var16 = 0;
                            }

                            var2 = (var20 << 20) + var18;
                            var23 = (var22 - var20 >> 3 << 20) + (var16 - var18 >> 3);
                            var7 += var8;
                            var21 = var7 >> 8;
                            --var17;
                        } while(var17 > 0);
                    }

                    var17 = var6 - var5 & 7;
                    if(var17 > 0) {
                        do {
                            if((var3 = var1[(var2 & 4032) + (var2 >>> 26)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            --var17;
                        } while(var17 > 0);

                    }
                }
            } else {
                var15 = var5 - originViewX;
                var9 += (var12 >> 3) * var15;
                var10 += (var13 >> 3) * var15;
                var11 += (var14 >> 3) * var15;
                var19 = var11 >> 14;
                if(var19 != 0) {
                    var20 = var9 / var19;
                    var18 = var10 / var19;
                    if(var20 < 0) {
                        var20 = 0;
                    } else if(var20 > 16256) {
                        var20 = 16256;
                    }
                } else {
                    var20 = 0;
                    var18 = 0;
                }

                var9 += var12;
                var10 += var13;
                var11 += var14;
                var19 = var11 >> 14;
                if(var19 != 0) {
                    var22 = var9 / var19;
                    var16 = var10 / var19;
                    if(var22 < 0) {
                        var22 = 0;
                    } else if(var22 > 16256) {
                        var22 = 16256;
                    }
                } else {
                    var22 = 0;
                    var16 = 0;
                }

                var2 = (var20 << 18) + var18;
                var23 = (var22 - var20 >> 3 << 18) + (var16 - var18 >> 3);
                var17 >>= 3;
                var8 <<= 3;
                var21 = var7 >> 8;
                if(aBoolean1463) {
                    if(var17 > 0) {
                        do {
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var20 = var22;
                            var18 = var16;
                            var9 += var12;
                            var10 += var13;
                            var11 += var14;
                            var19 = var11 >> 14;
                            if(var19 != 0) {
                                var22 = var9 / var19;
                                var16 = var10 / var19;
                                if(var22 < 0) {
                                    var22 = 0;
                                } else if(var22 > 16256) {
                                    var22 = 16256;
                                }
                            } else {
                                var22 = 0;
                                var16 = 0;
                            }

                            var2 = (var20 << 18) + var18;
                            var23 = (var22 - var20 >> 3 << 18) + (var16 - var18 >> 3);
                            var7 += var8;
                            var21 = var7 >> 8;
                            --var17;
                        } while(var17 > 0);
                    }

                    var17 = var6 - var5 & 7;
                    if(var17 > 0) {
                        do {
                            var3 = var1[(var2 & 16256) + (var2 >>> 25)];
                            var0[var4++] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            var2 += var23;
                            --var17;
                        } while(var17 > 0);

                    }
                } else {
                    if(var17 > 0) {
                        do {
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var20 = var22;
                            var18 = var16;
                            var9 += var12;
                            var10 += var13;
                            var11 += var14;
                            var19 = var11 >> 14;
                            if(var19 != 0) {
                                var22 = var9 / var19;
                                var16 = var10 / var19;
                                if(var22 < 0) {
                                    var22 = 0;
                                } else if(var22 > 16256) {
                                    var22 = 16256;
                                }
                            } else {
                                var22 = 0;
                                var16 = 0;
                            }

                            var2 = (var20 << 18) + var18;
                            var23 = (var22 - var20 >> 3 << 18) + (var16 - var18 >> 3);
                            var7 += var8;
                            var21 = var7 >> 8;
                            --var17;
                        } while(var17 > 0);
                    }

                    var17 = var6 - var5 & 7;
                    if(var17 > 0) {
                        do {
                            if((var3 = var1[(var2 & 16256) + (var2 >>> 25)]) != 0) {
                                var0[var4] = ((var3 & 16711935) * var21 & -16711936) + ((var3 & '\uff00') * var21 & 16711680) >> 8;
                            }

                            ++var4;
                            var2 += var23;
                            --var17;
                        } while(var17 > 0);

                    }
                }
            }
        }
    }

    public static void drawTexturedTriangle2(int y_a, int y_b, int y_c, int x_a, int x_b, int x_c,
                                            int k1, int l1, int i2, int Px, int Mx, int Nx, int Pz, int Mz, int Nz, int Py, int My,
                                            int Ny, int k4, float z_a, float z_b, float z_c) {
        if (z_a < 0 || z_b < 0 || z_c < 0) {
            return;
        }
        int texture[] = getTexturePixels(k4);
        aBoolean1463 = !textureIsTransparant[k4];
        Mx = Px - Mx;
        Mz = Pz - Mz;
        My = Py - My;
        Nx -= Px;
        Nz -= Pz;
        Ny -= Py;
        int Oa = (Nx * Pz - Nz * Px) * Rasterizer3D.fieldOfView << 5;
        int Ha = Nz * Py - Ny * Pz << 8;
        int Va = Ny * Px - Nx * Py << 5;

        int Ob = (Mx * Pz - Mz * Px) * Rasterizer3D.fieldOfView << 5;
        int Hb = Mz * Py - My * Pz << 8;
        int Vb = My * Px - Mx * Py << 5;

        int Oc = (Mz * Nx - Mx * Nz) * Rasterizer3D.fieldOfView << 5;
        int Hc = My * Nz - Mz * Ny << 8;
        int Vc = Mx * Ny - My * Nx << 5;
        int a_to_b = 0;
        int grad_a_off = 0;
        if (y_b != y_a) {
            a_to_b = (x_b - x_a << 16) / (y_b - y_a);
            grad_a_off = (l1 - k1 << 16) / (y_b - y_a);
        }
        int b_to_c = 0;
        int grad_b_off = 0;
        if (y_c != y_b) {
            b_to_c = (x_c - x_b << 16) / (y_c - y_b);
            grad_b_off = (i2 - l1 << 16) / (y_c - y_b);
        }
        int c_to_a = 0;
        int grad_c_off = 0;
        if (y_c != y_a) {
            c_to_a = (x_a - x_c << 16) / (y_a - y_c);
            grad_c_off = (k1 - i2 << 16) / (y_a - y_c);
        }
        float b_aX = x_b - x_a;
        float b_aY = y_b - y_a;
        float c_aX = x_c - x_a;
        float c_aY = y_c - y_a;
        float b_aZ = z_b - z_a;
        float c_aZ = z_c - z_a;

        float div = b_aX * c_aY - c_aX * b_aY;
        float depth_slope = (b_aZ * c_aY - c_aZ * b_aY) / div;
        float depth_increment = (c_aZ * b_aX - b_aZ * c_aX) / div;
        if (y_a <= y_b && y_a <= y_c) {
            if (y_a >= Rasterizer2D.clip_bottom) {
                return;
            }
            if (y_b > Rasterizer2D.clip_bottom) {
                y_b = Rasterizer2D.clip_bottom;
            }
            if (y_c > Rasterizer2D.clip_bottom) {
                y_c = Rasterizer2D.clip_bottom;
            }
            z_a = z_a - depth_slope * x_a + depth_slope;
            if (y_b < y_c) {
                x_c = x_a <<= 16;
                i2 = k1 <<= 16;
                if (y_a < 0) {
                    x_c -= c_to_a * y_a;
                    x_a -= a_to_b * y_a;
                    z_a -= depth_increment * y_a;
                    i2 -= grad_c_off * y_a;
                    k1 -= grad_a_off * y_a;
                    y_a = 0;
                }
                x_b <<= 16;
                l1 <<= 16;
                if (y_b < 0) {
                    x_b -= b_to_c * y_b;
                    l1 -= grad_b_off * y_b;
                    y_b = 0;
                }
                int k8 = y_a - originViewY;
                Oa += Va * k8;
                Ob += Vb * k8;
                Oc += Vc * k8;
                if (y_a != y_b && c_to_a < a_to_b || y_a == y_b && c_to_a > b_to_c) {
                    y_c -= y_b;
                    y_b -= y_a;
                    y_a = scanOffsets[y_a];
                    while (--y_b >= 0) {
                        drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_c >> 16, x_a >> 16, i2 >> 8,
                                k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                        x_c += c_to_a;
                        x_a += a_to_b;
                        z_a += depth_increment;
                        i2 += grad_c_off;
                        k1 += grad_a_off;
                        y_a += Rasterizer2D.width;
                        Oa += Va;
                        Ob += Vb;
                        Oc += Vc;
                    }
                    while (--y_c >= 0) {
                        drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_c >> 16, x_b >> 16, i2 >> 8,
                                l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                        x_c += c_to_a;
                        x_b += b_to_c;
                        z_a += depth_increment;
                        i2 += grad_c_off;
                        l1 += grad_b_off;
                        y_a += Rasterizer2D.width;
                        Oa += Va;
                        Ob += Vb;
                        Oc += Vc;
                    }
                    return;
                }
                y_c -= y_b;
                y_b -= y_a;
                y_a = scanOffsets[y_a];
                while (--y_b >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_a >> 16, x_c >> 16, k1 >> 8,
                            i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                    x_c += c_to_a;
                    x_a += a_to_b;
                    z_a += depth_increment;
                    i2 += grad_c_off;
                    k1 += grad_a_off;
                    y_a += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                while (--y_c >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_b >> 16, x_c >> 16, l1 >> 8,
                            i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                    x_c += c_to_a;
                    x_b += b_to_c;
                    z_a += depth_increment;
                    i2 += grad_c_off;
                    l1 += grad_b_off;
                    y_a += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                return;
            }
            x_b = x_a <<= 16;
            l1 = k1 <<= 16;
            if (y_a < 0) {
                x_b -= c_to_a * y_a;
                x_a -= a_to_b * y_a;
                z_a -= depth_increment * y_a;
                l1 -= grad_c_off * y_a;
                k1 -= grad_a_off * y_a;
                y_a = 0;
            }
            x_c <<= 16;
            i2 <<= 16;
            if (y_c < 0) {
                x_c -= b_to_c * y_c;
                i2 -= grad_b_off * y_c;
                y_c = 0;
            }
            int l8 = y_a - originViewY;
            Oa += Va * l8;
            Ob += Vb * l8;
            Oc += Vc * l8;
            if (y_a != y_c && c_to_a < a_to_b || y_a == y_c && b_to_c > a_to_b) {
                y_b -= y_c;
                y_c -= y_a;
                y_a = scanOffsets[y_a];
                while (--y_c >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_b >> 16, x_a >> 16, l1 >> 8,
                            k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                    x_b += c_to_a;
                    x_a += a_to_b;
                    l1 += grad_c_off;
                    k1 += grad_a_off;
                    z_a += depth_increment;
                    y_a += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                while (--y_b >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_c >> 16, x_a >> 16, i2 >> 8,
                            k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                    x_c += b_to_c;
                    x_a += a_to_b;
                    i2 += grad_b_off;
                    k1 += grad_a_off;
                    z_a += depth_increment;
                    y_a += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                return;
            }
            y_b -= y_c;
            y_c -= y_a;
            y_a = scanOffsets[y_a];
            while (--y_c >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_a >> 16, x_b >> 16, k1 >> 8,
                        l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                x_b += c_to_a;
                x_a += a_to_b;
                l1 += grad_c_off;
                k1 += grad_a_off;
                z_a += depth_increment;
                y_a += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            while (--y_b >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_a, x_a >> 16, x_c >> 16, k1 >> 8,
                        i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_a, depth_slope);
                x_c += b_to_c;
                x_a += a_to_b;
                i2 += grad_b_off;
                k1 += grad_a_off;
                z_a += depth_increment;
                y_a += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            return;
        }
        if (y_b <= y_c) {
            if (y_b >= Rasterizer2D.clip_bottom) {
                return;
            }
            if (y_c > Rasterizer2D.clip_bottom) {
                y_c = Rasterizer2D.clip_bottom;
            }
            if (y_a > Rasterizer2D.clip_bottom) {
                y_a = Rasterizer2D.clip_bottom;
            }
            z_b = z_b - depth_slope * x_b + depth_slope;
            if (y_c < y_a) {
                x_a = x_b <<= 16;
                k1 = l1 <<= 16;
                if (y_b < 0) {
                    x_a -= a_to_b * y_b;
                    x_b -= b_to_c * y_b;
                    z_b -= depth_increment * y_b;
                    k1 -= grad_a_off * y_b;
                    l1 -= grad_b_off * y_b;
                    y_b = 0;
                }
                x_c <<= 16;
                i2 <<= 16;
                if (y_c < 0) {
                    x_c -= c_to_a * y_c;
                    i2 -= grad_c_off * y_c;
                    y_c = 0;
                }
                int i9 = y_b - originViewY;
                Oa += Va * i9;
                Ob += Vb * i9;
                Oc += Vc * i9;
                if (y_b != y_c && a_to_b < b_to_c || y_b == y_c && a_to_b > c_to_a) {
                    y_a -= y_c;
                    y_c -= y_b;
                    y_b = scanOffsets[y_b];
                    while (--y_c >= 0) {
                        drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_a >> 16, x_b >> 16, k1 >> 8,
                                l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                        x_a += a_to_b;
                        x_b += b_to_c;
                        k1 += grad_a_off;
                        l1 += grad_b_off;
                        z_b += depth_increment;
                        y_b += Rasterizer2D.width;
                        Oa += Va;
                        Ob += Vb;
                        Oc += Vc;
                    }
                    while (--y_a >= 0) {
                        drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_a >> 16, x_c >> 16, k1 >> 8,
                                i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                        x_a += a_to_b;
                        x_c += c_to_a;
                        k1 += grad_a_off;
                        i2 += grad_c_off;
                        z_b += depth_increment;
                        y_b += Rasterizer2D.width;
                        Oa += Va;
                        Ob += Vb;
                        Oc += Vc;
                    }
                    return;
                }
                y_a -= y_c;
                y_c -= y_b;
                y_b = scanOffsets[y_b];
                while (--y_c >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_b >> 16, x_a >> 16, l1 >> 8,
                            k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                    x_a += a_to_b;
                    x_b += b_to_c;
                    k1 += grad_a_off;
                    l1 += grad_b_off;
                    z_b += depth_increment;
                    y_b += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                while (--y_a >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_c >> 16, x_a >> 16, i2 >> 8,
                            k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                    x_a += a_to_b;
                    x_c += c_to_a;
                    k1 += grad_a_off;
                    i2 += grad_c_off;
                    z_b += depth_increment;
                    y_b += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                return;
            }
            x_c = x_b <<= 16;
            i2 = l1 <<= 16;
            if (y_b < 0) {
                x_c -= a_to_b * y_b;
                x_b -= b_to_c * y_b;
                z_b -= depth_increment * y_b;
                i2 -= grad_a_off * y_b;
                l1 -= grad_b_off * y_b;
                y_b = 0;
            }
            x_a <<= 16;
            k1 <<= 16;
            if (y_a < 0) {
                x_a -= c_to_a * y_a;
                k1 -= grad_c_off * y_a;
                y_a = 0;
            }
            int j9 = y_b - originViewY;
            Oa += Va * j9;
            Ob += Vb * j9;
            Oc += Vc * j9;
            if (a_to_b < b_to_c) {
                y_c -= y_a;
                y_a -= y_b;
                y_b = scanOffsets[y_b];
                while (--y_a >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_c >> 16, x_b >> 16, i2 >> 8,
                            l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                    x_c += a_to_b;
                    x_b += b_to_c;
                    i2 += grad_a_off;
                    l1 += grad_b_off;
                    z_b += depth_increment;
                    y_b += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                while (--y_c >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_a >> 16, x_b >> 16, k1 >> 8,
                            l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                    x_a += c_to_a;
                    x_b += b_to_c;
                    k1 += grad_c_off;
                    l1 += grad_b_off;
                    z_b += depth_increment;
                    y_b += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                return;
            }
            y_c -= y_a;
            y_a -= y_b;
            y_b = scanOffsets[y_b];
            while (--y_a >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_b >> 16, x_c >> 16, l1 >> 8,
                        i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                x_c += a_to_b;
                x_b += b_to_c;
                i2 += grad_a_off;
                l1 += grad_b_off;
                z_b += depth_increment;
                y_b += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            while (--y_c >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_b, x_b >> 16, x_a >> 16, l1 >> 8,
                        k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_b, depth_slope);
                x_a += c_to_a;
                x_b += b_to_c;
                k1 += grad_c_off;
                l1 += grad_b_off;
                z_b += depth_increment;
                y_b += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            return;
        }
        if (y_c >= Rasterizer2D.clip_bottom) {
            return;
        }
        if (y_a > Rasterizer2D.clip_bottom) {
            y_a = Rasterizer2D.clip_bottom;
        }
        if (y_b > Rasterizer2D.clip_bottom) {
            y_b = Rasterizer2D.clip_bottom;
        }
        z_c = z_c - depth_slope * x_c + depth_slope;
        if (y_a < y_b) {
            x_b = x_c <<= 16;
            l1 = i2 <<= 16;
            if (y_c < 0) {
                x_b -= b_to_c * y_c;
                x_c -= c_to_a * y_c;
                z_c -= depth_increment * y_c;
                l1 -= grad_b_off * y_c;
                i2 -= grad_c_off * y_c;
                y_c = 0;
            }
            x_a <<= 16;
            k1 <<= 16;
            if (y_a < 0) {
                x_a -= a_to_b * y_a;
                k1 -= grad_a_off * y_a;
                y_a = 0;
            }
            int k9 = y_c - originViewY;
            Oa += Va * k9;
            Ob += Vb * k9;
            Oc += Vc * k9;
            if (b_to_c < c_to_a) {
                y_b -= y_a;
                y_a -= y_c;
                y_c = scanOffsets[y_c];
                while (--y_a >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_b >> 16, x_c >> 16, l1 >> 8,
                            i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
                    x_b += b_to_c;
                    x_c += c_to_a;
                    l1 += grad_b_off;
                    i2 += grad_c_off;
                    z_c += depth_increment;
                    y_c += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                while (--y_b >= 0) {
                    drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_b >> 16, x_a >> 16, l1 >> 8,
                            k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
                    x_b += b_to_c;
                    x_a += a_to_b;
                    l1 += grad_b_off;
                    k1 += grad_a_off;
                    z_c += depth_increment;
                    y_c += Rasterizer2D.width;
                    Oa += Va;
                    Ob += Vb;
                    Oc += Vc;
                }
                return;
            }
            y_b -= y_a;
            y_a -= y_c;
            y_c = scanOffsets[y_c];
            while (--y_a >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_c >> 16, x_b >> 16, i2 >> 8,
                        l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
                x_b += b_to_c;
                x_c += c_to_a;
                l1 += grad_b_off;
                i2 += grad_c_off;
                z_c += depth_increment;
                y_c += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            while (--y_b >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_a >> 16, x_b >> 16, k1 >> 8,
                        l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
                x_b += b_to_c;
                x_a += a_to_b;
                l1 += grad_b_off;
                k1 += grad_a_off;
                z_c += depth_increment;
                y_c += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            return;
        }
        x_a = x_c <<= 16;
        k1 = i2 <<= 16;
        if (y_c < 0) {
            x_a -= b_to_c * y_c;
            x_c -= c_to_a * y_c;
            z_c -= depth_increment * y_c;
            k1 -= grad_b_off * y_c;
            i2 -= grad_c_off * y_c;
            y_c = 0;
        }
        x_b <<= 16;
        l1 <<= 16;
        if (y_b < 0) {
            x_b -= a_to_b * y_b;
            l1 -= grad_a_off * y_b;
            y_b = 0;
        }
        int l9 = y_c - originViewY;
        Oa += Va * l9;
        Ob += Vb * l9;
        Oc += Vc * l9;
        if (b_to_c < c_to_a) {
            y_a -= y_b;
            y_b -= y_c;
            y_c = scanOffsets[y_c];
            while (--y_b >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_a >> 16, x_c >> 16, k1 >> 8,
                        i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
                x_a += b_to_c;
                x_c += c_to_a;
                k1 += grad_b_off;
                i2 += grad_c_off;
                z_c += depth_increment;
                y_c += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            while (--y_a >= 0) {
                drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_b >> 16, x_c >> 16, l1 >> 8,
                        i2 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
                x_b += a_to_b;
                x_c += c_to_a;
                l1 += grad_a_off;
                i2 += grad_c_off;
                z_c += depth_increment;
                y_c += Rasterizer2D.width;
                Oa += Va;
                Ob += Vb;
                Oc += Vc;
            }
            return;
        }
        y_a -= y_b;
        y_b -= y_c;
        y_c = scanOffsets[y_c];
        while (--y_b >= 0) {
            drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_c >> 16, x_a >> 16, i2 >> 8,
                    k1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
            x_a += b_to_c;
            x_c += c_to_a;
            k1 += grad_b_off;
            i2 += grad_c_off;
            z_c += depth_increment;
            y_c += Rasterizer2D.width;
            Oa += Va;
            Ob += Vb;
            Oc += Vc;
        }
        while (--y_a >= 0) {
            drawTexturedScanline(Rasterizer2D.pixels, texture, y_c, x_c >> 16, x_b >> 16, i2 >> 8,
                    l1 >> 8, Oa, Ob, Oc, Ha, Hb, Hc, z_c, depth_slope);
            x_b += a_to_b;
            x_c += c_to_a;
            l1 += grad_a_off;
            i2 += grad_c_off;
            z_c += depth_increment;
            y_c += Rasterizer2D.width;
            Oa += Va;
            Ob += Vb;
            Oc += Vc;
        }
    }

    public static void drawTexturedScanline(int dest[], int texture[], int dest_off, int start_x,
                                            int end_x, int shadeValue, int gradient, int l1, int i2, int j2, int k2, int l2, int i3,
                                            float depth, float depth_slope) {
        int rgb = 0;
        int loops = 0;
        if (start_x >= end_x) {
            return;
        }
        int j3;
        int k3;
        if (textureOutOfDrawingBounds) {
            j3 = (gradient - shadeValue) / (end_x - start_x);
            if (end_x > Rasterizer2D.lastX) {
                end_x = Rasterizer2D.lastX;
            }
            if (start_x < 0) {
                shadeValue -= start_x * j3;
                start_x = 0;
            }
            if (start_x >= end_x) {
                return;
            }
            k3 = end_x - start_x >> 3;
            j3 <<= 12;
            shadeValue <<= 9;
        } else {
            if (end_x - start_x > 7) {
                k3 = end_x - start_x >> 3;
                j3 = (gradient - shadeValue) * anIntArray1468[k3] >> 6;
            } else {
                k3 = 0;
                j3 = 0;
            }
            shadeValue <<= 9;
        }
        dest_off += start_x;
        depth += depth_slope * start_x;
        if (lowMem) {
            int i4 = 0;
            int k4 = 0;
            int k6 = start_x - originViewX;
            l1 += (k2 >> 3) * k6;
            i2 += (l2 >> 3) * k6;
            j2 += (i3 >> 3) * k6;
            int i5 = j2 >> 12;
            if (i5 != 0) {
                rgb = l1 / i5;
                loops = i2 / i5;
                if (!repeatTexture) {
                    if (rgb < 0) {
                        rgb = 0;
                    } else if (rgb > 4032) {
                        rgb = 4032;
                    }
                }
            }
            l1 += k2;
            i2 += l2;
            j2 += i3;
            i5 = j2 >> 12;
            if (i5 != 0) {
                i4 = l1 / i5;
                k4 = i2 / i5;
                if (i4 < 7) {
                    i4 = 7;
                } else if (i4 > 4032) {
                    i4 = 4032;
                }
            }
            int i7 = i4 - rgb >> 3;
            int k7 = k4 - loops >> 3;
            rgb += (shadeValue & 0x600000) >> 3;
            int i8 = shadeValue >> 23;
            if (aBoolean1463) {
                while (k3-- > 0) {
                    for (int i = 0; i < 8; i++) {
                        if (true) {
                            drawAlpha(
                                    dest,
                                    dest_off,
                                    texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8,
                                    255);
                            Rasterizer2D.depthBuffer[dest_off] = depth;
                        }
                        dest_off++;
                        depth += depth_slope;
                        rgb += i7;
                        loops += k7;
                    }
                    rgb = i4;
                    loops = k4;
                    l1 += k2;
                    i2 += l2;
                    j2 += i3;
                    int j5 = j2 >> 12;
                    if (j5 != 0) {
                        i4 = l1 / j5;
                        k4 = i2 / j5;
                        if (i4 < 7) {
                            i4 = 7;
                        } else if (i4 > 4032) {
                            i4 = 4032;
                        }
                    }
                    i7 = i4 - rgb >> 3;
                    k7 = k4 - loops >> 3;
                    shadeValue += j3;
                    rgb += (shadeValue & 0x600000) >> 3;
                    i8 = shadeValue >> 23;
                }
                for (k3 = end_x - start_x & 7; k3-- > 0;) {
                    if (true) {
                        drawAlpha(
                                dest,
                                dest_off,
                                texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8,
                                255);
                        Rasterizer2D.depthBuffer[dest_off] = depth;
                    }
                    dest_off++;
                    depth += depth_slope;
                    rgb += i7;
                    loops += k7;
                }

                return;
            }
            while (k3-- > 0) {
                int k8;
                for (int i = 0; i < 8; i++) {
                    if ((k8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
                        drawAlpha(
                                dest,
                                dest_off,
                                k8,
                                255);
                        Rasterizer2D.depthBuffer[dest_off] = depth;
                    }
                    dest_off++;
                    depth += depth_slope;
                    rgb += i7;
                    loops += k7;
                }

                rgb = i4;
                loops = k4;
                l1 += k2;
                i2 += l2;
                j2 += i3;
                int k5 = j2 >> 12;
                if (k5 != 0) {
                    i4 = l1 / k5;
                    k4 = i2 / k5;
                    if (i4 < 7) {
                        i4 = 7;
                    } else if (i4 > 4032) {
                        i4 = 4032;
                    }
                }
                i7 = i4 - rgb >> 3;
                k7 = k4 - loops >> 3;
                shadeValue += j3;
                rgb += (shadeValue & 0x600000) >> 3;
                i8 = shadeValue >> 23;
            }
            for (k3 = end_x - start_x & 7; k3-- > 0;) {
                int l8;
                if ((l8 = texture[(loops & 0xfc0) + (rgb >> 6)] >>> i8) != 0) {
                    drawAlpha(
                            dest,
                            dest_off,
                            l8,
                            255);
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                dest_off++;
                depth += depth_slope;
                rgb += i7;
                loops += k7;
            }

            return;
        }
        int j4 = 0;
        int l4 = 0;
        int l6 = start_x - originViewX;
        l1 += (k2 >> 3) * l6;
        i2 += (l2 >> 3) * l6;
        j2 += (i3 >> 3) * l6;
        int l5 = j2 >> 14;
        if (l5 != 0) {
            rgb = l1 / l5;
            loops = i2 / l5;
            if (rgb < 0) {
                rgb = 0;
            } else if (rgb > 16256) {
                rgb = 16256;
            }
        }
        l1 += k2;
        i2 += l2;
        j2 += i3;
        l5 = j2 >> 14;
        if (l5 != 0) {
            j4 = l1 / l5;
            l4 = i2 / l5;
            if (j4 < 7) {
                j4 = 7;
            } else if (j4 > 16256) {
                j4 = 16256;
            }
        }
        int j7 = j4 - rgb >> 3;
        int l7 = l4 - loops >> 3;
        rgb += shadeValue & 0x600000;
        int j8 = shadeValue >> 23;
        if (aBoolean1463) {
            while (k3-- > 0) {
                for (int i = 0; i < 8; i++) {
                    if (true) {
                        drawAlpha(
                                dest,
                                dest_off,
                                texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8,
                                255);
                        Rasterizer2D.depthBuffer[dest_off] = depth;
                    }
                    depth += depth_slope;
                    dest_off++;
                    rgb += j7;
                    loops += l7;
                }
                rgb = j4;
                loops = l4;
                l1 += k2;
                i2 += l2;
                j2 += i3;
                int i6 = j2 >> 14;
                if (i6 != 0) {
                    j4 = l1 / i6;
                    l4 = i2 / i6;
                    if (j4 < 7) {
                        j4 = 7;
                    } else if (j4 > 16256) {
                        j4 = 16256;
                    }
                }
                j7 = j4 - rgb >> 3;
                l7 = l4 - loops >> 3;
                shadeValue += j3;
                rgb += shadeValue & 0x600000;
                j8 = shadeValue >> 23;
            }
            for (k3 = end_x - start_x & 7; k3-- > 0;) {
                if (true) {
                    drawAlpha(
                            dest,
                            dest_off,
                            texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8,
                            255);
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                dest_off++;
                depth += depth_slope;
                rgb += j7;
                loops += l7;
            }

            return;
        }
        while (k3-- > 0) {
            int i9;
            for (int i = 0; i < 8; i++) {
                if ((i9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
                    drawAlpha(
                            dest,
                            dest_off,
                            i9,
                            255);
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                dest_off++;
                depth += depth_slope;
                rgb += j7;
                loops += l7;
            }
            rgb = j4;
            loops = l4;
            l1 += k2;
            i2 += l2;
            j2 += i3;
            int j6 = j2 >> 14;
            if (j6 != 0) {
                j4 = l1 / j6;
                l4 = i2 / j6;
                if (j4 < 7) {
                    j4 = 7;
                } else if (j4 > 16256) {
                    j4 = 16256;
                }
            }
            j7 = j4 - rgb >> 3;
            l7 = l4 - loops >> 3;
            shadeValue += j3;
            rgb += shadeValue & 0x600000;
            j8 = shadeValue >> 23;
        }
        for (int l3 = end_x - start_x & 7; l3-- > 0;) {
            int j9;
            if ((j9 = texture[(loops & 0x3f80) + (rgb >> 7)] >>> j8) != 0) {
                drawAlpha(
                        dest,
                        dest_off,
                        j9,
                        255);
                Rasterizer2D.depthBuffer[dest_off] = depth;
            }
            depth += depth_slope;
            dest_off++;
            rgb += j7;
            loops += l7;
        }
    }

    public static void drawDepthTriangle(int x_a, int x_b, int x_c, int y_a, int y_b, int y_c,
                                         float z_a, float z_b, float z_c) {
        int a_to_b = 0;
        if (y_b != y_a) {
            a_to_b = (x_b - x_a << 16) / (y_b - y_a);
        }
        int b_to_c = 0;
        if (y_c != y_b) {
            b_to_c = (x_c - x_b << 16) / (y_c - y_b);
        }
        int c_to_a = 0;
        if (y_c != y_a) {
            c_to_a = (x_a - x_c << 16) / (y_a - y_c);
        }

        float b_aX = x_b - x_a;
        float b_aY = y_b - y_a;
        float c_aX = x_c - x_a;
        float c_aY = y_c - y_a;
        float b_aZ = z_b - z_a;
        float c_aZ = z_c - z_a;

        float div = b_aX * c_aY - c_aX * b_aY;
        float depth_slope = (b_aZ * c_aY - c_aZ * b_aY) / div;
        float depth_increment = (c_aZ * b_aX - b_aZ * c_aX) / div;
        if (y_a <= y_b && y_a <= y_c) {
            if (y_a < Rasterizer2D.clip_bottom) {
                if (y_b > Rasterizer2D.clip_bottom) {
                    y_b = Rasterizer2D.clip_bottom;
                }
                if (y_c > Rasterizer2D.clip_bottom) {
                    y_c = Rasterizer2D.clip_bottom;
                }
                z_a = z_a - depth_slope * x_a + depth_slope;
                if (y_b < y_c) {
                    x_c = x_a <<= 16;
                    if (y_a < 0) {
                        x_c -= c_to_a * y_a;
                        x_a -= a_to_b * y_a;
                        z_a -= depth_increment * y_a;
                        y_a = 0;
                    }
                    x_b <<= 16;
                    if (y_b < 0) {
                        x_b -= b_to_c * y_b;
                        y_b = 0;
                    }
                    if (y_a != y_b && c_to_a < a_to_b || y_a == y_b && c_to_a > b_to_c) {
                        y_c -= y_b;
                        y_b -= y_a;
                        y_a = scanOffsets[y_a];
                        while (--y_b >= 0) {
                            drawDepthTriangleScanline(y_a, x_c >> 16, x_a >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_a, x_c >> 16, x_b >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_b += b_to_c;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                    } else {
                        y_c -= y_b;
                        y_b -= y_a;
                        y_a = scanOffsets[y_a];
                        while (--y_b >= 0) {
                            drawDepthTriangleScanline(y_a, x_a >> 16, x_c >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_a, x_b >> 16, x_c >> 16, z_a, depth_slope);
                            x_c += c_to_a;
                            x_b += b_to_c;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                    }
                } else {
                    x_b = x_a <<= 16;
                    if (y_a < 0) {
                        x_b -= c_to_a * y_a;
                        x_a -= a_to_b * y_a;
                        z_a -= depth_increment * y_a;
                        y_a = 0;
                    }
                    x_c <<= 16;
                    if (y_c < 0) {
                        x_c -= b_to_c * y_c;
                        y_c = 0;
                    }
                    if (y_a != y_c && c_to_a < a_to_b || y_a == y_c && b_to_c > a_to_b) {
                        y_b -= y_c;
                        y_c -= y_a;
                        y_a = scanOffsets[y_a];
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_a, x_b >> 16, x_a >> 16, z_a, depth_slope);
                            x_b += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                        while (--y_b >= 0) {
                            drawDepthTriangleScanline(y_a, x_c >> 16, x_a >> 16, z_a, depth_slope);
                            x_c += b_to_c;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                    } else {
                        y_b -= y_c;
                        y_c -= y_a;
                        y_a = scanOffsets[y_a];
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_a, x_a >> 16, x_b >> 16, z_a, depth_slope);
                            x_b += c_to_a;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                        while (--y_b >= 0) {
                            drawDepthTriangleScanline(y_a, x_a >> 16, x_c >> 16, z_a, depth_slope);
                            x_c += b_to_c;
                            x_a += a_to_b;
                            z_a += depth_increment;
                            y_a += Rasterizer2D.width;
                        }
                    }
                }
            }
        } else if (y_b <= y_c) {
            if (y_b < Rasterizer2D.clip_bottom) {
                if (y_c > Rasterizer2D.clip_bottom) {
                    y_c = Rasterizer2D.clip_bottom;
                }
                if (y_a > Rasterizer2D.clip_bottom) {
                    y_a = Rasterizer2D.clip_bottom;
                }
                z_b = z_b - depth_slope * x_b + depth_slope;
                if (y_c < y_a) {
                    x_a = x_b <<= 16;
                    if (y_b < 0) {
                        x_a -= a_to_b * y_b;
                        x_b -= b_to_c * y_b;
                        z_b -= depth_increment * y_b;
                        y_b = 0;
                    }
                    x_c <<= 16;
                    if (y_c < 0) {
                        x_c -= c_to_a * y_c;
                        y_c = 0;
                    }
                    if (y_b != y_c && a_to_b < b_to_c || y_b == y_c && a_to_b > c_to_a) {
                        y_a -= y_c;
                        y_c -= y_b;
                        y_b = scanOffsets[y_b];
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_b, x_a >> 16, x_b >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                        while (--y_a >= 0) {
                            drawDepthTriangleScanline(y_b, x_a >> 16, x_c >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_c += c_to_a;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                    } else {
                        y_a -= y_c;
                        y_c -= y_b;
                        y_b = scanOffsets[y_b];
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_b, x_b >> 16, x_a >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                        while (--y_a >= 0) {
                            drawDepthTriangleScanline(y_b, x_c >> 16, x_a >> 16, z_b, depth_slope);
                            x_a += a_to_b;
                            x_c += c_to_a;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                    }
                } else {
                    x_c = x_b <<= 16;
                    if (y_b < 0) {
                        x_c -= a_to_b * y_b;
                        x_b -= b_to_c * y_b;
                        z_b -= depth_increment * y_b;
                        y_b = 0;
                    }
                    x_a <<= 16;
                    if (y_a < 0) {
                        x_a -= c_to_a * y_a;
                        y_a = 0;
                    }
                    if (a_to_b < b_to_c) {
                        y_c -= y_a;
                        y_a -= y_b;
                        y_b = scanOffsets[y_b];
                        while (--y_a >= 0) {
                            drawDepthTriangleScanline(y_b, x_c >> 16, x_b >> 16, z_b, depth_slope);
                            x_c += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_b, x_a >> 16, x_b >> 16, z_b, depth_slope);
                            x_a += c_to_a;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                    } else {
                        y_c -= y_a;
                        y_a -= y_b;
                        y_b = scanOffsets[y_b];
                        while (--y_a >= 0) {
                            drawDepthTriangleScanline(y_b, x_b >> 16, x_c >> 16, z_b, depth_slope);
                            x_c += a_to_b;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                        while (--y_c >= 0) {
                            drawDepthTriangleScanline(y_b, x_b >> 16, x_a >> 16, z_b, depth_slope);
                            x_a += c_to_a;
                            x_b += b_to_c;
                            z_b += depth_increment;
                            y_b += Rasterizer2D.width;
                        }
                    }
                }
            }
        } else if (y_c < Rasterizer2D.clip_bottom) {
            if (y_a > Rasterizer2D.clip_bottom) {
                y_a = Rasterizer2D.clip_bottom;
            }
            if (y_b > Rasterizer2D.clip_bottom) {
                y_b = Rasterizer2D.clip_bottom;
            }
            z_c = z_c - depth_slope * x_c + depth_slope;
            if (y_a < y_b) {
                x_b = x_c <<= 16;
                if (y_c < 0) {
                    x_b -= b_to_c * y_c;
                    x_c -= c_to_a * y_c;
                    z_c -= depth_increment * y_c;
                    y_c = 0;
                }
                x_a <<= 16;
                if (y_a < 0) {
                    x_a -= a_to_b * y_a;
                    y_a = 0;
                }
                if (b_to_c < c_to_a) {
                    y_b -= y_a;
                    y_a -= y_c;
                    y_c = scanOffsets[y_c];
                    while (--y_a >= 0) {
                        drawDepthTriangleScanline(y_c, x_b >> 16, x_c >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                    while (--y_b >= 0) {
                        drawDepthTriangleScanline(y_c, x_b >> 16, x_a >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_a += a_to_b;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                } else {
                    y_b -= y_a;
                    y_a -= y_c;
                    y_c = scanOffsets[y_c];
                    while (--y_a >= 0) {
                        drawDepthTriangleScanline(y_c, x_c >> 16, x_b >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                    while (--y_b >= 0) {
                        drawDepthTriangleScanline(y_c, x_a >> 16, x_b >> 16, z_c, depth_slope);
                        x_b += b_to_c;
                        x_a += a_to_b;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                }
            } else {
                x_a = x_c <<= 16;
                if (y_c < 0) {
                    x_a -= b_to_c * y_c;
                    x_c -= c_to_a * y_c;
                    z_c -= depth_increment * y_c;
                    y_c = 0;
                }
                x_b <<= 16;
                if (y_b < 0) {
                    x_b -= a_to_b * y_b;
                    y_b = 0;
                }
                if (b_to_c < c_to_a) {
                    y_a -= y_b;
                    y_b -= y_c;
                    y_c = scanOffsets[y_c];
                    while (--y_b >= 0) {
                        drawDepthTriangleScanline(y_c, x_a >> 16, x_c >> 16, z_c, depth_slope);
                        x_a += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                    while (--y_a >= 0) {
                        drawDepthTriangleScanline(y_c, x_b >> 16, x_c >> 16, z_c, depth_slope);
                        x_b += a_to_b;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                } else {
                    y_a -= y_b;
                    y_b -= y_c;
                    y_c = scanOffsets[y_c];
                    while (--y_b >= 0) {
                        drawDepthTriangleScanline(y_c, x_c >> 16, x_a >> 16, z_c, depth_slope);
                        x_a += b_to_c;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                    while (--y_a >= 0) {
                        drawDepthTriangleScanline(y_c, x_c >> 16, x_b >> 16, z_c, depth_slope);
                        x_b += a_to_b;
                        x_c += c_to_a;
                        z_c += depth_increment;
                        y_c += Rasterizer2D.width;
                    }
                }
            }
        }
    }

    private static void drawDepthTriangleScanline(int dest_off, int start_x, int end_x, float depth,
                                                  float depth_slope) {

        int dbl = Rasterizer2D.depthBuffer.length;
        if (textureOutOfDrawingBounds) {
            if (end_x > Rasterizer2D.width) {
                end_x = Rasterizer2D.width;
            }
            if (start_x < 0) {
                start_x = 0;
            }
        }
        if (start_x >= end_x) {
            return;
        }
        dest_off += start_x - 1;
        int loops = end_x - start_x >> 2;
        depth += depth_slope * start_x;
        if (alpha == 0) {
            while (--loops >= 0) {
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl) {
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl) {
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl) {
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl) {
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
            }
            for (loops = end_x - start_x & 3; --loops >= 0;) {
                dest_off++;
                if (dest_off >= 0 && dest_off < dbl) {
                    Rasterizer2D.depthBuffer[dest_off] = depth;
                }
                depth += depth_slope;
            }
            return;
        }
        while (--loops >= 0) {
            dest_off++;
            if (dest_off >= 0 && dest_off < dbl) {
                Rasterizer2D.depthBuffer[dest_off] = depth;
            }
            depth += depth_slope;
            dest_off++;
            if (dest_off >= 0 && dest_off < dbl) {
                Rasterizer2D.depthBuffer[dest_off] = depth;
            }
            depth += depth_slope;
            dest_off++;
            if (dest_off >= 0 && dest_off < dbl) {
                Rasterizer2D.depthBuffer[dest_off] = depth;
            }
            depth += depth_slope;
            dest_off++;
            if (dest_off >= 0 && dest_off < dbl) {
                Rasterizer2D.depthBuffer[dest_off] = depth;
            }
            depth += depth_slope;
        }
        for (loops = end_x - start_x & 3; --loops >= 0;) {
            dest_off++;
            if (dest_off >= 0 && dest_off < dbl) {
                Rasterizer2D.depthBuffer[dest_off] = depth;
            }
            depth += depth_slope;
        }
    }

}
