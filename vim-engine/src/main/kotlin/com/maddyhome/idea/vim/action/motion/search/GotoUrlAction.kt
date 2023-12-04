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
import com.maddyhome.idea.vim.api.VimEditor
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.Command
import com.maddyhome.idea.vim.command.CommandFlags
import com.maddyhome.idea.vim.command.OperatorArguments
import com.maddyhome.idea.vim.diagnostic.vimLogger
import com.maddyhome.idea.vim.handler.VimActionHandler
import com.maddyhome.idea.vim.helper.enumSetOf
import com.maddyhome.idea.vim.helper.vimStateMachine
import com.maddyhome.idea.vim.macro.VimMacroBase.Companion.logger
import java.util.*
import java.util.concurrent.Future
import java.util.regex.Pattern

@CommandOrMotion(keys = ["gx"], modes = [Mode.NORMAL, Mode.VISUAL])
public class GotoUrlAction : VimActionHandler.SingleExecution() {
  override val type: Command.Type = Command.Type.OTHER_READONLY
  private val URL_REGEX = "^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$"
  private val pattern = Pattern.compile(URL_REGEX);

  override fun execute(
    editor: VimEditor,
    context: ExecutionContext,
    cmd: Command,
    operatorArguments: OperatorArguments,
  ): Boolean {
    val wordUnderCursor = exactWordUnderCursor(editor);
    logger.info("word: $wordUnderCursor")
    if(!isValidUrl(wordUnderCursor)){
      logger.info("word $wordUnderCursor in not url")
      return false;
    }
    injector.jumpService.saveJumpLocation(editor)
    injector.actionExecutor.executeAction("GotoDeclaration", context)
    return true
  }

  private fun exactWordUnderCursor(editor: VimEditor): String {
    var col = editor.currentCaret().vimLastColumn;
    var line = editor.getLineText(editor.currentCaret().vimLine - 1);
    if(line.isBlank() || line.get(col).isWhitespace()){
      return "";
    }
    logger.info("col: $col line: $line")
    var start = col;
    var end = col;
    while( start > 0 && !line.get(start).isWhitespace()){
      start--;
    }
    while(end < line.length && !line.get(end).isWhitespace()){
      end++;
    }
    logger.info("start: $start end $end")
    return line.substring(start+1, end);
  }

  private fun isValidUrl(url: String): Boolean {
    var matcher = pattern.matcher(url)
    return matcher.matches()
  }
}
