package at.jddev0.lang.data;

public final class CharValue extends DataValue {
    private final int charValue;

    public CharValue(int charValue) {
        this.charValue = charValue;
    }

    @Override
    public int getChar() {
        return charValue;
    }
}
