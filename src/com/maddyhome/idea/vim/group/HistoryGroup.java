package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2006 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.option.NumberOption;
import com.maddyhome.idea.vim.option.Options;
import org.jdom.CDATA;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryGroup extends AbstractActionGroup {
  public static final String SEARCH = "search";
  public static final String COMMAND = "cmd";
  public static final String EXPRESSION = "expr";
  public static final String INPUT = "input";

  public void addEntry(String key, String text) {
    if (logger.isDebugEnabled()) {
      logger.debug("Add entry '" + text + "' to " + key);
    }

    HistoryBlock block = blocks(key);
    block.addEntry(text);
  }

  public String getEntry(String key, int index) {
    HistoryBlock block = blocks(key);

    return block.getEntry(index);
  }

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

  /**
   * Allows the group to save its state and any configuration. This does nothing.
   *
   * @param element The plugin's root XML element that this group can add a child to
   */
  public void saveData(Element element) {
    logger.debug("saveData");
    Element hist = new Element("history");

    saveData(hist, SEARCH);
    saveData(hist, COMMAND);
    saveData(hist, EXPRESSION);
    saveData(hist, INPUT);

    element.addContent(hist);
  }

  private void saveData(Element element, String key) {
    HistoryBlock block = histories.get(key);
    if (block == null) {
      return;
    }

    Element root = new Element("history-" + key);

    List<HistoryEntry> elems = block.getEntries();
    for (HistoryEntry entry : elems) {
      Element text = new Element("entry");
      CDATA data = new CDATA(StringHelper.entities(entry.getEntry()));
      text.addContent(data);
      root.addContent(text);
    }

    element.addContent(root);
  }

  /**
   * Allows the group to restore its state and any configuration. This does nothing.
   *
   * @param element The plugin's root XML element that this group can add a child to
   */
  public void readData(Element element) {
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

  private void readData(Element element, String key) {
    HistoryBlock block = histories.get(key);
    if (block != null) {
      return;
    }

    block = new HistoryBlock();
    histories.put(key, block);

    Element root = element.getChild("history-" + key);
    if (root != null) {
      List items = root.getChildren("entry");
      for (Object item : items) {
        block.addEntry(StringHelper.unentities(((Element)item).getText()));
      }
    }
  }

  private static int maxLength() {
    NumberOption opt = (NumberOption)Options.getInstance().getOption("history");

    return opt.value();
  }

  private static class HistoryBlock {
    public void addEntry(String text) {
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

    public String getEntry(int index) {
      if (index < entries.size()) {
        HistoryEntry entry = entries.get(index);
        return entry.getEntry();
      }
      else {
        return null;
      }
    }

    public List<HistoryEntry> getEntries() {
      return entries;
    }

    private List<HistoryEntry> entries = new ArrayList<HistoryEntry>();
    private int counter;
  }

  public static class HistoryEntry {
    public HistoryEntry(int number, String entry) {
      this.number = number;
      this.entry = entry;
    }

    public int getNumber() {
      return number;
    }

    public String getEntry() {
      return entry;
    }

    private int number;
    private String entry;
  }

  private Map<String, HistoryBlock> histories = new HashMap<String, HistoryBlock>();

  private static Logger logger = Logger.getInstance(HistoryGroup.class.getName());
}
