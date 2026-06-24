package com.livestockhusbandry.block.trough;

import net.minecraft.util.StringRepresentable;

public enum TroughPart implements StringRepresentable {
    SINGLE("single"),
    LEFT("left"),
    MIDDLE("middle"),
    RIGHT("right");

    private final String name;

    TroughPart(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}