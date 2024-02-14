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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.EditorSettings.LineNumerationType
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.TextEditor
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
import com.maddyhome.idea.vim.api.GlobalOptionToGlobalLocalExternalSettingMapper
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
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.NumberOption
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.options.StringListOption
import com.maddyhome.idea.vim.options.ToggleOption
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimInt
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import com.maddyhome.idea.vim.vimscript.model.datatypes.asVimInt
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

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
    addOptionValueOverride(IjOptions.bomb, BombOptionMapper())
    addOptionValueOverride(IjOptions.breakindent, BreakIndentOptionMapper(IjOptions.breakindent))
    addOptionValueOverride(IjOptions.colorcolumn, ColorColumnOptionValueProvider(IjOptions.colorcolumn))
    addOptionValueOverride(IjOptions.cursorline, CursorLineOptionMapper(IjOptions.cursorline))
    addOptionValueOverride(IjOptions.fileencoding, FileEncodingOptionMapper())
    addOptionValueOverride(IjOptions.fileformat, FileFormatOptionMapper())
    addOptionValueOverride(IjOptions.list, ListOptionMapper(IjOptions.list))
    addOptionValueOverride(IjOptions.number, NumberOptionMapper(IjOptions.number))
    addOptionValueOverride(IjOptions.relativenumber, RelativeNumberOptionMapper(IjOptions.number))
    addOptionValueOverride(IjOptions.textwidth, TextWidthOptionMapper(IjOptions.textwidth))
    addOptionValueOverride(IjOptions.wrap, WrapOptionMapper(IjOptions.wrap))

    addOptionValueOverride(Options.scrolljump, ScrollJumpOptionMapper())
    addOptionValueOverride(Options.sidescroll, SideScrollOptionMapper())
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

    val hasBom = getLocalValue(storedValue, editor).value.asBoolean()
    if (hasBom == newValue.value.asBoolean()) return false

    // Use IntelliJ's own actions to modify the BOM. This will change the BOM stored in the virtual file, update the
    // file contents and save it
    val actionId = if (hasBom) "RemoveBom" else "AddBom"
    val action = injector.actionExecutor.getAction(actionId) ?: throw ExException("Cannot find native action: $actionId")
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    injector.actionExecutor.executeAction(editor, action, context)
    return true
  }
}


/**
 * Maps the `'breakindent'` local-to-window Vim option to the IntelliJ custom soft wrap indent global-local setting
 */
// TODO: We could also implement 'breakindentopt', but only the shift:{n} component would be supportable
private class BreakIndentOptionMapper(breakIndentOption: ToggleOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(breakIndentOption) {

  override fun getGlobalExternalValue(editor: VimEditor) =
    EditorSettingsExternalizable.getInstance().isUseCustomSoftWrapIndent.asVimInt()

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isUseCustomSoftWrapIndent.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isUseCustomSoftWrapIndent = value.asBoolean()
  }
}


/**
 * Maps the `'colorcolumn'` local-to-window Vim option to the IntelliJ global-local soft margin settings
 */
