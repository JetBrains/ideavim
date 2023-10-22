/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
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
  @VimNlsSafe
  public static final String FMT = "defaults %s -globalDomain ApplePressAndHoldEnabled";
  @NotNull
  private static final MacKeyRepeat INSTANCE = new MacKeyRepeat();
  @NonNls
  private static final String EXEC_COMMAND = "launchctl stop com.apple.SystemUIServer.agent";
  @NonNls
  private static final String delete = "delete";
  @NonNls
  private static final String write = "write";
  @NonNls
  private static final String read = "read";

  public static @NotNull
  MacKeyRepeat getInstance() {
    return INSTANCE;
  }

  private static @NotNull
  String read(@NotNull InputStream stream) throws IOException {
    return CharStreams.toString(new InputStreamReader(stream));
  }

  public @Nullable
  Boolean isEnabled() {
    final String command = String.format(FMT, read);
    try {
      final Process process = Runtime.getRuntime().exec(command);
      final String data = read(process.getInputStream()).trim();
      try {
        return Integer.parseInt(data) == 0;
      } catch (NumberFormatException e) {
        return null;
      }
    } catch (IOException e) {
      return null;
    }
  }

  public void setEnabled(@Nullable Boolean value) {
    final String command;
    if (value == null) {
      command = String.format(FMT, delete);
    } else {
      final String arg = value ? "0" : "1";
      command = String.format(FMT, write) + " " + arg;
    }
    try {
      final Runtime runtime = Runtime.getRuntime();
      final Process defaults = runtime.exec(command);
      defaults.waitFor();
      final Process restartSystemUI = runtime.exec(EXEC_COMMAND);
      restartSystemUI.waitFor();
    } catch (IOException | InterruptedException ignored) {
    }
  }
}
