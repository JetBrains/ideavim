package org.jetbrains.plugins.ideavim.ex

import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.command.SelectionType
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.helper.StringHelper.parseKeys
import org.jetbrains.plugins.ideavim.VimTestCase

class MultipleCaretsTest : VimTestCase() {
  fun testGotoToNthCharacter() {
    val before = "qwe rty a<caret>sd\n fgh zx<caret>c <caret>vbn"
    configureByText(before)
    typeText(commandToKeys("go 5"))
    val after = "qwe <caret>rty asd\n fgh zxc vbn"
    myFixture.checkResult(after)
  }

  fun testGotoLine() {
    val before = "qwe\n" + "rty\n" + "asd\n" + "f<caret>gh\n" + "zxc\n" + "v<caret>bn\n"
    configureByText(before)
    typeText(commandToKeys("2"))
    val after = "qwe\n" + "<caret>rty\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testGotoLineInc() {
    val before = "qwe\n" + "rt<caret>y\n" + "asd\n" + "fgh\n" + "zxc\n" + "v<caret>bn\n"
    configureByText(before)
    typeText(commandToKeys("+2"))
    val after = "qwe\n" + "rty\n" + "asd\n" + "<caret>fgh\n" + "zxc\n" + "<caret>vbn\n"
    myFixture.checkResult(after)
  }

  fun testJoinLines() {
    val before = "qwe\n" + "r<caret>ty\n" + "asd\n" + "fg<caret>h\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("j"))
    val after = "qwe\n" + "rty<caret> asd\n" + "fgh<caret> zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

//  fun testJoinVisualLines() {
//    val before = "qwe\n" + "r<caret>ty\n" + "asd\n" + "fg<caret>h\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys("j"))
//    val after = "qwe\n" + "rty<caret> asd\n" + "fgh<caret> zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testCopyText() {
    val before = "qwe\n" + "rty\n" + "a<caret>sd\n" + "fg<caret>h\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("co 2"))
    val after = "qwe\n" + "rty\n" + "<caret>asd\n" + "<caret>fgh\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

//  fun testCopyVisualText() {
//    val before = "qwe\n" + "<caret>rty\n" + "asd\n" + "f<caret>gh\n" + "zxc\n" + "vbn\n"
//    configureByText(before)
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys(":co 2"))
//    val after = "qwe\n" + "rty\n" + "<caret>rty\n" + "asd\n" + "<caret>fgh\n" + "zxc\n" + "asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testPutText() {
    val before = "<caret>qwe\n" + "rty\n" + "<caret>as<caret>d\n" + "fgh\n" + "zxc\n" + "vbn\n"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(commandToKeys("pu"))
    val after = "qwe\n" + "<caret>zxc\n" + "rty\n" + "asd\n" + "<caret>zxc\n" + "<caret>zxc\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testPutTextCertainLine() {
    val before = "<caret>qwe\n" + "rty\n" + "<caret>as<caret>d\n" + "fgh\n" + "zxc\n" + "vbn\n"
    val editor = configureByText(before)
    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
    typeText(commandToKeys("4pu"))
    val after = "qwe\n" + "rty\n" + "asd\n" + "fgh\n" + "<caret>zxc\n" + "<caret>zxc\n" + "<caret>zxc\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

//  fun testPutVisualLines() {
//    val before = "<caret>qwe\n" + "rty\n" + "as<caret>d\n" + "fgh\n" + "zxc\n" + "vbn\n"
//    val editor = configureByText(before)
//    VimPlugin.getRegister().storeText(editor, TextRange(16, 19), SelectionType.CHARACTER_WISE, false)
//
//    typeText(parseKeys("vj"))
//    typeText(commandToKeys("pu"))
//
//    val after = "qwe\n" + "rty\n" + "<caret>zxc\n" + "asd\n" + "fgh\n" + "<caret>zxc\n" + "zxc\n" + "vbn\n"
//    myFixture.checkResult(after)
//  }

