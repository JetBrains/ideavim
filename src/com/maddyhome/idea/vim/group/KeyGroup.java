package com.maddyhome.idea.vim.group;

import com.google.common.collect.ImmutableList;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.ex.ActionManagerEx;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.action.VimCommandAction;
import com.maddyhome.idea.vim.action.VimShortcutKeyAction;
import com.maddyhome.idea.vim.command.Argument;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.MappingMode;
import com.maddyhome.idea.vim.ex.ExOutputModel;
import com.maddyhome.idea.vim.helper.StringHelper;
import com.maddyhome.idea.vim.key.*;
import com.maddyhome.idea.vim.key.Shortcut;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.*;

import static com.maddyhome.idea.vim.helper.StringHelper.leftJustify;
import static com.maddyhome.idea.vim.helper.StringHelper.toKeyNotation;

/**
 * @author vlan
 */
public class KeyGroup {
  public static final String SHORTCUT_CONFLICTS_ELEMENT = "shortcut-conflicts";
  public static final String SHORTCUT_CONFLICT_ELEMENT = "shortcut-conflict";
  public static final String OWNER_ATTRIBUTE = "owner";
  public static final String TEXT_ELEMENT = "text";

  @NotNull private Map<KeyStroke, ShortcutOwner> shortcutConflicts = new LinkedHashMap<KeyStroke, ShortcutOwner>();
  @NotNull private Set<KeyStroke> requiredShortcutKeys = new HashSet<KeyStroke>();
  @NotNull private HashMap<MappingMode, RootNode> keyRoots = new HashMap<MappingMode, RootNode>();
  @NotNull private Map<MappingMode, KeyMapping> keyMappings = new HashMap<MappingMode, KeyMapping>();

  public void registerRequiredShortcutKeys(@NotNull Editor editor) {
    final Set<KeyStroke> requiredKeys = VimPlugin.getKey().getRequiredShortcutKeys();
    final JComponent component = editor.getComponent();
    final AnAction action = getShortcutKeyAction();
    action.registerCustomShortcutSet(toShortcutSet(requiredKeys), component);
  }

  public void unregisterShortcutKeys(@NotNull Editor editor) {
    getShortcutKeyAction().unregisterCustomShortcutSet(editor.getComponent());
  }

  public boolean showKeyMappings(@NotNull Set<MappingMode> modes, @NotNull Editor editor) {
    final List<MappingInfo> rows = getKeyMappingRows(modes);
    final StringBuilder builder = new StringBuilder();
    for (MappingInfo row : rows) {
      builder.append(leftJustify(getModesStringCode(row.getMappingModes()), 2, ' '));
      builder.append(" ");
      builder.append(leftJustify(toKeyNotation(row.getFromKeys()), 11, ' '));
      builder.append(" ");
      builder.append(row.isRecursive() ? " " : "*");
      builder.append(" ");
      builder.append(toKeyNotation(row.getToKeys()));
      builder.append("\n");
    }
    ExOutputModel.getInstance(editor).output(builder.toString());
    return true;
  }

