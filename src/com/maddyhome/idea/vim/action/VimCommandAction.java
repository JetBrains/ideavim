package com.maddyhome.idea.vim.action;

import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.Set;

/**
 * Action that represents a Vim command.
 *
 * Actions should be registered in plugin.xml.
 *
 * @author vlan
 */
public abstract class VimCommandAction extends EditorAction {
  protected VimCommandAction(EditorActionHandler defaultHandler) {
    super(defaultHandler);
  }

  @NotNull
  public abstract Set<MappingMode> getMappingModes();

  @NotNull
  public abstract Set<List<KeyStroke>> getKeyStrokesSet();

  @NotNull
  public abstract Command.Type getType();

  @NotNull
  public Argument.Type getArgumentType() {
    return Argument.Type.NONE;
  }

  /**
   * Returns various binary flags for the command.
   *
   * These legacy flags will be refactored in future releases.
   *
   * @see {@link Command}.
   */
  public int getFlags() {
    return 0;
  }

  /**
   * Returns Vim help topic tags for the command.
   *
   * The names of the topic tags should match tags in the Vim help.
   */
  @NotNull
  public abstract List<String> getHelpTopics();
}
