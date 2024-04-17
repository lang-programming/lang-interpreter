package at.jddev0.lang;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import at.jddev0.lang.LangInterpreter.InterpretingError;

/**
 * Lang-Module<br>
 * Class for variable data
 *
 * @author JDDev0
 * @version v1.0.0
 */
public class DataObject {
	public final static DataTypeConstraint CONSTRAINT_NORMAL = DataTypeConstraint.fromNotAllowedTypes(new ArrayList<>());
	public final static DataTypeConstraint CONSTRAINT_COMPOSITE = DataTypeConstraint.fromAllowedTypes(Arrays.asList(DataType.ARRAY, DataType.LIST, DataType.STRUCT, DataType.OBJECT, DataType.NULL));
	public final static DataTypeConstraint CONSTRAINT_FUNCTION_POINTER = DataTypeConstraint.fromAllowedTypes(Arrays.asList(DataType.FUNCTION_POINTER, DataType.NULL));

	//Value
	private String txt;
	private byte[] byteBuf;
	private DataObject[] arr;
	private LinkedList<DataObject> list;
	private VarPointerObject vp;
	private FunctionPointerObject fp;
	private StructObject sp;
	private LangObject op;
	private int intValue;
	private long longValue;
	private float floatValue;
	private double doubleValue;
	private char charValue;
	private ErrorObject error;
	private DataType typeValue;

	private DataTypeConstraint typeConstraint = CONSTRAINT_NORMAL;

	//Meta-Data
	/**
	 * Variable name of the DataObject (null for anonymous variable)
	 */
	private String variableName;
	private DataType type;
	private boolean finalData;
	private boolean staticData;
	private boolean copyStaticAndFinalModifiers;
	private boolean langVar;

	public static DataTypeConstraint getTypeConstraintFor(String variableName) {
		if(variableName == null)
			return CONSTRAINT_NORMAL;

		if(variableName.startsWith("&"))
			return CONSTRAINT_COMPOSITE;

		if(variableName.startsWith("mp.") || variableName.startsWith("fp.") || variableName.startsWith("func.") ||
				variableName.startsWith("fn.") || variableName.startsWith("linker.") || variableName.startsWith("ln."))
			return CONSTRAINT_FUNCTION_POINTER;

		return CONSTRAINT_NORMAL;
	}

	public DataObject(DataObject dataObject) {
		setData(dataObject);
	}

	/**
	 * Creates a new data object of type NULL
	 */
	public DataObject() {
		setNull();
	}
	public DataObject(String txt) {
		this(txt, false);
	}
	public DataObject(String txt, boolean finalData) {
		setText(txt);
		setFinalData(finalData);
	}

	/**
	 * This method <b>ignores</b> the final and static state of the data object<br>
	 * This method will not modify variableName<br>
	 * This method will also not modify finalData nor staticData (<b>Except</b>: {@code dataObject.copyStaticAndFinalModifiers} flag is set)
	 */
	public void setData(DataObject dataObject) throws DataTypeConstraintViolatedException {
		this.type = checkAndRetType(dataObject.type);

		this.txt = dataObject.txt;
		this.byteBuf = dataObject.byteBuf; //ByteBuffer: copy reference only
		this.arr = dataObject.arr; //Array: copy reference only
		this.list = dataObject.list; //List: copy reference only
		this.vp = dataObject.vp; //Var pointer: copy reference only

		//Func pointer: copy reference only
		//Set function name for better debugging experience
		this.fp = (dataObject.fp != null && getVariableName() != null && dataObject.fp.getFunctionName() == null)?
				dataObject.fp.withFunctionName(getVariableName()):dataObject.fp;

		this.sp = dataObject.sp; //Struct: copy reference only
		this.op = dataObject.op; //Object: copy reference only

		this.intValue = dataObject.intValue;
		this.longValue = dataObject.longValue;
		this.floatValue = dataObject.floatValue;
		this.doubleValue = dataObject.doubleValue;
		this.charValue = dataObject.charValue;
		this.error = dataObject.error; //Error: copy reference only
		this.typeValue = dataObject.typeValue;

		if(dataObject.copyStaticAndFinalModifiers) {
			if(dataObject.finalData)
				this.finalData = true;
			if(dataObject.staticData)
				this.staticData = true;
		}
	}

	//Data value methods
	/**
	 * This method <b>ignores</b> the final state of the data object<br>
	 * This method will not change variableName nor finalData
	 */
	private void resetValue() {
		this.txt = null;
		this.byteBuf = null;
		this.arr = null;
		this.list = null;
		this.vp = null;
		this.fp = null;
		this.sp = null;
		this.op = null;
		this.intValue = 0;
		this.longValue = 0;
		this.floatValue = 0;
		this.doubleValue = 0;
		this.charValue = 0;
		this.error = null;
		this.typeValue = null;
	}

	DataObject setArgumentSeparator(String txt) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(txt == null)
			return setNull();

		this.type = checkAndRetType(DataType.ARGUMENT_SEPARATOR);
		resetValue();
		this.txt = txt;

