/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands.mapping

import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.AbbreviationListing
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.MessageType
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import com.maddyhome.idea.vim.vimscript.model.commands.Command
import com.maddyhome.idea.vim.vimscript.model.commands.CommandModifier

@ExCommand(command = "ab[breviate],ia[bbrev],ca[bbrev],norea[bbrev],inorea[bbrev],cnorea[bbrev]")
data class AbbrevCommand(val range: Range, val cmd: String, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags: CommandHandlerFlags =
    flags(RangeFlag.RANGE_FORBIDDEN, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    val variant = AbbrevVariant.matching(cmd) ?: return ExecutionResult.Error
    when (val parsed = parseAbbrevArgument(argument)) {
      is AbbrevArgument.Definition -> if (parsed.bufferLocal) {
        injector.abbreviationGroup.setBufferLocalAbbreviation(
          parsed.toEntry(variant.modes, variant.recursive), editor
        )
      } else {
        injector.abbreviationGroup.setAbbreviation(
          parsed.toEntry(variant.modes, variant.recursive), editor
        )
      }

      is AbbrevArgument.Listing -> showAbbreviations(variant.modes, parsed.bufferLocal, editor)
    }
    return ExecutionResult.Success
  }

  private fun showAbbreviations(modes: Set<MappingMode>, bufferLocalOnly: Boolean, editor: VimEditor) {
    val listings = injector.abbreviationGroup.listAbbreviations(modes, editor, bufferLocalOnly)
    val output = if (listings.isEmpty()) NO_ABBREVIATIONS_FOUND else listings.joinToString(
      separator = "\n", transform = ::formatListingLine
    )
    showOutputPanel(editor, output)
  }

  private fun showOutputPanel(editor: VimEditor, text: String) {
    val context = injector.executionContextManager.getEditorExecutionContext(editor)
    val outputPanel = injector.outputPanel.getOrCreate(editor, context)
    outputPanel.addText(text, true, MessageType.STANDARD)
    outputPanel.show()
  }

  private fun formatListingLine(listing: AbbreviationListing): String {
    val modeChar = modeCharOf(listing.mode)
    val scopeMarker = if (listing.bufferLocal) BUFFER_LOCAL_MARKER else NO_MARKER
    val exprMarker = if (listing.isExpression) EXPRESSION_MARKER else NO_MARKER
    val paddedLhs = listing.lhs.padEnd(LHS_COLUMN_WIDTH, ' ')
    return "$modeChar $scopeMarker$paddedLhs$exprMarker ${listing.rhs}"
  }

  private fun modeCharOf(mode: MappingMode): Char = when (mode) {
    MappingMode.INSERT -> 'i'
    MappingMode.CMD_LINE -> 'c'
    else -> '!'
  }

  private companion object {
    private const val LHS_COLUMN_WIDTH = 13
    private const val BUFFER_LOCAL_MARKER = '@'
    private const val EXPRESSION_MARKER = '*'
    private const val NO_MARKER = ' '
    private const val NO_ABBREVIATIONS_FOUND = "No abbreviations found"
  }

  private enum class AbbrevVariant(val prefix: String, val modes: Set<MappingMode>, val recursive: Boolean) {
    ABBREVIATE("ab", MappingMode.IC, true),
    IABBREV("ia", MappingMode.I, true),
    CABBREV("ca", MappingMode.C, true),
    NOREABBREV("norea", MappingMode.IC, false),
    INOREABBREV("inorea", MappingMode.I, false),
    CNOREABBREV("cnorea", MappingMode.C, false),
    ;

    companion object {
      fun matching(commandName: String): AbbrevVariant? = entries.find { commandName.startsWith(it.prefix) }
    }
  }
}
