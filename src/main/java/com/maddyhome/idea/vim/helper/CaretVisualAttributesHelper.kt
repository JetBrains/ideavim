/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretVisualAttributes
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable
import com.maddyhome.idea.vim.command.CommandState
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionChangeListener
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDataType
import com.maddyhome.idea.vim.options.helpers.GuiCursorMode
import com.maddyhome.idea.vim.options.helpers.GuiCursorOptionHelper
import com.maddyhome.idea.vim.options.helpers.GuiCursorType
import org.jetbrains.annotations.TestOnly
import java.awt.Color

/**
 * Force the use of the bar caret
 *
 * Avoid this if possible - we should be using caret shape based on mode. This is only used for IntelliJ specific
 * behaviour, e.g. handling selection updates during mouse drag.
 */
fun Caret.forceBarCursor() {
  editor.caretModel.primaryCaret.visualAttributes = BAR
}

fun Editor.updateCaretsVisualAttributes() {
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
fun Editor.removeCaretsVisualAttributes() {
  caretModel.allCarets.forEach { it.visualAttributes = CaretVisualAttributes.DEFAULT }
}

fun Editor.hasBlockOrUnderscoreCaret() = isBlockCursorOverride() ||
  GuiCursorOptionHelper.getAttributes(guicursorMode()).type.let {
    it == GuiCursorType.BLOCK || it == GuiCursorType.HOR
  }

object GuicursorChangeListener : OptionChangeListener<VimDataType> {
  override fun processGlobalValueChange(oldValue: VimDataType?) {
    AttributesCache.clear()
    GuiCursorOptionHelper.clearEffectiveValues()
    localEditors().forEach { it.updatePrimaryCaretVisualAttributes() }
  }
}

private fun Editor.guicursorMode(): GuiCursorMode {
  if (this.vim.commandState.isReplaceCharacter) {
    // Can be true for NORMAL and VISUAL
    return GuiCursorMode.REPLACE
  }

  // Note that Vim does not change the caret for SELECT mode and continues to use VISUAL or VISUAL_EXCLUSIVE. IdeaVim
  // makes much more use of SELECT than Vim does (e.g. it's the default for idearefactormode) so it makes sense for us
  // to more visually distinguish VISUAL and SELECT. So we use INSERT; a selection and the insert caret is intuitively
  // the same as SELECT
  return when (mode) {
    CommandState.Mode.COMMAND -> GuiCursorMode.NORMAL
    CommandState.Mode.VISUAL -> GuiCursorMode.VISUAL // TODO: VISUAL_EXCLUSIVE
    CommandState.Mode.SELECT -> GuiCursorMode.INSERT
    CommandState.Mode.INSERT -> GuiCursorMode.INSERT
    CommandState.Mode.OP_PENDING -> GuiCursorMode.OP_PENDING
    CommandState.Mode.REPLACE -> GuiCursorMode.REPLACE
    // This doesn't handle ci and cr, but we don't care - our CMD_LINE will never call this
    CommandState.Mode.CMD_LINE -> GuiCursorMode.CMD_LINE
    CommandState.Mode.INSERT_NORMAL -> GuiCursorMode.NORMAL
    CommandState.Mode.INSERT_VISUAL -> GuiCursorMode.VISUAL
    CommandState.Mode.INSERT_SELECT -> GuiCursorMode.INSERT
  }
}

/**
 * Allow the "use block caret" setting to override guicursor options - if set, we use block caret everywhere, if
 * not, we use guicursor options.
 *
 * Note that we look at the persisted value because for pre-212 at least, we modify the per-editor value.
 */
private fun isBlockCursorOverride() = EditorSettingsExternalizable.getInstance().isBlockCursor

private fun Editor.updatePrimaryCaretVisualAttributes() {
  caretModel.primaryCaret.visualAttributes = AttributesCache.getCaretVisualAttributes(this)

  // Make sure the caret is visible as soon as it's set. It might be invisible while blinking
  (this as? EditorEx)?.setCaretVisible(true)
}

private fun Editor.updateSecondaryCaretsVisualAttributes() {
  // IntelliJ simulates visual block with multiple carets with selections. Do our best to hide them
  val attributes = if (inBlockSubMode) HIDDEN else AttributesCache.getCaretVisualAttributes(this)
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
  private val cache = mutableMapOf<GuiCursorMode, CaretVisualAttributes>()

  fun getCaretVisualAttributes(editor: Editor): CaretVisualAttributes {
    if (isBlockCursorOverride()) {
      return BLOCK
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

  fun clear() = cache.clear()
}

@TestOnly
fun getGuiCursorMode(editor: Editor) = editor.guicursorMode()
