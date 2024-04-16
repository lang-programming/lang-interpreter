package at.jddev0.lang;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraintException;
import at.jddev0.lang.DataObject.FunctionPointerObject;
import at.jddev0.lang.DataObject.StructObject;
import at.jddev0.lang.LangFunction.AllowedTypes;
import at.jddev0.lang.LangFunction.LangParameter;
import at.jddev0.lang.LangFunction.LangParameter.RawVarArgs;
import at.jddev0.lang.LangFunction.LangParameter.VarArgs;
import at.jddev0.lang.LangInterpreter.InterpretingError;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Lang-Module<br>
 * Lang conversions definitions
 * 
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangConversions {
	private final LangInterpreter interpreter;

	public LangConversions(LangInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	private DataObject callConversionMethod(String conversionName, DataObject operand,
										  int lineNumber, final int SCOPE_ID) {
		String methodName = "to:" + conversionName;

        if(operand.getType() != DataType.OBJECT || operand.getObject().isClass())
			return null;

		FunctionPointerObject[] method = operand.getObject().getMethods().get(methodName);
		if(method == null)
			return null;

		FunctionPointerObject fp = LangUtils.getMostRestrictiveFunction(method, new ArrayList<>(0));
		if(fp == null)
			return null;

		DataObject ret = interpreter.callFunctionPointer(fp, methodName, new ArrayList<>(0), lineNumber, SCOPE_ID);
		if(ret == null)
			return new DataObject().setVoid();

		return ret;
	}

	//DataType conversion methods
	//TODO

	//Special conversion methods
	public boolean toBool(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("bool", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
				return !operand.getText().isEmpty();
			case CHAR:
				return operand.getChar() != 0;
			case INT:
				return operand.getInt() != 0;
			case LONG:
				return operand.getLong() != 0;
			case FLOAT:
				return operand.getFloat() != 0;
			case DOUBLE:
				return operand.getDouble() != 0;
			case BYTE_BUFFER:
				return operand.getByteBuffer().length > 0;
			case ARRAY:
				return operand.getArray().length > 0;
			case LIST:
				return !operand.getList().isEmpty();
			case STRUCT:
				return operand.getStruct().getMemberNames().length > 0;
			case ERROR:
				return operand.getError().getErrno() != 0;

			case VAR_POINTER:
			case FUNCTION_POINTER:
			case TYPE:
			case OBJECT:
				return true;

			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
				return false;
		}

		return false;
	}
}