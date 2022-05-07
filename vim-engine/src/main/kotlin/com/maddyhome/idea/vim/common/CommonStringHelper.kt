package com.maddyhome.idea.vim.common

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.KeyStroke

object CommonStringHelper {
  private const val META_PREFIX = "m-"
  private const val ALT_PREFIX = "a-"
  private const val CTRL_PREFIX = "c-"
  private const val SHIFT_PREFIX = "s-"

  private const val VK_PLUG = KeyEvent.CHAR_UNDEFINED.code - 1
  const val VK_ACTION = KeyEvent.CHAR_UNDEFINED.code - 2

  fun toKeyNotation(keys: List<KeyStroke>): String {
    if (keys.isEmpty()) {
      return "<Nop>"
    }
    val builder = StringBuilder()
    for (key in keys) {
      builder.append(toKeyNotation(key))
    }
    return builder.toString()
  }

  fun toKeyNotation(key: KeyStroke): String {
    val c = key.keyChar
    val keyCode = key.keyCode
    val modifiers = key.modifiers
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
      val escape = toEscapeNotation(key)
      if (escape != null) {
        return escape
      }
      try {
        name = String(Character.toChars(keyCode))
      } catch (ignored: IllegalArgumentException) {
      }
    }
    return if (name != null) "<$prefix$name>" else "<<$key>>"
  }

  fun parseKeys(vararg strings: String): List<KeyStroke> {
    val result: MutableList<KeyStroke> = ArrayList()
    var specialKeyStart = '<'
    for (s in strings) {
      var state = KeyParserState.INIT
      var specialKeyBuilder = StringBuilder()
      for (element in s) {
        when (state) {
          KeyParserState.INIT -> when (element) {
            '\\' -> state = KeyParserState.ESCAPE
            '<', '«' -> {
              specialKeyStart = element
              state = KeyParserState.SPECIAL
              specialKeyBuilder = StringBuilder()
            }
            else -> {
              val stroke: KeyStroke = if (element == '\t' || element == '\n') {
                KeyStroke.getKeyStroke(element.code, 0)
              } else if (isControlCharacter(element)) {
                KeyStroke.getKeyStroke(element.code + 'A'.code - 1, InputEvent.CTRL_DOWN_MASK)
              } else {
                KeyStroke.getKeyStroke(element)
              }
              result.add(stroke)
            }
          }
          KeyParserState.ESCAPE -> {
            state = KeyParserState.INIT
            if (element != '\\') {
              result.add(KeyStroke.getKeyStroke('\\'))
            }
            result.add(KeyStroke.getKeyStroke(element))
          }
          KeyParserState.SPECIAL -> if (element == '>' || element == '»') {
            state = KeyParserState.INIT
            val specialKeyName = specialKeyBuilder.toString()
            val lower = specialKeyName.lowercase(Locale.getDefault())
            require("sid" != lower) { "<$specialKeyName> is not supported" }
            if ("comma" == lower) {
              result.add(KeyStroke.getKeyStroke(','))
            } else if ("nop" != lower) {
              val leader = parseMapLeader(specialKeyName)
              val specialKey = parseSpecialKey(specialKeyName, 0)
              if (leader != null) {
                result.addAll(leader)
              } else if (specialKey != null && specialKeyName.length > 1) {
                result.add(specialKey)
              } else {
                result.add(KeyStroke.getKeyStroke('<'))
                result.addAll(stringToKeys(specialKeyName))
                result.add(KeyStroke.getKeyStroke('>'))
              }
            }
          } else {
            // e.g. move '<-2<CR> - the first part does not belong to any special key
            if (element == '<' || element == '«') {
              result.add(KeyStroke.getKeyStroke(specialKeyStart))
              result.addAll(stringToKeys(specialKeyBuilder.toString()))
              specialKeyBuilder = StringBuilder()
            } else {
              specialKeyBuilder.append(element)
            }
          }
        }
      }
      if (state == KeyParserState.ESCAPE) {
        result.add(KeyStroke.getKeyStroke('\\'))
      } else if (state == KeyParserState.SPECIAL) {
        result.add(KeyStroke.getKeyStroke(specialKeyStart))
        result.addAll(stringToKeys(specialKeyBuilder.toString()))
      }
    }
    return result
  }

  private fun parseMapLeader(s: String): List<KeyStroke>? {
    if ("leader".equals(s, ignoreCase = true)) {
      val mapLeader: Any? = injector.variableService.getGlobalVariableValue("mapleader")
      return if (mapLeader is VimString) {
        stringToKeys(mapLeader.value)
      } else {
        stringToKeys("\\")
      }
    }
    return null
  }

  fun stringToKeys(s: @NonNls String): List<KeyStroke> {
    val res: MutableList<KeyStroke> = ArrayList()
    for (element in s) {
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
      VK_PLUG -> "plug"
      VK_ACTION -> "action"
      else -> null
    }
  }

  private fun containsDisplayUppercaseKeyNames(lower: String): Boolean {
    return "cr" == lower || "bs" == lower
  }

  @Contract(pure = true)
  fun capitalize(s: String): String {
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

  /**
   * See ":h string"
   * \...   three-digit octal number
   * \..    two-digit octal number (must be followed by non-digit)
   * \.     one-digit octal number (must be followed by non-digit)
   * \x..   byte specified with two hex numbers (e.g., "\x1f")
   * \x.    byte specified with one hex number (must be followed by non-hex char)
   * \X..   same as \x..
   * \X.    same as \x.
   * \\u.... character specified with up to 4 hex numbers, stored as UTF-8
   * \U.... same as \\u but allows up to 8 hex numbers
   * \b     backspace
   * \e     escape
   * \f     formfeed
   * \n     newline
   * \r     return
   * \t     tab
   * \\     backslash
   * \"     double quote
   * \<xxx> special key named "xxx". e.g. "\<C-W>" for CTRL-W. This is for use in mapping, the 0x80 byte is escaped
   * to use double quote character is must be escaped:  "<M-></M->\">".
   *
   *
   * Note that "\000" and "\x00" force end of the string (same for \\u, \U etc)
  </C-W></xxx> */
  fun parseVimString(vimString: String): String {
    val result = StringBuilder()
    var state = VimStringState.INIT
    var specialKeyBuilder: StringBuilder? = null
    var digitsLeft = 0
    var number = 0
    val vimStringWithForceEnd = vimString + 0.toChar()
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
              } else if (specialKey.modifiers and InputEvent.CTRL_DOWN_MASK == InputEvent.CTRL_DOWN_MASK) {
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

  private enum class KeyParserState {
    INIT, ESCAPE, SPECIAL
  }

  private enum class VimStringState {
    INIT, ESCAPE, OCTAL_NUMBER, HEX_NUMBER, SPECIAL
  }

  private fun octalDigitToNumber(c: Char): Int? {
    return if (c in '0'..'7') {
      c.code - '0'.code
    } else null
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

  private fun parseSpecialKey(s: String, modifiers: Int): KeyStroke? {
    val lower = s.lowercase(Locale.getDefault())
    val keyCode = getVimKeyName(lower)
    val typedChar = getVimTypedKeyName(lower)
    if (keyCode != null) {
      return KeyStroke.getKeyStroke(keyCode, modifiers)
    } else if (typedChar != null) {
      return getTypedOrPressedKeyStroke(typedChar, modifiers)
    } else if (lower.startsWith(META_PREFIX)) {
      return parseSpecialKey(s.substring(META_PREFIX.length), modifiers or InputEvent.META_DOWN_MASK)
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
      "plug" -> VK_PLUG
      "action" -> VK_ACTION
      else -> null
    }
  }

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
}
