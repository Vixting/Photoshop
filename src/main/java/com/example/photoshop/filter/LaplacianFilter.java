package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class LaplacianFilter implements Filters {

    // Define a 5x5 matrix as the kernel for the Laplacian filter. This matrix is used to calculate
    // the new intensity of each pixel based on its neighbors.
    private static final int[][] LAPLACIAN_FILTER = {
            {-4, -1, 0, -1, -4},
            {-1, 2, 3, 2, -1},
            {0, 3, 4, 3, 0},
            {-1, 2, 3, 2, -1},
            {-4, -1, 0, -1, -4}
    };

    @Override
    public Image applyFilter(Image image) {
        // Retrieve the dimensions of the input image.
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        // Create a new writable image to store the filtered result.
        WritableImage result = new WritableImage(width, height);

        // PixelReader and PixelWriter are used to read and write pixel data respectively.
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        // Initialize arrays to store the minimum and maximum intensities for each color channel.
        // This will later be used for normalization.
        double[][] minMaxIntensity = {
                {Double.MAX_VALUE, Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MIN_VALUE},
                {Double.MAX_VALUE, Double.MIN_VALUE}
        };

        // This 3D array stores the calculated intensity values for each pixel and each color channel.
        double[][][] intensities = new double[height][width][3];

        // Loop through each pixel of the image.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Process each color channel (Red, Green, Blue) separately.
                for (int i = 0; i < 3; i++) {
                    // Apply the kernel to the current pixel and color channel.
                    double intensity = applyKernel(reader, x, y, width, height, i);
                    // Store the intensity in the array.
                    intensities[y][x][i] = intensity;
                    // Update the minimum and maximum values for normalization.
                    minMaxIntensity[i][0] = Math.min(minMaxIntensity[i][0], intensity);
                    minMaxIntensity[i][1] = Math.max(minMaxIntensity[i][1], intensity);
                }
            }
        }

        // Normalize the intensities to the range [0, 1] and set the pixel colors in the result image.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, new Color(
                        normalizeIntensity(intensities[y][x][0], minMaxIntensity[0][0], minMaxIntensity[0][1]),
                        normalizeIntensity(intensities[y][x][1], minMaxIntensity[1][0], minMaxIntensity[1][1]),
                        normalizeIntensity(intensities[y][x][2], minMaxIntensity[2][0], minMaxIntensity[2][1]),
                        1.0));
            }
        }

        // Return the processed image.
        return result;
    }

    // Method to apply the Laplacian kernel to a specific pixel and color channel.
    private double applyKernel(PixelReader reader, int x, int y, int width, int height, int colorIndex) {
        double intensity = 0;
        int kernelSize = LAPLACIAN_FILTER.length;
        // Iterate over the kernel's cells.
        for (int dy = 0; dy < kernelSize; dy++) {
            for (int dx = 0; dx < kernelSize; dx++) {
                // Calculate the coordinates of the image pixel corresponding to the current kernel cell.
                int imageX = clamp(x - kernelSize / 2 + dx, 0, width - 1);
                int imageY = clamp(y - kernelSize / 2 + dy, 0, height - 1);
                // Read the color of the pixel.
                Color color = reader.getColor(imageX, imageY);
                // Select the appropriate color channel based on colorIndex.
                double value = (colorIndex == 0) ? color.getRed() : (colorIndex == 1) ? color.getGreen() : color.getBlue();
                // Multiply the kernel value with the intensity and accumulate.
                intensity += value * LAPLACIAN_FILTER[dy][dx];
            }
        }
        return intensity;
    }

    // Utility method to ensure a value is within a specified range.
    private int clamp(int value, int min, int max) {
        // Clamp the value to be within the range [min, max].
        return Math.max(min, Math.min(value, max));
    }

    // Method to normalize the intensity of a pixel.
    private double normalizeIntensity(double intensity, double minIntensity, double maxIntensity) {
        // Normalize the intensity value to be within the range [0, 1].
        return (intensity - minIntensity) / (maxIntensity - minIntensity);
    }
}
