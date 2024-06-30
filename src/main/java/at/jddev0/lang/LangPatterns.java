package at.jddev0.lang;

import java.util.regex.Pattern;

/**
 * Lang-Module<br>
 * RegEx patterns for other Lang classes
 * 
 * @author JDDev0
 * @version v1.0.0
 */
final class LangPatterns {
	//General patterns
	/**
	 * RegEx: "<code>\.</code>"
	 */
	public static final Pattern GENERAL_DOT = Pattern.compile("\\.");
	/**
	 * RegEx: "<code>\w+</code>"
	 */
	public static final Pattern WORD = Pattern.compile("\\w+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?(\$|&)LANG_.*</code>"
	 */
	public static final Pattern LANG_VAR = Pattern.compile("(\\[\\[\\w+\\]\\]::)?(\\$|&)LANG_.*");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?&LANG_.*</code>"
	 */
	public static final Pattern LANG_VAR_ARRAY = Pattern.compile("(\\[\\[\\w+\\]\\]::)?&LANG_.*");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?\$\w+</code>"
	 */
	public static final Pattern VAR_NAME_NORMAL = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$\\w+");
	/**
	 * RegEx: "<code>(\$|&|fp\.)\w+</code>"
	 */
	public static final Pattern VAR_NAME_WITHOUT_PREFIX = Pattern.compile("(\\$|&|fp\\.)\\w+");
	/**
	 * RegEx: "<code>(\$|fp\.)\w+</code>"
	 */
	public static final Pattern VAR_NAME_NORMAL_FUNCTION_WITHOUT_PREFIX = Pattern.compile("(\\$|fp\\.)\\w+");
	/**
	 * RegEx: "<code>(\$|&)\w+</code>"
	 */
	public static final Pattern VAR_NAME_NORMAL_ARRAY_WITHOUT_PREFIX = Pattern.compile("(\\$|&)\\w+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?(\$|&|fp\.)\w+</code>"
	 */
	public static final Pattern VAR_NAME = Pattern.compile("(\\[\\[\\w+\\]\\]::)?(\\$|&|fp\\.)\\w+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?(\$\**|&|fp\.)\w+</code>"
	 */
	public static final Pattern VAR_NAME_FULL = Pattern.compile("(\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.)\\w+");
	/**
	 * RegEx: "<code>((\[\[\w+\]\]::)?fp|mp|func|fn|linker|ln)\.\w+</code>"
	 */
	public static final Pattern VAR_NAME_FUNCS = Pattern.compile("((\\[\\[\\w+\\]\\]::)?fp|mp|func|fn|linker|ln)\\.\\w+");
	/**
	 * RegEx: "<code>((\[\[\w+\]\]::)?(\$\**|&|fp\.)|mp\.|func\.|fn\.|linker\.|ln\.)\w+</code>"
	 */
	public static final Pattern VAR_NAME_FULL_WITH_FUNCS = Pattern.compile("((\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.)|mp\\.|func\\.|fn\\.|linker\\.|ln\\.)\\w+");
	/**
	 * RegEx: "<code>((\[\[\w+\]\]::)?(\$\**|&|fp\.)\w+|\$\**\[+\w+\]+)</code>"
	 */
	public static final Pattern VAR_NAME_FULL_WITH_PTR_AND_DEREFERENCE = Pattern.compile("((\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.)\\w+|\\$\\**\\[+\\w+\\]+)");
	/**
	 * RegEx: "<code>(((\[\[\w+\]\]::)?(\$\**|&|fp\.|mp\.)|func\.|fn\.|linker\.|ln\.)\w+|(\[\[\w+\]\]::)?\$\**\[+\w+\]+)</code>"
	 */
	public static final Pattern VAR_NAME_FULL_WITH_FUNCS_AND_PTR_AND_DEREFERENCE = Pattern.compile(
			"(((\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.|mp\\.)|func\\.|fn\\.|linker\\.|ln\\.)\\w+|(\\[\\[\\w+\\]\\]::)?\\$\\**\\[+\\w+\\]+)");
	/**
	 * RegEx: "<code>((\[\[\w+\]\]::)?(\$\**|&)\w*|\$\**\[+\w+\]+)</code>"
	 */
	public static final Pattern VAR_NAME_PREFIX_ARRAY_AND_NORMAL_WITH_PTR_AND_DEREFERENCE_WITH_OPTIONAL_NAME = Pattern.compile("((\\[\\[\\w+\\]\\]::)?(\\$\\**|&)\\w*|\\$\\**\\[+\\w+\\]+)");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?fp\.\w+</code>"
	 */
	public static final Pattern VAR_NAME_FUNC_PTR = Pattern.compile("(\\[\\[\\w+\\]\\]::)?fp\\.\\w+");
	/**
	 * RegEx: "<code>((\[\[\w+\]\]::)?fp|func|fn|linker|ln)\.\w+</code>"
	 */
	public static final Pattern VAR_NAME_FUNC_PTR_WITH_FUNCS = Pattern.compile("((\\[\\[\\w+\\]\\]::)?fp|func|fn|linker|ln)\\.\\w+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?&\w+</code>"
	 */
	public static final Pattern VAR_NAME_ARRAY = Pattern.compile("(\\[\\[\\w+\\]\\]::)?&\\w+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?\$\[+\w+\]+</code>"
	 */
	public static final Pattern VAR_NAME_PTR = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$\\[+\\w+\\]+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?(\$\**|&)\w+</code>"
	 */
	public static final Pattern VAR_NAME_DEREFERENCE_AND_ARRAY = Pattern.compile("(\\[\\[\\w+\\]\\]::)?(\\$\\**|&)\\w+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?\$\**\[+\w+\]+</code>"
	 */
	public static final Pattern VAR_NAME_PTR_AND_DEREFERENCE = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$\\**\\[+\\w+\\]+");
	/**
	 * RegEx: "<code>(func\.|fn\.|linker\.|ln\.)\w+</code>"
	 */
	public static final Pattern FUNC_NAME = Pattern.compile("(func\\.|fn\\.|linker\\.|ln\\.)\\w+");
	/**
	 * RegEx: "<code>mp\.\w+</code>"
	 */
	public static final Pattern METHOD_NAME = Pattern.compile("mp\\.\\w+");
	/**
	 * RegEx: "<code>op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|((r-)?(concat|add|sub|mul|pow|div|truncDiv|
	 * floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem)|(call))</code>"
	 */
	public static final Pattern OPERATOR_METHOD_NAME = Pattern.compile("op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|" +
			"((r-)?(concat|add|sub|mul|pow|div|truncDiv|floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|" +
			"isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem)|(call))");
	/**
	 * RegEx: "<code>to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number)</code>"
	 */
	public static final Pattern CONVERSION_METHOD_NAME = Pattern.compile("to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number)");
	/**
	 * RegEx: "<code>\{[?!]?([A-Z_]+\|)*[A-Z_]+\}</code>"
	 */
	public static final Pattern TYPE_CONSTRAINT = Pattern.compile("\\{[?!]?([A-Z_]+\\|)*[A-Z_]+\\}");
	/**
	 * RegEx: "<code>\{(([?!]?([A-Z_]+\|)*[A-Z_]+)|(bool|number|callable))\}</code>"
	 */
	public static final Pattern TYPE_CONSTRAINT_WITH_SPECIAL_TYPES = Pattern.compile("\\{(([?!]?([A-Z_]+\\|)*[A-Z_]+)|(bool|number|callable))\\}");
	
	//Function call specific
	/**
	 * RegEx: "<code>(\$|&)\w+\.\.\.</code>"
	 */
	public static final Pattern FUNC_CALL_VAR_ARGS = Pattern.compile("(\\$|&)\\w+\\.\\.\\.");
	/**
	 * RegEx: "<code>\$\[\w+\]</code>"
	 */
	public static final Pattern FUNC_CALL_CALL_BY_PTR = Pattern.compile("\\$\\[\\w+\\]");
	/**
	 * RegEx: "<code>\$\[LANG_.*\]</code>"
	 */
	public static final Pattern FUNC_CALL_CALL_BY_PTR_LANG_VAR = Pattern.compile("\\$\\[LANG_.*\\]");
	/**
	 * RegEx: "<code>parser\.\w+\</code>"
	 */
	public static final Pattern PARSER_FUNCTION_IDENTIFIER = Pattern.compile("parser\\.\\w+");
	/**
	 * RegEx: "<code>\s*,\s*</code>"
	 */
	public static final Pattern ARGUMENT_SEPARATOR = Pattern.compile("\\s*,\\s*");
	
	//LangParser specific
	/**
	 * RegEx: "<code>((\[\[\w+\]\]::)?fp|mp|func|fn|linker|ln)\.\w+\(.*\).*</code>"
	 */
	public static final Pattern PARSING_STARTS_WITH_FUNCTION_CALL = Pattern.compile("((\\[\\[\\w+\\]\\]::)?fp|mp|func|fn|linker|ln)\\.\\w+\\(.*\\).*");
	/**
	 * RegEx: "<code>parser\.\w+\(.*\).*</code>"
	 */
	public static final Pattern PARSING_STARTS_WITH_PARSER_FUNCTION_CALL = Pattern.compile("parser\\.\\w+\\(.*\\).*");
	/**
	 * RegEx: "<code>\(.*\).*</code>"
	 */
	public static final Pattern PARSING_STARTS_WITH_FUNCTION_CALL_PREVIOUS_VALUE = Pattern.compile("\\(.*\\).*");
	/**
	 * RegEx: "<code>\s*,.*</code>"
	 */
	public static final Pattern PARSING_ARGUMENT_SEPARATOR_LEADING_WHITESPACE = Pattern.compile("\\s*,.*");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?\$\**\[+\w+\]+</code>"
	 */
	public static final Pattern PARSING_VAR_NAME_PTR_AND_DEREFERENCE = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$\\**\\[+\\w+\\]+");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?\$(\*+\w+|\[+\w+\]+|\*+\[+\w+\]+).*</code>"
	 */
	public static final Pattern PARSING_STARTS_WITH_VAR_NAME_PTR_OR_DEREFERENCE = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$(\\*+\\w+|\\[+\\w+\\]+|\\*+\\[+\\w+\\]+).*");
	/**
	 * RegEx: "<code>(-?(NaN|Infinity))|(.*[fFdD])|(0[xX].*)</code>"
	 */
	public static final Pattern PARSING_INVALID_FLOATING_POINT_NUMBER = Pattern.compile("(-?(NaN|Infinity))|(.*[dD])|(0[xX].*)");
	/**
	 * RegEx: "<code>((([1-9]\d*|0)(\.\d*)?)|(\.\d+))[eE]</code>"
	 */
	public static final Pattern PARSING_FLOATING_POINT_E_SYNTAX_START = Pattern.compile("((([1-9]\\d*|0)(\\.\\d*)?)|(\\.\\d+))[eE]");
	/**
	 * RegEx: "<code>(.*[fFdD])|(0[xX].*)</code>"
	 */
	public static final Pattern PARSING_INVALID_FLOATING_POINT_NUMBER_ALLOW_NaN_INFINITY = Pattern.compile("(.*[fFdD])|(0[xX].*)");
	/**
	 * RegEx: "<code>(.*[fFdD])|(0[xX].*)|(\s.*|.*\s)</code>"
	 */
	public static final Pattern PARSING_INVALID_FLOATING_POINT_NUMBER_ALLOW_NaN_INFINITY_OR_LEADING_OR_TRAILING_WHITESPACES = Pattern.compile("(.*[dD])|(0[xX].*)|(\\s.*|.*\\s)");
	/**
	 * RegEx: "<code>\W.*</code>"
	 */
	public static final Pattern PARSING_STARTS_WITH_NON_WORD_CHAR = Pattern.compile("\\W.*");
	/**
	 * RegEx: "<code>\{[?!]?([A-Z_]+\|)*[A-Z_]+\}</code>"
	 */
	public static final Pattern PARSING_TYPE_CONSTRAINT = Pattern.compile("\\{[?!]?([A-Z_]+\\|)*[A-Z_]+\\}");
	
	//LangParser assignment specific
	/**
	 * RegEx: "<code>parser(\.\w+)+</code>"
	 */
	public static final Pattern PARSING_PARSER_FLAG = Pattern.compile("parser(\\.\\w+)+");
	/**
	 * RegEx: "<code>  [^\\= ]{0,3}= </code>"
	 */
	public static final Pattern PARSING_ASSIGNMENT_OPERATOR = Pattern.compile(" [^\\\\= ]{1,3}= ");
	/**
	 * RegEx: "<code>(\[\[\w+\]\]::)?\$\**\w+</code>"
	 */
	public static final Pattern PARSING_SIMPLE_ASSIGNMENT_VARIABLE_NAME_LVALUE = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$\\**\\w+");
	/**
	 * RegEx: "<code>[\w\-\.\:]+</code>"
	 */
	public static final Pattern PARSING_SIMPLE_TRANSLATION_KEY = Pattern.compile("[\\w\\-\\.\\:]+");
	
	public static boolean matches(String str, Pattern pattern) {
		return pattern.matcher(str).matches();
	}
	
	public static String replaceAll(String str, String replacement, Pattern pattern) {
		return pattern.matcher(str).replaceAll(replacement);
	}
	
	private LangPatterns() {}
}