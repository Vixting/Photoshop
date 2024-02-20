package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class BilinearInterpolator implements Interpolator {

    @Override
    public Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int xFloor = clamp((int) Math.floor(x), 0, maxWidth - 1);
        int yFloor = clamp((int) Math.floor(y), 0, maxHeight - 1);
        int xCeil = clamp(xFloor + 1, 0, maxWidth - 1);
        int yCeil = clamp(yFloor + 1, 0, maxHeight - 1);

        double xFraction = x - xFloor;
        double yFraction = y - yFloor;

        Color topLeft = getColor(reader, xFloor, yFloor);
        Color topRight = getColor(reader, xCeil, yFloor);
        Color bottomLeft = getColor(reader, xFloor, yCeil);
        Color bottomRight = getColor(reader, xCeil, yCeil);

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
        double red = interpolateComponent(start.getRed(), end.getRed(), fraction);
        double green = interpolateComponent(start.getGreen(), end.getGreen(), fraction);
        double blue = interpolateComponent(start.getBlue(), end.getBlue(), fraction);
        double alpha = interpolateComponent(start.getOpacity(), end.getOpacity(), fraction);

        return new Color(red, green, blue, alpha);
    }

    private double interpolateComponent(double start, double end, double fraction) {
        return start + fraction * (end - start);
    }
}


