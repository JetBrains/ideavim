/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.group

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.UndoConfirmationPolicy
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.actionSystem.TypedActionHandler
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.impl.editorId
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.Options
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.change.ChangeRemoteApi
import com.maddyhome.idea.vim.group.format.FormatRemoteApi
import com.maddyhome.idea.vim.handler.commandContinuation
import com.maddyhome.idea.vim.helper.CodeWrapper
import com.maddyhome.idea.vim.helper.CommentLeaderParser
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.key.KeyHandlerKeeper
import com.maddyhome.idea.vim.listener.VimInsertListener
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.ijOptions
import com.maddyhome.idea.vim.options.OptionAccessScope
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.undo.VimKeyBasedUndoService
import com.maddyhome.idea.vim.undo.VimTimestampBasedUndoService

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

  fun editorCreated(editor: VimEditor, disposable: Disposable) {
    EventFacade.getInstance().addEditorMouseListener(editor.ij, listener, disposable)
  }

  override fun type(vimEditor: VimEditor, context: ExecutionContext, key: Char) {
    doType(vimEditor, context) {
      it.execute(vimEditor.ij, key, context.ij)
    }
  }

  override fun type(vimEditor: VimEditor, context: ExecutionContext, string: String) {
    doType(vimEditor, context) { handler ->
      string.forEach { char ->
        handler.execute(vimEditor.ij, char, context.ij)
      }
    }
  }

  private fun doType(vimEditor: VimEditor, context: ExecutionContext, action: (TypedActionHandler) -> Unit) {
    val editor = (vimEditor as IjVimEditor).editor
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
        ApplicationManager.getApplication().runWriteAction {
          action(KeyHandlerKeeper.getInstance().originalHandler)
        }
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

  override fun repeatInsertText(editor: VimEditor, context: ExecutionContext, count: Int) {
    if (count <= 0) return

    val ijEditor = (editor as IjVimEditor).editor
    val editorId = ijEditor.editorId()

    // Register start/finish undo marks on the backend via RPC so that all
    // replayed strokes (text insertions + actions like backspace) are grouped
    // into a single undo step on both frontend and backend.
    // We cannot use StartMarkAction locally because the UndoSpy/CmdMeta
    // pipeline does not reliably transmit marks to the backend.
    rpcSplitModeOnly(ijEditor.project) { ChangeRemoteApi.getInstance().startUndoMark(editorId, "Vim Repeat") }
    try {
      super.repeatInsertText(editor, context, count)
    } finally {
      rpcSplitModeOnly(ijEditor.project) { ChangeRemoteApi.getInstance().finishUndoMark(editorId) }
    }
  }

  override fun reformatCode(editor: VimEditor, start: Int, end: Int) {
    val project = (editor as IjVimEditor).editor.project ?: return
    val file = PsiUtilBase.getPsiFileInEditor(editor.editor, project) ?: return
    val textRange = com.intellij.openapi.util.TextRange.create(start, end)
    injector.application.runWriteAction {
      CodeStyleManager.getInstance(project).reformatText(file, listOf(textRange))
    }
    wrapText(editor, start, end)
  }

  private fun wrapText(editor: IjVimEditor, start: Int, end: Int) {
    val textwidth = injector.ijOptions(editor).textwidth
    if (textwidth <= 0) {
      return
    }
    wrapTextToWidth(editor, start, end, textwidth)
  }

  private fun wrapTextToWidth(editor: IjVimEditor, start: Int, end: Int, width: Int) {
    val ijEditor = editor.editor
    val document = ijEditor.document

    val text = document.getText(com.intellij.openapi.util.TextRange.create(start, end))
    val commentsValue = injector.optionGroup
      .getOptionValue(Options.comments, OptionAccessScope.LOCAL(editor))
      .value
    val wrapper = CodeWrapper(
      width = width,
      tabWidth = ijEditor.settings.getTabSize(ijEditor.project),
      leaders = CommentLeaderParser.parse(commentsValue),
    )
    val wrapped = wrapper.wrap(text)

    if (wrapped == text) {
      return
    }

    injector.application.runWriteAction {
      document.replaceString(start, end, wrapped)
    }
  }

  override fun autoIndentRange(
    editor: VimEditor,
    context: ExecutionContext,
    ranges: List<TextRange>,
    carets: List<VimCaret>,
  ) {
    val ijEditor = (editor as IjVimEditor).editor
    rpc(ijEditor.project) {
      FormatRemoteApi.getInstance().format(
        ijEditor.editorId(),
        ranges.map { it.startOffset },
        ranges.map { it.endOffset },
      )
    }

    for ((caret, range) in carets.zip(ranges)) {
      moveCaretToFirstNonBlank(editor, range, caret)
    }
  }

  private fun moveCaretToFirstNonBlank(
    editor: IjVimEditor,
    range: TextRange,
    caret: VimCaret,
  ) {
    val firstLine = editor.offsetToBufferPosition(range.startOffset).line
    val newOffset = injector.motion.moveCaretToLineStartSkipLeading(editor, firstLine)
    caret.moveToOffset(newOffset)
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
