package at.jddev0.lang;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import at.jddev0.lang.LangInterpreter.InterpretingError;
import at.jddev0.lang.data.*;

/**
 * Lang-Module<br>
 * Class for variable data
 *
 * @author JDDev0
 * @version v1.0.0
 */
public class DataObject {
    public static final DataTypeConstraint CONSTRAINT_NORMAL = DataTypeConstraint.fromNotAllowedTypes(new ArrayList<>());
    public static final DataTypeConstraint CONSTRAINT_COMPOSITE = DataTypeConstraint.fromAllowedTypes(Arrays.asList(DataType.ARRAY, DataType.LIST, DataType.STRUCT, DataType.OBJECT, DataType.NULL));
    public static final DataTypeConstraint CONSTRAINT_FUNCTION_POINTER = DataTypeConstraint.fromAllowedTypes(Arrays.asList(DataType.FUNCTION_POINTER, DataType.NULL));

    private static final int FLAG_FINAL = 1;
    private static final int FLAG_STATIC = 2;
    private static final int FLAG_COPY_STATIC_AND_FINAL = 4;
    private static final int FLAG_LANG_VAR = 8;

    //Value
    private DataValue value;

    private DataTypeConstraint typeConstraint = CONSTRAINT_NORMAL;

    //Meta-Data
    /**
     * Variable name of the DataObject (null for anonymous variable)
     */
    private String variableName;
    private int flags;
    private DataType type;
    private long memberOfClassId;
    private Visibility memberVisibility;

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
    public DataObject(String str) {
        this(str, false);
    }
    public DataObject(String str, boolean finalData) {
        setText(str);
        setFinalData(finalData);
    }
    public DataObject(Text txt) {
        this(txt, false);
    }
    public DataObject(Text txt, boolean finalData) {
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

        //Func pointer: copy reference only
        //Set function name for better debugging experience
        if(dataObject.value.getFunctionPointer() != null)
            this.value = new FunctionValue((getVariableName() != null && dataObject.value.getFunctionPointer().getFunctionName() == null)?
                    dataObject.value.getFunctionPointer().withFunctionName(getVariableName()):dataObject.value.getFunctionPointer());
        else
            this.value = dataObject.value;

        if(dataObject.isCopyStaticAndFinalModifiers()) {
            if(dataObject.isFinalData())
                this.flags |= FLAG_FINAL;
            if(dataObject.isStaticData())
                this.flags |= FLAG_STATIC;
        }
    }

    DataObject setArgumentSeparator(String str) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(str == null)
            return setNull();

        this.type = checkAndRetType(DataType.ARGUMENT_SEPARATOR);
        this.value = new TextValue(Text.fromString(str));

