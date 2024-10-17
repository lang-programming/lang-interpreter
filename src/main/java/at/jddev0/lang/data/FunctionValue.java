package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class FunctionValue extends DataValue {
    private final DataObject.FunctionPointerObject fp;

    public FunctionValue(DataObject.FunctionPointerObject fp) {
        this.fp = fp;
    }

    @Override
    public DataObject.FunctionPointerObject getFunctionPointer() {
        return fp;
    }
}
