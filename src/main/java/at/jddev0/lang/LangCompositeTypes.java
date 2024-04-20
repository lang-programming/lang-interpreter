package at.jddev0.lang;

import java.util.*;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraint;
import at.jddev0.lang.DataObject.LangObject;
import at.jddev0.lang.DataObject.StructObject;
import at.jddev0.lang.LangFunction.*;
import at.jddev0.lang.LangFunction.LangParameter.*;

/**
 * Lang-Module<br>
 * Definition of Lang composite types
 *
 * @author JDDev0
 * @version v1.0.0
 */
public class LangCompositeTypes {
	private static final DataTypeConstraint TYPE_CONSTRAINT_OPTIONAL_TEXT = DataTypeConstraint.fromAllowedTypes(Arrays.asList(
			DataType.NULL, DataType.TEXT
	));

	private static final DataTypeConstraint TYPE_CONSTRAINT_DOUBLE_ONLY = DataTypeConstraint.fromAllowedTypes(Arrays.asList(
			DataType.DOUBLE
	));

	private static final DataTypeConstraint TYPE_CONSTRAINT_INT_ONLY = DataTypeConstraint.fromAllowedTypes(Arrays.asList(
			DataType.INT
	));

	public static final StructObject STRUCT_STACK_TRACE_ELEMENT = new StructObject(new String[] {
			"$path",
			"$file",
			"$lineNumber",
			"$functionName",
			"$modulePath",
			"$moduleFile"
	}, new DataTypeConstraint[] {
			TYPE_CONSTRAINT_OPTIONAL_TEXT,
			TYPE_CONSTRAINT_OPTIONAL_TEXT,
			TYPE_CONSTRAINT_INT_ONLY,
			TYPE_CONSTRAINT_OPTIONAL_TEXT,
			TYPE_CONSTRAINT_OPTIONAL_TEXT,
			TYPE_CONSTRAINT_OPTIONAL_TEXT
	});
	public static StructObject createStackTraceElement(String path, String file, int lineNumber, String functionName,
			String modulePath, String moduleFile) {
		return new StructObject(LangCompositeTypes.STRUCT_STACK_TRACE_ELEMENT, new DataObject[] {
				new DataObject(path),
				new DataObject(file),
				new DataObject().setInt(lineNumber),
				new DataObject(functionName),
				new DataObject(modulePath),
				new DataObject(moduleFile)
		});
	}

	public static final StructObject STRUCT_PAIR = new StructObject(new String[] {
			"$first",
			"$second"
	});
	public static StructObject createPair(DataObject first, DataObject second) {
		return new StructObject(LangCompositeTypes.STRUCT_PAIR, new DataObject[] {
				first,
				second
		});
	}

	public static final LangObject CLASS_MAYBE;
	static {
		Map<String, DataObject.FunctionPointerObject[]> methods = new HashMap<>();
		Map<String, Boolean[]> methodOverrideFlags = new HashMap<>();

		methods.put("mp.isPresent", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="mp.isPresent", isMethod=true)
					@LangInfo("Returns 1 if a just &Maybe value is provided for nothing &Maybe value 0 is returned")
					@AllowedTypes(DataType.INT)
					@SuppressWarnings("unused")
					public DataObject isPresentMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						try {
							return new DataObject().setBoolean(interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID));
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("mp.isPresent", new Boolean[] {
				false
		});

		methods.put("mp.get", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="mp.get", isMethod=true)
					@LangInfo("Returns the value if a just &Maybe value is provided for nothing an INVALID_ARGUMENTS exception will be thrown")
					@SuppressWarnings("unused")
					public DataObject getMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						try {
							boolean present = interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID);
							if(!present)
								return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_ARGUMENTS, "Value is not present", SCOPE_ID);

							return new DataObject(thisObject.getMember("$value"));
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("mp.get", new Boolean[] {
				false
		});

