package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class VarPointerValue extends DataValue {
    private final DataObject.VarPointerObject vp;

    public VarPointerValue(DataObject.VarPointerObject vp) {
        this.vp = vp;
    }

    @Override
    public DataObject.VarPointerObject getVarPointer() {
        return vp;
    }
}
