package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * This is an interface for interpolators.
 * It provides a method to interpolate a pixel in an image.
 */
public interface Interpolator {

    /**
     * This method interpolates a pixel in an image.
     *
     * @param reader The PixelReader of the image.
     * @param x The x-coordinate of the pixel.
     * @param y The y-coordinate of the pixel.
     * @param maxWidth The maximum width of the image.
     * @param maxHeight The maximum height of the image.
     * @return The color of the interpolated pixel.
     */
    Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight);
}