		return this;
	}

	public DataObject setText(String txt) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(txt == null)
			return setNull();

		this.type = checkAndRetType(DataType.TEXT);
		resetValue();
		this.txt = txt;

		return this;
	}

	public String getText() {
		return txt;
	}

	public DataObject setByteBuffer(byte[] byteBuf) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(byteBuf == null)
			return setNull();

		this.type = checkAndRetType(DataType.BYTE_BUFFER);
		resetValue();
		this.byteBuf = byteBuf;

		return this;
	}

	public byte[] getByteBuffer() {
		return byteBuf;
	}

	public DataObject setArray(DataObject[] arr) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(arr == null)
			return setNull();

		this.type = checkAndRetType(DataType.ARRAY);
		resetValue();
		this.arr = arr;

		return this;
	}

	public DataObject[] getArray() {
		return arr;
	}

	public DataObject setList(LinkedList<DataObject> list) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(list == null)
			return setNull();

		this.type = checkAndRetType(DataType.LIST);
		resetValue();
		this.list = list;

		return this;
	}

	public LinkedList<DataObject> getList() {
		return list;
	}

	public DataObject setVarPointer(VarPointerObject vp) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(vp == null)
			return setNull();

		this.type = checkAndRetType(DataType.VAR_POINTER);
		resetValue();
		this.vp = vp;

		return this;
	}

	public VarPointerObject getVarPointer() {
		return vp;
	}

	public DataObject setFunctionPointer(FunctionPointerObject fp) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(fp == null)
			return setNull();

		this.type = checkAndRetType(DataType.FUNCTION_POINTER);
		resetValue();

		this.fp = (getVariableName() != null && fp.getFunctionName() == null)?fp.withFunctionName(getVariableName()):fp;

		return this;
	}

	public FunctionPointerObject getFunctionPointer() {
		return fp;
	}

	public DataObject setStruct(StructObject sp) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(sp == null)
			return setNull();

		this.type = checkAndRetType(DataType.STRUCT);
		resetValue();
		this.sp = sp;

		return this;
	}

	public StructObject getStruct() {
		return sp;
	}

	public DataObject setObject(LangObject op) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(op == null)
			return setNull();

		this.type = checkAndRetType(DataType.OBJECT);
		resetValue();
		this.op = op;

		return this;
	}

	public LangObject getObject() {
		return op;
	}

	public DataObject setNull() throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.NULL);
		resetValue();

		return this;
	}

	public DataObject setVoid() throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.VOID);
		resetValue();

		return this;
	}

	public DataObject setInt(int intValue) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.INT);
		resetValue();
		this.intValue = intValue;

		return this;
	}

	public int getInt() {
		return intValue;
	}

	/**
	 * Sets data to INT = 1 if boolean value is true else INT = 0
	 */
	public DataObject setBoolean(boolean booleanValue) throws DataTypeConstraintViolatedException {
		return setInt(booleanValue?1:0);
	}

	public DataObject setLong(long longValue) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.LONG);
		resetValue();
		this.longValue = longValue;

		return this;
	}

	public long getLong() {
		return longValue;
	}

	public DataObject setFloat(float floatValue) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.FLOAT);
		resetValue();
		this.floatValue = floatValue;

		return this;
	}

	public float getFloat() {
		return floatValue;
	}

	public DataObject setDouble(double doubleValue) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.DOUBLE);
		resetValue();
		this.doubleValue = doubleValue;

		return this;
	}

	public double getDouble() {
		return doubleValue;
	}

	public DataObject setChar(char charValue) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;

		this.type = checkAndRetType(DataType.CHAR);
		resetValue();
		this.charValue = charValue;

		return this;
	}

	public char getChar() {
		return charValue;
	}

	public DataObject setError(ErrorObject error) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(error == null)
			return setNull();

		this.type = checkAndRetType(DataType.ERROR);
		resetValue();
		this.error = error;

		return this;
	}

	public ErrorObject getError() {
		return error;
	}

	public DataObject setTypeValue(DataType typeValue) throws DataTypeConstraintViolatedException {
		if(finalData)
			return this;
		if(typeValue == null)
			return setNull();

		this.type = checkAndRetType(DataType.TYPE);
		resetValue();
		this.typeValue = typeValue;

		return this;
	}

	public DataType getTypeValue() {
		return typeValue;
	}

	//Meta data methods
	public DataObject setVariableName(String variableName) throws DataTypeConstraintViolatedException {
		DataTypeConstraint newTypeRequirement = getTypeConstraintFor(variableName);
		if(!newTypeRequirement.isTypeAllowed(type))
			throw new DataTypeConstraintViolatedException();

		this.typeConstraint = newTypeRequirement;
		this.variableName = variableName;

		return this;
	}

	public String getVariableName() {
		return variableName;
	}

	public DataObject setFinalData(boolean finalData) {
		this.finalData = finalData;

		return this;
	}

	public boolean isFinalData() {
		return finalData;
	}

	public DataObject setStaticData(boolean staticData) {
		this.staticData = staticData;

		return this;
	}

	public boolean isStaticData() {
		return staticData;
	}

	DataObject setCopyStaticAndFinalModifiers(boolean copyStaticAndFinalModifiers) {
		this.copyStaticAndFinalModifiers = copyStaticAndFinalModifiers;

		return this;
	}

	public boolean isCopyStaticAndFinalModifiers() {
		return copyStaticAndFinalModifiers;
	}

	DataObject setLangVar() {
		this.langVar = true;

		return this;
	}

	public boolean isLangVar() {
		return langVar;
	}

	public DataType getType() {
		return type;
	}

	DataObject setTypeConstraint(DataTypeConstraint typeConstraint) throws DataTypeConstraintException {
		for(DataType type:this.typeConstraint.getNotAllowedTypes()) {
			if(typeConstraint.isTypeAllowed(type))
				throw new DataTypeConstraintException("New type constraint must not allow types which were not allowed previously");
		}

		if(!typeConstraint.isTypeAllowed(type))
			throw new DataTypeConstraintViolatedException();

		this.typeConstraint = typeConstraint;

		return this;
	}

	public DataType checkAndRetType(DataType type) throws DataTypeConstraintViolatedException {
		if(!typeConstraint.isTypeAllowed(type))
			throw new DataTypeConstraintViolatedException();

		return type;
	}

	public DataTypeConstraint getTypeConstraint() {
		return typeConstraint;
	}

	private String convertByteBufferToText() {
		StringBuilder builder = new StringBuilder();
		if(byteBuf.length > 0) {
			final String HEX_DIGITS = "0123456789ABCDEF";

			builder.append("0x");
			for(byte b:byteBuf) {
				builder.append(HEX_DIGITS.charAt((b >> 4) & 0xF));
				builder.append(HEX_DIGITS.charAt(b & 0xF));
			}
		}else {
			builder.append("<Empty ByteBuffer>");
		}
		return builder.toString();
	}

	private void convertCompositeElementToText(DataObject ele, StringBuilder builder) {
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
		}else {
			builder.append(ele.toText());
		}
		builder.append(", ");
	}

	private String convertArrayToText() {
		StringBuilder builder = new StringBuilder("[");
		if(arr.length > 0) {
			for(DataObject ele:arr)
				convertCompositeElementToText(ele, builder);
			builder.delete(builder.length() - 2, builder.length());
		}
		builder.append(']');
		return builder.toString();
	}

	private String convertListToText() {
		StringBuilder builder = new StringBuilder("[");
		if(list.size() > 0) {
			for(DataObject ele:list)
				convertCompositeElementToText(ele, builder);
			builder.delete(builder.length() - 2, builder.length());
		}
		builder.append(']');
		return builder.toString();
	}

	private String convertStructToText() {
		StringBuilder builder = new StringBuilder("{");
		String[] memberNames = sp.getMemberNames();
		if(memberNames.length > 0) {
			if(sp.isDefinition()) {
				for(String memberName:memberNames)
					builder.append(memberName).append(", ");
			}else {
				for(String memberName:memberNames) {
					builder.append(memberName).append(": ");
					convertCompositeElementToText(sp.getMember(memberName), builder);
				}
			}

			builder.delete(builder.length() - 2, builder.length());
		}
		builder.append('}');
		return builder.toString();
	}

	//Conversion functions
	public String toText() {
		switch(type) {
			case TEXT:
			case ARGUMENT_SEPARATOR:
				return txt;
			case BYTE_BUFFER:
				return convertByteBufferToText();
			case ARRAY:
				return convertArrayToText();
			case LIST:
				return convertListToText();
			case VAR_POINTER:
				return vp.toString();
			case FUNCTION_POINTER:
				if(variableName != null)
					return variableName;

				return fp.toString();
			case STRUCT:
				return convertStructToText();
			case OBJECT:
				return op.toString();
			case VOID:
				return "";
			case NULL:
				return "null";
			case INT:
				return intValue + "";
			case LONG:
				return longValue + "";
			case FLOAT:
				return floatValue + "";
			case DOUBLE:
				return doubleValue + "";
			case CHAR:
				return charValue + "";
			case ERROR:
				return error.toString();
			case TYPE:
				return typeValue.name();
		}

		return null;
	}
	public byte[] toByteBuffer() {
		switch(type) {
			case BYTE_BUFFER:
				return byteBuf;

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
	public DataObject[] toArray() {
		switch(type) {
			case ARRAY:
				return arr;
			case LIST:
				return list.stream().map(DataObject::new).toArray(len -> new DataObject[len]);
			case STRUCT:
				try {
					return Arrays.stream(sp.getMemberNames()).
							map(memberName -> new DataObject(sp.getMember(memberName))).toArray(DataObject[]::new);
				}catch(DataTypeConstraintException e) {
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
	public LinkedList<DataObject> toList() {
		switch(type) {
			case ARRAY:
				return new LinkedList<DataObject>(Arrays.stream(arr).map(DataObject::new).collect(Collectors.toList()));
			case LIST:
				return list;
			case STRUCT:
				try {
					return new LinkedList<DataObject>(Arrays.asList(Arrays.stream(sp.getMemberNames()).
							map(memberName -> new DataObject(sp.getMember(memberName))).toArray(DataObject[]::new)));
				}catch(DataTypeConstraintException e) {
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

	@Override
	public String toString() {
		return toText();
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj)
			return true;

		if(obj == null)
			return false;

		if(!(obj instanceof DataObject))
			return false;

		try {
			DataObject that = (DataObject)obj;
			return this.type.equals(that.type) && Objects.equals(this.txt, that.txt) && Objects.equals(this.byteBuf, that.byteBuf) &&
					Objects.deepEquals(this.arr, that.arr) && Objects.deepEquals(this.list, that.list) && Objects.equals(this.vp, that.vp) &&
					Objects.equals(this.fp, that.fp) && Objects.equals(this.sp, that.sp) && this.intValue == that.intValue && this.longValue == that.longValue &&
					this.floatValue == that.floatValue && this.doubleValue == that.doubleValue && this.charValue == that.charValue &&
					Objects.equals(this.error, that.error) && this.typeValue == that.typeValue;
		}catch(StackOverflowError e) {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, txt, byteBuf, arr, list == null?null:list.toArray(), vp, fp, sp, intValue, longValue, floatValue, doubleValue, charValue, error, typeValue);
	}

	public static enum DataType {
		TEXT, CHAR, INT, LONG, FLOAT, DOUBLE, BYTE_BUFFER, ARRAY, LIST, VAR_POINTER, FUNCTION_POINTER, STRUCT, OBJECT, ERROR, NULL, VOID, ARGUMENT_SEPARATOR, TYPE;
	}
	public static final class DataTypeConstraint {
		private final Set<DataType> types;
		private final boolean allowed;

		public static DataTypeConstraint fromAllowedTypes(Collection<DataType> allowedTypes) {
			return new DataTypeConstraint(allowedTypes, true);
		}

		public static DataTypeConstraint fromNotAllowedTypes(Collection<DataType> notAllowedTypes) {
			return new DataTypeConstraint(notAllowedTypes, false);
		}

		public static DataTypeConstraint fromSingleAllowedType(DataType allowedType) {
			return new DataTypeConstraint(Arrays.asList(allowedType), true);
		}

		private DataTypeConstraint(Collection<DataType> types, boolean allowed) {
			this.types = new HashSet<>(types);
			this.allowed = allowed;
		}

		public boolean isTypeAllowed(DataType type) {
			return type == null || types.contains(type) == allowed;
		}

		public List<DataType> getAllowedTypes() {
			if(allowed)
				return types.stream().collect(Collectors.toList());

			return Arrays.stream(DataType.values()).filter(((Predicate<DataType>)types::contains).negate()).collect(Collectors.toList());
		}

		public List<DataType> getNotAllowedTypes() {
			if(allowed)
				return Arrays.stream(DataType.values()).filter(((Predicate<DataType>)types::contains).negate()).collect(Collectors.toList());

			return types.stream().collect(Collectors.toList());
		}

		public String toTypeConstraintSyntax() {
			StringBuilder strBuilder = new StringBuilder();
			strBuilder.append("{");

			//Invert "!" if no types are set and print all types
			boolean inverted = !allowed ^ this.types.size() == 0;

			if(inverted)
				strBuilder.append("!");

			Set<DataType> types = new HashSet<>(this.types.size() == 0?Arrays.asList(DataType.values()):this.types);

			if(!inverted && types.contains(DataType.NULL) && types.size() > 1) {
				types.remove(DataType.NULL);

				strBuilder.append("?");
			}

			for(DataType type:types)
				strBuilder.append(type).append("|");

			strBuilder.delete(strBuilder.length() - 1, strBuilder.length());

			strBuilder.append("}");
			return strBuilder.toString();
		}

		@Override
		public String toString() {
			return (allowed?"= ":"! ") + "[" + types.stream().map(DataType::name).collect(Collectors.joining(", ")) + "]";
		}

		public String printAllowedTypes() {
			if(allowed)
				return "[" + types.stream().map(DataType::name).collect(Collectors.joining(", ")) + "]";

			return "[" + Arrays.stream(DataType.values()).filter(((Predicate<DataType>)types::contains).negate()).map(DataType::name).collect(Collectors.joining(", ")) + "]";
		}

		public String printNotAllowedTypes() {
			if(allowed)
				return "[" + Arrays.stream(DataType.values()).filter(((Predicate<DataType>)types::contains).negate()).map(DataType::name).collect(Collectors.joining(", ")) + "]";

			return "[" + types.stream().map(DataType::name).collect(Collectors.joining(", ")) + "]";
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;

			if(obj == null)
				return false;

			if(!(obj instanceof DataTypeConstraint))
				return false;

			DataTypeConstraint that = (DataTypeConstraint)obj;
			return Objects.deepEquals(new HashSet<>(this.getAllowedTypes()), new HashSet<>(that.getAllowedTypes()));
		}

		@Override
		public int hashCode() {
			return Objects.hash(new HashSet<>(getAllowedTypes()));
		}
	}
	public static final class FunctionPointerObject {
		/**
		 * Normal function pointer
		 */
		public static final int NORMAL = 0;
		/**
		 * Pointer to a native function
		 */
		public static final int NATIVE = 1;

		/**
		 * If langPath is set, the Lang path from the stack frame element which is created for the function call will be overridden
		 */
		private final String langPath;
		/**
		 * If langFile or langPath is set, the Lang file from the stack frame element which is created for the function call will be overridden<br>
		 * This behavior allows for keeping the "&lt;shell&gt;" special case - when the Lang file is null - if a function within a stack frame element where the Lang file is null is
		 * called from within a stack frame element where Lang file is not null.
		 */
		private final String langFile;
		/**
		 * For methods "this" will be set to the object the method was called on
		 */
		private final LangObject thisObject;
		/**
		 * For methods the super level of the class [= level of parent]
		 */
		private final int superLevel;
		/**
		 * If functionName is set, the function name from the stack frame element which is created for the function call will be overridden
		 */
		private final String functionName;
		private final LangNormalFunction normalFunction;
		private final LangNativeFunction nativeFunction;
		private final int functionPointerType;

		/**
		 * For normal and native function pointer definition
		 * Used for setting superLevel for objects
		 */
		public FunctionPointerObject(FunctionPointerObject func, int superLevel) throws DataTypeConstraintException {
			this.langPath = func.langPath;
			this.langFile = func.langFile;
			this.thisObject = func.thisObject;
			this.superLevel = superLevel;
			this.functionName = func.functionName;
			this.normalFunction = func.normalFunction;
			this.nativeFunction = func.nativeFunction;
			this.functionPointerType = func.functionPointerType;
		}

		/**
		 * For normal and native function pointer definition
		 * Used for binding the this-object
		 */
		public FunctionPointerObject(FunctionPointerObject func, LangObject thisObject) throws DataTypeConstraintException {
			if(thisObject.isClass())
				throw new DataTypeConstraintException("The this-object must be an object");

			this.langPath = func.langPath;
			this.langFile = func.langFile;
			this.thisObject = thisObject;
			this.superLevel = func.superLevel;
			this.functionName = func.functionName;
			this.normalFunction = func.normalFunction;
			this.nativeFunction = func.nativeFunction;
			this.functionPointerType = func.functionPointerType;
		}

		/**
		 * For normal and native function pointer definition
		 * Used for binding the this-object
		 */
		public FunctionPointerObject(FunctionPointerObject func, LangObject thisObject, int superLevel) throws DataTypeConstraintException {
			if(thisObject.isClass())
				throw new DataTypeConstraintException("The this-object must be an object");

			this.langPath = func.langPath;
			this.langFile = func.langFile;
			this.thisObject = thisObject;
			this.superLevel = superLevel;
			this.functionName = func.functionName;
			this.normalFunction = func.normalFunction;
			this.nativeFunction = func.nativeFunction;
			this.functionPointerType = func.functionPointerType;
		}

		/**
		 * For normal function pointer definition
		 */
		public FunctionPointerObject(String langPath, String langFile, String functionName, LangNormalFunction normalFunction) {
			this.langPath = langPath;
			this.langFile = langFile;
			this.thisObject = null;
			this.superLevel = -1;
			this.functionName = functionName;
			this.normalFunction = normalFunction;
			this.nativeFunction = null;
			this.functionPointerType = NORMAL;
		}
		/**
		 * For normal function pointer definition
		 */
		public FunctionPointerObject(String langPath, String langFile, LangNormalFunction normalFunction) {
			this(langPath, langFile, null, normalFunction);
		}
		/**
		 * For normal function pointer definition
		 */
		public FunctionPointerObject(String functionName, LangNormalFunction normalFunction) {
			this(null, null, functionName, normalFunction);
		}
		/**
		 * For normal function pointer definition
		 */
		public FunctionPointerObject(LangNormalFunction normalFunction) {
			this(null, normalFunction);
		}

		/**
		 * For pointer to native function/linker function
		 */
		public FunctionPointerObject(String langPath, String langFile, String functionName, LangNativeFunction nativeFunction) {
			this.langPath = langPath;
			this.langFile = langFile;
			this.thisObject = null;
			this.superLevel = -1;
			this.functionName = functionName == null?nativeFunction.getFunctionName():functionName;
			this.normalFunction = null;
			this.nativeFunction = nativeFunction;
			this.functionPointerType = NATIVE;
		}
		/**
		 * For pointer to native function/linker function
		 */
		public FunctionPointerObject(String langPath, String langFile, LangNativeFunction nativeFunction) {
			this(langPath, langFile, nativeFunction.getFunctionName(), nativeFunction);
		}
		/**
		 * For pointer to native function/linker function
		 */
		public FunctionPointerObject(String functionName, LangNativeFunction nativeFunction) {
			this(null, null, functionName, nativeFunction);
		}
		/**
		 * For pointer to native function/linker function
		 */
		public FunctionPointerObject(LangNativeFunction nativeFunction) {
			this(nativeFunction.getFunctionName(), nativeFunction);
		}

		public FunctionPointerObject withFunctionName(String functionName) {
			switch(functionPointerType) {
				case NORMAL:
					return new FunctionPointerObject(langPath, langFile, functionName, normalFunction);
				case NATIVE:
					return new FunctionPointerObject(langPath, langFile, functionName, nativeFunction);
			}

			return null;
		}

		public String getLangPath() {
			return langPath;
		}

		public String getLangFile() {
			return langFile;
		}

		public int getSuperLevel() {
			return superLevel;
		}

		public LangObject getThisObject() {
			return thisObject;
		}

		public String getFunctionName() {
			return functionName;
		}

		public LangNormalFunction getNormalFunction() {
			return normalFunction;
		}

		public LangNativeFunction getNativeFunction() {
			return nativeFunction;
		}

		public int getFunctionPointerType() {
			return functionPointerType;
		}

		@Override
		public String toString() {
			if(functionName != null)
				return functionName;

			switch(functionPointerType) {
				case NORMAL:
					return "<Normal " + (thisObject == null?"Function":"Method") + ">";
				case NATIVE:
					return "<Native " + (thisObject == null?"Function":"Method") + ">";
				default:
					return "Error";
			}
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;

			if(obj == null)
				return false;

			if(!(obj instanceof FunctionPointerObject))
				return false;

			FunctionPointerObject that = (FunctionPointerObject)obj;
			return this.functionPointerType == that.functionPointerType && Objects.equals(this.thisObject, that.thisObject) &&
					Objects.equals(this.superLevel, that.superLevel) && Objects.equals(this.normalFunction, that.normalFunction) &&
					Objects.equals(this.nativeFunction, that.nativeFunction);
		}

		@Override
		public int hashCode() {
			return Objects.hash(functionPointerType, thisObject, superLevel, normalFunction, nativeFunction);
		}
	}
	public static final class VarPointerObject {
		private final DataObject var;

		public VarPointerObject(DataObject var) {
			this.var = var;
		}

		public DataObject getVar() {
			return var;
		}

		@Override
		public String toString() {
			if(var.getType() == DataType.VAR_POINTER)
				return "-->{-->{...}}";

			return "-->{" + var.toText() + "}";
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;

			if(obj == null)
				return false;

			if(!(obj instanceof VarPointerObject))
				return false;

			VarPointerObject that = (VarPointerObject)obj;
			return this.var.equals(that.var);
		}

		@Override
		public int hashCode() {
			return Objects.hash(var);
		}
	}
	public static final class StructObject {
		private final String[] memberNames;
		private final DataTypeConstraint[] typeConstraints;
		private final DataObject[] members;
		/**
		 * If null: This is the struct definition<br>
		 * If not null: This is an instance of structBaseDefinition<br>
		 * This is also been used for instance of checks
		 */
		private final StructObject structBaseDefinition;

		public StructObject(String[] memberNames) throws DataTypeConstraintException {
			this(memberNames, null);
		}

		public StructObject(String[] memberNames, DataTypeConstraint[] typeConstraints) throws DataTypeConstraintException {
			this.memberNames = Arrays.copyOf(memberNames, memberNames.length);
			this.typeConstraints = typeConstraints == null?new DataTypeConstraint[this.memberNames.length]:
					Arrays.copyOf(typeConstraints, typeConstraints.length);

			if(this.memberNames.length != this.typeConstraints.length)
				throw new DataTypeConstraintException("The count of members must be equals to the count of type constraints");

			this.members = null;
			this.structBaseDefinition = null;
		}

		public StructObject(StructObject structBaseDefinition) throws DataTypeConstraintException {
			this(structBaseDefinition, null);
		}

		public StructObject(StructObject structBaseDefinition, DataObject[] values) throws DataTypeConstraintException {
			if(!structBaseDefinition.isDefinition())
				throw new DataTypeConstraintException("No instance can be created of another struct instance");

			//Must be set first for isDefinition checks
			this.structBaseDefinition = structBaseDefinition;

			this.memberNames = Arrays.copyOf(structBaseDefinition.memberNames, structBaseDefinition.memberNames.length);
			this.typeConstraints = Arrays.copyOf(structBaseDefinition.typeConstraints, structBaseDefinition.typeConstraints.length);
			this.members = new DataObject[this.memberNames.length];

			if(values != null && this.memberNames.length != values.length)
				throw new DataTypeConstraintException("The count of members must be equals to the count of values");

			for(int i = 0;i < this.members.length;i++) {
				this.members[i] = new DataObject().setVariableName(this.memberNames[i]);

				if(values != null && values[i] != null)
					this.members[i].setData(values[i]);

				if(this.typeConstraints[i] != null)
					this.members[i].setTypeConstraint(this.typeConstraints[i]);
			}
		}

		public boolean isDefinition() {
			return structBaseDefinition == null;
		}

		public String[] getMemberNames() {
			return Arrays.copyOf(memberNames, memberNames.length);
		}

		public DataTypeConstraint[] getTypeConstraints() {
			return Arrays.copyOf(typeConstraints, typeConstraints.length);
		}

		public StructObject getStructBaseDefinition() {
			return structBaseDefinition;
		}

		/**
		 * @return Will -1 null, if the member was not found
		 */
		public int getIndexOfMember(String memeberName) {
			for(int i = 0;i < memberNames.length;i++)
				if(memberNames[i].equals(memeberName))
					return i;

			return -1;
		}

		public DataTypeConstraint getTypeConstraint(String memberName) throws DataTypeConstraintException {
			int index = getIndexOfMember(memberName);
			if(index == -1)
				throw new DataTypeConstraintException("The member \"" + memberName + "\" is not part of this struct");

			return typeConstraints[index];
		}

		public DataObject getMember(String memberName) throws DataTypeConstraintException {
			if(isDefinition())
				throw new DataTypeConstraintException("The struct definition is no struct instance and has no member values");

			int index = getIndexOfMember(memberName);
			if(index == -1)
				throw new DataTypeConstraintException("The member \"" + memberName + "\" is not part of this struct");

			return members[index];
		}

		public void setMember(String memberName, DataObject dataObject) throws DataTypeConstraintException {
			if(isDefinition())
				throw new DataTypeConstraintException("The struct definition is no struct instance and has no member values");

			int index = getIndexOfMember(memberName);
			if(index == -1)
				throw new DataTypeConstraintException("The member \"" + memberName + "\" is not part of this struct");

			members[index].setData(dataObject);
		}

		@Override
		public String toString() {
			return structBaseDefinition == null?"<Struct[Definition]>":"<Struct[Instance]>";
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;

			if(obj == null)
				return false;

			if(!(obj instanceof StructObject))
				return false;

			StructObject that = (StructObject)obj;
			return Objects.deepEquals(this.memberNames, that.memberNames) && Objects.deepEquals(this.members, that.members) &&
					Objects.deepEquals(this.typeConstraints, that.typeConstraints) && Objects.equals(this.structBaseDefinition, that.structBaseDefinition);
		}

		@Override
		public int hashCode() {
			return Objects.hash(memberNames, members, typeConstraints, structBaseDefinition);
		}
	}
	public static final class LangObject {
		public static final LangObject OBJECT_CLASS;
		static {
			Map<String, FunctionPointerObject[]> methods = new HashMap<>();
			Map<String, Boolean[]> methodOverrideFlags = new HashMap<>();
			methods.put("mp.getClass", new FunctionPointerObject[] {
					new FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
						@LangFunction(value="mp.getClass", isMethod=true)
						@LangFunction.AllowedTypes(DataType.OBJECT)
						@SuppressWarnings("unused")
						public DataObject getClassMethod(
								LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
						) {
							return new DataObject().setObject(thisObject.getClassBaseDefinition());
						}
					}))
			});
			methodOverrideFlags.put("mp.getClass", new Boolean[] {
					false
			});

			FunctionPointerObject[] constructors = new FunctionPointerObject[] {
					new FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
						@LangFunction(value="construct", isMethod=true)
						@LangFunction.AllowedTypes(DataType.VOID)
						@SuppressWarnings("unused")
						public DataObject defaultConstructMethod(
								LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
						) {
							return null;
						}
					}))
			};

			OBJECT_CLASS = new LangObject(true, new DataObject[0], new String[0], new DataTypeConstraint[0],
					new boolean[0], methods, methodOverrideFlags, constructors, null);
		}

		private int superLevel = 0;

		private final DataObject[] staticMembers;
		private final String[] memberNames;
		private final DataTypeConstraint[] memberTypeConstraints;
		private final boolean[] memberFinalFlags;
		private final DataObject[] members;
		private final Map<String, FunctionPointerObject[]> methods;
		private final FunctionPointerObject[] constructors;
		/**
		 * If size = 0: This is the base object<br>
		 * If size > 0: This is a normal object
		 */
		private final LangObject[] parentClasses;
		/**
		 * If null: This is a class<br>
		 * If not null: This is an object (= instance of structClassDefinition)<br>
		 * This is also been used for instance of checks
		 */
		private final LangObject classBaseDefinition;
		/**
		 * false for classes and uninitialized objects, true for objects where post construct was called
		 */
		private boolean initialized = false;

		public LangObject(DataObject[] staticMembers, String[] memberNames, DataTypeConstraint[] memberTypeConstraints,
						  boolean[] memberFinalFlags, Map<String, FunctionPointerObject[]> methods,
						  Map<String, Boolean[]> methodOverrideFlags, FunctionPointerObject[] constructors,
						  LangObject[] parentClasses) throws DataTypeConstraintException {
			this(false, staticMembers, memberNames, memberTypeConstraints, memberFinalFlags, methods, methodOverrideFlags,
					constructors, parentClasses);
		}

		private LangObject(boolean isBaseObject, DataObject[] staticMembers, String[] memberNames,
						   DataTypeConstraint[] memberTypeConstraints, boolean[] memberFinalFlags,
						   Map<String, FunctionPointerObject[]> methods, Map<String, Boolean[]> methodOverrideFlags,
						   FunctionPointerObject[] constructors, LangObject[] parentClasses) throws DataTypeConstraintException {
			if(isBaseObject) {
				this.parentClasses = parentClasses = new LangObject[0];
			}else if(parentClasses == null || parentClasses.length == 0) {
				this.parentClasses = parentClasses = new LangObject[] {
						OBJECT_CLASS
				};
			}else {
				for(LangObject parentClass:parentClasses)
					if(!parentClass.isClass())
						throw new DataTypeConstraintException("Parent classes must not be an object");

				if(parentClasses.length == 1) {
					this.parentClasses = Arrays.copyOf(parentClasses, parentClasses.length);
				}else {
					throw new DataTypeConstraintException("Multi-inheritance is not allowed");
				}
			}

			for(DataObject staticMember:staticMembers) {
				String staticMemberName = staticMember.getVariableName();

				if(Arrays.stream(parentClasses).map(LangObject::getStaticMembers).
						anyMatch(superStaticMembers -> Arrays.stream(superStaticMembers).map(DataObject::getVariableName).
								anyMatch(superStaticMemberName -> superStaticMemberName.equals(staticMemberName))))
					throw new DataTypeConstraintException("Super class static member must not be shadowed" +
							" (For static member \"" + staticMemberName + "\")");
			}

			//TODO allow multi-inheritance (Check if a static member is in both super classes)
			this.staticMembers = Arrays.copyOf(staticMembers, staticMembers.length + Arrays.stream(parentClasses).
					mapToInt(parentClass -> parentClass.getStaticMembers().length).sum());
			{
				int staticMemberIndex = staticMembers.length;
				for(LangObject parentClass:parentClasses) {
					for(DataObject superStaticMember:parentClass.getStaticMembers()) {
						//No copies (static members must be the same reference across all uses)
						this.staticMembers[staticMemberIndex] = superStaticMember;

						staticMemberIndex++;
					}
				}
			}

			if(memberNames.length != memberTypeConstraints.length)
				throw new DataTypeConstraintException("The count of members must be equals to the count of member type constraints");

			if(memberNames.length != memberFinalFlags.length)
				throw new DataTypeConstraintException("The count of members must be equals to the count of member final flags");

			for(String memberName:memberNames) {
				if(Arrays.stream(parentClasses).map(LangObject::getStaticMembers).
						anyMatch(superStaticMembers -> Arrays.stream(superStaticMembers).map(DataObject::getVariableName).
								anyMatch(superStaticMemberName -> superStaticMemberName.equals(memberName))))
					throw new DataTypeConstraintException("Static members of a super class must not be shadowed" +
							" (For member \"" + memberName + "\")");

				if(Arrays.stream(parentClasses).map(LangObject::getMemberNames).
						anyMatch(members -> Arrays.asList(members).contains(memberName)))
					throw new DataTypeConstraintException("Super class member must not be shadowed" +
							" (For member \"" + memberName + "\")");

				if(Arrays.stream(staticMembers).map(DataObject::getVariableName).
						anyMatch(staticMemberName -> staticMemberName.equals(memberName)))
					throw new DataTypeConstraintException("Static members must not be shadowed" +
							" (For member \"" + memberName + "\")");
			}

			//TODO allow multi-inheritance (Check if a member is in both super classes)
			int superClassMemberCount = Arrays.stream(parentClasses).
					mapToInt(parentClass -> parentClass.getMemberNames().length).sum();
			this.memberNames = Arrays.copyOf(memberNames, memberNames.length + superClassMemberCount);
			this.memberTypeConstraints = Arrays.copyOf(memberTypeConstraints, memberTypeConstraints.length + superClassMemberCount);
			this.memberFinalFlags = Arrays.copyOf(memberFinalFlags, memberFinalFlags.length + superClassMemberCount);
			{
				int memberIndex = memberNames.length;
				for(LangObject parentClass:parentClasses) {
					for(int i = 0;i < parentClass.getMemberNames().length;i++) {
						this.memberNames[memberIndex] = parentClass.getMemberNames()[i];
						this.memberTypeConstraints[memberIndex] = parentClass.getMemberTypeConstraints()[i];
						this.memberFinalFlags[memberIndex] = parentClass.getMemberFinalFlags()[i];

						memberIndex++;
					}
				}
			}

			Map<String, List<FunctionPointerObject>> rawMethods = new HashMap<>();
			methods.forEach((k, v) -> rawMethods.put(k, new LinkedList<>(Arrays.stream(v).
					map(fp -> new FunctionPointerObject(fp, 0)).
					collect(Collectors.toList()))));
			List<String> methodNames = new ArrayList<>(methods.keySet());
			for(String methodName:methodNames) {
				FunctionPointerObject[] overloadedMethods = methods.get(methodName);
				Boolean[] overloadedMethodOverrideFlags = methodOverrideFlags.get(methodName);

				if(overloadedMethodOverrideFlags == null || overloadedMethods.length != overloadedMethodOverrideFlags.length)
					throw new DataTypeConstraintException("Invalid override flag values for \"" + methodName + "\"");
				if(overloadedMethods.length == 0)
					throw new DataTypeConstraintException("No method present for method \"" + methodName + "\"");

				List<LangBaseFunction> functionSignatures = new LinkedList<>();
				for(FunctionPointerObject overloadedMethod:overloadedMethods) {
					if(overloadedMethod.getFunctionPointerType() == DataObject.FunctionPointerObject.NORMAL) {
						functionSignatures.add(overloadedMethod.getNormalFunction());
					}else if(overloadedMethod.getFunctionPointerType() == DataObject.FunctionPointerObject.NATIVE) {
						List<LangNativeFunction.InternalFunction> internalFunctions = overloadedMethod.getNativeFunction().getInternalFunctions();
						functionSignatures.addAll(internalFunctions);
					}else {
						throw new DataTypeConstraintException("Invalid function type \"" + overloadedMethod.getFunctionPointerType() + "\"");
					}
				}

				for(int i = 0;i < functionSignatures.size();i++) {
					for(int j = 0;j < functionSignatures.size();j++) {
						//Do not compare to same function signature
						if(i == j)
							continue;

						if(LangUtils.areFunctionSignaturesEquals(functionSignatures.get(i), functionSignatures.get(j)))
							throw new DataTypeConstraintException("Duplicated function signatures: " +
									"\"" + methodName + functionSignatures.get(i).toFunctionSignatureSyntax() + "\" and \"" +
									methodName + functionSignatures.get(j).toFunctionSignatureSyntax() + "\"");
					}
				}
			}

			for(String methodName:methods.keySet()) {
				String functionVarName = "fp." + methodName.substring(3);

				if(Arrays.stream(parentClasses).map(LangObject::getStaticMembers).
						anyMatch(superStaticMembers -> Arrays.stream(superStaticMembers).map(DataObject::getVariableName).
								anyMatch(superStaticMemberName -> superStaticMemberName.equals(functionVarName))))
					throw new DataTypeConstraintException("\"fp.\" static members of a super class must not be shadowed by method " +
							" (For method \"" + methodName + "\" and member \"" + functionVarName + "\")");

				if(Arrays.stream(parentClasses).map(LangObject::getMemberNames).
						anyMatch(members -> Arrays.asList(members).contains(functionVarName)))
					throw new DataTypeConstraintException("\"fp.\" members of a super class not be shadowed by method " +
							" (For method \"" + methodName + "\" and member \"" + functionVarName + "\")");

				if(Arrays.stream(staticMembers).map(DataObject::getVariableName).
						anyMatch(staticMemberName -> staticMemberName.equals(functionVarName)))
					throw new DataTypeConstraintException("\"fp.\" static members must not be shadowed by method " +
							" (For method \"" + methodName + "\" and member \"" + functionVarName + "\")");

				if(Arrays.asList(memberNames).contains(functionVarName))
					throw new DataTypeConstraintException("\"fp.\" members must not be shadowed by method " +
							" (For method \"" + methodName + "\" and member \"" + functionVarName + "\")");
			}

			//TODO allow multi-inheritance (Check if a method with the same function signature is in both super classes)
			{
				for(LangObject parentClass:parentClasses) {
					parentClass.getMethods().forEach((k, v) -> {
						List<FunctionPointerObject> overloadedMethods = rawMethods.get(k);
						if(overloadedMethods == null) {
							rawMethods.put(k, new LinkedList<>(Arrays.stream(v).
									map(fp -> new FunctionPointerObject(fp, fp.getSuperLevel() + 1)).
									collect(Collectors.toList())));

							return;
						}

						//Override check
						Boolean[] overloadedMethodOverrideFlags = methodOverrideFlags.get(k);
						List<LangBaseFunction> functionSignatures = new LinkedList<>();
						List<Boolean> overrideFlags = new LinkedList<>();
						for(int i = 0;i < overloadedMethods.size();i++) {
							FunctionPointerObject overloadedMethod = overloadedMethods.get(i);

							if(overloadedMethod.getFunctionPointerType() == DataObject.FunctionPointerObject.NORMAL) {
								functionSignatures.add(overloadedMethod.getNormalFunction());
								overrideFlags.add(overloadedMethodOverrideFlags[i]);
							}else if(overloadedMethod.getFunctionPointerType() == DataObject.FunctionPointerObject.NATIVE) {
								List<LangNativeFunction.InternalFunction> internalFunctions = overloadedMethod.getNativeFunction().getInternalFunctions();
								functionSignatures.addAll(internalFunctions);
								for(int j = 0;j < internalFunctions.size();j++)
									overrideFlags.add(overloadedMethodOverrideFlags[i]);
							}else {
								throw new DataTypeConstraintException("Invalid function type \"" + overloadedMethod.getFunctionPointerType() + "\"");
							}
						}

						List<LangBaseFunction> superFunctionSignatures = new LinkedList<>();
						for(int i = 0;i < parentClass.methods.get(k).length;i++) {
							FunctionPointerObject overloadedMethod = parentClass.methods.get(k)[i];

							if(overloadedMethod.getFunctionPointerType() == DataObject.FunctionPointerObject.NORMAL) {
								superFunctionSignatures.add(overloadedMethod.getNormalFunction());
							}else if(overloadedMethod.getFunctionPointerType() == DataObject.FunctionPointerObject.NATIVE) {
								List<LangNativeFunction.InternalFunction> internalFunctions = overloadedMethod.getNativeFunction().getInternalFunctions();
								superFunctionSignatures.addAll(internalFunctions);
							}else {
								throw new DataTypeConstraintException("Invalid function type \"" + overloadedMethod.getFunctionPointerType() + "\"");
							}
						}

						for(int i = 0;i < functionSignatures.size();i++) {
							boolean isAnyEquals = false;
                            for(LangBaseFunction superFunctionSignature:superFunctionSignatures) {
                                if(LangUtils.areFunctionSignaturesEquals(functionSignatures.get(i), superFunctionSignature)) {
                                    isAnyEquals = true;
                                    break;
                                }
                            }

							if(overrideFlags.get(i)) {
								if(!isAnyEquals)
									throw new DataTypeConstraintException("No method for override was found for function signature: " +
											"\"" + k + functionSignatures.get(i).toFunctionSignatureSyntax());
							}else {
								if(isAnyEquals)
									throw new DataTypeConstraintException("Method was not declared as override for function signature: " +
											"\"" + k + functionSignatures.get(i).toFunctionSignatureSyntax());
							}
						}

                        for(FunctionPointerObject method:v)
                            overloadedMethods.add(new FunctionPointerObject(method, method.getSuperLevel() + 1));
					});
				}
			}

			this.methods = new HashMap<>();
			rawMethods.forEach((k, v) -> this.methods.put(k, v.toArray(new FunctionPointerObject[0])));

			this.constructors = new FunctionPointerObject[constructors.length];
			for(int i = 0;i < constructors.length;i++)
				this.constructors[i] = new FunctionPointerObject(constructors[i], 0);
			if(this.constructors.length < 1)
				throw new DataTypeConstraintException("There must be at least one constructor");

			{
				List<LangBaseFunction> functionSignatures = new LinkedList<>();
				for(FunctionPointerObject constructor:constructors) {
					if(constructor.getFunctionPointerType() == DataObject.FunctionPointerObject.NORMAL) {
						functionSignatures.add(constructor.getNormalFunction());
					}else if(constructor.getFunctionPointerType() == DataObject.FunctionPointerObject.NATIVE) {
						List<LangNativeFunction.InternalFunction> internalFunctions = constructor.getNativeFunction().getInternalFunctions();
						functionSignatures.addAll(internalFunctions);
					}else {
						throw new DataTypeConstraintException("Invalid function type \"" + constructor.getFunctionPointerType() + "\"");
					}
				}

				for(int i = 0;i < functionSignatures.size();i++) {
					for(int j = 0;j < functionSignatures.size();j++) {
						//Do not compare to same function signature
						if(i == j)
							continue;

						if(LangUtils.areFunctionSignaturesEquals(functionSignatures.get(i), functionSignatures.get(j)))
							throw new DataTypeConstraintException("Duplicated function signatures: " +
									"\"construct" + functionSignatures.get(i).toFunctionSignatureSyntax() +
									"\" and \"construct" + functionSignatures.get(j).toFunctionSignatureSyntax() + "\"");
					}
				}
			}

			//TODO allow super constructor call from constructor [Dynamically bind super constructors to this]

			this.members = null;
			this.classBaseDefinition = null;
		}

		/**
		 * The constructor must be called separately, afterward postConstructor must be called
		 */
		public LangObject(LangObject classBaseDefinition) throws DataTypeConstraintException {
			//Must be set first for isClass checks
			this.classBaseDefinition = classBaseDefinition;

			//No copies, because static members should be the same across all objects
			this.staticMembers = Arrays.copyOf(classBaseDefinition.staticMembers, classBaseDefinition.staticMembers.length);

			this.memberNames = Arrays.copyOf(classBaseDefinition.memberNames, classBaseDefinition.memberNames.length);
			this.memberTypeConstraints = Arrays.copyOf(classBaseDefinition.memberTypeConstraints, classBaseDefinition.memberTypeConstraints.length);
			this.memberFinalFlags = Arrays.copyOf(classBaseDefinition.memberFinalFlags, classBaseDefinition.memberFinalFlags.length);

			this.members = new DataObject[classBaseDefinition.memberNames.length];
			for(int i = 0;i < members.length;i++)
				this.members[i] = new DataObject().setNull().setVariableName(memberNames[i]);

			this.methods = new HashMap<>(classBaseDefinition.methods);
			this.methods.replaceAll((k, v) -> {
				FunctionPointerObject[] arrayCopy = Arrays.copyOf(v, v.length);
				for(int i = 0;i < arrayCopy.length;i++)
					arrayCopy[i] = new FunctionPointerObject(arrayCopy[i], this);

				return arrayCopy;
			});

			this.constructors = Arrays.copyOf(classBaseDefinition.constructors, classBaseDefinition.constructors.length);
			for(int i = 0;i < this.constructors.length;i++)
				this.constructors[i] = new FunctionPointerObject(this.constructors[i], this);

			this.parentClasses = Arrays.copyOf(classBaseDefinition.parentClasses, classBaseDefinition.parentClasses.length);
		}

		public int getSuperLevel() {
			if(isClass())
				throw new DataTypeConstraintException("Super level can only be queried on objects");

			return superLevel;
		}

		public void setSuperLevel(int superLevel) {
			if(isClass())
				throw new DataTypeConstraintException("Super level can only be modified on objects");

			this.superLevel = superLevel;
		}

		public void postConstructor() throws DataTypeConstraintException {
			if(isClass())
				throw new DataTypeConstraintException("Post construct must be called on an object");

			if(isInitialized())
				throw new DataTypeConstraintException("Object is already initialized");

			for(int i = 0;i < members.length;i++) {
				if(memberTypeConstraints[i] != null)
					this.members[i].setTypeConstraint(memberTypeConstraints[i]);

				if(memberFinalFlags[i])
					this.members[i].setFinalData(true);
			}

			initialized = true;
		}

		public boolean isInitialized() {
			return initialized;
		}

		public boolean isClass() {
			return classBaseDefinition == null;
		}

		public DataObject[] getStaticMembers() {
			return Arrays.copyOf(staticMembers, staticMembers.length);
		}

		/**
		 * @return Will return -1, if the member was not found
		 */
		public int getIndexOfStaticMember(String memeberName) {
			for(int i = 0;i < staticMembers.length;i++)
				if(staticMembers[i].getVariableName().equals(memeberName))
					return i;

			return -1;
		}

		public DataObject getStaticMember(String memberName) throws DataTypeConstraintException {
			int index = getIndexOfStaticMember(memberName);
			if(index == -1)
				throw new DataTypeConstraintException("The static member \"" + memberName + "\" is not part of this class or object");

			return staticMembers[index];
		}

		public String[] getMemberNames() {
			return Arrays.copyOf(memberNames, memberNames.length);
		}

		public DataTypeConstraint[] getMemberTypeConstraints() {
			return Arrays.copyOf(memberTypeConstraints, memberTypeConstraints.length);
		}

		public boolean[] getMemberFinalFlags() {
			return Arrays.copyOf(memberFinalFlags, memberFinalFlags.length);
		}

		/**
		 * @return Will return -1, if the member was not found
		 */
		public int getIndexOfMember(String memeberName) {
			for(int i = 0;i < members.length;i++)
				if(members[i].getVariableName().equals(memeberName))
					return i;

			return -1;
		}

		public DataObject getMember(String memberName) throws DataTypeConstraintException {
			if(isClass())
				throw new DataTypeConstraintException("The class is no object and has no member values");

			int index = getIndexOfMember(memberName);
			if(index == -1)
				throw new DataTypeConstraintException("The member \"" + memberName + "\" is not part of this object");

			return members[index];
		}

		public Map<String, FunctionPointerObject[]> getMethods() {
			return new HashMap<>(methods);
		}

		private Map<String, List<FunctionPointerObject>> getRawSuperMethods(int superLevel) {
			Map<String, List<FunctionPointerObject>> rawSuperMethods = new HashMap<>();
			for(LangObject parentClass:parentClasses) {
				if(superLevel > 0) {
					Map<String, List<FunctionPointerObject>> superRawSuperMethods = parentClass.
							getRawSuperMethods(superLevel - 1);
					superRawSuperMethods.forEach((k, v) -> {
						if(!rawSuperMethods.containsKey(k))
							rawSuperMethods.put(k, new LinkedList<>());

						rawSuperMethods.get(k).addAll(v);
					});
				}else {
					parentClass.getMethods().forEach((k, v) -> {
						if(!rawSuperMethods.containsKey(k))
							rawSuperMethods.put(k, new LinkedList<>());

						rawSuperMethods.get(k).addAll(Arrays.asList(v));
					});
				}
			}

			return rawSuperMethods;
		}

		public Map<String, FunctionPointerObject[]> getSuperMethods() {
			Map<String, List<FunctionPointerObject>> rawSuperMethods = getRawSuperMethods(this.superLevel);

			Map<String, FunctionPointerObject[]> superMethods = new HashMap<>();
			rawSuperMethods.forEach((k, v) -> superMethods.put(k, v.toArray(new FunctionPointerObject[0])));

			return new HashMap<>(superMethods);
		}

		public FunctionPointerObject[] getConstructors() {
			return Arrays.copyOf(constructors, constructors.length);
		}

		/**
		 * @return Returns all constructors for the current super level without going to super
		 * [0 is current, 1 is parent, 2 is grandparent]
		 */
		public FunctionPointerObject[] getConstructorsForCurrentSuperLevel() {
			if(superLevel == 0)
				return getConstructors();

			List<FunctionPointerObject> rawSuperMethods = getRawSuperConstructors(this.superLevel - 1);

			return rawSuperMethods.toArray(new FunctionPointerObject[0]);
		}

		private List<FunctionPointerObject> getRawSuperConstructors(int superLevel) {
			List<FunctionPointerObject> rawSuperConstructors = new LinkedList<>();
			for(LangObject parentClass:parentClasses) {
				if(superLevel > 0) {
					List<FunctionPointerObject> superRawSuperConstructors = parentClass.
							getRawSuperConstructors(superLevel - 1);
					rawSuperConstructors.addAll(superRawSuperConstructors);
				}else {
					rawSuperConstructors.addAll(Arrays.asList(parentClass.getConstructors()));
				}
			}

			return rawSuperConstructors;
		}

		public FunctionPointerObject[] getSuperConstructors() {
			List<FunctionPointerObject> rawSuperMethods = getRawSuperConstructors(this.superLevel);

			return rawSuperMethods.toArray(new FunctionPointerObject[0]);
		}

		public LangObject[] getParentClasses() {
			return Arrays.copyOf(parentClasses, parentClasses.length);
		}

		public LangObject getClassBaseDefinition() {
			return classBaseDefinition;
		}

		public boolean isInstanceOf(LangObject classObject) throws DataTypeConstraintException {
			if(!classObject.isClass())
				return false;

			if(!isClass())
				return classBaseDefinition.isInstanceOf(classObject);

			if(this.equals(classObject))
				return true;

			for(LangObject parentClass:parentClasses)
				if(parentClass.isInstanceOf(classObject))
					return true;

			return false;
		}

		@Override
		public String toString() {
			return classBaseDefinition == null?"<Class>":"<Object>";
		}

		//TODO equals and hashCode
	}
	public static final class ErrorObject {
		private final InterpretingError err;
		private final String message;

		public ErrorObject(InterpretingError err, String message) {
			if(err == null)
				this.err = InterpretingError.NO_ERROR;
			else
				this.err = err;

			this.message = message;
		}
		public ErrorObject(InterpretingError err) {
			this(err, null);
		}

		public InterpretingError getInterprettingError() {
			return err;
		}

		public int getErrno() {
			return err.getErrorCode();
		}

		public String getErrtxt() {
			return err.getErrorText();
		}

		public String getMessage() {
			return message;
		}

		@Override
		public String toString() {
			return "Error";
		}

		@Override
		public boolean equals(Object obj) {
			if(this == obj)
				return true;

			if(obj == null)
				return false;

			if(!(obj instanceof ErrorObject))
				return false;

			ErrorObject that = (ErrorObject)obj;
			return this.err == that.err;
		}

		@Override
		public int hashCode() {
			return Objects.hash(err);
		}
	}

	public static class DataTypeConstraintException extends RuntimeException {
		private static final long serialVersionUID = 7335599147999542200L;

		public DataTypeConstraintException(String msg) {
			super(msg);
		}
	}
	public static class DataTypeConstraintViolatedException extends DataTypeConstraintException {
		private static final long serialVersionUID = 7449156115495467372L;

		public DataTypeConstraintViolatedException() {
			super("The data type would violate a type constraint");
		}
	}
}