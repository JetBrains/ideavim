/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.textobjindent

import com.intellij.openapi.editor.Caret
import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.state.mode.Mode.OP_PENDING
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import kotlin.math.max

/**
 * Port of vim-indent-object:
 * [vim-indent-object](https://github.com/michaeljsmith/vim-indent-object)
 * 
 * 
 * 
 * vim-indent-object provides these text objects based on the cursor line's indentation:
 * 
 *  * `ai` **A**n **I**ndentation level and line above.
 *  * `ii` **I**nner **I**ndentation level (no line above).
 *  * `aI` **A**n **I**ndentation level and lines above and below.
 *  * `iI` **I**nner **I**ndentation level (no lines above and below). Synonym of `ii`
 * 
 * 
 * See also the reference manual for more details at:
 * [indent-object.txt](https://github.com/michaeljsmith/vim-indent-object/blob/master/doc/indent-object.txt)
 */
class VimIndentObject : VimExtension {
  override fun getName(): String {
    return "textobj-indent"
  }

  override fun init() {
    putExtensionHandlerMapping(
      MappingMode.XO, injector.parser.parseKeys("<Plug>textobj-indent-ai"), getOwner(),
      IndentObject(true, false), false
    )
    putExtensionHandlerMapping(
      MappingMode.XO, injector.parser.parseKeys("<Plug>textobj-indent-aI"), getOwner(),
      IndentObject(true, true), false
    )
    putExtensionHandlerMapping(
      MappingMode.XO, injector.parser.parseKeys("<Plug>textobj-indent-ii"), getOwner(),
      IndentObject(false, false), false
    )

    putKeyMapping(
      MappingMode.XO,
      injector.parser.parseKeys("ai"),
      getOwner(),
      injector.parser.parseKeys("<Plug>textobj-indent-ai"),
      true
    )
    putKeyMapping(
      MappingMode.XO,
      injector.parser.parseKeys("aI"),
      getOwner(),
      injector.parser.parseKeys("<Plug>textobj-indent-aI"),
      true
    )
    putKeyMapping(
      MappingMode.XO,
      injector.parser.parseKeys("ii"),
      getOwner(),
      injector.parser.parseKeys("<Plug>textobj-indent-ii"),
      true
    )
  }

  internal class IndentObject(val includeAbove: Boolean, val includeBelow: Boolean) : ExtensionHandler {
    override val isRepeatable: Boolean
      get() = false

