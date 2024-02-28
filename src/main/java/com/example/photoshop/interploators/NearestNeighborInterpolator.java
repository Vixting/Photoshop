package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * Represents a nearest neighbor interpolator.
 * Implements the Interpolator interface & overrides the interpolate method to apply nearest neighbor interpolation.
 */
public class NearestNeighborInterpolator implements Interpolator {

    /**
     * This method applies the nearest neighbor interpolation to a pixel.
     *
     * @param reader PixelReader of the image.
     * @param x x-coordinate of the pixel.
     * @param y y-coordinate of the pixel.
     * @param maxWidth Maximum width of the image.
     * @param maxHeight Maximum height of the image.
     * @return Colour of the nearest pixel.
     */
    @Override
    public Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int nearestX = clamp((int) Math.round(x), 0, maxWidth - 1);
        int nearestY = clamp((int) Math.round(y), 0, maxHeight - 1);
        return reader.getColor(nearestX, nearestY);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}