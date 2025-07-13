/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.CHAR_UNDEFINED
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.NO_MODIFIERS
import com.maddyhome.idea.vim.key.VimKeyStroke.Constants.VK_UNDEFINED

data class VimKeyStroke(val keyChar: Char, val keyCode: Int, val modifiers: Int) {
  companion object {

    fun getKeyStroke(c: Char): VimKeyStroke {
      return VimKeyStroke(c, VK_UNDEFINED, NO_MODIFIERS)
    }

    fun getKeyStroke(keyChar: Char, modifiers: Int): VimKeyStroke {
      return VimKeyStroke(keyChar, VK_UNDEFINED, computeModifiers(modifiers))
    }

    fun getKeyStroke(keycode: Int, modifiers: Int): VimKeyStroke {
      return VimKeyStroke(CHAR_UNDEFINED, keycode, computeModifiers(modifiers))
    }

    private fun computeModifiers(modifiers: Int): Int {
      var mod = modifiers

      if (modifiers and Constants.CTRL_DOWN_MASK != 0) {
        mod = mod or Constants.CTRL_MASK
      }

      if (modifiers and Constants.SHIFT_DOWN_MASK != 0) {
        mod = mod or Constants.SHIFT_MASK
      }

      if (modifiers and Constants.META_DOWN_MASK != 0) {
        mod = mod or Constants.META_MASK
      }

      if (modifiers and Constants.ALT_DOWN_MASK != 0) {
        mod = mod or Constants.ALT_MASK
      }

      if (modifiers and Constants.ALT_GRAPH_DOWN_MASK != 0) {
        mod = mod or Constants.ALT_GRAPH_MASK
      }

      return mod
    }

  }

  object Constants {
    const val CHAR_UNDEFINED = 0xFFFF.toChar()
    const val NO_MODIFIERS = 0x0

