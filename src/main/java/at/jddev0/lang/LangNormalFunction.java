package at.jddev0.lang;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class LangNormalFunction extends LangBaseFunction {
    private final List<CodePosition> argumentPosList;
    private final AbstractSyntaxTree functionBody;

    LangNormalFunction(String langPath, String langFile, List<DataObject> parameterList,
                       List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList,
                       List<ParameterAnnotation> parameterAnnotationList, List<String> parameterInfoList, int varArgsParameterIndex, boolean textVarArgsParameter,
                       boolean rawVarArgsParameter, DataObject.DataTypeConstraint returnValueTypeConstraint,
                       List<CodePosition> argumentPosList, AbstractSyntaxTree functionBody, boolean combinatorFunction,
                       int combinatorFunctionCallCount, List<DataObject> combinatorProvidedArgumentList, String functionName) {
        super(langPath, langFile, parameterList, parameterDataTypeConstraintList, parameterAnnotationList, parameterInfoList,
                varArgsParameterIndex, textVarArgsParameter, rawVarArgsParameter, returnValueTypeConstraint,
                combinatorFunction, combinatorFunctionCallCount, combinatorProvidedArgumentList, functionName);

        this.argumentPosList = argumentPosList;
        this.functionBody = functionBody;
    }

    public List<CodePosition> getArgumentPosList() {
        return new ArrayList<>(argumentPosList);
    }

    public AbstractSyntaxTree getFunctionBody() {
        return functionBody;
    }

    protected DataObject combinatorCall(DataObject.LangObject thisObject, int superLevel,
                                      List<DataObject> combinedArgumentList) {
        LangNormalFunction langNormalFunction = new LangNormalFunction(langPath, langFile, parameterList,
                parameterDataTypeConstraintList, parameterAnnotationList, parameterInfoList, varArgsParameterIndex,
                textVarArgsParameter, rawVarArgsParameter, returnValueTypeConstraint, argumentPosList, functionBody,
                combinatorFunction, combinatorFunctionCallCount + 1, combinedArgumentList,
                functionName);

        String functionNames = combinedArgumentList.stream().map(dataObject -> {
            if(dataObject.getType() != DataObject.DataType.FUNCTION_POINTER)
                return "<arg>";

            String functionName = dataObject.getFunctionPointer().getFunctionName();
            return functionName == null?dataObject.getVariableName():functionName;
        }).collect(Collectors.joining(", "));

        String functionName = "<" + (varArgsParameterIndex == -1?"":"inf-") + this.functionName + "-func(" + functionNames + ")>";

        DataObject.FunctionPointerObject fp = new DataObject.FunctionPointerObject(functionName, langNormalFunction);
        if(thisObject != null)
            fp = new DataObject.FunctionPointerObject(fp, thisObject).withMappedFunctions(internalFunction ->
                    new DataObject.FunctionPointerObject.InternalFunction(internalFunction, superLevel));

        return new DataObject().setFunctionPointer(fp);
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
