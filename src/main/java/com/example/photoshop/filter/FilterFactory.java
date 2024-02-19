package com.example.photoshop.filter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class FilterFactory {
    private static final Map<String, Class<? extends Filters>> filterMap = new HashMap<>();

    static {
        filterMap.put("Laplacian", LaplacianFilter.class);
    }

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

    public static Set<String> getFilterNames() {
        return filterMap.keySet();
    }
}
