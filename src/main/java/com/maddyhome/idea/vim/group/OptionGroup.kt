/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group

import com.intellij.application.options.CodeStyle
import com.intellij.codeStyle.AbstractConvertLineSeparatorsAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.EditorSettings.LineNumerationType
import com.intellij.openapi.editor.ScrollPositionCalculator
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.editor.impl.softwrap.SoftWrapAppliancePlaces
import com.intellij.openapi.editor.textarea.TextComponentEditor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.fileEditor.impl.text.TextEditorImpl
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectLocator
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtilRt
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.encoding.ChangeFileEncodingAction
import com.intellij.openapi.vfs.encoding.EncodingUtil.Magic8
import com.intellij.util.ArrayUtil
import com.intellij.util.LineSeparator
import com.intellij.util.PatternUtil
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.GlobalLocalOptionToGlobalLocalExternalSettingMapper
import com.maddyhome.idea.vim.api.GlobalOptionToGlobalLocalExternalSettingMapper
import com.maddyhome.idea.vim.api.GlobalOptionValueOverride
import com.maddyhome.idea.vim.api.LocalOptionToGlobalLocalExternalSettingMapper
import com.maddyhome.idea.vim.api.LocalOptionValueOverride
import com.maddyhome.idea.vim.api.OptionValue
import com.maddyhome.idea.vim.api.OptionValueOverride
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimOptionGroup
import com.maddyhome.idea.vim.api.VimOptionGroupBase
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.ex.ExException
import com.maddyhome.idea.vim.ex.exExceptionMessage
import com.maddyhome.idea.vim.helper.vimDisabled
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.Option
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

internal interface IjVimOptionGroup : VimOptionGroup {
  /**
   * Return an accessor for options that only have a global value
   */
  fun getGlobalIjOptions(): GlobalIjOptions

  /**
   * Return an accessor for the effective value of local options
   */
  fun getEffectiveIjOptions(editor: VimEditor): EffectiveIjOptions
}

private interface InternalOptionValueAccessor {
  fun <T : VimDataType> getOptionValueInternal(option: Option<T>, scope: OptionAccessScope): OptionValue<T>
  fun <T : VimDataType> setOptionValueInternal(option: Option<T>, scope: OptionAccessScope, value: OptionValue<T>)
}

internal class OptionGroup : VimOptionGroupBase(), IjVimOptionGroup, InternalOptionValueAccessor, Disposable.Default {
  private val namedOverrides = mutableMapOf<String, IdeaBackedOptionValueOverride>()
  private val simpleOverrides = mutableSetOf<IdeaBackedOptionValueOverride>()

  init {
    addOptionValueOverride(IjOptions.bomb, BombOptionMapper())
    addOptionValueOverride(IjOptions.breakindent, BreakIndentOptionMapper(IjOptions.breakindent, this))
    addOptionValueOverride(IjOptions.colorcolumn, ColorColumnOptionValueProvider(IjOptions.colorcolumn))
    addOptionValueOverride(IjOptions.cursorline, CursorLineOptionMapper(IjOptions.cursorline))
    addOptionValueOverride(IjOptions.fileencoding, FileEncodingOptionMapper())
    addOptionValueOverride(IjOptions.fileformat, FileFormatOptionMapper())
    addOptionValueOverride(IjOptions.list, ListOptionMapper(IjOptions.list, this))
    addOptionValueOverride(IjOptions.relativenumber, RelativeNumberOptionMapper(IjOptions.relativenumber, this))
    addOptionValueOverride(IjOptions.textwidth, TextWidthOptionMapper(IjOptions.textwidth))
    addOptionValueOverride(IjOptions.wrap, WrapOptionMapper(IjOptions.wrap, this))

    // These options are defined and implemented in vim-engine, but IntelliJ has similar features with settings we can map
    addOptionValueOverride(Options.number, NumberOptionMapper(Options.number, this))
    addOptionValueOverride(Options.scrolljump, ScrollJumpOptionMapper(Options.scrolljump, this))
    addOptionValueOverride(Options.sidescroll, SideScrollOptionMapper(Options.sidescroll, this))
    addOptionValueOverride(Options.scrolloff, ScrollOffOptionMapper(Options.scrolloff, this))
    addOptionValueOverride(Options.sidescrolloff, SideScrollOffOptionMapper(Options.sidescrolloff, this))

    // Apply fold level when foldlevel option changes
    addOptionValueOverride(Options.foldlevel, FoldLevelOptionMapper())

    // When a global editor setting changes, try to update the equivalent Vim option. We don't always update the Vim
    // option when the IDE setting changes. Typically, if the user has explicitly set the Vim option, we don't reset it.
    // The exception is if the option was set in ~/.ideavimrc. This is kind of like setting a global value, so it's
    // reasonable to update the value when the IDE's global value changes. Vim's global options are always updated, too.
    // Note that this callback runs even when Vim is disabled. This is because Vim options can set the local value of an
    // IDE setting, and this callback can be the only way to reset them.
    // There isn't a similar notification for code style changes, so we can't handle colorcolumn or textwidth
    EditorSettingsExternalizable.getInstance().addPropertyChangeListener({ event ->
      namedOverrides[event.propertyName]?.onGlobalIdeaValueChanged(event.propertyName)
      simpleOverrides.forEach { override ->
        override.onGlobalIdeaValueChanged(event.propertyName)
      }
    }, this)
  }

  override fun <T : VimDataType> addOptionValueOverride(option: Option<T>, override: OptionValueOverride<T>) {
    if (override is IdeaBackedOptionValueOverride) {
      override.ideaPropertyName?.let { namedOverrides[it] = override }
      if (override.ideaPropertyName == null) {
        simpleOverrides.add(override)
      }
    }

    super.addOptionValueOverride(option, override)
  }

  override fun initialiseOptions() {
    // We MUST call super!
    super.initialiseOptions()
    IjOptions.initialise()
  }

  override fun getGlobalIjOptions() = GlobalIjOptions(OptionAccessScope.GLOBAL(null))
  override fun getEffectiveIjOptions(editor: VimEditor) = EffectiveIjOptions(OptionAccessScope.EFFECTIVE(editor))

  // Not redundant, it changes visibility for the InternalOptionValueAccessor interface
  @Suppress("RedundantOverride")
  override fun <T : VimDataType> getOptionValueInternal(option: Option<T>, scope: OptionAccessScope): OptionValue<T> {
    return super.getOptionValueInternal(option, scope)
  }

  @Suppress("RedundantOverride")
  override fun <T : VimDataType> setOptionValueInternal(
    option: Option<T>,
    scope: OptionAccessScope,
    value: OptionValue<T>,
  ) {
    super.setOptionValueInternal(option, scope, value)
  }

