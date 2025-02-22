package at.jddev0.lang;

import java.util.LinkedList;
import java.util.List;

/**
 * Lang-Module<br>
 * Lang native module<br>
 * The default constructor must be public and will be called for the instantiation
 *
 * @author JDDev0
 * @version v1.0.0
 */
public abstract class LangNativeModule {
    /**
     * The value for this field will be injected after the instantiation with the default constructor
     */
    protected LangInterpreter interpreter;
    /**
     * The value for this field will be injected after the instantiation with the default constructor
     */
    protected LangModule module;

    protected final DataObject createDataObject(String stringValue) {
        return new DataObject(stringValue);
    }

    protected final DataObject createDataObject(DataObject.Text textValue) {
        return new DataObject(textValue);
    }

    protected final DataObject createDataObject(byte[] byteBuf) {
        return new DataObject().setByteBuffer(byteBuf);
    }

    protected final DataObject createDataObject(DataObject[] arrayValue) {
        return new DataObject().setArray(arrayValue);
    }

    protected final DataObject createDataObject(List<DataObject> listValue) {
        return new DataObject().setList(new LinkedList<>(listValue));
    }

    protected final DataObject createDataObject(DataObject.VarPointerObject varPointerValue) {
        return new DataObject().setVarPointer(varPointerValue);
    }

    protected final DataObject createDataObject(DataObject.FunctionPointerObject functionPointerValue) {
        return new DataObject().setFunctionPointer(functionPointerValue);
    }

    protected final DataObject createDataObject() {
        return new DataObject();
    }

    protected final DataObject createDataObject(Void v) {
        return new DataObject().setVoid();
    }

    protected final DataObject createDataObject(int intValue) {
        return new DataObject().setInt(intValue);
    }

    protected final DataObject createDataObject(boolean booleanValue) {
        return new DataObject().setBoolean(booleanValue);
    }

    protected final DataObject createDataObject(long longValue) {
        return new DataObject().setLong(longValue);
    }

    protected final DataObject createDataObject(float floatValue) {
        return new DataObject().setFloat(floatValue);
    }

    protected final DataObject createDataObject(double doubleValue) {
        return new DataObject().setDouble(doubleValue);
    }

    protected final DataObject createDataObject(char charValue) {
        return new DataObject().setChar(charValue);
    }

    protected final DataObject createDataObject(DataObject.StructObject structValue) {
        return new DataObject().setStruct(structValue);
    }

    protected final DataObject createDataObject(DataObject.ErrorObject errorValue) {
        return new DataObject().setError(errorValue);
    }

    protected final DataObject createDataObject(DataObject.DataType typeValue) {
        return new DataObject().setTypeValue(typeValue);
    }

