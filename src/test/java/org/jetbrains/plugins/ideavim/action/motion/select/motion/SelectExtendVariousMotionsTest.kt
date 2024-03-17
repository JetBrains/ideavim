/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.motion.select.motion

import com.maddyhome.idea.vim.api.injector
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 *
 * All dots in these tests are replaced with tabs
 */
class SelectExtendVariousMotionsTest : VimTestCase() {

  @TestWithoutNeovim(SkipNeovimReason.TABS)
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

    typeText(injector.parser.parseKeys("g<C-H>" + "<S-UP>".repeat(2) + "<S-Right>".repeat(2)))

    assertState(
      """
        class Scratch {
        .public static void main(String[] args) {
        ..try {
        ...if ()
        ..}
        .}
        }

        ${s}fu${c}${se}nc myFunc() {
        ${s}${c}${se}.return anything
        ${s}}${c}${se}
      """.trimIndent().dotToTab(),
    )

    typeText(injector.parser.parseKeys("<S-UP>".repeat(7) + "<S-Right>".repeat(3)))

    assertState(
      """
        class Scratch {
        ${s}.pu${c}${se}blic static void main(String[] args) {
        ${s}.${c}${se}.try {
        ${s}.${c}${se}..if ()
        ${s}.${c}${se}.}
        ${s}.}${c}${se}
        ${s}}${c}${se}

        ${s}func m${c}${se}yFunc() {
        ${s}.re${c}${se}turn anything
        ${s}}${c}${se}
      """.trimIndent().dotToTab(),
    )

    typeText(injector.parser.parseKeys("<S-Right>".repeat(2)))

    assertState(
      """
        class Scratch {
        ${s}.publ${c}${se}ic static void main(String[] args) {
        ${s}..${c}${se}try {
        ${s}..${c}${se}.if ()
        ${s}..${c}${se}}
        ${s}.}${c}${se}
        ${s}}${c}${se}

        ${s}func myF${c}${se}unc() {
        ${s}.retu${c}${se}rn anything
        ${s}}${c}${se}
      """.trimIndent().dotToTab(),
    )
  }
}
