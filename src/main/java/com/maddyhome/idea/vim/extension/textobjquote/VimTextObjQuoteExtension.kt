/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.extension.textobjquote

import com.maddyhome.idea.vim.KeyHandler.Companion.getInstance
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.ImmutableVimCaret
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.command.TextObjectVisualType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.group.visual.vimSetSelection
import com.maddyhome.idea.vim.handler.TextObjectActionHandler
import com.maddyhome.idea.vim.helper.moveToInlayAwareOffset
import com.maddyhome.idea.vim.listener.SelectionVimListenerSuppressor
import com.maddyhome.idea.vim.newapi.IjVimCaret
import com.maddyhome.idea.vim.state.mode.Mode.OP_PENDING
import com.maddyhome.idea.vim.state.mode.Mode.VISUAL
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import java.util.function.Consumer
import kotlin.math.max

class VimTextObjQuoteExtension : VimExtension {
  override fun getName(): String = "textobj-quote"

  override fun init() {
    for (kind in QuoteKind.entries) {
      val motion = globalString(kind.motionVariable) ?: kind.defaultMotion
      for (isInner in listOf(true, false)) {
        val plug = injector.parser.parseKeys(kind.plug(isInner))
        putExtensionHandlerMapping(MappingMode.XO, plug, owner, QuoteHandler(kind, isInner), false)
        val keys = injector.parser.parseKeys((if (isInner) "i" else "a") + motion)
        putKeyMappingIfMissing(MappingMode.XO, keys, owner, plug, true)
      }
    }
  }

  internal enum class QuoteKind(
    private val plugName: String,
    val motionVariable: String,
    val defaultMotion: String,
    private val pairVariable: String,
    private val defaultPair: String,
  ) {
    DOUBLE("d", "textobj#quote#doubleMotion", "q", "textobj#quote#doubleDefault", "“”"),
    SINGLE("s", "textobj#quote#singleMotion", "Q", "textobj#quote#singleDefault", "‘’"),
    ;

    fun plug(isInner: Boolean): String = "<Plug>(textobj-quote-select-$plugName-${if (isInner) "i" else "a"})"

    fun quotes(): String = globalString(pairVariable)?.takeIf { it.length == 2 } ?: defaultPair
  }

  internal class QuoteHandler(private val kind: QuoteKind, private val isInner: Boolean) : ExtensionHandler {
    override val isRepeatable: Boolean
      get() = false

    override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
      val keyHandlerState = getInstance().keyHandlerState

      val quotes = kind.quotes()
      val startQuote = quotes[0]
      val endQuote = quotes[1]
      val textObjectHandler = QuoteTextObjectHandler(
        isInner = isInner,
        startQuote = startQuote,
        endQuote = endQuote,
        endQuoteMayBeApostrophe = endQuote == '’',
      )
      if (editor.mode is OP_PENDING) {
        keyHandlerState.commandBuilder.addAction(textObjectHandler)
        return
      }

      val count0 = operatorArguments.count0
      editor.nativeCarets().forEach(Consumer { caret: VimCaret ->
        val range = textObjectHandler.getRange(editor, caret, context, max(1, count0), count0) ?: return@Consumer
        SelectionVimListenerSuppressor.lock {
          if (editor.mode is VISUAL) {
            caret.vimSetSelection(range.startOffset, range.endOffset - 1, true)
            return@Consumer
          }
          (caret as IjVimCaret).caret.moveToInlayAwareOffset(range.startOffset)
        }
      })
    }
  }

  internal class QuoteTextObjectHandler(
    private val isInner: Boolean,
    private val startQuote: Char,
    private val endQuote: Char,
    private val endQuoteMayBeApostrophe: Boolean = false
  ) : TextObjectActionHandler() {
    override fun getRange(
      editor: VimEditor,
      caret: ImmutableVimCaret,
      context: ExecutionContext,
      count: Int,
      rawCount: Int,
    ): TextRange? {
      val offset = caret.offset
      val prevQuote = findPrevQuote(offset, editor) ?: return null
      val nextQuote = findNextQuote(offset, editor) ?: return null

      val outerOffset = if (isInner) 0 else 1
      return TextRange(prevQuote - outerOffset, nextQuote + outerOffset)
    }

    private fun findNextQuote(offset: Int, editor: VimEditor): Int? {
      return find(offset, editor, startQuote, endQuote, offset until editor.text().length)
    }

    private fun findPrevQuote(offset: Int, editor: VimEditor): Int? {
      return find(offset, editor, endQuote, startQuote, offset downTo 0)?.plus(1)
    }

    private fun find(
      offset: Int,
      editor: VimEditor,
      oppositeChar: Char,
      lookupChar: Char,
      range: IntProgression,
    ): Int? {
      val editorText = editor.text()
      if (editorText.isEmpty()) return null
      var foundCounter = 0
      for (i in range) {
        if (i == editorText.length) continue
        val ch = editorText[i]
        if (ch == endQuote && endQuoteMayBeApostrophe && i < editorText.length - 1 && editorText[i + 1].isLetterOrDigit()) continue
        if (ch == lookupChar) foundCounter++
        if (ch == oppositeChar && isNotUnderCaret(i, offset)) foundCounter--
        if (foundCounter == 1) return i
      }
      return null
    }

    private fun isNotUnderCaret(i: Int, offset: Int): Boolean = i != offset

    override val visualType: TextObjectVisualType
      get() = TextObjectVisualType.CHARACTER_WISE

    override val supportsLinewiseDeletePromotion: Boolean = false
  }
}

private fun globalString(name: String): String? =
  (injector.variableService.getGlobalVariableValue(name) as? VimString)?.value
