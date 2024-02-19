package com.example.photoshop;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.util.stream.IntStream;

public class Photoshop extends Application {

    private final ImageView imageView = new ImageView();
    private final CheckBox nnCheckbox = new CheckBox("Use Nearest Neighbour");
    private final Label interpolationLabel = new Label("Interpolation: Bilinear");
    private final Slider gammaSlider = new Slider(0.1, 3.0, 1.0);
    private final Slider resizeSlider = new Slider(0.1, 2.0, 1.0);
    private final Button laplacianButton = new Button("Toggle Laplacian Filter");
    private boolean applyLaplacian = false;
    private double lastScale = 1.0;
    private static final int[][] LAPLACIAN_FILTER = {
            {-4,-1, 0,-1,-4},
            {-1, 2, 3, 2,-1},
            { 0, 3, 4, 3, 0},
            {-1, 2, 3, 2,-1},
            {-4,-1, 0,-1,-4}
    };

    public static void main(String[] args) {
        launch(args);
    }

    private final PauseTransition debouncePause = new PauseTransition(Duration.millis(1));

    private void setupSliderWithDebounce(Slider slider, Label valueLabel, Image originalImage) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (slider == resizeSlider) {
                lastScale = newValue.doubleValue();
                valueLabel.setText(String.format("Resize: %.2fx", newValue.doubleValue()));
            } else if (slider == gammaSlider) {
                valueLabel.setText(String.format("Gamma: %.2f", newValue.doubleValue()));
            }

            debouncePause.setOnFinished(event -> updateImage(originalImage));
            debouncePause.playFromStart();
        });
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Mark's CS-256 application");
        Image originalImage = new Image(new FileInputStream("src/main/java/com/example/photoshop/raytrace.jpg"));
        imageView.setImage(originalImage);

        Label gammaValueLabel = new Label("Gamma: 1.00"); // Initial value for gamma
        Label resizeValueLabel = new Label("Resize: 1.00x"); // Initial value for resize

        setupSliderWithDebounce(gammaSlider, gammaValueLabel, originalImage);
        setupSliderWithDebounce(resizeSlider, resizeValueLabel, originalImage);

        nnCheckbox.selectedProperty().addListener(e -> {
            interpolationLabel.setText("Interpolation: " + (nnCheckbox.isSelected() ? "Nearest Neighbour" : "Bilinear"));
            updateImage(originalImage);
        });

        laplacianButton.setOnAction(e -> {
            applyLaplacian = !applyLaplacian;
            updateImage(originalImage);
        });

        VBox root = new VBox(10); // added spacing
        root.setPadding(new Insets(15, 20, 15, 20)); // added padding
        root.getChildren().addAll(
                new Label("Gamma Correction"), gammaSlider, gammaValueLabel,
                new Label("Resize Image"), resizeSlider, resizeValueLabel,
                nnCheckbox, interpolationLabel,
                laplacianButton, imageView
        );

        primaryStage.setScene(new Scene(root, 400, 600));
        primaryStage.show();
    }


    private void updateImage(Image originalImage) {
        int width = (int) originalImage.getWidth(), height = (int) originalImage.getHeight();
        WritableImage resized = new WritableImage((int) (width * lastScale), (int) (height * lastScale));
        PixelReader reader = originalImage.getPixelReader();
        PixelWriter writer = resized.getPixelWriter();
        int newWidth = (int) resized.getWidth(), newHeight = (int) resized.getHeight();

        IntStream.range(0, newHeight).parallel().forEach(y -> {
            for (int x = 0; x < newWidth; x++) {
                double scaleX = x / lastScale, scaleY = y / lastScale;
                Color color = nnCheckbox.isSelected() ?
                        getColorSafe(reader, (int) scaleX, (int) scaleY, width, height) :
                        bilinearInterpolation(reader, scaleX, scaleY, width, height);
                synchronized (writer) {
                    writer.setColor(x, y, color);
                }
            }
        });

        Image processedImage = applyGammaCorrection(resized, gammaSlider.getValue());
        if (applyLaplacian) {
            processedImage = applyLaplacianFilter(processedImage);
        }
        Image finalProcessedImage = processedImage;
        Platform.runLater(() -> imageView.setImage(finalProcessedImage));
    }

    private Color bilinearInterpolation(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int xBase = (int) x, yBase = (int) y;
        double xFraction = x - xBase, yFraction = y - yBase;

        Color c00 = getColorSafe(reader, xBase, yBase, maxWidth, maxHeight),
                c01 = getColorSafe(reader, xBase, yBase + 1, maxWidth, maxHeight),
                c10 = getColorSafe(reader, xBase + 1, yBase, maxWidth, maxHeight),
                c11 = getColorSafe(reader, xBase + 1, yBase + 1, maxWidth, maxHeight);

        return lerp(lerp(c00, c10, xFraction), lerp(c01, c11, xFraction), yFraction);
    }

    private Color getColorSafe(PixelReader reader, int x, int y, int maxWidth, int maxHeight) {
        if (x >= 0 && y >= 0 && x < maxWidth && y < maxHeight) return reader.getColor(x, y);
        return Color.BLACK;
    }

    private Color lerp(Color start, Color end, double ratio) {
        return new Color(
                start.getRed() + ratio * (end.getRed() - start.getRed()),
                start.getGreen() + ratio * (end.getGreen() - start.getGreen()),
                start.getBlue() + ratio * (end.getBlue() - start.getBlue()), 1.0
        );
    }

    private Image applyGammaCorrection(Image image, double gamma) {
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        WritableImage writableImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = writableImage.getPixelWriter();

        double[] gammaLUT = new double[256];
        for (int i = 0; i < 256; i++) {
            gammaLUT[i] = Math.pow(i / 255.0, gamma);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color color = reader.getColor(x, y);
                writer.setColor(x, y, new Color(
                        gammaLUT[(int) (color.getRed() * 255)],
                        gammaLUT[(int) (color.getGreen() * 255)],
                        gammaLUT[(int) (color.getBlue() * 255)], 1.0
                ));
            }
        }
        return writableImage;
    }


    private Color crossCorrelate(PixelReader reader, int x, int y, int width, int height) {
        double red = 0, green = 0, blue = 0;

        for (int filterY = 0; filterY < 5; filterY++) {
            for (int filterX = 0; filterX < 5; filterX++) {
                int imageX = Math.min(Math.max(x - 2 + filterX, 0), width - 1);
                int imageY = Math.min(Math.max(y - 2 + filterY, 0), height - 1);
                Color pixelColor = reader.getColor(imageX, imageY);

                int filterValue = LAPLACIAN_FILTER[filterY][filterX];
                red += pixelColor.getRed() * filterValue;
                green += pixelColor.getGreen() * filterValue;
                blue += pixelColor.getBlue() * filterValue;
            }
        }

        return new Color(clamp(red), clamp(green), clamp(blue), 1.0);
    }

    private double clamp(double value) {
        if (value < 0.0) return 0.0;
        return Math.min(value, 1.0);
    }

    private Image applyLaplacianFilter(Image image) {
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
}
