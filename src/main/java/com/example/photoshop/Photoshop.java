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
    private final Slider gammaSlider = new Slider(0.1, 5.0, 1.0);
    private final Slider resizeSlider = new Slider(0.1, 5.0, 1.0);
    private double initialX;
    private double initialY;
    private String currentInterpolationMethod = "Bilinear";
    private String currentFilter = "None";
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Button resetButton = new Button("Reset Image");
    private Future<?> lastTask;
    private ProgressIndicator progressIndicator;

    public static void main(String[] args) {
        launch(args);
    }

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
     * @param primaryStage Primary stage for this application.
     * @throws Exception if any error occurs during application start.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Image originalImage = loadImage();
        resetButton.setOnAction(event -> resetImage(originalImage));
        imageView.setImage(originalImage);
        setupComboBoxes(originalImage);
        setupImageView();
        VBox root = setupRoot(originalImage);
        Scene scene = new Scene(root, 1300, 1300);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Image loadImage() throws Exception {
        return new Image(new FileInputStream("src/main/java/com/example/photoshop/raytrace.jpg"));
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

        imageView.setOnMouseDragged(this::handleDrag);

        imageView.setOnScroll(this::handleScroll);
    }

    private void handleDrag(MouseEvent event) {
        double offsetX = event.getX() - initialX;
        double offsetY = event.getY() - initialY;
        double newTranslateX = imageView.getTranslateX() + offsetX;
        double newTranslateY = imageView.getTranslateY() + offsetY;

        newTranslateX = clampTranslation(newTranslateX, imageView.getBoundsInParent().getWidth(), imageView.getFitWidth());
        newTranslateY = clampTranslation(newTranslateY, imageView.getBoundsInParent().getHeight(), imageView.getFitHeight());

        imageView.setTranslateX(newTranslateX);
        imageView.setTranslateY(newTranslateY);
    }

    private double clampTranslation(double translation, double boundsDimension, double fitDimension) {
        double bound = Math.max(boundsDimension - fitDimension, 0) / 2;
        return clamp(translation, -bound, bound);
    }

    private void handleScroll(ScrollEvent event) {
        double zoomFactor = 1.05;
        if (event.getDeltaY() < 0) {
            zoomFactor = 1 / zoomFactor;
        }

        double newScaleX = imageView.getScaleX() * zoomFactor;
        double newScaleY = imageView.getScaleY() * zoomFactor;

        newScaleX = clampScale(newScaleX);
        newScaleY = clampScale(newScaleY);

        imageView.setScaleX(newScaleX);
        imageView.setScaleY(newScaleY);
    }

    private double clampScale(double scale) {
        double minScale = 0.5;
        double maxScale = 5.0;
        return Math.min(Math.max(scale, minScale), maxScale);
    }

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

        updateStatusLabel("Starting processing...", true);

        lastTask = executorService.submit(() -> {
            updateStatusLabel("Processing image...", true);
            Image processedImage = processImage(originalImage, currentScale, currentGamma);
            Platform.runLater(() -> {
                imageView.setImage(processedImage);
                updateStatusLabel("Processing complete", false);
            });
        });
    }

    private void updateStatusLabel(String text, boolean isProcessing) {
        Platform.runLater(() -> {
            statusLabel.setText(text);
            progressIndicator.setVisible(isProcessing);
        });
    }


    private void cancelPreviousTask() {
        if (lastTask != null && !lastTask.isDone()) {
            lastTask.cancel(true);
        }
    }

    private Image processImage(Image originalImage, double scale, double gamma) {
        int originalWidth = (int) originalImage.getWidth();
        int originalHeight = (int) originalImage.getHeight();

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        WritableImage resizedImage = new WritableImage(newWidth, newHeight);
        PixelReader reader = originalImage.getPixelReader();
        PixelWriter writer = resizedImage.getPixelWriter();

        Interpolator interpolator = InterpolatorFactory.createInterpolator(currentInterpolationMethod);

        for (int y = 0; y < newHeight; y++) {
            for (int x = 0; x < newWidth; x++) {
                double scaleX = (x / scale);
                double scaleY = (y / scale);

                Color color = interpolator.interpolate(reader, scaleX, scaleY, originalWidth, originalHeight);
                writer.setColor(x, y, color);
            }
        }

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