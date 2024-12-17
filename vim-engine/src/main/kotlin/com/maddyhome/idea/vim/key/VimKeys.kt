/*
 * Copyright 2003-2024 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.key

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.key.VimKeyStroke.Printable
import com.maddyhome.idea.vim.key.VimKeyStroke.Special

@Suppress("Unused")
val VimKeyStroke.Companion.CR  get() = keyMap["ENTER"]!! as Special    // <CR>
@Suppress("Unused")
val VimKeyStroke.Companion.BACKSPACE  get() = keyMap["BACKSPACE"]!! as Special    // <BS>
@Suppress("Unused")
// TODO should it be special? maybe it should be in vimTypedKeyByName?
val VimKeyStroke.Companion.TAB  get() = keyMap["TAB"]!! as Special                // <Tab>
@Suppress("Unused")
val VimKeyStroke.Companion.CANCEL  get() = keyMap["CANCEL"]!!          // <C-c>
@Suppress("Unused")
val VimKeyStroke.Companion.CLEAR  get() = keyMap["CLEAR"]!!            // <Clear>
@Suppress("Unused")
val VimKeyStroke.Companion.SHIFT  get() = keyMap["SHIFT"]!!            // <S>
@Suppress("Unused")
val VimKeyStroke.Companion.CONTROL  get() = keyMap["CONTROL"]!!        // <C>
@Suppress("Unused")
val VimKeyStroke.Companion.ALT  get() = keyMap["ALT"]!!                // <M>
@Suppress("Unused")
val VimKeyStroke.Companion.PAUSE  get() = keyMap["PAUSE"]!!            // <Pause>
@Suppress("Unused")
val VimKeyStroke.Companion.CAPS  get() = keyMap["CAPS"]!!              // <CapsLock>
@Suppress("Unused")
val VimKeyStroke.Companion.ESC  get() = keyMap["ESCAPE"]!! as Special             // <Esc>
@Suppress("Unused")
val VimKeyStroke.Companion.SPACE  get() = keyMap["SPACE"]!!            // <Space>
@Suppress("Unused")
val VimKeyStroke.Companion.PAGE_UP  get() = keyMap["PAGE_UP"]!! as Special        // <PageUp>
@Suppress("Unused")
val VimKeyStroke.Companion.PAGE_DOWN  get() = keyMap["PAGE_DOWN"]!! as Special    // <PageDown>
@Suppress("Unused")
val VimKeyStroke.Companion.END  get() = keyMap["END"]!! as Special                // <End>
@Suppress("Unused")
val VimKeyStroke.Companion.HOME  get() = keyMap["HOME"]!! as Special              // <Home>
@Suppress("Unused")
val VimKeyStroke.Companion.LEFT  get() = keyMap["LEFT"]!! as Special              // <Left>
@Suppress("Unused")
val VimKeyStroke.Companion.UP  get() = keyMap["UP"]!! as Special                  // <Up>
@Suppress("Unused")
val VimKeyStroke.Companion.RIGHT  get() = keyMap["RIGHT"]!! as Special            // <Right>
@Suppress("Unused")
val VimKeyStroke.Companion.DOWN  get() = keyMap["DOWN"]!! as Special              // <Down>

// Special characters
@Suppress("Unused")
val VimKeyStroke.Companion.EXCLAMATION_MARK  get() = keyMap["EXCLAMATION_MARK"]!!      // !
@Suppress("Unused")
val VimKeyStroke.Companion.AT  get() = keyMap["AT"]!!                                  // @
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_SIGN  get() = keyMap["NUMBER_SIGN"]!!                // #
@Suppress("Unused")
val VimKeyStroke.Companion.DOLLAR  get() = keyMap["DOLLAR"]!!                          // $
@Suppress("Unused")
val VimKeyStroke.Companion.PERCENT  get() = keyMap["PERCENT"]!!                        // %
@Suppress("Unused")
val VimKeyStroke.Companion.CIRCUMFLEX  get() = keyMap["CIRCUMFLEX"]!!                  // ^
@Suppress("Unused")
val VimKeyStroke.Companion.AMPERSAND  get() = keyMap["AMPERSAND"]!!                    // &
@Suppress("Unused")
val VimKeyStroke.Companion.ASTERISK  get() = keyMap["ASTERISK"]!!                      // *
@Suppress("Unused")
val VimKeyStroke.Companion.LEFT_PARENTHESIS  get() = keyMap["LEFT_PARENTHESIS"]!!      // (
@Suppress("Unused")
val VimKeyStroke.Companion.RIGHT_PARENTHESIS  get() = keyMap["RIGHT_PARENTHESIS"]!!    // )
@Suppress("Unused")
val VimKeyStroke.Companion.UNDERSCORE  get() = keyMap["UNDERSCORE"]!!                  // _
@Suppress("Unused")
val VimKeyStroke.Companion.PLUS  get() = keyMap["PLUS"]!!                              // +
@Suppress("Unused")
val VimKeyStroke.Companion.MINUS  get() = keyMap["MINUS"]!!                            // -
@Suppress("Unused")
val VimKeyStroke.Companion.EQUALS  get() = keyMap["EQUALS"]!!                          // =
@Suppress("Unused")
val VimKeyStroke.Companion.COMMA  get() = keyMap["COMMA"]!!                            // ,
@Suppress("Unused")
val VimKeyStroke.Companion.PERIOD  get() = keyMap["PERIOD"]!!                          // .
@Suppress("Unused")
val VimKeyStroke.Companion.SLASH  get() = keyMap["SLASH"]!!                            // /
@Suppress("Unused")
val VimKeyStroke.Companion.BACK_SLASH  get() = keyMap["BACK_SLASH"]!!                  // \
@Suppress("Unused")
val VimKeyStroke.Companion.SEMICOLON  get() = keyMap["SEMICOLON"]!!                    // ;
@Suppress("Unused")
val VimKeyStroke.Companion.COLON  get() = keyMap["COLON"]!!                            // :
@Suppress("Unused")
val VimKeyStroke.Companion.QUOTE  get() = keyMap["QUOTE"]!!                            // '
@Suppress("Unused")
val VimKeyStroke.Companion.DOUBLE_QUOTE  get() = keyMap["DOUBLE_QUOTE"]!!              // "
@Suppress("Unused")
val VimKeyStroke.Companion.BACK_QUOTE  get() = keyMap["BACK_QUOTE"]!!                  // `
@Suppress("Unused")
val VimKeyStroke.Companion.LESS  get() = keyMap["LESS"]!!                              // <
@Suppress("Unused")
val VimKeyStroke.Companion.GREATER  get() = keyMap["GREATER"]!!                        // >
@Suppress("Unused")
val VimKeyStroke.Companion.BRACELEFT  get() = keyMap["BRACELEFT"]!!                    // {
@Suppress("Unused")
val VimKeyStroke.Companion.BRACERIGHT  get() = keyMap["BRACERIGHT"]!!                  // }
@Suppress("Unused")
val VimKeyStroke.Companion.OPEN_BRACKET  get() = keyMap["OPEN_BRACKET"]!!              // [
@Suppress("Unused")
val VimKeyStroke.Companion.CLOSE_BRACKET  get() = keyMap["CLOSE_BRACKET"]!!            // ]
@Suppress("Unused")
val VimKeyStroke.Companion.EURO_SIGN  get() = keyMap["EURO_SIGN"]!!                    // €

// Numbers 0-9
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_0  get() = keyMap["NUMBER_0"]!!    // 0
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_1  get() = keyMap["NUMBER_1"]!!    // 1
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_2  get() = keyMap["NUMBER_2"]!!    // 2
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_3  get() = keyMap["NUMBER_3"]!!    // 3
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_4  get() = keyMap["NUMBER_4"]!!    // 4
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_5  get() = keyMap["NUMBER_5"]!!    // 5
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_6  get() = keyMap["NUMBER_6"]!!    // 6
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_7  get() = keyMap["NUMBER_7"]!!    // 7
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_8  get() = keyMap["NUMBER_8"]!!    // 8
@Suppress("Unused")
val VimKeyStroke.Companion.NUMBER_9  get() = keyMap["NUMBER_9"]!!    // 9

// Letters A-Z
@Suppress("Unused")
val VimKeyStroke.Companion.A  get() = keyMap["A"]!!    // a
@Suppress("Unused")
val VimKeyStroke.Companion.B  get() = keyMap["B"]!!    // b
@Suppress("Unused")
val VimKeyStroke.Companion.C  get() = keyMap["C"]!!    // c
@Suppress("Unused")
val VimKeyStroke.Companion.D  get() = keyMap["D"]!!    // d
@Suppress("Unused")
val VimKeyStroke.Companion.E  get() = keyMap["E"]!!    // e
@Suppress("Unused")
val VimKeyStroke.Companion.F  get() = keyMap["F"]!!    // f
@Suppress("Unused")
val VimKeyStroke.Companion.G  get() = keyMap["G"]!!    // g
@Suppress("Unused")
val VimKeyStroke.Companion.H  get() = keyMap["H"]!!    // h
@Suppress("Unused")
val VimKeyStroke.Companion.I  get() = keyMap["I"]!!    // i
@Suppress("Unused")
val VimKeyStroke.Companion.J  get() = keyMap["J"]!!    // j
@Suppress("Unused")
val VimKeyStroke.Companion.K  get() = keyMap["K"]!!    // k
@Suppress("Unused")
val VimKeyStroke.Companion.L  get() = keyMap["L"]!!    // l
@Suppress("Unused")
val VimKeyStroke.Companion.M  get() = keyMap["M"]!!    // m
@Suppress("Unused")
val VimKeyStroke.Companion.N  get() = keyMap["N"]!!    // n
@Suppress("Unused")
val VimKeyStroke.Companion.O  get() = keyMap["O"]!!    // o
@Suppress("Unused")
val VimKeyStroke.Companion.P  get() = keyMap["P"]!!    // p
@Suppress("Unused")
val VimKeyStroke.Companion.Q  get() = keyMap["Q"]!!    // q
@Suppress("Unused")
val VimKeyStroke.Companion.R  get() = keyMap["R"]!!    // r
@Suppress("Unused")
val VimKeyStroke.Companion.S  get() = keyMap["S"]!!    // s
@Suppress("Unused")
val VimKeyStroke.Companion.T  get() = keyMap["T"]!!    // t
@Suppress("Unused")
val VimKeyStroke.Companion.U  get() = keyMap["U"]!!    // u
@Suppress("Unused")
val VimKeyStroke.Companion.V  get() = keyMap["V"]!!    // v
@Suppress("Unused")
val VimKeyStroke.Companion.W  get() = keyMap["W"]!!    // w
@Suppress("Unused")
val VimKeyStroke.Companion.X  get() = keyMap["X"]!!    // x
@Suppress("Unused")
val VimKeyStroke.Companion.Y  get() = keyMap["Y"]!!    // y
@Suppress("Unused")
val VimKeyStroke.Companion.Z  get() = keyMap["Z"]!!    // z

// Numpad
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD0  get() = keyMap["NUMPAD0"]!! as Special               // <k0>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD1  get() = keyMap["NUMPAD1"]!! as Special               // <k1>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD2  get() = keyMap["NUMPAD2"]!! as Special               // <k2>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD3  get() = keyMap["NUMPAD3"]!! as Special               // <k3>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD4  get() = keyMap["NUMPAD4"]!! as Special               // <k4>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD5  get() = keyMap["NUMPAD5"]!! as Special               // <k5>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD6  get() = keyMap["NUMPAD6"]!! as Special               // <k6>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD7  get() = keyMap["NUMPAD7"]!! as Special               // <k7>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD8  get() = keyMap["NUMPAD8"]!! as Special               // <k8>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD9  get() = keyMap["NUMPAD9"]!! as Special               // <k9>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD_MULTIPLY  get() = keyMap["MULTIPLY"]!! as Special      // <k*>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD_ADD  get() = keyMap["ADD"]!! as Special                // <k+>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD_SEPARATOR  get() = keyMap["SEPARATOR"]!! as Special    // <k,>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD_SUBTRACT  get() = keyMap["SUBTRACT"]!! as Special      // <k->
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD_DECIMAL  get() = keyMap["DECIMAL"]!! as Special        // <k.>
@Suppress("Unused")
val VimKeyStroke.Companion.NUMPAD_DIVIDE  get() = keyMap["DIVIDE"]!! as Special          // <k/>

// Function keys
@Suppress("Unused")
val VimKeyStroke.Companion.F1  get() = keyMap["F1"]!! as Special      // <F1>
@Suppress("Unused")
val VimKeyStroke.Companion.F2  get() = keyMap["F2"]!! as Special      // <F2>
@Suppress("Unused")
val VimKeyStroke.Companion.F3  get() = keyMap["F3"]!! as Special      // <F3>
@Suppress("Unused")
val VimKeyStroke.Companion.F4  get() = keyMap["F4"]!! as Special      // <F4>
@Suppress("Unused")
val VimKeyStroke.Companion.F5  get() = keyMap["F5"]!! as Special      // <F5>
@Suppress("Unused")
val VimKeyStroke.Companion.F6  get() = keyMap["F6"]!! as Special      // <F6>
@Suppress("Unused")
val VimKeyStroke.Companion.F7  get() = keyMap["F7"]!! as Special      // <F7>
@Suppress("Unused")
val VimKeyStroke.Companion.F8  get() = keyMap["F8"]!! as Special      // <F8>
@Suppress("Unused")
val VimKeyStroke.Companion.F9  get() = keyMap["F9"]!! as Special      // <F9>
@Suppress("Unused")
val VimKeyStroke.Companion.F10  get() = keyMap["F10"]!! as Special    // <F10>
@Suppress("Unused")
val VimKeyStroke.Companion.F11  get() = keyMap["F11"]!! as Special    // <F11>
@Suppress("Unused")
val VimKeyStroke.Companion.F12  get() = keyMap["F12"]!! as Special    // <F12>
@Suppress("Unused")
val VimKeyStroke.Companion.F13  get() = keyMap["F13"]!! as Special    // <F13>
@Suppress("Unused")
val VimKeyStroke.Companion.F14  get() = keyMap["F14"]!! as Special    // <F14>
@Suppress("Unused")
val VimKeyStroke.Companion.F15  get() = keyMap["F15"]!! as Special    // <F15>
@Suppress("Unused")
val VimKeyStroke.Companion.F16  get() = keyMap["F16"]!! as Special    // <F16>
@Suppress("Unused")
val VimKeyStroke.Companion.F17  get() = keyMap["F17"]!! as Special    // <F17>
@Suppress("Unused")
val VimKeyStroke.Companion.F18  get() = keyMap["F18"]!! as Special    // <F18>
@Suppress("Unused")
val VimKeyStroke.Companion.F19  get() = keyMap["F19"]!! as Special    // <F19>
@Suppress("Unused")
val VimKeyStroke.Companion.F20  get() = keyMap["F20"]!! as Special    // <F20>
@Suppress("Unused")
val VimKeyStroke.Companion.F21  get() = keyMap["F21"]!! as Special    // <F21>
@Suppress("Unused")
val VimKeyStroke.Companion.F22  get() = keyMap["F22"]!! as Special    // <F22>
@Suppress("Unused")
val VimKeyStroke.Companion.F23  get() = keyMap["F23"]!! as Special    // <F23>
@Suppress("Unused")
val VimKeyStroke.Companion.F24  get() = keyMap["F24"]!! as Special    // <F24>

// Special keys
@Suppress("Unused")
val VimKeyStroke.Companion.PRINTSCREEN  get() = keyMap["PRINTSCREEN"]!!    // <PrintScreen>
@Suppress("Unused")
val VimKeyStroke.Companion.INSERT  get() = keyMap["INSERT"]!! as Special              // <Insert>
@Suppress("Unused")
val VimKeyStroke.Companion.UNDO  get() = keyMap["UNDO"]!! as Special                  // <Undo>
@Suppress("Unused")
val VimKeyStroke.Companion.HELP  get() = keyMap["HELP"]!!                  // <Help>
@Suppress("Unused")
val VimKeyStroke.Companion.META  get() = keyMap["META"]!!                  // <M>
@Suppress("Unused")
val VimKeyStroke.Companion.DELETE  get() = keyMap["DELETE"]!! as Special              // <Del>
@Suppress("Unused")
val VimKeyStroke.Companion.NUM_LOCK  get() = keyMap["NUM_LOCK"]!!          // <NumLock>
@Suppress("Unused")
val VimKeyStroke.Companion.SCROLL_LOCK  get() = keyMap["SCROLL_LOCK"]!!    // <ScrollLock>

// Numpad arrows
@Suppress("Unused")
val VimKeyStroke.Companion.KP_UP  get() = keyMap["KP_UP"]!! as Special          // <kUp>
@Suppress("Unused")
val VimKeyStroke.Companion.KP_DOWN  get() = keyMap["KP_DOWN"]!! as Special      // <kDown>
@Suppress("Unused")
val VimKeyStroke.Companion.KP_LEFT  get() = keyMap["KP_LEFT"]!! as Special      // <kLeft>
@Suppress("Unused")
val VimKeyStroke.Companion.KP_RIGHT  get() = keyMap["KP_RIGHT"]!! as Special    // <kRight>

// Windows specific
@Suppress("Unused")
val VimKeyStroke.Companion.WINDOWS get() = keyMap["WINDOWS"]!!
@Suppress("Unused")
val VimKeyStroke.Companion.CONTEXT_MENU get() = keyMap["CONTEXT_MENU"]!!

// Input method keys
@Suppress("Unused")
val VimKeyStroke.Companion.FINAL get() = keyMap["FINAL"]!!
@Suppress("Unused")
val VimKeyStroke.Companion.CONVERT get() = keyMap["CONVERT"]!!
@Suppress("Unused")
val VimKeyStroke.Companion.NONCONVERT get() = keyMap["NONCONVERT"]!!
@Suppress("Unused")
val VimKeyStroke.Companion.ACCEPT get() = keyMap["ACCEPT"]!!
@Suppress("Unused")
val VimKeyStroke.Companion.MODECHANGE get() = keyMap["MODECHANGE"]!!

// TODO what's the usecase for UNDEFINED?
@Suppress("Unused")
val VimKeyStroke.Companion.UNDEFINED get() = keyMap["UNDEFINED"]!!
// TODO do we need separate keys for ACTION, PLUG? There are no such keys in Vim
@Suppress("Unused")
val VimKeyStroke.Companion.ACTION get() = keyMap["ACTION"]!! as Special
@Suppress("Unused")
val VimKeyStroke.Companion.PLUG get() = keyMap["PLUG"]!! as Special
// Indicates that the following sequence inside <> should be treated as a single key
// <C-S-E> - 7 keys (<, C, -, S, -, E, >)
// [KEYSTROKE]<C-S-E> - single key (E with Ctrl, Shift modifiers)
@Suppress("Unused")
val VimKeyStroke.Companion.KEYSTROKE get() = keyMap["KEYSTROKE"]!!

private val keyMap by lazy {
  with(injector.keyCodeProvider) {
    mapOf(
      "ENTER" to Special(ENTER),
      "BACKSPACE" to Special(BACKSPACE),
      "TAB" to Printable(TAB),
      "CANCEL" to Special(CANCEL),
      "CLEAR" to Special(CLEAR),
      "SHIFT" to Special(SHIFT),
      "CONTROL" to Special(CONTROL),
      "ALT" to Special(ALT),
      "PAUSE" to Special(PAUSE),
      "CAPS" to Special(CAPS_LOCK),
      "ESCAPE" to Special(ESCAPE),
      "SPACE" to Printable(SPACE),
      "PAGE_UP" to Special(PAGE_UP),
      "PAGE_DOWN" to Special(PAGE_DOWN),
      "END" to Special(END),
      "HOME" to Special(HOME),
      "LEFT" to Special(LEFT),
      "UP" to Special(UP),
      "RIGHT" to Special(RIGHT),
      "DOWN" to Special(DOWN),
      "EXCLAMATION_MARK" to Printable(EXCLAMATION_MARK),
      "AT" to Printable(AT),
      "NUMBER_SIGN" to Printable(NUMBER_SIGN),
      "DOLLAR" to Printable(DOLLAR),
      "PERCENT" to Printable(PERCENT),
      "CIRCUMFLEX" to Printable(CIRCUMFLEX),
      "AMPERSAND" to Printable(AMPERSAND),
      "ASTERISK" to Printable(ASTERISK),
      "LEFT_PARENTHESIS" to Printable(LEFT_PARENTHESIS),
      "RIGHT_PARENTHESIS" to Printable(RIGHT_PARENTHESIS),
      "UNDERSCORE" to Printable(UNDERSCORE),
      "PLUS" to Printable(PLUS),
      "MINUS" to Printable(MINUS),
      "EQUALS" to Printable(EQUALS),
      "COMMA" to Printable(COMMA),
      "PERIOD" to Printable(PERIOD),
      "SLASH" to Printable(SLASH),
      "BACK_SLASH" to Printable(BACK_SLASH),
      "SEMICOLON" to Printable(SEMICOLON),
      "COLON" to Printable(COLON),
      "QUOTE" to Printable(QUOTE),
      "DOUBLE_QUOTE" to Printable(DOUBLE_QUOTE),
      "BACK_QUOTE" to Printable(BACK_QUOTE),
      "LESS" to Printable(LESS),
      "GREATER" to Printable(GREATER),
      "BRACELEFT" to Printable(BRACELEFT),
      "BRACERIGHT" to Printable(BRACERIGHT),
      "OPEN_BRACKET" to Printable(OPEN_BRACKET),
      "CLOSE_BRACKET" to Printable(CLOSE_BRACKET),
      "EURO_SIGN" to Printable(EURO_SIGN),
      "NUMBER_0" to Printable(NUMBER_0),
      "NUMBER_1" to Printable(NUMBER_1),
      "NUMBER_2" to Printable(NUMBER_2),
      "NUMBER_3" to Printable(NUMBER_3),
      "NUMBER_4" to Printable(NUMBER_4),
      "NUMBER_5" to Printable(NUMBER_5),
      "NUMBER_6" to Printable(NUMBER_6),
      "NUMBER_7" to Printable(NUMBER_7),
      "NUMBER_8" to Printable(NUMBER_8),
      "NUMBER_9" to Printable(NUMBER_9),
      "A" to Printable(A),
      "B" to Printable(B),
      "C" to Printable(C),
      "D" to Printable(D),
      "E" to Printable(E),
      "F" to Printable(F),
      "G" to Printable(G),
      "H" to Printable(H),
      "I" to Printable(I),
      "J" to Printable(J),
      "K" to Printable(K),
      "L" to Printable(L),
      "M" to Printable(M),
      "N" to Printable(N),
      "O" to Printable(O),
      "P" to Printable(P),
      "Q" to Printable(Q),
      "R" to Printable(R),
      "S" to Printable(S),
      "T" to Printable(T),
      "U" to Printable(U),
      "V" to Printable(V),
      "W" to Printable(W),
      "X" to Printable(X),
      "Y" to Printable(Y),
      "Z" to Printable(Z),
      "NUMPAD0" to Printable(NUMPAD0),
      "NUMPAD1" to Printable(NUMPAD1),
      "NUMPAD2" to Printable(NUMPAD2),
      "NUMPAD3" to Printable(NUMPAD3),
      "NUMPAD4" to Printable(NUMPAD4),
      "NUMPAD5" to Printable(NUMPAD5),
      "NUMPAD6" to Printable(NUMPAD6),
      "NUMPAD7" to Printable(NUMPAD7),
      "NUMPAD8" to Printable(NUMPAD8),
      "NUMPAD9" to Printable(NUMPAD9),
      "MULTIPLY" to Printable(MULTIPLY),
      "ADD" to Printable(ADD),
      "SEPARATOR" to Printable(SEPARATOR),
      "SUBTRACT" to Printable(SUBTRACT),
      "DECIMAL" to Printable(DECIMAL),
      "DIVIDE" to Printable(DIVIDE),
      "F1" to Special(F1),
      "F2" to Special(F2),
      "F3" to Special(F3),
      "F4" to Special(F4),
      "F5" to Special(F5),
      "F6" to Special(F6),
      "F7" to Special(F7),
      "F8" to Special(F8),
      "F9" to Special(F9),
      "F10" to Special(F10),
      "F11" to Special(F11),
      "F12" to Special(F12),
      "F13" to Special(F13),
      "F14" to Special(F14),
      "F15" to Special(F15),
      "F16" to Special(F16),
      "F17" to Special(F17),
      "F18" to Special(F18),
      "F19" to Special(F19),
      "F20" to Special(F20),
      "F21" to Special(F21),
      "F22" to Special(F22),
      "F23" to Special(F23),
      "F24" to Special(F24),
      "DELETE" to Special(DELETE),
      "NUM_LOCK" to Special(NUM_LOCK),
      "SCROLL_LOCK" to Special(SCROLL_LOCK),
      "PRINTSCREEN" to Special(PRINTSCREEN),
      "INSERT" to Special(INSERT),
      "HELP" to Special(HELP),
      "META" to Special(META),
      "KP_UP" to Special(KP_UP),
      "KP_DOWN" to Special(KP_DOWN),
      "KP_LEFT" to Special(KP_LEFT),
      "KP_RIGHT" to Special(KP_RIGHT),
      "WINDOWS" to Special(WINDOWS),
      "CONTEXT_MENU" to Special(CONTEXT_MENU),
      "FINAL" to Special(FINAL),
      "CONVERT" to Special(CONVERT),
      "NONCONVERT" to Special(NONCONVERT),
      "ACCEPT" to Special(ACCEPT),
      "MODECHANGE" to Special(MODECHANGE),
      "UNDO" to Special(UNDO),
      "UNDEFINED" to Special(UNDEFINED),
      "ACTION" to Special(ACTION),
      "PLUG" to Special(PLUG),
      "KEYSTROKE" to Special(KEYSTROKE),
    )
  }
}

// TODO more keystrokes
@Suppress("SpellCheckingInspection")
fun getVimKeyCodeByName(lower: String): Char? {
  return when (lower) {
    "cr", "enter", "return" -> VimKeyStroke.CR
    "ins", "insert" -> VimKeyStroke.INSERT
    "home" -> VimKeyStroke.HOME
    "end" -> VimKeyStroke.END
    "pageup" -> VimKeyStroke.PAGE_UP
    "pagedown" -> VimKeyStroke.PAGE_DOWN
    "del", "delete" -> VimKeyStroke.DELETE
    "esc" -> VimKeyStroke.ESC
    "bs", "backspace" -> VimKeyStroke.BACKSPACE
    "tab" -> VimKeyStroke.TAB
    "up" -> VimKeyStroke.UP
    "down" -> VimKeyStroke.DOWN
    "left" -> VimKeyStroke.LEFT
    "right" -> VimKeyStroke.RIGHT
    "f1" -> VimKeyStroke.F1
    "f2" -> VimKeyStroke.F2
    "f3" -> VimKeyStroke.F3
    "f4" -> VimKeyStroke.F4
    "f5" -> VimKeyStroke.F5
    "f6" -> VimKeyStroke.F6
    "f7" -> VimKeyStroke.F7
    "f8" -> VimKeyStroke.F8
    "f9" -> VimKeyStroke.F9
    "f10" -> VimKeyStroke.F10
    "f11" -> VimKeyStroke.F11
    "f12" -> VimKeyStroke.F12
    "plug" -> VimKeyStroke.PLUG
    "action" -> VimKeyStroke.ACTION
    "k0" -> VimKeyStroke.NUMPAD0
    "k1" -> VimKeyStroke.NUMPAD1
    "k2" -> VimKeyStroke.NUMPAD2
    "k3" -> VimKeyStroke.NUMPAD3
    "k4" -> VimKeyStroke.NUMPAD4
    "k5" -> VimKeyStroke.NUMPAD5
    "k6" -> VimKeyStroke.NUMPAD6
    "k7" -> VimKeyStroke.NUMPAD7
    "k8" -> VimKeyStroke.NUMPAD8
    "k9" -> VimKeyStroke.NUMPAD9
    "khome" -> VimKeyStroke.HOME
    "kend" -> VimKeyStroke.END
    "kdown" -> VimKeyStroke.KP_DOWN
    "kup" -> VimKeyStroke.KP_UP
    "kleft" -> VimKeyStroke.KP_LEFT
    "kright" -> VimKeyStroke.KP_RIGHT
    "undo" -> VimKeyStroke.UNDO
    else -> null
  }
}
