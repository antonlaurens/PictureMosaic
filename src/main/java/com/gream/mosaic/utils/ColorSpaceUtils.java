package com.gream.mosaic.utils;

import java.awt.Color;

/**
 * Utility class for color space conversions.
 * Provides RGB to LAB (CIE L*a*b*) conversion for perceptual color matching.
 * LAB color space is designed to be perceptually uniform, meaning equal
 * distances
 * in LAB space correspond to equal perceived color differences.
 */
public class ColorSpaceUtils {

    /**
     * Converts RGB color to LAB color space.
     * 
     * @param color RGB color
     * @return LAB values as [L, a, b] where L is in [0, 100], a and b are in [-128,
     *         127]
     */
    public static double[] rgbToLab(Color color) {
        return rgbToLab(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Converts RGB values to LAB color space.
     * 
     * @param r Red component [0-255]
     * @param g Green component [0-255]
     * @param b Blue component [0-255]
     * @return LAB values as [L, a, b]
     */
    public static double[] rgbToLab(int r, int g, int b) {
        // First convert RGB to XYZ
        double[] xyz = rgbToXyz(r, g, b);

        // Then convert XYZ to LAB
        return xyzToLab(xyz[0], xyz[1], xyz[2]);
    }

    /**
     * Converts RGB to XYZ color space (intermediate step).
     */
    private static double[] rgbToXyz(int r, int g, int b) {
        // Normalize RGB values to [0, 1]
        double rNorm = r / 255.0;
        double gNorm = g / 255.0;
        double bNorm = b / 255.0;

        // Apply gamma correction
        rNorm = (rNorm > 0.04045) ? Math.pow((rNorm + 0.055) / 1.055, 2.4) : rNorm / 12.92;
        gNorm = (gNorm > 0.04045) ? Math.pow((gNorm + 0.055) / 1.055, 2.4) : gNorm / 12.92;
        bNorm = (bNorm > 0.04045) ? Math.pow((bNorm + 0.055) / 1.055, 2.4) : bNorm / 12.92;

        // Convert to linear RGB
        rNorm *= 100.0;
        gNorm *= 100.0;
        bNorm *= 100.0;

        // Observer = 2°, Illuminant = D65 (sRGB standard)
        double x = rNorm * 0.4124564 + gNorm * 0.3575761 + bNorm * 0.1804375;
        double y = rNorm * 0.2126729 + gNorm * 0.7151522 + bNorm * 0.0721750;
        double z = rNorm * 0.0193339 + gNorm * 0.1191920 + bNorm * 0.9503041;

        return new double[] { x, y, z };
    }

    /**
     * Converts XYZ to LAB color space.
     */
    private static double[] xyzToLab(double x, double y, double z) {
        // D65 reference white point
        double refX = 95.047;
        double refY = 100.000;
        double refZ = 108.883;

        x = x / refX;
        y = y / refY;
        z = z / refZ;

        x = (x > 0.008856) ? Math.pow(x, 1.0 / 3.0) : (7.787 * x + 16.0 / 116.0);
        y = (y > 0.008856) ? Math.pow(y, 1.0 / 3.0) : (7.787 * y + 16.0 / 116.0);
        z = (z > 0.008856) ? Math.pow(z, 1.0 / 3.0) : (7.787 * z + 16.0 / 116.0);

        double l = (116.0 * y) - 16.0;
        double a = 500.0 * (x - y);
        double b = 200.0 * (y - z);

        return new double[] { l, a, b };
    }

    /**
     * Calculates the squared Euclidean distance in LAB color space.
     * This provides perceptual color distance that better matches human vision.
     * 
     * @param lab1 First LAB color [L, a, b]
     * @param lab2 Second LAB color [L, a, b]
     * @return Squared distance in LAB space
     */
    public static double getLabDistanceSquared(double[] lab1, double[] lab2) {
        double dL = lab1[0] - lab2[0];
        double dA = lab1[1] - lab2[1];
        double dB = lab1[2] - lab2[2];
        return dL * dL + dA * dA + dB * dB;
    }

    /**
     * Calculates the Delta E (CIE76) color difference in LAB space.
     * This is the standard metric for perceptual color difference.
     * 
     * @param lab1 First LAB color [L, a, b]
     * @param lab2 Second LAB color [L, a, b]
     * @return Delta E color difference
     */
    public static double getDeltaE(double[] lab1, double[] lab2) {
        return Math.sqrt(getLabDistanceSquared(lab1, lab2));
    }
}
