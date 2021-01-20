/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.helper;

import com.google.common.io.CharStreams;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
* @author vlan
*/
public class MacKeyRepeat {
  @VimNlsSafe public static final String FMT = "defaults %s -globalDomain ApplePressAndHoldEnabled";
  @NotNull private static final MacKeyRepeat INSTANCE = new MacKeyRepeat();
  @NonNls private static final String EXEC_COMMAND = "launchctl stop com.apple.SystemUIServer.agent";
  @NonNls private static final String delete = "delete";
  @NonNls private static final String write = "write";
  @NonNls private static final String read = "read";

  public static @NotNull MacKeyRepeat getInstance() {
    return INSTANCE;
  }

  public @Nullable Boolean isEnabled() {
    final String command = String.format(FMT, read);
    try {
      final Process process = Runtime.getRuntime().exec(command);
      final String data = read(process.getInputStream()).trim();
      try {
        return Integer.parseInt(data) == 0;
      } catch (NumberFormatException e) {
        return null;
      }
    }
    catch (IOException e) {
      return null;
    }
  }

  public void setEnabled(@Nullable Boolean value) {
    final String command;
    if (value == null) {
      command = String.format(FMT, delete);
    }
    else {
      final String arg = value ? "0" : "1";
      command = String.format(FMT, write) + " " + arg;
    }
    try {
      final Runtime runtime = Runtime.getRuntime();
      final Process defaults = runtime.exec(command);
      defaults.waitFor();
      final Process restartSystemUI = runtime.exec(EXEC_COMMAND);
      restartSystemUI.waitFor();
    }
    catch (IOException | InterruptedException ignored) {
    }
  }

  private static @NotNull String read(@NotNull InputStream stream) throws IOException {
    return CharStreams.toString(new InputStreamReader(stream));
  }
}
