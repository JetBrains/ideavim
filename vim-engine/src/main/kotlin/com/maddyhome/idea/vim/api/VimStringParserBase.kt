/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.api

import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls
import java.util.*
import com.maddyhome.idea.vim.key.VimKeyStroke
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.ALT_DOWN_MASK
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CHAR_UNDEFINED
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CTRL_DOWN_MASK
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.META_DOWN_MASK
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.SHIFT_DOWN_MASK
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_BACK_SPACE
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_DELETE
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_DOWN
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_END
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ENTER
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_ESCAPE
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F1
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F10
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F11
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F12
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F2
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F3
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F4
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F5
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F6
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F7
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F8
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_F9
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_HOME
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_INSERT
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_KP_DOWN
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_KP_LEFT
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_KP_RIGHT
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_KP_UP
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_LEFT
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD0
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD1
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD2
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD3
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD4
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD5
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD6
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD7
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD8
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_NUMPAD9
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_PAGE_DOWN
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_PAGE_UP
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_RIGHT
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_TAB
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_UNDO
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_UP

abstract class VimStringParserBase : VimStringParser {
  override val plugKeyStroke: VimKeyStroke
    get() = parseKeys("<Plug>")[0]

  override val actionKeyStroke: VimKeyStroke
    get() = parseKeys("<Action>")[0]

  // todo what is the difference between this one and com.maddyhome.idea.vim.helper.EngineStringHelper#toPrintableCharacters
  override fun toPrintableString(keys: List<VimKeyStroke>): String {
    val builder = StringBuilder()
    for (key in keys) {
      val keyAsChar = keyStrokeToChar(key)
      builder.append(keyAsChar)
    }
    return builder.toString()
  }

  private fun keyStrokeToChar(key: VimKeyStroke): Char {
    if (key.keyChar != CHAR_UNDEFINED) {
      return key.keyChar
    } else if (key.modifiers and CTRL_DOWN_MASK == CTRL_DOWN_MASK) {
      return if (key.keyCode == 'J'.code) {
        // 'J' is a special case, keycode 10 is \n char
        0.toChar()
      } else {
        (key.keyCode - 'A'.code + 1).toChar()
      }
    } else if (key.keyChar == CHAR_UNDEFINED && key.keyCode == VK_ENTER) {
      return '\u000D'
    }
    return key.keyCode.toChar()
  }

  override fun toKeyNotation(keyStrokes: List<VimKeyStroke>): String {
    if (keyStrokes.isEmpty()) {
      return "<Nop>"
    }
    val builder = StringBuilder()
    for (key in keyStrokes) {
      builder.append(toKeyNotation(key))
    }
    return builder.toString()
  }

  override fun toKeyNotation(keyStroke: VimKeyStroke): String {
    val c = keyStroke.keyChar
    val keyCode = keyStroke.keyCode
    val modifiers = keyStroke.modifiers
    if (c != CHAR_UNDEFINED && !isControlCharacter(c)) {
      return c.toString()
    }
    var prefix = ""
    if (modifiers and META_DOWN_MASK != 0) {
      prefix += "M-"
    }
    if (modifiers and ALT_DOWN_MASK != 0) {
      prefix += "A-"
    }
    if (modifiers and CTRL_DOWN_MASK != 0) {
      prefix += "C-"
    }
    if (modifiers and SHIFT_DOWN_MASK != 0) {
      prefix += "S-"
    }
    var name = getVimKeyValue(keyCode)
    if (name != null) {
      name = if (containsDisplayUppercaseKeyNames(name)) {
        name.uppercase(Locale.getDefault())
      } else {
        capitalize(name)
      }
    }
    if (name == null) {
      val escape = toEscapeNotation(keyStroke)
      if (escape != null) {
        return escape
      }
      try {
        name = String(Character.toChars(keyCode))
      } catch (_: IllegalArgumentException) {
      }
    }
    return if (name != null) "<$prefix$name>" else "<<$keyStroke>>"
  }

