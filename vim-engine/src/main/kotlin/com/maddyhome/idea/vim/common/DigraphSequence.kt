/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.common.DigraphResult.Companion.done
import com.maddyhome.idea.vim.common.DigraphResult.Companion.handled
import com.maddyhome.idea.vim.diagnostic.vimLogger
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

class DigraphSequence : Cloneable {
  private var digraphState = DIG_STATE_PENDING
  private var digraphChar = 0.toChar()
  private lateinit var codeChars: CharArray
  private var codeCnt = 0
  private var codeType = 0
  private var codeMax = 0

  fun isDigraphStart(key: KeyStroke): Boolean {
    return digraphState == DIG_STATE_PENDING && // if state has changed, then it's not a start
      key.keyCode == KeyEvent.VK_K && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0
  }

  fun isLiteralStart(key: KeyStroke): Boolean {
    return digraphState == DIG_STATE_PENDING && // if state has changed, then it's not a start
      (key.keyCode == KeyEvent.VK_V || key.keyCode == KeyEvent.VK_Q) && key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0
  }

  fun startDigraphSequence(): DigraphResult {
    logger.debug("startDigraphSequence")
    digraphState = DIG_STATE_DIG_ONE
    return DigraphResult.HandledDigraph
  }

  fun startLiteralSequence(): DigraphResult {
    logger.debug("startLiteralSequence")
    digraphState = DIG_STATE_CODE_START
    codeChars = CharArray(8)
    codeCnt = 0
    return DigraphResult.HandledLiteral
  }

  fun processKey(key: KeyStroke, editor: VimEditor): DigraphResult {
    return when (digraphState) {
      DIG_STATE_PENDING -> {
        logger.debug("DIG_STATE_PENDING")
        if (key.keyCode == KeyEvent.VK_BACK_SPACE && injector.options(editor).digraph) {
          digraphState = DIG_STATE_BACK_SPACE
        } else if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          digraphChar = key.keyChar
        }
        DigraphResult.Unhandled
      }

      DIG_STATE_BACK_SPACE -> {
        logger.debug("DIG_STATE_BACK_SPACE")
        digraphState = DIG_STATE_PENDING
        if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          val codepoint = injector.digraphGroup.getCharacterForDigraph(digraphChar, key.keyChar)
          digraphChar = 0.toChar()
          return done(codepoint)
        }
        DigraphResult.Unhandled
      }

