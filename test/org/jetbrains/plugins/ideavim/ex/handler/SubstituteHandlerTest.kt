package org.jetbrains.plugins.ideavim.ex.handler

import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

/**
 * @author Alex Plate
 */
class SubstituteHandlerTest : VimTestCase() {
    fun `test one letter`() {
        doTest("s/a/b/",
                """a<caret>baba
                 |ab
               """.trimMargin(),
                """bbaba
                 |ab
               """.trimMargin()
        )
    }

    fun `test one letter multi per line`() {
        doTest("s/a/b/g",
                """a<caret>baba
                 |ab
               """.trimMargin(),
                """bbbbb
                 |ab
               """.trimMargin()
        )
    }

    fun `test one letter multi per line whole file`() {
        doTest("%s/a/b/g",
                """a<caret>baba
                 |ab
               """.trimMargin(),
                """bbbbb
                 |bb
               """.trimMargin()
        )
    }

    // VIM-146
    fun `test eoLto quote`() {
        doTest("s/$/'/g",
                """<caret>one
                  |two
               """.trimMargin(),
                """one'
                  |two
               """.trimMargin()
        )
    }

    fun `test soLto quote`() {
        doTest("s/^/'/g",
                """<caret>one
                  |two
               """.trimMargin(),
                """'one
                  |two
               """.trimMargin()
        )
    }

    fun `test dot to nul`() {
        doTest("s/\\./\\n/g",
                "<caret>one.two.three\n",
                "one\u0000two\u0000three\n")
    }

    // VIM-528
    fun `test groups`() {
        doTest("s/\\(a\\|b\\)/z\\1/g",
                "<caret>abcdefg",
                "zazbcdefg")
    }

    fun `test to nl`() {
        doTest("s/\\./\\r/g",
                "<caret>one.two.three\n",
                "one\ntwo\nthree\n")
    }

    // VIM-289
    fun `test dot to nlDot`() {
        doTest("s/\\./\\r\\./g",
                "<caret>one.two.three\n",
                "one\n.two\n.three\n")
    }

    // VIM-702
    fun `test end of line to nl`() {
        doTest("%s/$/\\r/g",
                "<caret>one\ntwo\nthree\n",
                "one\n\ntwo\n\nthree\n\n")
    }

    // VIM-702
    fun `test start of line to nl`() {
        doTest("%s/^/\\r/g",
                "<caret>one\ntwo\nthree\n",
                "\none\n\ntwo\n\nthree\n")
    }

    // VIM-864
    fun `test visual substitute doesnt change visual marks`() {
        myFixture.configureByText("a.java", "foo\nbar\nbaz\n")
        typeText(parseKeys("V", "j", ":'<,'>s/foo/fuu/<Enter>", "gv", "~"))
        myFixture.checkResult("FUU\nBAR\nbaz\n")
    }

    fun `test offset range`() {
        doTest(".,+2s/a/b/g",
                "aaa\naa<caret>a\naaa\naaa\naaa\n",
                "aaa\nbbb\nbbb\nbbb\naaa\n")
    }

    fun `test multiple carets`() {
        val before = """public class C {
      |  Stri<caret>ng a;
      |<caret>  String b;
      |  Stri<caret>ng c;
      |  String d;
      |}
    """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("s/String/Integer"))

        val after = """public class C {
      |  <caret>Integer a;
      |  <caret>Integer b;
      |  <caret>Integer c;
      |  String d;
      |}
    """.trimMargin()
        myFixture.checkResult(after)
    }

    fun `test multiple carets substitute all occurrences`() {
        val before = """public class C {
      |  Stri<caret>ng a; String e;
      |<caret>  String b;
      |  Stri<caret>ng c; String f;
      |  String d;
      |}
    """.trimMargin()
        configureByJavaText(before)

        typeText(commandToKeys("s/String/Integer/g"))

        val after = """public class C {
      |  <caret>Integer a; Integer e;
      |  <caret>Integer b;
      |  <caret>Integer c; Integer f;
      |  String d;
      |}
    """.trimMargin()
        myFixture.checkResult(after)
    }

    private fun doTest(command: String, before: String, after: String) {
        myFixture.configureByText("a.java", before)
        typeText(commandToKeys(command))
        myFixture.checkResult(after)
    }
}