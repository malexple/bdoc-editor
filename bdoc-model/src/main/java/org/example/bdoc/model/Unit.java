package org.example.bdoc.model;

public enum Unit {
    PT(1.0),
    MM(2.834645669),
    PICA(12.0),
    INCH(72.0);

    private final double pointsPerUnit;

    Unit(double pointsPerUnit) {
        this.pointsPerUnit = pointsPerUnit;
    }

    public double toPoints(double value) {
        return value * pointsPerUnit;
    }

    public double fromPoints(double valueInPoints) {
        return valueInPoints / pointsPerUnit;
    }

    public static Unit fromString(String code) {
        if (code == null) return PT;
        return switch (code) {
            case "mm" -> MM;
            case "pica" -> PICA;
            case "in" -> INCH;
            default -> PT;
        };
    }
}