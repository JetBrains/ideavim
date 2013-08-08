/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2013 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ex.handler;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ex.CommandHandler;
import com.maddyhome.idea.vim.ex.ExCommand;
import com.maddyhome.idea.vim.ex.ExException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * @author vlan
 */
public class HelpHandler extends CommandHandler {
  private static final String HELP_BASE_URL = "http://vimdoc.sourceforge.net";
  private static final String HELP_ROOT_URL = HELP_BASE_URL + "/htmldoc/";
  private static final String HELP_QUERY_URL = HELP_BASE_URL + "/search.php";

  public HelpHandler() {
    super("h", "elp", ARGUMENT_OPTIONAL);
  }

  public boolean execute(Editor editor, DataContext context, @NotNull ExCommand cmd) throws ExException {
    final String key = cmd.getArgument();
    BrowserUtil.launchBrowser(helpTopicUrl(key));
    return true;
  }

  @NotNull
  private static String helpTopicUrl(@Nullable String topic) {
    if (topic == null || "".equals(topic)) {
      return HELP_ROOT_URL;
    }
    try {
      return String.format("%s?docs=help&search=%s", HELP_QUERY_URL, URLEncoder.encode(topic, "UTF-8"));
    }
    catch (UnsupportedEncodingException e) {
      return HELP_ROOT_URL;
    }
  }
}
