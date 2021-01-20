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

// |X|
class DeleteCharacterLeftActionTest : VimTestCase() {
  fun `test delete single character`() {
    val keys = parseKeys("X")
    val before = "I f${c}ound it in a legendary land"
    val after = "I ${c}ound it in a legendary land"
    configureByText(before)
    typeText(keys)
    myFixture.checkResult(after)
  }

  fun `test delete multiple characters`() {
    val keys = parseKeys("5X")
    val before = "I found$c it in a legendary land"
    val after = "I $c it in a legendary land"
    configureByText(before)
    typeText(keys)
    myFixture.checkResult(after)
  }

  fun `test deletes min of count and start of line`() {
    val keys = parseKeys("25X")
    val before = """
            A Discovery

            I found$c it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    val after = """
            A Discovery

            $c it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
        """.trimIndent()
    configureByText(before)
    typeText(keys)
    myFixture.checkResult(after)
  }

  fun `test delete with inlay relating to preceding text`() {
    val keys = parseKeys("X")
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
    myFixture.checkResult(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. The cursor starts at offset 4, pushed to 5 by the inlay.
    // 'X' moves the cursor one column to the left (along with the text), which puts it at offset 4. But offset 4 can
    // now mean visual column 3 or 4 - the inlay or the text. Make sure the cursor is positioned on the text.
    assertVisualPosition(0, 4)
  }

  fun `test delete with inlay relating to following text`() {
    // This should have the same behaviour as related to preceding text
    val keys = parseKeys("X")
    val before = "I fo${c}und it in a legendary land"
    val after = "I f${c}und it in a legendary land"
    configureByText(before)

    // The inlay is inserted at offset 4 (0 based) - the 'u' in "found". It occupies visual column 4, and is associated
    // with the text in visual column 5 ('u' - because the inlay pushes it one visual column to the right).
    // Kotlin parameter hints are a real world example of inlays related to following text.
    // Hitting 'X' on the character before the inlay should place the cursor after the inlay
    // Before: "I fo«test:»|u|nd it in a legendary land."
    // After: "I f«test:»|u|nd it in a legendary land."
    addInlay(4, true, 5)

    typeText(keys)
    myFixture.checkResult(after)

    // It doesn't matter if the inlay is related to preceding or following text. Deleting visual column 3 moves the
    // inlay one visual column to the left, from column 4 to 3. The cursor starts at offset 4, pushed to 5 by the inlay.
    // 'X' moves the cursor one column to the left (along with the text), which puts it at offset 4. But offset 4 can
    // now mean visual column 3 or 4 - the inlay or the text. Make sure the cursor is positioned on the text.
    assertVisualPosition(0, 4)
  }
}