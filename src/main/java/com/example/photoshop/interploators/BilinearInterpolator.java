package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class BilinearInterpolator implements Interpolator {

    @Override
    public Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int xFloor = clamp((int) x, 0, maxWidth - 1);
        int yFloor = clamp((int) y, 0, maxHeight - 1);
        int xCeiling = clamp(xFloor + 1, 0, maxWidth - 1);
        int yCeiling = clamp(yFloor + 1, 0, maxHeight - 1);

        double xFraction = x - xFloor;
        double yFraction = y - yFloor;

        Color topLeft = reader.getColor(xFloor, yFloor);
        Color topRight = reader.getColor(xCeiling, yFloor);
        Color bottomLeft = reader.getColor(xFloor, yCeiling);
        Color bottomRight = reader.getColor(xCeiling, yCeiling);

        double r1 = interpolateComponent(topLeft.getRed(), topRight.getRed(), xFraction);
        double g1 = interpolateComponent(topLeft.getGreen(), topRight.getGreen(), xFraction);
        double b1 = interpolateComponent(topLeft.getBlue(), topRight.getBlue(), xFraction);
        double a1 = interpolateComponent(topLeft.getOpacity(), topRight.getOpacity(), xFraction);

        double r2 = interpolateComponent(bottomLeft.getRed(), bottomRight.getRed(), xFraction);
        double g2 = interpolateComponent(bottomLeft.getGreen(), bottomRight.getGreen(), xFraction);
        double b2 = interpolateComponent(bottomLeft.getBlue(), bottomRight.getBlue(), xFraction);
        double a2 = interpolateComponent(bottomLeft.getOpacity(), bottomRight.getOpacity(), xFraction);

        return new Color(
                interpolateComponent(r1, r2, yFraction),
                interpolateComponent(g1, g2, yFraction),
                interpolateComponent(b1, b2, yFraction),
                interpolateComponent(a1, a2, yFraction)
        );
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double interpolateComponent(double start, double end, double fraction) {
        return start + fraction * (end - start);
    }
}