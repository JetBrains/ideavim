/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.TextEditor
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.VimOptionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope

internal interface IjVimOptionGroup: VimOptionGroup {
  /**
   * Return an accessor for options that only have a global value
   */
  fun getGlobalIjOptions(): GlobalIjOptions

  /**
   * Return an accessor for the effective value of local options
   */
  fun getEffectiveIjOptions(editor: VimEditor): EffectiveIjOptions
}

internal class OptionGroup : VimOptionGroupBase(), IjVimOptionGroup {
  override fun initialiseOptions() {
    // We MUST call super!
    super.initialiseOptions()
    IjOptions.initialise()
  }

  override fun getGlobalIjOptions() = GlobalIjOptions(OptionAccessScope.GLOBAL(null))
  override fun getEffectiveIjOptions(editor: VimEditor) = EffectiveIjOptions(OptionAccessScope.EFFECTIVE(editor))

  companion object {
    fun fileEditorManagerSelectionChangedCallback(event: FileEditorManagerEvent) {
      // Vim only has one window, and it's not possible to close it. This means that editing a new file will always
      // reuse an existing window (opening a new window will always open from an existing window). More importantly,
      // this means that any newly edited file will always get up-to-date local-to-window options. A new window is based
      // on the opening window (treated as split then edit, so copy local + per-window "global" window values, then
      // apply the per-window "global" values) and an edit reapplies the per-window "global" values.
      // If we close all windows, and open a new one, we can only use the per-window "global" values from the fallback
      // window, but this is only initialised when we first read `~/.ideavimrc` during startup. Vim would use the values
      // from the current window, so to simulate this, we should update the fallback window with the values from the
      // window that was selected at the time that the last window was closed.
      // Unfortunately, we can't reliably know if a closing editor is the selected editor. Instead, we rely on selection
      // change events. If an editor is losing selection and there is no new selection, we can assume this means that
      // the last editor has been closed, and use the closed editor to update the fallback window
      //
      // XXX: event.oldEditor will must probably return a disposed editor. So, it should be treated with care
      if (event.newEditor == null) {
        (event.oldEditor as? TextEditor)?.editor?.let {
          (VimPlugin.getOptionGroup() as OptionGroup).updateFallbackWindow(injector.fallbackWindow, it.vim)
        }
      }
    }
  }
}

public class IjOptionConstants {
  @Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate", "ConstPropertyName")
  public companion object {

    public const val idearefactormode_keep: String = "keep"
    public const val idearefactormode_select: String = "select"
    public const val idearefactormode_visual: String = "visual"

    public const val ideastatusicon_enabled: String = "enabled"
    public const val ideastatusicon_gray: String = "gray"
    public const val ideastatusicon_disabled: String = "disabled"

    public const val ideavimsupport_dialog: String = "dialog"
    public const val ideavimsupport_singleline: String = "singleline"
    public const val ideavimsupport_dialoglegacy: String = "dialoglegacy"

    public const val ideawrite_all: String = "all"
    public const val ideawrite_file: String = "file"

    public val ideaStatusIconValues: Set<String> = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    public val ideaRefactorModeValues: Set<String> = setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    public val ideaWriteValues: Set<String> = setOf(ideawrite_all, ideawrite_file)
    public val ideavimsupportValues: Set<String> = setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}
