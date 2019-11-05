/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.maddyhome.idea.vim.ex.CommandHandler
import com.maddyhome.idea.vim.ex.ExCommand
import com.maddyhome.idea.vim.ex.flags
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

/**
 * @author vlan
 */
class HelpHandler : CommandHandler.SingleExecution() {
  override val argFlags = flags(RangeFlag.RANGE_OPTIONAL, ArgumentFlag.ARGUMENT_OPTIONAL, Access.READ_ONLY)
  override fun execute(editor: Editor, context: DataContext, cmd: ExCommand): Boolean {
    BrowserUtil.browse(helpTopicUrl(cmd.argument))
    return true
  }

  private fun helpTopicUrl(topic: String): String {
    if (topic.isBlank()) return HELP_ROOT_URL

    return try {
      String.format("%s?docs=help&search=%s", HELP_QUERY_URL, URLEncoder.encode(topic, "UTF-8"))
    } catch (e: UnsupportedEncodingException) {
      HELP_ROOT_URL
    }
  }

  companion object {
    private const val HELP_BASE_URL = "http://vimdoc.sourceforge.net"
    private const val HELP_ROOT_URL = "$HELP_BASE_URL/htmldoc/"
    private const val HELP_QUERY_URL = "$HELP_BASE_URL/search.php"
  }
}
