package me.jddev0.module.lang;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.jddev0.module.lang.DataObject.DataTypeConstraint;
import me.jddev0.module.lang.DataObject.DataTypeConstraintException;
import me.jddev0.module.lang.LangFunction.*;
import me.jddev0.module.lang.LangFunction.LangParameter.*;
import me.jddev0.module.lang.LangInterpreter.InterpretingError;

public class LangNativeFunction implements LangPredefinedFunctionObject {
	//TODO add support for native function overloading [Change class -> store list of public static InternalFunction and move all parameters into internal class, define callFunc for outer class and add getFunctions]
	
	//TODO remove and add as parameter to call Function instance to call
	public final LangInterpreter interpreter;
	
	public final String functionName;
	public final List<DataObject> parameterList;
	public final List<DataTypeConstraint> paramaterDataTypeConstraintList;
	public final List<Integer> numberTypeIndices;
	public final DataTypeConstraint returnValueTypeConstraint;
	public final Object instance;
	public final Method functionBody;
	public final boolean hasInterpreterParameter;
	
	public final boolean linkerFunction;
	public final boolean deprecated;
	public final String deprecatedRemoveVersion;
	public final String deprecatedReplacementFunction;
	
	public static Map<String, LangNativeFunction> getLangFunctionsOfClass(LangInterpreter interpreter, Object instance, Class<?> clazz)
			throws IllegalArgumentException, DataTypeConstraintException {
		Map<String, LangNativeFunction> langNativeFunctions = new HashMap<>();
		
		for(Method method:clazz.getDeclaredMethods()) {
			//Add static methods if instance is null else add non-static methods
			if(instance == null ^ Modifier.isStatic(method.getModifiers()))
				continue;
			
			if(method.isAnnotationPresent(LangFunction.class)) {
				LangNativeFunction langNativeFunction = create(interpreter, instance, method);
				String functionName = langNativeFunction.getFunctionName();
				
				/*if(langNativeFunctions.containsKey(functionName)) {
					//TODO add support for native function overloading [Change class -> store list of public static InternalFunction and move all parameters into internal class, define
					//     callFunc for outer class and add getFunctions]
				}*/
				
				langNativeFunctions.put(functionName, langNativeFunction);
			}
		}
		
		return langNativeFunctions;
	}
	
	public static LangNativeFunction create(LangInterpreter interpreter, Object instance, Method functionBody)
			throws IllegalArgumentException, DataTypeConstraintException {
		LangFunction langFunction = functionBody.getAnnotation(LangFunction.class);
		if(langFunction == null)
			throw new IllegalArgumentException("Method must be annotated with @LangFunction");
		
		String functionName = langFunction.value();
		
		boolean linkerFunction = langFunction.isLinkerFunction();
		
		boolean deprecated = langFunction.isDeprecated();
		String deprecatedRemoveVersion = langFunction.getDeprecatedRemoveVersion();
		if(deprecatedRemoveVersion.equals(""))
			deprecatedRemoveVersion = null;
		String deprecatedReplacementFunction = langFunction.getDeprecatedReplacementFunction();
		if(deprecatedReplacementFunction.equals(""))
			deprecatedReplacementFunction = null;
		
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
		if(parameters.length == 0)
			throw new IllegalArgumentException("Method must have at least one parameter (int SCOPE_ID)");
		
		Parameter firstParam = parameters[0];
		boolean hasInterpreterParameter = firstParam.getType().isAssignableFrom(LangInterpreter.class);
		
		Parameter secondParam = hasInterpreterParameter && parameters.length >= 2?parameters[1]:null;
		
		if(hasInterpreterParameter?
				secondParam == null || !secondParam.getType().isAssignableFrom(int.class):
					!firstParam.getType().isAssignableFrom(int.class))
			throw new IllegalArgumentException("The method must start with (LangInterpreter interpreter, int SCOPE_ID) or (int SCOPE_ID)");
		
		int diff = hasInterpreterParameter?2:1;
		List<DataObject> parameterList = new ArrayList<>(parameters.length - diff);
		List<DataTypeConstraint> paramaterDataTypeConstraintList = new ArrayList<>(parameters.length - diff);
		List<Integer> numberTypeIndices = new ArrayList<>();
		
		for(int i = diff;i < parameters.length;i++) {
			Parameter parameter = parameters[i];
			
			LangParameter langParameter = parameter.getAnnotation(LangParameter.class);
			if(langParameter == null || !parameter.getType().isAssignableFrom(DataObject.class))
				throw new IllegalArgumentException("After the SCOPE_ID parameter only DataObject parameters with the @LangParameter annotation are allowed");
			
			String variableName = langParameter.value();
			
			boolean isNumberValue = parameter.isAnnotationPresent(NumberValue.class);
			if(isNumberValue)
				numberTypeIndices.add(i - diff);
			
			DataTypeConstraint typeConstraint = DataObject.CONSTRAINT_NORMAL;
			
			allowedTypes = parameter.getAnnotation(AllowedTypes.class);
			if(allowedTypes != null)
				typeConstraint = DataTypeConstraint.fromAllowedTypes(Arrays.asList(allowedTypes.value()));
			
			notAllowedTypes = parameter.getAnnotation(NotAllowedTypes.class);
			if(notAllowedTypes != null)
				typeConstraint = DataTypeConstraint.fromNotAllowedTypes(Arrays.asList(notAllowedTypes.value()));
			
			if((allowedTypes != null && notAllowedTypes != null) || (isNumberValue && (allowedTypes != null || notAllowedTypes != null)))
				throw new IllegalArgumentException("DataObject parameter must be annotated with at most one of @AllowedTypes, @NotAllowedTypes, or @NumberValue");
			
			parameterList.add(new DataObject().setVariableName(variableName));
			paramaterDataTypeConstraintList.add(typeConstraint);
			
			//TODO error if parameter if name already exists or if name is invalid
		}
		
		return new LangNativeFunction(interpreter, functionName, parameterList, paramaterDataTypeConstraintList,
				numberTypeIndices, returnValueTypeConstraint, instance, functionBody, hasInterpreterParameter, linkerFunction, deprecated,
				deprecatedRemoveVersion, deprecatedReplacementFunction);
	}
	
