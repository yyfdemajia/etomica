package etomica.graph.engine;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;

// Have a local buffer with the whole data (until clear);
// Have a window size to determine how much is to be shown at any given point;
// Have a method update() that synchronizes the viewport with the buffer;
// Synchronize the scrollbar vertical length with the buffer size;
// Synchronize the scrollbar vertical scrolling with the viewport;
// For instance, use the length of the vertical scrollbar as the length
// of the buffer (in lines) so that scrolling maps the viewport to the
// the block starting from the line corresponding to the scrollbar position
// up to the viewport size...
// something like that; the jtextarea component cannot handle a large number
// of updates smoothly due to GUI processing issues- the processing has to
// be done in another thread for this to work decently
public class ConsoleUI extends JTextArea implements Console {

  private static final long serialVersionUID = 8775341949270012136L;

  private StringBuffer buffer = null;
  private int lineStart = 0;
  private ConsoleReader reader;

  public ConsoleUI() {

    setBackground(Color.black);
    setFont(new Font("monospaced", Font.PLAIN, 12));
    setForeground(Color.green);
    setCaretColor(Color.green);
    setLineWrap(true);
    setWrapStyleWord(true);
    addKeyListener(new KeyAdapter() {

      @Override
      public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {
          case KeyEvent.VK_LEFT:
          case KeyEvent.VK_BACK_SPACE:
            if (getCaretPosition() < lineStart || !canMoveLeft()) {
              e.consume();
            }
            break;
          case KeyEvent.VK_RIGHT:
          case KeyEvent.VK_DELETE:
            if (getCaretPosition() < lineStart || !canMoveRight()) {
              e.consume();
            }
            break;
          case KeyEvent.VK_ENTER:
            if (getCaretPosition() >= lineStart) {
              e.consume();
              processCommand();
            }
            break;
          case KeyEvent.VK_DOWN:
          case KeyEvent.VK_UP:
            if (getCaretPosition() >= lineStart) {
              e.consume();
            }
            break;
          case KeyEvent.VK_END:
          case KeyEvent.VK_HOME:
            break;
          default:
            e.consume();
        }
      }

      @Override
      public void keyTyped(KeyEvent e) {

        if (getCaretPosition() < lineStart) {
          e.consume();
        }
      }
    });
    printPrompt();
  }

  protected boolean canMoveLeft() {

    return getCaretPosition() > lineStart;
  }

  protected boolean canMoveRight() {

    return getCaretPosition() < getDocument().getLength();
  }

  private String extractCommand() {

    try {
      return getDocument().getText(lineStart, getDocument().getLength() - lineStart);
    }
    catch (BadLocationException e) {
      write(e);
    }
    return "";
  }

  protected String getLastLine() {

    return extractCommand();
  }

  public String getPrompt() {

    return ">";
  }

  private void printPrompt() {

    if (getDocument().getLength() != 0) {
      writeLn();
    }
    write(getPrompt());
    lineStart = getDocument().getLength();
    setCaretPosition(lineStart);
  }

  private void processCommand() {

    String command = extractCommand();
    try {
      if (reader != null) {
        reader.read(command);
      }
    }
    catch (Exception e) {
      write(e);
    }
    printPrompt();
  }

  public void setReader(ConsoleReader reader) {

    this.reader = reader;
  }

  public void updateBegin() {

    buffer = new StringBuffer("");
  }

  public void updateDone() {

    if (buffer != null) {
      append(buffer.toString());
      buffer = null;
      // updateUI, repaint, invalidate
      updateUI();
    }
  }

  private void localAppend(String value) {

    if (buffer != null) {
      buffer.append(value);
    }
    else {
      append(value);
    }
  }

  public void write(Exception e) {

    localAppend(e.getMessage());
  }

  public void write(final String value) {

    localAppend(value);
  }

  public void writeLn() {

    localAppend("\n");
  }

  public void clear() {

    setText(null);
  }
}