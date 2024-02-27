package com.example.photoshop.filter;

import javafx.scene.image.Image;

/**
 * Interface for filters.
 * Provides a method to apply a filter to an image.
 */
public interface Filters {

    /**
     * Applies a filter to an image.
     *
     * @param image Image to which the filter is to be applied.
     * @return Image after applying the filter.
     */
    Image applyFilter(Image image);
}