package at.jddev0.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;

import at.jddev0.io.TerminalIO;
import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraint;
import at.jddev0.lang.DataObject.DataTypeConstraintException;
import at.jddev0.lang.DataObject.FunctionPointerObject;
import at.jddev0.lang.DataObject.VarPointerObject;
import at.jddev0.lang.LangFunction.*;
import at.jddev0.lang.LangFunction.LangParameter.*;
import at.jddev0.lang.LangInterpreter.InterpretingError;

public class LangNativeFunction extends LangBaseFunction {
    private final List<Class<?>> methodParameterTypeList;
    private final Object instance;
    private final Method functionBody;

    private final boolean hasInterpreterParameter;

    private final boolean method;

    private final Object[] valueDependencies;

    public static FunctionPointerObject getSingleLangFunctionFromObject(Object obj, Object... valueDependencies)
            throws IllegalArgumentException, DataTypeConstraintException {
        Map<String, FunctionPointerObject> functions = getLangFunctionsFromObject(obj, valueDependencies);
        if(functions.isEmpty())
            throw new IllegalArgumentException("No methods which are annotated with @LangFunctions are defined in " + obj);

        if(functions.size() > 1 || functions.values().iterator().next().getOverloadedFunctionCount() > 1)
            throw new IllegalArgumentException("Multiple methods which are annotated with @LangFunctions are defined in " + obj);

        return functions.values().iterator().next();
    }

    public static Map<String, FunctionPointerObject> getLangFunctionsFromObject(Object obj) {
        return getLangFunctionsFromObject(obj, new Object[0]);
    }

    private static Map<String, FunctionPointerObject> getLangFunctionsFromObject(Object obj, Object... valueDependencies)
            throws IllegalArgumentException, DataTypeConstraintException {
        return getLangFunctionsOfClass(obj, obj.getClass(), valueDependencies);
    }

    public static Map<String, FunctionPointerObject> getLangFunctionsOfClass(Class<?> clazz)
            throws IllegalArgumentException, DataTypeConstraintException {
        return getLangFunctionsOfClass(clazz, new Object[0]);
    }

    private static Map<String, FunctionPointerObject> getLangFunctionsOfClass(Class<?> clazz, Object... valueDependencies)
            throws IllegalArgumentException, DataTypeConstraintException {
        return getLangFunctionsOfClass(null, clazz, valueDependencies);
    }

    private static Map<String, FunctionPointerObject> getLangFunctionsOfClass(Object instance, Class<?> clazz,
                                                                              Object... valueDependencies)
            throws IllegalArgumentException, DataTypeConstraintException {
        Map<String, List<Method>> methodsByLangFunctionName = new HashMap<>();

        for(Method method:clazz.getDeclaredMethods()) {
            //Add static methods if instance is null else add non-static methods
            if(instance == null ^ Modifier.isStatic(method.getModifiers()))
                continue;

            if(method.isAnnotationPresent(LangFunction.class)) {
                LangFunction langFunction = method.getAnnotation(LangFunction.class);
                String functionName = langFunction.value();

                if(!methodsByLangFunctionName.containsKey(functionName))
                    methodsByLangFunctionName.put(functionName, new LinkedList<>());

                if(langFunction.hasInfo())
                    methodsByLangFunctionName.get(functionName).add(0, method);
                else
                    methodsByLangFunctionName.get(functionName).add(method);
            }
        }

        Map<String, FunctionPointerObject> langNativeFunctions = new HashMap<>();
        for(List<Method> methods:methodsByLangFunctionName.values()) {
            if(methods.size() >= 2 && (!methods.get(0).getAnnotation(LangFunction.class).hasInfo() || methods.get(1).getAnnotation(LangFunction.class).hasInfo()))
                throw new IllegalArgumentException("The value of hasInfo() must be true for exactly one overloaded @LangFunction");

            List<LangNativeFunction> langNativeFunction = new LinkedList<>();
            langNativeFunction.add(create(instance, methods.get(0), valueDependencies));
            String functionName = langNativeFunction.get(0).getFunctionName();

            LangFunction langFunction = methods.get(0).getAnnotation(LangFunction.class);
            for(int i = 1;i < methods.size();i++)
                langNativeFunction.add(createFunction(instance, methods.get(i), functionName, valueDependencies));

            boolean linkerFunction = langFunction.isLinkerFunction();

            boolean deprecated = langFunction.isDeprecated();
            String deprecatedRemoveVersion = langFunction.getDeprecatedRemoveVersion();
            if(deprecatedRemoveVersion.isEmpty())
                deprecatedRemoveVersion = null;
            String deprecatedReplacementFunction = langFunction.getDeprecatedReplacementFunction();
            if(deprecatedReplacementFunction.isEmpty())
                deprecatedReplacementFunction = null;

            LangInfo langInfo = methods.get(0).getAnnotation(LangInfo.class);
            String functionInfo = langInfo == null?null:langInfo.value();

            langNativeFunctions.put(functionName, new FunctionPointerObject(null, null,
                    functionInfo, linkerFunction, deprecated, deprecatedRemoveVersion, deprecatedReplacementFunction,
                    langNativeFunction.stream().map(FunctionPointerObject.InternalFunction::new).
                            collect(Collectors.toList())));
        }

        return langNativeFunctions;
    }

