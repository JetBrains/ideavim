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

package com.maddyhome.idea.vim;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.MouseShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.util.SystemInfo;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.util.*;

import static com.google.common.collect.Lists.newLinkedList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * User: zolotov
 * Date: 1/8/13
 */
public class VimKeymapConflictResolveUtil {
  private static final String SHIFT = "shift";
  private static final String CONTROL = "control";
  private static final String META = "meta";
  private static final String ALT = "alt";
  private static final String ALT_GRAPH = "altGraph";
  private static final String DOUBLE_CLICK = "doubleClick";

  private static final String VIM_KEY_HANDLER_ACTION_ID = "VimKeyHandler";
  private static final String ACTION_TAG = "action";
  private static final String KEYBOARD_SHORTCUT_TAG = "keyboard-shortcut";
  private static final String MOUSE_SHORTCUT_TAG = "mouse-shortcut";
  private static final String ID_ATTRIBUTE = "id";
  private static final String FIRST_KEYSTROKE_ATTRIBUTE = "first-keystroke";
  private static final String KEYSTROKE_ATTRIBUTE = "keystroke";
  private static final String SECOND_KEYSTROKE_ATTRIBUTE = "second-keystroke";

  private static final List<Integer> ALTERNATIVE_MODIFIERS = ImmutableList.of(
    InputEvent.ALT_DOWN_MASK,
    InputEvent.CTRL_DOWN_MASK,
    InputEvent.SHIFT_DOWN_MASK,
    InputEvent.META_DOWN_MASK,
    InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK,
    InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
    InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK,
    InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK,
    InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);

  public static void resolveConflicts(Element targetKeymapRoot, Keymap parentKeymap) {
    final Collection<String> vimHandlingShortcuts = getVimHandlingShortcuts(targetKeymapRoot);
    final Map<String, List<Shortcut>> shortcutsToOverride = retrieveShortcutsToOverride(vimHandlingShortcuts, parentKeymap);
    overrideShortcuts(targetKeymapRoot, shortcutsToOverride);
  }

  /**
   * @param targetKeymapRoot root element of Vim keymap
   * @return all shortcuts in string representation that should be handled by Vim plugin
   */
  private static Collection<String> getVimHandlingShortcuts(Element targetKeymapRoot) {
    Element vimKeyHandlerAction = retrieveActionElement(targetKeymapRoot);
    if (vimKeyHandlerAction != null) {
      Collection<String> result = newLinkedList();
      for (Object childAction : vimKeyHandlerAction.getChildren()) {
        if (childAction instanceof Element) {
          Element shortcut = (Element)childAction;
          result.add(shortcut.getAttributeValue(FIRST_KEYSTROKE_ATTRIBUTE));
        }
      }
      return result;
    }
    return Collections.emptyList();
  }

  /**
   * @param targetKeymapRoot root element of Vim keymap
   * @return Retrieve VimKeyHandler action element
   */
  @Nullable
  private static Element retrieveActionElement(Element targetKeymapRoot) {
    Element vimKeyHandlerAction = null;
    for (Object child : targetKeymapRoot.getChildren(ACTION_TAG)) {
      if (child instanceof Element) {
        Element action = (Element)child;
        if (VIM_KEY_HANDLER_ACTION_ID.equals(action.getAttributeValue(ID_ATTRIBUTE))) {
          vimKeyHandlerAction = action;
          break;
        }
      }
    }
    return vimKeyHandlerAction;
  }

  /**
   * @param vimHandlingShortcuts collection of shortcuts that should be handled by Vim plugin
   * @param parentKeymap         selected parent keymap for vim keymap
   * @return mapping of action names to its shortcuts that we should save in Vim-keymap
   *         (or empty list of shortcuts if action just should be disabled)
   */
  private static Map<String, List<Shortcut>> retrieveShortcutsToOverride(Collection<String> vimHandlingShortcuts, Keymap parentKeymap) {
    Map<String, List<Shortcut>> result = newHashMap();
    for (String shortcut : vimHandlingShortcuts) {
      final Map<String, ArrayList<KeyboardShortcut>> conflicts = parentKeymap.getConflicts("", KeyboardShortcut.fromString(shortcut));
      for (Map.Entry<String, ArrayList<KeyboardShortcut>> conflict : conflicts.entrySet()) {
        String actionName = conflict.getKey();
        final ArrayList<KeyboardShortcut> conflictedShortcuts = conflict.getValue();
        if (result.containsKey(actionName)) {
          // found another conflict for already overridden action
          List<Shortcut> overridesShortcuts = result.get(actionName);
          for (KeyboardShortcut conflictedShortcut : conflictedShortcuts) {
            overridesShortcuts.remove(conflictedShortcut);
          }
        }
        else {
          // let's override action with all non-conflict shortcuts
          List<Shortcut> overriddenShortcuts = newLinkedList();
          for (Shortcut actionShortcut : parentKeymap.getShortcuts(actionName)) {
            if (!(actionShortcut instanceof KeyboardShortcut) || !conflictedShortcuts.contains(actionShortcut)) {
              overriddenShortcuts.add(actionShortcut);
            }
          }
          if (overriddenShortcuts.isEmpty()) {
            for (Integer modifier : ALTERNATIVE_MODIFIERS) {
              if (!SystemInfo.isMac && (modifier & InputEvent.META_DOWN_MASK) != 0) {
                continue;
              }
              final KeyStroke originalStroke = KeyStroke.getKeyStroke(shortcut);
              final int modifiers = originalStroke.getModifiers() | modifier;
              //noinspection MagicConstant
              final KeyStroke stroke = KeyStroke.getKeyStroke(originalStroke.getKeyCode(), modifiers);
              final KeyboardShortcut alternativeShortcut = new KeyboardShortcut(stroke, null);
              if (parentKeymap.getConflicts("", alternativeShortcut).isEmpty()) {
                overriddenShortcuts.add(alternativeShortcut);
                break;
              }
            }

          }
          result.put(actionName, overriddenShortcuts);
        }
      }
    }
    return result;
  }