      DIG_STATE_DIG_ONE -> {
        logger.debug("DIG_STATE_DIG_ONE")
        if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          digraphChar = key.keyChar
          digraphState = DIG_STATE_DIG_TWO
          return handled(digraphChar)
        }
        digraphState = DIG_STATE_PENDING
        DigraphResult.Bad
      }

      DIG_STATE_DIG_TWO -> {
        logger.debug("DIG_STATE_DIG_TWO")
        digraphState = DIG_STATE_PENDING
        if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
          val codepoint = injector.digraphGroup.getCharacterForDigraph(digraphChar, key.keyChar)
          return done(codepoint)
        }
        DigraphResult.Bad
      }

      DIG_STATE_CODE_START -> {
        logger.debug("DIG_STATE_CODE_START")
        when (key.keyChar) {
          'o', 'O' -> {
            codeMax = 3
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 8
            logger.debug("Octal")
            DigraphResult.HandledLiteral
          }

          'x', 'X' -> {
            codeMax = 2
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 16
            logger.debug("hex2")
            DigraphResult.HandledLiteral
          }

          'u' -> {
            codeMax = 4
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 16
            logger.debug("hex4")
            DigraphResult.HandledLiteral
          }

          'U' -> {
            codeMax = 8
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 16
            logger.debug("hex8")
            DigraphResult.HandledLiteral
          }

          '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> {
            codeMax = 3
            digraphState = DIG_STATE_CODE_CHAR
            codeType = 10
            codeChars[codeCnt++] = key.keyChar
            logger.debug("decimal")
            DigraphResult.HandledLiteral
          }

          else -> {
            if (key.keyCode == KeyEvent.VK_TAB) {
              digraphState = DIG_STATE_PENDING
              return done('\t'.code)
            }
            val codepoint = specialKeyToKeyCode(key)
            if (codepoint != null) {
              return done(codepoint)
            }
            logger.debug("unknown")
            digraphState = DIG_STATE_PENDING
            done(key.keyChar.code)
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
            digraphState = DIG_STATE_PENDING
            val digits = String(codeChars, 0, codeCnt)
            val codepoint = digits.toInt(codeType)
            done(codepoint)
          } else {
            DigraphResult.HandledLiteral
          }
        } else if (codeCnt > 0) {
          logger.debug("invalid")
          digraphState = DIG_STATE_PENDING
          val digits = String(codeChars, 0, codeCnt)
          val codepoint = digits.toInt(codeType)
          if (!injector.application.isUnitTest()) {
            // The key we received isn't part of the literal, so post it to be handled after we've handled the literal.
            // This requires swing, so we can't run it in tests.
            injector.application.postKey(key, editor)
          }
          return done(codepoint)
        } else if (codeCnt == 0) {
          digraphState = DIG_STATE_PENDING
          return specialKeyToKeyCode(key)?.let { done(it) } ?: done(key.keyChar.code)
        }
        DigraphResult.Bad
      }

      else -> DigraphResult.Bad
    }
  }

  private fun specialKeyToKeyCode(key: KeyStroke): Int? {
    // Handle special keys. Specifically, if it's CTRL+something, return the codepoint for `\something`. Or if it's
    // newline, return carriage return (Vim likes to treat newline as null), and escape should be returned as escape
    if (key.modifiers and KeyEvent.CTRL_DOWN_MASK != 0) {
      val specialKeyCode = injector.parser.parseVimScriptString("\\" + injector.parser.toKeyNotation(key))
      if (specialKeyCode.length == 1) {
        // TODO: If we get 10 here, we return 0. If we get 10 below, we return 13. Why the difference?
        return if (specialKeyCode[0].code == 10) 0 else { specialKeyCode[0].code }
      } else {
        logger.error("Digraph char was recognized as multiple chars")
      }
    } else {
      return when (key.keyCode) {
        10 -> 13
        27 -> 27
        else -> null
      }
    }
    return null
  }

  fun reset() {
    digraphState = DIG_STATE_PENDING
    codeChars = CharArray(8)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DigraphSequence

    if (digraphState != other.digraphState) return false
    if (digraphChar != other.digraphChar) return false
    if (codeCnt != other.codeCnt) return false
    if (codeType != other.codeType) return false
    if (codeMax != other.codeMax) return false

    if (::codeChars.isInitialized && other::codeChars.isInitialized) {
      if (!codeChars.contentEquals(other.codeChars)) return false
    } else if (::codeChars.isInitialized != other::codeChars.isInitialized) {
      return false
    }

    return true
  }

  override fun hashCode(): Int {
    var result = digraphState
    result = 31 * result + digraphChar.hashCode()
    result = 31 * result + if (::codeChars.isInitialized) codeChars.contentHashCode() else 0
    result = 31 * result + codeCnt
    result = 31 * result + codeType
    result = 31 * result + codeMax
    return result
  }

  public override fun clone(): DigraphSequence {
    val result = DigraphSequence()
    result.digraphState = digraphState
    result.digraphChar = digraphChar
    if (::codeChars.isInitialized) {
      result.codeChars = codeChars.copyOf()
    }
    result.codeCnt = codeCnt
    result.codeType = codeType
    result.codeMax = codeMax

    return result
  }

  override fun toString(): String {
    return "State = $digraphState, char = $digraphChar"
  }

  companion object {
    private const val DIG_STATE_PENDING = 1
    private const val DIG_STATE_DIG_ONE = 2
    private const val DIG_STATE_DIG_TWO = 3
    private const val DIG_STATE_CODE_START = 10
    private const val DIG_STATE_CODE_CHAR = 11
    private const val DIG_STATE_BACK_SPACE = 20
    private val logger = vimLogger<DigraphSequence>()
  }
}
