package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class ErrorValue extends DataValue {
    private final DataObject.ErrorObject error;

    public ErrorValue(DataObject.ErrorObject error) {
        this.error = error;
    }

    @Override
    public DataObject.ErrorObject getError() {
        return error;
    }
}
