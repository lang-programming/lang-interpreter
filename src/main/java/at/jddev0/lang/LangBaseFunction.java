package at.jddev0.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LangBaseFunction {
    /**
     * If langPath is set, the Lang path from the stack frame element which is created for the function call will be overridden
     */
    protected final String langPath;

    /**
     * If langFile or langPath is set, the Lang file from the stack frame element which is created for the function call will be overridden<br>
     * This behavior allows for keeping the "&lt;shell&gt;" special case - when the Lang file is null - if a function within a stack frame element where the Lang file is null is
     * called from within a stack frame element where Lang file is not null.
     */
    protected final String langFile;

    protected final List<DataObject> parameterList;
    protected final List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList;
    protected final List<ParameterAnnotation> parameterAnnotationList;
    protected final List<String> parameterInfoList;
    protected final int varArgsParameterIndex;
    protected final boolean textVarArgsParameter;
    protected final boolean rawVarArgsParameter;
    protected final DataObject.DataTypeConstraint returnValueTypeConstraint;

    protected final boolean combinatorFunction;
    protected final int combinatorFunctionCallCount;
    protected final List<DataObject> combinatorProvidedArgumentList;

    protected final String functionName;

    public LangBaseFunction(String langPath, String langFile, List<DataObject> parameterList,
                            List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList,
                            List<ParameterAnnotation> parameterAnnotationList, List<String> parameterInfoList,
                            int varArgsParameterIndex, boolean textVarArgsParameter, boolean rawVarArgsParameter,
                            DataObject.DataTypeConstraint returnValueTypeConstraint, boolean combinatorFunction,
                            int combinatorFunctionCallCount, List<DataObject> combinatorProvidedArgumentList,
                            String functionName) {
        this.langPath = langPath;
        this.langFile = langFile;
        this.parameterList = parameterList;
        this.parameterDataTypeConstraintList = parameterDataTypeConstraintList;
        this.parameterAnnotationList = parameterAnnotationList;
        this.parameterInfoList = parameterInfoList;
        this.varArgsParameterIndex = varArgsParameterIndex;
        this.textVarArgsParameter = textVarArgsParameter;
        this.rawVarArgsParameter = rawVarArgsParameter;
        this.returnValueTypeConstraint = returnValueTypeConstraint;

        this.combinatorFunction = combinatorFunction;
        this.combinatorFunctionCallCount = combinatorFunctionCallCount;
        this.combinatorProvidedArgumentList = combinatorProvidedArgumentList;

        this.functionName = functionName;
    }

    public String getLangPath() {
        return langPath;
    }

    public String getLangFile() {
        return langFile;
    }

    public List<DataObject> getParameterList() {
        return new ArrayList<>(parameterList);
    }

    public List<DataObject.DataTypeConstraint> getParameterDataTypeConstraintList() {
        return new ArrayList<>(parameterDataTypeConstraintList);
    }

    public List<String> getParameterInfoList() {
        return parameterInfoList;
    }

    public List<ParameterAnnotation> getParameterAnnotationList() {
        return new ArrayList<>(parameterAnnotationList);
    }

    public int getVarArgsParameterIndex() {
        return varArgsParameterIndex;
    }

    public boolean isTextVarArgsParameter() {
        return textVarArgsParameter;
    }

    public boolean isRawVarArgsParameter() {
        return rawVarArgsParameter;
    }

    public DataObject.DataTypeConstraint getReturnValueTypeConstraint() {
        return returnValueTypeConstraint;
    }

    public boolean isCombinatorFunction() {
        return combinatorFunction;
    }

    public int getCombinatorFunctionCallCount() {
        return combinatorFunctionCallCount;
    }

    public List<DataObject> getCombinatorProvidedArgumentList() {
        return combinatorProvidedArgumentList;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String toFunctionSignatureSyntax() {
        StringBuilder builder = new StringBuilder();
        builder.append("(");

        for(int i = 0;i < parameterList.size();i++) {
            String variableName = parameterList.get(i).getVariableName();

            if(parameterAnnotationList.get(i) == ParameterAnnotation.CALL_BY_POINTER) {
                builder.append("$[").append(variableName.substring(variableName.isEmpty()?0:1)).append("]");
            }else {
                builder.append(variableName);
            }

            if(parameterAnnotationList.get(i) == ParameterAnnotation.VAR_ARGS) {
                if(!parameterDataTypeConstraintList.get(i).equals(DataObject.CONSTRAINT_NORMAL))
                    builder.append(parameterDataTypeConstraintList.get(i).toTypeConstraintSyntax());

                builder.append("...");
            }else if(parameterAnnotationList.get(i) == ParameterAnnotation.RAW_VAR_ARGS) {
                if(!parameterDataTypeConstraintList.get(i).equals(DataObject.CONSTRAINT_NORMAL))
                    builder.append(parameterDataTypeConstraintList.get(i).toTypeConstraintSyntax());

                builder.append("...{raw}");
            }else if(parameterAnnotationList.get(i) == ParameterAnnotation.NUMBER) {
                builder.append("{number}");
            }else if(parameterAnnotationList.get(i) == ParameterAnnotation.CALLABLE) {
                builder.append("{callable}");
            }else if(parameterAnnotationList.get(i) == ParameterAnnotation.BOOLEAN) {
                builder.append("{boolean}");
            }else if(!parameterDataTypeConstraintList.get(i).equals(DataObject.getTypeConstraintFor(variableName))) {
                builder.append(parameterDataTypeConstraintList.get(i).toTypeConstraintSyntax());
            }

            builder.append(", ");
        }

        if(!parameterList.isEmpty())
            builder.delete(builder.length() - 2, builder.length());

        builder.append(")");
        return builder.toString();
    }

    public boolean isEquals(LangBaseFunction that, LangInterpreter interpreter, CodePosition pos) {
        if(this.varArgsParameterIndex != that.varArgsParameterIndex ||
                this.textVarArgsParameter != that.textVarArgsParameter ||
                this.parameterList.size() != that.parameterList.size() ||
                this.parameterDataTypeConstraintList.size() != that.parameterDataTypeConstraintList.size() ||
                this.parameterAnnotationList.size() != that.parameterAnnotationList.size() ||
                this.combinatorFunction != that.combinatorFunction ||
                this.combinatorProvidedArgumentList.size() != that.combinatorProvidedArgumentList.size())
            return false;

        for(int i = 0;i < this.parameterList.size();i++)
            if(!interpreter.operators.isEquals(this.parameterList.get(i), that.parameterList.get(i), pos))
                return false;

        for(int i = 0;i < this.parameterDataTypeConstraintList.size();i++)
            if(!Objects.equals(this.parameterDataTypeConstraintList.get(i), that.parameterDataTypeConstraintList.get(i)))
                return false;

        for(int i = 0;i < this.parameterAnnotationList.size();i++)
            if(this.parameterAnnotationList.get(i) != that.parameterAnnotationList.get(i))
                return false;

        for(int i = 0;i < this.combinatorProvidedArgumentList.size();i++)
            if(!interpreter.operators.isEquals(this.combinatorProvidedArgumentList.get(i),
                    that.combinatorProvidedArgumentList.get(i), pos))
                return false;

        return true;
    }

    public boolean isStrictEquals(LangBaseFunction that, LangInterpreter interpreter, CodePosition pos) {
        if(this.varArgsParameterIndex != that.varArgsParameterIndex ||
                this.textVarArgsParameter != that.textVarArgsParameter ||
                this.parameterList.size() != that.parameterList.size() ||
                this.parameterDataTypeConstraintList.size() != that.parameterDataTypeConstraintList.size() ||
                this.parameterAnnotationList.size() != that.parameterAnnotationList.size() ||
                this.combinatorFunction != that.combinatorFunction ||
                this.combinatorProvidedArgumentList.size() != that.combinatorProvidedArgumentList.size())
            return false;

        for(int i = 0;i < this.parameterList.size();i++)
            if(!interpreter.operators.isStrictEquals(this.parameterList.get(i), that.parameterList.get(i), pos))
                return false;

        for(int i = 0;i < this.parameterDataTypeConstraintList.size();i++)
            if(!Objects.equals(this.parameterDataTypeConstraintList.get(i), that.parameterDataTypeConstraintList.get(i)))
                return false;

        for(int i = 0;i < this.parameterAnnotationList.size();i++)
            if(this.parameterAnnotationList.get(i) != that.parameterAnnotationList.get(i))
                return false;

        for(int i = 0;i < this.combinatorProvidedArgumentList.size();i++)
            if(!interpreter.operators.isStrictEquals(this.combinatorProvidedArgumentList.get(i),
                    that.combinatorProvidedArgumentList.get(i), pos))
                return false;

        return true;
    }

    public enum ParameterAnnotation {
        NORMAL, NUMBER, CALLABLE, BOOLEAN, CALL_BY_POINTER, VAR_ARGS, RAW_VAR_ARGS;
    }
}
