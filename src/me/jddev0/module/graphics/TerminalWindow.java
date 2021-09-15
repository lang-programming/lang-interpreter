package me.jddev0.module.graphics;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;

import me.jddev0.module.io.ReaderActionObject;
import me.jddev0.module.io.TerminalIO;
import me.jddev0.module.io.TerminalIO.Level;

/**
 * Uses the io module<br>
 * <br>
 * Graphics-Module<br>
 * Will write TerminalIO output with colors in a window
 * 
 * @author JDDev0
 * @version v1.0.0
 */
public class TerminalWindow extends JFrame {
	private static final long serialVersionUID = 3517996790399999763L;
	
	private JTextField txtEnterCommands;
	private JTextPane term;
	
	private List<String> history = new LinkedList<String>();
	private int historyPos = 0;
	private String currentCommand = "";
	private boolean b = false;
	//Tmp for System.in
	private StringBuilder readingTmp;
	private TerminalIO termIO = null;

	public TerminalWindow() {
		this(12);
	}
	public TerminalWindow(int fontSize) {
		//Creates the TermIO control window
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setTitle("TermIO-Control");
		setSize((int)(750*fontSize / 12.), (int)(500*fontSize / 12.));
		setLocationRelativeTo(null);
		
		JPanel contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout(0, 0));
		
		txtEnterCommands = new JTextField();
		{
			txtEnterCommands.setFocusTraversalKeysEnabled(false);
			txtEnterCommands.addKeyListener(new KeyAdapter() {
				boolean flag = true;
				
				@Override
				public void keyPressed(KeyEvent e) {
					if(flag) {
						currentCommand = txtEnterCommands.getText();
					}
					if(e.getKeyCode() == KeyEvent.VK_ENTER && !b) { //Starts sending command to TerminalIO
						readingTmp = new StringBuilder(txtEnterCommands.getText() + "");
						history.add(readingTmp.toString());
						historyPos = history.size();
						termIO.logln(Level.USER, readingTmp.toString(), TerminalWindow.class);
						
						txtEnterCommands.setText(null);
						b = true;
					}else if(e.getKeyCode() == KeyEvent.VK_TAB) { //Auto completion
						Map<String, ReaderActionObject> commands = termIO.getCommands();
						List<String> tmp = new LinkedList<String>();
						String in = txtEnterCommands.getText();
						
						for(String str : commands.keySet()) {
							if(in.length() != 0 && str.startsWith(in)) {
								tmp.add(str);
							}
						}
						
						if(tmp.size() == 1) {
							txtEnterCommands.setText(tmp.get(0));
						}else if(tmp.size() > 0) {
							String tmpStr = "";
							char tmpC;
							int charIndex = 0;
							
							endWhile:
							while(true) {
								//Get char
								tmpC = tmp.get(0).charAt(charIndex);
								for(int i = 1;i < tmp.size();i++) {
									if(tmp.get(i).length() == charIndex || tmpC != tmp.get(i).charAt(charIndex)) {
										break endWhile;
									}
								}
								
								//Add if char is same
								tmpStr += tmpC;
								charIndex++;
							}
							txtEnterCommands.setText(tmpStr);
							
							String out = "Commands with \"" + tmpStr + "\":\n";
							for(int i = 0;i < tmp.size();i++)
								out += "    " + tmp.get(i) + "\n";
							termIO.log(Level.USER, out, TerminalWindow.class);
						}
					}else if(e.getKeyCode() == KeyEvent.VK_DOWN) {
						if(historyPos < history.size() - 1) {
							flag = false;
							historyPos++;
							txtEnterCommands.setText(history.get(historyPos));
						}else {
							flag = true;
							if(historyPos == history.size() - 1)
								historyPos++;
							txtEnterCommands.setText(currentCommand);
						}
					}else if(e.getKeyCode() == KeyEvent.VK_UP) {
						if(historyPos > 0) {
							flag = false;
							historyPos--;
							txtEnterCommands.setText(history.get(historyPos));
						}
					}else {
						flag = true;
					}
				}
			});
			txtEnterCommands.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
		}
		contentPane.add(txtEnterCommands, BorderLayout.NORTH);
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setDoubleBuffered(true);
		scrollPane.setRequestFocusEnabled(false);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		contentPane.add(scrollPane, BorderLayout.CENTER);
		
