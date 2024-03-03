package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class LaplacianFilter implements Filters {

    // Define the kernel for the Laplacian filter.
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

        // Initialize arrays to track min and max intensities for normalization.
        double[][] minMaxIntensity = {
                {Double.MAX_VALUE, Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MIN_VALUE}
        };

        // Buffer to hold the calculated intensities for each pixel and color channel.
        double[][][] intensities = new double[height][width][3];

        // Apply the Laplacian filter to each pixel and store the results.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (int i = 0; i < 3; i++) {
                    double intensity = applyKernel(reader, x, y, width, height, i);
                    intensities[y][x][i] = intensity;
                    // Update the min and max values for normalization.
                    minMaxIntensity[i][0] = Math.min(minMaxIntensity[i][0], intensity);
                    minMaxIntensity[i][1] = Math.max(minMaxIntensity[i][1], intensity);
                }
            }
        }

        // Normalize the intensities and set the colors for each pixel.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, new Color(
                        normalizeIntensity(intensities[y][x][0], minMaxIntensity[0][0], minMaxIntensity[0][1]),
                        normalizeIntensity(intensities[y][x][1], minMaxIntensity[1][0], minMaxIntensity[1][1]),
                        normalizeIntensity(intensities[y][x][2], minMaxIntensity[2][0], minMaxIntensity[2][1]),
                        1.0));
            }
        }

        return result;
    }

    // Apply the Laplacian kernel to a pixel at (x, y) for a specific color channel.
    private double applyKernel(PixelReader reader, int x, int y, int width, int height, int colorIndex) {
        double intensity = 0;
        int kernelSize = LAPLACIAN_FILTER.length;
        // Iterate over the kernel and calculate the weighted sum.
        for (int dy = 0; dy < kernelSize; dy++) {
            for (int dx = 0; dx < kernelSize; dx++) {
                int imageX = clamp(x - kernelSize / 2 + dx, 0, width - 1);
                int imageY = clamp(y - kernelSize / 2 + dy, 0, height - 1);
                Color color = reader.getColor(imageX, imageY);
                double value = (colorIndex == 0) ? color.getRed() : (colorIndex == 1) ? color.getGreen() : color.getBlue();
                intensity += value * LAPLACIAN_FILTER[dy][dx];
            }
        }
        return intensity;
    }

    // Clamp a value to stay within a specified range.
    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    // Normalize the intensity of a pixel to the range [0, 1].
    private double normalizeIntensity(double intensity, double minIntensity, double maxIntensity) {
        return (intensity - minIntensity) / (maxIntensity - minIntensity);
    }
}
