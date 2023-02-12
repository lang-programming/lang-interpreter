package me.jddev0.module.lang;

import java.util.List;

/**
 * Lang-Module<br>
 * Lang native module<br>
 * The default constructor may be public and will be called for the instantiation
 * 
 * @author JDDev0
 * @version v1.0.0
 */
public abstract class LangNativeModule {
	/**
	 * The value for this field will be injected after the instantiation with the default constructor
	 */
	protected LangInterpreter interpreter;
	/**
	 * The value for this field will be injected after the instantiation with the default constructor
	 */
	protected LangModule module;
	
	protected final void exportFunction(String functionName, LangPredefinedFunctionObject func) {
		if(!module.isLoad())
			throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");
		
		for(int i = 0;i < functionName.length();i++) {
			char c = functionName.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
				continue;
			
			throw new RuntimeException("The function name may only contain alphanumeric characters and underscores (_)");
		}
		
		module.getExportedFunctions().add(functionName);
		
		//Create function object container to force function to be a normal function
		interpreter.funcs.put(functionName, (argumentList, SCOPE_ID) -> {
			return func.callFunc(argumentList, SCOPE_ID);
		});
	}
	
	protected final void exportLinkerFunction(String functionName, LangPredefinedFunctionObject func) {
		if(!module.isLoad())
			throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");
		
		for(int i = 0;i < functionName.length();i++) {
			char c = functionName.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
				continue;
			
			throw new RuntimeException("The function name may only contain alphanumeric characters and underscores (_)");
		}
		
		module.getExportedFunctions().add(functionName);
		
		//Create function object container to force function to be a linker function
		interpreter.funcs.put(functionName, new LangPredefinedFunctionObject() {
			@Override
			public DataObject callFunc(List<DataObject> argumentList, final int SCOPE_ID) {
				return func.callFunc(argumentList, SCOPE_ID);
			}
			
			@Override
			public boolean isLinkerFunction() {
				return true;
			}
		});
	}
	
	protected final void exportNormalVariableFinal(String variableName, DataObject value) {
		exportNormalVariable(variableName, value, true);
	}
	protected final void exportNormalVariable(String variableName, DataObject value) {
		exportNormalVariable(variableName, value, false);
	}
	protected final void exportNormalVariable(String variableName, DataObject value, boolean finalData) {
		if(!module.isLoad())
			throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");
		
		for(int i = 0;i < variableName.length();i++) {
			char c = variableName.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
				continue;
			
			throw new RuntimeException("The variable name may only contain alphanumeric characters and underscores (_)");
		}
		
		if(variableName.startsWith("LANG"))
			throw new RuntimeException("The variable name may not start with LANG");
		
		variableName = "$" + variableName;
		value = new DataObject(value).setFinalData(finalData).setVariableName(variableName);
		
		module.getExportedVariables().put(variableName, value);
	}
	
	protected final void exportCollectionVariableFinal(String variableName, DataObject value) {
		exportCollectionVariable(variableName, value, true);
	}
	protected final void exportCollectionVariable(String variableName, DataObject value) {
		exportCollectionVariable(variableName, value, false);
	}
	private final void exportCollectionVariable(String variableName, DataObject value, boolean finalData) {
		if(!module.isLoad())
			throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");
		
		for(int i = 0;i < variableName.length();i++) {
			char c = variableName.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
				continue;
			
			throw new RuntimeException("The variable name may only contain alphanumeric characters and underscores (_)");
		}
		
		if(variableName.startsWith("LANG"))
			throw new RuntimeException("The variable name may not start with LANG");
		
		variableName = "&" + variableName;
		value = new DataObject(value).setFinalData(finalData).setVariableName(variableName);
		
		module.getExportedVariables().put(variableName, value);
	}
	
	protected final void exportFunctionPointerVariableFinal(String variableName, DataObject value) {
		exportFunctionPointerVariable(variableName, value, true);
	}
	protected final void exportFunctionPointerVariable(String variableName, DataObject value) {
		exportFunctionPointerVariable(variableName, value, false);
	}
	private final void exportFunctionPointerVariable(String variableName, DataObject value, boolean finalData) {
		if(!module.isLoad())
			throw new RuntimeException("This method may only be used inside a module which is in the \"load\" state");
		
		for(int i = 0;i < variableName.length();i++) {
			char c = variableName.charAt(i);
			if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
				continue;
			
			throw new RuntimeException("The variable name may only contain alphanumeric characters and underscores (_)");
		}
		
		variableName = "fp." + variableName;
		value = new DataObject(value).setFinalData(finalData).setVariableName(variableName);
		
		module.getExportedVariables().put(variableName, value);
	}
	
	/**
	 * Will be called if the module is loaded
	 * 
	 * @param args provided to "func.loadModule()" [modulePath included] separated with ARGUMENT_SEPARATORs
	 * @param SCOPE_ID the scope id for the module load execution
	 * @return Will be returned for the "linker.loadModule()" call
	 */
	public abstract DataObject load(List<DataObject> args, final int SCOPE_ID);
	
	/**
	 * Will be called if the module is unloaded
	 * 
	 * @param args provided to "func.unloadModule()" [modulePath included] separated with ARGUMENT_SEPARATORs
	 * @param SCOPE_ID the scope id for the module unload execution
	 * @return Will be returned for the "linker.unloadModule()" call
	 */
	public abstract DataObject unload(List<DataObject> args, final int SCOPE_ID);
}