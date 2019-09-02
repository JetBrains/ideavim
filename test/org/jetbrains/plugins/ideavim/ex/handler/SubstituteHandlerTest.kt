/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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

package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import com.maddyhome.idea.vim.option.IgnoreCaseOptionsData
import com.maddyhome.idea.vim.option.OptionsManager
import com.maddyhome.idea.vim.option.SmartCaseOptionsData
import org.jetbrains.plugins.ideavim.VimOptionDefault
import org.jetbrains.plugins.ideavim.VimOptionDefaultAll
import org.jetbrains.plugins.ideavim.VimOptionTestCase
import org.jetbrains.plugins.ideavim.VimOptionTestConfiguration
import org.jetbrains.plugins.ideavim.VimTestOption
import org.jetbrains.plugins.ideavim.VimTestOptionType
import javax.swing.KeyStroke

/**
 * @author Alex Plate
 */
class SubstituteHandlerTest : VimOptionTestCase(SmartCaseOptionsData.name, IgnoreCaseOptionsData.name) {
  @VimOptionDefaultAll
  fun `test one letter`() {
    doTest("s/a/b/",
      """a${c}baba
                 |ab
               """.trimMargin(),
      """bbaba
                 |ab
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  fun `test one letter multi per line`() {
    doTest("s/a/b/g",
      """a${c}baba
                 |ab
               """.trimMargin(),
      """bbbbb
                 |ab
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  fun `test one letter multi per line whole file`() {
    doTest("%s/a/b/g",
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
  fun `test eoLto quote`() {
    doTest("s/$/'/g",
      """${c}one
                  |two
               """.trimMargin(),
      """one'
                  |two
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  fun `test soLto quote`() {
    doTest("s/^/'/g",
      """${c}one
                  |two
               """.trimMargin(),
      """'one
                  |two
               """.trimMargin()
    )
  }

  @VimOptionDefaultAll
  fun `test dot to nul`() {
    doTest("s/\\./\\n/g",
      "${c}one.two.three\n",
      "one\u0000two\u0000three\n")
  }

  // VIM-528
  @VimOptionDefaultAll
  fun `test groups`() {
    doTest("s/\\(a\\|b\\)/z\\1/g",
      "${c}abcdefg",
      "zazbcdefg")
  }

  @VimOptionDefaultAll
  fun `test to nl`() {
    doTest("s/\\./\\r/g",
      "${c}one.two.three\n",
      "one\ntwo\nthree\n")
  }

  // VIM-289
  @VimOptionDefaultAll
  fun `test dot to nlDot`() {
    doTest("s/\\./\\r\\./g",
      "${c}one.two.three\n",
      "one\n.two\n.three\n")
  }

  // VIM-702
  @VimOptionDefaultAll
  fun `test end of line to nl`() {
    doTest("%s/$/\\r/g",
      "${c}one\ntwo\nthree\n",
      "one\n\ntwo\n\nthree\n\n")
  }

  // VIM-702
  @VimOptionDefaultAll
  fun `test start of line to nl`() {
    doTest("%s/^/\\r/g",
      "${c}one\ntwo\nthree\n",
      "\none\n\ntwo\n\nthree\n")
  }

  @VimOptionTestConfiguration(VimTestOption(IgnoreCaseOptionsData.name, VimTestOptionType.TOGGLE, ["true"]))
  @VimOptionDefault(SmartCaseOptionsData.name)
  fun `test ignorecase option`() {
    doTest("%s/foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar")
  }

  @VimOptionDefaultAll
  fun `test smartcase option`() {
    OptionsManager.smartcase.set()

    // smartcase does nothing if ignorecase is not set
    doTest("%s/foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar")
    doTest("%s/Foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo")

    OptionsManager.ignorecase.set()
    doTest("%s/foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar")
    doTest("%s/Foo/bar/g",
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo")
  }

  @VimOptionDefaultAll
  fun `test force ignore case flag`() {
    doTest("%s/foo/bar/gi",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar")

    OptionsManager.ignorecase.set()
    doTest("%s/foo/bar/gi",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar")

    OptionsManager.smartcase.set()
    doTest("%s/foo/bar/gi",
      "foo Foo foo\nFoo FOO foo",
      "bar bar bar\nbar bar bar")
  }

  @VimOptionDefaultAll
  fun `test force match case flag`() {
    doTest("%s/foo/bar/gI",
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar")

    OptionsManager.ignorecase.set()
    doTest("%s/foo/bar/gI",
      "foo Foo foo\nFoo FOO foo",
      "bar Foo bar\nFoo FOO bar")

    OptionsManager.smartcase.set()
    doTest("%s/Foo/bar/gI",
      "foo Foo foo\nFoo FOO foo",
      "foo bar foo\nbar FOO foo")
  }

  // VIM-864
  @VimOptionDefaultAll
  fun `test visual substitute doesnt change visual marks`() {
    myFixture.configureByText("a.java", "foo\nbar\nbaz\n")
    typeText(parseKeys("V", "j", ":'<,'>s/foo/fuu/<Enter>", "gv", "~"))
    myFixture.checkResult("FUU\nBAR\nbaz\n")
  }

  @VimOptionDefaultAll
  fun `test offset range`() {
    doTest(".,+2s/a/b/g",
      "aaa\naa${c}a\naaa\naaa\naaa\n",
      "aaa\nbbb\nbbb\nbbb\naaa\n")
  }

  @VimOptionDefaultAll
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
    myFixture.checkResult(after)
  }

  @VimOptionDefaultAll
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
    myFixture.checkResult(after)
  }

  @VimOptionDefaultAll
  fun `test with tabs`() {
    doTest("s/foo/bar", "\tfoo", "\tbar")
  }

  @VimOptionDefaultAll
  fun `test confirm all replaces all in range`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(parseKeys(":", ".,\$s/and/AND/gc", "<Enter>", "a"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary land
        |all rocks AND lavender AND tufted grass,
        |${c}where it was settled on some sodden sAND
        |hard by the torrent of a mountain pass.""".trimMargin())
  }

  @VimOptionDefaultAll
  fun `test confirm all replaces all in rest of range`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(parseKeys(":", "%s/and/AND/gc", "<Enter>", "n", "n", "a"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary land
        |all rocks and lavender AND tufted grass,
        |${c}where it was settled on some sodden sAND
        |hard by the torrent of a mountain pass.""".trimMargin())
  }

  @VimOptionDefaultAll
  fun `test confirm options`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(parseKeys(":", "%s/and/AND/gc", "<Enter>", "y", "n", "l"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary lAND
        |${c}all rocks and lavender AND tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
  }

  @VimOptionDefaultAll
  fun `test confirm options with quit`() {
    // Make sure the "a" is added as part of the same parseKeys as the <Enter>, as it needs to be available while the
    // <Enter> is processed
    doTest(parseKeys(":", "%s/and/AND/gc", "<Enter>", "y", "n", "q"),
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin(),
      """I found it in a legendary lAND
        |all rocks and lavender ${c}and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())
  }

  @VimOptionDefaultAll
  fun `test confirm moves caret to first match`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "%s/and/or/gc", "<Enter>"))
    assertPosition(0, 27)
  }

  @VimOptionDefaultAll
  fun `test confirm moves caret to next match`() {
    configureByText(
      """I found it in a legendary land
        |${c}all rocks and lavender and tufted grass,
        |where it was settled on some sodden sand
        |hard by the torrent of a mountain pass.""".trimMargin())

    typeText(parseKeys(":", "%s/and/or/gc", "<Enter>", "n"))
    assertPosition(1, 10)
  }

  private fun doTest(command: String, before: String, after: String) {
    doTest(commandToKeys(command), before, after)
  }

  private fun doTest(keys: List<KeyStroke>, before: String, after: String) {
    myFixture.configureByText("a.java", before)
    typeText(keys)
    myFixture.checkResult(after)
  }
}
