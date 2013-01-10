package org.jetbrains.plugins.ideavim;

import com.google.common.base.Charsets;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.components.impl.stores.StorageUtil;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.util.InvalidDataException;
import com.maddyhome.idea.vim.VimKeymapConflictResolveUtil;
import org.jdom.Document;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;

/**
 * User: zolotov
 * Date: 1/10/13
 */
public class KeymapGenerationTest extends VimTestCase {

  public void testOverrideSingleConflict() throws Exception {
    final KeymapImpl parentKeymap = new KeymapImpl();
    parentKeymap.addShortcut("action1", KeyboardShortcut.fromString("control C")); //should be overridden
    parentKeymap.addShortcut("action1", KeyboardShortcut.fromString("control X"));
    parentKeymap.addShortcut("action2", KeyboardShortcut.fromString("control V")); //should be overridden
    parentKeymap.addShortcut("action3", KeyboardShortcut.fromString("control Z"));

    final KeymapImpl resultKeymap = resolveConflicts(parentKeymap);
    final ArrayList<String> allShortcuts = newArrayList(resultKeymap.getActionIds());
    assertContainsElements(allShortcuts, "VimKeyHandler", "action1", "action2");
    assertDoesntContain(allShortcuts, "action3");

    assertEquals(1, resultKeymap.getShortcuts("action1").length);
    assertEquals(KeyboardShortcut.fromString("control X"), resultKeymap.getShortcuts("action1")[0]);
    assertEquals(0, resultKeymap.getShortcuts("action2").length);
  }

  public void testOverrideDoubleConflict() throws Exception {
    final KeymapImpl parentKeymap = new KeymapImpl();
    parentKeymap.addShortcut("action1", KeyboardShortcut.fromString("control C")); //should be overridden
    parentKeymap.addShortcut("action1", KeyboardShortcut.fromString("control V")); //should be overridden
    parentKeymap.addShortcut("action2", KeyboardShortcut.fromString("control V")); //should be overridden
    parentKeymap.addShortcut("action3", KeyboardShortcut.fromString("control Z"));

    final KeymapImpl resultKeymap = resolveConflicts(parentKeymap);
    final ArrayList<String> allShortcuts = newArrayList(resultKeymap.getActionIds());
    assertContainsElements(allShortcuts, "VimKeyHandler", "action1", "action2");
    assertDoesntContain(allShortcuts, "action3");

    assertEquals(0, resultKeymap.getShortcuts("action1").length);
    assertEquals(0, resultKeymap.getShortcuts("action2").length);
  }

  public void testAddExtraShortcutForOverridedMethods() throws Exception {
    final String actionName = "action";
    final String extraShortcut = "control D";

    KeymapImpl parentKeymap = new KeymapImpl();
    parentKeymap.addShortcut(actionName, KeyboardShortcut.fromString("control C")); //should be overridden

    final Map<String, List<String>> extraShortcuts = newHashMap();
    extraShortcuts.put(actionName, newArrayList(extraShortcut));

    final KeymapImpl resultKeymap = resolveConflicts(parentKeymap);
    assertEquals(1, resultKeymap.getShortcuts(actionName).length);
    assertEquals(KeyboardShortcut.fromString(extraShortcut), resultKeymap.getShortcuts(actionName)[0]);
  }

  public void testDoNotAddExtraShortcutIfAtLeastOneShortcutHadBeenSaved() throws Exception {
    final String actionName = "action";
    final String originalShortcut = "control C";
    final String extraShortcut = "control D";

    KeymapImpl parentKeymap = new KeymapImpl();
    parentKeymap.addShortcut(actionName, KeyboardShortcut.fromString(originalShortcut)); //should be overridden
    parentKeymap.addShortcut(actionName, KeyboardShortcut.fromString("control X"));

    final Map<String, List<String>> extraShortcuts = newHashMap();
    extraShortcuts.put(actionName, newArrayList(extraShortcut));

    final KeymapImpl resultKeymap = resolveConflicts(parentKeymap);
    assertEquals(1, resultKeymap.getShortcuts(actionName).length);
    assertEquals(KeyboardShortcut.fromString(originalShortcut), resultKeymap.getShortcuts(actionName)[0]);
  }

  private static KeymapImpl resolveConflicts(KeymapImpl parentKeymap) throws InvalidDataException {
    final Element stubKeymap = createStubKeymap();
    VimKeymapConflictResolveUtil.resolveConflicts(stubKeymap, parentKeymap);
    KeymapImpl resultKeymap = new KeymapImpl();
    resultKeymap.readExternal(stubKeymap, new Keymap[0]);
    return resultKeymap;
  }

  /**
   * Create simple keymap with binded Ctrl+V and Ctrl+C shortcuts
   *
   * @return root element of keymap in xml representation
   */
  private static Element createStubKeymap() {
    Document result = StorageUtil.loadDocument(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                                "<keymap version=\"1\" name=\"Vim\" disable-mnemonics=\"false\" parent=\"$default\">\n" +
                                                "  <action id=\"VimKeyHandler\">\n" +
                                                "    <keyboard-shortcut first-keystroke=\"control C\"/>\n" +
                                                "    <keyboard-shortcut first-keystroke=\"control V\"/>\n" +
                                                "  </action>\n" +
                                                "</keymap>").getBytes(Charsets.UTF_8));
    assert result != null;
    return result.getRootElement();
  }
}