  override fun parseKeys(string: String): List<VimKeyStroke> = buildList {
    val specialKeyBuilder = StringBuilder()
    var state = KeyParserState.INIT

    for (c in string) {
      when (state) {
        KeyParserState.INIT -> when (c) {
          '\\' -> state = KeyParserState.ESCAPE
          '<' -> {
            state = KeyParserState.SPECIAL
            specialKeyBuilder.clear()
          }

          else -> {
            val stroke: VimKeyStroke = if (c == '\t' || c == '\n') {
              VimKeyStroke.getKeyStroke(c.code, 0)
            } else if (isControlCharacter(c)) {
              VimKeyStroke.getKeyStroke(c.code + 'A'.code - 1, CTRL_DOWN_MASK)
            } else {
              VimKeyStroke.getKeyStroke(c)
            }
            add(stroke)
          }
        }

        KeyParserState.ESCAPE -> {
          state = KeyParserState.INIT
          if (c != '\\') {
            add(VimKeyStroke.getKeyStroke('\\'))
          }
          add(VimKeyStroke.getKeyStroke(c))
        }

        KeyParserState.SPECIAL -> {
          if (c == '>') {
            state = KeyParserState.INIT
            val specialKeyName = specialKeyBuilder.toString()
            val lower = specialKeyName.lowercase(Locale.getDefault())
            require("sid" != lower) { "<$specialKeyName> is not supported" }

            if ("leader" == lower) {
              addAll(getMapLeader())
            } else if ("nop" != lower) {
              val specialKey = parseSpecialKey(specialKeyName, 0)
              if (specialKey != null && specialKeyName.length > 1) {
                add(specialKey)
              } else {
                add(VimKeyStroke.getKeyStroke('<'))
                addAll(stringToKeys(specialKeyName))
                add(VimKeyStroke.getKeyStroke('>'))
              }
            }
          } else {
            // e.g. move '<-2<CR> - the first part does not belong to any special key
            if (c == '<') {
              add(VimKeyStroke.getKeyStroke('<'))
              addAll(stringToKeys(specialKeyBuilder.toString()))
              specialKeyBuilder.clear()
            } else {
              specialKeyBuilder.append(c)
            }
          }
        }
      }
    }

    if (state == KeyParserState.ESCAPE) {
      add(VimKeyStroke.getKeyStroke('\\'))
    } else if (state == KeyParserState.SPECIAL) {
      add(VimKeyStroke.getKeyStroke('<'))
      addAll(stringToKeys(specialKeyBuilder.toString()))
    }
  }

  private fun getMapLeader(): List<VimKeyStroke> {
    val mapLeader: Any? = injector.variableService.getGlobalVariableValue("mapleader")
    return if (mapLeader is VimString) {
      stringToKeys(mapLeader.value)
    } else {
      stringToKeys("\\")
    }
  }

  override fun stringToKeys(string: @NonNls String): List<VimKeyStroke> {
    val res: MutableList<VimKeyStroke> = ArrayList()
    for (element in string) {
      if (isControlCharacter(element) && element.code != 10) {
        if (element.code == 0) {
          // J is a special case, it's keycode is 0 because keycode 10 is reserved by \n
          res.add(VimKeyStroke.getKeyStroke('J'.code, CTRL_DOWN_MASK))
        } else if (element == '\t') {
          res.add(VimKeyStroke.getKeyStroke('\t'))
        } else {
          res.add(VimKeyStroke.getKeyStroke(element.code + 'A'.code - 1, CTRL_DOWN_MASK))
        }
      } else {
        res.add(VimKeyStroke.getKeyStroke(element))
      }
    }
    return res
  }

  private fun isControlCharacter(c: Char): Boolean {
    return c < '\u0020'
  }

