/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

interface VimKeyCodeProvider {
  val ENTER: Char                 // <CR>
  val BACKSPACE: Char            // <BS>
  val TAB: Char                   // <Tab>
  val CANCEL: Char                // <C-c>
  val CLEAR: Char                 // <Clear>
  val SHIFT: Char                 // <S>
  val CONTROL: Char               // <C>
  val ALT: Char                   // <M>
  val PAUSE: Char                 // <Pause>
  val CAPS_LOCK: Char             // <CapsLock>
  val ESCAPE: Char                // <Esc>
  val SPACE: Char                 // <Space>
  val PAGE_UP: Char               // <PageUp>
  val PAGE_DOWN: Char             // <PageDown>
  val END: Char                   // <End>
  val HOME: Char                  // <Home>
  val LEFT: Char                  // <Left>
  val UP: Char                    // <Up>
  val RIGHT: Char                 // <Right>
  val DOWN: Char                  // <Down>

  // Special characters
  val EXCLAMATION_MARK: Char      // !
  val AT: Char                    // @
  val NUMBER_SIGN: Char           // #
  val DOLLAR: Char                // $
  val PERCENT: Char               // %
  val CIRCUMFLEX: Char            // ^
  val AMPERSAND: Char             // &
  val ASTERISK: Char              // *
  val LEFT_PARENTHESIS: Char      // (
  val RIGHT_PARENTHESIS: Char     // )
  val UNDERSCORE: Char            // _
  val PLUS: Char                  // +
  val MINUS: Char                 // -
  val EQUALS: Char                // =
  val COMMA: Char                 // ,
  val PERIOD: Char                // .
  val SLASH: Char                 // /
  val BACK_SLASH: Char            // \
  val SEMICOLON: Char             // ;
  val COLON: Char                 // :
  val QUOTE: Char                 // '
  val DOUBLE_QUOTE: Char              // "
  val BACK_QUOTE: Char            // `
  val LESS: Char                  // <
  val GREATER: Char               // >
  val BRACELEFT: Char             // {
  val BRACERIGHT: Char            // }
  val OPEN_BRACKET: Char          // [
  val CLOSE_BRACKET: Char         // ]
  val EURO_SIGN: Char             // €

  // Numbers 0-9
  val NUMBER_0: Char              // 0
  val NUMBER_1: Char              // 1
  val NUMBER_2: Char              // 2
  val NUMBER_3: Char              // 3
  val NUMBER_4: Char              // 4
  val NUMBER_5: Char              // 5
  val NUMBER_6: Char              // 6
  val NUMBER_7: Char              // 7
  val NUMBER_8: Char              // 8
  val NUMBER_9: Char              // 9

  // Letters A-Z
  val A: Char                     // a
  val B: Char                     // b
  val C: Char                     // c
  val D: Char                     // d
  val E: Char                     // e
  val F: Char                     // f
  val G: Char                     // g
  val H: Char                     // h
  val I: Char                     // i
  val J: Char                     // j
  val K: Char                     // k
  val L: Char                     // l
  val M: Char                     // m
  val N: Char                     // n
  val O: Char                     // o
  val P: Char                     // p
  val Q: Char                     // q
  val R: Char                     // r
  val S: Char                     // s
  val T: Char                     // t
  val U: Char                     // u
  val V: Char                     // v
  val W: Char                     // w
  val X: Char                     // x
  val Y: Char                     // y
  val Z: Char                     // z

  // Numpad
  val NUMPAD0: Char               // <k0>
  val NUMPAD1: Char               // <k1>
  val NUMPAD2: Char               // <k2>
  val NUMPAD3: Char               // <k3>
  val NUMPAD4: Char               // <k4>
  val NUMPAD5: Char               // <k5>
  val NUMPAD6: Char               // <k6>
  val NUMPAD7: Char               // <k7>
  val NUMPAD8: Char               // <k8>
  val NUMPAD9: Char               // <k9>
  val MULTIPLY: Char              // <k*>
  val ADD: Char                   // <k+>
  val SEPARATOR: Char             // <k,>
  val SUBTRACT: Char              // <k->
  val DECIMAL: Char               // <k.>
  val DIVIDE: Char                // <k/>

  // Function keys
  val F1: Char                    // <F1>
  val F2: Char                    // <F2>
  val F3: Char                    // <F3>
  val F4: Char                    // <F4>
  val F5: Char                    // <F5>
  val F6: Char                    // <F6>
  val F7: Char                    // <F7>
  val F8: Char                    // <F8>
  val F9: Char                    // <F9>
  val F10: Char                   // <F10>
  val F11: Char                   // <F11>
  val F12: Char                   // <F12>
  val F13: Char                   // <F13>
  val F14: Char                   // <F14>
  val F15: Char                   // <F15>
  val F16: Char                   // <F16>
  val F17: Char                   // <F17>
  val F18: Char                   // <F18>
  val F19: Char                   // <F19>
  val F20: Char                   // <F20>
  val F21: Char                   // <F21>
  val F22: Char                   // <F22>
  val F23: Char                   // <F23>
  val F24: Char                   // <F24>

  val DELETE: Char                // <Del>
  val NUM_LOCK: Char              // <NumLock>
  val SCROLL_LOCK: Char           // <ScrollLock>

  // Special keys
  val PRINTSCREEN: Char           // <PrintScreen>
  val INSERT: Char                // <Insert>
  val HELP: Char                  // <Help>
  val META: Char                  // <M>

  // Numpad arrows
  val KP_UP: Char                 // <kUp>
  val KP_DOWN: Char               // <kDown>
  val KP_LEFT: Char               // <kLeft>
  val KP_RIGHT: Char              // <kRight>

  // Windows specific
  val WINDOWS: Char
  val CONTEXT_MENU: Char

  // Input method keys
  val FINAL: Char
  val CONVERT: Char
  val NONCONVERT: Char
  val ACCEPT: Char
  val MODECHANGE: Char
  val UNDO: Char

  // Undefined
  val UNDEFINED: Char

  val ACTION: Char
  val PLUG: Char
  val KEYSTROKE: Char
}