/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.util.PatternUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.LocalOptionToGlobalLocalExternalSettingMapper
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.VimOptionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt

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
  init {
    addOptionValueOverride(IjOptions.breakindent, BreakIndentOptionMapper())
    addOptionValueOverride(IjOptions.wrap, WrapOptionMapper())
  }

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

/* Mapping Vim options to IntelliJ settings
 *
 * There is an overlap between some Vim options and IntelliJ settings. Some Vim options such as 'wrap' and 'breakindent'
 * cannot be implemented in IdeaVim, but must be a feature of the host editor, which will have equivalent settings.
 * Similarly, IntelliJ has settings for features that also exist in IdeaVim, but with a different implementation (e.g.
 * IntelliJ has the equivalent of 'scrolloff' et al.) These Vim options can still be implemented by IdeaVim, and mapped
 * to the IntelliJ Setting values.
 *
 * The IntelliJ settings implemented are currently closest to Vim's global-local options. There is a persistent global
 * value maintained by [EditorSettingsExternalizable], and an initially unset local value in [EditorSettings]. The
 * global value is used when the local value is unset. The main difference with Vim's global-local is that IntelliJ does
 * not allow us to "unset" the local value. However, we don't actually care about this - it makes no difference to the
 * implementation.
 *
 * IdeaVim will still keep track of what it thinks the global and local values of these options are, but the
 * local/effective value is mapped to the IntelliJ setting. The current local value of the Vim option is always reported
 * as the current local/effective value of the IntelliJ setting, so it never gets out of sync. When setting the Vim
 * option, IdeaVim will only update the IntelliJ setting if the user explicitly sets it with `:set` or `:setlocal`. It
 * does not update the IntelliJ setting when setting the Vim defaults. This means that unless the user explicitly opts
 * in to the Vim option, the current IntelliJ setting is used. Changing the IntelliJ setting through the IDE is always
 * reflected.
 *
 * Normally, Vim updates both local and global values when changing the effective value of an option, and this is still
 * true for mapped options, although the global value is not mapped to anything. Instead, it is used to provide the
 * value when initialising a new window. If the user does not explicitly set the Vim option, the global value is still
 * a default value, and setting the new window's local value to default does not update the IntelliJ setting. But if the
 * user does explicitly set the Vim option, the global value is used to initialise the new window, and is used to update
 * the IntelliJ setting. This gives us expected Vim-like behaviour when creating new windows.
 *
 * Changing the IntelliJ setting through the IDE is treated like `:setlocal` - it updates the local value, but does not
 * change the global value, so it does not affect new window initialisation.
 *
 * Typically, options that are implemented in IdeaVim should be registered in vim-engine, even if they are mapped to
 * IntelliJ settings. Options that do not have an IdeaVim implementation should be registered in the host-specific
 * module.
 */


/**
 * Maps the `'breakindent'` local-to-window Vim option to the IntelliJ custom soft wrap indent global-local setting
 */
// TODO: We could also implement 'breakindentopt', but only the shift:{n} component would be supportable
private class BreakIndentOptionMapper : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>() {
  override fun getGlobalExternalValue(editor: VimEditor) =
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent.asVimInt()

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isUseCustomSoftWrapIndent.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isUseCustomSoftWrapIndent = value.asBoolean()
  }
}


/**
 * Maps the `'wrap'` Vim option to the IntelliJ soft wrap settings
 */
private class WrapOptionMapper : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>() {
  override fun getGlobalExternalValue(editor: VimEditor) = getGlobalIsUseSoftWraps(editor).asVimInt()
  override fun getEffectiveExternalValue(editor: VimEditor) = getEffectiveIsUseSoftWraps(editor).asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    setIsUseSoftWraps(editor, value.asBoolean())
  }

  private fun getGlobalIsUseSoftWraps(editor: VimEditor): Boolean {
    val settings = EditorSettingsExternalizable.getInstance()
    if (settings.isUseSoftWraps) {
      val masks = settings.softWrapFileMasks
      if (masks.trim() == "*") return true

      editor.ij.virtualFile?.let { file ->
        masks.split(";").forEach { mask ->
          val trimmed = mask.trim()
          if (trimmed.isNotEmpty() && PatternUtil.fromMask(trimmed).matcher(file.name).matches()) {
            return true
          }
        }
      }
    }

    return false
  }

  private fun getEffectiveIsUseSoftWraps(editor: VimEditor) = editor.ij.settings.isUseSoftWraps

  private fun setIsUseSoftWraps(editor: VimEditor, value: Boolean) {
    editor.ij.settings.isUseSoftWraps = value

    // Something goes wrong when disabling wraps in test mode. They enable correctly (which is good as it's the
    // default), and the editor scrollbars are reset to the current screen width. But when disabling, the
    // scrollbars aren't updated, so trying to scroll to the end of a long line doesn't fit, and fails. This
    // doesn't happen interactively, but I don't see why. The control flow in the debugger is different, perhaps
    // because tests run headless then the UI is updated less, or differently, at least.
    if (ApplicationManager.getApplication().isUnitTestMode) {
      (editor.ij as? EditorEx)?.scrollPane?.viewport?.doLayout()
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
