/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package com.maddyhome.idea.vim.action.motion.search

import com.intellij.vim.annotations.CommandOrMotion
import com.intellij.vim.annotations.Mode
import com.maddyhome.idea.vim.api.ExecutionContext
import com.maddyhome.idea.vim.api.VimCaret
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import java.util.*

@CommandOrMotion(keys = ["gx"], modes = [Mode.NORMAL])
class GotoUrlAction : VimActionHandler.ForEachCaret() {
  override val type: Command.Type = Command.Type.OTHER_READONLY

  override val flags: EnumSet<CommandFlags> = enumSetOf(CommandFlags.FLAG_SAVE_JUMP)

  override fun execute(
    editor: VimEditor,
    caret: VimCaret,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val url = findUrl(editor, caret) ?: return false
    injector.jumpService.saveJumpLocation(editor)
    injector.externalOpener.open(url, browsexViewer())
    return true
  }

  /**
   * The custom viewer command from `g:netrw_browsex_viewer`, or `null` to use the OS default handler.
   *
   * As in Vim's netrw, the special value `"-"` (and an empty/blank value) means "use the default
   * handler", so it is treated the same as if the variable were unset.
   */
  private fun browsexViewer(): String? {
    val viewer = injector.variableService.getGlobalVariableValue("netrw_browsex_viewer")
      ?.toVimString()?.value
      ?.trim()
    return viewer?.takeIf { it.isNotEmpty() && it != "-" }
  }

  /**
   * Finds the URL under [caret] on the current line, or `null` if the caret is not on a URL.
   *
   * A URL is any `scheme://` (matching Vim's netrw `^\a\{3,}://` recognition, e.g. `http`, `ssh`,
   * `vscode`) or a bare `www.`, followed by a run of non-blank characters. The match stops at
   * whitespace and the usual wrapping/quoting characters so that `(https://example.com)` yields
   * `https://example.com`. Trailing sentence punctuation is trimmed.
   */
  private fun findUrl(editor: VimEditor, caret: VimCaret): String? {
    val position = editor.offsetToBufferPosition(caret.offset)
    val lineText = editor.getLineText(position.line)
    if (lineText.isEmpty()) return null

    val column = position.column
    for (match in URL_REGEX.findAll(lineText)) {
      if (column in match.range) {
        return match.value.trimEnd(*TRAILING_CHARS)
      }
    }
    return null
  }

  companion object {
    private val URL_REGEX =
      Regex("""[a-z][a-z0-9+.\-]*://[^\s"'<>()\[\]{}|]+|www\.[^\s"'<>()\[\]{}|]+""", RegexOption.IGNORE_CASE)

    private val TRAILING_CHARS = charArrayOf('.', ',', ';', ':', '!', '?')
  }
}
