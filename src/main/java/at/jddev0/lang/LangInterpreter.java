package at.jddev0.lang;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import at.jddev0.io.TerminalIO;
import at.jddev0.io.TerminalIO.Level;
import at.jddev0.lang.AbstractSyntaxTree.*;
import at.jddev0.lang.AbstractSyntaxTree.OperationNode.Operator;
import at.jddev0.lang.AbstractSyntaxTree.OperationNode.OperatorType;
import at.jddev0.lang.DataObject.DataType;
import at.jddev0.lang.DataObject.DataTypeConstraint;
import at.jddev0.lang.DataObject.DataTypeConstraintException;
import at.jddev0.lang.DataObject.DataTypeConstraintViolatedException;
import at.jddev0.lang.DataObject.ErrorObject;
import at.jddev0.lang.DataObject.FunctionPointerObject;
import at.jddev0.lang.DataObject.LangObject;
import at.jddev0.lang.DataObject.StructObject;
import at.jddev0.lang.DataObject.VarPointerObject;
import at.jddev0.lang.LangUtils.InvalidTranslationTemplateSyntaxException;
import at.jddev0.lang.regex.InvalidPatternSyntaxException;
import at.jddev0.lang.regex.LangRegEx;

/**
 * Lang-Module<br>
 * Lang interpreter for interpreting AST created by LangParser
 *
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangInterpreter {
    static final String VERSION = "v1.0.0";

    private final LangParser parser = new LangParser();

    final LangModuleManager moduleManager = new LangModuleManager(this);
    final Map<String, LangModule> modules = new HashMap<>();

    private boolean isInitializingLangStandardImplementation = true;
    private int scopeId = -1;
    private StackElement currentCallStackElement;
    private final LinkedList<StackElement> callStack;

    final TerminalIO term;
    final ILangPlatformAPI langPlatformAPI;
    final Random RAN = new Random();

    //Lang tests
    final LangTest langTestStore = new LangTest();
    InterpretingError langTestExpectedThrowValue;
    DataObject langTestExpectedReturnValue;
    boolean langTestExpectedNoReturnValue;
    String langTestMessageForLastTestResult;
    int langTestExpectedReturnValueScopeID;

    //Fields for return/throw node, continue/break node, and force stopping execution
    private final ExecutionState executionState = new ExecutionState();
    final ExecutionFlags executionFlags = new ExecutionFlags();

    //DATA
    private final Map<Integer, Data> data = new HashMap<>();

    //Lang Standard implementation data
    final Map<String, DataObject> standardTypes = new HashMap<>();

    //Predefined functions & linker functions (= Predefined functions)
    Map<String, FunctionPointerObject> funcs = new HashMap<>();
    {
        LangPredefinedFunctions.addPredefinedFunctions(funcs);
        LangPredefinedFunctions.addLinkerFunctions(funcs);
    }
    public final LangOperators operators = new LangOperators(this);
    public final LangConversions conversions = new LangConversions(this);
    public final LangVars langVars = new LangVars(this);

    /**
     * @param term can be null
     */
    public LangInterpreter(String langPath, TerminalIO term, ILangPlatformAPI langPlatformAPI) {
        this(langPath, null, term, langPlatformAPI, null);
    }
    /**
     * @param langFile can be null
     * @param term can be null
     */
    public LangInterpreter(String langPath, String langFile, TerminalIO term, ILangPlatformAPI langPlatformAPI) {
        this(langPath, langFile, term, langPlatformAPI, null);
    }
    /**
     * @param term can be null
     * @param langArgs can be null
     */
    public LangInterpreter(String langPath, TerminalIO term, ILangPlatformAPI langPlatformAPI, String[] langArgs) {
        this(langPath, null, term, langPlatformAPI, langArgs);
    }
    /**
     * @param langFile can be null
     * @param term can be null
     * @param langArgs can be null
     */
    public LangInterpreter(String langPath, String langFile, TerminalIO term, ILangPlatformAPI langPlatformAPI, String[] langArgs) {
        callStack = new LinkedList<>();
        currentCallStackElement = new StackElement(langPath, langFile, null, null, null, null);
        this.term = term;
        this.langPlatformAPI = langPlatformAPI;

        initLangStandard();
        enterScope(langArgs);
    }

    public AbstractSyntaxTree parseLines(BufferedReader lines) throws IOException {
        return parser.parseLines(lines);
    }

    public DataObject interpretAST(AbstractSyntaxTree ast) {
        if(ast == null)
            return null;

        if(executionState.forceStopExecutionFlag)
            throw new StoppedException();

        DataObject ret = null;
        for(Node node:ast) {
            if(executionState.stopExecutionFlag)
                return null;

            ret = interpretNode(null, node);
        }

        return ret;
    }

    public DataObject interpretLines(BufferedReader lines) throws IOException, StoppedException {
        return interpretAST(parseLines(lines));
    }

    public Data getData() {
        return data.get(scopeId);
    }

    public void forceStop() {
        executionState.forceStopExecutionFlag = true;
    }

    public boolean isForceStopExecutionFlag() {
        return executionState.forceStopExecutionFlag;
    }

    public StackElement getCurrentCallStackElement() {
        return currentCallStackElement;
    }

    List<StackElement> getCallStackElements() {
        return new ArrayList<>(callStack);
    }

    void pushStackElement(StackElement stackElement, CodePosition parentPos) {
        callStack.addLast(currentCallStackElement.withPos(parentPos));
        currentCallStackElement = stackElement;
    }

    StackElement popStackElement() {
        currentCallStackElement = callStack.pollLast().withPos(CodePosition.EMPTY);
        return currentCallStackElement;
    }

    String printStackTrace(CodePosition pos) {
        StringBuilder builder = new StringBuilder();

        ListIterator<StackElement> iter = callStack.listIterator(callStack.size());
        builder.append(currentCallStackElement.withPos(pos));
        if(!iter.hasPrevious())
            return builder.toString();

        builder.append("\n");
        while(true) {
            builder.append(iter.previous());
            if(iter.hasPrevious())
                builder.append("\n");
            else
                break;
        }

        return builder.toString();
    }

    int getParserLineNumber() {
        return parser.getLineNumber();
    }

    void setParserLineNumber(int lineNumber) {
        parser.setLineNumber(lineNumber);
    }

    void resetParserPositionVars() {
        parser.resetPositionVars();
    }

    /**
     * @return Might return null
     */
    private DataObject interpretNode(DataObject compositeType, Node node) {
        if(executionState.forceStopExecutionFlag)
            throw new StoppedException();

        try {
            loop:
            while(true) {
                if(node == null) {
                    setErrno(InterpretingError.INVALID_AST_NODE);

                    return null;
                }

                switch(node.getNodeType()) {
                    case UNPROCESSED_VARIABLE_NAME:
                        node = processUnprocessedVariableNameNode(compositeType, (UnprocessedVariableNameNode)node);
                        continue loop;

                    case FUNCTION_CALL_PREVIOUS_NODE_VALUE:
                        node = processFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)node, null);
                        continue loop;

                    case LIST:
                        //Interpret a group of nodes
                        return interpretListNode(compositeType, (ListNode)node);

                    case CHAR_VALUE:
                    case TEXT_VALUE:
                    case INT_VALUE:
                    case LONG_VALUE:
                    case FLOAT_VALUE:
                    case DOUBLE_VALUE:
                    case NULL_VALUE:
                    case VOID_VALUE:
                        return interpretValueNode((ValueNode)node);

                    case PARSING_ERROR:
                        return interpretParsingErrorNode((ParsingErrorNode)node);

                    case IF_STATEMENT:
                        return new DataObject().setBoolean(interpretIfStatementNode((IfStatementNode)node));

                    case IF_STATEMENT_PART_ELSE:
                    case IF_STATEMENT_PART_IF:
                        return new DataObject().setBoolean(interpretIfStatementPartNode((IfStatementPartNode)node));

                    case LOOP_STATEMENT:
                        return new DataObject().setBoolean(interpretLoopStatementNode((LoopStatementNode)node));

                    case LOOP_STATEMENT_PART_WHILE:
                    case LOOP_STATEMENT_PART_UNTIL:
                    case LOOP_STATEMENT_PART_REPEAT:
                    case LOOP_STATEMENT_PART_FOR_EACH:
                    case LOOP_STATEMENT_PART_LOOP:
                    case LOOP_STATEMENT_PART_ELSE:
                        return new DataObject().setBoolean(interpretLoopStatementPartNode((LoopStatementPartNode)node));

                    case LOOP_STATEMENT_CONTINUE_BREAK:
                        interpretLoopStatementContinueBreak((LoopStatementContinueBreakStatement)node);
                        return null;

                    case TRY_STATEMENT:
                        return new DataObject().setBoolean(interpretTryStatementNode((TryStatementNode)node));

                    case TRY_STATEMENT_PART_TRY:
                    case TRY_STATEMENT_PART_SOFT_TRY:
                    case TRY_STATEMENT_PART_NON_TRY:
                    case TRY_STATEMENT_PART_CATCH:
                    case TRY_STATEMENT_PART_ELSE:
                    case TRY_STATEMENT_PART_FINALLY:
                        return new DataObject().setBoolean(interpretTryStatementPartNode((TryStatementPartNode)node));

                    case OPERATION:
                    case MATH:
                    case CONDITION:
                        return interpretOperationNode((OperationNode)node);

                    case RETURN:
                        interpretReturnNode((ReturnNode)node);
                        return null;

                    case THROW:
                        interpretThrowNode((ThrowNode)node);
                        return null;

                    case ASSIGNMENT:
                        return interpretAssignmentNode((AssignmentNode)node);

                    case VARIABLE_NAME:
                        return interpretVariableNameNode(compositeType, (VariableNameNode)node);

                    case ESCAPE_SEQUENCE:
                        return interpretEscapeSequenceNode((EscapeSequenceNode)node);

                    case UNICODE_ESCAPE_SEQUENCE:
                        return interpretUnicodeEscapeSequenceNode((UnicodeEscapeSequenceNode)node);

                    case ARGUMENT_SEPARATOR:
                        return interpretArgumentSeparatotNode((ArgumentSeparatorNode)node);

                    case FUNCTION_CALL:
                        return interpretFunctionCallNode(compositeType, (FunctionCallNode)node);

                    case FUNCTION_DEFINITION:
                        return interpretFunctionDefinitionNode((FunctionDefinitionNode)node);

                    case ARRAY:
                        return interpretArrayNode((ArrayNode)node);

                    case STRUCT_DEFINITION:
                        return interpretStructDefinitionNode((StructDefinitionNode)node);

                    case CLASS_DEFINITION:
                        return interpretClassDefinitionNode((ClassDefinitionNode)node);

                    case GENERAL:
                        setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
                        return null;
                }
            }
        }catch(ClassCastException e) {
            setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
        }

        return null;
    }

    /**
     * @param variablePrefixAppendAfterSearch If no part of the variable name matched an existing variable, the variable prefix will be added to the returned TextValueNode<br>
     *                                             (e.g. "func.abc" ("func." is not part of the variableNames in the set))
     * @param supportsPointerDereferencingAndReferencing If true, this node will return pointer reference or a dereferenced pointers as VariableNameNode<br>
     *                                   (e.g. $[abc] is not in variableNames, but $abc is -> $[abc] will return a VariableNameNode)
     */
    private Node convertVariableNameToVariableNameNodeOrComposition(String moduleName, String variableName,
                                                                    Set<String> variableNames, String variablePrefixAppendAfterSearch, final boolean supportsPointerDereferencingAndReferencing, CodePosition pos) {
        Stream<String> variableNameStream;
        if(moduleName == null) {
            variableNameStream = variableNames.stream();
        }else {
            LangModule module = modules.get(moduleName);
            if(module == null) {
                setErrno(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" is not loaded!", pos);

                return new TextValueNode(pos, "[[" + moduleName + "]]::" + variablePrefixAppendAfterSearch + variableName);
            }

            variableNameStream = module.getExportedVariables().keySet().stream();
        }

        //Sort keySet from large to small length (e.g.: $abcd and $abc and $ab)
        Optional<String> optionalReturnedVariableName = variableNameStream.filter(variableName::startsWith).min((s0, s1) -> {
            return Integer.compare(s1.length(), s0.length());//Sort keySet from large to small length (e.g.: $abcd and $abc and $ab)
        });

        if(!optionalReturnedVariableName.isPresent()) {
            if(supportsPointerDereferencingAndReferencing) {
                String dereferences = null;
                int startIndex = -1;
                String modifiedVariableName = variableName;
                Node returnedNode = null;
                String text = null;
                if(variableName.contains("*")) { //Check referenced variable name
                    startIndex = variableName.indexOf('*');
                    int endIndex = variableName.lastIndexOf('*') + 1;
                    if(endIndex >= variableName.length())
                        return new TextValueNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) + variablePrefixAppendAfterSearch + variableName);

                    dereferences = variableName.substring(startIndex, endIndex);
                    modifiedVariableName = variableName.substring(0, startIndex) + variableName.substring(endIndex);

                    if(!modifiedVariableName.contains("[") && !modifiedVariableName.contains("]"))
                        returnedNode = convertVariableNameToVariableNameNodeOrComposition(
                                moduleName, modifiedVariableName, variableNames, "", supportsPointerDereferencingAndReferencing, pos);
                }

                if(modifiedVariableName.contains("[") && modifiedVariableName.contains("]")) { //Check dereferenced variable name
                    int indexOpeningBracket = modifiedVariableName.indexOf("[");
                    int indexMatchingBracket = LangUtils.getIndexOfMatchingBracket(modifiedVariableName, indexOpeningBracket, Integer.MAX_VALUE, '[', ']');
                    if(indexMatchingBracket != -1) {
                        //Remove all "[" "]" pairs
                        int currentIndex = indexOpeningBracket;
                        int currentIndexMatchingBracket = indexMatchingBracket;
                        //"&" both "++" and "--" must be executed
                        while(modifiedVariableName.charAt(++currentIndex) == '[' & modifiedVariableName.charAt(--currentIndexMatchingBracket) == ']');

                        if(indexMatchingBracket != modifiedVariableName.length() - 1) {
                            text = modifiedVariableName.substring(indexMatchingBracket + 1);
                            modifiedVariableName = modifiedVariableName.substring(0, indexMatchingBracket + 1);
                        }

                        if(modifiedVariableName.indexOf('[', currentIndex) == -1) {
                            returnedNode = convertVariableNameToVariableNameNodeOrComposition(
                                    moduleName, modifiedVariableName.substring(0, indexOpeningBracket) +
                                            modifiedVariableName.substring(currentIndex, currentIndexMatchingBracket + 1), variableNames,
                                    "", supportsPointerDereferencingAndReferencing, pos);
                        }
                    }
                }

                if(returnedNode != null) {
                    if(dereferences != null)
                        modifiedVariableName = modifiedVariableName.substring(0, startIndex) + dereferences + modifiedVariableName.substring(startIndex);
                    switch(returnedNode.getNodeType()) {
                        case VARIABLE_NAME: //Variable was found without additional text -> valid pointer reference
                            if(text == null)
                                return new VariableNameNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) +
                                        variablePrefixAppendAfterSearch + variableName);

                            //Variable composition
                            List<Node> nodes = new ArrayList<>();
                            nodes.add(new VariableNameNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) +
                                    variablePrefixAppendAfterSearch + modifiedVariableName));
                            nodes.add(new TextValueNode(pos, text));
                            return new ListNode(nodes);

                        case LIST: //Variable was found with additional text -> no valid pointer reference
                        case TEXT_VALUE: //Variable was not found
                        default: //Default should never be reached
                            return new TextValueNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) +
                                    variablePrefixAppendAfterSearch + variableName);
                    }
                }
            }

            return new TextValueNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) + variablePrefixAppendAfterSearch + variableName);
        }

        String returendVariableName = optionalReturnedVariableName.get();
        if(returendVariableName.length() == variableName.length())
            return new VariableNameNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) + variablePrefixAppendAfterSearch + variableName);

        //Variable composition
        List<Node> nodes = new ArrayList<>();
        //Add matching part of variable as VariableNameNode
        nodes.add(new VariableNameNode(pos, (moduleName == null?"":("[[" + moduleName + "]]::")) + variablePrefixAppendAfterSearch + returendVariableName));
        nodes.add(new TextValueNode(pos, variableName.substring(returendVariableName.length()))); //Add composition part as TextValueNode
        return new ListNode(nodes);
    }
    private Node processUnprocessedVariableNameNode(DataObject compositeType, UnprocessedVariableNameNode node) {
        String variableName = node.getVariableName();

        if(executionFlags.rawVariableNames)
            return new VariableNameNode(node.getPos(), variableName);

        boolean isModuleVariable = variableName.startsWith("[[");
        String moduleName = null;
        if(isModuleVariable) {
            int indexModuleIdientifierEnd = variableName.indexOf("]]::");
            if(indexModuleIdientifierEnd == -1) {
                setErrno(InterpretingError.INVALID_AST_NODE, "Invalid variable name", node.getPos());

                return new TextValueNode(node.getPos(), variableName);
            }

            moduleName = variableName.substring(2, indexModuleIdientifierEnd);
            if(!isAlphaNumericWithUnderline(moduleName)) {
                setErrno(InterpretingError.INVALID_AST_NODE, "Invalid module name", node.getPos());

                return new TextValueNode(node.getPos(), variableName);
            }

            variableName = variableName.substring(indexModuleIdientifierEnd + 4);
        }

        if(variableName.startsWith("mp.") && compositeType != null && compositeType.getType() == DataType.OBJECT &&
                !compositeType.getObject().isClass()) {
            Set<String> variableNames = compositeType.getObject().getMethods().keySet().stream().
                    filter(key -> key.startsWith("mp.")).collect(Collectors.toSet());

            return convertVariableNameToVariableNameNodeOrComposition(moduleName, variableName, variableNames,
                    "", false, node.getPos());
        }

        if(variableName.startsWith("op:") && compositeType != null && compositeType.getType() == DataType.OBJECT &&
                !compositeType.getObject().isClass()) {
            Set<String> variableNames = compositeType.getObject().getMethods().keySet().stream().
                    filter(key -> key.startsWith("op:")).collect(Collectors.toSet());

            return convertVariableNameToVariableNameNodeOrComposition(moduleName, variableName, variableNames,
                    "", false, node.getPos());
        }

        if(variableName.startsWith("to:") && compositeType != null && compositeType.getType() == DataType.OBJECT &&
                !compositeType.getObject().isClass()) {
            Set<String> variableNames = compositeType.getObject().getMethods().keySet().stream().
                    filter(key -> key.startsWith("to:")).collect(Collectors.toSet());

            return convertVariableNameToVariableNameNodeOrComposition(moduleName, variableName, variableNames,
                    "", false, node.getPos());
        }

        if(variableName.startsWith("$") || variableName.startsWith("&") || variableName.startsWith("fp.")) {
            Set<String> variableNames;
            if(compositeType != null) {
                if(compositeType.getType() == DataType.ERROR) {
                    variableNames = new HashSet<>(Arrays.asList(
                            "$text",
                            "$code",
                            "$message"
                    ));
                }else if(compositeType.getType() == DataType.STRUCT) {
                    variableNames = new HashSet<>(Arrays.asList(compositeType.getStruct().getMemberNames()));
                }else if(compositeType.getType() == DataType.OBJECT) {
                    variableNames = Arrays.stream(compositeType.getObject().getStaticMembers()).
                            map(DataObject::getVariableName).collect(Collectors.toSet());

                    if(!compositeType.getObject().isClass())
                        variableNames.addAll(Arrays.asList(compositeType.getObject().getMemberNames()));
                }else {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "Invalid composite type", node.getPos());

                    return new TextValueNode(node.getPos(), variableName);
                }
            }else {
                variableNames = getData().var.keySet();
            }

            return convertVariableNameToVariableNameNodeOrComposition(moduleName, variableName, variableNames,
                    "", variableName.startsWith("$"), node.getPos());
        }

        if(compositeType != null) {
            setErrno(InterpretingError.INVALID_AST_NODE, "Invalid composite type member name: \"" + variableName + "\"", node.getPos());

            return new TextValueNode(node.getPos(), variableName);
        }

        final boolean isLinkerFunction;
        final String prefix;
        if(!isModuleVariable && variableName.startsWith("func.")) {
            isLinkerFunction = false;
            prefix = "func.";

            variableName = variableName.substring(5);
        }else if(!isModuleVariable && variableName.startsWith("fn.")) {
            isLinkerFunction = false;
            prefix = "fn.";

            variableName = variableName.substring(3);
        }else if(!isModuleVariable && variableName.startsWith("linker.")) {
            isLinkerFunction = true;
            prefix = "linker.";

            variableName = variableName.substring(7);
        }else if(!isModuleVariable && variableName.startsWith("ln.")) {
            isLinkerFunction = true;
            prefix = "ln.";

            variableName = variableName.substring(3);
        }else {
            setErrno(InterpretingError.INVALID_AST_NODE, "Invalid variable name", node.getPos());

            return new TextValueNode(node.getPos(), variableName);
        }

        return convertVariableNameToVariableNameNodeOrComposition(null, variableName,
                funcs.entrySet().stream().filter(entry -> {
                    return entry.getValue().isLinkerFunction() == isLinkerFunction;
                }).map(Entry::getKey).collect(Collectors.toSet()), prefix, false, node.getPos());
    }

    private Node processFunctionCallPreviousNodeValueNode(FunctionCallPreviousNodeValueNode node, DataObject previousValue) {
        if(previousValue != null) {
            if(previousValue.getType() == DataType.FUNCTION_POINTER || previousValue.getType() == DataType.TYPE)
                return node;

            if(previousValue.getType() == DataType.STRUCT && previousValue.getStruct().isDefinition())
                return node;

            if(previousValue.getType() == DataType.OBJECT && (previousValue.getObject().isClass() ||
                    previousValue.getObject().getMethods().containsKey("op:call")))
                return node;
        }

        //Previous node value wasn't a function -> return children of node in between "(" and ")" as ListNode
        List<Node> nodes = new ArrayList<>();
        nodes.add(new TextValueNode(node.getPos(), "(" + node.getLeadingWhitespace()));
        nodes.addAll(node.getChildren());
        nodes.add(new TextValueNode(node.getPos(), node.getTrailingWhitespace() + ")"));
        return new ListNode(nodes);
    }

    /**
     * @return Might return null
     */
    private DataObject interpretListNode(DataObject compositeType, ListNode node) {
        List<DataObject> dataObjects = new LinkedList<>();
        DataObject previousDataObject = null;

        for(Node childNode:node.getChildren()) {
            if(childNode.getNodeType() == NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE && previousDataObject != null) {
                try {
                    Node ret = processFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)childNode, previousDataObject);
                    if(ret.getNodeType() == NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE) {
                        dataObjects.remove(dataObjects.size() - 1); //Remove last data Object, because it is used as function pointer for a function call
                        dataObjects.add(interpretFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)ret, previousDataObject));
                    }else {
                        dataObjects.add(interpretNode(null, ret));
                    }
                }catch(ClassCastException e) {
                    dataObjects.add(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos()));
                }

                previousDataObject = dataObjects.get(dataObjects.size() - 1);

                continue;
            }

            DataObject ret = interpretNode(compositeType, childNode);
            if(ret != null)
                dataObjects.add(ret);

            compositeType = null;

            previousDataObject = ret;
        }

        return LangUtils.combineDataObjects(dataObjects, this, node.getPos());
    }

    private DataObject interpretValueNode(ValueNode node) {
        try {
            switch(node.getNodeType()) {
                case CHAR_VALUE:
                    return new DataObject().setChar(((CharValueNode)node).getChar());
                case TEXT_VALUE:
                    return new DataObject().setText(((TextValueNode)node).getText());
                case INT_VALUE:
                    return new DataObject().setInt(((IntValueNode)node).getInt());
                case LONG_VALUE:
                    return new DataObject().setLong(((LongValueNode)node).getLong());
                case FLOAT_VALUE:
                    return new DataObject().setFloat(((FloatValueNode)node).getFloat());
                case DOUBLE_VALUE:
                    return new DataObject().setDouble(((DoubleValueNode)node).getDouble());
                case NULL_VALUE:
                    return new DataObject();
                case VOID_VALUE:
                    return new DataObject().setVoid();

                default:
                    break;
            }
        }catch(ClassCastException e) {
            setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
        }

        return new DataObject().setError(new ErrorObject(InterpretingError.INVALID_AST_NODE));
    }

    private DataObject interpretParsingErrorNode(ParsingErrorNode node) {
        InterpretingError error = null;

        switch(node.getError()) {
            case BRACKET_MISMATCH:
                error = InterpretingError.BRACKET_MISMATCH;
                break;

            case CONT_FLOW_ARG_MISSING:
                error = InterpretingError.CONT_FLOW_ARG_MISSING;
                break;

            case EOF:
                error = InterpretingError.EOF;
                break;

            case INVALID_CON_PART:
                error = InterpretingError.INVALID_CON_PART;
                break;

            case INVALID_ASSIGNMENT:
                error = InterpretingError.INVALID_ASSIGNMENT;
                break;

            case INVALID_PARAMETER:
            case LEXER_ERROR:
                error = InterpretingError.INVALID_AST_NODE;
                break;
        }

        if(error == null)
            error = InterpretingError.INVALID_AST_NODE;
        return setErrnoErrorObject(error, node.getMessage() == null?node.getError().getErrorText():node.getMessage(),
                node.getPos());
    }

    /**
     * @return Returns true if any condition was true and if any block was executed
     */
    private boolean interpretIfStatementNode(IfStatementNode node) {
        List<IfStatementPartNode> ifPartNodes = node.getIfStatementPartNodes();
        if(ifPartNodes.isEmpty()) {
            setErrno(InterpretingError.INVALID_AST_NODE, "Empty if statement", node.getPos());

            return false;
        }

        for(IfStatementPartNode ifPartNode:ifPartNodes)
            if(interpretIfStatementPartNode(ifPartNode))
                return true;

        return false;
    }

    /**
     * @return Returns true if condition was true and if block was executed
     */
    private boolean interpretIfStatementPartNode(IfStatementPartNode node) {
        try {
            switch(node.getNodeType()) {
                case IF_STATEMENT_PART_IF:
                    if(!conversions.toBool(interpretOperationNode(((IfStatementPartIfNode)node).getCondition()),
                            node.getPos()))
                        return false;
                case IF_STATEMENT_PART_ELSE:
                    interpretAST(node.getIfBody());
                    return true;

                default:
                    break;
            }
        }catch(ClassCastException e) {
            setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
        }

        return false;
    }

    /**
     * @return Returns true if at least one loop iteration was executed
     */
    private boolean interpretLoopStatementNode(LoopStatementNode node) {
        List<LoopStatementPartNode> loopPartNodes = node.getLoopStatementPartNodes();
        if(loopPartNodes.isEmpty()) {
            setErrno(InterpretingError.INVALID_AST_NODE, "Empty loop statement", node.getPos());

            return false;
        }

        for(LoopStatementPartNode loopPartNode:loopPartNodes)
            if(interpretLoopStatementPartNode(loopPartNode))
                return true;

        return false;
    }

    /**
     * @return false if not break or continue with level <= 1<br>
     * true if break or continue with level > 1
     */
    private boolean shouldBreakCurrentLoopIteration() {
        if(executionState.stopExecutionFlag) {
            if(executionState.breakContinueCount == 0)
                return true;

            //Handle continue and break
            executionState.breakContinueCount -= 1;
            if(executionState.breakContinueCount > 0)
                return true;

            executionState.stopExecutionFlag = false;

            return !executionState.isContinueStatement;
        }

        return false;
    }

    /**
     * @return Returns true if at least one loop iteration was executed
     */
    private boolean interpretLoopStatementPartNode(LoopStatementPartNode node) {
        boolean flag = false;

        try {
            switch(node.getNodeType()) {
                case LOOP_STATEMENT_PART_LOOP:
                    while(true) {
                        interpretAST(node.getLoopBody());
                        if(shouldBreakCurrentLoopIteration())
                            return true;
                    }
                case LOOP_STATEMENT_PART_WHILE:
                    while(conversions.toBool(interpretOperationNode(((LoopStatementPartWhileNode)node).getCondition()),
                            node.getPos())) {
                        flag = true;

                        interpretAST(node.getLoopBody());
                        if(shouldBreakCurrentLoopIteration())
                            return true;
                    }

                    break;
                case LOOP_STATEMENT_PART_UNTIL:
                    while(!conversions.toBool(interpretOperationNode(((LoopStatementPartUntilNode)node).getCondition()),
                            node.getPos())) {
                        flag = true;

                        interpretAST(node.getLoopBody());
                        if(shouldBreakCurrentLoopIteration())
                            return true;
                    }

                    break;
                case LOOP_STATEMENT_PART_REPEAT:
                    LoopStatementPartRepeatNode repeatNode = (LoopStatementPartRepeatNode)node;
                    DataObject varPointer = interpretNode(null, repeatNode.getVarPointerNode());
                    if(varPointer.getType() != DataType.VAR_POINTER && varPointer.getType() != DataType.NULL) {
                        setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "con.repeat needs a variablePointer or a null value for the current iteration variable",
                                node.getPos());
                        return false;
                    }
                    DataObject var = varPointer.getType() == DataType.NULL?null:varPointer.getVarPointer().getVar();

                    DataObject numberObject = interpretNode(null, repeatNode.getRepeatCountNode());
                    Number number = numberObject == null?null:conversions.toNumber(numberObject, repeatNode.getRepeatCountNode().getPos());
                    if(number == null) {
                        setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "con.repeat needs a repeat count value",
                                node.getPos());
                        return false;
                    }

                    int iterations = number.intValue();
                    if(iterations < 0) {
                        setErrno(InterpretingError.INVALID_ARGUMENTS, "con.repeat repeat count can not be less than 0",
                                node.getPos());
                        return false;
                    }

                    for(int i = 0;i < iterations;i++) {
                        flag = true;

                        if(var != null) {
                            if(var.isFinalData() || var.isLangVar())
                                setErrno(InterpretingError.FINAL_VAR_CHANGE, "con.repeat current iteration value can not be set",
                                        node.getPos());
                            else if(var.getTypeConstraint().isTypeAllowed(DataType.INT))
                                var.setInt(i);
                            else
                                setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "con.repeat current iteration value can not be set",
                                        node.getPos());
                        }

                        interpretAST(node.getLoopBody());
                        if(shouldBreakCurrentLoopIteration())
                            return true;
                    }

                    break;
                case LOOP_STATEMENT_PART_FOR_EACH:
                    LoopStatementPartForEachNode forEachNode = (LoopStatementPartForEachNode)node;
                    varPointer = interpretNode(null, forEachNode.getVarPointerNode());
                    if(varPointer.getType() != DataType.VAR_POINTER) {
                        setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "con.foreach needs a variablePointer for the current element variable", node.getPos());
                        return false;
                    }

                    var = varPointer.getVarPointer().getVar();

                    CodePosition pos = forEachNode.getCompositeOrTextNode().getPos();

                    DataObject compositeOrText = interpretNode(null, forEachNode.getCompositeOrTextNode());
                    DataObject iterator = operators.opIter(compositeOrText, pos);
                    if(iterator == null) {
                        setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "The provided value to con.foreach does not support iteration", node.getPos());

                        return false;
                    }

                    while(true) {
                        DataObject hasNext = operators.opHasNext(iterator, pos);
                        if(hasNext == null) {
                            setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Invalid iterator implementation for value provided to con.foreach", node.getPos());

                            return false;
                        }

                        if(!conversions.toBool(hasNext, pos))
                            break;

                        DataObject next = operators.opNext(iterator, pos);
                        if(next == null) {
                            setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Invalid iterator implementation for value provided to con.foreach", node.getPos());

                            return false;
                        }

                        flag = true;
                        if(var != null) {
                            if(var.isFinalData() || var.isLangVar()) {
                                setErrno(InterpretingError.FINAL_VAR_CHANGE, "con.foreach current element value can not be set", node.getPos());
                                return false;
                            }else {
                                var.setData(next);
                            }
                        }

                        interpretAST(node.getLoopBody());
                        if(shouldBreakCurrentLoopIteration())
                            return true;
                    }

                    break;
                case LOOP_STATEMENT_PART_ELSE:
                    flag = true;
                    interpretAST(node.getLoopBody());
                    break;

                default:
                    break;
            }
        }catch(ClassCastException e) {
            setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
        }catch(DataTypeConstraintException e) {
            setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), node.getPos());
            return false;
        }

        return flag;
    }

    private void interpretLoopStatementContinueBreak(LoopStatementContinueBreakStatement node) {
        Node numberNode = node.getNumberNode();
        if(numberNode == null) {
            executionState.breakContinueCount = 1;
        }else {
            DataObject numberObject = interpretNode(null, numberNode);
            Number number = numberObject == null?null:conversions.toNumber(numberObject, numberNode.getPos());
            if(number == null) {
                setErrno(InterpretingError.INCOMPATIBLE_DATA_TYPE, "con." + (node.isContinueNode()?"continue":"break") + " needs either non value or a level number",
                        node.getPos());
                return;
            }

            executionState.breakContinueCount = number.intValue();
            if(executionState.breakContinueCount < 1) {
                executionState.breakContinueCount = 0;

                setErrno(InterpretingError.INVALID_ARGUMENTS, "con." + (node.isContinueNode()?"continue":"break") + " the level must be > 0", node.getPos());
                return;
            }
        }

        executionState.isContinueStatement = node.isContinueNode();
        executionState.stopExecutionFlag = true;
    }

    private void saveExecutionStopStateToVarAndReset(ExecutionState savedExecutionState) {
        savedExecutionState.stopExecutionFlag = executionState.stopExecutionFlag;
        savedExecutionState.returnedOrThrownValue = executionState.returnedOrThrownValue;
        savedExecutionState.isThrownValue = executionState.isThrownValue;
        savedExecutionState.returnOrThrowStatementPos = executionState.returnOrThrowStatementPos;
        savedExecutionState.breakContinueCount = executionState.breakContinueCount;
        savedExecutionState.isContinueStatement = executionState.isContinueStatement;
        executionState.stopExecutionFlag = false;
        executionState.returnedOrThrownValue = null;
        executionState.isThrownValue = false;
        executionState.returnOrThrowStatementPos = CodePosition.EMPTY;
        executionState.breakContinueCount = 0;
        executionState.isContinueStatement = false;
    }
    /**
     * @return Returns true if a catch or an else block was executed
     */
    private boolean interpretTryStatementNode(TryStatementNode node) {
        List<TryStatementPartNode> tryPartNodes = node.getTryStatementPartNodes();
        if(tryPartNodes.isEmpty()) {
            setErrno(InterpretingError.INVALID_AST_NODE, "Empty try statement", node.getPos());

            return false;
        }

        ExecutionState savedExecutionState = new ExecutionState();

        TryStatementPartNode tryPart = tryPartNodes.get(0);
        if(tryPart.getNodeType() != NodeType.TRY_STATEMENT_PART_TRY && tryPart.getNodeType() != NodeType.TRY_STATEMENT_PART_SOFT_TRY &&
                tryPart.getNodeType() != NodeType.TRY_STATEMENT_PART_NON_TRY) {
            setErrno(InterpretingError.INVALID_AST_NODE, "First part of try statement was no try nor soft try nor non try part", node.getPos());

            return false;
        }
        interpretTryStatementPartNode(tryPart);

        if(executionState.stopExecutionFlag)
            saveExecutionStopStateToVarAndReset(savedExecutionState);

        boolean flag = false;
        if(savedExecutionState.stopExecutionFlag && executionState.tryThrownError != null) {
            List<TryStatementPartNode> catchParts = new LinkedList<>();
            for(int i = 1;i < tryPartNodes.size();i++) {
                TryStatementPartNode tryPartNode = tryPartNodes.get(i);
                if(tryPartNode.getNodeType() != NodeType.TRY_STATEMENT_PART_CATCH)
                    break;

                catchParts.add(tryPartNode);
            }

            for(TryStatementPartNode catchPart:catchParts) {
                if(flag = interpretTryStatementPartNode(catchPart)) {
                    if(executionState.stopExecutionFlag) {
                        saveExecutionStopStateToVarAndReset(savedExecutionState);
                    }else {
                        //Reset saved execution state because the reason of the execution stop was handled by the catch block
                        savedExecutionState = new ExecutionState();
                        executionState.tryThrownError = null;

                        //Error was handled and (the try statement is the most outer try statement or no other error was thrown): reset $LANG_ERRNO
                        getAndClearErrnoErrorObject();
                    }

                    break;
                }
            }
        }

        boolean savedStopExecutionFlagForElseBlock = savedExecutionState.stopExecutionFlag;

        //Cancel execution stop because of error if most outer try block is reached or if inside a nontry statement
        if(savedExecutionState.stopExecutionFlag && (savedExecutionState.tryThrownError == null || savedExecutionState.tryBlockLevel == 0 ||
                (savedExecutionState.isSoftTry && savedExecutionState.tryBodyScopeID != scopeId)))
            savedExecutionState.stopExecutionFlag = false;

        if(!flag && !savedStopExecutionFlagForElseBlock) {
            TryStatementPartNode elsePart = null;
            if(tryPartNodes.size() > 1) {
                if(tryPartNodes.get(tryPartNodes.size() - 2).getNodeType() == NodeType.TRY_STATEMENT_PART_ELSE)
                    elsePart = tryPartNodes.get(tryPartNodes.size() - 2);
                if(tryPartNodes.get(tryPartNodes.size() - 1).getNodeType() == NodeType.TRY_STATEMENT_PART_ELSE)
                    elsePart = tryPartNodes.get(tryPartNodes.size() - 1);
            }
            if(elsePart != null) {
                flag = interpretTryStatementPartNode(elsePart);

                if(executionState.stopExecutionFlag)
                    saveExecutionStopStateToVarAndReset(savedExecutionState);
            }
        }

        TryStatementPartNode finallyPart = null;
        if(tryPartNodes.size() > 1 && tryPartNodes.get(tryPartNodes.size() - 1).getNodeType() == NodeType.TRY_STATEMENT_PART_FINALLY)
            finallyPart = tryPartNodes.get(tryPartNodes.size() - 1);

        if(finallyPart != null)
            interpretTryStatementPartNode(finallyPart);

        //Reset saved execution flag to stop execution if finally has not set the stop execution flag
        if(!executionState.stopExecutionFlag) {
            executionState.stopExecutionFlag = savedExecutionState.stopExecutionFlag;
            executionState.returnedOrThrownValue = savedExecutionState.returnedOrThrownValue;
            executionState.isThrownValue = savedExecutionState.isThrownValue;
            executionState.returnOrThrowStatementPos = savedExecutionState.returnOrThrowStatementPos;
            executionState.breakContinueCount = savedExecutionState.breakContinueCount;
            executionState.isContinueStatement = savedExecutionState.isContinueStatement;
        }

        return flag;
    }

    /**
     * @return Returns true if a catch or an else block was executed
     */
    private boolean interpretTryStatementPartNode(TryStatementPartNode node) {
        boolean flag = false;

        try {
            switch(node.getNodeType()) {
                case TRY_STATEMENT_PART_TRY:
                case TRY_STATEMENT_PART_SOFT_TRY:
                    executionState.tryThrownError = null;
                    executionState.tryBlockLevel++;
                    boolean isSoftTryOld = executionState.isSoftTry;
                    executionState.isSoftTry = node.getNodeType() == NodeType.TRY_STATEMENT_PART_SOFT_TRY;
                    int oldTryBlockScopeID = executionState.tryBodyScopeID;
                    executionState.tryBodyScopeID = scopeId;

                    try {
                        interpretAST(node.getTryBody());
                    }finally {
                        executionState.tryBlockLevel--;
                        executionState.isSoftTry = isSoftTryOld;
                        executionState.tryBodyScopeID = oldTryBlockScopeID;
                    }
                    break;
                case TRY_STATEMENT_PART_NON_TRY:
                    executionState.tryThrownError = null;
                    int oldTryBlockLevel = executionState.tryBlockLevel;
                    executionState.tryBlockLevel = 0;
                    isSoftTryOld = executionState.isSoftTry;
                    executionState.isSoftTry = false;
                    oldTryBlockScopeID = executionState.tryBodyScopeID;
                    executionState.tryBodyScopeID = 0;

                    try {
                        interpretAST(node.getTryBody());
                    }finally {
                        executionState.tryBlockLevel = oldTryBlockLevel;
                        executionState.isSoftTry = isSoftTryOld;
                        executionState.tryBodyScopeID = oldTryBlockScopeID;
                    }
                    break;
                case TRY_STATEMENT_PART_CATCH:
                    if(executionState.tryThrownError == null)
                        return false;

                    TryStatementPartCatchNode catchNode = (TryStatementPartCatchNode)node;
                    if(catchNode.getExceptions() != null) {
                        if(catchNode.getExceptions().isEmpty()) {
                            setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Empty catch part \"catch()\" is not allowed!\n"
                                    + "For checking all warnings \"catch\" without \"()\" should be used", node.getPos());

                            return false;
                        }

                        List<DataObject> catchErrors = new LinkedList<>();
                        List<DataObject> interpretedNodes = new LinkedList<>();
                        int foundErrorIndex = -1;
                        DataObject previousDataObject = null;
                        for(Node argument:catchNode.getExceptions()) {
                            if(argument.getNodeType() == NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE && previousDataObject != null) {
                                try {
                                    Node ret = processFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)argument, previousDataObject);
                                    if(ret.getNodeType() == NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE) {
                                        interpretedNodes.remove(interpretedNodes.size() - 1); //Remove last data Object, because it is used as function pointer for a function call
                                        interpretedNodes.add(interpretFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)ret, previousDataObject));
                                    }else {
                                        interpretedNodes.add(interpretNode(null, ret));
                                    }
                                }catch(ClassCastException e) {
                                    interpretedNodes.add(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos()));
                                }

                                previousDataObject = interpretedNodes.get(interpretedNodes.size() - 1);

                                continue;
                            }

                            DataObject argumentValue = interpretNode(null, argument);
                            if(argumentValue == null) {
                                previousDataObject = null;

                                continue;
                            }

                            interpretedNodes.add(argumentValue);
                            previousDataObject = argumentValue;
                        }
                        List<DataObject> errorList = LangUtils.combineArgumentsWithoutArgumentSeparators(interpretedNodes, this, node.getPos());
                        for(DataObject dataObject:errorList) {
                            if(dataObject.getType() != DataType.ERROR) {
                                setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Variable with type other than " + DataType.ERROR + " in catch statement",
                                        node.getPos());

                                continue;
                            }

                            if(dataObject.getError().getInterprettingError() == executionState.tryThrownError)
                                foundErrorIndex = catchErrors.size();

                            catchErrors.add(dataObject);
                        }

                        if(foundErrorIndex == -1)
                            return false;
                    }

                    flag = true;

                    interpretAST(node.getTryBody());
                    break;
                case TRY_STATEMENT_PART_ELSE:
                    if(executionState.tryThrownError != null)
                        return false;

                    flag = true;

                    interpretAST(node.getTryBody());
                    break;
                case TRY_STATEMENT_PART_FINALLY:
                    interpretAST(node.getTryBody());
                    break;

                default:
                    break;
            }
        }catch(ClassCastException e) {
            setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
        }

        return flag;
    }

    private DataObject interpretOperationNode(OperationNode node) {
        DataObject leftSideOperand = (node.getOperator().isUnary() && node.getOperator().isLazyEvaluation())?null:interpretNode(null, node.getLeftSideOperand());
        DataObject middleOperand = (!node.getOperator().isTernary() || node.getOperator().isLazyEvaluation())?null:interpretNode(null, node.getMiddleOperand());
        DataObject rightSideOperand = (node.getOperator().isUnary() || node.getOperator().isLazyEvaluation())?null:interpretNode(null, node.getRightSideOperand());

        //Forward Java null values for NON operators
        if(leftSideOperand == null && (node.getOperator() == Operator.NON || node.getOperator() == Operator.CONDITIONAL_NON || node.getOperator() == Operator.MATH_NON)) {
            return null;
        }

        //Allow null values in slice and replace with Lang VOID values
        if(node.getOperator() == Operator.SLICE || node.getOperator() == Operator.OPTIONAL_SLICE) {
            if(middleOperand == null)
                middleOperand = new DataObject().setVoid();

            if(rightSideOperand == null)
                rightSideOperand = new DataObject().setVoid();
        }

        if((leftSideOperand == null && (!node.getOperator().isUnary() || !node.getOperator().isLazyEvaluation())) ||
                (!node.getOperator().isLazyEvaluation() && ((!node.getOperator().isUnary() && rightSideOperand == null) ||
                        (node.getOperator().isTernary() && middleOperand == null))))
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing operand", node.getPos());

        if(node.getOperatorType() == OperatorType.ALL) {
            DataObject output;
            switch(node.getOperator()) {
                //Binary
                case COMMA:
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                            "The COMMA operator is parser-only (If you meant the text value of \",\", you must escape the COMMA operator: \"\\,\")",
                            node.getPos());
                case OPTIONAL_GET_ITEM:
                    output = operators.opOptionalGetItem(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case GET_ITEM:
                    output = operators.opGetItem(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case OPTIONAL_SLICE:
                    output = operators.opOptionalSlice(leftSideOperand, middleOperand, rightSideOperand, node.getPos());
                    break;
                case SLICE:
                    output = operators.opSlice(leftSideOperand, middleOperand, rightSideOperand, node.getPos());
                    break;
                case MEMBER_ACCESS_POINTER:
                    if(leftSideOperand.getType() != DataType.VAR_POINTER)
                        return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                                "The left side operand of the member access pointer operator (\"" + node.getOperator().getSymbol() + "\") must be a pointer",
                                node.getPos());

                    leftSideOperand = leftSideOperand.getVarPointer().getVar();
                    if(leftSideOperand == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_PTR, node.getPos());

                    if(!LangUtils.isMemberAccessAllowed(leftSideOperand))
                        return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                                "The left side operand of the member access pointer operator (\"" + node.getOperator().getSymbol() + "\") must be a pointer pointing to a composite type",
                                node.getPos());

                    return interpretNode(leftSideOperand, node.getRightSideOperand());
                case MEMBER_ACCESS:
                    if(!(node.getLeftSideOperand() instanceof TextValueNode && leftSideOperand.getType() == DataType.TEXT &&
                            leftSideOperand.getText().toString().equals("super")) && !LangUtils.isMemberAccessAllowed(leftSideOperand))
                        return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                                "The left side operand of the member access operator (\"" + node.getOperator().getSymbol() + "\") must be a composite type",
                                node.getPos());

                    return interpretNode(leftSideOperand, node.getRightSideOperand());
                case MEMBER_ACCESS_THIS:
                    DataObject compositeType = getData().var.get("&this");
                    if(compositeType == null || (compositeType.getType() != DataType.STRUCT && compositeType.getType() != DataType.OBJECT))
                        return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                                "\"&this\" is not present or invalid for the member access this operator (\"" + node.getOperator().getSymbol() + "\")",
                                node.getPos());

                    return interpretNode(compositeType, node.getLeftSideOperand());
                case OPTIONAL_MEMBER_ACCESS:
                    if(leftSideOperand.getType() == DataType.NULL || leftSideOperand.getType() == DataType.VOID)
                        return new DataObject().setVoid();

                    if(!LangUtils.isMemberAccessAllowed(leftSideOperand))
                        return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                                "The left side operand of the member access operator (\"" + node.getOperator().getSymbol() + "\") must be a composite type",
                                node.getPos());

                    return interpretNode(leftSideOperand, node.getRightSideOperand());

                default:
                    return null;
            }

            if(output == null)
                return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The \"" + node.getOperator().getSymbol() + "\" operator is not defined for " + leftSideOperand.getType().name() + (
                                node.getOperator().isTernary()?", " + middleOperand.getType().name() + ",":"") + (!node.getOperator().isUnary()?" and " + rightSideOperand.getType().name():""),
                        node.getPos());

            return output;
        }else if(node.getOperatorType() == OperatorType.GENERAL) {
            DataObject output;
            switch(node.getOperator()) {
                //Unary
                case NON:
                    return leftSideOperand;
                case LEN:
                    output = operators.opLen(leftSideOperand, node.getPos());
                    break;
                case DEEP_COPY:
                    output = operators.opDeepCopy(leftSideOperand, node.getPos());
                    break;

                //Binary
                case CONCAT:
                    output = operators.opConcat(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case SPACESHIP:
                    output = operators.opSpaceship(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case ELVIS:
                    if(conversions.toBool(leftSideOperand, node.getPos()))
                        return leftSideOperand;

                    rightSideOperand = interpretNode(null, node.getRightSideOperand());
                    if(rightSideOperand == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing operand", node.getPos());
                    return rightSideOperand;
                case NULL_COALESCING:
                    if(leftSideOperand.getType() != DataType.NULL && leftSideOperand.getType() != DataType.VOID)
                        return leftSideOperand;

                    rightSideOperand = interpretNode(null, node.getRightSideOperand());
                    if(rightSideOperand == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing operand", node.getPos());
                    return rightSideOperand;

                //Ternary
                case INLINE_IF:
                    DataObject operand = conversions.toBool(leftSideOperand, node.getPos())?
                            interpretNode(null, node.getMiddleOperand()):
                            interpretNode(null, node.getRightSideOperand());

                    if(operand == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing operand", node.getPos());
                    return operand;

                default:
                    return null;
            }

            if(output == null)
                return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The \"" + node.getOperator().getSymbol() + "\" operator is not defined for " + leftSideOperand.getType().name() + (
                                node.getOperator().isTernary()?", " + middleOperand.getType().name() + ",":"") + (!node.getOperator().isUnary()?" and " + rightSideOperand.getType().name():""),
                        node.getPos());

            return output;
        }else if(node.getOperatorType() == OperatorType.MATH) {
            DataObject output = null;

            switch(node.getOperator()) {
                //Unary
                case MATH_NON:
                    output = leftSideOperand;
                    break;
                case POS:
                    output = operators.opPos(leftSideOperand, node.getPos());
                    break;
                case INV:
                    output = operators.opInv(leftSideOperand, node.getPos());
                    break;
                case BITWISE_NOT:
                    output = operators.opNot(leftSideOperand, node.getPos());
                    break;
                case INC:
                    output = operators.opInc(leftSideOperand, node.getPos());
                    break;
                case DEC:
                    output = operators.opDec(leftSideOperand, node.getPos());
                    break;

                //Binary
                case POW:
                    output = operators.opPow(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case MUL:
                    output = operators.opMul(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case DIV:
                    output = operators.opDiv(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case TRUNC_DIV:
                    output = operators.opTruncDiv(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case FLOOR_DIV:
                    output = operators.opFloorDiv(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case CEIL_DIV:
                    output = operators.opCeilDiv(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case MOD:
                    output = operators.opMod(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case ADD:
                    output = operators.opAdd(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case SUB:
                    output = operators.opSub(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case LSHIFT:
                    output = operators.opLshift(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case RSHIFT:
                    output = operators.opRshift(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case RZSHIFT:
                    output = operators.opRzshift(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case BITWISE_AND:
                    output = operators.opAnd(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case BITWISE_XOR:
                    output = operators.opXor(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case BITWISE_OR:
                    output = operators.opOr(leftSideOperand, rightSideOperand, node.getPos());
                    break;

                default:
                    break;
            }

            if(output == null)
                return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The \"" + node.getOperator().getSymbol() + "\" operator is not defined for " + leftSideOperand.getType().name() + (
                                node.getOperator().isTernary()?", " + middleOperand.getType().name() + ",":"") + (!node.getOperator().isUnary()?" and " + rightSideOperand.getType().name():""),
                        node.getPos());

            return output;
        }else if(node.getOperatorType() == OperatorType.CONDITION) {
            boolean conditionOutput = false;

            switch(node.getOperator()) {
                //Unary (Logical operators)
                case CONDITIONAL_NON:
                case NOT:
                    conditionOutput = conversions.toBool(leftSideOperand, node.getPos());

                    if(node.getOperator() == Operator.NOT)
                        conditionOutput = !conditionOutput;
                    break;

                //Binary (Logical operators)
                case AND:
                    boolean leftSideOperandBoolean = conversions.toBool(leftSideOperand, node.getPos());
                    if(leftSideOperandBoolean) {
                        rightSideOperand = interpretNode(null, node.getRightSideOperand());
                        if(rightSideOperand == null)
                            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing operand", node.getPos());
                        conditionOutput = conversions.toBool(rightSideOperand, node.getPos());
                    }else {
                        conditionOutput = false;
                    }
                    break;
                case OR:
                    leftSideOperandBoolean = conversions.toBool(leftSideOperand, node.getPos());
                    if(leftSideOperandBoolean) {
                        conditionOutput = true;
                    }else {
                        rightSideOperand = interpretNode(null, node.getRightSideOperand());
                        if(rightSideOperand == null)
                            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing operand", node.getPos());
                        conditionOutput = conversions.toBool(rightSideOperand, node.getPos());
                    }
                    break;

                //Binary (Comparison operators)
                case INSTANCE_OF:
                    DataObject dataObject = leftSideOperand;
                    DataObject typeObject = rightSideOperand;

                    if(typeObject.getType() == DataType.TYPE) {
                        conditionOutput = leftSideOperand.getType() == rightSideOperand.getTypeValue();

                        break;
                    }

                    if(typeObject.getType() == DataType.STRUCT) {
                        StructObject typeStruct = typeObject.getStruct();

                        if(!typeStruct.isDefinition())
                            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The second operand of the \"" +
                                    node.getOperator().getSymbol() + "\" operator must be a struct definition ", node.getPos());

                        if(dataObject.getType() == DataType.STRUCT) {
                            StructObject dataStruct = dataObject.getStruct();

                            conditionOutput = !dataStruct.isDefinition() && dataStruct.getStructBaseDefinition().equals(typeStruct);

                            break;
                        }

                        conditionOutput = false;

                        break;
                    }

                    if(typeObject.getType() == DataType.OBJECT) {
                        LangObject typeClass = typeObject.getObject();

                        if(!typeClass.isClass())
                            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The second operand of the \"" +
                                    node.getOperator().getSymbol() + "\" operator must be a class", node.getPos());

                        if(dataObject.getType() == DataType.OBJECT) {
                            LangObject langObject = dataObject.getObject();

                            conditionOutput = !langObject.isClass() && langObject.isInstanceOf(typeClass);

                            break;
                        }

                        conditionOutput = false;

                        break;
                    }

                    return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "The second operand of the \"" +
                            node.getOperator().getSymbol() + "\" operator must be of type " + DataType.TYPE + ", " +
                            DataType.STRUCT + ", or " + DataType.OBJECT, node.getPos());
                case EQUALS:
                case NOT_EQUALS:
                    conditionOutput = operators.isEquals(leftSideOperand, rightSideOperand, node.getPos());

                    if(node.getOperator() == Operator.NOT_EQUALS)
                        conditionOutput = !conditionOutput;
                    break;
                case MATCHES:
                case NOT_MATCHES:
                    try {
                        conditionOutput = LangRegEx.matches(conversions.toText(leftSideOperand, node.getPos()),
                                conversions.toText(rightSideOperand, node.getPos()));
                    }catch(InvalidPatternSyntaxException e) {
                        return setErrnoErrorObject(InterpretingError.INVALID_REGEX_SYNTAX, e.getMessage(), node.getPos());
                    }

                    if(node.getOperator() == Operator.NOT_MATCHES)
                        conditionOutput = !conditionOutput;
                    break;
                case STRICT_EQUALS:
                case STRICT_NOT_EQUALS:
                    conditionOutput = operators.isStrictEquals(leftSideOperand, rightSideOperand, node.getPos());

                    if(node.getOperator() == Operator.STRICT_NOT_EQUALS)
                        conditionOutput = !conditionOutput;
                    break;
                case LESS_THAN:
                    conditionOutput = operators.isLessThan(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case GREATER_THAN:
                    conditionOutput = operators.isGreaterThan(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case LESS_THAN_OR_EQUALS:
                    conditionOutput = operators.isLessThanOrEquals(leftSideOperand, rightSideOperand, node.getPos());
                    break;
                case GREATER_THAN_OR_EQUALS:
                    conditionOutput = operators.isGreaterThanOrEquals(leftSideOperand, rightSideOperand, node.getPos());
                    break;

                default:
                    break;
            }

            return new DataObject().setBoolean(conditionOutput);
        }

        return null;
    }

    private void interpretReturnNode(ReturnNode node) {
        Node returnValueNode = node.getReturnValue();

        executionState.returnedOrThrownValue = returnValueNode == null?null:interpretNode(null, returnValueNode);
        executionState.returnOrThrowStatementPos = node.getPos();
        executionState.stopExecutionFlag = true;
    }

    private void interpretThrowNode(ThrowNode node) {
        Node throwValueNode = node.getThrowValue();

        DataObject errorObject = interpretNode(null, throwValueNode);
        if(errorObject == null || errorObject.getType() != DataType.ERROR) {
            executionState.returnedOrThrownValue = new DataObject().setError(new ErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE));
        }else {
            Node messageNode = node.getMessage();
            DataObject messageObject = messageNode == null?null:interpretNode(null, messageNode);
            if(messageObject != null)
                errorObject = new DataObject().setError(new ErrorObject(errorObject.getError().getInterprettingError(),
                        conversions.toText(messageObject, node.getMessage().getPos()).toString()));
            executionState.returnedOrThrownValue = errorObject;
        }
        executionState.isThrownValue = executionState.returnedOrThrownValue.getError().getErrno() > 0;
        executionState.returnOrThrowStatementPos = node.getPos();
        executionState.stopExecutionFlag = true;

        if(executionState.isThrownValue && scopeId > -1)
            setErrno(errorObject.getError().getInterprettingError(), errorObject.getError().getMessage(),
                    executionState.returnOrThrowStatementPos);

        if(executionState.returnedOrThrownValue.getError().getErrno() > 0 && executionState.tryBlockLevel > 0 && (!executionState.isSoftTry || executionState.tryBodyScopeID == scopeId)) {
            executionState.tryThrownError = executionState.returnedOrThrownValue.getError().getInterprettingError();
            executionState.stopExecutionFlag = true;
        }
    }

    private void interpretLangDataAndExecutionFlags(String langDataExecutionFlag, DataObject value, CodePosition pos) {
        if(value == null)
            value = new DataObject(); //Set value to null data object

        switch(langDataExecutionFlag) {
            //Data
            case "lang.version":
                String langVer = conversions.toText(value, pos).toString();
                Integer compVer = LangUtils.compareVersions(LangInterpreter.VERSION, langVer);
                if(compVer == null) {
                    setErrno(InterpretingError.LANG_VER_ERROR, "lang.version has an invalid format", pos);

                    return;
                }

                if(compVer > 0)
                    setErrno(InterpretingError.LANG_VER_WARNING, "Lang file's version is older than this version! The Lang file could not be executed correctly",
                            pos);
                else if(compVer < 0)
                    setErrno(InterpretingError.LANG_VER_ERROR, "Lang file's version is newer than this version! The Lang file will not be executed correctly!",
                            pos);

                break;

            case "lang.name":
                //Nothing to do
                break;

            //Flags
            case "lang.allowTermRedirect":
                Number number = conversions.toNumber(value, pos);
                if(number == null) {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "Invalid Data Type for the lang.allowTermRedirect flag!", pos);

                    return;
                }
                executionFlags.allowTermRedirect = number.intValue() != 0;
                break;
            case "lang.errorOutput":
                number = conversions.toNumber(value, pos);
                if(number == null) {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "Invalid Data Type for the lang.errorOutput flag!", pos);

                    return;
                }
                executionFlags.errorOutput = ExecutionFlags.ErrorOutputFlag.getErrorFlagFor(number.intValue());
                break;
            case "lang.test":
                number = conversions.toNumber(value, pos);
                if(number == null) {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "Invalid Data Type for the lang.test flag!", pos);

                    return;
                }

                boolean langTestNewValue = number.intValue() != 0;
                if(executionFlags.langTest && !langTestNewValue) {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "The lang.test flag can not be changed if it was once set to true!", pos);

                    return;
                }

                executionFlags.langTest = langTestNewValue;
                break;
            case "lang.rawVariableNames":
                number = conversions.toNumber(value, pos);
                if(number == null) {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "Invalid Data Type for the lang.rawVariableNames flag!", pos);

                    return;
                }
                executionFlags.rawVariableNames = number.intValue() != 0;
                break;
            case "lang.nativeStackTraces":
                number = conversions.toNumber(value, pos);
                if(number == null) {
                    setErrno(InterpretingError.INVALID_ARGUMENTS, "Invalid Data Type for the lang.nativeStackTraces flag!", pos);

                    return;
                }
                executionFlags.nativeStackTraces = number.intValue() != 0;
                break;
            default:
                setErrno(InterpretingError.INVALID_EXEC_FLAG_DATA, "\"" + langDataExecutionFlag + "\" is neither Lang data nor an execution flag", pos);
        }
    }
    private DataObject interpretAssignmentNode(AssignmentNode node) {
        DataObject rvalue = interpretNode(null, node.getRvalue());
        if(rvalue == null)
            rvalue = new DataObject(); //Set rvalue to null data object

        Node lvalueNode = node.getLvalue();
        if(lvalueNode == null)
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Assignment without lvalue", node.getPos());

        try {
            if(lvalueNode.getNodeType() == NodeType.OPERATION || lvalueNode.getNodeType() == NodeType.CONDITION ||
                    lvalueNode.getNodeType() == NodeType.MATH) {
                //Composite type lvalue assignment (MEMBER_ACCESS, MEMBER_ACCESS_THIS, and GET_ITEM)
                OperationNode operationNode = (OperationNode)lvalueNode;
                while((operationNode.getOperator() == Operator.NON ||
                        operationNode.getOperator() == Operator.CONDITIONAL_NON ||
                        operationNode.getOperator() == Operator.MATH_NON) &&
                        operationNode.getLeftSideOperand() instanceof OperationNode)
                    operationNode = (OperationNode)operationNode.getLeftSideOperand();

                boolean isMemberAccessPointerOperator = operationNode.getOperator() == Operator.MEMBER_ACCESS_POINTER;
                if(isMemberAccessPointerOperator || operationNode.getOperator() == Operator.MEMBER_ACCESS ||
                        operationNode.getOperator() == Operator.MEMBER_ACCESS_THIS) {
                    DataObject lvalue = interpretOperationNode(operationNode);
                    if(lvalue == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                                "Invalid arguments for member access" + (isMemberAccessPointerOperator?" pointer":""),
                                node.getPos());

                    String variableName = lvalue.getVariableName();
                    if(variableName == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT,
                                "Anonymous values can not be changed", node.getPos());

                    if(lvalue.isFinalData() || lvalue.isLangVar())
                        return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE,
                                node.getPos());

                    try {
                        lvalue.setData(rvalue);
                    }catch(DataTypeConstraintViolatedException e) {
                        return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                "Incompatible type for rvalue in assignment", node.getPos());
                    }

                    return rvalue;
                }else if(operationNode.getOperator() == Operator.GET_ITEM) {
                    DataObject compositeTypeObject = interpretNode(null, operationNode.getLeftSideOperand());
                    if(compositeTypeObject == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing composite type operand for set item",
                                node.getPos());

                    DataObject indexObject = interpretNode(null, operationNode.getRightSideOperand());
                    if(indexObject == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing index operand for set item",
                                node.getPos());

                    DataObject ret = operators.opSetItem(compositeTypeObject, indexObject, rvalue, operationNode.getPos());
                    if(ret == null)
                        return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                "Incompatible type for lvalue (composite type + index) or rvalue in assignment",
                                node.getPos());

                    return rvalue;
                }

                //Continue in "Lang translation" in the switch statement below
            }

            switch(lvalueNode.getNodeType()) {
                //Variable assignment
                case UNPROCESSED_VARIABLE_NAME:
                    UnprocessedVariableNameNode variableNameNode = (UnprocessedVariableNameNode)lvalueNode;
                    String variableName = variableNameNode.getVariableName();

                    boolean isModuleVariable = variableName.startsWith("[[");
                    String moduleName = null;
                    if(isModuleVariable) {
                        int indexModuleIdientifierEnd = variableName.indexOf("]]::");
                        if(indexModuleIdientifierEnd == -1) {
                            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid variable name", node.getPos());
                        }

                        moduleName = variableName.substring(2, indexModuleIdientifierEnd);
                        if(!isAlphaNumericWithUnderline(moduleName)) {
                            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid module name", node.getPos());
                        }

                        variableName = variableName.substring(indexModuleIdientifierEnd + 4);
                    }

                    if(isVarNameFullWithoutPrefix(variableName) || isVarNamePtrAndDereferenceWithoutPrefix(variableName)) {
                        if(!variableName.contains("[")) { //Pointer redirection is no longer supported
                            boolean[] flags = new boolean[] {false, false};
                            DataObject lvalue = getOrCreateDataObjectFromVariableName(null, moduleName, variableName,
                                    false, true, true, flags, node.getPos());
                            if(flags[0])
                                return lvalue; //Forward error from getOrCreateDataObjectFromVariableName()

                            variableName = lvalue.getVariableName();
                            if(variableName == null) {
                                return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT, "Anonymous values can not be changed", node.getPos());
                            }

                            if(lvalue.isFinalData() || lvalue.isLangVar()) {
                                if(flags[1])
                                    getData().var.remove(variableName);

                                return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, node.getPos());
                            }

                            try {
                                lvalue.setData(rvalue);
                            }catch(DataTypeConstraintViolatedException e) {
                                if(flags[1])
                                    getData().var.remove(variableName);

                                return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Incompatible type for rvalue in assignment", node.getPos());
                            }

                            if(variableName.startsWith("fp.")) {
                                final String functionNameCopy = variableName.substring(3);
                                Optional<Map.Entry<String, FunctionPointerObject>> ret = funcs.entrySet().stream().filter(entry -> {
                                    return functionNameCopy.equals(entry.getKey());
                                }).findFirst();

                                if(ret.isPresent())
                                    setErrno(InterpretingError.VAR_SHADOWING_WARNING, "\"" + variableName + "\" shadows a predfined, linker, or external function",
                                            node.getPos());
                            }
                            break;
                        }
                    }
                    //Fall-through to "Lang translation" if variableName is not valid

                    //Lang translation
                case ASSIGNMENT:
                case CHAR_VALUE:
                case CONDITION:
                case MATH:
                case OPERATION:
                case DOUBLE_VALUE:
                case ESCAPE_SEQUENCE:
                case UNICODE_ESCAPE_SEQUENCE:
                case FLOAT_VALUE:
                case FUNCTION_CALL:
                case FUNCTION_CALL_PREVIOUS_NODE_VALUE:
                case FUNCTION_DEFINITION:
                case IF_STATEMENT:
                case IF_STATEMENT_PART_ELSE:
                case IF_STATEMENT_PART_IF:
                case LOOP_STATEMENT:
                case LOOP_STATEMENT_PART_WHILE:
                case LOOP_STATEMENT_PART_UNTIL:
                case LOOP_STATEMENT_PART_REPEAT:
                case LOOP_STATEMENT_PART_FOR_EACH:
                case LOOP_STATEMENT_PART_LOOP:
                case LOOP_STATEMENT_PART_ELSE:
                case LOOP_STATEMENT_CONTINUE_BREAK:
                case TRY_STATEMENT:
                case TRY_STATEMENT_PART_TRY:
                case TRY_STATEMENT_PART_SOFT_TRY:
                case TRY_STATEMENT_PART_NON_TRY:
                case TRY_STATEMENT_PART_CATCH:
                case TRY_STATEMENT_PART_ELSE:
                case TRY_STATEMENT_PART_FINALLY:
                case INT_VALUE:
                case LIST:
                case LONG_VALUE:
                case NULL_VALUE:
                case PARSING_ERROR:
                case RETURN:
                case THROW:
                case TEXT_VALUE:
                case VARIABLE_NAME:
                case VOID_VALUE:
                case ARRAY:
                case STRUCT_DEFINITION:
                case CLASS_DEFINITION:
                    DataObject translationKeyDataObject = interpretNode(null, lvalueNode);
                    if(translationKeyDataObject == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid translationKey", node.getPos());

                    String translationKey = conversions.toText(translationKeyDataObject, node.getPos()).toString();
                    if(translationKey.startsWith("lang."))
                        interpretLangDataAndExecutionFlags(translationKey, rvalue, node.getPos());

                    getData().lang.put(translationKey, conversions.toText(rvalue, node.getPos()).toString());
                    break;

                case GENERAL:
                case ARGUMENT_SEPARATOR:
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Neither lvalue nor translationKey", node.getPos());
            }
        }catch(ClassCastException e) {
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos());
        }

        return rvalue;
    }

    /**
     * Will create a variable if doesn't exist or returns an error object, or returns null if shouldCreateDataObject is set to false and variable doesn't exist
     * @param supportsPointerReferencing If true, this node will return pointer reference as DataObject<br>
     *                                   (e.g. $[abc] is not in variableNames, but $abc is -> $[abc] will return a DataObject)
     * @param flags Will set by this method in format: [error, created]
     */
    private DataObject getOrCreateDataObjectFromVariableName(DataObject compositeType, String moduleName, String variableName, boolean supportsPointerReferencing,
                                                             boolean supportsPointerDereferencing, boolean shouldCreateDataObject, final boolean[] flags, CodePosition pos) {
        Map<String, DataObject> variables;
        if(compositeType != null) {
            if(compositeType.getType() == DataType.ERROR) {
                variables = new HashMap<>();
                variables.put("$text", new DataObject(compositeType.getError().getErrtxt()).setVariableName("$text").
                        setTypeConstraint(DataTypeConstraint.fromSingleAllowedType(DataType.TEXT)).setFinalData(true));
                variables.put("$code", new DataObject().setInt(compositeType.getError().getErrno()).setVariableName("$code").
                        setTypeConstraint(DataTypeConstraint.fromSingleAllowedType(DataType.INT)).setFinalData(true));

                String msg = compositeType.getError().getMessage();
                variables.put("$message", (msg == null?new DataObject().setNull():new DataObject().setText(msg)).setVariableName("$message").
                        setTypeConstraint(DataTypeConstraint.fromAllowedTypes(Arrays.asList(
                                DataType.NULL, DataType.TEXT
                        ))).setFinalData(true));
            }else if(compositeType.getType() == DataType.STRUCT) {
                variables = new HashMap<>();
                try {
                    for(String memberName:compositeType.getStruct().getMemberNames())
                        variables.put(memberName, compositeType.getStruct().getMember(memberName));
                }catch(DataTypeConstraintException e) {
                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);
                }
            }else if(compositeType.getType() == DataType.OBJECT) {
                variables = new HashMap<>();
                try {
                    if(variableName.startsWith("mp.")) {
                        compositeType.getObject().getMethods().entrySet().stream().filter(entry -> entry.getKey().startsWith("mp.")).forEach(entry -> {
                            String functionName = entry.getKey();
                            FunctionPointerObject functions = entry.getValue();

                            variables.put(functionName, new DataObject().setFunctionPointer(functions.withFunctionName(functionName)).
                                    setVariableName(functionName).setFinalData(true));
                        });
                    }else if(variableName.startsWith("op:")) {
                        compositeType.getObject().getMethods().entrySet().stream().filter(entry -> entry.getKey().startsWith("op:")).forEach(entry -> {
                            String functionName = entry.getKey();
                            FunctionPointerObject functions = entry.getValue();

                            variables.put(functionName, new DataObject().setFunctionPointer(functions.withFunctionName(functionName)).
                                    setVariableName(functionName).setFinalData(true));
                        });
                    }else if(variableName.startsWith("to:")) {
                        compositeType.getObject().getMethods().entrySet().stream().filter(entry -> entry.getKey().startsWith("to:")).forEach(entry -> {
                            String functionName = entry.getKey();
                            FunctionPointerObject functions = entry.getValue();

                            variables.put(functionName, new DataObject().setFunctionPointer(functions.withFunctionName(functionName)).
                                    setVariableName(functionName).setFinalData(true));
                        });
                    }else {
                        for(DataObject staticMember:compositeType.getObject().getStaticMembers())
                            variables.put(staticMember.getVariableName(), staticMember);

                        if(!compositeType.getObject().isClass()) {
                            //If a static member and a member have the same variable name, the static member will be shadowed
                            for(String memberName:compositeType.getObject().getMemberNames())
                                variables.put(memberName, compositeType.getObject().getMember(memberName));
                        }
                    }
                }catch(DataTypeConstraintException e) {
                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);
                }
            }else {
                return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Invalid composite type", pos);
            }
        }else if(moduleName == null) {
            variables = getData().var;
        }else {
            LangModule module = modules.get(moduleName);
            if(module == null) {
                if(flags != null && flags.length == 2)
                    flags[0] = true;

                return setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" is not loaded!", pos);
            }

            variables = module.getExportedVariables();
        }

        DataObject ret = variables.get(variableName);
        if(ret != null) {
            if(!ret.isAccessible(currentCallStackElement.getLangClass()))
                return setErrnoErrorObject(InterpretingError.MEMBER_NOT_ACCESSIBLE, "For member \"" + variableName + "\"");

            return ret;
        }

        if(supportsPointerDereferencing && variableName.contains("*")) {
            int index = variableName.indexOf('*');
            String referencedVariableName = variableName.substring(0, index) + variableName.substring(index + 1);
            DataObject referencedVariable = getOrCreateDataObjectFromVariableName(compositeType, moduleName, referencedVariableName,
                    supportsPointerReferencing, true, false, flags, pos);
            if(referencedVariable == null) {
                if(flags != null && flags.length == 2)
                    flags[0] = true;
                return setErrnoErrorObject(InterpretingError.INVALID_PTR, pos);
            }

            if(referencedVariable.getType() == DataType.VAR_POINTER)
                return referencedVariable.getVarPointer().getVar();

            return new DataObject(); //If no var pointer was dereferenced, return null data object
        }

        if(supportsPointerReferencing && variableName.contains("[") && variableName.contains("]")) { //Check referenced variable name
            int indexOpeningBracket = variableName.indexOf("[");
            int indexMatchingBracket = LangUtils.getIndexOfMatchingBracket(variableName, indexOpeningBracket, Integer.MAX_VALUE, '[', ']');
            if(indexMatchingBracket != variableName.length() - 1) {
                if(flags != null && flags.length == 2)
                    flags[0] = true;
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Non matching referencing brackets", pos);
            }

            String dereferencedVariableName = variableName.substring(0, indexOpeningBracket) + variableName.substring(indexOpeningBracket + 1, indexMatchingBracket);
            DataObject dereferencedVariable = getOrCreateDataObjectFromVariableName(compositeType, moduleName, dereferencedVariableName,
                    true, false, false, flags, pos);
            if(dereferencedVariable != null)
                return new DataObject().setVarPointer(new VarPointerObject(dereferencedVariable));

            if(shouldCreateDataObject) {
                if(flags != null && flags.length == 2)
                    flags[0] = true;

                return setErrnoErrorObject(InterpretingError.INVALID_PTR, "Pointer redirection is not supported", pos);
            }
        }

        if(!shouldCreateDataObject)
            return null;

        //Variable creation if possible
        if(compositeType != null || moduleName != null || isLangVarOrLangVarPointerRedirectionWithoutPrefix(variableName)) {
            if(flags != null && flags.length == 2)
                flags[0] = true;

            if(compositeType != null)
                return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, "Composite type members can not be created", pos);
            else if(moduleName == null)
                return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, pos);
            else
                return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, "Module variables can not be created", pos);
        }

        if(flags != null && flags.length == 2)
            flags[1] = true;

        DataObject dataObject = new DataObject().setVariableName(variableName);
        getData().var.put(variableName, dataObject);
        return dataObject;
    }
    /**
     * Will create a variable if doesn't exist or returns an error object
     */
    private DataObject interpretVariableNameNode(DataObject compositeType, VariableNameNode node) {
        String variableName = node.getVariableName();

        boolean isModuleVariable = compositeType == null && variableName.startsWith("[[");
        String moduleName = null;
        if(isModuleVariable) {
            int indexModuleIdientifierEnd = variableName.indexOf("]]::");
            if(indexModuleIdientifierEnd == -1) {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid variable name", node.getPos());
            }

            moduleName = variableName.substring(2, indexModuleIdientifierEnd);
            if(!isAlphaNumericWithUnderline(moduleName)) {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid module name", node.getPos());
            }

            variableName = variableName.substring(indexModuleIdientifierEnd + 4);
        }

        if(!isVarNameFullWithFuncsWithoutPrefix(variableName) && !isVarNamePtrAndDereferenceWithoutPrefix(variableName) &&
                !isOperatorMethodName(variableName) && !isConversionMethodName(variableName))
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid variable name", node.getPos());

        if((variableName.startsWith("mp.") || variableName.startsWith("op:") || variableName.startsWith("to:")) &&
                compositeType != null && compositeType.getType() == DataType.OBJECT && !compositeType.getObject().isClass()) {
            return getOrCreateDataObjectFromVariableName(compositeType, moduleName, variableName, false,
                    false, false, null, node.getPos());
        }

        if(variableName.startsWith("$") || variableName.startsWith("&") || variableName.startsWith("fp."))
            return getOrCreateDataObjectFromVariableName(compositeType, moduleName, variableName, variableName.startsWith("$"),
                    variableName.startsWith("$"), true, null, node.getPos());

        if(compositeType != null)
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid composite type member name: \"" + variableName + "\"", node.getPos());

        final boolean isLinkerFunction;
        if(!isModuleVariable && variableName.startsWith("func.")) {
            isLinkerFunction = false;

            variableName = variableName.substring(5);
        }else if(!isModuleVariable && variableName.startsWith("fn.")) {
            isLinkerFunction = false;

            variableName = variableName.substring(3);
        }else if(!isModuleVariable && variableName.startsWith("linker.")) {
            isLinkerFunction = true;

            variableName = variableName.substring(7);
        }else if(!isModuleVariable && variableName.startsWith("ln.")) {
            isLinkerFunction = true;

            variableName = variableName.substring(3);
        }else {
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid variable name", node.getPos());
        }

        final String variableNameCopy = variableName;
        Optional<Map.Entry<String, FunctionPointerObject>> ret = funcs.entrySet().stream().filter(entry -> {
            return entry.getValue().isLinkerFunction() == isLinkerFunction;
        }).filter(entry -> {
            return variableNameCopy.equals(entry.getKey());
        }).findFirst();

        if(!ret.isPresent())
            return setErrnoErrorObject(InterpretingError.FUNCTION_NOT_FOUND, "\"" + variableName + "\" was not found", node.getPos());

        FunctionPointerObject func = ret.get().getValue();
        return new DataObject().setFunctionPointer(func.withFunctionName(node.getVariableName())).setVariableName(node.getVariableName());
    }

    /**
     * @return Will return null for ("\!" escape sequence)
     */
    private DataObject interpretEscapeSequenceNode(EscapeSequenceNode node) {
        switch(node.getEscapeSequenceChar()) {
            case '0':
                return new DataObject().setChar('\0');
            case 'n':
                return new DataObject().setChar('\n');
            case 'r':
                return new DataObject().setChar('\r');
            case 'f':
                return new DataObject().setChar('\f');
            case 's':
                return new DataObject().setChar(' ');
            case 'e':
                return new DataObject("");
            case 'E':
                return new DataObject().setChar('\033');
            case 'b':
                return new DataObject().setChar('\b');
            case 't':
                return new DataObject().setChar('\t');
            case '$':
            case '&':
            case '#':
            case ',':
            case '.':
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '=':
            case '<':
            case '>':
            case '+':
            case '-':
            case '/':
            case '*':
            case '%':
            case '|':
            case '~':
            case '^':
            case '?':
            case ':':
            case '@':
            case '\u25b2':
            case '\u25bc':
            case '\"':
                return new DataObject().setChar(node.getEscapeSequenceChar());
            case '!':
                return null;
            case '\\':
                return new DataObject().setChar('\\');

            //If no escape sequence: Remove "\" anyway
            default:
                setErrno(InterpretingError.UNDEF_ESCAPE_SEQUENCE, "\"\\" + node.getEscapeSequenceChar() + "\" was used", node.getPos());

                return new DataObject().setChar(node.getEscapeSequenceChar());
        }
    }

    private DataObject interpretUnicodeEscapeSequenceNode(UnicodeEscapeSequenceNode node) {
        for(int i = 0;i < node.getHexCodepoint().length();i++) {
            char c = node.getHexCodepoint().charAt(i);

            if(!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F')))
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid unicode escape sequence: \"\\u{" + node.getHexCodepoint() + "}\"", node.getPos());
        }

        return new DataObject().setChar(Integer.valueOf(node.getHexCodepoint().toUpperCase(Locale.ENGLISH), 16));
    }

    private DataObject interpretArgumentSeparatotNode(ArgumentSeparatorNode node) {
        return new DataObject().setArgumentSeparator(node.getOriginalText());
    }

    DataObject getAndResetReturnValue() {
        DataObject retTmp = executionState.returnedOrThrownValue;
        executionState.returnedOrThrownValue = null;

        if(executionFlags.langTest && scopeId == langTestExpectedReturnValueScopeID) {
            if(langTestExpectedThrowValue != null) {
                InterpretingError gotError = retTmp != null && executionState.isThrownValue?retTmp.getError().getInterprettingError():null;
                langTestStore.addAssertResult(new LangTest.AssertResultThrow(gotError == langTestExpectedThrowValue,
                        printStackTrace(CodePosition.EMPTY), langTestMessageForLastTestResult, gotError, langTestExpectedThrowValue));

                langTestExpectedThrowValue = null;
            }

            if(langTestExpectedReturnValue != null) {
                langTestStore.addAssertResult(new LangTest.AssertResultReturn(!executionState.isThrownValue &&
                        operators.isStrictEquals(langTestExpectedReturnValue, retTmp, CodePosition.EMPTY),
                        printStackTrace(CodePosition.EMPTY),
                        langTestMessageForLastTestResult, retTmp, retTmp == null?null:conversions.toText(retTmp,
                        CodePosition.EMPTY).toString(), langTestExpectedReturnValue, conversions.toText(langTestExpectedReturnValue,
                        CodePosition.EMPTY).toString()));

                langTestExpectedReturnValue = null;
            }

            if(langTestExpectedNoReturnValue) {
                langTestStore.addAssertResult(new LangTest.AssertResultNoReturn(retTmp == null,
                        printStackTrace(CodePosition.EMPTY), langTestMessageForLastTestResult, retTmp, retTmp == null?null
                        :conversions.toText(retTmp, CodePosition.EMPTY).toString()));

                langTestExpectedNoReturnValue = false;
            }
            langTestMessageForLastTestResult = null;
            langTestExpectedReturnValueScopeID = 0;
        }

        executionState.isThrownValue = false;

        if(executionState.tryThrownError == null || executionState.tryBlockLevel == 0 || (executionState.isSoftTry &&
                executionState.tryBodyScopeID != scopeId))
            executionState.stopExecutionFlag = false;

        return retTmp == null?retTmp:new DataObject(retTmp);
    }
    boolean isThrownValue() {
        return executionState.isThrownValue ||
                (executionState.tryThrownError != null && executionState.tryBlockLevel > 0 &&
                        (!executionState.isSoftTry || executionState.tryBodyScopeID == scopeId));
    }

    DataObject callFunctionPointer(FunctionPointerObject fp, String functionName, List<DataObject> argumentList, CodePosition parentPos) {
        argumentList = new ArrayList<>(argumentList);

        LangObject thisObject = fp.getThisObject();
        int originalSuperLevel = -1;

        List<DataObject> combinedArgumentList = LangUtils.combineArgumentsWithoutArgumentSeparators(argumentList,
                this, parentPos);

        FunctionPointerObject.InternalFunction internalFunction;
        if(fp.getOverloadedFunctionCount() == 1) {
            internalFunction = fp.getFunction(0);
        }else {
            internalFunction = LangUtils.getMostRestrictiveFunction(fp, combinedArgumentList);
        }

        if(internalFunction == null)
            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "No matching function signature was found for the given arguments." +
                    " Available function signatures:\n    " + functionName + fp.getFunctions().stream().
                    map(FunctionPointerObject.InternalFunction::toFunctionSignatureSyntax).
                    collect(Collectors.joining("\n    " + functionName)));

        if(!internalFunction.isAccessible(getCurrentCallStackElement().getLangClass()))
            return setErrnoErrorObject(InterpretingError.MEMBER_NOT_ACCESSIBLE, "For member \"" + functionName + "\"");

        if(thisObject != null && !thisObject.isClass()) {
            originalSuperLevel = thisObject.getSuperLevel();
            thisObject.setSuperLevel(internalFunction.getSuperLevel());
        }

        LangObject memberOfClass = internalFunction.getMemberOfClass();

        try {
            String functionLangPath = internalFunction.getLangPath();
            String functionLangFile = internalFunction.getLangFile();

            functionName = (functionName == null || fp.getFunctionName() != null)?fp.toString():functionName;

            //Update call stack
            StackElement currentStackElement = getCurrentCallStackElement();
            pushStackElement(new StackElement(functionLangPath == null?currentStackElement.getLangPath():functionLangPath,
                    (functionLangPath == null && functionLangFile == null)?currentStackElement.getLangFile():functionLangFile,
                    memberOfClass != null && !memberOfClass.isClass()?memberOfClass.getClassBaseDefinition():memberOfClass,
                    memberOfClass == null?null:(memberOfClass.getClassName() == null?"<class>":memberOfClass.getClassName()),
                    functionName, currentStackElement.getModule()), parentPos);


            switch(internalFunction.getFunctionPointerType()) {
                case FunctionPointerObject.NORMAL:
                    LangNormalFunction normalFunction = internalFunction.getNormalFunction();
                    if(normalFunction == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "Function call of invalid FP", parentPos);

                    List<DataObject> parameterList = normalFunction.getParameterList();
                    List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList = normalFunction.getParameterDataTypeConstraintList();
                    List<LangBaseFunction.ParameterAnnotation> parameterAnnotationList = normalFunction.getParameterAnnotationList();
                    List<CodePosition> argumentPosList = normalFunction.getArgumentPosList();
                    int argCount = parameterList.size();

                    if(normalFunction.isCombinatorFunction()) {
                        combinedArgumentList = new ArrayList<>(combinedArgumentList.stream().map(DataObject::new).collect(Collectors.toList()));
                        combinedArgumentList.addAll(0, normalFunction.getCombinatorProvidedArgumentList());
                    }

                    AbstractSyntaxTree functionBody = normalFunction.getFunctionBody();

                    if(normalFunction.getVarArgsParameterIndex() == -1) {
                        if(!normalFunction.isCombinatorFunction() && combinedArgumentList.size() < argCount)
                            return setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Not enough arguments (%s needed)", argCount), parentPos);
                        if(combinedArgumentList.size() > argCount)
                            return setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Too many arguments (%s needed)", argCount), parentPos);
                    }else {
                        //Infinite combinator functions (= Combinator functions with var args argument) must be called exactly two times
                        if((!normalFunction.isCombinatorFunction() || normalFunction.getCombinatorFunctionCallCount() > 0) && combinedArgumentList.size() < argCount - 1)
                            return setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, String.format("Not enough arguments (at least %s needed)", argCount - 1), parentPos);
                    }

                    DataObject combinatorFunctionCallRet = null;
                    try {
                        Data callerData = getData();

                        enterScope();

                        //Copies must not be final
                        callerData.var.forEach((key, val) -> {
                            if(!val.isLangVar())
                                getData().var.put(key, new DataObject(val).setVariableName(val.getVariableName()));

                            if(val.isStaticData()) //Static Lang vars should also be copied
                                getData().var.put(key, val);
                        });

                        //Set this-object and This-class
                        if(thisObject != null) {
                            DataObject old = getData().var.put("&this", new DataObject().setObject(thisObject).
                                    setFinalData(true).setVariableName("&this"));
                            if(old != null && old.isStaticData())
                                setErrno(InterpretingError.VAR_SHADOWING_WARNING, "This-object \"&this\" shadows a static variable",
                                        functionBody.getPos());

                            old = getData().var.put("&This", new DataObject().setObject(thisObject.getClassBaseDefinition()).
                                    setFinalData(true).setVariableName("&This"));
                            if(old != null && old.isStaticData())
                                setErrno(InterpretingError.VAR_SHADOWING_WARNING, "This-class \"&This\" shadows a static variable",
                                        functionBody.getPos());
                        }

                        //Set arguments
                        int argumentIndex = 0;
                        for(int i = 0;i < argCount;i++) {
                            if(normalFunction.isCombinatorFunction() && argumentIndex >= combinedArgumentList.size() &&
                                    (normalFunction.getVarArgsParameterIndex() == -1 ||
                                            normalFunction.getCombinatorFunctionCallCount() == 0)) {
                                combinatorFunctionCallRet = normalFunction.combinatorCall(thisObject,
                                        internalFunction.getSuperLevel(), combinedArgumentList);

                                break;
                            }

                            final DataObject parameter = parameterList.get(i);
                            final DataObject.DataTypeConstraint typeConstraint = parameterDataTypeConstraintList.get(i);
                            final LangBaseFunction.ParameterAnnotation parameterAnnotation = parameterAnnotationList.get(i);
                            final CodePosition argumentPos = argumentPosList.get(i);

                            final String variableName = parameter.getVariableName();

                            if(parameterAnnotation == LangBaseFunction.ParameterAnnotation.VAR_ARGS) {
                                //Infinite combinator functions (= Combinator functions with var args argument) must be called exactly two times
                                if(normalFunction.isCombinatorFunction() && normalFunction.getCombinatorFunctionCallCount() == 0) {
                                    combinatorFunctionCallRet = normalFunction.combinatorCall(thisObject,
                                            internalFunction.getSuperLevel(), combinedArgumentList);

                                    break;
                                }

                                if(variableName.startsWith("$")) {
                                    //Text varargs
                                    if(!typeConstraint.equals(DataObject.CONSTRAINT_NORMAL)) {
                                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                                                "function parameter \"" + variableName + "\": Text var args argument must not have a type constraint definition",
                                                argumentPos);
                                    }

                                    List<DataObject> argumentListCopy = new ArrayList<>(argumentList);

                                    //Remove leading arguments
                                    for(int j = 0;j < i;j++)
                                        for(int k = 0;k < argumentListCopy.size();k++)
                                            if(argumentListCopy.remove(0).getType() == DataType.ARGUMENT_SEPARATOR)
                                                break;

                                    //Remove trailing arguments
                                    for(int j = 0;j < argCount - i - 1;j++)
                                        for(int k = argumentListCopy.size() - 1;k >= 0;k--)
                                            if(argumentListCopy.remove(k).getType() == DataType.ARGUMENT_SEPARATOR)
                                                break;

                                    DataObject dataObject = LangUtils.combineDataObjects(argumentListCopy,
                                            this, argumentPos);
                                    try {
                                        DataObject newDataObject = new DataObject(conversions.toText(dataObject == null?
                                                new DataObject().setVoid():dataObject, argumentPos)).setVariableName(variableName);

                                        DataObject old = getData().var.put(variableName, newDataObject);
                                        if(old != null && old.isStaticData())
                                            setErrno(InterpretingError.VAR_SHADOWING_WARNING, "Parameter \"" + variableName + "\" shadows a static variable",
                                                    argumentPos);
                                    }catch(DataTypeConstraintException e) {
                                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                                                "Invalid argument value for function parameter \"" + variableName + "\" (" + e.getMessage() + ")",
                                                argumentPos);
                                    }
                                }else {
                                    //Array varargs
                                    List<DataObject> varArgsArgumentList = combinedArgumentList.subList(i, combinedArgumentList.size() - argCount + i + 1).stream().
                                            map(DataObject::new).collect(Collectors.toList());

                                    for(int j = 0;j < varArgsArgumentList.size();j++) {
                                        DataObject varArgsArgument = varArgsArgumentList.get(j);
                                        if(!typeConstraint.isTypeAllowed(varArgsArgument.getType()))
                                            return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                                    "Invalid argument (Argument " + (argumentIndex + j + 1) + ") value for var args function parameter \"" +
                                                            variableName + "\": Value must be one of " + typeConstraint.getAllowedTypes(),
                                                    argumentPos);
                                    }

                                    try {
                                        DataObject newDataObject = new DataObject().
                                                setArray(varArgsArgumentList.toArray(new DataObject[0])).
                                                setVariableName(variableName);

                                        DataObject old = getData().var.put(variableName, newDataObject);
                                        if(old != null && old.isStaticData())
                                            setErrno(InterpretingError.VAR_SHADOWING_WARNING, "Parameter \"" + variableName + "\" shadows a static variable",
                                                    argumentPos);
                                    }catch(DataTypeConstraintException e) {
                                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                                                "Invalid argument value for function parameter \"" + variableName + "\" (" + e.getMessage() + ")",
                                                argumentPos);
                                    }
                                }

                                argumentIndex = combinedArgumentList.size() - argCount + i + 1;
                                continue;
                            }

                            if(parameterAnnotation == LangBaseFunction.ParameterAnnotation.CALL_BY_POINTER) {
                                try {
                                    DataObject newDataObject = new DataObject().
                                            setVarPointer(new VarPointerObject(combinedArgumentList.get(argumentIndex))).
                                            setVariableName(variableName);
                                    newDataObject.setTypeConstraint(typeConstraint);

                                    DataObject old = getData().var.put(variableName, newDataObject);
                                    if(old != null && old.isStaticData())
                                        setErrno(InterpretingError.VAR_SHADOWING_WARNING, "Parameter \"" + variableName + "\" shadows a static variable",
                                                argumentPos);
                                }catch(DataTypeConstraintException e) {
                                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                                            "Invalid argument value for function parameter \"" + variableName + "\" (" + e.getMessage() + ")",
                                            argumentPos);
                                }

                                argumentIndex++;
                                continue;
                            }

                            try {
                                DataObject value = combinedArgumentList.get(argumentIndex);
                                if(parameterAnnotation == LangBaseFunction.ParameterAnnotation.BOOLEAN) {
                                    value = new DataObject().setBoolean(conversions.toBool(value, argumentPos));
                                }else if(parameterAnnotation == LangBaseFunction.ParameterAnnotation.NUMBER) {
                                    value = conversions.convertToNumberAndCreateNewDataObject(value, argumentPos);
                                    if(value.getType() == DataType.NULL)
                                        return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                                "Invalid argument value for function parameter \"" + variableName + "\" (Must be a number)",
                                                argumentPos);
                                }else if(parameterAnnotation == LangBaseFunction.ParameterAnnotation.CALLABLE) {
                                    if(!LangUtils.isCallable(combinedArgumentList.get(argumentIndex)))
                                        return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                                "Invalid argument value for function parameter \"" + variableName + "\" (Must be callable)",
                                                argumentPos);
                                }

                                DataObject newDataObject = new DataObject(value).
                                        setVariableName(variableName);
                                if(typeConstraint != null)
                                    newDataObject.setTypeConstraint(typeConstraint);

                                DataObject old = getData().var.put(variableName, newDataObject);
                                if(old != null && old.isStaticData())
                                    setErrno(InterpretingError.VAR_SHADOWING_WARNING, "Parameter \"" + variableName + "\" shadows a static variable",
                                            argumentPos);
                            }catch(DataTypeConstraintViolatedException e) {
                                return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                        "Invalid argument value for function parameter \"" + variableName + "\" (" + e.getMessage() + ")",
                                        argumentPos);
                            }

                            argumentIndex++;
                        }

                        if(combinatorFunctionCallRet == null) {
                            //Call function
                            interpretAST(functionBody);
                        }
                    }finally {
                        Data scopeData = getData();

                        exitScope();

                        //Add translations after call
                        getData().lang.putAll(scopeData.lang);
                    }

                    if(fp.isDeprecated()) {
                        String message = String.format("Use of deprecated function \"%s\". This function will no longer be supported in \"%s\"!%s", functionName,
                                fp.getDeprecatedRemoveVersion() == null?"the future":fp.getDeprecatedRemoveVersion(),
                                fp.getDeprecatedReplacementFunction() == null?"":("\nUse \"" + fp.getDeprecatedReplacementFunction() + "\" instead!"));
                        setErrno(InterpretingError.DEPRECATED_FUNC_CALL, message, parentPos);
                    }

                    DataTypeConstraint returnValueTypeConstraint = combinatorFunctionCallRet == null?
                            normalFunction.getReturnValueTypeConstraint():null;

                    CodePosition returnOrThrowStatementPos = combinatorFunctionCallRet == null?
                            executionState.returnOrThrowStatementPos:CodePosition.EMPTY;

                    boolean thrownValue = isThrownValue();
                    DataObject retTmp = combinatorFunctionCallRet == null?LangUtils.nullToLangVoid(getAndResetReturnValue()):
                            combinatorFunctionCallRet;

                    if(returnValueTypeConstraint != null && !thrownValue) {
                        //Thrown values are always allowed

                        if(!returnValueTypeConstraint.isTypeAllowed(retTmp.getType()))
                            return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                                    "Invalid return value type \"" + retTmp.getType() + "\"",
                                    returnOrThrowStatementPos);
                    }

                    return retTmp;

                case FunctionPointerObject.NATIVE:
                    LangNativeFunction nativeFunction = internalFunction.getNativeFunction();
                    if(nativeFunction == null)
                        return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "Function call of invalid FP", parentPos);

                    DataObject ret = nativeFunction.callFunc(this, thisObject, internalFunction.getSuperLevel(), argumentList, combinedArgumentList);
                    if(fp.isDeprecated()) {
                        String message = String.format("Use of deprecated function \"%s\". This function will no longer be supported in \"%s\"!%s", functionName,
                                fp.getDeprecatedRemoveVersion() == null?"the future":fp.getDeprecatedRemoveVersion(),
                                fp.getDeprecatedReplacementFunction() == null?"":("\nUse \"" + fp.getDeprecatedReplacementFunction() + "\" instead!"));
                        setErrno(InterpretingError.DEPRECATED_FUNC_CALL, message, parentPos);
                    }

                    //Return non copy if copyStaticAndFinalModifiers flag is set for "func.asStatic()" and "func.asFinal()"
                    return ret == null?new DataObject().setVoid():(ret.isCopyStaticAndFinalModifiers()?ret:new DataObject(ret));

                default:
                    return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "Function call of invalid FP type", parentPos);
            }
        }finally {
            try {
                if(thisObject != null && !thisObject.isClass())
                    thisObject.setSuperLevel(originalSuperLevel);
            }finally {
                //Update call stack
                popStackElement();
            }
        }
    }
    DataObject callFunctionPointer(FunctionPointerObject fp, String functionName, List<DataObject> argumentValueList) {
        return callFunctionPointer(fp, functionName, argumentValueList, CodePosition.EMPTY);
    }
    private List<DataObject> interpretFunctionPointerArguments(List<Node> argumentList) {
        List<DataObject> argumentValueList = new LinkedList<>();
        DataObject previousDataObject = null;
        for(Node argument:argumentList) {
            if(argument.getNodeType() == NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE && previousDataObject != null) {
                try {
                    Node ret = processFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)argument, previousDataObject);
                    if(ret.getNodeType() == NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE) {
                        argumentValueList.remove(argumentValueList.size() - 1); //Remove last data Object, because it is used as function pointer for a function call
                        argumentValueList.add(interpretFunctionCallPreviousNodeValueNode((FunctionCallPreviousNodeValueNode)ret, previousDataObject));
                    }else {
                        argumentValueList.add(interpretNode(null, ret));
                    }
                }catch(ClassCastException e) {
                    argumentValueList.add(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, argument.getPos()));
                }

                previousDataObject = argumentValueList.get(argumentValueList.size() - 1);

                continue;
            }

            //Composite type unpacking
            if(argument.getNodeType() == NodeType.UNPROCESSED_VARIABLE_NAME) {
                try {
                    String variableName = ((UnprocessedVariableNameNode)argument).getVariableName();
                    if(variableName.contains("&") && variableName.endsWith("...")) {
                        boolean isModuleVariable = variableName.startsWith("[[");
                        String moduleName = null;
                        if(isModuleVariable) {
                            int indexModuleIdientifierEnd = variableName.indexOf("]]::");
                            if(indexModuleIdientifierEnd == -1) {
                                argumentValueList.add(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid variable name", argument.getPos()));

                                continue;
                            }

                            moduleName = variableName.substring(2, indexModuleIdientifierEnd);
                            if(!isAlphaNumericWithUnderline(moduleName)) {
                                argumentValueList.add(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid module name", argument.getPos()));

                                continue;
                            }

                            variableName = variableName.substring(indexModuleIdientifierEnd + 4);
                        }

                        if(variableName.startsWith("&")) {
                            DataObject dataObject = getOrCreateDataObjectFromVariableName(null, moduleName, variableName.
                                            substring(0, variableName.length() - 3), false, false,
                                    false, null, argument.getPos());
                            if(dataObject == null) {
                                argumentValueList.add(setErrnoErrorObject(InterpretingError.INVALID_ARR_PTR, "Unpacking of undefined variable",
                                        argument.getPos()));

                                continue;
                            }

                            if(dataObject.getType() == DataType.ARRAY) {
                                argumentValueList.addAll(LangUtils.asListWithArgumentSeparators(dataObject.getArray()));

                                continue;
                            }

                            if(dataObject.getType() == DataType.LIST) {
                                argumentValueList.addAll(LangUtils.separateArgumentsWithArgumentSeparators(dataObject.getList()));

                                continue;
                            }

                            if(dataObject.getType() == DataType.STRUCT) {
                                StructObject struct = dataObject.getStruct();

                                if(struct.isDefinition())
                                    argumentValueList.addAll(LangUtils.separateArgumentsWithArgumentSeparators(Arrays.stream(struct.getMemberNames()).
                                            map(DataObject::new).collect(Collectors.toList())));
                                else
                                    argumentValueList.addAll(LangUtils.separateArgumentsWithArgumentSeparators(Arrays.stream(struct.getMemberNames()).
                                            map(struct::getMember).collect(Collectors.toList())));

                                continue;
                            }

                            argumentValueList.add(setErrnoErrorObject(InterpretingError.INVALID_ARR_PTR, "Unpacking of unsupported composite type variable",
                                    argument.getPos()));

                            continue;
                        }
                    }
                }catch(ClassCastException e) {
                    argumentValueList.add(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, argument.getPos()));
                }
            }

            DataObject argumentValue = interpretNode(null, argument);
            if(argumentValue == null) {
                previousDataObject = null;

                continue;
            }

            argumentValueList.add(argumentValue);
            previousDataObject = argumentValue;
        }

        return argumentValueList;
    }
    /**
     * @return Will return void data for non return value functions
     */
    private DataObject interpretFunctionCallNode(DataObject compositeType, FunctionCallNode node) {
        String functionName = node.getFunctionName();
        final String originalFunctionName = functionName;

        if(functionName.startsWith("mp.")) {
            if(compositeType == null || (compositeType.getType() != DataType.OBJECT &&
                    !(compositeType.getType() == DataType.TEXT && compositeType.getText().toString().equals("super"))))
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Method call without object", node.getPos());

            if(compositeType.getType() == DataType.OBJECT && compositeType.getObject().isClass())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Method can not be called on classes", node.getPos());
        }

        boolean isModuleVariable = compositeType == null && functionName.startsWith("[[");
        Map<String, DataObject> variables;
        if(isModuleVariable) {
            int indexModuleIdientifierEnd = functionName.indexOf("]]::");
            if(indexModuleIdientifierEnd == -1) {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid function name", node.getPos());
            }

            String moduleName = functionName.substring(2, indexModuleIdientifierEnd);
            if(!isAlphaNumericWithUnderline(moduleName)) {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid module name", node.getPos());
            }

            functionName = functionName.substring(indexModuleIdientifierEnd + 4);

            LangModule module = modules.get(moduleName);
            if(module == null) {
                return setErrnoErrorObject(InterpretingError.MODULE_LOAD_UNLOAD_ERR, "The module \"" + moduleName + "\" is not loaded!", node.getPos());
            }

            variables = module.getExportedVariables();
        }else {
            variables = getData().var;
        }

        FunctionPointerObject fp;
        if(compositeType != null) {
            if(compositeType.getType() == DataType.STRUCT) {
                return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "\"" + node.getFunctionName() +
                        "\": Function pointer is invalid", node.getPos());
            }else if(compositeType.getType() == DataType.STRUCT) {
                if(!functionName.startsWith("fp."))
                    functionName = "fp." + functionName;

                try {
                    DataObject member = compositeType.getStruct().getMember(functionName);

                    if(member.getType() != DataType.FUNCTION_POINTER)
                        return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "\"" + node.getFunctionName() +
                                "\": Function pointer is invalid", node.getPos());

                    fp = member.getFunctionPointer();
                }catch(DataTypeConstraintException e) {
                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), node.getPos());
                }
            }else if(compositeType.getType() == DataType.TEXT && compositeType.getText().toString().equals("super")) {
                compositeType = getData().var.get("&this");

                if(compositeType == null || compositeType.getType() != DataType.OBJECT)
                    return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "Super can only be used in methods if \"&this\" is present");

                List<DataObject> argumentList = new LinkedList<>(interpretFunctionPointerArguments(node.getChildren()));

                if(functionName.equals("construct"))
                    return callSuperConstructor(compositeType.getObject(), argumentList, node.getPos());

                return callSuperMethod(compositeType.getObject(), functionName, argumentList, node.getPos());
            }else if(compositeType.getType() == DataType.OBJECT) {
                List<DataObject> argumentList = new LinkedList<>(interpretFunctionPointerArguments(node.getChildren()));

                if(functionName.equals("construct"))
                    return callConstructor(compositeType.getObject(), argumentList, node.getPos());

                return callMethod(compositeType.getObject(), functionName, argumentList, node.getPos());
            }else {
                return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Invalid composite type", node.getPos());
            }
        }else if(!isModuleVariable && isFuncName(functionName)) {
            final boolean isLinkerFunction;
            if(functionName.startsWith("func.")) {
                isLinkerFunction = false;

                functionName = functionName.substring(5);
            }else if(functionName.startsWith("fn.")) {
                isLinkerFunction = false;

                functionName = functionName.substring(3);
            }else if(functionName.startsWith("linker.")) {
                isLinkerFunction = true;

                functionName = functionName.substring(7);
            }else if(functionName.startsWith("ln.")) {
                isLinkerFunction = true;

                functionName = functionName.substring(3);
            }else {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid native, predfined, or linker function name", node.getPos());
            }

            final String functionNameCopy = functionName;
            Optional<Map.Entry<String, FunctionPointerObject>> ret = funcs.entrySet().stream().filter(entry -> {
                return entry.getValue().isLinkerFunction() == isLinkerFunction && functionNameCopy.equals(entry.getKey());
            }).findFirst();

            if(!ret.isPresent())
                return setErrnoErrorObject(InterpretingError.FUNCTION_NOT_FOUND, "\"" + node.getFunctionName() +
                        "\": Native, predfined, or linker function was not found", node.getPos());

            fp = ret.get().getValue().withFunctionName(originalFunctionName);
        }else if(isVarNameFuncPtrWithoutPrefix(functionName)) {
            DataObject ret = variables.get(functionName);
            if(ret == null || ret.getType() != DataType.FUNCTION_POINTER)
                return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "\"" + node.getFunctionName() +
                        "\": Function pointer was not found or is invalid", node.getPos());

            fp = ret.getFunctionPointer();
        }else {
            //Function call without prefix

            //Function pointer
            DataObject ret = variables.get("fp." + functionName);
            if(ret != null) {
                if(ret.getType() != DataType.FUNCTION_POINTER)
                    return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "\"" + node.getFunctionName() +
                            "\": Function pointer is invalid", node.getPos());

                fp = ret.getFunctionPointer();
            }else if(!isModuleVariable) {
                //Predefined/External function

                final String functionNameCopy = functionName;
                Optional<Map.Entry<String, FunctionPointerObject>> retPredefinedFunction = funcs.entrySet().stream().filter(entry -> {
                    return !entry.getValue().isLinkerFunction() && functionNameCopy.equals(entry.getKey());
                }).findFirst();

                if(retPredefinedFunction.isPresent()) {
                    fp = retPredefinedFunction.get().getValue().withFunctionName("func." + functionName);
                }else {
                    //Predefined linker function
                    retPredefinedFunction = funcs.entrySet().stream().filter(entry -> {
                        return entry.getValue().isLinkerFunction() && functionNameCopy.equals(entry.getKey());
                    }).findFirst();

                    if(!retPredefinedFunction.isPresent())
                        return setErrnoErrorObject(InterpretingError.FUNCTION_NOT_FOUND, "\"" + node.getFunctionName() +
                                "\": Normal, native, predfined, linker, or external function was not found", node.getPos());

                    fp = retPredefinedFunction.get().getValue().withFunctionName("linker." + functionName);
                }
            }else {
                return setErrnoErrorObject(InterpretingError.FUNCTION_NOT_FOUND, "\"" + node.getFunctionName() +
                        "\": Normal, native, predfined, linker, or external function was not found", node.getPos());
            }
        }

        List<DataObject> argumentList = interpretFunctionPointerArguments(node.getChildren());
        return callFunctionPointer(fp, functionName, argumentList, node.getPos());
    }

    private DataObject interpretFunctionCallPreviousNodeValueNode(FunctionCallPreviousNodeValueNode node, DataObject previousValue) {
        if(previousValue == null)
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Missing previous value for FunctionCallPreviousNodeValueNode",
                    node.getPos());

        DataObject ret = operators.opCall(previousValue, interpretFunctionPointerArguments(node.getChildren()),
                node.getPos());
        if(ret != null)
            return ret;

        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid data type", node.getPos());
    }

    private DataObject interpretFunctionDefinitionNode(FunctionDefinitionNode node) {
        String functionName = node.getFunctionName();
        String functionNameWithoutPrefix = null;
        boolean overloaded = node.isOverloaded();
        boolean combinator = node.isCombinator();
        DataObject functionPointerDataObject = null;
        boolean[] flags = new boolean[] {false, false};
        if(functionName != null) {
            if(functionName.startsWith("$") || functionName.startsWith("&"))
                functionNameWithoutPrefix = functionName.substring(1);

            if(functionName.startsWith("fp."))
                functionNameWithoutPrefix = functionName.substring(3);

            functionPointerDataObject = getOrCreateDataObjectFromVariableName(null, null, functionName,
                    false, false, !overloaded, flags,
                    node.getPos());
            if(flags[0])
                return functionPointerDataObject; //Forward error from getOrCreateDataObjectFromVariableName()

            if(functionPointerDataObject == null || (overloaded && functionPointerDataObject.getType() != DataType.FUNCTION_POINTER)) {
                return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT, "Can not overload variable \"" + functionName +
                        "\" because the variable does not exist or is not of type function pointer", node.getPos());
            }

            if(!overloaded && functionPointerDataObject.getType() != DataType.NULL) {
                return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT, "Can not set \"" + functionName +
                                "\" to function because the variable already exists (You should use \"function overload\" instead of \"function\" to overload a function)",
                        node.getPos());
            }

            if(functionPointerDataObject.getVariableName() == null) {
                return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT, "Anonymous values can not be changed", node.getPos());
            }

            if(functionPointerDataObject.isFinalData() || functionPointerDataObject.isLangVar()) {
                if(flags[1])
                    getData().var.remove(functionPointerDataObject.getVariableName());

                return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, node.getPos());
            }
        }

        String docComment = node.getDocComment();
        Map<String, String> parameterDocComments = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder();
        if(docComment != null) {
            for(String token:docComment.split("\\n")) {
                token = token.trim();
                if(token.startsWith("@param") && token.contains(":") && Character.isWhitespace(token.charAt(6))) {
                    int colonIndex = token.indexOf(':');

                    String name = token.substring(6, colonIndex).trim();
                    String comment = token.substring(colonIndex + 1).trim();
                    parameterDocComments.put(name, comment);

                    continue;
                }

                stringBuilder.append(token).append("\n");
            }

            //Remove trailing "\n"
            if(stringBuilder.length() > 0)
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        String functionDocComment = docComment == null?null:stringBuilder.toString();

        List<Node> children = node.getChildren();

        List<DataObject> parameterList = new ArrayList<>(children.size());
        List<DataObject.DataTypeConstraint> parameterDataTypeConstraintList = new ArrayList<>(children.size());
        List<LangBaseFunction.ParameterAnnotation> parameterAnnotationList = new ArrayList<>(children.size());
        List<String> parameterInfoList = new ArrayList<>(children.size());
        int varArgsParameterIndex = -1;
        boolean textVarArgsParameter = false;
        List<CodePosition> argumentPosList = new ArrayList<>(children.size());

        Iterator<Node> childrenIterator = children.listIterator();
        int index = 0;
        while(childrenIterator.hasNext()) {
            Node child = childrenIterator.next();
            try {
                if(child.getNodeType() != NodeType.VARIABLE_NAME) {
                    if(child.getNodeType() == NodeType.PARSING_ERROR)
                        return interpretNode(null, child);
                    else
                        return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                                "Invalid AST node type for parameter", node.getPos());
                }

                VariableNameNode parameter = (VariableNameNode)child;
                String rawVariableName = parameter.getVariableName();

                String rawParameterTypeConstraint = parameter.getTypeConstraint();
                LangBaseFunction.ParameterAnnotation parameterAnnotation = LangBaseFunction.ParameterAnnotation.NORMAL;
                DataTypeConstraint parameterTypeConstraint;
                if(rawParameterTypeConstraint == null) {
                    parameterTypeConstraint = null;
                }else if(rawParameterTypeConstraint.equals("bool")) {
                    parameterTypeConstraint = null;
                    parameterAnnotation = LangBaseFunction.ParameterAnnotation.BOOLEAN;
                }else if(rawParameterTypeConstraint.equals("number")) {
                    parameterTypeConstraint = null;
                    parameterAnnotation = LangBaseFunction.ParameterAnnotation.NUMBER;
                }else if(rawParameterTypeConstraint.equals("callable")) {
                    parameterTypeConstraint = null;
                    parameterAnnotation = LangBaseFunction.ParameterAnnotation.CALLABLE;
                }else {
                    DataObject errorOut = new DataObject().setVoid();
                    parameterTypeConstraint = interpretTypeConstraint(rawParameterTypeConstraint, errorOut, parameter.getPos());

                    if(errorOut.getType() == DataType.ERROR)
                        return errorOut;
                }

                if(!childrenIterator.hasNext() && !isLangVarWithoutPrefix(rawVariableName) && isFuncCallVarArgs(rawVariableName)) {
                    //Varargs (only the last parameter can be a varargs parameter)

                    varArgsParameterIndex = index;

                    String variableName = rawVariableName.substring(0, rawVariableName.length() - 3); //Remove "..."

                    textVarArgsParameter = variableName.charAt(0) == '$';

                    parameterList.add(new DataObject().setVariableName(variableName));
                    parameterDataTypeConstraintList.add(parameterTypeConstraint == null?DataObject.CONSTRAINT_NORMAL:parameterTypeConstraint);
                    parameterAnnotationList.add(LangBaseFunction.ParameterAnnotation.VAR_ARGS);
                    parameterInfoList.add(parameterDocComments.remove(variableName));
                    argumentPosList.add(node.getPos());

                    continue;
                }

                if(isFuncCallCallByPtr(rawVariableName) && !isFuncCallCallByPtrLangVar(rawVariableName)) {
                    String variableName = "$" + rawVariableName.substring(2, rawVariableName.length() - 1); //Remove '[' and ']' from variable name;

                    parameterList.add(new DataObject().setVariableName(variableName));
                    parameterDataTypeConstraintList.add(parameterTypeConstraint == null?DataObject.getTypeConstraintFor(variableName):parameterTypeConstraint);
                    parameterAnnotationList.add(LangBaseFunction.ParameterAnnotation.CALL_BY_POINTER);
                    parameterInfoList.add(parameterDocComments.remove(variableName));
                    argumentPosList.add(node.getPos());

                    continue;
                }

                if(!isVarNameWithoutPrefix(rawVariableName) || isLangVarWithoutPrefix(rawVariableName))
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE,
                            "Invalid parameter: \"" + rawVariableName + "\"", node.getPos());

                String variableName = rawVariableName;

                parameterList.add(new DataObject().setVariableName(variableName));
                parameterDataTypeConstraintList.add(parameterTypeConstraint == null?DataObject.getTypeConstraintFor(variableName):parameterTypeConstraint);
                parameterAnnotationList.add(parameterAnnotation);
                parameterInfoList.add(parameterDocComments.remove(variableName));
                argumentPosList.add(node.getPos());
            }catch(ClassCastException e) {
                setErrno(InterpretingError.INVALID_AST_NODE, node.getPos());
            }

            index++;
        }

        String rawReturnTypeConstraint = node.getReturnValueTypeConstraint();
        DataTypeConstraint returnValueTypeConstraint;
        if(rawReturnTypeConstraint == null) {
            returnValueTypeConstraint = DataObject.CONSTRAINT_NORMAL;
        }else {
            DataObject errorOut = new DataObject().setVoid();
            returnValueTypeConstraint = interpretTypeConstraint(rawReturnTypeConstraint, errorOut, node.getPos());

            if(errorOut.getType() == DataType.ERROR)
                return errorOut;
        }

        if(!parameterDocComments.isEmpty()) {
            setErrno(InterpretingError.INVALID_DOC_COMMENT, "The following parameters defined in the doc comment do not exist: " +
                    String.join(", ", parameterDocComments.keySet()), node.getPos());
        }

        StackElement currentStackElement = getCurrentCallStackElement();

        LangNormalFunction normalFunction = new LangNormalFunction(currentStackElement.getLangPath(),
                currentStackElement.getLangFile(), parameterList, parameterDataTypeConstraintList,
                parameterAnnotationList, parameterInfoList, varArgsParameterIndex, textVarArgsParameter,
                false, returnValueTypeConstraint, argumentPosList, node.getFunctionBody(),
                combinator, 0, new ArrayList<>(), functionNameWithoutPrefix);

        if(functionPointerDataObject == null)
            return new DataObject().setFunctionPointer(new FunctionPointerObject(normalFunction).
                    withFunctionInfo(functionDocComment).withMappedFunctions(internalFunction ->
                            new FunctionPointerObject.InternalFunction(internalFunction,
                                    getCurrentCallStackElement().langClass, DataObject.Visibility.PUBLIC)));

        try {
            if(overloaded) {
                functionPointerDataObject.setFunctionPointer(functionPointerDataObject.getFunctionPointer().
                        withAddedFunction(new FunctionPointerObject.InternalFunction(
                                new FunctionPointerObject.InternalFunction(normalFunction), getCurrentCallStackElement().langClass,
                                DataObject.Visibility.PUBLIC)));
            }else {
                functionPointerDataObject.setFunctionPointer(new FunctionPointerObject(normalFunction).
                                withFunctionInfo(functionDocComment).withMappedFunctions(internalFunction ->
                                        new FunctionPointerObject.InternalFunction(internalFunction,
                                                getCurrentCallStackElement().langClass, DataObject.Visibility.PUBLIC))).
                        setTypeConstraint(DataTypeConstraint.fromSingleAllowedType(DataType.FUNCTION_POINTER));
            }
        }catch(DataTypeConstraintViolatedException e) {
            if(flags[1])
                getData().var.remove(functionName);

            return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Incompatible type for function definition: \"" +
                    functionName + "\" was already defined and cannot be set to a function definition", node.getPos());
        }

        return functionPointerDataObject;
    }

    private DataObject interpretArrayNode(ArrayNode node) {
        List<DataObject> interpretedNodes = new LinkedList<>();

        for(Node element:node.getChildren()) {
            DataObject argumentValue = interpretNode(null, element);
            if(argumentValue == null)
                continue;
            interpretedNodes.add(new DataObject(argumentValue));
        }

        List<DataObject> elements = LangUtils.combineArgumentsWithoutArgumentSeparators(interpretedNodes,
                this, node.getPos());
        return new DataObject().setArray(elements.toArray(new DataObject[0]));
    }

    private DataObject interpretStructDefinitionNode(StructDefinitionNode node) {
        String structName = node.getStructName();
        DataObject structDataObject = null;
        boolean[] flags = new boolean[] {false, false};
        if(structName != null) {
            structDataObject = getOrCreateDataObjectFromVariableName(null, null, structName,
                    false, false, true, flags, node.getPos());
            if(flags[0])
                return structDataObject; //Forward error from getOrCreateDataObjectFromVariableName()

            if(structDataObject.getVariableName() == null) {
                return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT, "Anonymous values can not be changed", node.getPos());
            }

            if(structDataObject.isFinalData() || structDataObject.isLangVar()) {
                if(flags[1])
                    getData().var.remove(structDataObject.getVariableName());

                return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, node.getPos());
            }
        }

        List<String> memberNames = node.getMemberNames();
        List<String> typeConstraints = node.getTypeConstraints();

        if(memberNames.size() != typeConstraints.size())
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos());

        for(String memberName:memberNames)
            if(!isVarNameWithoutPrefix(memberName))
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "\"" + memberName + "\" is no valid struct member name", node.getPos());

        if(new HashSet<>(memberNames).size() < memberNames.size())
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Struct member name may not be duplicated", node.getPos());

        DataTypeConstraint[] typeConstraintsArray = new DataTypeConstraint[typeConstraints.size()];
        for(int i = 0;i < typeConstraintsArray.length;i++) {
            String typeConstraint = typeConstraints.get(i);
            if(typeConstraint == null)
                continue;

            DataObject errorOut = new DataObject().setVoid();
            typeConstraintsArray[i] = interpretTypeConstraint(typeConstraint, errorOut, node.getPos());

            if(errorOut.getType() == DataType.ERROR)
                return errorOut;
        }

        try {
            StructObject structObject = new StructObject(memberNames.toArray(new String[0]), typeConstraintsArray);

            if(structDataObject == null)
                return new DataObject().setStruct(structObject);

            try {
                structDataObject.setStruct(structObject).
                        setTypeConstraint(DataTypeConstraint.fromSingleAllowedType(DataType.STRUCT)).setFinalData(true);
            }catch(DataTypeConstraintViolatedException e) {
                if(flags[1])
                    getData().var.remove(structName);

                return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Incompatible type for struct definition: \"" +
                        structName + "\" was already defined and cannot be set to a struct definition", node.getPos());
            }

            return structDataObject;
        }catch(DataTypeConstraintException e) {
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, e.getMessage(), node.getPos());
        }
    }

    private DataObject interpretClassDefinitionNode(ClassDefinitionNode node) {
        String className = node.getClassName();

        try {
            //Update call stack
            StackElement currentStackElement = getCurrentCallStackElement();
            pushStackElement(new StackElement(currentStackElement.getLangPath(), currentStackElement.getLangFile(),
                    LangObject.DUMMY_CLASS_DEFINITION_CLASS, className == null?"<class>":className,
                    "<class-definition>", currentStackElement.getModule()), node.getPos());

            DataObject classDataObject = null;
            boolean[] flags = new boolean[] {false, false};
            if(className != null) {
                classDataObject = getOrCreateDataObjectFromVariableName(null, null, className,
                        false, false, true, flags, node.getPos());
                if(flags[0])
                    return classDataObject; //Forward error from getOrCreateDataObjectFromVariableName()

                if(classDataObject.getVariableName() == null) {
                    return setErrnoErrorObject(InterpretingError.INVALID_ASSIGNMENT, "Anonymous values can not be changed", node.getPos());
                }

                if(classDataObject.isFinalData() || classDataObject.isLangVar()) {
                    if(flags[1])
                        getData().var.remove(classDataObject.getVariableName());

                    return setErrnoErrorObject(InterpretingError.FINAL_VAR_CHANGE, node.getPos());
                }
            }

            List<AbstractSyntaxTree.Node> parentClasses = node.getParentClasses();

            List<DataObject> parentClassList = new LinkedList<>(interpretFunctionPointerArguments(parentClasses));
            List<DataObject> combinedParentClassList = LangUtils.combineArgumentsWithoutArgumentSeparators(parentClassList,
                    this, node.getPos());

            List<LangObject> parentClassObjectList = new LinkedList<>();
            for(DataObject parentClass:combinedParentClassList) {
                if(parentClass.getType() != DataType.OBJECT || !parentClass.getObject().isClass())
                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Parent classes must be classes",
                            node.getPos());

                parentClassObjectList.add(parentClass.getObject());
            }

            List<String> staticMemberNames = node.getStaticMemberNames();
            List<String> staticMemberTypeConstraints = node.getStaticMemberTypeConstraints();
            List<Node> staticMemberValues = node.getStaticMemberValues();
            List<Boolean> staticMemberFinalFlag = node.getStaticMemberFinalFlag();
            List<ClassDefinitionNode.Visibility> staticMemberVisibility = node.getStaticMemberVisibility();

            if(staticMemberNames.size() != staticMemberTypeConstraints.size() || staticMemberNames.size() != staticMemberValues.size() ||
                    staticMemberNames.size() != staticMemberFinalFlag.size() || staticMemberNames.size() != staticMemberVisibility.size())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos());

            for(String staticMemberName:staticMemberNames)
                if(!isVarNameWithoutPrefix(staticMemberName))
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "\"" + staticMemberName + "\" is no valid static member name",
                            node.getPos());

            if(new HashSet<>(staticMemberNames).size() < staticMemberNames.size())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Static member name may not be duplicated",
                        node.getPos());

            DataTypeConstraint[] staticMemberTypeConstraintsArray = new DataTypeConstraint[staticMemberTypeConstraints.size()];
            for(int i = 0;i < staticMemberTypeConstraintsArray.length;i++) {
                String typeConstraint = staticMemberTypeConstraints.get(i);
                if(typeConstraint == null)
                    continue;

                DataObject errorOut = new DataObject().setVoid();
                staticMemberTypeConstraintsArray[i] = interpretTypeConstraint(typeConstraint, errorOut, node.getPos());

                if(errorOut.getType() == DataType.ERROR)
                    return errorOut;
            }

            boolean[] staticMemberFinalFlagArray = new boolean[staticMemberFinalFlag.size()];
            for(int i = 0;i < staticMemberFinalFlagArray.length;i++) {
                if(staticMemberFinalFlag.get(i) == null)
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Null value in final flag for static member at index " + i,
                            node.getPos());

                staticMemberFinalFlagArray[i] = staticMemberFinalFlag.get(i);
            }

            DataObject[] staticMembers = new DataObject[staticMemberNames.size()];
            try {
                for(int i = 0;i < staticMembers.length;i++) {
                    DataObject value = staticMemberValues.get(i) == null?new DataObject().setNull():interpretNode(null, staticMemberValues.get(i));
                    if(value == null)
                        value = new DataObject().setVoid();
                    staticMembers[i] = new DataObject(value).setVariableName(staticMemberNames.get(i));

                    if(staticMemberTypeConstraintsArray[i] != null)
                        staticMembers[i].setTypeConstraint(staticMemberTypeConstraintsArray[i]);

                    if(staticMemberFinalFlagArray[i])
                        staticMembers[i].setFinalData(true);

                    staticMembers[i].setMemberVisibility(DataObject.Visibility.fromASTNode(staticMemberVisibility.get(i)));
                }
            }catch(DataTypeConstraintException e) {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, e.getMessage(), node.getPos());
            }

            List<String> memberNames = node.getMemberNames();
            List<String> memberTypeConstraints = node.getMemberTypeConstraints();
            List<Boolean> memberFinalFlag = node.getMemberFinalFlag();
            List<ClassDefinitionNode.Visibility> memberVisibility = node.getMemberVisibility();

            if(memberNames.size() != memberTypeConstraints.size() || memberNames.size() != memberFinalFlag.size() ||
                    memberNames.size() != memberVisibility.size())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos());

            for(String memberName:memberNames)
                if(!isVarNameWithoutPrefix(memberName))
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "\"" + memberName + "\" is no valid member name",
                            node.getPos());

            if(new HashSet<>(memberNames).size() < memberNames.size())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Member name may not be duplicated",
                        node.getPos());

            DataTypeConstraint[] memberTypeConstraintsArray = new DataTypeConstraint[memberTypeConstraints.size()];
            for(int i = 0;i < memberTypeConstraintsArray.length;i++) {
                String typeConstraint = memberTypeConstraints.get(i);
                if(typeConstraint == null)
                    continue;

                DataObject errorOut = new DataObject().setVoid();
                memberTypeConstraintsArray[i] = interpretTypeConstraint(typeConstraint, errorOut, node.getPos());

                if(errorOut.getType() == DataType.ERROR)
                    return errorOut;
            }

            boolean[] memberFinalFlagArray = new boolean[memberFinalFlag.size()];
            for(int i = 0;i < memberFinalFlagArray.length;i++) {
                if(memberFinalFlag.get(i) == null)
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Null value in final flag for member at index " + i,
                            node.getPos());

                memberFinalFlagArray[i] = memberFinalFlag.get(i);
            }

            DataObject.Visibility[] memberVisibilityArray = new DataObject.Visibility[memberVisibility.size()];
            for(int i = 0;i < memberVisibility.size();i++)
                memberVisibilityArray[i] = DataObject.Visibility.fromASTNode(memberVisibility.get(i));

            List<String> methodNames = node.getMethodNames();
            List<Node> methodDefinitions = node.getMethodDefinitions();
            List<Boolean> methodOverrideFlag = node.getMethodOverrideFlag();
            List<ClassDefinitionNode.Visibility> methodVisibility = node.getMethodVisibility();

            if(methodNames.size() != methodDefinitions.size() || methodNames.size() != methodOverrideFlag.size() ||
                    methodNames.size() != methodVisibility.size())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos());

            for(String methodName:methodNames)
                if(!isMethodName(methodName) && !isOperatorMethodName(methodName) && !isConversionMethodName(methodName))
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "\"" + methodName + "\" is no valid method name",
                            node.getPos());

            Map<String, FunctionPointerObject> methods = new HashMap<>();
            Map<String, List<Boolean>> rawMethodOverrideFlags = new HashMap<>();
            Map<String, List<DataObject.Visibility>> methodVisibilities = new HashMap<>();
            for(int i = 0;i < methodNames.size();i++) {
                if(methodOverrideFlag.get(i) == null)
                    return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Null value in override flag for method at index " + i,
                            node.getPos());

                String methodName = methodNames.get(i);

                DataObject methodDefinition = interpretNode(null, methodDefinitions.get(i));
                if(methodDefinition == null || methodDefinition.getType() != DataType.FUNCTION_POINTER)
                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Methods must be of type \"" + DataType.FUNCTION_POINTER + "\"",
                            methodDefinitions.get(i).getPos());

                if(methods.containsKey(methodName)) {
                    FunctionPointerObject functions = methods.get(methodName);
                    methods.put(methodName, functions.withAddedFunctions(methodDefinition.getFunctionPointer()));
                }else {
                    methods.put(methodName, methodDefinition.getFunctionPointer());
                    rawMethodOverrideFlags.put(methodName, new LinkedList<>());
                    methodVisibilities.put(methodName, new LinkedList<>());
                }

                List<Boolean> methodOverrideFlags = rawMethodOverrideFlags.get(methodName);
                for(int j = 0;j < methodDefinition.getFunctionPointer().getOverloadedFunctionCount();j++)
                    methodOverrideFlags.add(methodOverrideFlag.get(i));

                List<DataObject.Visibility> methodVisibilityList = methodVisibilities.get(methodName);
                for(int j = 0;j < methodDefinition.getFunctionPointer().getOverloadedFunctionCount();j++)
                    methodVisibilityList.add(DataObject.Visibility.fromASTNode(methodVisibility.get(i)));
            }

            Map<String, Boolean[]> methodOverrideFlags = new HashMap<>();
            rawMethodOverrideFlags.forEach((k, v) -> methodOverrideFlags.put(k, v.toArray(new Boolean[0])));

            List<Node> constructorDefinitions = node.getConstructorDefinitions();
            List<ClassDefinitionNode.Visibility> constructorVisibility = node.getConstructorVisibility();

            if(constructorDefinitions.size() != constructorVisibility.size())
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, node.getPos());

            FunctionPointerObject constructors = null;
            List<DataObject.Visibility> constructorVisibilities = new LinkedList<>();
            for(int i = 0;i < constructorDefinitions.size();i++) {
                DataObject constructorDefinition = interpretNode(null, constructorDefinitions.get(i));
                if(constructorDefinition == null || constructorDefinition.getType() != DataType.FUNCTION_POINTER)
                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Constructor must be of type \"" + DataType.FUNCTION_POINTER + "\"",
                            constructorDefinitions.get(i).getPos());

                if(constructors == null)
                    constructors = constructorDefinition.getFunctionPointer();
                else
                    constructors = constructors.withAddedFunctions(constructorDefinition.getFunctionPointer());

                for(int j = 0;j < constructorDefinition.getFunctionPointer().getOverloadedFunctionCount();j++)
                    constructorVisibilities.add(DataObject.Visibility.fromASTNode(constructorVisibility.get(i)));
            }

            //Set default constructor
            if(constructorDefinitions.isEmpty()) {
                constructors = LangNativeFunction.getSingleLangFunctionFromObject(new Object() {
                    @LangFunction(value="construct", isMethod=true)
                    @LangFunction.AllowedTypes(DataType.VOID)
                    @SuppressWarnings("unused")
                    public DataObject defaultConstructMethod(
                            LangInterpreter interpreter, LangObject thisObject
                    ) {
                        return null;
                    }
                });
                constructorVisibilities.add(DataObject.Visibility.PUBLIC);
            }

            try {
                LangObject classObject = new LangObject(className, staticMembers, memberNames.toArray(new String[0]),
                        memberTypeConstraintsArray, memberFinalFlagArray, memberVisibilityArray,
                        methods, methodOverrideFlags, methodVisibilities, constructors, constructorVisibilities,
                        parentClassObjectList.toArray(new LangObject[0]));

                if(classDataObject == null)
                    return new DataObject().setObject(classObject);

                try {
                    classDataObject.setObject(classObject).
                            setTypeConstraint(DataTypeConstraint.fromSingleAllowedType(DataType.OBJECT)).setFinalData(true);
                }catch(DataTypeConstraintViolatedException e) {
                    if(flags[1])
                        getData().var.remove(className);

                    return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, "Incompatible type for class definition: \"" +
                            className + "\" was already defined and cannot be set to a class", node.getPos());
                }

                return classDataObject;
            }catch(DataTypeConstraintException e) {
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, e.getMessage(), node.getPos());
            }
        }finally {
            //Update call stack
            popStackElement();
        }
    }

    private DataTypeConstraint interpretTypeConstraint(String typeConstraint, DataObject errorOut, CodePosition pos) {
        if(typeConstraint.isEmpty())
            errorOut.setData(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Empty type constraint is not allowed", pos));

        boolean nullable = typeConstraint.charAt(0) == '?';
        boolean inverted = typeConstraint.charAt(0) == '!';
        List<DataType> typeValues = new LinkedList<>();

        if(nullable || inverted)
            typeConstraint = typeConstraint.substring(1);

        int pipeIndex;
        do {
            pipeIndex = typeConstraint.indexOf('|');

            String type = pipeIndex > -1?typeConstraint.substring(0, pipeIndex):typeConstraint;

            if(type.isEmpty() || pipeIndex == typeConstraint.length() - 1)
                errorOut.setData(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Empty type constraint is not allowed", pos));

            typeConstraint = pipeIndex > -1?typeConstraint.substring(pipeIndex + 1):"";

            try {
                DataType typeValue = DataType.valueOf(type);
                typeValues.add(typeValue);
            }catch(IllegalArgumentException e) {
                errorOut.setData(setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid type: \"" + type + "\"", pos));
            }
        }while(pipeIndex > -1);

        if(nullable)
            typeValues.add(DataType.NULL);

        if(inverted)
            return DataTypeConstraint.fromNotAllowedTypes(typeValues);
        else
            return DataTypeConstraint.fromAllowedTypes(typeValues);
    }

    //Return values for format sequence errors
    private static final int FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE = -1;
    private static final int FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS = -2;
    private static final int FORMAT_SEQUENCE_ERROR_INVALID_ARG_COUNT = -3;
    private static final int FORMAT_SEQUENCE_ERROR_TRANSLATION_KEY_NOT_FOUND = -4;
    private static final int FORMAT_SEQUENCE_ERROR_SPECIFIED_INDEX_OUT_OF_BOUNDS = -5;
    private static final int FORMAT_SEQUENCE_ERROR_TRANSLATION_INVALID_PLURALIZATION_TEMPLATE = -6;

    /**
     * @param argumentList The argument list without argument separators of the function call without the format argument (= argument at index 0). Used data objects will be removed from the list
     * @param fullArgumentList The argument list of the function call where every argument are already combined to single values without argument separators with the format argument
     * (= argument at index 0). This list will not be modified and is used for value referencing by index
     *
     * @return The count of chars used for the format sequence
     * Will return any of
     * <ul>
     * <li>{@code FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE}</li>
     * <li>{@code FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS}</li>
     * <li>{@code FORMAT_SEQUENCE_ERROR_TRANSLATION_KEY_NOT_FOUND}</li>
     * <li>{@code FORMAT_SEQUENCE_ERROR_SPECIFIED_INDEX_OUT_OF_BOUNDS}</li>
     * </ul>
     * for errors
     */
    private int interpretNextFormatSequence(String format, StringBuilder builder, List<DataObject> argumentList, List<DataObject> fullArgumentList) {
        char[] posibleFormats = {'b', 'c', 'd', 'f', 'n', 'o', 's', 't', 'x', '?'};
        int[] indices = new int[posibleFormats.length];
        for(int i = 0;i < posibleFormats.length;i++)
            indices[i] = format.indexOf(posibleFormats[i]);

        int minEndIndex = Integer.MAX_VALUE;
        for(int index:indices) {
            if(index == -1)
                continue;

            if(index < minEndIndex)
                minEndIndex = index;
        }

        if(minEndIndex == Integer.MAX_VALUE)
            return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

        String fullFormat = format.substring(0, minEndIndex + 1);
        char formatType = fullFormat.charAt(fullFormat.length() - 1);

        //Parsing format arguments
        Integer valueSpecifiedIndex = null;
        if(fullFormat.charAt(0) == '[') {
            int valueSpecifiedIndexEndIndex = fullFormat.indexOf(']');
            if(valueSpecifiedIndexEndIndex < 0)
                return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

            String valueSpecifiedIndexString = fullFormat.substring(1, valueSpecifiedIndexEndIndex);
            fullFormat = fullFormat.substring(valueSpecifiedIndexEndIndex + 1);

            String number = "";
            while(!valueSpecifiedIndexString.isEmpty()) {
                if(valueSpecifiedIndexString.charAt(0) < '0' || valueSpecifiedIndexString.charAt(0) > '9')
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                number += valueSpecifiedIndexString.charAt(0);
                valueSpecifiedIndexString = valueSpecifiedIndexString.substring(1);
            }
            valueSpecifiedIndex = Integer.parseInt(number);
            if(valueSpecifiedIndex >= fullArgumentList.size())
                return FORMAT_SEQUENCE_ERROR_SPECIFIED_INDEX_OUT_OF_BOUNDS;
        }
        boolean leftJustify = fullFormat.charAt(0) == '-';
        if(leftJustify)
            fullFormat = fullFormat.substring(1);
        boolean forceSign = fullFormat.charAt(0) == '+';
        if(forceSign)
            fullFormat = fullFormat.substring(1);
        boolean signSpace = !forceSign && fullFormat.charAt(0) == ' ';
        if(signSpace)
            fullFormat = fullFormat.substring(1);
        boolean leadingZeros = fullFormat.charAt(0) == '0';
        if(leadingZeros)
            fullFormat = fullFormat.substring(1);
        boolean sizeInArgument = fullFormat.charAt(0) == '*';
        if(sizeInArgument)
            fullFormat = fullFormat.substring(1);
        Integer sizeArgumentIndex = null;
        if(sizeInArgument && fullFormat.charAt(0) == '[') {
            int sizeArgumentIndexEndIndex = fullFormat.indexOf(']');
            if(sizeArgumentIndexEndIndex < 0)
                return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

            String sizeArgumentIndexString = fullFormat.substring(1, sizeArgumentIndexEndIndex);
            fullFormat = fullFormat.substring(sizeArgumentIndexEndIndex + 1);

            String number = "";
            while(!sizeArgumentIndexString.isEmpty()) {
                if(sizeArgumentIndexString.charAt(0) < '0' || sizeArgumentIndexString.charAt(0) > '9')
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                number += sizeArgumentIndexString.charAt(0);
                sizeArgumentIndexString = sizeArgumentIndexString.substring(1);
            }
            sizeArgumentIndex = Integer.parseInt(number);
            if(sizeArgumentIndex >= fullArgumentList.size())
                return FORMAT_SEQUENCE_ERROR_SPECIFIED_INDEX_OUT_OF_BOUNDS;
        }
        Integer size = null;
        if(fullFormat.charAt(0) > '0' && fullFormat.charAt(0) <= '9') {
            String number = "";
            while(fullFormat.charAt(0) >= '0' && fullFormat.charAt(0) <= '9') {
                number += fullFormat.charAt(0);
                fullFormat = fullFormat.substring(1);
            }
            size = Integer.parseInt(number);
        }
        boolean decimalPlaces = fullFormat.charAt(0) == '.';
        boolean decimalPlacesInArgument = false;
        Integer decimalPlacesCountIndex = null;
        Integer decimalPlacesCount = null;
        if(decimalPlaces) {
            fullFormat = fullFormat.substring(1);
            decimalPlacesInArgument = fullFormat.charAt(0) == '*';
            if(decimalPlacesInArgument)
                fullFormat = fullFormat.substring(1);
            if(decimalPlacesInArgument && fullFormat.charAt(0) == '[') {
                int decimalPlacesCountIndexEndIndex = fullFormat.indexOf(']');
                if(decimalPlacesCountIndexEndIndex < 0)
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                String decimalPlacesCountIndexString = fullFormat.substring(1, decimalPlacesCountIndexEndIndex);
                fullFormat = fullFormat.substring(decimalPlacesCountIndexEndIndex + 1);

                String number = "";
                while(!decimalPlacesCountIndexString.isEmpty()) {
                    if(decimalPlacesCountIndexString.charAt(0) < '0' || decimalPlacesCountIndexString.charAt(0) > '9')
                        return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                    number += decimalPlacesCountIndexString.charAt(0);
                    decimalPlacesCountIndexString = decimalPlacesCountIndexString.substring(1);
                }
                decimalPlacesCountIndex = Integer.parseInt(number);
                if(decimalPlacesCountIndex >= fullArgumentList.size())
                    return FORMAT_SEQUENCE_ERROR_SPECIFIED_INDEX_OUT_OF_BOUNDS;
            }
            if(fullFormat.charAt(0) >= '0' && fullFormat.charAt(0) <= '9') {
                String number = "";
                while(fullFormat.charAt(0) >= '0' && fullFormat.charAt(0) <= '9') {
                    number += fullFormat.charAt(0);
                    fullFormat = fullFormat.substring(1);
                }
                boolean leadingZero = number.charAt(0) == '0';
                if(leadingZero && number.length() > 1)
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                decimalPlacesCount = Integer.parseInt(number);
            }
        }

        if(fullFormat.charAt(0) != formatType)
            return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE; //Invalid characters
        if((sizeInArgument && size != null) || (decimalPlacesInArgument && decimalPlacesCount != null) || (leftJustify && leadingZeros))
            return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE; //Invalid format argument combinations
        if(leftJustify && (!sizeInArgument && size == null))
            return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE; //Missing size format argument for leftJustify
        switch(formatType) { //Invalid arguments for formatType
            case 'f':
                break;

            case 'n':
                if(valueSpecifiedIndex != null || sizeInArgument || size != null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                //Fall-through
            case 'c':
            case '?':
                if(forceSign || signSpace || leadingZeros)
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;

                //Fall-through
            case 'b':
            case 'd':
            case 'o':
            case 'x':
                if(decimalPlaces)
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;
                break;

            case 's':
            case 't':
                if(forceSign || signSpace || leadingZeros)
                    return FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE;
        }

        //Get size from arguments
        if(sizeInArgument) {
            if(sizeArgumentIndex == null && argumentList.isEmpty())
                return FORMAT_SEQUENCE_ERROR_INVALID_ARG_COUNT;
            DataObject dataObject = sizeArgumentIndex == null?argumentList.remove(0):fullArgumentList.get(sizeArgumentIndex);
            Number number = conversions.toNumber(dataObject, CodePosition.EMPTY);
            if(number == null)
                return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

            size = number.intValue();
            if(size < 0)
                return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;
        }
        if(decimalPlacesInArgument) {
            if(decimalPlacesCountIndex == null && argumentList.isEmpty())
                return FORMAT_SEQUENCE_ERROR_INVALID_ARG_COUNT;
            DataObject dataObject = decimalPlacesCountIndex == null?argumentList.remove(0):fullArgumentList.get(decimalPlacesCountIndex);
            Number number = conversions.toNumber(dataObject, CodePosition.EMPTY);
            if(number == null)
                return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

            decimalPlacesCount = number.intValue();
            if(decimalPlacesCount < 0)
                return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;
        }

        //Format argument
        String output = null;
        if(formatType != 'n' && valueSpecifiedIndex == null && argumentList.isEmpty())
            return FORMAT_SEQUENCE_ERROR_INVALID_ARG_COUNT;
        DataObject dataObject = formatType == 'n'?null:(valueSpecifiedIndex == null?argumentList.remove(0):fullArgumentList.get(valueSpecifiedIndex));
        switch(formatType) {
            case 'd':
                Number number = conversions.toNumber(dataObject, CodePosition.EMPTY);
                if(number == null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

                output = number.longValue() + "";
                if(forceSign && output.charAt(0) != '-')
                    output = "+" + output;

                if(signSpace && output.charAt(0) != '+' && output.charAt(0) != '-')
                    output = " " + output;

                break;

            case 'b':
                number = conversions.toNumber(dataObject, CodePosition.EMPTY);
                if(number == null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

                output = Long.toString(number.longValue(), 2).toUpperCase(Locale.ENGLISH);
                if(forceSign && output.charAt(0) != '-')
                    output = "+" + output;

                if(signSpace && output.charAt(0) != '+' && output.charAt(0) != '-')
                    output = " " + output;

                break;

            case 'o':
                number = conversions.toNumber(dataObject, CodePosition.EMPTY);
                if(number == null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

                output = Long.toString(number.longValue(), 8).toUpperCase(Locale.ENGLISH);
                if(forceSign && output.charAt(0) != '-')
                    output = "+" + output;

                if(signSpace && output.charAt(0) != '+' && output.charAt(0) != '-')
                    output = " " + output;

                break;

            case 'x':
                number = conversions.toNumber(dataObject, CodePosition.EMPTY);
                if(number == null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

                output = Long.toString(number.longValue(), 16).toUpperCase(Locale.ENGLISH);
                if(forceSign && output.charAt(0) != '-')
                    output = "+" + output;

                if(signSpace && output.charAt(0) != '+' && output.charAt(0) != '-')
                    output = " " + output;

                break;

            case 'f':
                number = conversions.toNumber(dataObject, CodePosition.EMPTY);
                if(number == null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

                double value = number.doubleValue();
                if(Double.isNaN(value)) {
                    output = "NaN";
                    forceSign = false;
                    leadingZeros = false;
                }else if(Double.isInfinite(value)) {
                    output = (value == Double.NEGATIVE_INFINITY?"-":"") + "Infinity";
                    leadingZeros = false;
                    if(forceSign && output.charAt(0) != '-')
                        output = "+" + output;

                    if(signSpace && output.charAt(0) != '+' && output.charAt(0) != '-')
                        output = " " + output;
                }else {
                    output = String.format(Locale.ENGLISH, "%" + (decimalPlacesCount == null?"":("." + decimalPlacesCount)) + "f", value);
                    if(forceSign && output.charAt(0) != '-')
                        output = "+" + output;

                    if(signSpace && output.charAt(0) != '+' && output.charAt(0) != '-')
                        output = " " + output;
                }

                break;

            case 'c':
                number = conversions.toNumber(dataObject, CodePosition.EMPTY);
                if(number == null)
                    return FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS;

                output = new String(Character.toChars(number.intValue()));

                break;

            case 's':
                output = conversions.toText(dataObject, CodePosition.EMPTY).toString();

                if(decimalPlacesCount != null) {
                    try {
                        output = LangUtils.formatTranslationTemplatePluralization(output, decimalPlacesCount.intValue());
                    }catch(NumberFormatException|InvalidTranslationTemplateSyntaxException e) {
                        return FORMAT_SEQUENCE_ERROR_TRANSLATION_INVALID_PLURALIZATION_TEMPLATE;
                    }
                }

                break;

            case 't':
                String translationKey = conversions.toText(dataObject, CodePosition.EMPTY).toString();

                output = getData().lang.get(translationKey);
                if(output == null)
                    return FORMAT_SEQUENCE_ERROR_TRANSLATION_KEY_NOT_FOUND;

                if(decimalPlacesCount != null) {
                    try {
                        output = LangUtils.formatTranslationTemplatePluralization(output, decimalPlacesCount.intValue());
                    }catch(NumberFormatException|InvalidTranslationTemplateSyntaxException e) {
                        return FORMAT_SEQUENCE_ERROR_TRANSLATION_INVALID_PLURALIZATION_TEMPLATE;
                    }
                }

                break;

            case '?':
                output = conversions.toBool(dataObject, CodePosition.EMPTY)?"true":"false";

                break;

            case 'n':
                output = System.lineSeparator();

                break;
        }

        if(output != null) {
            if(size == null) {
                builder.append(output);
            }else {
                if(leftJustify) {
                    while(output.length() < size)
                        output = output + " ";
                }else if(leadingZeros) {
                    char signOutput = 0;
                    if(output.charAt(0) == '+' || output.charAt(0) == '-' || output.charAt(0) == ' ') {
                        signOutput = output.charAt(0);
                        output = output.substring(1);
                    }

                    int paddingSize = size - (signOutput == 0?0:1);
                    while(output.length() < paddingSize)
                        output = "0" + output;

                    if(signOutput != 0)
                        output = signOutput + output;
                }else {
                    while(output.length() < size)
                        output = " " + output;
                }

                builder.append(output);
            }
        }

        return minEndIndex + 1;
    }
    /**
     * @param argumentList The argument list without argument separators of the function call. Used data objects will be removed from the list
     *
     * @return The formated text as TextObject or an ErrorObject if an error occurred
     */
    DataObject formatText(String format, List<DataObject> argumentList) {
        StringBuilder builder = new StringBuilder();
        List<DataObject> fullArgumentList = new LinkedList<>(argumentList);
        fullArgumentList.add(0, new DataObject(format));

        int i = 0;
        while(i < format.length()) {
            char c = format.charAt(i);
            if(c == '%') {
                if(++i == format.length())
                    return setErrnoErrorObject(InterpretingError.INVALID_FORMAT);

                c = format.charAt(i);
                if(c == '%') {
                    builder.append(c);

                    i++;
                    continue;
                }

                int charCountUsed = interpretNextFormatSequence(format.substring(i), builder, argumentList, fullArgumentList);
                if(charCountUsed < 0) {
                    switch(charCountUsed) {
                        case FORMAT_SEQUENCE_ERROR_INVALID_FORMAT_SEQUENCE:
                            return setErrnoErrorObject(InterpretingError.INVALID_FORMAT);
                        case FORMAT_SEQUENCE_ERROR_INVALID_ARGUMENTS:
                            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS);
                        case FORMAT_SEQUENCE_ERROR_INVALID_ARG_COUNT:
                            return setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT);
                        case FORMAT_SEQUENCE_ERROR_TRANSLATION_KEY_NOT_FOUND:
                            return setErrnoErrorObject(InterpretingError.TRANS_KEY_NOT_FOUND);
                        case FORMAT_SEQUENCE_ERROR_SPECIFIED_INDEX_OUT_OF_BOUNDS:
                            return setErrnoErrorObject(InterpretingError.INDEX_OUT_OF_BOUNDS);
                        case FORMAT_SEQUENCE_ERROR_TRANSLATION_INVALID_PLURALIZATION_TEMPLATE:
                            return setErrnoErrorObject(InterpretingError.INVALID_TEMPLATE_SYNTAX);
                    }
                }

                i += charCountUsed;

                continue;
            }

            builder.append(c);

            i++;
        }

        return new DataObject(builder.toString());
    }

    public DataObject callConstructor(LangObject langObject, List<DataObject> argumentList, CodePosition pos) {
        if(langObject.isClass()) {
            DataObject createdObject = new DataObject().setObject(new LangObject(langObject));

            FunctionPointerObject constructors = createdObject.getObject().getConstructors();

            DataObject ret = callFunctionPointer(constructors, constructors.getFunctionName(), argumentList, pos);
            if(ret == null)
                ret = new DataObject().setVoid();

            if(ret.getType() != DataType.VOID)
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid constructor implementation: VOID must be returned",
                        pos);

            try {
                createdObject.getObject().postConstructor();
            }catch(DataTypeConstraintException e) {
                return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE,
                        "Invalid constructor implementation (Some members have invalid types): " + e.getMessage(),
                        pos);
            }

            return createdObject;
        }else {
            if(langObject.isInitialized())
                return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Object is already initialized",
                        pos);

            //Current constructors for super level instead of normal constructors, because constructors are not overridden
            FunctionPointerObject constructors = langObject.getConstructorsForCurrentSuperLevel();

            DataObject ret = callFunctionPointer(constructors, constructors.getFunctionName(), argumentList, pos);
            if(ret == null)
                ret = new DataObject().setVoid();

            if(ret.getType() != DataType.VOID)
                return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid constructor implementation: VOID must be returned",
                        pos);

            return ret;
        }
    }
    public DataObject callConstructor(LangObject langObject, List<DataObject> argumentList) {
        return callConstructor(langObject, argumentList, CodePosition.EMPTY);
    }

    public DataObject callMethod(LangObject langObject, String rawMethodName, List<DataObject> argumentList, CodePosition pos) {
        if(rawMethodName.startsWith("fn.") || rawMethodName.startsWith("func.") ||
                rawMethodName.startsWith("ln.") || rawMethodName.startsWith("linker."))
            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                    "The method \"" + rawMethodName + "\" is not part of this object", pos);

        String methodName = (langObject.isClass() || rawMethodName.startsWith("fp."))?
                null:((rawMethodName.startsWith("mp.") || rawMethodName.startsWith("op:") ||
                rawMethodName.startsWith("to:")?"":"mp.") + rawMethodName);

        FunctionPointerObject fp;
        try {
            FunctionPointerObject methods = methodName == null?null:langObject.getMethods().get(methodName);
            if(methods == null) {
                if(rawMethodName.startsWith("mp."))
                    return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                            "The method \"" + rawMethodName + "\" is not part of this object",
                            pos);

                if(!rawMethodName.startsWith("fp.") && !rawMethodName.startsWith("op:") && !rawMethodName.startsWith("to:"))
                    rawMethodName = "fp." + rawMethodName;

                DataObject member;
                try {
                    member = langObject.getStaticMember(rawMethodName);
                }catch(DataTypeConstraintException e) {
                    if(langObject.isClass())
                        return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);

                    member = langObject.getMember(rawMethodName);
                }

                if(!member.isAccessible(getCurrentCallStackElement().getLangClass()))
                    return setErrnoErrorObject(InterpretingError.MEMBER_NOT_ACCESSIBLE, "For member \"" + rawMethodName + "\"");

                if(member.getType() != DataType.FUNCTION_POINTER)
                    return setErrnoErrorObject(InterpretingError.INVALID_FUNC_PTR, "\"" + rawMethodName +
                            "\": Function pointer is invalid", pos);

                fp = member.getFunctionPointer();
            }else {
                fp = methods;
            }
        }catch(DataTypeConstraintException e) {
            return setErrnoErrorObject(InterpretingError.INCOMPATIBLE_DATA_TYPE, e.getMessage(), pos);
        }

        return callFunctionPointer(fp, rawMethodName, argumentList, pos);
    }
    public DataObject callMethod(LangObject langObject, String rawMethodName, List<DataObject> argumentList) {
        return callMethod(langObject, rawMethodName, argumentList, CodePosition.EMPTY);
    }

    public DataObject callSuperConstructor(LangObject langObject, List<DataObject> argumentList, CodePosition pos) {
        if(langObject.isClass())
            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Super constructor can not be called on class", pos);

        if(langObject.isInitialized())
            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS, "Object is already initialized", pos);

        FunctionPointerObject superConstructors = langObject.getSuperConstructors();

        //Bind "&this" on super constructor
        superConstructors = new FunctionPointerObject(superConstructors, langObject).withMappedFunctions(internalFunction ->
                new FunctionPointerObject.InternalFunction(internalFunction, internalFunction.getSuperLevel() + langObject.getSuperLevel() + 1));

        DataObject ret = callFunctionPointer(superConstructors, superConstructors.getFunctionName(), argumentList, pos);
        if(ret == null)
            ret = new DataObject().setVoid();

        if(ret.getType() != DataType.VOID)
            return setErrnoErrorObject(InterpretingError.INVALID_AST_NODE, "Invalid constructor implementation: VOID must be returned");

        return ret;
    }
    public DataObject callSuperConstructor(LangObject langObject, List<DataObject> argumentList) {
        return callSuperConstructor(langObject, argumentList, CodePosition.EMPTY);
    }

    public DataObject callSuperMethod(LangObject langObject, String rawMethodName, List<DataObject> argumentList, CodePosition pos) {
        if(rawMethodName.startsWith("fp.") || rawMethodName.startsWith("fn.") ||
                rawMethodName.startsWith("func.") || rawMethodName.startsWith("ln.") || rawMethodName.startsWith("linker."))
            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                    "The method \"" + rawMethodName + "\" is not part of this object", pos);

        String methodName = rawMethodName.startsWith("mp.") || rawMethodName.startsWith("op:") || rawMethodName.startsWith("to:")?
                rawMethodName:("mp." + rawMethodName);

        FunctionPointerObject methods = langObject.getSuperMethods().get(methodName);
        if(methods == null)
            return setErrnoErrorObject(InterpretingError.INVALID_ARGUMENTS,
                    "The method \"" + rawMethodName + "\" is not in any super class of this object",
                    pos);

        FunctionPointerObject fp = methods;

        //Bind "&this" on super method
        fp = new FunctionPointerObject(fp, langObject).withMappedFunctions(internalFunction ->
                new FunctionPointerObject.InternalFunction(internalFunction, internalFunction.getSuperLevel() + langObject.getSuperLevel() + 1));

        return callFunctionPointer(fp, rawMethodName, argumentList, pos);
    }
    public DataObject callSuperMethod(LangObject langObject, String rawMethodName, List<DataObject> argumentList) {
        return callSuperMethod(langObject, rawMethodName, argumentList, CodePosition.EMPTY);
    }

    /**
     * LangPatterns: Regex: \w+
     */
    private boolean isAlphaNumericWithUnderline(String token) {
        if(token.isEmpty())
            return false;

        for(int i = 0;i < token.length();i++) {
            char c = token.charAt(i);
            if(!((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_'))
                return false;
        }

        return true;
    }

    /**
     * LangPatterns: LANG_VAR ((\$|&)LANG_.*)
     */
    private boolean isLangVarWithoutPrefix(String token) {
        char firstChar = token.charAt(0);
        return (firstChar == '$' || firstChar == '&') && token.startsWith("LANG_",  1);
    }

    /**
     * LangPatterns: LANG_VAR ((\$|&)LANG_.*) || LANG_VAR_POINTER_REDIRECTION (\$\[+LANG_.*\]+)
     */
    private boolean isLangVarOrLangVarPointerRedirectionWithoutPrefix(String token) {
        char firstChar = token.charAt(0);
        return (firstChar == '$' || firstChar == '&') && (token.startsWith("LANG_",  1) || token.contains("[LANG_"));
    }

    /**
     * LangPatterns: FUNC_CALL_VAR_ARGS ((\$|&)\w+\.\.\.)
     */
    private boolean isFuncCallVarArgs(String token) {
        char firstChar = token.charAt(0);
        if(!((firstChar == '$' || firstChar == '&') && token.endsWith("...")))
            return false;

        boolean hasVarName = false;
        for(int i = 1;i < token.length() - 3;i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: FUNC_CALL_CALL_BY_PTR (\$\[\w+\])
     */
    private boolean isFuncCallCallByPtr(String token) {
        if(!(token.startsWith("$[") && token.endsWith("]")))
            return false;

        boolean hasVarName = false;
        for(int i = 2;i < token.length() - 1;i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: FUNC_CALL_CALL_BY_PTR_LANG_VAR (\$\[LANG_.*\])
     */
    private boolean isFuncCallCallByPtrLangVar(String token) {
        return token.startsWith("$[LANG_") && token.endsWith("]");
    }

    /**
     * LangPatterns: VAR_NAME_WITHOUT_PREFIX ((\$|&|fp\.)\w+)
     */
    private boolean isVarNameWithoutPrefix(String token) {
        boolean funcPtr = token.startsWith("fp.");

        char firstChar = token.charAt(0);
        if(!(funcPtr || firstChar == '$' || firstChar == '&'))
            return false;

        boolean hasVarName = false;
        for(int i = funcPtr?3:1;i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: METHOD_NAME (mp\.\w+)
     */
    private boolean isMethodName(String token) {
        if(!token.startsWith("mp."))
            return false;

        boolean hasVarName = false;
        for(int i = 3;i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: OPERATOR_METHOD_NAME <code>op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|
     * ((r-)?(concat|add|sub|mul|pow|div|truncDiv|floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|
     * isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call)))</code>
     */
    private static final String[] OPERATOR_METHOD_NAMES = new String[] {
            "op:len",
            "op:deepCopy",
            "op:inc",
            "op:dec",
            "op:pos",
            "op:inv",
            "op:not",
            "op:abs",
            "op:iter",
            "op:hasNext",
            "op:next",

            "op:concat", "op:r-concat",
            "op:add", "op:r-add",
            "op:sub", "op:r-sub",
            "op:mul", "op:r-mul",
            "op:pow", "op:r-pow",
            "op:div", "op:r-div",
            "op:truncDiv", "op:r-truncDiv",
            "op:floorDiv", "op:r-floorDiv",
            "op:ceilDiv", "op:r-ceilDiv",
            "op:mod", "op:r-mod",
            "op:and", "op:r-and",
            "op:or", "op:r-or",
            "op:xor", "op:r-xor",
            "op:lshift", "op:r-lshift",
            "op:rshift", "op:r-rshift",
            "op:rzshift", "op:r-rzshift",
            "op:isEquals", "op:r-isEquals",
            "op:isStrictEquals", "op:r-isStrictEquals",
            "op:isLessThan", "op:r-isLessThan",
            "op:isGreaterThan", "op:r-isGreaterThan",

            "op:getItem",
            "op:setItem",
            "op:slice",

            "op:call"
    };
    private boolean isOperatorMethodName(String token) {
        for(String operator:OPERATOR_METHOD_NAMES)
            if(operator.equals(token))
                return true;

        return false;
    }

    /**
     * LangPatterns: CONVERSION_METHOD_NAME <code>to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number)</code>
     */
    private static final String[] CONVERSION_METHOD_NAMES = new String[] {
            "to:text",
            "to:char",
            "to:int",
            "to:long",
            "to:float",
            "to:double",
            "to:byteBuffer",
            "to:array",
            "to:list",

            "to:bool",
            "to:number"
    };
    private boolean isConversionMethodName(String token) {
        for(String operator:CONVERSION_METHOD_NAMES)
            if(operator.equals(token))
                return true;

        return false;
    }

    /**
     * LangPatterns: VAR_NAME_FULL ((\$\**|&|fp\.)\w+)
     */
    private boolean isVarNameFullWithoutPrefix(String token) {
        boolean funcPtr = token.startsWith("fp.");
        char firstChar = token.charAt(0);
        boolean normalVar = firstChar == '$';

        if(!(funcPtr || normalVar || firstChar == '&'))
            return false;

        int i = funcPtr?3:1;

        if(normalVar)
            for(;i < token.length();i++)
                if(token.charAt(i) != '*')
                    break;

        boolean hasVarName = false;
        for(;i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: VAR_NAME_FULL_WITH_FUNCS ((\$\**|&|fp\.|mp\.|func\.|fn\.|linker\.|ln\.)\w+)
     */
    private boolean isVarNameFullWithFuncsWithoutPrefix(String token) {
        boolean funcPtr = token.startsWith("fp.");
        boolean methodPtr = token.startsWith("mp.");
        boolean func = token.startsWith("func.");
        boolean fn = token.startsWith("fn.");
        boolean linker = token.startsWith("linker.");
        boolean ln = token.startsWith("ln.");
        char firstChar = token.charAt(0);
        boolean normalVar = firstChar == '$';

        if(!(funcPtr || methodPtr || func || fn || linker || ln || normalVar || firstChar == '&'))
            return false;

        int i = (funcPtr || methodPtr || fn || ln)?3:(func?5:(linker?7:1));

        if(normalVar)
            for(;i < token.length();i++)
                if(token.charAt(i) != '*')
                    break;

        boolean hasVarName = false;
        for(;i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: VAR_NAME_PTR_AND_DEREFERENCE (\$\**\[+\w+\]+)
     */
    private boolean isVarNamePtrAndDereferenceWithoutPrefix(String token) {
        if(token.charAt(0) != '$')
            return false;

        int i = 1;
        for(;i < token.length();i++)
            if(token.charAt(i) != '*')
                break;

        boolean hasNoBracketOpening = true;
        for(;i < token.length();i++) {
            if(token.charAt(i) == '[')
                hasNoBracketOpening = false;
            else
                break;
        }

        if(hasNoBracketOpening)
            return false;

        boolean hasNoVarName = true;
        for(;i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasNoVarName = false;
            else
                break;
        }

        if(hasNoVarName)
            return false;

        boolean hasBracketClosing = false;
        for(;i < token.length();i++)
            if(token.charAt(i) == ']')
                hasBracketClosing = true;
            else
                return false;

        return hasBracketClosing;
    }

    /**
     * LangPatterns: FUNC_NAME ((func\.|fn\.|linker\.|ln\.)\w+)
     */
    private boolean isFuncName(String token) {
        boolean func = token.startsWith("func.");
        boolean linker = token.startsWith("linker.");

        if(!(func || linker || token.startsWith("fn.") || token.startsWith("ln.")))
            return false;

        boolean hasVarName = false;
        for(int i = func?5:(linker?7:3);i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    /**
     * LangPatterns: VAR_NAME_FUNC_PTR (fp\.\w+)
     */
    private boolean isVarNameFuncPtrWithoutPrefix(String token) {
        if(!token.startsWith("fp."))
            return false;

        boolean hasVarName = false;
        for(int i = 3;i < token.length();i++) {
            char c = token.charAt(i);
            if((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_')
                hasVarName = true;
            else
                return false;
        }

        return hasVarName;
    }

    int getScopeId() {
        return scopeId;
    }

    private void initLangStandard() {
        if(!isInitializingLangStandardImplementation)
            throw new IllegalStateException("Initialization of lang standard implementation was already completed");

        //Temporary scope for lang standard implementation in lang code
        pushStackElement(new StackElement("<standard>", "standard.lang", null, null, null, null),
                CodePosition.EMPTY);
        enterScope();

        try(BufferedReader langStandardImplementation = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("lang/standard.lang"))))
        ) {
            langVars.addEssentialLangVars(null);

            //Interpret lang standard implementation lang code
            interpretLines(langStandardImplementation);

            getData().var.forEach((variableName, variable) -> {
                if(variableName.startsWith("fp.") && variable.getType() == DataType.FUNCTION_POINTER) {
                    String functionName = variableName.substring(3);
                    funcs.put(functionName, variable.getFunctionPointer().withFunctionName("func." + functionName));
                }else if(variable.getType() == DataType.STRUCT || variable.getType() == DataType.OBJECT) {
                    standardTypes.put(variableName, variable);
                }
            });
        }catch(Exception e) {
            throw new IllegalStateException("Could not load lang standard implementation in lang code", e);
        } finally {
            //Cleanup of temporary scope
            popStackElement();

            exitScope();

            resetParserPositionVars();

            isInitializingLangStandardImplementation = false;
        }
    }

    void enterScope() {
        enterScope(null);
    }
    void enterScope(String[] langArgs) {
        scopeId++;

        data.put(scopeId, new Data());

        if(langArgs != null) {
            DataObject[] langArgsArray = new DataObject[langArgs.length];
            for(int i = 0;i < langArgs.length;i++)
                langArgsArray[i] = new DataObject(langArgs[i]);
            getData().var.put("&LANG_ARGS", new DataObject().setArray(langArgsArray).setFinalData(true).setVariableName("&LANG_ARGS"));
        }

        resetVarsAndFuncPtrs();

        if(scopeId > 0) {
            //Copy translation map (except "lang.* = *") to the new scope's translation map
            data.get(scopeId - 1).lang.forEach((k, v) -> {
                if(!k.startsWith("lang.")) {
                    getData().lang.put(k, v);
                }
            });
        }
    }
    private void resetVarsAndFuncPtrs() {
        DataObject langArgs = getData().var.get("&LANG_ARGS");
        getData().var.clear();

        if(isInitializingLangStandardImplementation)
            langVars.addEssentialLangVars(langArgs);
        else
            langVars.addLangVars(langArgs);
    }
    void resetVars() {
        Set<Map.Entry<String, DataObject>> entrySet = new HashSet<>(getData().var.entrySet());
        entrySet.forEach(entry -> {
            String key = entry.getKey();
            if(!entry.getValue().isLangVar() && (key.startsWith("$") || key.startsWith("&")))
                getData().var.remove(key);
        });

        //Not final vars
        setErrno(InterpretingError.NO_ERROR); //Set $LANG_ERRNO
    }

    void exitScope() {
        if(!isInitializingLangStandardImplementation && scopeId == 0) {
            setErrno(InterpretingError.SYSTEM_ERROR, "Main scope can not be exited");
            return;
        }

        data.remove(scopeId);

        scopeId--;
    }

    void setErrno(InterpretingError error) {
        setErrno(error, (String)null);
    }
    void setErrno(InterpretingError error, CodePosition pos) {
        setErrno(error, null, pos);
    }
    void setErrno(InterpretingError error, String message) {
        setErrno(error, message, CodePosition.EMPTY, false);
    }
    void setErrno(InterpretingError error, String message, CodePosition pos) {
        setErrno(error, message, pos, false);
    }
    private void setErrno(InterpretingError error, String message, CodePosition pos, boolean forceNoErrorOutput) {
        int currentErrno = getData().var.get("$LANG_ERRNO").getInt();
        int newErrno = error.getErrorCode();

        if(newErrno >= 0 || currentErrno < 1)
            getData().var.get("$LANG_ERRNO").setInt(newErrno);

        if(!forceNoErrorOutput && executionFlags.errorOutput.shouldPrint(newErrno)) {
            if(message == null)
                message = "";

            StackElement currentStackElement = getCurrentCallStackElement();
            String langPath = currentStackElement.getLangPath();
            String langFile = currentStackElement.getLangFile();
            langFile = langFile == null?"<shell>":langFile;

            String langPathWithFile = langPath + (langPath.endsWith("/")?"":"/") + langFile;
            String langFunctionName = currentStackElement.getLangFunctionName();

            String output = String.format("A%s %s occurred in \"%s:%s\" (FUNCTION: \"%s\", SCOPE_ID: \"%d\")!\n%s: %s (%d)%s\nStack trace:\n%s",
                    newErrno < 0?"":"n", newErrno < 0?"warning":"error", langPathWithFile, pos.equals(CodePosition.EMPTY)?"x":
                            pos.toCompactString(), langFunctionName == null?"<main>":langFunctionName, scopeId, newErrno < 0?"Warning":"Error",
                    error.getErrorText(), error.getErrorCode(), message.isEmpty()?"":"\nMessage: " + message,
                    printStackTrace(pos));
            if(term == null)
                System.err.println(output);
            else
                term.logln(newErrno < 0?Level.WARNING:Level.ERROR, output, LangInterpreter.class);
        }

        if(newErrno > 0) {
            executionState.isThrownValue = true;

            if(executionState.tryBlockLevel > 0 && (!executionState.isSoftTry || executionState.tryBodyScopeID == scopeId)) {
                executionState.tryThrownError = error;
                executionState.stopExecutionFlag = true;
            }
        }
    }

    DataObject setErrnoErrorObject(InterpretingError error) {
        return setErrnoErrorObject(error, (String)null);
    }
    DataObject setErrnoErrorObject(InterpretingError error, CodePosition pos) {
        return setErrnoErrorObject(error, null, pos);
    }
    DataObject setErrnoErrorObject(InterpretingError error, String message) {
        return setErrnoErrorObject(error, message, CodePosition.EMPTY, false);
    }
    DataObject setErrnoErrorObject(InterpretingError error, String message, CodePosition pos) {
        return setErrnoErrorObject(error, message, pos, false);
    }
    private DataObject setErrnoErrorObject(InterpretingError error, String message, CodePosition pos, boolean forceNoErrorOutput) {
        setErrno(error, message, pos, forceNoErrorOutput);

        return new DataObject().setError(new ErrorObject(error, message));
    }
    InterpretingError getAndClearErrnoErrorObject() {
        int errno = getData().var.get("$LANG_ERRNO").getInt();

        setErrno(InterpretingError.NO_ERROR); //Reset errno

        return InterpretingError.getErrorFromErrorCode(errno);
    }

    public static final class Data {
        public final Map<String, String> lang = new HashMap<>();
        public final Map<String, DataObject> var = new HashMap<>();
    }

    //Classes for call stack
    public static final class StackElement {
        private final String langPath;
        private final String langFile;
        private final CodePosition pos;
        private final LangObject langClass;
        private final String langClasName;
        private final String langFunctionName;
        final LangModule module;

        public StackElement(String langPath, String langFile, CodePosition pos, LangObject langClass, String langClasName, String langFunctionName, LangModule module) {
            this.langPath = langPath;
            this.langFile = langFile;
            this.langClass = langClass;
            this.langClasName = langClasName;
            this.langFunctionName = langFunctionName;
            this.pos = pos;
            this.module = module;
        }
        public StackElement(String langPath, String langFile, LangObject langClass, String langClasName, String langFunctionName, LangModule module) {
            this(langPath, langFile, CodePosition.EMPTY, langClass, langClasName, langFunctionName, module);
        }

        public StackElement withPos(CodePosition pos) {
            return new StackElement(langPath, langFile, pos, langClass, langClasName, langFunctionName, module);
        }

        public String getLangPath() {
            return langPath;
        }

        public String getLangFile() {
            return langFile;
        }

        public CodePosition getPos() {
            return pos;
        }

        public LangObject getLangClass() {
            return langClass;
        }

        public String getLangClasName() {
            return langClasName;
        }

        public String getLangFunctionName() {
            return langFunctionName;
        }

        public LangModule getModule() {
            return module;
        }

        @Override
        public String toString() {
            String langPathWithFile = langPath + (langPath.endsWith("/")?"":"/") + (langFile == null?"<shell>":langFile);
            return String.format("    at \"%s:%s\" in %s \"%s\"", langPathWithFile, pos.equals(CodePosition.EMPTY)?"x":
                    pos.toCompactString(), langClasName == null?"function":"method", langFunctionName == null?"<main>":
                    (langClasName == null?langFunctionName:(langClasName + "::" + langFunctionName)));
        }
    }

    //Classes for execution flags and execution of execution state
    public static class ExecutionFlags {
        /**
         * Allow terminal function to redirect to standard input, output, or error if no terminal is available
         */
        boolean allowTermRedirect = true;
        /**
         * Will print all errors and warnings in the terminal or to standard error if no terminal is available
         */
        ErrorOutputFlag errorOutput = ErrorOutputFlag.ERROR_ONLY;
        /**
         * Will enable langTest unit tests (Can not be disabled if enabled once)
         */
        boolean langTest = false;
        /**
         * Will disable variable name processing which makes the interpreter faster
         */
        boolean rawVariableNames = false;
        /**
         * Will enable printing of native stack traces
         */
        boolean nativeStackTraces = false;

        public static enum ErrorOutputFlag {
            NOTHING, ALL, ERROR_ONLY;

            public static ErrorOutputFlag getErrorFlagFor(int number) {
                if(number > 0)
                    return ALL;

                if(number < 0)
                    return ERROR_ONLY;

                return NOTHING;
            }

            public boolean shouldPrint(int errorCode) {
                return (errorCode < 0 && this == ALL) || (errorCode > 0 && this != NOTHING);
            }
        }
    }
    public static class ExecutionState {
        /**
         * Will be set to true for returning/throwing a value or breaking/continuing a loop or for try statements
         */
        private boolean stopExecutionFlag;
        private boolean forceStopExecutionFlag;

        //Fields for return statements
        private DataObject returnedOrThrownValue;
        private boolean isThrownValue;
        private CodePosition returnOrThrowStatementPos = CodePosition.EMPTY;

        //Fields for continue & break statements
        /**
         * If > 0: break or continue statement is being processed
         */
        private int breakContinueCount;
        private boolean isContinueStatement;

        //Fields for try statements
        /**
         * Current try block level
         */
        private int tryBlockLevel;
        private InterpretingError tryThrownError;
        private boolean isSoftTry;
        private int tryBodyScopeID;
    }

    public static enum InterpretingError {
        NO_ERROR               ( 0, "No Error"),

        //ERRORS
        FINAL_VAR_CHANGE       ( 1, "LANG or final vars must not be changed"),
        TO_MANY_INNER_LINKS    ( 2, "To many inner links"),
        NO_LANG_FILE           ( 3, "No .lang-File"),
        FILE_NOT_FOUND         ( 4, "File not found"),
        INVALID_FUNC_PTR       ( 5, "Function pointer is invalid"),
        STACK_OVERFLOW         ( 6, "Stack overflow"),
        NO_TERMINAL            ( 7, "No terminal available"),
        INVALID_ARG_COUNT      ( 8, "Invalid argument count"),
        INVALID_LOG_LEVEL      ( 9, "Invalid log level"),
        INVALID_ARR_PTR        (10, "Invalid array pointer"),
        NO_HEX_NUM             (11, "No hexadecimal number"),
        NO_CHAR                (12, "No char"),
        NO_NUM                 (13, "No number"),
        DIV_BY_ZERO            (14, "Dividing by 0"),
        NEGATIVE_ARRAY_LEN     (15, "Negative array length"),
        EMPTY_ARRAY            (16, "Empty array"),
        LENGTH_NAN             (17, "Length NAN"),
        INDEX_OUT_OF_BOUNDS    (18, "Index out of bounds"),
        ARG_COUNT_NOT_ARR_LEN  (19, "Argument count is not array length"),
        INVALID_FUNC_PTR_LOOP  (20, "Invalid function pointer"),
        INVALID_ARGUMENTS      (21, "Invalid arguments"),
        FUNCTION_NOT_FOUND     (22, "Function not found"),
        EOF                    (23, "End of file was reached early"),
        SYSTEM_ERROR           (24, "System Error"),
        NEGATIVE_REPEAT_COUNT  (25, "Negative repeat count"),
        TRANS_KEY_NOT_FOUND    (26, "Translation key does not exist"),
        FUNCTION_NOT_SUPPORTED (27, "Function not supported"),
        BRACKET_MISMATCH       (28, "Bracket mismatch"),
        CONT_FLOW_ARG_MISSING  (29, "Control flow statement condition(s) or argument(s) is/are missing"),
        INVALID_AST_NODE       (30, "Invalid AST node or AST node order"),
        INVALID_PTR            (31, "Invalid pointer"),
        INCOMPATIBLE_DATA_TYPE (32, "Incompatible data type"),
        LANG_ARRAYS_COPY       (33, "&LANG arrays can not be copied"),
        LANG_VER_ERROR         (34, "Lang file's version is not compatible with this version"),
        INVALID_CON_PART       (35, "Invalid statement in control flow statement"),
        INVALID_FORMAT         (36, "Invalid format sequence"),
        INVALID_ASSIGNMENT     (37, "Invalid assignment"),
        NO_BIN_NUM             (38, "No binary number"),
        NO_OCT_NUM             (39, "No octal number"),
        NO_BASE_N_NUM          (40, "Number is not in base N"),
        INVALID_NUMBER_BASE    (41, "Invalid number base"),
        INVALID_REGEX_SYNTAX   (42, "Invalid RegEx syntax"),
        INVALID_TEMPLATE_SYNTAX(43, "Invalid translation template syntax"),
        INVALID_MODULE         (44, "The Lang module is invalid"),
        MODULE_LOAD_UNLOAD_ERR (45, "Error during load or unload of Lang module"),
        MEMBER_NOT_ACCESSIBLE  (46, "The class/object member is not visible/accessible from the current scope"),

        //WARNINGS
        DEPRECATED_FUNC_CALL   (-1, "A deprecated predefined function was called"),
        NO_TERMINAL_WARNING    (-2, "No terminal available"),
        LANG_VER_WARNING       (-3, "Lang file's version is not compatible with this version"),
        INVALID_EXEC_FLAG_DATA (-4, "Execution flag or Lang data is invalid"),
        VAR_SHADOWING_WARNING  (-5, "Variable name shadows an other variable"),
        UNDEF_ESCAPE_SEQUENCE  (-6, "An undefined escape sequence was used"),
        INVALID_DOC_COMMENT    (-7, "Dangling or invalid doc comment syntax");

        private final int errorCode;
        private final String errorText;

        private InterpretingError(int errorCode, String errorText) {
            this.errorCode = errorCode;
            this.errorText = errorText;
        }

        public int getErrorCode() {
            return errorCode;
        }

        public String getErrorText() {
            return errorText;
        }

        public static InterpretingError getErrorFromErrorCode(int errorCode) {
            for(InterpretingError error:values())
                if(error.getErrorCode() == errorCode)
                    return error;

            return InterpretingError.NO_ERROR;
        }
    }

    /**
     * Class for communication between the LangInterpreter and Java
     */
    public static final class LangInterpreterInterface {
        private final LangInterpreter interpreter;

        public LangInterpreterInterface(LangInterpreter interpreter) {
            this.interpreter = interpreter;
        }

        public LangInterpreter getInterpreter() {
            return interpreter;
        }

        public Map<String, String> getTranslationMap() {
            Data data = interpreter.getData();
            if(data == null)
                return null;

            return data.lang;
        }
        public String getTranslation(String key) {
            Map<String, String> translations = getTranslationMap();
            if(translations == null)
                return null;

            return translations.get(key);
        }
        public void setTranslation(String key, String value) {
            Map<String, String> translations = getTranslationMap();
            if(translations != null)
                translations.put(key, value);
        }

        public Map<String, DataObject> getVarMap() {
            Data data = interpreter.getData();
            if(data == null)
                return null;

            return data.var;
        }
        public DataObject getVar(String varName) {
            Map<String, DataObject> vars = getVarMap();
            if(vars == null)
                return null;

            return vars.get(varName);
        }
        public void setVar(String varName, DataObject data) {
            setVar(varName, data, false);
        }
        public void setVar(String varName, DataObject data, boolean ignoreFinal) {
            Map<String, DataObject> vars = getVarMap();
            if(vars != null) {
                DataObject oldData = vars.get(varName);
                if(oldData == null)
                    vars.put(varName, data.setVariableName(varName));
                else if(ignoreFinal || !oldData.isFinalData())
                    oldData.setData(data);
            }
        }
        public void setVar(String varName, String text) {
            setVar(varName, text, false);
        }
        public void setVar(String varName, String text, boolean ignoreFinal) {
            setVar(varName, new DataObject(text), ignoreFinal);
        }
        public void setVar(String varName, DataObject[] arr) {
            setVar(varName, arr, false);
        }
        public void setVar(String varName, DataObject[] arr, boolean ignoreFinal) {
            setVar(varName, new DataObject().setArray(arr), ignoreFinal);
        }
        public void setVar(String varName, LangNativeFunction function) {
            setVar(varName, function, false);
        }
        public void setVar(String varName, LangNativeFunction function, boolean ignoreFinal) {
            setVar(varName, new DataObject().setFunctionPointer(new FunctionPointerObject(varName, function)), ignoreFinal);
        }
        public void setVar(String varName, InterpretingError error) {
            setVar(varName, error, false);
        }
        public void setVar(String varName, InterpretingError error, boolean ignoreFinal) {
            setVar(varName, new DataObject().setError(new ErrorObject(error)), false);
        }

        public void setErrno(InterpretingError error) {
            setErrno(error, "");
        }
        public void setErrno(InterpretingError error, CodePosition pos) {
            setErrno(error, "", pos);
        }
        public void setErrno(InterpretingError error, String message) {
            interpreter.setErrno(error, message);
        }
        public void setErrno(InterpretingError error, String message, CodePosition pos) {
            interpreter.setErrno(error, message, pos);
        }
        public DataObject setErrnoErrorObject(InterpretingError error) {
            return setErrnoErrorObject(error, "");
        }
        public DataObject setErrnoErrorObject(InterpretingError error, CodePosition pos) {
            return setErrnoErrorObject(error, "", pos);
        }
        public DataObject setErrnoErrorObject(InterpretingError error, String message) {
            return interpreter.setErrnoErrorObject(error, message);
        }
        public DataObject setErrnoErrorObject(InterpretingError error, String message, CodePosition pos) {
            return interpreter.setErrnoErrorObject(error, message, pos);
        }
        public InterpretingError getAndClearErrnoErrorObject() {
            return interpreter.getAndClearErrnoErrorObject();
        }

        /**
         * Creates a function which is accessible globally in the Interpreter (= in all scopes)<br>
         * If function already exists, it will be overridden<br>
         * Function can be accessed with "func.[funcName]"/"fn.[funcName]" or with "linker.[funcName]"/"ln.[funcName]" and can't be removed nor changed by the Lang file
         */
        public void addPredefinedFunction(String funcName, FunctionPointerObject function) {
            interpreter.funcs.put(funcName, function);
        }
        /**
         * Adds all static methods which are annotated with @LangFunction the object contains
         */
        public void addPredefinedFunctions(Class<?> clazz) {
            interpreter.funcs.putAll(LangNativeFunction.getLangFunctionsOfClass(clazz));
        }
        /**
         * Adds all non-static methods which are annotated with @LangFunction the object contains
         */
        public void addPredefinedFunctions(Object obj) {
            interpreter.funcs.putAll(LangNativeFunction.getLangFunctionsFromObject(obj));
        }
        public void addPredefinedFunctions(Map<String, FunctionPointerObject> funcs) {
            interpreter.funcs.putAll(funcs);
        }
        public Map<String, FunctionPointerObject> getPredefinedFunctions() {
            return interpreter.funcs;
        }

        public DataObject exec(BufferedReader lines) throws IOException, StoppedException {
            getAndResetReturnValue(); //Reset returned value else the interpreter would stop immediately
            return interpreter.interpretLines(lines);
        }
        public DataObject exec(String lines) throws IOException, StoppedException {
            try(BufferedReader reader = new BufferedReader(new StringReader(lines))) {
                return exec(reader);
            }
        }
        /**
         * Can be called in a different thread<br>
         * Any execution method which was previously called and are still running and any future call of execution methods will throw a
         * {@link at.jddev0.lang.LangInterpreter.StoppedException StoppedException} exception if the stop flag is set
         */
        public void stop() {
            interpreter.executionState.forceStopExecutionFlag = true;
        }
        /**
         * Must be called before execution if the {@link LangInterpreter.LangInterpreterInterface#stop() stop()} method was previously called
         */
        public void resetStopFlag() {
            interpreter.executionState.forceStopExecutionFlag = false;
        }

        public void setErrorOutputFlag(ExecutionFlags.ErrorOutputFlag errorOutput) {
            if(errorOutput == null)
                throw new NullPointerException();

            interpreter.executionFlags.errorOutput = errorOutput;
        }

        public StackElement getCurrentCallStackElement() {
            return interpreter.getCurrentCallStackElement();
        }

        /**
         * Must be called before {@link LangInterpreter.LangInterpreterInterface#getAndResetReturnValue() getAndResetReturnValue()} method
         */
        public boolean isReturnedValueThrowValue() {
            return interpreter.executionState.isThrownValue;
        }
        public CodePosition getThrowStatementPos() {
            return interpreter.executionState.returnOrThrowStatementPos;
        }
        public DataObject getAndResetReturnValue() {
            return interpreter.getAndResetReturnValue();
        }

        public AbstractSyntaxTree parseLines(BufferedReader lines) throws IOException {
            return interpreter.parseLines(lines);
        }

        public void interpretAST(AbstractSyntaxTree ast) throws StoppedException {
            getAndResetReturnValue(); //Reset returned value else the interpreter would stop immediately
            interpreter.interpretAST(ast);
        }
        public DataObject interpretNode(Node node) throws StoppedException {
            return interpreter.interpretNode(null, node);
        }
        public DataObject interpretFunctionCallNode(FunctionCallNode node) throws StoppedException {
            return interpreter.interpretFunctionCallNode(null, node);
        }
        public DataObject interpretFunctionPointer(FunctionPointerObject fp, String functionName, List<Node> argumentList,
                                                   CodePosition parentPos) throws StoppedException {
            return interpreter.callFunctionPointer(fp, functionName, interpreter.interpretFunctionPointerArguments(argumentList), parentPos);
        }
        public DataObject interpretFunctionPointer(FunctionPointerObject fp, String functionName, List<Node> argumentList) throws StoppedException {
            return interpreter.callFunctionPointer(fp, functionName, interpreter.interpretFunctionPointerArguments(argumentList));
        }

        public int getParserLineNumber() {
            return interpreter.getParserLineNumber();
        }

        public void setParserLineNumber(int lineNumber) {
            interpreter.setParserLineNumber(lineNumber);
        }

        public void resetParserPositionVars() {
            interpreter.resetParserPositionVars();
        }

        public DataObject callFunctionPointer(FunctionPointerObject fp, String functionName, List<DataObject> argumentValueList,
                                              CodePosition parentPos) throws StoppedException {
            return interpreter.callFunctionPointer(fp, functionName, argumentValueList, parentPos);
        }
        public DataObject callFunctionPointer(FunctionPointerObject fp, String functionName, List<DataObject> argumentValueList) throws StoppedException {
            return interpreter.callFunctionPointer(fp, functionName, argumentValueList);
        }

        public Map<String, LangModule> getModules() {
            return interpreter.modules;
        }

        public List<String> getModuleExportedFunctions(String moduleName) {
            LangModule module = interpreter.modules.get(moduleName);
            return module == null?null:module.getExportedFunctions();
        }

        public Map<String, DataObject> getModuleExportedVariables(String moduleName) {
            LangModule module = interpreter.modules.get(moduleName);
            return module == null?null:module.getExportedVariables();
        }
    }

    /**
     * Exception which will be thrown if {@link LangInterpreter#forceStop() forceStop()} or {@link LangInterpreter.LangInterpreterInterface#stop() stop()} is called
     */
    public static class StoppedException extends RuntimeException {
        private static final long serialVersionUID = 3184689513001702458L;

        public StoppedException() {
            super("The execution was stopped!");
        }
    }
}