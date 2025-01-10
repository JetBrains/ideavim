/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.vimscript.model.commands

import com.intellij.ide.BrowserUtil
import com.intellij.vim.annotations.ExCommand
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.vimscript.model.ExecutionResult
import org.jetbrains.annotations.NonNls
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * @author vlan
 * see "h :help"
 */
@ExCommand(command = "h[elp]")
internal data class HelpCommand(val range: Range, val modifier: CommandModifier, val argument: String) :
  Command.SingleExecution(range, modifier, argument) {

  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)

  override fun processCommand(
    editor: VimEditor,
    context: ExecutionContext,
    operatorArguments: OperatorArguments,
  ): ExecutionResult {
    BrowserUtil.browse(helpTopicUrl(argument))
    return ExecutionResult.Success
  }

  @NonNls
  private fun helpTopicUrl(topic: String): String {
    if (topic.isBlank()) return HELP_ROOT_URL

    return try {
      String.format("%s?docs=help&search=%s", HELP_QUERY_URL, URLEncoder.encode(topic, "UTF-8"))
    } catch (_: UnsupportedEncodingException) {
      HELP_ROOT_URL
    }
  }

  companion object {
    private const val HELP_BASE_URL = "http://vimdoc.sourceforge.net"
    private const val HELP_ROOT_URL = "$HELP_BASE_URL/htmldoc/"
    private const val HELP_QUERY_URL = "$HELP_BASE_URL/search.php"
  }
}
