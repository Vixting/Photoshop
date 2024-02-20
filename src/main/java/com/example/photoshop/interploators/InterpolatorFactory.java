package com.example.photoshop.interploators;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InterpolatorFactory {
    private static final Map<String, Class<? extends Interpolator>> interpolatorMap = new HashMap<>();

    static {
        interpolatorMap.put("Nearest Neighbor", NearestNeighborInterpolator.class);
        interpolatorMap.put("Bilinear", BilinearInterpolator.class);
    }

    public static Interpolator createInterpolator(String interpolatorName) {
        try {
            return interpolatorMap.get(interpolatorName).getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Error creating interpolator", e);
        }
    }

    public static Set<String> getInterpolatorNames() {
        return interpolatorMap.keySet();
    }
}
