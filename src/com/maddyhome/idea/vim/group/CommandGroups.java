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

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.diagnostic.Logger;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * This singleton maintains the instances of all the key groups. All the key/action mappings get created the first
 * this singleton is accessed.
 */
public class CommandGroups {
  /**
   * Gets the singleton instance
   *
   * @return The singleton instance
   */
  public static CommandGroups getInstance() {
    if (instance == null) {
      instance = new CommandGroups();
    }

    return instance;
  }

  /**
   * Creates all the groups
   */
  private CommandGroups() {
    motion = new MotionGroup();
    change = new ChangeGroup();
    copy = new CopyGroup();
    mark = new MarkGroup();
    register = new RegisterGroup();
    file = new FileGroup();
    search = new SearchGroup();
    process = new ProcessGroup();
    macro = new MacroGroup();
    digraph = new DigraphGroup();
    history = new HistoryGroup();
  }

  /**
   * Returns the motion group
   *
   * @return The motion group
   */
  public MotionGroup getMotion() {
    return motion;
  }

  /**
   * Returns the change group
   *
   * @return The change group
   */
  public ChangeGroup getChange() {
    return change;
  }

  /**
   * Returns the copy group
   *
   * @return The copy group
   */
  public CopyGroup getCopy() {
    return copy;
  }

  /**
   * Returns the mark group
   *
   * @return The mark group
   */
  public MarkGroup getMark() {
    return mark;
  }

  /**
   * Returns the register group
   *
   * @return The register group
   */
  public RegisterGroup getRegister() {
    return register;
  }

  /**
   * Returns the file group
   *
   * @return The file group
   */
  public FileGroup getFile() {
    return file;
  }

  /**
   * Returns the search group
   *
   * @return The search group
   */
  public SearchGroup getSearch() {
    return search;
  }

  /**
   * Returns the process group
   *
   * @return The process group
   */
  public ProcessGroup getProcess() {
    return process;
  }

  /**
   * Returns the macro group
   *
   * @return The macro group
   */
  public MacroGroup getMacro() {
    return macro;
  }

  public DigraphGroup getDigraph() {
    return digraph;
  }

  public HistoryGroup getHistory() {
    return history;
  }

  /**
   * Tells each group to save its data.
   *
   * @param element The root XML element of the plugin
   */
  public void saveData(@NotNull Element element) {
    motion.saveData(element);
    change.saveData(element);
    copy.saveData(element);
    mark.saveData(element);
    register.saveData(element);
    file.saveData(element);
    search.saveData(element);
    process.saveData(element);
    macro.saveData(element);
    digraph.saveData(element);
    history.saveData(element);
  }

  /**
   * Tells each group to read its data.
   *
   * @param element The root XML element of the plugin
   */
  public void readData(@NotNull Element element) {
    logger.debug("readData");
    motion.readData(element);
    change.readData(element);
    copy.readData(element);
    mark.readData(element);
    register.readData(element);
    file.readData(element);
    search.readData(element);
    process.readData(element);
    macro.readData(element);
    digraph.readData(element);
    history.readData(element);
  }

  private static CommandGroups instance;
  private MotionGroup motion;
  private ChangeGroup change;
  private CopyGroup copy;
  private MarkGroup mark;
  private RegisterGroup register;
  private FileGroup file;
  private SearchGroup search;
  private ProcessGroup process;
  private MacroGroup macro;
  private DigraphGroup digraph;
  private HistoryGroup history;

  private static Logger logger = Logger.getInstance(CommandGroups.class.getName());
}
