package me.jddev0.module.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import me.jddev0.module.io.TerminalIO;
import me.jddev0.module.io.TerminalIO.Level;
import me.jddev0.module.lang.DataObject;
import me.jddev0.module.lang.Lang;
import me.jddev0.module.lang.LangInterpreter;
import me.jddev0.module.lang.LangInterpreter.InterpretingError;
import me.jddev0.module.lang.LangPlatformAPI;
import me.jddev0.module.lang.LangPredefinedFunctionObject;
import me.jddev0.module.lang.LangUtils;

/**
 * Uses the io module<br>
 * <br>
 * Graphics-Module<br>
 * Lang Shell
 * 
 * @author JDDev0
 * @version v1.0.0
 */
public class LangShellWindow extends JDialog {
	private static final long serialVersionUID = 3517996790399999763L;
	
	private final JTextPane shell;
	private final KeyListener shellKeyListener;
	private final TerminalIO term;
	
	private SpecialCharInputWindow specialCharInputWindow = null;
	
	private List<String> history = new LinkedList<String>();
	private int historyPos = 0;
	private String currentCommand = "";
	
	private String autoCompleteText = "";
	private int autoCompletePos = 0;
	private Color lastColor = Color.BLACK;
	
	private Queue<String> executionQueue = new LinkedList<String>();
	private StringBuilder multiLineTmp = new StringBuilder();
	private int indent = 0;
	private boolean flagMultilineText = false;
	private boolean flagLineContinuation = false;
	private boolean flagEnd = false;
	private boolean flagRunning = false;
	private boolean flagExecutingQueue = false;
	
	private LangPlatformAPI langPlatformAPI = new LangPlatformAPI();
	private LangInterpreter.LangInterpreterInterface lii;
	private PrintStream oldOut;
	
	//Lists for auto complete
	private final List<String> langDataAndExecutionFlags = Arrays.asList("allowTermRedirect = ", "errorOutput = ", "name = ", "test = ", "version = ");
	private final List<String> controlFlowStatements = Arrays.asList("break", "catch", "continue", "elif(", "else", "endif", "endloop", "endtry", "finally", "foreach(", "if(", "loop", "nontry",
			"repeat(", "softtry", "try", "until(", "while(");
	private final List<String> parserFunctions = Arrays.asList("con(", "math(", "op(");
	
