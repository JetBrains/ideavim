/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex;

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
    typeText(commandToKeys("echo s"));
    assertExOutput("foo\n");
  }

  public void testLetStringLiteralEchoWithNumeric() {
    configureByText("\n");
    typeText(commandToKeys("let s = 100"));
    typeText(commandToKeys("echo s"));
    assertExOutput("100\n");
  }
}
