package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class ArrayValue extends DataValue {
    private final DataObject[] arr;

    public ArrayValue(DataObject[] arr) {
        this.arr = arr;
    }

    @Override
    public DataObject[] getArray() {
        return arr;
    }
}
