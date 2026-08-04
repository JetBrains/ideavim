/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.VimApi
import com.intellij.vim.api.models.CaretId
import com.intellij.vim.api.models.Mode
import com.intellij.vim.api.models.Range
import com.intellij.vim.api.models.TextType

/**
 * Scope that provides access to various listeners.
 */
@VimApiDsl
interface ListenersScope {
  /**
   * Registers a callback that is invoked when the editor mode changes.
   *
   * The callback receives the previous mode as a parameter.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onModeChange { oldMode ->
   *     if (mode == Mode.INSERT) {
   *       // Do something when entering INSERT mode
   *     }
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when the mode changes
   */
  fun onModeChange(callback: suspend VimApi.(Mode) -> Unit)

  /**
   * Registers a callback that is invoked when text is yanked.
   *
   * The callback receives a map of caret IDs to the yanked text ranges.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onYank { caretRangeMap ->
   *     // Process yanked text ranges
   *     caretRangeMap.forEach { (caretId, range) ->
   *       // Highlight or process the yanked range
   *     }
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when text is yanked
   */
  fun onYank(callback: suspend VimApi.(Map<CaretId, Range.Simple>) -> Unit)

  /**
   * Registers a callback that is invoked after text has been stored into a register.
   *
   * Unlike [onYank], which reports the ranges a yank covered, this reports what was actually
   * stored, and it fires for deletes and changes as well as yanks - they all end up writing a
   * register. It is raised once per operation, after every register the operation touches has been
   * written, so reading a register from the callback sees the new value.
   *
   * It is not raised for the black hole register, nor for writes that do not come from the buffer,
   * such as `let @a = "text"`.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onRegisterStore { register, text, type, isDelete ->
   *     // Record the text in a history of your own
   *   }
   * }
   * ```
   *
   * @param callback Receives the register the operation targeted, the stored text, whether it was
   *                 stored character-wise, line-wise or block-wise, and whether the text was
   *                 removed from the buffer rather than merely copied.
   */
  fun onRegisterStore(callback: suspend VimApi.(register: Char, text: String, type: TextType, isDelete: Boolean) -> Unit)

  /**
   * Registers a callback that is invoked when a new editor is created.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorCreate {
   *     // Initialize resources for the new editor
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor is created
   */
  fun onEditorCreate(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when an editor is released.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorRelease {
   *     // Clean up resources associated with the editor
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor is released
   */
  fun onEditorRelease(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when an editor gains focus.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorFocusGain {
   *     // Perform actions when editor gains focus
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor gains focus
   */
  fun onEditorFocusGain(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when an editor loses focus.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onEditorFocusLost {
   *     // Perform actions when editor loses focus
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when an editor loses focus
   */
  fun onEditorFocusLost(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when macro recording starts.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onMacroRecordingStart {
   *     // Perform actions when macro recording begins
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when macro recording starts
   */
  fun onMacroRecordingStart(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when macro recording finishes.
   *
   * Example:
   * ```kotlin
   * listeners {
   *   onMacroRecordingFinish {
   *     // Perform actions when macro recording ends
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when macro recording finishes
   */
  fun onMacroRecordingFinish(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when IdeaVim is enabled.
   *
   * Example usage:
   * ```kotlin
   * listeners {
   *   onIdeaVimEnabled {
   *     // Initialize plugin resources when IdeaVim is enabled
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when IdeaVim is enabled
   */
  fun onIdeaVimEnabled(callback: suspend VimApi.() -> Unit)

  /**
   * Registers a callback that is invoked when IdeaVim is disabled.
   *
   * Example usage:
   * ```kotlin
   * listeners {
   *   onIdeaVimDisabled {
   *     // Clean up plugin resources when IdeaVim is disabled
   *   }
   * }
   * ```
   *
   * @param callback The function to execute when IdeaVim is disabled
   */
  fun onIdeaVimDisabled(callback: suspend VimApi.() -> Unit)
}