		//Pane for displaying output
		term = new JTextPane();
		term.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
		term.setBackground(Color.BLACK);
		term.setEditable(false);
		term.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
		term.setMargin(new Insets(3, 5, 0, 5));
		term.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				if(e.isControlDown())
					return;
				
				txtEnterCommands.requestFocus();
				char c = e.getKeyChar();
				if(c == KeyEvent.CHAR_UNDEFINED)
					return;
				if((c > -1 && c < 8) || c == 12 || (c > 13 && c < 32)) //Ignores certain control chars
					return;
				txtEnterCommands.setText(txtEnterCommands.getText() + c);
			}
		});
		scrollPane.setViewportView(term);
		
		//Sets System.in
		System.setIn(new InputStream() {
			//Tmp for multibyte char
			private byte[] inputTmp;
			private int inputTmpPos;
			
			boolean n = false;
			@Override
			public int read() throws IOException {
				if(inputTmp != null && inputTmpPos < inputTmp.length)
					return inputTmp[inputTmpPos++];
				
				if(n) {
					n = false;
					return -1;
				}
				
				while(!b) {
					try {
						Thread.sleep(250);
					} catch (InterruptedException e) {
						termIO.logStackTrace(e, TerminalWindow.class);
					}
				}
				
				if(readingTmp.length() == 0) {
					b = false;
					n = true;
					
					return '\n';
				}
				
				inputTmp = readingTmp.substring(0, 1).getBytes();
				inputTmpPos = 0;
				readingTmp.delete(0, 1);
				return inputTmp[inputTmpPos++];
			}
		});
		
		//Sets System.out
		PrintStream out = System.out;
		System.setOut(new PrintStream(new OutputStream() {
			//Tmp for multibyte char
			private ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			
			//Tmp for the printing
			private StringBuilder printingTmp = new StringBuilder();
			private int type = 0;
			//Colors for the levels
			private Color[] colors = {Color.WHITE, Color.BLUE, Color.MAGENTA, Color.GREEN, Color.YELLOW, new Color(255, 127, 0), Color.RED, new Color(127, 0, 0)};
			
			@Override
			public void write(int b) throws IOException {
				out.write(b);
				byteOut.write(b);
			}
			
			@Override
			public void flush() throws IOException {
				String output = byteOut.toString();
				byteOut.reset();
				while(output.contains("\n")) {
					int newLineIndex = output.indexOf('\n');
					printingTmp.append(output.substring(0, newLineIndex + 1));
					
					//Sets color of message after new line
					String printStr = printingTmp.toString();
					if(printStr.startsWith("[" + Level.NOTSET + "]")) {
						type = 0;
					}else if(printStr.startsWith("[" + Level.USER + "]")) {
						type = 1;
					}else if(printStr.startsWith("[" + Level.DEBUG + "]")) {
						type = 2;
					}else if(printStr.startsWith("[" + Level.CONFIG + "]")) {
						type = 3;
					}else if(printStr.startsWith("[" + Level.INFO + "]")) {
						type = 4;
					}else if(printStr.startsWith("[" + Level.WARNING + "]")) {
						type = 5;
					}else if(printStr.startsWith("[" + Level.ERROR + "]")) {
						type = 6;
					}else if(printStr.startsWith("[" + Level.CRITICAL + "]")) {
						type = 7;
					}
					
					//Adds message to term
					if(printStr.contains("[From lang file]: ")) {
						GraphicsHelper.addText(term, printStr.split("\\[From lang file\\]: ")[1], colors[type]);
					}else if(printStr.contains("]: ")) {
						GraphicsHelper.addText(term, printStr.split("\\]: ")[1], colors[type]);
					}else {
						GraphicsHelper.addText(term, printStr, colors[type]);
					}
					
					//Auto scroll
					term.setCaretPosition(term.getDocument().getLength());
					
					//Clears tmp for the printing
					printingTmp.delete(0, printingTmp.length());
					
					if(newLineIndex == output.length() - 1)
						return;
					output = output.substring(output.indexOf('\n') + 1);
				}
				printingTmp.append(output);
			}
		}, true));
	}
	
	public void setFontSize(int fontSize) {
		txtEnterCommands.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
		term.setFont(new Font(Font.MONOSPACED, Font.PLAIN, fontSize));
		
		revalidate();
		
		//Auto scroll
		term.setCaretPosition(term.getDocument().getLength());
	}
	
	public void setTerminalIO(TerminalIO termIO) {
		this.termIO = termIO;
	}
}