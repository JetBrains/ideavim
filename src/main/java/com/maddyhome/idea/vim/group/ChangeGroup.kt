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
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.impl.editorId
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
import com.maddyhome.idea.vim.group.format.FormatRemoteApi
import com.maddyhome.idea.vim.helper.CodeWrapper
import com.maddyhome.idea.vim.helper.CommentLeaderParser
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.key.KeyHandlerKeeper
import com.maddyhome.idea.vim.listener.VimInsertListener
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.ijOptions
import com.maddyhome.idea.vim.options.OptionAccessScope
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

  override fun processBackspace(editor: VimEditor, context: ExecutionContext) {
    injector.actionExecutor.executeAction(editor, name = IdeActions.ACTION_EDITOR_BACKSPACE, context = context)
    injector.scroll.scrollCaretIntoView(editor)
  }

  override fun initBlockInsert(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    append: Boolean,
  ): Boolean {
    // Block insert spans: enter insert at top-of-block (user types `#`) → `<Esc>`
    // triggers `repeatInsert` which replays across N-1 lines below. To group all
    // N lines into one undo step, start the mark here (before the top-line `#`
    // is typed) and finish it in `repeatInsert`.
    startVimUndoGroup(editor, "Vim Block Insert")
    blockInsertActive = true
    return try {
      super.initBlockInsert(editor, context, range, append)
    } catch (t: Throwable) {
      finishVimUndoGroup(editor)
      blockInsertActive = false
      throw t
    }
  }

  override fun repeatInsert(editor: VimEditor, context: ExecutionContext, count: Int, started: Boolean) {
    // Replay paths to group as one undo step:
    //   • Block insert: `initBlockInsert` already opened the mark — close it.
    //   • count > 1 (e.g. `5iHi <Esc>`) — open+close locally.
    // Plain `iHi <Esc>` (count <= 1, no block) is NOT a replay and must not be
    // wrapped — an empty start/finish group on the backend would swallow the
    // first `u` (the frontend speculative-undo flicker we observed earlier).
    // Dot-repeat (started=false) is wrapped one level up in `RepeatChangeAction`
    // so the wrap covers the *whole* replayed command (incl. `s`/`c`'s delete).
    when {
      blockInsertActive -> {
        try {
          super.repeatInsert(editor, context, count, started)
        } finally {
          finishVimUndoGroup(editor)
          blockInsertActive = false
        }
      }
      count > 1 -> withVimUndoGroup(editor, "Vim Repeat") {
        super.repeatInsert(editor, context, count, started)
      }
      else -> super.repeatInsert(editor, context, count, started)
    }
  }

  private var blockInsertActive: Boolean = false

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
