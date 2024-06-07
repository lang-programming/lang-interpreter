package at.jddev0.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LangBaseFunction {
    protected final List<DataObject> parameterList;
    protected final List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList;
    protected final List<ParameterAnnotation> parameterAnnotationList;
    protected final List<String> parameterInfoList;
    protected final int varArgsParameterIndex;
    protected final boolean textVarArgsParameter;
    protected final boolean rawVarArgsParameter;
    protected final DataObject.DataTypeConstraint returnValueTypeConstraint;

    public LangBaseFunction(List<DataObject> parameterList, List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList,
                            List<ParameterAnnotation> parameterAnnotationList, List<String> parameterInfoList, int varArgsParameterIndex, boolean textVarArgsParameter,
                            boolean rawVarArgsParameter, DataObject.DataTypeConstraint returnValueTypeConstraint) {
        this.parameterList = parameterList;
        this.parameterDataTypeConstraintList = parameterDataTypeConstraintList;
        this.parameterAnnotationList = parameterAnnotationList;
        this.parameterInfoList = parameterInfoList;
        this.varArgsParameterIndex = varArgsParameterIndex;
        this.textVarArgsParameter = textVarArgsParameter;
        this.rawVarArgsParameter = rawVarArgsParameter;
        this.returnValueTypeConstraint = returnValueTypeConstraint;
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
            }else if(parameterAnnotationList.get(i) == ParameterAnnotation.BOOLEAN) {
                builder.append("{boolean}");
            }else if(!parameterDataTypeConstraintList.get(i).equals(DataObject.getTypeConstraintFor(variableName))) {
                builder.append(parameterDataTypeConstraintList.get(i).toTypeConstraintSyntax());
            }

            builder.append(", ");
        }

        if(parameterList.size() > 0)
            builder.delete(builder.length() - 2, builder.length());

        builder.append(")");
        return builder.toString();
    }

    public boolean isEquals(LangBaseFunction that, LangInterpreter interpreter, int lineNumber) {
        if(this.varArgsParameterIndex != that.varArgsParameterIndex ||
                this.textVarArgsParameter != that.textVarArgsParameter ||
                this.parameterList.size() != that.parameterList.size() ||
                this.parameterDataTypeConstraintList.size() != that.parameterDataTypeConstraintList.size() ||
                this.parameterAnnotationList.size() != that.parameterAnnotationList.size())
            return false;

        for(int i = 0;i < this.parameterList.size();i++)
            if(!interpreter.operators.isEquals(this.parameterList.get(i), that.parameterList.get(i), lineNumber))
                return false;

        for(int i = 0;i < this.parameterDataTypeConstraintList.size();i++)
            if(!Objects.equals(this.parameterDataTypeConstraintList.get(i), that.parameterDataTypeConstraintList.get(i)))
                return false;

        for(int i = 0;i < this.parameterAnnotationList.size();i++)
            if(this.parameterAnnotationList.get(i) != that.parameterAnnotationList.get(i))
                return false;

        return true;
    }

    public boolean isStrictEquals(LangBaseFunction that, LangInterpreter interpreter, int lineNumber) {
        if(this.varArgsParameterIndex != that.varArgsParameterIndex ||
                this.textVarArgsParameter != that.textVarArgsParameter ||
                this.parameterList.size() != that.parameterList.size() ||
                this.parameterDataTypeConstraintList.size() != that.parameterDataTypeConstraintList.size() ||
                this.parameterAnnotationList.size() != that.parameterAnnotationList.size())
            return false;

        for(int i = 0;i < this.parameterList.size();i++)
            if(!interpreter.operators.isStrictEquals(this.parameterList.get(i), that.parameterList.get(i), lineNumber))
                return false;

        for(int i = 0;i < this.parameterDataTypeConstraintList.size();i++)
            if(!Objects.equals(this.parameterDataTypeConstraintList.get(i), that.parameterDataTypeConstraintList.get(i)))
                return false;

        for(int i = 0;i < this.parameterAnnotationList.size();i++)
            if(this.parameterAnnotationList.get(i) != that.parameterAnnotationList.get(i))
                return false;

        return true;
    }

    public static enum ParameterAnnotation {
        NORMAL, NUMBER, BOOLEAN, CALL_BY_POINTER, VAR_ARGS, RAW_VAR_ARGS;
    }
}
