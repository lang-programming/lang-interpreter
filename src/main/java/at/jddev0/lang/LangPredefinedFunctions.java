package at.jddev0.lang;

import java.io.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import at.jddev0.io.TerminalIO.Level;
import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraintException;
import at.jddev0.lang.DataObject.ErrorObject;
import at.jddev0.lang.DataObject.FunctionPointerObject;
import at.jddev0.lang.DataObject.StructObject;
import at.jddev0.lang.LangFunction.*;
import at.jddev0.lang.LangFunction.LangParameter.*;
import at.jddev0.lang.LangInterpreter.InterpretingError;
import at.jddev0.lang.LangInterpreter.StackElement;
import at.jddev0.lang.LangUtils.InvalidTranslationTemplateSyntaxException;
import at.jddev0.lang.regex.InvalidPatternSyntaxException;
import at.jddev0.lang.regex.LangRegEx;

/**
 * Lang-Module<br>
 * Lang predefined and linker functions for the LangInterpreter
 *
 * @author JDDev0
 * @version v1.0.0
 */
final class LangPredefinedFunctions {
    private LangPredefinedFunctions() {}

    static void addPredefinedFunctions(Map<String, FunctionPointerObject> funcs) {
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedResetFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedErrorFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedLangFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedSystemFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedIOFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedNumberFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedTextFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedConversionFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedOperationFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedMathFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedCombinatorFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedFuncPtrFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedByteBufferFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedArrayFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedListFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedStructFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedModuleFunctions.class));
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedLangTestFunctions.class));
    }

    static void addLinkerFunctions(Map<String, FunctionPointerObject> funcs) {
        funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(LangPredefinedLinkerFunctions.class));
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedResetFunctions {
        private LangPredefinedResetFunctions() {}

        @LangFunction("freeVar")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject freeVarFunction(
                LangInterpreter interpreter,
                @LangParameter("$pointer") @AllowedTypes(DataObject.DataType.VAR_POINTER) DataObject pointerObject
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();
            if(dereferencedVarPointer == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS);

            String variableName = dereferencedVarPointer.getVariableName();
            if(variableName == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS);

            if(dereferencedVarPointer.isFinalData() || dereferencedVarPointer.isLangVar())
                return interpreter.setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE);

            interpreter.getData().var.remove(variableName);

            return null;
        }

        @LangFunction("freeAllVars")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject freeAllVarsFunction(
                LangInterpreter interpreter
        ) {
            interpreter.resetVars();

            return null;
        }

        @LangFunction("resetErrno")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject resetErrnoFunction(
                LangInterpreter interpreter
        ) {
            interpreter.getAndClearErrnoErrorObject();

            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedErrorFunctions {
        private LangPredefinedErrorFunctions() {}

        @LangFunction("getErrorText")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject getErrorTextFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject(interpreter.getAndClearErrnoErrorObject().getErrorText());
        }

        @LangFunction("withErrorMessage")
        @AllowedTypes(DataObject.DataType.ERROR)
        public static DataObject withErrorMessageFunction(
                LangInterpreter interpreter,
                @LangParameter("$error") @AllowedTypes(DataObject.DataType.ERROR) DataObject errorObject,
                @LangParameter("$text") DataObject textObject
        ) {
            return new DataObject().setError(new ErrorObject(errorObject.getError().getInterpretingError(),
                    interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString()));
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedLangFunctions {
        private LangPredefinedLangFunctions() {}

        @LangFunction("isLangVersionNewer")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isLangVersionNewerFunction(
                LangInterpreter interpreter
        ) {
            String langVer = interpreter.getData().lang.getOrDefault("lang.version", LangInterpreter.VERSION); //If lang.version = null -> return false
            Integer compVer = LangUtils.compareVersions(LangInterpreter.VERSION, langVer);
            if(compVer == null)
                return interpreter.setErrnoErrorObject(InterpretingError.LANG_VER_ERROR, "lang.version has an invalid format");
            return new DataObject().setBoolean(compVer > 0);
        }

        @LangFunction("isLangVersionOlder")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isLangVersionOlderFunction(
                LangInterpreter interpreter
        ) {
            String langVer = interpreter.getData().lang.getOrDefault("lang.version", LangInterpreter.VERSION); //If lang.version = null -> return false
            Integer compVer = LangUtils.compareVersions(LangInterpreter.VERSION, langVer);
            if(compVer == null)
                return interpreter.setErrnoErrorObject(InterpretingError.LANG_VER_ERROR, "lang.version has an invalid format");
            return new DataObject().setBoolean(compVer < 0);
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedSystemFunctions {
        private LangPredefinedSystemFunctions() {}

        @LangFunction("sleep")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject sleepFunction(
                LangInterpreter interpreter,
                @LangParameter("$milliSeconds") @NumberValue Number milliSeconds
        ) {
            if(milliSeconds.longValue() < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$milliSeconds\") must be >= 0");

            try {
                Thread.sleep(milliSeconds.longValue());
            }catch(InterruptedException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR, e.getMessage());
            }

            return null;
        }

        @LangFunction("nanoTime")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject nanoTimeFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setLong(System.nanoTime());
        }

        @LangFunction("currentTimeMillis")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject currentTimeMillisFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setLong(System.currentTimeMillis());
        }

        @LangFunction("currentUnixTime")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject currentUnixTimeFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setLong(Instant.now().getEpochSecond());
        }

        @LangFunction("getTranslationValue")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject getTranslationValueFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") @VarArgs DataObject translationKeyObject
        ) {
            String translationValue = interpreter.getData().lang.get(
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString());
            if(translationValue == null)
                return interpreter.setErrnoErrorObject(InterpretingError.TRANS_KEY_NOT_FOUND);

            return new DataObject(translationValue);
        }

        @LangFunction("getTranslationValueTemplatePluralization")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject getTranslationValueTemplatePluralizationFunction(
                LangInterpreter interpreter,
                @LangParameter("$count") @NumberValue Number count,
                @LangParameter("$translationKey") @VarArgs DataObject translationKeyObject
        ) {
            if(count.intValue() < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$count\") must be >= 0");

            String translationValue = interpreter.getData().lang.get(
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString());
            if(translationValue == null)
                return interpreter.setErrnoErrorObject(InterpretingError.TRANS_KEY_NOT_FOUND);

            try {
                return new DataObject(LangUtils.formatTranslationTemplatePluralization(translationValue, count.intValue()));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_TEMPLATE_SYNTAX, "Invalid count range");
            }catch(InvalidTranslationTemplateSyntaxException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_TEMPLATE_SYNTAX, e.getMessage());
            }
        }

        @LangFunction("makeFinal")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject makeFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$ptr") @AllowedTypes(DataType.VAR_POINTER) DataObject pointerObject
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();

            String variableName = dereferencedVarPointer.getVariableName();
            if(variableName == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Anonymous values can not be modified");

            if(dereferencedVarPointer.isLangVar())
                return interpreter.setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE);

            dereferencedVarPointer.setFinalData(true);

            return null;
        }

        @LangFunction("asFinal")
        public static DataObject asFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject
        ) {
            return new DataObject(valueObject).setCopyStaticAndFinalModifiers(true).setFinalData(true);
        }

        @LangFunction("isFinal")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") @CallByPointer DataObject pointerObject
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();

            return new DataObject().setBoolean(dereferencedVarPointer.isFinalData());
        }

        @LangFunction("makeStatic")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject makeStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$ptr") @AllowedTypes(DataType.VAR_POINTER) DataObject pointerObject
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();

            String variableName = dereferencedVarPointer.getVariableName();
            if(variableName == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Anonymous values can not be modified");

            if(dereferencedVarPointer.isLangVar())
                return interpreter.setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE);

            dereferencedVarPointer.setStaticData(true);

            return null;
        }

        @LangFunction("asStatic")
        public static DataObject asStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject
        ) {
            return new DataObject(valueObject).setCopyStaticAndFinalModifiers(true).setStaticData(true);
        }

        @LangFunction("isStatic")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") @CallByPointer DataObject pointerObject
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();

            return new DataObject().setBoolean(dereferencedVarPointer.isStaticData());
        }

        @LangFunction("constrainVariableAllowedTypes")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject constrainVariableAllowedTypesFunction(
                LangInterpreter interpreter,
                @LangParameter("$ptr") @AllowedTypes(DataType.VAR_POINTER) DataObject pointerObject,
                @LangParameter("&types") @AllowedTypes(DataType.TYPE) @VarArgs List<DataObject> typeObjects
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();

            String variableName = dereferencedVarPointer.getVariableName();
            if(variableName == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Anonymous values can not be modified");

            if(dereferencedVarPointer.isLangVar())
                return interpreter.setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE);

            List<DataType> types = typeObjects.stream().map(DataObject::getTypeValue).collect(Collectors.toList());

            try {
                dereferencedVarPointer.setTypeConstraint(DataObject.DataTypeConstraint.fromAllowedTypes(types));
            }catch(DataObject.DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, e.getMessage());
            }

            return null;
        }

        @LangFunction("constrainVariableNotAllowedTypes")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject constrainVariableNotAllowedTypesFunction(
                LangInterpreter interpreter,
                @LangParameter("$ptr") @AllowedTypes(DataType.VAR_POINTER) DataObject pointerObject,
                @LangParameter("&types") @AllowedTypes(DataType.TYPE) @VarArgs List<DataObject> typeObjects
        ) {
            DataObject dereferencedVarPointer = pointerObject.getVarPointer().getVar();

            String variableName = dereferencedVarPointer.getVariableName();
            if(variableName == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Anonymous values can not be modified");

            if(dereferencedVarPointer.isLangVar())
                return interpreter.setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE);

            List<DataType> types = typeObjects.stream().map(DataObject::getTypeValue).collect(Collectors.toList());

            try {
                dereferencedVarPointer.setTypeConstraint(DataObject.DataTypeConstraint.fromNotAllowedTypes(types));
            }catch(DataObject.DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, e.getMessage());
            }

            return null;
        }

        @LangFunction("exec")
        public static DataObject execFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            //Update call stack
            StackElement currentStackElement = interpreter.getCurrentCallStackElement();
            interpreter.pushStackElement(new StackElement(currentStackElement.getLangPath(),
                            currentStackElement.getLangFile(), currentStackElement.getLangClass(),
                            currentStackElement.getLangClassName(), "<exec-code>", currentStackElement.getModule()),
                    CodePosition.EMPTY);

            int originalLineNumber = interpreter.getParserLineNumber();
            try(BufferedReader lines = new BufferedReader(new StringReader(
                    interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString()))) {
                interpreter.resetParserPositionVars();
                interpreter.interpretLines(lines);

                return interpreter.getAndResetReturnValue();
            }catch(IOException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR, e.getMessage());
            }finally {
                interpreter.setParserLineNumber(originalLineNumber);

                //Update call stack
                interpreter.popStackElement();
            }
        }

        @LangFunction("isTerminalAvailable")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isTerminalAvailableFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setBoolean(interpreter.term != null);
        }

        @LangFunction("isCallable")
        @AllowedTypes(DataType.INT)
        public static DataObject isCallableFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject
        ) {
            return new DataObject().setBoolean(LangUtils.isCallable(valueObject));
        }

        @LangFunction(value="isInstanceOf", hasInfo=true)
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isInstanceOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject,
                @LangParameter("$type") @AllowedTypes(DataObject.DataType.TYPE) DataObject typeObject
        ) {
            return new DataObject().setBoolean(valueObject.getType() == typeObject.getTypeValue());
        }
        @LangFunction("isInstanceOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isInstanceOfWithStructParametersFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") @AllowedTypes(DataObject.DataType.STRUCT) DataObject valueObject,
                @LangParameter("$type") @AllowedTypes(DataObject.DataType.STRUCT) DataObject typeObject
        ) {
            StructObject valueStruct = valueObject.getStruct();
            StructObject typeStruct = typeObject.getStruct();

            if(!typeStruct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 2 (\"$type\") must be a definition struct");

            if(valueStruct.isDefinition())
                return new DataObject().setBoolean(false);

            return new DataObject().setBoolean(valueStruct.getStructBaseDefinition().equals(typeStruct));
        }
        @LangFunction("isInstanceOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isInstanceOfWithStructTypeParameterFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject,
                @LangParameter("$type") @AllowedTypes(DataObject.DataType.STRUCT) DataObject typeObject
        ) {
            StructObject typeStruct = typeObject.getStruct();

            if(!typeStruct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 2 (\"$type\") must be a definition struct");

            return new DataObject().setBoolean(false);
        }
        @LangFunction("isInstanceOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isInstanceOfWithObjectParametersFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") @AllowedTypes(DataObject.DataType.OBJECT) DataObject valueObject,
                @LangParameter("$type") @AllowedTypes(DataObject.DataType.OBJECT) DataObject typeObject
        ) {
            DataObject.LangObject langObject = valueObject.getObject();
            DataObject.LangObject typeClass = typeObject.getObject();

            if(!typeClass.isClass())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 2 (\"$type\") must be a class");

            if(langObject.isClass())
                return new DataObject().setBoolean(false);

            return new DataObject().setBoolean(langObject.isInstanceOf(typeClass));
        }
        @LangFunction("isInstanceOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isInstanceOfWithObjectTypeParameterFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject,
                @LangParameter("$type") @AllowedTypes(DataObject.DataType.OBJECT) DataObject typeObject
        ) {
            DataObject.LangObject typeClass = typeObject.getObject();

            if(!typeClass.isClass())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 2 (\"$type\") must be a class");

            return new DataObject().setBoolean(false);
        }

        @LangFunction("typeOf")
        @AllowedTypes(DataObject.DataType.TYPE)
        public static DataObject typeOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject
        ) {
            return new DataObject().setTypeValue(valueObject.getType());
        }

        @LangFunction("getCurrentStackTraceElement")
        @AllowedTypes(DataObject.DataType.STRUCT)
        public static DataObject getCurrentStackTraceElementFunction(
                LangInterpreter interpreter
        ) {
            StackElement currentStackElement = interpreter.getCallStackElements().get(interpreter.getCallStackElements().size() - 1);

            String modulePath = null;
            String moduleFile = null;
            if(currentStackElement.module != null) {
                String prefix = "<module:" + currentStackElement.module.getFile() + "[" + currentStackElement.module.getLangModuleConfiguration().getName() + "]>";

                modulePath = currentStackElement.getLangPath().substring(prefix.length());
                if(!modulePath.startsWith("/"))
                    modulePath = "/" + modulePath;

                moduleFile = currentStackElement.getLangFile();
            }

            return new DataObject().setStruct(new StructObject(interpreter.standardTypes.get("&StackTraceElement").getStruct(), new DataObject[] {
                    new DataObject(currentStackElement.getLangPath()),
                    new DataObject(currentStackElement.getLangFile()),
                    new DataObject().setStruct(new StructObject(interpreter.standardTypes.get("&CodePosition").getStruct(), new DataObject[] {
                            new DataObject().setInt(currentStackElement.getPos().lineNumberFrom),
                            new DataObject().setInt(currentStackElement.getPos().lineNumberTo),
                            new DataObject().setInt(currentStackElement.getPos().columnFrom),
                            new DataObject().setInt(currentStackElement.getPos().columnTo)
                    })),
                    new DataObject(currentStackElement.getLangClassName()),
                    new DataObject(currentStackElement.getLangFunctionName()),
                    new DataObject(modulePath),
                    new DataObject(moduleFile)
            }));
        }

        @LangFunction("getStackTraceElements")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject getStackTraceElementsElementFunction(
                LangInterpreter interpreter
        ) {
            List<StackElement> stackTraceElements = interpreter.getCallStackElements();

            return new DataObject().setArray(stackTraceElements.stream().map(ele -> {
                String modulePath = null;
                String moduleFile = null;
                if(ele.module != null) {
                    String prefix = "<module:" + ele.module.getFile() + "[" + ele.module.getLangModuleConfiguration().getName() + "]>";

                    modulePath = ele.getLangPath().substring(prefix.length());
                    if(!modulePath.startsWith("/"))
                        modulePath = "/" + modulePath;

                    moduleFile = ele.getLangFile();
                }

                return new DataObject().setStruct(new StructObject(interpreter.standardTypes.get("&StackTraceElement").getStruct(), new DataObject[] {
                        new DataObject(ele.getLangPath()),
                        new DataObject(ele.getLangFile()),
                        new DataObject().setStruct(new StructObject(interpreter.standardTypes.get("&CodePosition").getStruct(), new DataObject[] {
                                new DataObject().setInt(ele.getPos().lineNumberFrom),
                                new DataObject().setInt(ele.getPos().lineNumberTo),
                                new DataObject().setInt(ele.getPos().columnFrom),
                                new DataObject().setInt(ele.getPos().columnTo)
                        })),
                        new DataObject(ele.getLangClassName()),
                        new DataObject(ele.getLangFunctionName()),
                        new DataObject(modulePath),
                        new DataObject(moduleFile)
                }));
            }).toArray(DataObject[]::new));
        }

        @LangFunction("getStackTrace")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject getStackTraceFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject(interpreter.printStackTrace(CodePosition.EMPTY));
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedIOFunctions {
        private LangPredefinedIOFunctions() {}

        @LangFunction("readTerminal")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject readTerminalFunction(
                LangInterpreter interpreter,
                @LangParameter("$message") @VarArgs DataObject messageObject
        ) {
            if(interpreter.term == null && !interpreter.executionFlags.allowTermRedirect)
                return interpreter.setErrnoErrorObject(InterpretingError.NO_TERMINAL);

            String message = interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString();

            if(interpreter.term == null) {
                interpreter.setErrno(InterpretingError.NO_TERMINAL_WARNING);

                if(!message.isEmpty())
                    System.out.println(message);
                System.out.print("Input: ");

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String line = reader.readLine();
                    if(line == null)
                        return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR);
                    return new DataObject(line);
                }catch(IOException e) {
                    return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR);
                }
            }else {
                try {
                    return new DataObject(interpreter.langPlatformAPI.showInputDialog(message));
                }catch(Exception e) {
                    return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED);
                }
            }
        }

        @LangFunction("printTerminal")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject printTerminalFunction(
                LangInterpreter interpreter,
                @LangParameter("$logLevel") @NumberValue Number logLevelNumber,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            if(interpreter.term == null && !interpreter.executionFlags.allowTermRedirect)
                return interpreter.setErrnoErrorObject(InterpretingError.NO_TERMINAL);

            int logLevel = logLevelNumber.intValue();
            Level level = null;
            for(Level lvl:Level.values()) {
                if(lvl.getLevel() == logLevel) {
                    level = lvl;

                    break;
                }
            }
            if(level == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_LOG_LEVEL);

            String text = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();

            if(interpreter.term == null) {
                interpreter.setErrno(InterpretingError.NO_TERMINAL_WARNING);

                PrintStream stream = logLevel > 3?System.err:System.out; //Write to standard error if the log level is WARNING or higher
                stream.printf("[%-8s]: ", level.getLevelName());
                stream.println(text);
                return null;
            }else {
                interpreter.term.logln(level, "[From Lang file]: " + text, LangInterpreter.class);
            }

            return null;
        }

        @LangFunction("printError")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject printErrorFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            if(interpreter.term == null && !interpreter.executionFlags.allowTermRedirect)
                return interpreter.setErrnoErrorObject(InterpretingError.NO_TERMINAL);

            InterpretingError error = interpreter.getAndClearErrnoErrorObject();
            int errno = error.getErrorCode();
            Level level;
            if(errno > 0)
                level = Level.ERROR;
            else if(errno < 0)
                level = Level.WARNING;
            else
                level = Level.INFO;

            String text = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();
            text = text.isEmpty()?"":(text + ": ");

            if(interpreter.term == null) {
                PrintStream stream = level.getLevel() > 3?System.err:System.out; //Write to standard error if the log level is WARNING or higher
                stream.printf("[%-8s]: ", level.getLevelName());
                stream.println(text + error.getErrorText());
            }else {
                interpreter.term.logln(level, "[From Lang file]: " + text + error.getErrorText(), LangInterpreter.class);
            }

            return null;
        }

        @LangFunction(value="input", hasInfo=true)
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject inputFunction(
                LangInterpreter interpreter
        ) {
            return inputFunction(interpreter, null);
        }
        @LangFunction("input")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject inputFunction(
                LangInterpreter interpreter,
                @LangParameter("$maxByteCount") @NumberValue Number maxByteCount
        ) {
            if(maxByteCount != null && maxByteCount.intValue() < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$maxByteCount\") must be >= 0");

            try {
                if(maxByteCount == null) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                    String line = reader.readLine();
                    if(line == null)
                        return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR);
                    return new DataObject(line);
                }else {
                    InputStream reader = System.in;
                    byte[] buf = new byte[maxByteCount.intValue()];
                    int count = reader.read(buf);
                    if(count == -1)
                        return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR);
                    return new DataObject(new String(buf));
                }
            }catch(IOException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.SYSTEM_ERROR, e.getMessage());
            }
        }

        @LangFunction("print")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject printFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            System.out.print(interpreter.conversions.toText(textObject, CodePosition.EMPTY));

            return null;
        }

        @LangFunction("println")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject printlnFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            System.out.println(interpreter.conversions.toText(textObject, CodePosition.EMPTY));

            return null;
        }

        @LangFunction("printf")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject printfFunction(
                LangInterpreter interpreter,
                @LangParameter("$format") DataObject formatObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject out = interpreter.formatText(
                    interpreter.conversions.toText(formatObject, CodePosition.EMPTY).toString(), args);
            if(out.getType() == DataType.ERROR)
                return out;

            System.out.print(interpreter.conversions.toText(out, CodePosition.EMPTY));

            return null;
        }

        @LangFunction("error")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject errorFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            System.err.print(interpreter.conversions.toText(textObject, CodePosition.EMPTY));

            return null;
        }

        @LangFunction("errorln")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject errorlnFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            System.err.println(interpreter.conversions.toText(textObject, CodePosition.EMPTY));

            return null;
        }

        @LangFunction("errorf")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject errorfFunction(
                LangInterpreter interpreter,
                @LangParameter("$format") DataObject formatObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject out = interpreter.formatText(
                    interpreter.conversions.toText(formatObject, CodePosition.EMPTY).toString(), args);
            if(out.getType() == DataType.ERROR)
                return out;

            System.err.print(
                    interpreter.conversions.toText(out, CodePosition.EMPTY));

            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedNumberFunctions {
        private LangPredefinedNumberFunctions() {}

        @LangFunction("binToDec")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject binToDecFunction(
                LangInterpreter interpreter,
                @LangParameter("$bin") DataObject binObject
        ) {
            String binString = interpreter.conversions.toText(binObject, CodePosition.EMPTY).toString();
            if(!binString.startsWith("0b") && !binString.startsWith("0B"))
                return interpreter.setErrnoErrorObject(InterpretingError.NO_BIN_NUM, "Wrong prefix (Should be 0b or 0B)");

            try {
                return new DataObject().setInt(Integer.parseInt(binString.substring(2), 2));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.NO_BIN_NUM, e.getMessage());
            }
        }

        @LangFunction("octToDec")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject octToDecFunction(
                LangInterpreter interpreter,
                @LangParameter("$oct") DataObject octObject
        ) {
            String octString = interpreter.conversions.toText(octObject, CodePosition.EMPTY).toString();
            if(!octString.startsWith("0o") && !octString.startsWith("0O"))
                return interpreter.setErrnoErrorObject(InterpretingError.NO_OCT_NUM, "Wrong prefix (Should be 0o or 0O)");

            try {
                return new DataObject().setInt(Integer.parseInt(octString.substring(2), 8));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.NO_OCT_NUM, e.getMessage());
            }
        }

        @LangFunction("hexToDec")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject hexToDecFunction(
                LangInterpreter interpreter,
                @LangParameter("$hex") DataObject hexObject
        ) {
            String hexString = interpreter.conversions.toText(hexObject, CodePosition.EMPTY).toString();
            if(!hexString.startsWith("0x") && !hexString.startsWith("0X"))
                return interpreter.setErrnoErrorObject(InterpretingError.NO_HEX_NUM, "Wrong prefix (Should be 0x or 0X)");

            try {
                return new DataObject().setInt(Integer.parseInt(hexString.substring(2), 16));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.NO_HEX_NUM, e.getMessage());
            }
        }

        @LangFunction("toNumberBase")
        @AllowedTypes({DataObject.DataType.INT, DataObject.DataType.LONG})
        public static DataObject toNumberBaseFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") DataObject numberObject,
                @LangParameter("$base") @NumberValue Number base
        ) {
            if(base.intValue() < 2 || base.intValue() > 36)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_NUMBER_BASE,
                        "Argument 2 (\"$base\") must be between 2 (inclusive) and 36 (inclusive)");

            String numberString = interpreter.conversions.toText(numberObject, CodePosition.EMPTY).toString();

            try {
                return new DataObject().setInt(Integer.parseInt(numberString, base.intValue()));
            }catch(NumberFormatException e1) {
                try {
                    return new DataObject().setLong(Long.parseLong(numberString, base.intValue()));
                }catch(NumberFormatException e) {
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_BASE_N_NUM,
                            "Argument 1 (\"$number\" = \"" + numberString + "\") is not in base \"" + base.intValue() + "\"");
                }
            }
        }

        @LangFunction("toTextBase")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject toTextBaseFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number,
                @LangParameter("$base") @NumberValue Number base
        ) {
            if(base.intValue() < 2 || base.intValue() > 36)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_NUMBER_BASE,
                        "Argument 2 (\"$base\") must be between 2 (inclusive) and 36 (inclusive)");

            int numberInt = number.intValue();
            long numberLong = number.longValue();
            if(numberLong == (long)(numberInt))
                return new DataObject().setText(Integer.toString(number.intValue(), base.intValue()).toUpperCase(Locale.ENGLISH));
            else
                return new DataObject().setText(Long.toString(number.longValue(), base.intValue()).toUpperCase(Locale.ENGLISH));
        }

        @LangFunction("toIntBits")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject toIntBitsFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setInt(Float.floatToRawIntBits(number.floatValue()));
        }

        @LangFunction("toFloatBits")
        @AllowedTypes(DataObject.DataType.FLOAT)
        public static DataObject toFloatBitsFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setFloat(Float.intBitsToFloat(number.intValue()));
        }

        @LangFunction("toLongBits")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject toLongBitsFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setLong(Double.doubleToRawLongBits(number.doubleValue()));
        }

        @LangFunction("toDoubleBits")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject toDoubleBitsFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Double.longBitsToDouble(number.longValue()));
        }

        @LangFunction("ttoi")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject ttoiFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject
        ) {
            String str = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();
            try {
                return new DataObject().setInt(Integer.parseInt(str));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to an INT value");
            }
        }

        @LangFunction("ttol")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject ttolFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject
        ) {
            String str = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();
            try {
                return new DataObject().setLong(Long.parseLong(str));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a LONG value");
            }
        }

        @LangFunction("ttof")
        @AllowedTypes(DataObject.DataType.FLOAT)
        public static DataObject ttofFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject
        ) {
            String str = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();

            if(str.isEmpty())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a FLOAT value");

            char lastChar = str.charAt(str.length() - 1);
            if(str.trim().length() != str.length() || lastChar == 'f' || lastChar == 'F' || lastChar == 'd' ||
                    lastChar == 'D' || str.contains("x") || str.contains("X"))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a FLOAT value");

            try {
                return new DataObject().setFloat(Float.parseFloat(str));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a FLOAT value");
            }
        }

        @LangFunction("ttod")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject ttodFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject
        ) {
            String str = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();

            if(str.isEmpty())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a DOUBLE value");

            char lastChar = str.charAt(str.length() - 1);
            if(str.trim().length() != str.length() || lastChar == 'f' || lastChar == 'F' || lastChar == 'd' ||
                    lastChar == 'D' || str.contains("x") || str.contains("X"))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a DOUBLE value");

            try {
                return new DataObject().setDouble(Double.parseDouble(str));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$text\") can not be converted to a DOUBLE value");
            }
        }

        @LangFunction("isNaN")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isNaNFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            if(number instanceof Float)
                return new DataObject().setBoolean(Float.isNaN(number.floatValue()));

            if(number instanceof Double)
                return new DataObject().setBoolean(Double.isNaN(number.doubleValue()));

            return new DataObject().setBoolean(false);
        }

        @LangFunction("isInfinite")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isInfiniteFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            if(number instanceof Float)
                return new DataObject().setBoolean(Float.isInfinite(number.floatValue()));

            if(number instanceof Double)
                return new DataObject().setBoolean(Double.isInfinite(number.doubleValue()));

            return new DataObject().setBoolean(false);
        }

        @LangFunction("isFinite")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject isFiniteFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            if(number instanceof Float)
                return new DataObject().setBoolean(Float.isFinite(number.floatValue()));

            if(number instanceof Double)
                return new DataObject().setBoolean(Double.isFinite(number.doubleValue()));

            return new DataObject().setBoolean(false);
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedTextFunctions {
        private LangPredefinedTextFunctions() {}

        @LangFunction("toUpper")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject toUpperFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            return new DataObject().setText(interpreter.conversions.toText(textObject, CodePosition.EMPTY).toUpperCase());
        }

        @LangFunction("toLower")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject toLowerFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            return new DataObject().setText(interpreter.conversions.toText(textObject, CodePosition.EMPTY).toLowerCase());
        }

        @LangFunction("trim")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject trimFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            return new DataObject().setText(interpreter.conversions.toText(textObject, CodePosition.EMPTY).trim());
        }

        @LangFunction("replace")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject replaceFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$regex") DataObject regexObject,
                @LangParameter("$replacement") @VarArgs DataObject replacementObject
        ) {
            try {
                return new DataObject(LangRegEx.replace(interpreter.conversions.toText(textObject, CodePosition.EMPTY),
                        interpreter.conversions.toText(regexObject, CodePosition.EMPTY),
                        interpreter.conversions.toText(replacementObject, CodePosition.EMPTY)));
            }catch(InvalidPatternSyntaxException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_REGEX_SYNTAX, e.getMessage());
            }
        }

        @LangFunction("lpad")
        @LangInfo("Adds padding to the left of the $text value if needed")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject lpadFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$paddingText") DataObject paddingTextObject,
                @LangParameter("$len") @NumberValue Number lenNum
        ) {
            int len = lenNum.intValue();
            if(len < 0) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 3 \"$len\" must be >= 0");
            }

            String paddingText = interpreter.conversions.toText(paddingTextObject, CodePosition.EMPTY).toString();
            if(paddingText.isEmpty())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The padding text must not be empty");

            String text = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();

            if(text.length() >= len)
                return new DataObject(textObject);

            StringBuilder builder = new StringBuilder(text);
            while(builder.length() < len)
                builder.insert(0, paddingText);

            if(builder.length() > len)
                builder.delete(0, builder.length() - len);

            return new DataObject(builder.toString());
        }

        @LangFunction("rpad")
        @LangInfo("Adds padding to the right of the $text value if needed")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject rpadFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$paddingText") DataObject paddingTextObject,
                @LangParameter("$len") @NumberValue Number lenNum
        ) {
            int len = lenNum.intValue();
            if(len < 0) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 3 \"$len\" must be >= 0");
            }

            String paddingText = interpreter.conversions.toText(paddingTextObject, CodePosition.EMPTY).toString();
            if(paddingText.isEmpty())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The padding text must not be empty");

            String text = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();

            if(text.length() >= len)
                return new DataObject(textObject);

            StringBuilder builder = new StringBuilder(text);
            while(builder.length() < len)
                builder.append(paddingText);

            builder.delete(len, builder.length());

            return new DataObject(builder.toString());
        }

        @LangFunction("formatTemplatePluralization")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject formatTemplatePluralizationFunction(
                LangInterpreter interpreter,
                @LangParameter("$count") @NumberValue Number count,
                @LangParameter("$translationValue") @VarArgs DataObject translationValueObject
        ) {
            if(count.intValue() < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Count must be >= 0");

            String translationValue = interpreter.conversions.toText(translationValueObject, CodePosition.EMPTY).toString();

            try {
                return new DataObject(LangUtils.formatTranslationTemplatePluralization(translationValue, count.intValue()));
            }catch(NumberFormatException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_TEMPLATE_SYNTAX, "Invalid count range");
            }catch(InvalidTranslationTemplateSyntaxException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_TEMPLATE_SYNTAX, e.getMessage());
            }
        }

        @LangFunction("contains")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject containsFunction(
                LangInterpreter interpreter,
                @LangParameter("$haystack") DataObject haystackObject,
                @LangParameter("$needle") DataObject needleObject
        ) {
            return new DataObject().setBoolean(interpreter.conversions.toText(haystackObject, CodePosition.EMPTY).
                    contains(interpreter.conversions.toText(needleObject, CodePosition.EMPTY)));
        }

        @LangFunction(value="indexOf", hasInfo=true)
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject indexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$searchText") DataObject searchTextObject
        ) {
            return new DataObject().setInt(interpreter.conversions.toText(textObject, CodePosition.EMPTY).
                    indexOf(interpreter.conversions.toText(searchTextObject, CodePosition.EMPTY)));
        }
        @LangFunction("indexOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject indexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$searchText") DataObject searchTextObject,
                @LangParameter("$fromIndex") @NumberValue Number fromIndexNumber
        ) {
            DataObject.Text txt = interpreter.conversions.toText(textObject, CodePosition.EMPTY);
            int len = txt.length();
            int fromIndex = fromIndexNumber.intValue();
            if(fromIndex < 0)
                fromIndex += len;

            if(fromIndex < 0 || fromIndex >= len)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            return new DataObject().setInt(interpreter.conversions.toText(textObject, CodePosition.EMPTY).
                    indexOf(interpreter.conversions.toText(searchTextObject, CodePosition.EMPTY), fromIndex));
        }

        @LangFunction(value="lastIndexOf", hasInfo=true)
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject lastIndexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$searchText") DataObject searchTextObject
        ) {
            return new DataObject().setInt(interpreter.conversions.toText(textObject, CodePosition.EMPTY).
                    lastIndexOf(interpreter.conversions.toText(searchTextObject, CodePosition.EMPTY)));
        }
        @LangFunction("lastIndexOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject lastIndexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$searchText") DataObject searchTextObject,
                @LangParameter("$toIndex") @NumberValue Number toIndexNumber
        ) {
            DataObject.Text txt = interpreter.conversions.toText(textObject, CodePosition.EMPTY);
            int len = txt.length();
            int toIndex = toIndexNumber.intValue();
            if(toIndex < 0)
                toIndex += len;

            if(toIndex < 0 || toIndex >= len)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            return new DataObject().setInt(interpreter.conversions.toText(textObject, CodePosition.EMPTY).
                    lastIndexOf(interpreter.conversions.toText(searchTextObject, CodePosition.EMPTY), toIndex));
        }

        @LangFunction("startsWith")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject startsWithFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$prefix") DataObject prefixObject
        ) {
            return new DataObject().setBoolean(interpreter.conversions.toText(textObject, CodePosition.EMPTY).
                    startsWith(interpreter.conversions.toText(prefixObject, CodePosition.EMPTY)));
        }

        @LangFunction("endsWith")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject endsWithFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$suffix") DataObject suffixObject
        ) {
            return new DataObject().setBoolean(interpreter.conversions.toText(textObject, CodePosition.EMPTY).
                    endsWith(interpreter.conversions.toText(suffixObject, CodePosition.EMPTY)));
        }

        @LangFunction("matches")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject matchesFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$regex") DataObject regexObject
        ) {
            try {
                return new DataObject().setBoolean(LangRegEx.matches(interpreter.conversions.toText(textObject, CodePosition.EMPTY),
                        interpreter.conversions.toText(regexObject, CodePosition.EMPTY)));
            }catch(InvalidPatternSyntaxException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_REGEX_SYNTAX, e.getMessage());
            }
        }

        @LangFunction("repeatText")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject repeatTextFunction(
                LangInterpreter interpreter,
                @LangParameter("$count") @NumberValue Number count,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            if(count.intValue() < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Count must be >= 0");

            String text = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();

            StringBuilder builder = new StringBuilder();
            for(int i = 0;i < count.intValue();i++)
                builder.append(text);

            return new DataObject(builder.toString());
        }

        @LangFunction("charsOf")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject charsOfFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            DataObject.Text text = interpreter.conversions.toText(textObject, CodePosition.EMPTY);
            int[] chars = text.toCharArray();
            DataObject[] arr = new DataObject[chars.length];

            for(int i = 0;i < chars.length;i++)
                arr[i] = new DataObject().setChar(chars[i]);

            return new DataObject().setArray(arr);
        }

        @LangFunction("join")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject joinFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject,
                @LangParameter("&collection") @AllowedTypes({DataObject.DataType.ARRAY, DataObject.DataType.LIST}) DataObject collectionObject
        ) {
            String text = interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString();
            Stream<DataObject> dataObjectStream = collectionObject.getType() == DataType.ARRAY?Arrays.stream(collectionObject.getArray()):collectionObject.getList().stream();

            return new DataObject(dataObjectStream.map(ele ->
                    interpreter.conversions.toText(ele, CodePosition.EMPTY).toString()).collect(Collectors.joining(text)));
        }

        @LangFunction(value="split", hasInfo=true)
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject splitFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$regex") DataObject regexObject
        ) {
            return splitFunction(interpreter, textObject, regexObject, null);
        }
        @LangFunction("split")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject splitFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") DataObject textObject,
                @LangParameter("$regex") DataObject regexObject,
                @LangParameter("$maxSplitCount") @NumberValue Number maxSplitCount
        ) {
            DataObject.Text[] arrTmp;
            try {
                if(maxSplitCount == null || maxSplitCount.intValue() <= 0) {
                    arrTmp = LangRegEx.split(interpreter.conversions.toText(textObject, CodePosition.EMPTY),
                            interpreter.conversions.toText(regexObject, CodePosition.EMPTY));
                }else {
                    arrTmp = LangRegEx.split(interpreter.conversions.toText(textObject, CodePosition.EMPTY),
                            interpreter.conversions.toText(regexObject, CodePosition.EMPTY), maxSplitCount.intValue());
                }
            }catch(InvalidPatternSyntaxException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_REGEX_SYNTAX, e.getMessage());
            }

            DataObject[] arr = new DataObject[arrTmp.length];
            for(int i = 0;i < arr.length;i++)
                arr[i] = new DataObject(arrTmp[i]);

            return new DataObject().setArray(arr);
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedConversionFunctions {
        private LangPredefinedConversionFunctions() {}

        @LangFunction("number")
        @AllowedTypes({DataObject.DataType.INT, DataObject.DataType.LONG, DataObject.DataType.FLOAT, DataObject.DataType.DOUBLE})
        public static DataObject numberFunction(
                LangInterpreter interpreter,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject value = interpreter.conversions.convertToNumberAndCreateNewDataObject(valueObject, CodePosition.EMPTY);
            if(value.getType() == DataType.NULL)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "Argument 1 (\"$value\") can not be converted to type number");

            return value;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedOperationFunctions {
        private LangPredefinedOperationFunctions() {}

        @LangFunction("iter")
        public static DataObject iterFunction(
                LangInterpreter interpreter,
                @LangParameter("$operand") DataObject operand
        ) {
            DataObject ret = interpreter.operators.opIter(operand, CodePosition.EMPTY);
            if(ret == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The iter operator is not defined for " + operand.getType());

            return ret;
        }

        @LangFunction("hasNext")
        public static DataObject hasNextFunction(
                LangInterpreter interpreter,
                @LangParameter("$operand") DataObject operand
        ) {
            DataObject ret = interpreter.operators.opHasNext(operand, CodePosition.EMPTY);
            if(ret == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The hasNext operator is not defined for " + operand.getType());

            return ret;
        }

        @LangFunction("next")
        public static DataObject nextFunction(
                LangInterpreter interpreter,
                @LangParameter("$operand") DataObject operand
        ) {
            DataObject ret = interpreter.operators.opNext(operand, CodePosition.EMPTY);
            if(ret == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The next operator is not defined for " + operand.getType());

            return ret;
        }

        @LangFunction("cast")
        public static DataObject castFunction(
                LangInterpreter interpreter,
                @LangParameter("$leftSideOperand") DataObject leftSideOperand,
                @LangParameter("$rightSideOperand") DataObject rightSideOperand
        ) {
            DataObject ret = interpreter.operators.opCast(leftSideOperand, rightSideOperand, CodePosition.EMPTY);
            if(ret == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The cast operator is not defined for " + leftSideOperand.getType() + " and " + rightSideOperand.getType());

            return ret;
        }

        @LangFunction("call")
        public static DataObject callFunction(
                LangInterpreter interpreter,
                @LangParameter("$callee") DataObject callee,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject ret = interpreter.operators.opCall(callee, LangUtils.separateArgumentsWithArgumentSeparators(args),
                    CodePosition.EMPTY);
            if(ret == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "Invalid arguments for the call operator.");

            return ret;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedMathFunctions {
        private LangPredefinedMathFunctions() {}

        @LangFunction("rand")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject randFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setInt(interpreter.RAN.nextInt(interpreter.getData().var.get("$LANG_RAND_MAX").getInt()));
        }

        @LangFunction("randi")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject randiFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setInt(interpreter.RAN.nextInt());
        }

        @LangFunction("randl")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject randlFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setLong(interpreter.RAN.nextLong());
        }

        @LangFunction("randf")
        @AllowedTypes(DataObject.DataType.FLOAT)
        public static DataObject randfFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setFloat(interpreter.RAN.nextFloat());
        }

        @LangFunction("randd")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject randdFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setDouble(interpreter.RAN.nextDouble());
        }

        @LangFunction("randb")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject randbFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setBoolean(interpreter.RAN.nextBoolean());
        }

        @LangFunction("randRange")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject randRangeFunction(
                LangInterpreter interpreter,
                @LangParameter("$bound") @NumberValue Number boundNumber
        ) {
            int bound = boundNumber.intValue();
            if(bound <= 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 1 (\"$bound\") must be positive");

            return new DataObject().setInt(interpreter.RAN.nextInt(bound));
        }

        @LangFunction(value="randChoice", hasInfo=true)
        public static DataObject randChoiceWithArrayParameterFunction(
                LangInterpreter interpreter,
                @LangParameter("&arr") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            return arr.length == 0?null:arr[interpreter.RAN.nextInt(arr.length)];
        }
        @LangFunction("randChoice")
        public static DataObject randChoiceWithListParameterFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            List<DataObject> list = listObject.getList();
            return list.isEmpty()?null:list.get(interpreter.RAN.nextInt(list.size()));
        }
        @LangFunction("randChoice")
        public static DataObject randChoiceWithStructParameterFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();
            String[] memberNames = struct.getMemberNames();

            if(struct.isDefinition())
                return memberNames.length == 0?null:new DataObject(memberNames[interpreter.RAN.nextInt(memberNames.length)]);

            return memberNames.length == 0?null:struct.getMember(memberNames[interpreter.RAN.nextInt(memberNames.length)]);
        }
        @LangFunction("randChoice")
        public static DataObject randChoiceFunction(
                LangInterpreter interpreter,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            return args.isEmpty()?null:args.get(interpreter.RAN.nextInt(args.size()));
        }

        @LangFunction("setSeed")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject setSeedFunction(
                LangInterpreter interpreter,
                @LangParameter("$seed") @NumberValue Number seedNumber
        ) {
            interpreter.RAN.setSeed(seedNumber.longValue());

            return null;
        }

        @LangFunction("addi")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject addiFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            int sum = 0;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                sum += number.intValue();
            }

            return new DataObject().setInt(sum);
        }

        @LangFunction("muli")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject muliFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            int prod = 1;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                prod *= number.intValue();
            }

            return new DataObject().setInt(prod);
        }

        @LangFunction("addl")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject addlFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            long sum = 0;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                sum += number.longValue();
            }

            return new DataObject().setLong(sum);
        }

        @LangFunction("mull")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject mullFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            long prod = 1;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                prod *= number.longValue();
            }

            return new DataObject().setLong(prod);
        }

        @LangFunction("addf")
        @AllowedTypes(DataObject.DataType.FLOAT)
        public static DataObject addfFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            float sum = 0.f;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                sum += number.floatValue();
            }

            return new DataObject().setFloat(sum);
        }

        @LangFunction("mulf")
        @AllowedTypes(DataObject.DataType.FLOAT)
        public static DataObject mulfFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            float prod = 1.f;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                prod *= number.floatValue();
            }

            return new DataObject().setFloat(prod);
        }

        @LangFunction("addd")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject adddFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            double sum = 0.d;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                sum += number.doubleValue();
            }

            return new DataObject().setDouble(sum);
        }

        @LangFunction("muld")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject muldFunction(
                LangInterpreter interpreter,
                @LangParameter("&numbers") @VarArgs List<DataObject> numberObjects
        ) {
            double prod = 1.d;

            for(int i = 0;i < numberObjects.size();i++) {
                DataObject numberObject = numberObjects.get(i);
                Number number = interpreter.conversions.toNumber(numberObject, CodePosition.EMPTY);
                if(number == null)
                    return interpreter.setErrnoErrorObject(InterpretingError.NO_NUM,
                            "The type of argument " + (i + 1) + " (for var args parameter \"&numbers\") must be a number");

                prod *= number.doubleValue();
            }

            return new DataObject().setDouble(prod);
        }

        @LangFunction("sqrt")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject sqrtFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.sqrt(number.doubleValue()));
        }

        @LangFunction("cbrt")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject cbrtFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.cbrt(number.doubleValue()));
        }

        @LangFunction("hypot")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject hypotFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @NumberValue Number leftNumber,
                @LangParameter("$b") @NumberValue Number rightNumber
        ) {
            return new DataObject().setDouble(Math.hypot(leftNumber.doubleValue(), rightNumber.doubleValue()));
        }

        @LangFunction("toRadians")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject toRadiansFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.toRadians(number.doubleValue()));
        }

        @LangFunction("toDegrees")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject toDegreesFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.toDegrees(number.doubleValue()));
        }

        @LangFunction("sin")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject sinFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.sin(number.doubleValue()));
        }

        @LangFunction("cos")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject cosFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.cos(number.doubleValue()));
        }

        @LangFunction("tan")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject tanFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.tan(number.doubleValue()));
        }

        @LangFunction("asin")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject asinFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.asin(number.doubleValue()));
        }

        @LangFunction("acos")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject acosFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.acos(number.doubleValue()));
        }

        @LangFunction("atan")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject atanFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.atan(number.doubleValue()));
        }

        @LangFunction("atan2")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject atan2Function(
                LangInterpreter interpreter,
                @LangParameter("$a") @NumberValue Number leftNumber,
                @LangParameter("$b") @NumberValue Number rightNumber
        ) {
            return new DataObject().setDouble(Math.atan2(leftNumber.doubleValue(), rightNumber.doubleValue()));
        }

        @LangFunction("sinh")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject sinhFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.sinh(number.doubleValue()));
        }

        @LangFunction("cosh")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject coshFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.cosh(number.doubleValue()));
        }

        @LangFunction("tanh")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject tanhFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.tanh(number.doubleValue()));
        }

        @LangFunction("exp")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject expFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.exp(number.doubleValue()));
        }

        @LangFunction("loge")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject logeFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.log(number.doubleValue()));
        }

        @LangFunction("log10")
        @AllowedTypes(DataObject.DataType.DOUBLE)
        public static DataObject log10Function(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setDouble(Math.log10(number.doubleValue()));
        }

        @LangFunction("round")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject roundFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setLong((Math.signum(number.doubleValue()) < 0?-1:1) * Math.round(Math.abs(number.doubleValue())));
        }

        @LangFunction("ceil")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject ceilFunction(
                LangInterpreter interpreter,
                @LangParameter("$number") @NumberValue Number number
        ) {
            return new DataObject().setLong((long)Math.ceil(number.doubleValue()));
        }

        @LangFunction("floor")
        @AllowedTypes(DataObject.DataType.LONG)
        public static DataObject floorFunction(
                LangInterpreter interpreter,
                @LangParameter("$floor") @NumberValue Number number
        ) {
            return new DataObject().setLong((long)Math.floor(number.doubleValue()));
        }

        @LangFunction("abs")
        public static DataObject absFunction(
                LangInterpreter interpreter,
                @LangParameter("$operand") DataObject operand
        ) {
            DataObject ret = interpreter.operators.opAbs(operand, CodePosition.EMPTY);
            if(ret == null)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The abs operator is not defined for " + operand.getType());

            return ret;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedCombinatorFunctions {
        private LangPredefinedCombinatorFunctions() {}

        @LangFunction("combBN")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(b(args[0]), b(args[1]), ...)")
        public static DataObject combBNFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("$b") @CallableValue DataObject b,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            List<DataObject> argsA = new LinkedList<>();
            for(DataObject arg:args) {
                DataObject retB = interpreter.operators.opCall(b, Arrays.asList(
                        arg
                ), CodePosition.EMPTY);
                argsA.add(LangUtils.nullToLangVoid(retB));
            }
            argsA = LangUtils.separateArgumentsWithArgumentSeparators(argsA);

            return interpreter.operators.opCall(a, argsA, CodePosition.EMPTY);
        }

        @LangFunction("combBV")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(b(args[0]), b(args[1]), ...)")
        public static DataObject combBVFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("$b") @CallableValue DataObject b,
                @LangParameter("&args") @AllowedTypes(DataObject.DataType.ARRAY) DataObject args
        ) {
            List<DataObject> argsA = new LinkedList<>();
            for(DataObject ele:args.getArray()) {
                DataObject retB = interpreter.operators.opCall(b, Arrays.asList(
                        ele
                ), CodePosition.EMPTY);
                argsA.add(LangUtils.nullToLangVoid(retB));
            }
            argsA = LangUtils.separateArgumentsWithArgumentSeparators(argsA);

            return interpreter.operators.opCall(a, argsA, CodePosition.EMPTY);
        }

        @LangFunction("combBZ")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(..., b(args[1]), b(args[0]))")
        public static DataObject combBZFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("$b") @CallableValue DataObject b,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            List<DataObject> argsA = new LinkedList<>();
            for(int i = args.size() - 1;i >= 0;i--) {
                DataObject retB = interpreter.operators.opCall(b, Arrays.asList(
                        args.get(i)
                ), CodePosition.EMPTY);
                argsA.add(LangUtils.nullToLangVoid(retB));
            }
            argsA = LangUtils.separateArgumentsWithArgumentSeparators(argsA);

            return interpreter.operators.opCall(a, argsA, CodePosition.EMPTY);
        }

        @LangFunction("combNN")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(args[0])(args[1])(...)")
        public static DataObject combNNFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject ret = a;
            for(int i = 0;i < args.size();i++) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(ret))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The return value after iteration " + (i + 1) + " must be callable");

                ret = interpreter.operators.opCall(ret, Arrays.asList(
                        n
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(ret);
            }

            return ret;
        }

        @LangFunction("combNV")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(args[0])(args[1])(...)")
        public static DataObject combNVFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("&args") @AllowedTypes(DataObject.DataType.ARRAY) DataObject args
        ) {
            DataObject ret = a;
            for(int i = 0;i < args.getArray().length;i++) {
                DataObject n = args.getArray()[i];

                if(!LangUtils.isCallable(ret))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The return value after iteration " + (i + 1) + " must be callable");

                ret = interpreter.operators.opCall(ret, Arrays.asList(
                        n
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(ret);
            }

            return ret;
        }

        @LangFunction("combNZ")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(...)(args[1])(args[0])")
        public static DataObject combNZFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject ret = a;
            for(int i = args.size() - 1;i >= 0;i--) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(ret))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The return value after iteration " + (args.size() - i) + " must be callable");

                ret = interpreter.operators.opCall(ret, Arrays.asList(
                        n
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(ret);
            }

            return ret;
        }

        @LangFunction("combPN")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(args[0](b), args[1](b), ...)")
        public static DataObject combPNFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("$b") DataObject b,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            List<DataObject> argsA = new LinkedList<>();
            for(int i = 0;i < args.size();i++) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The value at index " + (i + 3) + " must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        b
                ), CodePosition.EMPTY);
                argsA.add(LangUtils.nullToLangVoid(retN));
            }
            argsA = LangUtils.separateArgumentsWithArgumentSeparators(argsA);

            return interpreter.operators.opCall(a, argsA, CodePosition.EMPTY);
        }

        @LangFunction("combPV")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(args[0](b), args[1](b), ...)")
        public static DataObject combPVFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("$b") DataObject b,
                @LangParameter("&args") @AllowedTypes(DataObject.DataType.ARRAY) DataObject args
        ) {
            List<DataObject> argsA = new LinkedList<>();
            for(int i = 0;i < args.getArray().length;i++) {
                DataObject n = args.getArray()[i];

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "Value at index " + i + " of Argument 3 (\"&args\") must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        b
                ), CodePosition.EMPTY);
                argsA.add(LangUtils.nullToLangVoid(retN));
            }
            argsA = LangUtils.separateArgumentsWithArgumentSeparators(argsA);

            return interpreter.operators.opCall(a, argsA, CodePosition.EMPTY);
        }

        @LangFunction("combPZ")
        @CombinatorFunction
        @LangInfo("Combinator execution: a(..., args[1](b), args[0](b))")
        public static DataObject combPZFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") @CallableValue DataObject a,
                @LangParameter("$b") DataObject b,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            List<DataObject> argsA = new LinkedList<>();
            for(int i = args.size() - 1;i >= 0;i--) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The value at index " + (i + 3) + " must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        b
                ), CodePosition.EMPTY);
                argsA.add(LangUtils.nullToLangVoid(retN));
            }
            argsA = LangUtils.separateArgumentsWithArgumentSeparators(argsA);

            return interpreter.operators.opCall(a, argsA, CodePosition.EMPTY);
        }

        @LangFunction("combQN")
        @CombinatorFunction
        @LangInfo("Combinator execution: ...(args[1](args[0](a)))")
        public static DataObject combQNFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") DataObject a,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject ret = a;
            for(int i = 0;i < args.size();i++) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The value at index " + (i + 2) + " must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        ret
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(retN);
            }

            return ret;
        }

        @LangFunction("combQV")
        @CombinatorFunction
        @LangInfo("Combinator execution: ...(args[1](args[0](a)))")
        public static DataObject combQVFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") DataObject a,
                @LangParameter("&args") @AllowedTypes(DataObject.DataType.ARRAY) DataObject args
        ) {
            DataObject ret = a;
            for(int i = 0;i < args.getArray().length;i++) {
                DataObject n = args.getArray()[i];

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "Value at index " + i + " of Argument 2 (\"&args\") must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        ret
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(retN);
            }

            return ret;
        }

        @LangFunction("combQZ")
        @CombinatorFunction
        @LangInfo("Combinator execution: args[0](args[1](...(a)))")
        public static DataObject combQZFunction(
                LangInterpreter interpreter,
                @LangParameter("$a") DataObject a,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            DataObject ret = a;
            for(int i = args.size() - 1;i >= 0;i--) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The value at index " + (i + 2) + " must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        ret
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(retN);
            }

            return ret;
        }

        @LangFunction("combTN")
        @CombinatorFunction
        @LangInfo("Combinator execution: ...(args[1](args[0](z)))")
        public static DataObject combTNFunction(
                LangInterpreter interpreter,
                @LangParameter("&args") @VarArgs List<DataObject> args,
                @LangParameter("$z") DataObject z
        ) {
            DataObject ret = z;
            for(int i = 0;i < args.size();i++) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The value at index " + (i + 1) + " must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        ret
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(retN);
            }

            return ret;
        }

        @LangFunction("combTV")
        @CombinatorFunction
        @LangInfo("Combinator execution: ...(args[1](args[0](z)))")
        public static DataObject combTVFunction(
                LangInterpreter interpreter,
                @LangParameter("&args") @AllowedTypes(DataObject.DataType.ARRAY) DataObject args,
                @LangParameter("$z") DataObject z
        ) {
            DataObject ret = z;
            for(int i = 0;i < args.getArray().length;i++) {
                DataObject n = args.getArray()[i];

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "Value at index " + i + " of Argument 2 (\"&args\") must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        ret
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(retN);
            }

            return ret;
        }

        @LangFunction("combTZ")
        @CombinatorFunction
        @LangInfo("Combinator execution: args[0](args[1](...(z)))")
        public static DataObject combTZFunction(
                LangInterpreter interpreter,
                @LangParameter("&args") @VarArgs List<DataObject> args,
                @LangParameter("$z") DataObject z
        ) {
            DataObject ret = z;
            for(int i = args.size() - 1;i >= 0;i--) {
                DataObject n = args.get(i);

                if(!LangUtils.isCallable(n))
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The value at index " + (i + 1) + " must be callable");

                DataObject retN = interpreter.operators.opCall(n, Arrays.asList(
                        ret
                ), CodePosition.EMPTY);
                ret = LangUtils.nullToLangVoid(retN);
            }

            return ret;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedFuncPtrFunctions {
        private LangPredefinedFuncPtrFunctions() {}

        @LangFunction("argCnt0")
        @AllowedTypes(DataObject.DataType.FUNCTION_POINTER)
        public static DataObject argCnt0Function(
                LangInterpreter interpreter,
                @LangParameter("$func") @CallableValue DataObject funcObject
        ) {
            String functionName;
            if(funcObject.getType() == DataObject.DataType.FUNCTION_POINTER) {
                functionName = funcObject.getFunctionPointer().getFunctionName();
                functionName = functionName == null?funcObject.getVariableName():functionName;
            }else {
                functionName = "<arg>";
            }

            return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction("argCnt0-func")
                public DataObject argCnt0FuncFunction() {
                    return interpreter.operators.opCall(funcObject, new LinkedList<>(), CodePosition.EMPTY);
                }
            }, funcObject).withFunctionName("<argCnt0(" + functionName + ")>"));
        }

        @LangFunction("argCnt1")
        @AllowedTypes(DataObject.DataType.FUNCTION_POINTER)
        public static DataObject argCnt1Function(
                LangInterpreter interpreter,
                @LangParameter("$func") @CallableValue DataObject funcObject
        ) {
            String functionName;
            if(funcObject.getType() == DataObject.DataType.FUNCTION_POINTER) {
                functionName = funcObject.getFunctionPointer().getFunctionName();
                functionName = functionName == null?funcObject.getVariableName():functionName;
            }else {
                functionName = "<arg>";
            }

            return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction("argCnt1-func")
                public DataObject argCnt1FuncFunction(
                        @LangParameter("$a") DataObject a
                ) {
                    return interpreter.operators.opCall(funcObject, Arrays.asList(
                            a
                    ), CodePosition.EMPTY);
                }
            }, funcObject).withFunctionName("<argCnt1(" + functionName + ")>"));
        }

        @LangFunction("argCnt2")
        @AllowedTypes(DataObject.DataType.FUNCTION_POINTER)
        public static DataObject argCnt2Function(
                LangInterpreter interpreter,
                @LangParameter("$func") @CallableValue DataObject funcObject
        ) {
            String functionName;
            if(funcObject.getType() == DataObject.DataType.FUNCTION_POINTER) {
                functionName = funcObject.getFunctionPointer().getFunctionName();
                functionName = functionName == null?funcObject.getVariableName():functionName;
            }else {
                functionName = "<arg>";
            }

            return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction("argCnt2-func")
                public DataObject argCnt2FuncFunction(
                        @LangParameter("$a") DataObject a,
                        @LangParameter("$b") DataObject b
                ) {
                    return interpreter.operators.opCall(funcObject, LangUtils.asListWithArgumentSeparators(
                            a, b
                    ), CodePosition.EMPTY);
                }
            }, funcObject).withFunctionName("<argCnt2(" + functionName + ")>"));
        }

        @LangFunction("argCnt3")
        @AllowedTypes(DataObject.DataType.FUNCTION_POINTER)
        public static DataObject argCnt3Function(
                LangInterpreter interpreter,
                @LangParameter("$func") @CallableValue DataObject funcObject
        ) {
            String functionName;
            if(funcObject.getType() == DataObject.DataType.FUNCTION_POINTER) {
                functionName = funcObject.getFunctionPointer().getFunctionName();
                functionName = functionName == null?funcObject.getVariableName():functionName;
            }else {
                functionName = "<arg>";
            }

            return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction("argCnt3-func")
                public DataObject argCnt3FuncFunction(
                        @LangParameter("$a") DataObject a,
                        @LangParameter("$b") DataObject b,
                        @LangParameter("$c") DataObject c
                ) {
                    return interpreter.operators.opCall(funcObject, LangUtils.asListWithArgumentSeparators(
                            a, b, c
                    ), CodePosition.EMPTY);
                }
            }, funcObject).withFunctionName("<argCnt3(" + functionName + ")>"));
        }

        @LangFunction("argCnt4")
        @AllowedTypes(DataObject.DataType.FUNCTION_POINTER)
        public static DataObject argCnt4Function(
                LangInterpreter interpreter,
                @LangParameter("$func") @CallableValue DataObject funcObject
        ) {
            String functionName;
            if(funcObject.getType() == DataObject.DataType.FUNCTION_POINTER) {
                functionName = funcObject.getFunctionPointer().getFunctionName();
                functionName = functionName == null?funcObject.getVariableName():functionName;
            }else {
                functionName = "<arg>";
            }

            return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction("argCnt4-func")
                public DataObject argCnt4FuncFunction(
                        @LangParameter("$a") DataObject a,
                        @LangParameter("$b") DataObject b,
                        @LangParameter("$c") DataObject c,
                        @LangParameter("$d") DataObject d
                ) {
                    return interpreter.operators.opCall(funcObject, LangUtils.asListWithArgumentSeparators(
                            a, b, c, d
                    ), CodePosition.EMPTY);
                }
            }, funcObject).withFunctionName("<argCnt4(" + functionName + ")>"));
        }

        @LangFunction("argCnt5")
        @AllowedTypes(DataObject.DataType.FUNCTION_POINTER)
        public static DataObject argCnt5Function(
                LangInterpreter interpreter,
                @LangParameter("$func") @CallableValue DataObject funcObject
        ) {
            String functionName;
            if(funcObject.getType() == DataObject.DataType.FUNCTION_POINTER) {
                functionName = funcObject.getFunctionPointer().getFunctionName();
                functionName = functionName == null?funcObject.getVariableName():functionName;
            }else {
                functionName = "<arg>";
            }

            return new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                @LangFunction("argCnt5-func")
                public DataObject argCnt5FuncFunction(
                        @LangParameter("$a") DataObject a,
                        @LangParameter("$b") DataObject b,
                        @LangParameter("$c") DataObject c,
                        @LangParameter("$d") DataObject d,
                        @LangParameter("$e") DataObject e
                ) {
                    return interpreter.operators.opCall(funcObject, LangUtils.asListWithArgumentSeparators(
                            a, b, c, d, e
                    ), CodePosition.EMPTY);
                }
            }, funcObject).withFunctionName("<argCnt5(" + functionName + ")>"));
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedByteBufferFunctions {
        private LangPredefinedByteBufferFunctions() {}

        @LangFunction("byteBufferCreate")
        @AllowedTypes(DataObject.DataType.BYTE_BUFFER)
        public static DataObject byteBufferCreateFunction(
                LangInterpreter interpreter,
                @LangParameter("$length") @NumberValue Number lengthNumber
        ) {
            int length = lengthNumber.intValue();

            if(length < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.NEGATIVE_ARRAY_LEN);

            return new DataObject().setByteBuffer(new byte[length]);
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedArrayFunctions {
        private LangPredefinedArrayFunctions() {}

        @LangFunction("arrayCreate")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayCreateFunction(
                LangInterpreter interpreter,
                @LangParameter("$length") @NumberValue Number lengthNumber
        ) {
            int length = lengthNumber.intValue();

            if(length < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.NEGATIVE_ARRAY_LEN);

            DataObject[] arr = new DataObject[length];
            for(int i = 0;i < arr.length;i++)
                arr[i] = new DataObject();

            return new DataObject().setArray(arr);
        }

        @LangFunction("arrayOf")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&elements") @VarArgs List<DataObject> elements
        ) {
            elements = elements.stream().map(DataObject::new).collect(Collectors.toList());

            return new DataObject().setArray(elements.toArray(new DataObject[0]));
        }

        @LangFunction("arrayGenerateFrom")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayGenerateFromFunction(
                LangInterpreter interpreter,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject funcPointerObject,
                @LangParameter("$count") @NumberValue Number countNumber
        ) {
            if(countNumber.intValue() < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.NEGATIVE_ARRAY_LEN);

            return new DataObject().setArray(IntStream.range(0, countNumber.intValue()).mapToObj(i -> {
                return interpreter.callFunctionPointer(funcPointerObject.getFunctionPointer(), funcPointerObject.getVariableName(), Arrays.asList(
                        new DataObject().setInt(i)
                ));
            }).toArray(DataObject[]::new));
        }

        @LangFunction("arrayZip")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayZipFunction(
                LangInterpreter interpreter,
                @LangParameter("&arrays") @AllowedTypes(DataObject.DataType.ARRAY) @VarArgs List<DataObject> arrays
        ) {
            int len = 0;
            for(int i = 0;i < arrays.size();i++) {
                int lenTest = arrays.get(i).getArray().length;
                if(i == 0) {
                    len = lenTest;

                    continue;
                }

                if(len != lenTest)
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The size of argument " + (i + 1) + " (for var args parameter \"&arrays\") must be " + len);
            }

            DataObject[] zippedArray = new DataObject[len];
            for(int i = 0;i < len;i++) {
                DataObject[] arr = new DataObject[arrays.size()];
                for(int j = 0;j < arr.length;j++)
                    arr[j] = new DataObject(arrays.get(j).getArray()[i]);

                zippedArray[i] = new DataObject().setArray(arr);
            }

            return new DataObject().setArray(zippedArray);
        }

        @LangFunction("arraySet")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arraySetFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$index") @NumberValue Number indexNumber,
                @LangParameter("$value") DataObject valueObject
        ) {
            int index = indexNumber.intValue();

            DataObject[] arr = arrayObject.getArray();
            if(index < 0)
                index += arr.length;

            if(index < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(index >= arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            arr[index] = new DataObject(valueObject);

            return null;
        }

        @LangFunction(value="arraySetAll", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arraySetAllFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();

            for(int i = 0;i < arr.length;i++)
                arr[i] = new DataObject(valueObject);

            return null;
        }
        @LangFunction("arraySetAll")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arraySetAllFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("&values") @VarArgs List<DataObject> values
        ) {
            DataObject[] arr = arrayObject.getArray();

            if(values.size() < arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT,
                        "The var args argument (\"&values\") has not enough values (" + arr.length + " needed)");
            if(values.size() > arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT,
                        "The var args argument (\"&values\") has too many values (" + arr.length + " needed)");

            Iterator<DataObject> valueIterator = values.iterator();
            for(int i = 0;i < arr.length;i++)
                arr[i] = new DataObject(valueIterator.next());

            return null;
        }

        @LangFunction("arrayGet")
        public static DataObject arrayGetFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$index") @NumberValue Number indexNumber
        ) {
            int index = indexNumber.intValue();

            DataObject[] arr = arrayObject.getArray();
            if(index < 0)
                index += arr.length;

            if(index < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(index >= arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            return arr[index];
        }

        @LangFunction("arrayGetAll")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject arrayGetAllFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            return new DataObject(Arrays.stream(arrayObject.getArray()).map(ele ->
                    interpreter.conversions.toText(ele, CodePosition.EMPTY).toString()).collect(Collectors.joining(", ")));
        }

        @LangFunction("arrayRead")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayReadFunction(
                LangInterpreter interpreter,
                @LangParameter("&arraySetAll") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("&pointers") @AllowedTypes(DataObject.DataType.VAR_POINTER) @VarArgs List<DataObject> pointers
        ) {
            DataObject[] arr = arrayObject.getArray();

            if(pointers.size() < arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT,
                        "The var args argument (\"&pointers\") has not enough values (" + arr.length + " needed)");
            if(pointers.size() > arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT,
                        "The var args argument (\"&pointers\") has too many values (" + arr.length + " needed)");

            for(int i = 0;i < pointers.size();i++) {
                DataObject dereferencedPointer = pointers.get(i).getVarPointer().getVar();
                if(dereferencedPointer.isFinalData() || dereferencedPointer.isLangVar())
                    return interpreter.setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE,
                            "For the dereferenced pointer (argument " + (i + 1) +
                                    " for var args parameter (\"&pointers\"))");

                if(!dereferencedPointer.getTypeConstraint().isTypeAllowed(arr[i].getType()))
                    return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                            "The dereferenced pointer (argument " + (i + 1) +
                                    " for var args parameter (\"&pointers\")) does not allow the type " + arr[i].getType());

                dereferencedPointer.setData(arr[i]);
            }

            return null;
        }

        @LangFunction("arrayFill")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayFillFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            for(int i = 0;i < arr.length;i++)
                arr[i] = new DataObject(valueObject);

            return null;
        }

        @LangFunction("arrayFillFrom")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayFillFromFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$startIndex") @NumberValue Number startIndexNumber,
                @LangParameter("$value") DataObject valueObject
        ) {
            int startIndex = startIndexNumber.intValue();

            DataObject[] arr = arrayObject.getArray();
            if(startIndex < 0)
                startIndex += arr.length;

            if(startIndex < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(startIndex >= arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            for(int i = startIndex;i < arr.length;i++)
                arr[i] = new DataObject(valueObject);

            return null;
        }

        @LangFunction("arrayFillTo")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayFillToFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$endIndex") @NumberValue Number endIndexNumber,
                @LangParameter("$value") DataObject valueObject
        ) {
            int endIndex = endIndexNumber.intValue();

            DataObject[] arr = arrayObject.getArray();
            if(endIndex < 0)
                endIndex += arr.length;

            if(endIndex < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(endIndex >= arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            for(int i = 0;i <= endIndex;i++)
                arr[i] = new DataObject(valueObject);

            return null;
        }

        @LangFunction("arrayCountOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayCountOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            long count = Arrays.stream(arr).filter(ele -> interpreter.operators.isStrictEquals(ele, valueObject, CodePosition.EMPTY)).count();
            return new DataObject().setInt((int)count);
        }

        @LangFunction("arrayIndexOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayIndexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            for(int i = 0;i < arr.length;i++)
                if(interpreter.operators.isStrictEquals(arr[i], valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("arrayLastIndexOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayLastIndexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            for(int i = arr.length - 1;i >= 0;i--)
                if(interpreter.operators.isStrictEquals(arr[i], valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("arrayCountLike")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayCountLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            long count = Arrays.stream(arr).filter(ele -> interpreter.operators.isEquals(ele, valueObject, CodePosition.EMPTY)).count();
            return new DataObject().setInt((int)count);
        }

        @LangFunction("arrayIndexLike")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayIndexLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            for(int i = 0;i < arr.length;i++)
                if(interpreter.operators.isEquals(arr[i], valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("arrayLastIndexLike")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayLastIndexLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            for(int i = arr.length - 1;i >= 0;i--)
                if(interpreter.operators.isEquals(arr[i], valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("arrayLength")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayLengthFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            return new DataObject().setInt(arrayObject.getArray().length);
        }

        @LangFunction("arrayDistinctValuesOf")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayDistinctValuesOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            DataObject[] arr = arrayObject.getArray();

            List<DataObject> distinctValues = new LinkedList<>();
            for(DataObject ele:arr) {
                boolean flag = true;
                for(DataObject distinctEle:distinctValues) {
                    if(interpreter.operators.isStrictEquals(ele, distinctEle, CodePosition.EMPTY)) {
                        flag = false;
                        break;
                    }
                }

                if(flag)
                    distinctValues.add(new DataObject(ele));
            }

            return new DataObject().setArray(distinctValues.toArray(new DataObject[0]));
        }

        @LangFunction("arrayDistinctValuesLike")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayDistinctValuesLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            DataObject[] arr = arrayObject.getArray();

            List<DataObject> distinctValues = new LinkedList<>();
            for(DataObject ele:arr) {
                boolean flag = true;
                for(DataObject distinctEle:distinctValues) {
                    if(interpreter.operators.isEquals(ele, distinctEle, CodePosition.EMPTY)) {
                        flag = false;
                        break;
                    }
                }

                if(flag)
                    distinctValues.add(new DataObject(ele));
            }

            return new DataObject().setArray(distinctValues.toArray(new DataObject[0]));
        }

        @LangFunction("arraySorted")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arraySortedFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.comparator") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject comparatorObject
        ) {
            DataObject[] arr = arrayObject.getArray();

            return new DataObject().setArray(Arrays.stream(arr).map(DataObject::new).sorted((a, b) -> {
                DataObject retObject = interpreter.callFunctionPointer(comparatorObject.getFunctionPointer(), comparatorObject.getVariableName(),
                        LangUtils.asListWithArgumentSeparators(
                                a, b
                        ));
                Number retNumber = interpreter.conversions.toNumber(retObject, CodePosition.EMPTY);
                if(retNumber == null) {
                    interpreter.setErrno(InterpretingError.NO_NUM, "The value returned by Argument 2 (\"fp.comparator\") must be a number.");

                    return 0;
                }

                return retNumber.intValue();
            }).toArray(DataObject[]::new));
        }

        @LangFunction("arrayFiltered")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayFilteredFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.filter") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject filterObject
        ) {
            DataObject[] arr = arrayObject.getArray();

            return new DataObject().setArray(Arrays.stream(arr).map(DataObject::new).filter(dataObject -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(filterObject.getFunctionPointer(),
                        filterObject.getVariableName(), Arrays.asList(
                                dataObject
                        )), CodePosition.EMPTY);
            }).toArray(DataObject[]::new));
        }

        @LangFunction("arrayFilteredCount")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayFilteredCountFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.filter") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject filterObject
        ) {
            DataObject[] arr = arrayObject.getArray();

            long count = Arrays.stream(arr).map(DataObject::new).filter(dataObject -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(filterObject.getFunctionPointer(),
                        filterObject.getVariableName(), Arrays.asList(
                                dataObject
                        )), CodePosition.EMPTY);
            }).count();
            return new DataObject().setInt((int)count);
        }

        @LangFunction("arrayMap")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayMapFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.map") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject mapFunction
        ) {
            DataObject[] arr = arrayObject.getArray();

            for(int i = 0;i < arr.length;i++) {
                arr[i] = interpreter.callFunctionPointer(mapFunction.getFunctionPointer(), mapFunction.getVariableName(), Arrays.asList(
                        arr[i]
                ));
            }

            return null;
        }

        @LangFunction("arrayMapToNew")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayMapToNewFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.map") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject mapFunction
        ) {
            DataObject[] arr = arrayObject.getArray();

            DataObject[] newArr = new DataObject[arr.length];
            for(int i = 0;i < arr.length;i++) {
                newArr[i] = interpreter.callFunctionPointer(mapFunction.getFunctionPointer(), mapFunction.getVariableName(), Arrays.asList(
                        arr[i]
                ));
            }

            return new DataObject().setArray(newArr);
        }

        @LangFunction(value="arrayMapToOne", hasInfo=true)
        @LangInfo("Alias for \"func.arrayReduce()\"")
        public static DataObject arrayMapToOneFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            return arrayReduceFunction(interpreter, arrayObject, combineFunction);
        }
        @LangFunction("arrayMapToOne")
        public static DataObject arrayMapToOneFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$initialValue") DataObject initialValueObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            return arrayReduceFunction(interpreter, arrayObject, initialValueObject, combineFunction);
        }

        @LangFunction(value="arrayReduce", hasInfo=true)
        public static DataObject arrayReduceFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            return arrayReduceFunction(interpreter, arrayObject, null, combineFunction);
        }
        @LangFunction("arrayReduce")
        public static DataObject arrayReduceFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$initialValue") DataObject initialValueObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            DataObject[] arr = arrayObject.getArray();

            DataObject currentValueObject = initialValueObject;

            for(DataObject ele:arr) {
                if(currentValueObject == null) {
                    //Set first element as currentValue if no initial value was provided

                    currentValueObject = ele;

                    continue;
                }

                currentValueObject = interpreter.callFunctionPointer(combineFunction.getFunctionPointer(), combineFunction.getVariableName(),
                        LangUtils.asListWithArgumentSeparators(
                                currentValueObject,
                                ele
                        ));
            }

            return currentValueObject;
        }

        @LangFunction(value="arrayReduceColumn", hasInfo=true)
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayReduceColumnFunction(
                LangInterpreter interpreter,
                @LangParameter("&arrays") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObjects,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            return arrayReduceColumnFunction(interpreter, arrayObjects, null, combineFunction);
        }
        @LangFunction("arrayReduceColumn")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayReduceColumnFunction(
                LangInterpreter interpreter,
                @LangParameter("&arrays") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObjects,
                @LangParameter("$initialValue") DataObject initialValueObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            DataObject[] arrayOfArrays = arrayObjects.getArray();

            int len = -1;
            List<DataObject[]> arrays = new LinkedList<>();
            for(int i = 0;i < arrayOfArrays.length;i++) {
                DataObject arg = arrayOfArrays[i];
                if(arg.getType() != DataType.ARRAY)
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The element at index " + i + " of argument 1 (\"&arrays\") must be of type " + DataObject.DataType.ARRAY);

                arrays.add(arg.getArray());

                int lenTest = arg.getArray().length;
                if(len == -1) {
                    len = lenTest;

                    continue;
                }

                if(len != lenTest)
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The length of the array at index " + i + " of argument 1 (\"&arrays\") must be " + len);
            }

            if(arrays.isEmpty())
                return new DataObject().setArray(new DataObject[0]);

            DataObject[] reducedArrays = new DataObject[len];
            for(int i = 0;i < len;i++) {
                DataObject currentValueObject = initialValueObject;

                for(DataObject[] arr:arrays) {
                    DataObject ele = arr[i];

                    if(currentValueObject == null) {
                        //Set first element as currentValue if no initial value was provided
                        currentValueObject = ele;

                        continue;
                    }

                    currentValueObject = interpreter.callFunctionPointer(combineFunction.getFunctionPointer(), combineFunction.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    currentValueObject,
                                    ele
                            ));
                }

                reducedArrays[i] = LangUtils.nullToLangVoid(currentValueObject);
            }

            return new DataObject().setArray(reducedArrays);
        }

        @LangFunction(value="arrayForEach", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayForEachFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            return arrayForEachFunction(interpreter, arrayObject, functionObject, false);
        }
        @LangFunction("arrayForEach")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayForEachFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject,
                @LangParameter("$breakable") @BooleanValue boolean breakable
        ) {
            DataObject[] arr = arrayObject.getArray();

            if(breakable) {
                boolean[] shouldBreak = new boolean[] {false};

                DataObject breakFunc = new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                    @LangFunction("break")
                    @AllowedTypes(DataObject.DataType.VOID)
                    public DataObject breakFunction() {
                        shouldBreak[0] = true;

                        return null;
                    }
                }));

                for(DataObject ele:arr) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    ele,
                                    breakFunc
                            ));

                    if(shouldBreak[0])
                        break;
                }
            }else {
                for(DataObject ele:arr) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(), Arrays.asList(
                            ele
                    ));
                }
            }

            return null;
        }

        @LangFunction(value="arrayEnumerate", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayEnumerateFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            return arrayEnumerateFunction(interpreter, arrayObject, functionObject, false);
        }
        @LangFunction("arrayEnumerate")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayEnumerateFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject,
                @LangParameter("$breakable") @BooleanValue boolean breakable
        ) {
            DataObject[] arr = arrayObject.getArray();

            if(breakable) {
                boolean[] shouldBreak = new boolean[] {false};

                DataObject breakFunc = new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                    @LangFunction("break")
                    @AllowedTypes(DataObject.DataType.VOID)
                    public DataObject breakFunction() {
                        shouldBreak[0] = true;

                        return null;
                    }
                }));

                for(int i = 0;i < arr.length;i++) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    new DataObject().setInt(i),
                                    arr[i],
                                    breakFunc
                            ));

                    if(shouldBreak[0])
                        break;
                }
            }else {
                for(int i = 0;i < arr.length;i++) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    new DataObject().setInt(i),
                                    arr[i]
                            ));
                }
            }

            return null;
        }

        @LangFunction("arrayAllMatch")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayAllMatchFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.predicate") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject predicateFunction
        ) {
            DataObject[] arr = arrayObject.getArray();

            return new DataObject().setBoolean(Arrays.stream(arr).map(DataObject::new).allMatch(ele -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(predicateFunction.getFunctionPointer(),
                        predicateFunction.getVariableName(), Arrays.asList(
                                ele
                        )), CodePosition.EMPTY);
            }));
        }

        @LangFunction("arrayAnyMatch")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayAnyMatchFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.predicate") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject predicateFunction
        ) {
            DataObject[] arr = arrayObject.getArray();

            return new DataObject().setBoolean(Arrays.stream(arr).map(DataObject::new).anyMatch(ele -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(predicateFunction.getFunctionPointer(),
                        predicateFunction.getVariableName(), Arrays.asList(
                                ele
                        )), CodePosition.EMPTY);
            }));
        }

        @LangFunction("arrayNoneMatch")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject arrayNoneMatchFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.predicate") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject predicateFunction
        ) {
            DataObject[] arr = arrayObject.getArray();

            return new DataObject().setBoolean(Arrays.stream(arr).map(DataObject::new).noneMatch(ele -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(predicateFunction.getFunctionPointer(),
                        predicateFunction.getVariableName(), Arrays.asList(
                                ele
                        )), CodePosition.EMPTY);
            }));
        }

        @LangFunction("arrayCombine")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayCombineFunction(
                LangInterpreter interpreter,
                @LangParameter("&arrays") @AllowedTypes(DataObject.DataType.ARRAY) @VarArgs List<DataObject> arrayObjects
        ) {
            List<DataObject> combinedArrays = new LinkedList<>();

            for(DataObject arrayObject:arrayObjects)
                for(DataObject ele:arrayObject.getArray())
                    combinedArrays.add(new DataObject(ele));

            return new DataObject().setArray(combinedArrays.toArray(new DataObject[0]));
        }

        @LangFunction(value="arrayPermutations", hasInfo=true)
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayPermutationsFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            return arrayPermutationsFunction(interpreter, arrayObject, (Integer)arrayObject.getArray().length);
        }
        @LangFunction("arrayPermutations")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject arrayPermutationsFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("$r")
                @LangInfo("The amount of selected items per permutation")
                @NumberValue Number countNumber
        ) {
            DataObject[] arr = arrayObject.getArray();

            int count = countNumber.intValue();

            if(count < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 2 (\"$count\") must be >= 0!");

            if(count > arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 2 (\"$count\") must be <= " + arr.length + "!");

            if(arr.length == 0 || count == 0)
                return new DataObject().setArray(new DataObject[0]);

            List<DataObject> permutations = new LinkedList<>();
            int[] indices = new int[count];
            int currentPermutationIndex = count - 1;
            for(int i = 0;i < count;i++)
                indices[i] = i;

            outer:
            while(true) {
                DataObject[] permutationArr = new DataObject[count];
                for(int i = 0;i < count;i++)
                    permutationArr[i] = new DataObject(arr[indices[i]]);
                permutations.add(new DataObject().setArray(permutationArr));

                List<Integer> usedIndices = new LinkedList<>();
                for(int i = 0;i < currentPermutationIndex;i++)
                    usedIndices.add((Integer)indices[i]);

                for(;currentPermutationIndex < count;currentPermutationIndex++) {
                    int index = indices[currentPermutationIndex] + 1;
                    while(usedIndices.contains((Integer)index))
                        index++;

                    if(index == arr.length) {
                        if(!usedIndices.isEmpty())
                            usedIndices.remove(usedIndices.size() - 1);

                        indices[currentPermutationIndex] = -1;
                        currentPermutationIndex -= 2; //Will be incremented in for loop
                        if(currentPermutationIndex < -1)
                            break outer;

                        continue;
                    }

                    indices[currentPermutationIndex] = index;

                    usedIndices.add((Integer)index);
                }
                currentPermutationIndex = count - 1;
            }

            return new DataObject().setArray(permutations.toArray(new DataObject[0]));
        }

        @LangFunction(value="arrayPermutationsForEach", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayPermutationsForEachFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.func")
                @LangInfo("If the value returned by fp.func evaluates to true, this function will stop the execution early.")
                @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            return arrayPermutationsForEachFunction(interpreter, arrayObject, functionObject, (Integer)arrayObject.getArray().length);
        }
        @LangFunction("arrayPermutationsForEach")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayPermutationsForEachFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject,
                @LangParameter("fp.func")
                @LangInfo("If the value returned by fp.func evaluates to true, this function will stop the execution early.")
                @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject,
                @LangParameter("$r")
                @LangInfo("The amount of selected items per permutation")
                @NumberValue Number countNumber
        ) {
            DataObject[] arr = arrayObject.getArray();

            int count = countNumber.intValue();

            if(count < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 3 (\"$count\") must be >= 0!");

            if(count > arr.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Argument 3 (\"$count\") must be <= " + arr.length + "!");

            if(arr.length == 0 || count == 0)
                return new DataObject().setArray(new DataObject[0]);

            int[] indices = new int[count];
            int currentPermutationIndex = count - 1;
            for(int i = 0;i < count;i++)
                indices[i] = i;

            int permutationNumber = 0;

            outer:
            while(true) {
                DataObject[] permutationArr = new DataObject[count];
                for(int i = 0;i < count;i++)
                    permutationArr[i] = new DataObject(arr[indices[i]]);

                if(interpreter.conversions.toBool(interpreter.callFunctionPointer(functionObject.getFunctionPointer(),
                        functionObject.getVariableName(), LangUtils.asListWithArgumentSeparators(
                                new DataObject().setArray(permutationArr),
                                new DataObject().setInt(permutationNumber++)
                        )), CodePosition.EMPTY))
                    return null;

                List<Integer> usedIndices = new LinkedList<>();
                for(int i = 0;i < currentPermutationIndex;i++)
                    usedIndices.add((Integer)indices[i]);

                for(;currentPermutationIndex < count;currentPermutationIndex++) {
                    int index = indices[currentPermutationIndex] + 1;
                    while(usedIndices.contains((Integer)index))
                        index++;

                    if(index == arr.length) {
                        if(!usedIndices.isEmpty())
                            usedIndices.remove(usedIndices.size() - 1);

                        indices[currentPermutationIndex] = -1;
                        currentPermutationIndex -= 2; //Will be incremented in for loop
                        if(currentPermutationIndex < -1)
                            break outer;

                        continue;
                    }

                    indices[currentPermutationIndex] = index;

                    usedIndices.add((Integer)index);
                }
                currentPermutationIndex = count - 1;
            }

            return null;
        }

        @LangFunction("arrayReset")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject arrayResetFunction(
                LangInterpreter interpreter,
                @LangParameter("&array") @AllowedTypes(DataObject.DataType.ARRAY) DataObject arrayObject
        ) {
            DataObject[] arr = arrayObject.getArray();
            for(DataObject ele:arr)
                ele.setNull();

            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedListFunctions {
        private LangPredefinedListFunctions() {}

        @LangFunction("listCreate")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listCreateFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setList(new LinkedList<>());
        }

        @LangFunction("listOf")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&elements") @VarArgs List<DataObject> elements
        ) {
            elements = elements.stream().map(DataObject::new).collect(Collectors.toList());

            return new DataObject().setList(new LinkedList<>(elements));
        }

        @LangFunction("listGenerateFrom")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listGenerateFromFunction(
                LangInterpreter interpreter,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject funcPointerObject,
                @LangParameter("$count") @NumberValue Number countNumber
        ) {
            List<DataObject> elements = IntStream.range(0, countNumber.intValue()).mapToObj(i -> {
                return interpreter.callFunctionPointer(funcPointerObject.getFunctionPointer(), funcPointerObject.getVariableName(), Arrays.asList(
                        new DataObject().setInt(i)
                ));
            }).collect(Collectors.toList());
            return new DataObject().setList(new LinkedList<>(elements));
        }

        @LangFunction("listZip")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listZipFunction(
                LangInterpreter interpreter,
                @LangParameter("&lists") @AllowedTypes(DataObject.DataType.LIST) @VarArgs List<DataObject> lists
        ) {
            int len = -1;
            for(int i = 0;i < lists.size();i++) {
                int lenTest = lists.get(i).getList().size();
                if(len == -1) {
                    len = lenTest;

                    continue;
                }

                if(len != lenTest)
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The size of argument " + (i + 1) + " (for var args parameter \"&lists\") must be " + len);
            }

            if(len == -1)
                len = 0;

            LinkedList<DataObject> zippedList = new LinkedList<>();
            for(int i = 0;i < len;i++) {
                LinkedList<DataObject> list = new LinkedList<>();
                for(DataObject dataObject:lists)
                    list.add(new DataObject(dataObject.getList().get(i)));

                zippedList.add(new DataObject().setList(list));
            }

            return new DataObject().setList(zippedList);
        }

        @LangFunction("listAdd")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listAddFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            listObject.getList().add(new DataObject(valueObject));

            return null;
        }

        @LangFunction("listSet")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listSetFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$index") @NumberValue Number indexNumber,
                @LangParameter("$value") DataObject valueObject
        ) {
            int index = indexNumber.intValue();

            List<DataObject> list = listObject.getList();
            if(index < 0)
                index += list.size();

            if(index < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(index >= list.size())
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            list.set(index, new DataObject(valueObject));

            return null;
        }

        @LangFunction("listShift")
        public static DataObject listShiftFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            if(list.isEmpty())
                return null;

            return list.pollFirst();
        }

        @LangFunction("listUnshift")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listUnshiftFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            list.addFirst(new DataObject(valueObject));
            return null;
        }

        @LangFunction("listPeekFirst")
        public static DataObject listPeekFirstFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            if(list.isEmpty())
                return null;

            return list.peekFirst();
        }

        @LangFunction("listPop")
        public static DataObject listPopFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            if(list.isEmpty())
                return null;

            return list.pollLast();
        }

        @LangFunction("listPush")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listPushFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            list.addLast(new DataObject(valueObject));
            return null;
        }

        @LangFunction("listPeekLast")
        public static DataObject listPeekLastFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            if(list.isEmpty())
                return null;

            return list.peekLast();
        }

        @LangFunction("listRemove")
        public static DataObject listRemoveFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            for(int i = 0;i < list.size();i++) {
                DataObject dataObject = list.get(i);
                if(interpreter.operators.isStrictEquals(dataObject, valueObject, CodePosition.EMPTY)) {
                    list.remove(i);

                    return dataObject;
                }
            }

            return null;
        }

        @LangFunction("listRemoveLike")
        public static DataObject listRemoveLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            LinkedList<DataObject> list = listObject.getList();
            for(int i = 0;i < list.size();i++) {
                DataObject dataObject = list.get(i);
                if(interpreter.operators.isEquals(dataObject, valueObject, CodePosition.EMPTY)) {
                    list.remove(i);

                    return dataObject;
                }
            }

            return null;
        }

        @LangFunction("listRemoveAt")
        public static DataObject listRemoveAtFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$index") @NumberValue Number indexNumber
        ) {
            int index = indexNumber.intValue();

            List<DataObject> list = listObject.getList();
            if(index < 0)
                index += list.size();

            if(index < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(index >= list.size())
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            return list.remove(index);
        }

        @LangFunction("listGet")
        public static DataObject listGetFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$index") @NumberValue Number indexNumber
        ) {
            int index = indexNumber.intValue();

            List<DataObject> list = listObject.getList();
            if(index < 0)
                index += list.size();

            if(index < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(index >= list.size())
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            return list.get(index);
        }

        @LangFunction("listGetAll")
        @AllowedTypes(DataObject.DataType.TEXT)
        public static DataObject listGetAllFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            return new DataObject(listObject.getList().stream().map(ele ->
                    interpreter.conversions.toText(ele, CodePosition.EMPTY).toString()).collect(Collectors.joining(", ")));
        }

        @LangFunction("listFill")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listFillFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            list.replaceAll(ignored -> new DataObject(valueObject));

            return null;
        }

        @LangFunction("listFillFrom")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listFillFromFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$startIndex") @NumberValue Number startIndexNumber,
                @LangParameter("$value") DataObject valueObject
        ) {
            int startIndex = startIndexNumber.intValue();

            List<DataObject> list = listObject.getList();
            if(startIndex < 0)
                startIndex += list.size();

            if(startIndex < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(startIndex >= list.size())
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            for(int i = startIndex;i < list.size();i++)
                list.set(i, new DataObject(valueObject));

            return null;
        }

        @LangFunction("listFillTo")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listFillToFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$endIndex") @NumberValue Number endIndexNumber,
                @LangParameter("$value") DataObject valueObject
        ) {
            int endIndex = endIndexNumber.intValue();

            List<DataObject> list = listObject.getList();
            if(endIndex < 0)
                endIndex += list.size();

            if(endIndex < 0)
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
            else if(endIndex >= list.size())
                return interpreter.setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);

            for(int i = 0;i <= endIndex;i++)
                list.set(i, new DataObject(valueObject));

            return null;
        }

        @LangFunction("listCountOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listCountOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            long count = list.stream().filter(ele -> interpreter.operators.isStrictEquals(ele, valueObject, CodePosition.EMPTY)).count();
            return new DataObject().setInt((int)count);
        }

        @LangFunction("listIndexOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listIndexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            for(int i = 0;i < list.size();i++)
                if(interpreter.operators.isStrictEquals(list.get(i), valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("listLastIndexOf")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listLastIndexOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            for(int i = list.size() - 1;i >= 0;i--)
                if(interpreter.operators.isStrictEquals(list.get(i), valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("listCountLike")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listCountLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            long count = list.stream().filter(ele -> interpreter.operators.isEquals(ele, valueObject, CodePosition.EMPTY)).count();
            return new DataObject().setInt((int)count);
        }

        @LangFunction("listIndexLike")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listIndexLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            for(int i = 0;i < list.size();i++)
                if(interpreter.operators.isEquals(list.get(i), valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("listLastIndexLike")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listLastIndexLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$value") DataObject valueObject
        ) {
            List<DataObject> list = listObject.getList();
            for(int i = list.size() - 1;i >= 0;i--)
                if(interpreter.operators.isEquals(list.get(i), valueObject, CodePosition.EMPTY))
                    return new DataObject().setInt(i);

            return new DataObject().setInt(-1);
        }

        @LangFunction("listLength")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listLengthFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            return new DataObject().setInt(listObject.getList().size());
        }

        @LangFunction("listDistinctValuesOf")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listDistinctValuesOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            LinkedList<DataObject> distinctValues = new LinkedList<>();
            for(DataObject ele:listObject.getList()) {
                boolean flag = true;
                for(DataObject distinctEle:distinctValues) {
                    if(interpreter.operators.isStrictEquals(ele, distinctEle, CodePosition.EMPTY)) {
                        flag = false;
                        break;
                    }
                }

                if(flag)
                    distinctValues.add(new DataObject(ele));
            }

            return new DataObject().setList(distinctValues);
        }

        @LangFunction("listDistinctValuesLike")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listDistinctValuesLikeFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            LinkedList<DataObject> distinctValues = new LinkedList<>();
            for(DataObject ele:listObject.getList()) {
                boolean flag = true;
                for(DataObject distinctEle:distinctValues) {
                    if(interpreter.operators.isEquals(ele, distinctEle, CodePosition.EMPTY)) {
                        flag = false;
                        break;
                    }
                }

                if(flag)
                    distinctValues.add(new DataObject(ele));
            }

            return new DataObject().setList(distinctValues);
        }

        @LangFunction("listSorted")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listSortedFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.comparator") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject comparatorObject
        ) {
            List<DataObject> list = listObject.getList();

            List<DataObject> elements = list.stream().map(DataObject::new).sorted((a, b) -> {
                DataObject retObject = interpreter.callFunctionPointer(comparatorObject.getFunctionPointer(), comparatorObject.getVariableName(),
                        LangUtils.asListWithArgumentSeparators(
                                a, b
                        ));
                Number retNumber = interpreter.conversions.toNumber(retObject, CodePosition.EMPTY);
                if(retNumber == null) {
                    interpreter.setErrno(InterpretingError.NO_NUM, "The value returned by Argument 2 (\"fp.comparator\") must be a number.");

                    return 0;
                }

                return retNumber.intValue();
            }).collect(Collectors.toList());
            return new DataObject().setList(new LinkedList<>(elements));
        }

        @LangFunction("listFiltered")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listFilteredFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.filter") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject filterObject
        ) {
            List<DataObject> list = listObject.getList();

            List<DataObject> elements = list.stream().map(DataObject::new).filter(dataObject -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(filterObject.getFunctionPointer(),
                        filterObject.getVariableName(), Arrays.asList(
                                dataObject
                        )), CodePosition.EMPTY);
            }).collect(Collectors.toList());
            return new DataObject().setList(new LinkedList<>(elements));
        }

        @LangFunction("listFilteredCount")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listFilteredCountFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.filter") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject filterObject
        ) {
            List<DataObject> list = listObject.getList();

            long count = list.stream().map(DataObject::new).filter(dataObject -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(filterObject.getFunctionPointer(),
                        filterObject.getVariableName(), Arrays.asList(
                                dataObject
                        )), CodePosition.EMPTY);
            }).count();
            return new DataObject().setInt((int)count);
        }

        @LangFunction("listMap")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listMapFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.map") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject mapFunction
        ) {
            List<DataObject> list = listObject.getList();

            list.replaceAll(dataObject -> interpreter.callFunctionPointer(mapFunction.getFunctionPointer(), mapFunction.getVariableName(), Arrays.asList(
                    dataObject
            )));

            return null;
        }

        @LangFunction("listMapToNew")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listMapToNewFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.map") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject mapFunction
        ) {
            List<DataObject> list = listObject.getList();

            LinkedList<DataObject> newList = new LinkedList<>();
            for(DataObject dataObject:list) {
                newList.add(interpreter.callFunctionPointer(mapFunction.getFunctionPointer(), mapFunction.getVariableName(), Arrays.asList(
                        dataObject
                )));
            }

            return new DataObject().setList(newList);
        }

        @LangFunction(value="listReduce", hasInfo=true)
        public static DataObject listReduceFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            return listReduceFunction(interpreter, listObject, null, combineFunction);
        }
        @LangFunction("listReduce")
        public static DataObject listReduceFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("$initialValue") DataObject initialValueObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            List<DataObject> list = listObject.getList();

            DataObject currentValueObject = initialValueObject;

            for(DataObject ele:list) {
                if(currentValueObject == null) {
                    //Set first element as currentValue if no initial value was provided

                    currentValueObject = ele;

                    continue;
                }

                currentValueObject = interpreter.callFunctionPointer(combineFunction.getFunctionPointer(), combineFunction.getVariableName(),
                        LangUtils.asListWithArgumentSeparators(
                                currentValueObject,
                                ele
                        ));
            }

            return currentValueObject;
        }

        @LangFunction(value="listReduceColumn", hasInfo=true)
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listReduceColumnFunction(
                LangInterpreter interpreter,
                @LangParameter("&lists") @AllowedTypes(DataObject.DataType.LIST) DataObject listObjects,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            return listReduceColumnFunction(interpreter, listObjects, null, combineFunction);
        }
        @LangFunction("listReduceColumn")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listReduceColumnFunction(
                LangInterpreter interpreter,
                @LangParameter("&lists") @AllowedTypes(DataObject.DataType.LIST) DataObject listObjects,
                @LangParameter("$initialValue") DataObject initialValueObject,
                @LangParameter("fp.combine") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject combineFunction
        ) {
            LinkedList<DataObject> listOfLists = listObjects.getList();

            int len = -1;
            List<LinkedList<DataObject>> lists = new LinkedList<>();
            for(int i = 0;i < listOfLists.size();i++) {
                DataObject arg = listOfLists.get(i);
                if(arg.getType() != DataType.LIST)
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The element at index " + i + " of argument 1 (\"&lists\") must be of type " + DataObject.DataType.LIST);

                lists.add(arg.getList());

                int lenTest = arg.getList().size();
                if(len == -1) {
                    len = lenTest;

                    continue;
                }

                if(len != lenTest)
                    return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The length of the array at index " + i + " of argument 1 (\"&lists\") must be " + len);
            }

            if(lists.isEmpty())
                return new DataObject().setList(new LinkedList<>());

            LinkedList<DataObject> reduceedLists = new LinkedList<>();
            for(int i = 0;i < len;i++) {
                DataObject currentValueObject = initialValueObject == null?null:new DataObject(initialValueObject);

                for(LinkedList<DataObject> list:lists) {
                    DataObject ele = list.get(i);

                    if(currentValueObject == null) {
                        //Set first element as currentValue if no initial value was provided
                        currentValueObject = ele;

                        continue;
                    }

                    currentValueObject = interpreter.callFunctionPointer(combineFunction.getFunctionPointer(), combineFunction.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    currentValueObject,
                                    ele
                            ));
                }

                reduceedLists.add(LangUtils.nullToLangVoid(currentValueObject));
            }

            return new DataObject().setList(reduceedLists);
        }

        @LangFunction(value="listForEach", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listForEachFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            return listForEachFunction(interpreter, listObject, functionObject, false);
        }
        @LangFunction("listForEach")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listForEachFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject,
                @LangParameter("$breakable") @BooleanValue boolean breakable
        ) {
            List<DataObject> list = listObject.getList();

            if(breakable) {
                boolean[] shouldBreak = new boolean[] {false};

                DataObject breakFunc = new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                    @LangFunction("break")
                    @AllowedTypes(DataObject.DataType.VOID)
                    public DataObject breakFunction() {
                        shouldBreak[0] = true;

                        return null;
                    }
                }));

                for(DataObject ele:list) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    ele,
                                    breakFunc
                            ));

                    if(shouldBreak[0])
                        break;
                }
            }else {
                for(DataObject ele:list) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(), Arrays.asList(
                            ele
                    ));
                }
            }

            return null;
        }

        @LangFunction(value="listEnumerate", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listEnumerateFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            return listEnumerateFunction(interpreter, listObject, functionObject, false);
        }
        @LangFunction("listEnumerate")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listEnumerateFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject,
                @LangParameter("$breakable") @BooleanValue boolean breakable
        ) {
            List<DataObject> list = listObject.getList();

            if(breakable) {
                boolean[] shouldBreak = new boolean[] {false};

                DataObject breakFunc = new DataObject().setFunctionPointer(LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                    @LangFunction("break")
                    @AllowedTypes(DataObject.DataType.VOID)
                    public DataObject breakFunction() {
                        shouldBreak[0] = true;

                        return null;
                    }
                }));

                for(int i = 0;i < list.size();i++) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    new DataObject().setInt(i),
                                    list.get(i),
                                    breakFunc
                            ));

                    if(shouldBreak[0])
                        break;
                }
            }else {
                for(int i = 0;i < list.size();i++) {
                    interpreter.callFunctionPointer(functionObject.getFunctionPointer(), functionObject.getVariableName(),
                            LangUtils.asListWithArgumentSeparators(
                                    new DataObject().setInt(i),
                                    list.get(i)
                            ));
                }
            }

            return null;
        }

        @LangFunction("listAllMatch")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listAllMatchFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.predicate") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject predicateFunction
        ) {
            List<DataObject> list = listObject.getList();

            return new DataObject().setBoolean(list.stream().map(DataObject::new).allMatch(ele -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(predicateFunction.getFunctionPointer(),
                        predicateFunction.getVariableName(), Arrays.asList(
                                ele
                        )), CodePosition.EMPTY);
            }));
        }

        @LangFunction("listAnyMatch")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listAnyMatchFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.predicate") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject predicateFunction
        ) {
            List<DataObject> list = listObject.getList();

            return new DataObject().setBoolean(list.stream().map(DataObject::new).anyMatch(ele -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(predicateFunction.getFunctionPointer(),
                        predicateFunction.getVariableName(), Arrays.asList(
                                ele
                        )), CodePosition.EMPTY);
            }));
        }

        @LangFunction("listNoneMatch")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject listNoneMatchFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject,
                @LangParameter("fp.predicate") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject predicateFunction
        ) {
            List<DataObject> list = listObject.getList();

            return new DataObject().setBoolean(list.stream().map(DataObject::new).noneMatch(ele -> {
                return interpreter.conversions.toBool(interpreter.callFunctionPointer(predicateFunction.getFunctionPointer(),
                        predicateFunction.getVariableName(), Arrays.asList(
                                ele
                        )), CodePosition.EMPTY);
            }));
        }

        @LangFunction("listCombine")
        @AllowedTypes(DataObject.DataType.LIST)
        public static DataObject listCombineFunction(
                LangInterpreter interpreter,
                @LangParameter("&lists") @AllowedTypes(DataObject.DataType.LIST) @VarArgs List<DataObject> listObjects
        ) {
            LinkedList<DataObject> combinedLists = new LinkedList<>();

            for(DataObject listObject:listObjects)
                for(DataObject ele:listObject.getList())
                    combinedLists.add(new DataObject(ele));

            return new DataObject().setList(combinedLists);
        }

        @LangFunction("listClear")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject listClearFunction(
                LangInterpreter interpreter,
                @LangParameter("&list") @AllowedTypes(DataObject.DataType.LIST) DataObject listObject
        ) {
            listObject.getList().clear();

            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedStructFunctions {
        private LangPredefinedStructFunctions() {}

        @LangFunction("structCreate")
        @LangInfo("Returns an empty struct object of type &Struct." +
                " This function is not compatible with all struct types," +
                " because all values will be set to null - use \"func.structOf()\" for those struct types instead.")
        @AllowedTypes(DataObject.DataType.STRUCT)
        public static DataObject structCreateFunction(
                LangInterpreter interpreter,
                @LangParameter("&Struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            if(!struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct definition",
                        structObject.getVariableName()));

            try {
                return new DataObject().setStruct(new StructObject(struct));
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structOf")
        @AllowedTypes(DataObject.DataType.STRUCT)
        public static DataObject structOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&Struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            StructObject struct = structObject.getStruct();

            if(!struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct definition",
                        structObject.getVariableName()));

            String[] memberNames = structObject.getStruct().getMemberNames();
            if(args.size() != memberNames.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The var args argument count is not equals to the count of the member names (" + memberNames.length + ")");

            try {
                return new DataObject().setStruct(new StructObject(struct, args.toArray(new DataObject[0])));
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structSet")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject structSetFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject,
                @LangParameter("$memberName") @AllowedTypes(DataObject.DataType.TEXT) DataObject memberNameObject,
                @LangParameter("$memberObject") DataObject memberObject
        ) {
            StructObject struct = structObject.getStruct();

            if(struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct instance",
                        structObject.getVariableName()));

            String memberName = memberNameObject.getText().toString();

            try {
                struct.setMember(memberName, memberObject);
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }

            return null;
        }

        @LangFunction("structSetAll")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject structSetAllFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            StructObject struct = structObject.getStruct();

            if(struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct instance",
                        structObject.getVariableName()));

            String[] memberNames = structObject.getStruct().getMemberNames();
            if(args.size() != memberNames.length)
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                        "The var args argument count is not equals to the count of the member names (" + memberNames.length + ")");

            int i = 0;
            try {
                for(;i < memberNames.length;i++)
                    struct.setMember(memberNames[i], args.get(i));
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Argument " + (i + 2) + ": " + e.getMessage());
            }

            return null;
        }

        @LangFunction("structGet")
        public static DataObject structGetFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject,
                @LangParameter("$memberName") @AllowedTypes(DataObject.DataType.TEXT) DataObject memberNameObject
        ) {
            StructObject struct = structObject.getStruct();

            if(struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct instance",
                        structObject.getVariableName()));

            String memberName = memberNameObject.getText().toString();

            try {
                return struct.getMember(memberName);
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structGetAll")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject structGetAllFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            if(struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct instance",
                        structObject.getVariableName()));

            try {
                return new DataObject().setArray(Arrays.stream(struct.getMemberNames()).
                        map(struct::getMember).map(DataObject::new).toArray(DataObject[]::new));
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structGetMemberNames")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject structGetMemberNamesFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            try {
                return new DataObject().setArray(Arrays.stream(struct.getMemberNames()).map(DataObject::new).toArray(DataObject[]::new));
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structGetMemberCount")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject structGetMemberCountFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            try {
                return new DataObject().setInt(struct.getMemberNames().length);
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structIsDefinition")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject structIsDefinitionFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            try {
                return new DataObject().setBoolean(struct.isDefinition());
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structIsInstance")
        @AllowedTypes(DataObject.DataType.INT)
        public static DataObject structIsInstanceFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            try {
                return new DataObject().setBoolean(!struct.isDefinition());
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }

        @LangFunction("structDefinitionTypeOf")
        @AllowedTypes(DataObject.DataType.STRUCT)
        public static DataObject structDefinitionTypeOfFunction(
                LangInterpreter interpreter,
                @LangParameter("&struct") @AllowedTypes(DataObject.DataType.STRUCT) DataObject structObject
        ) {
            StructObject struct = structObject.getStruct();

            if(struct.isDefinition())
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, String.format("Argument 1 (\"%s\") must be a struct instance",
                        structObject.getVariableName()));

            try {
                return new DataObject().setStruct(struct.getStructBaseDefinition());
            }catch(DataTypeConstraintException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage());
            }
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedModuleFunctions {
        private LangPredefinedModuleFunctions() {}

        private static boolean containsNonWordChars(String moduleName) {
            for(int i = 0;i < moduleName.length();i++) {
                char c = moduleName.charAt(i);
                if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                    continue;

                return true;
            }

            return false;
        }

        @LangFunction("getLoadedModules")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject getLoadedModulesFunction(
                LangInterpreter interpreter
        ) {
            return new DataObject().setArray(interpreter.modules.keySet().stream().map(DataObject::new).toArray(DataObject[]::new));
        }

        @LangFunction("getModuleVariableNames")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject getModuleVariableNamesFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(moduleName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");

            LangModule module = interpreter.modules.get(moduleName);
            if(module == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" was not found");

            return new DataObject().setArray(module.getExportedVariables().keySet().stream().map(DataObject::new).toArray(DataObject[]::new));
        }

        @LangFunction("getModuleFunctionNames")
        @AllowedTypes(DataObject.DataType.ARRAY)
        public static DataObject getModuleFunctionNamesFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(moduleName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");

            LangModule module = interpreter.modules.get(moduleName);
            if(module == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" was not found");

            return new DataObject().setArray(module.getExportedFunctions().stream().map(DataObject::new).toArray(DataObject[]::new));
        }

        @LangFunction("getModuleVariable")
        public static DataObject getModuleVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject,
                @LangParameter("$variableName") DataObject variableNameObject
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();
            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(moduleName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");

            if(!variableName.startsWith("$") && !variableName.startsWith("&") && !variableName.startsWith("fp."))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name must start with \"$\", \"&\", or \"fp.\"");

            int variablePrefixLen = (variableName.charAt(0) == '$' || variableName.charAt(0) == '&')?1:3;

            for(int i = variablePrefixLen;i < variableName.length();i++) {
                char c = variableName.charAt(i);
                if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                    continue;

                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");
            }

            LangModule module = interpreter.modules.get(moduleName);
            if(module == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" was not found");

            DataObject variable = module.getExportedVariables().get(variableName);
            if(variable == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The variable \"" + variableName + "\" was not found in the module \""
                        + moduleName + "\"");

            return variable;
        }

        @LangFunction("getModuleVariableNormal")
        public static DataObject getModuleVariableNormalFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject,
                @LangParameter("$variableName") DataObject variableNameObject
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();
            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(moduleName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");

            if(containsNonWordChars(variableName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");

            variableName = "$" + variableName;

            LangModule module = interpreter.modules.get(moduleName);
            if(module == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" was not found");

            DataObject variable = module.getExportedVariables().get(variableName);
            if(variable == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The variable \"" + variableName + "\" was not found in the module \""
                        + moduleName + "\"");

            return variable;
        }

        @LangFunction("getModuleVariableComposite")
        public static DataObject getModuleVariableCompositeFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject,
                @LangParameter("$variableName") DataObject variableNameObject
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();
            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(moduleName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");

            if(containsNonWordChars(variableName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");

            variableName = "&" + variableName;

            LangModule module = interpreter.modules.get(moduleName);
            if(module == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" was not found");

            DataObject variable = module.getExportedVariables().get(variableName);
            if(variable == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The variable \"" + variableName + "\" was not found in the module \""
                        + moduleName + "\"");

            return variable;
        }

        @LangFunction("getModuleVariableFunctionPointer")
        public static DataObject getModuleVariableFunctionPointerFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject,
                @LangParameter("$variableName") DataObject variableNameObject
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();
            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(moduleName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");

            if(containsNonWordChars(variableName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");

            variableName = "fp." + variableName;

            LangModule module = interpreter.modules.get(moduleName);
            if(module == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" was not found");

            DataObject variable = module.getExportedVariables().get(variableName);
            if(variable == null)
                return interpreter.setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The variable \"" + variableName + "\" was not found in the module \""
                        + moduleName + "\"");

            return variable;
        }

        @LangFunction("moduleExportFunction")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportFunctionFunction(
                LangInterpreter interpreter,
                @LangParameter("$functionName") DataObject functionNameObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || !module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"func.moduleExportFunction\" can only be used inside a module which "
                        + "is in the \"load\" state");

            String functionName = interpreter.conversions.toText(functionNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(functionName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The function name may only contain alphanumeric characters and underscore (_)");

            module.getExportedFunctions().add(functionName);

            FunctionPointerObject function = functionObject.getFunctionPointer();

            interpreter.funcs.put(functionName, function.withLinkerFunction(false));

            return null;
        }

        @LangFunction("moduleExportLinkerFunction")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportLinkerFunctionFunction(
                LangInterpreter interpreter,
                @LangParameter("$functionName") DataObject functionNameObject,
                @LangParameter("fp.func") @AllowedTypes(DataObject.DataType.FUNCTION_POINTER) DataObject functionObject
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || !module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"func.moduleExportLinkerFunction\" can only be used inside a module which "
                        + "is in the \"load\" state");

            String functionName = interpreter.conversions.toText(functionNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(functionName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The function name may only contain alphanumeric characters and underscore (_)");

            module.getExportedFunctions().add(functionName);

            FunctionPointerObject function = functionObject.getFunctionPointer();

            interpreter.funcs.put(functionName, function.withLinkerFunction(true));

            return null;
        }

        @LangFunction(value="moduleExportNormalVariable", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportNormalVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$variableName") DataObject variableNameObject,
                @LangParameter("$variable") DataObject variableObject
        ) {
            return moduleExportNormalVariableFunction(interpreter, variableNameObject, variableObject, false);
        }
        @LangFunction("moduleExportNormalVariable")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportNormalVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$variableName") DataObject variableNameObject,
                @LangParameter("$variable") DataObject variableObject,
                @LangParameter("$final") @BooleanValue boolean finalData
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || !module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"func.moduleExportNormalVariable\" can only be used inside a module which "
                        + "is in the \"load\" state");

            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(variableName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");

            variableName = "$" + variableName;

            module.getExportedVariables().put(variableName, new DataObject(variableObject).setFinalData(finalData).setVariableName(variableName));

            return null;
        }

        @LangFunction(value="moduleExportCompositeVariable", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportCompositeVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$variableName") DataObject variableNameObject,
                @LangParameter("$variable") DataObject variableObject
        ) {
            return moduleExportCompositeVariableFunction(interpreter, variableNameObject, variableObject, false);
        }
        @LangFunction("moduleExportCompositeVariable")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportCompositeVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$variableName") DataObject variableNameObject,
                @LangParameter("$variable") DataObject variableObject,
                @LangParameter("$final") @BooleanValue boolean finalData
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || !module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"func.moduleExportNormalVariable\" can only be used inside a module which "
                        + "is in the \"load\" state");

            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(variableName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");

            variableName = "&" + variableName;

            module.getExportedVariables().put(variableName, new DataObject(variableObject).setFinalData(finalData).setVariableName(variableName));

            return null;
        }

        @LangFunction(value="moduleExportFunctionPointerVariable", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportFunctionPointerVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$variableName") DataObject variableNameObject,
                @LangParameter("$variable") DataObject variableObject
        ) {
            return moduleExportFunctionPointerVariableFunction(interpreter, variableNameObject, variableObject, false);
        }
        @LangFunction("moduleExportFunctionPointerVariable")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject moduleExportFunctionPointerVariableFunction(
                LangInterpreter interpreter,
                @LangParameter("$variableName") DataObject variableNameObject,
                @LangParameter("$variable") DataObject variableObject,
                @LangParameter("$final") @BooleanValue boolean finalData
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || !module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"func.moduleExportNormalVariable\" can only be used inside a module which "
                        + "is in the \"load\" state");

            String variableName = interpreter.conversions.toText(variableNameObject, CodePosition.EMPTY).toString();

            if(containsNonWordChars(variableName))
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The variable name may only contain alphanumeric characters and underscore (_)");

            variableName = "fp." + variableName;

            module.getExportedVariables().put(variableName, new DataObject(variableObject).setFinalData(finalData).setVariableName(variableName));

            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedLangTestFunctions {
        private LangPredefinedLangTestFunctions() {}

        @LangFunction("testUnit")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testUnitFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addUnit(interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString());

            return null;
        }

        @LangFunction("testSubUnit")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testSubUnitFunction(
                LangInterpreter interpreter,
                @LangParameter("$text") @VarArgs DataObject textObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            try {
                interpreter.langTestStore.addSubUnit(interpreter.conversions.toText(textObject, CodePosition.EMPTY).toString());
            }catch(IllegalStateException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, e.getMessage());
            }

            return null;
        }

        @LangFunction(value="testAssertError", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertErrorFunction(
                LangInterpreter interpreter,
                @LangParameter("$error") @AllowedTypes(DataObject.DataType.ERROR) DataObject errorObject
        ) {
            return testAssertErrorFunction(interpreter, errorObject, null);
        }
        @LangFunction("testAssertError")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertErrorFunction(
                LangInterpreter interpreter,
                @LangParameter("$error") @AllowedTypes(DataObject.DataType.ERROR) DataObject errorObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            InterpretingError langErrno = interpreter.getAndClearErrnoErrorObject();
            InterpretingError expectedError = errorObject.getError().getInterpretingError();

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultError(langErrno == expectedError,
                    interpreter.printStackTrace(CodePosition.EMPTY), messageObject == null?null:
                    interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(), langErrno,
                    expectedError));

            return null;
        }

        @LangFunction(value="testAssertEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertEqualsFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultEquals(
                    interpreter.operators.isEquals(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertNotEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertNotEqualsFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertNotEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultNotEquals(
                    !interpreter.operators.isEquals(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertLessThan", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertLessThanFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertLessThanFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertLessThan")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertLessThanFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultLessThan(
                    interpreter.operators.isLessThan(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertLessThanOrEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertLessThanOrEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertLessThanOrEqualsFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertLessThanOrEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertLessThanOrEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultLessThanOrEquals(
                    interpreter.operators.isLessThanOrEquals(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertGreaterThan", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertGreaterThanFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertGreaterThanFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertGreaterThan")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertGreaterThanFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultGreaterThan(
                    interpreter.operators.isGreaterThan(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertGreaterThanOrEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertGreaterThanOrEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertGreaterThanOrEqualsFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertGreaterThanOrEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertGreaterThanOrEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultGreaterThanOrEquals(
                    interpreter.operators.isGreaterThanOrEquals(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertStrictEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertStrictEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertStrictEqualsFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertStrictEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertStrictEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultStrictEquals(
                    interpreter.operators.isStrictEquals(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertStrictNotEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertStrictNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertStrictNotEqualsFunction(interpreter, actualValueObject, expectedValueObject, null);
        }
        @LangFunction("testAssertStrictNotEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertStrictNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultStrictNotEquals(
                    !interpreter.operators.isStrictEquals(actualValueObject, expectedValueObject, CodePosition.EMPTY), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, actualValueObject == null?null:interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedValueObject, expectedValueObject == null?null:interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertTranslationValueEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationValueEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertTranslationValueEqualsFunction(interpreter, translationKeyObject, expectedValueObject, null);
        }
        @LangFunction("testAssertTranslationValueEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationValueEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            String translationValue = interpreter.getData().lang.get(
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString());
            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultTranslationValueEquals(
                    translationValue != null && translationValue.equals(
                            interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()),
                    interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString(), translationValue,
                    interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertTranslationValueNotEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationValueNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject
        ) {
            return testAssertTranslationValueNotEqualsFunction(interpreter, translationKeyObject, expectedValueObject, null);
        }
        @LangFunction("testAssertTranslationValueNotEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationValueNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject,
                @LangParameter("$expectedValue") DataObject expectedValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            String translationValue = interpreter.getData().lang.get(
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString());
            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultTranslationValueNotEquals(
                    translationValue != null && !translationValue.equals(
                            interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()),
                    interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString(), translationValue,
                    interpreter.conversions.toText(expectedValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertTranslationKeyFound", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationKeyFoundFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject
        ) {
            return testAssertTranslationKeyFoundFunction(interpreter, translationKeyObject, null);
        }
        @LangFunction("testAssertTranslationKeyFound")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationKeyFoundFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            String translationValue = interpreter.getData().lang.get(
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString());
            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultTranslationKeyFound(
                    translationValue != null, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString(), translationValue));

            return null;
        }

        @LangFunction(value="testAssertTranslationKeyNotFound", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationKeyNotFoundFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject
        ) {
            return testAssertTranslationKeyNotFoundFunction(interpreter, translationKeyObject, null);
        }
        @LangFunction("testAssertTranslationKeyNotFound")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTranslationKeyNotFoundFunction(
                LangInterpreter interpreter,
                @LangParameter("$translationKey") DataObject translationKeyObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            String translationValue = interpreter.getData().lang.get(
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString());
            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultTranslationKeyNotFound(
                    translationValue == null, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    interpreter.conversions.toText(translationKeyObject, CodePosition.EMPTY).toString(), translationValue));

            return null;
        }

        @LangFunction(value="testAssertTypeEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTypeEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedType") @AllowedTypes(DataObject.DataType.TYPE) DataObject expectedTypeObject
        ) {
            return testAssertTypeEqualsFunction(interpreter, actualValueObject, expectedTypeObject, null);
        }
        @LangFunction("testAssertTypeEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTypeEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedType") @AllowedTypes(DataObject.DataType.TYPE) DataObject expectedTypeObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            DataType expectedType = expectedTypeObject.getTypeValue();

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultTypeEquals(
                    actualValueObject.getType() == expectedType, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedType));

            return null;
        }

        @LangFunction(value="testAssertTypeNotEquals", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTypeNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedType") @AllowedTypes(DataObject.DataType.TYPE) DataObject expectedTypeObject
        ) {
            return testAssertTypeNotEqualsFunction(interpreter, actualValueObject, expectedTypeObject, null);
        }
        @LangFunction("testAssertTypeNotEquals")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertTypeNotEqualsFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$expectedType") @AllowedTypes(DataObject.DataType.TYPE) DataObject expectedTypeObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            DataType expectedType = expectedTypeObject.getTypeValue();

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultTypeNotEquals(
                    actualValueObject.getType() != expectedType, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString(),
                    expectedType));

            return null;
        }

        @LangFunction(value="testAssertNull", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNullFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertNullFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertNull")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNullFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultNull(
                    actualValueObject.getType() == DataType.NULL, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertNotNull", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotNullFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertNotNullFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertNotNull")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotNullFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultNotNull(
                    actualValueObject.getType() != DataType.NULL, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertVoid", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertVoidFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertVoidFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertVoid")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertVoidFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultVoid(
                    actualValueObject.getType() == DataType.VOID, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertNotVoid", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotVoidFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertNotVoidFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertNotVoid")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotVoidFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultNotVoid(
                    actualValueObject.getType() != DataType.VOID, interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertFinal", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertFinalFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertFinal")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultFinal(actualValueObject.isFinalData(),
                    interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertNotFinal", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertNotFinalFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertNotFinal")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotFinalFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultNotFinal(
                    !actualValueObject.isFinalData(), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertStatic", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertStaticFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertStatic")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultStatic(
                    actualValueObject.isStaticData(), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertNotStatic", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject
        ) {
            return testAssertNotStaticFunction(interpreter, actualValueObject, null);
        }
        @LangFunction("testAssertNotStatic")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNotStaticFunction(
                LangInterpreter interpreter,
                @LangParameter("$actualValue") DataObject actualValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultNotStatic(
                    !actualValueObject.isStaticData(), interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString(),
                    actualValueObject, interpreter.conversions.toText(actualValueObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction(value="testAssertThrow", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertThrowFunction(
                LangInterpreter interpreter,
                @LangParameter("$expectedThrownValue") @AllowedTypes(DataObject.DataType.ERROR) DataObject expectedThrownValueObject
        ) {
            return testAssertThrowFunction(interpreter, expectedThrownValueObject, null);
        }
        @LangFunction("testAssertThrow")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertThrowFunction(
                LangInterpreter interpreter,
                @LangParameter("$expectedThrownValue") @AllowedTypes(DataObject.DataType.ERROR) DataObject expectedThrownValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            InterpretingError expectedError = expectedThrownValueObject.getError().getInterpretingError();

            interpreter.langTestExpectedReturnValueScopeID = interpreter.getScopeId();
            interpreter.langTestExpectedThrowValue = expectedError;
            interpreter.langTestMessageForLastTestResult = messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString();

            return null;
        }

        @LangFunction(value="testAssertReturn", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertReturnFunction(
                LangInterpreter interpreter,
                @LangParameter("$expectedReturnValue") DataObject expectedReturnValueObject
        ) {
            return testAssertReturnFunction(interpreter, expectedReturnValueObject, null);
        }
        @LangFunction("testAssertReturn")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertReturnFunction(
                LangInterpreter interpreter,
                @LangParameter("$expectedReturnValue") DataObject expectedReturnValueObject,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestExpectedReturnValueScopeID = interpreter.getScopeId();
            interpreter.langTestExpectedReturnValue = expectedReturnValueObject;
            interpreter.langTestMessageForLastTestResult = messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString();

            return null;
        }

        @LangFunction(value="testAssertNoReturn", hasInfo=true)
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNoReturnFunction(
                LangInterpreter interpreter
        ) {
            return testAssertNoReturnFunction(interpreter, null);
        }
        @LangFunction("testAssertNoReturn")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertNoReturnFunction(
                LangInterpreter interpreter,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestExpectedReturnValueScopeID = interpreter.getScopeId();
            interpreter.langTestExpectedNoReturnValue = true;
            interpreter.langTestMessageForLastTestResult = messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString();

            return null;
        }

        @LangFunction("testAssertFail")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testAssertFailFunction(
                LangInterpreter interpreter,
                @LangParameter("$message") DataObject messageObject
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            interpreter.langTestStore.addAssertResult(new LangTest.AssertResultFail(interpreter.printStackTrace(CodePosition.EMPTY),
                    messageObject == null?null:interpreter.conversions.toText(messageObject, CodePosition.EMPTY).toString()));

            return null;
        }

        @LangFunction("testClearAllTranslations")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testClearAllTranslationsFunction(
                LangInterpreter interpreter
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            new HashSet<>(interpreter.getData().lang.keySet()).forEach(translationKey -> {
                if(!translationKey.startsWith("lang."))
                    interpreter.getData().lang.remove(translationKey);
            });

            return null;
        }

        @LangFunction("testPrintResults")
        @AllowedTypes(DataObject.DataType.VOID)
        public static DataObject testPrintResultsFunction(
                LangInterpreter interpreter
        ) {
            if(!interpreter.executionFlags.langTest)
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "langTest functions can only be used if the langTest flag is true");

            if(interpreter.term == null)
                System.out.println(interpreter.langTestStore.printResults());
            else
                interpreter.langTestStore.printResultsToTerminal(interpreter.term);

            return null;
        }
    }

    @SuppressWarnings("unused")
    public static final class LangPredefinedLinkerFunctions {
        private LangPredefinedLinkerFunctions() {}

        private static DataObject executeLinkerFunction(LangInterpreter interpreter, String langFileName,
                                                        List<DataObject> args, Consumer<LangInterpreter.Data> function) {
            if(!langFileName.endsWith(".lang"))
                return interpreter.setErrnoErrorObject(InterpretingError.NO_LANG_FILE);

            String[] langArgs = args.stream().map(ele ->
                    interpreter.conversions.toText(ele, CodePosition.EMPTY).toString()).toArray(String[]::new);

            String absolutePath;

            boolean insideLangStandardImplementation = interpreter.getCurrentCallStackElement().getLangPath().startsWith("<standard>");

            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            boolean insideModule = module != null;
            if(insideLangStandardImplementation) {
                absolutePath = "lang" + LangUtils.removeDotsFromFilePath(
                        interpreter.getCurrentCallStackElement().getLangPath().substring(10) + "/" + langFileName);
            }else if(insideModule) {
                absolutePath = LangModuleManager.getModuleFilePath(module, interpreter.getCurrentCallStackElement().getLangPath(), langFileName);
            }else if(new File(langFileName).isAbsolute()) {
                absolutePath = langFileName;
            }else {
                absolutePath = interpreter.getCurrentCallStackElement().getLangPath() + "/" + langFileName;
            }

            String langPathTmp;
            if(insideLangStandardImplementation) {
                langPathTmp = absolutePath.substring(4, absolutePath.lastIndexOf('/'));

                //Update call stack
                interpreter.pushStackElement(new StackElement("<standard>" + langPathTmp, langFileName.substring(langFileName.lastIndexOf('/') + 1),
                        null, null, null, module), CodePosition.EMPTY);
            }else if(insideModule) {
                langPathTmp = absolutePath.substring(0, absolutePath.lastIndexOf('/'));

                //Update call stack
                interpreter.pushStackElement(new StackElement("<module:" + module.getFile() + "[" + module.getLangModuleConfiguration().getName() + "]>" + langPathTmp,
                        langFileName.substring(langFileName.lastIndexOf('/') + 1), null, null, null, module), CodePosition.EMPTY);
            }else {
                langPathTmp = interpreter.langPlatformAPI.getLangPath(absolutePath);

                //Update call stack
                interpreter.pushStackElement(new StackElement(langPathTmp, interpreter.langPlatformAPI.getLangFileName(langFileName),
                        null, null, null, null), CodePosition.EMPTY);
            }

            LangInterpreter.Data callerData = interpreter.getData();

            interpreter.enterScope(langArgs);

            DataObject retTmp;

            InputStream inputStream = insideLangStandardImplementation?LangPredefinedFunctions.class.getClassLoader().getResourceAsStream(absolutePath):null;
            if(insideLangStandardImplementation && inputStream == null)
                return interpreter.setErrnoErrorObject(InterpretingError.FILE_NOT_FOUND, "File not found during loading of lang standard implementation.");

            int originalLineNumber = interpreter.getParserLineNumber();
            try(BufferedReader reader = insideLangStandardImplementation?new BufferedReader(new InputStreamReader(inputStream)):
                    (insideModule?LangModuleManager.readModuleLangFile(module, absolutePath):
                            interpreter.langPlatformAPI.getLangReader(absolutePath))) {
                interpreter.resetParserPositionVars();
                interpreter.interpretLines(reader);

                function.accept(callerData);
            }catch(IOException e) {
                return interpreter.setErrnoErrorObject(InterpretingError.FILE_NOT_FOUND, e.getMessage());
            }finally {
                interpreter.setParserLineNumber(originalLineNumber);

                interpreter.exitScope();

                //Get returned value from executed Lang file
                retTmp = interpreter.getAndResetReturnValue();

                //Update call stack
                interpreter.popStackElement();
            }

            return retTmp;
        }

        @LangFunction(value="bindLibrary", isLinkerFunction=true)
        @LangInfo("Executes a lang file and copy all variables to the current scope")
        public static DataObject bindLibraryFunction(
                LangInterpreter interpreter,
                @LangParameter("$fileName")
                @LangInfo("The path of the lang file")
                DataObject fileNameObject,
                @LangParameter("&args")
                @LangInfo("Arguments which are set as &LANG_ARGS of the executed lang file")
                @VarArgs List<DataObject> args
        ) {
            return executeLinkerFunction(interpreter, interpreter.conversions.toText(fileNameObject, CodePosition.EMPTY).toString(),
                    args, callerData -> {
                        //Copy all vars
                        interpreter.getData().var.forEach((name, val) -> {
                            DataObject oldData = callerData.var.get(name);
                            if(oldData == null || (!oldData.isFinalData() && !oldData.isLangVar())) {
                                //No LANG data vars nor final data
                                callerData.var.put(name, val);
                            }
                        });
                    });
        }

        @LangFunction(value="link", isLinkerFunction=true)
        @LangInfo("Executes a lang file and copy all translation to the main scope")
        public static DataObject linkFunction(
                LangInterpreter interpreter,
                @LangParameter("$fileName")
                @LangInfo("The path of the lang file")
                DataObject fileNameObject,
                @LangParameter("&args")
                @LangInfo("Arguments which are set as &LANG_ARGS of the executed lang file")
                @VarArgs List<DataObject> args
        ) {
            return executeLinkerFunction(interpreter, interpreter.conversions.toText(fileNameObject, CodePosition.EMPTY).toString(),
                    args, callerData -> {
                        //Copy linked translation map (except "lang.* = *") to the "link caller"'s translation map
                        interpreter.getData().lang.forEach((k, v) -> {
                            if(!k.startsWith("lang.")) {
                                callerData.lang.put(k, v);
                            }
                        });
                    });
        }

        @LangFunction(value="include", isLinkerFunction=true)
        @LangInfo("Executes a lang file and copy all variables to the current scope and all translations to the main scope")
        public static DataObject includeFunction(
                LangInterpreter interpreter,
                @LangParameter("$fileName")
                @LangInfo("The path of the lang file")
                DataObject fileNameObject,
                @LangParameter("&args")
                @LangInfo("Arguments which are set as &LANG_ARGS of the executed lang file")
                @VarArgs List<DataObject> args
        ) {
            return executeLinkerFunction(interpreter, interpreter.conversions.toText(fileNameObject, CodePosition.EMPTY).toString(),
                    args, callerData -> {
                        //Copy linked translation map (except "lang.* = *") to the "link caller"'s translation map
                        interpreter.getData().lang.forEach((k, v) -> {
                            if(!k.startsWith("lang.")) {
                                callerData.lang.put(k, v);
                            }
                        });

                        //Copy all vars
                        interpreter.getData().var.forEach((name, val) -> {
                            DataObject oldData = callerData.var.get(name);
                            if(oldData == null || (!oldData.isFinalData() && !oldData.isLangVar())) {
                                //No LANG data vars nor final data
                                callerData.var.put(name, val);
                            }
                        });
                    });
        }

        @LangFunction(value="loadModule", isLinkerFunction=true)
        public static DataObject loadModuleFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleFile") DataObject moduleFileObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            String moduleFile = interpreter.conversions.toText(moduleFileObject, CodePosition.EMPTY).toString();

            if(!moduleFile.endsWith(".lm"))
                return interpreter.setErrnoErrorObject(InterpretingError.NO_LANG_FILE, "Modules must have a file extension of\".lm\"");

            if(!new File(moduleFile).isAbsolute())
                moduleFile = interpreter.getCurrentCallStackElement().getLangPath() + "/" + moduleFile;

            return interpreter.moduleManager.load(moduleFile, LangUtils.separateArgumentsWithArgumentSeparators(args));
        }

        @LangFunction(value="unloadModule", isLinkerFunction=true)
        public static DataObject unloadModuleFunction(
                LangInterpreter interpreter,
                @LangParameter("$moduleName") DataObject moduleNameObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            String moduleName = interpreter.conversions.toText(moduleNameObject, CodePosition.EMPTY).toString();
            for(int i = 0;i < moduleName.length();i++) {
                char c = moduleName.charAt(i);
                if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                    continue;

                return interpreter.setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The module name may only contain alphanumeric characters and underscore (_)");
            }

            return interpreter.moduleManager.unload(moduleName, LangUtils.separateArgumentsWithArgumentSeparators(args));
        }

        @LangFunction(value="moduleLoadNative", isLinkerFunction=true)
        public static DataObject moduleLoadNativeFunction(
                LangInterpreter interpreter,
                @LangParameter("$entryPoint") DataObject entryPointObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || !module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"moduleLoadNative\" can only be used inside a module which "
                        + "is in the \"load\" state");

            String entryPoint = interpreter.conversions.toText(entryPointObject, CodePosition.EMPTY).toString();

            return interpreter.moduleManager.loadNative(entryPoint, module, LangUtils.separateArgumentsWithArgumentSeparators(args));
        }

        @LangFunction(value="moduleUnloadNative", isLinkerFunction=true)
        public static DataObject moduleUnloadNativeFunction(
                LangInterpreter interpreter,
                @LangParameter("$entryPoint") DataObject entryPointObject,
                @LangParameter("&args") @VarArgs List<DataObject> args
        ) {
            LangModule module = interpreter.getCurrentCallStackElement().getModule();
            if(module == null || module.isLoad())
                return interpreter.setErrnoErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED, "\"moduleUnloadNative\" can only be used inside a module which "
                        + "is in the \"unload\" state");

            String entryPoint = interpreter.conversions.toText(entryPointObject, CodePosition.EMPTY).toString();

            return interpreter.moduleManager.unloadNative(entryPoint, module, LangUtils.separateArgumentsWithArgumentSeparators(args));
        }
    }
}