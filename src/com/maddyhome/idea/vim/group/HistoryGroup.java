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
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryGroup extends AbstractActionGroup {
  public static final String SEARCH = "search";
  public static final String COMMAND = "cmd";
  public static final String EXPRESSION = "expr";
  public static final String INPUT = "input";

  public void addEntry(String key, @NotNull String text) {
    if (logger.isDebugEnabled()) {
      logger.debug("Add entry '" + text + "' to " + key);
    }

    HistoryBlock block = blocks(key);
    block.addEntry(text);
  }

  @NotNull
  public List<HistoryEntry> getEntries(String key, int first, int last) {
    HistoryBlock block = blocks(key);

    List<HistoryEntry> entries = block.getEntries();
    List<HistoryEntry> res = new ArrayList<HistoryEntry>();
    if (first < 0) {
      if (-first > entries.size()) {
        first = Integer.MAX_VALUE;
      }
      else {
        HistoryEntry entry = entries.get(entries.size() + first);
        first = entry.getNumber();
      }
    }
    if (last < 0) {
      if (-last > entries.size()) {
        last = Integer.MIN_VALUE;
      }
      else {
        HistoryEntry entry = entries.get(entries.size() + last);
        last = entry.getNumber();
      }
    }
    else if (last == 0) {
      last = Integer.MAX_VALUE;
    }

    if (logger.isDebugEnabled()) {
      logger.debug("first=" + first);
      logger.debug("last=" + last);
    }

    for (HistoryEntry entry : entries) {
      if (entry.getNumber() >= first && entry.getNumber() <= last) {
        res.add(entry);
      }
    }

    return res;
  }

  private HistoryBlock blocks(String key) {
    HistoryBlock block = histories.get(key);
    if (block == null) {
      block = new HistoryBlock();
      histories.put(key, block);
    }

    return block;
  }

  public void saveData(@NotNull Element element) {
    logger.debug("saveData");
    Element hist = new Element("history");

    saveData(hist, SEARCH);
    saveData(hist, COMMAND);
    saveData(hist, EXPRESSION);
    saveData(hist, INPUT);

    element.addContent(hist);
  }

  private void saveData(@NotNull Element element, String key) {
    final HistoryBlock block = histories.get(key);
    if (block == null) {
      return;
    }

    final Element root = new Element("history-" + key);

    for (HistoryEntry entry : block.getEntries()) {
      final Element entryElement = new Element("entry");
      StringHelper.setSafeXmlText(entryElement, entry.getEntry());
      root.addContent(entryElement);
    }

    element.addContent(root);
  }

  public void readData(@NotNull Element element) {
    logger.debug("readData");
    Element hist = element.getChild("history");
    if (hist == null) {
      return;
    }

    readData(hist, SEARCH);
    readData(hist, COMMAND);
    readData(hist, EXPRESSION);
    readData(hist, INPUT);
  }

  private void readData(@NotNull Element element, String key) {
    HistoryBlock block = histories.get(key);
    if (block != null) {
      return;
    }

    block = new HistoryBlock();
    histories.put(key, block);

    final Element root = element.getChild("history-" + key);
    if (root != null) {
      //noinspection unchecked
      List<Element> items = root.getChildren("entry");
      for (Element item : items) {
        final String text = StringHelper.getSafeXmlText(item);
        if (text != null) {
          block.addEntry(text);
        }
      }
    }
  }

  private static int maxLength() {
    NumberOption opt = (NumberOption)Options.getInstance().getOption("history");

    return opt.value();
  }

  private static class HistoryBlock {
    public void addEntry(@NotNull String text) {
      for (int i = 0; i < entries.size(); i++) {
        HistoryEntry entry = entries.get(i);
        if (text.equals(entry.getEntry())) {
          entries.remove(i);
          break;
        }
      }

      entries.add(new HistoryEntry(++counter, text));

      if (entries.size() > maxLength()) {
        entries.remove(0);
      }
    }

    @NotNull
    public List<HistoryEntry> getEntries() {
      return entries;
    }

    @NotNull private final List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
    private int counter;
  }

  public static class HistoryEntry {
    public HistoryEntry(int number, @NotNull String entry) {
      this.number = number;
      this.entry = entry;
    }

    public int getNumber() {
      return number;
    }

    @NotNull
    public String getEntry() {
      return entry;
    }

    private int number;
    @NotNull private String entry;
  }

  @NotNull private Map<String, HistoryBlock> histories = new HashMap<String, HistoryBlock>();

  private static Logger logger = Logger.getInstance(HistoryGroup.class.getName());
}
