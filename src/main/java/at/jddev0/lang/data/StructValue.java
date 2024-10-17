package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class StructValue extends DataValue {
    private final DataObject.StructObject sp;

    public StructValue(DataObject.StructObject sp) {
        this.sp = sp;
    }

    @Override
    public DataObject.StructObject getStruct() {
        return sp;
    }
}
