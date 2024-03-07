package com.example.photoshop.filter;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class GammaCorrectionFilter implements Filters {
    // Lookup table for gamma correction.
    private final double[] gammaLUT;

    /**
     * Constructor to initialize the gamma correction filter.
     * It creates a lookup table for fast gamma correction of pixel values.
     *
     * @param gamma The gamma value for correction. Must be positive.
     * @throws IllegalArgumentException if gamma is not positive.
     */
    public GammaCorrectionFilter(double gamma) {
        if (gamma <= 0) {
            throw new IllegalArgumentException("Gamma value must be positive");
        }
        this.gammaLUT = createGammaLUT(gamma);
    }

    /**
     * Applies gamma correction to an entire image.
     *
     * @param image The image to which the gamma correction is applied.
     * @return A new image with gamma correction applied.
     */
    @Override
    public Image applyFilter(Image image) {
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        WritableImage correctedImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = correctedImage.getPixelWriter();

        // Iterate over all pixels and apply gamma correction.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                writer.setColor(x, y, applyGammaCorrection(reader.getColor(x, y)));
            }
        }

        return correctedImage;
    }

    /**
     * Creates a lookup table (LUT) for gamma correction.
     *
     * @param gamma The gamma correction value.
     * @return An array representing the LUT for gamma correction.
     */
    private double[] createGammaLUT(double gamma) {
        double[] lut = new double[256];
        double inverseGamma = 1.0 / gamma;
        for (int i = 0; i < lut.length; i++) {
            lut[i] = Math.pow(i / 255.0, inverseGamma);
        }
        return lut;
    }

    // Applies gamma correction to a single color using the precomputed LUT.
    private Color applyGammaCorrection(Color color) {
        return new Color(
                gammaLUT[(int) (color.getRed() * 255)],
                gammaLUT[(int) (color.getGreen() * 255)],
                gammaLUT[(int) (color.getBlue() * 255)],
                color.getOpacity()
        );
    }
}
