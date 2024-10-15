package at.jddev0.lang;

import java.util.*;

import at.jddev0.lang.LangParser.ParsingError;

/**
 * Lang-Module<br>
 * AbstractSyntaxTree for Lang
 *
 * @author JDDev0
 * @version v1.0.0
 */
public final class AbstractSyntaxTree implements Iterable<AbstractSyntaxTree.Node> {
    private final List<Node> nodes;

    public AbstractSyntaxTree() {
        nodes = new ArrayList<>();
    }

    public CodePosition getPos() {
        return nodes.isEmpty()?CodePosition.EMPTY:nodes.get(0).getPos().combine(nodes.get(nodes.size() - 1).getPos());
    }

    public void addChild(Node node) {
        nodes.add(node);
    }

    public List<Node> getChildren() {
        return nodes;
    }

    public Node convertToNode() {
        if(nodes.size() == 1)
            return nodes.get(0);
        return new AbstractSyntaxTree.ListNode(nodes);
    }

    @Override
    public Iterator<AbstractSyntaxTree.Node> iterator() {
        return nodes.iterator();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AST: Children: {\n");
        nodes.forEach(node -> {
            String[] tokens = node.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
        });
        builder.append("}\n");

        return builder.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)
            return true;

        if(obj == null)
            return false;

        if(!(obj instanceof AbstractSyntaxTree))
            return false;

        AbstractSyntaxTree that = (AbstractSyntaxTree)obj;
        return this.nodes.equals(that.nodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodes);
    }

    public static interface Node extends Iterable<Node> {
        public List<Node> getChildren();
        public NodeType getNodeType();
        public CodePosition getPos();

        @Override
        public default Iterator<AbstractSyntaxTree.Node> iterator() {
            return getChildren().iterator();
        }
    }

    public static final class ListNode implements Node {
        private final List<Node> nodes;
        private final CodePosition pos;

        public ListNode(CodePosition pos, List<Node> nodes) {
            this.nodes = new ArrayList<>(nodes);

            this.pos = pos;
        }
        public ListNode(List<Node> nodes) {
            this(nodes.isEmpty()?CodePosition.EMPTY:nodes.get(0).getPos().combine(
                    nodes.get(nodes.size() - 1).getPos()), nodes);
        }

        @Override
        public List<Node> getChildren() {
            return nodes;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LIST;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ListNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Children: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ListNode))
                return false;

