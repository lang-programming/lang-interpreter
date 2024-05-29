package at.jddev0.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LangNormalFunction extends LangBaseFunction {
    private final List<Integer> lineNumberFromList;
    private final List<Integer> lineNumberToList;
    private final AbstractSyntaxTree functionBody;

    LangNormalFunction(List<DataObject> parameterList, List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList,
                              List<ParameterAnnotation> parameterAnnotationList, int varArgsParameterIndex, boolean textVarArgsParameter,
                              boolean rawVarArgsParameter, DataObject.DataTypeConstraint returnValueTypeConstraint,
                              List<Integer> lineNumberFromList, List<Integer> lineNumberToList,
                              AbstractSyntaxTree functionBody) {
        super(parameterList, parameterDataTypeConstraintList, parameterAnnotationList, varArgsParameterIndex,
                textVarArgsParameter, rawVarArgsParameter, returnValueTypeConstraint);

        this.lineNumberFromList = lineNumberFromList;
        this.lineNumberToList = lineNumberToList;
        this.functionBody = functionBody;
    }

    public List<Integer> getLineNumberFromList() {
        return new ArrayList<>(lineNumberFromList);
    }

    public List<Integer> getLineNumberToList() {
        return new ArrayList<>(lineNumberToList);
    }

    public AbstractSyntaxTree getFunctionBody() {
        return functionBody;
    }

    @Override
    public boolean isEquals(LangBaseFunction that, LangInterpreter interpreter, int lineNumber) {
        if(this == that)
            return true;

        return that instanceof LangNormalFunction &&
                super.isEquals(that, interpreter, lineNumber) &&
                Objects.equals(this.functionBody, ((LangNormalFunction)that).functionBody);
    }

    @Override
    public boolean isStrictEquals(LangBaseFunction that, LangInterpreter interpreter, int lineNumber) {
        if(this == that)
            return true;

        return that instanceof LangNormalFunction &&
                super.isStrictEquals(that, interpreter, lineNumber) &&
                Objects.equals(this.functionBody, ((LangNormalFunction)that).functionBody);
    }
}
