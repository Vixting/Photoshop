package com.example.photoshop.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FilterFactory {
    // Map of filter names to their corresponding classes
    private static final Map<String, Class<? extends Filters>> filterMap = new HashMap<>();

    // Static initializer block to populate the filter map
    static {
        filterMap.put("Laplacian", LaplacianFilter.class);
    }

    /**
     * Creates an instance of the specified filter.
     * If the filter is a Gamma filter, it uses the provided gamma value.
     *
     * @param filterName The name of the filter to create an instance of.
     * @param gammaValue The gamma value for the Gamma filter.
     * @return An instance of the specified filter.
     * @throws RuntimeException if there is an error creating the filter instance.
     */
    public static Filters createFilter(String filterName, double gammaValue) {
        try {
            if ("Gamma".equals(filterName)) {
                return filterMap.get(filterName).getDeclaredConstructor(double.class).newInstance(gammaValue);
            } else {
                return filterMap.get(filterName).getDeclaredConstructor().newInstance();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error creating filter", e);
        }
    }

    /**
     * Returns the names of all available filters.
     *
     * @return A set of names of all available filters.
     */
    public static Set<String> getFilterNames() {
        return filterMap.keySet();
    }
}