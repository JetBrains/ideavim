/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes.caret

import com.intellij.vim.api.CaretId
import com.intellij.vim.api.Line
import com.intellij.vim.api.Mark
import com.intellij.vim.api.Path
import com.intellij.vim.api.Range
import com.intellij.vim.api.TextType

/**
 * Interface for read operations on a caret in Vim.
 */
interface CaretRead {
  /**
   * The unique identifier for this caret.
   */
  val caretId: CaretId

  /**
   * The current offset (position) of the caret in the document.
   */
  val offset: Int

  /**
   * The current selection range of the caret.
   */
  val selection: Range

  /**
   * Information about the current line where the caret is positioned.
   */
  val line: Line

  /**
   * The last register that was selected for operations.
   * 
   * Example: After using `"ay` to yank into register 'a', this would return 'a'.
   * In VimScript, variable `v:register` contains this value.
   */
  val lastSelectedReg: Char

  /**
   * The default register used when no register is explicitly specified.
   * 
   * In Vim, this is typically the unnamed register (").
   */
  val defaultRegister: Char

  /**
   * Indicates whether the current register was explicitly specified by the user.
   * 
   * Example: After `"ay`, this would be true; after just `y`, this would be false.
   */
  val isRegisterSpecifiedExplicitly: Boolean

  /**
   * Selects a register for subsequent operations.
   * 
   * Example: In Vim, pressing `"a` before an operation selects register 'a'.
   *
   * @param register The register to select
   * @return True if the register was successfully selected, false otherwise
   */
  fun selectRegister(register: Char): Boolean

  /**
   * Resets all registers to their default state.
   */
  fun resetRegisters()

  /**
   * Checks if a register is writable.
   * 
   * Some registers in Vim are read-only. Examples of read-only registers:
   *  - ':' (last executed command)
   *  - '.' (last inserted text)
   *  - '/' (last search pattern)
   *
   * @param register The register to check
   * @return True if the register is writable, false otherwise
   */
  fun isWritable(register: Char): Boolean

  /**
   * Checks if a register is connected to the system clipboard.
   *
   * In Vim, registers '+' and '*' are connected to the system clipboard.
   * Example: Using `"+y` yanks text to the system clipboard.
   *
   * @param register The register to check
   * @return True if the register is connected to the system clipboard, false otherwise
   */
  fun isSystemClipboard(register: Char): Boolean

  /**
   * Checks if the primary selection register is supported.
   * 
   * Example: On Linux, using `"*y` yanks text to the primary selection.
   * 
   * @return True if the primary selection register is supported, false otherwise
   */
  fun isPrimaryRegisterSupported(): Boolean

  /**
   * The marks for the current visual selection.
   * 
   * In Vim, these are the '< and '> marks.
   * Example: After making a visual selection and then pressing ESC, `'<` and `'>` mark the beginning and end.
   * In VimScript `getpos("'<")` and `getpos("'>")` are used to get these marks.
   */
  val selectionMarks: Range?

  /**
   * The marks for the last change.
   * 
   * In Vim, these are the '[ and '] marks.
   * Example: After a change operation like `cw`, these marks indicate the changed region.
   * In VimScript, `getpos("'[")` and `getpos("']")` are used to get these marks.
   */
  val changeMarks: Range?

  /**
   * Gets the text content of a register.
   * 
   * Example: In Vim, `:echo @a` shows the content of register 'a'.
   * In VimScript `getreg('a')` is used to get the content of register 'a'.
   * 
   * @param register The register to get text from
   * @return The text content of the register, or null if the register is empty or doesn't exist
   */
  fun getReg(register: Char): String?

  /**
   * Gets the type of text stored in a register (character-wise, line-wise, or block-wise).
   * 
   * In VimScript, `getregtype('a')` is used to get the type of register 'a'.
   * 
   * @param register The register to get the type from
   * @return The type of text in the register, or null if the register is empty or doesn't exist
   */
  fun getRegType(register: Char): TextType?

  /**
   * Sets the text content and type of a register.
   * 
   * In VimScript, `setreg('a', 'text', 'c')` is used to set register 'a' to "text" with character-wise type.
   * 
   * @param register The register to set
   * @param text The text to store in the register
   * @param textType The type of text (character-wise, line-wise, or block-wise)
   * @return True if the register was successfully set, false otherwise
   */
  fun setReg(register: Char, text: String, textType: TextType = TextType.CHARACTER_WISE): Boolean

  /**
   * Gets a mark by its character key for the current caret.
   *
   * @param char The character key of the mark (a-z, 0-9, etc.)
   * @return The mark, or null if the mark doesn't exist
   */
  fun getMark(char: Char): Mark?

  /**
   * All local marks for the current caret.
   */
  val localMarks: Set<Mark>

  /**
   * Sets a mark at the current caret position.
   *
   * @param char The character key of the mark (a-z, etc.)
   * @return True if the mark was successfully set, false otherwise
   */
  fun setMark(char: Char): Boolean

  /**
   * Sets a mark at the specified offset.
   *
   * @param char The character key of the mark (a-z, etc.)
   * @param offset The offset to set the mark to
   * @return True if the mark was successfully set, false otherwise
   */
  fun setMark(char: Char, offset: Int): Boolean

  /**
   * Removes a local mark for the current caret.
   *
   * @param char The character key of the mark to remove (a-z, etc.)
   */
  fun removeLocalMark(char: Char)

  /**
   * Resets all marks for the current caret.
   */
  fun resetAllMarksForCaret()

  /**
   * Scrolls a full page up or down.
   *
   * @param pages The number of pages to scroll. Positive values scroll down, negative values scroll up.
   * @return True if the scroll was successful, false otherwise
   */
  fun scrollFullPage(pages: Int): Boolean

  /**
   * Scrolls half a page up.
   *
   * @param lines The number of lines to scroll. If 0, uses half the window height.
   * @return True if the scroll was successful, false otherwise
   */
  fun scrollHalfPageUp(lines: Int): Boolean

  /**
   * Scrolls half a page up.
   *
   * @param lines The number of lines to scroll. If 0, uses half the window height.
   * @return True if the scroll was successful, false otherwise
   */
  fun scrollHalfPageDown(lines: Int): Boolean
}
