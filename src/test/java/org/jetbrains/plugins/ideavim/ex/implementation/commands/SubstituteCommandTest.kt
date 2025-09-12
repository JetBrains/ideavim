/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package org.jetbrains.plugins.ideavim.ex.implementation.commands

import org.jetbrains.plugins.ideavim.SkipNeovimReason
import org.jetbrains.plugins.ideavim.TestOptionConstants
import org.jetbrains.plugins.ideavim.TestWithoutNeovim
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.jetbrains.plugins.ideavim.impl.OptionTest
import org.jetbrains.plugins.ideavim.impl.TraceOptions
import org.jetbrains.plugins.ideavim.impl.VimOption
import org.junit.jupiter.api.Disabled

/**
 * @author Alex Plate
 */
@TraceOptions(TestOptionConstants.smartcase, TestOptionConstants.ignorecase)
class SubstituteCommandTest : VimTestCase() {
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test one letter`() {
    doTest(
      exCommand("s/a/b/"),
      """
        |a${c}baba
        |ab
      """.trimMargin(),
      """
        |bbaba
        |ab
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test one letter multi per line`() {
    doTest(
      exCommand("s/a/b/g"),
      """
        |a${c}baba
        |ab
      """.trimMargin(),
      """
        |bbbbb
        |ab
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test one letter multi per line whole file`() {
    doTest(
      exCommand("%s/a/b/g"),
      """
        |a${c}baba
        |ab
      """.trimMargin(),
      """
        |bbbbb
        |bb
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test range from line 0 works like from line 1`() {
    doTest(
      exCommand("0,\$s/a/b/g"),
      """
        |a${c}baba
        |ab
      """.trimMargin(),
      """
        |bbbbb
        |bb
      """.trimMargin(),
    )
  }

  // VIM-3428
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute pattern ends with backslash`() {
    doTest(
      exCommand("""s/#/b\\\\/g"""), // :s/#/b\\/g
      "#a#",
      """b\ab\"""
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute pattern contains escaped backslash`() {
    doTest(
      exCommand("""s/#/b\\\\c/g"""), // :s/#/b\\c/g
      "#a#",
      """b\cab\c"""
    )
  }

  // VIM-146
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test eoLto quote`() {
    doTest(
      exCommand("s/$/'/g"),
      """
        |${c}one
        |two
      """.trimMargin(),
      """
        |one'
        |two
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test soLto quote`() {
    doTest(
      exCommand("s/^/'/g"),
      """
        |${c}one
        |two
      """.trimMargin(),
      """
        |'one
        |two
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test dot to nul`() {
    doTest(
      exCommand("s/\\./\\n/g"),
      "${c}one.two.three\n",
      "one\u0000two\u0000three\n",
    )
  }

  // VIM-528
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test groups`() {
    doTest(
      exCommand("s/\\(a\\|b\\)/z\\1/g"),
      "${c}abcdefg",
      "zazbcdefg",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test ampersand group`() {
    doTest(
      exCommand("s/a\\|b/z&/g"),
      "${c}abcdefg",
      "zazbcdefg",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test missing group`() {
    doTest(
      exCommand("s/b/<\\7>/"),
      "${c}abc",
      "a<>c",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test to nl`() {
    doTest(
      exCommand("s/\\./\\r/g"),
      "${c}one.two.three\n",
      "one\ntwo\nthree\n",
    )
  }

  // VIM-289
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test dot to nlDot`() {
    doTest(
      exCommand("s/\\./\\r\\./g"),
      "${c}one.two.three\n",
      "one\n.two\n.three\n",
    )
  }

  // VIM-702
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test end of line to nl`() {
    doTest(
      exCommand("%s/$/\\r/g"),
      """
        ${c}one
        two
        three
        
      """.trimIndent(),
      """
        one
        
        two
        
        three
        
        
        
      """.trimIndent(),
    )
  }

  // VIM-702
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test start of line to nl`() {
    doTest(
      exCommand("%s/^/\\r/g"),
      """
        ${c}one
        two
        three
        
      """.trimIndent(),
      """
        
        one
        
        two
        
        three
        
        
      """.trimIndent(),
    )
  }

  // Tests two things. Firstly, VIM-698, which was a bug in the old regex engine that would skip lines when substituting
  // with newlines and secondly to test the special case of '\n' matching end of file
  @OptionTest(
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute newlines`() {
    // Note that this is correct Vim behaviour. AIUI, Vim (and the old regex engine) use '\0' to delimit lines while
    // matching patterns, so when checking for '\n' checks against NULL. This also matches the end of the file, so with
    // this pattern, we get an additional line
    doTest(
      exCommand("%s/\\n/,\\r/"),
      """
          |1
          |2
          |3
          |4
        |""".trimMargin(),
      """
          |1,
          |2,
          |3,
          |4,
          |,
        |""".trimMargin()
    )
  }

  // VIM-2141
  @OptionTest(
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with multiline regex`() {
    doTest(
      exCommand("""%s/<div>\(\_.\{-}\)<\/div>/<h1>\1<\/h1>/"""),
      """
        |<div>
        |    <p>Para1</p>
        |    <p>Para2</p>
        |</div>
        |<div>
        |    <p>Para3
        |           is two lines.</p>
        |    <p>Para4</p>
        |</div>
        |""".trimMargin(),
      """
        |<h1>
        |    <p>Para1</p>
        |    <p>Para2</p>
        |</h1>
        |<h1>
        |    <p>Para3
        |           is two lines.</p>
        |    <p>Para4</p>
        |</h1>
        |""".trimMargin()
    )
  }

  // VIM-2141
  @OptionTest(
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with multiline regex 2`() {
    doTest(
      exCommand("""%s/<div>\(\_.\{-}\)<\/div>/Gone with the div/"""),
      """
        |<div>
        |    <p>Para1</p>
        |    <p>Para2</p>
        |</div>
        |<div>
        |    <p>Para3
        |           is two lines.</p>
        |    <p>Para4</p>
        |</div>
        |""".trimMargin(),
      """
        |Gone with the div
        |Gone with the div
        |""".trimMargin()
    )
  }

  // VIM-2141
  @OptionTest(
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with multiline regex 3`() {
    doTest(
      exCommand("""%s/<p>\(\_.\{-}\)<\/p>/<span>\1<\/span>/"""),
      """
        |<div>
        |    <p>Para1</p>
        |    <p>Para2</p>
        |</div>
        |<div>
        |    <p>Para3
        |           is two lines.</p>
        |    <p>Para4</p>
        |</div>
        |""".trimMargin(),
      """
        |<div>
        |    <span>Para1</span>
        |    <span>Para2</span>
        |</div>
        |<div>
        |    <span>Para3
        |           is two lines.</span>
        |    <span>Para4</span>
        |</div>
        |""".trimMargin()
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test beginning of file atom`() {
    doTest(
      exCommand("""%s/\%^one/three"""),
      """
        one
        ${c}two
        one
        two
      """.trimIndent(),
      """
        three
        two
        one
        two
      """.trimIndent()
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test end of file atom`() {
    doTest(
      exCommand("""%s/two\%$/three"""),
      """
        one
        two
        one
        two
      """.trimIndent(),
      """
        one
        two
        one
        three
      """.trimIndent()
    )
  }


  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, limitedValues = ["true"]),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test ignorecase option`() {
    doTest(
      exCommand("%s/foo/bar/g"),
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test smartcase option`() {
    // smartcase does nothing if ignorecase is not set
    doTest(
      exCommand("%s/foo/bar/g"),
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar",
    ) {
      enterCommand("set smartcase")
    }
    doTest(
      exCommand("%s/Foo/bar/g"),
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo",
    ) {
      enterCommand("set smartcase")
    }

    doTest(
      exCommand("%s/foo/bar/g"),
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar",
    ) {
      enterCommand("set ignorecase")
    }
    doTest(
      exCommand("%s/Foo/bar/g"),
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo",
    ) {
      enterCommand("set ignorecase")
    }
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test force ignore case flag`() {
    doTest(
      exCommand("%s/foo/bar/gi"),
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar",
    )

    doTest(
      exCommand("%s/foo/bar/gi"),
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar",
    ) {
      enterCommand("set ignorecase")
    }

    doTest(
      exCommand("%s/foo/bar/gi"),
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar",
    ) {
      enterCommand("set smartcase")
    }
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test force match case flag`() {
    doTest(
      exCommand("%s/foo/bar/gI"),
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar",
    )

    enterCommand("set ignorecase")
    doTest(
      exCommand("%s/foo/bar/gI"),
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar",
    )

    enterCommand("set smartcase")
    doTest(
      exCommand("%s/Foo/bar/gI"),
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo",
    )
  }

  // VIM-864
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test visual substitute doesnt change visual marks`() {
    configureByText("foo\nbar\nbaz\n")
    typeText("V", "j", ":'<,'>s/foo/fuu/<Enter>", "gv", "~")
    assertState("FUU\nBAR\nbaz\n")
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test offset range`() {
    doTest(
      exCommand(".,+2s/a/b/g"),
      "aaa\naa${c}a\naaa\naaa\naaa\n",
      "aaa\nbbb\nbbb\nbbb\naaa\n",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test with tabs`() {
    doTest(exCommand("s/foo/bar"), "\tfoo", "\tbar")
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm all replaces all in range`() {
    // Make sure the "a" is added as part of the same injector.parser.parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand(".,\$s/and/AND/gc"), "a"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
      """I found it in a legendary land
        |all rocks AND lavender AND tufted grass,
        |${c}where it was settled on some sodden sAND
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm all replaces all in rest of range`() {
    // Make sure the "a" is added as part of the same injector.parser.parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand("%s/and/AND/gc"), "n", "n", "a"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
      """I found it in a legendary land
        |all rocks and lavender AND tufted grass,
        |${c}where it was settled on some sodden sAND
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm options`() {
    // Make sure the "a" is added as part of the same injector.parser.parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand("%s/and/AND/gc"), "y", "n", "l"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
      """I found it in a legendary lAND
        |${c}all rocks and lavender AND tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm options with quit`() {
    // Make sure the "a" is added as part of the same injector.parser.parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(
      listOf(exCommand("%s/and/AND/gc"), "y", "n", "q"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
      """I found it in a legendary lAND
        |all rocks and lavender ${c}and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm moves caret to first match`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )

    enterCommand("%s/and/or/gc")
    assertPosition(0, 27)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test confirm moves caret to next match`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.
      """.trimMargin(),
    )

    typeText(exCommand("%s/and/or/gc"), "n")
    assertPosition(1, 10)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test alternative range format`() {
    configureByText(
      """One
        |${c}One
        |One
        |One
        |One
        |One
      """.trimMargin(),
    )

    enterCommand(",+3s/One/Two/g")
    assertState(
      """One
        |Two
        |Two
        |Two
        |${c}Two
        |One
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test alternative range format with second dot`() {
    configureByText(
      """One
        |${c}One
        |One
        |One
        |One
        |One
      """.trimMargin(),
    )

    enterCommand(",.+3s/One/Two/g") // todo empty left range!
    assertState(
      """One
        |Two
        |Two
        |Two
        |${c}Two
        |One
      """.trimMargin(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution with reset flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found it in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. :s does not keep the 'i' flag, so there is no match for `I`
    doTest(listOf(exCommand("s/I/z/i"), exCommand("s")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand repeats last substitution with reset flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found it in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. :s does not keep the 'i' flag, so there is no match for `I`
    doTest(listOf(exCommand("s/I/z/i"), exCommand("&")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution with new flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found zt in a legendary land"

    // Substitute lowercase `i`, then repeat ignoring case, replacing the initial uppercase `I`
    doTest(listOf(exCommand("s/i/z"), exCommand("s i")), before, after)
  }

  @VimBehaviorDiffers(
    description = "Vim supports :s[flags] but IdeaVim's command parser does not handle this." +
      "It tries to find a command called e.g. 'si'",
  )
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @Disabled
  fun `test colon-s repeats last substitution with new flags (no spaces)`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found zt in a legendary land"

    doTest(listOf(exCommand("s/i/z"), exCommand("si")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand repeats last substitution with new flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found zt in a legendary land"

    // Substitute lowercase `i`, then repeat ignoring case, replacing the initial uppercase `I`
    doTest(listOf(exCommand("s/i/z"), exCommand("& i")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s ampersand does NOT repeat last substitution`() {
    // :s [flag] - & means use previous flags
    // See :h :& - "Note that after :substitute the '&' flag can't be used, it's recognized as a separator"
    val before = "${c}I found it in a legendary land"
    val after = "${c}z found it in a legendary land"

    // Help :&& - "Note that :s and :& don't keep the flags"
    // Change the first I, ignoring case. `:s &` *does not keep flags* (:s/P/z/& would)
    doTest(listOf(exCommand("s/I/z/i"), exCommand("s &")), before, after)
    assertPluginErrorMessage("E486: Pattern not found: I")
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
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

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-s repeats last substitution after search`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I found zt zn a legendary land"

    // Change the first `i`. Search for `d`. Repeat substitution to change second `i` (not the `d`!)
    doTest(listOf(exCommand("s/i/z"), searchCommand("/d"), exCommand("s")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-tilde repeats last substitution with last search pattern`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I founz zt in a legendary land"

    // Change the first `i`. Search for `d`. Repeat substitution with the last search pattern (same as `s/d/z/`)
    doTest(listOf(exCommand("s/i/z"), searchCommand("/d"), exCommand("~")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test colon-ampersand r repeats last substitution with last search pattern`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I founz zt in a legendary land"

    // Change the first `i`. Search for `d`. Repeat substitution with the last search pattern (same as `s/d/z/`)
    doTest(listOf(exCommand("s/i/z"), searchCommand("/d"), exCommand("&r")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test new substitution using previous flags`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I fouxd zt zx a legexdary laxd"

    // Change all `i`. Change all `n`, by reusing the `g` flag from the previous substitution
    doTest(listOf(exCommand("s/i/z/g"), exCommand("s/n/x/&")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace tilde with last replace string`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I found zzzt in a zzzegendary zzzand"

    // Change `i` to `zzz`. Use `~` to change `l` to `zzz`
    doTest(listOf(exCommand("s/i/zzz"), exCommand("s/l/~"), exCommand("s/l/~")), before, after)
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace tilde with last replace string 2`() {
    val before = "${c}I found it in a legendary land"
    val after = "${c}I found zzzt in a aazzzbbzzzegendary land"

    // Change `i` to `zzz`. Use `~` to change `l` to `zzz`
    doTest(listOf(exCommand("s/i/zzz"), exCommand("s/l/aa~bb~")), before, after)
  }

  // VIM-2409
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test inline comment is a part of substitute command`() {
    doTest(
      exCommand("s/'/\"/g"),
      "'quoted string'",
      "\"quoted string\"",
    )
  }

  // VIM-2417
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test bar in subsitute command`() {
    doTest(
      exCommand("%s/|/\\&"),
      "true | true = true",
      "true & true = true",
    )
  }

  // Incsearch highlights handled by SearchGroupTest

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test underscore as delimiter`() {
    doTest(
      exCommand("s_1_2"),
      "1 + 2 = 4",
      "2 + 2 = 4",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test simple expression`() {
    configureByText(
      """
      val s1 = "oh"
      val s2 = "hi"
      val s3 = "Mark"
      """.trimIndent(),
    )
    enterCommand("%s/\\d/\\=21*2")
    assertState(
      """
      val s42 = "oh"
      val s42 = "hi"
      val s42 = "Mark"
      """.trimIndent(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test line-dependent expression`() {
    configureByText(
      """
      0. Milk (1 l.)
      0. Bread
      0. Coke (2 l.)
      """.trimIndent(),
    )
    enterCommand("%s/\\d\\+/\\=line('.')")
    assertState(
      """
      1. Milk (1 l.)
      2. Bread
      3. Coke (2 l.)
      """.trimIndent(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with submatch function`() {
    configureByText(
      """
      val ch1 = tree.getChild(0)
      ${c}val ch1 = tree.getChild(0)
      """.trimIndent(),
    )
    enterCommand("s/\\d\\+/\\=submatch(0)+1/g")
    assertState(
      """
      val ch1 = tree.getChild(0)
      val ch2 = tree.getChild(1)
      """.trimIndent(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with submatch function2`() {
    configureByText(
      """
      val ch1 = tree.getChild(0)
      val ch1 = tree.getChild(0)
      val ch1 = tree.getChild(0)
      val ch1 = tree.getChild(0)
      """.trimIndent(),
    )
    executeVimscript(
      """
      function! IncrementWholeLine() range|
        execute ":" .. a:firstline .. "," .. a:lastline .. "s/\\d\\+/\\=submatch(0)+line('.')-a:firstline+1/g"|
      endfunction
      """.trimIndent(),
    )
    enterCommand("2,4call IncrementWholeLine()")
    assertState(
      """
      val ch1 = tree.getChild(0)
      val ch2 = tree.getChild(1)
      val ch3 = tree.getChild(2)
      val ch4 = tree.getChild(3)
      """.trimIndent(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test exception during expression evaluation`() {
    configureByText(
      """
      val str = "first"
      16128
      16132
      16136
      16140
      val str2 = "second"
      """.trimIndent(),
    )
    enterCommand("%s/\\d\\+/\\=printf('0x%04x', submatch(0))")
    assertPluginError(true)
    assertPluginErrorMessage("E117: Unknown function: printf")
    assertState(
      """
      val str = "first"
      
      
      
      
      val str = "second"
      """.trimIndent(),
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test invalid expression`() {
    configureByText(
      """
      val str = "first"
      16128
      16132
      16136
      16140
      val str2 = "second"
      """.trimIndent(),
    )
    enterCommand("%s/\\d\\+/\\=*&(")
    assertPluginError(true)
    assertPluginErrorMessage("E15: Invalid expression: \"*&(\"")
    assertState(
      """
      val str = "first"
      
      
      
      
      val str = "second"
      """.trimIndent(),
    )
  }

  // VIM-2553
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test removing consecutive matches`() {
    doTest(
      exCommand("%s/[*/]//g"),
      "/* comment */",
      " comment ",
    )
  }

  // VIM-3510
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace action U`() {
    doTest(
      exCommand("s/\\(foo\\)/\\U\\1bar/"),
      "${c}a foo",
      "a FOOBAR",
    )
  }

  // VIM-3510
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace action U and E`() {
    doTest(
      exCommand("s/\\(foo\\)/\\U\\1\\ebar/"),
      "${c}a foo",
      "a FOObar",
    )
  }

  // VIM-3510
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace action u`() {
    doTest(
      exCommand("s/\\(foo\\)/\\u\\1bar/"),
      "${c}a foo",
      "a Foobar",
    )
  }

  // VIM-3510
  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test replace action u and empty group`() {
    doTest(
      exCommand("s/a foo\\(\\)/a foo\\u\\1bar/"),
      "${c}a foo",
      "a fooBar",
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute and undo`() {
    configureByText(
      """
      |Hello ${c}world
      |Hello world
      |Hello world
      """.trimMargin()
    )

    typeText(commandToKeys("s/world/universe/"))
    assertState(
      """
      |${c}Hello universe
      |Hello world
      |Hello world
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |Hello ${c}world
      |Hello world
      |Hello world
      """.trimMargin()
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute global and undo`() {
    configureByText(
      """
      |${c}Hello world world world
      |Hello world
      """.trimMargin()
    )

    typeText(commandToKeys("s/world/universe/g"))
    assertState(
      """
      |${c}Hello universe universe universe
      |Hello world
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |${c}Hello world world world
      |Hello world
      """.trimMargin()
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with range and undo`() {
    configureByText(
      """
      |First line
      |${c}Hello world
      |Hello world
      |Hello world
      |Last line
      """.trimMargin()
    )

    typeText(commandToKeys("2,4s/world/universe/"))
    assertState(
      """
      |First line
      |Hello universe
      |Hello universe
      |${c}Hello universe
      |Last line
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |First line
      |${c}Hello world
      |Hello world
      |Hello world
      |Last line
      """.trimMargin()
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute all lines and undo`() {
    configureByText(
      """
      |${c}Hello world
      |Hello world
      |Hello world
      """.trimMargin()
    )

    typeText(commandToKeys("%s/world/universe/"))
    assertState(
      """
      |Hello universe
      |Hello universe
      |${c}Hello universe
      """.trimMargin()
    )

    typeText("u")
    assertState(
      """
      |${c}Hello world
      |Hello world
      |Hello world
      """.trimMargin()
    )
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute and undo with oldundo`() {
    configureByText(
      """
      |Hello ${c}world
      |Hello world
      |Hello world
      """.trimMargin()
    )

    try {
      enterCommand("set oldundo")
      typeText(commandToKeys("s/world/universe/"))
      assertState(
        """
      |${c}Hello universe
      |Hello world
      |Hello world
      """.trimMargin()
      )

      typeText("u")
      assertState(
        """
      |Hello ${c}world
      |Hello world
      |Hello world
      """.trimMargin()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute global and undo with oldundo`() {
    configureByText(
      """
      |${c}Hello world world world
      |Hello world
      """.trimMargin()
    )

    try {
      enterCommand("set oldundo")
      typeText(commandToKeys("s/world/universe/g"))
      assertState(
        """
      |${c}Hello universe universe universe
      |Hello world
      """.trimMargin()
      )

      typeText("u")
      assertState(
        """
      |${c}Hello world world world
      |Hello world
      """.trimMargin()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute with range and undo with oldundo`() {
    configureByText(
      """
      |First line
      |${c}Hello world
      |Hello world
      |Hello world
      |Last line
      """.trimMargin()
    )

    try {
      enterCommand("set oldundo")
      typeText(commandToKeys("2,4s/world/universe/"))
      assertState(
        """
      |First line
      |Hello universe
      |Hello universe
      |${c}Hello universe
      |Last line
      """.trimMargin()
      )

      typeText("u")
      assertState(
        """
      |First line
      |${c}Hello world
      |Hello world
      |Hello world
      |Last line
      """.trimMargin()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }

  @OptionTest(
    VimOption(TestOptionConstants.smartcase, doesntAffectTest = true),
    VimOption(TestOptionConstants.ignorecase, doesntAffectTest = true),
  )
  @TestWithoutNeovim(reason = SkipNeovimReason.OPTION)
  fun `test substitute all lines and undo with oldundo`() {
    configureByText(
      """
      |${c}Hello world
      |Hello world
      |Hello world
      """.trimMargin()
    )

    try {
      enterCommand("set oldundo")
      typeText(commandToKeys("%s/world/universe/"))
      assertState(
        """
      |Hello universe
      |Hello universe
      |${c}Hello universe
      """.trimMargin()
      )

      typeText("u")
      assertState(
        """
      |${c}Hello world
      |Hello world
      |Hello world
      """.trimMargin()
      )
    } finally {
      enterCommand("set nooldundo")
    }
  }
}
