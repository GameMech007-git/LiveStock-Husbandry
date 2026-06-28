package com.livestockhusbandry.ai.largelivestock;

public final class LargeLivestockFoldRules {

    private LargeLivestockFoldRules() {
    }

    public static int stablePopulation(int troughCount) {
        return troughCount;
    }

    public static int extraPopulation(int troughCount) {
        return troughCount / 4 + 2;
    }

    public static int breedingLimit(int troughCount) {
        return stablePopulation(troughCount) + extraPopulation(troughCount);
    }
}