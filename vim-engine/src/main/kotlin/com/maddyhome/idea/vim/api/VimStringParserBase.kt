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
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

abstract class VimStringParserBase : VimStringParser {
  override val plugKeyStroke: KeyStroke
    get() = parseKeys("<Plug>")[0]

  override val actionKeyStroke: KeyStroke
    get() = parseKeys("<Action>")[0]

  // todo what is the difference between this one and com.maddyhome.idea.vim.helper.EngineStringHelper#toPrintableCharacters
  override fun toPrintableString(keys: List<KeyStroke>): String {
    val builder = StringBuilder()
    for (key in keys) {
      val keyAsString = keyStrokeToString(key)
      builder.append(keyAsString)
    }
    return builder.toString()
  }

  private fun keyStrokeToString(key: KeyStroke): String {
    if (key.keyChar != KeyEvent.CHAR_UNDEFINED) {
      return key.keyChar.toString()
    } else if (key.modifiers and InputEvent.CTRL_DOWN_MASK == InputEvent.CTRL_DOWN_MASK) {
      return if (isControlCharacterKeyCode(key.keyCode)) {
        if (key.keyCode == 'J'.code) {
          // 'J' is a special case, keycode 10 is \n char
          0.toChar().toString()
        } else {
          (key.keyCode - 'A'.code + 1).toChar().toString()
        }
      } else {
        "^" + key.keyCode.toChar()
      }
    } else if (key.keyChar == KeyEvent.CHAR_UNDEFINED && key.keyCode == KeyEvent.VK_ENTER) {
      return "\u000D"
    }
    return key.keyCode.toChar().toString()
  }

  override fun toKeyNotation(keyStrokes: List<KeyStroke>): String {
    if (keyStrokes.isEmpty()) {
      return "<Nop>"
    }
    val builder = StringBuilder()
    for (key in keyStrokes) {
      builder.append(toKeyNotation(key))
    }
    return builder.toString()
  }

