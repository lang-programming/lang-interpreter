package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class ObjectValue extends DataValue {
    private final DataObject.LangObject op;

    public ObjectValue(DataObject.LangObject op) {
        this.op = op;
    }

    @Override
    public DataObject.LangObject getObject() {
        return op;
    }
}
