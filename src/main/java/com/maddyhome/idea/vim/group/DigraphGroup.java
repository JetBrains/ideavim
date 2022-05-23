/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2022 The IdeaVim authors
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

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.api.VimDigraphGroupBase;
import com.maddyhome.idea.vim.api.VimEditor;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.helper.EditorHelper;
import com.maddyhome.idea.vim.newapi.IjVimEditor;
import org.jetbrains.annotations.NotNull;

public class DigraphGroup extends VimDigraphGroupBase {

  public void showDigraphs(@NotNull VimEditor editor) {
    int width = EditorHelper.getApproximateScreenWidth(((IjVimEditor) editor).getEditor());
    if (width < 10) {
      width = 80;
    }
    int colCount = width / 12;
    int height = (int)Math.ceil((double) getDigraphs().size() / (double)colCount);

    if (logger.isDebugEnabled()) {
      logger.debug("width=" + width);
      logger.debug("colCount=" + colCount);
      logger.debug("height=" + height);
    }

    StringBuilder res = new StringBuilder();
    int cnt = 0;
    for (Character code : getKeys().keySet()) {
      String key = getKeys().get(code);

      res.append(key);
      res.append(' ');
      if (code < 32) {
        res.append('^');
        res.append((char)(code + '@'));
      }
      else if (code >= 128 && code <= 159) {
        res.append('~');
        res.append((char)(code - 128 + '@'));
      }
      else {
        res.append(code);
        res.append(' ');
      }
      res.append(' ');
      if (code < 0x1000) {
        res.append('0');
      }
      if (code < 0x100) {
        res.append('0');
      }
      if (code < 0x10) {
        res.append('0');
      }
      res.append(Integer.toHexString(code));
      res.append("  ");

      cnt++;
      if (cnt == colCount) {
        res.append('\n');
        cnt = 0;
      }
    }

    ExOutputModel.getInstance(((IjVimEditor) editor).getEditor()).output(res.toString());
  }

  private static final Logger logger = Logger.getInstance(DigraphGroup.class.getName());
}
