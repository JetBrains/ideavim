/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.group;

import com.google.common.collect.ImmutableList;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.maddyhome.idea.vim.EventFacade;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.ComplicatedKeysAction;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.extension.VimExtensionHandler;
import com.maddyhome.idea.vim.handler.ActionBeanClass;
import com.maddyhome.idea.vim.handler.EditorActionHandlerBase;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.key.*;
import kotlin.Pair;
import kotlin.text.StringsKt;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.*;

import static com.maddyhome.idea.vim.helper.StringHelper.toKeyNotation;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * @author vlan
 */
@State(name = "VimKeySettings", storages = {@Storage(value = "$APP_CONFIG$/vim_settings.xml")})
public class KeyGroup implements PersistentStateComponent<Element> {
  public static final String SHORTCUT_CONFLICTS_ELEMENT = "shortcut-conflicts";
  private static final String SHORTCUT_CONFLICT_ELEMENT = "shortcut-conflict";
  private static final String OWNER_ATTRIBUTE = "owner";
  private static final String TEXT_ELEMENT = "text";

  private static final Logger logger = Logger.getInstance(KeyGroup.class);

  private final @NotNull Map<KeyStroke, ShortcutOwner> shortcutConflicts = new LinkedHashMap<>();
  private final @NotNull Set<RequiredShortcut> requiredShortcutKeys = new HashSet<>(300);
  private final @NotNull Map<MappingMode, CommandPartNode> keyRoots = new EnumMap<>(MappingMode.class);
  private final @NotNull Map<MappingMode, KeyMapping> keyMappings = new EnumMap<>(MappingMode.class);
  private @Nullable OperatorFunction operatorFunction = null;

