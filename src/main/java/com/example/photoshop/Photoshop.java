package com.example.photoshop;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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

public class Photoshop extends Application {

    private final ImageView imageView = new ImageView();
    private Label statusLabel;
    private final ComboBox<String> interpolationComboBox = new ComboBox<>();
    private final ComboBox<String> filterComboBox = new ComboBox<>();
    private final Slider gammaSlider = new Slider(0.1, 5, 1);
    private final Slider resizeSlider = new Slider(0.1, 5.0, 1.0);
    private double initialX, initialY; // For tracking mouse drag
    private String currentInterpolationMethod = "Bilinear";
    private double zoomLevel = 1.0; // Default zoom level

    private String currentFilter = "None";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Button resetButton = new Button("Reset Image");
    private Future<?> lastTask; // To handle async image processing tasks
    private ProgressIndicator progressIndicator;

    public static void main(String[] args) {
        launch(args);
    }

    // Clamps a value between a specified range
    private double clamp(double value, double min, double max) {
        if (value < min) return min;
        return Math.min(value, max);
    }

    // Resets the image view to its original state
    private void resetImage(Image originalImage) {
        // Cancel any ongoing image processing task
        cancelPreviousTask();
        // Reset image transformations and settings
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
     * @param primaryStage Primary stage for this application.
     * @throws Exception if any error occurs during application start.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Image originalImage = loadImage();
        // Configure the reset button to reset the image view
        resetButton.setOnAction(event -> resetImage(originalImage));
        // Set up the initial image and UI components
        imageView.setImage(originalImage);
        setupComboBoxes(originalImage);
        setupImageView();
        VBox root = setupRoot(originalImage);
        Scene scene = new Scene(root, 1300, 1300);
        // Load and apply CSS stylesheet
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Loads the initial image to be displayed
    private Image loadImage() throws Exception {
        return new Image(new FileInputStream("src/main/java/com/example/photoshop/raytrace.jpg"));
    }

    // Sets up the combo boxes for selecting interpolation and filters
    private void setupComboBoxes(Image originalImage) {
        // Populate and set default values for interpolation methods
        interpolationComboBox.getItems().addAll(InterpolatorFactory.getInterpolatorNames());
        interpolationComboBox.setValue("Bilinear");
        interpolationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentInterpolationMethod = newValue;
            updateImageAsync(originalImage);
        });

