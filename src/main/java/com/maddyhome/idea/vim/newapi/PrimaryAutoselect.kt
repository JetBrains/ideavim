/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.newapi

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.options.GlobalOptionChangeListener
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.inVisualMode
import com.maddyhome.idea.vim.state.mode.selectionType

/*
 * `autoselect` in 'clipboard': the visual selection goes to PRIMARY. Two things put a selection there:
 * - IdeaVim, through [PrimaryAutoselect], with the type of the visual selection
 * - the platform, which copies every editor selection as untyped text, and which [ClipboardAutoselectChangeListener]
 *   turns off without `autoselect`
 */

/** Publishes the visual selection to PRIMARY with `autoselect`. */
internal class PrimaryAutoselect(
  private val primary: IjPrimarySelection,
  private val isPrimarySupported: () -> Boolean = { injector.registerGroup.isPrimaryRegisterSupported() },
) {
  fun onVisualSelectionChange(editor: VimEditor, caret: ImmutableVimCaret) {
    if (!shouldRepublishVisualSelection(editor)) return
    val text = readVisualSelectionText(editor, caret) ?: return
    val selectionType = editor.mode.selectionType ?: SelectionType.CHARACTER_WISE
    primary.write(IjVimCopiedText(text, emptyList()), selectionType)
  }

  // vim-engine invokes setVisualSelection transiently from operators (yy, dd, c…), text-object
  // handlers, and put-with-selection too — pushing those would corrupt PRIMARY mid-operation.
  private fun shouldRepublishVisualSelection(editor: VimEditor): Boolean {
    if (!editor.inVisualMode) return false
    if (OptionConstants.clipboard_autoselect !in injector.globalOptions().clipboard) return false
    if (injector.vimState.executingCommand?.type in OPERATORS_SKIPPING_PRIMARY_PUSH) return false
    return isPrimarySupported()
  }

  private fun readVisualSelectionText(editor: VimEditor, caret: ImmutableVimCaret): String? {
    val start = caret.selectionStart
    val end = caret.selectionEnd
    if (start >= end) return null
    val rawText = runCatching { editor.text().subSequence(start, end).toString() }.getOrNull() ?: return null
    if (rawText.isEmpty()) return null
    return ensureLinewiseTrailingNewline(rawText, editor.mode.selectionType)
  }

  // `lineToNativeSelection` ends the selection at the trailing `\n` rather than after it, so the
  // raw substring excludes the newline. Re-add it for line-wise selections so external readers
  // observe linewise content.
  private fun ensureLinewiseTrailingNewline(text: String, selectionType: SelectionType?): String =
    if (selectionType == SelectionType.LINE_WISE && !text.endsWith("\n")) "$text\n" else text

  private companion object {
    val OPERATORS_SKIPPING_PRIMARY_PUSH = setOf(
      Command.Type.COPY,
      Command.Type.DELETE,
      Command.Type.CHANGE,
      Command.Type.PASTE,
      Command.Type.INSERT,
    )
  }
}

/**
 * Keeps the platform's own PRIMARY updates in line with `autoselect` in 'clipboard'.
 *
 * The IDE copies every editor selection to PRIMARY on its own, so without `autoselect` IdeaVim
 * turns that off through the registry. The previous registry value is restored once `autoselect` is
 * back or IdeaVim is turned off, so a user who disabled it by hand keeps it disabled.
 */
internal object ClipboardAutoselectChangeListener : GlobalOptionChangeListener {
  private const val UPDATE_PRIMARY_REGISTRY_KEY = "editor.caret.update.primary.selection"

  private var suppression: Disposable? = null

  override fun onGlobalOptionChanged() {
    if (OptionConstants.clipboard_autoselect in injector.globalOptions().clipboard) {
      restore()
    } else {
      suppress()
    }
  }

  fun restore() {
    suppression?.let { Disposer.dispose(it) }
    suppression = null
  }

  private fun suppress() {
    // The suppression is a child of onOffDisposable, so it may already have been disposed with it
    if (suppression?.let { !Disposer.isDisposed(it) } == true) return
    val disposable = Disposer.newDisposable(VimPlugin.getInstance().onOffDisposable, "IdeaVimAutoselect")
    Registry.get(UPDATE_PRIMARY_REGISTRY_KEY).setValue(false, disposable)
    suppression = disposable
  }
}
