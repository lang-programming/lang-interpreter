package at.jddev0.lang;

import java.util.Objects;

/**
 * Lang-Module<br>
 * Code position information for Tokens, AST nodes, and error handling
 *
 * @author JDDev0
 * @version v1.0.0
 */
public class CodePosition {
    public static final CodePosition EMPTY = new CodePosition(-1, -1, -1, -1);

    public final int lineNumberFrom;
    public final int lineNumberTo;
    public final int columnFrom;
    public final int columnTo;

    public CodePosition(int lineNumberFrom, int lineNumberTo, int columnFrom, int columnTo) {
        this.lineNumberFrom = lineNumberFrom;
        this.lineNumberTo = lineNumberTo;
        this.columnFrom = columnFrom;
        this.columnTo = columnTo;
    }

    public CodePosition combine(CodePosition codePosition) {
        if(this.equals(EMPTY))
            return codePosition;

        if(codePosition.equals(EMPTY))
            return this;

        int columnFrom;
        if(this.lineNumberFrom == codePosition.lineNumberFrom)
            columnFrom = Math.min(this.columnFrom, codePosition.columnFrom);
        else if(this.lineNumberFrom < codePosition.lineNumberFrom)
            columnFrom = this.columnFrom;
        else
            columnFrom = codePosition.columnFrom;

        int columnTo;
        if(this.lineNumberTo == codePosition.lineNumberTo)
            columnTo = Math.max(this.columnTo, codePosition.columnTo);
        else if(this.lineNumberTo > codePosition.lineNumberTo)
            columnTo = this.columnTo;
        else
            columnTo = codePosition.columnTo;

        return new CodePosition(
                Math.min(this.lineNumberFrom, codePosition.lineNumberFrom),
                Math.max(this.lineNumberTo, codePosition.lineNumberTo),

                columnFrom, columnTo
        );
    }

    public int getLineNumberFrom() {
        return lineNumberFrom;
    }

    public int getLineNumberTo() {
        return lineNumberTo;
    }

    public int getColumnFrom() {
        return columnFrom;
    }

    public int getColumnTo() {
        return columnTo;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if(!(obj instanceof CodePosition))
            return false;

        CodePosition that = (CodePosition)obj;
        return lineNumberFrom == that.lineNumberFrom && lineNumberTo == that.lineNumberTo &&
                columnFrom == that.columnFrom && columnTo == that.columnTo;
    }

    @Override
    public int hashCode() {
        return Objects.hash(lineNumberFrom, lineNumberTo, columnFrom, columnTo);
    }

    @Override
    public String toString() {
        return String.format("%5d:%3d - %5d:%3d", lineNumberFrom, columnFrom, lineNumberTo, columnTo);
    }

    public String toCompactString() {
        return String.format("%d:%d-%d:%d", lineNumberFrom, columnFrom, lineNumberTo, columnTo);
    }
}
