package org.jetbrains.plugins.ideavim;

import com.google.common.base.Charsets;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.actionSystem.Shortcut;
import com.intellij.openapi.components.impl.stores.StorageUtil;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.impl.KeymapImpl;
import com.intellij.openapi.util.InvalidDataException;
import com.maddyhome.idea.vim.VimKeymapConflictResolveUtil;
import org.jdom.Document;
import org.jdom.Element;

import java.util.ArrayList;

import static com.google.common.collect.Lists.newArrayList;

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

    final Shortcut[] action1Shortcuts = resultKeymap.getShortcuts("action1");
    assertEquals(1, action1Shortcuts.length);
    assertEquals(KeyboardShortcut.fromString("control X"), action1Shortcuts[0]);
    final Shortcut[] action2Shortcuts = resultKeymap.getShortcuts("action2");
    assertEquals(1, action2Shortcuts.length);
    assertEquals(KeyboardShortcut.fromString("control alt V"), action2Shortcuts[0]);
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

    final Shortcut[] action1Shortcuts = resultKeymap.getShortcuts("action1");
    assertEquals(0, action1Shortcuts.length);
    final Shortcut[] action2Shortcuts = resultKeymap.getShortcuts("action2");
    assertEquals(1, action2Shortcuts.length);
    assertEquals(KeyboardShortcut.fromString("control alt V"), action2Shortcuts[0]);
  }

  private static KeymapImpl resolveConflicts(KeymapImpl parentKeymap) throws InvalidDataException {
    final Element stubKeymap = createStubKeymap();
    VimKeymapConflictResolveUtil.resolveConflicts(stubKeymap, parentKeymap);
    KeymapImpl resultKeymap = new KeymapImpl();
    resultKeymap.readExternal(stubKeymap, new Keymap[0]);
    return resultKeymap;
  }

  /**
   * Create simple keymap with bound Ctrl+V and Ctrl+C shortcuts
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
                                                "  \n" +
                                                "</keymap>").getBytes(Charsets.UTF_8));
    assert result != null;
    return result.getRootElement();
  }
}
