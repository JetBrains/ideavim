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
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMapping
import com.maddyhome.idea.vim.extension.VimExtensionFacade.putKeyMappingIfMissing
import com.maddyhome.idea.vim.extension.exportOperatorFunction
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.key.OperatorFunction
import com.maddyhome.idea.vim.newapi.ij
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimDictionary
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString
import javax.swing.KeyStroke

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
    registerUserDefinedCoercions()
    val subvert = SubvertCommand()
    addCommand("Subvert", subvert)
    addCommand("S", subvert)
  }

  /**
   * Reads `g:abolish_coercions` (a dict of single-character → built-in style name)
   * and binds each entry as an additional `cr<char>` mapping. Values that don't
   * match a [CaseStyle] are silently skipped — same forgiving policy tpope has
   * for unknown letters.
   */
  private fun registerUserDefinedCoercions() {
    val dict = VimPlugin.getVariableService().getGlobalVariableValue(USER_COERCIONS_VARIABLE) as? VimDictionary ?: return
    dict.dictionary.forEach { (charKey, styleValue) ->
      val char = charKey.value.singleOrNull() ?: return@forEach
      val style = resolveStyleByName((styleValue as? VimString)?.value) ?: return@forEach
      bindAlias("cr$char", plugWordKeysFor(style), plugOperatorKeysFor(style))
    }
  }

  private fun registerCoercion(coercion: Coercion) {
    val plugOperator = plugOperatorKeysFor(coercion.style)
    val plugWord = plugWordKeysFor(coercion.style)

    putExtensionHandlerMapping(MappingMode.N, plugOperator, owner, CoercionOperatorTrigger(coercion.style), false)
    putExtensionHandlerMapping(MappingMode.X, plugOperator, owner, CoercionVisualHandler(coercion.style), false)
    putExtensionHandlerMapping(MappingMode.N, plugWord, owner, CoercionWordHandler(coercion.style), false)

    bindPrimaryKeyUnlessUserOverrode(coercion.primaryKey, plugWord, plugOperator)
    coercion.aliases.forEach { alias -> bindAlias(alias, plugWord, plugOperator) }
  }

  private fun plugWordKeysFor(style: CaseStyle): List<KeyStroke> =
    injector.parser.parseKeys("<Plug>(abolish-coerce-word-${style.name.lowercase()})")

  private fun plugOperatorKeysFor(style: CaseStyle): List<KeyStroke> =
    injector.parser.parseKeys("<Plug>(abolish-coerce-${style.name.lowercase()})")

  private fun resolveStyleByName(name: String?): CaseStyle? =
    name?.let { needle -> CaseStyle.entries.firstOrNull { it.name.equals(needle, ignoreCase = true) } }

  private fun bindPrimaryKeyUnlessUserOverrode(
    key: String,
    plugWord: List<KeyStroke>,
    plugOperator: List<KeyStroke>,
  ) {
    val parsed = injector.parser.parseKeys(key)
    putKeyMappingIfMissing(MappingMode.N, parsed, owner, plugWord, true)
    putKeyMappingIfMissing(MappingMode.X, parsed, owner, plugOperator, true)
  }

  private fun bindAlias(
    key: String,
    plugWord: List<KeyStroke>,
    plugOperator: List<KeyStroke>,
  ) {
    val parsed = injector.parser.parseKeys(key)
    putKeyMapping(MappingMode.N, parsed, owner, plugWord, true)
    putKeyMapping(MappingMode.X, parsed, owner, plugOperator, true)
  }

  private data class Coercion(val style: CaseStyle, val primaryKey: String, val aliases: List<String> = emptyList())

  companion object {
    internal const val OPERATOR_FUNC = "AbolishCoerce"
    private const val USER_COERCIONS_VARIABLE = "abolish_coercions"

    private val COERCIONS = listOf(
      Coercion(CaseStyle.SNAKE, primaryKey = "crs", aliases = listOf("cr_")),
      Coercion(CaseStyle.PASCAL, primaryKey = "crm", aliases = listOf("crp")),
      Coercion(CaseStyle.CAMEL, primaryKey = "crc"),
      Coercion(CaseStyle.UPPER_SNAKE, primaryKey = "cru", aliases = listOf("crU")),
      Coercion(CaseStyle.KEBAB, primaryKey = "cr-", aliases = listOf("crk")),
      Coercion(CaseStyle.DOT, primaryKey = "cr."),
      Coercion(CaseStyle.SPACE, primaryKey = "cr<Space>"),
      Coercion(CaseStyle.TITLE, primaryKey = "crt"),
    )
  }
}

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
    injector.globalOptions().operatorfunc = AbolishExtension.OPERATOR_FUNC
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
