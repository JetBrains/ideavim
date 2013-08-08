/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.HistoryGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.text.Document;
import javax.swing.text.Keymap;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Date;
import java.util.List;

/**
 * Provides a custom keymap for the text field. The keymap is the VIM Ex command keymapping
 */
public class ExTextField extends JTextField {
  /**
   */
  public ExTextField() {
    Font font = EditorColorsManager.getInstance().getGlobalScheme().getFont(EditorFontType.PLAIN);
    setFont(font);

    // Do not override getActions() method, because it is has side effect: propogates these actions to defaults.
    final Action[] actions = ExEditorKit.getInstance().getActions();
    final ActionMap actionMap = getActionMap();
    int n = actions.length;
    for (int i = 0; i < n; i++) {
      Action a = actions[i];
      actionMap.put(a.getValue(Action.NAME), a);
      //System.out.println("  " + a.getValue(Action.NAME));
    }

    setInputMap(WHEN_FOCUSED, new InputMap());
    Keymap map = addKeymap("ex", getKeymap());
    loadKeymap(map, ExKeyBindings.getBindings(), actions);
    map.setDefaultAction(new ExEditorKit.DefaultExKeyHandler());
    setKeymap(map);

    //origCaret = getCaret();
    //blockCaret = new BlockCaret();
  }

  public void setType(@NotNull String type) {
    String hkey = null;
    switch (type.charAt(0)) {
      case '/':
      case '?':
        hkey = HistoryGroup.SEARCH;
        break;
      case ':':
        hkey = HistoryGroup.COMMAND;
        break;
    }

    if (hkey != null) {
      history = CommandGroups.getInstance().getHistory().getEntries(hkey, 0, 0);
      histIndex = history.size();
    }
  }

  public void saveLastEntry() {
    lastEntry = getText();
  }

  public void selectHistory(boolean isUp, boolean filter) {
    int dir = isUp ? -1 : 1;
    if (histIndex + dir < 0 || histIndex + dir > history.size()) {
      VimPlugin.indicateError();

      return;
    }

    if (filter) {
      for (int i = histIndex + dir; i >= 0 && i <= history.size(); i += dir) {
        String txt;
        if (i == history.size()) {
          txt = lastEntry;
        }
        else {
          HistoryGroup.HistoryEntry entry = history.get(i);
          txt = entry.getEntry();
        }

        if (txt.startsWith(lastEntry)) {
          updateText(txt);
          histIndex = i;

          return;
        }
      }

      VimPlugin.indicateError();
    }
    else {
      histIndex = histIndex + dir;
      String txt;
      if (histIndex == history.size()) {
        txt = lastEntry;
      }
      else {
        HistoryGroup.HistoryEntry entry = history.get(histIndex);
        txt = entry.getEntry();
      }

      updateText(txt);
    }
  }

  void setEditor(Editor editor, DataContext context) {
    this.editor = editor;
    this.context = context;
  }

  public Editor getEditor() {
    return editor;
  }

  public DataContext getContext() {
    return context;
  }

  public void handleKey(@NotNull KeyStroke stroke) {
    if (logger.isDebugEnabled()) logger.debug("stroke=" + stroke);
    final char keyChar = stroke.getKeyChar();
    char c = keyChar;
    final int modifiers = stroke.getModifiers();
    final int keyCode = stroke.getKeyCode();
    if ((modifiers & KeyEvent.CTRL_MASK) != 0) {
      final int codePoint = keyCode - KeyEvent.VK_A + 1;
      if (codePoint > 0) {
        c = Character.toChars(codePoint)[0];
      }
    }
    KeyEvent event = new KeyEvent(this, keyChar != KeyEvent.CHAR_UNDEFINED ? KeyEvent.KEY_TYPED :
                                        (stroke.isOnKeyRelease() ? KeyEvent.KEY_RELEASED : KeyEvent.KEY_PRESSED),
                                  (new Date()).getTime(), modifiers, keyCode, c);

    super.processKeyEvent(event);
  }

  public void updateText(String string) {
    super.setText(string);
  }

  public void setText(String string) {
    super.setText(string);

    saveLastEntry();
  }

  protected void processKeyEvent(KeyEvent e) {
    if (logger.isDebugEnabled()) logger.debug("key=" + e);
    super.processKeyEvent(e);
    /*
    boolean keep = false;
    switch (e.getID())
    {
        case KeyEvent.KEY_TYPED:
            keep = true;
            break;
        case KeyEvent.KEY_PRESSED:
            logger.debug("pressed");
            if (e.getKeyChar() == KeyEvent.CHAR_UNDEFINED)
            {
                logger.debug("keeping");
                keep = true;
            }
            break;
        case KeyEvent.KEY_RELEASED:
            logger.debug("released");
            if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED)
            {
                if (e.getModifiers() != 0)
                {
                    logger.debug("keeping");
                    keep = true;
                }
            }
            break;
    }
    if (keep)
    {
        KeyHandler.getInstance().handleKey(editor, KeyStroke.getKeyStrokeForEvent(e), context);
        e.consume();
    }
    else
    {
        super.processKeyEvent(e);
    }
    */

  }