  companion object {
    fun editorReleased(editor: Editor) {
      // Vim always has at least one window; it's not possible to close it. Editing a new file will open a new buffer in
      // the current window, or it's possible to split the current buffer into a new window, or open a new buffer in a
      // new window. This is important for us because when Vim opens a new window, the new window's local options are
      // copied from the current window.
      // In detail: splitting the current window gets a complete copy of local and per-window global option values.
      // Editing a new file will split the current window and then edit the new buffer in-place.
      // IntelliJ does not always have an open window. It would be weird to close the last editor tab, and then open
      // the next tab with different options - the user would expect the editor to look like the last one did.
      // Therefore, we have a dummy "fallback" window that captures the options of the last closed editor. When opening
      // an editor and there are no currently open editors, we use the fallback window to initialise the new window.
      // This callback tracks when editors are closed, and if the last editor in a project is being closed, updates the
      // fallback window's options.
      val project = editor.project ?: return
      if (!injector.editorGroup.getEditorsRaw()
          .any { it.ij != editor && it.ij.project === project && it.ij.editorKind == EditorKind.MAIN_EDITOR }
      ) {
        (VimPlugin.getOptionGroup() as OptionGroup).updateFallbackWindow(injector.fallbackWindow, editor.vim)
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
 * IdeaVim will never set the global, persistent IntelliJ setting. `:set {option}` in Vim is not persistent, and does
 * not affect all windows, so `:set {option}` in IdeaVim should also not be persistent, and should not affect all
 * windows. IdeaVim will update the local value of the IntelliJ setting. For local-to-window options, only that window's
 * IntelliJ value is updated. For local-to-buffer options, all open windows for the current document are modified. The
 * drawback of this approach is that changing the global IntelliJ value in the Settings dialog will not update current
 * windows. However, modifying the local value through the IDE will, and the global value can be reset with
 * `:set {option}&`.
 *
 * IdeaVim will still keep track of what it thinks the global and local values of these options are, but the
 * local/effective value is mapped to the local/effective IntelliJ setting. The current local value of the Vim option is
 * always reported as the current local/effective value of the IntelliJ setting, so it never gets out of sync. When
 * setting the Vim option, IdeaVim will only update the local IntelliJ setting if the user explicitly sets it with
 * `:set` or `:setlocal`. It does not update the IntelliJ setting when setting the Vim defaults. This means that unless
 * the user explicitly opts in to the Vim option, the current IntelliJ setting value is used. Changing the local
 * IntelliJ setting through the IDE is always reflected. Changing the global IntelliJ value is only reflected if the Vim
 * option is the default value.
 *
 * Normally, Vim updates both local and global values when changing the effective value of an option, and this is still
 * true for mapped options, although the global value is not mapped to anything. This value is used to provide the value
 * when initialising a new window. If the user does not explicitly set the Vim option, the global value is still a
 * default value, and setting the new window's local value to default does not update the IntelliJ setting. But if the
 * user does explicitly set the Vim option, the global value is used to initialise the new window, and is used to update
 * the IntelliJ setting. This gives us expected Vim-like behaviour when creating new windows.
 *
 * Changing the IntelliJ setting's local value through the IDE is treated like `:setlocal` - it updates the local Vim
 * value, but does not change the global Vim value, so it does not affect new window initialisation. Changing the
 * IntelliJ setting's global value through the IDE also behaves the same way when the Vim option is set to default.
 *
 * Typically, options that are implemented in IdeaVim should be registered in vim-engine, even if they are mapped to
 * IntelliJ settings. Options that do not have an IdeaVim implementation should be registered in the host-specific
 * module.
 */



private interface IdeaBackedOptionValueOverride {
  val ideaPropertyName: String?
  fun onGlobalIdeaValueChanged(propertyName: String)
}

/**
 * Base class to map a local Vim option to a global-local IntelliJ setting, handling changes to the global value of the
 * IDE setting.
 */
private abstract class LocalOptionToGlobalLocalIdeaSettingMapper<T : VimDataType>(
  option: Option<T>,
  private val internalOptionValueAccessor: InternalOptionValueAccessor,
) : LocalOptionToGlobalLocalExternalSettingMapper<T>(option), IdeaBackedOptionValueOverride {

  override val ideaPropertyName: String? = null

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == ideaPropertyName) {
      doOnGlobalIdeaValueChanged()
    }
  }

  protected fun doOnGlobalIdeaValueChanged() {
    // This is a local Vim option, and its global value is only used for initialising new windows. Vim does not have a
    // way to change the effective value across all windows.
    // It is mapped to a global-local IntelliJ setting that might, in practice, be global, with no way for the user to
    // set the local value.
    // If the Vim option is "default", then the local part of the global-local IntelliJ setting is unset, and any
    // changes to the global IntelliJ value are reflected in the IdeaVim value. So setting the global IntelliJ value can
    // affect Vim options, unless they've been explicitly set by the user.
    // This can be confusing to the user, as sometimes the local editor will update, and sometimes it won't, especially
    // if the Vim option was set during plugin startup.
    // We reset the local IntelliJ setting if Vim thinks the option is "default" (i.e., explicitly set, then reset with
    // `:set {option}&`, which only copies the current global value), or if it was set during plugin startup. If the
    // user explicitly set the Vim option with `:set {option}` or changed the local IntelliJ setting, we do not reset
    // local editors.
    // Always update if Vim is disabled. This ensures that we update the local value of the IDE's global-local settings.
    // TODO: If the IntelliJ setting is in practice global, should we reset local Vim values?
    // If the IntelliJ setting is truly global-local (e.g. show whitespaces), then we shouldn't reset, to match existing
    // IntelliJ behaviour. But if a local Vim option is mapped to a global IntelliJ setting, is it more intuitive to
    // reset the Vim option when the IntelliJ global value changes? This would be closer to existing IntelliJ behaviour
    injector.editorGroup.getEditors().forEach { editor ->
      val scope = OptionAccessScope.EFFECTIVE(editor)
      val globalValue = getGlobalExternalValue(editor)
      if (getEffectiveExternalValue(editor) != globalValue) {

        val storedValue = internalOptionValueAccessor.getOptionValueInternal(option, scope)
        val newValue = when (storedValue) {
          is OptionValue.Default -> OptionValue.Default(globalValue)
          is OptionValue.InitVimRc -> OptionValue.InitVimRc(globalValue)
          is OptionValue.External -> null
          is OptionValue.User -> null
        } ?: if (vimDisabled(null)) OptionValue.Default(globalValue) else null
        if (newValue != null) {
          resetLocalExternalValueToGlobal(editor)
          internalOptionValueAccessor.setOptionValueInternal(
            option,
            scope,
            newValue
          )
        }
      }
    }
  }
}

/**
 * Base class to map a global Vim option to a global-local IntelliJ setting, handling changes to the global value of the
 * IDE setting.
 *
 * This class assumes that the global-local IntelliJ is effectively global, with no UI to modify the local value. This
 * simplifies the implementation, and is true for all current derived instances.
 */
private abstract class GlobalOptionToGlobalLocalIdeaSettingMapper<T : VimDataType>(
  private val option: Option<T>,
  private val internalOptionValueAccessor: InternalOptionValueAccessor,
) : GlobalOptionToGlobalLocalExternalSettingMapper<T>(), IdeaBackedOptionValueOverride {

  override val ideaPropertyName: String? = null

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == ideaPropertyName) {
      doOnGlobalIdeaValueChanged()
    }
  }

  protected fun doOnGlobalIdeaValueChanged() {
    // All derived options currently return false for this, and the assumption simplifies implementation.
    // We assume that the IntelliJ setting is, in practice, global. The local value of the IntelliJ setting is only set
    // by IdeaVim to avoid modifying the persistent global value. If both Vim option and IntelliJ setting are global, we
    // can update all editors whenever either one changes.
    assert(!canUserModifyExternalLocalValue)

    val globalIdeaValue = getGlobalExternalValue()
    injector.editorGroup.getEditors().forEach { editor ->
      val storedValue = internalOptionValueAccessor.getOptionValueInternal(option, OptionAccessScope.EFFECTIVE(editor))
      if (storedValue.value != globalIdeaValue) {
        resetLocalExternalValue(editor, globalIdeaValue)
        if (storedValue !is OptionValue.Default) {
          internalOptionValueAccessor.setOptionValueInternal(
            option,
            OptionAccessScope.EFFECTIVE(editor),
            OptionValue.External(globalIdeaValue)
          )
        }
      }
    }
  }
}

/**
 * Base class to map a global-local Vim option to a global-local IntelliJ setting, handling changes to the global value
 * of the IDE setting.
 *
 * This class assumes that the global-local IntelliJ is effectively global, with no UI to modify the local value. This
 * simplifies the implementation, and is true for all current derived instances.
 */
