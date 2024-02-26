package com.example.photoshop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import com.example.photoshop.filter.FilterFactory;
import com.example.photoshop.filter.Filters;
import com.example.photoshop.filter.GammaCorrectionFilter;
import com.example.photoshop.interploators.Interpolator;
import com.example.photoshop.interploators.InterpolatorFactory;
import java.io.FileInputStream;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Photoshop is a JavaFX application for image processing. It allows users to apply various
 * filters, perform gamma correction, resize images, and adjust using different interpolation methods.
 * The application also supports basic image manipulation like zooming and dragging.
 */
public class Photoshop extends Application {

    private final ImageView imageView = new ImageView();
    private final ComboBox<String> interpolationComboBox = new ComboBox<>();
    private final ComboBox<String> filterComboBox = new ComboBox<>();
    private final Slider gammaSlider = new Slider(0.1, 5.0, 1.0);
    private final Slider resizeSlider = new Slider(0.1, 5.0, 1.0);
    private double initialX;
    private double initialY;
    private String currentInterpolationMethod = "Bilinear";
    private String currentFilter = "None";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Button resetButton = new Button("Reset Image");
    private Future<?> lastTask;

    public static void main(String[] args) {
        launch(args);
    }

    /**
     * Clamps a given value within a specified range.
     *
     * @param value The value to clamp.
     * @param min   The minimum allowed value.
     * @param max   The maximum allowed value.
     * @return The clamped value.
     */
    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    private void resetImage(Image originalImage) {
        if (lastTask != null && !lastTask.isDone()) {
            lastTask.cancel(true);
        }
        imageView.setImage(originalImage);
        imageView.setScaleX(1.0);
        imageView.setScaleY(1.0);
        imageView.setTranslateX(0);
        imageView.setTranslateY(0);
        gammaSlider.setValue(1.0);
        resizeSlider.setValue(1.0);
        interpolationComboBox.setValue("Bilinear");
        filterComboBox.setValue("None");
    }

