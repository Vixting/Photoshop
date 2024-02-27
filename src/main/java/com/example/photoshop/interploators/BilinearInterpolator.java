package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

/**
 * Represents a bilinear interpolator.
 * Implements the Interpolator interface and overrides the interpolate method to apply bilinear interpolation.
 */
public class BilinearInterpolator implements Interpolator {

    /**
     * Applies the bilinear interpolation to a pixel.
     *
     * @param reader PixelReader of the image.
     * @param x x-coordinate of the pixel.
     * @param y y-coordinate of the pixel.
     * @param maxWidth Maximum width of the image.
     * @param maxHeight Maximum height of the image.
     * @return Color of the interpolated pixel.
     */
    @Override
    public Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int xFloor = clamp((int) Math.floor(x), 0, maxWidth - 1);
        int yFloor = clamp((int) Math.floor(y), 0, maxHeight - 1);

        double xFraction = x - xFloor;
        double yFraction = y - yFloor;

        Color topLeft = getColor(reader, xFloor, yFloor);
        Color topRight = getColor(reader, xFloor + 1, yFloor);
        Color bottomLeft = getColor(reader, xFloor, yFloor + 1);
        Color bottomRight = getColor(reader, xFloor + 1, yFloor + 1);

        Color topInterpolated = interpolateColor(topLeft, topRight, xFraction);
        Color bottomInterpolated = interpolateColor(bottomLeft, bottomRight, xFraction);

        return interpolateColor(topInterpolated, bottomInterpolated, yFraction);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private Color getColor(PixelReader reader, int x, int y) {
        try {
            return reader.getColor(x, y);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    private Color interpolateColor(Color start, Color end, double fraction) {
        return new Color(
                interpolateComponent(start.getRed(), end.getRed(), fraction),
                interpolateComponent(start.getGreen(), end.getGreen(), fraction),
                interpolateComponent(start.getBlue(), end.getBlue(), fraction),
                interpolateComponent(start.getOpacity(), end.getOpacity(), fraction)
        );
    }

    private double interpolateComponent(double start, double end, double fraction) {
        return start + fraction * (end - start);
    }
}