/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.helper

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.ex.EditorEx
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.option.GuiCursorMode
import com.maddyhome.idea.vim.option.GuiCursorType
import com.maddyhome.idea.vim.option.OptionChangeListener
import com.maddyhome.idea.vim.option.OptionsManager
import java.awt.Color

/**
 * Force the use of the bar caret
 *
 * Avoid this if possible - we should be using caret shape based on mode. This is only used for IntelliJ specific
 * behaviour, e.g. handling selection updates during mouse drag.
 */
fun Caret.forceBarCursor() {
  // [VERSION UPDATE] 2021.2+
  // Create + cache CaretVisualAttributes
  provider.setBarCursor(editor)
}

fun Editor.updateCaretsVisualAttributes() {
  updatePrimaryCaretVisualAttributes()
  updateSecondaryCaretsVisualAttributes()
}

fun Editor.guicursorMode(): GuiCursorMode {
  if (subMode == CommandState.SubMode.REPLACE_CHARACTER) {
    // Can be true for NORMAL and VISUAL
    return GuiCursorMode.REPLACE
  }
  return when (mode) {
    CommandState.Mode.COMMAND -> GuiCursorMode.NORMAL
    CommandState.Mode.VISUAL -> GuiCursorMode.VISUAL  // TODO: VISUAL_EXCLUSIVE
    CommandState.Mode.SELECT -> GuiCursorMode.VISUAL
    CommandState.Mode.INSERT -> GuiCursorMode.INSERT
    CommandState.Mode.OP_PENDING -> GuiCursorMode.OP_PENDING
    CommandState.Mode.REPLACE -> GuiCursorMode.REPLACE
    // This doesn't handle ci and cr, but we don't care - our CMD_LINE will never call this
    CommandState.Mode.CMD_LINE -> GuiCursorMode.CMD_LINE
  }
}

fun Editor.hasBlockOrUnderscoreCaret() = OptionsManager.guicursor.getAttributes(guicursorMode()).type.let {
  it == GuiCursorType.BLOCK || it == GuiCursorType.HOR
}

// [VERSION UPDATE] 2021.2+
// Don't bother saving/restoring EditorSettings.blockCursor if we're not using it
fun usesBlockCursorEditorSettings() = ApplicationInfo.getInstance().build.baselineVersion < 212

private fun Editor.updatePrimaryCaretVisualAttributes() {
  provider.setPrimaryCaretVisualAttributes(this)
}

private fun Editor.updateSecondaryCaretsVisualAttributes() {
  // IntelliJ simulates visual block with multiple carets with selections. Do our best to hide them
  val attributes = provider.getSecondaryCaretVisualAttributes(this, inBlockSubMode)
  this.caretModel.allCarets.forEach {
    if (it != this.caretModel.primaryCaret) {
      it.visualAttributes = attributes
    }
  }
}

object GuicursorChangeListener : OptionChangeListener<String> {
  override fun valueChange(oldValue: String?, newValue: String?) {
    provider.clearCache()
    localEditors().forEach { it.updatePrimaryCaretVisualAttributes() }
  }
}


// [VERSION UPDATE] 2021.2+
// Once the plugin requires 2021.2 as a base version, get rid of all this and just set the attributes directly
private val provider: CaretVisualAttributesProvider by lazy {
  if (ApplicationInfo.getInstance().build.baselineVersion >= 212) {
    DefaultCaretVisualAttributesProvider()
  }
  else {
    LegacyCaretVisualAttributesProvider()
  }
}

private interface CaretVisualAttributesProvider {
  fun setPrimaryCaretVisualAttributes(editor: Editor)
  fun getSecondaryCaretVisualAttributes(editor: Editor, inBlockSubMode: Boolean): CaretVisualAttributes
  fun setBarCursor(editor: Editor)
  fun clearCache() {}
}

private class DefaultCaretVisualAttributesProvider : CaretVisualAttributesProvider {
  companion object {
    private val HIDDEN = CaretVisualAttributes(null, CaretVisualAttributes.Weight.NORMAL, CaretVisualAttributes.Shape.BAR, 0F)
  }

  private val cache = mutableMapOf<GuiCursorMode, CaretVisualAttributes>()

  private fun getCaretVisualAttributes(editor: Editor): CaretVisualAttributes {
    val guicursorMode = editor.guicursorMode()
    return cache.getOrPut(guicursorMode) {
      val attributes = OptionsManager.guicursor.getAttributes(guicursorMode)
      val shape = when (attributes.type) {
        GuiCursorType.BLOCK -> CaretVisualAttributes.Shape.BLOCK
        GuiCursorType.VER -> CaretVisualAttributes.Shape.BAR
        GuiCursorType.HOR -> CaretVisualAttributes.Shape.UNDERSCORE
      }
      val colour: Color? = null // Support highlight group?
      CaretVisualAttributes(colour, CaretVisualAttributes.Weight.NORMAL, shape, attributes.thickness / 100F)
    }
  }

  override fun setPrimaryCaretVisualAttributes(editor: Editor) {
    editor.caretModel.primaryCaret.visualAttributes = getCaretVisualAttributes(editor)

    // If the caret is blinking, make sure it's made visible as soon as the mode changes
    // See also EditorImpl.updateCaretCursor (called when changing EditorSettings.setBlockCursor)
    (editor as? EditorEx)?.setCaretVisible(true)
  }

  override fun getSecondaryCaretVisualAttributes(editor: Editor, inBlockSubMode: Boolean): CaretVisualAttributes {
    return if (inBlockSubMode) HIDDEN else getCaretVisualAttributes(editor)
  }

  override fun setBarCursor(editor: Editor) {
    editor.caretModel.primaryCaret.visualAttributes =
      CaretVisualAttributes(null, CaretVisualAttributes.Weight.NORMAL, CaretVisualAttributes.Shape.BAR, 0.25F)
  }

  override fun clearCache() {
    cache.clear()
  }
}

// For 2021.1 and below
private class LegacyCaretVisualAttributesProvider : CaretVisualAttributesProvider {
  override fun setPrimaryCaretVisualAttributes(editor: Editor) {
    when (OptionsManager.guicursor.getAttributes(editor.guicursorMode()).type) {
      GuiCursorType.BLOCK, GuiCursorType.HOR -> editor.settings.isBlockCursor = true
      GuiCursorType.VER -> editor.settings.isBlockCursor = false
    }
  }

  override fun getSecondaryCaretVisualAttributes(editor: Editor, inBlockSubMode: Boolean): CaretVisualAttributes =
    if (inBlockSubMode) {
      // Do our best to hide the caret
      val color = editor.colorsScheme.getColor(EditorColors.SELECTION_BACKGROUND_COLOR)
      CaretVisualAttributes(color, CaretVisualAttributes.Weight.NORMAL)
    }
    else {
      CaretVisualAttributes.DEFAULT
    }

  override fun setBarCursor(editor: Editor) {
    editor.settings.isBlockCursor = false
  }
}
