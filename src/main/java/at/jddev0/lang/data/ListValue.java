package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

import java.util.LinkedList;

public final class ListValue extends DataValue {
    private final LinkedList<DataObject> list;

    public ListValue(LinkedList<DataObject> list) {
        this.list = list;
    }

    @Override
    public LinkedList<DataObject> getList() {
        return list;
    }
}
