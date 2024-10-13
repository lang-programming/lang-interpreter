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
     * RegEx: "<code>\w+</code>"
     */
    public static final Pattern WORD = Pattern.compile("\\w+");
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
     * RegEx: "<code>(\[\[\w+\]\]::)?(\$\**|&|fp\.)\w+</code>"
     */
    public static final Pattern VAR_NAME_FULL = Pattern.compile("(\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.)\\w+");
    /**
     * RegEx: "<code>((\[\[\w+\]\]::)?fp|mp|func|fn|linker|ln)\.\w+</code>"
     */
    public static final Pattern VAR_NAME_FUNCS = Pattern.compile("((\\[\\[\\w+\\]\\]::)?fp|mp|func|fn|linker|ln)\\.\\w+");
    /**
     * RegEx: "<code>(op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|((r-)?(concat|add|sub|mul|pow|div|truncDiv|
     * floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call)))|
     * (to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number))|(((\[\[\w+\]\]::)?fp|mp|func|fn|linker|ln)\.\w+)</code>"
     */
    public static final Pattern VAR_NAME_FUNCS_WITH_OPERATOR_AND_CONVERSION_METHOD = Pattern.compile(
            "(op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|((r-)?(concat|add|sub|mul|pow|div|truncDiv|" +
                    "floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call)))|" +
                    "(to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number))|(((\\[\\[\\w+\\]\\]::)?fp|mp|func|fn|linker|ln)\\.\\w+)");
    /**
     * RegEx: "<code>(((\[\[\w+\]\]::)?(\$\**|&|fp\.|mp\.)|func\.|fn\.|linker\.|ln\.)\w+|(\[\[\w+\]\]::)?\$\**\[+\w+\]+)</code>"
     */
    public static final Pattern VAR_NAME_FULL_WITH_FUNCS_AND_PTR_AND_DEREFERENCE = Pattern.compile(
            "(((\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.|mp\\.)|func\\.|fn\\.|linker\\.|ln\\.)\\w+|(\\[\\[\\w+\\]\\]::)?\\$\\**\\[+\\w+\\]+)");
    /**
     * RegEx: "<code>(op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|((r-)?(concat|add|sub|mul|pow|div|truncDiv|
     * floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call)))|
     * (to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number))|
     * (((\[\[\w+\]\]::)?(\$\**|&|fp\.|mp\.)|func\.|fn\.|linker\.|ln\.)\w+|(\[\[\w+\]\]::)?\$\**\[+\w+\]+)</code>"
     */
    public static final Pattern VAR_NAME_FULL_WITH_FUNCS_AND_PTR_AND_DEREFERENCE_WITH_OPERATOR_AND_CONVERSION_METHODS = Pattern.compile(
            "(op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|((r-)?(concat|add|sub|mul|pow|div|truncDiv|" +
                    "floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call)))|" +
                    "(to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number))|" +
                    "(((\\[\\[\\w+\\]\\]::)?(\\$\\**|&|fp\\.|mp\\.)|func\\.|fn\\.|linker\\.|ln\\.)\\w+|(\\[\\[\\w+\\]\\]::)?\\$\\**\\[+\\w+\\]+)");
    /**
     * RegEx: "<code>((\[\[\w+\]\]::)?fp|func|fn|linker|ln)\.\w+</code>"
     */
    public static final Pattern VAR_NAME_FUNC_PTR_WITH_FUNCS = Pattern.compile("((\\[\\[\\w+\\]\\]::)?fp|func|fn|linker|ln)\\.\\w+");
    /**
     * RegEx: "<code>mp\.\w+</code>"
     */
    public static final Pattern METHOD_NAME = Pattern.compile("mp\\.\\w+");
    /**
     * RegEx: "<code>op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|((r-)?(concat|add|sub|mul|pow|div|truncDiv|
     * floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call))</code>"
     */
    public static final Pattern OPERATOR_METHOD_NAME = Pattern.compile("op:((len|deepCopy|inc|dec|pos|inv|not|abs|iter|hasNext|next)|" +
            "((r-)?(concat|add|sub|mul|pow|div|truncDiv|floorDiv|ceilDiv|mod|and|or|xor|lshift|rshift|rzshift|" +
            "isEquals|isStrictEquals|isLessThan|isGreaterThan))|(getItem|setItem|slice)|(call))");
    /**
     * RegEx: "<code>to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number)</code>"
     */
    public static final Pattern CONVERSION_METHOD_NAME = Pattern.compile("to:(text|char|int|long|float|double|byteBuffer|array|list|bool|number)");
    /**
     * RegEx: "<code>\{(([?!]?([A-Z_]+\|)*[A-Z_]+)|(bool|number|callable))\}</code>"
     */
    public static final Pattern TYPE_CONSTRAINT_WITH_SPECIAL_TYPES = Pattern.compile("\\{(([?!]?([A-Z_]+\\|)*[A-Z_]+)|(bool|number|callable))\\}");

    //Function call specific
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
     * RegEx: "<code>(\[\[\w+\]\]::)?\$\**\[+\w+\]+</code>"
     */
    public static final Pattern PARSING_VAR_NAME_PTR_AND_DEREFERENCE = Pattern.compile("(\\[\\[\\w+\\]\\]::)?\\$\\**\\[+\\w+\\]+");
    /**
     * RegEx: "<code>((([1-9]\d*|0)(\.\d*)?)|(\.\d+))[eE]</code>"
     */
    public static final Pattern PARSING_FLOATING_POINT_E_SYNTAX_START = Pattern.compile("((([1-9]\\d*|0)(\\.\\d*)?)|(\\.\\d+))[eE]");
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