/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

class CopyCommandTest : VimTestCase() {
  @Test
  @VimBehaviorDiffers(
    """
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """,
    "caret position differs")
  fun `test duplicate line below`() {
    configureByText("""
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non ${c}scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent())
    enterCommand("copy .")
    assertState("""
      Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas efficitur nec odio vel malesuada.
      Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      ${c}Nunc tincidunt viverra ligula non scelerisque. Aliquam erat volutpat. Praesent in fermentum orci. 
      Fusce sit amet mi ut purus volutpat vulputate vitae sed tortor. Aliquam felis neque, varius eu 
      accumsan vitae, facilisis ac nulla.
    """.trimIndent())
  }
}