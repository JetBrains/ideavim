package org.jetbrains.plugins.ideavim.ex;

import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author vlan
 */
public class MapCommandTest extends VimTestCase {
  public void testMapKtoJ() {
    configureByText("<caret>foo\n" +
                    "bar\n");
    typeText(commandToKeys("nmap k j"));
    assertOffset(0);
    assertPluginError(false);
    typeText(parseKeys("k"));
    assertOffset(4);
  }
}
