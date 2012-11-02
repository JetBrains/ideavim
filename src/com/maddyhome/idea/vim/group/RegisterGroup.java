package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.Register;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.ui.ClipboardHandler;
import org.jdom.CDATA;
import org.jdom.Element;

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
   * The regsister key for the default register
   */
  public static final char REGISTER_DEFAULT = '"';

  /**
   * Creates the group
   */
  public RegisterGroup() {
  }

  /**
   * Check to see if the last selected register can be written to
   *
   * @return true if writable register, false if not
   */
  public boolean isRegisterWritable() {
    return READONLY_REGISTERS.indexOf(lastRegister) < 0;
  }

  /**
   * Stores which register the user wishes to work with
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
   * Resets the selected register back to the default register
   */
  public void resetRegister() {
    lastRegister = REGISTER_DEFAULT;
    logger.debug("register reset");
  }

  /**
   * Stores text into the last register
   *
   * @param editor   The editor to get the text from
   * @param context  The data context
   * @param range    The range of the text to store
   * @param type     The type of copy - linewise or characterwise
   * @param isDelete is from a delete
   * @param isYank   is from a yank
   * @return true if able to store the text into the register, false if not
   */
  public boolean storeText(Editor editor, DataContext context, TextRange range, int type, boolean isDelete, boolean isYank) {
    if (isRegisterWritable()) {
      String text = EditorHelper.getText(editor, range);

      return storeTextInternal(editor, range, text, type, lastRegister, isDelete, isYank);
    }

    return false;
  }

  public void storeKeys(List<KeyStroke> strokes, int type, char register) {
    registers.put(register, new Register(register, type, strokes));
  }

  public boolean storeTextInternal(Editor editor, TextRange range, String text, int type,
                                   char register, boolean isDelete, boolean isYank) {
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

    if (type == Command.FLAG_MOT_LINEWISE && text.length() > 0 && text.charAt(text.length() - 1) != '\n') {
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
      if (type == Command.FLAG_MOT_CHARACTERWISE) {
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
      CommandGroups.getInstance().getMark().setMark(editor, ']', end - 1);
    }

    return true;
  }

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  public Register getLastRegister() {
    return getRegister(lastRegister);
  }

  public Register getPlaybackRegister(char r) {
    if (PLAYBACK_REGISTER.indexOf(r) != 0) {
      return getRegister(r);
    }
    else {
      return null;
    }
  }

  public Register getRegister(char r) {
    // Uppercase registers actually get the lowercase register
    if (Character.isUpperCase(r)) {
      r = Character.toLowerCase(r);
    }

    Register reg = null;
    if (r == '*' || r == '+') {
      String text = ClipboardHandler.getClipboardText();
      if (text != null) {
        reg = new Register(r, Command.FLAG_MOT_CHARACTERWISE, text);
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

  public void addKeyStroke(KeyStroke key) {
    if (recordRegister != 0) {
      recordList.add(key);
    }
  }

  public void addText(String text) {
    if (recordRegister != 0) {
      recordList.addAll(StringHelper.stringToKeys(text));
    }
  }

  public void finishRecording(Editor editor) {
    if (recordRegister != 0) {
      Register reg = null;
      if (Character.isUpperCase(recordRegister)) {
        reg = getRegister(recordRegister);
      }

      if (reg == null) {
        reg = new Register(Character.toLowerCase(recordRegister), Command.FLAG_MOT_CHARACTERWISE, recordList);
        registers.put(Character.toLowerCase(recordRegister), reg);
      }
      else {
        reg.addKeys(recordList);
      }
      CommandState.getInstance(editor).setRecording(false);
    }

    recordRegister = 0;
  }

  /**
   * Save all the registers
   *
   * @param element The plugin's root XML element that this group can add a child to
   */
  public void saveData(Element element) {
    logger.debug("saveData");
    Element regs = new Element("registers");
    for (Character key : registers.keySet()) {
      Register register = registers.get(key);

      Element reg = new Element("register");
      reg.setAttribute("name", String.valueOf(key));
      reg.setAttribute("type", Integer.toString(register.getType()));
      if (register.isText()) {
        Element text = new Element("text");
        CDATA data = new CDATA(/*CDATA.normalizeString*/(StringHelper.entities(register.getText())));
        text.addContent(data);
        reg.addContent(text);
        if (logger.isDebugEnabled()) {
          logger.debug("register='" + register.getText() + "'");
          logger.debug("data=" + data);
          logger.debug("text=" + text);
        }
      }
      else {
        Element keys = new Element("keys");
        List<KeyStroke> list = register.getKeys();
        for (KeyStroke stroke : list) {
          Element k = new Element("key");
          k.setAttribute("char", Integer.toString(stroke.getKeyChar()));
          k.setAttribute("code", Integer.toString(stroke.getKeyCode()));
          k.setAttribute("mods", Integer.toString(stroke.getModifiers()));
          keys.addContent(k);
        }
        reg.addContent(keys);
      }
      regs.addContent(reg);
    }

    element.addContent(regs);
  }

  /**
   * Restore all the registers
   *
   * @param element The plugin's root XML element that this group can add a child to
   */
  public void readData(Element element) {
    logger.debug("readData");
    Element regs = element.getChild("registers");
    if (regs != null) {
      List<Element> list = regs.getChildren("register");
      for (Element reg : list) {
        Character key = reg.getAttributeValue("name").charAt(0);
        Register register;
        if (reg.getChild("text") != null) {
          register = new Register(key, Integer.parseInt(reg.getAttributeValue("type")),
                                  StringHelper.unentities(reg.getChild("text").getText/*Normalize*/()));
        }
        else {
          Element keys = reg.getChild("keys");
          List<Element> klist = keys.getChildren("key");
          List<KeyStroke> strokes = new ArrayList<KeyStroke>();
          for (Element kelem : klist) {
            int code = Integer.parseInt(kelem.getAttributeValue("code"));
            int mods = Integer.parseInt(kelem.getAttributeValue("mods"));
            char ch = (char)Integer.parseInt(kelem.getAttributeValue("char"));
            if (ch == KeyEvent.CHAR_UNDEFINED) {
              strokes.add(KeyStroke.getKeyStroke(code, mods));
            }
            else {
              strokes.add(KeyStroke.getKeyStroke(new Character(ch), mods));
            }
          }
          register = new Register(key, Integer.parseInt(reg.getAttributeValue("type")), strokes);
        }
        registers.put(key, register);
      }
    }
  }

  private char lastRegister = REGISTER_DEFAULT;
  private HashMap<Character, Register> registers = new HashMap<Character, Register>();
  private char recordRegister = 0;
  private List<KeyStroke> recordList = null;

  private static final String WRITABLE_REGISTERS = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-*+_/\"";
  private static final String READONLY_REGISTERS = ":.%#=/";
  private static final String RECORDABLE_REGISTER = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String PLAYBACK_REGISTER = RECORDABLE_REGISTER + "\".*+";
  private static final String VALID_REGISTERS = WRITABLE_REGISTERS + READONLY_REGISTERS;

  private static Logger logger = Logger.getInstance(RegisterGroup.class.getName());
}
