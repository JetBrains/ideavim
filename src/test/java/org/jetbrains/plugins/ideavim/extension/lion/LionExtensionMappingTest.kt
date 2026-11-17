/*
 * Copyright 2003-2026 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.extension.lion

import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * The mapping variables are read when the extension is enabled, so each test enables it itself.
 */
class LionExtensionMappingTest : VimTestCase() {
  @Test
  fun `test custom right align mapping`() {
    configureByText(
      """
        |${c}a = 1
        |bb = 2
      """.trimMargin(),
    )
    executeVimscript("let g:lion_map_right = 'ga'")
    enableExtensions("lion")
    typeText("gaip=")
    assertState(
      """
        |${c}a  = 1
        |bb = 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test custom left align mapping`() {
    configureByText(
      """
        |${c}a: 1
        |bbb: 2
      """.trimMargin(),
    )
    executeVimscript("let g:lion_map_left = 'gA'")
    enableExtensions("lion")
    typeText("gAip:")
    assertState(
      """
        |${c}a:   1
        |bbb: 2
      """.trimMargin(),
    )
  }

  @Test
  fun `test plug mappings work without default mappings`() {
    configureByText(
      """
        |${c}a = 1
        |bb = 2
        |
        |c = 3
        |ddd = 4
      """.trimMargin(),
    )
    executeVimscript("let g:lion_create_maps = 0")
    enableExtensions("lion")
    executeVimscript("nmap ga <Plug>LionRight")
    executeVimscript("xmap ga <Plug>VLionRight")
    typeText("gaip=", "3j", "Vjga=")
    assertState(
      """
        |a  = 1
        |bb = 2
        |
        |c   = 3
        |${c}ddd = 4
      """.trimMargin(),
    )
  }
}
