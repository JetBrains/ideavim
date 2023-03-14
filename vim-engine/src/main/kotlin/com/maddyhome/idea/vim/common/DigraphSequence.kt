/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.DigraphResult.Companion.done
import com.maddyhome.idea.vim.common.DigraphResult.Companion.handled
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

public class DigraphSequence {
  private var digraphState = DIG_STATE_PENDING
  private var digraphChar = 0.toChar()
  private lateinit var codeChars: CharArray
  private var codeCnt = 0
  private var codeType = 0
  private var codeMax = 0

  public fun isDigraphStart(key: KeyStroke): Boolean {
    return digraphState == DIG_STATE_PENDING && // if state has changed, then it's not a start
      key.keyCode == KeyEvent.VK_K && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0
  }

  public fun isLiteralStart(key: KeyStroke): Boolean {
    return digraphState == DIG_STATE_PENDING && // if state has changed, then it's not a start
      (key.keyCode == KeyEvent.VK_V || key.keyCode == KeyEvent.VK_Q) && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0
  }

  public fun startDigraphSequence(): DigraphResult {
    logger.debug("startDigraphSequence")
    digraphState = DIG_STATE_DIG_ONE
    return DigraphResult.HANDLED_DIGRAPH
  }

  public fun startLiteralSequence(): DigraphResult {
    logger.debug("startLiteralSequence")
    digraphState = DIG_STATE_CODE_START
    codeChars = CharArray(8)
    codeCnt = 0
    return DigraphResult.HANDLED_LITERAL
  }

  public fun processKey(key: KeyStroke, editor: VimEditor): DigraphResult {
    return when (digraphState) {
      DIG_STATE_PENDING -> {
        logger.debug("DIG_STATE_PENDING")
        if (key.keyCode == KeyEvent.VK_BACK_SPACE &&
          injector.options(editor).isSet(Options.digraph)
        ) {
          digraphState = DIG_STATE_BACK_SPACE
        } else if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          digraphChar = key.keyChar
        }
        DigraphResult.UNHANDLED
      }
      DIG_STATE_BACK_SPACE -> {
        logger.debug("DIG_STATE_BACK_SPACE")
        digraphState = DIG_STATE_PENDING
        if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          val ch = injector.digraphGroup.getDigraph(digraphChar, key.keyChar)
          digraphChar = 0.toChar()
          return done(KeyStroke.getKeyStroke(ch))
        }
        DigraphResult.UNHANDLED
      }
      DIG_STATE_DIG_ONE -> {
        logger.debug("DIG_STATE_DIG_ONE")
        if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          digraphChar = key.keyChar
          digraphState = DIG_STATE_DIG_TWO
          return handled(digraphChar)
        }
        digraphState = DIG_STATE_PENDING
        DigraphResult.BAD
      }
      DIG_STATE_DIG_TWO -> {
        logger.debug("DIG_STATE_DIG_TWO")
        digraphState = DIG_STATE_PENDING
        if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          val ch = injector.digraphGroup.getDigraph(digraphChar, key.keyChar)
          return done(KeyStroke.getKeyStroke(ch))
        }
        DigraphResult.BAD
      }
      DIG_STATE_CODE_START -> {
        logger.debug("DIG_STATE_CODE_START")
        when (key.keyChar) {
          'o', 'O' -> {
            codeMax = 3
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 8
            logger.debug("Octal")
            DigraphResult.HANDLED_LITERAL
          }
          'x', 'X' -> {
            codeMax = 2
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 16
            logger.debug("hex2")
            DigraphResult.HANDLED_LITERAL
          }
          'u' -> {
            codeMax = 4
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 16
            logger.debug("hex4")
            DigraphResult.HANDLED_LITERAL
          }
          'U' -> {
            codeMax = 8
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 16
            logger.debug("hex8")
            DigraphResult.HANDLED_LITERAL
          }
          '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
            codeMax = 3
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 10
            codeChars[codeCnt++] = key.keyChar
            logger.debug("decimal")
            DigraphResult.HANDLED_LITERAL
          }
          else -> {
            if (key.keyCode == KeyEvent.VK_TAB) {
              val code = KeyStroke.getKeyStroke('\t')
              digraphState = DIG_STATE_PENDING
              return done(code)
            }
            val ks = specialKeyToKeyCode(key)
            if (ks != null) {
              return done(ks)
            }
            logger.debug("unknown")
            digraphState = DIG_STATE_PENDING
            done(key)
          }
        }
      }
      DIG_STATE_CODE_CHAR -> {
        logger.debug("DIG_STATE_CODE_CHAR")
        var valid = false
        when (codeType) {
          10 -> if (key.keyChar in '0'..'9') {
            valid = true
          }
          8 -> if (key.keyChar in '0'..'7') {
            valid = true
          }
          16 -> if (key.keyChar in '0'..'9' || key.keyChar in 'a'..'f' || key.keyChar in 'A'..'F') {
            valid = true
          }
        }
        if (valid) {
          logger.debug("valid")
          codeChars[codeCnt++] = key.keyChar
          return if (codeCnt == codeMax) {
            val digits = String(codeChars, 0, codeCnt)
            val `val` = digits.toInt(codeType)
            val code = KeyStroke.getKeyStroke(`val`.toChar())
            digraphState = DIG_STATE_PENDING
            done(code)
          } else {
            DigraphResult.HANDLED_LITERAL
          }
        } else if (codeCnt > 0) {
          logger.debug("invalid")
          val digits = String(codeChars, 0, codeCnt)
          val `val` = digits.toInt(codeType)
          digraphState = DIG_STATE_PENDING
          val code = KeyStroke.getKeyStroke(`val`.toChar())
          if (!injector.application.isUnitTest()) {
            // The key we received isn't part of the literal, so post it to be handled after we've handled the literal.
            // This requires swing, so we can't run it in tests.
            injector.application.postKey(key, editor)
          }
          return done(code)
        } else if (codeCnt == 0) {
          digraphState = DIG_STATE_PENDING
          return if (specialKeyToKeyCode(key) != null) {
            done(specialKeyToKeyCode(key))
          } else {
            done(key)
          }
        }
        DigraphResult.BAD
      }
      else -> DigraphResult.BAD
    }
  }

  private fun specialKeyToKeyCode(key: KeyStroke): KeyStroke? {
    if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
      val specialKeyCode = injector.parser.parseVimScriptString("\\" + injector.parser.toKeyNotation(key))
      if (specialKeyCode.length == 1) {
        return if (specialKeyCode[0].code == 10) {
          KeyStroke.getKeyStroke(0.toChar())
        } else {
          KeyStroke.getKeyStroke(specialKeyCode[0])
        }
      } else {
        logger.error("Digraph char was recognized as multiple chars")
      }
    } else {
      return when (key.keyCode) {
        10 -> KeyStroke.getKeyStroke(13.toChar())
        27 -> KeyStroke.getKeyStroke(27.toChar())
        else -> null
      }
    }
    return null
  }

  public fun reset() {
    digraphState = DIG_STATE_PENDING
    codeChars = CharArray(8)
  }

  public companion object {
    private const val DIG_STATE_PENDING = 1
    private const val DIG_STATE_DIG_ONE = 2
    private const val DIG_STATE_DIG_TWO = 3
    private const val DIG_STATE_CODE_START = 10
    private const val DIG_STATE_CODE_CHAR = 11
    private const val DIG_STATE_BACK_SPACE = 20
    private val logger = vimLogger<DigraphSequence>()
  }
}