    public static LangNativeFunction create(Object instance, Method functionBody, Object... valueDependencies)
            throws IllegalArgumentException, DataTypeConstraintException {
        LangFunction langFunction = functionBody.getAnnotation(LangFunction.class);
        if(langFunction == null)
            throw new IllegalArgumentException("Method must be annotated with @LangFunction");

        if(!DataObject.class.isAssignableFrom(functionBody.getReturnType()))
            throw new IllegalArgumentException("Method must be annotated with @LangFunction must return a DataObject");

        String functionName = langFunction.value();

        return createFunction(instance, functionBody, functionName, valueDependencies);
    }

    public static LangNativeFunction createFunction(Object instance, Method functionBody, String functionName,
                                                    Object[] valueDependencies)
            throws IllegalArgumentException, DataTypeConstraintException {
        LangFunction langFunction = functionBody.getAnnotation(LangFunction.class);
        if(langFunction == null)
            throw new IllegalArgumentException("Method must be annotated with @LangFunction");

        if(!DataObject.class.isAssignableFrom(functionBody.getReturnType()))
            throw new IllegalArgumentException("Method must be annotated with @LangFunction must return a DataObject");

        boolean combinatorFunction = functionBody.isAnnotationPresent(CombinatorFunction.class);

        DataTypeConstraint returnValueTypeConstraint = DataObject.CONSTRAINT_NORMAL;

        AllowedTypes allowedTypes = functionBody.getAnnotation(AllowedTypes.class);
        if(allowedTypes != null)
            returnValueTypeConstraint = DataTypeConstraint.fromAllowedTypes(Arrays.asList(allowedTypes.value()));

        NotAllowedTypes notAllowedTypes = functionBody.getAnnotation(NotAllowedTypes.class);
        if(notAllowedTypes != null)
            returnValueTypeConstraint = DataTypeConstraint.fromNotAllowedTypes(Arrays.asList(notAllowedTypes.value()));

        if(allowedTypes != null && notAllowedTypes != null)
            throw new IllegalArgumentException("Method must not be annotated with both @AllowedTypes and @NotAllowedTypes");

        Parameter[] parameters = functionBody.getParameters();

        Parameter firstParam = parameters.length >= 1?parameters[0]:null;
        boolean hasInterpreterParameter = firstParam != null && firstParam.getType().isAssignableFrom(LangInterpreter.class);

        Parameter secondParam = parameters.length >= 2?parameters[1]:null;

        boolean method = langFunction.isMethod();

        if(method && (hasInterpreterParameter?secondParam == null || !secondParam.getType().isAssignableFrom(DataObject.LangObject.class):
                (firstParam == null || !firstParam.getType().isAssignableFrom(DataObject.LangObject.class))))
            throw new IllegalArgumentException("The method for LangObjects must start with " +
                    "(LangInterpreter interpreter, LangObject thisObject) or (LangObject thisObject)");

        int diff = (hasInterpreterParameter?1:0) + (method?1:0);
        List<Class<?>> methodParameterTypeList = new ArrayList<>(parameters.length - diff);
        List<DataObject> parameterList = new ArrayList<>(parameters.length - diff);
        List<DataTypeConstraint> parameterDataTypeConstraintList = new ArrayList<>(parameters.length - diff);
        List<ParameterAnnotation> parameterAnnotationList = new ArrayList<>();
        List<String> parameterInfoList = new ArrayList<>(parameters.length - diff);
        int varArgsParameterIndex = -1;
        boolean textVarArgsParameter = false;
        boolean rawVarArgsParameter = false;

        for(int i = diff;i < parameters.length;i++) {
            Parameter parameter = parameters[i];

            LangParameter langParameter = parameter.getAnnotation(LangParameter.class);
            if(langParameter == null)
                throw new IllegalArgumentException("Method parameters after the optional LangInterpreter parameter must be annotated with @LangParameter");

            ParameterAnnotation parameterAnnotation;

            //If parameterCount > 1 -> Multiple type constraining annotations where used
            int typeConstraintingParameterCount = 0;
            int specialTypeConstraintingParameterCount = 0;

            boolean isNumberValue = parameter.isAnnotationPresent(NumberValue.class) && typeConstraintingParameterCount++ >= 0 && specialTypeConstraintingParameterCount++ >= 0;
            boolean isCallableValue = parameter.isAnnotationPresent(CallableValue.class) && typeConstraintingParameterCount++ >= 0 && specialTypeConstraintingParameterCount++ >= 0;
            boolean isBooleanValue = parameter.isAnnotationPresent(BooleanValue.class) && typeConstraintingParameterCount++ >= 0 && specialTypeConstraintingParameterCount++ >= 0;

            //Call by pointer can be used with additional @AllowedTypes or @NotAllowedTypes type constraint
            boolean isCallByPointer = parameter.isAnnotationPresent(CallByPointer.class) && specialTypeConstraintingParameterCount++ >= 0;

            boolean isContainsVarArgsParameter = parameter.isAnnotationPresent(VarArgs.class) && specialTypeConstraintingParameterCount++ >= 0;
            if(specialTypeConstraintingParameterCount > 1)
                throw new IllegalArgumentException("DataObject parameter must be annotated with at most one of @NumberValue, @CallableValue, @BooleanValue, @CallByPointer, or @VarArgs");

            boolean isContainsRawVarArgsParameter = parameter.isAnnotationPresent(RawVarArgs.class) && typeConstraintingParameterCount++ >= 0 && specialTypeConstraintingParameterCount++ >= 0;
            if(isContainsRawVarArgsParameter && combinatorFunction)
                throw new IllegalArgumentException("@RawVarArgs can not be used for combinator functions");
            if(typeConstraintingParameterCount > 1 || specialTypeConstraintingParameterCount > 1)
                throw new IllegalArgumentException("@LangParameter which are annotated with @RawVarArgs can not be annotated with other lang parameter annotations apart from @LangInfo");

            String variableName = langParameter.value();

            //TODO error if parameter name already exists or if name is invalid [Check for all cases: {NUMBER, BOOLEAN, CALL_BY_POINTER -> $, VAR_ARGS -> $, &, NORMAL -> $, &, fp.}]

            if(isBooleanValue) {
                parameterAnnotation = ParameterAnnotation.BOOLEAN;

                if(!parameter.getType().isAssignableFrom(DataObject.class) && !parameter.getType().isAssignableFrom(boolean.class))
                    throw new IllegalArgumentException("@LangParameter which are annotated with @BooleanValue must be of type DataObject or boolean");
            }else if(isNumberValue) {
                parameterAnnotation = ParameterAnnotation.NUMBER;

                if(!parameter.getType().isAssignableFrom(DataObject.class) && !parameter.getType().isAssignableFrom(Number.class))
                    throw new IllegalArgumentException("@LangParameter which are annotated with @NumberValue must be of type DataObject or Number");
            }else if(isCallableValue) {
                parameterAnnotation = ParameterAnnotation.CALLABLE;

                if(!parameter.getType().isAssignableFrom(DataObject.class))
                    throw new IllegalArgumentException("@LangParameter which are annotated with @CallableValue must be of type DataObject");
            }else if(isCallByPointer) {
                parameterAnnotation = ParameterAnnotation.CALL_BY_POINTER;

                if(!parameter.getType().isAssignableFrom(DataObject.class))
                    throw new IllegalArgumentException("@LangParameter which are annotated with @CallByPointer must be of type DataObject");
            }else if(isContainsRawVarArgsParameter) {
                parameterAnnotation = ParameterAnnotation.RAW_VAR_ARGS;

                varArgsParameterIndex = parameterList.size();

                rawVarArgsParameter = true;

                boolean isListParameter = parameter.getType().isAssignableFrom(List.class);
                if(isListParameter) {
                    Type parameterType = parameter.getParameterizedType();
                    if(parameterType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType)parameterType;
                        Type[] types = parameterizedType.getActualTypeArguments();

                        isListParameter = types.length == 1 && types[0] instanceof Class<?> && ((Class<?>)types[0]).isAssignableFrom(DataObject.class);
                    }else {
                        isListParameter = false;
                    }
                }

                if(!isListParameter)
                    throw new IllegalArgumentException("@LangParameter which are annotated with @RawVarArgs must be of type List<DataObject>");
            }else if(isContainsVarArgsParameter) {
                parameterAnnotation = ParameterAnnotation.VAR_ARGS;

                if(varArgsParameterIndex != -1)
                    throw new IllegalArgumentException("There can only be one @LangParameter which is annotated with @VarArgs");

                varArgsParameterIndex = parameterList.size();

                textVarArgsParameter = variableName.startsWith("$");
                if(textVarArgsParameter && combinatorFunction)
                    throw new IllegalArgumentException("Combinator functions can not have @LangParameters with @VarArgs as text var args parameter");

                if(textVarArgsParameter && !parameter.getType().isAssignableFrom(DataObject.class))
                    throw new IllegalArgumentException("@LangParameter which are annotated with @VarArgs as text var args parameter must be of type DataObject");

                boolean isListParameter = parameter.getType().isAssignableFrom(List.class);
                if(isListParameter) {
                    Type parameterType = parameter.getParameterizedType();
                    if(parameterType instanceof ParameterizedType) {
                        ParameterizedType parameterizedType = (ParameterizedType)parameterType;
                        Type[] types = parameterizedType.getActualTypeArguments();

                        isListParameter = types.length == 1 && types[0] instanceof Class<?> && ((Class<?>)types[0]).isAssignableFrom(DataObject.class);
                    }else {
                        isListParameter = false;
                    }
                }

                if(!parameter.getType().isAssignableFrom(DataObject.class) && !parameter.getType().isAssignableFrom(DataObject[].class) && !isListParameter)
                    throw new IllegalArgumentException("@LangParameter which are annotated with @VarArgs as array var args parameter"
                            + " must be of type DataObject, DataObject[], or List<DataObject>");
            }else {
                parameterAnnotation = ParameterAnnotation.NORMAL;

                if(!parameter.getType().isAssignableFrom(DataObject.class))
                    throw new IllegalArgumentException("@LangParameter must be of type DataObject (Other types besides DataObject are allowed for certain annotations)");
            }

            DataTypeConstraint typeConstraint = null;

            allowedTypes = parameter.getAnnotation(AllowedTypes.class);
            if(allowedTypes != null && !isContainsVarArgsParameter && !parameter.getType().isAssignableFrom(DataObject.class))
                throw new IllegalArgumentException("@AllowedTypes can only be used if @LangParameter is of type DataObject");
            if(allowedTypes != null && isContainsVarArgsParameter && textVarArgsParameter)
                throw new IllegalArgumentException("@AllowedTypes can not be used with @VarArgs as text var args");
            if(allowedTypes != null && typeConstraintingParameterCount++ >= 0)
                typeConstraint = DataTypeConstraint.fromAllowedTypes(Arrays.asList(allowedTypes.value()));

            notAllowedTypes = parameter.getAnnotation(NotAllowedTypes.class);
            if(notAllowedTypes != null && !isContainsVarArgsParameter && !parameter.getType().isAssignableFrom(DataObject.class))
                throw new IllegalArgumentException("@NotAllowedTypes can only be used if @LangParameter is of type DataObject");
            if(notAllowedTypes != null && isContainsVarArgsParameter && textVarArgsParameter)
                throw new IllegalArgumentException("@NotAllowedTypes can not be used with @VarArgs as text var args");
            if(notAllowedTypes != null && typeConstraintingParameterCount++ >= 0)
                typeConstraint = DataTypeConstraint.fromNotAllowedTypes(Arrays.asList(notAllowedTypes.value()));

            if(typeConstraintingParameterCount > 1)
                throw new IllegalArgumentException("DataObject parameter must be annotated with at most one of @RawVarArgs, @AllowedTypes, @NotAllowedTypes, @NumberValue, @CallableValue, or @BooleanValue");

            LangInfo langInfo = parameter.getAnnotation(LangInfo.class);
            String parameterInfo = langInfo == null?null:langInfo.value();

            //Allow all types for var args parameters
            if(typeConstraint == null)
                typeConstraint = (isContainsVarArgsParameter || isContainsRawVarArgsParameter)?DataObject.CONSTRAINT_NORMAL:DataObject.getTypeConstraintFor(variableName);

            methodParameterTypeList.add(parameter.getType());
            parameterList.add(new DataObject().setVariableName(variableName));
            parameterDataTypeConstraintList.add(typeConstraint);
            parameterInfoList.add(parameterInfo);
            parameterAnnotationList.add(parameterAnnotation);
        }

