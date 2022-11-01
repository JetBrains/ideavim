/*
 * Copyright 2022 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.key

import com.maddyhome.idea.vim.key.MappingOwner
import junit.framework.TestCase
import org.jetbrains.plugins.ideavim.VimTestCase

class MappingOwnerTest : VimTestCase() {
  fun `test get two plugin owners`() {
    val pluginName = "MyPlugin"
    val firstOwner = MappingOwner.Plugin.get(pluginName)
    val secondOwner = MappingOwner.Plugin.get(pluginName)
    TestCase.assertSame(firstOwner, secondOwner)
  }
}
