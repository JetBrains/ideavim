/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.commentary

import com.intellij.openapi.editor.impl.editorId
import com.intellij.vim.api.VimInitApi
import com.maddyhome.idea.vim.KeyHandler
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
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
import com.maddyhome.idea.vim.group.comment.CommentaryRemoteApi
import com.maddyhome.idea.vim.group.rpc
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType

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
      val ijEditor = editor.ij
      val document = ijEditor.document

      val editorId = ijEditor.editorId()
      val caretOffset = if (resetCaret) range.startOffset else -1
      if (selectionType === SelectionType.LINE_WISE) {
        val startLine = document.getLineNumber(range.startOffset)
        var endLine = document.getLineNumber(range.endOffset)
        // Adjust endLine if the range ends at the start of a line (don't include that line)
        if (endLine > startLine && document.getLineStartOffset(endLine) == range.endOffset) {
          endLine--
        }
        val finalEndLine = endLine
        rpc(ijEditor.project) {
          CommentaryRemoteApi.getInstance().toggleLineComment(editorId, startLine, finalEndLine, caretOffset)
        }
      } else {
        rpc(ijEditor.project) {
          CommentaryRemoteApi.getInstance()
            .toggleBlockComment(editorId, range.startOffset, range.endOffset, caretOffset)
        }
      }
      return true
    }
  }

  companion object {
    private const val OPERATOR_FUNC = "CommentaryOperatorFunc"
  }

  override fun getName() = "commentary"

  override fun init(initApi: VimInitApi) {
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
      val keyState = KeyHandler.getInstance().keyHandlerState
      keyState.commandBuilder.addAction(CommentaryTextObjectMotionHandler)
    }
  }

  /**
   * The text object handler that provides the motion in e.g. `dgc`
   *
   * Delegates to [VimPsiService.getCommentBlockRange][com.maddyhome.idea.vim.api.VimPsiService.getCommentBlockRange]
   * which uses PSI on the backend to detect contiguous comment lines.
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
      return injector.psiService.getCommentBlockRange(editor, caret.getBufferPosition().line)
    }
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
