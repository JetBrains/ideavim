package com.maddyhome.idea.vim.action.plugin.surround;

import com.maddyhome.idea.vim.action.plugin.Plugin;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.group.KeyGroup;
import com.maddyhome.idea.vim.key.Shortcut;

/**
 * @author dhleong
 */
public class SurroundPlugin implements Plugin {

  public static final String NAME = "surround";

  @Override
  public String getOptionName() {
    return NAME;
  }

  @Override
  public void registerActions(KeyGroup parser) {
    System.out.println("REGISTER! ");
    parser.registerAction(MappingMode.N, "VimSurroundMotion", Command.Type.CHANGE, Command.FLAG_OP_PEND,
                          new Shortcut("ys"), Argument.Type.MOTION);
  }
}