private class ColorColumnOptionValueProvider(private val colorColumnOption: StringListOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimString>(colorColumnOption) {

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
    }
    else {
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
        }
        else {
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
 */
private class CursorLineOptionMapper(cursorLineOption: ToggleOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(cursorLineOption) {

  override fun getGlobalExternalValue(editor: VimEditor) =
    EditorSettingsExternalizable.getInstance().isCaretRowShown.asVimInt()

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isCaretRowShown.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isCaretRowShown = value.asBoolean()
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

    val charsetName = newValue.value.asString()
    if (charsetName.isBlank()) return false   // Default value is "", which is an illegal charset name
    if (!Charset.isSupported(charsetName)) {
      // This is usually reported when writing the file with `:w`
      throw ExException("E213: Cannot convert")
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
      Objects.requireNonNull<Project?>(project),
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

  private fun isSafeToReloadIn(virtualFile: VirtualFile, text: CharSequence, bytes: ByteArray, charset: Charset): Magic8 {
    val bom = virtualFile.bom
    if (bom != null && !CharsetToolkit.canHaveBom(charset, bom)) return Magic8.NO_WAY

    val mandatoryBom = CharsetToolkit.getMandatoryBom(charset)
    if (mandatoryBom != null && !ArrayUtil.startsWith(bytes, mandatoryBom)) return Magic8.NO_WAY
    val loaded = LoadTextUtil.getTextByBinaryPresentation(bytes, charset).toString()
    val separator = FileDocumentManager.getInstance().getLineSeparator(virtualFile, null)
    val failReason = LoadTextUtil.getCharsetAutoDetectionReason(virtualFile)
    if (failReason != null && StandardCharsets.UTF_8 == virtualFile.charset && StandardCharsets.UTF_8 != charset) return Magic8.NO_WAY

    var bytesToSave: ByteArray?
    bytesToSave = try {
      StringUtil.convertLineSeparators(loaded, separator).toByteArray(charset)
    }
    catch (e: UnsupportedOperationException) {
      return Magic8.NO_WAY
    }
    catch (e: NullPointerException) {
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
    val value = VimString(when (separator) {
      LineSeparator.LF.separatorString -> "unix"
      LineSeparator.CR.separatorString -> "mac"
      LineSeparator.CRLF.separatorString -> "dos"
      else -> if (injector.systemInfoService.isWindows) "dos" else "unix"
    })

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
private class ListOptionMapper(listOption: ToggleOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(listOption) {

  override fun getGlobalExternalValue(editor: VimEditor) =
    EditorSettingsExternalizable.getInstance().isWhitespacesShown.asVimInt()

  override fun getEffectiveExternalValue(editor: VimEditor) =
    editor.ij.settings.isWhitespacesShown.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.isWhitespacesShown = value.asBoolean()
  }
}


/**
 * Maps the `'number'` local-to-window option to the IntelliJ's existing (global-local) line number feature
 *
 * Note that this must work with `'relativenumber'` to correctly handle the hybrid modes.
 */
private class NumberOptionMapper(numberOption: ToggleOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(numberOption) {

  override fun getGlobalExternalValue(editor: VimEditor): VimInt {
    return (EditorSettingsExternalizable.getInstance().isLineNumbersShown
      && isShowingAbsoluteLineNumbers(EditorSettingsExternalizable.getInstance().lineNumeration)).asVimInt()
  }

  override fun getEffectiveExternalValue(editor: VimEditor): VimInt {
    return (editor.ij.settings.isLineNumbersShown && isShowingAbsoluteLineNumbers(editor.ij.settings.lineNumerationType)).asVimInt()
  }

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    if (value.asBoolean()) {
      if (editor.ij.settings.isLineNumbersShown) {
        if (isShowingRelativeLineNumbers(editor.ij.settings.lineNumerationType)) {
          editor.ij.settings.lineNumerationType = LineNumerationType.HYBRID
        }
      }
      else {
        editor.ij.settings.isLineNumbersShown = true
        editor.ij.settings.lineNumerationType = LineNumerationType.ABSOLUTE
      }
    }
    else {
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
}


/**
 * Maps the `'relativenumber'` local-to-window option to the IntelliJ's existing (global-local) line number feature
 *
 * Note that this must work with `'number'` to correctly handle the hybrid modes.
 */
private class RelativeNumberOptionMapper(relativeNumberOption: ToggleOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(relativeNumberOption) {

  override fun getGlobalExternalValue(editor: VimEditor): VimInt {
    return (EditorSettingsExternalizable.getInstance().isLineNumbersShown
      && isShowingRelativeLineNumbers(EditorSettingsExternalizable.getInstance().lineNumeration)).asVimInt()
  }

  override fun getEffectiveExternalValue(editor: VimEditor): VimInt {
    return (editor.ij.settings.isLineNumbersShown && isShowingRelativeLineNumbers(editor.ij.settings.lineNumerationType)).asVimInt()
  }

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    if (value.asBoolean()) {
      if (editor.ij.settings.isLineNumbersShown) {
        if (isShowingAbsoluteLineNumbers(editor.ij.settings.lineNumerationType)) {
          editor.ij.settings.lineNumerationType = LineNumerationType.HYBRID
        }
      }
      else {
        editor.ij.settings.isLineNumbersShown = true
        editor.ij.settings.lineNumerationType = LineNumerationType.RELATIVE
      }
    }
    else {
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
private class ScrollJumpOptionMapper : GlobalOptionToGlobalLocalExternalSettingMapper<VimInt>() {
  override fun getGlobalExternalValue() = EditorSettingsExternalizable.getInstance().verticalScrollJump.asVimInt()
  override fun getEffectiveExternalValue(editor: VimEditor) = editor.ij.settings.verticalScrollJump.asVimInt()

  override fun setLocalExternalValue(editor: VimEditor, value: VimInt) {
    editor.ij.settings.verticalScrollJump = value.value
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
private class SideScrollOptionMapper : GlobalOptionToGlobalLocalExternalSettingMapper<VimInt>() {
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
 * Map the `'textwidth'` local-to-buffer Vim option to the IntelliJ global-local hard wrap settings
 *
 * Note that this option is local-to-buffer, while the IntelliJ settings are either per-language, or local editor
 * (window) overrides. The [LocalOptionToGlobalLocalExternalSettingMapper] base class will handle this by calling
 * [setLocalExternalValue] for all open editors for the changed buffer.
 */
private class TextWidthOptionMapper(textWidthOption: NumberOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(textWidthOption) {

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
    }
    else {
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
private class WrapOptionMapper(wrapOption: ToggleOption)
  : LocalOptionToGlobalLocalExternalSettingMapper<VimInt>(wrapOption) {

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