private abstract class GlobalLocalOptionToGlobalLocalIdeaSettingMapper<T : VimDataType>(
  option: Option<T>,
  private val internalOptionValueAccessor: InternalOptionValueAccessor,
) : GlobalLocalOptionToGlobalLocalExternalSettingMapper<T>(option), IdeaBackedOptionValueOverride {

  override val ideaPropertyName: String? = null

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == ideaPropertyName) {
      doOnGlobalIdeaValueChanged()
    }
  }

  protected fun doOnGlobalIdeaValueChanged() {
    // All derived options currently return false for this, and the assumption simplifies implementation.
    // We assume that the IntelliJ setting is, in practice, global. The local value of the IntelliJ setting is only set
    // by IdeaVim to avoid modifying the persistent global value. When the IntelliJ global value is changed, we can
    // reset all editors that IdeaVim thinks are set globally (including default).
    assert(!canUserModifyExternalLocalValue)

    // This is a global-local Vim option and a global (in practice) IntelliJ setting. Changing the IntelliJ global value
    // should update the global value of the Vim option, but leave locally set Vim values unchanged.
    // E.g. the user does `:set scrolloff=10` either in `~/.ideavimrc` or at the command line. This sets the global
    // value of the Vim option, but leaves the local value unset. It also updates the local setting in applicable open
    // editors. The user then changes "Vertical Scroll Offset" in the settings dialog. This should update the global
    // value of the Vim option, leaving any local values unchanged.
    val globalValue = getGlobalExternalValue()
    injector.editorGroup.getEditors().forEach { editor ->
      val localVimValue = internalOptionValueAccessor.getOptionValueInternal(option, OptionAccessScope.LOCAL(editor))
      if (getEffectiveExternalValue(editor) != globalValue && localVimValue.value == option.unsetValue) {
        setLocalExternalValue(editor, globalValue)
      }

      val globalScope = OptionAccessScope.GLOBAL(editor)
      val storedValue = internalOptionValueAccessor.getOptionValueInternal(option, globalScope)
      if (storedValue !is OptionValue.Default) {
        internalOptionValueAccessor.setOptionValueInternal(option, globalScope, OptionValue.External(globalValue))
        // Tell the base class that we've changed the global value, so it can update state
        setGlobalValue(storedValue, OptionValue.External(globalValue), editor)
      }
    }
  }
}


/**
 * Maps the `'bomb'` local-to-buffer Vim option to the file's current byte order mark
 *
 * Note that this behaves slightly differently to Vim's `'bomb'` option, which will set the buffer as modified and
 * update the BOM when the file is saved. IdeaVim's `'bomb'` option maps directly to the current state of the file's
 * BOM and updates the file immediately on being set.
 *
 * To prevent unexpected conversions, we treat the option as local-noglobal, so we don't apply the global value as the
 * new local value during window initialisation. See `':help local-noglobal'`.
 */
private class BombOptionMapper : LocalOptionValueOverride<VimInt> {
  override fun getLocalValue(storedValue: OptionValue<VimInt>?, editor: VimEditor): OptionValue<VimInt> {
    // TODO: When would we not have a virtual file? (Other than the fallback window)
    val virtualFile = editor.ij.virtualFile ?: return OptionValue.Default(VimInt.ZERO)

    // It doesn't matter if this is user/external/default - it's the only value it can be
    return OptionValue.User((virtualFile.bom == null).not().asVimInt())
  }

  override fun setLocalValue(
    storedValue: OptionValue<VimInt>?,
    newValue: OptionValue<VimInt>,
    editor: VimEditor,
  ): Boolean {
    // Do nothing if we're setting the initial default
    if (newValue is OptionValue.Default && storedValue == null) return false

    val hasBom = getLocalValue(storedValue, editor).value.booleanValue
    if (hasBom == newValue.value.booleanValue) return false

    // Use IntelliJ's own actions to modify the BOM. This will change the BOM stored in the virtual file, update the
    // file contents and save it
    val actionId = if (hasBom) "RemoveBom" else "AddBom"
    val action =
      injector.actionExecutor.getAction(actionId) ?: throw ExException("Cannot find native action: $actionId")
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.actionExecutor.executeAction(editor, action, context)
    return true
  }
}


/**
 * Maps the `'breakindent'` local-to-window Vim option to the IntelliJ custom soft wrap indent global-local setting
 */
// TODO: We could also implement 'breakindentopt', but only the shift:{n} component would be supportable
private class BreakIndentOptionMapper(
  breakIndentOption: ToggleOption,
  internalOptionValueAccessor: InternalOptionValueAccessor,
) : LocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(breakIndentOption, internalOptionValueAccessor) {

  // The IntelliJ setting is in practice global, from the user's perspective
  override val canUserModifyExternalLocalValue: Boolean = false
  override val ideaPropertyName: String = EditorSettingsExternalizable.PropNames.PROP_USE_CUSTOM_SOFT_WRAP_INDENT

  override fun getGlobalExternalValue(editor: VimEditor) =
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent.asVimInt()

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isUseCustomSoftWrapIndent.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isUseCustomSoftWrapIndent = value.booleanValue
  }
}


/**
 * Maps the `'colorcolumn'` local-to-window Vim option to the IntelliJ global-local soft margin settings
 *
 * TODO: This is a code style setting - how can we react to changes?
 */
private class ColorColumnOptionValueProvider(private val colorColumnOption: StringListOption) :
  LocalOptionToGlobalLocalExternalSettingMapper<VimString>(colorColumnOption) {

  // The IntelliJ setting is in practice global, from the user's perspective
  override val canUserModifyExternalLocalValue: Boolean = false

  override fun getGlobalExternalValue(editor: VimEditor): VimString {
    if (!EditorSettingsExternalizable.getInstance().isRightMarginShown) {
      return VimString.EMPTY
    }

    val ijEditor = editor.ij
    val language = ijEditor.project?.let { TextEditorImpl.getDocumentLanguage(ijEditor) }
    val softMargins = CodeStyle.getSettings(ijEditor).getSoftMargins(language)
    return VimString(buildString {
      softMargins.joinTo(this, ",")

      // Add the default "+0" to mimic Vim showing the 'textwidth' column. See above.
      if (this.isNotEmpty()) append(",")
      append("+0")
    })
  }

  override fun getEffectiveExternalValue(editor: VimEditor): VimString {
    // If isRightMarginShown is disabled, then we don't show any visual guides, including the right margin
    if (!editor.ij.settings.isRightMarginShown) {
      return VimString.EMPTY
    }

    // The fallback editor always has an uninitialised value for softMargins, rather than falling back to the global
    // default. If the global value has been set, the empty value looks like it's been set and we treat it as External.
    // By returning the global value, we treat it as Default, so we don't reset the value when initialising a new
    // window/editor's options
    if (editor.ij is TextComponentEditor) {
      return getGlobalExternalValue(editor)
    }

    val softMargins = editor.ij.settings.softMargins
    return VimString(buildString {
      softMargins.joinTo(this, ",")

      // IntelliJ treats right margin and visual guides as the same - if we're showing either, we're showing both.
      // Vim supports the "+0" syntax to show a highlight column relative to the 'textwidth' value. The user can set
      // the value to an empty string to remove this, and disable the right margin.
      // IntelliJ behaves slightly differently to Vim here - "+0" in Vim will only show the column if 'textwidth' is
      // set, while IntelliJ will show the current right margin even if wrap at margin is false.
      if (this.isNotEmpty()) append(",")
      append("+0")
    })
  }

  override fun setLocalExternalValue(editor: VimEditor, value: VimString) {
    // Given an empty string, hide the margin.
    if (value == VimString.EMPTY) {
      editor.ij.settings.isRightMarginShown = false
    } else {
      editor.ij.settings.isRightMarginShown = true

      val softMargins = mutableListOf<Int>()
      colorColumnOption.split(value.value).forEach {
        if (it.startsWith("+") || it.startsWith("-")) {
          // TODO: Support ±1, ±2, ±n, etc. But this is difficult
          // This would need a listener for the right margin IntelliJ value, and would still add a visual guide at +0
          // We'd also need some mechanism for saving the relative offsets. The override getters would return real
          // column values, while the stored Vim option will be relative
          // We could perhaps add a property change listener from editor settings state?
          // (editor.ij as EditorImpl).state.addPropertyChangeListener(...)
          // (editor.ij.settings as SettingsImpl).getState().addPropertyChangeListener(...)
        } else {
          it.toIntOrNull()?.let(softMargins::add)
        }
      }
      editor.ij.settings.setSoftMargins(softMargins)
    }
  }

  override fun resetLocalExternalValueToGlobal(editor: VimEditor) {
    // Reset the current settings back to default by setting both the flag and the visual guides
    val ijEditor = editor.ij
    val language = ijEditor.project?.let { TextEditorImpl.getDocumentLanguage(ijEditor) }

    // Remember to only update if the value has changed! We don't want to force the global-local values to local only
    if (ijEditor.settings.isRightMarginShown != EditorSettingsExternalizable.getInstance().isRightMarginShown) {
      ijEditor.settings.isRightMarginShown = EditorSettingsExternalizable.getInstance().isRightMarginShown
    }

    val codeStyle = CodeStyle.getSettings(ijEditor)
    val globalSoftMargins = codeStyle.getSoftMargins(language)
    val localSoftMargins = ijEditor.settings.softMargins

    if (globalSoftMargins.count() != localSoftMargins.count() || !localSoftMargins.containsAll(globalSoftMargins)) {
      ijEditor.settings.setSoftMargins(codeStyle.getSoftMargins(language))
    }
  }
}


