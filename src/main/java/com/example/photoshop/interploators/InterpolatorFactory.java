package com.example.photoshop.interploators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InterpolatorFactory {
    // Map of interpolator names to their corresponding classes
    private static final Map<String, Class<? extends Interpolator>> interpolatorMap = new HashMap<>();

    // Static initializer block to populate the interpolator map
    static {
        interpolatorMap.put("Nearest Neighbor", NearestNeighborInterpolator.class);
        interpolatorMap.put("Bilinear", BilinearInterpolator.class);
    }

    /**
     * Creates an instance of the specified interpolator.
     *
     * @param interpolatorName Name of the interpolator to create an instance of.
     * @return Instance of the specified interpolator.
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
     * Returns the names of all available interpolators.
     *
     * @return Set of names of all available interpolators.
     */
    public static Set<String> getInterpolatorNames() {
        return interpolatorMap.keySet();
    }
}