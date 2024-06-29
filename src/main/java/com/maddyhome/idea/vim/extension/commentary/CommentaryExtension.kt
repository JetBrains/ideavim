/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.commentary

import com.intellij.codeInsight.actions.AsyncActionExecutionService
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.getLineEndOffset
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Argument
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.ex.ranges.toTextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.addCommand
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.PsiHelper
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import java.util.*

internal class CommentaryExtension : VimExtension {

  object Util {
    fun doCommentary(
      editor: VimEditor,
      context: ExecutionContext,
      range: TextRange,
      selectionType: SelectionType,
      resetCaret: Boolean = true,
    ): Boolean {
      val mode = editor.mode
      if (mode !is Mode.VISUAL) {
        editor.ij.selectionModel.setSelection(range.startOffset, range.endOffset)
      }

      return runWriteAction {
        // Treat block- and character-wise selections as block comments. Fall back if the first action isn't available
        val actions = if (selectionType === SelectionType.LINE_WISE) {
          listOf(IdeActions.ACTION_COMMENT_LINE, IdeActions.ACTION_COMMENT_BLOCK)
        } else {
          listOf(IdeActions.ACTION_COMMENT_BLOCK, IdeActions.ACTION_COMMENT_LINE)
        }

        val project = editor.ij.project!!
        val callback = { afterCommenting(mode, editor, resetCaret, range) }
        actions.any { executeActionWithCallbackOnSuccess(it, project, context, callback) }
      }
    }

    private fun executeActionWithCallbackOnSuccess(
      action: String,
      project: Project,
      context: ExecutionContext,
      callback: () -> Unit,
    ): Boolean {
      val res = Ref.create<Boolean>(false)
      AsyncActionExecutionService.getInstance(project).withExecutionAfterAction(
        action,
        { res.set(injector.actionExecutor.executeAction(action, context)) },
        { if (res.get()) callback() })
      return res.get()
    }

    private fun afterCommenting(
      mode: Mode,
      editor: VimEditor,
      resetCaret: Boolean,
      range: TextRange,
    ) {
      // Remove the selection, if we added it
      if (mode !is Mode.VISUAL) {
        editor.removeSelection()
      }

      // Put the caret back at the start of the range, as though it was moved by the operator's motion argument.
      // This is what Vim does. If IntelliJ is configured to add comments at the start of the line, this might put
      // the caret in the "wrong" place. E.g. gc_ should put the caret on the first non-whitespace character. This
      // is calculated by the motion, saved in the marks, and then we insert the comment. If it's inserted at the
      // first non-whitespace character, then the caret is in the right place. If it's inserted at the first column,
      // then the caret is now in a bit of a weird place. We can't detect this scenario, so we just have to accept
      // the difference
      // TODO: If we don't move the caret to the start offset, we should maintain the current logical position
      if (resetCaret) {
        editor.primaryCaret().moveToOffset(range.startOffset)
      }
    }
  }

  companion object {
    private const val OPERATOR_FUNC = "CommentaryOperatorFunc"
  }

  override fun getName() = "commentary"

  override fun init() {
    val plugCommentaryKeys = injector.parser.parseKeys("<Plug>Commentary")
    val plugCommentaryLineKeys = injector.parser.parseKeys("<Plug>CommentaryLine")
    putExtensionHandlerMapping(MappingMode.NX, plugCommentaryKeys, owner, CommentaryOperatorHandler(), false)
    putExtensionHandlerMapping(MappingMode.O, plugCommentaryKeys, owner, CommentaryMappingHandler(), false)
    putKeyMappingIfMissing(MappingMode.N, plugCommentaryLineKeys, owner, injector.parser.parseKeys("gc_"), true)

    putKeyMappingIfMissing(MappingMode.NXO, injector.parser.parseKeys("gc"), owner, plugCommentaryKeys, true)
    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys("gcc"), owner, plugCommentaryLineKeys, true)
    putKeyMappingIfMissing(
      MappingMode.N,
      injector.parser.parseKeys("gcu"),
      owner,
      injector.parser.parseKeys("<Plug>Commentary<Plug>Commentary"),
      true,
    )