/**
 * Maps the `'cursorline'` local-to-window Vim option to the IntelliJ global-local caret row setting
 *
 * Note that there isn't a global IntelliJ setting for this option.
 */
private class CursorLineOptionMapper(cursorLineOption: ToggleOption) :
  LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(cursorLineOption) {

  // The IntelliJ setting is in practice global, from the user's perspective
  override val canUserModifyExternalLocalValue: Boolean = false

  override fun getGlobalExternalValue(editor: VimEditor): VimInt {
    // Note that this is hardcoded to `true`
    return EditorSettingsExternalizable.getInstance().isCaretRowShown.asVimInt()
  }

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isCaretRowShown.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isCaretRowShown = value.booleanValue
  }
}


/**
 * Maps the `'fileencoding'` local-to-buffer Vim option to the file's current encoding
 *
 * Note that this behaves somewhat differently to Vim's `'fileencoding'` option. Vim will set the option, but it only
 * applies when the file is written - it just sets the file modified. IdeaVim's option maps directly to the current file
 * encoding and when set, will use IntelliJ's own actions to change the encoding.
 *
 * Vim will set this option when editing a new buffer, based on the value of `'fileencodings'` and the contents of the
 * buffer. We don't support `'fileencodings'`. Instead, IntelliJ will auto-detect the encoding. To prevent unexpected
 * conversions, we mark this option as local-noglobal, even though it's not in Vim's list of local-noglobal options
 * (see `:help local-noglobal`). This prevents the global value being applied to the local value during window
 * initialisation.
 */
private class FileEncodingOptionMapper : LocalOptionValueOverride<VimString> {
  override fun getLocalValue(storedValue: OptionValue<VimString>?, editor: VimEditor): OptionValue<VimString> {
    val virtualFile = editor.ij.virtualFile ?: return OptionValue.External(VimString.EMPTY)

    return OptionValue.External(VimString(virtualFile.charset.name().lowercase(Locale.getDefault())))
  }

  override fun setLocalValue(
    storedValue: OptionValue<VimString>?,
    newValue: OptionValue<VimString>,
    editor: VimEditor,
  ): Boolean {
    // Do nothing if we're setting the initial default
    if (newValue is OptionValue.Default && storedValue == null) return false

    // TODO: When would virtual file be null?
    val virtualFile = editor.ij.virtualFile ?: return false

    val charsetName = newValue.value.value
    if (charsetName.isBlank()) return false   // Default value is "", which is an illegal charset name
    if (!Charset.isSupported(charsetName)) {
      // This is usually reported when writing the file with `:w`
      throw exExceptionMessage("E213")
    }

    val bytes: ByteArray?
    try {
      bytes = if (!virtualFile.isDirectory) VfsUtilCore.loadBytes(virtualFile) else return false
    } catch (e: IOException) {
      return false
    }

    val charset = Charset.forName(charsetName)
    val document = editor.ij.document
    val text = document.text
    val isSafeToConvert = isSafeToConvertTo(virtualFile, text, bytes, charset)
    val isSafeToReload = isSafeToReloadIn(virtualFile, text, bytes, charset)

    val project = editor.ij.project ?: ProjectLocator.getInstance().guessProjectForFile(virtualFile)
    return ChangeFileEncodingAction.changeTo(
      project!!,
      document,
      editor.ij,
      virtualFile,
      charset,
      isSafeToConvert,
      isSafeToReload
    )
  }

  // Based on EncodingUtil.isSafeToConvertTo (copied all over the place...)
  private fun isSafeToConvertTo(
    virtualFile: VirtualFile,
    text: CharSequence,
    bytesOnDisk: ByteArray,
    charset: Charset,
  ): Magic8 {
    try {
      val lineSeparator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null)
      val textToSave = if (lineSeparator == "\n") text else StringUtilRt.convertLineSeparators(text, lineSeparator)

      val chosen = LoadTextUtil.chooseMostlyHarmlessCharset(virtualFile.charset, charset, textToSave.toString())
      val saved = chosen.second
      val textLoadedBack = LoadTextUtil.getTextByBinaryPresentation(saved, charset)

      return when {
        !StringUtil.equals(text, textLoadedBack) -> Magic8.NO_WAY
        saved.contentEquals(bytesOnDisk) -> Magic8.ABSOLUTELY
        else -> Magic8.WELL_IF_YOU_INSIST
      }
    } catch (e: UnsupportedOperationException) { // unsupported encoding
      return Magic8.NO_WAY
    }
  }

  private fun isSafeToReloadIn(
    virtualFile: VirtualFile,
    text: CharSequence,
    bytes: ByteArray,
    charset: Charset,
  ): Magic8 {
    val bom = virtualFile.bom
    if (bom != null && !CharsetToolkit.canHaveBom(charset, bom)) return Magic8.NO_WAY

    val mandatoryBom = CharsetToolkit.getMandatoryBom(charset)
    if (mandatoryBom != null && !ArrayUtil.startsWith(bytes, mandatoryBom)) return Magic8.NO_WAY
    val loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, charset).toString()
    val separator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null)
    val failReason = LoadTextUtil.getCharsetAutoDetectionReason(virtualFile)
    if (failReason != null && StandardCharsets.UTF_8 == virtualFile.charset && StandardCharsets.UTF_8 != charset) return Magic8.NO_WAY

    var bytesToSave = try {
      StringUtil.convertLineSeparators(loaded, separator).toByteArray(charset)
    } catch (e: UnsupportedOperationException) {
      return Magic8.NO_WAY
    } catch (e: NullPointerException) {
      return Magic8.NO_WAY
    }
    if (bom != null && !ArrayUtil.startsWith(bytesToSave, bom)) {
      bytesToSave = ArrayUtil.mergeArrays(bom, bytesToSave)
    }

    return if (!bytesToSave.contentEquals(bytes)) Magic8.NO_WAY
    else if (StringUtil.equals(loaded, text)) Magic8.ABSOLUTELY
    else Magic8.WELL_IF_YOU_INSIST
  }
}


/**
 * Maps the `'fileformat'` local-to-buffer Vim option to the current line separators for the file
 *
 * Note that this behaves slightly differently to Vim's `'fileformat'` option. Vim will set the option, and it only
 * applies when the file is saved. IdeaVim's `'fileformat'` maps directly to the current value of the file's line
 * separators and applies immediately.
 *
 * Vim will set this option when editing a new buffer, based on the value of the `'fileformats'` option, and potentially
 * the contents of the buffer. We don't support `'fileformats'`, we just let IntelliJ auto-detect the value. As such, we
 * don't want the global value of `'fileformat'` being copied over during initialisation and unexpectedly converting
 * line numbers. So we treat the option as `local-noglobal` (see `:help local-noglobal`) even though Vim does't list it
 * as such.
 *
 * Since this is such a simple mapping, we can implement [OptionValueOverride] directly.
 */