        // Populate and set default values for image filters
        filterComboBox.getItems().addAll("None");
        filterComboBox.setValue("None");
        filterComboBox.getItems().addAll(FilterFactory.getFilterNames());
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentFilter = newValue;
            updateImageAsync(originalImage);
        });
    }
    // Sets up mouse event handlers for the ImageView.
    private void setupImageView() {
        // Handles mouse press events to initialize drag position.
        imageView.setOnMousePressed(event -> {
            initialX = event.getX();
            initialY = event.getY();
        });

        // Handles mouse drag events to move the image within the ImageView.
        imageView.setOnMouseDragged(this::handleDrag);

        // Handles scroll events to zoom in or out of the image.
        imageView.setOnScroll(this::handleScroll);
    }

    // Processes the dragging of the image within the ImageView.
    private void handleDrag(MouseEvent event) {
        // Calculates the offset of the drag.
        double offsetX = event.getX() - initialX;
        double offsetY = event.getY() - initialY;

        // Applies the new translation to the image, clamped within bounds.
        imageView.setTranslateX(clampTranslation(imageView.getTranslateX() + offsetX, imageView.getBoundsInParent().getWidth(), imageView.getFitWidth()));
        imageView.setTranslateY(clampTranslation(imageView.getTranslateY() + offsetY, imageView.getBoundsInParent().getHeight(), imageView.getFitHeight()));
    }

    // Clamps the translation of the image to keep it within the ImageView bounds.
    private double clampTranslation(double translation, double boundsDimension, double fitDimension) {
        double bound = Math.max(boundsDimension - fitDimension, 0) / 2;
        return clamp(translation, -bound, bound);
    }

    // Handles zooming in or out of the image on scroll events.
    private void handleScroll(ScrollEvent event) {
        double zoomFactor = event.getDeltaY() < 0 ? 0.95 : 1.05;
        zoomLevel *= zoomFactor;
        zoomLevel = clampScale(zoomLevel);

        double newResizeValue = zoomLevel;
        resizeSlider.setValue(newResizeValue);

        try {
            updateImageAsync(loadImage());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Clamps the scale of the image to predefined min and max values.
    private double clampScale(double scale) {
        final double minScale = 0.5;
        final double maxScale = 5.0;
        return Math.min(Math.max(scale, minScale), maxScale);
    }

    // Sets up the root VBox with all UI controls and the ImageView.
    private VBox setupRoot(Image originalImage) {
        HBox dropdownMenus = new HBox(10);
        dropdownMenus.getChildren().addAll(
                new Label("Interpolation Method"), interpolationComboBox,
                new Label("Filter"), filterComboBox
        );

        Label gammaValueLabel = new Label("Gamma: 1.00");
        Label resizeValueLabel = new Label("Resize: 1.00x");
        setupSliderWithDebounce(gammaSlider, gammaValueLabel, originalImage);
        setupSliderWithDebounce(resizeSlider, resizeValueLabel, originalImage);

        HBox gammaControls = new HBox(5);
        gammaControls.getChildren().addAll(new Label("Gamma Correction"), gammaSlider, gammaValueLabel);

        HBox resizeControls = new HBox(5);
        resizeControls.getChildren().addAll(new Label("Resize Image"), resizeSlider, resizeValueLabel);

        statusLabel = new Label("Status: Idle");
        progressIndicator = new ProgressIndicator();
        progressIndicator.setPrefSize(22, 22);
        progressIndicator.setVisible(false);

        HBox statusContainer = new HBox(5);
        statusContainer.getChildren().addAll(statusLabel, progressIndicator);

        HBox combinedControls = new HBox(10);
        combinedControls.getChildren().addAll(dropdownMenus, resetButton);
        combinedControls.setPadding(new Insets(10));

        VBox root = new VBox(10);
        root.setPadding(new Insets(15, 20, 15, 20));

        root.getChildren().addAll(
                gammaControls,
                resizeControls,
                combinedControls,
                statusContainer,
                imageView
        );
        return root;
    }

    // Sets up a slider with a listener to update the label and image based on the slider's value.
    private void setupSliderWithDebounce(Slider slider, Label valueLabel, Image originalImage) {
        slider.setShowTickLabels(true);
        slider.setShowTickMarks(true);
        // Add a listener to respond to slider value changes.
        slider.valueProperty().addListener((observable, oldValue, newValue) -> {
            // Determine the label and effect based on the slider's identity and new value.
            String label = slider == gammaSlider ? "Gamma" : "Resize";
            String effect = newValue.doubleValue() < 1.0 ? (slider == gammaSlider ? "Darker" : "Smaller") : (slider == gammaSlider ? "Brighter" : "Larger");
            // Update the value label to reflect the current state.
            valueLabel.setText(String.format("%s: %.4f (%s)", label, newValue.doubleValue(), effect));
            // Initiate an asynchronous update of the image.
            updateImageAsync(originalImage);
        });
    }

    // Updates the status label and progress indicator on the UI thread.
    private void updateStatusLabel(String text, boolean isProcessing) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            progressIndicator.setVisible(isProcessing);
        });
    }

    // Cancels the previous task if it's still running.
    private void cancelPreviousTask() {
        // Check if the last task is still active and cancel it if necessary.
        if (lastTask != null && !lastTask.isDone()) {
            lastTask.cancel(true);
        }
    }

    // Updates the image asynchronously using the current slider values.
    private void updateImageAsync(Image originalImage) {
        // Cancel any ongoing task to avoid conflicts.
        cancelPreviousTask();
        double currentScale = resizeSlider.getValue();
        double currentGamma = gammaSlider.getValue();

        // Update the status indicating the start of processing.
        updateStatusLabel("Starting processing...", true);

        // Submit a new task for image processing.
        lastTask = executorService.submit(() -> {
            updateStatusLabel("Processing image...", true);
            // Process the image with current parameters.
            Image processedImage = processImage(originalImage, currentScale, currentGamma);
            Platform.runLater(() -> {
                imageView.setImage(processedImage);
                updateStatusLabel("Processing complete", false);
            });
        });
    }

    // Processes the image by applying gamma correction and resizing.
    private Image processImage(Image originalImage, double scale, double gamma) {        // Apply gamma correction filter.
        Image filteredImage = applyFilters(originalImage, gamma);
        // Calculate new dimensions for resizing.
        int newWidth = (int) (filteredImage.getWidth() * scale);
        int newHeight = (int) (filteredImage.getHeight() * scale);

        // Prepare for writing the resized image.
        WritableImage resizedImage = new WritableImage(newWidth, newHeight);
        PixelReader reader = filteredImage.getPixelReader();
        PixelWriter writer = resizedImage.getPixelWriter();

        // Create an interpolator for resizing.
        Interpolator interpolator = InterpolatorFactory.createInterpolator(currentInterpolationMethod);

        // Resize the image using the interpolator.
        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double scaleX = (x / scale);
                double scaleY = (y / scale);

                Color color = interpolator.interpolate(reader, scaleX, scaleY, (int) filteredImage.getWidth(), (int) filteredImage.getHeight());
                writer.setColor(x, y, color);
            }
        }

        return resizedImage;
    }

    // Applies gamma correction and other filters to the image.
    private Image applyFilters(Image image, double currentGamma) {
        if (currentGamma != 1.0) {
            image = new GammaCorrectionFilter(currentGamma).applyFilter(image);
        }

        // Apply additional filters if selected.
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