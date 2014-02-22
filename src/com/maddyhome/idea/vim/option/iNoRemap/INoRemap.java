package com.maddyhome.idea.vim.option.iNoRemap;

import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.key.Shortcut;

public class INoRemap {
  private final KeyParser parser;

  public INoRemap(KeyParser parser) {
    this.parser = parser;
  }

  public INoRemapResult lineIsUseableINoRemap(String line) {
    String[] split = line.split(" ");
    if (split.length != 3) {
      return INoRemapResult.False;
    }
    else if (!split[0].equals("inoremap")) {
      return INoRemapResult.False;
    }
    else if (!split[2].equals("<esc>")) {
      return INoRemapResult.NotImplemented;
    }
    else {
      return INoRemapResult.True;
    }
  }

  public INoRemapResult tryToAddCustomEscape(String line) {
    INoRemapResult useable = lineIsUseableINoRemap(line);
    if (useable == INoRemapResult.True) {
      String customSequence = line.split(" ")[1];
      parser.registerAction(KeyParser.MAPPING_INSERT, "VimInsertExitMode", Command.Type.INSERT, new Shortcut[]{
        new Shortcut(customSequence)
      });
    }
    return useable;
  }
}
