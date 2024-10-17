package at.jddev0.lang.data;

import at.jddev0.lang.DataObject;

import java.util.LinkedList;

public abstract class DataValue {
    public DataObject.Text getText() {
        return null;
    }

    public byte[] getByteBuffer() {
        return null;
    }

    public DataObject[] getArray() {
        return null;
    }

    public LinkedList<DataObject> getList() {
        return null;
    }

    public DataObject.VarPointerObject getVarPointer() {
        return null;
    }

    public DataObject.FunctionPointerObject getFunctionPointer() {
        return null;
    }

    public DataObject.StructObject getStruct() {
        return null;
    }

    public DataObject.LangObject getObject() {
        return null;
    }

    public int getInt() {
        return 0;
    }

    public long getLong() {
        return 0L;
    }

    public float getFloat() {
        return 0.f;
    }

    public double getDouble() {
        return 0.;
    }

    public int getChar() {
        return 0;
    }

    public DataObject.ErrorObject getError() {
        return null;
    }

    public DataObject.DataType getTypeValue() {
        return null;
    }
}
