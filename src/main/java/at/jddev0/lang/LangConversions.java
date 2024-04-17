package at.jddev0.lang;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.FunctionPointerObject;

import java.util.*;

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
	//TODO toText
	public Character toChar(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("char", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case INT:
				return (char)operand.getInt();
			case LONG:
				return (char)operand.getLong();
			case FLOAT:
				return (char)operand.getFloat();
			case DOUBLE:
				return (char)operand.getDouble();
			case CHAR:
				return operand.getChar();
			case TEXT:
			case ARGUMENT_SEPARATOR:
			case BYTE_BUFFER:
			case ARRAY:
			case LIST:
			case VAR_POINTER:
			case FUNCTION_POINTER:
			case STRUCT:
			case OBJECT:
			case NULL:
			case VOID:
			case ERROR:
			case TYPE:
				return null;
		}

		return null;
	}
	public Integer toInt(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("int", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
				if(operand.getText().trim().length() == operand.getText().length()) {
					try {
						return Integer.parseInt(operand.getText());
					}catch(NumberFormatException ignore) {}
				}

				return null;
			case CHAR:
				return (int)operand.getChar();
			case INT:
				return operand.getInt();
			case LONG:
				return (int)operand.getLong();
			case FLOAT:
				return (int)operand.getFloat();
			case DOUBLE:
				return (int)operand.getDouble();
			case ERROR:
				return operand.getError().getErrno();
			case BYTE_BUFFER:
				return operand.getByteBuffer().length;
			case ARRAY:
				return operand.getArray().length;
			case LIST:
				return operand.getList().size();
			case STRUCT:
				return operand.getStruct().getMemberNames().length;

			case VAR_POINTER:
			case FUNCTION_POINTER:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return null;
		}

		return null;
	}

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

	public Number toNumber(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("number", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
				String txt = operand.getText();
				if(txt.length() > 0) {
					char lastChar = txt.charAt(txt.length() - 1);

					if(txt.trim().length() == txt.length() && lastChar != 'f' && lastChar != 'F' && lastChar != 'd' &&
							lastChar != 'D' && !txt.contains("x") && !txt.contains("X")) {
						//INT
						try {
							return Integer.parseInt(txt);
						}catch(NumberFormatException ignore) {}

						//LONG
						try {
							if(txt.endsWith("l") || txt.endsWith("L"))
								return Long.parseLong(txt.substring(0, txt.length() - 1));
							else
								return Long.parseLong(txt);
						}catch(NumberFormatException ignore) {}

						//FLOAT
						if(txt.endsWith("f") || txt.endsWith("F")) {
							try {
								return Float.parseFloat(txt.substring(0, txt.length() - 1));
							}catch(NumberFormatException ignore) {}
						}

						//DOUBLE
						try {
							return Double.parseDouble(txt);
						}catch(NumberFormatException ignore) {}
					}
				}

				return null;
			case CHAR:
				return (int)operand.getChar();
			case INT:
				return operand.getInt();
			case LONG:
				return operand.getLong();
			case FLOAT:
				return operand.getFloat();
			case DOUBLE:
				return operand.getDouble();
			case BYTE_BUFFER:
				return operand.getByteBuffer().length;
			case ERROR:
				return operand.getError().getErrno();
			case ARRAY:
				return operand.getArray().length;
			case LIST:
				return operand.getList().size();
			case STRUCT:
				return operand.getStruct().getMemberNames().length;

			case VAR_POINTER:
			case FUNCTION_POINTER:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return null;
		}

		return null;
	}
	public DataObject convertToNumberAndCreateNewDataObject(DataObject operand, int lineNumber, final int SCOPE_ID) {
		Number number = toNumber(operand, lineNumber, SCOPE_ID);
		if(number == null)
			return new DataObject().setNull();

		if(number instanceof Integer)
			return new DataObject().setInt(number.intValue());

		if(number instanceof Long)
			return new DataObject().setLong(number.longValue());

		if(number instanceof Float)
			return new DataObject().setFloat(number.floatValue());

		if(number instanceof Double)
			return new DataObject().setDouble(number.doubleValue());

		return new DataObject().setNull();
	}
}