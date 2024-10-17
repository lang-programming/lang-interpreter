package at.jddev0.lang.data;

public final class DoubleValue extends DataValue {
    private final double doubleValue;

    public DoubleValue(double doubleValue) {
        this.doubleValue = doubleValue;
    }

    @Override
    public double getDouble() {
        return doubleValue;
    }
}