  /**
   * Fill vim keymap with overridden actions.
   * Only keyboard and mouse shortcuts will be overridden.
   *
   * @param targetKeymapRoot    root element of Vim keymap
   * @param shortcutsToOverride overriding mapping: actions -> shortcuts_should_be_saved
   */

  private static void overrideShortcuts(Element targetKeymapRoot, Map<String, List<Shortcut>> shortcutsToOverride) {
    for (Map.Entry<String, List<Shortcut>> action : shortcutsToOverride.entrySet()) {
      targetKeymapRoot.addContent(createActionElement(action.getKey(), action.getValue()));
    }
  }

  public static Element createActionElement(String actionName, List<Shortcut> shortcuts) {
    final Element overridesAction = new Element(ACTION_TAG);
    overridesAction.setAttribute(ID_ATTRIBUTE, actionName);
    for (Shortcut shortcut : shortcuts) {
      if (shortcut instanceof KeyboardShortcut) {
        KeyboardShortcut keyboardShortcut = (KeyboardShortcut)shortcut;
        final String firstShortcutString = KeymapImpl.getKeyShortcutString(keyboardShortcut.getFirstKeyStroke());
        final Element shortcutElement = createShortcutElement(KEYBOARD_SHORTCUT_TAG, FIRST_KEYSTROKE_ATTRIBUTE,
                                                              firstShortcutString);
        overridesAction.addContent(shortcutElement);
        final KeyStroke secondKeyStroke = keyboardShortcut.getSecondKeyStroke();
        if (secondKeyStroke != null) {
          final String secondShortcutString = KeymapImpl.getKeyShortcutString(secondKeyStroke);
          shortcutElement.setAttribute(SECOND_KEYSTROKE_ATTRIBUTE, secondShortcutString);
        }
      }
      else if (shortcut instanceof MouseShortcut) {
        overridesAction.addContent(createShortcutElement(MOUSE_SHORTCUT_TAG, KEYSTROKE_ATTRIBUTE, getMouseShortcutString((MouseShortcut)shortcut)));
      }
    }
    return overridesAction;
  }

  private static Element createShortcutElement(String elementName, String shortcutAttributeName, String shortcut) {
    final Element shortcutElement = new Element(elementName);
    shortcutElement.setAttribute(shortcutAttributeName, shortcut);
    return shortcutElement;
  }

  /**
   * Create string representation of mouse shortcut
   * KeymapImpl has implementation for mouse shortcut marshaling, but it is private :-(
   *
   * @param shortcut mouse shortcut
   * @return string representation of mouse shortcut
   */
  private static String getMouseShortcutString(MouseShortcut shortcut) {
    StringBuilder builder = new StringBuilder();
    int modifiers = shortcut.getModifiers();
    if ((MouseEvent.SHIFT_DOWN_MASK & modifiers) > 0) {
      builder.append(SHIFT).append(' ');
    }
    if ((MouseEvent.CTRL_DOWN_MASK & modifiers) > 0) {
      builder.append(CONTROL).append(' ');
    }
    if ((MouseEvent.META_DOWN_MASK & modifiers) > 0) {
      builder.append(META).append(' ');
    }
    if ((MouseEvent.ALT_DOWN_MASK & modifiers) > 0) {
      builder.append(ALT).append(' ');
    }
    if ((MouseEvent.ALT_GRAPH_DOWN_MASK & modifiers) > 0) {
      builder.append(ALT_GRAPH).append(' ');
    }
    builder.append("button").append(shortcut.getButton()).append(' ');
    if (shortcut.getClickCount() > 1) {
      builder.append(DOUBLE_CLICK);
    }
    return builder.toString().trim();
  }

}
