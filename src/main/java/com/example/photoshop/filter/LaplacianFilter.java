package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class LaplacianFilter extends Filters {

    private static final int[][] LAPLACIAN_FILTER = {
            {-4, -1, 0, -1, -4},
            {-1, 2, 3, 2, -1},
            {0, 3, 4, 3, 0},
            {-1, 2, 3, 2, -1},
            {-4, -1, 0, -1, -4}
    };
    public Image applyFilter(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage result = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = result.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = crossCorrelate(reader, x, y, width, height);
                writer.setColor(x, y, color);
            }
        }

        return result;
    }

    private Color crossCorrelate(PixelReader reader, int x, int y, int width, int height) {
        double red = 0, green = 0, blue = 0;
        int filterSize = LAPLACIAN_FILTER.length;

        for (int filterY = 0; filterY < filterSize; filterY++) {
            for (int filterX = 0; filterX < filterSize; filterX++) {
                int imageX = x - filterSize / 2 + filterX;
                int imageY = y - filterSize / 2 + filterY;

                if (imageX < 0 || imageX >= width || imageY < 0 || imageY >= height) {
                    continue;
                }

                Color pixelColor = reader.getColor(imageX, imageY);
                int filterValue = LAPLACIAN_FILTER[filterY][filterX];

                red += pixelColor.getRed() * filterValue;
                green += pixelColor.getGreen() * filterValue;
                blue += pixelColor.getBlue() * filterValue;
            }
        }

        red = (red + 4) / 8;
        green = (green + 4) / 8;
        blue = (blue + 4) / 8;

        return new Color(clamp(red), clamp(green), clamp(blue), 1.0);
    }

    private double clamp(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }
}