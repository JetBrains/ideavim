package com.maddyhome.idea.vim.helper;

import com.google.common.io.CharStreams;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
* @author vlan
*/
public class MacKeyRepeat {
  public static final String FMT = "defaults %s -globalDomain ApplePressAndHoldEnabled";
  @NotNull private static final MacKeyRepeat INSTANCE = new MacKeyRepeat();

  @NotNull
  public static MacKeyRepeat getInstance() {
    return INSTANCE;
  }

  @Nullable
  public Boolean isEnabled() {
    final String command = String.format(FMT, "read");
    try {
      final Process process = Runtime.getRuntime().exec(command);
      final String data = read(process.getInputStream()).trim();
      try {
        return Integer.valueOf(data) == 0;
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
      command = String.format(FMT, "delete");
    }
    else {
      final String arg = value ? "0" : "1";
      command = String.format(FMT, "write") + " " + arg;
    }
    final Process process;
    try {
      process = Runtime.getRuntime().exec(command);
      process.waitFor();
    }
    catch (IOException e) {
    }
    catch (InterruptedException e) {
    }
  }

  @NotNull
  private static String read(@NotNull InputStream stream) throws IOException {
    return CharStreams.toString(new InputStreamReader(stream));
  }
}