    const val VK_ENTER          = '\n'.code
    const val VK_BACK_SPACE     = '\b'.code
    const val VK_TAB            = '\t'.code
    const val VK_CANCEL         = 0x03
    const val VK_CLEAR          = 0x0C
    const val VK_SHIFT          = 0x10
    const val VK_CONTROL        = 0x11
    const val VK_ALT            = 0x12
    const val VK_PAUSE          = 0x13
    const val VK_CAPS_LOCK      = 0x14
    const val VK_ESCAPE         = 0x1B
    const val VK_SPACE          = 0x20
    const val VK_PAGE_UP        = 0x21
    const val VK_PAGE_DOWN      = 0x22
    const val VK_END            = 0x23
    const val VK_HOME           = 0x24
    const val VK_LEFT           = 0x25
    const val VK_UP             = 0x26
    const val VK_RIGHT          = 0x27
    const val VK_DOWN           = 0x28
    const val VK_COMMA          = 0x2C
    const val VK_MINUS          = 0x2D
    const val VK_PERIOD         = 0x2E
    const val VK_SLASH          = 0x2F
    const val VK_0              = 0x30
    const val VK_1              = 0x31
    const val VK_2              = 0x32
    const val VK_3              = 0x33
    const val VK_4              = 0x34
    const val VK_5              = 0x35
    const val VK_6              = 0x36
    const val VK_7              = 0x37
    const val VK_8              = 0x38
    const val VK_9              = 0x39
    const val VK_SEMICOLON      = 0x3B
    const val VK_EQUALS         = 0x3D
    const val VK_A              = 0x41
    const val VK_B              = 0x42
    const val VK_C              = 0x43
    const val VK_D              = 0x44
    const val VK_E              = 0x45
    const val VK_F              = 0x46
    const val VK_G              = 0x47
    const val VK_H              = 0x48
    const val VK_I              = 0x49
    const val VK_J              = 0x4A
    const val VK_K              = 0x4B
    const val VK_L              = 0x4C
    const val VK_M              = 0x4D
    const val VK_N              = 0x4E
    const val VK_O              = 0x4F
    const val VK_P              = 0x50
    const val VK_Q              = 0x51
    const val VK_R              = 0x52
    const val VK_S              = 0x53
    const val VK_T              = 0x54
    const val VK_U              = 0x55
    const val VK_V              = 0x56
    const val VK_W              = 0x57
    const val VK_X              = 0x58
    const val VK_Y              = 0x59
    const val VK_Z              = 0x5A
    const val VK_OPEN_BRACKET   = 0x5B
    const val VK_BACK_SLASH     = 0x5C
    const val VK_CLOSE_BRACKET  = 0x5D
    const val VK_NUMPAD0        = 0x60
    const val VK_NUMPAD1        = 0x61
    const val VK_NUMPAD2        = 0x62
    const val VK_NUMPAD3        = 0x63
    const val VK_NUMPAD4        = 0x64
    const val VK_NUMPAD5        = 0x65
    const val VK_NUMPAD6        = 0x66
    const val VK_NUMPAD7        = 0x67
    const val VK_NUMPAD8        = 0x68
    const val VK_NUMPAD9        = 0x69
    const val VK_MULTIPLY       = 0x6A
    const val VK_ADD            = 0x6B
    const val VK_SEPARATER      = 0x6C
    const val VK_SEPARATOR      = VK_SEPARATER
    const val VK_SUBTRACT       = 0x6D
    const val VK_DECIMAL        = 0x6E
    const val VK_DIVIDE         = 0x6F
    const val VK_DELETE         = 0x7F
    const val VK_NUM_LOCK       = 0x90
    const val VK_SCROLL_LOCK    = 0x91
    const val VK_F1             = 0x70
    const val VK_F2             = 0x71
    const val VK_F3             = 0x72
    const val VK_F4             = 0x73
    const val VK_F5             = 0x74
    const val VK_F6             = 0x75
    const val VK_F7             = 0x76
    const val VK_F8             = 0x77
    const val VK_F9             = 0x78
    const val VK_F10            = 0x79
    const val VK_F11            = 0x7A
    const val VK_F12            = 0x7B
    const val VK_F13            = 0xF000
    const val VK_F14            = 0xF001
    const val VK_F15            = 0xF002
    const val VK_F16            = 0xF003
    const val VK_F17            = 0xF004
    const val VK_F18            = 0xF005
    const val VK_F19            = 0xF006
    const val VK_F20            = 0xF007
    const val VK_F21            = 0xF008
    const val VK_F22            = 0xF009
    const val VK_F23            = 0xF00A
    const val VK_F24            = 0xF00B
    const val VK_PRINTSCREEN    = 0x9A
    const val VK_INSERT         = 0x9B
    const val VK_HELP           = 0x9C
    const val VK_META           = 0x9D
    const val VK_BACK_QUOTE     = 0xC0
    const val VK_QUOTE          = 0xDE
    const val VK_KP_UP          = 0xE0
    const val VK_KP_DOWN        = 0xE1
    const val VK_KP_LEFT        = 0xE2
    const val VK_KP_RIGHT       = 0xE3
    const val VK_DEAD_GRAVE               = 0x80
    const val VK_DEAD_ACUTE               = 0x81
    const val VK_DEAD_CIRCUMFLEX          = 0x82
    const val VK_DEAD_TILDE               = 0x83
    const val VK_DEAD_MACRON              = 0x84
    const val VK_DEAD_BREVE               = 0x85
    const val VK_DEAD_ABOVEDOT            = 0x86
    const val VK_DEAD_DIAERESIS           = 0x87
    const val VK_DEAD_ABOVERING           = 0x88
    const val VK_DEAD_DOUBLEACUTE         = 0x89
    const val VK_DEAD_CARON               = 0x8a
    const val VK_DEAD_CEDILLA             = 0x8b
    const val VK_DEAD_OGONEK              = 0x8c
    const val VK_DEAD_IOTA                = 0x8d
    const val VK_DEAD_VOICED_SOUND        = 0x8e
    const val VK_DEAD_SEMIVOICED_SOUND    = 0x8f
    const val VK_AMPERSAND                = 0x96
    const val VK_ASTERISK                 = 0x97
    const val VK_QUOTEDBL                 = 0x98
    const val VK_LESS                     = 0x99
    const val VK_GREATER                  = 0xa0
    const val VK_BRACELEFT                = 0xa1
    const val VK_BRACERIGHT               = 0xa2
    const val VK_AT                       = 0x0200
    const val VK_COLON                    = 0x0201
    const val VK_CIRCUMFLEX               = 0x0202
    const val VK_DOLLAR                   = 0x0203
    const val VK_EURO_SIGN                = 0x0204
    const val VK_EXCLAMATION_MARK         = 0x0205
    const val VK_INVERTED_EXCLAMATION_MARK = 0x0206
    const val VK_LEFT_PARENTHESIS         = 0x0207
    const val VK_NUMBER_SIGN              = 0x0208
    const val VK_PLUS                     = 0x0209
    const val VK_RIGHT_PARENTHESIS        = 0x020A
    const val VK_UNDERSCORE               = 0x020B
    const val VK_WINDOWS                  = 0x020C
    const val VK_CONTEXT_MENU             = 0x020D
    const val VK_FINAL                    = 0x0018
    const val VK_CONVERT                  = 0x001C
    const val VK_NONCONVERT               = 0x001D
    const val VK_ACCEPT                   = 0x001E
    const val VK_MODECHANGE               = 0x001F
    const val VK_KANA                     = 0x0015
    const val VK_KANJI                    = 0x0019
    const val VK_ALPHANUMERIC             = 0x00F0
    const val VK_KATAKANA                 = 0x00F1
    const val VK_HIRAGANA                 = 0x00F2
    const val VK_FULL_WIDTH               = 0x00F3
    const val VK_HALF_WIDTH               = 0x00F4
    const val VK_ROMAN_CHARACTERS         = 0x00F5
    const val VK_ALL_CANDIDATES           = 0x0100
    const val VK_PREVIOUS_CANDIDATE       = 0x0101
    const val VK_CODE_INPUT               = 0x0102
    const val VK_JAPANESE_KATAKANA        = 0x0103
    const val VK_JAPANESE_HIRAGANA        = 0x0104
    const val VK_JAPANESE_ROMAN           = 0x0105
    const val VK_KANA_LOCK                = 0x0106
    const val VK_INPUT_METHOD_ON_OFF      = 0x0107
    const val VK_CUT                      = 0xFFD1
    const val VK_COPY                     = 0xFFCD
    const val VK_PASTE                    = 0xFFCF
    const val VK_UNDO                     = 0xFFCB
    const val VK_AGAIN                    = 0xFFC9
    const val VK_FIND                     = 0xFFD0
    const val VK_PROPS                    = 0xFFCA
    const val VK_STOP                     = 0xFFC8
    const val VK_COMPOSE                  = 0xFF20
    const val VK_ALT_GRAPH                = 0xFF7E
    const val VK_BEGIN                    = 0xFF58
    const val VK_UNDEFINED      = 0x0

