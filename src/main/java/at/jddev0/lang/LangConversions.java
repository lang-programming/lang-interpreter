package at.jddev0.lang;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.FunctionPointerObject;

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


	private String convertByteBufferToText(DataObject operand, int lineNumber, final int SCOPE_ID) {
		StringBuilder builder = new StringBuilder();
		if(operand.getByteBuffer().length > 0) {
			final String HEX_DIGITS = "0123456789ABCDEF";

			builder.append("0x");
			for(byte b:operand.getByteBuffer()) {
				builder.append(HEX_DIGITS.charAt((b >> 4) & 0xF));
				builder.append(HEX_DIGITS.charAt(b & 0xF));
			}
		}else {
			builder.append("<Empty ByteBuffer>");
		}
		return builder.toString();
	}

	private void convertCompositeElementToText(DataObject ele, StringBuilder builder, int lineNumber, final int SCOPE_ID) {
		if(ele.getType() == DataType.ARRAY) {
			builder.append("<Array[" + ele.getArray().length + "]>");
		}else if(ele.getType() == DataType.LIST) {
			builder.append("<List[" + ele.getList().size() + "]>");
		}else if(ele.getType() == DataType.VAR_POINTER) {
			builder.append("-->{");
			DataObject data = ele.getVarPointer().getVar();
			if(data != null && data.getType() == DataType.ARRAY) {
				builder.append("<Array[" + data.getArray().length + "]>");
			}else if(data != null && data.getType() == DataType.LIST) {
				builder.append("<List[" + data.getList().size() + "]>");
			}else if(data != null && data.getType() == DataType.VAR_POINTER) {
				builder.append("-->{...}");
			}else if(data != null && data.getType() == DataType.STRUCT) {
				builder.append(data.getStruct().isDefinition()?"<Struct[Definition]>":"<Struct[Instance]>");
			}else {
				builder.append(data);
			}
			builder.append("}");
		}else if(ele.getType() == DataType.STRUCT) {
			builder.append(ele.getStruct().isDefinition()?"<Struct[Definition]>":"<Struct[Instance]>");
		}else if(ele.getType() == DataType.OBJECT) {
			builder.append(ele.getObject().isClass()?"<Class>":"<Object>");
		}else {
			builder.append(toText(ele, lineNumber, SCOPE_ID));
		}
		builder.append(", ");
	}

	private String convertArrayToText(DataObject operand, int lineNumber, final int SCOPE_ID) {
		StringBuilder builder = new StringBuilder("[");
		if(operand.getArray().length > 0) {
			for(DataObject ele:operand.getArray())
				convertCompositeElementToText(ele, builder, lineNumber, SCOPE_ID);
			builder.delete(builder.length() - 2, builder.length());
		}
		builder.append(']');
		return builder.toString();
	}

	private String convertListToText(DataObject operand, int lineNumber, final int SCOPE_ID) {
		StringBuilder builder = new StringBuilder("[");
		if(operand.getList().size() > 0) {
			for(DataObject ele:operand.getList())
				convertCompositeElementToText(ele, builder, lineNumber, SCOPE_ID);
			builder.delete(builder.length() - 2, builder.length());
		}
		builder.append(']');
		return builder.toString();
	}

	private String convertStructToText(DataObject operand, int lineNumber, final int SCOPE_ID) {
		StringBuilder builder = new StringBuilder("{");
		String[] memberNames = operand.getStruct().getMemberNames();
		if(memberNames.length > 0) {
			if(operand.getStruct().isDefinition()) {
				for(String memberName:memberNames)
					builder.append(memberName).append(", ");
			}else {
				for(String memberName:memberNames) {
					builder.append(memberName).append(": ");
					convertCompositeElementToText(operand.getStruct().getMember(memberName), builder, lineNumber, SCOPE_ID);
				}
			}

			builder.delete(builder.length() - 2, builder.length());
		}
		builder.append('}');
		return builder.toString();
	}

	//Conversion functions
	public String toText(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("text", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
			case ARGUMENT_SEPARATOR:
				return operand.getText();
			case BYTE_BUFFER:
				return convertByteBufferToText(operand, lineNumber, SCOPE_ID);
			case ARRAY:
				return convertArrayToText(operand, lineNumber, SCOPE_ID);
			case LIST:
				return convertListToText(operand, lineNumber, SCOPE_ID);
			case VAR_POINTER:
				DataObject var = operand.getVarPointer().getVar();
				if(var.getType() == DataType.VAR_POINTER)
					return "-->{-->{...}}";

				return "-->{" + toText(var, lineNumber, SCOPE_ID) + "}";

			case FUNCTION_POINTER:
				if(operand.getVariableName() != null)
					return operand.getVariableName();

				return operand.getFunctionPointer().toString();
			case STRUCT:
				return convertStructToText(operand, lineNumber, SCOPE_ID);
			case OBJECT:
				return operand.getObject().toString();
			case VOID:
				return "";
			case NULL:
				return "null";
			case INT:
				return operand.getInt() + "";
			case LONG:
				return operand.getLong() + "";
			case FLOAT:
				return operand.getFloat() + "";
			case DOUBLE:
				return operand.getDouble() + "";
			case CHAR:
				return operand.getChar() + "";
			case ERROR:
				return operand.getError().toString();
			case TYPE:
				return operand.getTypeValue().name();
		}

		return null;
	}
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
	public Long toLong(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("long", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
				if(operand.getText().trim().length() == operand.getText().length()) {
					try {
						return Long.parseLong(operand.getText());
					}catch(NumberFormatException ignore) {}
				}

				return null;
			case CHAR:
				return (long)operand.getChar();
			case INT:
				return (long)operand.getInt();
			case LONG:
				return operand.getLong();
			case FLOAT:
				return (long)operand.getFloat();
			case DOUBLE:
				return (long)operand.getDouble();
			case ERROR:
				return (long)operand.getError().getErrno();
			case BYTE_BUFFER:
				return (long)operand.getByteBuffer().length;
			case ARRAY:
				return (long)operand.getArray().length;
			case LIST:
				return (long)operand.getList().size();
			case STRUCT:
				return (long)operand.getStruct().getMemberNames().length;

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
	public Float toFloat(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("float", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
				if(operand.getText().length() > 0) {
					char lastChar = operand.getText().charAt(operand.getText().length() - 1);

					if(operand.getText().trim().length() == operand.getText().length() && lastChar != 'f' && lastChar != 'F' && lastChar != 'd' &&
							lastChar != 'D' && !operand.getText().contains("x") && !operand.getText().contains("X")) {
						try {
							return Float.parseFloat(operand.getText());
						}catch(NumberFormatException ignore) {}
					}
				}

				return null;
			case CHAR:
				return (float)operand.getChar();
			case INT:
				return (float)operand.getInt();
			case LONG:
				return (float)operand.getLong();
			case FLOAT:
				return operand.getFloat();
			case DOUBLE:
				return (float)operand.getDouble();
			case ERROR:
				return (float)operand.getError().getErrno();
			case BYTE_BUFFER:
				return (float)operand.getByteBuffer().length;
			case ARRAY:
				return (float)operand.getArray().length;
			case LIST:
				return (float)operand.getList().size();
			case STRUCT:
				return (float)operand.getStruct().getMemberNames().length;

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
	public Double toDouble(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("double", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case TEXT:
				if(operand.getText().length() > 0) {
					char lastChar = operand.getText().charAt(operand.getText().length() - 1);

					if(operand.getText().trim().length() == operand.getText().length() && lastChar != 'f' && lastChar != 'F' && lastChar != 'd' &&
							lastChar != 'D' && !operand.getText().contains("x") && !operand.getText().contains("X")) {
						try {
							return Double.parseDouble(operand.getText());
						}catch(NumberFormatException ignore) {}
					}
				}

				return null;
			case CHAR:
				return (double)operand.getChar();
			case INT:
				return (double)operand.getInt();
			case LONG:
				return (double)operand.getLong();
			case FLOAT:
				return (double)operand.getFloat();
			case DOUBLE:
				return operand.getDouble();
			case ERROR:
				return (double)operand.getError().getErrno();
			case BYTE_BUFFER:
				return (double)operand.getByteBuffer().length;
			case ARRAY:
				return (double)operand.getArray().length;
			case LIST:
				return (double)operand.getList().size();
			case STRUCT:
				return (double)operand.getStruct().getMemberNames().length;

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
	public byte[] toByteBuffer(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("byteBuffer", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case BYTE_BUFFER:
				return operand.getByteBuffer();

			case TEXT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case ARRAY:
			case LIST:
			case ERROR:
			case VAR_POINTER:
			case FUNCTION_POINTER:
			case STRUCT:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return null;
		}

		return null;
	}
	public DataObject[] toArray(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("array", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case ARRAY:
				return operand.getArray();
			case LIST:
				return operand.getList().stream().map(DataObject::new).toArray(len -> new DataObject[len]);
			case STRUCT:
				try {
					DataObject operandCopy = operand;
					return Arrays.stream(operand.getStruct().getMemberNames()).
							map(memberName -> new DataObject(operandCopy.getStruct().getMember(memberName))).toArray(DataObject[]::new);
				}catch(DataObject.DataTypeConstraintException e) {
					return null;
				}

			case TEXT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case BYTE_BUFFER:
			case ERROR:
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
	public LinkedList<DataObject> toList(DataObject operand, int lineNumber, final int SCOPE_ID) {
		DataObject ret = callConversionMethod("list", operand, lineNumber, SCOPE_ID);
		if(ret != null)
			operand = ret;

		switch(operand.getType()) {
			case ARRAY:
				return new LinkedList<>(Arrays.stream(operand.getArray()).map(DataObject::new).collect(Collectors.toList()));
			case LIST:
				return operand.getList();
			case STRUCT:
				try {
					DataObject operandCopy = operand;
					return new LinkedList<>(Arrays.asList(Arrays.stream(operand.getStruct().getMemberNames()).
							map(memberName -> new DataObject(operandCopy.getStruct().getMember(memberName))).toArray(DataObject[]::new)));
				}catch(DataObject.DataTypeConstraintException e) {
					return null;
				}

			case TEXT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case BYTE_BUFFER:
			case ERROR:
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