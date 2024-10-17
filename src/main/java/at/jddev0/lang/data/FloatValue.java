package at.jddev0.lang.data;

public final class FloatValue extends DataValue {
    private final float floatValue;

    public FloatValue(float floatValue) {
        this.floatValue = floatValue;
    }

    @Override
    public float getFloat() {
        return floatValue;
    }
}