  override fun toKeyNotation(keyStroke: KeyStroke): String {
    val c = keyStroke.keyChar
    val keyCode = keyStroke.keyCode
    val modifiers = keyStroke.modifiers
    if (c != KeyEvent.CHAR_UNDEFINED && !isControlCharacter(c)) {
      return c.toString()
    }
    var prefix = ""
    if (modifiers and InputEvent.META_DOWN_MASK != 0) {
      prefix += "M-"
    }
    if (modifiers and InputEvent.ALT_DOWN_MASK != 0) {
      prefix += "A-"
    }
    if (modifiers and InputEvent.CTRL_DOWN_MASK != 0) {
      prefix += "C-"
    }
    if (modifiers and InputEvent.SHIFT_DOWN_MASK != 0) {
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

  override fun parseKeys(string: String): List<KeyStroke> = buildList {
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
            val stroke: KeyStroke = if (c == '\t' || c == '\n') {
              KeyStroke.getKeyStroke(c.code, 0)
            } else if (isControlCharacter(c)) {
              KeyStroke.getKeyStroke(c.code + 'A'.code - 1, InputEvent.CTRL_DOWN_MASK)
            } else {
              KeyStroke.getKeyStroke(c)
            }
            add(stroke)
          }
        }

        KeyParserState.ESCAPE -> {
          state = KeyParserState.INIT
          if (c != '\\') {
            add(KeyStroke.getKeyStroke('\\'))
          }
          add(KeyStroke.getKeyStroke(c))
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
                add(KeyStroke.getKeyStroke('<'))
                addAll(stringToKeys(specialKeyName))
                add(KeyStroke.getKeyStroke('>'))
              }
            }
          } else {
            // e.g. move '<-2<CR> - the first part does not belong to any special key
            if (c == '<') {
              add(KeyStroke.getKeyStroke('<'))
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
      add(KeyStroke.getKeyStroke('\\'))
    } else if (state == KeyParserState.SPECIAL) {
      add(KeyStroke.getKeyStroke('<'))
      addAll(stringToKeys(specialKeyBuilder.toString()))
    }
  }

  private fun getMapLeader(): List<KeyStroke> {
    val mapLeader: Any? = injector.variableService.getGlobalVariableValue("mapleader")
    return if (mapLeader is VimString) {
      val v: String = mapLeader.value
      if (v[0] == '<' && v[v.length - 1] == '>') {
        listOf(parseSpecialKey(v.substring(1, v.length - 1), 0)!!)
      } else {
        stringToKeys(mapLeader.value)
      }
    } else {
      stringToKeys("\\")
    }
  }

  override fun stringToKeys(string: @NonNls String): List<KeyStroke> {
    val res: MutableList<KeyStroke> = ArrayList()
    for (element in string) {
      if (isControlCharacter(element) && element.code != 10) {
        if (element.code == 0) {
          // J is a special case, it's keycode is 0 because keycode 10 is reserved by \n
          res.add(KeyStroke.getKeyStroke('J'.code, InputEvent.CTRL_DOWN_MASK))
        } else if (element == '\t') {
          res.add(KeyStroke.getKeyStroke('\t'))
        } else {
          res.add(KeyStroke.getKeyStroke(element.code + 'A'.code - 1, InputEvent.CTRL_DOWN_MASK))
        }
      } else {
        res.add(KeyStroke.getKeyStroke(element))
      }
    }
    return res
  }

  private fun isControlCharacter(c: Char): Boolean {
    return c < '\u0020'
  }

  private fun isControlCharacterKeyCode(code: Int): Boolean {
    // Ctrl-(A..Z [\]^_) are ASCII control characters
    return code >= 'A'.code && code <= '_'.code
  }

  @Suppress("SpellCheckingInspection")
  private fun getVimKeyValue(c: Int): @NonNls String? {
    return when (c) {
      KeyEvent.VK_ENTER -> "cr"
      KeyEvent.VK_INSERT -> "ins"
      KeyEvent.VK_HOME -> "home"
      KeyEvent.VK_END -> "end"
      KeyEvent.VK_PAGE_UP -> "pageup"
      KeyEvent.VK_PAGE_DOWN -> "pagedown"
      KeyEvent.VK_DELETE -> "del"
      KeyEvent.VK_ESCAPE -> "esc"
      KeyEvent.VK_BACK_SPACE -> "bs"
      KeyEvent.VK_TAB -> "tab"
      KeyEvent.VK_UP -> "up"
      KeyEvent.VK_DOWN -> "down"
      KeyEvent.VK_LEFT -> "left"
      KeyEvent.VK_RIGHT -> "right"
      KeyEvent.VK_F1 -> "f1"
      KeyEvent.VK_F2 -> "f2"
      KeyEvent.VK_F3 -> "f3"
      KeyEvent.VK_F4 -> "f4"
      KeyEvent.VK_F5 -> "f5"
      KeyEvent.VK_F6 -> "f6"
      KeyEvent.VK_F7 -> "f7"
      KeyEvent.VK_F8 -> "f8"
      KeyEvent.VK_F9 -> "f9"
      KeyEvent.VK_F10 -> "f10"
      KeyEvent.VK_F11 -> "f11"
      KeyEvent.VK_F12 -> "f12"
      KeyEvent.VK_F13 -> "f13"
      KeyEvent.VK_F14 -> "f14"
      KeyEvent.VK_F15 -> "f15"
      KeyEvent.VK_F16 -> "f16"
      KeyEvent.VK_F17 -> "f17"
      KeyEvent.VK_F18 -> "f18"
      KeyEvent.VK_F19 -> "f19"
      KeyEvent.VK_F20 -> "f20"
      KeyEvent.VK_F21 -> "f21"
      KeyEvent.VK_F22 -> "f22"
      KeyEvent.VK_F23 -> "f23"
      KeyEvent.VK_F24 -> "f24"
      VK_PLUG -> "plug"
      VK_ACTION -> "action"
      KeyEvent.VK_NUMPAD0 -> "k0"
      KeyEvent.VK_NUMPAD1 -> "k1"
      KeyEvent.VK_NUMPAD2 -> "k2"
      KeyEvent.VK_NUMPAD3 -> "k3"
      KeyEvent.VK_NUMPAD4 -> "k4"
      KeyEvent.VK_NUMPAD5 -> "k5"
      KeyEvent.VK_NUMPAD6 -> "k6"
      KeyEvent.VK_NUMPAD7 -> "k7"
      KeyEvent.VK_NUMPAD8 -> "k8"
      KeyEvent.VK_NUMPAD9 -> "k9"
      KeyEvent.VK_KP_DOWN -> "kdown"
      KeyEvent.VK_KP_UP -> "kup"
      KeyEvent.VK_KP_LEFT -> "kleft"
      KeyEvent.VK_KP_RIGHT -> "kright"
      KeyEvent.VK_UNDO -> "undo"
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

  private fun toEscapeNotation(key: KeyStroke): String? {
    val c = key.keyChar
    if (isControlCharacter(c)) {
      return "^" + (c.code + 'A'.code - 1).toChar()
    } else if (isControlKeyCode(key)) {
      return "^" + (key.keyCode + 'A'.code - 1).toChar()
    }
    return null
  }

  private fun isControlKeyCode(key: KeyStroke): Boolean {
    return key.keyChar == KeyEvent.CHAR_UNDEFINED && key.keyCode < 0x20 && key.modifiers == 0
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
            var keyCode: Int? = null
            if (specialKey != null) {
              if (specialKey.keyCode == 0) {
                keyCode = specialKey.keyChar.code
              } else if (specialKey.modifiers and InputEvent.CTRL_DOWN_MASK == InputEvent.CTRL_DOWN_MASK) {
                if (isControlCharacterKeyCode(specialKey.keyCode)) {
                  keyCode = if (specialKey.keyCode == 'J'.code) {
                    // 'J' is a special case, keycode 10 is \n char
                    0
                  } else {
                    specialKey.keyCode - 'A'.code + 1
                  }
                }
              } else {
                keyCode = specialKey.keyCode
              }
            }
            if (keyCode != null) {
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
  private fun parseSpecialKey(s: String, modifiers: Int): KeyStroke? {
    val lower = s.lowercase(Locale.getDefault())
    val keyCode = getVimKeyName(lower)
    val typedChar = getVimTypedKeyName(lower)
    if (keyCode != null) {
      return KeyStroke.getKeyStroke(keyCode, modifiers)
    } else if (typedChar != null) {
      return getTypedOrPressedKeyStroke(typedChar, modifiers)
    } else if (lower.startsWith(CMD_PREFIX)) {
      return parseSpecialKey(s.substring(CMD_PREFIX.length), modifiers or InputEvent.META_DOWN_MASK)
    } else if (lower.startsWith(META_PREFIX)) {
      // Meta and alt prefixes are the same thing. See the key notation of vim
      return parseSpecialKey(s.substring(META_PREFIX.length), modifiers or InputEvent.ALT_DOWN_MASK)
    } else if (lower.startsWith(ALT_PREFIX)) {
      return parseSpecialKey(s.substring(ALT_PREFIX.length), modifiers or InputEvent.ALT_DOWN_MASK)
    } else if (lower.startsWith(CTRL_PREFIX)) {
      return parseSpecialKey(s.substring(CTRL_PREFIX.length), modifiers or InputEvent.CTRL_DOWN_MASK)
    } else if (lower.startsWith(SHIFT_PREFIX)) {
      return parseSpecialKey(s.substring(SHIFT_PREFIX.length), modifiers or InputEvent.SHIFT_DOWN_MASK)
    } else if (s.length == 1) {
      return getTypedOrPressedKeyStroke(s[0], modifiers)
    }
    return null
  }

  @Suppress("SpellCheckingInspection")
  private fun getVimKeyName(lower: @NonNls String?): Int? {
    return when (lower) {
      "cr", "enter", "return" -> KeyEvent.VK_ENTER
      "ins", "insert" -> KeyEvent.VK_INSERT
      "home" -> KeyEvent.VK_HOME
      "end" -> KeyEvent.VK_END
      "pageup" -> KeyEvent.VK_PAGE_UP
      "pagedown" -> KeyEvent.VK_PAGE_DOWN
      "del", "delete" -> KeyEvent.VK_DELETE
      "esc" -> KeyEvent.VK_ESCAPE
      "bs", "backspace" -> KeyEvent.VK_BACK_SPACE
      "tab" -> KeyEvent.VK_TAB
      "up" -> KeyEvent.VK_UP
      "down" -> KeyEvent.VK_DOWN
      "left" -> KeyEvent.VK_LEFT
      "right" -> KeyEvent.VK_RIGHT
      "f1" -> KeyEvent.VK_F1
      "f2" -> KeyEvent.VK_F2
      "f3" -> KeyEvent.VK_F3
      "f4" -> KeyEvent.VK_F4
      "f5" -> KeyEvent.VK_F5
      "f6" -> KeyEvent.VK_F6
      "f7" -> KeyEvent.VK_F7
      "f8" -> KeyEvent.VK_F8
      "f9" -> KeyEvent.VK_F9
      "f10" -> KeyEvent.VK_F10
      "f11" -> KeyEvent.VK_F11
      "f12" -> KeyEvent.VK_F12
      "f13" -> KeyEvent.VK_F13
      "f14" -> KeyEvent.VK_F14
      "f15" -> KeyEvent.VK_F15
      "f16" -> KeyEvent.VK_F16
      "f17" -> KeyEvent.VK_F17
      "f18" -> KeyEvent.VK_F18
      "f19" -> KeyEvent.VK_F19
      "f20" -> KeyEvent.VK_F20
      "f21" -> KeyEvent.VK_F21
      "f22" -> KeyEvent.VK_F22
      "f23" -> KeyEvent.VK_F23
      "f24" -> KeyEvent.VK_F24
      "plug" -> VK_PLUG
      "action" -> VK_ACTION
      "k0" -> KeyEvent.VK_NUMPAD0
      "k1" -> KeyEvent.VK_NUMPAD1
      "k2" -> KeyEvent.VK_NUMPAD2
      "k3" -> KeyEvent.VK_NUMPAD3
      "k4" -> KeyEvent.VK_NUMPAD4
      "k5" -> KeyEvent.VK_NUMPAD5
      "k6" -> KeyEvent.VK_NUMPAD6
      "k7" -> KeyEvent.VK_NUMPAD7
      "k8" -> KeyEvent.VK_NUMPAD8
      "k9" -> KeyEvent.VK_NUMPAD9
      "khome" -> KeyEvent.VK_HOME
      "kend" -> KeyEvent.VK_END
      "kdown" -> KeyEvent.VK_KP_DOWN
      "kup" -> KeyEvent.VK_KP_UP
      "kleft" -> KeyEvent.VK_KP_LEFT
      "kright" -> KeyEvent.VK_KP_RIGHT
      "undo" -> KeyEvent.VK_UNDO
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

  private fun getTypedOrPressedKeyStroke(c: Char, modifiers: Int): KeyStroke {
    return if (modifiers == 0) {
      KeyStroke.getKeyStroke(c)
    } else if (modifiers == InputEvent.SHIFT_DOWN_MASK && Character.isLetter(c)) {
      KeyStroke.getKeyStroke(Character.toUpperCase(c))
    } else {
      KeyStroke.getKeyStroke(Character.toUpperCase(c).code, modifiers)
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
    private const val VK_PLUG = KeyEvent.CHAR_UNDEFINED.code - 1
    private const val VK_ACTION = KeyEvent.CHAR_UNDEFINED.code - 2
  }
}
