package at.jddev0.lang;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Lang-Module<br>
 * Parsing of Lang files into an AST structure for the LangInterpreter
 *
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangParser {
	private final LangLexer lexer = new LangLexer();

	private String langDocComment;

	public LangParser() {
		resetPositionVars();
	}

	public void resetPositionVars() {
		langDocComment = null;
		lexer.resetPositionVars();
	}

	public int getLineNumber() {
		return lexer.getLineNumber();
	}

	public void setLineNumber(int lineNumber) {
		lexer.setLineNumber(lineNumber);
		lexer.setColumn(1);
	}

	public AbstractSyntaxTree parseLines(BufferedReader lines) throws IOException {
		return parseTokens(lexer.readTokens(lines));
	}

	public AbstractSyntaxTree parseTokens(List<Token> tokens) {
		removeLineContinuationAndSingleLineTextQuotesTokens(tokens);

		return parseTokensInternal(tokens);
	}

	private AbstractSyntaxTree parseTokensInternal(List<Token> tokens) {
		if(tokens.isEmpty())
			return null;

		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		int blockPos = 0;

		List<AbstractSyntaxTree.Node> errorNodes = new LinkedList<>();

		while(!tokens.isEmpty()) {
			trimFirstLine(tokens);

			parseCommentTokens(tokens, errorNodes);
			if(!errorNodes.isEmpty()) {
				errorNodes.forEach(ast::addChild);
				return ast;
			}

			trimFirstLine(tokens);

			if(tokens.isEmpty())
				break;

			if(tokens.get(0).getTokenType() == Token.TokenType.EOL) {
				tokens.remove(0);

				continue;
			}

			if(tokens.get(0).getTokenType() == Token.TokenType.EOF) {
				Token token = tokens.remove(0);

				if(!tokens.isEmpty())
					ast.addChild(new AbstractSyntaxTree.ParsingErrorNode(token.pos, ParsingError.LEXER_ERROR,
							"Tokens after EOF are not allowed"));

				break;
			}

			Token currentToken = tokens.get(0);

			//Blocks
			if(currentToken.getTokenType() == Token.TokenType.OPENING_BLOCK_BRACKET) {
				tokens.remove(0);

				blockPos++;

				continue;
			}else if(currentToken.getTokenType() == Token.TokenType.CLOSING_BLOCK_BRACKET) {
				tokens.remove(0);

				blockPos--;

				if(blockPos < 0)
					break;

				continue;
			}

			//Assignments
			if(tokens.isEmpty() || tokens.get(0).getTokenType() != Token.TokenType.OTHER ||
					!(tokens.get(0).getValue().equals("return") || tokens.get(0).getValue().equals("throw"))) {
				AbstractSyntaxTree.AssignmentNode returnedNode = parseAssignment(tokens, false);
				if(returnedNode != null) {
					ast.addChild(returnedNode);
					continue;
				}
			}

			//Non assignments
			AbstractSyntaxTree returnedAst = parseLine(tokens);
			if(returnedAst == null) //End of if
				return ast;

			ast.addChild(returnedAst.convertToNode());
		}

		return ast;
	}

	private AbstractSyntaxTree.OperationNode parseCondition(List<Token> tokens) {
		return parseOperationExpr(tokens, AbstractSyntaxTree.OperationNode.OperatorType.CONDITION);
	}

	private AbstractSyntaxTree.OperationNode parseMathExpr(List<Token> tokens) {
		return parseOperationExpr(tokens, AbstractSyntaxTree.OperationNode.OperatorType.MATH);
	}

	private AbstractSyntaxTree.OperationNode parseOperationExpr(List<Token> tokens) {
		return parseOperationExpr(tokens, AbstractSyntaxTree.OperationNode.OperatorType.GENERAL);
	}
	private AbstractSyntaxTree.OperationNode parseOperationExpr(List<Token> tokens, AbstractSyntaxTree.OperationNode.OperatorType type) {
		return parseOperationExpr(tokens, null, null, 0, type);
	}
	private AbstractSyntaxTree.OperationNode parseOperationExpr(List<Token> tokens, List<Token> tokensLeft,
																List<Token> tokensLeftBehindMiddlePartEnd,
																int currentOperatorPrecedence,
																AbstractSyntaxTree.OperationNode.OperatorType type) {
		trimFirstLine(tokens);

		final AbstractSyntaxTree.OperationNode.Operator nonOperator;
		switch(type) {
			case MATH:
				nonOperator = AbstractSyntaxTree.OperationNode.Operator.MATH_NON;
				break;
			case CONDITION:
				nonOperator = AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON;
				break;
			case GENERAL:
				nonOperator = AbstractSyntaxTree.OperationNode.Operator.NON;
				break;

			default:
				return null;
		}

		trimFirstLine(tokens);

		AbstractSyntaxTree.OperationNode.Operator operator = null;
		List<AbstractSyntaxTree.Node> leftNodes = new ArrayList<>();
		AbstractSyntaxTree.Node middleNode = null;
		AbstractSyntaxTree.Node rightNode = null;

		List<Token> whitespaces = new LinkedList<>();

		List<Token> otherTokens = new LinkedList<>();
		tokenProcessing:
		while(!tokens.isEmpty()) {
			Token t = tokens.get(0);

			switch(t.getTokenType()) {
				case EOL:
				case EOF:
					break tokenProcessing;

				case START_COMMENT:
				case START_DOC_COMMENT:
					parseCommentTokens(tokens, leftNodes);

					break;

				case LITERAL_NULL:
				case LITERAL_TEXT:
				case LITERAL_NUMBER:
				case ESCAPE_SEQUENCE:
				case ASSIGNMENT:
				case CLOSING_BRACKET:
				case LEXER_ERROR:
					if(!whitespaces.isEmpty()) {
						otherTokens.addAll(whitespaces);
						whitespaces.clear();
					}

					if(!otherTokens.isEmpty()) {
						parseTextAndCharValue(otherTokens, leftNodes);
						otherTokens.clear();
					}

					tokens.remove(0);

					switch(t.getTokenType()) {
						case LITERAL_NULL:
							leftNodes.add(new AbstractSyntaxTree.NullValueNode(t.pos));
							break;

						case LITERAL_TEXT:
						case ASSIGNMENT:
						case CLOSING_BRACKET:
							leftNodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

							break;

						case LITERAL_NUMBER:
							parseNumberToken(t, leftNodes);

							break;

						case ESCAPE_SEQUENCE:
							parseEscapeSequenceToken(t, leftNodes);

							break;

						case LEXER_ERROR:
							parseLexerErrorToken(t, leftNodes);

							break;

						default:
							break;
					}

					break;

				case START_MULTILINE_TEXT:
					if(!whitespaces.isEmpty()) {
						otherTokens.addAll(whitespaces);
						whitespaces.clear();
					}

					if(!otherTokens.isEmpty()) {
						parseTextAndCharValue(otherTokens, leftNodes);
						otherTokens.clear();
					}

					tokens.remove(0);
					do {
						t = tokens.remove(0);

						if(t.getTokenType() == Token.TokenType.LITERAL_TEXT ||
								t.getTokenType() == Token.TokenType.EOL)
							leftNodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						if(t.getTokenType() == Token.TokenType.ESCAPE_SEQUENCE)
							parseEscapeSequenceToken(t, leftNodes);

						if(t.getTokenType() == Token.TokenType.LEXER_ERROR)
							leftNodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR, t.getValue()));
					}while(t.getTokenType() != Token.TokenType.END_MULTILINE_TEXT);

					break;
				case WHITESPACE:
					tokens.remove(0);
					whitespaces.add(t);

					break;

				case IDENTIFIER:
				case PARSER_FUNCTION_IDENTIFIER:
					//TODO: Improve
					//Parse "&<name>" if something is before "&<name>" as "&" operator with new other value lexical analysis of "<name>"
					if((!otherTokens.isEmpty() || !leftNodes.isEmpty()) && t.getValue().startsWith("&")) {
						tokens.remove(0);

						tokens.add(0, new Token(t.pos, "&", Token.TokenType.OPERATOR));
						tokens.add(lexer.tokenizeOtherValue(t.getValue().substring(1), t.pos));

						break;
					}

					if(!whitespaces.isEmpty()) {
						otherTokens.addAll(whitespaces);
						whitespaces.clear();
					}

					if(!otherTokens.isEmpty()) {
						parseTextAndCharValue(otherTokens, leftNodes);
						otherTokens.clear();
					}

					boolean isIdentifier = t.getTokenType() == Token.TokenType.IDENTIFIER;
					AbstractSyntaxTree.Node ret = isIdentifier?parseVariableNameAndFunctionCall(tokens, type):
							parseParserFunctionCall(tokens);
					if(ret != null) {
						if(isIdentifier && ret instanceof AbstractSyntaxTree.UnprocessedVariableNameNode &&
								!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.OPERATOR &&
								tokens.get(0).getValue().equals("...")) {
							Token arrayUnpackingOperatorToken = tokens.remove(0);
							leftNodes.add(new AbstractSyntaxTree.UnprocessedVariableNameNode(ret.getPos().
									combine(arrayUnpackingOperatorToken.getPos()),
									((AbstractSyntaxTree.UnprocessedVariableNameNode)ret).getVariableName() +
											arrayUnpackingOperatorToken.getValue()));
						}else {
							leftNodes.add(ret);
						}
					}

					break;

				case OPENING_BRACKET:
				case OPERATOR:
				case ARGUMENT_SEPARATOR:
					String value = t.getValue();

					//Convert argument separator token to operator token with additional whitespace
					if(t.getTokenType() == Token.TokenType.ARGUMENT_SEPARATOR) {
						int index = value.indexOf(',');

						if(index > 0)
							whitespaces.add(new Token(t.pos, value.substring(0, index), Token.TokenType.WHITESPACE));

						t = new Token(t.getPos(), ",", Token.TokenType.OPERATOR);
						value = t.getValue();
						tokens.set(0, t);

						if(index < value.length() - 1)
							tokens.add(1, new Token(t.pos, value.substring(index + 1), Token.TokenType.WHITESPACE));
					}

					//Grouping
					if(t.getTokenType() == Token.TokenType.OPENING_BRACKET && value.equals("(")) {
						int endIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
						if(endIndex == -1) {
							leftNodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.BRACKET_MISMATCH,
									"Bracket in operator expression is missing"));

							break tokenProcessing;
						}

						//Parse "()" as function call previous value if something was before
						if(otherTokens.isEmpty() && leftNodes.isEmpty()) {
							if(!whitespaces.isEmpty())
								whitespaces.clear();

							List<Token> parameterTokens = new ArrayList<>(tokens.subList(1, endIndex));
							tokens.subList(0, endIndex + 1).clear();

							leftNodes.add(parseOperationExpr(parameterTokens, type));

							continue tokenProcessing;
						}else {
							if(!whitespaces.isEmpty())
								whitespaces.clear();

							if(!otherTokens.isEmpty()) {
								parseTextAndCharValue(otherTokens, leftNodes);
								otherTokens.clear();
							}

							Token openingBracketToken = tokens.get(0);
							Token closingBracketToken = tokens.get(endIndex);
							CodePosition pos = openingBracketToken.pos.combine(closingBracketToken.pos);

							List<Token> functionCall = new ArrayList<>(tokens.subList(1, endIndex));
							tokens.subList(0, endIndex + 1).clear();

							leftNodes.add(new AbstractSyntaxTree.FunctionCallPreviousNodeValueNode(pos, "", "",
									convertCommaOperatorsToArgumentSeparators(parseOperationExpr(functionCall, type))));

							continue tokenProcessing;
						}
					}

					//(Optional) Get Item / Array Creation
					if((t.getTokenType() == Token.TokenType.OPENING_BRACKET && value.equals("[")) ||
							(t.getTokenType() == Token.TokenType.OPERATOR && value.equals("?.") &&
									tokens.size() >= 2 && tokens.get(1).getTokenType() == Token.TokenType.OPENING_BRACKET &&
									tokens.get(1).getValue().equals("["))) {
						boolean startsWithOptionalMarker = t.getTokenType() == Token.TokenType.OPERATOR;

						int endIndex = LangUtils.getIndexOfMatchingBracket(tokens, startsWithOptionalMarker?1:0, Integer.MAX_VALUE, "[", "]", true);
						if(endIndex == -1) {
							leftNodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.BRACKET_MISMATCH,
									"Bracket in operator expression is missing"));

							break tokenProcessing;
						}

						//Binary operator if something was before else unary operator
						if(AbstractSyntaxTree.OperationNode.OperatorType.ALL.isCompatibleWith(type) && (!otherTokens.isEmpty() || !leftNodes.isEmpty())) {
							AbstractSyntaxTree.OperationNode.Operator oldOperator = operator;

							if(startsWithOptionalMarker)
								operator = AbstractSyntaxTree.OperationNode.Operator.OPTIONAL_GET_ITEM;
							else
								operator = AbstractSyntaxTree.OperationNode.Operator.GET_ITEM;

							if(tokensLeft != null && currentOperatorPrecedence <= operator.getPrecedence()) {
								tokensLeft.addAll(tokens);
								tokens.clear();

								if(!whitespaces.isEmpty())
									whitespaces.clear();

								operator = oldOperator;

								break tokenProcessing;
							}

							if(!whitespaces.isEmpty())
								whitespaces.clear();

							if(!otherTokens.isEmpty()) {
								parseTextAndCharValue(otherTokens, leftNodes);
								otherTokens.clear();
							}

							List<Token> tokensList = new ArrayList<>(tokens.subList(startsWithOptionalMarker?2:1, endIndex));
							tokens.subList(0, endIndex + 1).clear();

							AbstractSyntaxTree.OperationNode node = parseOperationExpr(tokensList, type);
							if(tokens.isEmpty()) {
								//Add node directly if node has NON operator
								if(node.getOperator() == nonOperator)
									rightNode = node.getLeftSideOperand();
								else
									rightNode = node;

								break tokenProcessing;
							}else {
								AbstractSyntaxTree.Node innerRightNode;

								//Add node directly if node has NON operator
								if(node.getOperator() == nonOperator)
									innerRightNode = node.getLeftSideOperand();
								else
									innerRightNode = node;

								AbstractSyntaxTree.Node leftNode;
								if(leftNodes.size() == 1)
									leftNode = leftNodes.get(0);
								else
									leftNode = new AbstractSyntaxTree.ListNode(leftNodes);

								leftNodes.clear();
								leftNodes.add(new AbstractSyntaxTree.OperationNode(leftNode, innerRightNode, operator, type));
								operator = null;
								continue tokenProcessing;
							}
						}else if(AbstractSyntaxTree.OperationNode.OperatorType.ALL.isCompatibleWith(type) && !startsWithOptionalMarker) {
							if(!whitespaces.isEmpty())
								whitespaces.clear();

							if(!otherTokens.isEmpty()) {
								parseTextAndCharValue(otherTokens, leftNodes);
								otherTokens.clear();
							}

							//Array creation
							List<Token> tokensList = new ArrayList<>(tokens.subList(1, endIndex));
							tokens.subList(0, endIndex + 1).clear();

							leftNodes.add(new AbstractSyntaxTree.ArrayNode(convertCommaOperatorsToArgumentSeparators(
									parseOperationExpr(tokensList, type))));

							if(tokens.isEmpty()) {
								operator = null;

								break tokenProcessing;
							}

							continue tokenProcessing;
						}else {
							//Incompatible type
							if(!whitespaces.isEmpty()) {
								otherTokens.addAll(whitespaces);
								whitespaces.clear();
							}
						}
					}

					if(value.equals("**")) {
						AbstractSyntaxTree.OperationNode.Operator oldOperator = operator;

						operator = AbstractSyntaxTree.OperationNode.Operator.POW;

						//If something is before operator and operator type is compatible with type
						if(operator.getOperatorType().isCompatibleWith(type) && (!otherTokens.isEmpty() || leftNodes.size() >= 0)) {
							if(tokensLeft != null && currentOperatorPrecedence < operator.getPrecedence()) { //No "<=" because it should be parsed right-to-left
								tokensLeft.addAll(tokens);
								tokens.clear();

								if(!whitespaces.isEmpty())
									whitespaces.clear();

								operator = oldOperator;

								break tokenProcessing;
							}

							//Add as value if nothing is behind operator
							if(tokens.size() == 1) {
								if(!whitespaces.isEmpty()) {
									otherTokens.addAll(whitespaces);
									whitespaces.clear();
								}

								operator = null;
								otherTokens.add(t);
								tokens.remove(0);

								break tokenProcessing;
							}

							if(!whitespaces.isEmpty())
								whitespaces.clear();

							if(!otherTokens.isEmpty()) {
								parseTextAndCharValue(otherTokens, leftNodes);
								otherTokens.clear();
							}

							List<Token> innerTokensLeft = new LinkedList<>();
							List<Token> tokensList = new ArrayList<>(tokens.subList(1, tokens.size()));
							AbstractSyntaxTree.OperationNode node = parseOperationExpr(tokensList, innerTokensLeft, tokensLeftBehindMiddlePartEnd, operator.getPrecedence(), type);
							if(node == null) //End was reached inside middle part of a ternary operator
								return null;

							tokens.clear();
							tokens.addAll(innerTokensLeft);

							if(tokens.isEmpty()) {
								//Add node directly if node has NON operator
								if(node.getOperator() == nonOperator)
									rightNode = node.getLeftSideOperand();
								else
									rightNode = node;

								break tokenProcessing;
							}else {
								AbstractSyntaxTree.Node innerRightNode;

								//Add node directly if node has NON operator
								if(node.getOperator() == nonOperator)
									innerRightNode = node.getLeftSideOperand();
								else
									innerRightNode = node;

								AbstractSyntaxTree.Node leftNode;
								if(leftNodes.size() == 1)
									leftNode = leftNodes.get(0);
								else
									leftNode = new AbstractSyntaxTree.ListNode(leftNodes);

								leftNodes.clear();
								leftNodes.add(new AbstractSyntaxTree.OperationNode(leftNode, innerRightNode, operator, type));
								operator = null;
								continue tokenProcessing;
							}
						}else {
							operator = oldOperator;

							//Ignore operator: nothing was before for binary operator or operator type is not compatible with type
							if(!whitespaces.isEmpty()) {
								otherTokens.addAll(whitespaces);
								whitespaces.clear();
							}
						}
					}

					if(value.equals("!==") || value.equals("!=~") || value.equals("!=") || value.equals("===") || value.equals("=~") || value.equals("==") ||
							value.equals("<=>") || value.equals("<=") || value.equals(">=") || value.equals("<") || value.equals(">") || value.equals("|||") || value.equals("&&") ||
							value.equals("||") || value.equals("!") || value.equals("&") || value.equals("~~") || value.equals("~/") || value.equals("~") || value.equals("\u25b2") ||
							value.equals("\u25bc") || value.equals("*") || value.equals("//") || value.equals("^/") || value.equals("/") || value.equals("%") || value.equals("^") ||
							value.equals("|") || value.equals("<<") || value.equals(">>>") || value.equals(">>") || value.equals("+|") || value.equals("-|") || value.equals("+") ||
							value.equals("->") || value.equals("-") || value.equals("@") || value.equals("?:") || value.equals("??") || value.equals(",") || value.equals("?::") ||
							value.equals("::")) {
						boolean somethingBeforeOperator = !otherTokens.isEmpty() || !leftNodes.isEmpty();

						AbstractSyntaxTree.OperationNode.Operator oldOperator = operator;
						if(value.equals("!==") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.STRICT_NOT_EQUALS;
						}else if(value.equals("!=~") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.NOT_MATCHES;
						}else if(value.equals("!=") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.NOT_EQUALS;
						}else if(value.equals("===") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.STRICT_EQUALS;
						}else if(value.equals("=~") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.MATCHES;
						}else if(value.equals("==") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.EQUALS;
						}else if(value.equals("?::") && AbstractSyntaxTree.OperationNode.OperatorType.ALL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.OPTIONAL_MEMBER_ACCESS;
						}else if(value.equals("::") && AbstractSyntaxTree.OperationNode.OperatorType.ALL.isCompatibleWith(type)) {
							if(somethingBeforeOperator)
								operator = AbstractSyntaxTree.OperationNode.Operator.MEMBER_ACCESS;
							else
								operator = AbstractSyntaxTree.OperationNode.Operator.MEMBER_ACCESS_THIS;
						}else if(value.equals("->") && AbstractSyntaxTree.OperationNode.OperatorType.ALL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.MEMBER_ACCESS_POINTER;
						}else if(value.equals("<<") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.LSHIFT;
						}else if(value.equals(">>>") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.RZSHIFT;
						}else if(value.equals(">>") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.RSHIFT;
						}else if(value.equals("<=>") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.SPACESHIP;
						}else if(value.equals("<=") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.LESS_THAN_OR_EQUALS;
						}else if(value.equals(">=") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.GREATER_THAN_OR_EQUALS;
						}else if(value.equals("<") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.LESS_THAN;
						}else if(value.equals(">") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.GREATER_THAN;
						}else if(value.equals("|||") && AbstractSyntaxTree.OperationNode.OperatorType.GENERAL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.CONCAT;
						}else if(value.equals("&&") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.AND;
						}else if(value.equals("||") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.OR;
						}else if(value.equals("!") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.NOT;
						}else if(value.equals("&") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_AND;
						}else if(value.equals("~~") && AbstractSyntaxTree.OperationNode.OperatorType.CONDITION.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.INSTANCE_OF;
						}else if(value.equals("~/") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.TRUNC_DIV;
						}else if(value.equals("~") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_NOT;
						}else if(value.equals("\u25b2") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.INC;
						}else if(value.equals("\u25bc") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.DEC;
						}else if(value.equals("+|") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.INC;
						}else if(value.equals("-|") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.DEC;
						}else if(value.equals("*") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.MUL;
						}else if(value.equals("^/") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.CEIL_DIV;
						}else if(value.equals("//") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.FLOOR_DIV;
						}else if(value.equals("/") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.DIV;
						}else if(value.equals("%") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.MOD;
						}else if(value.equals("^")) {
							if(somethingBeforeOperator && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type))
								operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_XOR;
							else if(!somethingBeforeOperator && AbstractSyntaxTree.OperationNode.OperatorType.GENERAL.isCompatibleWith(type))
								operator = AbstractSyntaxTree.OperationNode.Operator.DEEP_COPY;
						}else if(value.equals("|") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_OR;
						}else if(value.equals("+") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = somethingBeforeOperator?AbstractSyntaxTree.OperationNode.Operator.ADD:AbstractSyntaxTree.OperationNode.Operator.POS;
						}else if(value.equals("-") && AbstractSyntaxTree.OperationNode.OperatorType.MATH.isCompatibleWith(type)) {
							operator = somethingBeforeOperator?AbstractSyntaxTree.OperationNode.Operator.SUB:AbstractSyntaxTree.OperationNode.Operator.INV;
						}else if(value.equals("@") && AbstractSyntaxTree.OperationNode.OperatorType.GENERAL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.LEN;
						}else if(value.equals("?:") && AbstractSyntaxTree.OperationNode.OperatorType.GENERAL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.ELVIS;
						}else if(value.equals("??") && AbstractSyntaxTree.OperationNode.OperatorType.GENERAL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.NULL_COALESCING;
						}else if(value.equals(",") && AbstractSyntaxTree.OperationNode.OperatorType.ALL.isCompatibleWith(type)) {
							operator = AbstractSyntaxTree.OperationNode.Operator.COMMA;
						}else {
							operator = null;
						}

						if(operator != null && operator.isBinary() && somethingBeforeOperator) {
							//Binary

							if(tokensLeft != null && currentOperatorPrecedence <= operator.getPrecedence()) {
								tokensLeft.addAll(tokens);
								tokens.clear();

								if(!whitespaces.isEmpty())
									whitespaces.clear();

								operator = oldOperator;

								break tokenProcessing;
							}

							//Add as value if nothing is behind "operator"
							if(tokens.size() == 1) {
								if(!whitespaces.isEmpty()) {
									otherTokens.addAll(whitespaces);
									whitespaces.clear();
								}

								operator = null;
								otherTokens.add(t);
								tokens.remove(0);

								break tokenProcessing;
							}

							if(!whitespaces.isEmpty())
								whitespaces.clear();

							if(!otherTokens.isEmpty()) {
								parseTextAndCharValue(otherTokens, leftNodes);
								otherTokens.clear();
							}

							List<Token> innerTokensLeft = new LinkedList<>();
							List<Token> tokensList = new ArrayList<>(tokens.subList(1, tokens.size()));
							AbstractSyntaxTree.OperationNode node = parseOperationExpr(tokensList, innerTokensLeft, tokensLeftBehindMiddlePartEnd, operator.getPrecedence(), type);
							if(node == null) //End was reached inside middle part of a ternary operator
								return null;

							tokens.clear();
							tokens.addAll(innerTokensLeft);

							if(tokens.isEmpty()) {
								//Add node directly if node has NON operator
								if(node.getOperator() == nonOperator)
									rightNode = node.getLeftSideOperand();
								else
									rightNode = node;

								break tokenProcessing;
							}else {
								AbstractSyntaxTree.Node innerRightNode;

								//Add node directly if node has NON operator
								if(node.getOperator() == nonOperator)
									innerRightNode = node.getLeftSideOperand();
								else
									innerRightNode = node;

								AbstractSyntaxTree.Node leftNode;
								if(leftNodes.size() == 1)
									leftNode = leftNodes.get(0);
								else
									leftNode = new AbstractSyntaxTree.ListNode(leftNodes);

								leftNodes.clear();
								leftNodes.add(new AbstractSyntaxTree.OperationNode(leftNode, innerRightNode, operator, type));
								operator = null;
								continue tokenProcessing;
							}
						}else if(operator != null && operator.isUnary() && !somethingBeforeOperator) {
							//Unary

							if(!whitespaces.isEmpty())
								whitespaces.clear();

							List<Token> innerTokensLeft = new LinkedList<>();
							List<Token> tokensList = new ArrayList<>(tokens.subList(1, tokens.size()));
							AbstractSyntaxTree.OperationNode node = parseOperationExpr(tokensList, innerTokensLeft, tokensLeftBehindMiddlePartEnd, operator.getPrecedence(), type);
							if(node == null) //End was reached inside middle part of a ternary operator
								return null;

							tokens.clear();
							tokens.addAll(innerTokensLeft);

							AbstractSyntaxTree.Node innerRightNode;

							//Add node directly if node has NON operator
							if(node.getOperator() == nonOperator)
								innerRightNode = node.getLeftSideOperand();
							else
								innerRightNode = node;

							leftNodes.add(new AbstractSyntaxTree.OperationNode(innerRightNode, operator, type));
							operator = null;

							if(tokens.isEmpty())
								break tokenProcessing;
							else
								continue tokenProcessing;
						}else {
							operator = oldOperator;

							//Ignore operator: something was before for unary operator or nothing was before for binary operator or operator type is not compatible with type
							if(!whitespaces.isEmpty()) {
								otherTokens.addAll(whitespaces);
								whitespaces.clear();
							}
						}
					}


					if(value.equals("?")) {
						AbstractSyntaxTree.OperationNode.Operator oldOperator = operator;

						operator = AbstractSyntaxTree.OperationNode.Operator.INLINE_IF;

						//Inline if -> Only parse if something is before and ":" was found -> else "?" will be parsed as text

						if(operator.getOperatorType().isCompatibleWith(type) && (!otherTokens.isEmpty() || !leftNodes.isEmpty())) {
							if(tokensLeft != null && currentOperatorPrecedence < operator.getPrecedence()) { //No "<=" because it should be parsed right-to-left
								tokensLeft.addAll(tokens);
								tokens.clear();

								if(!whitespaces.isEmpty())
									whitespaces.clear();

								operator = oldOperator;

								break tokenProcessing;
							}

							//Parse middle part
							List<Token> innerTokensLeftBehindMiddlePartEnd = new LinkedList<>();
							List<Token> tokensList = new ArrayList<>(tokens.subList(1, tokens.size()));
							AbstractSyntaxTree.OperationNode innerMiddleNodeRet = parseOperationExpr(tokensList, null, innerTokensLeftBehindMiddlePartEnd, 0, type);
							if(innerMiddleNodeRet != null) {
								//Only parse as operator if matching ":" was found

								//Add as value if nothing is behind "operator"
								if(innerTokensLeftBehindMiddlePartEnd.isEmpty()) {
									if(!whitespaces.isEmpty()) {
										otherTokens.addAll(whitespaces);
										whitespaces.clear();
									}

									operator = null;
									otherTokens.add(t);

									break tokenProcessing;
								}

								tokens.clear();
								tokens.addAll(innerTokensLeftBehindMiddlePartEnd);

								if(!whitespaces.isEmpty())
									whitespaces.clear();

								if(!otherTokens.isEmpty()) {
									parseTextAndCharValue(otherTokens, leftNodes);
									otherTokens.clear();
								}

								List<Token> innerTokensLeft = new LinkedList<>();
								tokensList = new ArrayList<>(tokens);
								AbstractSyntaxTree.OperationNode innerRightNodeRet = parseOperationExpr(tokensList, innerTokensLeft,
										tokensLeftBehindMiddlePartEnd, operator.getPrecedence(), type);

								tokens.clear();
								tokens.addAll(innerTokensLeft);

								if(tokens.isEmpty()) {
									//Add middle node directly if node has NON operator
									if(innerMiddleNodeRet.getOperator() == nonOperator)
										middleNode = innerMiddleNodeRet.getLeftSideOperand();
									else
										middleNode = innerMiddleNodeRet;

									//Add right node directly if node has NON operator
									if(innerRightNodeRet.getOperator() == nonOperator)
										rightNode = innerRightNodeRet.getLeftSideOperand();
									else
										rightNode = innerRightNodeRet;

									break;
								}else {
									AbstractSyntaxTree.Node innerMiddleNode;
									AbstractSyntaxTree.Node innerRightNode;

									//Add middle node directly if node has NON operator
									if(innerMiddleNodeRet.getOperator() == nonOperator)
										innerMiddleNode = innerMiddleNodeRet.getLeftSideOperand();
									else
										innerMiddleNode = innerMiddleNodeRet;

									//Add node directly if node has NON operator
									if(innerRightNodeRet.getOperator() == nonOperator)
										innerRightNode = innerRightNodeRet.getLeftSideOperand();
									else
										innerRightNode = innerRightNodeRet;

									AbstractSyntaxTree.Node leftNode;
									if(leftNodes.size() == 1)
										leftNode = leftNodes.get(0);
									else
										leftNode = new AbstractSyntaxTree.ListNode(leftNodes);

									leftNodes.clear();
									leftNodes.add(new AbstractSyntaxTree.OperationNode(leftNode, innerMiddleNode, innerRightNode, operator, type));
									operator = null;
									continue tokenProcessing;
								}
							}else {
								operator = oldOperator;

								//Ignore operator: nothing was before for ternary operator or operator type is not compatible with type
								if(!whitespaces.isEmpty()) {
									otherTokens.addAll(whitespaces);
									whitespaces.clear();
								}
							}
						}else {
							operator = oldOperator;

							//Ignore operator: nothing was before for ternary operator or operator type is not compatible with type
							if(!whitespaces.isEmpty()) {
								otherTokens.addAll(whitespaces);
								whitespaces.clear();
							}
						}
					}

					if(tokensLeftBehindMiddlePartEnd != null && value.equals(":")) {
						//End of inline if

						if(!whitespaces.isEmpty())
							whitespaces.clear();

						tokens.remove(0);
						tokensLeftBehindMiddlePartEnd.addAll(tokens);
						tokens.clear();

						//Reset (Simulated end)
						if(tokensLeft != null && !tokensLeft.isEmpty())
							tokensLeft.clear();

						break tokenProcessing;
					}

					tokens.remove(0);

					if(!whitespaces.isEmpty()) {
						otherTokens.addAll(whitespaces);
						whitespaces.clear();
					}

					if(!otherTokens.isEmpty()) {
						parseTextAndCharValue(otherTokens, leftNodes);
						otherTokens.clear();
					}

					//Allow "+<LITERAL_NUMBER>" and "-<LITERAL_NUMBER>" in conditional parsing (if nothing is before)
					if((otherTokens.isEmpty() && leftNodes.isEmpty()) && (t.getValue().equals("-") ||
							t.getValue().equals("+")) && !tokens.isEmpty() &&
							tokens.get(0).getTokenType() == Token.TokenType.LITERAL_NUMBER) {
						Token numberToken = tokens.remove(0);

						Token combinedNumberToken = new Token(t.pos.combine(numberToken.pos),
								t.getValue() + numberToken.getValue(), Token.TokenType.LITERAL_NUMBER);

						parseNumberToken(combinedNumberToken, leftNodes);
					}else {
						leftNodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, value));
					}

					break;

				case OTHER:
					if(!whitespaces.isEmpty()) {
						otherTokens.addAll(whitespaces);
						whitespaces.clear();
					}

					if(!otherTokens.isEmpty()) {
						parseTextAndCharValue(otherTokens, leftNodes);
						otherTokens.clear();
					}

					ret = parseFunctionCallWithoutPrefix(tokens, type);
					if(ret == null) {
						tokens.remove(0);
						otherTokens.add(t);
					}else {
						leftNodes.add(ret);
					}

					break;

				case OPENING_BLOCK_BRACKET:
				case CLOSING_BLOCK_BRACKET:
				case LINE_CONTINUATION:
				case END_COMMENT:
				case END_MULTILINE_TEXT:
				case SINGLE_LINE_TEXT_QUOTES:
					leftNodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
							"Invalid token type for operation expression: \"" + t.getTokenType().name() + "\""));

					break tokenProcessing;
			}
		}

		//End of middle part was not found for ternary operator -> ignore ternary operator
		if(tokensLeftBehindMiddlePartEnd != null && tokensLeftBehindMiddlePartEnd.isEmpty())
			return null;

		if(!whitespaces.isEmpty()) {
			otherTokens.addAll(whitespaces);
			whitespaces.clear();
		}

		if(!otherTokens.isEmpty()) {
			parseTextAndCharValue(otherTokens, leftNodes);
			otherTokens.clear();
		}

		if(operator == null)
			operator = nonOperator;

		AbstractSyntaxTree.Node leftNode;
		if(leftNodes.size() == 1)
			leftNode = leftNodes.get(0);
		else
			leftNode = new AbstractSyntaxTree.ListNode(leftNodes);

		if(tokensLeft != null && !tokens.isEmpty()) {
			tokensLeft.addAll(tokens);
			tokens.clear();
		}

		if(operator.isUnary())
			return new AbstractSyntaxTree.OperationNode(leftNode, operator, type);
		if(operator.isBinary())
			return new AbstractSyntaxTree.OperationNode(leftNode, rightNode, operator, type);
		if(operator.isTernary())
			return new AbstractSyntaxTree.OperationNode(leftNode, middleNode, rightNode, operator, type);

		return null;
	}

	private List<AbstractSyntaxTree.Node> convertCommaOperatorsToArgumentSeparators(AbstractSyntaxTree.OperationNode operatorNode) {
		List<AbstractSyntaxTree.Node> nodes = new LinkedList<>();

		if(operatorNode.getOperator() == AbstractSyntaxTree.OperationNode.Operator.NON ||
				operatorNode.getOperator() == AbstractSyntaxTree.OperationNode.Operator.MATH_NON ||
				operatorNode.getOperator() == AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON) {
			//Ignore NON operators
			AbstractSyntaxTree.Node operand = operatorNode.getLeftSideOperand();

			if(operand instanceof AbstractSyntaxTree.OperationNode)
				nodes.addAll(convertCommaOperatorsToArgumentSeparators((AbstractSyntaxTree.OperationNode)operand));
			else
				nodes.add(operand);

		}else if(operatorNode.getOperator() == AbstractSyntaxTree.OperationNode.Operator.COMMA) {
			//Only parse COMMA operators and COMMA operators inside COMMA operators but only if they are the left node
			AbstractSyntaxTree.Node leftSideOperand = operatorNode.getLeftSideOperand();

			//Add left side operand
			if(leftSideOperand instanceof AbstractSyntaxTree.OperationNode)
				nodes.addAll(convertCommaOperatorsToArgumentSeparators((AbstractSyntaxTree.OperationNode)leftSideOperand));
			else
				nodes.add(leftSideOperand);

			//Add argument separator
			nodes.add(new AbstractSyntaxTree.ArgumentSeparatorNode(operatorNode.getPos(), ", "));

			//Add right side operand
			nodes.add(operatorNode.getRightSideOperand());
		}else {
			nodes.add(operatorNode);
		}

		return nodes;
	}

	private AbstractSyntaxTree.AssignmentNode parseAssignment(List<Token> tokens, boolean isInnerAssignment) {
		if(tokens.isEmpty())
			return null;

		trimFirstLine(tokens);

		int assignmentIndex = -1;
		int tokenCountFirstLine = -1;
		for(int i = 0;i < tokens.size();i++) {
			if(tokens.get(i).getTokenType() == Token.TokenType.EOL ||
					tokens.get(i).getTokenType() == Token.TokenType.EOF) {
				tokenCountFirstLine = i;
				break;
			}

			if(assignmentIndex == -1) {
				if(tokens.get(i).getTokenType() == Token.TokenType.ASSIGNMENT)
					assignmentIndex = i;

				//Do not parse assignments in function body definition
				if(i + 2 < tokens.size() &&
						tokens.get(i).getTokenType() == Token.TokenType.CLOSING_BRACKET && tokens.get(i).getValue().equals(")") &&
						tokens.get(i + 1).getTokenType() == Token.TokenType.WHITESPACE &&
						tokens.get(i + 2).getTokenType() == Token.TokenType.OPERATOR && tokens.get(i + 2).getValue().equals("->"))
					return null;
			}
		}

		if(tokenCountFirstLine == -1)
			tokenCountFirstLine = tokens.size();

		if(assignmentIndex == -1) {
			if(isInnerAssignment)
				return null;

			List<Token> variableNameTokens = new ArrayList<>(tokens.subList(0, tokenCountFirstLine));
			if(variableNameTokens.size() != 1 || variableNameTokens.get(0).getTokenType() != Token.TokenType.IDENTIFIER)
				return null;

			Token variableNameToken = variableNameTokens.get(0);
			String variableName = variableNameToken.getValue();

			if(LangPatterns.matches(variableName, LangPatterns.VAR_NAME_FULL)) {
				tokens.remove(0);

				return new AbstractSyntaxTree.AssignmentNode(new AbstractSyntaxTree.UnprocessedVariableNameNode(
						variableNameToken.pos, variableNameToken.getValue()),
						new AbstractSyntaxTree.NullValueNode(variableNameToken.pos));
			}

			return null;
		}

		Token assignmentToken = tokens.get(assignmentIndex);
		List<Token> lvalueTokens = new ArrayList<>(tokens.subList(0, assignmentIndex));
		trimFirstLine(lvalueTokens);

		if(lvalueTokens.isEmpty())
			return null;

		boolean isSimpleAssignment = assignmentToken.getValue().equals("=");
		if(isSimpleAssignment || assignmentToken.getValue().equals(" = ")) {
			CodePosition pos = lvalueTokens.get(0).pos.combine(lvalueTokens.get(lvalueTokens.size() - 1).pos);

			if(lvalueTokens.size() == 1 && lvalueTokens.get(0).getTokenType() == Token.TokenType.IDENTIFIER &&
					LangPatterns.matches(lvalueTokens.get(0).getValue(), isSimpleAssignment?
							LangPatterns.PARSING_SIMPLE_ASSIGNMENT_VARIABLE_NAME_LVALUE:
							LangPatterns.VAR_NAME_FULL)) {
				tokens.subList(0, assignmentIndex + 1).clear();
				trimFirstLine(tokens);

				if(isSimpleAssignment) {
					//The assignment value for empty simple assignments will be set to empty text ""
					return new AbstractSyntaxTree.AssignmentNode(new AbstractSyntaxTree.UnprocessedVariableNameNode(
							pos, lvalueTokens.get(0).getValue()), parseSimpleAssignmentValue(tokens).convertToNode());
				}

				AbstractSyntaxTree.AssignmentNode returnedNode = parseAssignment(tokens, true);
				AbstractSyntaxTree.Node rvalueNode = returnedNode != null?returnedNode:parseLRvalue(tokens, true).convertToNode();
				return new AbstractSyntaxTree.AssignmentNode(new AbstractSyntaxTree.UnprocessedVariableNameNode(
						pos, lvalueTokens.get(0).getValue()), rvalueNode);
			}

			String lvalue = lvalueTokens.stream().map(Token::toRawString).collect(Collectors.joining());
			if(LangPatterns.matches(lvalue, LangPatterns.PARSING_PARSER_FLAG)) {
				List<Token> rvalueTokens = new ArrayList<>(tokens.subList(assignmentIndex + 1, tokenCountFirstLine));
				trimFirstLine(rvalueTokens);

				parseParserFlags(lvalue, (isSimpleAssignment?parseSimpleAssignmentValue(rvalueTokens):
						parseLRvalue(rvalueTokens, true)).convertToNode());

				tokens.subList(0, tokenCountFirstLine).clear();

				return null;
			}

			if(LangPatterns.matches(lvalue, LangPatterns.PARSING_SIMPLE_TRANSLATION_KEY)) {
				tokens.subList(0, assignmentIndex + 1).clear();

				//The translation value for empty simple translation will be set to empty text ""
				return new AbstractSyntaxTree.AssignmentNode(new AbstractSyntaxTree.TextValueNode(pos, lvalue),
						(isSimpleAssignment?parseSimpleAssignmentValue(tokens):
								parseLRvalue(tokens, true)).convertToNode());
			}

		}

		boolean isVariableAssignment = lvalueTokens.size() == 1 && lvalueTokens.get(0).getTokenType() == Token.TokenType.IDENTIFIER &&
				LangPatterns.matches(lvalueTokens.get(0).getValue(), LangPatterns.VAR_NAME_FULL);

		if(assignmentToken.getValue().equals(" =")) {
			tokens.subList(0, tokenCountFirstLine).clear();

			return new AbstractSyntaxTree.AssignmentNode((isVariableAssignment?parseLRvalue(lvalueTokens, false):parseTranslationKey(lvalueTokens)).convertToNode(),
					new AbstractSyntaxTree.NullValueNode(assignmentToken.pos));
		}

		if(LangPatterns.matches(assignmentToken.getValue(), LangPatterns.PARSING_ASSIGNMENT_OPERATOR)) {
			tokens.subList(0, assignmentIndex + 1).clear();

			String assignmentOperator = assignmentToken.getValue().substring(1, assignmentToken.getValue().length() - 2);

			AbstractSyntaxTree.OperationNode.Operator operator = null;
			if(!assignmentOperator.isEmpty()) {
				switch(assignmentOperator) {
					case "**":
						operator = AbstractSyntaxTree.OperationNode.Operator.POW;
						break;
					case "*":
						operator = AbstractSyntaxTree.OperationNode.Operator.MUL;
						break;
					case "/":
						operator = AbstractSyntaxTree.OperationNode.Operator.DIV;
						break;
					case "~/":
						operator = AbstractSyntaxTree.OperationNode.Operator.TRUNC_DIV;
						break;
					case "//":
						operator = AbstractSyntaxTree.OperationNode.Operator.FLOOR_DIV;
						break;
					case "^/":
						operator = AbstractSyntaxTree.OperationNode.Operator.CEIL_DIV;
						break;
					case "%":
						operator = AbstractSyntaxTree.OperationNode.Operator.MOD;
						break;
					case "+":
						operator = AbstractSyntaxTree.OperationNode.Operator.ADD;
						break;
					case "-":
						operator = AbstractSyntaxTree.OperationNode.Operator.SUB;
						break;
					case "<<":
						operator = AbstractSyntaxTree.OperationNode.Operator.LSHIFT;
						break;
					case ">>":
						operator = AbstractSyntaxTree.OperationNode.Operator.RSHIFT;
						break;
					case ">>>":
						operator = AbstractSyntaxTree.OperationNode.Operator.RZSHIFT;
						break;
					case "&":
						operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_AND;
						break;
					case "^":
						operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_XOR;
						break;
					case "|":
						operator = AbstractSyntaxTree.OperationNode.Operator.BITWISE_OR;
						break;
					case "|||":
						operator = AbstractSyntaxTree.OperationNode.Operator.CONCAT;
						break;
					case "?:":
						operator = AbstractSyntaxTree.OperationNode.Operator.ELVIS;
						break;
					case "??":
						operator = AbstractSyntaxTree.OperationNode.Operator.NULL_COALESCING;
						break;
					case "?":
						operator = AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON;
						break;
					case ":":
						operator = AbstractSyntaxTree.OperationNode.Operator.MATH_NON;
						break;
					case "$":
						operator = AbstractSyntaxTree.OperationNode.Operator.NON;
						break;
				}
			}

			AbstractSyntaxTree.Node lvalueNode;
			if(isVariableAssignment) {
				lvalueNode = parseLRvalue(lvalueTokens, false).convertToNode();
			}else {
				if (operator == AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON)
					lvalueNode = parseCondition(lvalueTokens);
				else if (operator == AbstractSyntaxTree.OperationNode.Operator.MATH_NON)
					lvalueNode = parseMathExpr(lvalueTokens);
				else
					lvalueNode = parseOperationExpr(lvalueTokens);
			}

			AbstractSyntaxTree.Node rvalueNode;

			if(assignmentOperator.equals("::")) {
				AbstractSyntaxTree.AssignmentNode returnedNode = parseAssignment(tokens, true);
				rvalueNode = returnedNode != null?returnedNode:parseLRvalue(tokens, true).convertToNode();
			}else {
				if(operator == null)
					rvalueNode = new AbstractSyntaxTree.ParsingErrorNode(assignmentToken.pos, ParsingError.INVALID_ASSIGNMENT);
				else if(operator == AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON)
					rvalueNode = parseCondition(tokens);
				else if(operator == AbstractSyntaxTree.OperationNode.Operator.MATH_NON)
					rvalueNode = parseMathExpr(tokens);
				else if(operator == AbstractSyntaxTree.OperationNode.Operator.NON)
					rvalueNode = parseOperationExpr(tokens);
				else
					rvalueNode = new AbstractSyntaxTree.OperationNode(lvalueNode, parseOperationExpr(tokens), operator, operator.getOperatorType());
			}

			return new AbstractSyntaxTree.AssignmentNode(lvalueNode, rvalueNode);
		}

		//Translation with " = "
		if(assignmentToken.getValue().equals(" = ")) {
			tokens.subList(0, assignmentIndex + 1).clear();

			//The translation value for empty simple translation will be set to empty text ""
			return new AbstractSyntaxTree.AssignmentNode(parseTranslationKey(lvalueTokens).convertToNode(),
					parseLRvalue(tokens, true).convertToNode());
		}

		return null;
	}

	private AbstractSyntaxTree parseLine(List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		int tokenCountFirstLine = getTokenCountFirstLine(tokens);
		if(tokenCountFirstLine == -1)
			tokenCountFirstLine = tokens.size();

		//Control flow statements
		boolean startsWithConExpression = tokenCountFirstLine > 0 && tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
				tokens.get(0).getValue().startsWith("con.");
		boolean endsWithOpeningBracket = tokenCountFirstLine > 0 && tokens.get(tokenCountFirstLine - 1).getTokenType() ==
				Token.TokenType.OPENING_BLOCK_BRACKET;
		if(startsWithConExpression || endsWithOpeningBracket) {
			String conExpression = tokens.get(0).getValue();
			String originalConExpression = conExpression;

			//"con." is optional if the curly brackets syntax is used
			if(endsWithOpeningBracket && !startsWithConExpression)
				conExpression = "con." + conExpression;

			if(!endsWithOpeningBracket && (conExpression.equals("con.continue") || conExpression.equals("con.break"))) {
				Token conExpressionToken = tokens.remove(0);
				Token lastToken = conExpressionToken;

				List<AbstractSyntaxTree.Node> argumentNodes;
				if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
						tokens.get(0).getValue().equals("(")) {
					int argumentsEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
					if(argumentsEndIndex == -1) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
								"Bracket for con.break or con.continue is missing"));
						return ast;
					}

					lastToken = tokens.get(argumentsEndIndex + 1);
					argumentNodes = parseFunctionParameterList(new ArrayList<>(tokens.subList(1, argumentsEndIndex)), false).getChildren();
					tokens.subList(0, argumentsEndIndex + 1).clear();
				}else {
					argumentNodes = null;
				}

				CodePosition pos = conExpressionToken.pos.combine(lastToken.pos);

				AbstractSyntaxTree.Node numberNode = argumentNodes == null?null:(argumentNodes.size() == 1?argumentNodes.get(0):new AbstractSyntaxTree.ListNode(argumentNodes));
				ast.addChild(new AbstractSyntaxTree.LoopStatementContinueBreakStatement(pos, numberNode, conExpression.equals("con.continue")));
				return ast;
			}else if(conExpression.equals("con.try") || conExpression.equals("con.softtry") || conExpression.equals("con.nontry")) {
				List<AbstractSyntaxTree.TryStatementPartNode> tryStatmentParts = new ArrayList<>();

				boolean blockBracketFlag = endsWithOpeningBracket;
				while(!tokens.isEmpty()) {
					trimFirstLine(tokens);

					tokenCountFirstLine = getTokenCountFirstLine(tokens);
					if(tokenCountFirstLine == -1)
						tokenCountFirstLine = tokens.size();
					endsWithOpeningBracket = tokenCountFirstLine > 0 && tokens.get(tokenCountFirstLine - 1).getTokenType() ==
							Token.TokenType.OPENING_BLOCK_BRACKET;

					if(tokenCountFirstLine == 0)
						break;

					conExpression = tokens.get(0).getValue();

					//"con." is optional if the curly brackets syntax is used
					if(endsWithOpeningBracket && !startsWithConExpression)
						conExpression = "con." + conExpression;

					if(blockBracketFlag) {
						//Remove "{" and "}" for the curly brackets if statement syntax
						CodePosition pos = tokens.get(tokenCountFirstLine - 1).pos;

						if(tokens.get(tokenCountFirstLine - 1).getTokenType() != Token.TokenType.OPENING_BLOCK_BRACKET)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.INVALID_CON_PART));

						tokens.remove(tokenCountFirstLine - 1);

						tokenCountFirstLine--;

						if(tokenCountFirstLine == 0 || tokens.get(0).getTokenType() != Token.TokenType.OTHER)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.INVALID_CON_PART));

						conExpression = tokens.get(0).getValue();

						//"con." is optional if "{" syntax is used
						if(!conExpression.startsWith("con."))
							conExpression = "con." + conExpression;

						trimFirstLine(tokens);

						tokenCountFirstLine = getTokenCountFirstLine(tokens);
						if(tokenCountFirstLine == -1)
							tokenCountFirstLine = tokens.size();
					}

					Token tryStatementPartToken;
					List<Token> tryArguments;
					if(conExpression.equals("con.try") || conExpression.equals("con.softtry") || conExpression.equals("con.nontry") || conExpression.equals("con.else") ||
							conExpression.equals("con.finally")) {
						tryStatementPartToken = tokens.remove(0);
						tokenCountFirstLine--;

						if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET && tokens.get(0).getValue().equals("(")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tryStatementPartToken.pos, ParsingError.INVALID_CON_PART,
									"Try/Softtry/Nontry/Finally/Else part with arguments"));

							return ast;
						}

						tryArguments = null;
					}else if(conExpression.equals("con.catch")) {
						if(tokenCountFirstLine == 1) {
							tryStatementPartToken = tokens.remove(0);
							tokenCountFirstLine--;

							tryArguments = null;
						}else {
							tryStatementPartToken = tokens.remove(0);
							tokenCountFirstLine--;

							if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
									tokens.get(0).getValue().equals("(")) {
								int argumentsEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
								if(argumentsEndIndex == -1) {
									nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
											"Missing catch statement arguments"));
									return ast;
								}

								tryArguments = new ArrayList<>(tokens.subList(1, argumentsEndIndex));
								tokens.subList(0, argumentsEndIndex + 1).clear();
								tokenCountFirstLine -= argumentsEndIndex + 1;
							}else {
								tryArguments = null;
							}

							if(tokenCountFirstLine != 0) {
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tryStatementPartToken.pos, ParsingError.INVALID_CON_PART,
										"Trailing stuff behind arguments"));
								return ast;
							}
						}
					}else if(!blockBracketFlag && conExpression.equals("con.endtry")) {
						tokens.remove(0);

						break;
					}else {
						//TODO lineNumber
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.INVALID_CON_PART,
								"Try statement part is invalid"));
						return ast;
					}

					AbstractSyntaxTree tryBody = parseTokensInternal(tokens);
					if(tryBody == null) {
						//TODO line numbers
						nodes.add(new AbstractSyntaxTree.TryStatementNode(CodePosition.EMPTY, tryStatmentParts));
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.EOF, "In try body"
						));

						return ast;
					}

					//TODO line numbers
					if(conExpression.equals("con.try")) {
						tryStatmentParts.add(new AbstractSyntaxTree.TryStatementPartTryNode(CodePosition.EMPTY, tryBody));
					}else if(conExpression.equals("con.softtry")) {
						tryStatmentParts.add(new AbstractSyntaxTree.TryStatementPartSoftTryNode(CodePosition.EMPTY, tryBody));
					}else if(conExpression.equals("con.nontry")) {
						tryStatmentParts.add(new AbstractSyntaxTree.TryStatementPartNonTryNode(CodePosition.EMPTY, tryBody));
					}else if(conExpression.equals("con.catch")) {
						tryStatmentParts.add(new AbstractSyntaxTree.TryStatementPartCatchNode(CodePosition.EMPTY, tryBody,
								tryArguments == null?null:parseFunctionParameterList(tryArguments, false).
										getChildren()));
					}else if(conExpression.equals("con.else")) {
						tryStatmentParts.add(new AbstractSyntaxTree.TryStatementPartElseNode(CodePosition.EMPTY, tryBody));
					}else if(conExpression.equals("con.finally")) {
						tryStatmentParts.add(new AbstractSyntaxTree.TryStatementPartFinallyNode(CodePosition.EMPTY, tryBody));
					}
				}

				//TODO line numbers
				nodes.add(new AbstractSyntaxTree.TryStatementNode(CodePosition.EMPTY, tryStatmentParts));
				return ast;
			}else if(conExpression.equals("con.loop") || conExpression.equals("con.while") || conExpression.equals("con.until") ||
					conExpression.equals("con.repeat") || conExpression.equals("con.foreach")) {
				List<AbstractSyntaxTree.LoopStatementPartNode> loopStatmentParts = new ArrayList<>();

				boolean blockBracketFlag = endsWithOpeningBracket;

				while(!tokens.isEmpty()) {
					trimFirstLine(tokens);

					tokenCountFirstLine = getTokenCountFirstLine(tokens);
					if(tokenCountFirstLine == -1)
						tokenCountFirstLine = tokens.size();
					endsWithOpeningBracket = tokenCountFirstLine > 0 && tokens.get(tokenCountFirstLine - 1).getTokenType() ==
							Token.TokenType.OPENING_BLOCK_BRACKET;

					if(tokenCountFirstLine == 0)
						break;

					conExpression = tokens.get(0).getValue();

					//"con." is optional if the curly brackets syntax is used
					if(endsWithOpeningBracket && !startsWithConExpression)
						conExpression = "con." + conExpression;

					if(blockBracketFlag) {
						//Remove "{" and "}" for the curly brackets if statement syntax
						CodePosition pos = tokens.get(tokenCountFirstLine - 1).pos;

						if(tokens.get(tokenCountFirstLine - 1).getTokenType() != Token.TokenType.OPENING_BLOCK_BRACKET)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.INVALID_CON_PART));

						tokens.remove(tokenCountFirstLine - 1);

						tokenCountFirstLine--;

						if(tokenCountFirstLine == 0 || tokens.get(0).getTokenType() != Token.TokenType.OTHER)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.INVALID_CON_PART));

						conExpression = tokens.get(0).getValue();

						//"con." is optional if "{" syntax is used
						if(!conExpression.startsWith("con."))
							conExpression = "con." + conExpression;

						trimFirstLine(tokens);

						tokenCountFirstLine = getTokenCountFirstLine(tokens);
						if(tokenCountFirstLine == -1)
							tokenCountFirstLine = tokens.size();
					}

					Token loopStatementPartToken;
					List<Token> loopCondition;
					if(conExpression.equals("con.else") || conExpression.equals("con.loop")) {
						loopStatementPartToken = tokens.remove(0);
						tokenCountFirstLine--;

						if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET && tokens.get(0).getValue().equals("(")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(loopStatementPartToken.pos, ParsingError.INVALID_CON_PART,
									"Loop/Else part with arguments"));

							return ast;
						}

						loopCondition = null;
					}else if(conExpression.equals("con.while") || conExpression.equals("con.until") ||
							conExpression.equals("con.repeat") || conExpression.equals("con.foreach")) {
						loopStatementPartToken = tokens.remove(0);
						tokenCountFirstLine--;

						if(tokenCountFirstLine == 0) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(loopStatementPartToken.pos, ParsingError.CONT_FLOW_ARG_MISSING,
									"Missing loop statement arguments"));

							return ast;
						}

						if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
								tokens.get(0).getValue().equals("(")) {
							int argumentsEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
							if(argumentsEndIndex == -1) {
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
										"Missing loop statement arguments"));
								return ast;
							}

							loopCondition = new ArrayList<>(tokens.subList(1, argumentsEndIndex));
							tokens.subList(0, argumentsEndIndex + 1).clear();
							tokenCountFirstLine -= argumentsEndIndex + 1;
						}else {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(loopStatementPartToken.pos, ParsingError.BRACKET_MISMATCH,
									"Bracket for loop statement is missing"));
							return ast;
						}

						if(tokenCountFirstLine != 0) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(loopStatementPartToken.pos, ParsingError.INVALID_CON_PART,
									"Trailing stuff behind loop arguments"));
							return ast;
						}
					}else if(!blockBracketFlag && conExpression.equals("con.endloop")) {
						tokens.remove(0);

						break;
					}else {
						//TODO lineNumber
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.INVALID_CON_PART,
								"Loop statement part is invalid"));
						return ast;
					}

					AbstractSyntaxTree loopBody = parseTokensInternal(tokens);
					if(loopBody == null) {
						//TODO line numbers
						nodes.add(new AbstractSyntaxTree.LoopStatementNode(CodePosition.EMPTY, loopStatmentParts));
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.EOF, "In loop body"
						));

						return ast;
					}

					//TODO line numbers
					if(conExpression.equals("con.else")) {
						loopStatmentParts.add(new AbstractSyntaxTree.LoopStatementPartElseNode(CodePosition.EMPTY, loopBody));
					}else if(conExpression.equals("con.loop")) {
						loopStatmentParts.add(new AbstractSyntaxTree.LoopStatementPartLoopNode(CodePosition.EMPTY, loopBody));
					}else if(conExpression.equals("con.while")) {
						AbstractSyntaxTree.OperationNode conNonNode = new AbstractSyntaxTree.OperationNode(parseOperationExpr(loopCondition),
								AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON, AbstractSyntaxTree.OperationNode.OperatorType.CONDITION);
						loopStatmentParts.add(new AbstractSyntaxTree.LoopStatementPartWhileNode(CodePosition.EMPTY, loopBody, conNonNode
						));
					}else if(conExpression.equals("con.until")) {
						AbstractSyntaxTree.OperationNode conNonNode = new AbstractSyntaxTree.OperationNode(parseOperationExpr(loopCondition),
								AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON, AbstractSyntaxTree.OperationNode.OperatorType.CONDITION);
						loopStatmentParts.add(new AbstractSyntaxTree.LoopStatementPartUntilNode(CodePosition.EMPTY, loopBody, conNonNode
						));
					}else if(conExpression.startsWith("con.repeat") || conExpression.startsWith("con.foreach")) {
						List<AbstractSyntaxTree.Node> arguments = convertCommaOperatorsToArgumentSeparators(parseOperationExpr(loopCondition));
						Iterator<AbstractSyntaxTree.Node> argumentIter = arguments.iterator();

						AbstractSyntaxTree.Node varPointerNode = null;
						boolean flag = false;
						while(argumentIter.hasNext()) {
							AbstractSyntaxTree.Node node = argumentIter.next();

							if(node.getNodeType() == AbstractSyntaxTree.NodeType.ARGUMENT_SEPARATOR || varPointerNode != null) {
								flag = true;
								break;
							}

							varPointerNode = node;
						}
						if(!flag) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.INVALID_CON_PART,
									"con.repeat or con.foreach arguments are invalid"));
							return ast;
						}

						List<AbstractSyntaxTree.Node> repeatCountArgument = new LinkedList<>();
						while(argumentIter.hasNext()) {
							AbstractSyntaxTree.Node node = argumentIter.next();

							if(node.getNodeType() == AbstractSyntaxTree.NodeType.ARGUMENT_SEPARATOR) {
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.INVALID_CON_PART,
										"con.repeat or con.foreach arguments are invalid"));
								return ast;
							}

							repeatCountArgument.add(node);
						}

						AbstractSyntaxTree.Node repeatCountOrArrayOrTextNode = repeatCountArgument.size() == 1?
								repeatCountArgument.get(0):new AbstractSyntaxTree.ListNode(repeatCountArgument);

						if(conExpression.equals("con.repeat"))
							loopStatmentParts.add(new AbstractSyntaxTree.LoopStatementPartRepeatNode(CodePosition.EMPTY, loopBody,
									varPointerNode, repeatCountOrArrayOrTextNode));
						else
							loopStatmentParts.add(new AbstractSyntaxTree.LoopStatementPartForEachNode(CodePosition.EMPTY, loopBody,
									varPointerNode, repeatCountOrArrayOrTextNode));
					}
				}

				//TODO line numbers
				nodes.add(new AbstractSyntaxTree.LoopStatementNode(CodePosition.EMPTY, loopStatmentParts));

				return ast;
			}else if(conExpression.equals("con.if")) {
				List<AbstractSyntaxTree.IfStatementPartNode> ifStatmentParts = new ArrayList<>();

				boolean blockBracketFlag = endsWithOpeningBracket;

				while(!tokens.isEmpty()) {
					trimFirstLine(tokens);

					tokenCountFirstLine = getTokenCountFirstLine(tokens);
					if(tokenCountFirstLine == -1)
						tokenCountFirstLine = tokens.size();
					endsWithOpeningBracket = tokenCountFirstLine > 0 && tokens.get(tokenCountFirstLine - 1).getTokenType() ==
							Token.TokenType.OPENING_BLOCK_BRACKET;

					if(tokenCountFirstLine == 0)
						break;

					conExpression = tokens.get(0).getValue();

					//"con." is optional if the curly brackets syntax is used
					if(endsWithOpeningBracket && !startsWithConExpression)
						conExpression = "con." + conExpression;

					if(blockBracketFlag) {
						//Remove "{" and "}" for the curly brackets if statement syntax
						CodePosition pos = tokens.get(tokenCountFirstLine - 1).pos;

						if(tokens.get(tokenCountFirstLine - 1).getTokenType() != Token.TokenType.OPENING_BLOCK_BRACKET)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.INVALID_CON_PART));

						tokens.remove(tokenCountFirstLine - 1);

						tokenCountFirstLine--;

						if(tokenCountFirstLine == 0 || tokens.get(0).getTokenType() != Token.TokenType.OTHER)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.INVALID_CON_PART));

						conExpression = tokens.get(0).getValue();

						//"con." is optional if "{" syntax is used
						if(!conExpression.startsWith("con."))
							conExpression = "con." + conExpression;

						trimFirstLine(tokens);

						tokenCountFirstLine = getTokenCountFirstLine(tokens);
						if(tokenCountFirstLine == -1)
							tokenCountFirstLine = tokens.size();
					}

					Token ifStatementPartToken;
					List<Token> ifCondition;
					if(conExpression.equals("con.else")) {
						ifStatementPartToken = tokens.remove(0);
						tokenCountFirstLine--;

						if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET && tokens.get(0).getValue().equals("(")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(ifStatementPartToken.pos, ParsingError.INVALID_CON_PART,
									"Else part with arguments"));

							return ast;
						}

						ifCondition = null;
					}else if(conExpression.equals("con.if") || conExpression.equals("con.elif")) {
						ifStatementPartToken = tokens.remove(0);
						tokenCountFirstLine--;

						if(tokenCountFirstLine == 0) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(ifStatementPartToken.pos, ParsingError.CONT_FLOW_ARG_MISSING,
									"Missing if statement arguments"));

							return ast;
						}

						if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
								tokens.get(0).getValue().equals("(")) {
							int argumentsEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
							if(argumentsEndIndex == -1) {
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
										"Missing if/elif statement arguments"));
								return ast;
							}

							ifCondition = new ArrayList<>(tokens.subList(1, argumentsEndIndex));
							tokens.subList(0, argumentsEndIndex + 1).clear();
							tokenCountFirstLine -= argumentsEndIndex + 1;
						}else {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(ifStatementPartToken.pos, ParsingError.BRACKET_MISMATCH,
									"Bracket for if statement is missing"));
							return ast;
						}

						if(tokenCountFirstLine != 0) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(ifStatementPartToken.pos, ParsingError.INVALID_CON_PART,
									"Trailing stuff behind if arguments"));
							return ast;
						}
					}else if(!blockBracketFlag && conExpression.equals("con.endif")) {
						tokens.remove(0);

						break;
					}else {
						//TODO lineNumber
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.INVALID_CON_PART,
								"If statement part is invalid"));
						return ast;
					}

					AbstractSyntaxTree ifBody = parseTokensInternal(tokens);
					if(ifBody == null) {
						//TODO line numbers
						nodes.add(new AbstractSyntaxTree.IfStatementNode(CodePosition.EMPTY, ifStatmentParts));
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.EOF, "In if body"
						));

						return ast;
					}

					//TODO line numbers
					if(ifCondition == null) {
						ifStatmentParts.add(new AbstractSyntaxTree.IfStatementPartElseNode(CodePosition.EMPTY, ifBody));
					}else {
						AbstractSyntaxTree.OperationNode conNonNode = new AbstractSyntaxTree.OperationNode(parseOperationExpr(ifCondition),
								AbstractSyntaxTree.OperationNode.Operator.CONDITIONAL_NON, AbstractSyntaxTree.OperationNode.OperatorType.CONDITION);

						ifStatmentParts.add(new AbstractSyntaxTree.IfStatementPartIfNode(CodePosition.EMPTY, ifBody, conNonNode));
					}
				}

				//TODO line numbers
				nodes.add(new AbstractSyntaxTree.IfStatementNode(CodePosition.EMPTY, ifStatmentParts));

				return ast;
			}else if(originalConExpression.startsWith("con.")) {
				return null;
			}
		}

		//Return values
		if(tokenCountFirstLine >= 1 && tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
				tokens.get(0).getValue().equals("return")) {
			//Return without value
			if(tokenCountFirstLine == 1) {
				Token returnStatementToken = tokens.remove(0);
				nodes.add(new AbstractSyntaxTree.ReturnNode(returnStatementToken.pos));

				return ast;
			}

			//Return with value
			tokens.remove(0);

			if(tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE) {
				AbstractSyntaxTree.AssignmentNode returnedNode = parseAssignment(tokens, true);
				nodes.add(new AbstractSyntaxTree.ReturnNode(returnedNode == null?parseLRvalue(tokens, true).convertToNode():returnedNode));

				return ast;
			}
		}

		//Throw values
		if(tokenCountFirstLine > 1 && tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
				tokens.get(0).getValue().equals("throw") && tokens.get(1).getTokenType() == Token.TokenType.WHITESPACE) {
			tokens.remove(0);

			List<AbstractSyntaxTree.Node> arguments = convertCommaOperatorsToArgumentSeparators(parseOperationExpr(tokens));
			Iterator<AbstractSyntaxTree.Node> argumentIter = arguments.iterator();

			List<AbstractSyntaxTree.Node> errorNodes = new LinkedList<>();
			boolean flag = false;
			while(argumentIter.hasNext()) {
				AbstractSyntaxTree.Node node = argumentIter.next();

				if(node.getNodeType() == AbstractSyntaxTree.NodeType.ARGUMENT_SEPARATOR) {
					flag = true;
					break;
				}

				errorNodes.add(node);
			}
			if(!flag && errorNodes.isEmpty()) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.LEXER_ERROR,
						"throw arguments are invalid"));
				return ast;
			}

			List<AbstractSyntaxTree.Node> messageNodes = new LinkedList<>();
			while(argumentIter.hasNext()) {
				AbstractSyntaxTree.Node node = argumentIter.next();

				if(node.getNodeType() == AbstractSyntaxTree.NodeType.ARGUMENT_SEPARATOR) {
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.LEXER_ERROR,
							"throw arguments are invalid"));
					return ast;
				}

				messageNodes.add(node);
			}

			AbstractSyntaxTree.Node errorNode = errorNodes.size() == 1?errorNodes.get(0):
					new AbstractSyntaxTree.ListNode(errorNodes);
			AbstractSyntaxTree.Node messageNode = messageNodes.isEmpty()?null:(messageNodes.size() == 1?messageNodes.get(0):
					new AbstractSyntaxTree.ListNode(messageNodes));

			nodes.add(new AbstractSyntaxTree.ThrowNode(errorNode, messageNode));

			return ast;
		}

		//Function definition
		if(tokenCountFirstLine > 1 && tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
				tokens.get(0).getValue().equals("function") && endsWithOpeningBracket) {
			Token functionDefinitionStartToken = tokens.remove(0);
			tokenCountFirstLine--;

			if(tokens.get(0).getTokenType() != Token.TokenType.WHITESPACE) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(functionDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid function definition: Whitespace is missing after \"function\""
				));

				return ast;
			}

			tokens.remove(0);
			tokenCountFirstLine--;

			boolean overloaded = tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
					tokens.get(0).getValue().equals("overload") && tokens.get(1).getTokenType() == Token.TokenType.WHITESPACE;
			if(overloaded) {
				tokens.remove(0);
				tokens.remove(0);

				tokenCountFirstLine -= 2;
			}

			boolean combinator = tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
					tokens.get(0).getValue().equals("combinator") && tokens.get(1).getTokenType() == Token.TokenType.WHITESPACE;
			if(combinator) {
				tokens.remove(0);
				tokens.remove(0);

				tokenCountFirstLine -= 2;
			}

			if(!(tokens.get(0).getTokenType() == Token.TokenType.IDENTIFIER &&
					LangPatterns.matches(tokens.get(0).getValue(), LangPatterns.VAR_NAME_NORMAL_FUNCTION_WITHOUT_PREFIX)) &&
					!(tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
							LangPatterns.matches(tokens.get(0).getValue(), LangPatterns.WORD))) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(functionDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid function definition: Invalid function identifier: " + tokens.get(0).getValue()
				));

				return ast;
			}

			Token functionNameToken = tokens.remove(0);
			tokenCountFirstLine--;

			String functionName = functionNameToken.getValue();
			if(!functionName.startsWith("fp.") && !functionName.startsWith("$"))
				functionName = "fp." + functionName;

			if(tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE) {
				tokens.remove(0);
				tokenCountFirstLine--;
			}

			if(tokens.get(0).getTokenType() != Token.TokenType.OPENING_BRACKET ||
					!tokens.get(0).getValue().equals("(")) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(functionDefinitionStartToken.pos, ParsingError.BRACKET_MISMATCH,
						"Bracket is missing in parameter list in function definition"));

				return ast;
			}

			int bracketEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
			if(bracketEndIndex == -1) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(functionNameToken.pos, ParsingError.BRACKET_MISMATCH,
						"Bracket is missing in parameter list in function definition"));
				return ast;
			}

			List<Token> parameterList = new ArrayList<>(tokens.subList(1, bracketEndIndex));
			tokens.subList(0, bracketEndIndex + 1).clear();
			tokenCountFirstLine -= bracketEndIndex + 1;

			String typeConstraint = null;
			if(tokenCountFirstLine > 2 && tokens.get(0).getTokenType() == Token.TokenType.OPERATOR && tokens.get(0).getValue().equals(":") &&
					tokens.get(1).getTokenType() == Token.TokenType.OPENING_BRACKET && tokens.get(1).getValue().equals("{")) {
				tokens.remove(0);
				tokenCountFirstLine--;

				bracketEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "{", "}", true);
				if(bracketEndIndex == -1) {
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(functionNameToken.pos, ParsingError.BRACKET_MISMATCH,
							"Bracket is missing in return type constraint in function definition"
					));
					return ast;
				}

				List<Token> typeConstraintTokens = new ArrayList<>(tokens.subList(0, bracketEndIndex + 1));
				tokens.subList(0, bracketEndIndex + 1).clear();
				tokenCountFirstLine -= bracketEndIndex + 1;

				typeConstraint = parseTypeConstraint(typeConstraintTokens, false, nodes);
			}

			if(tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE) {
				tokens.remove(0);
				tokenCountFirstLine--;
			}

			if(tokenCountFirstLine != 1) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(functionNameToken.pos, ParsingError.LEXER_ERROR,
						"Invalid tokens after function return type constraint"));
				return ast;
			}

			tokens.remove(0);

			if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.EOL)
				tokens.remove(0);

			nodes.addAll(parseFunctionDefinition(functionName, overloaded, combinator, parameterList, typeConstraint,
					tokens).getChildren());

			return ast;
		}

		//Struct definition
		if(tokenCountFirstLine > 1 && tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
				tokens.get(0).getValue().equals("struct") && endsWithOpeningBracket) {
			Token structDefinitionStartToken = tokens.remove(0);
			tokenCountFirstLine--;

			if(tokens.get(0).getTokenType() != Token.TokenType.WHITESPACE) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(structDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid struct definition: Whitespace is missing after \"struct\""));

				return ast;
			}

			tokens.remove(0);
			tokenCountFirstLine--;

			if(tokens.get(0).getTokenType() != Token.TokenType.IDENTIFIER ||
					!LangPatterns.matches(tokens.get(0).getValue(), LangPatterns.VAR_NAME_NORMAL_ARRAY_WITHOUT_PREFIX)) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(structDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid struct definition: Invalid struct identifier: " + tokens.get(0).getValue()
				));

				return ast;
			}

			Token structNameToken = tokens.remove(0);
			tokenCountFirstLine--;

			String structName = structNameToken.getValue();

			if(tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE) {
				tokens.remove(0);
				tokenCountFirstLine--;
			}

			if(tokenCountFirstLine != 1) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(structDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid tokens after struct constraint"));
				return ast;
			}

			tokens.remove(0);

			if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.EOL)
				tokens.remove(0);

			nodes.addAll(parseStructDefinition(structName, tokens).getChildren());

			return ast;
		}

		//Class definition
		if(tokenCountFirstLine > 1 && tokens.get(0).getTokenType() == Token.TokenType.OTHER &&
				tokens.get(0).getValue().equals("class") && endsWithOpeningBracket) {
			Token structDefinitionStartToken = tokens.remove(0);
			tokenCountFirstLine--;

			if(tokens.get(0).getTokenType() != Token.TokenType.WHITESPACE) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(structDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid class definition: Whitespace is missing after \"class\""));

				return ast;
			}

			tokens.remove(0);
			tokenCountFirstLine--;

			if(tokens.get(0).getTokenType() != Token.TokenType.IDENTIFIER ||
					!LangPatterns.matches(tokens.get(0).getValue(), LangPatterns.VAR_NAME_NORMAL_ARRAY_WITHOUT_PREFIX)) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(structDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid class definition: Invalid class identifier: " + tokens.get(0).getValue()
				));

				return ast;
			}

			Token classNameToken = tokens.remove(0);
			tokenCountFirstLine--;

			String className = classNameToken.getValue();

			if(tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE) {
				tokens.remove(0);
				tokenCountFirstLine--;
			}

			List<Token> parentClassesToken = new ArrayList<>();
			if(tokens.get(0).getTokenType() == Token.TokenType.OPERATOR &&
					tokens.get(0).getValue().equals("<")) {
				//TODO check for matching brackets ("<" and ">")
				int parentClassesEndIndex = -1;
				for(int i = tokenCountFirstLine;i >= 0;i--) {
					if(tokens.get(i).getTokenType() == Token.TokenType.OPERATOR && tokens.get(i).getValue().equals(">")) {
						parentClassesEndIndex = i;

						break;
					}
				}
				if(parentClassesEndIndex == -1) {
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
							"Bracket is missing in class definition"));

					return ast;
				}

				parentClassesToken = new ArrayList<>(tokens.subList(1, parentClassesEndIndex));
				tokens.subList(0, parentClassesEndIndex + 1).clear();
				tokenCountFirstLine -= parentClassesEndIndex + 1;

				if(tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE) {
					tokens.remove(0);
					tokenCountFirstLine--;
				}
			}

			if(tokenCountFirstLine != 1) {
				nodes.add(new AbstractSyntaxTree.ParsingErrorNode(structDefinitionStartToken.pos, ParsingError.LEXER_ERROR,
						"Invalid tokens after class definition"));
				return ast;
			}

			tokens.remove(0);

			if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.EOL)
				tokens.remove(0);

			nodes.addAll(parseClassDefinition(className, parentClassesToken, tokens).getChildren());

			return ast;
		}

		nodes.addAll(parseToken(tokens).getChildren());

		return ast;
	}

	/**
	 * @return true if the parser flag was valid else false
	 */
	private boolean parseParserFlags(String parserFlag, AbstractSyntaxTree.Node value) {
		//String[] tokens = LangPatterns.GENERAL_DOT.split(parserFlag);

		return false;
	}

	private AbstractSyntaxTree parseTranslationKey(List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		//TODO line numbers
		nodes.add(new AbstractSyntaxTree.TextValueNode(CodePosition.EMPTY, ""));

		if(tokens.size() >= 2 && tokens.get(0).getTokenType() == Token.TokenType.OPERATOR &&
				tokens.get(0).getValue().equals("%") &&
				tokens.get(1).getTokenType() == Token.TokenType.IDENTIFIER &&
				tokens.get(1).getValue().startsWith("$")) {
			//Prepare "%$" for translation key
			tokens.remove(0);
		}

		tokenProcessing:
		while(!tokens.isEmpty()) {
			Token t = tokens.get(0);

			switch(t.getTokenType()) {
				case EOL:
				case EOF:
					break tokenProcessing;

				case START_COMMENT:
				case START_DOC_COMMENT:
					parseCommentTokens(tokens, nodes);

					break;

				case LITERAL_NULL:
				case LITERAL_TEXT:
				case LITERAL_NUMBER:
				case ARGUMENT_SEPARATOR:
				case ASSIGNMENT:
				case OPERATOR:
				case OPENING_BRACKET:
				case CLOSING_BRACKET:
				case OPENING_BLOCK_BRACKET:
				case CLOSING_BLOCK_BRACKET:
				case WHITESPACE:
				case OTHER:
					tokens.remove(0);

					nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

					break;

				case ESCAPE_SEQUENCE:
					tokens.remove(0);

					parseEscapeSequenceToken(t, nodes);

					break;

				case LEXER_ERROR:
					tokens.remove(0);

					parseLexerErrorToken(t, nodes);

					break;

				case START_MULTILINE_TEXT:
					tokens.remove(0);
					do {
						t = tokens.remove(0);

						if(t.getTokenType() == Token.TokenType.LITERAL_TEXT ||
								t.getTokenType() == Token.TokenType.EOL)
							nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						if(t.getTokenType() == Token.TokenType.ESCAPE_SEQUENCE)
							parseEscapeSequenceToken(t, nodes);

						if(t.getTokenType() == Token.TokenType.LEXER_ERROR)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR, t.getValue()));
					}while(t.getTokenType() != Token.TokenType.END_MULTILINE_TEXT);

					break;

				case IDENTIFIER:
				case PARSER_FUNCTION_IDENTIFIER:
					if(t.getTokenType() == Token.TokenType.IDENTIFIER &&
							!LangPatterns.matches(t.getValue(), LangPatterns.VAR_NAME_FULL)) {
						tokens.remove(0);

						nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						break;
					}

					AbstractSyntaxTree.Node ret = t.getTokenType() == Token.TokenType.IDENTIFIER?
							parseVariableNameAndFunctionCall(tokens):parseParserFunctionCall(tokens);
					if(ret != null)
						nodes.add(ret);

					break;

				case LINE_CONTINUATION:
				case END_COMMENT:
				case END_MULTILINE_TEXT:
				case SINGLE_LINE_TEXT_QUOTES:
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
							"Invalid token type for translation key expression: \"" + t.getTokenType().name() + "\""
					));

					break tokenProcessing;
			}
		}

		return ast;
	}

	private AbstractSyntaxTree parseLRvalue(List<Token> tokens, boolean isRvalue) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		int tokenCountFirstLine = getTokenCountFirstLine(tokens);
		if(tokenCountFirstLine == -1)
			tokenCountFirstLine = tokens.size();

		if(isRvalue) {
			if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
					tokens.get(0).getValue().equals("(")) {
				//Possible function definition

				int parameterListEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
				if(parameterListEndIndex == -1) {
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
							"Bracket is missing in function definition"));

					return ast;
				}

				List<Token> parameterListTokens = new ArrayList<>(tokens.subList(1, parameterListEndIndex));
				List<AbstractSyntaxTree.Node> parameterList = parseFunctionParameterList(parameterListTokens, true).getChildren();

				int tokenIndex = parameterListEndIndex + 1;
				String returnTypeConstraint = null;
				if(tokenCountFirstLine > tokenIndex + 1 &&
						tokens.get(tokenIndex).getTokenType() == Token.TokenType.OPERATOR &&
						tokens.get(tokenIndex).getValue().equals(":") &&
						tokens.get(tokenIndex + 1).getTokenType() == Token.TokenType.OPENING_BRACKET &&
						tokens.get(tokenIndex + 1).getValue().equals("{")) {
					int returnTypeConstraintEndIndex = LangUtils.getIndexOfMatchingBracket(tokens,
							tokenIndex + 1, Integer.MAX_VALUE, "{", "}", true);
					if(returnTypeConstraintEndIndex == -1) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(tokens.get(0).pos, ParsingError.BRACKET_MISMATCH,
								"Bracket is missing in return type constraint of function definition"
						));

						return ast;
					}

					List<Token> typeConstraintTokens = new ArrayList<>(tokens.subList(tokenIndex + 1, returnTypeConstraintEndIndex + 1));
					returnTypeConstraint = parseTypeConstraint(typeConstraintTokens, false, nodes);

					tokenIndex = returnTypeConstraintEndIndex + 1;
				}

				if(tokenCountFirstLine > tokenIndex + 2 && tokens.get(tokenIndex).getTokenType() == Token.TokenType.WHITESPACE &&
						tokens.get(tokenIndex + 1).getTokenType() == Token.TokenType.OPERATOR &&
						tokens.get(tokenIndex + 1).getValue().equals("->") &&
						tokens.get(tokenIndex + 2).getTokenType() == Token.TokenType.WHITESPACE) {
					tokens.subList(0, tokenIndex + 3).clear();
					tokenCountFirstLine -= tokenIndex + 3;

					//TODO line numbers
					if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BLOCK_BRACKET) {
						tokens.remove(0);

						nodes.add(new AbstractSyntaxTree.FunctionDefinitionNode(CodePosition.EMPTY, null, false, false, langDocComment, parameterList,
								returnTypeConstraint, parseTokensInternal(tokens)));
						langDocComment = null;
					}else {
						List<Token> functionBody = new ArrayList<>(tokens.subList(0, tokenCountFirstLine));
						tokens.subList(0, tokenCountFirstLine).clear();

						nodes.add(new AbstractSyntaxTree.FunctionDefinitionNode(CodePosition.EMPTY, null, false, false, langDocComment, parameterList,
								returnTypeConstraint, parseTokensInternal(functionBody)));
						langDocComment = null;
					}

					return ast;
				}
			}

			if(tokenCountFirstLine == 1 && tokens.get(0).getTokenType() == Token.TokenType.IDENTIFIER &&
					LangPatterns.matches(tokens.get(0).getValue(), LangPatterns.VAR_NAME_FUNC_PTR_WITH_FUNCS)) {
				//Function pointer copying

				Token t = tokens.remove(0);
				nodes.add(new AbstractSyntaxTree.UnprocessedVariableNameNode(t.pos, t.getValue()));
				return ast;
			}else if(tokenCountFirstLine == 1 && tokenCountFirstLine != tokens.size() &&
					tokens.get(0).getTokenType() == Token.TokenType.OPENING_BLOCK_BRACKET) {
				//Struct definition

				tokens.remove(0);
				tokens.remove(0);
				nodes.addAll(parseStructDefinition(null, tokens).getChildren());

				return ast;
			}else if(tokenCountFirstLine >= 3 && tokenCountFirstLine != tokens.size() && tokens.get(0).getTokenType() == Token.TokenType.OPERATOR &&
					tokens.get(0).getValue().equals("<") && tokens.get(tokenCountFirstLine - 2).getTokenType() == Token.TokenType.OPERATOR &&
					tokens.get(tokenCountFirstLine - 2).getValue().equals(">") &&
					tokens.get(tokenCountFirstLine - 1).getTokenType() == Token.TokenType.OPENING_BLOCK_BRACKET) {
				//Class definition

				//TODO check for matching brackets ("<" and ">")
				List<Token> parentClassesToken = new ArrayList<>(tokens.subList(1, tokenCountFirstLine - 2));
				tokens.subList(0, tokenCountFirstLine + 1).clear();

				nodes.addAll(parseClassDefinition(null, parentClassesToken, tokens).getChildren());

				return ast;
			}
		}

		nodes.addAll(parseToken(tokens).getChildren());

		return ast;
	}

	private AbstractSyntaxTree parseFunctionDefinition(String functionName, boolean overloaded, boolean combinator,
													   List<Token> parameterListTokens, String functionReturnValueTypeConstraint,
													   List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		List<AbstractSyntaxTree.Node> parameterListNodes = parseFunctionParameterList(parameterListTokens, true).getChildren();

		//TODO line numbers
		nodes.add(new AbstractSyntaxTree.FunctionDefinitionNode(CodePosition.EMPTY, functionName, overloaded, combinator,
				langDocComment, parameterListNodes, functionReturnValueTypeConstraint, parseTokensInternal(tokens)));
		langDocComment = null;

		return ast;
	}

	private AbstractSyntaxTree parseStructDefinition(String structName, List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		boolean hasEndBrace = false;

		List<String> memberNames = new LinkedList<>();
		List<String> typeConstraints = new LinkedList<>();

		tokenProcessing:
		while(!tokens.isEmpty()) {
			Token t = tokens.get(0);

			switch(t.getTokenType()) {
				case EOF:
					break tokenProcessing;

				case EOL:
					tokens.remove(0);

					trimFirstLine(tokens);

					break;

				case WHITESPACE:
					tokens.remove(0);

					break;

				case START_COMMENT:
				case START_DOC_COMMENT:
					parseCommentTokens(tokens, nodes);

					break;

				case CLOSING_BLOCK_BRACKET:
					tokens.remove(0);

					hasEndBrace = true;

					break tokenProcessing;

				case IDENTIFIER:
					if(!LangPatterns.matches(t.getValue(), LangPatterns.VAR_NAME_WITHOUT_PREFIX)) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
								"Invalid struct member name: \"" + t.getValue() + "\""));

						return ast;
					}

					Token identifierToken = tokens.remove(0);

					String typeConstraint = null;
					if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
							tokens.get(0).getValue().equals("{")) {
						int bracketEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "{", "}", true);
						if(bracketEndIndex == -1) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(identifierToken.pos, ParsingError.BRACKET_MISMATCH,
									"Bracket is missing in type constraint in struct definition for member: \"" +
											identifierToken.getValue() + "\""));

							return ast;
						}

						List<Token> typeConstraintTokens = new ArrayList<>(tokens.subList(0, bracketEndIndex + 1));
						tokens.subList(0, bracketEndIndex + 1).clear();

						typeConstraint = parseTypeConstraint(typeConstraintTokens, false, nodes);
					}

					if(memberNames.contains(identifierToken.getValue())) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(identifierToken.pos, ParsingError.INVALID_ASSIGNMENT,
								"Duplicated struct member name: \"" + identifierToken.getValue() + "\""
						));

						return ast;
					}

					memberNames.add(identifierToken.getValue());
					typeConstraints.add(typeConstraint);

					break;

				case LEXER_ERROR:
					tokens.remove(0);

					parseLexerErrorToken(t, nodes);

					break tokenProcessing;

				case PARSER_FUNCTION_IDENTIFIER:
				case LITERAL_NULL:
				case LITERAL_NUMBER:
				case LITERAL_TEXT:
				case ARGUMENT_SEPARATOR:
				case ASSIGNMENT:
				case OTHER:
				case OPERATOR:
				case OPENING_BRACKET:
				case CLOSING_BRACKET:
				case OPENING_BLOCK_BRACKET:
				case ESCAPE_SEQUENCE:
				case START_MULTILINE_TEXT:
				case LINE_CONTINUATION:
				case END_COMMENT:
				case END_MULTILINE_TEXT:
				case SINGLE_LINE_TEXT_QUOTES:
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
							"Invalid token type for struct definition expression: \"" + t.getTokenType().name() + "\""));

					return ast;
			}
		}

		if(!hasEndBrace) {
			//TODO line numbers
			nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.EOF, "\"}\" is missing in struct definition"
			));

			return ast;
		}

		//TODO line numbers
		nodes.add(new AbstractSyntaxTree.StructDefinitionNode(CodePosition.EMPTY, structName, memberNames, typeConstraints));

		return ast;
	}

	private AbstractSyntaxTree parseClassDefinition(String className, List<Token> parentClassesToken, List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		List<AbstractSyntaxTree.Node> parentClasses = parseFunctionParameterList(parentClassesToken, false).getChildren();

		boolean hasEndBrace = false;

		List<String> staticMemberNames = new LinkedList<>();
		List<String> staticMemberTypeConstraints = new LinkedList<>();
		List<AbstractSyntaxTree.Node> staticMemberValues = new LinkedList<>();
		List<Boolean> staticMemberFinalFlag = new LinkedList<>();

		List<String> memberNames = new LinkedList<>();
		List<String> memberTypeConstraints = new LinkedList<>();
		List<Boolean> memberFinalFlag = new LinkedList<>();

		List<String> methodNames = new LinkedList<>();
		List<AbstractSyntaxTree.Node> methodDefinitions = new LinkedList<>();
		List<Boolean> methodOverrideFlag = new LinkedList<>();

		List<AbstractSyntaxTree.Node> constructorDefinitions = new LinkedList<>();

		tokenProcessing:
		while(!tokens.isEmpty()) {
			Token t = tokens.get(0);

			switch(t.getTokenType()) {
				case EOF:
					break tokenProcessing;

				case EOL:
					tokens.remove(0);

					trimFirstLine(tokens);

					break;

				case WHITESPACE:
					tokens.remove(0);

					break;

				case START_COMMENT:
				case START_DOC_COMMENT:
					parseCommentTokens(tokens, nodes);

					break;

				case CLOSING_BLOCK_BRACKET:
					tokens.remove(0);

					hasEndBrace = true;

					break tokenProcessing;

				case OTHER:
				case OPERATOR:
					char visibility = t.getValue().charAt(0);
					if(visibility != '-' && visibility != '~' && visibility != '+') {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
								"Invalid visibility specifier (One of [\"-\", \"~\", \"+\"] must be used)"
						));

						return ast;
					}
					tokens.remove(0);

					if(tokens.isEmpty()) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
								"Missing value after visibility specifier"));

						return ast;
					}

					//Constructor methods
					t = tokens.get(0);
					if(t.getTokenType() == Token.TokenType.OTHER && t.getValue().equals("construct")) {
						tokens.remove(0);

						if(tokens.isEmpty()) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Missing value after construct method"));

							return ast;
						}

						t = tokens.get(0);
						if(t.getTokenType() != Token.TokenType.ASSIGNMENT || !t.getValue().equals(" = ")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Invalid assignment for constructor (only \" = \" is allowed)"));

							return ast;
						}

						tokens.remove(0);

						constructorDefinitions.add(parseLRvalue(tokens, true).convertToNode());

						continue tokenProcessing;
					}

					//Methods
					boolean isOverrideMethod = tokens.size() >= 2 && t.getTokenType() == Token.TokenType.OTHER &&
							t.getValue().equals("override") && tokens.get(1).getTokenType() == Token.TokenType.OPERATOR &&
							tokens.get(1).getValue().equals(":");
					if(isOverrideMethod) {
						tokens.remove(0);
						tokens.remove(0);

						if(tokens.isEmpty()) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Missing identifier after override keyword"));

							return ast;
						}

						t = tokens.get(0);
					}

					if(t.getTokenType() == Token.TokenType.IDENTIFIER && t.getValue().startsWith("op:")) {
						if(visibility != '+') {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
									"Invalid visibility for operator method (only \"+\" is allowed)"));

							return ast;
						}

						Token methodNameToken = tokens.remove(0);
						String methodName = methodNameToken.getValue();
						if(!LangPatterns.matches(methodName, LangPatterns.OPERATOR_METHOD_NAME)) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Invalid operator method name: \"" + methodName + "\""));

							return ast;
						}

						t = tokens.get(0);
						if(t.getTokenType() != Token.TokenType.ASSIGNMENT || !t.getValue().equals(" = ")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
									"Invalid assignment for operator method (only \" = \" is allowed)"));

							return ast;
						}

						tokens.remove(0);

						AbstractSyntaxTree.Node rvalueNode = parseLRvalue(tokens, true).convertToNode();

						methodNames.add(methodName);
						methodDefinitions.add(rvalueNode);
						methodOverrideFlag.add(isOverrideMethod);

						continue tokenProcessing;
					}

					if(t.getTokenType() == Token.TokenType.IDENTIFIER && t.getValue().startsWith("to:")) {
						if(visibility != '+') {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
									"Invalid visibility for conversion method (only \"+\" is allowed)"));

							return ast;
						}

						Token methodNameToken = tokens.remove(0);
						String methodName = methodNameToken.getValue();
						if(!LangPatterns.matches(methodName, LangPatterns.CONVERSION_METHOD_NAME)) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Invalid conversion method name: \"" + methodName + "\""));

							return ast;
						}

						t = tokens.get(0);
						if(t.getTokenType() != Token.TokenType.ASSIGNMENT || !t.getValue().equals(" = ")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
									"Invalid assignment for conversion method (only \" = \" is allowed)"));

							return ast;
						}

						tokens.remove(0);

						AbstractSyntaxTree.Node rvalueNode = parseLRvalue(tokens, true).convertToNode();

						methodNames.add(methodName);
						methodDefinitions.add(rvalueNode);
						methodOverrideFlag.add(isOverrideMethod);

						continue tokenProcessing;
					}

					if(t.getTokenType() == Token.TokenType.IDENTIFIER &&
							LangPatterns.matches(t.getValue(), LangPatterns.METHOD_NAME)) {
						Token methodNameToken = tokens.remove(0);
						String methodName = methodNameToken.getValue();

						if(tokens.isEmpty()) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Missing value after normal method"));

							return ast;
						}

						t = tokens.get(0);
						if(t.getTokenType() != Token.TokenType.ASSIGNMENT || !t.getValue().equals(" = ")) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
									"Invalid assignment for method (only \" = \" is allowed)"));

							return ast;
						}

						tokens.remove(0);

						AbstractSyntaxTree.Node rvalueNode = parseLRvalue(tokens, true).convertToNode();

						methodNames.add(methodName);
						methodDefinitions.add(rvalueNode);
						methodOverrideFlag.add(isOverrideMethod);

						continue tokenProcessing;
					}

					if(isOverrideMethod) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
								"The override keyword can only be used for methods"));

						return ast;
					}

					//Members
					boolean isStaticMember = tokens.size() >= 2 && t.getTokenType() == Token.TokenType.OTHER &&
							t.getValue().equals("static") && tokens.get(1).getTokenType() == Token.TokenType.OPERATOR &&
							tokens.get(1).getValue().equals(":");
					if(isStaticMember) {
						tokens.remove(0);
						tokens.remove(0);

						if(tokens.isEmpty()) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Missing identifier after static keyword"));

							return ast;
						}

						t = tokens.get(0);
					}

					boolean isFinalMember = tokens.size() >= 2 && t.getTokenType() == Token.TokenType.OTHER &&
							t.getValue().equals("final") && tokens.get(1).getTokenType() == Token.TokenType.OPERATOR &&
							tokens.get(1).getValue().equals(":");
					if(isFinalMember) {
						tokens.remove(0);
						tokens.remove(0);

						if(tokens.isEmpty()) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Missing identifier after final keyword"));

							return ast;
						}

						t = tokens.get(0);
					}

					//Also check if "static:" is after "final:"
					if(!isStaticMember && isFinalMember && tokens.size() >= 2 && t.getTokenType() == Token.TokenType.OTHER &&
							t.getValue().equals("static") && tokens.get(1).getTokenType() == Token.TokenType.OPERATOR &&
							tokens.get(1).getValue().equals(":")) {
						isStaticMember = true;

						tokens.remove(0);
						tokens.remove(0);

						if(tokens.isEmpty()) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.EOF,
									"Missing identifier after static keyword"));

							return ast;
						}

						t = tokens.get(0);
					}

					if(t.getTokenType() != Token.TokenType.IDENTIFIER) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
								"Invalid token type for class definition expression: \"" + t.getTokenType().name() + "\""
						));

						return ast;
					}

					if(!LangPatterns.matches(t.getValue(), LangPatterns.VAR_NAME_WITHOUT_PREFIX)) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
								"Invalid " + (isStaticMember?"static ":"") + "member name: \"" + t.getValue() + "\""
						));

						return ast;
					}

					Token memberNameToken = tokens.remove(0);
					String memberName = memberNameToken.getValue();

					String typeConstraint = null;
					if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
							tokens.get(0).getValue().equals("{")) {
						int bracketEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "{", "}", true);
						if(bracketEndIndex == -1) {
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(memberNameToken.pos, ParsingError.BRACKET_MISMATCH,
									"Bracket is missing in type constraint in class definition for " +
											(isStaticMember?"static ":"") + "member: \"" + memberNameToken.getValue() + "\""
							));

							return ast;
						}

						List<Token> typeConstraintTokens = new ArrayList<>(tokens.subList(0, bracketEndIndex + 1));
						tokens.subList(0, bracketEndIndex + 1).clear();

						typeConstraint = parseTypeConstraint(typeConstraintTokens, false, nodes);
					}

					if((isStaticMember?staticMemberNames:memberNames).contains(memberName)) {
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(memberNameToken.pos, ParsingError.INVALID_ASSIGNMENT,
								"Duplicated " + (isStaticMember?"static ":"") + " member name: \"" + memberName + "\""
						));

						return ast;
					}

					if(isStaticMember) {
						AbstractSyntaxTree.Node staticMemberValue = null;
						if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.ASSIGNMENT) {
							Token assignmentToken = tokens.remove(0);
							String assignmentOperator = assignmentToken.getValue();

							if(tokens.isEmpty() || tokens.get(0).getTokenType() == Token.TokenType.EOL ||
									tokens.get(0).getTokenType() == Token.TokenType.EOF) {
								if(!assignmentOperator.equals("=") && !assignmentOperator.equals(" =")) {
									nodes.add(new AbstractSyntaxTree.ParsingErrorNode(assignmentToken.pos, ParsingError.INVALID_ASSIGNMENT,
											"Rvalue is missing in member assignment"));

									return ast;
								}

								staticMemberValue = assignmentOperator.equals("=")?
										new AbstractSyntaxTree.TextValueNode(assignmentToken.pos, ""):
										new AbstractSyntaxTree.NullValueNode(assignmentToken.pos);
							}else {
								switch(assignmentOperator) {
									case "=":
										staticMemberValue = parseSimpleAssignmentValue(tokens).convertToNode();
										break;
									case " = ":
										staticMemberValue = parseLRvalue(tokens, true).convertToNode();
										break;
									case " ?= ":
										staticMemberValue = parseCondition(tokens);
										break;
									case " := ":
										staticMemberValue = parseMathExpr(tokens);
										break;
									case " $= ":
										staticMemberValue = parseOperationExpr(tokens);
										break;

									default:
										nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_ASSIGNMENT,
												"Invalid assignment for static member (only the following operators are allowed: \"=\", \" = \", " +
														"\" ?= \", \" := \", and \" $= \")"));

										return ast;
								}
							}
						}

						staticMemberNames.add(memberName);
						staticMemberTypeConstraints.add(typeConstraint);
						staticMemberValues.add(staticMemberValue);
						staticMemberFinalFlag.add(isFinalMember);

						continue tokenProcessing;
					}

					memberNames.add(memberName);
					memberTypeConstraints.add(typeConstraint);
					memberFinalFlag.add(isFinalMember);

					break;

				case LEXER_ERROR:
					tokens.remove(0);

					parseLexerErrorToken(t, nodes);

					break tokenProcessing;

				case IDENTIFIER:
				case PARSER_FUNCTION_IDENTIFIER:
				case LITERAL_NULL:
				case LITERAL_NUMBER:
				case LITERAL_TEXT:
				case ARGUMENT_SEPARATOR:
				case ASSIGNMENT:
				case OPENING_BRACKET:
				case CLOSING_BRACKET:
				case OPENING_BLOCK_BRACKET:
				case ESCAPE_SEQUENCE:
				case START_MULTILINE_TEXT:
				case LINE_CONTINUATION:
				case END_COMMENT:
				case END_MULTILINE_TEXT:
				case SINGLE_LINE_TEXT_QUOTES:
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
							"Invalid token type for class definition expression: \"" + t.getTokenType().name() + "\""
					));

					return ast;
			}
		}

		if(!hasEndBrace) {
			//TODO line numbers
			nodes.add(new AbstractSyntaxTree.ParsingErrorNode(CodePosition.EMPTY, ParsingError.EOF, "\"}\" is missing in class definition"
			));

			return ast;
		}

		//TODO line numbers
		nodes.add(new AbstractSyntaxTree.ClassDefinitionNode(CodePosition.EMPTY, className, staticMemberNames,
				staticMemberTypeConstraints, staticMemberValues, staticMemberFinalFlag, memberNames, memberTypeConstraints,
				memberFinalFlag, methodNames, methodDefinitions, methodOverrideFlag, constructorDefinitions, parentClasses
		));

		return ast;
	}

	private AbstractSyntaxTree parseToken(List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		tokenProcessing:
		while(!tokens.isEmpty()) {
			Token t = tokens.get(0);

			switch(t.getTokenType()) {
				case EOL:
				case EOF:
					break tokenProcessing;

				case START_COMMENT:
				case START_DOC_COMMENT:
					parseCommentTokens(tokens, nodes);

					break;

				case LITERAL_NULL:
					tokens.remove(0);

					nodes.add(new AbstractSyntaxTree.NullValueNode(t.pos));
					break;

				case LITERAL_TEXT:
				case ARGUMENT_SEPARATOR:
				case ASSIGNMENT:
				case CLOSING_BRACKET:
				case OPENING_BLOCK_BRACKET:
				case CLOSING_BLOCK_BRACKET:
				case WHITESPACE:
					tokens.remove(0);

					nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

					break;

				case OTHER:
					tokens.remove(0);

					parseTextAndCharValue(new ArrayList<>(Arrays.asList(t)), nodes);

					break;

				case OPERATOR:
					tokens.remove(0);

					if(nodes.isEmpty() && (t.getValue().equals("-") || t.getValue().equals("+")) && !tokens.isEmpty() &&
							tokens.get(0).getTokenType() == Token.TokenType.LITERAL_NUMBER) {
						Token numberToken = tokens.remove(0);

						Token combinedNumberToken = new Token(t.pos.combine(numberToken.pos),
								t.getValue() + numberToken.getValue(), Token.TokenType.LITERAL_NUMBER);

						parseNumberToken(combinedNumberToken, nodes);

						break;
					}

					nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

					break;

				case LITERAL_NUMBER:
					tokens.remove(0);

					parseNumberToken(t, nodes);

					break;

				case OPENING_BRACKET:
					if(t.getTokenType() == Token.TokenType.OPENING_BRACKET && t.getValue().equals("(")) {
						int endIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
						if(endIndex != -1) {
							Token openingBracketToken = tokens.get(0);
							Token closingBracketToken = tokens.get(endIndex);
							CodePosition pos = openingBracketToken.pos.combine(closingBracketToken.pos);

							List<Token> functionCall = new ArrayList<>(tokens.subList(1, endIndex));
							tokens.subList(0, endIndex + 1).clear();

							nodes.add(new AbstractSyntaxTree.FunctionCallPreviousNodeValueNode(pos, "", "",
									parseFunctionParameterList(functionCall, false).getChildren()
							));

							continue tokenProcessing;
						}
					}

					tokens.remove(0);

					nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

					break;

				case ESCAPE_SEQUENCE:
					tokens.remove(0);

					parseEscapeSequenceToken(t, nodes);

					break;

				case LEXER_ERROR:
					tokens.remove(0);

					parseLexerErrorToken(t, nodes);

					break;

				case START_MULTILINE_TEXT:
					tokens.remove(0);
					do {
						t = tokens.remove(0);

						if(t.getTokenType() == Token.TokenType.LITERAL_TEXT ||
								t.getTokenType() == Token.TokenType.EOL)
							nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						if(t.getTokenType() == Token.TokenType.ESCAPE_SEQUENCE)
							parseEscapeSequenceToken(t, nodes);

						if(t.getTokenType() == Token.TokenType.LEXER_ERROR)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR, t.getValue()));
					}while(t.getTokenType() != Token.TokenType.END_MULTILINE_TEXT);

					break;

				case IDENTIFIER:
				case PARSER_FUNCTION_IDENTIFIER:
					AbstractSyntaxTree.Node ret = t.getTokenType() == Token.TokenType.IDENTIFIER?
							parseVariableNameAndFunctionCall(tokens):parseParserFunctionCall(tokens);
					if(ret != null)
						nodes.add(ret);

					break;

				case LINE_CONTINUATION:
				case END_COMMENT:
				case END_MULTILINE_TEXT:
				case SINGLE_LINE_TEXT_QUOTES:
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
							"Invalid token type for translation key expression: \"" + t.getTokenType().name() + "\""
					));

					break tokenProcessing;
			}
		}

		return ast;
	}

	private AbstractSyntaxTree parseSimpleAssignmentValue(List<Token> tokens) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		int tokenCountFirstLine = getTokenCountFirstLine(tokens);
		if(tokenCountFirstLine == -1)
			tokenCountFirstLine = tokens.size();

		if(tokenCountFirstLine == 0 && tokenCountFirstLine != tokens.size()) {
			nodes.add(new AbstractSyntaxTree.TextValueNode(tokens.get(0).pos, ""));
		}

		tokenProcessing:
		while(!tokens.isEmpty()) {
			Token t = tokens.get(0);

			switch(t.getTokenType()) {
				case EOL:
				case EOF:
					break tokenProcessing;

				case START_COMMENT:
				case START_DOC_COMMENT:
					parseCommentTokens(tokens, nodes);

					break;

				case LITERAL_NULL:
				case LITERAL_TEXT:
				case LITERAL_NUMBER:
				case ARGUMENT_SEPARATOR:
				case IDENTIFIER:
				case PARSER_FUNCTION_IDENTIFIER:
				case ASSIGNMENT:
				case OPERATOR:
				case OPENING_BRACKET:
				case CLOSING_BRACKET:
				case OPENING_BLOCK_BRACKET:
				case CLOSING_BLOCK_BRACKET:
				case WHITESPACE:
				case OTHER:
					tokens.remove(0);

					nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

					break;

				case ESCAPE_SEQUENCE:
					tokens.remove(0);

					parseEscapeSequenceToken(t, nodes);

					break;

				case LEXER_ERROR:
					tokens.remove(0);

					parseLexerErrorToken(t, nodes);

					break;

				case START_MULTILINE_TEXT:
					tokens.remove(0);
					do {
						t = tokens.remove(0);

						if(t.getTokenType() == Token.TokenType.LITERAL_TEXT ||
								t.getTokenType() == Token.TokenType.EOL)
							nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						if(t.getTokenType() == Token.TokenType.ESCAPE_SEQUENCE)
							parseEscapeSequenceToken(t, nodes);

						if(t.getTokenType() == Token.TokenType.LEXER_ERROR)
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR, t.getValue()));
					}while(t.getTokenType() != Token.TokenType.END_MULTILINE_TEXT);

					break;

				case LINE_CONTINUATION:
				case END_COMMENT:
				case END_MULTILINE_TEXT:
				case SINGLE_LINE_TEXT_QUOTES:
					nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
							"Invalid token type for simple assignment value expression: \"" +
									t.getTokenType().name() + "\""));

					break tokenProcessing;
			}
		}

		return ast;
	}

	private void parseTextAndCharValue(List<Token> valueTokens, List<AbstractSyntaxTree.Node> nodes) {
		if(valueTokens.isEmpty())
			return;

		CodePosition pos = valueTokens.get(0).pos.combine(valueTokens.get(valueTokens.size() - 1).pos);

		String value = valueTokens.stream().map(Token::toRawString).collect(Collectors.joining());

		//CHAR
		if(value.length() == 1) {
			nodes.add(new AbstractSyntaxTree.CharValueNode(pos, value.charAt(0)));

			return;
		}

		//TEXT
		nodes.add(new AbstractSyntaxTree.TextValueNode(pos, value));
	}

	private void parseEscapeSequenceToken(Token escapeSequenceToken, List<AbstractSyntaxTree.Node> nodes) {
		if(escapeSequenceToken.getValue().length() != 2 || escapeSequenceToken.getValue().charAt(0) != '\\') {
			nodes.add(new AbstractSyntaxTree.ParsingErrorNode(escapeSequenceToken.pos, ParsingError.LEXER_ERROR,
					"Invalid escape sequence: " + escapeSequenceToken.getValue()));

			return;
		}

		nodes.add(new AbstractSyntaxTree.EscapeSequenceNode(escapeSequenceToken.pos, escapeSequenceToken.getValue().charAt(1)));
	}

	private void parseNumberToken(Token numberToken, List<AbstractSyntaxTree.Node> nodes) {
		String token = numberToken.getValue();

		//INT
		try {
			nodes.add(new AbstractSyntaxTree.IntValueNode(numberToken.pos, Integer.parseInt(token)));

			return;
		}catch(NumberFormatException ignore) {}

		//LONG
		try {
			if(token.endsWith("l") || token.endsWith("L"))
				nodes.add(new AbstractSyntaxTree.LongValueNode(numberToken.pos, Long.parseLong(token.substring(0, token.length() - 1))
				));
			else
				nodes.add(new AbstractSyntaxTree.LongValueNode(numberToken.pos, Long.parseLong(token)));


			return;
		}catch(NumberFormatException ignore) {}

		//FLOAT
		if(token.endsWith("f") || token.endsWith("F")) {
			try {
				nodes.add(new AbstractSyntaxTree.FloatValueNode(numberToken.pos, Float.parseFloat(token.substring(0, token.length() - 1))
				));

				return;
			}catch(NumberFormatException ignore) {}
		}

		//DOUBLE
		try {
			nodes.add(new AbstractSyntaxTree.DoubleValueNode(numberToken.pos, Double.parseDouble(token)));

			return;
		}catch(NumberFormatException ignore) {}
	}

	private void parseLexerErrorToken(Token lexerErrorToken, List<AbstractSyntaxTree.Node> nodes) {
		if(lexerErrorToken.getTokenType() != Token.TokenType.LEXER_ERROR)
			return;

		nodes.add(new AbstractSyntaxTree.ParsingErrorNode(lexerErrorToken.pos, ParsingError.LEXER_ERROR, lexerErrorToken.getValue()
		));
	}

	private AbstractSyntaxTree parseFunctionParameterList(List<Token> tokens, boolean functionDefinition) {
		AbstractSyntaxTree ast = new AbstractSyntaxTree();
		List<AbstractSyntaxTree.Node> nodes = ast.getChildren();

		trimFirstLine(tokens);

		if(functionDefinition) {
			if(!tokens.isEmpty()) {
				tokenProcessing:
				while(!tokens.isEmpty()) {
					Token t = tokens.get(0);

					switch(t.getTokenType()) {
						case EOL:
						case EOF:
							break tokenProcessing;

						case START_COMMENT:
						case START_DOC_COMMENT:
							parseCommentTokens(tokens, nodes);

							break;

						case ARGUMENT_SEPARATOR:
							tokens.remove(0);

							if(nodes.isEmpty()) {
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_PARAMETER,
										"Empty function parameter"));
							}

							if(tokens.isEmpty() || tokens.get(0).getTokenType() == Token.TokenType.EOL ||
									tokens.get(0).getTokenType() == Token.TokenType.EOF) {
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.INVALID_PARAMETER,
										"Empty function parameter"));
							}

							break;

						case IDENTIFIER:
							Token variableNameToken = t;
							String variableName = t.getValue();
							tokens.remove(0);

							String typeConstraint = null;

							if(tokens.size() > 1 && tokens.get(0).getTokenType() == Token.TokenType.OPENING_BRACKET &&
									tokens.get(0).getValue().equals("{")) {
								int bracketEndIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "{", "}", true);
								if(bracketEndIndex == -1) {
									nodes.add(new AbstractSyntaxTree.ParsingErrorNode(variableNameToken.pos, ParsingError.BRACKET_MISMATCH,
											"Bracket is missing in return type constraint in function parameter list definition for parameter \"" +
													variableName + "\""));
									return ast;
								}

								List<Token> typeConstraintTokens = new ArrayList<>(tokens.subList(0, bracketEndIndex + 1));
								tokens.subList(0, bracketEndIndex + 1).clear();

								typeConstraint = parseTypeConstraint(typeConstraintTokens, true, nodes);
							}

							if(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.OPERATOR &&
									tokens.get(0).getValue().equals("...")) {
								//Varargs parameter
								tokens.remove(0);

								variableName += "...";
							}

							nodes.add(new AbstractSyntaxTree.VariableNameNode(t.pos, variableName, typeConstraint));

							break;

						case LEXER_ERROR:
							tokens.remove(0);

							parseLexerErrorToken(t, nodes);

							break;

						case LITERAL_NULL:
						case LITERAL_TEXT:
						case ASSIGNMENT:
						case CLOSING_BRACKET:
						case WHITESPACE:
						case OTHER:
						case OPERATOR:
						case LITERAL_NUMBER:
						case OPENING_BRACKET:
						case OPENING_BLOCK_BRACKET:
						case CLOSING_BLOCK_BRACKET:
						case ESCAPE_SEQUENCE:
						case PARSER_FUNCTION_IDENTIFIER:
						case START_MULTILINE_TEXT:
						case LINE_CONTINUATION:
						case END_COMMENT:
						case END_MULTILINE_TEXT:
						case SINGLE_LINE_TEXT_QUOTES:
							nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
									"Invalid token type for function parameter list expression: \"" +
											t.getTokenType().name() + "\""));

							break tokenProcessing;
					}
				}
			}
		}else {
			tokenProcessing:
			while(!tokens.isEmpty()) {
				Token t = tokens.get(0);

				switch(t.getTokenType()) {
					case EOL:
					case EOF:
						break tokenProcessing;

					case START_COMMENT:
					case START_DOC_COMMENT:
						parseCommentTokens(tokens, nodes);

						break;

					case ARGUMENT_SEPARATOR:
						tokens.remove(0);

						if(nodes.isEmpty() || nodes.get(nodes.size() - 1) instanceof AbstractSyntaxTree.ArgumentSeparatorNode) {
							//Add empty TextObject in between two and before first argument separator
							nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, ""));
						}

						nodes.add(new AbstractSyntaxTree.ArgumentSeparatorNode(t.pos, t.getValue()));

						if(tokens.isEmpty() || tokens.get(0).getTokenType() == Token.TokenType.EOL ||
								tokens.get(0).getTokenType() == Token.TokenType.EOF) {
							//Add empty TextObject after last argument separator
							nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, ""));
						}

						break;

					case LITERAL_NULL:
						tokens.remove(0);

						nodes.add(new AbstractSyntaxTree.NullValueNode(t.pos));
						break;

					case LITERAL_TEXT:
					case ASSIGNMENT:
					case CLOSING_BRACKET:
					case WHITESPACE:
						tokens.remove(0);

						nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						break;

					case OTHER:
						tokens.remove(0);

						parseTextAndCharValue(new ArrayList<>(Arrays.asList(t)), nodes);

						break;

					case OPERATOR:
						tokens.remove(0);

						if((nodes.isEmpty() || nodes.get(nodes.size() - 1) instanceof AbstractSyntaxTree.ArgumentSeparatorNode) &&
								(t.getValue().equals("-") || t.getValue().equals("+")) && !tokens.isEmpty() &&
								tokens.get(0).getTokenType() == Token.TokenType.LITERAL_NUMBER) {
							Token numberToken = tokens.remove(0);

							Token combinedNumberToken = new Token(t.pos.combine(numberToken.pos),
									t.getValue() + numberToken.getValue(), Token.TokenType.LITERAL_NUMBER);

							parseNumberToken(combinedNumberToken, nodes);

							break;
						}

						nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						break;

					case LITERAL_NUMBER:
						tokens.remove(0);

						parseNumberToken(t, nodes);

						break;

					case OPENING_BRACKET:
						if(t.getTokenType() == Token.TokenType.OPENING_BRACKET && t.getValue().equals("(")) {
							int endIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
							if(endIndex != -1) {
								Token openingBracketToken = tokens.get(0);
								Token closingBracketToken = tokens.get(endIndex);
								CodePosition pos = openingBracketToken.pos.combine(closingBracketToken.pos);

								List<Token> functionCall = new ArrayList<>(tokens.subList(1, endIndex));
								tokens.subList(0, endIndex + 1).clear();

								nodes.add(new AbstractSyntaxTree.FunctionCallPreviousNodeValueNode(pos, "", "",
										parseFunctionParameterList(functionCall, false).getChildren()
								));

								continue tokenProcessing;
							}
						}

						tokens.remove(0);

						nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

						break;

					case ESCAPE_SEQUENCE:
						tokens.remove(0);

						parseEscapeSequenceToken(t, nodes);

						break;

					case LEXER_ERROR:
						tokens.remove(0);

						parseLexerErrorToken(t, nodes);

						break;

					case START_MULTILINE_TEXT:
						tokens.remove(0);
						do {
							t = tokens.remove(0);

							if(t.getTokenType() == Token.TokenType.LITERAL_TEXT ||
									t.getTokenType() == Token.TokenType.EOL)
								nodes.add(new AbstractSyntaxTree.TextValueNode(t.pos, t.getValue()));

							if(t.getTokenType() == Token.TokenType.ESCAPE_SEQUENCE)
								parseEscapeSequenceToken(t, nodes);

							if(t.getTokenType() == Token.TokenType.LEXER_ERROR)
								nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR, t.getValue()));
						}while(t.getTokenType() != Token.TokenType.END_MULTILINE_TEXT);

						break;

					case IDENTIFIER:
					case PARSER_FUNCTION_IDENTIFIER:
						boolean isIdentifier = t.getTokenType() == Token.TokenType.IDENTIFIER;

						if(isIdentifier && tokens.size() >= 2 && tokens.get(1).getTokenType() == Token.TokenType.OPERATOR &&
								tokens.get(1).getValue().equals("...")) {
							//Array unpacking

							Token identifierToken = tokens.remove(0);
							Token operatorToken = tokens.remove(0);

							CodePosition pos = identifierToken.pos.combine(operatorToken.pos);

							nodes.add(new AbstractSyntaxTree.UnprocessedVariableNameNode(pos, identifierToken.getValue() + "..."));
						}else {
							AbstractSyntaxTree.Node ret = isIdentifier?parseVariableNameAndFunctionCall(tokens):parseParserFunctionCall(tokens);
							if(ret != null)
								nodes.add(ret);
						}

						break;

					case OPENING_BLOCK_BRACKET:
					case CLOSING_BLOCK_BRACKET:
					case LINE_CONTINUATION:
					case END_COMMENT:
					case END_MULTILINE_TEXT:
					case SINGLE_LINE_TEXT_QUOTES:
						nodes.add(new AbstractSyntaxTree.ParsingErrorNode(t.pos, ParsingError.LEXER_ERROR,
								"Invalid token type for function argument expression: \"" + t.getTokenType().name() + "\""));

						break tokenProcessing;
				}
			}
		}

		return ast;
	}

	private AbstractSyntaxTree.Node parseFunctionCallWithoutPrefix(List<Token> tokens) {
		return parseFunctionCallWithoutPrefix(tokens, null);
	}
	private AbstractSyntaxTree.Node parseFunctionCallWithoutPrefix(List<Token> tokens, AbstractSyntaxTree.OperationNode.OperatorType type) {
		if(tokens.size() < 2)
			return null;

		Token identifierToken = tokens.get(0);
		if(identifierToken.getTokenType() != Token.TokenType.OTHER ||
				!LangPatterns.matches(identifierToken.getValue(), LangPatterns.WORD) ||
				tokens.get(1).getTokenType() != Token.TokenType.OPENING_BRACKET || !tokens.get(1).getValue().equals("("))
			return null;

		tokens.remove(0);

		return parseFunctionCall(identifierToken, tokens, type);
	}

	private AbstractSyntaxTree.Node parseVariableNameAndFunctionCall(List<Token> tokens) {
		return parseVariableNameAndFunctionCall(tokens, null);
	}
	private AbstractSyntaxTree.Node parseVariableNameAndFunctionCall(List<Token> tokens, AbstractSyntaxTree.OperationNode.OperatorType type) {
		if(tokens.isEmpty())
			return null;

		Token identifierToken = tokens.get(0);
		if(identifierToken.getTokenType() != Token.TokenType.IDENTIFIER)
			return null;

		tokens.remove(0);

		if(tokens.isEmpty() || tokens.get(0).getTokenType() != Token.TokenType.OPENING_BRACKET ||
				!tokens.get(0).getValue().equals("(") || !LangPatterns.matches(identifierToken.getValue(),
				LangPatterns.VAR_NAME_FUNCS_WITH_OPERATOR_AND_CONVERSION_METHOD)) {
			return new AbstractSyntaxTree.UnprocessedVariableNameNode(identifierToken.pos, identifierToken.getValue());
		}

		return parseFunctionCall(identifierToken, tokens, type);
	}

	private AbstractSyntaxTree.Node parseFunctionCall(Token identifierToken, List<Token> tokens) {
		return parseFunctionCall(identifierToken, tokens, null);
	}
	private AbstractSyntaxTree.Node parseFunctionCall(Token identifierToken, List<Token> tokens, AbstractSyntaxTree.OperationNode.OperatorType type) {
		int endIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
		if(endIndex == -1) {
			return new AbstractSyntaxTree.ParsingErrorNode(identifierToken.pos, ParsingError.BRACKET_MISMATCH,
					"Bracket is missing in function call");
		}

		CodePosition pos = identifierToken.getPos().combine(tokens.get(endIndex).pos);

		List<Token> functionParameterTokens = new ArrayList<>(tokens.subList(1, endIndex));
		tokens.subList(0, endIndex + 1).clear();

		if(type == null)
			return new AbstractSyntaxTree.FunctionCallNode(pos, parseFunctionParameterList(functionParameterTokens, false).
					getChildren(), identifierToken.getValue());

		return new AbstractSyntaxTree.FunctionCallNode(pos, convertCommaOperatorsToArgumentSeparators(
				parseOperationExpr(functionParameterTokens, type)), identifierToken.getValue());
	}

	private AbstractSyntaxTree.Node parseParserFunctionCall(List<Token> tokens) {
		if(tokens.isEmpty())
			return null;

		Token parserFunctionIdentifierToken = tokens.get(0);
		if(parserFunctionIdentifierToken.getTokenType() != Token.TokenType.PARSER_FUNCTION_IDENTIFIER)
			return null;

		tokens.remove(0);

		int endIndex = LangUtils.getIndexOfMatchingBracket(tokens, 0, Integer.MAX_VALUE, "(", ")", true);
		if(endIndex == -1) {
			return new AbstractSyntaxTree.ParsingErrorNode(parserFunctionIdentifierToken.pos, ParsingError.BRACKET_MISMATCH,
					"Bracket is missing in parser function call");
		}

		List<Token> parameterTokens = new ArrayList<>(tokens.subList(1, endIndex));
		tokens.subList(0, endIndex + 1).clear();

		switch(parserFunctionIdentifierToken.getValue()) {
			case "parser.con":
				return parseCondition(parameterTokens);
			case "parser.math":
				return parseMathExpr(parameterTokens);
			case "parser.norm":
				return parseToken(parameterTokens).convertToNode();
			case "parser.op":
				return parseOperationExpr(parameterTokens);
		}

		return new AbstractSyntaxTree.ParsingErrorNode(parserFunctionIdentifierToken.pos, ParsingError.INVALID_PARAMETER,
				"Invalid parser function: \"" + parserFunctionIdentifierToken.getValue() + "\"");
	}

	private String parseTypeConstraint(List<Token> tokens, boolean allowSpecialTypeConstraints, List<AbstractSyntaxTree.Node> errorNodes) {
		if(tokens.isEmpty())
			return null;

		String typeConstraint = tokens.stream().map(Token::toRawString).collect(Collectors.joining());
		if(!LangPatterns.matches(typeConstraint, allowSpecialTypeConstraints?LangPatterns.TYPE_CONSTRAINT_WITH_SPECIAL_TYPES:
				LangPatterns.PARSING_TYPE_CONSTRAINT)) {
			CodePosition pos = tokens.get(0).pos.combine(tokens.get(tokens.size() - 1).pos);

			errorNodes.add(new AbstractSyntaxTree.ParsingErrorNode(pos, ParsingError.BRACKET_MISMATCH,
					"Invalid type constraint syntax"));

			return null;
		}

		//Remove "{" and "}"
		return typeConstraint.substring(1, typeConstraint.length() - 1);
	}

	private void parseCommentTokens(List<Token> tokens, List<AbstractSyntaxTree.Node> errorNodes) {
		if(tokens.isEmpty())
			return;

		Token currentToken = tokens.get(0);
		while(currentToken.getTokenType() == Token.TokenType.START_COMMENT ||
				currentToken.getTokenType() == Token.TokenType.START_DOC_COMMENT) {
			tokens.remove(0);

			boolean isDocComment = currentToken.getTokenType() == Token.TokenType.START_DOC_COMMENT;
			if(currentToken.getTokenType() == Token.TokenType.START_COMMENT || isDocComment) {
				StringBuilder stringBuilder = new StringBuilder();

				while(currentToken.getTokenType() != Token.TokenType.END_COMMENT) {
					if(tokens.isEmpty())
						break;

					currentToken = tokens.remove(0);
					if(currentToken.getTokenType() == Token.TokenType.LEXER_ERROR)
						errorNodes.add(new AbstractSyntaxTree.ParsingErrorNode(currentToken.pos, ParsingError.LEXER_ERROR,
								currentToken.getValue()));

					if(isDocComment) {
						if(currentToken.getTokenType() == Token.TokenType.LITERAL_TEXT ||
								currentToken.getTokenType() == Token.TokenType.ESCAPE_SEQUENCE)
							stringBuilder.append(currentToken.getValue());
						else if(currentToken.getTokenType() == Token.TokenType.EOL)
							stringBuilder.append("\n");
					}
				}

				if(isDocComment) {
					String docComment = stringBuilder.toString();
					if(langDocComment == null)
						langDocComment = docComment;
					else
						langDocComment += "\n" + docComment;
				}
			}

			if(tokens.isEmpty())
				break;

			currentToken = tokens.get(0);
		}
	}

	private void trimFirstLine(List<Token> tokens) {
		while(!tokens.isEmpty() && tokens.get(0).getTokenType() == Token.TokenType.WHITESPACE)
			tokens.remove(0);

		int tokenCountFirstLine = getTokenCountFirstLine(tokens);

		int i = (tokenCountFirstLine == -1?tokens.size():tokenCountFirstLine) - 1;
		while(i >= 0 && (tokens.get(i).getTokenType() == Token.TokenType.WHITESPACE ||
				tokens.get(i).getTokenType() == Token.TokenType.END_COMMENT)) {
			//Trim before comment
			if(tokens.get(i).getTokenType() == Token.TokenType.END_COMMENT) {
				while(i >= 0 && tokens.get(i).getTokenType() != Token.TokenType.START_COMMENT &&
						tokens.get(i).getTokenType() != Token.TokenType.START_DOC_COMMENT) {

					i--;
				}

				i--;

				continue;
			}

			tokens.remove(i);

			i--;
		}
	}

	private void removeLineContinuationAndSingleLineTextQuotesTokens(List<Token> tokens) {
		ListIterator<Token> iter = tokens.listIterator();
		while(iter.hasNext()) {
			Token token = iter.next();
			if(token.getTokenType() == Token.TokenType.LINE_CONTINUATION) {
				iter.remove();

				if(iter.hasNext()) {
					if(iter.next().getTokenType() == Token.TokenType.EOL)
						iter.remove();
					else
						iter.previous();
				}
			}else if(token.getTokenType() == Token.TokenType.SINGLE_LINE_TEXT_QUOTES) {
				iter.remove();
			}
		}
	}

	private int getTokenCountFirstLine(List<Token> tokens) {
		ListIterator<Token> iter = tokens.listIterator();
		while(iter.hasNext()) {
			Token token = iter.next();
			if(token.getTokenType() == Token.TokenType.EOL ||
					token.getTokenType() == Token.TokenType.EOF)
				return iter.previousIndex();
		}

		return -1;
	}

	public static enum ParsingError {
		BRACKET_MISMATCH     (-1, "Bracket mismatch"),
		CONT_FLOW_ARG_MISSING(-2, "Control flow statement condition(s) or argument(s) is/are missing"),
		EOF                  (-3, "End of file was reached early"),
		INVALID_CON_PART     (-4, "Invalid statement part in control flow statement"),
		INVALID_ASSIGNMENT   (-5, "Invalid assignment operation"),
		INVALID_PARAMETER    (-6, "Invalid function parameter"),
		LEXER_ERROR          (-7, "Error during lexical parsing");

		private final int errorCode;
		private final String errorText;

		private ParsingError(int errorCode, String errorText) {
			this.errorCode = errorCode;
			this.errorText = errorText;
		}

		public int getErrorCode() {
			return errorCode;
		}

		public String getErrorText() {
			return errorText;
		}
	}
}
