package com.example.photoshop.interploators;

import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

public interface Interpolator {
    Color interpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight);
}