  @Suppress("SpellCheckingInspection")
  private fun getVimKeyValue(c: Int): @NonNls String? {
    return when (c) {
      VK_ENTER -> "cr"
      VK_INSERT -> "ins"
      VK_HOME -> "home"
      VK_END -> "end"
      VK_PAGE_UP -> "pageup"
      VK_PAGE_DOWN -> "pagedown"
      VK_DELETE -> "del"
      VK_ESCAPE -> "esc"
      VK_BACK_SPACE -> "bs"
      VK_TAB -> "tab"
      VK_UP -> "up"
      VK_DOWN -> "down"
      VK_LEFT -> "left"
      VK_RIGHT -> "right"
      VK_F1 -> "f1"
      VK_F2 -> "f2"
      VK_F3 -> "f3"
      VK_F4 -> "f4"
      VK_F5 -> "f5"
      VK_F6 -> "f6"
      VK_F7 -> "f7"
      VK_F8 -> "f8"
      VK_F9 -> "f9"
      VK_F10 -> "f10"
      VK_F11 -> "f11"
      VK_F12 -> "f12"
      VK_PLUG -> "plug"
      VK_ACTION -> "action"
      VK_NUMPAD0 -> "k0"
      VK_NUMPAD1 -> "k1"
      VK_NUMPAD2 -> "k2"
      VK_NUMPAD3 -> "k3"
      VK_NUMPAD4 -> "k4"
      VK_NUMPAD5 -> "k5"
      VK_NUMPAD6 -> "k6"
      VK_NUMPAD7 -> "k7"
      VK_NUMPAD8 -> "k8"
      VK_NUMPAD9 -> "k9"
      VK_KP_DOWN -> "kdown"
      VK_KP_UP -> "kup"
      VK_KP_LEFT -> "kleft"
      VK_KP_RIGHT -> "kright"
      VK_UNDO -> "undo"
      else -> null
    }
  }

  private fun containsDisplayUppercaseKeyNames(lower: String): Boolean {
    return "cr" == lower || "bs" == lower
  }

  @Contract(pure = true)
  private fun capitalize(s: String): String {
    if (s.isEmpty()) return s
    if (s.length == 1) return s.uppercase()
    return if (Character.isUpperCase(s[0])) s else s[0].uppercaseChar().toString() + s.substring(1)
  }

  private fun toEscapeNotation(key: VimKeyStroke): String? {
    val c = key.keyChar
    if (isControlCharacter(c)) {
      return "^" + (c.code + 'A'.code - 1).toChar()
    } else if (isControlKeyCode(key)) {
      return "^" + (key.keyCode + 'A'.code - 1).toChar()
    }
    return null
  }

  private fun isControlKeyCode(key: VimKeyStroke): Boolean {
    return key.keyChar == CHAR_UNDEFINED && key.keyCode < 0x20 && key.modifiers == 0
  }