        return this;
    }

    public DataObject setText(String str) throws DataTypeConstraintViolatedException {
        return setText(Text.fromString(str));
    }
    public DataObject setText(Text txt) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(txt == null)
            return setNull();

        this.type = checkAndRetType(DataType.TEXT);
        this.value = new TextValue(txt);

        return this;
    }

    public Text getText() {
        return value.getText();
    }

    public DataObject setByteBuffer(byte[] byteBuf) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(byteBuf == null)
            return setNull();

        this.type = checkAndRetType(DataType.BYTE_BUFFER);
        this.value = new ByteBufferValue(byteBuf);

        return this;
    }

    public byte[] getByteBuffer() {
        return value.getByteBuffer();
    }

    public DataObject setArray(DataObject[] arr) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(arr == null)
            return setNull();

        this.type = checkAndRetType(DataType.ARRAY);
        this.value = new ArrayValue(arr);

        return this;
    }

    public DataObject[] getArray() {
        return value.getArray();
    }

    public DataObject setList(LinkedList<DataObject> list) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(list == null)
            return setNull();

        this.type = checkAndRetType(DataType.LIST);
        this.value = new ListValue(list);

        return this;
    }

    public LinkedList<DataObject> getList() {
        return value.getList();
    }

    public DataObject setVarPointer(VarPointerObject vp) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(vp == null)
            return setNull();

        this.type = checkAndRetType(DataType.VAR_POINTER);
        this.value = new VarPointerValue(vp);

        return this;
    }

    public VarPointerObject getVarPointer() {
        return value.getVarPointer();
    }

    public DataObject setFunctionPointer(FunctionPointerObject fp) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(fp == null)
            return setNull();

        this.type = checkAndRetType(DataType.FUNCTION_POINTER);
        this.value = new FunctionValue((getVariableName() != null && fp.getFunctionName() == null)?fp.withFunctionName(getVariableName()):fp);

        return this;
    }

    public FunctionPointerObject getFunctionPointer() {
        return value.getFunctionPointer();
    }

    public DataObject setStruct(StructObject sp) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(sp == null)
            return setNull();

        this.type = checkAndRetType(DataType.STRUCT);
        this.value = new StructValue(sp);

        return this;
    }

    public StructObject getStruct() {
        return value.getStruct();
    }

    public DataObject setObject(LangObject op) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(op == null)
            return setNull();

        this.type = checkAndRetType(DataType.OBJECT);
        this.value = new ObjectValue(op);

        return this;
    }

    public LangObject getObject() {
        return value.getObject();
    }

    public DataObject setNull() throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.NULL);
        this.value = EmptyValue.INSTANCE;

        return this;
    }

    public DataObject setVoid() throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.VOID);
        this.value = EmptyValue.INSTANCE;

        return this;
    }

    public DataObject setInt(int intValue) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.INT);
        this.value = new IntValue(intValue);

        return this;
    }

    public int getInt() {
        return value.getInt();
    }

    /**
     * Sets data to INT = 1 if boolean value is true else INT = 0
     */
    public DataObject setBoolean(boolean booleanValue) throws DataTypeConstraintViolatedException {
        return setInt(booleanValue?1:0);
    }

    public DataObject setLong(long longValue) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.LONG);
        this.value = new LongValue(longValue);

        return this;
    }

    public long getLong() {
        return value.getLong();
    }

    public DataObject setFloat(float floatValue) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.FLOAT);
        this.value = new FloatValue(floatValue);

        return this;
    }

    public float getFloat() {
        return value.getFloat();
    }

    public DataObject setDouble(double doubleValue) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.DOUBLE);
        this.value = new DoubleValue(doubleValue);

        return this;
    }

    public double getDouble() {
        return value.getDouble();
    }

    public DataObject setChar(int charValue) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;

        this.type = checkAndRetType(DataType.CHAR);
        this.value = new CharValue(Character.isValidCodePoint(charValue)?charValue:'\uFFFD');

        return this;
    }

    public int getChar() {
        return value.getChar();
    }

    public DataObject setError(ErrorObject error) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(error == null)
            return setNull();

        this.type = checkAndRetType(DataType.ERROR);
        this.value = new ErrorValue(error);

        return this;
    }

    public ErrorObject getError() {
        return value.getError();
    }

    public DataObject setTypeValue(DataType typeValue) throws DataTypeConstraintViolatedException {
        if(isFinalData())
            return this;
        if(typeValue == null)
            return setNull();

        this.type = checkAndRetType(DataType.TYPE);
        this.value = new TypeValue(typeValue);

        return this;
    }

    public DataType getTypeValue() {
        return value.getTypeValue();
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
        this.flags = (flags | FLAG_FINAL) ^ (finalData?0:FLAG_FINAL);

        return this;
    }

    public boolean isFinalData() {
        return (flags & FLAG_FINAL) != 0;
    }

    public DataObject setStaticData(boolean staticData) {
        this.flags = (flags | FLAG_STATIC) ^ (staticData?0:FLAG_STATIC);

        return this;
    }

    public boolean isStaticData() {
        return (flags & FLAG_STATIC) != 0;
    }

    DataObject setCopyStaticAndFinalModifiers(boolean copyStaticAndFinalModifiers) {
        this.flags = (flags | FLAG_COPY_STATIC_AND_FINAL) ^ (copyStaticAndFinalModifiers?0:FLAG_COPY_STATIC_AND_FINAL);

        return this;
    }

    public boolean isCopyStaticAndFinalModifiers() {
        return (flags & FLAG_COPY_STATIC_AND_FINAL) != 0;
    }

    DataObject setLangVar() {
        this.flags = flags | FLAG_LANG_VAR;

        return this;
    }

    public boolean isLangVar() {
        return (flags & FLAG_LANG_VAR) != 0;
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

    DataObject setMemberVisibility(Visibility memberVisibility) {
        this.memberVisibility = memberVisibility;

        return this;
    }

    public Visibility getMemberVisibility() {
        return memberVisibility;
    }

    DataObject setMemberOfClassId(LangObject memberOfClassId) {
        this.memberOfClassId = memberOfClassId == null?-1:memberOfClassId.classId;

        return this;
    }

    public long getMemberOfClassId() {
        return memberOfClassId;
    }

    public boolean isAccessible(LangObject accessingClass) {
        return memberOfClassId == -1 || memberVisibility == null || memberVisibility == Visibility.PUBLIC ||
                (accessingClass != null && (accessingClass.classId == memberOfClassId ||
                        (memberVisibility == Visibility.PROTECTED && Arrays.stream(LangObject.CLASS_ID_TO_SUPER_CLASS_IDS.get(accessingClass.classId)).
                                anyMatch(classId -> classId == memberOfClassId))));
    }

    @Override
    public String toString() {
        return "<DataObject>";
    }

    public enum DataType {
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
                return new ArrayList<>(types);

            return Arrays.stream(DataType.values()).filter(((Predicate<DataType>)types::contains).negate()).collect(Collectors.toList());
        }

        public List<DataType> getNotAllowedTypes() {
            if(allowed)
                return Arrays.stream(DataType.values()).filter(((Predicate<DataType>)types::contains).negate()).collect(Collectors.toList());

            return new ArrayList<>(types);
        }

        public String toTypeConstraintSyntax() {
            StringBuilder strBuilder = new StringBuilder();
            strBuilder.append("{");

            //Invert "!" if no types are set and print all types
            boolean inverted = !allowed ^ this.types.isEmpty();

            if(inverted)
                strBuilder.append("!");

            Set<DataType> types = new HashSet<>(this.types.isEmpty()?Arrays.asList(DataType.values()):this.types);

            if(!inverted && types.size() > 1 && types.contains(DataType.NULL)) {
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
    public static final class Text implements Comparable<Text> {
        public static final Text EMPTY = new Text("");

        private final String cachedStr;
        private final int[] chars;

        public static Text fromString(String str) {
            return str == null?null:new Text(str);
        }

        public static Text fromCodePoint(int codePoint) {
            if(!Character.isValidCodePoint(codePoint))
                return new Text("\uFFFD");

            return new Text(new String(Character.toChars(codePoint)));
        }

        private Text(String cachedStr, int[] chars) {
            this.cachedStr = cachedStr;
            this.chars = chars;
        }

        private Text(String cachedStr) {
            this.cachedStr = cachedStr;
            this.chars = cachedStr.codePoints().toArray();
        }

        private Text(int[] chars) {
            StringBuilder builder = new StringBuilder(chars.length);

            IntStream.of(chars).forEach(builder::appendCodePoint);

            this.cachedStr = builder.toString();
            this.chars = chars;
        }

        public int charAt(int index) {
            return chars[index];
        }

        public int length() {
            return chars.length;
        }

        public Text trim() {
            return Text.fromString(cachedStr.trim());
        }

        public Text toLowerCase() {
            return Text.fromString(cachedStr.toLowerCase(Locale.ENGLISH));
        }

        public Text toUpperCase() {
            return Text.fromString(cachedStr.toUpperCase(Locale.ENGLISH));
        }

        public int indexOf(Text txt) {
            int index = cachedStr.indexOf(txt.cachedStr);
            int newIndex = index;
            for(int i = 0;i <= index;i++)
                if(Character.isLowSurrogate(cachedStr.charAt(i)))
                    newIndex--;

            return newIndex;
        }

        public int indexOf(Text txt, int fromIndex) {
            int fromIndexOrig = fromIndex;
            for(int i = 0;i <= fromIndexOrig && i < cachedStr.length();i++)
                if(Character.isLowSurrogate(cachedStr.charAt(i)))
                    fromIndex++;

            int index = cachedStr.indexOf(txt.cachedStr, fromIndex);
            int newIndex = index;
            for(int i = 0;i <= index;i++)
                if(Character.isLowSurrogate(cachedStr.charAt(i)))
                    newIndex--;

            return newIndex;
        }

        public int lastIndexOf(Text txt) {
            int index = cachedStr.lastIndexOf(txt.cachedStr);
            int newIndex = index;
            for(int i = 0;i <= index;i++)
                if(Character.isLowSurrogate(cachedStr.charAt(i)))
                    newIndex--;

            return newIndex;
        }

        public int lastIndexOf(Text txt, int fromIndex) {
            int fromIndexOrig = fromIndex;
            for(int i = 0;i <= fromIndexOrig && i < cachedStr.length();i++)
                if(Character.isLowSurrogate(cachedStr.charAt(i)))
                    fromIndex++;

            int index = cachedStr.lastIndexOf(txt.cachedStr, fromIndex);
            int newIndex = index;
            for(int i = 0;i <= index;i++)
                if(Character.isLowSurrogate(cachedStr.charAt(i)))
                    newIndex--;

            return newIndex;
        }

        public Text substring(int fromIndex, int toIndex) {
            return new Text(Arrays.copyOfRange(chars, fromIndex, toIndex));
        }

        public boolean startsWith(Text txt) {
            return cachedStr.startsWith(txt.cachedStr);
        }

        public boolean endsWith(Text txt) {
            return cachedStr.endsWith(txt.cachedStr);
        }

        public boolean isEmpty() {
            return cachedStr.isEmpty();
        }

        public boolean contains(CharSequence str) {
            return cachedStr.contains(str);
        }

        public boolean contains(Text txt) {
            return cachedStr.contains(txt.cachedStr);
        }

        public int[] toCharArray() {
            return Arrays.copyOf(chars, chars.length);
        }

        @Override
        public int compareTo(Text txt) {
            return cachedStr.compareTo(txt.cachedStr);
        }

        @Override
        public String toString() {
            return cachedStr;
        }

        @Override
        public boolean equals(Object o) {
            if(this == o) return true;
            if(o == null || getClass() != o.getClass()) return false;
            Text text = (Text)o;
            return Objects.equals(cachedStr, text.cachedStr);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(cachedStr);
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
         * For methods "this" will be set to the object the method was called on
         */
        private final LangObject thisObject;
        /**
         * If functionName is set, the function name from the stack frame element which is created for the function call will be overridden
         */
        private final String functionName;
        private final String functionInfo;

        private final boolean linkerFunction;
        private final boolean deprecated;
        private final String deprecatedRemoveVersion;
        private final String deprecatedReplacementFunction;

        private final List<InternalFunction> functions;

        /**
         * For normal and native function pointer definition
         * Used for binding the this-object
         */
        public FunctionPointerObject(FunctionPointerObject func, LangObject thisObject) throws DataTypeConstraintException {
            if(thisObject.isClass())
                throw new DataTypeConstraintException("The this-object must be an object");

            this.thisObject = thisObject;
            this.functionName = func.functionName;
            this.functionInfo = func.functionInfo;
            this.linkerFunction = func.linkerFunction;
            this.deprecated = func.deprecated;
            this.deprecatedRemoveVersion = func.deprecatedRemoveVersion;
            this.deprecatedReplacementFunction = func.deprecatedReplacementFunction;
            this.functions = func.functions;
        }

        /**
         * For normal and native function pointer definition
         */
        public FunctionPointerObject(LangObject thisObject, String functionName, String functionInfo,
                                     boolean linkerFunction, boolean deprecated, String deprecatedRemoveVersion,
                                     String deprecatedReplacementFunction, List<InternalFunction> functions) {
            if(thisObject != null && thisObject.isClass())
                throw new DataTypeConstraintException("The this-object must be an object");

            this.thisObject = thisObject;
            this.functionName = functionName;
            this.functionInfo = functionInfo;
            this.linkerFunction = linkerFunction;
            this.deprecated = deprecated;
            this.deprecatedRemoveVersion = deprecatedRemoveVersion;
            this.deprecatedReplacementFunction = deprecatedReplacementFunction;
            this.functions = new ArrayList<>(functions);
        }

        /**
         * For normal function pointer definition
         */
        public FunctionPointerObject(String functionName, String functionInfo, boolean linkerFunction,
                                     boolean deprecated, String deprecatedRemoveVersion,
                                     String deprecatedReplacementFunction, LangNormalFunction normalFunction) {
            this.thisObject = null;
            this.functionName = functionName == null?normalFunction.getFunctionName():functionName;
            this.functionInfo = functionInfo;
            this.linkerFunction = linkerFunction;
            this.deprecated = deprecated;
            this.deprecatedRemoveVersion = deprecatedRemoveVersion;
            this.deprecatedReplacementFunction = deprecatedReplacementFunction;
            this.functions = new ArrayList<>();
            functions.add(new InternalFunction(normalFunction));
        }
        /**
         * For normal function pointer definition
         */
        public FunctionPointerObject(String functionName, LangNormalFunction normalFunction) {
            this(functionName, null, false, false, null, null, normalFunction);
        }
        /**
         * For normal function pointer definition
         */
        public FunctionPointerObject(LangNormalFunction normalFunction) {
            this(null, normalFunction);
        }

        /**
         * For pointer to native function
         */
        public FunctionPointerObject(String functionName, String functionInfo,
                                     boolean linkerFunction, boolean deprecated, String deprecatedRemoveVersion,
                                     String deprecatedReplacementFunction, LangNativeFunction nativeFunction) {
            this.thisObject = null;
            this.functionName = functionName == null?nativeFunction.getFunctionName():functionName;
            this.functionInfo = functionInfo;
            this.linkerFunction = linkerFunction;
            this.deprecated = deprecated;
            this.deprecatedRemoveVersion = deprecatedRemoveVersion;
            this.deprecatedReplacementFunction = deprecatedReplacementFunction;
            this.functions = new ArrayList<>();
            functions.add(new InternalFunction(nativeFunction));
        }
        /**
         * For pointer to native function
         */
        public FunctionPointerObject(String functionName, LangNativeFunction nativeFunction) {
            this(functionName, null, false, false, null, null, nativeFunction);
        }
        /**
         * For pointer to native function
         */
        public FunctionPointerObject(LangNativeFunction nativeFunction) {
            this(nativeFunction.getFunctionName(), nativeFunction);
        }

        public FunctionPointerObject withFunctionName(String functionName) {
            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withFunctionInfo(String functionInfo) {
            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withLinkerFunction(boolean linkerFunction) {
            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withDeprecationInformation(boolean deprecated, String deprecatedRemoveVersion,
                                                                String deprecatedReplacementFunction) {
            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withFunctions(List<InternalFunction> functions) {
            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withAddedFunction(InternalFunction function) {
            List<InternalFunction> functions = new ArrayList<>(this.functions);
            functions.add(function);

            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withAddedFunctions(FunctionPointerObject function) {
            List<InternalFunction> functions = new ArrayList<>(this.functions);
            functions.addAll(function.getFunctions());

            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public FunctionPointerObject withMappedFunctions(UnaryOperator<InternalFunction> mapper) {
            List<InternalFunction> functions = this.functions.stream().map(mapper).collect(Collectors.toList());

            return new FunctionPointerObject(thisObject, functionName, functionInfo, linkerFunction, deprecated,
                    deprecatedRemoveVersion, deprecatedReplacementFunction, functions);
        }

        public LangObject getThisObject() {
            return thisObject;
        }

        public String getFunctionName() {
            return functionName;
        }

        public String getFunctionInfo() {
            return functionInfo;
        }

        public boolean isLinkerFunction() {
            return linkerFunction;
        }

        public boolean isDeprecated() {
            return deprecated;
        }

        public String getDeprecatedRemoveVersion() {
            return deprecatedRemoveVersion;
        }

        public String getDeprecatedReplacementFunction() {
            return deprecatedReplacementFunction;
        }

        public List<InternalFunction> getFunctions() {
            return new ArrayList<>(functions);
        }

        public InternalFunction getFunction(int index) {
            return functions.get(index);
        }

        public int getOverloadedFunctionCount() {
            return functions.size();
        }

        @Override
        public String toString() {
            if(functionName != null)
                return functionName;

            return thisObject == null?"<Function>":"<Method>";
        }

        public boolean isEquals(FunctionPointerObject that, LangInterpreter interpreter, CodePosition pos) {
            if(this == that)
                return true;

            //Check for same reference of thisObjects
            if(this.thisObject != that.thisObject ||
                    this.functions.size() != that.functions.size())
                return false;

            for(int i = 0;i < this.functions.size();i++)
                if(!this.functions.get(i).isEquals(that.functions.get(i), interpreter, pos))
                    return false;

            return true;
        }

        public boolean isStrictEquals(FunctionPointerObject that, LangInterpreter interpreter, CodePosition pos) {
            if(this == that)
                return true;

            //Check for same reference of thisObjects
            if(this.thisObject != that.thisObject ||
                    this.functions.size() != that.functions.size())
                return false;

            for(int i = 0;i < this.functions.size();i++)
                if(!this.functions.get(i).isStrictEquals(that.functions.get(i), interpreter, pos))
                    return false;

            return true;
        }

        public static final class InternalFunction {
            /**
             * For methods the super level of the class [= level of parent]
             */
            private final int superLevel;

            private final LangBaseFunction function;
            private final int functionPointerType;

            private final LangObject memberOfClass;
            private final Visibility memberVisibility;

            /**
             * For normal and native function pointer definition
             * Used for setting member of class and visibility for objects
             */
            InternalFunction(InternalFunction func, LangObject memberOfClass, Visibility memberVisibility) {
                this.superLevel = func.superLevel;
                this.function = func.function;
                this.functionPointerType = func.functionPointerType;
                this.memberOfClass = memberOfClass;
                this.memberVisibility = memberVisibility;
            }

            /**
             * For normal and native function pointer definition
             * Used for setting superLevel for objects
             */
            InternalFunction(InternalFunction func, int superLevel) {
                this.superLevel = superLevel;
                this.function = func.function;
                this.functionPointerType = func.functionPointerType;
                this.memberOfClass = func.memberOfClass;
                this.memberVisibility = func.memberVisibility;
            }

            /**
             * For normal function pointer definition
             */
            public InternalFunction(LangNormalFunction normalFunction) {
                this.superLevel = -1;
                this.function = normalFunction;
                this.functionPointerType = NORMAL;
                this.memberOfClass = null;
                this.memberVisibility = null;
            }

            /**
             * For native function pointer definition
             */
            public InternalFunction(LangNativeFunction nativeFunction) {
                this.superLevel = -1;
                this.function = nativeFunction;
                this.functionPointerType = NATIVE;
                this.memberOfClass = null;
                this.memberVisibility = null;
            }

            public int getSuperLevel() {
                return superLevel;
            }

            public LangBaseFunction getFunction() {
                return function;
            }

            public LangNormalFunction getNormalFunction() {
                if(functionPointerType != NORMAL)
                    return null;

                return (LangNormalFunction)function;
            }

            public LangNativeFunction getNativeFunction() {
                if(functionPointerType != NATIVE)
                    return null;

                return (LangNativeFunction)function;
            }

            public int getFunctionPointerType() {
                return functionPointerType;
            }

            public LangObject getMemberOfClass() {
                return memberOfClass;
            }

            public Visibility getMemberVisibility() {
                return memberVisibility;
            }

            public boolean isAccessible(LangObject accessingClass) {
                return memberOfClass == null || memberVisibility == null || memberVisibility == Visibility.PUBLIC ||
                        (accessingClass != null && (accessingClass.equals(memberOfClass) ||
                                (memberVisibility == Visibility.PROTECTED && accessingClass.isInstanceOf(memberOfClass))));
            }

            public String toFunctionSignatureSyntax() {
                return function.toFunctionSignatureSyntax();
            }

            public String getLangPath() {
                return function.getLangPath();
            }

            public String getLangFile() {
                return function.getLangFile();
            }

            public boolean isEquals(InternalFunction that, LangInterpreter interpreter, CodePosition pos) {
                return this.functionPointerType == that.functionPointerType &&
                        this.superLevel == that.superLevel &&
                        this.function.isEquals(that.function, interpreter, pos);
            }

            public boolean isStrictEquals(InternalFunction that, LangInterpreter interpreter, CodePosition pos) {
                return this.functionPointerType == that.functionPointerType &&
                        this.superLevel == that.superLevel &&
                        this.function.isStrictEquals(that.function, interpreter, pos);
            }
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
        public int getIndexOfMember(String memberName) {
            for(int i = 0;i < memberNames.length;i++)
                if(memberNames[i].equals(memberName))
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
    }
    public static final class LangObject {
        private static long nextClassId = 0;
        static final Map<Long, Long[]> CLASS_ID_TO_SUPER_CLASS_IDS = new HashMap<>();

        public static final LangObject OBJECT_CLASS;
        static {
            Map<String, FunctionPointerObject> methods = new HashMap<>();
            Map<String, Boolean[]> methodOverrideFlags = new HashMap<>();
            Map<String, List<Visibility>> methodVisibility = new HashMap<>();
            methods.put("mp.getClass", LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction(value="mp.getClass", isMethod=true)
                @LangFunction.AllowedTypes(DataType.OBJECT)
                @SuppressWarnings("unused")
                public DataObject getClassMethod(
                        LangInterpreter interpreter, LangObject thisObject
                ) {
                    return new DataObject().setObject(thisObject.getClassBaseDefinition());
                }
            }));
            methodOverrideFlags.put("mp.getClass", new Boolean[] {
                    false
            });
            methodVisibility.put("mp.getClass", Arrays.asList(
                    Visibility.PUBLIC
            ));

            FunctionPointerObject constructors = LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction(value="construct", isMethod=true)
                @LangFunction.AllowedTypes(DataType.VOID)
                @SuppressWarnings("unused")
                public DataObject defaultConstructMethod(
                        LangInterpreter interpreter, LangObject thisObject
                ) {
                    return null;
                }
            });
            List<Visibility> constructorVisibility = new LinkedList<>();
            constructorVisibility.add(Visibility.PUBLIC);

            OBJECT_CLASS = new LangObject(true, "&Object", new DataObject[0], new String[0],
                    new DataTypeConstraint[0], new boolean[0], new Visibility[0], methods, methodOverrideFlags,
                    methodVisibility, constructors, constructorVisibility, null);
        }

        static final LangObject DUMMY_CLASS_DEFINITION_CLASS;
        static {
            FunctionPointerObject constructors = LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction(value="construct", isMethod=true)
                @LangFunction.AllowedTypes(DataType.VOID)
                @SuppressWarnings("unused")
                public DataObject defaultConstructMethod(
                        LangInterpreter interpreter, LangObject thisObject
                ) {
                    return null;
                }
            });
            List<Visibility> constructorVisibility = new LinkedList<>();
            constructorVisibility.add(Visibility.PUBLIC);

            DUMMY_CLASS_DEFINITION_CLASS = new LangObject("<class-definition>", new DataObject[0], new String[0],
                    new DataTypeConstraint[0], new boolean[0], new Visibility[0], new HashMap<>(), new HashMap<>(),
                    new HashMap<>(), constructors, constructorVisibility, new LangObject[] {OBJECT_CLASS});
        }

        private int superLevel = 0;

        private final long classId;

        private final String className;

        private final DataObject[] staticMembers;
        private final String[] memberNames;
        private final DataTypeConstraint[] memberTypeConstraints;
        private final boolean[] memberFinalFlags;
        private final Visibility[] memberVisibility;
        private final LangObject[] memberOfClass;
        private final DataObject[] members;
        private final Map<String, FunctionPointerObject> methods;
        private final FunctionPointerObject constructors;
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

        public LangObject(String className, DataObject[] staticMembers, String[] memberNames,
                          DataTypeConstraint[] memberTypeConstraints, boolean[] memberFinalFlags,
                          DataObject.Visibility[] memberVisibility,
                          Map<String, FunctionPointerObject> methods, Map<String, Boolean[]> methodOverrideFlags,
                          Map<String, List<DataObject.Visibility>> methodVisibility, FunctionPointerObject constructors,
                          List<DataObject.Visibility> constructorVisibility, LangObject[] parentClasses) throws DataTypeConstraintException {
            this(false, className, staticMembers, memberNames, memberTypeConstraints, memberFinalFlags, memberVisibility,
                    methods, methodOverrideFlags, methodVisibility, constructors, constructorVisibility, parentClasses);
        }

        private LangObject(boolean isBaseObject, String className, DataObject[] staticMembers, String[] memberNames,
                           DataTypeConstraint[] memberTypeConstraints, boolean[] memberFinalFlags,
                           DataObject.Visibility[] memberVisibility, Map<String, FunctionPointerObject> methods,
                           Map<String, Boolean[]> methodOverrideFlags, Map<String, List<DataObject.Visibility>> methodVisibility,
                           FunctionPointerObject constructors, List<DataObject.Visibility> constructorVisibility,
                           LangObject[] parentClasses) throws DataTypeConstraintException {
            classId = nextClassId;
            nextClassId++;

            this.className = className;

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

            HashSet<Long> superClassIds = new HashSet<>();
            superClassIds.add(classId);
            for(LangObject parentClass:this.parentClasses) {
                Long[] superSuperClassIds = CLASS_ID_TO_SUPER_CLASS_IDS.get(parentClass.classId);
                superClassIds.addAll(Arrays.asList(superSuperClassIds));
            }
            CLASS_ID_TO_SUPER_CLASS_IDS.put(classId, superClassIds.toArray(new Long[0]));

            for(DataObject staticMember:staticMembers) {
                String staticMemberName = staticMember.getVariableName();

                if(Arrays.stream(parentClasses).map(LangObject::getStaticMembers).
                        anyMatch(superStaticMembers -> Arrays.stream(superStaticMembers).map(DataObject::getVariableName).
                                anyMatch(superStaticMemberName -> superStaticMemberName.equals(staticMemberName))))
                    throw new DataTypeConstraintException("Super class static member must not be shadowed" +
                            " (For static member \"" + staticMemberName + "\")");
            }

            for(DataObject staticMember:staticMembers) {
                staticMember.setMemberOfClassId(this);

                if(staticMember.getType() == DataType.FUNCTION_POINTER && staticMember.getFunctionPointer() != null) {
                    staticMember.value = new FunctionValue(staticMember.getFunctionPointer().withMappedFunctions(internalFunction -> {
                        if(internalFunction.getMemberOfClass() == DUMMY_CLASS_DEFINITION_CLASS) {
                            return new FunctionPointerObject.InternalFunction(internalFunction,
                                    this, Visibility.PUBLIC);
                        }

                        return internalFunction;
                    }));
                }
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

            if(memberNames.length != memberVisibility.length)
                throw new DataTypeConstraintException("The count of members must be equals to the count of member visibility");

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
            this.memberVisibility = Arrays.copyOf(memberVisibility, memberVisibility.length + superClassMemberCount);
            this.memberOfClass = new LangObject[memberNames.length + superClassMemberCount];
            for(int i = 0;i < memberNames.length;i++)
                this.memberOfClass[i] = this;
            {
                int memberIndex = memberNames.length;
                for(LangObject parentClass:parentClasses) {
                    for(int i = 0;i < parentClass.getMemberNames().length;i++) {
                        this.memberNames[memberIndex] = parentClass.getMemberNames()[i];
                        this.memberTypeConstraints[memberIndex] = parentClass.getMemberTypeConstraints()[i];
                        this.memberFinalFlags[memberIndex] = parentClass.getMemberFinalFlags()[i];
                        this.memberVisibility[memberIndex] = parentClass.getMemberVisibility()[i];
                        this.memberOfClass[memberIndex] = parentClass;

                        memberIndex++;
                    }
                }
            }

            this.methods = new HashMap<>();
            List<String> methodNames = new ArrayList<>(methods.keySet());
            for(String methodName:methodNames) {
                FunctionPointerObject overloadedMethods = methods.get(methodName);
                Boolean[] overloadedMethodOverrideFlags = methodOverrideFlags.get(methodName);
                List<Visibility> overloadedMethodVisibility = methodVisibility.get(methodName);

                if(overloadedMethodOverrideFlags == null || overloadedMethods.getOverloadedFunctionCount() != overloadedMethodOverrideFlags.length)
                    throw new DataTypeConstraintException("Invalid override flag values for \"" + methodName + "\"");
                if(overloadedMethodVisibility == null || overloadedMethods.getOverloadedFunctionCount() != overloadedMethodVisibility.size())
                    throw new DataTypeConstraintException("Invalid visibility values for \"" + methodName + "\"");
                if(overloadedMethods.getOverloadedFunctionCount() == 0)
                    throw new DataTypeConstraintException("No method present for method \"" + methodName + "\"");

                //Set visibility
                List<FunctionPointerObject.InternalFunction> internalFunctions = overloadedMethods.getFunctions();
                for(int i = 0;i < internalFunctions.size();i++)
                    internalFunctions.set(i, new FunctionPointerObject.InternalFunction(
                            new FunctionPointerObject.InternalFunction(internalFunctions.get(i),
                                    this, overloadedMethodVisibility.get(i)), 0));
                overloadedMethods = overloadedMethods.withFunctions(internalFunctions);
                this.methods.put(methodName, overloadedMethods);

                //Check override flag
                List<LangBaseFunction> functionSignatures = overloadedMethods.getFunctions().stream().
                        map(FunctionPointerObject.InternalFunction::getFunction).collect(Collectors.toList());

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
                        FunctionPointerObject overloadedMethods = this.methods.get(k);
                        if(overloadedMethods == null) {
                            this.methods.put(k, v.withMappedFunctions(internalFunction -> new FunctionPointerObject.InternalFunction(
                                    internalFunction, internalFunction.getSuperLevel() + 1)));

                            return;
                        }

                        //Override check
                        Boolean[] overloadedMethodOverrideFlags = methodOverrideFlags.get(k);
                        List<LangBaseFunction> functionSignatures = new LinkedList<>();
                        List<Boolean> overrideFlags = new LinkedList<>();
                        for(int i = 0;i < overloadedMethods.getOverloadedFunctionCount();i++) {
                            FunctionPointerObject.InternalFunction overloadedMethod = overloadedMethods.getFunction(i);

                            functionSignatures.add(overloadedMethod.getFunction());
                            overrideFlags.add(overloadedMethodOverrideFlags[i]);
                        }

                        List<LangBaseFunction> superFunctionSignatures = new LinkedList<>();
                        for(int i = 0;i < v.getOverloadedFunctionCount();i++) {
                            FunctionPointerObject.InternalFunction overloadedMethod = v.getFunction(i);
                            superFunctionSignatures.add(overloadedMethod.getFunction());
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

                        this.methods.put(k, overloadedMethods.withAddedFunctions(v.
                                withMappedFunctions(internalFunction -> new FunctionPointerObject.InternalFunction(
                                        internalFunction, internalFunction.getSuperLevel() + 1))));
                    });
                }
            }

            if(constructors.getOverloadedFunctionCount() != constructorVisibility.size())
                throw new DataTypeConstraintException("The count of constructors must be equals to the count of constructor visibility");

            constructors = constructors.withFunctionName("construct").withMappedFunctions(
                    internalFunction -> new FunctionPointerObject.InternalFunction(internalFunction, 0));
            List<FunctionPointerObject.InternalFunction> internalFunctions = constructors.getFunctions();
            for(int i = 0;i < internalFunctions.size();i++)
                internalFunctions.set(i, new FunctionPointerObject.InternalFunction(internalFunctions.get(i),
                        this, constructorVisibility.get(i)));
            constructors = constructors.withFunctions(internalFunctions);
            this.constructors = constructors;
            if(this.constructors.getOverloadedFunctionCount() < 1)
                throw new DataTypeConstraintException("There must be at least one constructor");

            {
                List<LangBaseFunction> functionSignatures = new LinkedList<>();
                for(FunctionPointerObject.InternalFunction constructor:constructors.getFunctions())
                    functionSignatures.add(constructor.getFunction());

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
            if(!classBaseDefinition.isClass())
                throw new DataTypeConstraintException("Class base definition must be a class");

            //Must be set first for isClass checks
            this.classBaseDefinition = classBaseDefinition;

            this.classId = classBaseDefinition.classId;

            this.className = classBaseDefinition.className;

            //No copies, because static members should be the same across all objects
            this.staticMembers = Arrays.copyOf(classBaseDefinition.staticMembers, classBaseDefinition.staticMembers.length);

            this.memberNames = Arrays.copyOf(classBaseDefinition.memberNames, classBaseDefinition.memberNames.length);
            this.memberTypeConstraints = Arrays.copyOf(classBaseDefinition.memberTypeConstraints, classBaseDefinition.memberTypeConstraints.length);
            this.memberFinalFlags = Arrays.copyOf(classBaseDefinition.memberFinalFlags, classBaseDefinition.memberFinalFlags.length);
            this.memberVisibility = Arrays.copyOf(classBaseDefinition.memberVisibility, classBaseDefinition.memberVisibility.length);
            this.memberOfClass = Arrays.copyOf(classBaseDefinition.memberOfClass, classBaseDefinition.memberOfClass.length);

            this.members = new DataObject[classBaseDefinition.memberNames.length];
            for(int i = 0;i < members.length;i++)
                this.members[i] = new DataObject().setNull().setMemberVisibility(this.memberVisibility[i]).
                        setMemberOfClassId(this.memberOfClass[i]).setVariableName(this.memberNames[i]);

            this.methods = new HashMap<>(classBaseDefinition.methods);
            this.methods.replaceAll((k, v) -> new FunctionPointerObject(v, this));

            this.constructors = new FunctionPointerObject(classBaseDefinition.constructors, this);

            this.parentClasses = Arrays.copyOf(classBaseDefinition.parentClasses, classBaseDefinition.parentClasses.length);
        }

        public int getSuperLevel() {
            if(isClass())
                throw new DataTypeConstraintException("Super level can only be queried on objects");

            return superLevel;
        }

        public void setSuperLevel(int superLevel) throws DataTypeConstraintException {
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

        public String getClassName() {
            return className;
        }

        public DataObject[] getStaticMembers() {
            return Arrays.copyOf(staticMembers, staticMembers.length);
        }

        /**
         * @return Will return -1, if the member was not found
         */
        public int getIndexOfStaticMember(String memberName) {
            for(int i = 0;i < staticMembers.length;i++)
                if(staticMembers[i].getVariableName().equals(memberName))
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

        public LangObject[] getMemberOfClass() {
            return Arrays.copyOf(memberOfClass, memberOfClass.length);
        }

        public Visibility[] getMemberVisibility() {
            return Arrays.copyOf(memberVisibility, memberVisibility.length);
        }

        /**
         * @return Will return -1, if the member was not found
         */
        public int getIndexOfMember(String memberName) {
            for(int i = 0;i < members.length;i++)
                if(members[i].getVariableName().equals(memberName))
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

        public Map<String, FunctionPointerObject> getMethods() {
            return new HashMap<>(methods);
        }

        private Map<String, FunctionPointerObject> getRawSuperMethods(int superLevel) {
            Map<String, FunctionPointerObject> rawSuperMethods = new HashMap<>();
            for(LangObject parentClass:parentClasses) {
                if(superLevel > 0) {
                    Map<String, FunctionPointerObject> superRawSuperMethods = parentClass.
                            getRawSuperMethods(superLevel - 1);
                    superRawSuperMethods.forEach((k, v) -> {
                        if(rawSuperMethods.containsKey(k))
                            rawSuperMethods.put(k, rawSuperMethods.get(k).withAddedFunctions(v));
                        else
                            rawSuperMethods.put(k, v);
                    });
                }else {
                    parentClass.getMethods().forEach((k, v) -> {
                        if(rawSuperMethods.containsKey(k))
                            rawSuperMethods.put(k, rawSuperMethods.get(k).withAddedFunctions(v));
                        else
                            rawSuperMethods.put(k, v);
                    });
                }
            }

            return rawSuperMethods;
        }

        public Map<String, FunctionPointerObject> getSuperMethods() {
            return getRawSuperMethods(this.superLevel);
        }

        public FunctionPointerObject getConstructors() {
            return constructors;
        }

        /**
         * @return Returns all constructors for the current super level without going to super
         * [0 is current, 1 is parent, 2 is grandparent]
         */
        public FunctionPointerObject getConstructorsForCurrentSuperLevel() {
            if(superLevel == 0)
                return getConstructors();

            return getRawSuperConstructors(this.superLevel - 1);
        }

        private FunctionPointerObject getRawSuperConstructors(int superLevel) {
            FunctionPointerObject rawSuperConstructors = null;
            for(LangObject parentClass:parentClasses) {
                if(superLevel > 0) {
                    FunctionPointerObject superRawSuperConstructors = parentClass.getRawSuperConstructors(superLevel - 1);
                    if(rawSuperConstructors == null)
                        rawSuperConstructors = superRawSuperConstructors;
                    else
                        rawSuperConstructors = rawSuperConstructors.withAddedFunctions(superRawSuperConstructors);
                }else {
                    if(rawSuperConstructors == null)
                        rawSuperConstructors = parentClass.getConstructors();
                    else
                        rawSuperConstructors = rawSuperConstructors.withAddedFunctions(parentClass.getConstructors());
                }
            }

            return rawSuperConstructors;
        }

        public FunctionPointerObject getSuperConstructors() {
            return getRawSuperConstructors(this.superLevel);
        }

        public LangObject[] getParentClasses() {
            return Arrays.copyOf(parentClasses, parentClasses.length);
        }

        public LangObject getClassBaseDefinition() {
            return classBaseDefinition;
        }

        public boolean isInstanceOf(LangObject classObject) {
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

        public InterpretingError getInterpretingError() {
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
    public enum Visibility {
        PRIVATE, PROTECTED, PUBLIC;

        public static Visibility fromASTNode(AbstractSyntaxTree.ClassDefinitionNode.Visibility visibility) {
            if(visibility == null)
                return null;

            switch(visibility) {
                case PRIVATE:
                    return PRIVATE;
                case PROTECTED:
                    return PROTECTED;
                case PUBLIC:
                    return PUBLIC;
            }

            return null;
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