  /**
   * Creates the default implementation of the model
   * to be used at construction if one isn't explicitly
   * given.  An instance of <code>PlainDocument</code> is returned.
   *
   * @return the default model implementation
   */
  @NotNull
  protected Document createDefaultModel() {
    return new ExDocument();
  }

  public void escape() {
    if (currentAction != null) {
      currentAction = null;
    }
    else {
      CommandGroups.getInstance().getProcess().cancelExEntry(editor, context);
    }
  }

  public void setCurrentAction(@Nullable Action action) {
    this.currentAction = action;
  }

  @Nullable
  public Action getCurrentAction() {
    return currentAction;
  }

  public void toggleInsertReplace() {
    ExDocument doc = (ExDocument)getDocument();
    doc.toggleInsertReplace();

    /*
    Caret caret;
    int width;
    if (doc.isOverwrite())
    {
        caret = blockCaret;
        width = 8;
    }
    else
    {
        caret = origCaret;
        width = 1;
    }

    setCaret(caret);
    putClientProperty("caretWidth", new Integer(width));
    */
  }

  /*
  private static class BlockCaret extends DefaultCaret
  {
      public void paint(Graphics g)
      {
          if(!isVisible())
              return;

          try
          {
              Rectangle rectangle;
              TextUI textui = getComponent().getUI();
              rectangle = textui.modelToView(getComponent(), getDot(), Position.Bias.Forward);
              if (rectangle == null || rectangle.width == 0 && rectangle.height == 0)
              {
                  return;
              }
              if (width > 0 && height > 0 && !_contains(rectangle.x, rectangle.y, rectangle.width, rectangle.height))
              {
                  Rectangle rectangle1 = g.getClipBounds();
                  if (rectangle1 != null && !rectangle1.contains(this))
                  {
                      repaint();
                  }
                  damage(rectangle);
              }
              g.setColor(getComponent().getCaretColor());
              int i = 8;
              //rectangle.x -= i >> 1;
              g.fillRect(rectangle.x, rectangle.y, i, rectangle.height - 1);
              Document document = getComponent().getDocument();
              if (document instanceof AbstractDocument)
              {
                  Element element = ((AbstractDocument)document).getBidiRootElement();
                  if (element != null && element.getElementCount() > 1)
                  {
                      int[] flagXPoints = new int[3];
                      int[] flagYPoints = new int[3];
                      flagXPoints[0] = rectangle.x + i;
                      flagYPoints[0] = rectangle.y;
                      flagXPoints[1] = flagXPoints[0];
                      flagYPoints[1] = flagYPoints[0] + 4;
                      flagXPoints[2] = flagXPoints[0] + 4;
                      flagYPoints[2] = flagYPoints[0];
                      g.fillPolygon(flagXPoints, flagYPoints, 3);
                  }
              }
          }
          catch (BadLocationException badlocationexception)
          {
              // ignore
          }
      }

      private boolean _contains(int i, int j, int k, int l)
      {
          int i1 = width;
          int j1 = height;
          if ((i1 | j1 | k | l) < 0)
          {
              return false;
          }
          int k1 = x;
          int l1 = y;
          if (i < k1 || j < l1)
          {
              return false;
          }
          if (k > 0)
          {
              i1 += k1;
              k += i;
              if (k <= i)
              {
                  if (i1 >= k1 || k > i1)
                  {
                      return false;
                  }
              }
              else if (i1 >= k1 && k > i1)
              {
                  return false;
              }
          }
          else if (k1 + i1 < i)
          {
              return false;
          }
          if (l > 0)
          {
              j1 += l1;
              l += j;
              if (l <= j)
              {
                  if (j1 >= l1 || l > j1)
                  {
                      return false;
                  }
              }
              else if (j1 >= l1 && l > j1)
              {
                  return false;
              }
          }
          else if (l1 + j1 < j)
          {
              return false;
          }
          return true;
      }
  }
  */

  private Editor editor;
  private DataContext context;
  private String lastEntry;
  private List<HistoryGroup.HistoryEntry> history;
  private int histIndex = 0;
  @Nullable private Action currentAction;
  // TODO - support block cursor for overwrite mode
  //private Caret origCaret;
  //private Caret blockCaret;

  private static final Logger logger = Logger.getInstance(ExTextField.class.getName());
}
