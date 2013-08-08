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

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.command.SelectionType;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.ui.ClipboardHandler;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * This group works with command associated with copying and pasting text
 */
public class RegisterGroup extends AbstractActionGroup {
  /**
   * The register key for the default register
   */
  public static final char REGISTER_DEFAULT = '"';

  private static final String WRITABLE_REGISTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-*+_/\"";
  private static final String READONLY_REGISTERS = ":.%#=/";
  private static final String RECORDABLE_REGISTER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String PLAYBACK_REGISTER = RECORDABLE_REGISTER + "\".*+";
  private static final String VALID_REGISTERS = WRITABLE_REGISTERS + READONLY_REGISTERS;

  private static final Logger logger = Logger.getInstance(RegisterGroup.class.getName());

  private char lastRegister = REGISTER_DEFAULT;
  @NotNull private HashMap<Character, Register> registers = new HashMap<Character, Register>();
  private char recordRegister = 0;
  @Nullable private List<KeyStroke> recordList = null;
  public RegisterGroup() {}

  /**
   * Check to see if the last selected register can be written to.
   */
  public boolean isRegisterWritable() {
    return READONLY_REGISTERS.indexOf(lastRegister) < 0;
  }

  /**
   * Store which register the user wishes to work with.
   *
   * @param reg The register name
   * @return true if a valid register name, false if not
   */
  public boolean selectRegister(char reg) {
    if (VALID_REGISTERS.indexOf(reg) != -1) {
      lastRegister = reg;
      if (logger.isDebugEnabled()) logger.debug("register selected: " + lastRegister);

      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Reset the selected register back to the default register.
   */
  public void resetRegister() {
    lastRegister = REGISTER_DEFAULT;
    logger.debug("register reset");
  }

  /**
   * Store text into the last register.
   *
   * @param editor   The editor to get the text from
   * @param range    The range of the text to store
   * @param type     The type of copy
   * @param isDelete is from a delete
   * @return true if able to store the text into the register, false if not
   */
  public boolean storeText(@NotNull Editor editor, @NotNull TextRange range, @NotNull SelectionType type,
                           boolean isDelete) {
    if (isRegisterWritable()) {
      String text = EditorHelper.getText(editor, range);

      return storeTextInternal(editor, range, text, type, lastRegister, isDelete);
    }

    return false;
  }

  public boolean storeTextInternal(@NotNull Editor editor, @NotNull TextRange range, @NotNull String text,
                                   @NotNull SelectionType type, char register, boolean isDelete) {
    // Null register doesn't get saved
    if (lastRegister == '_') return true;

    int start = range.getStartOffset();
    int end = range.getEndOffset();
    // Normalize the start and end
    if (start > end) {
      int t = start;
      start = end;
      end = t;
    }

    if (type == SelectionType.LINE_WISE && text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
      text = text + '\n';
    }

    // If this is an uppercase register, we need to append the text to the corresponding lowercase register
    if (Character.isUpperCase(register)) {
      char lreg = Character.toLowerCase(register);
      Register r = registers.get(new Character(lreg));
      // Append the text if the lowercase register existed
      if (r != null) {
        r.addText(text);
      }
      // Set the text if the lowercase register didn't exist yet
      else {
        registers.put(lreg, new Register(lreg, type, text));
        if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + text + "\"");
      }
    }
    else if (register == '*' || register == '+') {
      ClipboardHandler.setClipboardText(text);
    }
    // Put the text in the specified register
    else {
      registers.put(register, new Register(register, type, text));
      if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + text + "\"");
    }

    // Also add it to the default register if the default wasn't specified
    if (register != REGISTER_DEFAULT && ".:/".indexOf(register) == -1) {
      registers.put(REGISTER_DEFAULT, new Register(REGISTER_DEFAULT, type, text));
      if (logger.isDebugEnabled()) logger.debug("register '" + register + "' contains: \"" + text + "\"");
    }

    // Deletes go into register 1. Old 1 goes to 2, etc. Old 8 to 9, old 9 is lost
    if (isDelete) {
      for (char d = '8'; d >= '1'; d--) {
        Register t = registers.get(new Character(d));
        if (t != null) {
          t.rename((char)(d + 1));
          registers.put((char)(d + 1), t);
        }
      }
      registers.put('1', new Register('1', type, text));

      // Deletes small than one line also go the the - register
      if (type == SelectionType.CHARACTER_WISE) {
        if (editor.offsetToLogicalPosition(start).line == editor.offsetToLogicalPosition(end).line) {
          registers.put('-', new Register('-', type, text));
        }
      }
    }
    // Yanks also go to register 0 if the default register was used
    else if (register == REGISTER_DEFAULT) {
      registers.put('0', new Register('0', type, text));
      if (logger.isDebugEnabled()) logger.debug("register '" + '0' + "' contains: \"" + text + "\"");
    }

    if (start != -1) {
      CommandGroups.getInstance().getMark().setMark(editor, '[', start);
      CommandGroups.getInstance().getMark().setMark(editor, ']', Math.max(end - 1, 0));
    }

    return true;
  }

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  @Nullable
  public Register getLastRegister() {
    return getRegister(lastRegister);
  }

