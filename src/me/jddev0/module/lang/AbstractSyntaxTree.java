package me.jddev0.module.lang;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import me.jddev0.module.lang.LangParser.ParsingError;

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
		
		@Override
		public default Iterator<AbstractSyntaxTree.Node> iterator() {
			return getChildren().iterator();
		}
	}
	
	public static final class ListNode implements Node {
		private final List<Node> nodes;
		
		public ListNode(List<Node> nodes) {
			this.nodes = new ArrayList<>(nodes);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ListNode: Children: {\n");
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
			return this.getNodeType().equals(that.getNodeType()) && that.nodes.equals(that.nodes);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.nodes);
		}
	}
	
	//Is only super class for other nodes
	public static class ChildlessNode implements Node {
		private final List<Node> nodes;
		
		public ChildlessNode() {
			this.nodes = new ArrayList<>(0);
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
			
			if(!(obj instanceof ChildlessNode))
				return false;
			
			ListNode that = (ListNode)obj;
			return this.getNodeType().equals(that.getNodeType()) && that.nodes.equals(that.nodes);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.nodes);
		}
	}
	
	public static final class ParsingErrorNode extends ChildlessNode {
		private final ParsingError error;
		private final String message;

		public ParsingErrorNode(ParsingError error, String message) {
			this.error = error;
			this.message = message;
		}
		public ParsingErrorNode(ParsingError error) {
			this(error, null);
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
			builder.append("ParsingErrorNode: Error: \"");
			builder.append(error);
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
		
		public AssignmentNode(Node lvalue, Node rvalue) {
			nodes = new ArrayList<>(2);
			nodes.add(lvalue);
			nodes.add(rvalue);
		}
		
		@Override
		public List<Node> getChildren() {
			return nodes;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.ASSIGNMENT;
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
			builder.append("AssignmentNode: lvalue: {\n");
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
			return this.getNodeType().equals(that.getNodeType()) && that.nodes.equals(that.nodes);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.nodes);
		}
	}
	
	public static final class EscapeSequenceNode extends ChildlessNode {
		private final char c;
		
		public EscapeSequenceNode(char c) {
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
			builder.append("EscapeSequenceNode: Char: \"");
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
		
		public UnprocessedVariableNameNode(String variableName) {
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
			builder.append("UnprocessedVariableNameNode: VariableName: \"");
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
		
		public VariableNameNode(String variableName) {
			this.variableName = variableName;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.VARIABLE_NAME;
		}
		
		public String getVariableName() {
			return variableName;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("VariableNameNode: VariableName: \"");
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
			
			if(!(obj instanceof VariableNameNode))
				return false;
			
			VariableNameNode that = (VariableNameNode)obj;
			return this.getNodeType().equals(that.getNodeType()) && this.variableName.equals(that.variableName);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.variableName);
		}
	}
	
	/**
	 * Will store an "," with whitespaces if needed
	 */
	public static final class ArgumentSeparatorNode extends ChildlessNode {
		private final String originalText;
		
		public ArgumentSeparatorNode(String originalText) {
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
			builder.append("ArgumentSeparatorNode: OriginalText: \"");
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
			return this.getNodeType().equals(that.getNodeType()) && this.originalText.equals(originalText);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), originalText);
		}
	}
	
	public static final class FunctionCallNode implements Node {
		private final List<Node> argumentList;
		private final String functionName;
		
		public FunctionCallNode(List<Node> argumentList, String functionName) {
			this.argumentList = new ArrayList<>(argumentList);
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
		
		public String getFunctionName() {
			return functionName;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FunctionCallNode: FunctionName: \"");
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
		private final List<Node> argumentList;
		
		public FunctionCallPreviousNodeValueNode(List<Node> argumentList) {
			this.argumentList = new ArrayList<>(argumentList);
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
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FunctionCallPreviousNodeValueNode: ArgumentList: {\n");
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
			
			if(!(obj instanceof FunctionCallPreviousNodeValueNode))
				return false;
			
			FunctionCallPreviousNodeValueNode that = (FunctionCallPreviousNodeValueNode)obj;
			return this.getNodeType().equals(that.getNodeType()) && this.argumentList.equals(that.argumentList);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.argumentList);
		}
	}
	
	public static final class FunctionDefinitionNode implements Node {
		private final List<Node> parameterList;
		private final AbstractSyntaxTree functionBody;
		
		public FunctionDefinitionNode(List<Node> parameterList, AbstractSyntaxTree functionBody) {
			this.parameterList = new ArrayList<>(parameterList);
			this.functionBody = functionBody;
		}
		
		@Override
		public List<Node> getChildren() {
			return parameterList;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.FUNCTION_DEFINITION;
		}
		
		public AbstractSyntaxTree getFunctionBody() {
			return functionBody;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("FunctionDefinitionNode: ParameterList: {\n");
			parameterList.forEach(node -> {
				String[] tokens = node.toString().split("\\n");
				for(String token:tokens) {
					builder.append("\t");
					builder.append(token);
					builder.append("\n");
				}
			});
			builder.append("}, FunctionBody: {\n");
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
			return this.getNodeType().equals(that.getNodeType()) && this.parameterList.equals(that.parameterList) && this.functionBody.equals(that.functionBody);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.parameterList, this.functionBody);
		}
	}
	
	public static final class ConditionNode implements Node {
		private final List<Node> nodes;
		private final Operator operator;
		
		/**
		 * For binary operators
		 */
		public ConditionNode(Node leftSideOperand, Node rightSideOperand, Operator operator) {
			if(operator.isUnary())
				throw new IllegalStateException("Unary operator \"" + operator.getSymbol() + "\" can only have one operand");
			
			nodes = new ArrayList<>(2);
			nodes.add(leftSideOperand);
			nodes.add(rightSideOperand);
			
			this.operator = operator;
		}
		
		/**
		 * For unary operators
		 */
		public ConditionNode(Node operand, Operator operator) {
			if(!operator.isUnary())
				throw new IllegalStateException("Binary operator \"" + operator.getSymbol() + "\" must have two operands");
			
			nodes = new ArrayList<>(1);
			nodes.add(operand);
			
			this.operator = operator;
		}
		
		@Override
		public List<Node> getChildren() {
			return nodes;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.CONDITION;
		}
		
		public Node getLeftSideOperand() {
			return nodes.get(0);
		}
		
		public Node getRightSideOperand() {
			if(nodes.size() < 2)
				throw new IllegalStateException("Unary operator \"" + operator.getSymbol() + "\" has only one operand");
			
			return nodes.get(1);
		}
		
		public Operator getOperator() {
			return operator;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ConditionNode: Operator: \"");
			builder.append(operator);
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
			
			if(!(obj instanceof ConditionNode))
				return false;
			
			ConditionNode that = (ConditionNode)obj;
			return this.getNodeType().equals(that.getNodeType()) && this.operator.equals(that.operator) && this.nodes.equals(that.nodes);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.operator, this.nodes);
		}
		
		public static enum Operator {
			NON("", true, -1), NOT("!", true, 1), AND("&&", 3), OR("||", 4), EQUALS("==", 2), NOT_EQUALS("!=", 2), STRICT_EQUALS("===", 2), STRICT_NOT_EQUALS("!==", 2),
			LESS_THAN("<", 2), GREATER_THAN(">", 2), LESS_THAN_OR_EQUALS("<=", 2), GREATER_THAN_OR_EQUALS(">=", 2);
			
			private final String symbol;
			private final boolean unary;
			private final int precedence;
			
			private Operator(String symbol, boolean onlyOneNode, int precedence) {
				this.symbol = symbol;
				this.unary = onlyOneNode;
				this.precedence = precedence;
			}
			private Operator(String symbol, int precedence) {
				this(symbol, false, precedence);
			}
			
			public String getSymbol() {
				return symbol;
			}
			
			public boolean isUnary() {
				return unary;
			}
			
			public int getPrecedence() {
				return precedence;
			}
		}
	}
	
	//Is only super class for other nodes
	public static class IfStatementPartNode extends ChildlessNode {
		private final AbstractSyntaxTree ifBody;
		
		public IfStatementPartNode(AbstractSyntaxTree ifBody) {
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
		private final ConditionNode condition;
		
		public IfStatementPartIfNode(AbstractSyntaxTree ifBody, ConditionNode condition) {
			super(ifBody);
			
			this.condition = condition;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.IF_STATEMENT_PART_IF;
		}
		
		public ConditionNode getCondition() {
			return condition;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("IfStatementPartIfNode: Condition: {\n");
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
		public IfStatementPartElseNode(AbstractSyntaxTree ifBody) {
			super(ifBody);
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.IF_STATEMENT_PART_ELSE;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("IfStatementPartElseNode: IfBody: {\n");
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
		
		public IfStatementNode(List<IfStatementPartNode> nodes) {
			this.nodes = nodes;
		}
		
		@Override
		public List<Node> getChildren() {
			return new ArrayList<>(nodes);
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.IF_STATEMENT;
		}
		
		public List<IfStatementPartNode> getIfStatementPartNodes() {
			return nodes;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("IfStatementNode: Children: {\n");
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
	public static class LoopStatementPartNode extends ChildlessNode {
		private final AbstractSyntaxTree loopBody;
		
		public LoopStatementPartNode(AbstractSyntaxTree loopBody) {
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
		public LoopStatementPartLoopNode(AbstractSyntaxTree loopBody) {
			super(loopBody);
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.LOOP_STATEMENT_PART_LOOP;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoopStatementPartLoopNode: LoopBody: {\n");
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
		private final ConditionNode condition;
		
		public LoopStatementPartWhileNode(AbstractSyntaxTree loopBody, ConditionNode condition) {
			super(loopBody);
			
			this.condition = condition;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.LOOP_STATEMENT_PART_WHILE;
		}
		
		public ConditionNode getCondition() {
			return condition;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoopStatementPartWhileNode: Condition: {\n");
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
		private final ConditionNode condition;
		
		public LoopStatementPartUntilNode(AbstractSyntaxTree loopBody, ConditionNode condition) {
			super(loopBody);
			
			this.condition = condition;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.LOOP_STATEMENT_PART_UNTIL;
		}
		
		public ConditionNode getCondition() {
			return condition;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoopStatementPartUntilNode: Condition: {\n");
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
		
		public LoopStatementPartRepeatNode(AbstractSyntaxTree loopBody, Node varPointerNode, Node repeatCountNode) {
			super(loopBody);
			
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
			builder.append("LoopStatementPartRepeatNode: varPointer: {\n");
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
		private final Node arrayOrTextNode;
		
		public LoopStatementPartForEachNode(AbstractSyntaxTree loopBody, Node varPointerNode, Node arrayOrTextNode) {
			super(loopBody);
			
			this.varPointerNode = varPointerNode;
			this.arrayOrTextNode = arrayOrTextNode;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.LOOP_STATEMENT_PART_FOR_EACH;
		}
		
		public Node getVarPointerNode() {
			return varPointerNode;
		}
		
		public Node getArrayOrTextNode() {
			return arrayOrTextNode;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoopStatementPartRepeatNode: varPointer: {\n");
			String[] tokens = varPointerNode.toString().split("\\n");
			for(String token:tokens) {
				builder.append("\t");
				builder.append(token);
				builder.append("\n");
			}
			builder.append("}, arrayOrTextNode: {\n");
			tokens = arrayOrTextNode.toString().split("\\n");
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
			return this.getNodeType().equals(that.getNodeType()) && this.varPointerNode.equals(that.varPointerNode) && this.arrayOrTextNode.equals(that.arrayOrTextNode) &&
					this.getLoopBody().equals(that.getLoopBody());
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.varPointerNode, this.arrayOrTextNode, this.getLoopBody());
		}
	}
	
	public static final class LoopStatementPartElseNode extends LoopStatementPartNode {
		public LoopStatementPartElseNode(AbstractSyntaxTree loopBody) {
			super(loopBody);
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.LOOP_STATEMENT_PART_ELSE;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoopStatementPartElseNode: LoopBody: {\n");
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
		
		public LoopStatementNode(List<LoopStatementPartNode> nodes) {
			this.nodes = nodes;
		}
		
		@Override
		public List<Node> getChildren() {
			return new ArrayList<>(nodes);
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.LOOP_STATEMENT;
		}
		
		public List<LoopStatementPartNode> getLoopStatementPartNodes() {
			return nodes;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("LoopStatementNode: Children: {\n");
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
		
		public LoopStatementContinueBreakStatement(Node numberNode, boolean continueNode) {
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
			builder.append("ConditionalContinueBreakStatementNode: numberNode: {\n");
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
	

	public static final class MathNode implements Node {
		private final List<Node> nodes;
		private final Operator operator;
		
		/**
		 * For binary operator
		 */
		public MathNode(Node leftSideOperand, Node rightSideOperand, Operator operator) {
			if(operator.isUnary())
				throw new IllegalStateException("Unary operator \"" + operator.getSymbol() + "\" can only have one operand");
			
			nodes = new ArrayList<>(2);
			nodes.add(leftSideOperand);
			nodes.add(rightSideOperand);
			
			this.operator = operator;
		}
		
		/**
		 * For unary operator
		 */
		public MathNode(Node operand, Operator operator) {
			if(!operator.isUnary())
				throw new IllegalStateException("Binary operator \"" + operator.getSymbol() + "\" must have two operands");
			
			nodes = new ArrayList<>(1);
			nodes.add(operand);
			
			this.operator = operator;
		}
		
		@Override
		public List<Node> getChildren() {
			return nodes;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.MATH;
		}
		
		public Node getLeftSideOperand() {
			return nodes.get(0);
		}
		
		public Node getRightSideOperand() {
			if(nodes.size() < 2)
				throw new IllegalStateException("Unary operator \"" + operator.getSymbol() + "\" has only one operand");
			
			return nodes.get(1);
		}
		
		public Operator getOperator() {
			return operator;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("MathNode: Operator: \"");
			builder.append(operator);
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
			
			if(!(obj instanceof MathNode))
				return false;
			
			MathNode that = (MathNode)obj;
			return this.getNodeType().equals(that.getNodeType()) && this.operator.equals(that.operator) && this.nodes.equals(that.nodes);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(this.getNodeType(), this.operator, this.nodes);
		}
		
		public static enum Operator {
			NON("", true, -1), POW("**", 1), POS("+", true, 2), INV("-", true, 2), BITWISE_NOT("~", true, 2), MUL("*", 3), DIV("/", 3), INT_DIV("//", 3), MOD("%", 3),
			ADD("+", 4), SUB("-", 4), LSHIFT("<<", 5), RSHIFT(">>", 5), RZSHIFT(">>>", 5), BITWISE_AND("&", 6), BITWISE_XOR("^", 7), BITWISE_OR("|", 8);
			
			private final String symbol;
			private final boolean unary;
			private final int precedence;
			
			private Operator(String symbol, boolean onlyOneNode, int precedence) {
				this.symbol = symbol;
				this.unary = onlyOneNode;
				this.precedence = precedence;
			}
			private Operator(String symbol, int precedence) {
				this(symbol, false, precedence);
			}
			
			public String getSymbol() {
				return symbol;
			}
			
			public boolean isUnary() {
				return unary;
			}
			
			public int getPrecedence() {
				return precedence;
			}
		}
	}
	
	public static final class ReturnNode implements Node {
		private final List<Node> nodes;
		
		public ReturnNode(Node returnValue) {
			nodes = new ArrayList<>(1);
			nodes.add(returnValue);
		}
		
		public ReturnNode() {
			nodes = new ArrayList<>(0);
		}
		
		@Override
		public List<Node> getChildren() {
			return nodes;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.RETURN;
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
			builder.append("ReturnNode: Children: {\n");
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
		private final List<Node> nodes;
		
		public ThrowNode(Node throwValue) {
			nodes = new ArrayList<>(1);
			nodes.add(throwValue);
		}
		
		@Override
		public List<Node> getChildren() {
			return nodes;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.THROW;
		}
		
		public Node getThrowValue() {
			return nodes.get(0);
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("ThrowNode: ThrowValue: {\n");
			String[] tokens = nodes.get(0).toString().split("\\n");
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
	
	//Is only super class for other nodes
	public static class ValueNode extends ChildlessNode {
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
		
		public IntValueNode(int i) {
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
			builder.append("IntValueNode: Value: \"");
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
		
		public LongValueNode(long l) {
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
			builder.append("LongValueNode: Value: \"");
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
		
		public FloatValueNode(float f) {
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
			builder.append("FloatValueNode: Value: \"");
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
		
		public DoubleValueNode(double d) {
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
			builder.append("DoubleValueNode: Value: \"");
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
		private final char c;
		
		public CharValueNode(char c) {
			this.c = c;
		}
		
		@Override
		public NodeType getNodeType() {
			return NodeType.CHAR_VALUE;
		}
		
		public char getChar() {
			return c;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CharValueNode: Value: \"");
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
		
		public TextValueNode(String text) {
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
			builder.append("TextValueNode: Value: \"");
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
		@Override
		public NodeType getNodeType() {
			return NodeType.NULL_VALUE;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("NullValueNode\n");
			
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
		@Override
		public NodeType getNodeType() {
			return NodeType.VOID_VALUE;
		}
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("VoidValueNode\n");
			
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
	
	public static enum NodeType {
		GENERAL, LIST, PARSING_ERROR, ASSIGNMENT, ESCAPE_SEQUENCE, UNPROCESSED_VARIABLE_NAME, VARIABLE_NAME, ARGUMENT_SEPARATOR,
		FUNCTION_CALL, FUNCTION_CALL_PREVIOUS_NODE_VALUE, FUNCTION_DEFINITION, CONDITION, IF_STATEMENT_PART_IF, IF_STATEMENT_PART_ELSE,
		IF_STATEMENT, LOOP_STATEMENT_PART_LOOP, LOOP_STATEMENT_PART_WHILE, LOOP_STATEMENT_PART_UNTIL, LOOP_STATEMENT_PART_REPEAT, LOOP_STATEMENT_PART_FOR_EACH,
		LOOP_STATEMENT_PART_ELSE, LOOP_STATEMENT, LOOP_STATEMENT_CONTINUE_BREAK, MATH, RETURN, THROW, INT_VALUE, LONG_VALUE, FLOAT_VALUE, DOUBLE_VALUE, CHAR_VALUE,
		TEXT_VALUE, NULL_VALUE, VOID_VALUE;
	}
}