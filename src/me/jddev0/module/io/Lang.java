package me.jddev0.module.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import me.jddev0.module.lang.AbstractSyntaxTree;
import me.jddev0.module.lang.AbstractSyntaxTree.AssignmentNode;
import me.jddev0.module.lang.AbstractSyntaxTree.FunctionDefinitionNode;
import me.jddev0.module.lang.AbstractSyntaxTree.Node;
import me.jddev0.module.lang.AbstractSyntaxTree.NodeType;
import me.jddev0.module.lang.AbstractSyntaxTree.ParsingErrorNode;
import me.jddev0.module.lang.AbstractSyntaxTree.VariableNameNode;
import me.jddev0.module.lang.LangExternalFunctionObject;
import me.jddev0.module.lang.LangInterpreter;
import me.jddev0.module.lang.LangInterpreter.InterpretingError;
import me.jddev0.module.lang.LangParser.ParsingError;
import me.jddev0.module.lang.LangPredefinedFunctionObject;
import me.jddev0.module.lang.LangUtils;

@Deprecated
/**
 * IO-Module<br>
 * Read and write Lang-Files in <b>UTF-8</b>
 * <br>
 * 
 * @author JDDev0
 * @version v1.0.0
 * @deprecated Will be removed in v1.2.0
 */
public final class Lang {
	//Lang cache
	private final static Map<String, String> LANG_CACHE = new HashMap<>(); //translation key = translation value
	private static String lastCachedLangFileName;
	
	private Lang() {}
	
	/**
	 * @return Returns all available lang files
	 */
	public static List<String> getLangFiles(String langPath, LangPlatformAPI langPlatformAPI) {
		return langPlatformAPI.getLangFiles(langPath);
	}
	
	/**
	 * Without interpreter: Only lang translations will be read without any other features (Used for reading written lang file)<br>
	 * Call getCached... methods afterwards for retrieving certain lang translations
	 * @return Returns all translations of <b>langFile</b>
	 */
	public static Map<String, String> getTranslationMapWithoutInterpreter(String langFile, boolean reloadNotFromChache, TerminalIO term, LangPlatformAPI langPlatformAPI) throws IOException {
		synchronized(LANG_CACHE) {
			if(langFile.equals(lastCachedLangFileName) && !reloadNotFromChache) {
				return new HashMap<>(LANG_CACHE);
			}else {
				LANG_CACHE.clear();
				lastCachedLangFileName = langFile;
			}
			
			try(BufferedReader reader = langPlatformAPI.getLangReader(langFile)) {
				//Cache lang translations
				reader.lines().forEach(line -> {
					if(!line.contains(" = "))
						return;
					
					String[] langTranslation = line.split(" = ", 2);
					LANG_CACHE.put(langTranslation[0], langTranslation[1].replace("\\n", "\n"));
				});
			}
			return new HashMap<>(LANG_CACHE);
		}
	}
	
	/**
	 * @return Returns all translations of <b>langFile</b>
	 */
	public static Map<String, String> getTranslationMap(String langFile, boolean reloadNotFromChache, TerminalIO term, LangPlatformAPI langPlatformAPI) throws IOException {
		return getTranslationMap(langFile, reloadNotFromChache, term, langPlatformAPI, null);
	}

	/**
	 * @return Returns all translations of <b>langFile</b>
	 */
	public static Map<String, String> getTranslationMap(String langFile, boolean reloadNotFromChache, TerminalIO term, LangPlatformAPI langPlatformAPI, String[] langArgs) throws IOException {
		synchronized(LANG_CACHE) {
			if(langFile.equals(lastCachedLangFileName) && !reloadNotFromChache) {
				return new HashMap<>(LANG_CACHE);
			}else {
				LANG_CACHE.clear();
				lastCachedLangFileName = langFile;
			}
			
			//Set path for Interpreter
			String pathLangFile = langPlatformAPI.getLangPath(langFile);
			
			//Create new Interpreter instance
			LangInterpreter interpreter = new LangInterpreter(pathLangFile, langPlatformAPI.getLangFileName(langFile), term, langPlatformAPI, langArgs);
			
			try(BufferedReader reader = langPlatformAPI.getLangReader(langFile)) {
				interpreter.interpretLines(reader);
			}
			
			//Cache lang translations
			LANG_CACHE.putAll(interpreter.getData().get(0).lang);
			return new HashMap<>(LANG_CACHE);
		}
	}
	
	/**
	 * @return Returns translation <b>key</b> of <b>langFile</b><br>
	 * If key wasn't found -> <code>return key;</code>
	 */
	public static String getTranslation(String langFile, String key, LangPlatformAPI langPlatformAPI) throws IOException {
		synchronized(LANG_CACHE) {
			if(getTranslationMap(langFile, false, null, langPlatformAPI).get(key) == null)
				return key;
			
			return getTranslationMap(langFile, false, null, langPlatformAPI).get(key);
		}
	}
	
