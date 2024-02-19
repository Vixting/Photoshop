package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GammaCorrectionFilter extends Filters {
    private final double gamma;

    public GammaCorrectionFilter(double gamma) {
        this.gamma = gamma;
    }

    @Override
    public Image applyFilter(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage correctedImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = correctedImage.getPixelWriter();

        double[] gammaLUT = createGammaLUT(gamma);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color originalColor = reader.getColor(x, y);
                writer.setColor(x, y, applyGammaCorrection(originalColor, gammaLUT));
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

    private Color applyGammaCorrection(Color color, double[] lut) {
        return new Color(
                lut[(int) (color.getRed() * 255)],
                lut[(int) (color.getGreen() * 255)],
                lut[(int) (color.getBlue() * 255)],
                color.getOpacity()
        );
    }
}

