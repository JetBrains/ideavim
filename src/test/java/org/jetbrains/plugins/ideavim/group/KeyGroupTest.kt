/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.group

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.command.MappingMode
import com.maddyhome.idea.vim.key.MappingOwner
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class KeyGroupTest : VimTestCase() {
  private val owner = MappingOwner.Plugin.get("KeyGroupTest")

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  @Test
  fun `test remove key mapping`() {
    val keyGroup = VimPlugin.getKey()
    val keys = injector.parser.parseKeys("<C-S-B>")

    configureByText("I ${c}found it in a legendary land")
    typeText(keys)
    assertState("I ${c}found it in a legendary land")

    keyGroup.putKeyMapping(MappingMode.N, keys, owner, injector.parser.parseKeys("h"), false)
    typeText(keys)
    assertState("I$c found it in a legendary land")

    keyGroup.removeKeyMapping(owner)
    typeText(keys)
    assertState("I$c found it in a legendary land")
  }

  @TestWithoutNeovim(reason = SkipNeovimReason.PLUGIN)
  @Test
  fun `test remove and add key mapping`() {
    val keyGroup = VimPlugin.getKey()
    val keys = injector.parser.parseKeys("<C-S-B>")

    configureByText("I ${c}found it in a legendary land")
    typeText(keys)
    assertState("I ${c}found it in a legendary land")

    keyGroup.putKeyMapping(MappingMode.N, keys, owner, injector.parser.parseKeys("h"), false)
    typeText(keys)
    assertState("I$c found it in a legendary land")

    repeat(10) {
      keyGroup.removeKeyMapping(owner)
      keyGroup.putKeyMapping(MappingMode.N, keys, owner, injector.parser.parseKeys("h"), false)
    }
    typeText(keys)
    assertState("${c}I found it in a legendary land")
  }
}