		methods.put("mp.flatMap", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="mp.flatMap", isMethod=true)
					@LangInfo("fp.mapper is executed which must return a new &Maybe value if value of &maybe is present otherwise a new empty maybe is returned")
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject flatMapMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("fp.mapper") @AllowedTypes(DataType.FUNCTION_POINTER) DataObject mapperFuncObject
					) {
						DataObject.FunctionPointerObject mapperFunc = mapperFuncObject.getFunctionPointer();

						try {
							boolean present = interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID);
							if(present) {
								DataObject ret = interpreter.callFunctionPointer(mapperFunc, mapperFuncObject.getVariableName(), Arrays.asList(
										new DataObject(thisObject.getMember("$value"))
								), SCOPE_ID);

								if(ret == null || ret.getType() != DataType.OBJECT || thisObject.isClass() ||
										!thisObject.isInstanceOf(CLASS_MAYBE))
									return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_FUNC_PTR,
											"The value returned by fp.mapperFunc() must be of type \"&Maybe\"", SCOPE_ID);

								return ret;
							}

							return interpreter.callConstructor(CLASS_MAYBE, new ArrayList<>(0), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE,
									e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("mp.flatMap", new Boolean[] {
				false
		});

		methods.put("mp.ifPresent", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="mp.ifPresent", isMethod=true)
					@LangInfo("fp.func is executed with the value of &Maybe if the value is present otherwise fp.func will not be called")
					@AllowedTypes(DataType.VOID)
					@SuppressWarnings("unused")
					public DataObject ifPresentMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("fp.func") @AllowedTypes(DataType.FUNCTION_POINTER) DataObject funcObject
					) {
						DataObject.FunctionPointerObject func = funcObject.getFunctionPointer();

						try {
							boolean present = interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID);
							if(present) {
								interpreter.callFunctionPointer(func, funcObject.getVariableName(), Arrays.asList(
										new DataObject(thisObject.getMember("$value"))
								), SCOPE_ID);
							}

							return null;
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE,
									e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("mp.ifPresent", new Boolean[] {
				false
		});

		methods.put("op:deepCopy", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:deepCopy", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject deepCopyMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						DataObject value = thisObject.getMember("$value");
						boolean present = interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID);

						try {
							List<DataObject> arguments = new LinkedList<>();
							if(present) {
								DataObject deepCopyRet = interpreter.operators.opDeepCopy(value, -1, SCOPE_ID);
								if(deepCopyRet == null) {
									return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_ARGUMENTS,
											"The deep copy operator is not defined for " + value.getType(), SCOPE_ID);
								}

								arguments.add(deepCopyRet);
							}

							return interpreter.callConstructor(LangCompositeTypes.CLASS_MAYBE, arguments, SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:deepCopy", new Boolean[] {
				false
		});

		methods.put("op:isEquals", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:isEquals", isMethod=true)
					@AllowedTypes(DataType.INT)
					@SuppressWarnings("unused")
					public DataObject isEqualsMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$operand") @AllowedTypes(DataObject.DataType.OBJECT) DataObject operand
					) {
						LangObject operandObject = operand.getObject();

						if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_MAYBE))
							return new DataObject().setBoolean(false);

						try {
							DataObject thisValue = thisObject.getMember("$value");
							boolean thisPresent = interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID);

							DataObject operandValue = operandObject.getMember("$value");
							boolean operandPresent = interpreter.conversions.toBool(operandObject.getMember("$present"), -1, SCOPE_ID);

							return new DataObject().setBoolean(thisPresent == operandPresent && (!thisPresent ||
									interpreter.operators.isEquals(thisValue, operandValue, -1, SCOPE_ID)));
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("op:isEquals", new Boolean[] {
				false
		});

		methods.put("to:text", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="to:text", isMethod=true)
					@AllowedTypes(DataType.TEXT)
					@SuppressWarnings("unused")
					public DataObject toTextMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						try {
							DataObject value = thisObject.getMember("$value");
							boolean present = interpreter.conversions.toBool(thisObject.getMember("$present"), -1, SCOPE_ID);
							if(present)
								return new DataObject("just(" + interpreter.conversions.toText(value, -1, SCOPE_ID) + ")");

							return new DataObject("nothing");
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("to:text", new Boolean[] {
				false
		});

		DataObject.FunctionPointerObject[] constructors = new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="construct", isMethod=true)
					@LangInfo("Creates a nothing &Maybe object")
					@AllowedTypes(DataType.VOID)
					@SuppressWarnings("unused")
					public DataObject nothingConstructMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						thisObject.getMember("$value").setVoid();
						thisObject.getMember("$present").setBoolean(false);

						return null;
					}
				})),
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="construct", isMethod=true)
					@AllowedTypes(DataType.VOID)
					@LangInfo("Creates a just &Maybe object")
					@SuppressWarnings("unused")
					public DataObject justConstructMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$value") DataObject valueObject
					) {
						thisObject.getMember("$value").setData(valueObject);
						thisObject.getMember("$present").setBoolean(true);

						return null;
					}
				}))
		};

		DataObject[] staticMembers = new DataObject[] {
				new DataObject().setFunctionPointer(
						new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
							@LangFunction("fp.nothing")
							@AllowedTypes(DataType.OBJECT)
							@LangInfo("Creates a nothing &Maybe object")
							@SuppressWarnings("unused")
							public DataObject nothingStaticMethod(
									LangInterpreter interpreter, int SCOPE_ID
							) {
								return interpreter.callConstructor(CLASS_MAYBE, new ArrayList<>(0), SCOPE_ID);
							}
						}))
				).setVariableName("fp.nothing").setFinalData(true),
				new DataObject().setFunctionPointer(
						new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
							@LangFunction("fp.just")
							@AllowedTypes(DataType.OBJECT)
							@LangInfo("Creates a just &Maybe object")
							@SuppressWarnings("unused")
							public DataObject justStaticMethod(
									LangInterpreter interpreter, int SCOPE_ID,
									@LangParameter("$value") DataObject valueObject
							) {
								return interpreter.callConstructor(CLASS_MAYBE, Arrays.asList(valueObject), SCOPE_ID);
							}
						}))
				).setVariableName("fp.just").setFinalData(true)
		};

		String[] memberNames = new String[] {
				"$value",
				"$present"
		};
		DataTypeConstraint[] memberDataTypeConstraints = new DataTypeConstraint[] {
				DataObject.CONSTRAINT_NORMAL, //$value
				TYPE_CONSTRAINT_INT_ONLY      //$present
		};
		boolean[] memberFinalFlags = new boolean[] {
				true, //$value
				true  //$present
		};

		LangObject[] parentClasses = new LangObject[] {
				LangObject.OBJECT_CLASS
		};

		CLASS_MAYBE = new LangObject(staticMembers, memberNames, memberDataTypeConstraints,
				memberFinalFlags, methods, methodOverrideFlags, constructors, parentClasses);
	}

	public static final LangObject CLASS_COMPLEX;
	static {
		Map<String, DataObject.FunctionPointerObject[]> methods = new HashMap<>();
		Map<String, Boolean[]> methodOverrideFlags = new HashMap<>();

		methods.put("mp.conjugate", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="mp.conjugate", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject conjugateMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						double real = thisObject.getMember("$real").getDouble();
						double imag = thisObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(real),
									new DataObject().setDouble(-imag)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("mp.conjugate", new Boolean[] {
				false
		});

		methods.put("op:deepCopy", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:deepCopy", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject deepCopyMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						double real = thisObject.getMember("$real").getDouble();
						double imag = thisObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(real),
									new DataObject().setDouble(imag)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:deepCopy", new Boolean[] {
				false
		});

		methods.put("op:inv", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:inv", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject invMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						double real = thisObject.getMember("$real").getDouble();
						double imag = thisObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(-real),
									new DataObject().setDouble(-imag)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:inv", new Boolean[] {
				false
		});

		methods.put("op:abs", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:abs", isMethod=true)
					@AllowedTypes(DataType.DOUBLE)
					@SuppressWarnings("unused")
					public DataObject absMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						double real = thisObject.getMember("$real").getDouble();
						double imag = thisObject.getMember("$imag").getDouble();

						try {
							try {
								return new DataObject().setDouble(Math.hypot(real, imag));
							}catch(DataObject.DataTypeConstraintException e) {
								return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
							}
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:abs", new Boolean[] {
				false
		});

		methods.put("op:add", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:add", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject addMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("&z") @AllowedTypes(DataObject.DataType.OBJECT) DataObject operand
					) {
						LangObject operandObject = operand.getObject();

						if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_COMPLEX))
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be of type \"&Complex\"",
									operand.getVariableName()), SCOPE_ID);

						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandObject.getMember("$real").getDouble();
						double imagB = operandObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA + realB),
									new DataObject().setDouble(imagA + imagB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:add", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject addMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandNumber.doubleValue();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA + realB),
									new DataObject().setDouble(imagA)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:add", new Boolean[] {
				false,
				false
		});

		methods.put("op:r-add", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:r-add", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject rAddMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = operandNumber.doubleValue();

						double realB = thisObject.getMember("$real").getDouble();
						double imagB = thisObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA + realB),
									new DataObject().setDouble(imagB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:r-add", new Boolean[] {
				false
		});

		methods.put("op:sub", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:sub", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject subMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("&z") @AllowedTypes(DataObject.DataType.OBJECT) DataObject operand
					) {
						LangObject operandObject = operand.getObject();

						if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_COMPLEX))
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be of type \"&Complex\"",
									operand.getVariableName()), SCOPE_ID);

						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandObject.getMember("$real").getDouble();
						double imagB = operandObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA - realB),
									new DataObject().setDouble(imagA - imagB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:sub", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject subMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandNumber.doubleValue();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA - realB),
									new DataObject().setDouble(imagA)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:sub", new Boolean[] {
				false,
				false
		});

		methods.put("op:r-sub", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:r-sub", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject rSubMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = operandNumber.doubleValue();

						double realB = thisObject.getMember("$real").getDouble();
						double imagB = thisObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA - realB),
									new DataObject().setDouble(-imagB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:r-sub", new Boolean[] {
				false
		});

		methods.put("op:mul", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:mul", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject mulMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("&z") @AllowedTypes(DataObject.DataType.OBJECT) DataObject operand
					) {
						LangObject operandObject = operand.getObject();

						if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_COMPLEX))
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be of type \"&Complex\"",
									operand.getVariableName()), SCOPE_ID);

						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandObject.getMember("$real").getDouble();
						double imagB = operandObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA * realB - imagA * imagB),
									new DataObject().setDouble(realA * imagB + imagA * realB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:mul", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject mulMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandNumber.doubleValue();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA * realB),
									new DataObject().setDouble(imagA * realB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:mul", new Boolean[] {
				false,
				false
		});

		methods.put("op:r-mul", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:r-mul", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject rMulMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = operandNumber.doubleValue();

						double realB = thisObject.getMember("$real").getDouble();
						double imagB = thisObject.getMember("$imag").getDouble();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA * realB),
									new DataObject().setDouble(realA * imagB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:r-mul", new Boolean[] {
				false
		});

		methods.put("op:div", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:div", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject divMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("&z") @AllowedTypes(DataObject.DataType.OBJECT) DataObject operand
					) {
						LangObject operandObject = operand.getObject();

						if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_COMPLEX))
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be of type \"&Complex\"",
									operand.getVariableName()), SCOPE_ID);

						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandObject.getMember("$real").getDouble();
						double imagB = operandObject.getMember("$imag").getDouble();

						double realNumerator = realA * realB + imagA * imagB;
						double imagNumerator = imagA * realB - realA * imagB;

						double denominator = realB * realB + imagB * imagB;

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realNumerator / denominator),
									new DataObject().setDouble(imagNumerator / denominator)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:div", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject divMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = thisObject.getMember("$real").getDouble();
						double imagA = thisObject.getMember("$imag").getDouble();

						double realB = operandNumber.doubleValue();

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realA / realB),
									new DataObject().setDouble(imagA / realB)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:div", new Boolean[] {
				false,
				false
		});

		methods.put("op:r-div", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:r-div", isMethod=true)
					@AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject rDivMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$x") @NumberValue Number operandNumber
					) {
						double realA = operandNumber.doubleValue();

						double realB = thisObject.getMember("$real").getDouble();
						double imagB = thisObject.getMember("$imag").getDouble();

						double realNumerator = realA * realB;
						double imagNumerator = - realA * imagB;

						double denominator = realB * realB + imagB * imagB;

						try {
							return interpreter.callConstructor(LangCompositeTypes.CLASS_COMPLEX, LangUtils.asListWithArgumentSeparators(
									new DataObject().setDouble(realNumerator / denominator),
									new DataObject().setDouble(imagNumerator / denominator)
							), SCOPE_ID);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				}))
		});
		methodOverrideFlags.put("op:r-div", new Boolean[] {
				false
		});

		methods.put("op:isEquals", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:isEquals", isMethod=true)
					@AllowedTypes(DataType.INT)
					@SuppressWarnings("unused")
					public DataObject isEqualsMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$operand") DataObject operand
					) {
						try {
							double thisReal = thisObject.getMember("$real").getDouble();
							double thisImag = thisObject.getMember("$imag").getDouble();

							if(thisImag == 0) {
								Number number = interpreter.conversions.toNumber(operand, -1, SCOPE_ID);
								if(number != null) {
									return new DataObject().setBoolean(thisReal == number.doubleValue());
								}
							}

							if(operand.getType() != DataType.OBJECT)
								return new DataObject().setBoolean(false);

							LangObject operandObject = operand.getObject();

							if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_COMPLEX))
								return new DataObject().setBoolean(false);

							double operandReal = operandObject.getMember("$real").getDouble();
							double operandImag = operandObject.getMember("$imag").getDouble();

							return new DataObject().setBoolean(thisReal == operandReal && thisImag == operandImag);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("op:isEquals", new Boolean[] {
				false
		});

		methods.put("op:r-isEquals", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="op:r-isEquals", isMethod=true)
					@AllowedTypes(DataType.INT)
					@SuppressWarnings("unused")
					public DataObject rIsEqualsMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$operand") DataObject operand
					) {
						try {
							double thisReal = thisObject.getMember("$real").getDouble();
							double thisImag = thisObject.getMember("$imag").getDouble();

							if(thisImag == 0) {
								Number number = interpreter.conversions.toNumber(operand, -1, SCOPE_ID);
								if(number != null) {
									return new DataObject().setBoolean(number.doubleValue() == thisReal);
								}
							}

							if(operand.getType() != DataType.OBJECT)
								return new DataObject().setBoolean(false);

							LangObject operandObject = operand.getObject();

							if(operandObject.isClass() || !operandObject.isInstanceOf(LangCompositeTypes.CLASS_COMPLEX))
								return new DataObject().setBoolean(false);

							double operandReal = operandObject.getMember("$real").getDouble();
							double operandImag = operandObject.getMember("$imag").getDouble();

							return new DataObject().setBoolean(operandReal == thisReal && operandImag == thisImag);
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("op:r-isEquals", new Boolean[] {
				false
		});

		methods.put("to:text", new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="to:text", isMethod=true)
					@AllowedTypes(DataType.TEXT)
					@SuppressWarnings("unused")
					public DataObject toTextMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						try {
							double real = thisObject.getMember("$real").getDouble();
							double imag = thisObject.getMember("$imag").getDouble();

							return new DataObject(real + " + " + imag + "i");
						}catch(DataObject.DataTypeConstraintException e) {
							return interpreter.setErrnoErrorObject(LangInterpreter.InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), SCOPE_ID);
						}
					}
				})),
		});
		methodOverrideFlags.put("to:text", new Boolean[] {
				false
		});

		DataObject.FunctionPointerObject[] constructors = new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="construct", isMethod=true)
					@LangInfo("Creates a &Complex object")
					@AllowedTypes(DataType.VOID)
					@SuppressWarnings("unused")
					public DataObject constructMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangParameter("$real") @NumberValue Number realNumber,
							@LangParameter("$imag") @NumberValue Number imagNumber
					) {
						thisObject.getMember("$real").setDouble(realNumber.doubleValue());
						thisObject.getMember("$imag").setDouble(imagNumber.doubleValue());

						return null;
					}
				}))
		};

		DataObject[] staticMembers = new DataObject[] {

		};

		String[] memberNames = new String[] {
				"$real",
				"$imag"
		};
		DataTypeConstraint[] memberDataTypeConstraints = new DataTypeConstraint[] {
				TYPE_CONSTRAINT_DOUBLE_ONLY, //$real
				TYPE_CONSTRAINT_DOUBLE_ONLY  //$imag
		};
		boolean[] memberFinalFlags = new boolean[] {
				true, //$value
				true  //$present
		};

		LangObject[] parentClasses = new LangObject[] {
				LangObject.OBJECT_CLASS
		};

		CLASS_COMPLEX = new LangObject(staticMembers, memberNames, memberDataTypeConstraints,
				memberFinalFlags, methods, methodOverrideFlags, constructors, parentClasses);
	}

	private LangCompositeTypes() {}
}