  void registerRequiredShortcutKeys(@NotNull Editor editor) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(requiredShortcutKeys),
                                 editor.getComponent());
  }

  public void registerShortcutsForLookup(@NotNull LookupImpl lookup) {
    EventFacade.getInstance()
      .registerCustomShortcutSet(VimShortcutKeyAction.getInstance(), toShortcutSet(requiredShortcutKeys),
                                 lookup.getComponent(), lookup);
  }

  void unregisterShortcutKeys(@NotNull Editor editor) {
    EventFacade.getInstance().unregisterCustomShortcutSet(VimShortcutKeyAction.getInstance(), editor.getComponent());
  }

  public boolean showKeyMappings(@NotNull Set<MappingMode> modes, @NotNull Editor editor) {
    List<Pair<EnumSet<MappingMode>, MappingInfo>> rows = getKeyMappingRows(modes);
    final StringBuilder builder = new StringBuilder();
    for (Pair<EnumSet<MappingMode>, MappingInfo> row : rows) {
      MappingInfo mappingInfo = row.getSecond();
      builder.append(StringsKt.padEnd(getModesStringCode(row.getFirst()), 2, ' '));
      builder.append(" ");
      builder.append(StringsKt.padEnd(toKeyNotation(mappingInfo.getFromKeys()), 11, ' '));
      builder.append(" ");
      builder.append(mappingInfo.isRecursive() ? " " : "*");
      builder.append(" ");
      if (mappingInfo instanceof ToKeysMappingInfo) {
        List<KeyStroke> toKeys = ((ToKeysMappingInfo)mappingInfo).getToKeys();
        builder.append(toKeyNotation(toKeys));
      }
      else if (mappingInfo instanceof ToHandlerMappingInfo) {
        final VimExtensionHandler extensionHandler = ((ToHandlerMappingInfo)mappingInfo).getExtensionHandler();
        builder.append("call ");
        builder.append(extensionHandler.getClass().getCanonicalName());
      }
      builder.append("\n");
    }
    ExOutputModel.getInstance(editor).output(builder.toString());
    return true;
  }

  public void removeKeyMapping(@NotNull MappingOwner owner) {
    Arrays.stream(MappingMode.values()).map(this::getKeyMapping).forEach(o -> o.delete(owner));
    unregisterKeyMapping(owner);
  }

  public void putKeyMapping(@NotNull Set<MappingMode> modes,
                            @NotNull List<KeyStroke> fromKeys,
                            @NotNull MappingOwner owner,
                            @NotNull VimExtensionHandler extensionHandler,
                            boolean recursive) {
    modes.stream().map(this::getKeyMapping).forEach(o -> o.put(fromKeys, owner, extensionHandler, recursive));
    registerKeyMapping(fromKeys, owner);
  }

  public void putKeyMapping(@NotNull Set<MappingMode> modes,
                            @NotNull List<KeyStroke> fromKeys,
                            @NotNull MappingOwner owner,
                            @NotNull List<KeyStroke> toKeys,
                            boolean recursive) {
    modes.stream().map(this::getKeyMapping).forEach(o -> o.put(fromKeys, toKeys, owner, recursive));
    registerKeyMapping(fromKeys, owner);
  }

  public List<Pair<List<KeyStroke>, MappingInfo>> getKeyMappingByOwner(@NotNull MappingOwner owner) {
    return Arrays.stream(MappingMode.values()).map(this::getKeyMapping).flatMap(o -> o.getByOwner(owner).stream())
      .collect(toList());
  }

  private void unregisterKeyMapping(MappingOwner owner) {
    final int oldSize = requiredShortcutKeys.size();
    requiredShortcutKeys.removeIf(requiredShortcut -> requiredShortcut.getOwner().equals(owner));
    if (requiredShortcutKeys.size() != oldSize) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        unregisterShortcutKeys(editor);
        registerRequiredShortcutKeys(editor);
      }
    }
  }

  private void registerKeyMapping(@NotNull List<KeyStroke> fromKeys, MappingOwner owner) {
    final int oldSize = requiredShortcutKeys.size();
    for (KeyStroke key : fromKeys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        requiredShortcutKeys.add(new RequiredShortcut(key, owner));
      }
    }
    if (requiredShortcutKeys.size() != oldSize) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        unregisterShortcutKeys(editor);
        registerRequiredShortcutKeys(editor);
      }
    }
  }

  public @Nullable OperatorFunction getOperatorFunction() {
    return operatorFunction;
  }

  public void setOperatorFunction(@NotNull OperatorFunction function) {
    operatorFunction = function;
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

  public @NotNull List<AnAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    final Keymap keymap = keymapManager.getActiveKeymap();
    final KeyboardShortcut shortcut = new KeyboardShortcut(keyStroke, null);
    final Map<String, ? extends List<KeyboardShortcut>> conflicts = keymap.getConflicts("", shortcut);
    final List<AnAction> actions = new ArrayList<>();
    for (String actionId : conflicts.keySet()) {
      final AnAction action = ActionManagerEx.getInstanceEx().getAction(actionId);
      if (action != null) {
        actions.add(action);
      }
    }
    return actions;
  }

  public @NotNull Map<KeyStroke, ShortcutOwner> getShortcutConflicts() {
    final Set<RequiredShortcut> requiredShortcutKeys = this.requiredShortcutKeys;
    final Map<KeyStroke, ShortcutOwner> savedConflicts = getSavedShortcutConflicts();
    final Map<KeyStroke, ShortcutOwner> results = new HashMap<>();
    for (RequiredShortcut requiredShortcut : requiredShortcutKeys) {
      KeyStroke keyStroke = requiredShortcut.getKeyStroke();
      if (!VimShortcutKeyAction.VIM_ONLY_EDITOR_KEYS.contains(keyStroke)) {
        final List<AnAction> conflicts = getKeymapConflicts(keyStroke);
        if (!conflicts.isEmpty()) {
          final ShortcutOwner owner = savedConflicts.get(keyStroke);
          results.put(keyStroke, owner != null ? owner : ShortcutOwner.UNDEFINED);
        }
      }
    }
    return results;
  }

  public @NotNull Map<KeyStroke, ShortcutOwner> getSavedShortcutConflicts() {
    return shortcutConflicts;
  }

  public @NotNull KeyMapping getKeyMapping(@NotNull MappingMode mode) {
    KeyMapping mapping = keyMappings.get(mode);
    if (mapping == null) {
      mapping = new KeyMapping();
      keyMappings.put(mode, mapping);
    }
    return mapping;
  }

  public void resetKeyMappings() {
    keyMappings.clear();
  }

  /**
   * Returns the root of the key mapping for the given mapping mode
   *
   * @param mappingMode The mapping mode
   * @return The key mapping tree root
   */
  public @NotNull CommandPartNode getKeyRoot(@NotNull MappingMode mappingMode) {
    return keyRoots.computeIfAbsent(mappingMode, (key) -> new RootNode());
  }

  /**
   * Registers a shortcut that is handled by KeyHandler#handleKey directly, rather than by an action
   *
   * <p>
   * Digraphs are handled directly by KeyHandler#handleKey instead of via an action, but we need to still make sure the
   * shortcuts are registered, or the key handler won't see them
   * </p>
   *
   * @param keyStroke The shortcut to register
   */
  public void registerShortcutWithoutAction(KeyStroke keyStroke, MappingOwner owner) {
    registerRequiredShortcut(Collections.singletonList(keyStroke), owner);
  }

  public void unregisterCommandActions() {
    requiredShortcutKeys.clear();
    keyRoots.clear();
    if (identityChecker != null) identityChecker.clear();
    if (prefixes != null) prefixes.clear();
  }

  public void registerCommandAction(@NotNull ActionBeanClass actionHolder) {

    if (!VimPlugin.getPluginId().equals(actionHolder.getPluginId())) {
      logger.error("IdeaVim doesn't accept contributions to `vimActions` extension points. " +
                   "Please create a plugin using `VimExtension`. " +
                   "Plugin to blame: " +
                   actionHolder.getPluginId());
      return;
    }

    Set<List<KeyStroke>> actionKeys = actionHolder.getParsedKeys();
    if (actionKeys == null) {
      final EditorActionHandlerBase action = actionHolder.getAction();
      if (action instanceof ComplicatedKeysAction) {
        actionKeys = ((ComplicatedKeysAction)action).getKeyStrokesSet();
      }
      else {
        throw new RuntimeException("Cannot register action: " + action.getClass().getName());
      }
    }

    Set<MappingMode> actionModes = actionHolder.getParsedModes();
    if (actionModes == null) {
      throw new RuntimeException("Cannot register action: " + actionHolder.getImplementation());
    }

    if (ApplicationManager.getApplication().isUnitTestMode()) {
      if (identityChecker == null) {
        identityChecker = new HashMap<>();
        prefixes = new HashMap<>();
      }
      for (List<KeyStroke> keys : actionKeys) {
        checkCommand(actionModes, actionHolder.getAction(), keys);
      }
    }

    for (List<KeyStroke> keyStrokes : actionKeys) {
      registerRequiredShortcut(keyStrokes, MappingOwner.IdeaVim.INSTANCE);

      for (MappingMode mappingMode : actionModes) {
        Node node = getKeyRoot(mappingMode);
        final int len = keyStrokes.size();
        // Add a child for each keystroke in the shortcut for this action
        for (int i = 0; i < len; i++) {
          if (!(node instanceof CommandPartNode)) {
            throw new Error("Error in tree constructing");
          }

          node = addMNode((CommandPartNode)node, actionHolder, keyStrokes.get(i), i == len - 1);
        }
      }
    }
  }

  private void registerRequiredShortcut(@NotNull List<KeyStroke> keys, MappingOwner owner) {
    for (KeyStroke key : keys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        requiredShortcutKeys.add(new RequiredShortcut(key, owner));
      }
    }
  }

  private void checkCommand(@NotNull Set<MappingMode> mappingModes,
                            EditorActionHandlerBase action,
                            List<KeyStroke> keys) {
    for (MappingMode mappingMode : mappingModes) {
      checkIdentity(mappingMode, action.getId(), keys);
    }
    checkCorrectCombination(action, keys);
  }

  private void checkIdentity(MappingMode mappingMode, String actName, List<KeyStroke> keys) {
    Set<List<KeyStroke>> keySets = Objects.requireNonNull(identityChecker).computeIfAbsent(mappingMode, k -> new HashSet<>());
    if (keySets.contains(keys)) {
      throw new RuntimeException(
        "This keymap already exists: " + mappingMode + " keys: " + keys + " action:" + actName);
    }
    keySets.add(keys);
  }

  private void checkCorrectCombination(EditorActionHandlerBase action, List<KeyStroke> keys) {
    for (Map.Entry<List<KeyStroke>, String> entry : Objects.requireNonNull(prefixes).entrySet()) {
      List<KeyStroke> prefix = entry.getKey();
      if (prefix.size() == keys.size()) continue;
      int shortOne = Math.min(prefix.size(), keys.size());
      int i;
      for (i = 0; i < shortOne; i++) {
        if (!prefix.get(i).equals(keys.get(i))) break;
      }

      List<String> actionExceptions = Arrays
        .asList("VimInsertDeletePreviousWordAction", "VimInsertAfterCursorAction", "VimInsertBeforeCursorAction",
                "VimFilterVisualLinesAction", "VimAutoIndentMotionAction");
      if (i == shortOne && !actionExceptions.contains(action.getId()) && !actionExceptions.contains(entry.getValue())) {
        throw new RuntimeException("Prefix found! " +
                                   keys +
                                   " in command " +
                                   action.getId() +
                                   " is the same as " +
                                   prefix.stream().map(Object::toString).collect(joining(", ")) +
                                   " in " +
                                   entry.getValue());
      }
    }
    prefixes.put(keys, action.getId());
  }

  private @Nullable Map<MappingMode, Set<List<KeyStroke>>> identityChecker;
  private @Nullable Map<List<KeyStroke>, String> prefixes;

  private @NotNull Node addMNode(@NotNull CommandPartNode base,
                                 ActionBeanClass actionHolder,
                                 @NotNull KeyStroke key,
                                 boolean isLastInSequence) {
    Node existing = base.get(key);
    if (existing != null) return existing;

    Node newNode;
    if (isLastInSequence) {
      newNode = new CommandNode(actionHolder);
    }
    else {
      newNode = new CommandPartNode();
    }
    base.put(key, newNode);
    return newNode;
  }

  private static @NotNull ShortcutSet toShortcutSet(@NotNull Collection<RequiredShortcut> requiredShortcuts) {
    final List<Shortcut> shortcuts = new ArrayList<>();
    for (RequiredShortcut key : requiredShortcuts) {
      shortcuts.add(new KeyboardShortcut(key.getKeyStroke(), null));
    }
    return new CustomShortcutSet(shortcuts.toArray(new Shortcut[0]));
  }

  private static @NotNull List<Pair<EnumSet<MappingMode>, MappingInfo>> getKeyMappingRows(@NotNull Set<MappingMode> modes) {
    final Map<ImmutableList<KeyStroke>, EnumSet<MappingMode>> actualModes = new HashMap<>();
    for (MappingMode mode : modes) {
      final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
      for (List<KeyStroke> fromKeys : mapping) {
        final ImmutableList<KeyStroke> key = ImmutableList.copyOf(fromKeys);
        final EnumSet<MappingMode> value = actualModes.get(key);
        final EnumSet<MappingMode> newValue;
        if (value != null) {
          newValue = value.clone();
          newValue.add(mode);
        }
        else {
          newValue = EnumSet.of(mode);
        }
        actualModes.put(key, newValue);
      }
    }
    final List<Pair<EnumSet<MappingMode>, MappingInfo>> rows = new ArrayList<>();
    for (Map.Entry<ImmutableList<KeyStroke>, EnumSet<MappingMode>> entry : actualModes.entrySet()) {
      final ArrayList<KeyStroke> fromKeys = new ArrayList<>(entry.getKey());
      final EnumSet<MappingMode> mappingModes = entry.getValue();
      if (!mappingModes.isEmpty()) {
        final MappingMode mode = mappingModes.iterator().next();
        final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
        final MappingInfo mappingInfo = mapping.get(fromKeys);
        if (mappingInfo != null) {
          rows.add(new Pair<>(mappingModes, mappingInfo));
        }
      }
    }
    rows.sort(Comparator.comparing(Pair<EnumSet<MappingMode>, MappingInfo>::getSecond));
    return rows;
  }

  private static @NotNull String getModesStringCode(@NotNull Set<MappingMode> modes) {
    if (modes.equals(MappingMode.NVO)) {
      return "";
    }
    else if (modes.contains(MappingMode.INSERT)) {
      return "i";
    }
    else if (modes.contains(MappingMode.NORMAL)) {
      return "n";
    }
    // TODO: Add more codes
    return "";
  }

  public @NotNull List<AnAction> getActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    results.addAll(getLocalActions(component, keyStroke));
    results.addAll(getKeymapActions(keyStroke));
    return results;
  }

  private static @NotNull List<AnAction> getLocalActions(@NotNull Component component, @NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final KeyboardShortcut keyStrokeShortcut = new KeyboardShortcut(keyStroke, null);
    for (Component c = component; c != null; c = c.getParent()) {
      if (c instanceof JComponent) {
        final List<AnAction> actions = ActionUtil.getActions((JComponent)c);
        for (AnAction action : actions) {
          if (action instanceof VimShortcutKeyAction) {
            continue;
          }
          final com.intellij.openapi.actionSystem.Shortcut[] shortcuts = action.getShortcutSet().getShortcuts();
          for (com.intellij.openapi.actionSystem.Shortcut shortcut : shortcuts) {
            if (shortcut.isKeyboard() && shortcut.startsWith(keyStrokeShortcut) && !results.contains(action)) {
              results.add(action);
            }
          }
        }
      }
    }
    return results;
  }

  private static @NotNull List<AnAction> getKeymapActions(@NotNull KeyStroke keyStroke) {
    final List<AnAction> results = new ArrayList<>();
    final Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    for (String id : keymap.getActionIds(keyStroke)) {
      final AnAction action = ActionManager.getInstance().getAction(id);
      if (action != null) {
        results.add(action);
      }
    }
    return results;
  }

  @Nullable
  @Override
  public Element getState() {
    Element element = new Element("key");
    saveData(element);
    return element;
  }

  @Override
  public void loadState(@NotNull Element state) {
    readData(state);
  }
}
