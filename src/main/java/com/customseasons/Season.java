package com.customseasons;

public enum Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    public Season next() {
        Season[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
