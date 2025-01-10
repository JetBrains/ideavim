/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.codeInsight.actions.AsyncActionExecutionService.Companion.getInstance
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.handler.commandContinuation
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.key.KeyHandlerKeeper.Companion.getInstance
import com.maddyhome.idea.vim.listener.VimInsertListener
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimCopiedText
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService
import kotlin.math.min

/**
 * Provides all the insert/replace related functionality
 */
class ChangeGroup : VimChangeGroupBase() {
  private val listener: EditorMouseListener = object : EditorMouseListener {
    override fun mouseClicked(event: EditorMouseEvent) {
      val editor = event.editor
      if (editor.inInsertMode) {
        clearStrokes(IjVimEditor(editor))
      }
    }
  }

  fun editorCreated(editor: Editor?, disposable: Disposable) {
    EventFacade.getInstance().addEditorMouseListener(editor!!, listener, disposable)
  }

  override fun type(vimEditor: VimEditor, context: ExecutionContext, key: Char) {
    val editor = (vimEditor as IjVimEditor).editor
    val ijContext = context.ij
    val doc = vimEditor.editor.document

    val undo = injector.undo
    when (undo) {
      is VimKeyBasedUndoService -> undo.setInsertNonMergeUndoKey()
      is VimTimestampBasedUndoService -> {
        val nanoTime = System.nanoTime()
        vimEditor.forEachCaret { undo.startInsertSequence(it, it.offset, nanoTime) }
      }
    }
    CommandProcessor.getInstance().executeCommand(
      editor.project, {
        ApplicationManager.getApplication()
          .runWriteAction { getInstance().originalHandler.execute(editor, key, ijContext) }
      }, "", doc,
      UndoConfirmationPolicy.DEFAULT, doc
    )
    injector.scroll.scrollCaretIntoView(vimEditor)
  }

  /**
   * If this is REPLACE mode we need to turn off OVERWRITE before and then turn OVERWRITE back on after sending the
   * "ENTER" key.
   */
  override fun processEnter(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
  ) {
    if (editor.mode is Mode.REPLACE) {
      editor.insertMode = true
    }
    try {
      val continuation = (context.context as UserDataHolder).getUserData(commandContinuation)
      val ijEditor = editor.ij
      val ij = context.ij
      val ijCaret = caret.ij
      if (continuation != null) {
        continuation.execute(ijEditor, ijCaret, ij)
      } else {
        EnterAction().handler.execute(ijEditor, ijCaret, ij)
      }
    } finally {
      if (editor.mode is Mode.REPLACE) {
        editor.insertMode = false
      }
    }
  }

  override fun processBackspace(editor: VimEditor, context: ExecutionContext) {
    injector.actionExecutor.executeAction(editor, name = IdeActions.ACTION_EDITOR_BACKSPACE, context = context)
    injector.scroll.scrollCaretIntoView(editor)
  }

  private fun restoreCursor(editor: VimEditor, caret: VimCaret, startLine: Int) {
    if (caret != editor.primaryCaret()) {
      (editor as IjVimEditor).editor.caretModel.addCaret(
        editor.editor.offsetToVisualPosition(injector.motion.moveCaretToLineStartSkipLeading(editor, startLine)), false
      )
    }
  }

  override fun reformatCode(editor: VimEditor, start: Int, end: Int) {
    val project = (editor as IjVimEditor).editor.project ?: return
    val file = PsiUtilBase.getPsiFileInEditor(editor.editor, project) ?: return
    val textRange = com.intellij.openapi.util.TextRange.create(start, end)
    CodeStyleManager.getInstance(project).reformatText(file, listOf(textRange))
  }

  override fun autoIndentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
  ) {
    val startOffset = editor.getLineStartForOffset(range.startOffset)
    val endOffset = editor.getLineEndForOffset(range.endOffset)
    val ijEditor = (editor as IjVimEditor).editor

    // FIXME: Here we do selection, and it is not a good idea, because it updates primary selection in Linux
    // FIXME: I'll leave here a dirty fix that restores primary selection, but it would be better to rewrite this method
    var copiedText: IjVimCopiedText? = null
    try {
      if (injector.registerGroup.isPrimaryRegisterSupported()) {
        copiedText = injector.clipboardManager.getPrimaryContent(editor, context) as IjVimCopiedText
      }
    } catch (e: Exception) {
      // FIXME: [isPrimaryRegisterSupported()] is not implemented perfectly, so there might be thrown an exception after trying to access the primary selection
      logger.warn("False positive X11 primary selection support")
    }
    ijEditor.selectionModel.vimSetSystemSelectionSilently(startOffset, endOffset)
    val project = ijEditor.project
    val actionExecution = {
      val joinLinesAction = injector.nativeActionManager.indentLines
      if (joinLinesAction != null) {
        injector.actionExecutor.executeAction(editor, joinLinesAction, context)
      }
    }
    val afterAction = {
      val firstLine = editor.offsetToBufferPosition(
        min(startOffset.toDouble(), endOffset.toDouble()).toInt()
      ).line
      val newOffset = injector.motion.moveCaretToLineStartSkipLeading(editor, firstLine)
      caret.moveToOffset(newOffset)
      restoreCursor(editor, caret, (caret as IjVimCaret).caret.logicalPosition.line)
    }
    if (project != null) {
      getInstance(project)
        .withExecutionAfterAction(IdeActions.ACTION_EDITOR_AUTO_INDENT_LINES, actionExecution, afterAction)
    } else {
      actionExecution.invoke()
      afterAction.invoke()
    }
    try {
      if (copiedText != null) {
        injector.clipboardManager.setPrimaryContent(editor, context, copiedText)
      }
    } catch (e: Exception) {
      // FIXME: [isPrimaryRegisterSupported()] is not implemented perfectly, so there might be thrown an exception after trying to access the primary selection
    }
  }

  @Deprecated(
    message = "Please use listenersNotifier",
    replaceWith = ReplaceWith(
      "injector.listenersNotifier.modeChangeListeners.add",
      imports = ["import com.maddyhome.idea.vim.api.injector"]
    )
  )
  fun addInsertListener(listener: VimInsertListener) {
    injector.listenersNotifier.modeChangeListeners.add(listener)
  }

  @Deprecated(
    message = "Please use listenersNotifier",
    replaceWith = ReplaceWith(
      "injector.listenersNotifier.modeChangeListeners.remove",
      imports = ["import com.maddyhome.idea.vim.api.injector"]
    )
  )
  fun removeInsertListener(listener: VimInsertListener) {
    injector.listenersNotifier.modeChangeListeners.remove(listener)
  }

  private companion object {
    private val logger = logger<ChangeGroup>()
  }
}
