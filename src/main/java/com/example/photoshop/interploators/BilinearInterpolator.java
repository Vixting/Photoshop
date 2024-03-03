package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class BilinearInterpolator implements Interpolator {

    /**
     * Performs bilinear interpolation for a given point (x, y) in the image.
     *
     * @param reader PixelReader to access pixel data from the image.
     * @param x X-coordinate of the point for interpolation.
     * @param y Y-coordinate of the point for interpolation.
     * @param maxWidth Maximum width of the image.
     * @param maxHeight Maximum height of the image.
     * @return Interpolated color at the specified point.
     */
    @Override
    public Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int xFloor = (int) x;
        int yFloor = (int) y;
        int xCeil = Math.min(xFloor + 1, maxWidth - 1);
        int yCeil = Math.min(yFloor + 1, maxHeight - 1);

        double xFraction = x - xFloor;
        double yFraction = y - yFloor;

        Color topLeft = reader.getColor(xFloor, yFloor);
        Color topRight = reader.getColor(xCeil, yFloor);
        Color bottomLeft = reader.getColor(xFloor, yCeil);
        Color bottomRight = reader.getColor(xCeil, yCeil);

        return interpolateColors(topLeft, topRight, bottomLeft, bottomRight, xFraction, yFraction);
    }


    // Interpolates colors based on the fractional positions
    private Color interpolateColors(Color topLeft, Color topRight, Color bottomLeft, Color bottomRight, double xFraction, double yFraction) {
        // Interpolate the color components separately
        double r = interpolate(topLeft.getRed(), topRight.getRed(), bottomLeft.getRed(), bottomRight.getRed(), xFraction, yFraction);
        double g = interpolate(topLeft.getGreen(), topRight.getGreen(), bottomLeft.getGreen(), bottomRight.getGreen(), xFraction, yFraction);
        double b = interpolate(topLeft.getBlue(), topRight.getBlue(), bottomLeft.getBlue(), bottomRight.getBlue(), xFraction, yFraction);
        double a = interpolate(topLeft.getOpacity(), topRight.getOpacity(), bottomLeft.getOpacity(), bottomRight.getOpacity(), xFraction, yFraction);

        // Return the interpolated color
        return new Color(r, g, b, a);
    }

    // Performs linear interpolation between two values based on a fractional position
    private double interpolate(double topLeft, double topRight, double bottomLeft, double bottomRight, double xFraction, double yFraction) {
        // Interpolate along the top and bottom edges
        double top = interpolateLine(topLeft, topRight, xFraction);
        double bottom = interpolateLine(bottomLeft, bottomRight, xFraction);

        // Interpolate between the top and bottom interpolations
        return interpolateLine(top, bottom, yFraction);
    }

    // Linearly interpolates between two values based on a fraction
    private double interpolateLine(double start, double end, double fraction) {
        // Linear interpolation formula
        return start + fraction * (end - start);
    }
}