	/**
	 * @return Returns translation <b>key</b> of <b>langFile</b><br>
	 * If key wasn't found -> <code>return key;</code>
	 */
	public static String getTranslationFormat(String langFile, String key, LangPlatformAPI langPlatformAPI, Object... args) throws IOException {
		synchronized(LANG_CACHE) {
			if(getTranslation(langFile, key, langPlatformAPI) == null)
				return key;
			
			try {
				return String.format(getTranslation(langFile, key, langPlatformAPI), args);
			}catch(Exception e) {
				return getTranslation(langFile, key, langPlatformAPI);
			}
		}
	}
	
	/**
	 * @return Returns language name of <b>langFile</b><br>
	 */
	public static String getLangName(String langFile, LangPlatformAPI langPlatformAPI) throws IOException {
		return getTranslation(langFile, "lang.name", langPlatformAPI);
	}
	
	/**
	 * @return Returns language version of <b>langFile</b><br>
	 */
	public static String getLangVersion(String langFile, LangPlatformAPI langPlatformAPI) throws IOException {
		return getTranslation(langFile, "lang.version", langPlatformAPI);
	}
	
	/**
	 * Writes all translations of <b>translationMap</b> to <b>langFile</b>
	 * 
	 * @return Returns true if successful, false otherwise
	 */
	public static boolean write(File langFile, Map<String, String> translationMap, TerminalIO term, LangPlatformAPI langPlatformAPI) {
		synchronized(LANG_CACHE) {
			LANG_CACHE.clear();
			
			return langPlatformAPI.writeLangFile(langFile, translationMap, term);
		}
	}
	
	/**
	 * @return Returns all translations from the cach
	 */
	public static Map<String, String> getCachedTranslationMap() {
		synchronized(LANG_CACHE) {
			return new HashMap<>(LANG_CACHE);
		}
	}
	
	/**
	 * @return Returns translation <b>key</b> from the cache<br>
	 * If key wasn't found -> <code>return key;</code>
	 */
	public static String getCachedTranslation(String key) {
		synchronized(LANG_CACHE) {
			if(getCachedTranslationMap().get(key) == null)
				return key;
			
			return getCachedTranslationMap().get(key);
		}
	}
	
	/**
	 * @return Returns translation <b>key</b> from the cache<br>
	 * If key wasn't found -> <code>return key;</code>
	 */
	public static String getCachedTranslationFormat(String key, Object... args) {
		synchronized(LANG_CACHE) {
			if(getCachedTranslation(key) == null)
				return key;
			
			try {
				return String.format(getCachedTranslation(key), args);
			}catch(Exception e) {
				return getCachedTranslation(key);
			}
		}
	}
	
	/**
	 * @return Returns language name from cache<br>
	 */
	public static String getCachedLangName() {
		return getCachedTranslation("lang.name");
	}
	
	/**
	 * @return Returns language version from cache<br>
	 */
	public static String getCachedLangVersion() {
		return getCachedTranslation("lang.version");
	}
	
	/**
	 * Writes all translations from cache to <b>langFile</b>
	 * 
	 * @return Returns true if successful, false otherwise
	 */
	public static boolean writeCache(File langFile, TerminalIO term, LangPlatformAPI langPlatformAPI) {
		return langPlatformAPI.writeLangFile(langFile, getCachedTranslationMap(), term);
	}
	
	/**
	 * Clears the lang translation cache
	 */
	public static void clearCache() {
		synchronized(LANG_CACHE) {
			LANG_CACHE.clear();
			lastCachedLangFileName = null;
		}
	}
	
	public static LangInterpreter.LangInterpreterInterface createInterpreterInterface(String langFile, boolean writeToCache,
	TerminalIO term, LangPlatformAPI langPlatformAPI, String[] langArgs) throws IOException {
		if(writeToCache) {
			synchronized(LANG_CACHE) {
				LANG_CACHE.clear();
				lastCachedLangFileName = langFile;
			}
		}
		
		String pathLangFile = langPlatformAPI.getLangPath(langFile);
		
		LangInterpreter interpreter = new LangInterpreter(pathLangFile, langPlatformAPI.getLangFileName(langFile), term, langPlatformAPI, langArgs);
		
		try(BufferedReader reader = langPlatformAPI.getLangReader(langFile)) {
			interpreter.interpretLines(reader);
		}
		
		if(writeToCache) {
			synchronized(LANG_CACHE) {
				//Cache lang translations
				LANG_CACHE.putAll(interpreter.getData().get(0).lang);
			}
		}
		
		return new LangInterpreter.LangInterpreterInterface(interpreter);
	}
	public static LangInterpreter.LangInterpreterInterface createInterpreterInterface(String langFile, boolean writeToCache, TerminalIO term, LangPlatformAPI langPlatformAPI) throws IOException {
		return createInterpreterInterface(langFile, writeToCache, term, langPlatformAPI, null);
	}
	public static LangInterpreter.LangInterpreterInterface createInterpreterInterface(String langFile, TerminalIO term, LangPlatformAPI langPlatformAPI, String[] langArgs) throws IOException {
		return createInterpreterInterface(langFile, false, term, langPlatformAPI, langArgs);
	}
	public static LangInterpreter.LangInterpreterInterface createInterpreterInterface(String langFile, TerminalIO term, LangPlatformAPI langPlatformAPI) throws IOException {
		return createInterpreterInterface(langFile, false, term, langPlatformAPI);
	}
	public static LangInterpreter.LangInterpreterInterface createInterpreterInterface(TerminalIO term, LangPlatformAPI langPlatformAPI, String[] langArgs) {
		return new LangInterpreter.LangInterpreterInterface(new LangInterpreter(new File("").getAbsolutePath(), term, langPlatformAPI, langArgs));
	}
	public static LangInterpreter.LangInterpreterInterface createInterpreterInterface(TerminalIO term, LangPlatformAPI langPlatformAPI) {
		return createInterpreterInterface(term, langPlatformAPI, null);
	}
	