  @Nullable
  public Register getPlaybackRegister(char r) {
    if (PLAYBACK_REGISTER.indexOf(r) != 0) {
      return getRegister(r);
    }
    else {
      return null;
    }
  }

  @Nullable
  public Register getRegister(char r) {
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(r)) {
      r = Character.toLowerCase(r);
    }

    Register reg = null;
    if (r == '*' || r == '+') {
      String text = ClipboardHandler.getClipboardText();
      if (text != null) {
        reg = new Register(r, SelectionType.CHARACTER_WISE, text);
      }
    }
    else {
      reg = registers.get(new Character(r));
    }

    return reg;
  }

  /**
   * Gets the last register name selected by the user
   *
   * @return The register name
   */
  public char getCurrentRegister() {
    return lastRegister;
  }

  @NotNull
  public List<Register> getRegisters() {
    ArrayList<Register> res = new ArrayList<Register>(registers.values());
    Collections.sort(res, new Register.KeySorter<Register>());

    return res;
  }

  public boolean startRecording(Editor editor, char register) {
    if (RECORDABLE_REGISTER.indexOf(register) != -1) {
      CommandState.getInstance(editor).setRecording(true);
      recordRegister = register;
      recordList = new ArrayList<KeyStroke>();
      return true;
    }
    else {
      return false;
    }
  }

  public void recordKeyStroke(KeyStroke key) {
    if (recordRegister != 0 && recordList != null) {
      recordList.add(key);
    }
  }

  public void recordText(@NotNull String text) {
    if (recordRegister != 0 && recordList != null) {
      recordList.addAll(StringHelper.stringToKeys(text));
    }
  }

  public void setKeys(char register, @NotNull List<KeyStroke> keys) {
    registers.put(register, new Register(register, SelectionType.CHARACTER_WISE, keys));
  }

  public void finishRecording(Editor editor) {
    if (recordRegister != 0) {
      Register reg = null;
      if (Character.isUpperCase(recordRegister)) {
        reg = getRegister(recordRegister);
      }

      if (recordList != null) {
        if (reg == null) {
          reg = new Register(Character.toLowerCase(recordRegister), SelectionType.CHARACTER_WISE, recordList);
          registers.put(Character.toLowerCase(recordRegister), reg);
        }
        else {
          reg.addKeys(recordList);
        }
      }
      CommandState.getInstance(editor).setRecording(false);
    }

    recordRegister = 0;
  }

  public void saveData(@NotNull final Element element) {
    logger.debug("saveData");
    final Element registersElement = new Element("registers");
    for (Character key : registers.keySet()) {
      final Register register = registers.get(key);
      final Element registerElement = new Element("register");
      registerElement.setAttribute("name", String.valueOf(key));
      registerElement.setAttribute("type", Integer.toString(register.getType().getValue()));
      final String text = register.getText();
      if (text != null) {
        final Element textElement = new Element("text");
        StringHelper.setSafeXmlText(textElement, text);
        registerElement.addContent(textElement);
      }
      else {
        final Element keys = new Element("keys");
        final List<KeyStroke> list = register.getKeys();
        for (KeyStroke stroke : list) {
          final Element k = new Element("key");
          k.setAttribute("char", Integer.toString(stroke.getKeyChar()));
          k.setAttribute("code", Integer.toString(stroke.getKeyCode()));
          k.setAttribute("mods", Integer.toString(stroke.getModifiers()));
          keys.addContent(k);
        }
        registerElement.addContent(keys);
      }
      registersElement.addContent(registerElement);
    }

    element.addContent(registersElement);
  }

  public void readData(@NotNull final Element element) {
    logger.debug("readData");
    final Element registersElement = element.getChild("registers");
    if (registersElement != null) {
      //noinspection unchecked
      final List<Element> registerElements = registersElement.getChildren("register");
      for (Element registerElement : registerElements) {
        final char key = registerElement.getAttributeValue("name").charAt(0);
        final Register register;
        final Element textElement = registerElement.getChild("text");
        final String typeText = registerElement.getAttributeValue("type");
        final SelectionType type = SelectionType.fromValue(Integer.parseInt(typeText));
        if (textElement != null) {
          final String text = StringHelper.getSafeXmlText(textElement);
          if (text != null) {
            register = new Register(key, type, text);
          }
          else {
            register = null;
          }
        }
        else {
          final Element keysElement = registerElement.getChild("keys");
          //noinspection unchecked
          final List<Element> keyElements = keysElement.getChildren("key");
          final List<KeyStroke> strokes = new ArrayList<KeyStroke>();
          for (Element keyElement : keyElements) {
            final int code = Integer.parseInt(keyElement.getAttributeValue("code"));
            final int modifiers = Integer.parseInt(keyElement.getAttributeValue("mods"));
            final char c = (char)Integer.parseInt(keyElement.getAttributeValue("char"));
            //noinspection MagicConstant
            strokes.add(c == KeyEvent.CHAR_UNDEFINED ?
                        KeyStroke.getKeyStroke(code, modifiers) :
                        KeyStroke.getKeyStroke(c));
          }
          register = new Register(key, type, strokes);
        }
        registers.put(key, register);
      }
    }
  }
}
