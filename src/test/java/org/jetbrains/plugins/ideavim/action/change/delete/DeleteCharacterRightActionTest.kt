/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

// |x|
class DeleteCharacterRightActionTest : VimTestCase() {
  @Test
  fun `test delete single character`() {
    val keys = injector.parser.parseKeys("x")
    val before = "I ${c}found it in a legendary land"
    val after = "I ${c}ound it in a legendary land"
    configureByText(before)
    typeText(keys)
    assertState(after)
  }

  @Test
  fun `test delete multiple characters`() {
    val keys = injector.parser.parseKeys("5x")
    val before = "I ${c}found it in a legendary land"
    val after = "I $c it in a legendary land"
    configureByText(before)
    typeText(keys)
    assertState(after)
  }

  @Test
  fun `test deletes min of count and end of line`() {
    val keys = injector.parser.parseKeys("20x")
    val before = """
            Lorem Ipsum

            I found it in a legendary l${c}and
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            I found it in a legendary ${c}l
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    configureByText(before)
    typeText(keys)
    assertState(after)
  }

  @Test
  fun `test delete with inlay relating to preceding text`() {
    val keys = injector.parser.parseKeys("x")
    val before = "I f${c}ound it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    // The inlay is inserted at offset 4 (0 based) - the 'u' in "found". It occupies visual column 4, and is associated
    // with the text in visual column 3 ('o'). The 'u' is moved to the right one visual column, and now lives at offset
    // 4, visual column 5.
    // Kotlin type annotations are a real world example of inlays related to preceding text.
    // Hitting 'x' on the character before the inlay should place the cursor after the inlay
    // Before: "I f|o|«:test»und it in a legendary land."
    // After: "I f«:test»|u|nd it in a legendary land."
    addInlay(4, true, 5)

    typeText(keys)
    assertState(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. 'x' doesn't move the logical position/offset of the
    // cursor, but offset 3 can now refer to the inlay as well as text - visual column 3 and 4. Make sure the cursor is
    // positioned on the text, not the inlay.
    // Note that the inlay isn't deleted - deleting a character from the end of a variable name shouldn't delete the
    // type annotation
    assertVisualPosition(0, 4)
  }

  @Test
  fun `test delete with inlay relating to following text`() {
    // This should have the same behaviour as related to preceding text
    val keys = injector.parser.parseKeys("x")
    val before = "I f${c}ound it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    // The inlay is inserted at offset 4 (0 based) - the 'u' in "found". It occupies visual column 4, and is associated
    // with the text in visual column 5 ('u' - because the inlay pushes it one visual column to the right).
    // Kotlin parameter hints are a real world example of inlays related to following text.
    // Hitting 'x' on the character before the inlay should place the cursor after the inlay
    // Before: "I f|o|«test:»und it in a legendary land."
    // After: "I f«test:»|u|nd it in a legendary land."
    addInlay(4, false, 5)

    typeText(keys)
    assertState(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. 'x' doesn't move the logical position/offset of the
    // cursor, but offset 3 can now refer to the inlay as well as text - visual column 3 and 4. Make sure the cursor is
    // positioned on the text, not the inlay.
    // Note that the inlay isn't deleted - deleting a character from the end of a variable name shouldn't delete the
    // type annotation
    assertVisualPosition(0, 4)
  }

  @Test
  fun `undo after deleting character`() {
    configureByText("foo ${c}foo")
    typeText("xx")
    assertState("foo ${c}o")
    typeText("u")
    assertState("foo ${c}oo")
    typeText("u")
    assertState("foo ${c}foo")
  }

  @Test
  fun `undo after deleting character with oldundo`() {
    configureByText("foo ${c}foo")
    try {
      enterCommand("set oldundo")
      typeText("xx")
      assertState("foo ${c}o")
      typeText("u")
      assertState("foo ${c}oo")
      typeText("u")
      assertState("foo ${c}foo")
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
