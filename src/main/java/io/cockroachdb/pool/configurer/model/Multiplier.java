package io.cockroachdb.pool.configurer.model;

public enum Multiplier {
    X2("2x") {
        @Override
        public int apply(int value) {
            return value * 2;
        }
    },
    X3("3x") {
        @Override
        public int apply(int value) {
            return value * 3;
        }
    },
    X4("4x") {
        @Override
        public int apply(int value) {
            return value * 4;
        }
    },
    X5("5x") {
        @Override
        public int apply(int value) {
            return value * 5;
        }
    },
    X6("6x") {
        @Override
        public int apply(int value) {
            return value * 6;
        }
    };

    private final String displayValue;

    Multiplier(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }

    public abstract int apply(int value);
}
