/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.group;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.history.HistoryBlock;
import com.maddyhome.idea.vim.history.HistoryEntry;
import com.maddyhome.idea.vim.history.VimHistory;
import com.maddyhome.idea.vim.history.VimHistoryBase;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.List;

import static com.maddyhome.idea.vim.history.HistoryConstants.*;

@State(name = "VimHistorySettings", storages = {
  @Storage(value = "$APP_CONFIG$/vim_settings_local.xml", roamingType = RoamingType.DISABLED)
})
public class HistoryGroup extends VimHistoryBase implements PersistentStateComponent<Element> {

  public void saveData(@NotNull Element element) {
    logger.debug("saveData");
    Element hist = new Element("history");

    for (Type type : getHistories().keySet()) {
      saveData(hist, type);
    }

    element.addContent(hist);
  }

  private void saveData(@NotNull Element element, VimHistory.Type type) {
    final HistoryBlock block = getHistories().get(type);
    if (block == null) {
      return;
    }

    final Element root = new Element("history-" + typeToKey(type));

    for (HistoryEntry entry : block.getEntries()) {
      final Element entryElement = new Element("entry");
      VimPlugin.getXML().setSafeXmlText(entryElement, entry.getEntry());
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

    for (Element child : hist.getChildren()) {
      String key = child.getName().replace("history-", "");
      readData(hist, key);
    }
  }

  private void readData(@NotNull Element element, String key) {
    HistoryBlock block = getHistories().get(key);
    if (block != null) {
      return;
    }

    block = new HistoryBlock();
    getHistories().put(getTypeForString(key), block);

    final Element root = element.getChild("history-" + key);
    if (root != null) {
      List<Element> items = root.getChildren("entry");
      for (Element item : items) {
        final String text = VimPlugin.getXML().getSafeXmlText(item);
        if (text != null) {
          block.addEntry(text);
        }
      }
    }
  }

  private String typeToKey(VimHistory.Type type) {
    if (type instanceof VimHistory.Type.Search) {
      return SEARCH;
    }
    if (type instanceof VimHistory.Type.Command) {
      return COMMAND;
    }
    if (type instanceof VimHistory.Type.Expression) {
      return EXPRESSION;
    }
    if (type instanceof VimHistory.Type.Input) {
      return INPUT;
    }
    if (type instanceof VimHistory.Type.Custom) {
      return ((Type.Custom) type).getId();
    }
    return "unreachable";
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("history");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }

  @TestOnly
  public void clear() {
    getHistories().clear();
  }

  private static final Logger logger = Logger.getInstance(HistoryGroup.class.getName());
}
