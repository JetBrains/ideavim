/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.action.change.insert

import com.maddyhome.idea.vim.command.CommandState
import org.jetbrains.plugins.ideavim.VimTestCase

class InsertExitModeActionTest : VimTestCase() {
  fun `test exit visual mode`() {
    doTest("i<Esc>", "12${c}3", "1${c}23", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }

  fun `test exit visual mode on line start`() {
    doTest("i<Esc>", "${c}123", "${c}123", CommandState.Mode.COMMAND, CommandState.SubMode.NONE)
  }
}