  public void putKeyMapping(@NotNull Set<MappingMode> modes, @NotNull List<KeyStroke> fromKeys,
                            @NotNull List<KeyStroke> toKeys, boolean recursive) {
    for (MappingMode mode : modes) {
      final KeyMapping mapping = getKeyMapping(mode);
      mapping.put(EnumSet.of(mode), fromKeys, toKeys, recursive);
    }
    final int oldSize = requiredShortcutKeys.size();
    for (KeyStroke key : fromKeys) {
      if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
        requiredShortcutKeys.add(key);
      }
    }
    if (requiredShortcutKeys.size() != oldSize) {
      for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
        unregisterShortcutKeys(editor);
        registerRequiredShortcutKeys(editor);
      }
    }
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

  @NotNull
  public List<AnAction> getKeymapConflicts(@NotNull KeyStroke keyStroke) {
    final KeymapManagerEx keymapManager = KeymapManagerEx.getInstanceEx();
    final Keymap keymap = keymapManager.getActiveKeymap();
    final KeyboardShortcut shortcut = new KeyboardShortcut(keyStroke, null);
    final Map<String, ArrayList<KeyboardShortcut>> conflicts = keymap.getConflicts("", shortcut);
    final List<AnAction> actions = new ArrayList<AnAction>();
    for (String actionId : conflicts.keySet()) {
      final AnAction action = ActionManagerEx.getInstanceEx().getAction(actionId);
      if (action != null) {
        actions.add(action);
      }
    }
    return actions;
  }

  @NotNull
  public Map<KeyStroke, ShortcutOwner> getShortcutConflicts() {
    final Set<KeyStroke> requiredShortcutKeys = getRequiredShortcutKeys();
    final Map<KeyStroke, ShortcutOwner> savedConflicts = getSavedShortcutConflicts();
    final Map<KeyStroke, ShortcutOwner> results = new HashMap<KeyStroke, ShortcutOwner>();
    for (KeyStroke keyStroke : requiredShortcutKeys) {
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

  @NotNull
  public Map<KeyStroke, ShortcutOwner> getSavedShortcutConflicts() {
    return shortcutConflicts;
  }

  @NotNull
  public Set<KeyStroke> getRequiredShortcutKeys() {
    return requiredShortcutKeys;
  }

  @NotNull
  public KeyMapping getKeyMapping(@NotNull MappingMode mode) {
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
  public RootNode getKeyRoot(@NotNull MappingMode mappingMode) {
    RootNode res = keyRoots.get(mappingMode);
    // Create the root node if one doesn't exist yet for this mode
    if (res == null) {
      res = new RootNode();
      keyRoots.put(mappingMode, res);
    }

    return res;
  }

  public void registerCommandAction(@NotNull VimCommandAction commandAction, @NotNull String actionId) {
    final List<Shortcut> shortcuts = new ArrayList<Shortcut>();
    for (List<KeyStroke> keyStrokes : commandAction.getKeyStrokesSet()) {
      shortcuts.add(new Shortcut(keyStrokes.toArray(new KeyStroke[keyStrokes.size()])));
    }
    //noinspection deprecation
    registerAction(commandAction.getMappingModes(), actionId, commandAction.getType(), commandAction.getFlags(),
                   shortcuts.toArray(new Shortcut[shortcuts.size()]), commandAction.getArgumentType());
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, Shortcut shortcut) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, new Shortcut[]{shortcut});
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, Shortcut shortcut) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, cmdFlags, new Shortcut[]{shortcut});
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, Shortcut shortcut,
                             @NotNull Argument.Type argType) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, new Shortcut[]{shortcut}, argType);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, Shortcut shortcut,
                             @NotNull Argument.Type argType) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, cmdFlags, new Shortcut[]{shortcut}, argType);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, @NotNull Shortcut[] shortcuts) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, 0, shortcuts);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, @NotNull Shortcut[] shortcuts,
                             @NotNull Argument.Type argType) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, 0, shortcuts, argType);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull Shortcut[] shortcuts) {
    //noinspection deprecation
    registerAction(mappingModes, actName, cmdType, cmdFlags, shortcuts, Argument.Type.NONE);
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  public void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull Shortcut[] shortcuts,
                             @NotNull Argument.Type argType) {
    for (Shortcut shortcut : shortcuts) {
      final KeyStroke[] keys = shortcut.getKeys();
      for (KeyStroke key : keys) {
        if (key.getKeyChar() == KeyEvent.CHAR_UNDEFINED) {
          requiredShortcutKeys.add(key);
        }
      }
      //noinspection deprecation
      registerAction(mappingModes, actName, cmdType, cmdFlags, keys, argType);
    }
  }

  /**
   * @deprecated Inherit your action from {@link com.maddyhome.idea.vim.action.VimCommandAction} instead.
   */
  @Deprecated
  private void registerAction(@NotNull Set<MappingMode> mappingModes, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull KeyStroke[] keys,
                              @NotNull Argument.Type argType) {
    for (MappingMode mappingMode : mappingModes) {
      Node node = getKeyRoot(mappingMode);
      final int len = keys.length;
      // Add a child for each keystroke in the shortcut for this action
      for (int i = 0; i < len; i++) {
        if (node instanceof ParentNode) {
          final ParentNode base = (ParentNode)node;
          node = addNode(base, actName, cmdType, cmdFlags, keys[i], argType, i == len - 1);
        }
      }
    }
  }

  @Nullable
  private Node addNode(@NotNull ParentNode base, @NotNull String actName, @NotNull Command.Type cmdType, int cmdFlags, @NotNull KeyStroke key,
                       @NotNull Argument.Type argType, boolean last) {
    // Lets get the actual action for the supplied action name
    ActionManager aMgr = ActionManager.getInstance();
    AnAction action = aMgr.getAction(actName);
    assert action != null;

    Node node = base.getChild(key);
    // Is this the first time we have seen this character at this point in the tree?
    if (node == null) {
      // If this is the last keystroke in the shortcut, and there is no argument, add a command node
      if (last && argType == Argument.Type.NONE) {
        node = new CommandNode(key, actName, action, cmdType, cmdFlags);
      }
      // If this are more keystrokes in the shortcut or there is an argument, add a branch node
      else {
        node = new BranchNode(key, cmdFlags);
      }

      base.addChild(node, key);
    }

    // If this is the last keystroke in the shortcut and we have an argument, add an argument node
    if (last && node instanceof BranchNode && argType != Argument.Type.NONE) {
      ArgumentNode arg = new ArgumentNode(actName, action, cmdType, argType, cmdFlags);
      ((BranchNode)node).addChild(arg, BranchNode.ARGUMENT);
    }

    return node;
  }

  @NotNull
  private static AnAction getShortcutKeyAction() {
    return ActionManagerEx.getInstanceEx().getAction("VimShortcutKeyAction");
  }

  @NotNull
  private static ShortcutSet toShortcutSet(@NotNull Collection<KeyStroke> keyStrokes) {
    final List<com.intellij.openapi.actionSystem.Shortcut> shortcuts = new ArrayList<com.intellij.openapi.actionSystem.Shortcut>();
    for (KeyStroke key : keyStrokes) {
      shortcuts.add(new KeyboardShortcut(key, null));
    }
    return new CustomShortcutSet(shortcuts.toArray(new com.intellij.openapi.actionSystem.Shortcut[shortcuts.size()]));
  }

  private static List<MappingInfo> getKeyMappingRows(@NotNull Set<MappingMode> modes) {
    final Map<ImmutableList<KeyStroke>, Set<MappingMode>> actualModes = new HashMap<ImmutableList<KeyStroke>, Set<MappingMode>>();
    for (MappingMode mode : modes) {
      final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
      for (List<KeyStroke> fromKeys : mapping) {
        final ImmutableList<KeyStroke> key = ImmutableList.copyOf(fromKeys);
        final Set<MappingMode> value = actualModes.get(key);
        final Set<MappingMode> newValue;
        if (value != null) {
          newValue = new HashSet<MappingMode>(value);
          newValue.add(mode);
        }
        else {
          newValue = EnumSet.of(mode);
        }
        actualModes.put(key, newValue);
      }
    }
    final List<MappingInfo> rows = new ArrayList<MappingInfo>();
    for (Map.Entry<ImmutableList<KeyStroke>, Set<MappingMode>> entry : actualModes.entrySet()) {
      final ArrayList<KeyStroke> fromKeys = new ArrayList<KeyStroke>(entry.getKey());
      final Set<MappingMode> mappingModes = entry.getValue();
      if (!mappingModes.isEmpty()) {
        final MappingMode mode = mappingModes.iterator().next();
        final KeyMapping mapping = VimPlugin.getKey().getKeyMapping(mode);
        final MappingInfo mappingInfo = mapping.get(fromKeys);
        if (mappingInfo != null) {
          rows.add(new MappingInfo(mappingModes, mappingInfo.getFromKeys(), mappingInfo.getToKeys(),
                                   mappingInfo.isRecursive()));
        }
      }
    }
    Collections.sort(rows);
    return rows;
  }

  private static String getModesStringCode(@NotNull Set<MappingMode> modes) {
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
}
