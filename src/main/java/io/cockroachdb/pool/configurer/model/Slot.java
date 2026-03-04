package io.cockroachdb.pool.configurer.model;

public enum Slot {
    RED("Red", "danger"),
    GREEN("Green", "success"),
    BLUE("Blue", "primary"),
    YELLOW("Yellow", "warning");

    private final String displayName;

    private final String className;

    private boolean occupied;

    Slot(String displayName, String className) {
        this.displayName = displayName;
        this.className = className;
    }

    public String getClassName() {
        return "btn " + (!isOccupied() ? "btn-outline-" : "btn-") + className;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }
}
