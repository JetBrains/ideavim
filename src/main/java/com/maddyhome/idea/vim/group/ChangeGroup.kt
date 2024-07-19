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
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.actions.EnterAction
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.util.UserDataHolder
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiUtilBase
import com.intellij.util.containers.ContainerUtil
import com.maddyhome.idea.vim.EventFacade
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimChangeGroupBase
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.VimMotionGroupBase
import com.maddyhome.idea.vim.api.getLineEndForOffset
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.getLineStartForOffset
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.api.lineLength
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.common.IndentConfig.Companion.create
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.group.visual.vimSetSystemSelectionSilently
import com.maddyhome.idea.vim.handler.commandContinuation
import com.maddyhome.idea.vim.helper.CharacterHelper
import com.maddyhome.idea.vim.helper.CharacterHelper.charType
import com.maddyhome.idea.vim.helper.EditorHelper
import com.maddyhome.idea.vim.helper.inInsertMode
import com.maddyhome.idea.vim.helper.moveToInlayAwareLogicalPosition
import com.maddyhome.idea.vim.key.KeyHandlerKeeper.Companion.getInstance
import com.maddyhome.idea.vim.listener.VimInsertListener
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.annotations.TestOnly
import java.util.function.Consumer
import kotlin.math.max
import kotlin.math.min

/**
 * Provides all the insert/replace related functionality
 */
class ChangeGroup : VimChangeGroupBase() {
  private val insertListeners = ContainerUtil.createLockFreeCopyOnWriteList<VimInsertListener>()
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
    val nanoTime = System.nanoTime()
    vimEditor.forEachCaret { undo.startInsertSequence(it, it.offset, nanoTime) }
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

  override fun blockInsert(
    editor: VimEditor,
    context: ExecutionContext,
    range: TextRange,
    append: Boolean,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val lines = getLinesCountInVisualBlock(editor, range)
    val startPosition = editor.offsetToBufferPosition(range.startOffset)
    val mode = operatorArguments.mode
    val visualBlockMode = mode is VISUAL && mode.selectionType === SelectionType.BLOCK_WISE
    for (caret in editor.carets()) {
      val line = startPosition.line
      var column = startPosition.column
      if (!visualBlockMode) {
        column = 0
      } else if (append) {
        column += range.maxLength
        if (caret.vimLastColumn == VimMotionGroupBase.LAST_COLUMN) {
          column = VimMotionGroupBase.LAST_COLUMN
        }
      }
      val lineLength = editor.lineLength(line)
      if (column < VimMotionGroupBase.LAST_COLUMN && lineLength < column) {
        val pad = EditorHelper.pad((editor as IjVimEditor).editor, line, column)
        val offset = editor.getLineEndOffset(line)
        insertText(editor, caret, offset, pad)
      }
      if (visualBlockMode || !append) {
        (caret as IjVimCaret).caret.moveToInlayAwareLogicalPosition(LogicalPosition(line, column))
      }
      if (visualBlockMode) {
        setInsertRepeat(lines, column, append)
      }
    }
    if (visualBlockMode || !append) {
      insertBeforeCursor(editor, context)
    } else {
      insertAfterCursor(editor, context)
    }
    return true
  }

  private fun restoreCursor(editor: VimEditor, caret: VimCaret, startLine: Int) {
    if (caret != editor.primaryCaret()) {
      (editor as IjVimEditor).editor.caretModel.addCaret(
        editor.editor.offsetToVisualPosition(injector.motion.moveCaretToLineStartSkipLeading(editor, startLine)), false
      )
    }
  }

  override fun reformatCodeRange(editor: VimEditor, caret: VimCaret, range: TextRange): Boolean {
    val starts = range.startOffsets
    val ends = range.endOffsets
    val firstLine = editor.offsetToBufferPosition(range.startOffset).line
    for (i in ends.indices.reversed()) {
      val startOffset = editor.getLineStartForOffset(starts[i])
      val offset = ends[i] - if (startOffset == ends[i]) 0 else 1
      val endOffset = editor.getLineEndForOffset(offset)
      reformatCode(editor, startOffset, endOffset)
    }
    val newOffset = injector.motion.moveCaretToLineStartSkipLeading(editor, firstLine)
    caret.moveToOffset(newOffset)
    return true
  }