  override fun parseVimScriptString(string: String): String {
    val result = StringBuilder()
    var state = VimStringState.INIT
    var specialKeyBuilder: StringBuilder? = null
    var digitsLeft = 0
    var number = 0
    val vimStringWithForceEnd = string + 0.toChar()
    var i = 0
    while (i < vimStringWithForceEnd.length) {
      val c = vimStringWithForceEnd[i]
      when (state) {
        VimStringState.INIT -> if (c == '\\') {
          state = VimStringState.ESCAPE
        } else if (c.code == 0) {
          i = vimStringWithForceEnd.length
        } else {
          result.append(c)
        }

        VimStringState.ESCAPE -> {
          val octalToDigital = octalDigitToNumber(c)
          if (octalToDigital != null) {
            number = octalToDigital
            digitsLeft = 2
            state = VimStringState.OCTAL_NUMBER
          } else if (Character.toLowerCase(c) == 'x') {
            digitsLeft = 2
            state = VimStringState.HEX_NUMBER
          } else if (c == 'u') {
            digitsLeft = 4
            state = VimStringState.HEX_NUMBER
          } else if (c == 'U') {
            digitsLeft = 8
            state = VimStringState.HEX_NUMBER
          } else if (c == 'b') {
            result.append(8.toChar())
            state = VimStringState.INIT
          } else if (c == 'e') {
            result.append(27.toChar())
            state = VimStringState.INIT
          } else if (c == 'f') {
            result.append(12.toChar())
            state = VimStringState.INIT
          } else if (c == 'n') {
            result.append('\n')
            state = VimStringState.INIT
          } else if (c == 'r') {
            result.append('\r')
            state = VimStringState.INIT
          } else if (c == 't') {
            result.append('\t')
            state = VimStringState.INIT
          } else if (c == '\\') {
            result.append('\\')
            state = VimStringState.INIT
          } else if (c == '"') {
            result.append('"')
            state = VimStringState.INIT
          } else if (c == '<') {
            state = VimStringState.SPECIAL
            specialKeyBuilder = StringBuilder()
          } else if (c.code == 0) {
            i = vimStringWithForceEnd.length // force end of the string
          } else {
            result.append(c)
            state = VimStringState.INIT
          }
        }

        VimStringState.OCTAL_NUMBER -> {
          val value = octalDigitToNumber(c)
          if (value != null) {
            digitsLeft -= 1
            number = number * 8 + value
            if (digitsLeft == 0 || i == vimStringWithForceEnd.length - 1) {
              if (number != 0) {
                result.append(number.toChar())
              } else {
                i = vimStringWithForceEnd.length
              }
              number = 0
              state = VimStringState.INIT
            }
          } else {
            if (number != 0) {
              result.append(number.toChar())
            } else {
              i = vimStringWithForceEnd.length
            }
            number = 0
            digitsLeft = 0
            state = VimStringState.INIT
            i -= 1
          }
        }

        VimStringState.HEX_NUMBER -> {
          val `val` = hexDigitToNumber(c)
          if (`val` == null) {
            // if there was at least one number after '\', append number, otherwise - append letter after '\'
            if (vimStringWithForceEnd[i - 2] == '\\') {
              result.append(vimStringWithForceEnd[i - 1])
            } else {
              if (number != 0) {
                result.append(number.toChar())
              } else {
                i = vimStringWithForceEnd.length
              }
            }
            number = 0
            digitsLeft = 0
            state = VimStringState.INIT
            i -= 1
          } else {
            number = number * 16 + `val`
            digitsLeft -= 1
            if (digitsLeft == 0 || i == vimStringWithForceEnd.length - 1) {
              if (number != 0) {
                result.append(number.toChar())
              } else {
                i = vimStringWithForceEnd.length
              }
              number = 0
              state = VimStringState.INIT
            }
          }
        }

        VimStringState.SPECIAL -> {
          if (c.code == 0) {
            result.append(specialKeyBuilder)
          }
          if (c == '>') {
            val specialKey = parseSpecialKey(specialKeyBuilder.toString(), 0)
            if (specialKey != null) {
              var keyCode = specialKey.keyCode
              if (specialKey.keyCode == 0) {
                keyCode = specialKey.keyChar.code
              } else if (specialKey.modifiers and CTRL_DOWN_MASK == CTRL_DOWN_MASK) {
                keyCode = if (specialKey.keyCode == 'J'.code) {
                  // 'J' is a special case, keycode 10 is \n char
                  0
                } else {
                  specialKey.keyCode - 'A'.code + 1
                }
              }
              result.append(keyCode.toChar())
            } else {
              result.append("<").append(specialKeyBuilder).append(">")
            }
            specialKeyBuilder = StringBuilder()
            state = VimStringState.INIT
          } else if (c.code == 0) {
            result.append("<").append(specialKeyBuilder)
            state = VimStringState.INIT
          } else {
            specialKeyBuilder!!.append(c)
          }
        }
      }
      i += 1
    }
    return result.toString()
  }

  private enum class VimStringState {
    INIT, ESCAPE, OCTAL_NUMBER, HEX_NUMBER, SPECIAL
  }

  private fun octalDigitToNumber(c: Char): Int? {
    return if (c in '0'..'7') {
      c.code - '0'.code
    } else {
      null
    }
  }

  private fun hexDigitToNumber(c: Char): Int? {
    val lowerChar = Character.toLowerCase(c)
    if (Character.isDigit(lowerChar)) {
      return lowerChar.code - '0'.code
    } else if (lowerChar in 'a'..'f') {
      return lowerChar.code - 'a'.code + 10
    }
    return null
  }

