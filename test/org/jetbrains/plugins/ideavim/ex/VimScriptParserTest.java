package org.jetbrains.plugins.ideavim.ex;

import com.maddyhome.idea.vim.ex.vimscript.VimScriptGlobalEnvironment;
import org.jetbrains.plugins.ideavim.VimTestCase;

/**
 * @author vlan
 */
public class VimScriptParserTest extends VimTestCase {
  public void testEchoStringLiteral() {
    configureByText("\n");
    typeText(commandToKeys("echo \"Hello, World!\""));
    assertExOutput("Hello, World!\n");
  }

  public void testLetStringLiteralEcho() {
    configureByText("\n");
    typeText(commandToKeys("let s = \"foo\""));
    try {
      typeText(commandToKeys("echo s"));
      assertExOutput("foo\n");
    }
    finally {
      VimScriptGlobalEnvironment.getInstance().getVariables().clear();
    }
  }
}
