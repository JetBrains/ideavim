/*
 * Copyright 2003-2022 The IdeaVim authors
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

    saveData(hist, SEARCH);
    saveData(hist, COMMAND);
    saveData(hist, EXPRESSION);
    saveData(hist, INPUT);

    element.addContent(hist);
  }

  private void saveData(@NotNull Element element, String key) {
    final HistoryBlock block = getHistories().get(key);
    if (block == null) {
      return;
    }

    final Element root = new Element("history-" + key);

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

    readData(hist, SEARCH);
    readData(hist, COMMAND);
    readData(hist, EXPRESSION);
    readData(hist, INPUT);
  }

  private void readData(@NotNull Element element, String key) {
    HistoryBlock block = getHistories().get(key);
    if (block != null) {
      return;
    }

    block = new HistoryBlock();
    getHistories().put(key, block);

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
