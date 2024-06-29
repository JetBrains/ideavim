/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.listener

import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.LookupManagerListener
import com.intellij.codeInsight.lookup.impl.LookupImpl
import com.intellij.codeInsight.lookup.impl.actions.ChooseItemAction
import com.intellij.codeInsight.template.Template
import com.intellij.codeInsight.template.TemplateEditingAdapter
import com.intellij.codeInsight.template.TemplateManagerListener
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.find.FindModelListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.AnActionResult
import com.intellij.openapi.actionSystem.AnActionWrapper
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.ex.AnActionListener
import com.intellij.openapi.actionSystem.impl.ProxyShortcutSet
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.TextRange
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.inNormalMode
import com.maddyhome.idea.vim.vimscript.model.options.helpers.IdeaRefactorModeHelper
import com.maddyhome.idea.vim.vimscript.model.options.helpers.isIdeaRefactorModeKeep
import org.jetbrains.annotations.NonNls
import java.awt.event.KeyEvent
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */
internal object IdeaSpecifics {
  class VimActionListener : AnActionListener {
    @NonNls
    private val surrounderItems = listOf("if", "if / else", "for")
    private val surrounderAction =
      "com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler\$InvokeSurrounderAction"
    private var editor: Editor? = null
    private var completionPrevDocumentLength: Int? = null
    private var completionPrevDocumentOffset: Int? = null
    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
      if (VimPlugin.isNotEnabled()) return

      val hostEditor = event.dataContext.getData(CommonDataKeys.HOST_EDITOR)
      if (hostEditor != null) {
        editor = hostEditor
      }

      val isVimAction = (action as? AnActionWrapper)?.delegate is VimShortcutKeyAction
      if (!isVimAction && injector.globalIjOptions().trackactionids) {
        if (action !is NotificationService.ActionIdNotifier.CopyActionId && action !is NotificationService.ActionIdNotifier.StopTracking) {
          val id: String? = ActionManager.getInstance().getId(action) ?: (action.shortcutSet as? ProxyShortcutSet)?.actionId
          VimPlugin.getNotifications(event.dataContext.getData(CommonDataKeys.PROJECT)).notifyActionId(id)
        }
      }

      if (hostEditor != null && action is ChooseItemAction && injector.registerGroup.isRecording) {
        val lookup = LookupManager.getActiveLookup(hostEditor)
        if (lookup != null) {
          val charsToRemove = hostEditor.caretModel.primaryCaret.offset - lookup.lookupStart

          val register = VimPlugin.getRegister()
          val backSpace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
          repeat(charsToRemove) {
            register.recordKeyStroke(backSpace)
          }

          completionPrevDocumentLength = hostEditor.document.textLength - charsToRemove
          completionPrevDocumentOffset = lookup.lookupStart
        }
      }
    }

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
      if (VimPlugin.isNotEnabled()) return

      val editor = editor
      if (editor != null && action is ChooseItemAction && injector.registerGroup.isRecording) {
        val prevDocumentLength = completionPrevDocumentLength
        val prevDocumentOffset = completionPrevDocumentOffset

        if (prevDocumentLength != null && prevDocumentOffset != null) {
          val register = VimPlugin.getRegister()
          val addedTextLength = editor.document.textLength - prevDocumentLength
          val caretShift = addedTextLength - (editor.caretModel.primaryCaret.offset - prevDocumentOffset)
          val leftArrow = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)

          register.recordText(editor.document.getText(TextRange(prevDocumentOffset, prevDocumentOffset + addedTextLength)))
          repeat(caretShift.coerceAtLeast(0)) {
            register.recordKeyStroke(leftArrow)
          }
        }

        this.completionPrevDocumentLength = null
        this.completionPrevDocumentOffset = null
      }

      //region Enter insert mode after surround with if
      if (surrounderAction == action.javaClass.name && surrounderItems.any {
          action.templatePresentation.text.endsWith(
            it,
          )
        }
      ) {
        editor?.let {
          it.vim.mode = Mode.NORMAL()
          VimPlugin.getChange().insertBeforeCursor(it.vim, event.dataContext.vim)
          KeyHandler.getInstance().reset(it.vim)
        }
      }
      //endregion

      this.editor = null
    }
  }

  //region Enter insert mode for surround templates without selection
  class VimTemplateManagerListener : TemplateManagerListener {
    override fun templateStarted(state: TemplateState) {
      if (VimPlugin.isNotEnabled()) return
      val editor = state.editor ?: return

      state.addTemplateStateListener(object : TemplateEditingAdapter() {
        override fun currentVariableChanged(
          templateState: TemplateState,
          template: Template?,
          oldIndex: Int,
          newIndex: Int,
        ) {
          if (templateState.editor.vim.isIdeaRefactorModeKeep) {
            IdeaRefactorModeHelper.correctSelection(templateState.editor)
          }
        }
      })

      if (state.editor.vim.isIdeaRefactorModeKeep) {
        IdeaRefactorModeHelper.correctSelection(editor)
      } else {
        if (!editor.selectionModel.hasSelection()) {
          // Enable insert mode if there is no selection in template
          // Template with selection is handled by [com.maddyhome.idea.vim.group.visual.VisualMotionGroup.controlNonVimSelectionChange]
          if (editor.vim.inNormalMode) {
            VimPlugin.getChange().insertBeforeCursor(
              editor.vim,
              injector.executionContextManager.getEditorExecutionContext(editor.vim),
            )
            KeyHandler.getInstance().reset(editor.vim)
          }
        }
      }
    }
  }
  //endregion

  //region Register shortcuts for lookup and perform partial reset
  class LookupTopicListener : LookupManagerListener {
    override fun activeLookupChanged(oldLookup: Lookup?, newLookup: Lookup?) {
      if (VimPlugin.isNotEnabled()) return

      // Lookup opened
      if (oldLookup == null && newLookup is LookupImpl) {
        if (newLookup.editor.isIdeaVimDisabledHere) return

        VimPlugin.getKey().registerShortcutsForLookup(newLookup)
      }

      // Lookup closed
      if (oldLookup != null && newLookup == null) {
        val editor = oldLookup.editor
        if (editor.isIdeaVimDisabledHere) return
        // VIM-1858
        KeyHandler.getInstance().partialReset(editor.vim)
      }
    }
  }
  //endregion

  //region Hide Vim search highlights when showing IntelliJ search results
  class VimFindModelListener : FindModelListener {
    override fun findNextModelChanged() {
      if (VimPlugin.isNotEnabled()) return
      VimPlugin.getSearch().clearSearchHighlight()
    }
  }
  //endregion
}

//region Find action ID
internal class FindActionIdAction : DumbAwareToggleAction() {
  override fun isSelected(e: AnActionEvent): Boolean = injector.globalIjOptions().trackactionids

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    injector.globalIjOptions().trackactionids = !injector.globalIjOptions().trackactionids
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
//endregion