private class FileFormatOptionMapper : LocalOptionValueOverride<VimString> {
  override fun getLocalValue(storedValue: OptionValue<VimString>?, editor: VimEditor): OptionValue<VimString> {
    // We should have a virtual file for most scenarios, e.g., scratch files, commit message dialog, etc.
    // The fallback window (TextComponentEditorImpl) does not have a virtual file
    val separator = editor.ij.virtualFile?.let { LoadTextUtil.detectLineSeparator(it, false) }
    val value = VimString(
      when (separator) {
        LineSeparator.LF.separatorString -> "unix"
        LineSeparator.CR.separatorString -> "mac"
        LineSeparator.CRLF.separatorString -> "dos"
        else -> if (injector.systemInfoService.isWindows) "dos" else "unix"
      }
    )

    // There is no difference between user/external/default - the file is always just one format
    return OptionValue.User(value)
  }

  override fun setLocalValue(
    storedValue: OptionValue<VimString>?,
    newValue: OptionValue<VimString>,
    editor: VimEditor,
  ): Boolean {
    // Do nothing if we're setting the initial default
    if (newValue is OptionValue.Default && storedValue == null) return false

    // TODO: If project is null (why would it be? Scratch files?) we could use LoadTextUtil.changeLineSeparators
    // We would have to investigate if we need to wrap it in a write command, etc.
    // Would need a repro to test before implementing.
    val project = editor.ij.project ?: return false
    val virtualFile = editor.ij.virtualFile ?: return false

    val newSeparator = when (newValue.value.value) {
      "dos" -> LineSeparator.CRLF.separatorString
      "mac" -> LineSeparator.CR.separatorString
      "unix" -> LineSeparator.LF.separatorString
      else -> LineSeparator.LF.separatorString
    }
    if (LoadTextUtil.detectLineSeparator(virtualFile, false) != newSeparator) {
      AbstractConvertLineSeparatorsAction.changeLineSeparators(project, virtualFile, newSeparator)
      return true
    }

    return false
  }
}


/**
 * Maps the `'list'` local-to-window Vim option to the IntelliJ global-local whitespace setting
 */
private class ListOptionMapper(listOption: ToggleOption, internalOptionValueAccessor: InternalOptionValueAccessor) :
  LocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(listOption, internalOptionValueAccessor) {

  override val ideaPropertyName: String = EditorSettingsExternalizable.PropNames.PROP_IS_WHITESPACES_SHOWN

  // This is a global-local setting, and can be modified by the user via _View | Active Editor | Show Whitespaces_
  override val canUserModifyExternalLocalValue: Boolean = true

  override fun getGlobalExternalValue(editor: VimEditor) =
    EditorSettingsExternalizable.getInstance().isWhitespacesShown.asVimInt()

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isWhitespacesShown.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isWhitespacesShown = value.booleanValue
  }
}


/**
 * Maps the `'number'` local-to-window option to the IntelliJ's existing (global-local) line number feature
 *
 * Note that this must work with `'relativenumber'` to correctly handle the hybrid modes.
 */
private class NumberOptionMapper(numberOption: ToggleOption, internalOptionValueAccessor: InternalOptionValueAccessor) :
  LocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(numberOption, internalOptionValueAccessor) {

  // This is a global-local setting, and can be modified by the user via _View | Active Editor | Show Line Numbers_
  override val canUserModifyExternalLocalValue: Boolean = true

  override fun getGlobalExternalValue(editor: VimEditor): VimInt {
    return (EditorSettingsExternalizable.getInstance().isLineNumbersShown
      && isShowingAbsoluteLineNumbers(EditorSettingsExternalizable.getInstance().lineNumeration)).asVimInt()
  }

  override fun getEffectiveExternalValue(editor: VimEditor): VimInt {
    return (editor.ij.settings.isLineNumbersShown && isShowingAbsoluteLineNumbers(editor.ij.settings.lineNumerationType)).asVimInt()
  }

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    if (value.booleanValue) {
      if (editor.ij.settings.isLineNumbersShown) {
        if (isShowingRelativeLineNumbers(editor.ij.settings.lineNumerationType)) {
          editor.ij.settings.lineNumerationType = LineNumerationType.HYBRID
        }
      } else {
        editor.ij.settings.isLineNumbersShown = true
        editor.ij.settings.lineNumerationType = LineNumerationType.ABSOLUTE
      }
    } else {
      // Turn off 'number'. Hide lines if 'relativenumber' is not set, else switch to relative
      if (editor.ij.settings.isLineNumbersShown) {
        if (isShowingRelativeLineNumbers(editor.ij.settings.lineNumerationType)) {
          editor.ij.settings.lineNumerationType = LineNumerationType.RELATIVE
        } else {
          editor.ij.settings.isLineNumbersShown = false
        }
      }
    }
  }

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == EditorSettingsExternalizable.PropNames.PROP_ARE_LINE_NUMBERS_SHOWN
      || propertyName == EditorSettingsExternalizable.PropNames.PROP_LINE_NUMERATION
    ) {
      doOnGlobalIdeaValueChanged()
    }
  }
}


/**
 * Maps the `'relativenumber'` local-to-window option to the IntelliJ's existing (global-local) line number feature
 *
 * Note that this must work with `'number'` to correctly handle the hybrid modes.
 */
private class RelativeNumberOptionMapper(
  relativeNumberOption: ToggleOption,
  internalOptionValueAccessor: InternalOptionValueAccessor,
) : LocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(relativeNumberOption, internalOptionValueAccessor) {

  // The lineNumerationType IntelliJ setting is in practice global, from the user's perspective.
  override val canUserModifyExternalLocalValue: Boolean = false

  override fun getGlobalExternalValue(editor: VimEditor): VimInt {
    return (EditorSettingsExternalizable.getInstance().isLineNumbersShown
      && isShowingRelativeLineNumbers(EditorSettingsExternalizable.getInstance().lineNumeration)).asVimInt()
  }

  override fun getEffectiveExternalValue(editor: VimEditor): VimInt {
    return (editor.ij.settings.isLineNumbersShown && isShowingRelativeLineNumbers(editor.ij.settings.lineNumerationType)).asVimInt()
  }

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    if (value.booleanValue) {
      if (editor.ij.settings.isLineNumbersShown) {
        if (isShowingAbsoluteLineNumbers(editor.ij.settings.lineNumerationType)) {
          editor.ij.settings.lineNumerationType = LineNumerationType.HYBRID
        }
      } else {
        editor.ij.settings.isLineNumbersShown = true
        editor.ij.settings.lineNumerationType = LineNumerationType.RELATIVE
      }
    } else {
      // Turn off 'relativenumber'. Hide lines if 'number' is not set, else switch to relative
      if (editor.ij.settings.isLineNumbersShown) {
        if (isShowingAbsoluteLineNumbers(editor.ij.settings.lineNumerationType)) {
          editor.ij.settings.lineNumerationType = LineNumerationType.ABSOLUTE
        } else {
          editor.ij.settings.isLineNumbersShown = false
        }
      }
    }
  }

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == EditorSettingsExternalizable.PropNames.PROP_ARE_LINE_NUMBERS_SHOWN
      || propertyName == EditorSettingsExternalizable.PropNames.PROP_LINE_NUMERATION
    ) {
      doOnGlobalIdeaValueChanged()
    }
  }
}

private fun isShowingAbsoluteLineNumbers(lineNumerationType: LineNumerationType) = when (lineNumerationType) {
  LineNumerationType.ABSOLUTE -> true
  LineNumerationType.RELATIVE -> false
  LineNumerationType.HYBRID -> true
}

