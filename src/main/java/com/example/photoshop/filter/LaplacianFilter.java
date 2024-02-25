package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class LaplacianFilter implements Filters {

    private static final int[][] LAPLACIAN_FILTER = {
            {-4, -1, 0, -1, -4},
            {-1, 2, 3, 2, -1},
            {0, 3, 4, 3, 0},
            {-1, 2, 3, 2, -1},
            {-4, -1, 0, -1, -4}
    };

    @Override
    public Image applyFilter(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage result = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, applyKernel(reader, x, y, width, height));
            }
        }

        return result;
    }

    private Color applyKernel(PixelReader reader, int x, int y, int width, int height) {
        double intensity = 0;
        int kernelSize = LAPLACIAN_FILTER.length;

        for (int dy = 0; dy < kernelSize; dy++) {
            for (int dx = 0; dx < kernelSize; dx++) {
                int imageX = clamp(x - kernelSize / 2 + dx, 0, width - 1);
                int imageY = clamp(y - kernelSize / 2 + dy, 0, height - 1);

                Color pixelColor = reader.getColor(imageX, imageY);
                double grayScale = (pixelColor.getRed() + pixelColor.getGreen() + pixelColor.getBlue()) / 3;
                intensity += grayScale * LAPLACIAN_FILTER[dy][dx];
            }
        }

        intensity = normalizeIntensity(intensity);
        return new Color(intensity, intensity, intensity, 1.0);
    }

    private <T extends Comparable<T>> T clamp(T value, T min, T max) {
        if (value.compareTo(min) < 0) return min;
        if (value.compareTo(max) > 0) return max;
        return value;
    }


    private double normalizeIntensity(double intensity) {
        intensity = (intensity + 4) / 8;
        return clamp(intensity, 0.0, 1.0);
    }
}

