/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.abolish

import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.globalOptions
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade
import com.maddyhome.idea.vim.extension.VimExtensionFacade.addCommand
import com.maddyhome.idea.vim.extension.VimExtensionFacade.executeNormalWithoutMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType

/**
 * Emulation of [vim-abolish](https://github.com/tpope/vim-abolish): `cr<x>`
 * coercions and `:Subvert`/`:S`. `:Abolish` is not implemented — IdeaVim has
 * no `:iabbrev` infrastructure to build insert-mode auto-correct on top of.
 *
 * Each coercion exposes two `<Plug>` mappings: `<Plug>(abolish-coerce-word-X)`
 * recases the inner word under the cursor (bound to `crX` by default), and
 * `<Plug>(abolish-coerce-X)` is operator-pending — `:nmap crs <Plug>(abolish-coerce-snake)`
 * makes `crs{motion}` recase the motion target (e.g. `crsiw`, `crsap`).
 */
internal class AbolishExtension : VimExtension {

  override fun getName(): String = "abolish"

  override fun init() {
    VimExtensionFacade.exportOperatorFunction(OPERATOR_FUNC, CoercionOperator())
    COERCIONS.forEach(::registerCoercion)
    val subvert = SubvertCommand()
    addCommand("Subvert", subvert)
    addCommand("S", subvert)
  }

  private fun registerCoercion(coercion: Coercion) {
    val plugOperator = injector.parser.parseKeys(coercion.plugOperatorName)
    putExtensionHandlerMapping(MappingMode.N, plugOperator, owner, CoercionOperatorTrigger(coercion.style), false)
    // Same <Plug> name in visual mode: the selection is the operand, no motion needed.
    putExtensionHandlerMapping(MappingMode.X, plugOperator, owner, CoercionVisualHandler(coercion.style), false)

    val plugWord = injector.parser.parseKeys(coercion.plugWordName)
    putExtensionHandlerMapping(MappingMode.N, plugWord, owner, CoercionWordHandler(coercion.style), false)

    val keys = injector.parser.parseKeys(coercion.keys)
    putKeyMappingIfMissing(MappingMode.N, keys, owner, plugWord, true)
    putKeyMappingIfMissing(MappingMode.X, keys, owner, plugOperator, true)
  }

  private data class Coercion(val keys: String, val style: CaseStyle) {
    val plugWordName: String = "<Plug>(abolish-coerce-word-${style.name.lowercase()})"
    val plugOperatorName: String = "<Plug>(abolish-coerce-${style.name.lowercase()})"
  }

  private companion object {
    private const val OPERATOR_FUNC = "AbolishCoerce"

    private val COERCIONS = listOf(
      Coercion("crs", CaseStyle.SNAKE),
      Coercion("crm", CaseStyle.PASCAL),
      Coercion("crc", CaseStyle.CAMEL),
      Coercion("cru", CaseStyle.UPPER_SNAKE),
      Coercion("cr-", CaseStyle.KEBAB),
      Coercion("cr.", CaseStyle.DOT),
      Coercion("cr<Space>", CaseStyle.SPACE),
      Coercion("crt", CaseStyle.TITLE),
    )
  }
}

/**
 * Visual-mode coercion: the current selection is the operand. Recase, replace,
 * exit visual back to normal with the caret at the start of the rewritten span.
 */
private class CoercionVisualHandler(private val style: CaseStyle) : ExtensionHandler {

  override val isRepeatable: Boolean = true

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val caret = editor.primaryCaret()
    val start = caret.selectionStart
    val end = caret.selectionEnd
    if (start >= end) return
    val original = editor.text().substring(start, end)
    val recased = style.recase(original)
    executeNormalWithoutMapping(injector.parser.parseKeys("<Esc>"), editor.ij)
    if (recased == original) return
    injector.changeGroup.replaceText(editor, caret, start, end, recased)
    caret.moveToOffset(start)
  }
}

/** Default direct coercion: recase the inner word under each caret in normal mode. */
private class CoercionWordHandler(private val targetStyle: CaseStyle) : ExtensionHandler {

  override val isRepeatable: Boolean = true

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    val count = operatorArguments.count0.coerceAtLeast(1)
    // Right-to-left so an earlier caret's offsets survive a later replacement.
    editor.sortedCarets().reversed().forEach { caret -> recaseWordAtCaret(editor, caret, targetStyle, count) }
  }
}

/**
 * Operator-pending coercion: stash the chosen style, set `opfunc`, press `g@`.
 * Vim then collects the motion and invokes the registered operator function,
 * which reads the stashed style back. Same mechanism tpope uses with `s:transformation`.
 */
private class CoercionOperatorTrigger(private val style: CaseStyle) : ExtensionHandler {

  override val isRepeatable: Boolean = true

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    PendingCoercion.style = style
    injector.globalOptions().operatorfunc = "AbolishCoerce"
    executeNormalWithoutMapping(injector.parser.parseKeys("g@"), editor.ij)
  }
}

private class CoercionOperator : OperatorFunction {
  override fun apply(editor: VimEditor, context: ExecutionContext, selectionType: SelectionType?): Boolean {
    val style = PendingCoercion.style ?: return false
    val range = injector.markService.getChangeMarks(editor.primaryCaret()) ?: return false
    val original = editor.text().substring(range.startOffset, range.endOffset)
    val recased = style.recase(original)
    if (recased == original) return true
    injector.changeGroup.replaceText(editor, editor.primaryCaret(), range.startOffset, range.endOffset, recased)
    editor.primaryCaret().moveToOffset(range.startOffset)
    return true
  }
}

private object PendingCoercion {
  @Volatile var style: CaseStyle? = null
}

private fun recaseWordAtCaret(editor: VimEditor, caret: VimCaret, targetStyle: CaseStyle, count: Int) {
  val wordRange = injector.searchHelper.findWordObject(editor, caret, count, isOuter = false, isBig = false)
  if (wordRange.startOffset == wordRange.endOffset) return
  val originalWord = editor.text().substring(wordRange.startOffset, wordRange.endOffset)
  val recased = targetStyle.recase(originalWord)
  if (recased == originalWord) return
  injector.changeGroup.replaceText(editor, caret, wordRange.startOffset, wordRange.endOffset, recased)
  caret.moveToOffset(wordRange.startOffset)
}