	//TODO add helper method to get list of LangNativeFunctions from Class (With instance object: if null static methods else non-static methods)
	
	/**
	 * @param instance Null for static method
	 */
	private LangNativeFunction(LangInterpreter interpreter, String functionName, List<DataObject> parameterList,
			List<DataTypeConstraint> paramaterDataTypeConstraintList, List<Integer> numberTypeIndices,
			DataTypeConstraint returnValueTypeConstraint, Object instance, Method functionBody, boolean hasInterpreterParameter,
			boolean linkerFunction, boolean deprecated, String deprecatedRemoveVersion, String deprecatedReplacementFunction) {
		this.interpreter = interpreter;
		this.functionName = functionName;
		this.parameterList = parameterList;
		this.paramaterDataTypeConstraintList = paramaterDataTypeConstraintList;
		this.numberTypeIndices = numberTypeIndices;
		this.returnValueTypeConstraint = returnValueTypeConstraint;
		this.instance = instance;
		this.functionBody = functionBody;
		this.hasInterpreterParameter = hasInterpreterParameter;
		this.linkerFunction = linkerFunction;
		this.deprecated = deprecated;
		this.deprecatedRemoveVersion = deprecatedRemoveVersion;
		this.deprecatedReplacementFunction = deprecatedReplacementFunction;
	}
	
	@Override
	public DataObject callFunc(List<DataObject> argumentList, int SCOPE_ID) {
		//TODO remove checks from this method and move to directly to interpreter
		//TODO add varargs support (If variable name ends with "...") [Type will be array or text]
		//TODO add call by pointer support (If variable name is "$[...]")
		
		int argCount = parameterList.size();
		
		List<DataObject> combinedArgumentList = LangUtils.combineArgumentsWithoutArgumentSeparators(argumentList);
		if(combinedArgumentList.size() < argCount)
			return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Not enough arguments (%s needed)", argCount), SCOPE_ID);
		if(combinedArgumentList.size() > argCount)
			return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Too many arguments (%s needed)", argCount), SCOPE_ID);
		
		int diff = hasInterpreterParameter?2:1;
		Object[] methodArgumentList = new Object[diff + argCount];
		if(hasInterpreterParameter) {
			methodArgumentList[0] = interpreter;
			methodArgumentList[1] = SCOPE_ID;
		}else {
			methodArgumentList[0] = SCOPE_ID;
		}
		for(int i = 0;i < argCount;i++) {
			if(!paramaterDataTypeConstraintList.get(i).isTypeAllowed(combinedArgumentList.get(i).getType()))
				return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("The type of argument %d must be one of %s", i + 1,
						paramaterDataTypeConstraintList.get(i).getAllowedTypes()), SCOPE_ID);
			
			if(numberTypeIndices.contains(i) && combinedArgumentList.get(i).toNumber() == null)
				return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM, String.format("Argument %d must be a number", i + 1), SCOPE_ID);
			
			//TODO remove varargs "..." and call by pointer
			String variableName = parameterList.get(i).getVariableName();
			
			methodArgumentList[i + diff] = new DataObject(combinedArgumentList.get(i)).setVariableName(variableName).
					setTypeConstraint(paramaterDataTypeConstraintList.get(i));
		}
		
		try {
			//TODO check return type in LangInterpreter
			return (DataObject)functionBody.invoke(instance, methodArgumentList);
		}catch(IllegalAccessException|IllegalArgumentException e) {
			return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR,
					"Native Error (\"" + e.getClass().getSimpleName() + "\"): " + e.getMessage(), SCOPE_ID);
		}catch(InvocationTargetException e) {
			Throwable t = e.getTargetException();
			if(t == null)
				t = e;
			
			return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR,
					"Native Error (\"" + t.getClass().getSimpleName() + "\"): " + t.getMessage(), SCOPE_ID);
		}
	}
	
	public String getFunctionName() {
		return functionName;
	}
	
	public List<DataObject> getParameterList() {
		return parameterList;
	}
	
	public List<DataTypeConstraint> getParamaterDataTypeConstraintList() {
		return paramaterDataTypeConstraintList;
	}
	
	public List<Integer> getNumberTypeIndices() {
		return numberTypeIndices;
	}
	
	public DataTypeConstraint getReturnValueTypeConstraint() {
		return returnValueTypeConstraint;
	}
	
	@Override
	public boolean isLinkerFunction() {
		return linkerFunction;
	}
	
	@Override
	public boolean isDeprecated() {
		return deprecated;
	}
	
	@Override
	public String getDeprecatedRemoveVersion() {
		return deprecatedRemoveVersion;
	}
	
	@Override
	public String getDeprecatedReplacementFunction() {
		return deprecatedReplacementFunction;
	}
}