    // Modifiers masks
    const val SHIFT_DOWN_MASK: Int = 1 shl 6
    const val CTRL_DOWN_MASK: Int = 1 shl 7
    const val META_DOWN_MASK: Int = 1 shl 8
    const val ALT_DOWN_MASK: Int = 1 shl 9
    const val BUTTON1_DOWN_MASK: Int = 1 shl 10
    const val BUTTON2_DOWN_MASK: Int = 1 shl 11
    const val BUTTON3_DOWN_MASK: Int = 1 shl 12
    const val ALT_GRAPH_DOWN_MASK: Int = 1 shl 13
    val BUTTON_DOWN_MASK: IntArray = intArrayOf(
      BUTTON1_DOWN_MASK,
      BUTTON2_DOWN_MASK,
      BUTTON3_DOWN_MASK,
      1 shl 14,  //4th physical button (this is not a wheel!)
      1 shl 15,  //(this is not a wheel!)
      1 shl 16,
      1 shl 17,
      1 shl 18,
      1 shl 19,
      1 shl 20,
      1 shl 21,
      1 shl 22,
      1 shl 23,
      1 shl 24,
      1 shl 25,
      1 shl 26,
      1 shl 27,
      1 shl 28,
      1 shl 29,
      1 shl 30
    )

    const val SHIFT_MASK: Int = 1 shl 0
    const val CTRL_MASK: Int = 1 shl 1
    const val META_MASK: Int = 1 shl 2
    const val ALT_MASK: Int = 1 shl 3
    const val ALT_GRAPH_MASK: Int = 1 shl 5
  }
}
