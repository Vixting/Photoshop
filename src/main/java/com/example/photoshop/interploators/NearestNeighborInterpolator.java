package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public class NearestNeighborInterpolator implements Interpolator {

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
