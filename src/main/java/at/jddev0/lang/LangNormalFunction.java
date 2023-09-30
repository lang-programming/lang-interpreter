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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        LangNormalFunction that = (LangNormalFunction) o;
        return Objects.equals(functionBody, that.functionBody);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), functionBody);
    }
}
