package at.jddev0.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class LangNormalFunction extends LangBaseFunction {
    private final List<CodePosition> argumentPosList;
    private final AbstractSyntaxTree functionBody;

    LangNormalFunction(String langPath, String langFile, List<DataObject> parameterList,
                       List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList,
                       List<ParameterAnnotation> parameterAnnotationList, List<String> parameterInfoList, int varArgsParameterIndex, boolean textVarArgsParameter,
                       boolean rawVarArgsParameter, DataObject.DataTypeConstraint returnValueTypeConstraint,
                       List<CodePosition> argumentPosList, AbstractSyntaxTree functionBody) {
        super(langPath, langFile, parameterList, parameterDataTypeConstraintList, parameterAnnotationList, parameterInfoList,
                varArgsParameterIndex, textVarArgsParameter, rawVarArgsParameter, returnValueTypeConstraint);

        this.argumentPosList = argumentPosList;
        this.functionBody = functionBody;
    }

    public List<CodePosition> getArgumentPosList() {
        return new ArrayList<>(argumentPosList);
    }

    public AbstractSyntaxTree getFunctionBody() {
        return functionBody;
    }

    @Override
    public boolean isEquals(LangBaseFunction that, LangInterpreter interpreter, CodePosition pos) {
        if(this == that)
            return true;

        return that instanceof LangNormalFunction &&
                super.isEquals(that, interpreter, pos) &&
                Objects.equals(this.functionBody, ((LangNormalFunction)that).functionBody);
    }

    @Override
    public boolean isStrictEquals(LangBaseFunction that, LangInterpreter interpreter, CodePosition pos) {
        if(this == that)
            return true;

        return that instanceof LangNormalFunction &&
                super.isStrictEquals(that, interpreter, pos) &&
                Objects.equals(this.functionBody, ((LangNormalFunction)that).functionBody);
    }
}
