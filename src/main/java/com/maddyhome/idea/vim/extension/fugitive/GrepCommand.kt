/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.fugitive

import com.intellij.platform.project.projectId
import com.intellij.util.execution.ParametersListUtil
import com.maddyhome.idea.vim.api.BufferPosition
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.CommandAliasHandler
import com.maddyhome.idea.vim.ex.ranges.Range
import com.maddyhome.idea.vim.group.grep.GrepMatch
import com.maddyhome.idea.vim.group.grep.GrepRemoteApi
import com.maddyhome.idea.vim.group.grep.GrepResult
import com.maddyhome.idea.vim.group.rpc
import com.maddyhome.idea.vim.newapi.ij

/**
 * `:Ggrep[!] {args}` — runs `git grep {args}` and jumps to the first match, like fugitive's
 * `s:GrepSubcommand` followed by `:cfirst`. With `!` it doesn't jump.
 */
internal class GrepCommand : CommandAliasHandler {

  override fun execute(command: String, range: Range, editor: VimEditor, context: ExecutionContext) {
    val invocation = GrepInvocation.parse(command)
    val result = grep(editor, invocation.gitArgs) ?: return

    val errorMessage = result.errorMessage
    if (errorMessage != null) {
      injector.messages.showErrorMessage(editor, errorMessage)
    } else if (invocation.jumpToFirstMatch) {
      result.matches.firstOrNull()?.let { jumpTo(editor, it) }
    }
  }

  private fun grep(editor: VimEditor, gitArgs: List<String>): GrepResult? {
    val project = editor.ij.project ?: return null
    return rpc(project) {
      GrepRemoteApi.getInstance().grep(project.projectId(), editor.getPath(), editor.extractProtocol(), gitArgs)
    }
  }

  /**
   * Like `:cfirst`, remembers the current position in the jump list first.
   */
  private fun jumpTo(editor: VimEditor, match: GrepMatch) {
    injector.jumpService.saveJumpLocation(editor)
    val target = injector.file.selectEditor(editor.projectId, match.filepath, match.protocol) ?: return
    target.currentCaret().moveToOffset(target.bufferPositionToOffset(BufferPosition(match.line, match.col)))
  }
}

/**
 * The parsed `:Ggrep[!] {args}` command line.
 */
private data class GrepInvocation(val jumpToFirstMatch: Boolean, val gitArgs: List<String>) {
  companion object {
    fun parse(command: String): GrepInvocation {
      val trimmed = command.trim()
      val name = trimmed.substringBefore(' ')
      val arguments = trimmed.substringAfter(' ', missingDelimiterValue = "")
      return GrepInvocation(
        jumpToFirstMatch = !name.endsWith('!'),
        // Split like a shell does, so `'foo bar'` stays one argument
        gitArgs = ParametersListUtil.parse(arguments, /* keepQuotes = */ false, /* supportSingleQuotes = */ true),
      )
    }
  }
}
