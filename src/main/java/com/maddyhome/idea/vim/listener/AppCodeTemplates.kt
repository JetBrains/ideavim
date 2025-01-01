/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.Key
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.visual.VimVisualTimer
import com.maddyhome.idea.vim.helper.fileSize
import com.maddyhome.idea.vim.helper.inVisualMode
import com.maddyhome.idea.vim.newapi.vim

/**
 * A collection of hacks to improve the interaction with fancy AppCode templates
 */
internal object AppCodeTemplates {
  private val facedAppCodeTemplate = Key.create<IntRange>("FacedAppCodeTemplate")

  private const val TEMPLATE_START = "<#T##"
  private const val TEMPLATE_END = "#>"

  class ActionListener : AnActionListener {

    private var editor: Editor? = null

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
      if (VimPlugin.isNotEnabled()) return

      val hostEditor = event.dataContext.getData(CommonDataKeys.HOST_EDITOR)
      if (hostEditor != null) {
        editor = hostEditor
      }
    }

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
      if (VimPlugin.isNotEnabled()) return

      if (ActionManager.getInstance().getId(action) == IdeActions.ACTION_CHOOSE_LOOKUP_ITEM) {
        val myEditor = editor
        if (myEditor != null) {
          VimVisualTimer.doNow()
          if (myEditor.inVisualMode) {
            injector.visualMotionGroup.toggleSelectVisual(myEditor.vim)
            KeyHandler.getInstance().partialReset(myEditor.vim)
          }
        }
      }
    }
  }

  @JvmStatic
  fun onMovement(
    editor: Editor,
    caret: Caret,
    toRight: Boolean,
  ) {
    val offset = caret.offset
    val offsetRightEnd = offset + TEMPLATE_START.length
    val offsetLeftEnd = offset - 1
    val templateRange = caret.getUserData(facedAppCodeTemplate)
    if (templateRange == null) {
      if (offsetRightEnd < editor.fileSize &&
        editor.document.charsSequence.subSequence(offset, offsetRightEnd).toString() == TEMPLATE_START
      ) {
        caret.shake()

        val templateEnd = editor.findTemplateEnd(offset) ?: return

        caret.putUserData(facedAppCodeTemplate, offset..templateEnd)
      }
      if (offsetLeftEnd >= 0 &&
        offset + 1 <= editor.fileSize &&
        editor.document.charsSequence.subSequence(offsetLeftEnd, offset + 1).toString() == TEMPLATE_END
      ) {
        caret.shake()

        val templateStart = editor.findTemplateStart(offsetLeftEnd) ?: return

        caret.putUserData(facedAppCodeTemplate, templateStart..offset)
      }
    } else {
      if (offset in templateRange) {
        if (toRight) {
          caret.moveToOffset(templateRange.last + 1)
        } else {
          caret.moveToOffset(templateRange.first)
        }
      }
      caret.putUserData(facedAppCodeTemplate, null)
      caret.shake()
    }
  }

  fun Editor.appCodeTemplateCaptured(): Boolean {
    return this.caretModel.allCarets.any { it.getUserData(facedAppCodeTemplate) != null }
  }

  private fun Caret.shake() {
    moveCaretRelatively(1, 0, false, false)
    moveCaretRelatively(-1, 0, false, false)
  }

  private fun Editor.findTemplateEnd(start: Int): Int? {
    val charSequence = this.document.charsSequence
    val length = charSequence.length
    for (i in start until length - 1) {
      if (charSequence[i] == TEMPLATE_END[0] && charSequence[i + 1] == TEMPLATE_END[1]) {
        return i + 1
      }
    }
    return null
  }

  private fun Editor.findTemplateStart(start: Int): Int? {
    val charSequence = this.document.charsSequence
    val templateLastIndex = TEMPLATE_START.length
    for (i in start downTo templateLastIndex) {
      if (charSequence.subSequence(i - templateLastIndex, i).toString() == TEMPLATE_START) {
        return i - templateLastIndex
      }
    }
    return null
  }
}
