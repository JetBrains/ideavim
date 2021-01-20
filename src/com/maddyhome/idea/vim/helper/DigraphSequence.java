/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.option.OptionsManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.event.KeyEvent;

public class DigraphSequence {
  public DigraphSequence() {
  }

  public static boolean isDigraphStart(@NotNull KeyStroke key) {
    return key.getKeyCode() == KeyEvent.VK_K && (key.getModifiers() & KeyEvent.CTRL_DOWN_MASK) != 0;
  }

  public static boolean isLiteralStart(@NotNull KeyStroke key) {
    return (key.getKeyCode() == KeyEvent.VK_V || key.getKeyCode() == KeyEvent.VK_Q) && (key.getModifiers() & KeyEvent.CTRL_DOWN_MASK) != 0;
  }

  public DigraphResult startDigraphSequence() {
    logger.debug("startDigraphSequence");

    digraphState = DIG_STATE_DIG_ONE;
    return DigraphResult.HANDLED_DIGRAPH;
  }

  public DigraphResult startLiteralSequence() {
    logger.debug("startLiteralSequence");

    digraphState = DIG_STATE_CODE_START;
    codeChars = new char[8];
    codeCnt = 0;
    return DigraphResult.HANDLED_LITERAL;
  }

  public @NotNull DigraphResult processKey(@NotNull KeyStroke key, @NotNull Editor editor) {
    switch (digraphState) {
      case DIG_STATE_PENDING:
        logger.debug("DIG_STATE_PENDING");
        if (key.getKeyCode() == KeyEvent.VK_BACK_SPACE && OptionsManager.INSTANCE.getDigraph().isSet()) {
          digraphState = DIG_STATE_BACK_SPACE;
        } else if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
          digraphChar = key.getKeyChar();
        }
        return DigraphResult.UNHANDLED;

      case DIG_STATE_BACK_SPACE:
        logger.debug("DIG_STATE_BACK_SPACE");
        digraphState = DIG_STATE_PENDING;
        if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
          char ch = VimPlugin.getDigraph().getDigraph(digraphChar, key.getKeyChar());
          digraphChar = 0;
          return DigraphResult.done(KeyStroke.getKeyStroke(ch));
        }
        return DigraphResult.UNHANDLED;

      case DIG_STATE_DIG_ONE:
        logger.debug("DIG_STATE_DIG_ONE");
        if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
          digraphChar = key.getKeyChar();
          digraphState = DIG_STATE_DIG_TWO;

          return DigraphResult.handled(digraphChar);
        }
        digraphState = DIG_STATE_PENDING;
        return DigraphResult.BAD;

      case DIG_STATE_DIG_TWO:
        logger.debug("DIG_STATE_DIG_TWO");
        digraphState = DIG_STATE_PENDING;
        if (key.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
          char ch = VimPlugin.getDigraph().getDigraph(digraphChar, key.getKeyChar());

          return DigraphResult.done(KeyStroke.getKeyStroke(ch));
        }
        return DigraphResult.BAD;

      case DIG_STATE_CODE_START:
        logger.debug("DIG_STATE_CODE_START");
        switch (key.getKeyChar()) {
          case 'o':
          case 'O':
            codeMax = 3;
            digraphState = DIG_STATE_CODE_CHAR;
            codeType = 8;
            logger.debug("Octal");
            return DigraphResult.HANDLED_LITERAL;
          case 'x':
          case 'X':
            codeMax = 2;
            digraphState = DIG_STATE_CODE_CHAR;
            codeType = 16;
            logger.debug("hex2");
            return DigraphResult.HANDLED_LITERAL;
          case 'u':
            codeMax = 4;
            digraphState = DIG_STATE_CODE_CHAR;
            codeType = 16;
            logger.debug("hex4");
            return DigraphResult.HANDLED_LITERAL;
          case 'U':
            codeMax = 8;
            digraphState = DIG_STATE_CODE_CHAR;
            codeType = 16;
            logger.debug("hex8");
            return DigraphResult.HANDLED_LITERAL;
          case '0':
          case '1':
          case '2':
          case '3':
          case '4':
          case '5':
          case '6':
          case '7':
          case '8':
          case '9':
            codeMax = 3;
            digraphState = DIG_STATE_CODE_CHAR;
            codeType = 10;
            codeChars[codeCnt++] = key.getKeyChar();
            logger.debug("decimal");
            return DigraphResult.HANDLED_LITERAL;
          default:
            switch (key.getKeyCode()) {
              case KeyEvent.VK_TAB:
                KeyStroke code = KeyStroke.getKeyStroke('\t');
                digraphState = DIG_STATE_PENDING;

                return DigraphResult.done(code);
              default:
                logger.debug("unknown");
                digraphState = DIG_STATE_PENDING;

                return DigraphResult.done(key);
            }
        }

      case DIG_STATE_CODE_CHAR:
        logger.debug("DIG_STATE_CODE_CHAR");
        boolean valid = false;
        switch (codeType) {
          case 10:
            if (key.getKeyChar() >= '0' && key.getKeyChar() <= '9') {
              valid = true;
            }
            break;
          case 8:
            if (key.getKeyChar() >= '0' && key.getKeyChar() <= '7') {
              valid = true;
            }
            break;
          case 16:
            if (key.getKeyChar() >= '0' && key.getKeyChar() <= '9' ||
                key.getKeyChar() >= 'a' && key.getKeyChar() <= 'f' ||
                key.getKeyChar() >= 'A' && key.getKeyChar() <= 'F') {
              valid = true;
            }
            break;
        }
        if (valid) {
          logger.debug("valid");
          codeChars[codeCnt++] = key.getKeyChar();
          if (codeCnt == codeMax) {
            String digits = new String(codeChars, 0, codeCnt);
            int val = Integer.parseInt(digits, codeType);
            KeyStroke code = KeyStroke.getKeyStroke((char)val);
            digraphState = DIG_STATE_PENDING;

            return DigraphResult.done(code);
          }
          else {
            return DigraphResult.HANDLED_LITERAL;
          }
        }
        else if (codeCnt > 0) {
          logger.debug("invalid");
          String digits = new String(codeChars, 0, codeCnt);
          int val = Integer.parseInt(digits, codeType);
          digraphState = DIG_STATE_PENDING;
          KeyStroke code = KeyStroke.getKeyStroke((char)val);

          if (!ApplicationManager.getApplication().isUnitTestMode()) {
            // The key we received isn't part of the literal, so post it to be handled after we've handled the literal.
            // This requires swing, so we can't run it in tests.
            VimPlugin.getMacro().postKey(key, editor);
          }

          return DigraphResult.done(code);
        }
        return DigraphResult.BAD;

      default:
        return DigraphResult.BAD;
    }
  }

  public void reset() {
    digraphState = DIG_STATE_PENDING;
    codeChars = new char[8];
  }

  private int digraphState = DIG_STATE_PENDING;
  private char digraphChar;
  private char[] codeChars;
  private int codeCnt;
  private int codeType;
  private int codeMax;

  private static final int DIG_STATE_PENDING = 1;
  private static final int DIG_STATE_DIG_ONE = 2;
  private static final int DIG_STATE_DIG_TWO = 3;
  private static final int DIG_STATE_CODE_START = 10;
  private static final int DIG_STATE_CODE_CHAR = 11;
  private static final int DIG_STATE_BACK_SPACE = 20;

  private static final Logger logger = Logger.getInstance(DigraphSequence.class.getName());
}