    internal class IndentObjectHandler(val includeAbove: Boolean, val includeBelow: Boolean) :
      TextObjectActionHandler() {
      override fun getRange(
        editor: VimEditor,
        caret: ImmutableVimCaret,
        context: ExecutionContext,
        count: Int,
        rawCount: Int,
      ): TextRange {
        val charSequence = (editor as IjVimEditor).editor.getDocument().getCharsSequence()
        val caretOffset = (caret as IjVimCaret).caret.getOffset()

        // Part 1: Find the start of the caret line.
        var caretLineStartOffset = caretOffset
        var accumulatedWhitespace = 0
        while (--caretLineStartOffset >= 0) {
          val ch = charSequence.get(caretLineStartOffset)
          if (ch == ' ' || ch == '\t') {
            ++accumulatedWhitespace
          } else if (ch == '\n') {
            ++caretLineStartOffset
            break
          } else {
            accumulatedWhitespace = 0
          }
        }
        if (caretLineStartOffset < 0) {
          caretLineStartOffset = 0
        }

        // `caretLineStartOffset` points to the first character in the line where the caret is located.

        // Part 2: Compute the indentation level of the caret line.
        // This is done as a separate step so that it works even when the caret is inside the indentation.
        var offset = caretLineStartOffset
        var indentSize = 0
        while (offset < charSequence.length) {
          val ch = charSequence.get(offset)
          if (ch == ' ' || ch == '\t') {
            ++indentSize
            ++offset
          } else {
            break
          }
        }

        // `indentSize` contains the amount of indent to be used for the text object range to be returned.
        var upperBoundaryOffset: Int? = null
        // Part 3: Find a line above the caret line, that has an indentation lower than `indentSize`.
        var pos1 = caretLineStartOffset - 1
        var isUpperBoundaryFound = false
        while (upperBoundaryOffset == null) {
          // 3.1: Going backwards from `caretLineStartOffset`, find the first non-whitespace character.
          while (--pos1 >= 0) {
            val ch = charSequence.get(pos1)
            if (ch != ' ' && ch != '\t' && ch != '\n') {
              break
            }
          }
          // 3.2: Find the indent size of the line with this non-whitespace character and check against `indentSize`.
          accumulatedWhitespace = 0
          while (--pos1 >= 0) {
            val ch = charSequence.get(pos1)
            if (ch == ' ' || ch == '\t') {
              ++accumulatedWhitespace
            } else if (ch == '\n') {
              if (accumulatedWhitespace < indentSize) {
                upperBoundaryOffset = pos1 + 1
                isUpperBoundaryFound = true
              }
              break
            } else {
              accumulatedWhitespace = 0
            }
          }
          if (pos1 < 0) {
            // Reached start of the buffer.
            upperBoundaryOffset = 0
            isUpperBoundaryFound = accumulatedWhitespace < indentSize
          }
        }

        // Now `upperBoundaryOffset` marks the beginning of an `ai` text object.
        if (isUpperBoundaryFound && !includeAbove) {
          while (++upperBoundaryOffset < charSequence.length) {
            val ch = charSequence.get(upperBoundaryOffset)
            if (ch == '\n') {
              ++upperBoundaryOffset
              break
            }
          }
          while (charSequence.get(upperBoundaryOffset) == '\n') {
            ++upperBoundaryOffset
          }
        }

        // Part 4: Find the start of the caret line.
        var caretLineEndOffset = caretOffset
        while (++caretLineEndOffset < charSequence.length) {
          val ch = charSequence.get(caretLineEndOffset)
          if (ch == '\n') {
            ++caretLineEndOffset
            break
          }
        }

        // `caretLineEndOffset` points to the first charater in the line below caret line.
        var lowerBoundaryOffset: Int? = null
        // Part 5: Find a line below the caret line, that has an indentation lower than `indentSize`.
        var pos2 = caretLineEndOffset - 1
        var isLowerBoundaryFound = false
        while (lowerBoundaryOffset == null) {
          var accumulatedWhitespace2 = 0
          var lastNewlinePos = caretLineEndOffset - 1
          var isInIndent = true
          while (++pos2 < charSequence.length) {
            val ch = charSequence.get(pos2)
            if (isIndentChar(ch) && isInIndent) {
              ++accumulatedWhitespace2
            } else if (ch == '\n') {
              accumulatedWhitespace2 = 0
              lastNewlinePos = pos2
              isInIndent = true
            } else {
              if (isInIndent && accumulatedWhitespace2 < indentSize) {
                lowerBoundaryOffset = lastNewlinePos
                isLowerBoundaryFound = true
                break
              }
              isInIndent = false
            }
          }
          if (pos2 >= charSequence.length) {
            // Reached end of the buffer.
            lowerBoundaryOffset = charSequence.length - 1
          }
        }

        // Now `lowerBoundaryOffset` marks the end of an `ii` text object.
        if (isLowerBoundaryFound && includeBelow) {
          while (++lowerBoundaryOffset < charSequence.length) {
            val ch = charSequence.get(lowerBoundaryOffset)
            if (ch == '\n') {
              break
            }
          }
        }

        return TextRange(upperBoundaryOffset, lowerBoundaryOffset)
      }

      override val visualType: TextObjectVisualType
        get() = TextObjectVisualType.LINE_WISE

      private fun isIndentChar(ch: Char): Boolean {
        return ch == ' ' || ch == '\t'
      }
    }

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val vimEditor = editor as IjVimEditor
      val keyHandlerState = getInstance().keyHandlerState

      val textObjectHandler = IndentObjectHandler(includeAbove, includeBelow)

      if (editor.mode !is OP_PENDING) {
        val count0 = operatorArguments.count0
        editor.editor.getCaretModel().runForEachCaret { caret: Caret ->
          val range = textObjectHandler.getRange(vimEditor, IjVimCaret(caret), context, max(1, count0), count0)
          SelectionVimListenerSuppressor.lock().use { ignored ->
            if (editor.mode is VISUAL) {
              IjVimCaret(caret).vimSetSelection(range.startOffset, range.endOffset - 1, true)
            } else {
              caret.moveToInlayAwareOffset(range.startOffset)
            }
          }
        }
      } else {
        keyHandlerState.commandBuilder.addAction(textObjectHandler)
      }
    }
  }
}
