package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GammaCorrectionFilter implements Filters {
    private final double[] gammaLUT;

    public GammaCorrectionFilter(double gamma) {
        if (gamma <= 0) {
            throw new IllegalArgumentException("Gamma value must be positive");
        }
        this.gammaLUT = createGammaLUT(gamma);
    }

    @Override
    public Image applyFilter(Image image) {
        System.out.println("Applying gamma correction filter");
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage correctedImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = correctedImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color originalColor = getColor(reader, x, y);
                writer.setColor(x, y, applyGammaCorrection(originalColor));
            }
        }

        return correctedImage;
    }

    private double[] createGammaLUT(double gamma) {
        double[] lut = new double[256];
        double inverseGamma = 1.0 / gamma;
        for (int i = 0; i < 256; i++) {
            lut[i] = Math.pow((double) i / 255.0, inverseGamma);
        }
        return lut;
    }

    private Color applyGammaCorrection(Color color) {
        return new Color(
                gammaLUT[(int) (color.getRed() * 255)],
                gammaLUT[(int) (color.getGreen() * 255)],
                gammaLUT[(int) (color.getBlue() * 255)],
                color.getOpacity()
        );
    }

    private Color getColor(PixelReader reader, int x, int y) {
        try {
            return reader.getColor(x, y);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }
}