    // Previous versions of IdeaVim used different mappings to Vim's Commentary. Make sure everything works if someone
    // is still using the old mapping
    putKeyMapping(MappingMode.N, injector.parser.parseKeys("<Plug>(CommentMotion)"), owner, plugCommentaryKeys, true)
    putKeyMapping(MappingMode.XO, injector.parser.parseKeys("<Plug>(CommentMotionV)"), owner, plugCommentaryKeys, true)
    putKeyMapping(MappingMode.N, injector.parser.parseKeys("<Plug>(CommentLine)"), owner, plugCommentaryLineKeys, true)

    addCommand("Commentary", CommentaryCommandAliasHandler())

    VimExtensionFacade.exportOperatorFunction(OPERATOR_FUNC, CommentaryOperatorFunction())
 }

  private class CommentaryOperatorFunction : OperatorFunction {
    // todo make it multicaret
    override fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType?): Boolean {
      val range = injector.markService.getChangeMarks(editor.primaryCaret()) ?: return false
      return Util.doCommentary(editor, context, range, selectionType ?: SelectionType.CHARACTER_WISE, true)
    }
  }

  /**
   * Sets up the operator, pending a motion
   *
   * E.g. handles the `gc` in `gc_`, by setting the operator function, then invoking `g@` to receive the `_` motion to
   * invoke the operator. This object is both the mapping handler and the operator function.
   */
  private class CommentaryOperatorHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      injector.globalOptions().operatorfunc = OPERATOR_FUNC
      executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
    }
  }

  private class CommentaryMappingHandler : ExtensionHandler {
    override val isRepeatable = true

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val command = Command(operatorArguments.count1, CommentaryTextObjectMotionHandler, Command.Type.MOTION, EnumSet.noneOf(CommandFlags::class.java))

      val keyState = KeyHandler.getInstance().keyHandlerState
      keyState.commandBuilder.completeCommandPart(Argument(command))
    }
  }

  /**
   * The text object handler that provides the motion in e.g. `dgc`
   *
   * This object is both the `<Plug>Commentary` mapping handler and the text object handler
   */
  private object CommentaryTextObjectMotionHandler : TextObjectActionHandler() {
    override val visualType: TextObjectVisualType = TextObjectVisualType.LINE_WISE

    override fun getRange(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      count: Int,
      rawCount: Int,
    ): TextRange? {
      val nativeEditor = (editor as IjVimEditor).editor
      val file = PsiHelper.getFile(nativeEditor) ?: return null
      val lastLine = editor.lineCount()

      var startLine = caret.getBufferPosition().line
      while (startLine > 0 && isCommentLine(file, nativeEditor, startLine - 1)) startLine--
      var endLine = caret.getBufferPosition().line - 1
      while (endLine < lastLine && isCommentLine(file, nativeEditor, endLine + 1)) endLine++

      if (startLine <= endLine) {
        val startOffset = editor.getLineStartOffset(startLine)
        val endOffset = editor.getLineStartOffset(endLine + 1)
        return TextRange(startOffset, endOffset)
      }

      return null
    }

    // Check all leaf nodes in the given line are whitespace, comments, or are owned by comments
    private fun isCommentLine(file: PsiFile, editor: Editor, logicalLine: Int): Boolean {
      val startOffset = editor.vim.getLineStartOffset(logicalLine)
      val endOffset = editor.vim.getLineEndOffset(logicalLine, true)
      val startElement = file.findElementAt(startOffset) ?: return false
      var next: PsiElement? = startElement
      while (next != null && next.textRange.startOffset <= endOffset) {
        if (next !is PsiWhiteSpace && !isComment(next)) {
          return false
        }
        next = PsiTreeUtil.nextLeaf(next, true)
      }

      return true
    }

    private fun isComment(element: PsiElement) =
      PsiTreeUtil.getParentOfType(element, PsiComment::class.java, false) != null
  }

  /**
   * The handler for the `Commentary` user defined command
   *
   * Used like `:1,3Commentary` or `g/fun/Commentary`
   */
  private class CommentaryCommandAliasHandler : CommandAliasHandler {
    override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
      Util.doCommentary(
        editor,
        context,
        range.getLineRange(editor, editor.primaryCaret()).toTextRange(editor),
        SelectionType.LINE_WISE,
        false
      )
    }
  }
}
