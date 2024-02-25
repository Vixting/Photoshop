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

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Mark's CS-256 application");
        Image originalImage = new Image(new FileInputStream("src/main/java/com/example/photoshop/raytrace.jpg"));
        resetButton.setOnAction(event -> resetImage(originalImage));
        imageView.setImage(originalImage);
        Label gammaValueLabel = new Label("Gamma: 1.00");
        Label resizeValueLabel = new Label("Resize: 1.00x");
        setupSliderWithDebounce(gammaSlider, gammaValueLabel, originalImage);
        setupSliderWithDebounce(resizeSlider, resizeValueLabel, originalImage);
        interpolationComboBox.getItems().addAll(InterpolatorFactory.getInterpolatorNames());
        interpolationComboBox.setValue("Bilinear");
        interpolationComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentInterpolationMethod = newValue;
            updateImageAsync(originalImage);
        });
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
        filterComboBox.getItems().addAll("None");
        filterComboBox.setValue("None");
        filterComboBox.getItems().addAll(FilterFactory.getFilterNames());
        filterComboBox.valueProperty().addListener((observable, oldValue, newValue) -> {
            currentFilter = newValue;
            updateImageAsync(originalImage);
        });
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
                new Label("Gamma Correction"), gammaSlider, gammaValueLabel,
                new Label("Resize Image"), resizeSlider, resizeValueLabel,
                dropdownMenus,
                imageView,
                controls
        );
        Scene scene = new Scene(root, 1300, 1300);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/style.css")).toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
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
        if (lastTask != null && !lastTask.isDone()) {
            lastTask.cancel(true);
        }
        double currentScale = resizeSlider.getValue();
        double currentGamma = gammaSlider.getValue();
        lastTask = executorService.submit(() -> {
            int originalWidth = (int) originalImage.getWidth();
            int originalHeight = (int) originalImage.getHeight();
            int newWidth = (int) (originalWidth * currentScale);
            int newHeight = (int) (originalHeight * currentScale);
            WritableImage resizedImage = new WritableImage(newWidth, newHeight);
            PixelReader reader = originalImage.getPixelReader();
            PixelWriter writer = resizedImage.getPixelWriter();
            Interpolator interpolator = InterpolatorFactory.createInterpolator(currentInterpolationMethod);
            for (int y = 0; y < newHeight; y++) {
                for (int x = 0; x < newWidth; x++) {
                    double scaleX = (x / currentScale);
                    double scaleY = (y / currentScale);
                    Color color = interpolator.interpolate(reader, scaleX, scaleY, originalWidth, originalHeight);
                    writer.setColor(x, y, color);
                }
            }
            Image finalImage = applyFilters(resizedImage, currentGamma);
            Platform.runLater(() -> imageView.setImage(finalImage));
        });
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

    @Override
    public void stop() throws Exception {
        executorService.shutdown();
        super.stop();
    }
}