private fun isShowingRelativeLineNumbers(lineNumerationType: LineNumerationType) = when (lineNumerationType) {
  LineNumerationType.ABSOLUTE -> false
  LineNumerationType.RELATIVE -> true
  LineNumerationType.HYBRID -> true
}


/**
 * Maps the `'scrolljump'` global Vim option to IntelliJ's global-to-local vertical scroll jump setting
 *
 * Note that `'scrolljump'` is a global Vim option, mapped to a global-local IDE setting. Since IdeaVim handles all
 * scrolling, we should ideally be able to ignore the IDE settings completely. However, when typing, IntelliJ will
 * update the scroll position before IdeaVim gets a chance. If the IDE setting is greater than the IdeaVim value, the
 * editor will be updated to the wrong scroll position. Therefore, we update the local value of all editors (and all new
 * editors) to mimic a global value.
 *
 * We can also clear the overridden IDE setting value by setting it to `-1`. So when the user resets the Vim option to
 * defaults, it will again map to the global IDE value. It's a shame not all IDE settings do this.
 */
private class ScrollJumpOptionMapper(option: NumberOption, internalOptionValueAccessor: InternalOptionValueAccessor) :
  GlobalOptionToGlobalLocalIdeaSettingMapper<VimInt>(option, internalOptionValueAccessor) {

  override val ideaPropertyName: String = EditorSettingsExternalizable.PropNames.PROP_VERTICAL_SCROLL_JUMP

  // The IntelliJ setting is in practice global, from the user's perspective
  override val canUserModifyExternalLocalValue: Boolean = false

  override fun getGlobalExternalValue() = EditorSettingsExternalizable.getInstance().verticalScrollJump.asVimInt()
  override fun getEffectiveExternalValue(editor: VimEditor) = editor.ij.settings.verticalScrollJump.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    // Note that Vim supports -1 to -100 as a percentage value. IntelliJ does not have any validation, but does not
    // handle or expect negative values
    editor.ij.settings.verticalScrollJump = value.value.coerceAtLeast(0)
  }

  override fun resetLocalExternalValue(editor: VimEditor, defaultValue: VimInt) {
    editor.ij.settings.verticalScrollJump = -1
  }
}


/**
 * Maps the `'sidescroll'` global Vim option to IntelliJ's global-local horizontal scroll jump setting
 *
 * Note that `'sidescroll'` is a global Vim option, mapped to a global-local IDE setting. Since IdeaVim handles all
 * scrolling, we should ideally be able to ignore the IDE settings completely. However, when typing, IntelliJ will
 * update the scroll position before IdeaVim gets a chance. If the IDE setting is greater than the IdeaVim value, the
 * editor will be updated to the wrong scroll position. Therefore, we update the local value of all editors (and all new
 * editors) to mimic a global value.
 *
 * We can also clear the overridden IDE setting value by setting it to `-1`. So when the user resets the Vim option to
 * defaults, it will again map to the global IDE value. It's a shame not all IDE settings do this.
 */
private class SideScrollOptionMapper(option: NumberOption, internalOptionValueAccessor: InternalOptionValueAccessor) :
  GlobalOptionToGlobalLocalIdeaSettingMapper<VimInt>(option, internalOptionValueAccessor) {

  override val ideaPropertyName: String = EditorSettingsExternalizable.PropNames.PROP_HORIZONTAL_SCROLL_JUMP

  // The IntelliJ setting is in practice global, from the user's perspective
  override val canUserModifyExternalLocalValue: Boolean = false

  override fun getGlobalExternalValue() = EditorSettingsExternalizable.getInstance().horizontalScrollJump.asVimInt()
  override fun getEffectiveExternalValue(editor: VimEditor) = editor.ij.settings.horizontalScrollJump.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.horizontalScrollJump = value.value
  }

  override fun resetLocalExternalValue(editor: VimEditor, defaultValue: VimInt) {
    editor.ij.settings.horizontalScrollJump = -1
  }
}


/**
 * Map the `'scrolloff'` global-local Vim option to the IntelliJ global-local vertical scroll offset setting
 *
 * This is a global-local Vim option, mapped to a global-local IntelliJ setting. We don't set the persistent global
 * setting value, and there is no UI to modify the local IntelliJ settings. Once the value has been set in IdeaVim, it
 * takes precedence over the global, persistent setting until the option is reset with either `:set scrolloff&` or
 * `:setlocal scrolloff<`.
 *
 * Note that when the IdeaVim value is set, we set the IntelliJ local value to 0 rather than sharing the value. This is
 * to prevent conflicts between IntelliJ and IdeaVim's separate implementations for scrolling. IntelliJ's scrolling
 * includes virtual space at the bottom of the file, while (Idea)Vim doesn't. Combining this with a non-zero
 * `'scrolloff'` value can reposition the bottom of the file. E.g., using `G` will position the last line at the bottom
 * of the file, but then IntelliJ moves it up `'scrolloff'` when the caret is moved.
 *
 * With a large value like `999`, IntelliJ will try to move the current line to the centre of the screen, but then
 * IdeaVim will try to reposition. Normally, this doesn't cause too much of a problem, because setting the scroll
 * position will cancel any outstanding animations. However, using backspace updates the scroll position with animations
 * disabled, so the scroll happens immediately, with a visible "twitch" as the editor scrolls for IntelliJ and then back
 * for IdeaVim.
 *
 * We should consider implementing [ScrollPositionCalculator] which would allow IdeaVim to completely take over
 * scrolling from IntelliJ. This would be a non-trivial change, and it might be better to move the scrolling to
 * vim-engine so it can also work in Fleet.
 */
private class ScrollOffOptionMapper(
  scrollOffOption: NumberOption,
  internalOptionValueAccessor: InternalOptionValueAccessor,
) : OneWayGlobalLocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(scrollOffOption, internalOptionValueAccessor) {

  override val ideaPropertyName: String = EditorSettingsExternalizable.PropNames.PROP_VERTICAL_SCROLL_OFFSET

  override fun getExternalGlobalValue() =
    EditorSettingsExternalizable.getInstance().verticalScrollOffset.asVimInt()

  override fun suppressExternalLocalValue(editor: VimEditor) {
    editor.ij.settings.verticalScrollOffset = 0
  }
}


/**
 * Map the `'sidescrolloff'` global-local Vim option to the IntelliJ global-local horizontal scroll offset setting
 *
 * IntelliJ supports horizontal scroll offset in a similar manner to Vim. However, the implementation calculates offsets
 * using integer font sizes, which can lead to minor inaccuracies when compared to the IdeaVim implementation, such as
 * differences running tests on a Mac.
 *
 * For example, given a `'sidescrolloff'` value of `10`, and a fractional font width of `7.8`, IntelliJ will scroll `80`
 * pixels instead of `78`. This is a very minor difference, but because it overshoots, it means that IdeaVim doesn't
 * need to scroll, which in turn can cause issues with `'sidescroll'` (jump), because IntelliJ doesn't support
 * `sidescroll=0`, which would scroll to position the caret in the middle of the display.
 *
 * It also causes precision problems in the tests. The display is scrolled to a couple of pixels _before_ the leftmost
 * column, which means the rightmost column ends a couple of pixels _after_ the rightmost edge of the display. The tests
 * are quite strict about expecting IdeaVim to scroll to character boundaries, and this can cause failures, e.g.
 * `InsertBackspaceActionTest`.
 *
 * Therefore, this mapping does not update the local external horizontal scroll offset value to match the current Vim
 * value. But it can't ignore it, either - if the IntelliJ value is ever greater than the Vim value, the IntelliJ value
 * will be incorrectly applied to scrolling. Instead, we always set the local external value to `0`, so IntelliJ won't
 * try to apply horizontal scrolling offsets. This means IdeaVim will adjust the scroll position, correctly handling
 * fractional font width, horizontal scroll jump and also handling inlay hints.
 *
 * We should consider implementing [ScrollPositionCalculator] which would allow IdeaVim to completely take over
 * scrolling from IntelliJ. This would be a non-trivial change, and it might be better to move the scrolling to
 * vim-engine so it can also work in Fleet.
 */
