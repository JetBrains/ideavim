/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.register

import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import org.jetbrains.annotations.TestOnly
import javax.swing.KeyStroke

public interface VimRegisterGroup {

  /**
   * Get the last register selected by the user
   *
   * @return The register, null if no such register
   */
  public val lastRegister: Register?
  public var lastRegisterChar: Char
  public val currentRegister: Char

  /**
   * When we access last register, it can be e.g. " because of two reasons:
   * 1. Because the default register value was used
   * 2. If the " was specified explicitly
   * We have this boolean flag to differ this to cases
   */
  public val isRegisterSpecifiedExplicitly: Boolean
  public val defaultRegister: Char

  public fun isValid(reg: Char): Boolean
  public fun selectRegister(reg: Char): Boolean
  public fun resetRegister()
  public fun resetRegisters()
  public fun recordKeyStroke(key: KeyStroke)
  public fun isRegisterWritable(): Boolean

  /** Store text into the last register. */
  public fun storeText(
    editor: VimEditor,
    caret: ImmutableVimCaret,
    range: TextRange,
    type: SelectionType,
    isDelete: Boolean,
  ): Boolean

  /**
   * Stores text to any writable register (used for the let command)
   */
  public fun storeText(register: Char, text: String): Boolean

  /**
   * Stores text to any writable register (used for multicaret tests)
   */
  @TestOnly
  // todo better tests
  public fun storeText(register: Char, text: String, selectionType: SelectionType): Boolean

  /**
   * Stores text, character wise, in the given special register
   *
   *
   * This method is intended to support writing to registers when the text cannot be yanked from an editor. This is
   * expected to only be used to update the search and command registers. It will not update named registers.
   *
   *
   * While this method allows setting the unnamed register, this should only be done from tests, and only when it's
   * not possible to yank or cut from the fixture editor. This method will skip additional text processing, and won't
   * update other registers such as the small delete register or reorder the numbered registers. It is much more
   * preferable to yank from the fixture editor.
   */
  public fun storeTextSpecial(register: Char, text: String): Boolean
  public fun getRegister(r: Char): Register?
  public fun getRegisters(): List<Register>
  public fun saveRegister(r: Char, register: Register)
  public fun startRecording(editor: VimEditor, register: Char): Boolean

  public fun getPlaybackRegister(r: Char): Register?
  public fun recordText(text: String)
  public fun setKeys(register: Char, keys: List<KeyStroke>)
  public fun setKeys(register: Char, keys: List<KeyStroke>, type: SelectionType)
  public fun finishRecording(editor: VimEditor)
  public fun getCurrentRegisterForMulticaret(): Char // `set clipbaard+=unnamedplus` should not make system register the default one when working with multiple carets VIM-2804
  public fun isSystemClipboard(register: Char): Boolean
  public fun isPrimaryRegisterSupported(): Boolean
}
