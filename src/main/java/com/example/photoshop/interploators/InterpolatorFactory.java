package com.example.photoshop.interploators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * This class is a factory for creating interpolator instances.
 * It maintains a map of interpolator names to their corresponding classes.
 * It provides methods to create an interpolator instance and to get the names of all available interpolators.
 */
public class InterpolatorFactory {
    // Map of interpolator names to their corresponding classes
    private static final Map<String, Class<? extends Interpolator>> interpolatorMap = new HashMap<>();

    // Static initializer block to populate the interpolator map
    static {
        interpolatorMap.put("Nearest Neighbor", NearestNeighborInterpolator.class);
        interpolatorMap.put("Bilinear", BilinearInterpolator.class);
    }

    /**
     * This method creates an instance of the specified interpolator.
     *
     * @param interpolatorName The name of the interpolator to create an instance of.
     * @return An instance of the specified interpolator.
     * @throws RuntimeException if there is an error creating the interpolator instance.
     */
    public static Interpolator createInterpolator(String interpolatorName) {
        try {
            return interpolatorMap.get(interpolatorName).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating interpolator", e);
        }
    }

    /**
     * This method returns the names of all available interpolators.
     *
     * @return A set of names of all available interpolators.
     */
    public static Set<String> getInterpolatorNames() {
        return interpolatorMap.keySet();
    }
}