package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import java.util.stream.IntStream;

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

        double[][] grayScales = new double[height][width];
        double[][] intensities = new double[height][width];
        double[] minMaxIntensity = {Double.MAX_VALUE, Double.MIN_VALUE};

        // Precompute grayscale values
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                grayScales[y][x] = color.getRed() * 0.21 + color.getGreen() * 0.72 + color.getBlue() * 0.07;
            }
        }

        // Apply filter using parallel processing
        IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                double intensity = applyKernel(grayScales, x, y, width, height);
                intensities[y][x] = intensity;
                synchronized (minMaxIntensity) {
                    minMaxIntensity[0] = Math.min(minMaxIntensity[0], intensity);
                    minMaxIntensity[1] = Math.max(minMaxIntensity[1], intensity);
                }
            }
        });

        // Normalize and write the final image
        double minIntensity = minMaxIntensity[0];
        double maxIntensity = minMaxIntensity[1];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double normalizedIntensity = normalizeIntensity(intensities[y][x], minIntensity, maxIntensity);
                writer.setColor(x, y, new Color(normalizedIntensity, normalizedIntensity, normalizedIntensity, 1.0));
            }
        }

        return result;
    }

    private double applyKernel(double[][] grayScales, int x, int y, int width, int height) {
        double intensity = 0;
        int kernelSize = LAPLACIAN_FILTER.length;

        for (int dy = 0; dy < kernelSize; dy++) {
            for (int dx = 0; dx < kernelSize; dx++) {
                int imageX = clamp(x - kernelSize / 2 + dx, 0, width - 1);
                int imageY = clamp(y - kernelSize / 2 + dy, 0, height - 1);
                intensity += grayScales[imageY][imageX] * LAPLACIAN_FILTER[dy][dx];
            }
        }
        return intensity;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private double normalizeIntensity(double intensity, double minIntensity, double maxIntensity) {
        return maxIntensity == minIntensity ? 0 : (intensity - minIntensity) / (maxIntensity - minIntensity);
    }
}