	//DEPRACTED methods and classes
	/**
	 * @deprecated Will be removed in v1.2.0
	 * @return Returns all available lang files
	 */
	@Deprecated
	public static List<String> getLangFiles(String langPath) {
		List<String> files = new LinkedList<>();
		
		String[] in = new File(langPath).list();
		if(in != null) {
			for(String str:in) {
				File f = new File(langPath, str);
				if(!f.isDirectory() && f.getName().toLowerCase().endsWith(".lang")) {
					files.add(f.getPath());
				}
			}
		}
		
		return files;
	}
	/**
	 * @deprecated Will be removed in v1.2.0
	 * @return Returns all translations of <b>langFile</b>
	 */
	@Deprecated
	public static Map<String, String> getTranslationMap(String langFile, boolean reload, TerminalIO term) throws Exception {
		return getTranslationMap(langFile, reload, term, new LangPlatformAPI());
	}
	/**
	 * @deprecated Will be removed in v1.2.0
	 * @return Returns translation <b>key</b> of <b>langFile</b><br>
	 * If key wasn't found -> <code>return key;</code>
	 */
	@Deprecated
	public static String getTranslation(String langFile, String key) throws Exception {
		synchronized(LANG_CACHE) {
			if(getTranslationMap(langFile, false, null).get(key) == null) {
				return key;
			}
			
			return getTranslationMap(langFile, false, null).get(key);
		}
	}
	/**
	 * @deprecated Will be removed in v1.2.0
	 * @return Returns translation <b>key</b> of <b>langFile</b><br>
	 * If key wasn't found -> <code>return key;</code>
	 */
	@Deprecated
	public static String getTranslationFormat(String langFile, String key, Object... args) throws Exception {
		synchronized(LANG_CACHE) {
			if(getTranslation(langFile, key) == null) {
				return key;
			}
			
			try {
				return String.format(getTranslation(langFile, key), args);
			}catch(Exception e) {
				return getTranslation(langFile, key);
			}
		}
	}
	/**
	 * @deprecated Will be removed in v1.2.0
	 * @return Returns language name of <b>langFile</b><br>
	 * <code>return getTranslation(langFile, "lang.name");</code>
	 */
	@Deprecated
	public static String getLangName(String langFile) throws Exception {
		synchronized(LANG_CACHE) {
			return getTranslation(langFile, "lang.name");
		}
	}
	/**
	 * @deprecated Will be removed in v1.2.0
	 * @return Returns language version of <b>langFile</b><br>
	 * <code>return getTranslation(langFile, "lang.name");</code>
	 */
	@Deprecated
	public static String getLangVersion(String langFile) throws Exception {
		synchronized(LANG_CACHE) {
			return getTranslation(langFile, "lang.version");
		}
	}
	/**
	 * @deprecated Will be removed in v1.2.0
	 * Writes all translations of <b>translationMap</b> in <b>langFile</b>
	 * 
	 * @return Returns true if successful, false otherwise
	 */
	@Deprecated
	public static boolean write(File langFile, Map<String, String> translationMap, TerminalIO term) {
		synchronized(LANG_CACHE) {
			LANG_CACHE.clear();
			
			try {
				BufferedWriter w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(langFile), StandardCharsets.UTF_8));
				
				for(String str:translationMap.keySet()) {
					String value = translationMap.get(str);
					//For multiline
					value = value.replace("\n", "\\n");
					
					w.write(str + " = " + value);
					w.newLine();
				}
				
				w.close();
			}catch (IOException e) {
				term.logStackTrace(e, Lang.class);
				
				return false;
			}
			