            ListNode that = (ListNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    //Is only super class for other nodes
    public static abstract class ChildlessNode implements Node {
        private final List<Node> nodes;
        protected final CodePosition pos;

        public ChildlessNode(CodePosition pos) {
            this.nodes = new ArrayList<>(0);

            this.pos = pos;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        @Override
        public List<Node> getChildren() {
            return new ArrayList<>(nodes);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.GENERAL;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ListNode))
                return false;

            ListNode that = (ListNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    public static final class ParsingErrorNode extends ChildlessNode {
        private final ParsingError error;
        private final String message;

        public ParsingErrorNode(CodePosition pos, ParsingError error, String message) {
            super(pos);

            this.error = error;
            this.message = message;
        }
        public ParsingErrorNode(CodePosition pos, ParsingError error) {
            this(pos, error, null);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.PARSING_ERROR;
        }

        public ParsingError getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ParsingErrorNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Error: \"");
            builder.append(error);
            builder.append("\", Message: \"");
            builder.append(message);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ParsingErrorNode))
                return false;

            ParsingErrorNode that = (ParsingErrorNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.error == that.error && this.message.equals(that.message);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.error, this.message);
        }
    }

    public static final class AssignmentNode implements Node {
        private final List<Node> nodes;
        private final CodePosition pos;

        public AssignmentNode(CodePosition pos, Node lvalue, Node rvalue) {
            nodes = new ArrayList<>(2);
            nodes.add(lvalue);
            nodes.add(rvalue);

            this.pos = pos;
        }
        public AssignmentNode(Node lvalue, Node rvalue) {
            this(lvalue.getPos().combine(rvalue.getPos()), lvalue, rvalue);
        }

        @Override
        public List<Node> getChildren() {
            return nodes;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.ASSIGNMENT;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public Node getLvalue() {
            return nodes.get(0);
        }

        public Node getRvalue() {
            return nodes.get(1);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("AssignmentNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", lvalue: {\n");
            String[] tokens = getLvalue().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, rvalue: {\n");
            tokens = getRvalue().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof AssignmentNode))
                return false;

            AssignmentNode that = (AssignmentNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    public static final class EscapeSequenceNode extends ChildlessNode {
        private final char c;

        public EscapeSequenceNode(CodePosition pos, char c) {
            super(pos);

            this.c = c;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.ESCAPE_SEQUENCE;
        }

        public char getEscapeSequenceChar() {
            return c;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("EscapeSequenceNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Char: \"");
            builder.append(c);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof EscapeSequenceNode))
                return false;

            EscapeSequenceNode that = (EscapeSequenceNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.c == that.c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.c);
        }
    }

    /**
     * Unprocessed name of variable (Could be more than variable name)<br>
     * e.g.: $var = ABC<br>
     *       $test = $vars<br> #"$vars" would be in the variable name node although only "$var" is the variable name
     */
    public static final class UnprocessedVariableNameNode extends ChildlessNode {
        private final String variableName;

        public UnprocessedVariableNameNode(CodePosition pos, String variableName) {
            super(pos);

            this.variableName = variableName;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.UNPROCESSED_VARIABLE_NAME;
        }

        public String getVariableName() {
            return variableName;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("UnprocessedVariableNameNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", VariableName: \"");
            builder.append(variableName);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof UnprocessedVariableNameNode))
                return false;

            UnprocessedVariableNameNode that = (UnprocessedVariableNameNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.variableName.equals(that.variableName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.variableName);
        }
    }

    public static final class VariableNameNode extends ChildlessNode {
        private final String variableName;
        private final String typeConstraint;

        public VariableNameNode(CodePosition pos, String variableName, String typeConstraint) {
            super(pos);

            this.variableName = variableName;
            this.typeConstraint = typeConstraint;
        }
        public VariableNameNode(CodePosition pos, String variableName) {
            this(pos, variableName, null);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.VARIABLE_NAME;
        }

        public String getVariableName() {
            return variableName;
        }

        public String getTypeConstraint() {
            return typeConstraint;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("VariableNameNode: LineFrom: ");
            builder.append(pos.toCompactString());
            builder.append(", VariableName: \"");
            builder.append(variableName);
            builder.append(", TypeConstraint: \"");
            builder.append(typeConstraint);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof VariableNameNode))
                return false;

            VariableNameNode that = (VariableNameNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.variableName.equals(that.variableName) &&
                    Objects.equals(this.typeConstraint, that.typeConstraint);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.variableName, this.typeConstraint);
        }
    }

    /**
     * Will store an "," with whitespaces if needed
     */
    public static final class ArgumentSeparatorNode extends ChildlessNode {
        private final String originalText;

        public ArgumentSeparatorNode(CodePosition pos, String originalText) {
            super(pos);

            this.originalText = originalText;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.ARGUMENT_SEPARATOR;
        }

        public String getOriginalText() {
            return originalText;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ArgumentSeparatorNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", OriginalText: \"");
            builder.append(originalText);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ArgumentSeparatorNode))
                return false;

            ArgumentSeparatorNode that = (ArgumentSeparatorNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.originalText.equals(that.originalText);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), originalText);
        }
    }

    public static final class FunctionCallNode implements Node {
        private final List<Node> argumentList;
        private final CodePosition pos;
        private final String functionName;

        public FunctionCallNode(CodePosition pos, List<Node> argumentList, String functionName) {
            this.argumentList = new ArrayList<>(argumentList);

            this.pos = pos;

            this.functionName = functionName;
        }

        @Override
        public List<Node> getChildren() {
            return argumentList;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.FUNCTION_CALL;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public String getFunctionName() {
            return functionName;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FunctionCallNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", FunctionName: \"");
            builder.append(functionName);
            builder.append("\", ParameterList: {\n");
            argumentList.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof FunctionCallNode))
                return false;

            FunctionCallNode that = (FunctionCallNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.functionName.equals(that.functionName) && this.argumentList.equals(that.argumentList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.functionName, this.argumentList);
        }
    }

    public static final class FunctionCallPreviousNodeValueNode implements Node {
        private final String leadingWhitespace;
        private final String trailingWhitespace;
        private final List<Node> argumentList;
        private final CodePosition pos;

        public FunctionCallPreviousNodeValueNode(CodePosition pos, String leadingWhitespace, String trailingWhitespace,
                                                 List<Node> argumentList) {
            this.leadingWhitespace = leadingWhitespace;
            this.trailingWhitespace = trailingWhitespace;
            this.argumentList = new ArrayList<>(argumentList);
            this.pos = pos;
        }

        public String getLeadingWhitespace() {
            return leadingWhitespace;
        }

        public String getTrailingWhitespace() {
            return trailingWhitespace;
        }

        @Override
        public List<Node> getChildren() {
            return argumentList;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.FUNCTION_CALL_PREVIOUS_NODE_VALUE;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FunctionCallPreviousNodeValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", ArgumentList: {\n");
            argumentList.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}, LeadingWhitespace: \"");
            builder.append(leadingWhitespace);
            builder.append("\", TrailingWhitespace: \"");
            builder.append(trailingWhitespace);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof FunctionCallPreviousNodeValueNode))
                return false;

            FunctionCallPreviousNodeValueNode that = (FunctionCallPreviousNodeValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.leadingWhitespace.equals(that.leadingWhitespace) && this.trailingWhitespace.equals(that.trailingWhitespace) &&
                    this.argumentList.equals(that.argumentList);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.leadingWhitespace, this.trailingWhitespace, this.argumentList);
        }
    }

    public static final class FunctionDefinitionNode implements Node {
        private final String functionName;
        private final boolean overloaded;
        private final boolean combinator;
        private final String docComment;
        private final List<Node> parameterList;
        private final String returnValueTypeConstraint;
        private final AbstractSyntaxTree functionBody;
        private final CodePosition pos;

        public FunctionDefinitionNode(CodePosition pos, String functionName, boolean overloaded, boolean combinator,
                                      String docComment, List<Node> parameterList, String returnValueTypeConstraint,
                                      AbstractSyntaxTree functionBody) {
            this.functionName = functionName;
            this.overloaded = overloaded;
            this.combinator = combinator;
            this.docComment = docComment;
            this.parameterList = new ArrayList<>(parameterList);
            this.returnValueTypeConstraint = returnValueTypeConstraint;
            this.functionBody = functionBody;
            this.pos = pos;
        }

        @Override
        public List<Node> getChildren() {
            return parameterList;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.FUNCTION_DEFINITION;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public String getFunctionName() {
            return functionName;
        }

        public boolean isOverloaded() {
            return overloaded;
        }

        public boolean isCombinator() {
            return combinator;
        }

        public String getDocComment() {
            return docComment;
        }

        public String getReturnValueTypeConstraint() {
            return returnValueTypeConstraint;
        }

        public AbstractSyntaxTree getFunctionBody() {
            return functionBody;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FunctionDefinitionNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", FunctionName: \"");
            builder.append(functionName);
            builder.append("\", Overloaded: ");
            builder.append(overloaded);
            builder.append(", Combinator: ");
            builder.append(combinator);
            builder.append(", Doc Comment: \"");
            builder.append(docComment);
            builder.append("\", ParameterList: {\n");
            parameterList.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}, ReturnValueTypeConstraint: ");
            builder.append(returnValueTypeConstraint);
            builder.append(", FunctionBody: {\n");
            String[] tokens = functionBody.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof FunctionDefinitionNode))
                return false;

            FunctionDefinitionNode that = (FunctionDefinitionNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.functionName.equals(that.functionName) &&
                    this.overloaded == that.overloaded && this.combinator == that.combinator &&
                    this.docComment.equals(that.docComment) && this.parameterList.equals(that.parameterList) &&
                    this.functionBody.equals(that.functionBody);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.functionName, this.overloaded, this.combinator, this.docComment,
                    this.parameterList, this.functionBody);
        }
    }

    //Is only super class for other nodes
    public static abstract class IfStatementPartNode extends ChildlessNode {
        private final AbstractSyntaxTree ifBody;

        public IfStatementPartNode(CodePosition pos, AbstractSyntaxTree ifBody) {
            super(pos);

            this.ifBody = ifBody;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.GENERAL;
        }

        public AbstractSyntaxTree getIfBody() {
            return ifBody;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof IfStatementPartNode))
                return false;

            IfStatementPartNode that = (IfStatementPartNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.ifBody.equals(that.ifBody);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.ifBody);
        }
    }

    public static final class IfStatementPartIfNode extends IfStatementPartNode {
        private final OperationNode condition;

        public IfStatementPartIfNode(CodePosition pos, AbstractSyntaxTree ifBody, OperationNode condition) {
            super(pos, ifBody);

            if(condition.getNodeType() != NodeType.CONDITION)
                throw new IllegalStateException("Node type is not compatible with this node");

            this.condition = condition;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.IF_STATEMENT_PART_IF;
        }

        public OperationNode getCondition() {
            return condition;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("IfStatementPartIfNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Condition: {\n");
            String[] tokens = condition.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, IfBody: {\n");
            tokens = getIfBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof IfStatementPartIfNode))
                return false;

            IfStatementPartIfNode that = (IfStatementPartIfNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.condition.equals(that.condition) && this.getIfBody().equals(that.getIfBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.condition, this.getIfBody());
        }
    }

    public static final class IfStatementPartElseNode extends IfStatementPartNode {
        public IfStatementPartElseNode(CodePosition pos, AbstractSyntaxTree ifBody) {
            super(pos, ifBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.IF_STATEMENT_PART_ELSE;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("IfStatementPartElseNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", IfBody: {\n");
            String[] tokens = getIfBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof IfStatementPartElseNode))
                return false;

            IfStatementPartElseNode that = (IfStatementPartElseNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getIfBody().equals(that.getIfBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getIfBody());
        }
    }

    public static final class IfStatementNode implements Node {
        private final List<IfStatementPartNode> nodes;
        private final CodePosition pos;

        public IfStatementNode(CodePosition pos, List<IfStatementPartNode> nodes) {
            this.nodes = nodes;

            this.pos = pos;
        }

        @Override
        public List<Node> getChildren() {
            return new ArrayList<>(nodes);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.IF_STATEMENT;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public List<IfStatementPartNode> getIfStatementPartNodes() {
            return nodes;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("IfStatementNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Children: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof IfStatementNode))
                return false;

            IfStatementNode that = (IfStatementNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    //Is only super class for other nodes
    public static abstract class LoopStatementPartNode extends ChildlessNode {
        private final AbstractSyntaxTree loopBody;

        public LoopStatementPartNode(CodePosition pos, AbstractSyntaxTree loopBody) {
            super(pos);

            this.loopBody = loopBody;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.GENERAL;
        }

        public AbstractSyntaxTree getLoopBody() {
            return loopBody;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartNode))
                return false;

            LoopStatementPartNode that = (LoopStatementPartNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.loopBody.equals(that.loopBody);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.loopBody);
        }
    }

    public static final class LoopStatementPartLoopNode extends LoopStatementPartNode {
        public LoopStatementPartLoopNode(CodePosition pos, AbstractSyntaxTree loopBody) {
            super(pos, loopBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_PART_LOOP;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementPartLoopNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", LoopBody: {\n");
            String[] tokens = getLoopBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartLoopNode))
                return false;

            LoopStatementPartLoopNode that = (LoopStatementPartLoopNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getLoopBody().equals(that.getLoopBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getLoopBody());
        }
    }

    public static final class LoopStatementPartWhileNode extends LoopStatementPartNode {
        private final OperationNode condition;

        public LoopStatementPartWhileNode(CodePosition pos, AbstractSyntaxTree loopBody, OperationNode condition) {
            super(pos, loopBody);

            if(condition.getNodeType() != NodeType.CONDITION)
                throw new IllegalStateException("Node type is not compatible with this node");

            this.condition = condition;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_PART_WHILE;
        }

        public OperationNode getCondition() {
            return condition;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementPartWhileNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Condition: {\n");
            String[] tokens = condition.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, LoopBody: {\n");
            tokens = getLoopBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartWhileNode))
                return false;

            LoopStatementPartWhileNode that = (LoopStatementPartWhileNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.condition.equals(that.condition) && this.getLoopBody().equals(that.getLoopBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.condition, this.getLoopBody());
        }
    }

    public static final class LoopStatementPartUntilNode extends LoopStatementPartNode {
        private final OperationNode condition;

        public LoopStatementPartUntilNode(CodePosition pos, AbstractSyntaxTree loopBody, OperationNode condition) {
            super(pos, loopBody);

            if(condition.getNodeType() != NodeType.CONDITION)
                throw new IllegalStateException("Node type is not compatible with this node");

            this.condition = condition;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_PART_UNTIL;
        }

        public OperationNode getCondition() {
            return condition;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementPartUntilNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Condition: {\n");
            String[] tokens = condition.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, LoopBody: {\n");
            tokens = getLoopBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartUntilNode))
                return false;

            LoopStatementPartUntilNode that = (LoopStatementPartUntilNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.condition.equals(that.condition) && this.getLoopBody().equals(that.getLoopBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.condition, this.getLoopBody());
        }
    }

    public static final class LoopStatementPartRepeatNode extends LoopStatementPartNode {
        private final Node varPointerNode;
        private final Node repeatCountNode;

        public LoopStatementPartRepeatNode(CodePosition pos, AbstractSyntaxTree loopBody, Node varPointerNode,
                                           Node repeatCountNode) {
            super(pos, loopBody);

            this.varPointerNode = varPointerNode;
            this.repeatCountNode = repeatCountNode;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_PART_REPEAT;
        }

        public Node getVarPointerNode() {
            return varPointerNode;
        }

        public Node getRepeatCountNode() {
            return repeatCountNode;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementPartRepeatNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", varPointer: {\n");
            String[] tokens = varPointerNode.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, repeatCount: {\n");
            tokens = repeatCountNode.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, LoopBody: {\n");
            tokens = getLoopBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartRepeatNode))
                return false;

            LoopStatementPartRepeatNode that = (LoopStatementPartRepeatNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.varPointerNode.equals(that.varPointerNode) && this.repeatCountNode.equals(that.repeatCountNode) &&
                    this.getLoopBody().equals(that.getLoopBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.varPointerNode, this.repeatCountNode, this.getLoopBody());
        }
    }

    public static final class LoopStatementPartForEachNode extends LoopStatementPartNode {
        private final Node varPointerNode;
        private final Node compositeOrTextNode;

        public LoopStatementPartForEachNode(CodePosition pos, AbstractSyntaxTree loopBody, Node varPointerNode,
                                            Node collectionOrTextNode) {
            super(pos, loopBody);

            this.varPointerNode = varPointerNode;
            this.compositeOrTextNode = collectionOrTextNode;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_PART_FOR_EACH;
        }

        public Node getVarPointerNode() {
            return varPointerNode;
        }

        public Node getCompositeOrTextNode() {
            return compositeOrTextNode;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementPartForEachNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", varPointer: {\n");
            String[] tokens = varPointerNode.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, compositeOrTextNode: {\n");
            tokens = compositeOrTextNode.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, LoopBody: {\n");
            tokens = getLoopBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartForEachNode))
                return false;

            LoopStatementPartForEachNode that = (LoopStatementPartForEachNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.varPointerNode.equals(that.varPointerNode) && this.compositeOrTextNode.equals(that.compositeOrTextNode) &&
                    this.getLoopBody().equals(that.getLoopBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.varPointerNode, this.compositeOrTextNode, this.getLoopBody());
        }
    }

    public static final class LoopStatementPartElseNode extends LoopStatementPartNode {
        public LoopStatementPartElseNode(CodePosition pos, AbstractSyntaxTree loopBody) {
            super(pos, loopBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_PART_ELSE;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementPartElseNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", LoopBody: {\n");
            String[] tokens = getLoopBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementPartElseNode))
                return false;

            LoopStatementPartElseNode that = (LoopStatementPartElseNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getLoopBody().equals(that.getLoopBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getLoopBody());
        }
    }

    public static final class LoopStatementNode implements Node {
        private final List<LoopStatementPartNode> nodes;
        private final CodePosition pos;

        public LoopStatementNode(CodePosition pos, List<LoopStatementPartNode> nodes) {
            this.nodes = nodes;
            this.pos = pos;
        }

        @Override
        public List<Node> getChildren() {
            return new ArrayList<>(nodes);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public List<LoopStatementPartNode> getLoopStatementPartNodes() {
            return nodes;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Children: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementNode))
                return false;

            LoopStatementNode that = (LoopStatementNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    public static final class LoopStatementContinueBreakStatement extends ChildlessNode {
        private final Node numberNode;
        private final boolean continueNode;

        public LoopStatementContinueBreakStatement(CodePosition pos, Node numberNode, boolean continueNode) {
            super(pos);

            this.numberNode = numberNode;
            this.continueNode = continueNode;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LOOP_STATEMENT_CONTINUE_BREAK;
        }

        public Node getNumberNode() {
            return numberNode;
        }

        public boolean isContinueNode() {
            return continueNode;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LoopStatementContinueBreakStatementNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", numberNode: {\n");
            if(numberNode == null) {
                builder.append("null");
            }else {
                String[] tokens = numberNode.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            }
            builder.append("}, continueNode: ");
            builder.append(continueNode);
            builder.append("\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LoopStatementContinueBreakStatement))
                return false;

            LoopStatementContinueBreakStatement that = (LoopStatementContinueBreakStatement)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.numberNode.equals(that.numberNode) && this.continueNode == that.continueNode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.numberNode, this.continueNode);
        }
    }

    //Is only super class for other nodes
    public static abstract class TryStatementPartNode extends ChildlessNode {
        private final AbstractSyntaxTree tryBody;

        public TryStatementPartNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            super(pos);

            this.tryBody = tryBody;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.GENERAL;
        }

        public AbstractSyntaxTree getTryBody() {
            return tryBody;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartNode))
                return false;

            TryStatementPartNode that = (TryStatementPartNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.tryBody.equals(that.tryBody);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.tryBody);
        }
    }

    public static final class TryStatementPartTryNode extends TryStatementPartNode {
        public TryStatementPartTryNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            super(pos, tryBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT_PART_TRY;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementPartTryNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", TryBody: {\n");
            String[] tokens = getTryBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartTryNode))
                return false;

            TryStatementPartTryNode that = (TryStatementPartTryNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getTryBody().equals(that.getTryBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getTryBody());
        }
    }

    public static final class TryStatementPartSoftTryNode extends TryStatementPartNode {
        public TryStatementPartSoftTryNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            super(pos, tryBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT_PART_SOFT_TRY;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementPartSoftTryNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", TryBody: {\n");
            String[] tokens = getTryBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartSoftTryNode))
                return false;

            TryStatementPartSoftTryNode that = (TryStatementPartSoftTryNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getTryBody().equals(that.getTryBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getTryBody());
        }
    }

    public static final class TryStatementPartNonTryNode extends TryStatementPartNode {
        public TryStatementPartNonTryNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            super(pos, tryBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT_PART_NON_TRY;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementPartNonTryNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", TryBody: {\n");
            String[] tokens = getTryBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartNonTryNode))
                return false;

            TryStatementPartNonTryNode that = (TryStatementPartNonTryNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getTryBody().equals(that.getTryBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getTryBody());
        }
    }

    public static final class TryStatementPartCatchNode extends TryStatementPartNode {
        private final List<Node> errors;

        /**
         * Every error will be accepted
         */
        public TryStatementPartCatchNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            this(pos, tryBody, null);
        }

        /**
         * @param errors Every error will be accepted if errors is null
         */
        public TryStatementPartCatchNode(CodePosition pos, AbstractSyntaxTree tryBody, List<Node> errors) {
            super(pos, tryBody);

            this.errors = errors == null?null:new ArrayList<>(errors);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT_PART_CATCH;
        }

        public List<Node> getExpections() {
            return errors == null?null:new ArrayList<>(errors);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementPartCatchNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Errors: {\n");
            if(errors == null)
                builder.append("\tnull\n");
            else
                errors.forEach(node -> {
                    String[] tokens = node.toString().split("\\n");
                    for(String token:tokens) {
                        builder.append("\t");
                        builder.append(token);
                        builder.append("\n");
                    }
                });
            builder.append("}, TryBody: {\n");
            String[] tokens = getTryBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartCatchNode))
                return false;

            TryStatementPartCatchNode that = (TryStatementPartCatchNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.errors.equals(that.errors) && this.getTryBody().equals(that.getTryBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.errors, this.getTryBody());
        }
    }

    public static final class TryStatementPartElseNode extends TryStatementPartNode {
        public TryStatementPartElseNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            super(pos, tryBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT_PART_ELSE;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementPartElseNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", TryBody: {\n");
            String[] tokens = getTryBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartElseNode))
                return false;

            TryStatementPartElseNode that = (TryStatementPartElseNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getTryBody().equals(that.getTryBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getTryBody());
        }
    }

    public static final class TryStatementPartFinallyNode extends TryStatementPartNode {
        public TryStatementPartFinallyNode(CodePosition pos, AbstractSyntaxTree tryBody) {
            super(pos, tryBody);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT_PART_FINALLY;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementPartFinallyNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", TryBody: {\n");
            String[] tokens = getTryBody().toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementPartTryNode))
                return false;

            TryStatementPartTryNode that = (TryStatementPartTryNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getTryBody().equals(that.getTryBody());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getTryBody());
        }
    }

    public static final class TryStatementNode implements Node {
        private final List<TryStatementPartNode> nodes;
        private final CodePosition pos;

        public  TryStatementNode(CodePosition pos, List<TryStatementPartNode> nodes) {
            this.nodes = nodes;
            this.pos = pos;
        }

        @Override
        public List<Node> getChildren() {
            return new ArrayList<>(nodes);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TRY_STATEMENT;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public List<TryStatementPartNode> getTryStatementPartNodes() {
            return nodes;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TryStatementNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Children: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TryStatementNode))
                return false;

            TryStatementNode that = (TryStatementNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    public static final class OperationNode implements Node {
        private final List<Node> nodes;
        private final Operator operator;
        private final OperatorType nodeType;
        private final CodePosition pos;

        /**
         * For ternary operator
         */
        public OperationNode(CodePosition pos, Node leftSideOperand, Node middleOperand, Node rightSideOperand, Operator operator, OperatorType nodeType) {
            this.pos = pos;

            if(!operator.isTernary())
                throw new IllegalStateException("Non ternary operator \"" + operator.getSymbol() + "\" must not have 3 operands");

            if(nodeType == null)
                nodeType = OperatorType.GENERAL;
            else if(!operator.getOperatorType().isCompatibleWith(nodeType))
                throw new IllegalStateException("Node type is not compatible with the operator");

            nodes = new ArrayList<>(3);
            nodes.add(leftSideOperand);
            nodes.add(middleOperand);
            nodes.add(rightSideOperand);

            this.operator = operator;
            this.nodeType = nodeType;
        }
        /**
         * For ternary operator
         */
        public OperationNode(Node leftSideOperand, Node middleOperand, Node rightSideOperand, Operator operator, OperatorType nodeType) {
            this(leftSideOperand.getPos().combine(rightSideOperand.getPos()), leftSideOperand, middleOperand, rightSideOperand, operator, nodeType);
        }

        /**
         * For binary operator
         */
        public OperationNode(CodePosition pos, Node leftSideOperand, Node rightSideOperand, Operator operator, OperatorType nodeType) {
            this.pos = pos;

            if(!operator.isBinary())
                throw new IllegalStateException("Non binary operator \"" + operator.getSymbol() + "\" must not have 2 operand");

            if(nodeType == null)
                nodeType = OperatorType.GENERAL;
            else if(!operator.getOperatorType().isCompatibleWith(nodeType))
                throw new IllegalStateException("Node type is not compatible with the operator");

            nodes = new ArrayList<>(2);
            nodes.add(leftSideOperand);
            nodes.add(rightSideOperand);

            this.operator = operator;
            this.nodeType = nodeType;
        }
        /**
         * For binary operator
         */
        public OperationNode(Node leftSideOperand, Node rightSideOperand, Operator operator, OperatorType nodeType) {
            this(leftSideOperand.getPos().combine(rightSideOperand.getPos()), leftSideOperand, rightSideOperand, operator, nodeType);
        }

        /**
         * For unary operator
         */
        public OperationNode(CodePosition pos, Node operand, Operator operator, OperatorType nodeType) {
            this.pos = pos;

            if(!operator.isUnary())
                throw new IllegalStateException("Non unary operator \"" + operator.getSymbol() + "\" must not have 1 operands");

            if(nodeType == null)
                nodeType = OperatorType.GENERAL;
            else if(!operator.getOperatorType().isCompatibleWith(nodeType))
                throw new IllegalStateException("Node type is not compatible with the operator");

            nodes = new ArrayList<>(1);
            nodes.add(operand);

            this.operator = operator;
            this.nodeType = nodeType;
        }
        /**
         * For unary operator
         */
        public OperationNode(Node operand, Operator operator, OperatorType nodeType) {
            this(operand.getPos(), operand, operator, nodeType);
        }

        @Override
        public List<Node> getChildren() {
            return nodes;
        }

        @Override
        public NodeType getNodeType() {
            switch(nodeType) {
                case ALL:
                case GENERAL:
                    return NodeType.OPERATION;
                case MATH:
                    return NodeType.MATH;
                case CONDITION:
                    return NodeType.CONDITION;
            }

            return NodeType.GENERAL;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public Node getLeftSideOperand() {
            return nodes.get(0);
        }

        public Node getMiddleOperand() {
            if(nodes.size() < 3)
                throw new IllegalStateException("Non ternary operator \"" + operator.getSymbol() + "\" has not 3 operand");

            return nodes.get(1);
        }

        public Node getRightSideOperand() {
            if(nodes.size() < 2)
                throw new IllegalStateException("Unary operator \"" + operator.getSymbol() + "\" has only 1 operand");

            return nodes.get(nodes.size() - 1);
        }

        public Operator getOperator() {
            return operator;
        }

        public OperatorType getOperatorType() {
            return operator.getOperatorType();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("OperationNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", NodeType: \"");
            builder.append(nodeType);
            builder.append("\", Operator: \"");
            builder.append(operator);
            builder.append("\", OperatorType: \"");
            builder.append(operator.getOperatorType());
            builder.append("\", Operands: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof OperationNode))
                return false;

            OperationNode that = (OperationNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.operator.equals(that.operator) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.operator, this.nodes);
        }

        public static enum Operator {
            //General
            NON                   ("",          1, -1,       OperatorType.GENERAL),
            LEN                   ("@",         1,  1,       OperatorType.GENERAL),
            DEEP_COPY             ("^",         1,  1,       OperatorType.GENERAL),
            CONCAT                ("|||",           5,       OperatorType.GENERAL),
            SPACESHIP             ("<=>",          10,       OperatorType.GENERAL),
            ELVIS                 ("?:",           13, true, OperatorType.GENERAL),
            NULL_COALESCING       ("??",           13, true, OperatorType.GENERAL),
            INLINE_IF             ("?...:",     3, 14, true, OperatorType.GENERAL),

            //Math
            MATH_NON              ("",          1, -1,       OperatorType.MATH),
            POW                   ("**",            2,       OperatorType.MATH),
            POS                   ("+",         1,  3,       OperatorType.MATH),
            INV                   ("-",         1,  3,       OperatorType.MATH),
            BITWISE_NOT           ("~",         1,  3,       OperatorType.MATH),
            INC                   ("+|",        1,  3,       OperatorType.MATH),
            DEC                   ("-|",        1,  3,       OperatorType.MATH),
            MUL                   ("*",             4,       OperatorType.MATH),
            DIV                   ("/",             4,       OperatorType.MATH),
            TRUNC_DIV             ("~/",            4,       OperatorType.MATH),
            FLOOR_DIV             ("//",            4,       OperatorType.MATH),
            CEIL_DIV              ("^/",            4,       OperatorType.MATH),
            MOD                   ("%",             4,       OperatorType.MATH),
            ADD                   ("+",             5,       OperatorType.MATH),
            SUB                   ("-",             5,       OperatorType.MATH),
            LSHIFT                ("<<",            6,       OperatorType.MATH),
            RSHIFT                (">>",            6,       OperatorType.MATH),
            RZSHIFT               (">>>",           6,       OperatorType.MATH),
            BITWISE_AND           ("&",             7,       OperatorType.MATH),
            BITWISE_XOR           ("^",             8,       OperatorType.MATH),
            BITWISE_OR            ("|",             9,       OperatorType.MATH),

            //Condition
            CONDITIONAL_NON       ("",          1, -1,       OperatorType.CONDITION),
            NOT                   ("!",         1,  3,       OperatorType.CONDITION),
            INSTANCE_OF           ("~~",           10,       OperatorType.CONDITION),
            EQUALS                ("==",           10,       OperatorType.CONDITION),
            NOT_EQUALS            ("!=",           10,       OperatorType.CONDITION),
            MATCHES               ("=~",           10,       OperatorType.CONDITION),
            NOT_MATCHES           ("!=~",          10,       OperatorType.CONDITION),
            STRICT_EQUALS         ("===",          10,       OperatorType.CONDITION),
            STRICT_NOT_EQUALS     ("!==",          10,       OperatorType.CONDITION),
            LESS_THAN             ("<",            10,       OperatorType.CONDITION),
            GREATER_THAN          (">",            10,       OperatorType.CONDITION),
            LESS_THAN_OR_EQUALS   ("<=",           10,       OperatorType.CONDITION),
            GREATER_THAN_OR_EQUALS(">=",           10,       OperatorType.CONDITION),
            AND                   ("&&",           11, true, OperatorType.CONDITION),
            OR                    ("||",           12, true, OperatorType.CONDITION),

            //ALL
            /**
             * COMMA is a parser-only operator (This operator can only be used during parsing)
             */
            COMMA                 (",",            15,       OperatorType.ALL),
            GET_ITEM              ("[...]",         0,       OperatorType.ALL),
            OPTIONAL_GET_ITEM     ("?.[...]",       0,       OperatorType.ALL),
            SLICE                 ("[...:...]",   3, 0,       OperatorType.ALL),
            OPTIONAL_SLICE        ("?.[...:...]", 3, 0,       OperatorType.ALL),
            MEMBER_ACCESS         ("::",            0, true, OperatorType.ALL),
            /**
             * MEMBER_ACCESS operator where composite type is "&amp;this"
             */
            MEMBER_ACCESS_THIS    ("::",        1,  0, true, OperatorType.ALL),
            OPTIONAL_MEMBER_ACCESS("?::",           0, true, OperatorType.ALL),
            MEMBER_ACCESS_POINTER ("->",            0, true, OperatorType.ALL);

            private final String symbol;
            private final int arity;
            private final int precedence;
            private final boolean lazyEvaluation;
            private final OperatorType operatorType;

            private Operator(String symbol, int arity, int precedence, boolean lazyEvaluation, OperatorType operatorType) {
                this.symbol = symbol;
                this.arity = arity;
                this.precedence = precedence;
                this.lazyEvaluation = lazyEvaluation;
                this.operatorType = operatorType;
            }
            private Operator(String symbol, int arity, int precedence, OperatorType operatorType) {
                this(symbol, arity, precedence, false, operatorType);
            }
            private Operator(String symbol, int precedence, boolean lazyEvaluation, OperatorType operatorType) {
                this(symbol, 2, precedence, lazyEvaluation, operatorType);
            }
            private Operator(String symbol, int precedence, OperatorType operatorType) {
                this(symbol, 2, precedence, false, operatorType);
            }

            public String getSymbol() {
                return symbol;
            }

            public boolean isUnary() {
                return arity == 1;
            }

            public boolean isBinary() {
                return arity == 2;
            }

            public boolean isTernary() {
                return arity == 3;
            }

            public int getPrecedence() {
                return precedence;
            }

            public boolean isLazyEvaluation() {
                return lazyEvaluation;
            }

            public OperatorType getOperatorType() {
                return operatorType;
            }
        }

        public static enum OperatorType {
            ALL, GENERAL, MATH, CONDITION;

            public boolean isCompatibleWith(OperatorType type) {
                return this == ALL || type == GENERAL || type == this;
            }
        }
    }

    public static final class ReturnNode implements Node {
        private final List<Node> nodes;
        private final CodePosition pos;

        public ReturnNode(CodePosition pos, Node returnValue) {
            nodes = new ArrayList<>(1);
            nodes.add(returnValue);
            this.pos = pos;
        }
        public ReturnNode(Node returnValue) {
            this(returnValue.getPos(), returnValue);
        }

        public ReturnNode(CodePosition pos) {
            nodes = new ArrayList<>(0);
            this.pos = pos;
        }

        @Override
        public List<Node> getChildren() {
            return nodes;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.RETURN;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        /**
         * @return Returns null for return without return value
         */
        public Node getReturnValue() {
            if(nodes.size() == 1)
                return nodes.get(0);

            return null;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ReturnNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Children: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ReturnNode))
                return false;

            ReturnNode that = (ReturnNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    public static final class ThrowNode implements Node {
        private final Node throwValue;
        private final Node message;
        private final CodePosition pos;

        public ThrowNode(CodePosition pos, Node throwValue, Node messageValue) {
            this.throwValue = throwValue;
            this.message = messageValue;
            this.pos = pos;
        }
        public ThrowNode(Node throwValue, Node messageValue) {
            this(messageValue == null?throwValue.getPos():throwValue.getPos().combine(messageValue.getPos()), throwValue, messageValue);
        }

        public ThrowNode(CodePosition pos, Node throwValue) {
            this.throwValue = throwValue;
            this.message = null;
            this.pos = pos;
        }
        public ThrowNode(Node throwValue) {
            this(throwValue.getPos(), throwValue);
        }

        @Override
        public List<Node> getChildren() {
            if(message == null)
                return Arrays.asList(throwValue);

            return Arrays.asList(throwValue, message);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.THROW;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        public Node getThrowValue() {
            return throwValue;
        }

        public Node getMessage() {
            return message;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ThrowNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", ThrowValue: {\n");
            String[] tokens = throwValue.toString().split("\\n");
            for(String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}, Message: ");
            if(message == null) {
                builder.append("null");
            }else {
                builder.append("{\n");
                tokens = throwValue.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
                builder.append("}\n");
            }

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ReturnNode))
                return false;

            ReturnNode that = (ReturnNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.getChildren().equals(that.getChildren());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.getChildren());
        }
    }

    //Is only super class for other nodes
    public static abstract class ValueNode extends ChildlessNode {
        public ValueNode(CodePosition pos) {
            super(pos);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.GENERAL;
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ValueNode))
                return false;

            ValueNode that = (ValueNode)obj;
            return this.getNodeType().equals(that.getNodeType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType());
        }
    }

    public static final class IntValueNode extends ValueNode {
        private final int i;

        public IntValueNode(CodePosition pos, int i) {
            super(pos);

            this.i = i;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.INT_VALUE;
        }

        public int getInt() {
            return i;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("IntValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Value: \"");
            builder.append(i);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof IntValueNode))
                return false;

            IntValueNode that = (IntValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.i == that.i;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), i);
        }
    }

    public static final class LongValueNode extends ValueNode {
        private final long l;

        public LongValueNode(CodePosition pos, long l) {
            super(pos);

            this.l = l;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.LONG_VALUE;
        }

        public long getLong() {
            return l;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("LongValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Value: \"");
            builder.append(l);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof LongValueNode))
                return false;

            LongValueNode that = (LongValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.l == that.l;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), l);
        }
    }

    public static final class FloatValueNode extends ValueNode {
        private final float f;

        public FloatValueNode(CodePosition pos, float f) {
            super(pos);

            this.f = f;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.FLOAT_VALUE;
        }

        public float getFloat() {
            return f;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FloatValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Value: \"");
            builder.append(f);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof FloatValueNode))
                return false;

            FloatValueNode that = (FloatValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.f == that.f;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), f);
        }
    }

    public static final class DoubleValueNode extends ValueNode {
        private final double d;

        public DoubleValueNode(CodePosition pos, double d) {
            super(pos);

            this.d = d;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.DOUBLE_VALUE;
        }

        public double getDouble() {
            return d;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("DoubleValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Value: \"");
            builder.append(d);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof DoubleValueNode))
                return false;

            DoubleValueNode that = (DoubleValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.d == that.d;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), d);
        }
    }

    public static final class CharValueNode extends ValueNode {
        private final int c;

        public CharValueNode(CodePosition pos, int c) {
            super(pos);

            this.c = c;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.CHAR_VALUE;
        }

        public int getChar() {
            return c;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("CharValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Value: \"");
            builder.appendCodePoint(c);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof CharValueNode))
                return false;

            CharValueNode that = (CharValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.c == that.c;
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), c);
        }
    }

    public static final class TextValueNode extends ValueNode {
        private final String text;

        public TextValueNode(CodePosition pos, String text) {
            super(pos);

            this.text = text;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.TEXT_VALUE;
        }

        public String getText() {
            return text;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("TextValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Value: \"");
            builder.append(text);
            builder.append("\"\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof TextValueNode))
                return false;

            TextValueNode that = (TextValueNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.text.equals(that.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), text);
        }
    }

    public static final class NullValueNode extends ValueNode {
        public NullValueNode(CodePosition pos) {
            super(pos);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.NULL_VALUE;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("NullValueNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append("\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof NullValueNode))
                return false;

            NullValueNode that = (NullValueNode)obj;
            return this.getNodeType().equals(that.getNodeType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType());
        }
    }

    public static final class VoidValueNode extends ValueNode {
        public VoidValueNode(CodePosition pos) {
            super(pos);
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.VOID_VALUE;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("VoidValueNode: Position");
            builder.append(pos.toCompactString());
            builder.append("\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof VoidValueNode))
                return false;

            VoidValueNode that = (VoidValueNode)obj;
            return this.getNodeType().equals(that.getNodeType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType());
        }
    }

    public static final class ArrayNode implements Node {
        private final List<Node> nodes;
        private final CodePosition pos;

        public ArrayNode(CodePosition pos, List<Node> nodes) {
            this.nodes = new ArrayList<>(nodes);

            this.pos = pos;
        }
        public ArrayNode(List<Node> nodes) {
            this(nodes.isEmpty()?CodePosition.EMPTY:nodes.get(0).getPos().combine(
                    nodes.get(nodes.size() - 1).getPos()), nodes);
        }

        @Override
        public List<Node> getChildren() {
            return nodes;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.ARRAY;
        }

        @Override
        public CodePosition getPos() {
            return pos;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ArrayNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", Elements: {\n");
            nodes.forEach(node -> {
                String[] tokens = node.toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t");
                    builder.append(token);
                    builder.append("\n");
                }
            });
            builder.append("}\n");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ArrayNode))
                return false;

            ArrayNode that = (ArrayNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && this.nodes.equals(that.nodes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.nodes);
        }
    }

    public static final class StructDefinitionNode extends ChildlessNode {
        private final String structName;

        private final List<String> memberNames;
        private final List<String> typeConstraints;

        public StructDefinitionNode(CodePosition pos, String structName, List<String> memberNames,
                                    List<String> typeConstraints) {
            super(pos);

            this.structName = structName;

            this.memberNames = memberNames;
            this.typeConstraints = typeConstraints;
        }

        public String getStructName() {
            return structName;
        }

        public List<String> getMemberNames() {
            return memberNames;
        }

        public List<String> getTypeConstraints() {
            return typeConstraints;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.STRUCT_DEFINITION;
        }

        @Override
        public String toString() {
            if(memberNames.size() != typeConstraints.size())
                return "StructDefinitionNode: <INVALID>";

            StringBuilder builder = new StringBuilder();
            builder.append("StructDefinitionNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", StructName: \"");
            builder.append(structName);
            builder.append("\", Members{TypeConstraints}: {\n");
            for(int i = 0;i < memberNames.size();i++) {
                builder.append("\t");
                builder.append(memberNames.get(i));
                if(typeConstraints.get(i) != null) {
                    builder.append("{");
                    builder.append(typeConstraints.get(i));
                    builder.append("}");
                }
                builder.append("\n");
            }
            builder.append("}");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof StructDefinitionNode))
                return false;

            StructDefinitionNode that = (StructDefinitionNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && Objects.equals(this.structName, that.structName) &&
                    this.memberNames.equals(that.memberNames) && this.typeConstraints.equals(that.typeConstraints);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.structName, this.memberNames, this.typeConstraints);
        }
    }

    public static final class ClassDefinitionNode extends ChildlessNode {
        private final String className;

        private final List<String> staticMemberNames;
        private final List<String> staticMemberTypeConstraints;
        private final List<Node> staticMemberValues;
        private final List<Boolean> staticMemberFinalFlag;
        private final List<Visibility> staticMemberVisibility;

        private final List<String> memberNames;
        private final List<String> memberTypeConstraints;
        private final List<Boolean> memberFinalFlag;
        private final List<Visibility> memberVisibility;

        /**
         * If multiple methods have the same name, they are overloaded
         */
        private final List<String> methodNames;
        private final List<Node> methodDefinitions;
        private final List<Boolean> methodOverrideFlag;
        private final List<Visibility> methodVisibility;

        private final List<Node> constructorDefinitions;
        private final List<Visibility> constructorVisibility;

        /**
         * List of parent nodes separated by ArgumentSeparator nodes
         */
        private final List<AbstractSyntaxTree.Node> parentClasses;

        public ClassDefinitionNode(CodePosition pos, String className, List<String> staticMemberNames,
                                   List<String> staticMemberTypeConstraints, List<Node> staticMemberValues,
                                   List<Boolean> staticMemberFinalFlag, List<Visibility> staticMemberVisibility,
                                   List<String> memberNames, List<String> memberTypeConstraints,
                                   List<Boolean> memberFinalFlag, List<Visibility> memberVisibility,
                                   List<String> methodNames, List<Node> methodDefinitions, List<Boolean> methodOverrideFlag,
                                   List<Visibility> methodVisibility, List<Node> constructorDefinitions,
                                   List<Visibility> constructorVisibility, List<Node> parentClasses) {
            super(pos);

            this.className = className;

            this.staticMemberNames = staticMemberNames;
            this.staticMemberTypeConstraints = staticMemberTypeConstraints;
            this.staticMemberValues = staticMemberValues;
            this.staticMemberFinalFlag = staticMemberFinalFlag;
            this.staticMemberVisibility = staticMemberVisibility;

            this.memberNames = memberNames;
            this.memberTypeConstraints = memberTypeConstraints;
            this.memberFinalFlag = memberFinalFlag;
            this.memberVisibility = memberVisibility;

            this.methodNames = methodNames;
            this.methodDefinitions = methodDefinitions;
            this.methodOverrideFlag = methodOverrideFlag;
            this.methodVisibility = methodVisibility;

            this.constructorDefinitions = constructorDefinitions;
            this.constructorVisibility = constructorVisibility;

            this.parentClasses = parentClasses;
        }

        public String getClassName() {
            return className;
        }

        public List<String> getStaticMemberNames() {
            return staticMemberNames;
        }

        public List<String> getStaticMemberTypeConstraints() {
            return staticMemberTypeConstraints;
        }

        public List<Node> getStaticMemberValues() {
            return staticMemberValues;
        }

        public List<Boolean> getStaticMemberFinalFlag() {
            return staticMemberFinalFlag;
        }

        public List<Visibility> getStaticMemberVisibility() {
            return staticMemberVisibility;
        }

        public List<String> getMemberNames() {
            return memberNames;
        }

        public List<String> getMemberTypeConstraints() {
            return memberTypeConstraints;
        }

        public List<Boolean> getMemberFinalFlag() {
            return memberFinalFlag;
        }

        public List<Visibility> getMemberVisibility() {
            return memberVisibility;
        }

        public List<String> getMethodNames() {
            return methodNames;
        }

        public List<Node> getMethodDefinitions() {
            return methodDefinitions;
        }

        public List<Boolean> getMethodOverrideFlag() {
            return methodOverrideFlag;
        }

        public List<Visibility> getMethodVisibility() {
            return methodVisibility;
        }

        public List<Node> getConstructorDefinitions() {
            return constructorDefinitions;
        }

        public List<Visibility> getConstructorVisibility() {
            return constructorVisibility;
        }

        public List<AbstractSyntaxTree.Node> getParentClasses() {
            return parentClasses;
        }

        @Override
        public NodeType getNodeType() {
            return NodeType.CLASS_DEFINITION;
        }

        @Override
        public String toString() {
            if(staticMemberNames.size() != staticMemberTypeConstraints.size() ||
                    staticMemberNames.size() != staticMemberValues.size() ||
                    staticMemberNames.size() != staticMemberFinalFlag.size() ||
                    staticMemberNames.size() != staticMemberVisibility.size() ||

                    memberNames.size() != memberTypeConstraints.size() ||
                    memberNames.size() != memberFinalFlag.size() ||
                    memberNames.size() != memberVisibility.size() ||

                    methodNames.size() != methodDefinitions.size() ||
                    methodNames.size() != methodOverrideFlag.size() ||
                    methodNames.size() != methodVisibility.size() ||

                    constructorDefinitions.size() != constructorVisibility.size())
                return "ClassDefinitionNode: <INVALID>";

            StringBuilder builder = new StringBuilder();
            builder.append("ClassDefinitionNode: Position: ");
            builder.append(pos.toCompactString());
            builder.append(", ClassName: \"");
            builder.append(className);
            builder.append("\", StaticMembers{TypeConstraints} = <value>: {\n");
            for(int i = 0;i < staticMemberNames.size();i++) {
                builder.append("\t");
                builder.append(staticMemberVisibility.get(i).getVisibilitySymbol());
                builder.append(staticMemberFinalFlag.get(i)?"final:":"");
                builder.append(staticMemberNames.get(i));
                if(staticMemberTypeConstraints.get(i) != null) {
                    builder.append("{");
                    builder.append(staticMemberTypeConstraints.get(i));
                    builder.append("}");
                }
                if(staticMemberValues.get(i) != null) {
                    builder.append(" = {\n");
                    String[] tokens = staticMemberValues.get(i).toString().split("\\n");
                    for(String token:tokens) {
                        builder.append("\t\t");
                        builder.append(token);
                        builder.append("\n");
                    }
                    builder.append("\t}");
                }
                builder.append("\n");
            }
            builder.append("}, Members{TypeConstraints}: {\n");
            for(int i = 0;i < memberNames.size();i++) {
                builder.append("\t");
                builder.append(memberVisibility.get(i).getVisibilitySymbol());
                builder.append(memberFinalFlag.get(i)?"final:":"");
                builder.append(memberNames.get(i));
                if(memberTypeConstraints.get(i) != null) {
                    builder.append("{");
                    builder.append(memberTypeConstraints.get(i));
                    builder.append("}");
                }
                builder.append("\n");
            }
            builder.append("}, MethodName = <definition>: {\n");
            for(int i = 0;i < methodNames.size();i++) {
                builder.append("\t");
                builder.append(methodVisibility.get(i).getVisibilitySymbol());
                builder.append(methodOverrideFlag.get(i)?"override:":"");
                builder.append(methodNames.get(i));
                builder.append(" = {\n");
                String[] tokens = methodDefinitions.get(i).toString().split("\\n");
                for(String token:tokens) {
                    builder.append("\t\t");
                    builder.append(token);
                    builder.append("\n");
                }
                builder.append("\t}\n");
            }
            builder.append("}, Constructors: {\n");
            for(int i = 0;i < constructorDefinitions.size();i++) {
                builder.append("\t");
                builder.append(constructorVisibility.get(i).getVisibilitySymbol());
                builder.append("construct = {\n");
                String[] tokens = constructorDefinitions.get(i).toString().split("\\n");
                for (String token:tokens) {
                    builder.append("\t\t");
                    builder.append(token);
                    builder.append("\n");
                }
                builder.append("\t}\n");
            }
            builder.append("}, ParentClasses: {\n");
            String[] tokens = parentClasses.toString().split("\\n");
            for (String token:tokens) {
                builder.append("\t");
                builder.append(token);
                builder.append("\n");
            }
            builder.append("}");

            return builder.toString();
        }

        @Override
        public boolean equals(Object obj) {
            if(this == obj)
                return true;

            if(obj == null)
                return false;

            if(!(obj instanceof ClassDefinitionNode))
                return false;

            ClassDefinitionNode that = (ClassDefinitionNode)obj;
            return this.getNodeType().equals(that.getNodeType()) && Objects.equals(this.className, that.className) &&

                    this.staticMemberNames.equals(that.staticMemberNames) &&
                    this.staticMemberTypeConstraints.equals(that.staticMemberTypeConstraints) &&
                    this.staticMemberFinalFlag.equals(that.staticMemberFinalFlag) &&
                    this.staticMemberValues.equals(that.staticMemberValues) &&
                    this.staticMemberVisibility.equals(that.staticMemberVisibility) &&

                    this.memberNames.equals(that.memberNames) &&
                    this.memberTypeConstraints.equals(that.memberTypeConstraints) &&
                    this.memberFinalFlag.equals(that.memberFinalFlag) &&
                    this.memberVisibility.equals(that.memberVisibility) &&

                    this.methodNames.equals(that.methodNames) &&
                    this.methodDefinitions.equals(that.methodDefinitions) &&
                    this.methodOverrideFlag.equals(that.methodOverrideFlag) &&
                    this.methodVisibility.equals(that.methodVisibility) &&

                    this.constructorDefinitions.equals(that.constructorDefinitions) &&
                    this.constructorVisibility.equals(that.constructorVisibility) &&

                    this.parentClasses.equals(that.parentClasses);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.getNodeType(), this.className, this.staticMemberNames, this.staticMemberTypeConstraints,
                    this.staticMemberFinalFlag, this.staticMemberValues, this.staticMemberVisibility, this.memberNames,
                    this.memberTypeConstraints, this.memberFinalFlag, this.memberVisibility, this.methodNames, this.methodDefinitions,
                    this.methodOverrideFlag, this.methodVisibility, this.constructorDefinitions, this.constructorVisibility,
                    this.parentClasses);
        }

        public enum Visibility {
            PRIVATE  ('-', "private"),
            PROTECTED('~', "protected"),
            PUBLIC   ('+', "public");

            private final char visibilitySymbol;
            private final String visibilityKeyword;

            Visibility(char visibilitySymbol, String visibilityKeyword) {
                this.visibilitySymbol = visibilitySymbol;
                this.visibilityKeyword = visibilityKeyword;
            }

            public static Visibility fromSymbol(char visibilitySymbol) {
                for(Visibility visibility:Visibility.values())
                    if(visibility.getVisibilitySymbol() == visibilitySymbol)
                        return visibility;

                return null;
            }

            public static Visibility fromKeyword(String visibilityKeyword) {
                for(Visibility visibility:Visibility.values())
                    if(visibility.getVisibilityKeyword().equals(visibilityKeyword))
                        return visibility;

                return null;
            }

            public char getVisibilitySymbol() {
                return visibilitySymbol;
            }

            public String getVisibilityKeyword() {
                return visibilityKeyword;
            }
        }
    }

    public static enum NodeType {
        GENERAL, LIST, PARSING_ERROR, ASSIGNMENT, ESCAPE_SEQUENCE, UNPROCESSED_VARIABLE_NAME, VARIABLE_NAME, ARGUMENT_SEPARATOR,
        FUNCTION_CALL, FUNCTION_CALL_PREVIOUS_NODE_VALUE, FUNCTION_DEFINITION, CONDITION, IF_STATEMENT_PART_IF, IF_STATEMENT_PART_ELSE,
        IF_STATEMENT, LOOP_STATEMENT_PART_LOOP, LOOP_STATEMENT_PART_WHILE, LOOP_STATEMENT_PART_UNTIL, LOOP_STATEMENT_PART_REPEAT, LOOP_STATEMENT_PART_FOR_EACH,
        LOOP_STATEMENT_PART_ELSE, LOOP_STATEMENT, LOOP_STATEMENT_CONTINUE_BREAK, TRY_STATEMENT_PART_TRY, TRY_STATEMENT_PART_SOFT_TRY, TRY_STATEMENT_PART_NON_TRY,
        TRY_STATEMENT_PART_CATCH, TRY_STATEMENT_PART_ELSE, TRY_STATEMENT_PART_FINALLY, TRY_STATEMENT, MATH, OPERATION, RETURN, THROW, INT_VALUE, LONG_VALUE,
        FLOAT_VALUE, DOUBLE_VALUE, CHAR_VALUE, TEXT_VALUE, NULL_VALUE, VOID_VALUE, ARRAY, STRUCT_DEFINITION, CLASS_DEFINITION;
    }
}