  fun testMoveTextBeforeCarets() {
    val before = "qwe\n" + "rty\n" + "<caret>asd\n" + "fgh\n" + "z<caret>xc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("m 1"))
    val after = "qwe\n" + "<caret>asd\n" + "<caret>zxc\n" + "rty\n" + "fgh\n" + "vbn\n"
    myFixture.checkResult(after)

  }

  fun testMoveTextAfterCarets() {
    val before = "q<caret>we\n" + "rty\n" + "<caret>asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("m 4"))
    val after = "rty\n" + "fgh\n" + "zxc\n" + "<caret>qwe\n" + "<caret>asd\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testMoveTextBetweenCarets() {
    val before = "q<caret>we\n" + "rty\n" + "<caret>asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    configureByText(before)
    typeText(commandToKeys("m 2"))
    val after = "rty\n" + "<caret>qwe\n" + "<caret>asd\n" + "fgh\n" + "zxc\n" + "vbn\n"
    myFixture.checkResult(after)
  }

  fun testYankLines() {
    val before = """qwe
      |rt<caret>y
      |asd
      |<caret>fgh
      |zxc
      |vbn
    """.trimMargin()
    configureByText(before)
    typeText(commandToKeys("y"))

    val lastRegister = VimPlugin.getRegister().lastRegister
    assertNotNull(lastRegister)
    val text = lastRegister!!.text
    assertNotNull(text)

    typeText(parseKeys("p"))
    val after = """qwe
      |rty
      |<caret>rty
      |fgh
      |asd
      |fgh
      |<caret>rty
      |fgh
      |zxc
      |vbn
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testDeleteLines() {
    val before = """qwe
      |r<caret>ty
      |asd
      |f<caret>gh
      |zxc
      |vbn
    """.trimMargin()

    configureByText(before)
    typeText(commandToKeys("d"))

    val lastRegister = VimPlugin.getRegister().lastRegister
    assertNotNull(lastRegister)
    val text = lastRegister!!.text
    assertNotNull(text)

    val after = """qwe
      |<caret>asd
      |<caret>zxc
      |vbn
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testShiftLeft() {
    val before = """qwe
      |   r<caret>ty
      |  asd
      |f<caret>gh
      |     z<caret>xc
      |vbn
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys("<"))

    val after = """qwe
      |<caret>rty
      |  asd
      |<caret>fgh
      | <caret>zxc
      |vbn
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testShiftRight() {
    val before = """qw<caret>e
      |   rty
      |  asd
      |f<caret>gh
      |     zxc
      |vb<caret>n
    """.trimMargin()
    configureByJavaText(before)

    typeText(commandToKeys(">>"))

    val after = """        <caret>qwe
      |   rty
      |  asd
      |        <caret>fgh
      |     zxc
      |        <caret>vbn
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSortRangeWholeFile() {
    val before = """qwe
      |as<caret>d
      |zxc
      |<caret>rty
      |fgh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("sor"))

    val after = "<caret>" + before.replace("<caret>", "").split('\n').sorted().joinToString(separator = "\n")
    myFixture.checkResult(after)
  }

  fun testSortRange() {
    val before = """qwe
      |as<caret>d
      | zxc
      |rty
      |f<caret>gh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("2,4 sor"))

    val after = """qwe
      | <caret>zxc
      |asd
      |rty
      |fgh
      |vbn
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSortRangeReverse() {
    val before = """qwe
      |as<caret>d
      |zxc
      |<caret>rty
      |fgh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("sor!"))

    val after = "<caret>" +
        before
            .replace("<caret>", "")
            .split('\n')
            .sortedWith(reverseOrder())
            .joinToString(separator = "\n")
    myFixture.checkResult(after)
  }

  fun testSortRangeIgnoreCase() {
    val before = """qwe
      |as<caret>d
      |   zxc
      |<caret>Rty
      |fgh
      |vbn
    """.trimMargin()
    configureByText(before)

    typeText(commandToKeys("2,4 sor i"))

    val after = """qwe
      |   <caret>zxc
      |asd
      |Rty
      |fgh
      |vbn
    """.trimMargin()
    myFixture.checkResult(after)
  }

  fun testSubstitute() {
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

  fun testSubstituteAllOccurrences() {
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
}