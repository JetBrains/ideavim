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
@file:JvmName("NeovimHelper")

package com.maddyhome.idea.vim.neovim

import com.ensarsarajcic.neovim.java.api.types.api.VimCoords
import com.intellij.openapi.editor.LogicalPosition

fun VimCoords.equalsTo(second: VimCoords): Boolean {
  return col == second.col && row == second.row
}

fun LogicalPosition.toVimCoords(): VimCoords {
  return VimCoords(this.line + 1, this.column)
}
