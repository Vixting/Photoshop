package com.example.photoshop;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.util.Objects;
import java.util.stream.IntStream;

public class Photoshop extends Application {

    private final ImageView imageView = new ImageView();
    private final ComboBox<String> interpolationComboBox = new ComboBox<>();
    private final ComboBox<String> filterComboBox = new ComboBox<>();
    private final Slider gammaSlider = new Slider(0.1, 2.0, 1.0);
    private final Slider resizeSlider = new Slider(0.1, 2.0, 1.0);
    private String currentInterpolationMethod = "Bilinear";
    private String currentFilter = "None";
    private double lastScale = 1.0;
    private static final int[][] LAPLACIAN_FILTER = {
            {-4, -1, 0, -1, -4},
            {-1, 2, 3, 2, -1},
            {0, 3, 4, 3, 0},
            {-1, 2, 3, 2, -1},
            {-4, -1, 0, -1, -4}
    };

    public static void main(String[] args) {
        launch(args);
    }

    private final PauseTransition debouncePause = new PauseTransition(Duration.millis(1));

    private void setupSliderWithDebounce(Slider slider, Label valueLabel, Image originalImage) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);

        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (slider == gammaSlider) {
                String gammaEffect = newValue.doubleValue() < 1.0 ? "Darker" : "Brighter";
                valueLabel.setText(String.format("Gamma: %.2f (%s)", newValue.doubleValue(), gammaEffect));
            } else if (slider == resizeSlider) {
                String resizeEffect = newValue.doubleValue() < 1.0 ? "Smaller" : "Larger";
                lastScale = newValue.doubleValue();

                valueLabel.setText(String.format("Resize: %.2fx (%s)", newValue.doubleValue(), resizeEffect));
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

        Label gammaValueLabel = new Label("Gamma: 1.00");
        Label resizeValueLabel = new Label("Resize: 1.00x");

        setupSliderWithDebounce(gammaSlider, gammaValueLabel, originalImage);
        setupSliderWithDebounce(resizeSlider, resizeValueLabel, originalImage);

        interpolationComboBox.getItems().addAll("Nearest Neighbor", "Bilinear");
        interpolationComboBox.setValue("Bilinear");
        interpolationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentInterpolationMethod = newValue;
            updateImage(originalImage);
        });

        filterComboBox.getItems().addAll("None", "Laplacian");
        filterComboBox.setValue("None");
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentFilter = newValue;
            updateImage(originalImage);
        });

        HBox dropdownMenus = new HBox(10);
        dropdownMenus.getChildren().addAll(
                new Label("Interpolation Method"), interpolationComboBox,
                new Label("Filter"), filterComboBox
        );

        VBox root = new VBox(10);
        root.setPadding(new Insets(15, 20, 15, 20));
        root.getChildren().addAll(
                new Label("Gamma Correction"), gammaSlider, gammaValueLabel,
                new Label("Resize Image"), resizeSlider, resizeValueLabel,
                dropdownMenus,
                imageView
        );

        gammaSlider.setMajorTickUnit(1);
        gammaSlider.setMinorTickCount(4);

        resizeSlider.setMajorTickUnit(1);
        resizeSlider.setMinorTickCount(4);

        Scene scene = new Scene(root, 1300, 1300);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void updateImage(Image originalImage) {
        int originalWidth = (int) originalImage.getWidth();
        int originalHeight = (int) originalImage.getHeight();
        int newWidth = (int) (originalWidth * lastScale);
        int newHeight = (int) (originalHeight * lastScale);
        WritableImage resizedImage = new WritableImage(newWidth, newHeight);
        PixelReader reader = originalImage.getPixelReader();
        PixelWriter writer = resizedImage.getPixelWriter();

        IntStream.range(0, newHeight).parallel().forEach(y -> {
            for (int x = 0; x < newWidth; x++) {
                double scaleX = x / lastScale;
                double scaleY = y / lastScale;
                Color color;
                if (currentInterpolationMethod.equals("Nearest Neighbor")) {
                    color = getColorSafe(reader, (int) scaleX, (int) scaleY, originalWidth, originalHeight);
                } else {
                    color = bilinearInterpolate(reader, scaleX, scaleY, originalWidth, originalHeight);
                }
                synchronized (writer) {
                    writer.setColor(x, y, color);
                }
            }
        });

        Image processedImage = applyGammaCorrection(resizedImage, gammaSlider.getValue());
        if (currentFilter.equals("Laplacian")) {
            processedImage = applyLaplacianFilter(processedImage);
        }

        Image finalProcessedImage = processedImage;
        Platform.runLater(() -> imageView.setImage(finalProcessedImage));
    }


    private Color bilinearInterpolate(PixelReader reader, double x, double y, int maxWidth, int maxHeight) {
        int xFloor = (int) x, yFloor = (int) y;
        double xFraction = x - xFloor, yFraction = y - yFloor;

        Color topLeft = getColorSafe(reader, xFloor, yFloor, maxWidth, maxHeight),
                topRight = getColorSafe(reader, xFloor + 1, yFloor, maxWidth, maxHeight),
                bottomLeft = getColorSafe(reader, xFloor, yFloor + 1, maxWidth, maxHeight),
                bottomRight = getColorSafe(reader, xFloor + 1, yFloor + 1, maxWidth, maxHeight);

        Color topInterpolated = linearInterpolate(topLeft, topRight, xFraction);
        Color bottomInterpolated = linearInterpolate(bottomLeft, bottomRight, xFraction);

        return linearInterpolate(topInterpolated, bottomInterpolated, yFraction);
    }

    private Color getColorSafe(PixelReader reader, int x, int y, int maxWidth, int maxHeight) {
        if (x >= 0 && y >= 0 && x < maxWidth && y < maxHeight) {
            return reader.getColor(x, y);
        }
        return Color.BLACK;
    }

    private Color linearInterpolate(Color start, Color end, double fraction) {
        return new Color(
                start.getRed() + fraction * (end.getRed() - start.getRed()),
                start.getGreen() + fraction * (end.getGreen() - start.getGreen()),
                start.getBlue() + fraction * (end.getBlue() - start.getBlue()), 1.0
        );
    }

    private Image applyGammaCorrection(Image image, double gamma) {
        int width = (int) image.getWidth(), height = (int) image.getHeight();
        WritableImage correctedImage = new WritableImage(width, height);
        PixelReader reader = image.getPixelReader();
        PixelWriter writer = correctedImage.getPixelWriter();

        double inverseGamma = 1 / gamma;
        double[] gammaLUT = new double[256];
        for (int i = 0; i < 256; i++) {
            gammaLUT[i] = Math.pow(i / 255.0, inverseGamma);
        }

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color originalColor = reader.getColor(x, y);
                writer.setColor(x, y, new Color(
                        gammaLUT[(int) (originalColor.getRed() * 255)],
                        gammaLUT[(int) (originalColor.getGreen() * 255)],
                        gammaLUT[(int) (originalColor.getBlue() * 255)], 1.0
                ));
            }
        }
        return correctedImage;
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
