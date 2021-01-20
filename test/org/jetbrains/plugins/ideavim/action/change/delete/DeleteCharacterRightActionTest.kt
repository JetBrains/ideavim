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

package org.jetbrains.plugins.ideavim.action.change.delete

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

// |x|
class DeleteCharacterRightActionTest : VimTestCase() {
  fun `test delete single character`() {
    val keys = parseKeys("x")
    val before = "I ${c}found it in a legendary land"
    val after = "I ${c}ound it in a legendary land"
    configureByText(before)
    typeText(keys)
    myFixture.checkResult(after)
  }

  fun `test delete multiple characters`() {
    val keys = parseKeys("5x")
    val before = "I ${c}found it in a legendary land"
    val after = "I $c it in a legendary land"
    configureByText(before)
    typeText(keys)
    myFixture.checkResult(after)
  }

  fun `test deletes min of count and end of line`() {
    val keys = parseKeys("20x")
    val before = """
            A Discovery

            I found it in a legendary l${c}and
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            I found it in a legendary ${c}l
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(before)
    typeText(keys)
    myFixture.checkResult(after)
  }

  fun `test delete with inlay relating to preceding text`() {
    val keys = parseKeys("x")
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
    myFixture.checkResult(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. 'x' doesn't move the logical position/offset of the
    // cursor, but offset 3 can now refer to the inlay as well as text - visual column 3 and 4. Make sure the cursor is
    // positioned on the text, not the inlay.
    // Note that the inlay isn't deleted - deleting a character from the end of a variable name shouldn't delete the
    // type annotation
    assertVisualPosition(0, 4)
  }

  fun `test delete with inlay relating to following text`() {
    // This should have the same behaviour as related to preceding text
    val keys = parseKeys("x")
    val before = "I f${c}ound it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    // The inlay is inserted at offset 4 (0 based) - the 'u' in "found". It occupies visual column 4, and is associated
    // with the text in visual column 5 ('u' - because the inlay pushes it one visual column to the right).
    // Kotlin parameter hints are a real world example of inlays related to following text.
    // Hitting 'x' on the character before the inlay should place the cursor after the inlay
    // Before: "I f|o|«test:»und it in a legendary land."
    // After: "I f«test:»|u|nd it in a legendary land."
    addInlay(4, true, 5)

    typeText(keys)
    myFixture.checkResult(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. 'x' doesn't move the logical position/offset of the
    // cursor, but offset 3 can now refer to the inlay as well as text - visual column 3 and 4. Make sure the cursor is
    // positioned on the text, not the inlay.
    // Note that the inlay isn't deleted - deleting a character from the end of a variable name shouldn't delete the
    // type annotation
    assertVisualPosition(0, 4)
  }
}