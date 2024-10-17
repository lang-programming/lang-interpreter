package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class TypeValue extends DataValue {
    private final DataObject.DataType typeValue;

    public TypeValue(DataObject.DataType typeValue) {
        this.typeValue = typeValue;
    }

    @Override
    public DataObject.DataType getTypeValue() {
        return typeValue;
    }
}