  private fun reformatCode(editor: VimEditor, start: Int, end: Int) {
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
    var primaryTextAndTransferableData: Pair<String, List<Any>?>? = null
    try {
      if (injector.registerGroup.isPrimaryRegisterSupported()) {
        primaryTextAndTransferableData = injector.clipboardManager.getPrimaryTextAndTransferableData()
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
      if (primaryTextAndTransferableData != null) {
        injector.clipboardManager.setPrimaryText(
          primaryTextAndTransferableData.first,
          primaryTextAndTransferableData.first,
          primaryTextAndTransferableData.second ?: emptyList()
        )
      }
    } catch (e: Exception) {
      // FIXME: [isPrimaryRegisterSupported()] is not implemented perfectly, so there might be thrown an exception after trying to access the primary selection
    }
  }

  override fun indentRange(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    range: TextRange,
    count: Int,
    dir: Int,
    operatorArguments: OperatorArguments,
  ) {
    if (logger.isDebugEnabled) {
      logger.debug("count=$count")
    }

    // Remember the current caret column
    val intendedColumn = caret.vimLastColumn
    val indentConfig = create((editor as IjVimEditor).editor)
    val sline = editor.offsetToBufferPosition(range.startOffset).line
    val endLogicalPosition = editor.offsetToBufferPosition(range.endOffset)
    val eline = if (endLogicalPosition.column == 0) max((endLogicalPosition.line - 1).toDouble(), 0.0)
      .toInt() else endLogicalPosition.line
    if (range.isMultiple) {
      val from = editor.offsetToBufferPosition(range.startOffset).column
      if (dir == 1) {
        // Right shift blockwise selection
        val indent = indentConfig.createIndentByCount(count)
        for (l in sline..eline) {
          val len = editor.lineLength(l)
          if (len > from) {
            val spos = BufferPosition(l, from, false)
            insertText(editor, caret, spos, indent)
          }
        }
      } else {
        // Left shift blockwise selection
        val chars = editor.text()
        for (l in sline..eline) {
          val len = editor.lineLength(l)
          if (len > from) {
            val spos = BufferPosition(l, from, false)
            val epos = BufferPosition(l, from + indentConfig.getTotalIndent(count) - 1, false)
            val wsoff = editor.bufferPositionToOffset(spos)
            val weoff = editor.bufferPositionToOffset(epos)
            var pos: Int
            pos = wsoff
            while (pos <= weoff) {
              if (charType(editor, chars[pos], false) !== CharacterHelper.CharacterType.WHITESPACE) {
                break
              }
              pos++
            }
            if (pos > wsoff) {
              deleteText(editor, TextRange(wsoff, pos), null, caret, operatorArguments, true)
            }
          }
        }
      }
    } else {
      // Shift non-blockwise selection
      for (l in sline..eline) {
        val soff = editor.getLineStartOffset(l)
        val eoff = editor.getLineEndOffset(l, true)
        val woff = injector.motion.moveCaretToLineStartSkipLeading(editor, l)
        val col = editor.offsetToBufferPosition(woff).column
        val limit = max(0.0, (col + dir * indentConfig.getTotalIndent(count)).toDouble())
          .toInt()
        if (col > 0 || soff != eoff) {
          val indent = indentConfig.createIndentBySize(limit)
          replaceText(editor, caret, soff, woff, indent)
        }
      }
    }
    if (!editor.editor.inInsertMode) {
      if (!range.isMultiple) {
        // The caret has moved, so reset the intended column before trying to get the expected offset
        val newCaret = caret.setVimLastColumnAndGetCaret(intendedColumn)
        val offset = injector.motion.moveCaretToLineWithStartOfLineOption(editor, sline, caret)
        newCaret.moveToOffset(offset)
      } else {
        caret.moveToOffset(range.startOffset)
      }
    }
  }

  override fun reset() {
    strokes.clear()
    repeatCharsCount = 0
    if (lastStrokes != null) {
      lastStrokes!!.clear()
    }
  }

  override fun saveStrokes(newStrokes: String?) {
    val chars = newStrokes!!.toCharArray()
    strokes.add(chars)
  }

  fun addInsertListener(listener: VimInsertListener) {
    insertListeners.add(listener)
  }

  fun removeInsertListener(listener: VimInsertListener) {
    insertListeners.remove(listener)
  }

  override fun notifyListeners(editor: VimEditor) {
    insertListeners.forEach(Consumer { listener: VimInsertListener -> listener.insertModeStarted((editor as IjVimEditor).editor) })
  }

  @TestOnly
  override fun resetRepeat() {
    setInsertRepeat(0, 0, false)
  }

  private companion object {
    private val logger = logger<ChangeGroup>()
  }
}
