package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * This class represents a nearest neighbor interpolator.
 * It implements the Interpolator interface and overrides the interpolate method to apply nearest neighbor interpolation.
 */
public class NearestNeighborInterpolator implements Interpolator {

    /**
     * This method applies the nearest neighbor interpolation to a pixel.
     *
     * @param reader The PixelReader of the image.
     * @param x The x-coordinate of the pixel.
     * @param y The y-coordinate of the pixel.
     * @param maxWidth The maximum width of the image.
     * @param maxHeight The maximum height of the image.
     * @return The color of the nearest pixel.
     */
    @Override
    public Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int nearestX = clamp((int) Math.round(x), 0, maxWidth - 1);
        int nearestY = clamp((int) Math.round(y), 0, maxHeight - 1);
        return reader.getColor(nearestX, nearestY);
    }

    /**
     * This method clamps a value between a minimum and maximum value.
     *
     * @param value The value to be clamped.
     * @param min The minimum value.
     * @param max The maximum value.
     * @return The clamped value.
     */
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}