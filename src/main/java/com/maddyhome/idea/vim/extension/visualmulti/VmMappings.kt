/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.extension.visualmulti

import com.intellij.openapi.util.NlsSafe
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.common.Direction
import com.maddyhome.idea.vim.extension.ExtensionHandler
import com.maddyhome.idea.vim.extension.multiplecursors.AllOccurrencesHandler
import com.maddyhome.idea.vim.extension.multiplecursors.NewSelection
import com.maddyhome.idea.vim.extension.multiplecursors.NextOccurrenceHandler
import com.maddyhome.idea.vim.extension.multiplecursors.RemoveOccurrenceHandler
import com.maddyhome.idea.vim.extension.multiplecursors.SkipOccurrenceHandler
import com.maddyhome.idea.vim.vimscript.model.datatypes.VimString

/**
 * The default `g:VM_leader`
 */
@NlsSafe
private const val LEADER = "<Bslash><Bslash>"

/**
 * A mapping of vim-visual-multi
 *
 * @param name The name in `g:VM_maps`
 * @param plug The `<Plug>` mapping, which is the name in the `<Plug>(VM-Name-With-Dashes)` form
 * @param alwaysMapped Whether the default keys are mapped even if `g:VM_default_mappings` is `0`
 */
internal class VmMapping(
  val name: String,
  val modes: Set<MappingMode>,
  val defaultKeys: String,
  val alwaysMapped: Boolean = false,
  val createHandler: () -> ExtensionHandler,
) {
  @NlsSafe
  val plug = "<Plug>(VM-${name.replace(' ', '-')})"
}

/**
 * The mappings available before a session starts. They start the session.
 */
internal fun getPermanentMappings(): List<VmMapping> {
  val leader = getLeader()
  return listOf(
    VmMapping("Find Under", MappingMode.N, "<C-n>", alwaysMapped = true) {
      NextOccurrenceHandler(wholeWord = true, VmCaseSetting, acceptsCount = true)
    },
    VmMapping("Find Subword Under", MappingMode.X, "<C-n>", alwaysMapped = true) {
      NextOccurrenceHandler(wholeWord = false, VmCaseSetting, acceptsCount = true, NewSelection.START_SEARCH)
    },
    VmMapping("Add Cursor Down", MappingMode.N, "<C-Down>") {
      NextLineHandler(NextLineHandler.Direction.DOWN)
    },
    VmMapping("Add Cursor Up", MappingMode.N, "<C-Up>") {
      NextLineHandler(NextLineHandler.Direction.UP)
    },
    VmMapping("Select All", MappingMode.N, "${leader}A") {
      AllOccurrencesHandler(
        wholeWord = true,
        VmCaseSetting
      )
    },
    VmMapping("Visual All", MappingMode.X, "${leader}A") {
      AllOccurrencesHandler(
        wholeWord = false,
        VmCaseSetting
      )
    },
    VmMapping("Visual Cursors", MappingMode.X, "${leader}c") { VisualCursorsHandler() },
    VmMapping("Visual Add", MappingMode.X, "${leader}a") { VisualAddHandler(VmCaseSetting) },
  )
}

/**
 * The mappings active only inside a session. Outside it, the keys keep their Vim meaning.
 *
 * vim-visual-multi maps them regardless of `g:VM_default_mappings`, and in Normal mode, because it stays in Normal mode
 * and draws the selections itself. Our selections are native, so we map them in Visual mode.
 */
internal val sessionMappings = listOf(
  VmMapping("Find Next", MappingMode.X, "n", alwaysMapped = true) {
    NextOccurrenceHandler(wholeWord = false, VmCaseSetting, acceptsCount = true, NewSelection.ADD_NEXT)
  },
  VmMapping("Find Prev", MappingMode.X, "N", alwaysMapped = true) {
    NextOccurrenceHandler(
      wholeWord = false,
      VmCaseSetting,
      acceptsCount = true,
      NewSelection.ADD_NEXT,
      Direction.BACKWARDS,
    )
  },
  VmMapping("Switch Mode", MappingMode.N + MappingMode.X, "<Tab>", alwaysMapped = true) { SwitchModeHandler() },
  VmMapping("Goto Next", MappingMode.X, "]", alwaysMapped = true) { GotoRegionHandler(Direction.FORWARDS) },
  VmMapping("Goto Prev", MappingMode.X, "[", alwaysMapped = true) { GotoRegionHandler(Direction.BACKWARDS) },
  VmMapping("Skip Region", MappingMode.X, "q", alwaysMapped = true) { SkipOccurrenceHandler(VmCaseSetting) },
  VmMapping("Remove Region", MappingMode.X, "Q", alwaysMapped = true) { RemoveOccurrenceHandler() },
  // In Normal mode, <Esc> is partly handled by the IDE, which we don't want to break
  VmMapping("Exit", MappingMode.X, "<Esc>", alwaysMapped = true) { ExitHandler() },
)

internal fun getLeader(): String {
  val leader = injector.variableService.getGlobalVariableValue("VM_leader") ?: return LEADER
  return (leader as VimString).value.replace("\\", "<Bslash>")
}