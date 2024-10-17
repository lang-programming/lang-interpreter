package at.jddev0.lang.data;

public final class IntValue extends DataValue {
    private final int intValue;

    public IntValue(int intValue) {
        this.intValue = intValue;
    }

    @Override
    public int getInt() {
        return intValue;
    }
}
