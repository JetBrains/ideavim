package com.maddyhome.idea.vim.group;

import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.key.ShortcutOwner;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author vlan
 */
public class KeyGroup {
  public static final String SHORTCUT_CONFLICTS_ELEMENT = "shortcut-conflicts";
  public static final String SHORTCUT_CONFLICT_ELEMENT = "shortcut-conflict";
  public static final String OWNER_ATTRIBUTE = "owner";
  public static final String TEXT_ELEMENT = "text";

  private Map<KeyStroke, ShortcutOwner> shortcutConflicts = new LinkedHashMap<KeyStroke, ShortcutOwner>();

  @NotNull
  public Map<KeyStroke, ShortcutOwner> getSavedShortcutConflicts() {
    return shortcutConflicts;
  }


  public void saveData(@NotNull Element element) {
    final Element conflictsElement = new Element(SHORTCUT_CONFLICTS_ELEMENT);
    for (Map.Entry<KeyStroke, ShortcutOwner> entry : shortcutConflicts.entrySet()) {
      final ShortcutOwner owner = entry.getValue();
      if (owner != ShortcutOwner.UNDEFINED) {
        final Element conflictElement = new Element(SHORTCUT_CONFLICT_ELEMENT);
        conflictElement.setAttribute(OWNER_ATTRIBUTE, owner.getName());
        final Element textElement = new Element(TEXT_ELEMENT);
        StringHelper.setSafeXmlText(textElement, entry.getKey().toString());
        conflictElement.addContent(textElement);
        conflictsElement.addContent(conflictElement);
      }
    }
    element.addContent(conflictsElement);
  }

  public void readData(@NotNull Element element) {
    final Element conflictsElement = element.getChild(SHORTCUT_CONFLICTS_ELEMENT);
    if (conflictsElement != null) {
      final java.util.List<Element> conflictElements = conflictsElement.getChildren(SHORTCUT_CONFLICT_ELEMENT);
      for (Element conflictElement : conflictElements) {
        final String ownerValue = conflictElement.getAttributeValue(OWNER_ATTRIBUTE);
        ShortcutOwner owner = ShortcutOwner.UNDEFINED;
        try {
          owner = ShortcutOwner.fromString(ownerValue);
        }
        catch (IllegalArgumentException ignored) {
        }
        final Element textElement = conflictElement.getChild(TEXT_ELEMENT);
        if (textElement != null) {
          final String text = StringHelper.getSafeXmlText(textElement);
          if (text != null) {
            final KeyStroke keyStroke = KeyStroke.getKeyStroke(text);
            if (keyStroke != null) {
              shortcutConflicts.put(keyStroke, owner);
            }
          }
        }
      }
    }
  }
}
