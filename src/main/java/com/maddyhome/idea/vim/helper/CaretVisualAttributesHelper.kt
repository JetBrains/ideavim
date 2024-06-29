/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.IsReplaceCharListener
import com.maddyhome.idea.vim.common.ModeChangeListener
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.EffectiveOptionValueChangeListener
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.GuiCursorType
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inBlockSelection
import org.jetbrains.annotations.TestOnly
import java.awt.Color

/**
 * Force the use of the bar caret
 *
 * Avoid this if possible - we should be using caret shape based on mode. This is only used for IntelliJ specific
 * behaviour, e.g. handling selection updates during mouse drag.
 */
internal fun Caret.forceBarCursor() {
  editor.caretModel.primaryCaret.visualAttributes = BAR
}

internal fun Editor.updateCaretsVisualAttributes() {
  // In notebooks command mode the caret is hidden
  // Without this if the caret appears inside a cell while it shouldn't
  if (!HandlerInjector.notebookCommandMode(this)) {
    updatePrimaryCaretVisualAttributes()
    updateSecondaryCaretsVisualAttributes()
  }
}

/**
 * Remove custom visual attributes and reset to defaults
 *
 * Used when Vim emulation is disabled
 */
internal fun Editor.removeCaretsVisualAttributes() {
  caretModel.allCarets.forEach { it.visualAttributes = CaretVisualAttributes.DEFAULT }
}

internal fun Editor.hasBlockOrUnderscoreCaret() = isBlockCursorOverride() ||
  GuiCursorOptionHelper.getAttributes(guicursorMode()).type.let {
    it == GuiCursorType.BLOCK || it == GuiCursorType.HOR
  }

internal object GuicursorChangeListener : EffectiveOptionValueChangeListener {
  override fun onEffectiveValueChanged(editor: VimEditor) {
    editor.ij.updatePrimaryCaretVisualAttributes()
  }
}

private fun Editor.guicursorMode(): GuiCursorMode {
  return GuiCursorMode.fromMode(vim.mode, injector.vimState.isReplaceCharacter)
}

/**
 * Allow the "use block caret" setting to override guicursor options - if set, we use block caret everywhere, if
 * not, we use guicursor options.
 *
 * Note that we look at the persisted value because for pre-212 at least, we modify the per-editor value.
 */
private fun isBlockCursorOverride() = EditorSettingsExternalizable.getInstance().isBlockCursor

private fun Editor.updatePrimaryCaretVisualAttributes() {
  if (VimPlugin.isNotEnabled()) thisLogger().error("The caret attributes should not be updated if the IdeaVim is disabled")
  caretModel.primaryCaret.visualAttributes = AttributesCache.getCaretVisualAttributes(this)

  // Make sure the caret is visible as soon as it's set. It might be invisible while blinking
  // NOTE: At the moment, this causes project leak in tests
  // IJPL-928 - this will be fixed in 2024.2
  // [VERSION UPDATE] 2024.2 - remove if wrapping
  if (!ApplicationManager.getApplication().isUnitTestMode) {
    (this as? EditorEx)?.setCaretVisible(true)
  }
}

private fun Editor.updateSecondaryCaretsVisualAttributes() {
  if (VimPlugin.isNotEnabled()) thisLogger().error("The caret attributes should not be updated if the IdeaVim is disabled")
  // IntelliJ simulates visual block with multiple carets with selections. Do our best to hide them
  val attributes = if (this.vim.inBlockSelection) HIDDEN else AttributesCache.getCaretVisualAttributes(this)
  this.caretModel.allCarets.forEach {
    if (it != this.caretModel.primaryCaret) {
      it.visualAttributes = attributes
    }
  }
}

private val HIDDEN = CaretVisualAttributes(null, CaretVisualAttributes.Weight.NORMAL, CaretVisualAttributes.Shape.BAR, 0F)
private val BLOCK = CaretVisualAttributes(null, CaretVisualAttributes.Weight.NORMAL, CaretVisualAttributes.Shape.BLOCK, 1.0F)
private val BAR = CaretVisualAttributes(null, CaretVisualAttributes.Weight.NORMAL, CaretVisualAttributes.Shape.BAR, 0.25F)

private object AttributesCache {
  private var lastGuicursorValue = ""
  private val cache = mutableMapOf<GuiCursorMode, CaretVisualAttributes>()

  fun getCaretVisualAttributes(editor: Editor): CaretVisualAttributes {
    if (isBlockCursorOverride()) {
      return BLOCK
    }

    val guicursor = injector.globalOptions().guicursor.value
    if (lastGuicursorValue != guicursor) {
      cache.clear()
      lastGuicursorValue = guicursor
    }

    val guicursorMode = editor.guicursorMode()
    return cache.getOrPut(guicursorMode) {
      val attributes = GuiCursorOptionHelper.getAttributes(guicursorMode)
      val shape = when (attributes.type) {
        GuiCursorType.BLOCK -> CaretVisualAttributes.Shape.BLOCK
        GuiCursorType.VER -> CaretVisualAttributes.Shape.BAR
        GuiCursorType.HOR -> CaretVisualAttributes.Shape.UNDERSCORE
      }
      val colour: Color? = null // Support highlight group?
      CaretVisualAttributes(colour, CaretVisualAttributes.Weight.NORMAL, shape, attributes.thickness / 100F)
    }
  }
}

@TestOnly
internal fun getGuiCursorMode(editor: Editor) = editor.guicursorMode()

class CaretVisualAttributesListener : IsReplaceCharListener, ModeChangeListener {
  override fun isReplaceCharChanged(editor: VimEditor) {
    updateCaretsVisual()
  }

  override fun modeChanged(editor: VimEditor, oldMode: Mode) {
    updateCaretsVisual()
  }

  private fun updateCaretsVisual() {
    updateAllEditorsCaretsVisual()
  }

  fun updateAllEditorsCaretsVisual() {
    injector.editorGroup.getEditors().forEach { editor ->
      val ijEditor = (editor as IjVimEditor).editor
      ijEditor.updateCaretsVisualAttributes()
      ijEditor.updateCaretsVisualPosition()
    }
  }
}