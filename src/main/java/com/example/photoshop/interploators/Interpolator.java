package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * Interface for interpolators.
 * It provides a method to interpolate a pixel in an image.
 */
public interface Interpolator {

    /**
     * Interpolates a pixel in an image.
     *
     * @param reader PixelReader of the image.
     * @param x x-coordinate of the pixel.
     * @param y y-coordinate of the pixel.
     * @param maxWidth Maximum width of the image.
     * @param maxHeight Maximum height of the image.
     * @return Color of the interpolated pixel.
     */
    Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight);
}