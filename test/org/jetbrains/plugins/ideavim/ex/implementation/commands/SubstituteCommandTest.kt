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

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.IgnoreCaseOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SmartCaseOptionsData
import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimOptionDefault
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType

/**
 * @author Alex Plate
 */
class SubstituteCommandTest : VimOptionTestCase(SmartCaseOptionsData.name, IgnoreCaseOptionsData.name) {
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test one letter`() {
    doTest(
      "s/a/b/",
      """a${c}baba
                 |ab
               """.trimMargin(),
      """bbaba
                 |ab
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test one letter multi per line`() {
    doTest(
      "s/a/b/g",
      """a${c}baba
                 |ab
               """.trimMargin(),
      """bbbbb
                 |ab
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test one letter multi per line whole file`() {
    doTest(
      "%s/a/b/g",
      """a${c}baba
                 |ab
               """.trimMargin(),
      """bbbbb
                 |bb
               """.trimMargin()
    )
  }

  // VIM-146
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test eoLto quote`() {
    doTest(
      "s/$/'/g",
      """${c}one
                  |two
               """.trimMargin(),
      """one'
                  |two
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test soLto quote`() {
    doTest(
      "s/^/'/g",
      """${c}one
                  |two
               """.trimMargin(),
      """'one
                  |two
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test dot to nul`() {
    doTest(
      "s/\\./\\n/g",
      "${c}one.two.three\n",
      "one\u0000two\u0000three\n"
    )
  }

  // VIM-528
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test groups`() {
    doTest(
      "s/\\(a\\|b\\)/z\\1/g",
      "${c}abcdefg",
      "zazbcdefg"
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test to nl`() {
    doTest(
      "s/\\./\\r/g",
      "${c}one.two.three\n",
      "one\ntwo\nthree\n"
    )
  }

  // VIM-289
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test dot to nlDot`() {
    doTest(
      "s/\\./\\r\\./g",
      "${c}one.two.three\n",
      "one\n.two\n.three\n"
    )
  }

  // VIM-702
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test end of line to nl`() {
    doTest(
      "%s/$/\\r/g",
      """
        ${c}one
        two
        three
        
      """.trimIndent(),
      """
        one
        
        two
        
        three
        
        
        
      """.trimIndent()
    )
  }

  // VIM-702
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test start of line to nl`() {
    doTest(
      "%s/^/\\r/g",
      """
        ${c}one
        two
        three
        
      """.trimIndent(),
      """
        
        one
        
        two
        
        three
        
        
      """.trimIndent()
    )
  }

  @VimOptionTestConfiguration(VimTestOption(IgnoreCaseOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  @VimOptionDefault(SmartCaseOptionsData.name)
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test ignorecase option`() {
    doTest(
      "%s/foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar"
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test smartcase option`() {
    OptionsManager.smartcase.set()

    // smartcase does nothing if ignorecase is not set
    doTest(
      "%s/foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar"
    )
    doTest(
      "%s/Foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo"
    )

    OptionsManager.ignorecase.set()
    doTest(
      "%s/foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar"
    )
    doTest(
      "%s/Foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo"
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test force ignore case flag`() {
    doTest(
      "%s/foo/bar/gi",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar"
    )

    OptionsManager.ignorecase.set()
    doTest(
      "%s/foo/bar/gi",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar"
    )

    OptionsManager.smartcase.set()
    doTest(
      "%s/foo/bar/gi",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar"
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test force match case flag`() {
    doTest(
      "%s/foo/bar/gI",
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar"
    )

    OptionsManager.ignorecase.set()
    doTest(
      "%s/foo/bar/gI",
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar"
    )

    OptionsManager.smartcase.set()
    doTest(
      "%s/Foo/bar/gI",
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo"
    )
  }

  // VIM-864
  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test visual substitute doesnt change visual marks`() {
    configureByText("foo\nbar\nbaz\n")
    typeText(parseKeys("V", "j", ":'<,'>s/foo/fuu/<Enter>", "gv", "~"))
    assertState("FUU\nBAR\nbaz\n")
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test offset range`() {
    doTest(
      ".,+2s/a/b/g",
      "aaa\naa${c}a\naaa\naaa\naaa\n",
      "aaa\nbbb\nbbb\nbbb\naaa\n"
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test multiple carets`() {
    val before = """public class C {
      |  Stri${c}ng a;
      |$c  String b;
      |  Stri${c}ng c;
      |  String d;
      |}
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("s/String/Integer"))

    val after = """public class C {
      |  ${c}Integer a;
      |  ${c}Integer b;
      |  ${c}Integer c;
      |  String d;
      |}
    """.trimMargin()
    assertState(after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test multiple carets substitute all occurrences`() {
    val before = """public class C {
      |  Stri${c}ng a; String e;
      |$c  String b;
      |  Stri${c}ng c; String f;
      |  String d;
      |}
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("s/String/Integer/g"))

    val after = """public class C {
      |  ${c}Integer a; Integer e;
      |  ${c}Integer b;
      |  ${c}Integer c; Integer f;
      |  String d;
      |}
    """.trimMargin()
    assertState(after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test with tabs`() {
    doTest("s/foo/bar", "\tfoo", "\tbar")
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm all replaces all in range`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand(".,\$s/and/AND/gc"), "a"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary land
        |all rocks AND lavender AND tufted grass,
        |${c}where it was settled on some sodden sAND
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm all replaces all in rest of range`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand("%s/and/AND/gc"), "n", "n", "a"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary land
        |all rocks and lavender AND tufted grass,
        |${c}where it was settled on some sodden sAND
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm options`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand("%s/and/AND/gc"), "y", "n", "l"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary lAND
        |${c}all rocks and lavender AND tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm options with quit`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand("%s/and/AND/gc"), "y", "n", "q"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary lAND
        |all rocks and lavender ${c}and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm moves caret to first match`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )

    enterCommand("%s/and/or/gc")
    assertPosition(0, 27)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm moves caret to next match`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin()
    )

    typeText(parseKeys(exCommand("%s/and/or/gc"), "n"))
    assertPosition(1, 10)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test alternative range format`() {
    configureByText(
      """One
        |${c}One
        |One
        |One
        |One
        |One""".trimMargin()
    )

    enterCommand(",+3s/One/Two/g")
    assertState(
      """One
        |Two
        |Two
        |Two
        |${c}Two
        |One""".trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test alternative range format with second dot`() {
    configureByText(
      """One
        |${c}One
        |One
        |One
        |One
        |One""".trimMargin()
    )

    enterCommand(",.+3s/One/Two/g") // todo empty left range!
    assertState(
      """One
        |Two
        |Two
        |Two
        |${c}Two
        |One""".trimMargin()
    )
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute pattern becomes last used pattern for search next`() {
    val before = """
       I found it in a legendary land
       ${c}all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I found it in a legendary land
       all rocks or lavender ${c}and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()

    doTest(listOf(exCommand("s/and/or"), "n"), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution`() {
    val before = """
       ${c}I found it in a legendary land
       all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I found it in a legendary lor
       ${c}all rocks or lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()

    doTest(listOf(exCommand("s/and/or"), "j", exCommand("s")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution without range`() {
    val before = """
       I found it in a legendary land
       ${c}all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I founz it in a legendary land
       ${c}all rocks anz lavenzer and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()

    // Convert the first occurrence of d to z in the first two lines. :s will repeat the substitution on the current line
    doTest(listOf(exCommand("1,2s/d/z"), exCommand("s")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand repeats last substitution without range`() {
    val before = """
       I found it in a legendary land
       ${c}all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I founz it in a legendary land
       ${c}all rocks anz lavenzer and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()

    // Convert the first occurrence of d to z in the first two lines. :s will repeat the substitution on the current line
    doTest(listOf(exCommand("1,2s/d/z"), exCommand("&")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution with new range`() {
    val before = """
       I found it in a legendary land
       ${c}all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I founz it in a legenzary land
       all rocks anz lavenzer and tufted grass,
       where it was settlez on some sodden sand
       ${c}harz by the torrent of a mountain pass.
    """.trimIndent()

    // Convert the first occurrence of d to z in the first two lines. :1,4s will repeat the substitution for all lines
    doTest(listOf(exCommand("1,2s/d/z"), exCommand("1,4s")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand repeats last substitution with new range`() {
    val before = """
       I found it in a legendary land
       ${c}all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I founz it in a legenzary land
       all rocks anz lavenzer and tufted grass,
       where it was settlez on some sodden sand
       ${c}harz by the torrent of a mountain pass.
    """.trimIndent()

    // Convert the first occurrence of d to z in the first two lines. :1,4s will repeat the substitution for all lines
    doTest(listOf(exCommand("1,2s/d/z"), exCommand("1,4&")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution with reset flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found it in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. :s does not keep the 'i' flag, so there is no match for `I`
    doTest(listOf(exCommand("s/I/z/i"), exCommand("s")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand repeats last substitution with reset flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found it in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. :s does not keep the 'i' flag, so there is no match for `I`
    doTest(listOf(exCommand("s/I/z/i"), exCommand("&")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution with new flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found zt in a legendary land"

    // Substitute lowercase `i`, then repeat ignoring case, replacing the initial uppercase `I`
    doTest(listOf(exCommand("s/i/z"), exCommand("s i")), before, after)
  }

//  @VimBehaviorDiffers(description = "Vim supports :s[flags] but IdeaVim's command parser does not handle this." +
//    "It tries to find a command called e.g. 'si'")
//  @VimOptionDefaultAll
//  fun `test colon-s repeats last substitution with new flags (no spaces)`() {
//    val before = "${c}I found it in a legendary land"
//    val after = "${c}z found zt in a legendary land"
//
//    doTest(listOf(exCommand("s/i/z"), exCommand("si")), before, after)
//  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand repeats last substitution with new flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found zt in a legendary land"

    // Substitute lowercase `i`, then repeat ignoring case, replacing the initial uppercase `I`
    doTest(listOf(exCommand("s/i/z"), exCommand("& i")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s ampersand does NOT repeat last substitution`() {
    // :s [flag] - & means use previous flags
    // See :h :& - "Note that after :substitute the '&' flag can't be used, it's recognized as a separator"
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found it in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. `:s &` *does not keep flags* (:s/P/z/& would)
    doTest(listOf(exCommand("s/I/z/i"), exCommand("s &")), before, after)
    assertPluginErrorMessageContains("Pattern not found: I")
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand-ampersand repeats last substitution with previous flags`() {
    // :s [flag] - & means use previous flags
    // See :h :& - "Note that after :substitute the '&' flag can't be used, it's recognized as a separator"
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found zt in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. `:s &` *does not keep flags* (:s/P/z/& would)
    doTest(listOf(exCommand("s/I/z/i"), exCommand("&&")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution with count acts like line range`() {
    val before = """
       ${c}I found it in a legendary land
       all rocks and lavender and tufted grass,
       where it was settled on some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()
    val after = """
       I fouzd it iz a legendary land
       all rocks azd lavender and tufted grass,
       ${c}where it was settled oz some sodden sand
       hard by the torrent of a mountain pass.
    """.trimIndent()

    // Change the first `n` to `z`. Then repeat 3 times - i.e. first occurrence on 3 lines
    doTest(listOf(exCommand("s/n/z"), exCommand("s 3")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution after search`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I found zt zn a legendary land"

    // Change the first `i`. Search for `d`. Repeat substitution to change second `i` (not the `d`!)
    doTest(listOf(exCommand("s/i/z"), searchCommand("/d"), exCommand("s")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-tilde repeats last substitution with last search pattern`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I founz zt in a legendary land"

    // Change the first `i`. Search for `d`. Repeat substitution with the last search pattern (same as `s/d/z/`)
    doTest(listOf(exCommand("s/i/z"), searchCommand("/d"), exCommand("~")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand r repeats last substitution with last search pattern`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I founz zt in a legendary land"

    // Change the first `i`. Search for `d`. Repeat substitution with the last search pattern (same as `s/d/z/`)
    doTest(listOf(exCommand("s/i/z"), searchCommand("/d"), exCommand("&r")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test new substitution using previous flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I fouxd zt zx a legexdary laxd"

    // Change all `i`. Change all `n`, by reusing the `g` flag from the previous substitution
    doTest(listOf(exCommand("s/i/z/g"), exCommand("s/n/x/&")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace tilde with last replace string`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I found zzzt in a zzzegendary land"

    // Change `i` to `zzz`. Use `~` to change `l` to `zzz`
    doTest(listOf(exCommand("s/i/zzz"), exCommand("s/l/~")), before, after)
  }

  @VimOptionDefaultAll
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace tilde with last replace string 2`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I found zzzt in a aazzzbbzzzegendary land"

    // Change `i` to `zzz`. Use `~` to change `l` to `zzz`
    doTest(listOf(exCommand("s/i/zzz"), exCommand("s/l/aa~bb~")), before, after)
  }

  // Incsearch highlights handled by SearchGroupTest

  private fun doTest(command: String, before: String, after: String) {
    doTest(listOf(exCommand(command)), before, after)
  }
}
