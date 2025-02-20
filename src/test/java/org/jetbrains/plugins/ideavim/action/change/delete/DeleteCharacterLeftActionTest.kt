/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.action.change.delete

import com.intellij.openapi.application.ApplicationManager
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

// |X|
class DeleteCharacterLeftActionTest : VimTestCase() {
  @Test
  fun `test delete single character`() {
    val keys = "X"
    val before = "I f${c}ound it in a legendary land"
    val after = "I ${c}ound it in a legendary land"
    configureByText(before)
    typeText(keys)
    assertState(after)
  }

  @Test
  fun `test delete multiple characters`() {
    val keys = "5X"
    val before = "I found$c it in a legendary land"
    val after = "I $c it in a legendary land"
    configureByText(before)
    typeText(keys)
    assertState(after)
  }

  @Test
  fun `test deletes min of count and start of line`() {
    val keys = "25X"
    val before = """
            Lorem Ipsum

            I found$c it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """.trimIndent()
    val after = """
            Lorem Ipsum

            $c it in a legendary land
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
    val keys = "X"
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    // The inlay is inserted at offset 4 (0 based) - the 'u' in "found". It occupies visual column 4, and is associated
    // with the text in visual column 3 ('o'). The 'u' is moved to the right one visual column, and now lives at offset
    // 4, visual column 5.
    // Kotlin type annotations are a real world example of inlays related to preceding text.
    // Hitting 'X' on the character before the inlay should place the cursor after the inlay
    // Before: "I fo«:test»|u|nd it in a legendary land."
    // After: "I f«:test»|u|nd it in a legendary land."
    addInlay(4, true, 5)

    typeText(keys)
    assertState(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. The cursor starts at offset 4, pushed to 5 by the inlay.
    // 'X' moves the cursor one column to the left (along with the text), which puts it at offset 4. But offset 4 can
    // now mean visual column 3 or 4 - the inlay or the text. Make sure the cursor is positioned on the text.
    assertVisualPosition(0, 4)
  }

  @Test
  fun `test delete with inlay relating to following text`() {
    // This should have the same behaviour as related to preceding text
    val keys = "X"
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    // The inlay is inserted at offset 4 (0 based) - the 'u' in "found". It occupies visual column 4, and is associated
    // with the text in visual column 5 ('u' - because the inlay pushes it one visual column to the right).
    // Kotlin parameter hints are a real world example of inlays related to following text.
    // Hitting 'X' on the character before the inlay should place the cursor after the inlay
    // Before: "I fo«test:»|u|nd it in a legendary land."
    // After: "I f«test:»|u|nd it in a legendary land."
    addInlay(4, false, 5)

    typeText(keys)
    assertState(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. The cursor starts at offset 4, pushed to 5 by the inlay.
    // 'X' moves the cursor one column to the left (along with the text), which puts it at offset 4. But offset 4 can
    // now mean visual column 3 or 4 - the inlay or the text. Make sure the cursor is positioned on the text.
    assertVisualPosition(0, 4)
  }

  @Test
  fun `test deleting characters scrolls caret into view`() {
    configureByText("Hello world".repeat(200))
    enterCommand("set nowrap")
    enterCommand("set sidescrolloff=5")

    // Scroll 70 characters to the left. First character on line should now be 71. sidescrolloff puts us at 76
    typeText("70zl")
    assertVisualPosition(0, 75)
    ApplicationManager.getApplication().invokeAndWait {
      assertVisibleLineBounds(0, 70, 149)
    }

    typeText("20X")

    // Deleting 20 characters to the left would move us 20 characters to the left, which will force a scroll.
    // sidescroll=0 scrolls half a page
    assertVisualPosition(0, 55)
    assertVisibleLineBounds(0, 15, 94)
  }
}
