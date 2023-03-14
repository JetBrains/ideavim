/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.updown

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 */
class VisualVariousMotionsTest : VimTestCase() {

  @Test
  fun `test with tabs`() {
    val code = """
        class Scratch {
        .public static void main(String[] args) {
        ..try {
        ...if ()
        ..}
        .}
        }

        func myFunc() {
        .return anything
        ${c}}
    """.trimIndent().dotToTab()

    configureByText(code)

    typeText(injector.parser.parseKeys("<C-V>" + "k".repeat(2) + "l".repeat(2)))

    assertState(
      """
        class Scratch {
        .public static void main(String[] args) {
        ..try {
        ...if ()
        ..}
        .}
        }

        ${s}fu${c}n${se}c myFunc() {
        ${s}${c}${se}.return anything
        ${s}${c}}${se}
      """.trimIndent().dotToTab(),
    )

    typeText(injector.parser.parseKeys("k".repeat(7) + "l".repeat(3)))

    // Carets 2-4 have 0 column as logical position, but ${se} - 1 column as visual position
    assertState(
      """
        class Scratch {
        ${s}.pu${c}b${se}lic static void main(String[] args) {
        ${s}${c}.${se}.try {
        ${s}${c}.${se}..if ()
        ${s}${c}.${se}.}
        ${s}.${c}}${se}
        ${s}${c}}${se}

        ${s}func m${c}y${se}Func() {
        ${s}.re${c}t${se}urn anything
        ${s}${c}}${se}
      """.trimIndent().dotToTab(),
    )

    kotlin.test.assertEquals(3, fixture.editor.caretModel.allCarets[1].visualPosition.column)
    kotlin.test.assertEquals(3, fixture.editor.caretModel.allCarets[2].visualPosition.column)
    kotlin.test.assertEquals(3, fixture.editor.caretModel.allCarets[3].visualPosition.column)

    typeText(injector.parser.parseKeys("l".repeat(2)))

    assertState(
      """
        class Scratch {
        ${s}.publ${c}i${se}c static void main(String[] args) {
        ${s}..${c}t${se}ry {
        ${s}.${c}.${se}.if ()
        ${s}..${c}}${se}
        ${s}.${c}}${se}
        ${s}${c}}${se}

        ${s}func myF${c}u${se}nc() {
        ${s}.retu${c}r${se}n anything
        ${s}${c}}${se}
      """.trimIndent().dotToTab(),
    )
    kotlin.test.assertEquals(7, fixture.editor.caretModel.allCarets[2].visualPosition.column)
  }
}