    protected final DataObject convertToDataObject(Object objectValue) throws DataObject.DataTypeConstraintException {
        if(objectValue == null) {
            return new DataObject();
        }else if(objectValue instanceof CharSequence) {
            return new DataObject("" + (CharSequence)objectValue);
        }else if(objectValue instanceof DataObject.Text) {
            return new DataObject((DataObject.Text)objectValue);
        }else if(objectValue instanceof byte[]) {
            byte[] byteBuf = (byte[])objectValue;

            return new DataObject().setByteBuffer(byteBuf);
        }else if(objectValue instanceof Object[]) {
            Object[] arrayValue = (Object[])objectValue;
            DataObject[] dataObjectArray = new DataObject[arrayValue.length];
            for(int i = 0;i < arrayValue.length;i++)
                dataObjectArray[i] = convertToDataObject(arrayValue[i]);

            return new DataObject().setArray(dataObjectArray);
        }else if(objectValue instanceof List<?>) {
            List<?> listValue = (List<?>)objectValue;
            LinkedList<DataObject> dataObjectList = new LinkedList<>();
            for(Object ele:listValue)
                dataObjectList.add(convertToDataObject(ele));

            return new DataObject().setList(dataObjectList);
        }else if(objectValue instanceof DataObject.VarPointerObject) {
            return new DataObject().setVarPointer((DataObject.VarPointerObject)objectValue);
        }else if(objectValue instanceof DataObject.FunctionPointerObject) {
            return new DataObject().setFunctionPointer((DataObject.FunctionPointerObject)objectValue);
        }else if(objectValue instanceof LangNativeFunction) {
            return new DataObject().setFunctionPointer(new DataObject.FunctionPointerObject((LangNativeFunction)objectValue));
        }else if(objectValue instanceof Void) {
            return new DataObject().setVoid();
        }else if(objectValue instanceof Integer) {
            return new DataObject().setInt((Integer)objectValue);
        }else if(objectValue instanceof Boolean) {
            return new DataObject().setBoolean((Boolean)objectValue);
        }else if(objectValue instanceof Long) {
            return new DataObject().setLong((Long)objectValue);
        }else if(objectValue instanceof Float) {
            return new DataObject().setFloat((Float)objectValue);
        }else if(objectValue instanceof Double) {
            return new DataObject().setDouble((Double)objectValue);
        }else if(objectValue instanceof Character) {
            return new DataObject().setChar((Character)objectValue);
        }else if(objectValue instanceof DataObject.StructObject) {
            return new DataObject().setStruct((DataObject.StructObject)objectValue);
        }else if(objectValue instanceof DataObject.ErrorObject) {
            return new DataObject().setError((DataObject.ErrorObject)objectValue);
        }else if(objectValue instanceof LangInterpreter.InterpretingError) {
            return new DataObject().setError(new DataObject.ErrorObject((LangInterpreter.InterpretingError)objectValue));
        }else if(objectValue instanceof Throwable) {
            Throwable throwableValue = (Throwable)objectValue;
            return new DataObject().setError(new DataObject.ErrorObject(LangInterpreter.InterpretingError.SYSTEM_ERROR,
                    "Native Error (\"" + throwableValue.getClass().getSimpleName() + "\"): " + throwableValue.getMessage()));
        }else if(objectValue instanceof DataObject.DataType) {
            return new DataObject().setTypeValue((DataObject.DataType)objectValue);
        }else if(objectValue instanceof Class<?>) {
            Class<?> classValue = (Class<?>)objectValue;
            if(CharSequence.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.TEXT);
            }else if(byte[].class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.BYTE_BUFFER);
            }else if(classValue.isArray()) {
                return new DataObject().setTypeValue(DataObject.DataType.ARRAY);
            }else if(List.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.LIST);
            }else if(DataObject.VarPointerObject.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.VAR_POINTER);
            }else if(DataObject.FunctionPointerObject.class.isAssignableFrom(classValue) ||
                    LangNativeFunction.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.FUNCTION_POINTER);
            }else if(Void.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.VOID);
            }else if(Integer.class.isAssignableFrom(classValue) ||
                    Boolean.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.INT);
            }else if(Long.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.LONG);
            }else if(Float.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.FLOAT);
            }else if(Double.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.DOUBLE);
            }else if(Character.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.CHAR);
            }else if(DataObject.ErrorObject.class.isAssignableFrom(classValue) ||
                    LangInterpreter.InterpretingError.class.isAssignableFrom(classValue) ||
                    Throwable.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.ERROR);
            }else if(DataObject.DataType.class.isAssignableFrom(classValue) ||
                    Class.class.isAssignableFrom(classValue)) {
                return new DataObject().setTypeValue(DataObject.DataType.TYPE);
            }
        }

        throw new DataObject.DataTypeConstraintException("Java object can not be converted to DataObject");
    }

    protected final DataObject throwError(DataObject dataObject) throws DataObject.DataTypeConstraintException {
        if(dataObject.getType() != DataObject.DataType.ERROR)
            throw new DataObject.DataTypeConstraintException("DataObject must be of type " + DataObject.DataType.ERROR);

        return throwError(dataObject.getError());
    }

    protected final DataObject throwError(DataObject.ErrorObject errorValue) {
        return throwError(errorValue.getInterpretingError(), errorValue.getMessage());
    }

    protected final DataObject throwError(LangInterpreter.InterpretingError error) {
        return interpreter.setErrnoErrorObject(error);
    }

    protected final DataObject throwError(LangInterpreter.InterpretingError error, String message) {
        return interpreter.setErrnoErrorObject(error, message);
    }

    protected final DataObject throwError(Throwable throwableValue) {
        return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.SYSTEM_ERROR,
                "Native Error (\"" + throwableValue.getClass().getSimpleName() + "\"): " + throwableValue.getMessage());
    }

    protected final DataObject getPredefinedFunctionAsDataObject(String name) {
        DataObject.FunctionPointerObject func = interpreter.funcs.get(name);
        if(func == null)
            return null;

        String functionName = (func.isLinkerFunction()?"linker.":"func.") + name;

        return new DataObject().setFunctionPointer(func).setVariableName(functionName);
    }

    protected final DataObject callFunctionPointer(DataObject func, List<DataObject> argumentValueList, CodePosition parentPos) {
        if(func.getType() != DataObject.DataType.FUNCTION_POINTER)
            throw new RuntimeException("\"func\" must be of type " + DataObject.DataType.FUNCTION_POINTER);

        return interpreter.callFunctionPointer(func.getFunctionPointer(), func.getVariableName(), argumentValueList, parentPos);
    }
    protected final DataObject callFunctionPointer(DataObject func, List<DataObject> argumentValueList) {
        if(func.getType() != DataObject.DataType.FUNCTION_POINTER)
            throw new RuntimeException("\"func\" must be of type " + DataObject.DataType.FUNCTION_POINTER);

        return interpreter.callFunctionPointer(func.getFunctionPointer(), func.getVariableName(), argumentValueList);
    }

    protected final DataObject callPredefinedFunction(String funcName, List<DataObject> argumentValueList, CodePosition parentPos) {
        return callFunctionPointer(getPredefinedFunctionAsDataObject(funcName), argumentValueList, parentPos);
    }
    protected final DataObject callPredefinedFunction(String funcName, List<DataObject> argumentValueList) {
        return callFunctionPointer(getPredefinedFunctionAsDataObject(funcName), argumentValueList);
    }

    /**
     * @param obj An object which contains a single LangFunction
     *             The function name of the LangFunction will be used as the function name
     */
    protected final void exportNativeFunction(Object obj) {
        exportNativeFunction(LangNativeFunction.getSingleLangFunctionFromObject(obj));
    }

    /**
     * @param functionName The name of the function
     * @param obj An object which contains a single LangFunction
     */
    protected final void exportNativeFunction(String functionName, Object obj) {
        exportNativeFunction(functionName, LangNativeFunction.getSingleLangFunctionFromObject(obj));
    }

    /**
     * @param func A LangNativeFunction can be a normal function or a linker function
     *             The function name of the LangFunction will be used as the function name
     */
    protected final void exportNativeFunction(DataObject.FunctionPointerObject func) {
        exportNativeFunction(func.getFunctionName(), func);
    }

    /**
     * @param functionName The name of the function
     * @param func A predefined function can be a normal function or a linker function
     */
    protected final void exportNativeFunction(String functionName, DataObject.FunctionPointerObject func) {
        if(!module.isLoad())
            throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");

        for(int i = 0;i < functionName.length();i++) {
            char c = functionName.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                continue;

            throw new RuntimeException("The function name may only contain alphanumeric characters and underscores (_)");
        }

        module.getExportedFunctions().add(functionName);

        interpreter.funcs.put(functionName, func);
    }

    protected final void exportNormalVariableFinal(String variableName, DataObject value) {
        exportNormalVariable(variableName, value, true);
    }
    protected final void exportNormalVariable(String variableName, DataObject value) {
        exportNormalVariable(variableName, value, false);
    }
    protected final void exportNormalVariable(String variableName, DataObject value, boolean finalData) {
        if(!module.isLoad())
            throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");

        for(int i = 0;i < variableName.length();i++) {
            char c = variableName.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                continue;

            throw new RuntimeException("The variable name may only contain alphanumeric characters and underscores (_)");
        }

        if(variableName.startsWith("LANG"))
            throw new RuntimeException("The variable name may not start with LANG");

        variableName = "$" + variableName;
        value = new DataObject(value).setFinalData(finalData).setVariableName(variableName);

        module.getExportedVariables().put(variableName, value);
    }

    protected final void exportCompositeVariableFinal(String variableName, DataObject value) {
        exportCompositeVariable(variableName, value, true);
    }
    protected final void exportCompositeVariable(String variableName, DataObject value) {
        exportCompositeVariable(variableName, value, false);
    }
    private void exportCompositeVariable(String variableName, DataObject value, boolean finalData) {
        if(!module.isLoad())
            throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");

        for(int i = 0;i < variableName.length();i++) {
            char c = variableName.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                continue;

            throw new RuntimeException("The variable name may only contain alphanumeric characters and underscores (_)");
        }

        if(variableName.startsWith("LANG"))
            throw new RuntimeException("The variable name may not start with LANG");

        variableName = "&" + variableName;
        value = new DataObject(value).setFinalData(finalData).setVariableName(variableName);

        module.getExportedVariables().put(variableName, value);
    }

    protected final void exportFunctionPointerVariableFinal(String variableName, DataObject value) {
        exportFunctionPointerVariable(variableName, value, true);
    }
    protected final void exportFunctionPointerVariable(String variableName, DataObject value) {
        exportFunctionPointerVariable(variableName, value, false);
    }
    private void exportFunctionPointerVariable(String variableName, DataObject value, boolean finalData) {
        if(!module.isLoad())
            throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");

        for(int i = 0;i < variableName.length();i++) {
            char c = variableName.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                continue;

            throw new RuntimeException("The variable name may only contain alphanumeric characters and underscores (_)");
        }

        variableName = "fp." + variableName;
        value = new DataObject(value).setFinalData(finalData).setVariableName(variableName);

        module.getExportedVariables().put(variableName, value);
    }

    /**
     * Will be called if the module is loaded
     *
     * @param args provided to "func.loadModule()" [modulePath included] separated with ARGUMENT_SEPARATORs
     * @return Will be returned for the "linker.loadModule()" call
     */
    public abstract DataObject load(List<DataObject> args);

    /**
     * Will be called if the module is unloaded
     *
     * @param args provided to "func.unloadModule()" [modulePath included] separated with ARGUMENT_SEPARATORs
     * @return Will be returned for the "linker.unloadModule()" call
     */
    public abstract DataObject unload(List<DataObject> args);
}