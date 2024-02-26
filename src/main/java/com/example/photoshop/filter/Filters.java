package com.example.photoshop.filter;

import javafx.scene.image.Image;

/**
 * This is an interface for filters.
 * It provides a method to apply a filter to an image.
 */
public interface Filters {

    /**
     * This method applies a filter to an image.
     *
     * @param image The image to which the filter is to be applied.
     * @return The image after applying the filter.
     */
    Image applyFilter(Image image);
}