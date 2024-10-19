package at.jddev0.lang;

import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.ErrorObject;
import at.jddev0.lang.LangInterpreter.InterpretingError;
import at.jddev0.lang.LangInterpreter.StackElement;

import java.nio.file.FileSystems;

/**
 * Lang-Module<br>
 * Definition of Lang vars
 *
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangVars {
    private final LangInterpreter interpreter;

    public LangVars(LangInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    private void addLangVar(String variableName, DataObject langVar) {
        interpreter.getData().var.put(variableName, langVar.setLangVar().setVariableName(variableName));
    }

    private void addStaticLangVar(String variableName, DataObject langVar) {
        interpreter.getData().var.computeIfAbsent(variableName, key -> langVar.setStaticData(true).setLangVar().setVariableName(variableName));
    }

    void addEssentialLangVars(DataObject langArgs) {
        interpreter.getData().var.put("&LANG_ARGS", langArgs == null?new DataObject().setArray(new DataObject[0]).
                setFinalData(true).setLangVar().setVariableName("&LANG_ARGS"):langArgs);

        addSystemLangVars();
        addBasicNumberLangVars();
        addErrorLangVars();
        addTypeLangVars();

        //Non-final
        addStaticLangVar("$LANG_ERRNO", new DataObject().setError(new ErrorObject(InterpretingError.NO_ERROR)));
    }
    private void addSystemLangVars() {
        addLangVar("$LANG_VERSION", new DataObject(LangInterpreter.VERSION, true).setFinalData(true));
        addLangVar("$LANG_NAME", new DataObject("Standard Lang", true).setFinalData(true));
        addLangVar("$LANG_RAND_MAX", new DataObject().setInt(Integer.MAX_VALUE).setFinalData(true));
        addLangVar("$LANG_OS_NAME", new DataObject(System.getProperty("os.name")).setFinalData(true));
        addLangVar("$LANG_OS_VER", new DataObject(System.getProperty("os.version")).setFinalData(true));
        addLangVar("$LANG_OS_ARCH", new DataObject(System.getProperty("os.arch")).setFinalData(true));
        addLangVar("$LANG_OS_FILE_SEPARATOR", new DataObject(FileSystems.getDefault().getSeparator()).setFinalData(true));
        addLangVar("$LANG_OS_LINE_SEPARATOR", new DataObject(System.lineSeparator()).setFinalData(true));
    }
    private void addBasicNumberLangVars() {
        addLangVar("$LANG_INT_MIN", new DataObject().setInt(Integer.MIN_VALUE).setFinalData(true));
        addLangVar("$LANG_INT_MAX", new DataObject().setInt(Integer.MAX_VALUE).setFinalData(true));

        addLangVar("$LANG_LONG_MIN", new DataObject().setLong(Long.MIN_VALUE).setFinalData(true));
        addLangVar("$LANG_LONG_MAX", new DataObject().setLong(Long.MAX_VALUE).setFinalData(true));

        addLangVar("$LANG_FLOAT_NAN", new DataObject().setFloat(Float.NaN).setFinalData(true));
        addLangVar("$LANG_FLOAT_POS_INF", new DataObject().setFloat(Float.POSITIVE_INFINITY).setFinalData(true));
        addLangVar("$LANG_FLOAT_NEG_INF", new DataObject().setFloat(Float.NEGATIVE_INFINITY).setFinalData(true));

        addLangVar("$LANG_DOUBLE_NAN", new DataObject().setDouble(Double.NaN).setFinalData(true));
        addLangVar("$LANG_DOUBLE_POS_INF", new DataObject().setDouble(Double.POSITIVE_INFINITY).setFinalData(true));
        addLangVar("$LANG_DOUBLE_NEG_INF", new DataObject().setDouble(Double.NEGATIVE_INFINITY).setFinalData(true));
    }
    private void addErrorLangVars() {
        for(InterpretingError error:InterpretingError.values()) {
            String upperCaseErrorName = error.name().toUpperCase();
            String variableName = "$LANG_ERROR_" + upperCaseErrorName;
            addLangVar(variableName, new DataObject().setError(new ErrorObject(error)).setFinalData(true));
            variableName = "$LANG_ERRNO_" + upperCaseErrorName;
            addLangVar(variableName, new DataObject().setInt(error.getErrorCode()).setFinalData(true));
        }
    }
    private void addTypeLangVars() {
        for(DataType type:DataType.values()) {
            String upperCaseTypeName = type.name().toUpperCase();
            String variableName = "$LANG_TYPE_" + upperCaseTypeName;
            addLangVar(variableName, new DataObject().setTypeValue(type).setFinalData(true));
        }
    }

    public void addLangVars(DataObject langArgs) {
        addEssentialLangVars(langArgs);

        addExecutionLangVars(interpreter.getCurrentCallStackElement());
        addNumberLangVars();
        addStructDefinitionLangVars();
        addClassDefinitionLangVars();
    }
    private void addExecutionLangVars(StackElement currentStackElement) {
        addLangVar("$LANG_PATH", new DataObject(currentStackElement.getLangPath(), true).setFinalData(true));
        addLangVar("$LANG_FILE", new DataObject(currentStackElement.getLangFile(), true).setFinalData(true));
        addLangVar("$LANG_CURRENT_FUNCTION", new DataObject(currentStackElement.getLangFunctionName(), true).setFinalData(true));

        //Module vars
        if(currentStackElement.module != null) {
            addLangVar("$LANG_MODULE_STATE", new DataObject(currentStackElement.module.isLoad()?"load":"unload"));

            String prefix = "<module:" + currentStackElement.module.getFile() + "[" + currentStackElement.module.getLangModuleConfiguration().getName() + "]>";

            String modulePath = currentStackElement.getLangPath().substring(prefix.length());
            if(!modulePath.startsWith("/"))
                modulePath = "/" + modulePath;

            addLangVar("$LANG_MODULE_PATH", new DataObject(modulePath, true));
            addLangVar("$LANG_MODULE_FILE", new DataObject(currentStackElement.getLangFile(), true));
        }
    }
    private void addNumberLangVars() {
        addLangVar("$LANG_MATH_PI", new DataObject().setDouble(Math.PI).setFinalData(true));
        addLangVar("$LANG_MATH_E", new DataObject().setDouble(Math.E).setFinalData(true));
        addLangVar("$LANG_MATH_I", interpreter.standardTypes.get("$COMPLEX_I").setFinalData(true));
    }
    private void addStructDefinitionLangVars() {
        addStaticLangVar("&CodePosition", interpreter.standardTypes.get("&CodePosition").setFinalData(true));
        addStaticLangVar("&StackTraceElement", interpreter.standardTypes.get("&StackTraceElement").setFinalData(true));
        addStaticLangVar("&Pair", interpreter.standardTypes.get("&Pair").setFinalData(true));
    }
    private void addClassDefinitionLangVars() {
        addStaticLangVar("&Object", new DataObject().setObject(DataObject.LangObject.OBJECT_CLASS).setFinalData(true));
        addStaticLangVar("&Maybe", interpreter.standardTypes.get("&Maybe").setFinalData(true));
        addStaticLangVar("&Complex", interpreter.standardTypes.get("&Complex").setFinalData(true));
        addStaticLangVar("&BasicIterator", interpreter.standardTypes.get("&BasicIterator").setFinalData(true));
    }
}