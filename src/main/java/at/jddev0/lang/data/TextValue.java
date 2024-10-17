package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

public final class TextValue extends DataValue {
    private final DataObject.Text txt;

    public TextValue(DataObject.Text txt) {
        this.txt = txt;
    }

    @Override
    public DataObject.Text getText() {
        return txt;
    }
}