private class SideScrollOffOptionMapper(
  sideScrollOffOption: NumberOption,
  internalOptionValueAccessor: InternalOptionValueAccessor,
) : OneWayGlobalLocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(sideScrollOffOption, internalOptionValueAccessor) {

  override val ideaPropertyName: String = EditorSettingsExternalizable.PropNames.PROP_HORIZONTAL_SCROLL_OFFSET

  override fun getExternalGlobalValue() =
    EditorSettingsExternalizable.getInstance().horizontalScrollOffset.asVimInt()

  override fun suppressExternalLocalValue(editor: VimEditor) {
    editor.ij.settings.horizontalScrollOffset = 0
  }
}

/**
 * An abstract base class to map a global-local IDEA setting to a global-local Vim option. The IDEA setting is not
 * updated to reflect the Vim changes, but is kept at a neutral value.
 *
 * This class is used for Vim options that have an IDEA equivalent, but the implementation is handled by IdeaVim, e.g.,
 * scroll jumps and offsets. The IDEA value is not updated, and kept to a neutral value, so that the IDEA implementation
 * does not interfere with the IdeaVim implementation.
 */
private abstract class OneWayGlobalLocalOptionToGlobalLocalIdeaSettingMapper<T : VimDataType>(
  private val option: Option<T>,
  private val internalOptionValueAccessor: InternalOptionValueAccessor,
) : GlobalOptionValueOverride<T>, LocalOptionValueOverride<T>, IdeaBackedOptionValueOverride {

  override fun getGlobalValue(storedValue: OptionValue<T>, editor: VimEditor?): OptionValue<T> {
    if (storedValue is OptionValue.Default) {
      return OptionValue.Default(getExternalGlobalValue())
    }

    return storedValue
  }

  override fun setGlobalValue(storedValue: OptionValue<T>, newValue: OptionValue<T>, editor: VimEditor?): Boolean {
    // The user is updating the global Vim value, via `:setglobal`. IdeaVim scrolling will be using this value. Make
    // sure the IntelliJ values won't interfere
    // Note that we don't reset the local IntelliJ value for `:set {option}&` or `:set {option}<` because the current
    // global IntelliJ value might still interfere with IdeaVim's implementation. We continue to suppress the IntelliJ
    // value.
    injector.editorGroup.getEditors().forEach { suppressExternalLocalValue(it) }
    return storedValue.value != newValue.value
  }

  override fun getLocalValue(storedValue: OptionValue<T>?, editor: VimEditor): OptionValue<T> {
    if (storedValue == null) {
      // Initialisation. Report the global value of the setting. We ignore the local value because the user doesn't have
      // a way to set it. If it has been changed (unlikely if stored value hasn't been set yet), then it would be 0
      return OptionValue.Default(getExternalGlobalValue())
    }

    if (storedValue is OptionValue.Default && storedValue.value != option.unsetValue) {
      // The local value has been reset to Default. It's not the Vim default of "unset", but a copy of the global value.
      // Return the current value of the global external value
      return OptionValue.Default(getExternalGlobalValue())
    }

    // Whatever is left is either explicitly set by the user, or option.unsetValue
    return storedValue
  }

  override fun setLocalValue(storedValue: OptionValue<T>?, newValue: OptionValue<T>, editor: VimEditor): Boolean {
    // Vim local value is being set. We do nothing but set the local IntelliJ value to 0, so IntelliJ's scrolling
    // doesn't affect IdeaVim's scrolling
    suppressExternalLocalValue(editor)
    return storedValue?.value != newValue.value
  }

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == ideaPropertyName) {
      // The IntelliJ global value has changed. We want to use this as the Vim global value. Since we control scrolling,
      // set the local IntelliJ value to 0
      injector.editorGroup.getEditors().forEach { suppressExternalLocalValue(it) }

      // Now update the Vim global value to match the new IntelliJ global value. If the current Vim global value is
      // Default, then it will already reflect the current global external value. Otherwise, update the Vim global value
      // to the external global value.
      val globalScope = OptionAccessScope.GLOBAL(null)
      val storedValue = internalOptionValueAccessor.getOptionValueInternal(option, globalScope)
      if (storedValue !is OptionValue.Default) {
        internalOptionValueAccessor.setOptionValueInternal(
          option,
          globalScope,
          OptionValue.External(getExternalGlobalValue())
        )
      }
    }
  }

  protected abstract fun getExternalGlobalValue(): T
  protected abstract fun suppressExternalLocalValue(editor: VimEditor)
}


/**
 * Map the `'textwidth'` local-to-buffer Vim option to the IntelliJ global-local hard wrap settings
 *
 * Note that this option is local-to-buffer, while the IntelliJ settings are either per-language, or local editor
 * (window) overrides. The [LocalOptionToGlobalLocalExternalSettingMapper] base class will handle this by calling
 * [setLocalExternalValue] for all open editors for the changed buffer.
 */
private class TextWidthOptionMapper(textWidthOption: NumberOption) :
  LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(textWidthOption) {

  // The IntelliJ setting is in practice global, from the user's perspective
  override val canUserModifyExternalLocalValue: Boolean = false

  override fun getGlobalExternalValue(editor: VimEditor): VimInt {
    // Get the default value for the current language. This requires a valid project attached to the editor, which we
    // won't have for the fallback window (it's really a TextComponentEditor). In this case, use a null language and
    // the default right margin for
    // If there's no project, we won't have a language for the editor (this will happen with the fallback window, which
    // is really a TextComponentEditor). In this case, we
    val ijEditor = editor.ij
    val language = ijEditor.project?.let { TextEditorImpl.getDocumentLanguage(ijEditor) }
    if (CodeStyle.getSettings(ijEditor).isWrapOnTyping(language)) {
      return CodeStyle.getSettings(ijEditor).getRightMargin(language).asVimInt()
    }
    return VimInt.ZERO
  }

  override fun getEffectiveExternalValue(editor: VimEditor): VimInt {
    // This requires a non-null project due to Kotlin's type safety. The project value is only used if the editor is
    // null, and for our purposes, it won't be.
    // This value comes from CodeStyle rather than EditorSettingsExternalizable,
    val ijEditor = editor.ij
    val project = ijEditor.project ?: ProjectManager.getInstance().defaultProject
    return if (ijEditor.settings.isWrapWhenTypingReachesRightMargin(project)) {
      ijEditor.settings.getRightMargin(ijEditor.project).asVimInt()
    } else {
      VimInt.ZERO
    }
  }

  // This function is called for all open editors, as 'textwidth' is local-to-buffer, but we set the IntelliJ setting
  // as if it were local-to-window
  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    val ijEditor = editor.ij
    ijEditor.settings.setWrapWhenTypingReachesRightMargin(value.value > 0)
    if (value.value > 0) {
      ijEditor.settings.setRightMargin(value.value)
    }
  }

  override fun resetLocalExternalValueToGlobal(editor: VimEditor) {
    // Reset the current settings back to default by changing both the right margin value, and the flag to wrap while
    // typing. We need to use this override because we don't normally reset the right margin when disabling the flag.
    // This is mainly because IntelliJ shows the hard wrap right margin visual guide by default, even when wrap while
    // typing is not enabled, so resetting the default right margin would be very visible and jarring. We also don't
    // want to try and control visibility of the guide with the 'textwidth' option, as the user is already used to
    // IntelliJ's default behaviour of showing the guide even when wrap while typing is not enabled. Also, visibility
    // of the right margin guide is tied with visibility of other visual guides, and we wouldn't know when to re-enable
    // it - what if we have 'textwidth' enabled but the user doesn't want to see the guide? It's better to let the
    // 'colorcolumn' option handle it. We can make sure it's always got a value of "+0" to show the 'textwidth' guide,
    // and the user can disable all visual guides with `:set colorcolumn=0`.
    val ijEditor = editor.ij
    val language = ijEditor.project?.let { TextEditorImpl.getDocumentLanguage(ijEditor) }

    // Remember to only update if the value has changed! We don't want to force the global-local value to be local only
    val globalRightMargin = CodeStyle.getSettings(ijEditor).getRightMargin(language)
    if (ijEditor.settings.getRightMargin(ijEditor.project) != globalRightMargin) {
      ijEditor.settings.setRightMargin(globalRightMargin)
    }

    val globalIsWrapOnTyping = CodeStyle.getSettings(ijEditor).isWrapOnTyping(language)
    if (ijEditor.settings.isWrapWhenTypingReachesRightMargin(ijEditor.project) != globalIsWrapOnTyping) {
      ijEditor.settings.setWrapWhenTypingReachesRightMargin(globalIsWrapOnTyping)
    }
  }
}


