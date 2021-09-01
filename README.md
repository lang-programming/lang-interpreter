# The Lang Programming Language
This project makes simple translation files Turing complete

### "TermIO-Control" window commands
**Execution of lang file**: Type "executeLang -*Path to .lang file*" in the white text input in the "TermIO-Control" window<br>
You can also run "java -jar LangCompiler.jar path/to/file.lang" in a terminal or console.<br>
**LangShell**: Type "startShell" in the white text input in the "TermIO-Control" window<br>
You can also run "java -jar LangCompiler.jar -startShell" in a terminal or console.<br>
**Language definitions**: See comment in src/me/jddev0/module/io/Lang.java and checkout assets/test.lang (It contains a basic overview of the programming language with explanations)<br>
**Language Tutorial**: You can find many tutorial lang files in /assets/tuts/.<br>
**Print AST tree**: Type "printAST -*Path to .lang file*" for parsing a Lang file and printing the parsed AST tree<br>
**4K-Support**: Type "toggle4k" for a larger font in the "TermIO-Control" window and the "LangShell" window<br>

### Breaking changes
- **v1.0.0**:
 - Arrays variable names starting with "&LANG_" are no longer allowed
 - Var/Array pointer names
 - Many deprecated methods and classes in the Lang class won't work as expected
