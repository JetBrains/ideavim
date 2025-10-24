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
import com.intellij.codeInsight.template.impl.TemplateImpl
import com.intellij.codeInsight.template.impl.TemplateState
import com.intellij.find.FindModelListener
import com.intellij.ide.actions.ApplyIntentionAction
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
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.util.TextRange
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.VimShortcutKeyAction
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.options
import com.maddyhome.idea.vim.group.NotificationService
import com.maddyhome.idea.vim.group.RegisterGroup
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.helper.exitSelectMode
import com.maddyhome.idea.vim.helper.exitVisualMode
import com.maddyhome.idea.vim.helper.hasVisualSelection
import com.maddyhome.idea.vim.helper.isIdeaVimDisabledHere
import com.maddyhome.idea.vim.newapi.globalIjOptions
import com.maddyhome.idea.vim.newapi.initInjector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService
import com.maddyhome.idea.vim.vimscript.model.options.helpers.IdeaRefactorModeHelper
import com.maddyhome.idea.vim.vimscript.model.options.helpers.isIdeaRefactorModeKeep
import com.maddyhome.idea.vim.vimscript.model.options.helpers.isIdeaRefactorModeSelect
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
    private var completionData: CompletionData? = null

    override fun beforeActionPerformed(action: AnAction, event: AnActionEvent) {
      if (VimPlugin.isNotEnabled()) return

      val hostEditor = event.dataContext.getData(CommonDataKeys.HOST_EDITOR)
      if (hostEditor != null) {
        editor = hostEditor
      }

      val isVimAction = (action as? AnActionWrapper)?.delegate is VimShortcutKeyAction
      if (!isVimAction && injector.vimState.mode == Mode.INSERT && action !is EnterAction) {
        val undoService = injector.undo as VimTimestampBasedUndoService
        val nanoTime = System.nanoTime()
        editor?.vim?.nativeCarets()?.forEach { undoService.endInsertSequence(it, it.offset, nanoTime) }
      }
      if (!isVimAction && injector.globalIjOptions().trackactionids) {
        if (action !is NotificationService.ActionIdNotifier.CopyActionId && action !is NotificationService.ActionIdNotifier.StopTracking) {
          val id: String? =
            ActionManager.getInstance().getId(action) ?: (action.shortcutSet as? ProxyShortcutSet)?.actionId
          val candidates = if (id == null) {
            // Some actions are specific to the component they're registered for, and are copies of a global action,
            // reusing the action ID and shortcuts (e.g. `NextTab` is different for editor tabs and tool window tabs).
            // Unfortunately, ActionManager doesn't know about these "local" actions, so can't return the action ID.
            // However, the new "local" action does copy the shortcuts of the global template action, so we can look up
            // all actions with matching shortcuts. We might return more action IDs than expected, so this is a list of
            // candidates, not a definite match of the action being executed, but the list should include our target
            // action. Note that we might return duplicate IDs because the keymap might have multiple shortcuts mapped
            // to the same action. The notifier will handle de-duplication and sorting as a presentation detail.
            action.shortcutSet.shortcuts.flatMap { KeymapManager.getInstance().activeKeymap.getActionIdList(it) }
          } else {
            emptyList()
          }
         val intentionName = if (action is ApplyIntentionAction) {
            action.name
          }
          else null

          // We can still get empty ID and empty candidates. Notably, for the tool window toggle buttons on the new UI.
          // We could filter out action events with `place == ActionPlaces.TOOLWINDOW_TOOLBAR_BAR`
          VimPlugin.getNotifications(event.dataContext.getData(CommonDataKeys.PROJECT)).notifyActionId(id, candidates, intentionName)
        }
      }

      if (hostEditor != null && action is ChooseItemAction && injector.registerGroup.isRecording) {
        val lookup = LookupManager.getActiveLookup(hostEditor)
        val lookupItem = lookup?.currentItem
        if (lookup is LookupImpl && lookupItem != null) {
          val caretOffset = hostEditor.caretModel.primaryCaret.offset
          val completionPrefixLength = lookup.itemMatcher(lookupItem).prefix.length + lookup.additionalPrefix.length
          val completionStartOffset = caretOffset - completionPrefixLength
          val documentLength = hostEditor.document.textLength
          val charsToRemove = caretOffset - completionStartOffset

          val register = VimPlugin.getRegister()

          if (charsToRemove > 0) {
            val backSpaceKey = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0)
            repeat(charsToRemove) {
              register.recordKeyStroke(backSpaceKey)
            }
          }

          val completionStartMarker = hostEditor.document.createRangeMarker(
            completionStartOffset,
            completionStartOffset
          ).apply {
            isGreedyToLeft = true
          }

          completionData = CompletionData(
            completionStartMarker,
            completionStartOffset,
            caretOffset,
            documentLength - charsToRemove
          )
        }
      }
    }

    override fun afterActionPerformed(action: AnAction, event: AnActionEvent, result: AnActionResult) {
      if (VimPlugin.isNotEnabled()) return

      val editor = editor
      if (editor != null && action is ChooseItemAction && injector.registerGroup.isRecording) {
        completionData?.recordCompletion(editor, VimPlugin.getRegister())
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
          VimPlugin.getChange().insertBeforeCaret(it.vim, event.dataContext.vim)
          KeyHandler.getInstance().reset(it.vim)
        }
      }
      //endregion

      this.editor = null

      this.completionData?.dispose()
      this.completionData = null
    }

    private data class CompletionData(
      val completionStartMarker: RangeMarker,
      val originalStartOffset: Int,
      val originalCaretOffset: Int,
      val originalDocumentLength: Int
    ) {
      fun recordCompletion(editor: Editor, register: RegisterGroup) {
        if (!completionStartMarker.isValid) {
          return
        }

        val completionStartOffset = completionStartMarker.startOffset
        val caretOffset = editor.caretModel.primaryCaret.offset
        val completedCharCount = editor.document.textLength - originalDocumentLength - (completionStartOffset - originalStartOffset)
        val completionEndOffset = completionStartOffset + completedCharCount

        val completedText = editor.document.getText(TextRange(
          completionStartOffset,
          completionEndOffset
        ))

        register.recordText(completedText)

        val caretShift = completedCharCount - (caretOffset - completionStartOffset)
        if (caretShift > 0) {
          val leftArrowKey = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0)
          repeat(caretShift) {
            register.recordKeyStroke(leftArrowKey)
          }
        }
      }

      fun dispose() {
        completionStartMarker.dispose()
      }
    }
  }

  //region Handle mode and selection for Live Templates and refactorings
  /**
   * Listen to template notifications to provide additional handling of templates, selection and Vim mode
   *
   * Most of the handling of templates is done by [IdeaSelectionControl]. When moving between editable segments of a
   * template, the editor will remove the current selection and add a selection for the text of the new segment, if any.
   * [IdeaSelectionControl] will be notified and handle Vim mode based on the `'idearefactormode'` option. This will
   * switch to Select mode for each variable by default (or Insert if there's no text) or Visual/Normal mode when the
   * option is set to "visual".
   *
   * Select mode makes IdeaVim behaviour a little more like a traditional editor. Inserting a Live Template is typically
   * done in Insert mode, so moving to the next editable segment always switches to Select mode, ready to continue
   * typing. At the end of the template, the caret is still in Insert mode, still ready to continue typing.
   *
   * The exception is for an "inline" template. This is an editable segment placed on top of existing text and is
   * typically used to rename a symbol. This is usually a refactoring started with an explicit action, and switching to
   * Select mode means the user is ready to start typing. However, accepting the change should switch back to Normal, as
   * the editing/refactoring is complete. This also helps when the refactoring shows a progress dialog, since Escape to
   * switch back to Normal can cancel the refactoring.
   *
   * Again, this is like a traditional editor, and still only changes Vim mode based on an explicit action.
   *
   * This class handles the following edge cases that [IdeaSelectionControl] cannot:
   * * When `'idearefactormode'` is "keep" it will maintain the current Vim mode, by removing the current selection
   *   (so [IdeaSelectionControl] does nothing). It also ensures that the current Vim selection mode matches the actual
   *   selection (i.e., character wise vs line wise).
   * * When the editor is moving to the next segment but there is no current selection and the next segment is empty,
   *   there will be no change in selection and [IdeaSelectionControl] will not be notified. This class will switch to
   *   Insert mode when `'idearefactormode'` is set to "select" and Normal when it's "visual".
   * * A special case of the above scenario is moving to the end of the template, which always has no selection. If
   *   there is no current selection [IdeaSelectionControl] is not called and the mode is not updated, so we stay in
   *   whatever mode the user had last - Insert, Normal, whatever. When there is a selection, [IdeaSelectionControl]
   *   will be called, but since there is no template active anymore, it would set the mode to Normal. This class will
   *   switch to Insert when `'idearefactormode'` is "select" and Normal for "visual". It does nothing for "keep".
   * * If the template is an "inline" template, it is typically a rename refactoring on existing text.
   *   When ending the template and `'idearefactormode'` is "select", the above would leave is in Insert mode. This
   *   class will switch to Normal for inline templates, for both "select" and "visual". It does nothing for "keep".
   */
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
          fun VimEditor.exitMode() = when (this.mode) {
            is Mode.SELECT -> this.exitSelectMode(adjustCaretPosition = false)
            is Mode.VISUAL -> this.exitVisualMode()
            is Mode.INSERT -> this.exitInsertMode(injector.executionContextManager.getEditorExecutionContext(this))
            else -> Unit
          }

          fun Template?.myIsInline() = this is TemplateImpl && this.isInline

          val vimEditor = editor.vim

          // This function is called when moving between variables. It is called with oldIndex == -1 when moving to the
          // first variable, and newIndex == -1 just before ending, when moving to the end of the template text, or to
          // $END$ (which is treated as a segment, but not a variable). If there are no variables, it is called with
          // oldIndex == newIndex == -1.
          if (vimEditor.isIdeaRefactorModeKeep) {
            IdeaRefactorModeHelper.correctEditorSelection(templateState.editor)
          }
          else {
            // The editor places the caret at the exclusive end of the variable. For Visual, unless we've enabled
            // exclusive selection, move it to the inclusive end.
            // Note that "keep" does this as part of IdeaRefactorModeHelper
            if (editor.selectionModel.hasSelection()
              && !editor.vim.isIdeaRefactorModeSelect
              && templateState.currentVariableRange?.endOffset == editor.caretModel.offset
              && !injector.options(vimEditor).selection.contains("exclusive")
            ) {
              vimEditor.primaryCaret()
                .moveToInlayAwareOffset((editor.selectionModel.selectionEnd - 1).coerceAtLeast(editor.selectionModel.selectionStart))
            }

            if (newIndex == -1 && template.myIsInline() && vimEditor.isIdeaRefactorModeSelect) {
              // Rename refactoring has just completed with 'idearefactormode' in "select". Return to Normal instead of
              // our default behaviour of switching to Insert
              if (vimEditor.mode !is Mode.NORMAL) {
                vimEditor.exitMode()
                vimEditor.mode = Mode.NORMAL()
              }
            } else {
              // IdeaSelectionControl will not be called if we're moving to a new variable with no change in selection.
              // And if we're moving to the end of the template, the change in selection will reset us to Normal because
              // IdeaSelectionControl will be called when the template is no longer active.
              if ((!editor.selectionModel.hasSelection() && !vimEditor.mode.hasVisualSelection) || newIndex == -1) {
                if (vimEditor.isIdeaRefactorModeSelect) {
                  if (vimEditor.mode !is Mode.INSERT) {
                    vimEditor.exitMode()
                    injector.application.runReadAction {
                      val context = injector.executionContextManager.getEditorExecutionContext(editor.vim)
                      VimPlugin.getChange().insertBeforeCaret(editor.vim, context)
                    }
                  }
                } else {
                  vimEditor.mode = Mode.NORMAL()
                }
              }
            }
          }
        }
      })
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
  override fun isSelected(e: AnActionEvent): Boolean {
    initInjector()
    return injector.globalIjOptions().trackactionids
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    initInjector()
    injector.globalIjOptions().trackactionids = !injector.globalIjOptions().trackactionids
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}
//endregion