  // See https://vimdoc.sourceforge.net/htmldoc/intro.html#key-notation
  private fun parseSpecialKey(s: String, modifiers: Int): VimKeyStroke? {
    val lower = s.lowercase(Locale.getDefault())
    val keyCode = getVimKeyName(lower)
    val typedChar = getVimTypedKeyName(lower)
    if (keyCode != null) {
      return VimKeyStroke.getKeyStroke(keyCode, modifiers)
    } else if (typedChar != null) {
      return getTypedOrPressedKeyStroke(typedChar, modifiers)
    } else if (lower.startsWith(CMD_PREFIX)) {
      return parseSpecialKey(s.substring(CMD_PREFIX.length), modifiers or META_DOWN_MASK)
    } else if (lower.startsWith(META_PREFIX)) {
      // Meta and alt prefixes are the same thing. See the key notation of vim
      return parseSpecialKey(s.substring(META_PREFIX.length), modifiers or ALT_DOWN_MASK)
    } else if (lower.startsWith(ALT_PREFIX)) {
      return parseSpecialKey(s.substring(ALT_PREFIX.length), modifiers or ALT_DOWN_MASK)
    } else if (lower.startsWith(CTRL_PREFIX)) {
      return parseSpecialKey(s.substring(CTRL_PREFIX.length), modifiers or CTRL_DOWN_MASK)
    } else if (lower.startsWith(SHIFT_PREFIX)) {
      return parseSpecialKey(s.substring(SHIFT_PREFIX.length), modifiers or SHIFT_DOWN_MASK)
    } else if (s.length == 1) {
      return getTypedOrPressedKeyStroke(s[0], modifiers)
    }
    return null
  }

  @Suppress("SpellCheckingInspection")
  private fun getVimKeyName(lower: @NonNls String?): Int? {
    return when (lower) {
      "cr", "enter", "return" -> VK_ENTER
      "ins", "insert" -> VK_INSERT
      "home" -> VK_HOME
      "end" -> VK_END
      "pageup" -> VK_PAGE_UP
      "pagedown" -> VK_PAGE_DOWN
      "del", "delete" -> VK_DELETE
      "esc" -> VK_ESCAPE
      "bs", "backspace" -> VK_BACK_SPACE
      "tab" -> VK_TAB
      "up" -> VK_UP
      "down" -> VK_DOWN
      "left" -> VK_LEFT
      "right" -> VK_RIGHT
      "f1" -> VK_F1
      "f2" -> VK_F2
      "f3" -> VK_F3
      "f4" -> VK_F4
      "f5" -> VK_F5
      "f6" -> VK_F6
      "f7" -> VK_F7
      "f8" -> VK_F8
      "f9" -> VK_F9
      "f10" -> VK_F10
      "f11" -> VK_F11
      "f12" -> VK_F12
      "plug" -> VK_PLUG
      "action" -> VK_ACTION
      "k0" -> VK_NUMPAD0
      "k1" -> VK_NUMPAD1
      "k2" -> VK_NUMPAD2
      "k3" -> VK_NUMPAD3
      "k4" -> VK_NUMPAD4
      "k5" -> VK_NUMPAD5
      "k6" -> VK_NUMPAD6
      "k7" -> VK_NUMPAD7
      "k8" -> VK_NUMPAD8
      "k9" -> VK_NUMPAD9
      "khome" -> VK_HOME
      "kend" -> VK_END
      "kdown" -> VK_KP_DOWN
      "kup" -> VK_KP_UP
      "kleft" -> VK_KP_LEFT
      "kright" -> VK_KP_RIGHT
      "undo" -> VK_UNDO
      else -> null
    }
  }

  @Suppress("SpellCheckingInspection")
  private fun getVimTypedKeyName(lower: String): Char? {
    return when (lower) {
      "space" -> ' '
      "bar" -> '|'
      "bslash" -> '\\'
      "lt" -> '<'
      else -> null
    }
  }

  private fun getTypedOrPressedKeyStroke(c: Char, modifiers: Int): VimKeyStroke {
    return if (modifiers == 0) {
      VimKeyStroke.getKeyStroke(c)
    } else if (modifiers == SHIFT_DOWN_MASK && Character.isLetter(c)) {
      VimKeyStroke.getKeyStroke(Character.toUpperCase(c))
    } else {
      VimKeyStroke.getKeyStroke(Character.toUpperCase(c).code, modifiers)
    }
  }

  private enum class KeyParserState {
    INIT, ESCAPE, SPECIAL
  }

  private companion object {
    private const val CMD_PREFIX = "d-"
    private const val META_PREFIX = "m-"
    private const val ALT_PREFIX = "a-"
    private const val CTRL_PREFIX = "c-"
    private const val SHIFT_PREFIX = "s-"
    private const val VK_PLUG = CHAR_UNDEFINED.code - 1
    private const val VK_ACTION = CHAR_UNDEFINED.code - 2
  }
}
