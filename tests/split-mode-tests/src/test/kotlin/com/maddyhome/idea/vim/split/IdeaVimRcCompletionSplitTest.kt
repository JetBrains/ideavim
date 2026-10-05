/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.maddyhome.idea.vim.split

import org.junit.jupiter.api.Test

/**
 * Action ID completion in the ideavimrc file.
 *
 * In split mode the completion runs on the backend, but `<Action>(...)` is executed on the frontend. `VimPluginToggle`
 * is registered by IdeaVim's frontend module only, so the backend can offer it only thanks to the action IDs sent by
 * the frontend.
 */
class IdeaVimRcCompletionSplitTest : IdeaVimStarterTestBase() {

  @Test
  fun `basic completion offers frontend-only action in action notation`() {
    openFile(createFile("notation/.ideavimrc", "nmap x <Action>(VimPluginTogg\n"))
    typeVim("A")
    invokeBasicCompletion()
    esc()

    assertEditorContains("nmap x <Action>(VimPluginToggle", "Frontend-only action ID should be completed")
  }

  @Test
  fun `basic completion offers frontend-only action in action command`() {
    openFile(createFile("command/.ideavimrc", "nnoremap x :action VimPluginTogg\n"))
    typeVim("A")
    invokeBasicCompletion()
    esc()

    assertEditorContains("nnoremap x :action VimPluginToggle", "Frontend-only action ID should be completed")
  }

  @Test
  fun `lookup pops up automatically after action notation`() {
    openFile(createFile("autopopup/.ideavimrc", "\" mappings\n"))
    typeVim("Gonmap x <Action>(")
    pause(1000)
    typeVim("VimPluginTogg")
    pause(1000)
    // Without the lookup, Enter would only break the line
    enter()
    esc()

    assertEditorContains("nmap x <Action>(VimPluginToggle", "Lookup should open after <Action>( and complete the ID")
  }
}
