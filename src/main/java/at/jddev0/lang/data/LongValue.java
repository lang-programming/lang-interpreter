package at.jddev0.lang.data;

public final class LongValue extends DataValue {
    private final long longValue;

    public LongValue(long longValue) {
        this.longValue = longValue;
    }

    @Override
    public long getLong() {
        return longValue;
    }
}
