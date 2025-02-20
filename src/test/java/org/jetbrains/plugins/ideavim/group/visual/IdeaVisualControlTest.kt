/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

@file:Suppress("RemoveCurlyBracesFromTemplate")

package org.jetbrains.plugins.ideavim.group.visual

import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.codeInsight.template.impl.TemplateManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.group.visual.IdeaSelectionControl
import com.maddyhome.idea.vim.group.visual.VimVisualTimer
import com.maddyhome.idea.vim.listener.VimListenerManager
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.options.OptionConstants
import com.maddyhome.idea.vim.state.mode.Mode
import com.maddyhome.idea.vim.state.mode.SelectionType
import com.maddyhome.idea.vim.state.mode.selectionType
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.jetbrains.plugins.ideavim.waitAndAssert
import org.jetbrains.plugins.ideavim.waitAndAssertMode
import kotlin.test.assertNull

@TraceOptions(TestOptionConstants.selectmode)
class IdeaVisualControlTest : VimTestCase() {
  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection no selection`() {
    configureByText(
      """
            Lorem Ipsum

            I $s$c${se}found it in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    assertMode(Mode.NORMAL())
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection cursor in the middle`() {
    configureByText(
      """
            Lorem Ipsum

            I ${s}found$c it$se in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            I ${s}found ${c}i${se}t in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @VimBehaviorDiffers(
    originalVimAfter = """
            Lorem Ipsum

            I ${s}found i${c}t$se in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
    """,
  )
  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection cursor on end`() {
    configureByText(
      """
            Lorem Ipsum

            I ${s}found it$c$se in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            I ${s}found it ${c}i${se}n a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection cursor on start`() {
    configureByText(
      """
            Lorem Ipsum

            I $s${c}found it$se in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            I f${s}${c}ound it$se in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection lineend`() {
    configureByText(
      """
            Lorem Ipsum

            I ${s}found ${c}it in a legendary land$se
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            I ${s}found i${c}t${se} in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection next line`() {
    configureByText(
      """
            Lorem Ipsum

            I ${s}found ${c}it in a legendary land
            ${se}consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            I ${s}found i${c}t${se} in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection start on line start`() {
    configureByText(
      """
            Lorem Ipsum

            ${s}I found ${c}it ${se}in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            ${s}I found i${c}t${se} in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection start on line end`() {
    configureByText(
      """
            Lorem Ipsum
            $s
            I found ${c}it ${se}in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum
            $s
            I found i${c}t${se} in a legendary land
            consectetur adipiscing elit
            Sed in orci mauris.
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection multicaret`() {
    configureByText(
      """
            Lorem Ipsum
            $s
            I found ${c}it ${se}in a legendary land
            all rocks $s$c${se}and lavender and tufted grass,
            where it was $s${c}settled$se on some sodden sand
            hard by the torrent of a mountain ${s}pass.$c$se
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum
            $s
            I found i${c}t${se} in a legendary land
            all rocks ${s}a${c}n${se}d lavender and tufted grass,
            where it was s$s${c}ettled$se on some sodden sand
            hard by the torrent of a mountain ${s}pass.$c$se
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable line selection`() {
    configureByText(
      """
            A Discovery

            ${s}I found ${c}it in a legendary land$se
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            A Discovery

            ${s}I found i${c}t in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
            A Discovery

            ${s}I found it in a legendary land
            all rocks$c and lavender and tufted grass,
            ${se}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable line selection next line`() {
    configureByText(
      """
            A Discovery

            ${s}I found ${c}it in a legendary land
            ${se}all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
            A Discovery

            ${s}I found it in a legendary land
            all rock${c}s and lavender and tufted grass,
            ${se}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable line selection cursor on last line`() {
    configureByText(
      """
            A Discovery

            ${s}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled ${c}on some sodden sand$se
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
            A Discovery

            ${s}I found it in a legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand
            hard by the torrent o${c}f a mountain pass.$se
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable line selection cursor on first line`() {
    configureByText(
      """
            A Discovery

            ${s}I found it in a ${c}legendary land
            all rocks and lavender and tufted grass,
            where it was settled on some sodden sand$se
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
            A Discovery

            I found it in a legendary land
            ${s}all rocks and la${c}vender and tufted grass,
            where it was settled on some sodden sand
            ${se}hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable line selection multicaret`() {
    configureByText(
      """
            A Discovery

            ${s}I found it in a ${c}legendary land$se
            all rocks and lavender and tufted grass,
            ${s}where it was settled ${c}on some sodden sand
            hard by the torrent of a mountain pass.$se
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
            A Discovery

            ${s}I found it in a legendary land
            all rocks and la${c}vender and tufted grass,
            ${se}where it was settled on some sodden sand
            ${s}hard by the torrent o${c}f a mountain pass.$se
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable line selection motion up`() {
    configureByText(
      """
            A Discovery

            I found it in a legendary land
            ${s}all rocks and lavender ${c}and tufted grass,$se
            where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.LINE_WISE))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("k"))
    assertState(
      """
            A Discovery

            ${s}I found it in a legenda${c}ry land
            all rocks and lavender and tufted grass,
            ${se}where it was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection looks like block`() {
    configureByText(
      """
            Lorem Ipsum

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            ha${s}rd by $c${se}the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection`() {
    configureByText(
      """
            Lorem Ipsum

            I ${s}found$c$se it in a legendary land
            al${s}l roc$c${se}ks and lavender and tufted grass,
            wh${s}ere i$c${se}t was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            Lorem Ipsum

            I ${s}found ${c}i${se}t in a legendary land
            al${s}l rock${c}s${se} and lavender and tufted grass,
            wh${s}ere it${c} ${se}was settled on some sodden sand
            Cras id tellus in ex imperdiet egestas.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection with longer line`() {
    configureByText(
      """
            A Discovery

            I ${s}found it in a legendary land$c$se
            al${s}l rocks and lavender and tufted grass,$c$se
            wh${s}ere it was settled on some sodden sand$c$se
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("j"))
    assertState(
      """
            A Discovery

            I ${s}found it in a legendary lan${c}d$se
            al${s}l rocks and lavender and tufted gras${c}s${se},
            wh${s}ere it was settled on some sodden sa${c}n${se}d
            ha${s}rd by the torrent of a mountain pass.${c}$se
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, doesntAffectTest = true))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test enable character selection caret to the left`() {
    configureByText(
      """
            A Discovery

            I $s${c}found$se it in a legendary land
            al$s${c}l roc${se}ks and lavender and tufted grass,
            wh$s${c}ere i${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.NORMAL())
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertCaretsVisualAttributes()

    typeText(injector.parser.parseKeys("l"))
    assertState(
      """
            A Discovery

            I f$s${c}ound$se it in a legendary land
            all$s$c roc${se}ks and lavender and tufted grass,
            whe$s${c}re i${se}t was settled on some sodden sand
            hard by the torrent of a mountain pass.
      """.trimIndent(),
    )
    assertMode(Mode.VISUAL(SelectionType.BLOCK_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, limitedValues = [OptionConstants.selectmode_ideaselection]))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test control selection`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
    VimListenerManager.EditorListeners.addAll()
    assertMode(Mode.NORMAL())

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(5, 10)
    }

    waitAndAssertMode(fixture, Mode.SELECT(SelectionType.CHARACTER_WISE))
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, limitedValues = [""]))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test control selection to visual mode`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
    VimListenerManager.EditorListeners.addAll()
    assertMode(Mode.NORMAL())

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(5, 10)
    }

    waitAndAssertMode(fixture, Mode.VISUAL(SelectionType.CHARACTER_WISE))
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, limitedValues = [""]))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test control selection from line to char visual modes`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("V"))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(2, 5)
    }
    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)

    waitAndAssert { fixture.editor.vim.mode.selectionType == SelectionType.CHARACTER_WISE }
    assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
    assertCaretsVisualAttributes()
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, limitedValues = [""]))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test control selection from line to char visual modes in keep mode`() {
    configureByText(
      """
            Lorem Ipsum

            I ${c}found it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )

    startDummyTemplate()

    ApplicationManager.getApplication().invokeAndWait {
      VimVisualTimer.doNow()
    }

    typeText(injector.parser.parseKeys("<esc>V"))
    assertMode(Mode.VISUAL(SelectionType.LINE_WISE))

    ApplicationManager.getApplication().invokeAndWait {
      fixture.editor.selectionModel.setSelection(2, 5)
      IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)

      waitAndAssert { fixture.editor.vim.mode.selectionType == SelectionType.CHARACTER_WISE }
      assertMode(Mode.VISUAL(SelectionType.CHARACTER_WISE))
      assertCaretsVisualAttributes()
    }
  }

  @OptionTest(VimOption(TestOptionConstants.selectmode, limitedValues = [""]))
  @TestWithoutNeovim(reason = SkipNeovimReason.NOT_VIM_TESTING)
  fun `test control selection interruption`() {
    configureByText(
      """
            Lorem Ipsum

            I $s${c}found$se it in a legendary land
            consectetur adipiscing elit
      """.trimIndent(),
    )

    IdeaSelectionControl.controlNonVimSelectionChange(fixture.editor)
    typeText(injector.parser.parseKeys("V"))
    assertNull(VimVisualTimer.swingTimer)
  }

  private fun startDummyTemplate() {
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteIntentReadAction<Any, Throwable> {
        TemplateManagerImpl.setTemplateTesting(fixture.testRootDisposable)
        val templateManager = TemplateManager.getInstance(fixture.project)
        val createdTemplate = templateManager.createTemplate("", "")
        createdTemplate.addVariable(ConstantNode("1"), true)
        templateManager.startTemplate(fixture.editor, createdTemplate)
      }
    }
  }
}
