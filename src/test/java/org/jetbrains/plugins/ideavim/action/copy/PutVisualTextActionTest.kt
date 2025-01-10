/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.action.copy

import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.annotations.TestWithoutPrimaryClipboard
import org.jetbrains.plugins.ideavim.rangeOf
import org.junit.jupiter.api.Test

/**
 * @author Alex Plate
 *
 * c - characterwise
 * l - linewise
 * b - blockwise
 *
 *   Table of test cases
 *
 *            ||    copied with
 *            ||  c  |  l  |  b  |
 *   p      ======================
 *   a w    c ||  1  |  2  |  3  |
 *   s i    --||------------------
 *   t t    l ||  4  |  5  |  6  |
 *   e h    --||------------------
 *   d      b ||  7  |  8  |  9  |
 */
class PutVisualTextActionTest : VimTestCase() {

  // ----- Case 1: Copied | Characterwise | --- pasted | Characterwise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual line without copy`() {
    val before = """
            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            ${c}all rocks and lavender and tufted grass,
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text without copy`() {
    val before = "${c}I found it in a legendary land"
    configureByText(before)
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = "$c it in a legendary land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "legendary",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = "legendar${c}y it in a legendary land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text twice`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "legendary",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("v2e" + "2p"))
    val after = "legendarylegendar${c}y in a legendary land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text full line`() {
    val before = "${c}I found it in a legendary land"
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "legendary",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("v$" + "2p"))
    val after = "legendarylegendar${c}y"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text multicaret`() {
    val before = "${c}I found ${c}it in a ${c}legendary land"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "legendary", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = "legendar${c}y legendar${c}y in a legendar${c}y land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text multicaret clipboard register`() {
    val before = "${c}I found ${c}it in a ${c}legendary land"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "legendary", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = "legendar${c}y legendar${c}y in a legendar${c}y land"
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text another direction`() {
    val before = "I foun${c}d it in a legendary land"
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "legendary",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("vb" + "p"))
    val after = "I legendar${c}y it in a legendary land"
    assertState(after)
  }

  // ----- Case 2: Copied | Linewise | --- pasted | Characterwise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = """
            A Discovery


            ${c}A Discovery
             it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise in middle`() {
    val before = """
            A Discovery

            I found$c it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = """
            A Discovery

            I found
            ${c}A Discovery
             in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
             by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise last line full line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("v$" + "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text linewise multicaret`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("ve" + "\"*p"))
    val after = """
            A Discovery


            ${c}A Discovery
             it in a legendary land

            ${c}A Discovery
             rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
             by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise multicaret clipboard register`() {
    val before = """
            A Discovery

            ${c}I found it in a legendary land
            ${c}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = """
            A Discovery


            ${c}A Discovery
             it in a legendary land

            ${c}A Discovery
             rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
             by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text linewise multicaret on same line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the$c torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
             by the
            ${c}A Discovery
             of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise multicaret on same line clipboard register`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the$c torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
             by the
            ${c}A Discovery
             of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text linewise multicaret on same line twice`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the$c torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("ve" + "2\"+p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
            A Discovery
             by the
            ${c}A Discovery
            A Discovery
             of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text linewise multicaret on same line twice clipboard register`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}hard by the$c torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("ve" + "2\"+p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand

            ${c}A Discovery
            A Discovery
             by the
            ${c}A Discovery
            A Discovery
             of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  // ----- Case 3: Copied | Blockwise | --- pasted | Characterwise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The ${c}features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|found| it combines mark it as new
            to s|l roc|cience: shape and shade -- the special tinge,
            akin|ere i| to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise on last line`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy $c|found|, the checquered fringe.
                      |l roc|
                      |ere i|
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise on last line twice`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("ve" + "2p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy $c|found||found|, the checquered fringe.
                      |l roc||l roc|
                      |ere i||ere i|
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text blockwise multicaret`() {
    val before = """
            A Discovery

            I |found| it in a ${c}legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "|found|\n|l roc|\n|ere i|", SelectionType.BLOCK_WISE)
