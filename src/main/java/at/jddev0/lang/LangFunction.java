package at.jddev0.lang;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LangFunction {
    String value();

    /**
     * @return true for the lang function which contains the function info if the function is overloaded
     */
    boolean hasInfo() default false;

    boolean isMethod() default false;

    boolean isLinkerFunction() default false;

    boolean isDeprecated() default false;
    String getDeprecatedRemoveVersion() default "";
    String getDeprecatedReplacementFunction() default "";

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface CombinatorFunction {}

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface LangParameter {
        String value();

        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface NumberValue {}

        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface CallableValue {}

        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface BooleanValue {}

        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface CallByPointer {}

        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface VarArgs {}

        @Documented
        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface RawVarArgs {}
    }

    /**
     * If used in method -> return value type constraint
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface AllowedTypes {
        DataObject.DataType[] value();
    }

    /**
     * If used in method -> return value type constraint
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface NotAllowedTypes {
        DataObject.DataType[] value();
    }

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.METHOD, ElementType.PARAMETER})
    @interface LangInfo {
        String value();
    }
}