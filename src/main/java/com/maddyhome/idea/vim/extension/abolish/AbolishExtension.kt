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
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.VimExtension
import com.maddyhome.idea.vim.extension.VimExtensionFacade.addCommand
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putExtensionHandlerMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing

/**
 * Emulation of [vim-abolish](https://github.com/tpope/vim-abolish): `cr<x>`
 * coercions and `:Subvert`/`:S`. `:Abolish` is not implemented — IdeaVim has
 * no `:iabbrev` infrastructure to build insert-mode auto-correct on top of.
 */
internal class AbolishExtension : VimExtension {

  override fun getName(): String = "abolish"

  override fun init() {
    COERCIONS.forEach(::registerCoercion)
    val subvert = SubvertCommand()
    addCommand("Subvert", subvert)
    addCommand("S", subvert)
  }

  private fun registerCoercion(coercion: Coercion) {
    val plug = injector.parser.parseKeys(coercion.plugName)
    putExtensionHandlerMapping(MappingMode.N, plug, owner, CoercionHandler(coercion.style), false)
    putKeyMappingIfMissing(MappingMode.N, injector.parser.parseKeys(coercion.keys), owner, plug, true)
  }

  private data class Coercion(val keys: String, val style: CaseStyle) {
    val plugName: String = "<Plug>(abolish-coerce-${style.name.lowercase()})"
  }

  private companion object {
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

private class CoercionHandler(private val targetStyle: CaseStyle) : ExtensionHandler {

  override val isRepeatable: Boolean = true

  override fun execute(editor: VimEditor, context: ExecutionContext, operatorArguments: OperatorArguments) {
    // Right-to-left so an earlier caret's offsets survive a later replacement.
    editor.sortedCarets().reversed().forEach { caret -> recaseWordAtCaret(editor, caret) }
  }

  private fun recaseWordAtCaret(editor: VimEditor, caret: VimCaret) {
    val wordRange = injector.searchHelper.findWordObject(editor, caret, 1, isOuter = false, isBig = false)
    if (wordRange.startOffset == wordRange.endOffset) return
    val originalWord = editor.text().substring(wordRange.startOffset, wordRange.endOffset)
    val recased = targetStyle.recase(originalWord)
    if (recased == originalWord) return
    injector.changeGroup.replaceText(editor, caret, wordRange.startOffset, wordRange.endOffset, recased)
    caret.moveToOffset(wordRange.startOffset)
  }
}
