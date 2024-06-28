package at.jddev0.lang;

import java.util.*;
import java.util.stream.Collectors;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraintException;
import at.jddev0.lang.DataObject.FunctionPointerObject;
import at.jddev0.lang.DataObject.StructObject;
import at.jddev0.lang.LangFunction.AllowedTypes;
import at.jddev0.lang.LangFunction.LangParameter;
import at.jddev0.lang.LangFunction.LangParameter.RawVarArgs;
import at.jddev0.lang.LangFunction.LangParameter.VarArgs;
import at.jddev0.lang.LangInterpreter.InterpretingError;

/**
 * Lang-Module<br>
 * Lang operators definitions
 * 
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangOperators {
	private final LangInterpreter interpreter;
	
	public LangOperators(LangInterpreter interpreter) {
		this.interpreter = interpreter;
	}

	private DataObject callOperatorMethod(String operatorName, DataObject operand, CodePosition pos) {
		return callOperatorMethod(operand, "op:" + operatorName, new ArrayList<>(0),
				pos);
	}

	private DataObject callOperatorMethod(String operatorName, boolean hasReverse, DataObject leftSideOperand,
										  DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod(leftSideOperand, "op:" + operatorName, Arrays.asList(rightSideOperand),
				pos);
		if(ret != null)
			return ret;

		return hasReverse?callOperatorMethod(rightSideOperand, "op:r-" + operatorName, Arrays.asList(leftSideOperand),
				pos):null;
	}

	private DataObject callOperatorMethod(String operatorName, DataObject leftSideOperand, DataObject middleOperand,
										  DataObject rightSideOperand, CodePosition pos) {
		return callOperatorMethod(leftSideOperand, "op:" + operatorName,
				LangUtils.asListWithArgumentSeparators(middleOperand, rightSideOperand), pos);
	}

	private DataObject callOperatorMethod(DataObject langObject, String methodName, List<DataObject> argumentList,
										  CodePosition pos) {
		if(langObject.getType() != DataType.OBJECT || langObject.getObject().isClass())
			return null;

		FunctionPointerObject method = langObject.getObject().getMethods().get(methodName);
		if(method == null)
			return null;

		DataObject ret = interpreter.callFunctionPointer(method, methodName, argumentList, pos);
		if(ret == null)
			return new DataObject().setVoid();

		return ret;
	}
	
	//General operation functions
	/**
	 * For "@"
	 */
	public DataObject opLen(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("len", operand, pos);
		if(ret != null)
			return ret;

		switch(operand.getType()) {
			case BYTE_BUFFER:
				return new DataObject().setInt(operand.getByteBuffer().length);
			case ARRAY:
				return new DataObject().setInt(operand.getArray().length);
			case LIST:
				return new DataObject().setInt(operand.getList().size());
			case TEXT:
				return new DataObject().setInt(operand.getText().length());
			case CHAR:
				return new DataObject().setInt(1);
			case STRUCT:
				return new DataObject().setInt(operand.getStruct().getMemberNames().length);
			
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
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
	/**
	 * For "^"
	 */
	public DataObject opDeepCopy(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("deepCopy", operand, pos);
		if(ret != null)
			return ret;

		switch(operand.getType()) {
			case BYTE_BUFFER:
				return new DataObject().setByteBuffer(Arrays.copyOf(operand.getByteBuffer(), operand.getByteBuffer().length));
			case ARRAY:
				DataObject[] arrCopy = new DataObject[operand.getArray().length];
				for(int i = 0;i < operand.getArray().length;i++) {
					arrCopy[i] = opDeepCopy(operand.getArray()[i], pos);
					if(arrCopy[i] == null)
						return null;
				}
				
				return new DataObject().setArray(arrCopy);
			case LIST:
				LinkedList<DataObject> listCopy = new LinkedList<>();
				for(int i = 0;i < operand.getList().size();i++) {
					listCopy.add(opDeepCopy(operand.getList().get(i), pos));
					if(listCopy.get(i) == null)
						return null;
				}
				
				return new DataObject().setList(listCopy);
			case STRUCT:
				StructObject struct = operand.getStruct();
				try {
					if(struct.isDefinition()) {
						return new DataObject().setStruct(new StructObject(struct.getMemberNames(), struct.getTypeConstraints()));
					}else {
						StructObject structCopy = new StructObject(struct.getStructBaseDefinition());
						for(String memberName:struct.getMemberNames())
							structCopy.setMember(memberName, opDeepCopy(struct.getMember(memberName), pos));
						
						return new DataObject().setStruct(structCopy);
					}
				}catch(DataTypeConstraintException e) {
					return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);
				}
			case OBJECT:
				return null;
			
			case TEXT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
			case ERROR:
			case VAR_POINTER:
			case FUNCTION_POINTER:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return new DataObject(operand);
		}
		
		return null;
	}
	/**
	 * For "|||"
	 */
	public DataObject opConcat(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("concat", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				return new DataObject(leftSideOperand.getInt() + interpreter.conversions.toText(rightSideOperand, pos));
			case LONG:
				return new DataObject(leftSideOperand.getLong() + interpreter.conversions.toText(rightSideOperand, pos));
			case FLOAT:
				return new DataObject(leftSideOperand.getFloat() + interpreter.conversions.toText(rightSideOperand, pos));
			case DOUBLE:
				return new DataObject(leftSideOperand.getDouble() + interpreter.conversions.toText(rightSideOperand, pos));
			case CHAR:
				return new DataObject(leftSideOperand.getChar() + interpreter.conversions.toText(rightSideOperand, pos));
			case TEXT:
				return new DataObject(leftSideOperand.getText() + interpreter.conversions.toText(rightSideOperand, pos));
			case BYTE_BUFFER:
				if(rightSideOperand.getType() != DataType.BYTE_BUFFER)
					return null;
				
				byte[] newByteBuf = new byte[leftSideOperand.getByteBuffer().length + rightSideOperand.getByteBuffer().length];
				
				System.arraycopy(leftSideOperand.getByteBuffer(), 0, newByteBuf, 0, leftSideOperand.getByteBuffer().length);
				System.arraycopy(rightSideOperand.getByteBuffer(), 0, newByteBuf, leftSideOperand.getByteBuffer().length,
						rightSideOperand.getByteBuffer().length);
				
				return new DataObject().setByteBuffer(newByteBuf);
			case ARRAY:
				if(rightSideOperand.getType() == DataType.ARRAY) {
					DataObject[] arrNew = new DataObject[leftSideOperand.getArray().length + rightSideOperand.getArray().length];
					for(int i = 0;i < leftSideOperand.getArray().length;i++)
						arrNew[i] = leftSideOperand.getArray()[i];
					for(int i = 0;i < rightSideOperand.getArray().length;i++)
						arrNew[leftSideOperand.getArray().length + i] = rightSideOperand.getArray()[i];
					
					return new DataObject().setArray(arrNew);
				}else if(rightSideOperand.getType() == DataType.LIST) {
					DataObject[] arrNew = new DataObject[leftSideOperand.getArray().length + rightSideOperand.getList().size()];
					for(int i = 0;i < leftSideOperand.getArray().length;i++)
						arrNew[i] = leftSideOperand.getArray()[i];
					for(int i = 0;i < rightSideOperand.getList().size();i++)
						arrNew[leftSideOperand.getArray().length + i] = rightSideOperand.getList().get(i);
					
					return new DataObject().setArray(arrNew);
				}
				
				return null;
			case LIST:
				if(rightSideOperand.getType() == DataType.ARRAY) {
					LinkedList<DataObject> listNew = new LinkedList<>(leftSideOperand.getList());
					for(int i = 0;i < rightSideOperand.getArray().length;i++)
						listNew.add(rightSideOperand.getArray()[i]);
					
					return new DataObject().setList(listNew);
				}else if(rightSideOperand.getType() == DataType.LIST) {
					LinkedList<DataObject> listNew = new LinkedList<>(leftSideOperand.getList());
					listNew.addAll(rightSideOperand.getList());
					
					return new DataObject().setList(listNew);
				}
				
				return null;
			
			case FUNCTION_POINTER:
				if(rightSideOperand.getType() != DataType.FUNCTION_POINTER)
					return null;
				
				final FunctionPointerObject aFunc = leftSideOperand.getFunctionPointer();
				final FunctionPointerObject bFunc = rightSideOperand.getFunctionPointer();
				return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction("concat-func")
					@SuppressWarnings("unused")
					public DataObject concatFuncFunction(
							LangInterpreter interpreter,
							@LangParameter("&args") @RawVarArgs List<DataObject> args
					) {
						DataObject retA = interpreter.callFunctionPointer(aFunc, leftSideOperand.getVariableName(), args);
						
						return interpreter.callFunctionPointer(bFunc, rightSideOperand.getVariableName(), Arrays.asList(
								LangUtils.nullToLangVoid(retA)
						));
					}
				}, leftSideOperand, rightSideOperand).withFunctionName("<concat-func(" + aFunc + ", " + bFunc + ")>"));
			
			case STRUCT:
			case OBJECT:
			case ERROR:
			case VAR_POINTER:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return null;
		}
		
		return null;
	}
	/**
	 * For "&lt;=&gt;"
	 */
	public DataObject opSpaceship(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(isLessThan(leftSideOperand, rightSideOperand, pos))
			return new DataObject().setInt(-1);
		if(isEquals(leftSideOperand, rightSideOperand, pos))
			return new DataObject().setInt(0);
		if(isGreaterThan(leftSideOperand, rightSideOperand, pos))
			return new DataObject().setInt(1);
		
		return new DataObject().setNull();
	}
	
	//Math operation functions
	/**
	 * For "+|"
	 */
	public DataObject opInc(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("inc", operand, pos);
		if(ret != null)
			return ret;

		switch(operand.getType()) {
			case INT:
				return new DataObject().setInt(operand.getInt() + 1);
			case LONG:
				return new DataObject().setLong(operand.getLong() + 1);
			case FLOAT:
				return new DataObject().setFloat(operand.getFloat() + 1.f);
			case DOUBLE:
				return new DataObject().setDouble(operand.getDouble() + 1.d);
			case CHAR:
				return new DataObject().setChar((char)(operand.getChar() + 1));
			
			case FUNCTION_POINTER:
				final FunctionPointerObject func = operand.getFunctionPointer();
				return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction("auto-unpack-func")
					public DataObject autoUnpackFuncFunction(
							LangInterpreter interpreter,
							@LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
					) {
						return interpreter.callFunctionPointer(func, operand.getVariableName(), LangUtils.separateArgumentsWithArgumentSeparators(
								Arrays.stream(arrayObject.getArray()).map(DataObject::new).collect(Collectors.toList())
						));
					}
				}, operand).withFunctionName("<auto-unpack-func(" + func + ")>"));
			
			case STRUCT:
			case TEXT:
			case BYTE_BUFFER:
			case ARRAY:
			case LIST:
			case ERROR:
			case VAR_POINTER:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return null;
		}
		
		return null;
	}
	/**
	 * For "-|"
	 */
	public DataObject opDec(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("dec", operand, pos);
		if(ret != null)
			return ret;

		switch(operand.getType()) {
			case INT:
				return new DataObject().setInt(operand.getInt() - 1);
			case LONG:
				return new DataObject().setLong(operand.getLong() - 1);
			case FLOAT:
				return new DataObject().setFloat(operand.getFloat() - 1.f);
			case DOUBLE:
				return new DataObject().setDouble(operand.getDouble() - 1.d);
			case CHAR:
				return new DataObject().setChar((char)(operand.getChar() - 1));
			
			case FUNCTION_POINTER:
				final FunctionPointerObject func = operand.getFunctionPointer();
				return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction("auto-pack-func")
					@SuppressWarnings("unused")
					public DataObject autoPackFuncFunction(
							LangInterpreter interpreter,
							@LangParameter("&args") @VarArgs List<DataObject> args
					) {
						return interpreter.callFunctionPointer(func, operand.getVariableName(), Arrays.asList(
								new DataObject().setArray(args.stream().map(DataObject::new).toArray(DataObject[]::new))
						));
					}
				}, operand).withFunctionName("<auto-pack-func(" + func + ")>"));
			
			case TEXT:
			case ARRAY:
			case BYTE_BUFFER:
			case LIST:
			case STRUCT:
			case ERROR:
			case VAR_POINTER:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return null;
		}
		
		return null;
	}
	/**
	 * For "+"
	 */
	public DataObject opPos(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("pos", operand, pos);
		if(ret != null)
			return ret;

		return new DataObject(operand);
	}
	/**
	 * For "-"
	 */
	public DataObject opInv(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("inv", operand, pos);
		if(ret != null)
			return ret;

		switch(operand.getType()) {
			case INT:
				return new DataObject().setInt(-operand.getInt());
			case LONG:
				return new DataObject().setLong(-operand.getLong());
			case FLOAT:
				return new DataObject().setFloat(-operand.getFloat());
			case DOUBLE:
				return new DataObject().setDouble(-operand.getDouble());
			case CHAR:
				return new DataObject().setChar((char)(-operand.getChar()));
			case TEXT:
				return new DataObject(new StringBuilder(operand.getText()).reverse().toString());
			case BYTE_BUFFER:
				byte[] revByteBuf = new byte[operand.getByteBuffer().length];
				for(int i = 0;i < revByteBuf.length;i++)
					revByteBuf[i] = operand.getByteBuffer()[revByteBuf.length - 1 - i];
				
				return new DataObject().setByteBuffer(revByteBuf);
			case ARRAY:
				DataObject[] arrInv = new DataObject[operand.getArray().length];
				int index = arrInv.length - 1;
				for(DataObject dataObject:operand.getArray())
					arrInv[index--] = dataObject;
				
				return new DataObject().setArray(arrInv);
			case LIST:
				LinkedList<DataObject> listInv = new LinkedList<>(operand.getList());
				Collections.reverse(listInv);
				
				return new DataObject().setList(listInv);
			
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
	/**
	 * For "+"
	 */
	public DataObject opAdd(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("add", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() + rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getInt() + rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getInt() + rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getInt() + rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setInt(leftSideOperand.getInt() + rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() + rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() + rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getLong() + rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getLong() + rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setLong(leftSideOperand.getLong() + rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setFloat(leftSideOperand.getFloat() + rightSideOperand.getInt());
					case LONG:
						return new DataObject().setFloat(leftSideOperand.getFloat() + rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getFloat() + rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getFloat() + rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setFloat(leftSideOperand.getFloat() + rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(leftSideOperand.getDouble() + rightSideOperand.getInt());
					case LONG:
						return new DataObject().setDouble(leftSideOperand.getDouble() + rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setDouble(leftSideOperand.getDouble() + rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getDouble() + rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setDouble(leftSideOperand.getDouble() + rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case CHAR:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getChar() + rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getChar() + rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getChar() + rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getChar() + rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setInt(leftSideOperand.getChar() + rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case TEXT:
				return new DataObject(leftSideOperand.getText() + interpreter.conversions.toText(rightSideOperand, pos));
			case ARRAY:
				DataObject[] arrNew = new DataObject[leftSideOperand.getArray().length + 1];
				for(int i = 0;i < leftSideOperand.getArray().length;i++)
					arrNew[i] = leftSideOperand.getArray()[i];
				
				arrNew[leftSideOperand.getArray().length] = new DataObject(rightSideOperand);
				return new DataObject().setArray(arrNew);
			case LIST:
				LinkedList<DataObject> listNew = new LinkedList<>(leftSideOperand.getList());
				listNew.add(new DataObject(rightSideOperand));
				return new DataObject().setList(listNew);

			case FUNCTION_POINTER:
				if(rightSideOperand.getType() != DataType.FUNCTION_POINTER)
					return null;

				return new DataObject().setFunctionPointer(leftSideOperand.getFunctionPointer().
						withAddedFunctions(rightSideOperand.getFunctionPointer()));

			case BYTE_BUFFER:
			case ERROR:
			case VAR_POINTER:
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
	/**
	 * For "-"
	 */
	public DataObject opSub(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("sub", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() - rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getInt() - rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getInt() - rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getInt() - rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setInt(leftSideOperand.getInt() - rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() - rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() - rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getLong() - rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getLong() - rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setLong(leftSideOperand.getLong() - rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setFloat(leftSideOperand.getFloat() - rightSideOperand.getInt());
					case LONG:
						return new DataObject().setFloat(leftSideOperand.getFloat() - rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getFloat() - rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getFloat() - rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setFloat(leftSideOperand.getFloat() - rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(leftSideOperand.getDouble() - rightSideOperand.getInt());
					case LONG:
						return new DataObject().setDouble(leftSideOperand.getDouble() - rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setDouble(leftSideOperand.getDouble() - rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getDouble() - rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setDouble(leftSideOperand.getDouble() - rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			case CHAR:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getChar() - rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getChar() - rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getChar() - rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getChar() - rightSideOperand.getDouble());
					case CHAR:
						return new DataObject().setInt(leftSideOperand.getChar() - rightSideOperand.getChar());
					
					case TEXT:
					case BYTE_BUFFER:
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
			
			case TEXT:
			case BYTE_BUFFER:
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
	/**
	 * For "*"
	 */
	public DataObject opMul(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("mul", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() * rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getInt() * rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getInt() * rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getInt() * rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() * rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() * rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getLong() * rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getLong() * rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setFloat(leftSideOperand.getFloat() * rightSideOperand.getInt());
					case LONG:
						return new DataObject().setFloat(leftSideOperand.getFloat() * rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getFloat() * rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getFloat() * rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(leftSideOperand.getDouble() * rightSideOperand.getInt());
					case LONG:
						return new DataObject().setDouble(leftSideOperand.getDouble() * rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setDouble(leftSideOperand.getDouble() * rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getDouble() * rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case TEXT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() < 0)
							return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Integer value must be larger than or equals to 0", pos);
						
						StringBuilder builder = new StringBuilder();
						for(int i = 0;i < rightSideOperand.getInt();i++)
							builder.append(leftSideOperand.getText());
						
						return new DataObject(builder.toString());
					
					case LONG:
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case CHAR:
			case ARRAY:
			case BYTE_BUFFER:
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
	/**
	 * For "**"
	 */
	public DataObject opPow(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("pow", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						double d = Math.pow(leftSideOperand.getInt(), rightSideOperand.getInt());
						if(Math.abs(d) > Integer.MAX_VALUE || rightSideOperand.getInt() < 0)
							return new DataObject().setDouble(d);
						return new DataObject().setInt((int)d);
					case LONG:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getInt(), rightSideOperand.getLong()));
					case FLOAT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getInt(), rightSideOperand.getFloat()));
					case DOUBLE:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getInt(), rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getLong(), rightSideOperand.getInt()));
					case LONG:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getLong(), rightSideOperand.getLong()));
					case FLOAT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getLong(), rightSideOperand.getFloat()));
					case DOUBLE:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getLong(), rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getFloat(), rightSideOperand.getInt()));
					case LONG:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getFloat(), rightSideOperand.getLong()));
					case FLOAT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getFloat(), rightSideOperand.getFloat()));
					case DOUBLE:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getFloat(), rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getDouble(), rightSideOperand.getInt()));
					case LONG:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getDouble(), rightSideOperand.getLong()));
					case FLOAT:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getDouble(), rightSideOperand.getFloat()));
					case DOUBLE:
						return new DataObject().setDouble(Math.pow(leftSideOperand.getDouble(), rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FUNCTION_POINTER:
				if(rightSideOperand.getType() != DataType.INT)
					return null;
				
				final int count = rightSideOperand.getInt();
				if(count < 0)
					return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Number must not be less than 0!", pos);
				
				final FunctionPointerObject func = leftSideOperand.getFunctionPointer();
				
				if(count == 0)
					return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
						@LangFunction("pow-func")
						@SuppressWarnings("unused")
						public DataObject powFuncFunction(
								LangInterpreter interpreter,
								@LangParameter("&args") @RawVarArgs List<DataObject> args
						) {
							return new DataObject().setVoid();
						}
					}).withFunctionName("<" + func + " ** " + count + ">"));
				
				return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction("pow-func")
					@SuppressWarnings("unused")
					public DataObject powFuncFunction(
							LangInterpreter interpreter,
							@LangParameter("&args") @RawVarArgs List<DataObject> args
					) {
						DataObject retN = interpreter.callFunctionPointer(func, leftSideOperand.getVariableName(), args);
						DataObject ret = LangUtils.nullToLangVoid(retN);
						
						for(int i = 1;i < count;i++) {
							retN = interpreter.callFunctionPointer(func, leftSideOperand.getVariableName(), Arrays.asList(
									ret
							));
							ret = LangUtils.nullToLangVoid(retN);
						}
						
						return ret;
					}
				}, count, leftSideOperand).withFunctionName("<" + func + " ** " + count + ">"));
			
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
			case ARRAY:
			case LIST:
			case ERROR:
			case VAR_POINTER:
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
	/**
	 * For "/"
	 */
	public DataObject opDiv(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("div", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return new DataObject().setDouble(leftSideOperand.getInt() / 0.);
						
						if(leftSideOperand.getInt() % rightSideOperand.getInt() != 0)
							return new DataObject().setDouble(leftSideOperand.getInt() / (double)rightSideOperand.getInt());
						
						return new DataObject().setInt(leftSideOperand.getInt() / rightSideOperand.getInt());
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return new DataObject().setDouble(leftSideOperand.getInt() / 0.);
						
						if(leftSideOperand.getInt() % rightSideOperand.getLong() != 0)
							return new DataObject().setDouble(leftSideOperand.getInt() / (double)rightSideOperand.getLong());
						
						return new DataObject().setLong(leftSideOperand.getInt() / rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getInt() / rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getInt() / rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return new DataObject().setDouble(leftSideOperand.getLong() / 0.);
						
						if(leftSideOperand.getLong() % rightSideOperand.getInt() != 0)
							return new DataObject().setDouble(leftSideOperand.getLong() / (double)rightSideOperand.getInt());
						
						return new DataObject().setLong(leftSideOperand.getLong() / rightSideOperand.getInt());
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return new DataObject().setDouble(leftSideOperand.getLong() / 0.);
						
						if(leftSideOperand.getLong() % rightSideOperand.getLong() != 0)
							return new DataObject().setDouble(leftSideOperand.getLong() / (double)rightSideOperand.getLong());
						
						return new DataObject().setLong(leftSideOperand.getLong() / rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getLong() / rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getLong() / rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setFloat(leftSideOperand.getFloat() / rightSideOperand.getInt());
					case LONG:
						return new DataObject().setFloat(leftSideOperand.getFloat() / rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setFloat(leftSideOperand.getFloat() / rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getFloat() / rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setDouble(leftSideOperand.getDouble() / rightSideOperand.getInt());
					case LONG:
						return new DataObject().setDouble(leftSideOperand.getDouble() / rightSideOperand.getLong());
					case FLOAT:
						return new DataObject().setDouble(leftSideOperand.getDouble() / rightSideOperand.getFloat());
					case DOUBLE:
						return new DataObject().setDouble(leftSideOperand.getDouble() / rightSideOperand.getDouble());
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "~/"
	 */
	public DataObject opTruncDiv(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("truncDiv", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setInt(leftSideOperand.getInt() / rightSideOperand.getInt());
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(leftSideOperand.getInt() / rightSideOperand.getLong());
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						float tmpF = leftSideOperand.getInt() / rightSideOperand.getFloat();
						if(tmpF > 0)
							tmpF = (float)Math.floor(tmpF);
						else
							tmpF = (float)Math.ceil(tmpF);
						return new DataObject().setFloat(tmpF);
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						double tmpD = leftSideOperand.getInt() / rightSideOperand.getDouble();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(leftSideOperand.getLong() / rightSideOperand.getInt());
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(leftSideOperand.getLong() / rightSideOperand.getLong());
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						float tmpF = leftSideOperand.getLong() / rightSideOperand.getFloat();
						if(tmpF > 0)
							tmpF = (float)Math.floor(tmpF);
						else
							tmpF = (float)Math.ceil(tmpF);
						return new DataObject().setFloat(tmpF);
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						double tmpD = leftSideOperand.getLong() / rightSideOperand.getDouble();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						float tmpF = leftSideOperand.getFloat() / rightSideOperand.getInt();
						if(tmpF > 0)
							tmpF = (float)Math.floor(tmpF);
						else
							tmpF = (float)Math.ceil(tmpF);
						return new DataObject().setFloat(tmpF);
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						tmpF = leftSideOperand.getFloat() / rightSideOperand.getLong();
						if(tmpF > 0)
							tmpF = (float)Math.floor(tmpF);
						else
							tmpF = (float)Math.ceil(tmpF);
						return new DataObject().setFloat(tmpF);
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						tmpF = leftSideOperand.getFloat() / rightSideOperand.getFloat();
						if(tmpF > 0)
							tmpF = (float)Math.floor(tmpF);
						else
							tmpF = (float)Math.ceil(tmpF);
						return new DataObject().setFloat(tmpF);
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						double tmpD = leftSideOperand.getFloat() / rightSideOperand.getDouble();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						double tmpD = leftSideOperand.getDouble() / rightSideOperand.getInt();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						tmpD = leftSideOperand.getDouble() / rightSideOperand.getLong();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						tmpD = leftSideOperand.getDouble() / rightSideOperand.getFloat();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						tmpD = leftSideOperand.getDouble() / rightSideOperand.getDouble();
						if(tmpD > 0)
							tmpD = Math.floor(tmpD);
						else
							tmpD = Math.ceil(tmpD);
						return new DataObject().setDouble(tmpD);
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "//"
	 */
	public DataObject opFloorDiv(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("floorDiv", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setInt(Math.floorDiv(leftSideOperand.getInt(), rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(Math.floorDiv(leftSideOperand.getInt(), rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.floor(leftSideOperand.getInt() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getInt() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(Math.floorDiv(leftSideOperand.getLong(), (long)rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(Math.floorDiv(leftSideOperand.getLong(), rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.floor(leftSideOperand.getLong() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getLong() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.floor(leftSideOperand.getFloat() / rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.floor(leftSideOperand.getFloat() / rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.floor(leftSideOperand.getFloat() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getFloat() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getDouble() / rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getDouble() / rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getDouble() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.floor(leftSideOperand.getDouble() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "^/"
	 */
	public DataObject opCeilDiv(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("ceilDiv", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setInt(-Math.floorDiv(-leftSideOperand.getInt(), rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(-Math.floorDiv(-leftSideOperand.getInt(), rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.ceil(leftSideOperand.getInt() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getInt() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(-Math.floorDiv(-leftSideOperand.getLong(), (long)rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(-Math.floorDiv(-leftSideOperand.getLong(), rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.ceil(leftSideOperand.getLong() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getLong() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case FLOAT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.ceil(leftSideOperand.getFloat() / rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.ceil(leftSideOperand.getFloat() / rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setFloat((float)Math.ceil(leftSideOperand.getFloat() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getFloat() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case DOUBLE:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getDouble() / rightSideOperand.getInt()));
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getDouble() / rightSideOperand.getLong()));
					case FLOAT:
						if(rightSideOperand.getFloat() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getDouble() / rightSideOperand.getFloat()));
					case DOUBLE:
						if(rightSideOperand.getDouble() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setDouble(Math.ceil(leftSideOperand.getDouble() / rightSideOperand.getDouble()));
					
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "%"
	 */
	public DataObject opMod(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("mod", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setInt(leftSideOperand.getInt() % rightSideOperand.getInt());
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(leftSideOperand.getInt() % rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						if(rightSideOperand.getInt() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(leftSideOperand.getLong() % rightSideOperand.getInt());
					case LONG:
						if(rightSideOperand.getLong() == 0)
							return interpreter.setErrnoErrorObject(InterpretingError.DIV_BY_ZERO, pos);
						
						return new DataObject().setLong(leftSideOperand.getLong() % rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case TEXT:
				if(rightSideOperand.getType() == DataType.ARRAY)
					return interpreter.formatText(leftSideOperand.getText(), new LinkedList<>(Arrays.asList(rightSideOperand.getArray())));
				
				return null;
			
			case FLOAT:
			case DOUBLE:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "&amp;"
	 */
	public DataObject opAnd(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("and", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() & rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getInt() & rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() & rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() & rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "|"
	 */
	public DataObject opOr(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("or", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		if(rightSideOperand.getType() == DataType.FUNCTION_POINTER) {
			FunctionPointerObject func = rightSideOperand.getFunctionPointer();
			
			return interpreter.callFunctionPointer(func, rightSideOperand.getVariableName(), Arrays.asList(
					leftSideOperand
			));
		}
		
		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() | rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getInt() | rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() | rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() | rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "^"
	 */
	public DataObject opXor(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("xor", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() ^ rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getInt() ^ rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() ^ rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() ^ rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "~"
	 */
	public DataObject opNot(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("not", operand, pos);
		if(ret != null)
			return ret;

		switch(operand.getType()) {
			case INT:
				return new DataObject().setInt(~operand.getInt());
			case LONG:
				return new DataObject().setLong(~operand.getLong());
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "&lt;&lt;"
	 */
	public DataObject opLshift(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("lshift", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() << rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong((long)leftSideOperand.getInt() << rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() << rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() << rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "&gt;&gt;"
	 */
	public DataObject opRshift(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("rshift", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		if(rightSideOperand.getType() == DataType.FUNCTION_POINTER) {
			FunctionPointerObject func = rightSideOperand.getFunctionPointer();
			
			return interpreter.callFunctionPointer(func, rightSideOperand.getVariableName(), Arrays.asList(
					leftSideOperand
			));
		}
		
		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() >> rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong((long)leftSideOperand.getInt() >> rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() >> rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() >> rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	/**
	 * For "&gt;&gt;&gt;"
	 */
	public DataObject opRzshift(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("rzshift", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		if(rightSideOperand.getType() == DataType.FUNCTION_POINTER && leftSideOperand.getType() == DataType.ARRAY) {
			FunctionPointerObject func = rightSideOperand.getFunctionPointer();
			
			return interpreter.callFunctionPointer(func, rightSideOperand.getVariableName(), LangUtils.separateArgumentsWithArgumentSeparators(
					Arrays.asList(leftSideOperand.getArray())
			));
		}
		
		switch(leftSideOperand.getType()) {
			case INT:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setInt(leftSideOperand.getInt() >>> rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong((long)leftSideOperand.getInt() >>> rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			case LONG:
				switch(rightSideOperand.getType()) {
					case INT:
						return new DataObject().setLong(leftSideOperand.getLong() >>> rightSideOperand.getInt());
					case LONG:
						return new DataObject().setLong(leftSideOperand.getLong() >>> rightSideOperand.getLong());
					
					case FLOAT:
					case DOUBLE:
					case TEXT:
					case CHAR:
					case BYTE_BUFFER:
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
			
			case FLOAT:
			case DOUBLE:
			case TEXT:
			case CHAR:
			case BYTE_BUFFER:
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
	
	//All operation functions
	public DataObject opCast(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(leftSideOperand.getType() != DataType.TYPE)
			return null;
		
		switch(leftSideOperand.getTypeValue()) {
			case TEXT:
				String txt = interpreter.conversions.toText(rightSideOperand, pos);
				if(txt == null)
					return null;
				
				return new DataObject(txt);
			case CHAR:
				Character chr = interpreter.conversions.toChar(rightSideOperand, pos);
				if(chr == null)
					return null;
				
				return new DataObject().setChar(chr);
			case INT:
				Integer i = interpreter.conversions.toInt(rightSideOperand, pos);
				if(i == null)
					return null;
				
				return new DataObject().setInt(i);
			case LONG:
				Long l = interpreter.conversions.toLong(rightSideOperand, pos);
				if(l == null)
					return null;
				
				return new DataObject().setLong(l);
			case FLOAT:
				Float f = interpreter.conversions.toFloat(rightSideOperand, pos);
				if(f == null)
					return null;
				
				return new DataObject().setFloat(f);
			case DOUBLE:
				Double d = interpreter.conversions.toDouble(rightSideOperand, pos);
				if(d == null)
					return null;
				
				return new DataObject().setDouble(d);
			case BYTE_BUFFER:
				byte[] byteBuffer = interpreter.conversions.toByteBuffer(rightSideOperand, pos);
				if(byteBuffer == null)
					return null;
				
				return new DataObject().setByteBuffer(byteBuffer);
			case ARRAY:
				DataObject[] arr = interpreter.conversions.toArray(rightSideOperand, pos);
				if(arr == null)
					return null;
				
				return new DataObject().setArray(arr);
			case LIST:
				LinkedList<DataObject> list = interpreter.conversions.toList(rightSideOperand, pos);
				if(list == null)
					return null;
				
				return new DataObject().setList(list);
			
			case ARGUMENT_SEPARATOR:
			case ERROR:
			case FUNCTION_POINTER:
			case NULL:
			case STRUCT:
			case OBJECT:
			case TYPE:
			case VAR_POINTER:
			case VOID:
				break;
		}
		
		return null;
	}
	/**
	 * For "[...]"
	 */
	public DataObject opGetItem(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("getItem", false, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case BYTE_BUFFER:
				if(rightSideOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getByteBuffer().length;
					int index = rightSideOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					return new DataObject().setInt(leftSideOperand.getByteBuffer()[index]);
				}
				
				return null;
			case ARRAY:
				if(rightSideOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getArray().length;
					int index = rightSideOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					return new DataObject(leftSideOperand.getArray()[index]);
				}
				
				return null;
			case LIST:
				if(rightSideOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getList().size();
					int index = rightSideOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					return new DataObject(leftSideOperand.getList().get(index));
				}
				
				return null;
			case TEXT:
				if(rightSideOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getText().length();
					int index = rightSideOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					return new DataObject().setChar(leftSideOperand.getText().charAt(index));
				}
				
				return null;
			case CHAR:
				if(rightSideOperand.getType() == DataType.INT) {
					int index = rightSideOperand.getInt();
					if(index < 0)
						index++;
					
					if(index != 0)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					return new DataObject().setChar(leftSideOperand.getChar());
				}
				
				return null;
			case STRUCT:
				if(rightSideOperand.getType() == DataType.TEXT) {
					try {
						return new DataObject(leftSideOperand.getStruct().getMember(rightSideOperand.getText()));
					}catch(DataTypeConstraintException e) {
						return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);
					}
				}
				
				return null;
			
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
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
	/**
	 * For "?[...]"
	 */
	public DataObject opOptionalGetItem(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(leftSideOperand.getType() == DataType.NULL || leftSideOperand.getType() == DataType.VOID)
			return new DataObject().setVoid();
		
		return opGetItem(leftSideOperand, rightSideOperand, pos);
	}
	public DataObject opSetItem(DataObject leftSideOperand, DataObject middleOperand, DataObject rightSideOperand, CodePosition pos) {
		DataObject ret = callOperatorMethod("setItem", leftSideOperand, middleOperand, rightSideOperand, pos);
		if(ret != null)
			return ret;

		switch(leftSideOperand.getType()) {
			case BYTE_BUFFER:
				if(middleOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getByteBuffer().length;
					int index = middleOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					Number valueNumber = interpreter.conversions.toNumber(rightSideOperand, pos);
					if(valueNumber == null)
						return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM, pos);
					byte value = valueNumber.byteValue();
					
					leftSideOperand.getByteBuffer()[index] = value;
					
					return new DataObject().setVoid();
				}
				
				return null;
			case ARRAY:
				if(middleOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getArray().length;
					int index = middleOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					leftSideOperand.getArray()[index] = new DataObject(rightSideOperand);
					
					return new DataObject().setVoid();
				}
				
				return null;
			case LIST:
				if(middleOperand.getType() == DataType.INT) {
					int len = leftSideOperand.getList().size();
					int index = middleOperand.getInt();
					if(index < 0)
						index += len;
					
					if(index < 0 || index >= len)
						return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS, pos);
					
					leftSideOperand.getList().set(index, new DataObject(rightSideOperand));
					
					return new DataObject().setVoid();
				}
			case STRUCT:
				if(middleOperand.getType() == DataType.TEXT) {
					try {
						leftSideOperand.getStruct().setMember(middleOperand.getText(), rightSideOperand);
						
						return new DataObject().setVoid();
					}catch(DataTypeConstraintException e) {
						return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);
					}
				}
				
				return null;
			
			case TEXT:
			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
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

	//Special operator functions
	/**
	 * For "func.abs()"
	 */
	public DataObject opAbs(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("abs", operand, pos);
		if(ret != null)
			return ret;
		switch(operand.getType()) {
			case INT:
				return new DataObject().setInt(Math.abs(operand.getInt()));
			case LONG:
				return new DataObject().setLong(Math.abs(operand.getLong()));
			case FLOAT:
				return new DataObject().setFloat(Math.abs(operand.getFloat()));
			case DOUBLE:
				return new DataObject().setDouble(Math.abs(operand.getDouble()));

			case TEXT:
			case CHAR:
			case ARRAY:
			case BYTE_BUFFER:
			case LIST:
			case STRUCT:
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
	/**
	 * For "func.iter()"
	 */
	public DataObject opIter(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("iter", operand, pos);
		if(ret != null)
			return ret;
		switch(operand.getType()) {
			case BYTE_BUFFER:
			case ARRAY:
			case LIST:
			case STRUCT:
			case TEXT:
				return interpreter.callConstructor(interpreter.standardTypes.get("&BasicIterator").getObject(),
						Arrays.asList(operand), pos);

			case CHAR:
			case INT:
			case LONG:
			case FLOAT:
			case DOUBLE:
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
	/**
	 * For "func.hasNext()"
	 */
	public DataObject opHasNext(DataObject operand, CodePosition pos) {
		DataObject ret = callOperatorMethod("hasNext", operand, pos);
		if(ret != null)
			return new DataObject().setBoolean(interpreter.conversions.toBool(ret, pos));

		return null;
	}
	/**
	 * For "func.next()"
	 */
	public DataObject opNext(DataObject operand, CodePosition pos) {
		return callOperatorMethod("next", operand, pos);
	}

	/**
	 * For "...(...)"
	 */
	public DataObject opCall(DataObject callee, List<DataObject> argumentList, CodePosition pos) {
		DataObject ret = callOperatorMethod(callee, "op:call", argumentList, pos);
		if(ret != null)
			return ret;

		if(callee.getType() == DataType.FUNCTION_POINTER)
			return interpreter.callFunctionPointer(callee.getFunctionPointer(), callee.getVariableName(), argumentList,
					pos);

		if(callee.getType() == DataType.TYPE) {
			List<DataObject> combinedArgumentList = LangUtils.combineArgumentsWithoutArgumentSeparators(argumentList,
					interpreter, pos);

			if(combinedArgumentList.isEmpty())
				return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT,
						"Not enough arguments for TYPE casting (1 needed)", pos);
			if(combinedArgumentList.size() > 1)
				return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT,
						"Too many arguments for TYPE casting (1 needed)", pos);

			DataObject arg = combinedArgumentList.get(0);

			DataObject output = opCast(callee, arg, pos);
			if(output == null)
				return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Data type \"" +
								arg.getType() + "\" can not be casted to \"" + callee.getTypeValue() + "\"!", pos);

			return output;
		}

		if(callee.getType() == DataType.STRUCT && callee.getStruct().isDefinition()) {
			List<DataObject> combinedArgumentList = LangUtils.combineArgumentsWithoutArgumentSeparators(argumentList,
					interpreter, pos);

			StructObject struct = callee.getStruct();

			String[] memberNames = struct.getMemberNames();
			if(combinedArgumentList.size() != memberNames.length) {
				return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
						"The argument count is not equals to the count of member names (" + memberNames.length + ")",
						pos);
			}

			try {
				return new DataObject().setStruct(new StructObject(struct, combinedArgumentList.toArray(new DataObject[0])));
			}catch(DataTypeConstraintException e) {
				return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(),
						pos);
			}
		}

		if(callee.getType() == DataType.OBJECT && callee.getObject().isClass()) {
			DataObject createdObject = new DataObject().setObject(new DataObject.LangObject(callee.getObject()));

			FunctionPointerObject constructors = createdObject.getObject().getConstructors();

			ret = interpreter.callFunctionPointer(constructors, constructors.getFunctionName(), argumentList,
					pos);
			if(ret == null)
				ret = new DataObject().setVoid();

			if(ret.getType() != DataType.VOID)
				return interpreter.setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
						"Invalid constructor implementation: VOID must be returned");

			try {
				createdObject.getObject().postConstructor();
			}catch(DataTypeConstraintException e) {
				return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
						"Invalid constructor implementation (Some members have invalid types): " + e.getMessage(),
						pos);
			}

			return createdObject;
		}

		return null;
	}

	//Comparison functions
	/**
	 * For "=="
	 */
	public boolean isEquals(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(leftSideOperand == null || rightSideOperand == null)
			return false;

		DataObject ret = callOperatorMethod("isEquals", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return interpreter.conversions.toBool(ret, pos);

		if(leftSideOperand == rightSideOperand)
			return true;

		Number number = interpreter.conversions.toNumber(rightSideOperand, pos);
		switch(leftSideOperand.getType()) {
			case TEXT:
				if(rightSideOperand.getType() == DataType.TEXT)
					return leftSideOperand.getText().equals(rightSideOperand.getText());

				if(leftSideOperand.getText().length() == 1 && rightSideOperand.getType() == DataType.CHAR)
					return leftSideOperand.getText().charAt(0) == rightSideOperand.getChar();

				return number != null && isEquals(rightSideOperand, leftSideOperand, pos);

			case CHAR:
				if(rightSideOperand.getType() == DataType.TEXT && rightSideOperand.getText().length() == 1)
					return leftSideOperand.getChar() == rightSideOperand.getText().charAt(0);

				return number != null && leftSideOperand.getChar() == number.intValue();

			case INT:
				return number != null && leftSideOperand.getInt() == number.intValue();

			case LONG:
				return number != null && leftSideOperand.getLong() == number.longValue();

			case FLOAT:
				return number != null && leftSideOperand.getFloat() == number.floatValue();

			case DOUBLE:
				return number != null && leftSideOperand.getDouble() == number.doubleValue();

			case BYTE_BUFFER:
				if(rightSideOperand.getType() == DataType.BYTE_BUFFER)
					return Arrays.equals(leftSideOperand.getByteBuffer(), rightSideOperand.getByteBuffer());

				return number != null && leftSideOperand.getByteBuffer().length == number.intValue();

			case ARRAY:
				if(rightSideOperand.getType() == DataType.ARRAY) {
					int len = leftSideOperand.getArray().length;
					if(len != rightSideOperand.getArray().length)
						return false;

					int i = 0;
					while(i < len) {
						if(!isEquals(leftSideOperand.getArray()[i], rightSideOperand.getArray()[i], pos))
							return false;

						i++;
					}

					return true;
				}

				if(rightSideOperand.getType() == DataType.LIST) {
					int len = leftSideOperand.getArray().length;
					if(len != rightSideOperand.getList().size())
						return false;

					int i = 0;
					while(i < len) {
						if(!isEquals(leftSideOperand.getArray()[i], rightSideOperand.getList().get(i), pos))
							return false;

						i++;
					}

					return true;
				}

				return number != null && leftSideOperand.getArray().length == number.intValue();

			case LIST:
				if(rightSideOperand.getType() == DataType.LIST) {
					int len = leftSideOperand.getList().size();
					if(len != rightSideOperand.getList().size())
						return false;

					int i = 0;
					while(i < len) {
						if(!isEquals(leftSideOperand.getList().get(i), rightSideOperand.getList().get(i), pos))
							return false;

						i++;
					}

					return true;
				}

				if(rightSideOperand.getType() == DataType.ARRAY) {
					int len = leftSideOperand.getList().size();
					if(len != rightSideOperand.getArray().length)
						return false;

					int i = 0;
					while(i < len) {
						if(!isEquals(leftSideOperand.getList().get(i), rightSideOperand.getArray()[i], pos))
							return false;

						i++;
					}

					return true;
				}

				return number != null && leftSideOperand.getList().size() == number.intValue();

			case STRUCT:
				if(rightSideOperand.getType() == DataType.STRUCT) {
					StructObject leftStruct = leftSideOperand.getStruct();
					StructObject rightStruct = rightSideOperand.getStruct();

					if(leftStruct.isDefinition() != rightStruct.isDefinition())
						return false;

					String[] leftMemberNames = leftStruct.getMemberNames();
					String[] rightMemberNames = rightStruct.getMemberNames();

					int len = leftMemberNames.length;
					if(len != rightMemberNames.length)
						return false;

					int i = 0;
					while(i < len) {
						if(!leftMemberNames[i].equals(rightMemberNames[i]) || (!leftStruct.isDefinition() &&
								!isEquals(leftStruct.getMember(leftMemberNames[i]), rightStruct.getMember(rightMemberNames[i]),
										pos)))
							return false;

						i++;
					}

					return true;
				}

				return number != null && leftSideOperand.getStruct().getMemberNames().length == number.intValue();

			case OBJECT:
				if(rightSideOperand.getType() == DataType.OBJECT)
					//Check for same reference only (For classes and objects if "op:isEquals()" is not defined)
					return leftSideOperand.getObject() == rightSideOperand.getObject();

				return false;

			case VAR_POINTER:
				//Check for same reference only
				return leftSideOperand.getVarPointer().getVar() == rightSideOperand.getVarPointer().getVar();

			case FUNCTION_POINTER:
				if(rightSideOperand.getType() == DataType.FUNCTION_POINTER)
					return leftSideOperand.getFunctionPointer().isEquals(rightSideOperand.getFunctionPointer(),
							interpreter, pos);

				return false;

			case ERROR:
				switch(rightSideOperand.getType()) {
					case TEXT:
						if(number == null)
							return leftSideOperand.getError().getErrtxt().equals(rightSideOperand.getText());
						return leftSideOperand.getError().getErrno() == number.intValue();

					case CHAR:
					case INT:
					case LONG:
					case FLOAT:
					case DOUBLE:
					case BYTE_BUFFER:
					case ARRAY:
					case LIST:
					case STRUCT:
						return number != null && leftSideOperand.getError().getErrno() == number.intValue();

					case ERROR:
						return leftSideOperand.getError().equals(rightSideOperand.getError());

					case VAR_POINTER:
					case FUNCTION_POINTER:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

				return false;

			case TYPE:
				return rightSideOperand.getType() == DataType.TYPE?leftSideOperand.getTypeValue() == rightSideOperand.getTypeValue():
						leftSideOperand.getTypeValue() == rightSideOperand.getType();

			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
				return leftSideOperand.getType() == rightSideOperand.getType();
		}

		return false;
	}
	/**
	 * For "==="
	 */
	public boolean isStrictEquals(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(leftSideOperand == null || rightSideOperand == null)
			return false;

		DataObject ret = callOperatorMethod("isStrictEquals", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return interpreter.conversions.toBool(ret, pos);

		if(leftSideOperand == rightSideOperand)
			return true;

		if(leftSideOperand.getType() != rightSideOperand.getType())
			return false;

		switch(leftSideOperand.getType()) {
			case TEXT:
				return leftSideOperand.getText().equals(rightSideOperand.getText());

			case CHAR:
				return leftSideOperand.getChar() == rightSideOperand.getChar();

			case INT:
				return leftSideOperand.getInt() == rightSideOperand.getInt();

			case LONG:
				return leftSideOperand.getLong() == rightSideOperand.getLong();

			case FLOAT:
				return leftSideOperand.getFloat() == rightSideOperand.getFloat();

			case DOUBLE:
				return leftSideOperand.getDouble() == rightSideOperand.getDouble();

			case BYTE_BUFFER:
				return Arrays.equals(leftSideOperand.getByteBuffer(), rightSideOperand.getByteBuffer());

			case ARRAY:
				{
					int len = leftSideOperand.getArray().length;
					if(len != rightSideOperand.getArray().length)
						return false;

					int i = 0;
					while(i < len) {
						if(!isStrictEquals(leftSideOperand.getArray()[i], rightSideOperand.getArray()[i], pos))
							return false;

						i++;
					}

					return true;
				}

			case LIST:
				{
					int len = leftSideOperand.getList().size();
					if(len != rightSideOperand.getList().size())
						return false;

					int i = 0;
					while(i < len) {
						if(!isStrictEquals(leftSideOperand.getList().get(i), rightSideOperand.getList().get(i), pos))
							return false;

						i++;
					}

					return true;
				}

			case STRUCT:
				{
					StructObject leftStruct = leftSideOperand.getStruct();
					StructObject rightStruct = rightSideOperand.getStruct();

					if(leftStruct.isDefinition() != rightStruct.isDefinition())
						return false;

					String[] leftMemberNames = leftStruct.getMemberNames();
					String[] rightMemberNames = rightStruct.getMemberNames();

					int len = leftMemberNames.length;
					if(len != rightMemberNames.length)
						return false;

					int i = 0;
					while(i < len) {
						if(!leftMemberNames[i].equals(rightMemberNames[i]) || (!leftStruct.isDefinition() &&
								!isStrictEquals(leftStruct.getMember(leftMemberNames[i]), rightStruct.getMember(rightMemberNames[i]),
										pos)))
							return false;

						i++;
					}

					return true;
				}

			case OBJECT:
				if(rightSideOperand.getType() == DataType.OBJECT)
					//Check for same reference only (For classes and objects if "op:isStrictEquals()" is not defined)
					return leftSideOperand.getObject() == rightSideOperand.getObject();

				return false;

			case VAR_POINTER:
				//Check for same reference only
				return leftSideOperand.getVarPointer().getVar() == rightSideOperand.getVarPointer().getVar();

			case FUNCTION_POINTER:
				return leftSideOperand.getFunctionPointer().isStrictEquals(rightSideOperand.getFunctionPointer(),
						interpreter, pos);

			case ERROR:
				return leftSideOperand.getError().equals(rightSideOperand.getError());

			case TYPE:
				return leftSideOperand.getTypeValue() == rightSideOperand.getTypeValue();

			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
				return leftSideOperand.getType() == rightSideOperand.getType();
		}

		return false;
	}
	/**
	 * For "&lt;"
	 */
	public boolean isLessThan(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(leftSideOperand == null || rightSideOperand == null)
			return false;

		DataObject ret = callOperatorMethod("isLessThan", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return interpreter.conversions.toBool(ret, pos);

		if(leftSideOperand == rightSideOperand)
			return false;

		DataObject number = interpreter.conversions.convertToNumberAndCreateNewDataObject(rightSideOperand, pos);
		switch(leftSideOperand.getType()) {
			case TEXT:
				if(rightSideOperand.getType() == DataType.TEXT)
					return leftSideOperand.getText().compareTo(rightSideOperand.getText()) < 0;

				if(leftSideOperand.getText().length() == 1 && rightSideOperand.getType() == DataType.CHAR)
					return leftSideOperand.getText().charAt(0) < rightSideOperand.getChar();

				Number thisNumber = interpreter.conversions.toNumber(leftSideOperand, pos);
				if(thisNumber == null)
					return false;

				switch(number.getType()) {
					case INT:
						return thisNumber.intValue() < number.getInt();
					case LONG:
						return thisNumber.longValue() < number.getLong();
					case FLOAT:
						return thisNumber.floatValue() < number.getFloat();
					case DOUBLE:
						return thisNumber.doubleValue() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

				return false;

			case CHAR:
				if(rightSideOperand.getType() == DataType.TEXT && rightSideOperand.getText().length() == 1)
					return leftSideOperand.getChar() < rightSideOperand.getText().charAt(0);

				switch(number.getType()) {
					case INT:
						return leftSideOperand.getChar() < number.getInt();
					case LONG:
						return leftSideOperand.getChar() < number.getLong();
					case FLOAT:
						return leftSideOperand.getChar() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getChar() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case INT:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getInt() < number.getInt();
					case LONG:
						return leftSideOperand.getInt() < number.getLong();
					case FLOAT:
						return leftSideOperand.getInt() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getInt() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case LONG:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getLong() < number.getInt();
					case LONG:
						return leftSideOperand.getLong() < number.getLong();
					case FLOAT:
						return leftSideOperand.getLong() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getLong() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case FLOAT:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getFloat() < number.getInt();
					case LONG:
						return leftSideOperand.getFloat() < number.getLong();
					case FLOAT:
						return leftSideOperand.getFloat() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getFloat() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case DOUBLE:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getDouble() < number.getInt();
					case LONG:
						return leftSideOperand.getDouble() < number.getLong();
					case FLOAT:
						return leftSideOperand.getDouble() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getDouble() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case BYTE_BUFFER:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getByteBuffer().length < number.getInt();
					case LONG:
						return leftSideOperand.getByteBuffer().length < number.getLong();
					case FLOAT:
						return leftSideOperand.getByteBuffer().length < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getByteBuffer().length < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case ARRAY:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getArray().length < number.getInt();
					case LONG:
						return leftSideOperand.getArray().length < number.getLong();
					case FLOAT:
						return leftSideOperand.getArray().length < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getArray().length < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case LIST:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getList().size() < number.getInt();
					case LONG:
						return leftSideOperand.getList().size() < number.getLong();
					case FLOAT:
						return leftSideOperand.getList().size() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getList().size() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case ERROR:
				if(rightSideOperand.getType() == DataType.TEXT && number.getType() == DataType.NULL)
					return leftSideOperand.getError().getErrtxt().compareTo(rightSideOperand.getText()) < 0;
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getError().getErrno() < number.getInt();
					case LONG:
						return leftSideOperand.getError().getErrno() < number.getLong();
					case FLOAT:
						return leftSideOperand.getError().getErrno() < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getError().getErrno() < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

				return false;

			case STRUCT:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getStruct().getMemberNames().length < number.getInt();
					case LONG:
						return leftSideOperand.getStruct().getMemberNames().length < number.getLong();
					case FLOAT:
						return leftSideOperand.getStruct().getMemberNames().length < number.getFloat();
					case DOUBLE:
						return leftSideOperand.getStruct().getMemberNames().length < number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case VAR_POINTER:
			case FUNCTION_POINTER:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return false;
		}

		return false;
	}
	/**
	 * For "&gt;"
	 */
	public boolean isGreaterThan(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		if(leftSideOperand == null || rightSideOperand == null)
			return false;

		DataObject ret = callOperatorMethod("isGreaterThan", true, leftSideOperand, rightSideOperand, pos);
		if(ret != null)
			return interpreter.conversions.toBool(ret, pos);

		if(leftSideOperand == rightSideOperand)
			return false;

		DataObject number = interpreter.conversions.convertToNumberAndCreateNewDataObject(rightSideOperand, pos);
		switch(leftSideOperand.getType()) {
			case TEXT:
				if(rightSideOperand.getType() == DataType.TEXT)
					return leftSideOperand.getText().compareTo(rightSideOperand.getText()) > 0;

				if(leftSideOperand.getText().length() == 1 && rightSideOperand.getType() == DataType.CHAR)
					return leftSideOperand.getText().charAt(0) > rightSideOperand.getChar();

				Number thisNumber = interpreter.conversions.toNumber(leftSideOperand, pos);
				if(thisNumber == null)
					return false;

				switch(number.getType()) {
					case INT:
						return thisNumber.intValue() > number.getInt();
					case LONG:
						return thisNumber.longValue() > number.getLong();
					case FLOAT:
						return thisNumber.floatValue() > number.getFloat();
					case DOUBLE:
						return thisNumber.doubleValue() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

				return false;

			case CHAR:
				if(rightSideOperand.getType() == DataType.TEXT && rightSideOperand.getText().length() == 1)
					return leftSideOperand.getChar() > rightSideOperand.getText().charAt(0);

				switch(number.getType()) {
					case INT:
						return leftSideOperand.getChar() > number.getInt();
					case LONG:
						return leftSideOperand.getChar() > number.getLong();
					case FLOAT:
						return leftSideOperand.getChar() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getChar() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case INT:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getInt() > number.getInt();
					case LONG:
						return leftSideOperand.getInt() > number.getLong();
					case FLOAT:
						return leftSideOperand.getInt() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getInt() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case LONG:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getLong() > number.getInt();
					case LONG:
						return leftSideOperand.getLong() > number.getLong();
					case FLOAT:
						return leftSideOperand.getLong() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getLong() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case FLOAT:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getFloat() > number.getInt();
					case LONG:
						return leftSideOperand.getFloat() > number.getLong();
					case FLOAT:
						return leftSideOperand.getFloat() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getFloat() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case DOUBLE:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getDouble() > number.getInt();
					case LONG:
						return leftSideOperand.getDouble() > number.getLong();
					case FLOAT:
						return leftSideOperand.getDouble() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getDouble() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case BYTE_BUFFER:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getByteBuffer().length > number.getInt();
					case LONG:
						return leftSideOperand.getByteBuffer().length > number.getLong();
					case FLOAT:
						return leftSideOperand.getByteBuffer().length > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getByteBuffer().length > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case ARRAY:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getArray().length > number.getInt();
					case LONG:
						return leftSideOperand.getArray().length > number.getLong();
					case FLOAT:
						return leftSideOperand.getArray().length > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getArray().length > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case LIST:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getList().size() > number.getInt();
					case LONG:
						return leftSideOperand.getList().size() > number.getLong();
					case FLOAT:
						return leftSideOperand.getList().size() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getList().size() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case ERROR:
				if(rightSideOperand.getType() == DataType.TEXT && number.getType() == DataType.NULL)
					return leftSideOperand.getError().getErrtxt().compareTo(rightSideOperand.getText()) > 0;
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getError().getErrno() > number.getInt();
					case LONG:
						return leftSideOperand.getError().getErrno() > number.getLong();
					case FLOAT:
						return leftSideOperand.getError().getErrno() > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getError().getErrno() > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

				return false;

			case STRUCT:
				switch(number.getType()) {
					case INT:
						return leftSideOperand.getStruct().getMemberNames().length > number.getInt();
					case LONG:
						return leftSideOperand.getStruct().getMemberNames().length > number.getLong();
					case FLOAT:
						return leftSideOperand.getStruct().getMemberNames().length > number.getFloat();
					case DOUBLE:
						return leftSideOperand.getStruct().getMemberNames().length > number.getDouble();

					case CHAR:
					case BYTE_BUFFER:
					case ERROR:
					case ARRAY:
					case LIST:
					case TEXT:
					case VAR_POINTER:
					case FUNCTION_POINTER:
					case STRUCT:
					case OBJECT:
					case NULL:
					case VOID:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						return false;
				}

			case VAR_POINTER:
			case FUNCTION_POINTER:
			case OBJECT:
			case NULL:
			case VOID:
			case ARGUMENT_SEPARATOR:
			case TYPE:
				return false;
		}

		return false;
	}
	/**
	 * For "&lt;="
	 */
	public boolean isLessThanOrEquals(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		return isLessThan(leftSideOperand, rightSideOperand, pos) ||
				isEquals(leftSideOperand, rightSideOperand, pos);
	}
	/**
	 * For "&gt;="
	 */
	public boolean isGreaterThanOrEquals(DataObject leftSideOperand, DataObject rightSideOperand, CodePosition pos) {
		return isGreaterThan(leftSideOperand, rightSideOperand, pos) ||
				isEquals(leftSideOperand, rightSideOperand, pos);
	}
}