//    registerService.storeText(editor.vim context,, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = """
            A Discovery

            I |found| it in a $c|found| land
            al|l roc|ks and la|l roc|vender and tufted grass,
            wh|ere i|t was set|ere i|tled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy $c|found|, the checquered fringe.
                      |l roc|
                      |ere i|
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise multicaret clipboard register`() {
    val before = """
            A Discovery

            I |found| it in a ${c}legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "|found|\n|l roc|\n|ere i|", SelectionType.BLOCK_WISE)
//    registerService.storeText(editor.vim context,, editor.rangeOf("|found|", 2), SelectionType.BLOCK_WISE, false)
    typeText(injector.parser.parseKeys("ve" + "\"+p"))
    val after = """
            A Discovery

            I |found| it in a $c|found| land
            al|l roc|ks and la|l roc|vender and tufted grass,
            wh|ere i|t was set|ere i|tled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy $c|found|, the checquered fringe.
                      |l roc|
                      |ere i|
    """.trimIndent()
    assertState(after)
  }

  // ----- Case 4: Copied | Characterwise | --- pasted | Linewise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to line`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to line twice`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "2p"))
    val after = """
            A Discovery

            ${c}Discovery
            Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by ${c}the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text character to line multicaret`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "Discovery", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("V" + "\"*p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to line multicaret clipboard register`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "Discovery", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text character to line multicaret on same line`() {
    val before = """
            A Discovery

            I found ${c}it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "Discovery", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to line multicaret on same line clipboard register`() {
    val before = """
            A Discovery

            I found ${c}it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "Discovery", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            ${c}Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}Discovery

    """.trimIndent()
    assertState(after)
  }

  // ----- Case 5: Copied | Linewise | --- pasted | Linewise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to line`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to line twice`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "2p"))
    val after = """
            A Discovery

            ${c}A Discovery
            A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to last line`() {
    val before = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            A Discovery

            I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text line to line multicaret`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("V" + "\"*p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to line multicaret clipboard register`() {
    val before = """
            A Discovery

            I found ${c}it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text line to line multicaret on same line`() {
    val before = """
            A Discovery

            I found ${c}it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to line multicaret on same line clipboard register`() {
    val before = """
            A Discovery

            I found ${c}it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the ${c}torrent of a mountain pass.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "A Discovery\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            ${c}A Discovery
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  // ----- Case 6: Copied | Blockwise | --- pasted | Linewise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to line`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The ${c}features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            $c|found|
            |l roc|
            |ere i|
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise on last line to line`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|



    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
            |found|
            |l roc|
            |ere i|
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise on last line twice to line`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("V" + "2p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            |found||found|
            |l roc||l roc|
            |ere i||ere i|



    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            $c|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @TestWithoutPrimaryClipboard
  fun `test put visual text blockwise multicaret to line`() {
    val before = """
            A Discovery

            I |found| it in a ${c}legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "|found|\n|l roc|\n|ere i|", SelectionType.BLOCK_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            $c|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|



    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            $c|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise multicaret to line clipboard register`() {
    val before = """
            A Discovery

            I |found| it in a ${c}legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy ${c}underside, the checquered fringe.
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '+', "|found|\n|l roc|\n|ere i|", SelectionType.BLOCK_WISE)
    typeText(injector.parser.parseKeys("V" + "\"+p"))
    val after = """
            A Discovery

            $c|found|
            |l roc|
            |ere i|
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            $c|found|
            |l roc|
            |ere i|



    """.trimIndent()
    assertState(after)
  }

  // ----- Case 7: Copied | Characterwise | --- pasted | Blockwise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual block without copy`() {
    val before = """
            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("<C-V>2ej" + "p"))
    val after = """
            I  it in a legendary land
            alks and lavender and tufted grass,
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to block`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e2j" + "p"))
    val after = """
            A Discovery

            I Discover${c}y it in a legendary land
            alDiscoveryks and lavender and tufted grass,
            whDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to block motion up`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh$c|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>3e2k" + "p"))
    val after = """
            A Discovery

            I Discover${c}y it in a legendary land
            alDiscoveryks and lavender and tufted grass,
            whDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to block twice`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e2j" + "2p"))
    val after = """
            A Discovery

            I DiscoveryDiscover${c}y it in a legendary land
            alDiscoveryDiscoveryks and lavender and tufted grass,
            whDiscoveryDiscoveryt was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text character to block with dollar motion`() {
    val before = """
            A Discovery

            I $c|found it in a legendary land
            al|l rocks and lavender and tufted grass,[ additional characters]
            wh|ere it was settled on some sodden sand
            ha|rd by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "Discovery",
      SelectionType.CHARACTER_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>3j$" + "p"))
    val after = """
            A Discovery

            I Discover${c}y
            alDiscovery
            whDiscovery
            haDiscovery
    """.trimIndent()
    assertState(after)
  }

  // ----- Case 8: Copied | Linewise | --- pasted | Blockwise | ---| small p |--------------------

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ${c}A Discovery
            hard by the torrent of a mountain pass.
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to block`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e2j" + "p"))
    val after = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ${c}A Discovery

            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to block before caret`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e2j" + "P"))
    val after = """
            A Discovery

            ${c}A Discovery
            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ${c}A Discovery
            A Discovery
            hard by the torrent of a mountain pass.
            """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to block twice`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e2j" + "2p"))
    val after = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ${c}A Discovery
            A Discovery

            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ha the torrent of a mountain pass.
            ${c}A Discovery
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to block till end`() {
    val before = """
            A Discovery

            I $c|found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            ha|rd by| the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e3j" + "p"))
    val after = """
            A Discovery

            I  it in a legendary land
            alks and lavender and tufted grass,
            wht was settled on some sodden sand
            ha the torrent of a mountain pass.
            ${c}A Discovery

    """.trimIndent()
    assertState(after)
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            A Discovery

            I
            a
            w
            ${c}A Discovery
            hard by the torrent of a mountain pass.
    """,
  )
  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text line to block with dollar motion`() {
    val before = """
            A Discovery

            I${c}| found it in a legendary land
            a|ll rocks and lavender and tufted grass,[ additional characters]
            w|here it was settled on some sodden sand
            hard by the torrent of a mountain pass.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      before rangeOf "A Discovery\n",
      SelectionType.LINE_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2j$" + "p"))
    val after = """
            A Discovery

            I
            a
            w
            ${c}A Discovery

            hard by the torrent of a mountain pass.
    """.trimIndent()
    assertState(after)
  }

  // ----- Case 9: Copied | Blockwise | --- pasted | Blockwise | ---| small p |--------------------

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to block`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|features| it combines mark it as new
            to s|cience: |shape and shade -- the special tinge,
            akin| to moon|light, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e2j" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|found| it combines mark it as new
            to s|l roc|shape and shade -- the special tinge,
            akin|ere i|light, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to longer block`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|features| it combines mark it as new
            to s|cience: |shape and shade -- the special tinge,
            akin| to moon|light, tempering its blue,
            the |dingy un|derside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2e3j" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|found| it combines mark it as new
            to s|l roc|shape and shade -- the special tinge,
            akin|ere i|light, tempering its blue,
            the derside, the checquered fringe.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to shorter block`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|features| it combines mark it as new
            to s|cience: |shape and shade -- the special tinge,
            akin to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2ej" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|found| it combines mark it as new
            to s|l roc|shape and shade -- the special tinge,
            akin|ere i| to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to shorter block on line end`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to $c|moonlight|, tempering its blue,
            the ding|y undersi|de, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)
    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>elj" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The features it combines mark it as new
            to science: shape and shade -- the special tinge,
            akin to $c|found|, tempering its blue,
            the ding|l roc|de, the checquered fringe.
                    |ere i|
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to block with dollar motion`() {
    val before = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|features it combines mark it as new
            to s|cience: shape and shade -- the special tinge,[ additional characters]
            akin| to moonlight, tempering its blue,
            the dingy underside, the checquered fringe.
    """.trimIndent()
    val editor = configureByText(before)

    val vimEditor = editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup

    registerService.storeText(
      vimEditor,
      context,
      vimEditor.primaryCaret(),
      editor.rangeOf("|found|", 2),
      SelectionType.BLOCK_WISE,
      false
    )
    typeText(injector.parser.parseKeys("<C-V>2j$" + "p"))
    val after = """
            A Discovery

            I |found| it in a legendary land
            al|l roc|ks and lavender and tufted grass,
            wh|ere i|t was settled on some sodden sand
            hard by the torrent of a mountain pass.

            The $c|found|
            to s|l roc|
            akin|ere i|
            the dingy underside, the checquered fringe.
    """.trimIndent()
    assertState(after)
  }

  @TestWithoutNeovim(SkipNeovimReason.DIFFERENT)
  @Test
  fun `test put visual text blockwise to block with dollar motion1`() {
    val before = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi gravida commodo orci, egestas placerat purus rhoncus non. Donec efficitur placerat lorem, non ullamcorper nisl. Aliquam vestibulum, purus a pretium sodales, lorem libero placerat tortor, ut gravida est arcu nec purus. Suspendisse luctus euismod mi, at consectetur sapien facilisis sed. Duis eu magna id nisi lacinia vehicula in quis mauris. Donec tincidunt, erat in euismod placerat, tortor eros efficitur ligula, non finibus metus enim in ex. Nam commodo libero quis vestibulum congue. Vivamus sit amet tincidunt orci, in luctus tortor. Ut aliquam porttitor pharetra. Sed vel mi lacinia, auctor eros vel, condimentum eros. Fusce suscipit auctor venenatis. Aliquam elit risus, eleifend quis mollis eu, venenatis quis ex. Nunc varius consectetur eros sit amet efficitur. Donec a elit rutrum, tristique est in, maximus sem. Donec eleifend magna vitae suscipit viverra. Phasellus luctus aliquam tellus viverra consequat.

        Aliquam tristique eros vel magna dictum tincidunt. Duis sagittis mi et bibendum congue. Donec sollicitudin, ipsum quis pellentesque efficitur, metus quam congue nulla, vel rutrum neque lectus vitae sem. In accumsan scelerisque risus, ac sollicitudin purus ornare in. Proin leo erat, tempus vitae purus nec, lobortis bibendum tortor. Aenean mauris sem, interdum id facilisis et, ullamcorper ut libero. Quisque magna ligula, euismod sit amet ipsum non, maximus ultrices nulla. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Cras facilisis arcu vitae orci scelerisque, vel dignissim massa dapibus. Fusce sed urna ut orci pellentesque consectetur. Maecenas rutrum erat ac libero elementum dictum. Donec pulvinar, sem feugiat suscipit mattis, turpis tellus consectetur dui, vitae vehicula dolor purus eu lectus. Nullam lorem ligula, aliquet id eros sed, rhoncus consequat neque. Cras eget erat non nunc convallis accumsan id in ipsum.

        In id lacus diam. Curabitur orci libero, sollicitudin sed magna efficitur, finibus elementum mi. Cras aliquam enim eu scelerisque consectetur. Ut lacinia, velit sed dictum sollicitudin, mauris metus fringilla quam, vitae pellentesque tortor leo ut lectus. Fusce facilisis, eros ac egestas porttitor, enim arcu molestie purus, ut porta erat neque ac est. Ut facilisis, ante vel feugiat ultricies, metus nulla vestibulum dui, eget luctus lorem urna sed ex. Mauris quis lectus efficitur, sollicitudin urna vel, suscipit mi. Aliquam fringilla fermentum nunc. Phasellus suscipit nunc a dui gravida, sed euismod elit mattis. Donec pharetra, sem at finibus fermentum, dui lacus ornare arcu, eget maximus massa purus at ipsum.

        Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nunc quis ligula sed quam suscipit varius id et metus. Ut hendrerit diam eu turpis semper luctus. Aliquam efficitur tortor ut eros consectetur tristique. Vestibulum odio nunc, finibus eu ex auctor, pharetra congue urna. Proin sit amet malesuada nisl. Proin sagittis metus diam, vitae sollicitudin eros rutrum id. Nam imperdiet lacus et mi iaculis, vitae suscipit felis consequat.

        Fusce in lectus eros. Vivamus imperdiet sodales enim$c id vulputate. Ut tincidunt hendrerit cursus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras maximus et justo et congue. Nam iaculis elementum ultrices. Quisque nec semper eros. Nulla nisl nunc, finibus ac ligula vel, ullamcorper egestas risus. Nunc dictum cursus leo, id pulvinar augue ullamcorper ac. Vivamus condimentum nunc non justo convallis, in condimentum ante malesuada. Vivamus gravida et metus vitae porta. Integer blandit magna metus, sodales commodo nibh rutrum ac. Ut tincidunt et justo a luctus. Nunc lacus lorem, finibus id vehicula eu, gravida ut augue.
    """.trimIndent()
    configureByText(before)
    enterCommand("set nowrap")
    typeText(injector.parser.parseKeys("Ypj<C-V>P"))
    val after = """
        Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi gravida commodo orci, egestas placerat purus rhoncus non. Donec efficitur placerat lorem, non ullamcorper nisl. Aliquam vestibulum, purus a pretium sodales, lorem libero placerat tortor, ut gravida est arcu nec purus. Suspendisse luctus euismod mi, at consectetur sapien facilisis sed. Duis eu magna id nisi lacinia vehicula in quis mauris. Donec tincidunt, erat in euismod placerat, tortor eros efficitur ligula, non finibus metus enim in ex. Nam commodo libero quis vestibulum congue. Vivamus sit amet tincidunt orci, in luctus tortor. Ut aliquam porttitor pharetra. Sed vel mi lacinia, auctor eros vel, condimentum eros. Fusce suscipit auctor venenatis. Aliquam elit risus, eleifend quis mollis eu, venenatis quis ex. Nunc varius consectetur eros sit amet efficitur. Donec a elit rutrum, tristique est in, maximus sem. Donec eleifend magna vitae suscipit viverra. Phasellus luctus aliquam tellus viverra consequat.

        Aliquam tristique eros vel magna dictum tincidunt. Duis sagittis mi et bibendum congue. Donec sollicitudin, ipsum quis pellentesque efficitur, metus quam congue nulla, vel rutrum neque lectus vitae sem. In accumsan scelerisque risus, ac sollicitudin purus ornare in. Proin leo erat, tempus vitae purus nec, lobortis bibendum tortor. Aenean mauris sem, interdum id facilisis et, ullamcorper ut libero. Quisque magna ligula, euismod sit amet ipsum non, maximus ultrices nulla. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Cras facilisis arcu vitae orci scelerisque, vel dignissim massa dapibus. Fusce sed urna ut orci pellentesque consectetur. Maecenas rutrum erat ac libero elementum dictum. Donec pulvinar, sem feugiat suscipit mattis, turpis tellus consectetur dui, vitae vehicula dolor purus eu lectus. Nullam lorem ligula, aliquet id eros sed, rhoncus consequat neque. Cras eget erat non nunc convallis accumsan id in ipsum.

        In id lacus diam. Curabitur orci libero, sollicitudin sed magna efficitur, finibus elementum mi. Cras aliquam enim eu scelerisque consectetur. Ut lacinia, velit sed dictum sollicitudin, mauris metus fringilla quam, vitae pellentesque tortor leo ut lectus. Fusce facilisis, eros ac egestas porttitor, enim arcu molestie purus, ut porta erat neque ac est. Ut facilisis, ante vel feugiat ultricies, metus nulla vestibulum dui, eget luctus lorem urna sed ex. Mauris quis lectus efficitur, sollicitudin urna vel, suscipit mi. Aliquam fringilla fermentum nunc. Phasellus suscipit nunc a dui gravida, sed euismod elit mattis. Donec pharetra, sem at finibus fermentum, dui lacus ornare arcu, eget maximus massa purus at ipsum.

        Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Nunc quis ligula sed quam suscipit varius id et metus. Ut hendrerit diam eu turpis semper luctus. Aliquam efficitur tortor ut eros consectetur tristique. Vestibulum odio nunc, finibus eu ex auctor, pharetra congue urna. Proin sit amet malesuada nisl. Proin sagittis metus diam, vitae sollicitudin eros rutrum id. Nam imperdiet lacus et mi iaculis, vitae suscipit felis consequat.

        Fusce in lectus eros. Vivamus imperdiet sodales enim id vulputate. Ut tincidunt hendrerit cursus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras maximus et justo et congue. Nam iaculis elementum ultrices. Quisque nec semper eros. Nulla nisl nunc, finibus ac ligula vel, ullamcorper egestas risus. Nunc dictum cursus leo, id pulvinar augue ullamcorper ac. Vivamus condimentum nunc non justo convallis, in condimentum ante malesuada. Vivamus gravida et metus vitae porta. Integer blandit magna metus, sodales commodo nibh rutrum ac. Ut tincidunt et justo a luctus. Nunc lacus lorem, finibus id vehicula eu, gravida ut augue.
        Fusce in lectus eros. Vivamus imperdiet sodales enim id vulputate. Ut tincidunt hendrerit cursus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras maximus et justo et congue. Nam iaculis elementum ultrices. Quisque nec semper eros. Nulla nisl nunc, finibus ac ligula vel, ullamcorper egestas risus. Nunc dictum cursus leo, id pulvinar augue ullamcorper ac. Vivamus condimentum nunc non justo convallis, in condimentum ante malesuada. Vivamus gravida et metus vitae porta. Integer blandit magna metus, sodales commodo nibh rutrum ac. Ut tincidunt et justo a luctus. Nunc lacus lorem, finibus id vehicula eu, gravida ut augue.
        Fusce in lectus eros. Vivamus imperdiet sodales enim id vulputate. Ut tincidunt hendrerit cursus. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; Cras maximus et justo et congue. Nam iaculis elementum ultrices. Quisque nec semper eros. Nulla nisl nunc, finibus ac ligula vel, ullamcorper egestas risus. Nunc dictum cursus leo, id pulvinar augue ullamcorper ac. Vivamus condimentum nunc non justo convallis, in condimentum ante malesuada. Vivamus gravida et metus vitae porta. Integer blandit magna metus, sodales commodo nibh rutrum ac. Ut tincidunt et justo a luctus. Nunc lacus lorem, finibus id vehicula eu, gravida ut augue.
    """.trimIndent()
    assertState(after)
  }
}