    /**
     * Initializes and displays the JavaFX application window.
     *
     * @param primaryStage The primary stage for this application.
     * @throws Exception if any error occurs during application start.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Mark's CS-256 application");
        Image originalImage = loadImage();
        resetButton.setOnAction(event -> resetImage(originalImage));
        imageView.setImage(originalImage);
        setupSliders(originalImage);
        setupComboBoxes(originalImage);
        setupImageView();
        VBox root = setupRoot();
        Scene scene = new Scene(root, 1300, 1300);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Image loadImage() throws Exception {
        return new Image(new FileInputStream("src/main/java/com/example/photoshop/raytrace.jpg"));
    }

    private void setupSliders(Image originalImage) {
        Label gammaValueLabel = new Label("Gamma: 1.00");
        Label resizeValueLabel = new Label("Resize: 1.00x");
        setupSliderWithDebounce(gammaSlider, gammaValueLabel, originalImage);
        setupSliderWithDebounce(resizeSlider, resizeValueLabel, originalImage);
    }

    private void setupComboBoxes(Image originalImage) {
        interpolationComboBox.getItems().addAll(InterpolatorFactory.getInterpolatorNames());
        interpolationComboBox.setValue("Bilinear");
        interpolationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentInterpolationMethod = newValue;
            updateImageAsync(originalImage);
        });

        filterComboBox.getItems().addAll("None");
        filterComboBox.setValue("None");
        filterComboBox.getItems().addAll(FilterFactory.getFilterNames());
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentFilter = newValue;
            updateImageAsync(originalImage);
        });
    }

    private void setupImageView() {
        imageView.setOnMousePressed(event -> {
            initialX = event.getX();
            initialY = event.getY();
        });

        imageView.setOnMouseDragged(event -> {
            double offsetX = event.getX() - initialX;
            double offsetY = event.getY() - initialY;
            double newTranslateX = imageView.getTranslateX() + offsetX;
            double newTranslateY = imageView.getTranslateY() + offsetY;
            double widthBound = Math.max(imageView.getBoundsInParent().getWidth() - imageView.getFitWidth(), 0) / 2;
            double heightBound = Math.max(imageView.getBoundsInParent().getHeight() - imageView.getFitHeight(), 0) / 2;
            newTranslateX = clamp(newTranslateX, -widthBound, widthBound);
            newTranslateY = clamp(newTranslateY, -heightBound, heightBound);
            imageView.setTranslateX(newTranslateX);
            imageView.setTranslateY(newTranslateY);
        });

        imageView.setOnScroll(event -> {
            double zoomFactor = 1.05;
            double deltaY = event.getDeltaY();
            if (deltaY < 0){
                zoomFactor = 1 / zoomFactor;
            }
            double newScaleX = imageView.getScaleX() * zoomFactor;
            double newScaleY = imageView.getScaleY() * zoomFactor;
            double minScale = 0.5;
            double maxScale = 5.0;
            if (newScaleX >= minScale && newScaleX <= maxScale && newScaleY >= minScale && newScaleY <= maxScale) {
                imageView.setScaleX(newScaleX);
                imageView.setScaleY(newScaleY);
            }
        });
    }

    private VBox setupRoot() {
        HBox dropdownMenus = new HBox(10);
        dropdownMenus.getChildren().addAll(
                new Label("Interpolation Method"), interpolationComboBox,
                new Label("Filter"), filterComboBox
        );

        HBox controls = new HBox(10, dropdownMenus, resetButton);
        controls.setPadding(new Insets(10));

        VBox root = new VBox(10);
        root.setPadding(new Insets(15, 20, 15, 20));

        root.getChildren().addAll(
                new Label("Gamma Correction"), gammaSlider,
                new Label("Resize Image"), resizeSlider,
                dropdownMenus,
                imageView,
                controls
        );
        return root;
    }
    private void setupSliderWithDebounce(Slider slider, Label valueLabel, Image originalImage) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            String label = slider == gammaSlider ? "Gamma" : "Resize";
            String effect = newValue.doubleValue() < 1.0 ? (slider == gammaSlider ? "Darker" : "Smaller") : (slider == gammaSlider ? "Brighter" : "Larger");
            valueLabel.setText(String.format("%s: %.2f (%s)", label, newValue.doubleValue(), effect));
            updateImageAsync(originalImage);
        });
    }

    private void updateImageAsync(Image originalImage) {
        cancelPreviousTask();
        double currentScale = resizeSlider.getValue();
        double currentGamma = gammaSlider.getValue();

        lastTask = executorService.submit(() -> {
            Image processedImage = processImage(originalImage, currentScale, currentGamma);
            Platform.runLater(() -> imageView.setImage(processedImage));
        });
    }

    private void cancelPreviousTask() {
        if (lastTask != null && !lastTask.isDone()) {
            lastTask.cancel(true);
        }
    }

    /**
     * This method processes the original image by resizing it based on the provided scale and applying gamma correction.
     * It also applies any selected interpolation method and filter to the image.
     *
     * @param originalImage The original image to be processed.
     * @param scale The scale factor to be used for resizing the image. A scale of 1.0 means the image size remains the same.
     * @param gamma The gamma correction factor to be applied to the image. A gamma of 1.0 means no gamma correction is applied.
     * @return The processed image after resizing, gamma correction, and applying the selected interpolation method and filter.
     */
    private Image processImage(Image originalImage, double scale, double gamma) {
        // Get the original image dimensions
        int originalWidth = (int) originalImage.getWidth();
        int originalHeight = (int) originalImage.getHeight();

        // Calculate the new dimensions based on the scale factor
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // Create a new writable image with the new dimensions
        WritableImage resizedImage = new WritableImage(newWidth, newHeight);

        // Get the pixel reader and writer for the original and new image respectively
        PixelReader reader = originalImage.getPixelReader();
        PixelWriter writer = resizedImage.getPixelWriter();

        // Create an interpolator based on the selected interpolation method
        Interpolator interpolator = InterpolatorFactory.createInterpolator(currentInterpolationMethod);

        // Loop over each pixel in the new image
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                // Calculate the corresponding coordinates in the original image
                double scaleX = (x / scale);
                double scaleY = (y / scale);

                // Get the color of the corresponding pixel in the original image using the interpolator
                Color color = interpolator.interpolate(reader, scaleX, scaleY, originalWidth, originalHeight);

                // Set the color of the pixel in the new image
                writer.setColor(x, y, color);
            }
        }

        // Apply the selected filter and gamma correction to the new image and return it
        return applyFilters(resizedImage, gamma);
    }

    private Image applyFilters(Image image, double currentGamma) {
        if (currentGamma != 1.0) {
            image = new GammaCorrectionFilter(currentGamma).applyFilter(image);
        }
        if (!"None".equals(currentFilter)) {
            Filters filter = FilterFactory.createFilter(currentFilter, currentGamma);
            image = filter.applyFilter(image);
        }
        return image;
    }

    /**
     * Invoked when the application should stop, and provides a chance to close resources.
     *
     * @throws Exception if any error occurs during application stop.
     */
    @Override
    public void stop() throws Exception {
        executorService.shutdown();
        super.stop();
    }
}
