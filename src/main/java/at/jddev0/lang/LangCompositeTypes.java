package at.jddev0.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraint;
import at.jddev0.lang.DataObject.LangObject;
import at.jddev0.lang.DataObject.StructObject;

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
	
	public static final StructObject STRUCT_COMPLEX = new StructObject(new String[] {
			"$real",
			"$imag"
	}, new DataTypeConstraint[] {
			TYPE_CONSTRAINT_DOUBLE_ONLY,
			TYPE_CONSTRAINT_DOUBLE_ONLY
	});
	public static StructObject createComplex(double real, double imag) {
		return new StructObject(LangCompositeTypes.STRUCT_COMPLEX, new DataObject[] {
				new DataObject().setDouble(real),
				new DataObject().setDouble(imag)
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
					@LangFunction.LangInfo("Returns 1 if a just &maybe value is provided for nothing &maybe value 0 is returned")
					@LangFunction.AllowedTypes(DataType.INT)
					@SuppressWarnings("unused")
					public DataObject isPresentMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						try {
							return new DataObject().setBoolean(thisObject.getMember("$present").getBoolean());
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
					@LangFunction.LangInfo("Returns the value if a just &maybe value is provided for nothing an INVALID_ARGUMENTS exception will be thrown")
					@SuppressWarnings("unused")
					public DataObject getMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						try {
							boolean present = thisObject.getMember("$present").getBoolean();
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
					@LangFunction.LangInfo("fp.mapper is executed which must return a new &Maybe value if value of &maybe is present otherwise a new empty maybe is returned")
					@LangFunction.AllowedTypes(DataType.OBJECT)
					@SuppressWarnings("unused")
					public DataObject flatMapMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangFunction.LangParameter("fp.mapper") @LangFunction.AllowedTypes(DataType.FUNCTION_POINTER) DataObject mapperFuncObject
					) {
						DataObject.FunctionPointerObject mapperFunc = mapperFuncObject.getFunctionPointer();

						try {
							boolean present = thisObject.getMember("$present").getBoolean();
							if(present) {
								DataObject ret = interpreter.callFunctionPointer(mapperFunc, mapperFuncObject.getVariableName(), Arrays.asList(
										new DataObject(thisObject.getMember("$value"))
								), SCOPE_ID);

								if(ret == null || ret.getType() != DataType.OBJECT || thisObject.isClass() ||
										!thisObject.getClassBaseDefinition().equals(CLASS_MAYBE))
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

		DataObject.FunctionPointerObject[] constructors = new DataObject.FunctionPointerObject[] {
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="construct", isMethod=true)
					@LangFunction.LangInfo("Creates a nothing &Maybe object")
					@LangFunction.AllowedTypes(DataType.VOID)
					@SuppressWarnings("unused")
					public DataObject nothingConstructMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject
					) {
						thisObject.getMember("$value").setNull();
						thisObject.getMember("$present").setBoolean(false);

						return null;
					}
				})),
				new DataObject.FunctionPointerObject(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
					@LangFunction(value="construct", isMethod=true)
					@LangFunction.AllowedTypes(DataType.VOID)
					@LangFunction.LangInfo("Creates a just &Maybe object")
					@SuppressWarnings("unused")
					public DataObject justConstructMethod(
							LangInterpreter interpreter, int SCOPE_ID, LangObject thisObject,
							@LangFunction.LangParameter("$value") DataObject valueObject
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
							@LangFunction.AllowedTypes(DataType.OBJECT)
							@LangFunction.LangInfo("Creates a nothing &Maybe object")
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
							@LangFunction.AllowedTypes(DataType.OBJECT)
							@LangFunction.LangInfo("Creates a just &Maybe object")
							@SuppressWarnings("unused")
							public DataObject justStaticMethod(
									LangInterpreter interpreter, int SCOPE_ID,
									@LangFunction.LangParameter("$value") DataObject valueObject
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

	private LangCompositeTypes() {}
}