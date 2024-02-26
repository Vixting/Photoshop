package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

/**
 * This class represents a gamma correction filter.
 * It implements the Filters interface and overrides the applyFilter method to apply gamma correction to an image.
 */
public class GammaCorrectionFilter implements Filters {
    private final double[] gammaLUT;

    /**
     * Constructor for the GammaCorrectionFilter class.
     * It takes a gamma value as input and creates a lookup table (LUT) for gamma correction.
     *
     * @param gamma The gamma value for the gamma correction. It must be a positive number.
     * @throws IllegalArgumentException if the gamma value is not positive.
     */
    public GammaCorrectionFilter(double gamma) {
        if (gamma <= 0) {
            throw new IllegalArgumentException("Gamma value must be positive");
        }
        this.gammaLUT = createGammaLUT(gamma);
    }

    /**
     * This method applies the gamma correction filter to an image.
     *
     * @param image The image to which the filter is to be applied.
     * @return The image after applying the gamma correction filter.
     */
    @Override
    public Image applyFilter(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage correctedImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = correctedImage.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, applyGammaCorrection(reader.getColor(x, y)));
            }
        }

        return correctedImage;
    }

    private double[] createGammaLUT(double gamma) {
        double[] lut = new double[256];
        double inverseGamma = 1.0 / gamma;
        for (int i = 0; i < lut.length; i++) {
            lut[i] = Math.pow(i / 255.0, inverseGamma);
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
}