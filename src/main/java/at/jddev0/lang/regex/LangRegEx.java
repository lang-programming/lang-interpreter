package at.jddev0.lang.regex;

import at.jddev0.lang.DataObject;

import java.util.Arrays;
import java.util.regex.PatternSyntaxException;

/**
 * Lang-Module<br>
 * Lang RegEx implementation
 *
 * @author JDDev0
 * @version v1.0.0
 */
public final class LangRegEx {
    private LangRegEx() {}

    public static boolean matches(DataObject.Text text, DataObject.Text regex) throws InvalidPaternSyntaxException {
        try {
            return text.toString().matches(regex.toString());
        }catch(PatternSyntaxException e) {
            throw new InvalidPaternSyntaxException(e.getMessage());
        }
    }

    public static DataObject.Text[] split(DataObject.Text text, DataObject.Text regex) throws InvalidPaternSyntaxException {
        try {
            return Arrays.stream(text.toString().split(regex.toString())).map(DataObject.Text::fromString).toArray(DataObject.Text[]::new);
        }catch(PatternSyntaxException e) {
            throw new InvalidPaternSyntaxException(e.getMessage());
        }
    }

    public static DataObject.Text[] split(DataObject.Text text, DataObject.Text regex, int limit) throws InvalidPaternSyntaxException {
        try {
            return Arrays.stream(text.toString().split(regex.toString(), limit)).map(DataObject.Text::fromString).toArray(DataObject.Text[]::new);
        }catch(PatternSyntaxException e) {
            throw new InvalidPaternSyntaxException(e.getMessage());
        }
    }

    public static DataObject.Text replace(DataObject.Text text, DataObject.Text regex, DataObject.Text replacement) throws InvalidPaternSyntaxException {
        try {
            return DataObject.Text.fromString(text.toString().replaceAll(regex.toString(), replacement.toString()));
        }catch(PatternSyntaxException e) {
            throw new InvalidPaternSyntaxException(e.getMessage());
        }
    }
}