/**
 * Maps the `'wrap'` Vim option to the IntelliJ soft wrap settings
 */
private class WrapOptionMapper(wrapOption: ToggleOption, internalOptionValueAccessor: InternalOptionValueAccessor) :
  LocalOptionToGlobalLocalIdeaSettingMapper<VimInt>(wrapOption, internalOptionValueAccessor) {

  // This is a global-local setting, and can be modified by the user via _View | Active Editor | Soft-Wrap_
  override val canUserModifyExternalLocalValue: Boolean = true

  override fun getGlobalExternalValue(editor: VimEditor) = getGlobalIsUseSoftWraps(editor).asVimInt()
  override fun getEffectiveExternalValue(editor: VimEditor) = getEffectiveIsUseSoftWraps(editor).asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    setIsUseSoftWraps(editor, value.booleanValue)
  }

  override fun canInitialiseOptionFrom(sourceEditor: VimEditor, targetEditor: VimEditor): Boolean {
    // IntelliJ's soft-wrap settings are based on editor kind, so there can be different wrap options for consoles,
    // main editors, etc. This is particularly noticeable in the console when running an application. The main editor
    // might have the Vim default with line wrap enabled. Initialising the run console will also have a default value,
    // and won't be updated by the options subsystem. It might have wrap enabled or not. If the editors were the same
    // kind, the same default value would be used.
    // However, if the main editor has 'wrap' explicitly set, this value is copied to the console, so the behaviour is
    // inconsistent. Furthermore, the run console has a soft-wraps toggle button that works at the global level, and
    // IdeaVim only sets the local value, so the toggle button can be inconsistent too.
    // By denying copying the main editor value during initialisation, the console gets the default value, and the IDE
    // decides what it should be. The behaviour is now more consistent.
    // We're happy to initialise diff editors from main editors, as there isn't a different soft wrap setting there.
    // Preview tabs might also have different settings, but because they're a type of main editor, it doesn't matter
    // so much
    fun editorKindToSoftWrapAppliancesPlace(kind: EditorKind) = when (kind) {
      EditorKind.UNTYPED,
      EditorKind.DIFF,
      EditorKind.MAIN_EDITOR,
        -> SoftWrapAppliancePlaces.MAIN_EDITOR

      EditorKind.CONSOLE -> SoftWrapAppliancePlaces.CONSOLE
      // Treat PREVIEW as a kind of MAIN_EDITOR instead of SWAP.PREVIEW. There are fewer noticeable differences
      EditorKind.PREVIEW -> SoftWrapAppliancePlaces.MAIN_EDITOR
    }

    val sourceKind = editorKindToSoftWrapAppliancesPlace(sourceEditor.ij.editorKind)
    val targetKind = editorKindToSoftWrapAppliancesPlace(targetEditor.ij.editorKind)
    return sourceKind == targetKind
  }


  private fun getGlobalIsUseSoftWraps(editor: VimEditor): Boolean {
    val softWrapAppliancePlace = when (editor.ij.editorKind) {
      EditorKind.UNTYPED,
      EditorKind.DIFF,
      EditorKind.MAIN_EDITOR,
        -> SoftWrapAppliancePlaces.MAIN_EDITOR

      EditorKind.CONSOLE -> SoftWrapAppliancePlaces.CONSOLE
      EditorKind.PREVIEW -> SoftWrapAppliancePlaces.PREVIEW
    }

    val settings = EditorSettingsExternalizable.getInstance()
    if (softWrapAppliancePlace == SoftWrapAppliancePlaces.MAIN_EDITOR) {
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

    return settings.isUseSoftWraps(softWrapAppliancePlace)
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

  override fun onGlobalIdeaValueChanged(propertyName: String) {
    if (propertyName == EditorSettingsExternalizable.PropNames.PROP_USE_SOFT_WRAPS
      || propertyName == EditorSettingsExternalizable.PropNames.PROP_SOFT_WRAP_FILE_MASKS
    ) {
      doOnGlobalIdeaValueChanged()
    }
  }
}


/**
 * Maps the `'foldlevel'` local-to-window Vim option to apply fold levels
 *
 * This mapper ensures that whenever the foldlevel option is changed, the fold state is immediately
 * applied to the editor. It coerces the value to valid bounds and handles the case where the value
 * is set to the same level (e.g., zM when foldlevel is already 0).
 */
private class FoldLevelOptionMapper : LocalOptionValueOverride<VimInt> {
  override fun getLocalValue(storedValue: OptionValue<VimInt>?, editor: VimEditor): OptionValue<VimInt> {
    val maxDepth = editor.getMaxFoldDepth()

    if (storedValue == null) {
      return OptionValue.Default(VimInt(maxDepth + 1))
    }

    val coercedLevel = storedValue.value.value.coerceIn(0, maxDepth + 1)

    return storedValue.withValue(VimInt(coercedLevel))
  }

  override fun setLocalValue(
    storedValue: OptionValue<VimInt>?,
    newValue: OptionValue<VimInt>,
    editor: VimEditor,
  ): Boolean {
    val maxDepth = editor.getMaxFoldDepth()
    val coercedLevel = newValue.value.value.coerceIn(0, maxDepth + 1)

    // When a new window opens, setLocalValue is called twice: first from copyLocalToWindowLocalValues,
    // then from initialiseLocalToWindowOptions. We skip applyFoldLevel in both cases to preserve
    // IntelliJ's default fold state. The first call has storedValue=null, and the second has both
    // storedValue and newValue as Default - these conditions define initialization.
    if (!isInitializing(storedValue, newValue)) {
      editor.applyFoldLevel(coercedLevel)
    }

    return storedValue?.value?.value != coercedLevel
  }

  private fun isInitializing(
    storedValue: OptionValue<VimInt>?,
    newValue: OptionValue<VimInt>,
  ): Boolean = storedValue == null ||
    (storedValue is OptionValue.Default && newValue is OptionValue.Default)
}


class IjOptionConstants {
  @Suppress("SpellCheckingInspection", "MemberVisibilityCanBePrivate", "ConstPropertyName")
  companion object {

    const val idearefactormode_keep: String = "keep"
    const val idearefactormode_select: String = "select"
    const val idearefactormode_visual: String = "visual"

    const val ideastatusicon_enabled: String = "enabled"
    const val ideastatusicon_gray: String = "gray"
    const val ideastatusicon_disabled: String = "disabled"

    const val ideavimsupport_dialog: String = "dialog"
    const val ideavimsupport_singleline: String = "singleline"
    const val ideavimsupport_dialoglegacy: String = "dialoglegacy"

    const val ideawrite_all: String = "all"
    const val ideawrite_file: String = "file"

    val ideaStatusIconValues: Set<String> = setOf(ideastatusicon_enabled, ideastatusicon_gray, ideastatusicon_disabled)
    val ideaRefactorModeValues: Set<String> =
      setOf(idearefactormode_keep, idearefactormode_select, idearefactormode_visual)
    val ideaWriteValues: Set<String> = setOf(ideawrite_all, ideawrite_file)
    val ideavimsupportValues: Set<String> =
      setOf(ideavimsupport_dialog, ideavimsupport_singleline, ideavimsupport_dialoglegacy)
  }
}