			return true;
		}
	}
	/**
	 * @deprecated Will be removed in v1.1.0
	 */
	@Deprecated
	public static LangCompilerInterface createCompilerInterface(String langFile, TerminalIO term) throws Exception {
		return createCompilerInterface(langFile, term, new LangPlatformAPI());
	}
	/**
	 * @deprecated Will be removed in v1.1.0
	 */
	@Deprecated
	public static LangCompilerInterface createCompilerInterface(String langFile, TerminalIO term, LangPlatformAPI langPlatformAPI) throws Exception {
		return new LangCompilerInterface(createInterpreterInterface(langFile, term, langPlatformAPI));
	}
	/**
	 * @deprecated Will be removed in v1.1.0
	 */
	@Deprecated
	public static LangCompilerInterface createCompilerInterface(TerminalIO term) {
		return createCompilerInterface(term, new LangPlatformAPI());
	}
	/**
	 * @deprecated Will be removed in v1.1.0
	 */
	@Deprecated
	public static LangCompilerInterface createCompilerInterface(TerminalIO term, LangPlatformAPI langPlatformAPI) {
		return new LangCompilerInterface(createInterpreterInterface(term, langPlatformAPI));
	}
	
	
	//Classes for compiling lang file
	/**
	 * @deprecated Will be removed in v1.1.0
	 */
	@Deprecated
	public static class LangCompilerInterface {
		private LangInterpreter.LangInterpreterInterface lii;
		
		private LangCompilerInterface(LangInterpreter.LangInterpreterInterface lii) {
			this.lii = lii;
		}
		
		public Map<Integer, Compiler.Data> getData() {
			HashMap<Integer, Compiler.Data> convertedDataMap = new HashMap<>();
			
			Map<Integer, LangInterpreter.Data> dataMap = lii.getData();
			dataMap.forEach((SCOPE_ID, data) -> {
				Compiler.Data convertedData = new Compiler.Data();
				convertedData.lang = data.lang;
				convertedData.var = new HashMap<>();
				data.var.forEach((varName, varData) -> convertedData.var.put(varName, Compiler.DataObject.convert(varData, lii)));
				
				convertedDataMap.put(SCOPE_ID, convertedData);
			});
			
			return convertedDataMap;
		}
		public Compiler.Data getData(final int SCOPE_ID) {
			return getData().get(SCOPE_ID);
		}
		
		public Map<String, String> getTranslationMap(final int SCOPE_ID) {
			Compiler.Data data = getData(SCOPE_ID);
			if(data == null)
				return null;
			
			return data.lang;
		}
		public String getTranslation(final int SCOPE_ID, String key) {
			Map<String, String> translations = getTranslationMap(SCOPE_ID);
			if(translations == null)
				return null;
			
			return translations.get(key);
		}
		
		public void setTranslation(final int SCOPE_ID, String key, String value) {
			Map<String, String> translations = getTranslationMap(SCOPE_ID);
			if(translations != null)
				translations.put(key, value);
		}
		
		public Map<String, Compiler.DataObject> getVarMap(final int SCOPE_ID) {
			Compiler.Data data = getData(SCOPE_ID);
			if(data == null)
				return null;
			
			return data.var;
		}
		public Compiler.DataObject getVar(final int SCOPE_ID, String varName) {
			Map<String, Compiler.DataObject> vars = getVarMap(SCOPE_ID);
			if(vars == null)
				return null;
			
			return vars.get(varName);
		}
		
		private void setVar(final int SCOPE_ID, String varName, Compiler.DataObject data, boolean ignoreFinal) {
			lii.setVar(SCOPE_ID, varName, data.convert(lii), ignoreFinal);
		}
		public void setVar(final int SCOPE_ID, String varName, String text) {
			setVar(SCOPE_ID, varName, text, false);
		}
		public void setVar(final int SCOPE_ID, String varName, String text, boolean ignoreFinal) {
			setVar(SCOPE_ID, varName, new Compiler.DataObject(text), ignoreFinal);
		}
		public void setVar(final int SCOPE_ID, String varName, Compiler.DataObject[] arr) {
			setVar(SCOPE_ID, varName, arr, false);
		}
		public void setVar(final int SCOPE_ID, String varName, Compiler.DataObject[] arr, boolean ignoreFinal) {
			setVar(SCOPE_ID, varName, new Compiler.DataObject().setArray(arr), ignoreFinal);
		}
		/**
		 * @param function Call: function(String funcArgs, int SCOPE_ID): String
		 */
		public void setVar(final int SCOPE_ID, String varName, BiFunction<String, Integer, String> function) {
			setVar(SCOPE_ID, varName, function, false);
		}
		/**
		 * @param function Call: function(String funcArgs, int SCOPE_ID): String
		 */
		public void setVar(final int SCOPE_ID, String varName, BiFunction<String, Integer, String> function, boolean ignoreFinal) {
			setVar(SCOPE_ID, varName, new Compiler.DataObject().setFunctionPointer(new Compiler.FunctionPointerObject(function)), ignoreFinal);
		}
		public void setVar(final int SCOPE_ID, String varName, int errno) {
			setVar(SCOPE_ID, varName, errno, false);
		}
		public void setVar(final int SCOPE_ID, String varName, int errno, boolean ignoreFinal) {
			setVar(SCOPE_ID, varName, new Compiler.DataObject().setError(new Compiler.ErrorObject(errno)), false);
		}
		/**
		 * @param voidNull Sets the var to null if voidNull else void
		 */
		public void setVar(final int SCOPE_ID, String varName, boolean voidNull) {
			setVar(SCOPE_ID, varName, voidNull, false);
		}
		/**
		 * @param voidNull Sets the var to null if voidNull else void
		 */
		public void setVar(final int SCOPE_ID, String varName, boolean voidNull, boolean ignoreFinal) {
			Compiler.DataObject dataObject = new Compiler.DataObject();
			if(voidNull)
				dataObject.setNull();
			else
				dataObject.setVoid();
			
			setVar(SCOPE_ID, varName, dataObject, ignoreFinal);
		}
		
		/**
		 * Creates an function which is accessible globally in the Compiler (= in all SCOPE_IDs)<br>
		 * If function already exists, it will be overridden<br>
		 * Function can be accessed with "func.[funcName]" and can't be removed nor changed by the lang file
		 */
		public void addPredefinedFunction(String funcName, BiFunction<String, Integer, String> function) {
			lii.addPredefinedFunction(funcName, new Compiler.DataObject().setFunctionPointer(new Lang.Compiler.FunctionPointerObject(function)).convert(lii).
			getFunctionPointer().getPredefinedFunction());
		}
		
		public void exec(final int SCOPE_ID, BufferedReader lines) throws IOException {
			lii.exec(SCOPE_ID, lines);
		}
		public void exec(final int SCOPE_ID, String lines) throws IOException {
			try(BufferedReader reader = new BufferedReader(new StringReader(lines))) {
				exec(SCOPE_ID, reader);
			}
		}
		public String execLine(final int SCOPE_ID, String line) {
			try {
				exec(SCOPE_ID, line);
			}catch(IOException e) {
				return "Error";
			}
			
			return "";
		}
		public String callFunction(final int SCOPE_ID, String funcName, String funcArgs) {
			List<Node> argumentList = new LinkedList<>();
			String code = "func.abc(" + funcArgs + ")";
			try(BufferedReader reader = new BufferedReader(new StringReader(code))) {
				AbstractSyntaxTree ast = lii.parseLines(reader);
				argumentList.addAll(ast.getChildren().get(0).getChildren());
			}catch(Exception e) {
				argumentList.add(new ParsingErrorNode(ParsingError.EOF));
			}
			
			me.jddev0.module.lang.DataObject.FunctionPointerObject fp;
			if(funcName.startsWith("func.") || funcName.startsWith("linker.")) {
				boolean isLinkerFunction = funcName.startsWith("l");
				
				funcName = funcName.substring(funcName.indexOf('.') + 1);
				String funcNameCopy = funcName;
				Optional<LangPredefinedFunctionObject> predefinedFunction = lii.getPredefinedFunctions().entrySet().stream().filter(entry -> {
					return entry.getValue().isLinkerFunction() == isLinkerFunction;
				}).filter(entry -> {
					return entry.getKey().equals(funcNameCopy);
				}).map(Map.Entry<String, LangPredefinedFunctionObject>::getValue).findFirst();
				fp = new me.jddev0.module.lang.DataObject.FunctionPointerObject(predefinedFunction.orElse(null));
			}else {
				me.jddev0.module.lang.DataObject dataObject = lii.getData(SCOPE_ID).var.get(funcName);
				fp = dataObject == null?null:dataObject.getFunctionPointer();
			}
			
			return lii.interpretFunctionPointer(fp, funcName, argumentList, SCOPE_ID).getText();
		}
	}
	/**
	 * @deprecated Will be removed in v1.1.0
	 */
	@Deprecated
	private static class Compiler {
		private Compiler() {}
		
		//Classes for variable data
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static class FunctionPointerObject {
			/**
			 * Normal function pointer
			 */
			public static final int NORMAL = 0;
			/**
			 * Pointer to a predefined function
			 */
			public static final int PREDEFINED = 1;
			/**
			 * Function which is defined in the language, were the Compiler/Interpreter is defined
			 */
			public static final int EXTERNAL = 2;
			/**
			 * Pointer to a linker function
			 */
			public static final int LINKER = 3;
			
			private final String head;
			private final String body;
			private final BiFunction<String, Integer, String> externalFunction;
			private final int functionPointerType;
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@Deprecated
			public FunctionPointerObject(String langFuncObjectName) {
				this.head = "";
				this.body = langFuncObjectName;
				this.externalFunction = null;
				this.functionPointerType = PREDEFINED;
			}
			/**
			 * For pointer to external function
			 */
			public FunctionPointerObject(BiFunction<String, Integer, String> externalFunction) {
				this.head = "";
				this.body = "";
				this.externalFunction = externalFunction;
				this.functionPointerType = EXTERNAL;
			}
			
			public String getBody() {
				return body;
			}
			
			public String getHead() {
				return head;
			}
			
			public BiFunction<String, Integer, String> getExternalFunction() {
				return externalFunction;
			}
			
			public int getFunctionPointerType() {
				return functionPointerType;
			}
			
			@Override
			public String toString() {
				return head + "\n" + body;
			}
		}
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static class VarPointerObject {
			private final String varName;
			private final DataObject var;
			
			public VarPointerObject(String varName, DataObject var) {
				this.varName = varName;
				this.var = var;
			}
			
			public String getVarName() {
				return varName;
			}
			
			public DataObject getVar() {
				return var;
			}
			
			@Override
			public String toString() {
				return varName;
			}
		}
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static class ClassObject {
			private final Map<String, DataObject> attributes = new HashMap<>();
			private final String className;
			private final String packageName;
			private final ClassObject superClass;
			private final boolean classDefinition; //Is true if the class object is only an class definition else it is an actual instance of the class
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public ClassObject(String className, String packageName, ClassObject superClass, boolean classDefinition) {
				this.className = className;
				this.packageName = packageName;
				this.superClass = superClass;
				this.classDefinition = classDefinition;
			}

			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public void setAttribute(String name, DataObject data) {
				attributes.put(name, data);
			}
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public DataObject getAttribute(String name) {
				return attributes.get(name);
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public Map<String, DataObject> getAttributes() {
				return attributes;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public String getPackageName() {
				return packageName;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public ClassObject getSuperClass() {
				return superClass;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public boolean isClassDefinition() {
				return classDefinition;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public boolean isInstanceOf(ClassObject classObject) {
				if(this.equals(classObject))
					return true;
				
				if(superClass == null)
					return classObject.superClass == null;
				
				return superClass.isInstanceOf(classObject);
			}
			
			@Override
			public String toString() {
				return "Class";
			}
			
			@Override
			public boolean equals(Object obj) {
				if(obj == null)
					return false;
				
				if(this == obj)
					return true;
				
				if(obj instanceof ClassObject) {
					ClassObject that = (ClassObject)obj;
					
					return Objects.equals(this.className, that.className) && Objects.equals(this.packageName, that.packageName);
				}
				
				return false;
			}
		}
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static class ErrorObject {
			private final int err;
			
			public ErrorObject(int err) {
				this.err = err;
			}
			
			public int getErrno() {
				return err;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public String getErrmsg() {
				return InterpretingError.getErrorFromErrorCode(err).getErrorText();
			}
			
			@Override
			public String toString() {
				return "Error";
			}
		}
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static enum DataType {
			TEXT, ARRAY, VAR_POINTER, FUNCTION_POINTER, VOID, NULL, INT, LONG, DOUBLE, FLOAT, CHAR, CLASS, ERROR;
		}
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static class DataObject {
			private DataType type;
			private String txt;
			private DataObject[] arr;
			private VarPointerObject vp;
			private FunctionPointerObject fp;
			private int intValue;
			private long longValue;
			private float floatValue;
			private double doubleValue;
			private char charValue;
			private ClassObject classObject;
			private ErrorObject error;
			private boolean finalData;
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public DataObject(DataObject dataObject) {
				setData(dataObject);
			}
			
			public DataObject() {
				this("");
			}
			public DataObject(String txt) {
				this(txt, false);
			}
			public DataObject(String txt, boolean finalData) {
				setText(txt);
				setFinalData(finalData);
			}
			
			/**
			 * This method <b>ignores</b> the final state of the data object
			 */
			private void setData(DataObject dataObject) {
				this.type = dataObject.type;
				this.txt = dataObject.txt;
				//Array won't be copied accurate, because function pointer should be able to change array data from inside
				this.arr = dataObject.arr;
				this.vp = dataObject.vp;
				this.fp = dataObject.fp;
				this.intValue = dataObject.intValue;
				this.longValue = dataObject.longValue;
				this.floatValue = dataObject.floatValue;
				this.doubleValue = dataObject.doubleValue;
				this.charValue = dataObject.charValue;
				//Class won't be copied accurate, because function pointer should be able to change class data from inside
				this.classObject = dataObject.classObject;
				this.error = dataObject.error;
				this.finalData = dataObject.finalData;
			}
			
			public DataObject setText(String txt) {
				if(finalData)
					return this;
				
				this.type = DataType.TEXT;
				this.txt = txt;
				
				return this;
			}
			
			public String getText() {
				switch(type) {
					case TEXT:
						return txt;
					case ARRAY:
						return Arrays.toString(arr);
					case VAR_POINTER:
						return vp.toString();
					case FUNCTION_POINTER:
						return fp.toString();
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
					case CLASS:
						return classObject.toString();
					case ERROR:
						return error.toString();
				}
				
				return null;
			}
			
			public DataObject setArray(DataObject[] arr) {
				if(finalData)
					return this;
				
				this.type = DataType.ARRAY;
				this.arr = arr;
				
				return this;
			}
			
			public DataObject[] getArray() {
				return arr;
			}
			
			public DataObject setVarPointer(VarPointerObject vp) {
				if(finalData)
					return this;
				
				this.type = DataType.VAR_POINTER;
				this.vp = vp;
				
				return this;
			}
			
			public VarPointerObject getVarPointer() {
				return vp;
			}
			
			public DataObject setFunctionPointer(FunctionPointerObject fp) {
				if(finalData)
					return this;
				
				this.type = DataType.FUNCTION_POINTER;
				this.fp = fp;
				
				return this;
			}
			
			public FunctionPointerObject getFunctionPointer() {
				return fp;
			}
			
			public DataObject setNull() {
				if(finalData)
					return this;
				
				this.type = DataType.NULL;
				
				return this;
			}
			
			public DataObject setVoid() {
				if(finalData)
					return this;
				
				this.type = DataType.VOID;
				
				return this;
			}
			
			public DataObject setInt(int intValue) {
				if(finalData)
					return this;
				
				this.type = DataType.INT;
				this.intValue = intValue;
				
				return this;
			}
			
			public int getInt() {
				return intValue;
			}
			
			public DataObject setLong(long longValue) {
				if(finalData)
					return this;
				
				this.type = DataType.LONG;
				this.longValue = longValue;
				
				return this;
			}
			
			public long getLong() {
				return longValue;
			}
			
			public DataObject setFloat(float floatValue) {
				if(finalData)
					return this;
				
				this.type = DataType.FLOAT;
				this.floatValue = floatValue;
				
				return this;
			}
			
			public float getFloat() {
				return floatValue;
			}
			
			public DataObject setDouble(double doubleValue) {
				if(finalData)
					return this;
				
				this.type = DataType.DOUBLE;
				this.doubleValue = doubleValue;
				
				return this;
			}
			
			public double getDouble() {
				return doubleValue;
			}
			
			public DataObject setChar(char charValue) {
				if(finalData)
					return this;
				
				this.type = DataType.CHAR;
				this.charValue = charValue;
				
				return this;
			}
			
			public char getChar() {
				return charValue;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public DataObject setClassObject(ClassObject classObject) {
				if(finalData)
					return this;
				
				this.type = DataType.CLASS;
				this.classObject = classObject;
				
				return this;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public ClassObject getClassObject() {
				return classObject;
			}
			
			public DataObject setError(ErrorObject error) {
				if(finalData)
					return this;
				
				this.type = DataType.ERROR;
				this.error = error;
				
				return this;
			}
			
			public ErrorObject getError() {
				return error;
			}
			
			public DataObject setFinalData(boolean finalData) {
				this.finalData = finalData;
				
				return this;
			}
			
			public boolean isFinalData() {
				return finalData;
			}
			
			/**
			 * @deprecated Will be removed in v1.1.0
			 * For pointer to predefined function
			 */
			@SuppressWarnings("unused")
			@Deprecated
			public DataType getType() {
				return type;
			}
			
			public me.jddev0.module.lang.DataObject convert(LangInterpreter.LangInterpreterInterface lii) {
				me.jddev0.module.lang.DataObject convertedDataObject = new me.jddev0.module.lang.DataObject();
				switch(type) {
					case ARRAY:
						DataObject[] arr = getArray();
						me.jddev0.module.lang.DataObject[] convertedArr = new me.jddev0.module.lang.DataObject[arr.length];
						for(int i = 0;i < arr.length;i++)
							convertedArr[i] = arr[i] == null?null:arr[i].convert(lii);
						convertedDataObject.setArray(convertedArr);
						break;
					case CHAR:
						convertedDataObject.setChar(getChar());
						break;
					case DOUBLE:
						convertedDataObject.setDouble(getDouble());
						break;
					case ERROR:
						ErrorObject err = getError();
						me.jddev0.module.lang.DataObject.ErrorObject convertedErr = new me.jddev0.module.lang.DataObject.ErrorObject(InterpretingError.getErrorFromErrorCode(err.getErrno()));
						convertedDataObject.setError(convertedErr);
						break;
					case FLOAT:
						convertedDataObject.setFloat(getFloat());
						break;
					case FUNCTION_POINTER:
						FunctionPointerObject fp = getFunctionPointer();
						me.jddev0.module.lang.DataObject.FunctionPointerObject convertedFP = null;
						switch(fp.getFunctionPointerType()) {
							case FunctionPointerObject.NORMAL:
								convertedFP = new me.jddev0.module.lang.DataObject.FunctionPointerObject((LangPredefinedFunctionObject)(argumentList, SCOPE_ID) -> {
									String function = "fp.abc = (" + fp.getHead() + ") -> {\n" + fp.getBody() + "}";
									try(BufferedReader reader = new BufferedReader(new StringReader(function))) {
										AbstractSyntaxTree ast = lii.parseLines(reader);
										FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode)((AssignmentNode)(ast.getChildren().get(0))).getRvalue();
										AbstractSyntaxTree functionBody = functionDefinitionNode.getFunctionBody();
										
										List<VariableNameNode> parameterList = new ArrayList<>();
										List<Node> children = functionDefinitionNode.getChildren();
										for(Node child:children) {
											if(child.getNodeType() != NodeType.VARIABLE_NAME)
												continue;
											
											VariableNameNode parameter = (VariableNameNode)child;
											if(!parameter.getVariableName().matches("(\\$|&|fp\\.)\\w+") || parameter.getVariableName().matches("(\\$|&)LANG_.*")) 
												continue;
											
											parameterList.add(parameter);
										}
										
										return lii.callFunctionPointer(new me.jddev0.module.lang.DataObject.FunctionPointerObject(parameterList, functionBody), null, argumentList, SCOPE_ID);
									}catch(ClassCastException|IOException e) {
										return new me.jddev0.module.lang.DataObject().setError(new me.jddev0.module.lang.DataObject.ErrorObject(InterpretingError.INVALID_AST_NODE));
									}
								});
								break;
							case FunctionPointerObject.PREDEFINED:
							case FunctionPointerObject.LINKER:
								String funcName = getFunctionPointer().getBody();
								if(funcName.startsWith("func.") || funcName.startsWith("linker.")) {
									boolean isLinkerFunction = funcName.startsWith("l");
									
									funcName = funcName.substring(funcName.indexOf('.') + 1);
									String funcNameCopy = funcName;
									Optional<LangPredefinedFunctionObject> predefinedFunction = lii.getPredefinedFunctions().entrySet().stream().filter(entry -> {
										return entry.getValue().isLinkerFunction() == isLinkerFunction;
									}).filter(entry -> {
										return entry.getKey().equals(funcNameCopy);
									}).map(Map.Entry<String, LangPredefinedFunctionObject>::getValue).findFirst();
									convertedFP = new me.jddev0.module.lang.DataObject.FunctionPointerObject(predefinedFunction.orElse(null));
								}
								break;
							case FunctionPointerObject.EXTERNAL:
								LangExternalFunctionObject convertedExternalFunction = (argumentList, SCOPE_ID) -> {
									String args = LangUtils.combineDataObjects(argumentList).getText();
									return new me.jddev0.module.lang.DataObject(fp.getExternalFunction().apply(args, SCOPE_ID));
								};
								
								convertedFP = new me.jddev0.module.lang.DataObject.FunctionPointerObject(convertedExternalFunction);
								break;
						}
						convertedDataObject.setFunctionPointer(convertedFP);
						break;
					case INT:
						convertedDataObject.setInt(getInt());
						break;
					case LONG:
						convertedDataObject.setLong(getLong());
						break;
					case NULL:
						convertedDataObject.setNull();
						break;
					case TEXT:
					case CLASS:
						convertedDataObject.setText(getText());
						break;
					case VAR_POINTER:
						DataObject var = getVarPointer().getVar();
						me.jddev0.module.lang.DataObject.VarPointerObject convertedVarPtr = new me.jddev0.module.lang.DataObject.VarPointerObject(var == null?null:var.convert(lii).setVariableName(getVarPointer().getVarName()));
						convertedDataObject.setVarPointer(convertedVarPtr);
						break;
					case VOID:
						convertedDataObject.setVoid();
						break;
				}
				
				return convertedDataObject.setFinalData(isFinalData());
			}
			
			public static DataObject convert(me.jddev0.module.lang.DataObject dataObject, LangInterpreter.LangInterpreterInterface lii) {
				if(dataObject == null)
					return null;
				
				DataObject convertedDataObject = new DataObject();
				
				switch(dataObject.getType()) {
					case ARRAY:
						me.jddev0.module.lang.DataObject[] arr = dataObject.getArray();
						DataObject[] convertedArr = new DataObject[arr.length];
						for(int i = 0;i < arr.length;i++)
							convertedArr[i] = convert(arr[i], lii);
						convertedDataObject.setArray(convertedArr);
						break;
					case CHAR:
						convertedDataObject.setChar(dataObject.getChar());
						break;
					case DOUBLE:
						convertedDataObject.setDouble(dataObject.getDouble());
						break;
					case ERROR:
						me.jddev0.module.lang.DataObject.ErrorObject err = dataObject.getError();
						ErrorObject convertedErr = new ErrorObject(err.getErrno());
						convertedDataObject.setError(convertedErr);
						break;
					case FLOAT:
						convertedDataObject.setFloat(dataObject.getFloat());
						break;
					case FUNCTION_POINTER:
						me.jddev0.module.lang.DataObject.FunctionPointerObject fp = dataObject.getFunctionPointer();
						FunctionPointerObject convertedFP = null;
						switch(fp.getFunctionPointerType()) {
							case me.jddev0.module.lang.DataObject.FunctionPointerObject.NORMAL:
							case me.jddev0.module.lang.DataObject.FunctionPointerObject.PREDEFINED:
								convertedFP = new FunctionPointerObject(dataObject.getVariableName());
								break;
							case me.jddev0.module.lang.DataObject.FunctionPointerObject.EXTERNAL:
								BiFunction<String, Integer, String> convertedExternalFunction = (args, SCOPE_ID) -> {
									List<Node> argumentList = new LinkedList<>();
									String code = "func.abc(" + args + ")";
									try(BufferedReader reader = new BufferedReader(new StringReader(code))) {
										AbstractSyntaxTree ast = lii.parseLines(reader);
										argumentList.addAll(ast.getChildren().get(0).getChildren());
									}catch(Exception e) {
										argumentList.add(new ParsingErrorNode(ParsingError.EOF));
									}
									
									me.jddev0.module.lang.DataObject ret = lii.interpretFunctionPointer(fp, dataObject.getVariableName(), argumentList, SCOPE_ID);
									return ret == null?"":ret.getText();
								};
								
								convertedFP = new FunctionPointerObject(convertedExternalFunction);
								break;
						}
						convertedDataObject.setFunctionPointer(convertedFP);
						break;
					case INT:
						convertedDataObject.setInt(dataObject.getInt());
						break;
					case LONG:
						convertedDataObject.setLong(dataObject.getLong());
						break;
					case NULL:
						convertedDataObject.setNull();
						break;
					case TEXT:
					case ARGUMENT_SEPARATOR:
					case TYPE:
						convertedDataObject.setText(dataObject.getText());
						break;
					case VAR_POINTER:
						me.jddev0.module.lang.DataObject var = dataObject.getVarPointer().getVar();
						VarPointerObject convertedVarPtr = new VarPointerObject(var == null?null:var.getVariableName(), var == null?null:convert(var, lii));
						convertedDataObject.setVarPointer(convertedVarPtr);
						break;
					case VOID:
						convertedDataObject.setVoid();
						break;
				}
				
				return convertedDataObject.setFinalData(dataObject.isFinalData());
			}
			
			@Override
			public String toString() {
				return getText();
			}
		}
		/**
		 * @deprecated Will be removed in v1.1.0
		 */
		@Deprecated
		public static class Data {
			public Map<String, String> lang;
			public Map<String, DataObject> var;
		}
	}
}