        if(rawVarArgsParameter && parameterList.size() != 1)
            throw new IllegalArgumentException("If @RawVarArgs is used there must be exactly one lang parameter");

        return new LangNativeFunction(methodParameterTypeList, parameterList, parameterDataTypeConstraintList,
                parameterAnnotationList, parameterInfoList, varArgsParameterIndex, textVarArgsParameter, rawVarArgsParameter,
                returnValueTypeConstraint, instance, functionBody, functionName, hasInterpreterParameter, method, combinatorFunction,
                0, new ArrayList<>(), valueDependencies);
    }

    private LangNativeFunction(List<Class<?>> methodParameterTypeList,
                               List<DataObject> parameterList, List<DataTypeConstraint> parameterDataTypeConstraintList,
                               List<ParameterAnnotation> parameterAnnotationList, List<String> parameterInfoList,
                               int varArgsParameterIndex, boolean textVarArgsParameter, boolean rawVarArgsParameter,
                               DataTypeConstraint returnValueTypeConstraint, Object instance, Method functionBody, String functionName,
                               boolean hasInterpreterParameter, boolean method, boolean combinatorFunction, int combinatorFunctionCallCount,
                               List<DataObject> combinatorProvidedArgumentList, Object[] valueDependencies) {
        super(null, null, parameterList, parameterDataTypeConstraintList, parameterAnnotationList,
                parameterInfoList, varArgsParameterIndex, textVarArgsParameter, rawVarArgsParameter, returnValueTypeConstraint,
                combinatorFunction, combinatorFunctionCallCount, combinatorProvidedArgumentList, functionName);

        this.methodParameterTypeList = methodParameterTypeList;
        this.instance = instance;
        this.functionBody = functionBody;
        functionBody.setAccessible(true);
        this.hasInterpreterParameter = hasInterpreterParameter;
        this.method = method;
        this.valueDependencies = valueDependencies;
    }

    public DataObject callFunc(LangInterpreter interpreter, DataObject.LangObject thisObject, int superLevel,
                               List<DataObject> argumentList, List<DataObject> combinedArgumentList) {
        if(method && thisObject == null)
            return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "This-object is not bound for native function for LangObject");

        if(!method && thisObject != null)
            return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "This-object is bound for native function");

        int argCount = parameterList.size();

        if(combinatorFunction) {
            combinedArgumentList = new ArrayList<>(combinedArgumentList.stream().map(DataObject::new).collect(Collectors.toList()));
            combinedArgumentList.addAll(0, combinatorProvidedArgumentList);
        }

        if(varArgsParameterIndex == -1) {
            if(!combinatorFunction && combinedArgumentList.size() < argCount)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Not enough arguments (%s needed)", argCount));
            if(combinedArgumentList.size() > argCount)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Too many arguments (%s needed)", argCount));
        }else {
            //Infinite combinator functions (= Combinator functions with var args argument) must be called exactly two times
            if((!combinatorFunction || combinatorFunctionCallCount > 0) && combinedArgumentList.size() < argCount - 1)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Not enough arguments (at least %s needed)", argCount - 1));
        }

        int diff = (hasInterpreterParameter?1:0) + (method?1:0);
        Object[] methodArguments = new Object[diff + argCount];
        if(hasInterpreterParameter) {
            methodArguments[0] = interpreter;
        }
        if(method)
            methodArguments[diff - 1] = thisObject;

        int argumentIndex = 0;
        for(int i = 0;i < argCount;i++) {
            if(combinatorFunction && argumentIndex >= combinedArgumentList.size() && (varArgsParameterIndex == -1 || combinatorFunctionCallCount == 0))
                return combinatorCall(thisObject, superLevel, combinedArgumentList);

            String variableName = parameterList.get(i).getVariableName();

            boolean ignoreTypeCheck = parameterAnnotationList.get(i) == ParameterAnnotation.CALL_BY_POINTER || parameterAnnotationList.get(i) == ParameterAnnotation.VAR_ARGS ||
                    parameterAnnotationList.get(i) == ParameterAnnotation.RAW_VAR_ARGS;

            if(!ignoreTypeCheck && !parameterDataTypeConstraintList.get(i).isTypeAllowed(combinedArgumentList.get(argumentIndex).getType()))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("The type of argument %d (\"%s\") must be one of %s", argumentIndex + 1,
                        variableName, parameterDataTypeConstraintList.get(i).getAllowedTypes()));

            Number argumentNumberValue = parameterAnnotationList.get(i) == ParameterAnnotation.NUMBER?
                    interpreter.conversions.toNumber(combinedArgumentList.get(argumentIndex), CodePosition.EMPTY):null;
            if(parameterAnnotationList.get(i) == ParameterAnnotation.NUMBER && argumentNumberValue == null)
                return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM, String.format("Argument %d (\"%s\") must be a number", argumentIndex + 1, variableName));

            if(parameterAnnotationList.get(i) == ParameterAnnotation.CALLABLE && !LangUtils.isCallable(combinedArgumentList.get(argumentIndex)))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument %d (\"%s\") must be callable", argumentIndex + 1, variableName));

            Class<?> methodParameterType = methodParameterTypeList.get(i);

            try {
                Object argument;
                if(parameterAnnotationList.get(i) == ParameterAnnotation.RAW_VAR_ARGS) {
                    argument = new LinkedList<>(argumentList);
                }else if(parameterAnnotationList.get(i) == ParameterAnnotation.VAR_ARGS) {
                    //Infinite combinator functions (= Combinator functions with var args argument) must be called exactly two times
                    if(combinatorFunction && combinatorFunctionCallCount == 0)
                        return combinatorCall(thisObject, superLevel, combinedArgumentList);

                    List<DataObject> varArgsArgumentList = combinedArgumentList.subList(i, combinedArgumentList.size() - argCount + i + 1).stream().
                            map(DataObject::new).collect(Collectors.toList());
                    if(!textVarArgsParameter) {
                        DataTypeConstraint typeConstraint = parameterDataTypeConstraintList.get(i);

                        for(int j = 0;j < varArgsArgumentList.size();j++) {
                            DataObject varArgsArgument = varArgsArgumentList.get(j);
                            if(!typeConstraint.isTypeAllowed(varArgsArgument.getType()))
                                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                                        String.format("The type of argument %d (for var args parameter \"%s\") must be one of %s", i + j + 1,
                                                variableName, typeConstraint.getAllowedTypes()));
                        }
                    }

                    if(methodParameterType.isAssignableFrom(DataObject.class)) {
                        if(textVarArgsParameter) {
                            List<DataObject> argumentListCopy = new ArrayList<>(argumentList);

                            //Remove leading arguments
                            for(int j = 0;j < i;j++)
                                for(int k = 0;k < argumentListCopy.size();k++)
                                    if(argumentListCopy.remove(0).getType() == DataType.ARGUMENT_SEPARATOR)
                                        break;

                            //Remove trailing arguments
                            for(int j = 0;j < argCount - i - 1;j++)
                                for(int k = argumentListCopy.size() - 1;k >= 0;k--)
                                    if(argumentListCopy.remove(k).getType() == DataType.ARGUMENT_SEPARATOR)
                                        break;

                            DataObject combinedArgument = LangUtils.combineDataObjects(argumentListCopy,
                                    interpreter, CodePosition.EMPTY);
                            argument = new DataObject(combinedArgument == null?DataObject.Text.EMPTY:interpreter.conversions.
                                    toText(combinedArgument, CodePosition.EMPTY)).setVariableName(variableName);
                        }else {
                            argument = new DataObject().setVariableName(variableName).
                                    setArray(varArgsArgumentList.toArray(new DataObject[0]));
                        }
                    }else if(methodParameterType.isAssignableFrom(DataObject[].class)) {
                        argument = varArgsArgumentList.toArray(new DataObject[0]);
                    }else {
                        argument = new ArrayList<>(varArgsArgumentList);
                    }

                    //Not "+1", because argumentIndex will be incremented at the end of the for loop
                    argumentIndex = combinedArgumentList.size() - argCount + i;
                }else if(methodParameterType.isAssignableFrom(DataObject.class)) {
                    if(parameterAnnotationList.get(i) == ParameterAnnotation.CALL_BY_POINTER) {
                        argument = new DataObject().setVariableName(variableName).
                                setVarPointer(new VarPointerObject(combinedArgumentList.get(argumentIndex))).
                                setTypeConstraint(parameterDataTypeConstraintList.get(i));
                    }else {
                        argument = new DataObject(combinedArgumentList.get(argumentIndex)).setVariableName(variableName).
                                setTypeConstraint(parameterDataTypeConstraintList.get(i));
                    }
                }else if(methodParameterType.isAssignableFrom(Number.class)) {
                    argument = argumentNumberValue;
                }else if(methodParameterType.isAssignableFrom(boolean.class)) {
                    argument = interpreter.conversions.toBool(combinedArgumentList.get(argumentIndex), CodePosition.EMPTY);
                }else {
                    return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR, "Invalid native method parameter argument type");
                }

                methodArguments[i + diff] = argument;
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR,
                        String.format("Native method contains invalid type constraint combinations for Argument %d (\"%s\"): %s", i + 1, variableName, e.getMessage()));
            }

            argumentIndex++;
        }

        try {
            DataObject ret = (DataObject)functionBody.invoke(instance, methodArguments);

            if(returnValueTypeConstraint != null && !interpreter.isThrownValue()) {
                //Thrown values are always allowed

                DataObject retTmp = LangUtils.nullToLangVoid(ret);

                if(!returnValueTypeConstraint.isTypeAllowed(retTmp.getType()))
                    return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                            "Invalid return value type \"" + retTmp.getType() + "\"");
            }

            return ret;
        }catch(IllegalAccessException|IllegalArgumentException e) {
            return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR,
                    "Native Error (\"" + e.getClass().getSimpleName() + "\"): " + e.getMessage());
        }catch(InvocationTargetException e) {
            Throwable t = e.getTargetException();
            if(t == null)
                t = e;

            if(interpreter.executionFlags.nativeStackTraces) {
                if(interpreter.term == null) {
                    System.out.println("Native Stack Trace:");
                    t.printStackTrace(System.out);
                    System.out.println();
                }else {
                    interpreter.term.logln(TerminalIO.Level.ERROR, "Native Stack Trace:", LangNativeFunction.class);
                    interpreter.term.logStackTrace(t, LangNativeFunction.class);
                }
            }

            return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR,
                    "Native Error (\"" + t.getClass().getSimpleName() + "\"): " + t.getMessage());
        }
    }

    private DataObject combinatorCall(DataObject.LangObject thisObject, int superLevel,
                                      List<DataObject> combinedArgumentList) {
        LangNativeFunction langNativeFunction = new LangNativeFunction(methodParameterTypeList, parameterList,
                parameterDataTypeConstraintList, parameterAnnotationList, parameterInfoList, varArgsParameterIndex,
                textVarArgsParameter, rawVarArgsParameter, returnValueTypeConstraint, instance, functionBody, functionName,
                hasInterpreterParameter, method, combinatorFunction, combinatorFunctionCallCount + 1,
                combinedArgumentList, valueDependencies);

        String functionNames = combinedArgumentList.stream().map(dataObject -> {
            if(dataObject.getType() != DataType.FUNCTION_POINTER)
                return "<arg>";

            String functionName = dataObject.getFunctionPointer().getFunctionName();
            return functionName == null?dataObject.getVariableName():functionName;
        }).collect(Collectors.joining(", "));

        String functionName = "<" + (varArgsParameterIndex == -1?"":"inf-") + this.functionName + "-func(" + functionNames + ")>";

        FunctionPointerObject fp = new FunctionPointerObject(functionName, langNativeFunction);
        if(method)
            fp = new FunctionPointerObject(fp, thisObject).withMappedFunctions(internalFunction ->
                    new FunctionPointerObject.InternalFunction(internalFunction, superLevel));

        return new DataObject().setFunctionPointer(fp);
    }

    public boolean isMethod() {
        return method;
    }

    @Override
    public boolean isEquals(LangBaseFunction baseFunction, LangInterpreter interpreter, CodePosition pos) {
        if(!(baseFunction instanceof LangNativeFunction))
            return false;

        LangNativeFunction that = (LangNativeFunction)baseFunction;

        if(!super.isEquals(that, interpreter, pos) ||
                this.hasInterpreterParameter != that.hasInterpreterParameter ||
                !Objects.equals(this.methodParameterTypeList, that.methodParameterTypeList) ||
                !Objects.equals(this.functionBody, that.functionBody) ||
                this.valueDependencies.length != that.valueDependencies.length)
            return false;

        for(int i = 0;i < this.valueDependencies.length;i++)
            if((this.valueDependencies[i] instanceof DataObject && that.valueDependencies[i] instanceof DataObject)?
                    !interpreter.operators.isEquals((DataObject)this.valueDependencies[i], (DataObject)that.valueDependencies[i],
                            pos):
                    !Objects.equals(this.valueDependencies[i], that.valueDependencies[i]))
                return false;

        return true;
    }

    @Override
    public boolean isStrictEquals(LangBaseFunction baseFunction, LangInterpreter interpreter, CodePosition pos) {
        if(!(baseFunction instanceof LangNativeFunction))
            return false;

        LangNativeFunction that = (LangNativeFunction)baseFunction;

        if(!super.isEquals(that, interpreter, pos) ||
                this.hasInterpreterParameter != that.hasInterpreterParameter ||
                this.combinatorFunction != that.combinatorFunction ||
                !Objects.equals(this.methodParameterTypeList, that.methodParameterTypeList) ||
                !Objects.equals(this.functionBody, that.functionBody) ||
                this.valueDependencies.length != that.valueDependencies.length)
            return false;

        for(int i = 0;i < this.valueDependencies.length;i++)
            if((this.valueDependencies[i] instanceof DataObject && that.valueDependencies[i] instanceof DataObject)?
                    !interpreter.operators.isStrictEquals((DataObject)this.valueDependencies[i], (DataObject)that.valueDependencies[i],
                            pos):
                    !Objects.equals(this.valueDependencies[i], that.valueDependencies[i]))
                return false;

        return true;
    }
}