	public LangShellWindow(Frame owner, TerminalIO term) {
		this(owner, term, 12);
	}
	public LangShellWindow(Frame owner, TerminalIO term, int fontSize) {
		this(owner, term, fontSize, null);
	}
	public LangShellWindow(Frame owner, TerminalIO term, int fontSize, String[] langArgs) {
		super(owner, true); //Make this window to an modal window (Focus won't be given back to owner window)
		
		this.term = term;
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setTitle("LangShell");
		setSize((int)(750*fontSize / 12.), (int)(500*fontSize / 12.));
		setLocationRelativeTo(owner);
		addWindowListener(new WindowAdapter() {
			@Override
			 public void windowClosing(WindowEvent e) {
				if(specialCharInputWindow != null)
					specialCharInputWindow.dispatchEvent(new WindowEvent(specialCharInputWindow, WindowEvent.WINDOW_CLOSING));
				
				lii.stop(); //Stop interpreter if window is closed
			}
		});
		
		JPanel contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setDoubleBuffered(true);
		scrollPane.setRequestFocusEnabled(false);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		//Pane for displaying output
		shell = new JTextPane();
		shell.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		shell.setBackground(Color.BLACK);
		shell.setEditable(false);
		shell.setAutoscrolls(true);
		shell.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
		shell.setMargin(new Insets(3, 5, 0, 5));
		shell.setFocusTraversalKeysEnabled(false);
		shellKeyListener = new KeyAdapter() {
			private StringBuilder lineTmp = new StringBuilder();
			private String lastHistoryEntryUsed = "";
			
			@Override
			public void keyTyped(KeyEvent e) {
				if(flagEnd)
					return;
				
				char c = e.getKeyChar();
				if(c == KeyEvent.CHAR_UNDEFINED)
					return;
				if((c > -1 && c < 8) || c == 12 || (c > 13 && c < 32) || c == 127) //Ignores certain control chars
					return;
				
				if(c == '\b') {
					//Remove the last char (if line is not empty)
					if(lineTmp.length() > 0) {
						removeAutoCompleteText();
						try {
							Document doc = shell.getDocument();
							doc.remove(doc.getLength() - 1, 1);
						}catch(BadLocationException e1) {}
						lineTmp.deleteCharAt(lineTmp.length() - 1);
						highlightSyntaxLastLine();
						updateAutoCompleteText(lineTmp.toString());
					}
				}else if(c == '\n') {
					if(autoCompleteText.isEmpty()) {
						removeAutoCompleteText();
						addLine(lineTmp.toString(), false, false);
						lineTmp.delete(0, lineTmp.length());
						lastHistoryEntryUsed = "";
					}else {
						lineTmp.append(autoCompleteText);
						GraphicsHelper.addText(shell, autoCompleteText, Color.WHITE);
						removeAutoCompleteText();
						highlightSyntaxLastLine();
					}
				}else if(c == '\t') { //Cycle trough auto completes
					int oldAutoCompletePos = autoCompletePos;
					removeAutoCompleteText();
					autoCompletePos = oldAutoCompletePos + (e.isShiftDown()?-1:1);
					updateAutoCompleteText(lineTmp.toString());
				}else {
					removeAutoCompleteText();
					lineTmp.append(c);
					GraphicsHelper.addText(shell, c + "", Color.WHITE);
					highlightSyntaxLastLine();
					updateAutoCompleteText(lineTmp.toString());
				}
			}
			
			@Override
			public void keyPressed(KeyEvent e) {
				if(flagEnd) {
					if(e.getKeyCode() == KeyEvent.VK_C && e.isControlDown())
						dispatchEvent(new WindowEvent(LangShellWindow.this, WindowEvent.WINDOW_CLOSING));
					
					return;
				}
				
				if(e.getKeyCode() == KeyEvent.VK_C && e.isControlDown() && e.isShiftDown()) {
					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(shell.getSelectedText()), null);
				}else if(e.getKeyCode() == KeyEvent.VK_V && e.isControlDown() && e.isShiftDown()) {
					try {
						removeAutoCompleteText();
						Object copiedRaw = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor);
						if(copiedRaw != null) {
							String copied = copiedRaw.toString();
							String[] lines = copied.split("\n");
							for(int i = 0;i < lines.length;i++) {
								String line = lines[i].trim();
								GraphicsHelper.addText(shell, line, Color.WHITE);
								highlightSyntaxLastLine();
								lineTmp.append(line);
								if(i != lines.length - 1) { //Line has an '\n' at end -> finished line
									addLine(lineTmp.toString(), true, true);
									lineTmp.delete(0, lineTmp.length());
								}
							}
							
							if(lines.length > 1) {
								addLine(lines[lines.length - 1], true, false);
								lineTmp.delete(0, lineTmp.length());
							}
							
							if(flagRunning) {
								if(!flagExecutingQueue) {
									executionQueue.clear();
									term.logln(Level.ERROR, "The interpreter is already executing stuff!\nPress CTRL + C for stopping the execution.", LangShellWindow.class);
								}
							}else if(lines.length > 1) {
								executeCodeFromExecutionQueue();
							}
						}
						
						updateAutoCompleteText(lineTmp.toString());
					}catch(UnsupportedFlavorException e1) {
						term.logln(Level.WARNING, "The clipboard contains no string data!", LangShellWindow.class);
					}catch(HeadlessException|IOException e1) {
						term.logStackTrace(e1, LangShellWindow.class);
					}
				}else if(e.getKeyCode() == KeyEvent.VK_S && e.isControlDown() && e.isShiftDown()) {
					if(specialCharInputWindow == null) {
						specialCharInputWindow = new SpecialCharInputWindow(LangShellWindow.this, new String[] {"^", "▲", "▼"});
						specialCharInputWindow.setVisible(true);
					}
				}else if(e.getKeyCode() == KeyEvent.VK_C && e.isControlDown()) {
					if(flagRunning) {
						lii.stop();
						GraphicsHelper.addText(shell, "^C\n", Color.WHITE);
					}else {
						end();
					}
				}else if(e.getKeyCode() == KeyEvent.VK_L && e.isControlDown()) {
					clear();
				}else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
					removeAutoCompleteText();
					if(historyPos < history.size() - 1) {
						historyPos++;
						
						String historyRet = history.get(historyPos);
						String[] lines = historyRet.split("\n");
						String lastLine = lines[lines.length - 1];
						
						lineTmp.delete(0, lineTmp.length());
						lineTmp.append(lastLine);
						
						removeLines(lastHistoryEntryUsed);
						lastHistoryEntryUsed = historyRet;
						addLinesWithoutExec(historyRet);
						updateAutoCompleteText(lineTmp.toString());
					}else {
						if(historyPos == history.size() - 1)
							historyPos++;
						else
							return;
						
						removeLines(lastHistoryEntryUsed);
						multiLineTmp.delete(0, multiLineTmp.length());
						
						String[] lines = currentCommand.split("\n");
						for(int i = 0;i < lines.length - 1;i++) {
							String line = lines[i];
							GraphicsHelper.addText(shell, line, Color.WHITE);
							highlightSyntaxLastLine();
							addLine(line, false, false);
						}
						lineTmp.delete(0, lineTmp.length());
						lineTmp.append(lines[lines.length - 1]);
						if(lines.length > 1)
							lineTmp.delete(0, 1); //Remove tmp space
						GraphicsHelper.addText(shell, lineTmp.toString(), Color.WHITE);
						highlightSyntaxLastLine();
						updateAutoCompleteText(lineTmp.toString());
					}
				}else if(e.getKeyCode() == KeyEvent.VK_UP) {
					if(historyPos > 0) {
						removeAutoCompleteText();
						if(historyPos == history.size()) {
							currentCommand = lineTmp.toString();
							if(multiLineTmp.length() > 0)
								currentCommand = multiLineTmp.toString() + " " + currentCommand; //Add tmp space for split at "\n" in removeLines()
							lastHistoryEntryUsed = currentCommand;
						}
						
						historyPos--;
						
						String historyRet = history.get(historyPos);
						String[] lines = historyRet.split("\n");
						String lastLine = lines[lines.length - 1];
						
						lineTmp.delete(0, lineTmp.length());
						lineTmp.append(lastLine);
						
						removeLines(lastHistoryEntryUsed);
						lastHistoryEntryUsed = historyRet;
						addLinesWithoutExec(historyRet);
						updateAutoCompleteText(lineTmp.toString());
					}
				}
			}
		};
		shell.addKeyListener(shellKeyListener);
		scrollPane.setViewportView(shell);
		
		initShell(langArgs);
	}
	
	public void setFontSize(int fontSize) {
		shell.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
		
		revalidate();
		
		//Auto scroll
		shell.setCaretPosition(shell.getDocument().getLength());
	}
	
	private void initShell(String[] langArgs) {
		//Sets System.out
		oldOut = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			//Tmp for multibyte char
			private ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			
			private int charsLeftInLogOutput;
			private int type = 0;
			//Colors for the levels
			private Color[] colors = {Color.WHITE, new Color(63, 63, 255), Color.MAGENTA, Color.GREEN, Color.YELLOW, new Color(255, 127, 0), Color.RED, new Color(127, 0, 0)};
			
			@Override
			public void write(int b) throws IOException {
				oldOut.write(b);
				byteOut.write(b);
			}
			
			@Override
			public void flush() throws IOException {
				String output = byteOut.toString();
				byteOut.reset();
				
				updateOutput(output);
				
				//Auto scroll
				shell.setCaretPosition(shell.getDocument().getLength());
			}
			
			private void updateOutput(String output) {
				if(output.length() == 0)
					return;
				
				if(charsLeftInLogOutput > 0) {
					if(output.length() > charsLeftInLogOutput) {
						GraphicsHelper.addText(shell, output.substring(0, charsLeftInLogOutput), colors[type]);
						
						String outputLeft = output.substring(charsLeftInLogOutput);
						charsLeftInLogOutput = 0;
						updateOutput(outputLeft);
					}else {
						charsLeftInLogOutput -= output.length();
						
						GraphicsHelper.addText(shell, output, colors[type]);
					}
					
					return;
				}
				
				int outputLength = getOutputLength(output);
				if(outputLength == -1) {
					type = 0;
					
					int bracketIndex = output.indexOf('[', 1); //Ignore "[" at start, because it was already tested
					
					if(bracketIndex == -1) {
						GraphicsHelper.addText(shell, output, colors[type]);
					}else {
						GraphicsHelper.addText(shell, output.substring(0, bracketIndex), colors[type]);
						
						String outputLeft = output.substring(bracketIndex);
						updateOutput(outputLeft);
					}
					
					return;
				}
				
				charsLeftInLogOutput = outputLength;
				
				//Sets color of message after new line
				if(output.startsWith("[" + Level.NOTSET + "]")) {
					type = 0;
				}else if(output.startsWith("[" + Level.USER + "]")) {
					type = 1;
				}else if(output.startsWith("[" + Level.DEBUG + "]")) {
					type = 2;
				}else if(output.startsWith("[" + Level.CONFIG + "]")) {
					type = 3;
				}else if(output.startsWith("[" + Level.INFO + "]")) {
					type = 4;
				}else if(output.startsWith("[" + Level.WARNING + "]")) {
					type = 5;
				}else if(output.startsWith("[" + Level.ERROR + "]")) {
					type = 6;
				}else if(output.startsWith("[" + Level.CRITICAL + "]")) {
					type = 7;
				}
				
				//Extract message from debug output
				output = output.split("]: ", 2)[1];
				
				if(output.startsWith("[From lang file]: ")) { //Drop "[From lang file]: " prefix
					output = output.substring(18);
					
					charsLeftInLogOutput -= 18;
				}
				
				updateOutput(output);
			}
			
			private int getOutputLength(String output) {
				if(!output.startsWith("[") || !output.contains("]: "))
					return -1;
				
				int msgLenIndex = output.indexOf("][Msg len: ");
				if(msgLenIndex == -1)
					return -1;
				
				msgLenIndex += 11; //Index at end of "][Msg len: "
				int endMsgLenIndex = output.indexOf(']', msgLenIndex);
				if(endMsgLenIndex == -1)
					return -1;
				
				String msgLen = output.substring(msgLenIndex, endMsgLenIndex);
				try {
					return Integer.parseInt(msgLen);
				}catch(NumberFormatException e) {
					return -1;
				}
			}
		}, true));
		
		lii = Lang.createInterpreterInterface(term, langPlatformAPI, langArgs);
		
		//Add debug functions
		lii.addPredefinedFunction("printHelp", (argumentList, DATA_ID) -> {
			term.logln(Level.DEBUG, "func.printHelp() # Prints this help text\n" +
			"func.printDebug(value) # Prints debug information about the provided DataObject", LangShellWindow.class);
			
			return null;
		});
		lii.addPredefinedFunction("printDebug", (argumentList, DATA_ID) -> {
			DataObject dataObject = LangUtils.getNextArgumentAndRemoveUsedDataObjects(argumentList, false);
			if(argumentList.size() > 0) //Not 1 argument
				return lii.setErrnoErrorObject(InterpretingError.INVALID_ARG_COUNT, DATA_ID);
			
			StringBuilder builder = new StringBuilder();
			builder.append("Debug[");
			builder.append(dataObject.getVariableName() == null?"<ANONYMOUS>":dataObject.getVariableName());
			builder.append("]:\n");
			builder.append(getDebugString(dataObject, 4));
			
			term.logln(Level.DEBUG, builder.toString(), LangShellWindow.class);
			
			return null;
		});
		
		//"Remove" input() function: Would not work ("TermIO-Control" window has to be accessible)
		lii.addPredefinedFunction("input", (argumentList, DATA_ID) -> {
			lii.setErrno(InterpretingError.FUNCTION_NOT_SUPPORTED, "Function not supported in the LangShell", DATA_ID);
			return new DataObject().setError(new DataObject.ErrorObject(InterpretingError.FUNCTION_NOT_SUPPORTED));
		});
		
		printWelcomeText();
	}
	private void printWelcomeText() {
		GraphicsHelper.addText(shell, "Lang-Shell", Color.RED);
		GraphicsHelper.addText(shell, " - Press CTRL + C for cancelling execution or for exiting!\n" +
		"• Copy with (CTRL + SHIFT + C) and paste with (CTRL + SHIT + V)\n" +
		"• Press CTRL + SHIFT + S for opening the special char input window\n" +
		"• Press UP and DOWN for scrolling through the history\n" +
		"• Press TAB and SHIFT + TAB for scrolling trough auto complete texts\n" +
		"    ◦ Press ENTER for accepting the auto complete text\n" +
		"• Press CTRL + L to clear the screen\n" +
		"• Use func.printHelp() to get information about LangShell functions\n> ", Color.WHITE);
	}
	
	private String getDebugString(DataObject dataObject, int maxRecursionDepth) {
		if(dataObject == null)
			return "<NULL>";
		
		if(maxRecursionDepth < 1)
			return "<Max recursion depth reached>";
		
		StringBuilder builder = new StringBuilder();
		builder.append("Raw Text: ");
		builder.append(dataObject.getText());
		builder.append("\nType: ");
		builder.append(dataObject.getType());
		builder.append("\nFinal: ");
		builder.append(dataObject.isFinalData());
		builder.append("\nStatic: ");
		builder.append(dataObject.isStaticData());
		builder.append("\nLang var: ");
		builder.append(dataObject.isLangVar());
		builder.append("\nVariable Name: ");
		builder.append(dataObject.getVariableName());
		builder.append("\nAllowed types: ");
		builder.append(dataObject.getTypeConstraint().printAllowedTypes());
		builder.append("\nNot allowed types: ");
		builder.append(dataObject.getTypeConstraint().printNotAllowedTypes());
		switch(dataObject.getType()) {
			case VAR_POINTER:
				builder.append("\nPointing to: {\n");
				String[] debugStringLines = getDebugString(dataObject.getVarPointer().getVar(), maxRecursionDepth - 1).toString().split("\\n");
				for(String debugStringLine:debugStringLines) {
					builder.append("    ");
					builder.append(debugStringLine);
					builder.append("\n");
				}
				builder.append("}");
				break;
			
			case ARRAY:
				builder.append("\nSize: ");
				builder.append(dataObject.getArray().length);
				builder.append("\nElements:");
				for(int i = 0;i < dataObject.getArray().length;i++) {
					DataObject ele = dataObject.getArray()[i];
					builder.append("\n    arr(");
					builder.append(i);
					builder.append("): {\n");
					debugStringLines = getDebugString(ele, maxRecursionDepth > 1?1:0).toString().split("\\n");
					for(String debugStringLine:debugStringLines) {
						builder.append("        ");
						builder.append(debugStringLine);
						builder.append("\n");
					}
					builder.append("    }");
				}
				break;
			
			case FUNCTION_POINTER:
				builder.append("\nFunction-Type: ");
				builder.append(dataObject.getFunctionPointer().getFunctionPointerType());
				builder.append("\nParameter List: ");
				builder.append(String.valueOf(dataObject.getFunctionPointer().getParameterList()).replace("\n", ""));
				builder.append("\nFunction Body: ");
				builder.append(dataObject.getFunctionPointer().getFunctionBody());
				builder.append("\nPredefined Function: ");
				LangPredefinedFunctionObject predefinedFunction = dataObject.getFunctionPointer().getPredefinedFunction();
				if(predefinedFunction == null) {
					builder.append(predefinedFunction);
				}else {
					builder.append("{");
					builder.append("\n    Raw String: ");
					builder.append(predefinedFunction);
					builder.append("\n    Deprecated: ");
					boolean deprecated = predefinedFunction.isDeprecated();
					builder.append(deprecated);
					if(deprecated) {
						builder.append("\n        Will be removed in: ");
						builder.append(predefinedFunction.getDeprecatedRemoveVersion());
						builder.append("\n        Replacement function: ");
						builder.append(predefinedFunction.getDeprecatedReplacementFunction());
					}
					builder.append("\n}");
				}
				builder.append("\nExternal Function: ");
				builder.append(dataObject.getFunctionPointer().getExternalFunction());
				break;
			
			case ERROR:
				builder.append("\nError-Code: ");
				builder.append(dataObject.getError().getErrno());
				builder.append("\nError-Text: ");
				builder.append(dataObject.getError().getErrtxt());
				break;
			
			default:
				break;
		}
		
		return builder.toString();
	}
	
	private void highlightSyntaxLastLine() {
		try {
			Document doc = shell.getDocument();
			int startOfLine;
			for(startOfLine = doc.getLength() - 1;startOfLine > 0;startOfLine--)
				if(doc.getText(startOfLine, 1).charAt(0) == '\n')
					break;
			startOfLine++; //The line starts on char after '\n'
			
			String line = doc.getText(startOfLine, doc.getLength() - startOfLine);
			doc.remove(startOfLine, doc.getLength() - startOfLine);
			
			boolean commentFlag = false, varFlag = false, funcFlag = false, bracketsFlag = false, dereferencingAndReferencingOperatorFlag = false, returnFlag = false, throwFlag = false,
			nullFlag = false;
			for(int i = 0;i < line.length();i++) {
				char c = line.charAt(i);
				
				if(!nullFlag)
					nullFlag = line.substring(i).startsWith("null");
				
				if(!commentFlag && c == '#' && !(i > 0 && line.charAt(i - 1) == '\\'))
					commentFlag = true;
				
				if(!varFlag && (c == '$' || c == '&'))
					varFlag = true;
				
				if(!funcFlag) {
					String checkTmp = line.substring(i);
					funcFlag = checkTmp.startsWith("fp.") || checkTmp.startsWith("func.") || checkTmp.startsWith("linker.") || checkTmp.startsWith("con.") || checkTmp.startsWith("math.") ||
					checkTmp.startsWith("parser.");
				}
				
				if(!returnFlag)
					returnFlag = line.substring(i).startsWith("return");
				if(!throwFlag)
					throwFlag = line.substring(i).startsWith("throw");
				
				bracketsFlag = c == '(' || c == ')' || c == '[' || c == ']' || c == '{' || c == '}' || c == '.' || c == ',';
				dereferencingAndReferencingOperatorFlag = varFlag && (c == '*' || c == '[' || c == ']');
				
				if(varFlag && !(Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c == '[' || c == ']' || c == '.' || c == '$' || c == '*' || c == '&'))
					varFlag = false;
				if(funcFlag && !(Character.isAlphabetic(c) || Character.isDigit(c) || c == '_' || c == '[' || c == ']' || c == '.'))
					funcFlag = false;
				if(returnFlag && i > 5 && line.substring(i - 6).startsWith("return"))
					returnFlag = false;
				if(throwFlag && i > 4 && line.substring(i - 5).startsWith("throw"))
					throwFlag = false;
				if(nullFlag && i > 3 && line.substring(i - 4).startsWith("null"))
					nullFlag = false;
				
				if(varFlag && i > 0 && line.charAt(i - 1) == '\\')
					varFlag = false;
				
				//Remove var highlighting if "&&"
				if(line.substring(i).startsWith("&&") || (i > 0 && line.substring(i - 1).startsWith("&&")))
					varFlag = false;
				
				Color col = Color.WHITE;
				if(commentFlag)
					col = Color.GREEN;
				else if(dereferencingAndReferencingOperatorFlag)
					col = Color.GRAY;
				else if(bracketsFlag)
					col = Color.LIGHT_GRAY;
				else if(funcFlag)
					col = Color.CYAN;
				else if(varFlag)
					col = Color.MAGENTA;
				else if(returnFlag || throwFlag)
					col = Color.LIGHT_GRAY;
				else if(Character.isDigit(c))
					col = Color.YELLOW;
				else if(nullFlag)
					col = Color.YELLOW;
				
				GraphicsHelper.addText(shell, c + "", col);
				lastColor = col;
			}
		}catch(BadLocationException e) {}
		
		//Auto scroll
		shell.setCaretPosition(shell.getDocument().getLength());
	}
	
	private void updateAutoCompleteText(String line) {
		Color col = lastColor.darker().darker();
		if(col.equals(lastColor)) //Color is already the darkest
			col = lastColor.brighter().brighter();
		
		if(line.startsWith("lang.") && !line.contains(" ")) {
			int indexConNameStart = line.indexOf('.') + 1;
			String conNameStart = indexConNameStart == line.length()?"":line.substring(indexConNameStart);
			List<String> autoCompletes = langDataAndExecutionFlags.stream().
			filter(conName -> conName.startsWith(conNameStart) && !conName.equals(conNameStart)).
			collect(Collectors.toList());
			if(autoCompletes.isEmpty())
				return;
			autoCompletePos = Math.max(-1, Math.min(autoCompletePos, autoCompletes.size()));
			if(autoCompletePos < 0 || autoCompletePos >= autoCompletes.size())
				autoCompleteText = "";
			else
				autoCompleteText = autoCompletes.get(autoCompletePos).substring(conNameStart.length());
		}else {
			String[] tokens = line.split(".(?=\\$|&|fp\\.|func\\.|linker\\.|con\\.|math\\.|parser\\.)");
			if(tokens.length == 0)
				return;
			
			String lastToken = tokens[tokens.length - 1];
			if(lastToken.matches("(\\$|&|fp\\.).*")) {
				final int appendClosingBracketCount;
				if(lastToken.matches("\\$\\**\\[*\\w*")) {
					//Handle var pointer referencing and dereferencing "$*" and "$["
					
					lastToken = lastToken.replace("*", ""); //Ignore "*"
					
					int oldLen = lastToken.length();
					lastToken = lastToken.replace("[", ""); //Ignore "["
					int diff = oldLen - lastToken.length();
					if(diff == 0 && lastToken.length() > 0)
						appendClosingBracketCount = -1;
					else
						appendClosingBracketCount = diff;
				}else {
					appendClosingBracketCount = -1;
				}
				
				final String lastTokenCopy = lastToken;
				List<String> autoCompletes = lii.getData(0).var.keySet().stream().filter(varName -> {
					int oldLen = varName.length();
					varName = varName.replace("[", "");
					
					return (oldLen == varName.length() || appendClosingBracketCount > -1) && varName.startsWith(lastTokenCopy) && !varName.equals(lastTokenCopy);
				}).sorted().collect(Collectors.toList());
				if(autoCompletes.isEmpty())
					return;
				autoCompletePos = Math.max(-1, Math.min(autoCompletePos, autoCompletes.size()));
				if(autoCompletePos < 0 || autoCompletePos >= autoCompletes.size()) {
					autoCompleteText = "";
				}else {
					autoCompleteText = autoCompletes.get(autoCompletePos).replace("]", "");
					
					int openingBracketCountVarName = (int)autoCompleteText.chars().filter(c -> c == '[').count();
					int diff = Math.max(0, openingBracketCountVarName - Math.max(0, appendClosingBracketCount));
					for(int i = 0;i < diff;i++)
						autoCompleteText = "$[" + autoCompleteText.substring(1);
					
					autoCompleteText = autoCompleteText.substring(openingBracketCountVarName - diff + lastTokenCopy.length()) + (lastTokenCopy.startsWith("fp.")?"(":"");
					
					for(int i = 0;i < Math.max(appendClosingBracketCount, openingBracketCountVarName);i++)
						autoCompleteText += "]";
				}
			}else if(lastToken.matches("(func|linker)\\..*")) {
				boolean isLinkerFunction = lastToken.startsWith("linker.");
				int indexFunctionNameStart = lastToken.indexOf('.') + 1;
				String functionNameStart = indexFunctionNameStart == lastToken.length()?"":lastToken.substring(indexFunctionNameStart);
				List<String> autoCompletes = lii.getPredefinedFunctions().entrySet().stream().filter(entry -> {
					return entry.getValue().isLinkerFunction() == isLinkerFunction;
				}).map(Entry<String, LangPredefinedFunctionObject>::getKey).filter(functionName ->
				functionName.startsWith(functionNameStart) && !functionName.equals(functionNameStart)).
				sorted().collect(Collectors.toList());
				if(autoCompletes.isEmpty())
					return;
				autoCompletePos = Math.max(-1, Math.min(autoCompletePos, autoCompletes.size()));
				if(autoCompletePos < 0 || autoCompletePos >= autoCompletes.size()) {
					autoCompleteText = "";
				}else {
					autoCompleteText = autoCompletes.get(autoCompletePos).substring(functionNameStart.length());
					
					//Mark deprecated function
					if(lii.getPredefinedFunctions().get(functionNameStart + autoCompleteText).isDeprecated())
						col = Color.RED.darker().darker();
					
					autoCompleteText += "(";
				}
			}else if(lastToken.matches("con\\..*")) {
				int indexConNameStart = lastToken.indexOf('.') + 1;
				String conNameStart = indexConNameStart == lastToken.length()?"":lastToken.substring(indexConNameStart);
				List<String> autoCompletes = controlFlowStatements.stream().
				filter(conName -> conName.startsWith(conNameStart) && !conName.equals(conNameStart)).
				collect(Collectors.toList());
				if(autoCompletes.isEmpty())
					return;
				autoCompletePos = Math.max(-1, Math.min(autoCompletePos, autoCompletes.size()));
				if(autoCompletePos < 0 || autoCompletePos >= autoCompletes.size())
					autoCompleteText = "";
				else
					autoCompleteText = autoCompletes.get(autoCompletePos).substring(conNameStart.length());
			}else if(lastToken.matches("parser\\..*")) {
				int indexConNameStart = lastToken.indexOf('.') + 1;
				String functionNameStart = indexConNameStart == lastToken.length()?"":lastToken.substring(indexConNameStart);
				List<String> autoCompletes = parserFunctions.stream().
				filter(functionName -> functionName.startsWith(functionNameStart) && !functionName.equals(functionNameStart)).
				collect(Collectors.toList());
				if(autoCompletes.isEmpty())
					return;
				autoCompletePos = Math.max(-1, Math.min(autoCompletePos, autoCompletes.size()));
				if(autoCompletePos < 0 || autoCompletePos >= autoCompletes.size())
					autoCompleteText = "";
				else
					autoCompleteText = autoCompletes.get(autoCompletePos).substring(functionNameStart.length());
			}else {
				return;
			}
		}
		
		GraphicsHelper.addText(shell, autoCompleteText, col);
	}
	private void removeAutoCompleteText() {
		try {
			Document doc = shell.getDocument();
			int startOfAutoComplete = doc.getLength() - autoCompleteText.length();
			doc.remove(startOfAutoComplete, autoCompleteText.length());
		}catch(BadLocationException e) {}
		autoCompleteText = "";
		autoCompletePos = 0;
	}
	
	private void addToHistory(String str) {
		if(!str.trim().isEmpty() && (history.isEmpty() || !history.get(history.size() - 1).equals(str)))
			history.add(str);
		
		historyPos = history.size();
		currentCommand = "";
	}
	
	private void resetAddLineFlags() {
		flagMultilineText = flagLineContinuation = false;
	}
	private void removeLines(String str) {
		resetAddLineFlags();
		multiLineTmp.delete(0, multiLineTmp.length());
		indent = 0;
		
		String[] lines = str.split("\n");
		for(int i = 0;i < lines.length;i++) {
			try {
				Document doc = shell.getDocument();
				int startOfLine;
				for(startOfLine = doc.getLength() - 1;startOfLine > 0;startOfLine--)
					if(doc.getText(startOfLine, 1).charAt(0) == '\n')
						break;
				doc.remove(startOfLine, doc.getLength() - startOfLine);
			}catch(BadLocationException e) {}
		}
		
		GraphicsHelper.addText(shell, "\n> ", Color.WHITE);
	}
	private void addLinesWithoutExec(String str) {
		String lastLine = str;
		if(str.contains("\n")) {
			String[] lines = str.split("\n");
			for(int i = 0;i < lines.length - 1;i++) {
				String line = lines[i];
				
				GraphicsHelper.addText(shell, line, Color.WHITE);
				highlightSyntaxLastLine();
				
				addLine(line, false, false);
			}
			lastLine = lines[lines.length - 1];
		}
		
		GraphicsHelper.addText(shell, lastLine, Color.WHITE);
		highlightSyntaxLastLine();
	}
	private boolean containsMultilineText(String line) {
		while(line.contains("{{{")) {
			//Ignore escaped multiline text start sequences
			int startIndex = line.indexOf("{{{");
			if(startIndex > 0 && !LangUtils.isBackshlashAtIndexEscaped(line, startIndex - 1)) {
				line = line.substring(startIndex + 3);
				continue;
			}
			
			int index = line.indexOf("}}}");
			if(index == -1)
				return true;
			
			line = line.substring(index + 3);
		}
		
		return false;
	}
	private boolean hasMultilineTextEnd(String line) {
		while(line.contains("}}}")) {
			int index = line.indexOf("{{{");
			if(index == -1)
				return true;
			
			line = line.substring(index + 3);
		}
		
		return false;
	}
	private void addLine(String line, boolean addToExecutionQueueOrExecute, boolean addNewLinePromptForLinesPutInExecutionQueue) {
		if(!flagMultilineText && !flagLineContinuation && indent == 0) {
			GraphicsHelper.addText(shell, "\n", Color.WHITE);
			
			flagMultilineText = containsMultilineText(line);
			if(!flagMultilineText)
				flagLineContinuation = line.endsWith("\\");
			if(line.trim().endsWith("{") || (line.trim().startsWith("con.") && !line.trim().startsWith("con.end") && !line.trim().startsWith("con.break") &&
					!line.trim().startsWith("con.continue")) ||
			flagMultilineText || flagLineContinuation) {
				indent++;
				multiLineTmp.append(line);
				multiLineTmp.append("\n");
				
				GraphicsHelper.addText(shell, "    > ", Color.WHITE);
			}else {
				addToHistory(line);
				if(addToExecutionQueueOrExecute) {
					executionQueue.add(line);
					if(addNewLinePromptForLinesPutInExecutionQueue)
						GraphicsHelper.addText(shell, "> ", Color.WHITE);
				}else {
					executeCode(line);
				}
			}
		}else {
			if(!flagMultilineText) {
				flagMultilineText = containsMultilineText(line);
				if(flagMultilineText) {
					if(flagLineContinuation)
						flagLineContinuation = false;
					else
						indent++;
				}
			}
			
			if(!flagMultilineText && (line.trim().endsWith("{") || line.trim().startsWith("con.if") || line.trim().startsWith("con.loop") || line.trim().startsWith("con.while") ||
					line.trim().startsWith("con.until") || line.trim().startsWith("con.repeat") || line.trim().startsWith("con.foreach") || line.trim().startsWith("con.try") ||
					line.trim().startsWith("con.softtry") || line.trim().startsWith("con.nontry")))
				indent++;
			
			multiLineTmp.append(line);
			multiLineTmp.append("\n");
			
			if(!flagMultilineText && (line.trim().startsWith("}") || (line.trim().startsWith("con.") && !line.trim().startsWith("con.loop") && !line.trim().startsWith("con.while") &&
					!line.trim().startsWith("con.until") && !line.trim().startsWith("con.repeat") && !line.trim().startsWith("con.foreach") && !line.trim().startsWith("con.if") &&
					!line.trim().startsWith("con.try") && !line.trim().startsWith("con.starttry") && !line.trim().startsWith("con.nontry") && !line.trim().startsWith("con.break") &&
					!line.trim().startsWith("con.continue")))) {
				indent--;
				
				if(line.trim().startsWith("con.") && !line.trim().startsWith("con.end"))
					indent++;
				
				//Remove the first indent from actual line
				try {
					Document doc = shell.getDocument();
					int startOfLine;
					for(startOfLine = doc.getLength() - 1;startOfLine > 0;startOfLine--)
						if(doc.getText(startOfLine, 1).charAt(0) == '\n')
							break;
					startOfLine++; //The line starts on char after '\n'
					doc.remove(startOfLine, 4);
				}catch(BadLocationException e) {}
			}
			
			if(flagMultilineText && hasMultilineTextEnd(line)) {
				flagMultilineText = false;
				indent--;
				
				//Remove the first indent from actual line
				try {
					Document doc = shell.getDocument();
					int startOfLine;
					for(startOfLine = doc.getLength() - 1;startOfLine > 0;startOfLine--)
						if(doc.getText(startOfLine, 1).charAt(0) == '\n')
							break;
					startOfLine++; //The line starts on char after '\n'
					doc.remove(startOfLine, 4);
				}catch(BadLocationException e) {}
			}
			
			if(!flagMultilineText) {
				if(flagLineContinuation) {
					flagLineContinuation = line.endsWith("\\");
					if(!flagLineContinuation)
						indent--;
				}else {
					flagLineContinuation = line.endsWith("\\");
					if(flagLineContinuation)
						indent++;
				}
			}
			
			GraphicsHelper.addText(shell, "\n", Color.WHITE);
			if(indent < 1) {
				indent = 0;
				String multiLineTmpString = multiLineTmp.toString();
				addToHistory(multiLineTmpString.substring(0, multiLineTmpString.length() - 1)); //Remove "\n"
				
				String code = multiLineTmp.toString();
				if(addToExecutionQueueOrExecute) {
					executionQueue.add(code);
					if(addNewLinePromptForLinesPutInExecutionQueue)
						GraphicsHelper.addText(shell, "> ", Color.WHITE);
				}else {
					executeCode(code);
				}
				
				multiLineTmp.delete(0, multiLineTmp.length());
				currentCommand = "";
			}else {
				for(int i = 0;i < indent;i++)
					GraphicsHelper.addText(shell, "    ", Color.WHITE);
				GraphicsHelper.addText(shell, "> ", Color.WHITE);
			}
		}
	}
	
	private void executeCode(String code) {
		if(flagRunning) {
			term.logln(Level.ERROR, "The interpreter is already executing stuff!\nPress CTRL + C for stopping the execution.", LangShellWindow.class);
		}else {
			flagRunning = true;
			Thread t = new Thread(() -> {
				try {
					lii.exec(0, code);
				}catch(IOException e) {
					term.logStackTrace(e, LangShellWindow.class);
				}catch(LangInterpreter.StoppedException e) {
					term.logStackTrace(e, LangShellWindow.class);
					lii.resetStopFlag();
				}
				GraphicsHelper.addText(shell, "> ", Color.WHITE);
				
				flagRunning = false;
			});
			t.setDaemon(true);
			t.start();
		}
	}
	
	private void executeCodeFromExecutionQueue() {
		if(flagRunning) {
			term.logln(Level.ERROR, "The interpreter is already executing stuff!\nPress CTRL + C for stopping the execution.", LangShellWindow.class);
		}else {
			flagRunning = true;
			flagExecutingQueue = true;
			Thread t = new Thread(() -> {
				while(!executionQueue.isEmpty()) {
					try {
						lii.exec(0, executionQueue.poll());
					}catch(IOException e) {
						term.logStackTrace(e, LangShellWindow.class);
					}catch(LangInterpreter.StoppedException e) {
						term.logStackTrace(e, LangShellWindow.class);
						lii.resetStopFlag();
					}
				}
				GraphicsHelper.addText(shell, "> ", Color.WHITE);
				
				flagExecutingQueue = false;
				flagRunning = false;
			});
			t.setDaemon(true);
			t.start();
		}
	}
	
	private void clear() {
		shell.setText("");
		printWelcomeText();
	}
	private void end() {
		flagEnd = true;
		
		GraphicsHelper.addText(shell, "^C\nTranslation map:\n", Color.WHITE);
		Map<String, String> lang = lii.getTranslationMap(0);
		lang.forEach((key, value) -> {
			term.logln(Level.DEBUG, key + " = " + value, LangShellWindow.class);
		});
		
		boolean isThrowValue = lii.isReturnedValueThrowValue();
		DataObject retValue = lii.getAndResetReturnValue();
		if(isThrowValue) {
			GraphicsHelper.addText(shell, "Throw Value:\n", Color.WHITE);
			term.logf(Level.DEBUG, "Error code: \"%d\"\nError message: \"%s\"\n", LangShellWindow.class, retValue.getError().getErrno(), retValue.getError().getErrtxt());
		}else {
			GraphicsHelper.addText(shell, "Returned Value:\n", Color.WHITE);
			if(retValue == null)
				term.logln(Level.DEBUG, "No returned value", LangShellWindow.class);
			else
				term.logf(Level.DEBUG, "Returned Value: \"%s\"\n", LangShellWindow.class, retValue.getText());
		}
		
		//Reset the printStream output
		System.setOut(oldOut);
	}
	
	private final class SpecialCharInputWindow extends JDialog {
		private static final long serialVersionUID = -5520154945750708443L;
		
		public SpecialCharInputWindow(Dialog owner, String[] specialCharInputs) {
			super(owner, false); //Make this window to an modal window (Focus won't be given back to owner window)
			
			setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			setTitle("Special Char Input");
			addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					specialCharInputWindow = null;
				}
			});
			
			int buttonCount = specialCharInputs.length;
			int gridXCount = (int)Math.ceil(Math.sqrt(buttonCount));
			int gridYCount = buttonCount / gridXCount + (buttonCount % gridXCount > 0?1:0);
			
			JPanel contentPane = new JPanel();
			setContentPane(contentPane);
			contentPane.setLayout(new GridLayout(gridYCount, gridXCount, 10, 10));
			contentPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
			
			for(String specialCharInput:specialCharInputs) {
				JButton button = new JButton("   " + specialCharInput + "   ");
				button.addActionListener(e -> {
					for(char c:specialCharInput.toCharArray())
						shellKeyListener.keyTyped(new KeyEvent(this, 0, 0, 0, 0, c, KeyEvent.KEY_LOCATION_UNKNOWN));
					
					LangShellWindow.this.requestFocus();
					shell.requestFocus();
				});
				button.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 24));
				contentPane.add(button);
			}
			
			pack();
			setMinimumSize(getSize());
			setLocationRelativeTo(owner);
		}
	}
}