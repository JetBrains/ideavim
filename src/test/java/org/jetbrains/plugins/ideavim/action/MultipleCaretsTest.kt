/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */
package org.jetbrains.plugins.ideavim.action

import com.intellij.idea.TestFor
import com.intellij.openapi.application.ApplicationManager
import com.maddyhome.idea.vim.VimPlugin
import com.maddyhome.idea.vim.action.motion.search.SearchWholeWordForwardAction
import com.maddyhome.idea.vim.api.injector
import com.maddyhome.idea.vim.common.TextRange
import com.maddyhome.idea.vim.newapi.IjVimEditor
import com.maddyhome.idea.vim.newapi.vim
import com.maddyhome.idea.vim.state.mode.SelectionType
import org.jetbrains.plugins.ideavim.VimBehaviorDiffers
import org.jetbrains.plugins.ideavim.VimTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull

/**
 * @author Vasily Alferov
 */
class MultipleCaretsTest : VimTestCase() {
  // com.maddyhome.idea.vim.action.visual.leftright
  @Test
  fun testMotionLeftAction() {
    typeTextInFile(injector.parser.parseKeys("3h"), "abc${c}de$c")
    assertState("${c}ab${c}cde")
  }

  @Test
  fun testMotionRightAction() {
    typeTextInFile(injector.parser.parseKeys("l"), "ab${c}cd${c}ef")
    assertState("abc${c}de${c}f")
  }

  @Test
  fun testMovementMerging() {
    val editor = typeTextInFile(injector.parser.parseKeys("2h"), "o${c}n${c}e")
    kotlin.test.assertEquals(1, editor.caretModel.caretCount)
    assertState("${c}one")
  }

  @Test
  fun testMotionColumnAction() {
    typeTextInFile(
      injector.parser.parseKeys("4|"),
      """
     one$c two
     three four fiv${c}e
     si${c}x seven$c
     ${c}eig${c}ht nine ten$c
      """.trimIndent(),
    )
    assertState(
      """
    one$c two
    thr${c}ee four five
    six$c seven
    eig${c}ht nine ten
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionFirstColumnAction() {
    typeTextInFile(
      injector.parser.parseKeys("0"),
      """
     one$c two
     three four fiv${c}e
     si${c}x seven$c
     ${c}eig${c}ht nine te${c}n
      """.trimIndent(),
    )
    assertState(
      """
    ${c}one two
    ${c}three four five
    ${c}six seven
    ${c}eight nine ten
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionFirstNonSpaceAction() {
    typeTextInFile(
      injector.parser.parseKeys("^"),
      """     one$c two
three$c four
  five$c six
 $c  seven eight""",
    )
    assertState(
      """     ${c}one two
${c}three four
  ${c}five six
   ${c}seven eight""",
    )
  }

  @Test
  fun testMotionLastNonSpaceAction() {
    typeTextInFile(
      injector.parser.parseKeys("g_"),
      """one$c two   
three$c four      
 five si${c}x
seven eight    $c  
""",
    )
    assertState(
      """one tw${c}o   
three fou${c}r      
 five si${c}x
seven eigh${c}t      
""",
    )
  }

  @Test
  fun testMotionLastColumnAction() {
    typeTextInFile(
      injector.parser.parseKeys("$"),
      """
     one ${c}two
     three fou${c}r
      """.trimIndent(),
    )
    assertState(
      """
    one tw${c}o
    three fou${c}r
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionLeftMatchCharAction() {
    typeTextInFile(injector.parser.parseKeys("2Fa"), "a${c}a${c}abab${c}ab${c}ab${c}b${c}x")
    assertState("${c}a${c}a${c}ab${c}ab${c}ababbx")
  }

  @Test
  fun testMotionRightMatchCharAction() {
    typeTextInFile(injector.parser.parseKeys("2fb"), "a${c}a${c}abab${c}ab${c}ab${c}b${c}x")
    assertState("aaaba${c}baba${c}b${c}b${c}x")
  }

  @Test
  fun testMotionLeftTillMatchCharAction() {
    typeTextInFile(injector.parser.parseKeys("2Ta"), "b${c}a${c}ba${c}a${c}a${c}ba${c}b")
    assertState("b${c}a${c}ba${c}a${c}a${c}bab")
  }

  @Test
  fun testMotionRightTillMatchCharAction() {
    typeTextInFile(
      injector.parser.parseKeys("2ta"),
      "${c}b${c}a${c}b${c}a${c}a${c}a${c}ba${c}b",
    )
    assertState("ba${c}b${c}a${c}a${c}a${c}ba${c}b")
  }

  @Test
  fun testMotionLastLeftMatchChar() {
    typeTextInFile(injector.parser.parseKeys("Fa;"), "a${c}a${c}abab${c}ab${c}ab${c}b${c}x")
    assertState("${c}aa${c}ab${c}ab${c}ababbx")
  }

  @Test
  fun testMotionLastRightMatchChar() {
    typeTextInFile(injector.parser.parseKeys("fb;"), "${c}a${c}aabab${c}ab${c}ab${c}b${c}x")
    assertState("aaaba${c}baba${c}b${c}b${c}x")
  }

  @Test
  fun testMotionLastRightTillMatchChar() {
    typeTextInFile(
      injector.parser.parseKeys("ta;"),
      "${c}b${c}a${c}b${c}a${c}a${c}a${c}ba${c}b",
    )
    assertState("ba${c}b${c}a${c}aa${c}ba${c}b")
  }

  @Test
  fun testMotionLastMatchCharReverse() {
    typeTextInFile(injector.parser.parseKeys("fa" + "2;" + "3,"), "abaab${c}a${c}baaa${c}abaaba")
    assertState("abaab${c}abaaa${c}abaaba")
  }

  @Test
  fun testMotionLeftWrap() {
    typeTextInFile(
      injector.parser.parseKeys("5<BS>"),
      """
     one
     t${c}wo three
     fo${c}ur
     
      """.trimIndent(),
    )
    assertState("${c}one\ntwo thr${c}ee\nfour\n")
  }

  @Test
  fun testMotionRightWrap() {
    typeTextInFile(
      injector.parser.parseKeys("5<Space>"),
      """
     ${c}one
     two thr${c}ee
     four
     
      """.trimIndent(),
    )
    assertState("one\nt${c}wo three\nfo${c}ur\n")
  }

  // com.maddyhome.idea.vim.action.visual.updown
  @Test
  fun testMotionUpAction() {
    typeTextInFile(
      injector.parser.parseKeys("k"),
      """
     o${c}ne
     t${c}wo$c 
     t${c}hree$c 
      """.trimIndent(),
    )
    assertState(
      """
    o${c}n${c}e
    t${c}wo$c 
    three 
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionDownAction() {
    typeTextInFile(
      injector.parser.parseKeys("2j"),
      """
                o${c}n${c}e
                ${c}tw${c}o          $c 
                three
                four
      """.trimIndent(),
    )
    assertState(
      """
    one
    two           
    t${c}h${c}ree
    ${c}fo${c}u${c}r
      """.trimIndent(),
    )
  }

  @Test
  fun testLeftRightAndUpDownMovements() {
    typeTextInFile(
      injector.parser.parseKeys("khj"),
      """
     abcde
     ab${c}cde
     abc${c}de
     abcd${c}e
     
      """.trimIndent(),
    )
    assertState(
      """
    abcde
    a${c}bcde
    ab${c}cde
    abc${c}de
    
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionDownFirstNonSpaceAction() {
    typeTextInFile(
      injector.parser.parseKeys("+"),
      """ $c on${c}e$c two
$c   three$c four
 five six
""",
    )
    assertState(
      """  one two
   ${c}three four
 ${c}five six
""",
    )
  }

  @Test
  fun testMotionDownLess1FirstNonSpaceActionWithNoCount() {
    typeTextInFile(
      injector.parser.parseKeys("_"),
      """     one$c two
three$c four
  five$c six
 $c  seven eight""",
    )
    assertState(
      """     ${c}one two
${c}three four
  ${c}five six
   ${c}seven eight""",
    )
  }

  @Test
  fun testMotionDownLess1FirstNonSpaceActionWithCount() {
    typeTextInFile(
      injector.parser.parseKeys("3_"),
      """x${c}y${c}z
  skip this ${c}line
   don't skip this line
    stop there
""",
    )
    assertState(
      """xyz
  skip this line
   ${c}don't skip this line
    ${c}stop there
""",
    )
  }

  @Test
  fun testMotionUpFirstNonSpaceAction() {
    typeTextInFile(
      injector.parser.parseKeys("-"),
      """ one
$c  tw${c}o
""",
    )
    assertState(
      """ ${c}one
  two
""",
    )
  }

  // com.maddyhome.idea.vim.action.visual.object
  @Test
  fun testMotionInnerBigWordAction() {
    typeTextInFile(injector.parser.parseKeys("v" + "iW"), "a,${c}bc${c}d,e f,g${c}hi,j")
    assertState("<selection>a,bcd,e</selection> <selection>f,ghi,j</selection>")
  }

  @Test
  fun testMotionInnerWordAction() {
    typeTextInFile(injector.parser.parseKeys("v" + "iw"), "a,${c}bc${c}d,e f,g${c}hi,j")
    assertState("a,<selection>bcd</selection>,e f,<selection>ghi</selection>,j")
  }

  @Test
  fun testMotionInnerBlockAngleAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2i<"),
      "<asdf<asdf<a${c}sdf>a${c}sdf>asdf> <asdf<as${c}df>asdf>",
    )
    assertState("<<selection>asdf<asdf<asdf>asdf>asdf</selection>> <<selection>asdf<asdf>asdf</selection>>")
  }

  @Test
  fun testMotionInnerBlockBackQuoteActionWithNoCount() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "i`"),
      "`as${c}d${c}f`asdf `a${c}sdf`a${c}sdf`a${c}sdf`",
    )
    assertState(
      "`<selection>asdf</selection>`asdf `<selection>asdf</selection>`<selection>asdf</selection>`<selection>asdf</selection>`",
    )
  }

  @Test
  fun testMotionInnerBlockBraceAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2i{"),
      "{asdf{asdf{a${c}sdf}a${c}sdf}asdf} {asdf{as${c}df}asdf}",
    )
    assertState("{<selection>asdf{asdf{asdf}asdf}asdf</selection>} {<selection>asdf{asdf}asdf</selection>}")
  }

  @Test
  fun testMotionInnerBlockBracketAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2i["),
      "[asdf[asdf[a${c}sdf]a${c}sdf]asdf] [asdf[as${c}df]asdf]",
    )
    assertState("[<selection>asdf[asdf[asdf]asdf]asdf</selection>] [<selection>asdf[asdf]asdf</selection>]")
  }

  @Test
  fun testMotionInnerBlockDoubleQuoteActionWithNoCount() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "i\""),
      "\"as${c}d${c}f\"asdf \"a${c}sdf\"a${c}sdf\"a${c}sdf\"",
    )
    assertState(
      "\"<selection>asdf</selection>\"asdf \"<selection>asdf</selection>\"<selection>asdf</selection>\"<selection>asdf</selection>\"",
    )
  }

  @Test
  fun testMotionInnerBlockParenAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2i("),
      "(asdf(asdf(a${c}sdf)a${c}sdf)asdf) (asdf(as${c}df)asdf)",
    )
    assertState("(<selection>asdf(asdf(asdf)asdf)asdf</selection>) (<selection>asdf(asdf)asdf</selection>)")
  }

  @Test
  fun testMotionInnerBlockSingleQuoteActionWithNoCount() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "i'"),
      "'as${c}d${c}f'asdf 'a${c}sdf'a${c}sdf'a${c}sdf'",
    )
    assertState(
      "'<selection>asdf</selection>'asdf '<selection>asdf</selection>'<selection>asdf</selection>'<selection>asdf</selection>'",
    )
  }

  @Test
  fun testMotionInnerBlockTagAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "it"),
      """
                <asdf1>qwer<asdf2>qwer<asdf3>qw${c}er</asdf3>qw${c}er</asdf2>qwer</asdf1>
                <asdf1>qwer<asdf2>qw${c}er</asdf2>qwer</asdf1>
      """.trimIndent(),
    )
    assertState(
      """
    <asdf1>qwer<asdf2><selection>qwer<asdf3>qwer</asdf3>qwer</selection></asdf2>qwer</asdf1>
    <asdf1>qwer<asdf2><selection>qwer</selection></asdf2>qwer</asdf1>
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionInnerParagraphAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "3ip"),
      "a${c}bcd\na${c}bcd\n\nabcd\nabcd\n\na${c}bcd\nabcd\n\nabcd\nabcd\n",
    )
    assertState("<selection>abcd\nabcd\n\nabcd\nabcd\n</selection>\n<selection>abcd\nabcd\n\nabcd\nabcd\n</selection>")
  }

  @Test
  fun testMotionInnerSentenceAction() {
    typeTextInFile(injector.parser.parseKeys("v" + "3is"), "a${c}bcd a${c}bcd. abcd abcd. a${c}bcd abcd.")
    assertState("<selection>abcd abcd. abcd abcd.</selection><selection> abcd abcd.</selection>")
  }

  @Test
  fun testMotionOuterBigWordAction() {
    // Careful with the trailing whitespace. If there isn't any, `vaW` will select leading whitespace and the carets
    // will merge
    typeTextInFile(
      injector.parser.parseKeys("v" + "aW"),
      " a${c}bcd${c}e.abcde.a${c}bcde  a${c}bcde.abcde \n",
    )
    assertState(" <selection>abcde.abcde.abcde  </selection><selection>abcde.abcde </selection>\n")
  }

  @Test
  fun testMotionOuterWordAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "aw"),
      " a${c}bcd${c}e.abcde.a${c}bcde  a${c}bcde.abcde",
    )
    assertState(" <selection>abcde</selection>.abcde.<selection>abcde  abcde</selection>.abcde")
  }

  @Test
  fun testMotionOuterBlockAngleAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2a<"),
      "<asdf<asdf<a${c}sdf>a${c}sdf>asdf> <asdf<a${c}sdf>asdf>",
    )
    assertState("<selection><asdf<asdf<asdf>asdf>asdf></selection> <selection><asdf<asdf>asdf></selection>")
  }

  @Test
  fun testMotionOuterBlockBackQuoteAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "a`"),
      "`asdf`asdf`a${c}sdf`a${c}sdf`asdf` `asdf`a${c}sdf`asdf`",
    )
    assertState("`asdf`asdf<selection>`asdf`asdf`</selection>asdf` `asdf<selection>`asdf`</selection>asdf`")
  }

  @Test
  fun testMotionOuterBraceAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2a{"),
      "{asdf{asdf{a${c}sdf}a${c}sdf}asdf} {asdf{a${c}sdf}asdf}",
    )
    assertState("<selection>{asdf{asdf{asdf}asdf}asdf}</selection> <selection>{asdf{asdf}asdf}</selection>")
  }

  @Test
  fun testMotionOuterBlockBracketAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2a["),
      "[asdf[asdf[a${c}sdf]a${c}sdf]asdf] [asdf[a${c}sdf]asdf]",
    )
    assertState("<selection>[asdf[asdf[asdf]asdf]asdf]</selection> <selection>[asdf[asdf]asdf]</selection>")
  }

  @Test
  fun testMotionOuterBlockDoubleQuoteAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "a\""),
      "\"asdf\"asdf\"a${c}sdf\"a${c}sdf\"asdf\" \"asdf\"a${c}sdf\"asdf\"",
    )
    assertState("\"asdf\"asdf<selection>\"asdf\"asdf\"</selection>asdf\" \"asdf<selection>\"asdf\"</selection>asdf\"")
  }

  @Test
  fun testMotionOuterBlockParenAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "2a("),
      "(asdf(asdf(a${c}sdf)a${c}sdf)asdf) (asdf(a${c}sdf)asdf)",
    )
    assertState("<selection>(asdf(asdf(asdf)asdf)asdf)</selection> <selection>(asdf(asdf)asdf)</selection>")
  }

  @Test
  fun testMotionOuterBlockSingleQuoteAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "a'"),
      "'asdf'asdf'a${c}sdf'a${c}sdf'asdf' 'asdf'a${c}sdf'asdf'",
    )
    assertState("'asdf'asdf<selection>'asdf'asdf'</selection>asdf' 'asdf<selection>'asdf'</selection>asdf'")
  }

  @Test
  fun testMotionOuterBlockTagAction() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "at"),
      """
                <asdf1>qwer<asdf2>qwer<asdf3>qw${c}er</asdf3>qw${c}er</asdf2>qwer</asdf1>
                <asdf1>qwer<asdf2>qw${c}er</asdf2>qwer</asdf1>
      """.trimIndent(),
    )
    assertState(
      """
    <asdf1>qwer<selection><asdf2>qwer<asdf3>qwer</asdf3>qwer</asdf2></selection>qwer</asdf1>
    <asdf1>qwer<selection><asdf2>qwer</asdf2></selection>qwer</asdf1>
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionOuterParagraphAction() {
    typeTextInFile(injector.parser.parseKeys("v" + "2ap"), "a${c}sdf\n\na${c}sdf\n\nasdf\n\n")
    assertState("<selection>asdf\n\nasdf\n\nasdf\n\n</selection>")
  }

  @Test
  fun testMotionOuterSentenceAction() {
    typeTextInFile(injector.parser.parseKeys("v" + "2as"), "a${c}sdf. a${c}sdf. asdf.")
    assertState("<selection>asdf. asdf. asdf.</selection>")
  }

  // com.maddyhime.idea.vim.action.visual.text
  @Test
  fun testMotionBigWordEndLeftAction() {
    typeTextInFile(injector.parser.parseKeys("gE"), "a.asdf. a${c}sdf$c.a a; as${c}df\n a${c}sdf")
    assertState("a.asdf$c. asdf.a a$c; asd${c}f\n asdf")
  }

  @Test
  fun testMotionBigWordEndRightAction() {
    typeTextInFile(
      injector.parser.parseKeys("E"),
      "a$c.as${c}df. a${c}s${c}df.a $c a; as${c}df",
    )
    assertState("a.asdf$c. asdf.${c}a  a$c; asd${c}f")
  }

  @Test
  fun testMotionBigWordLeftAction() {
    typeTextInFile(injector.parser.parseKeys("B"), "a$c.as${c}df. a${c}sdf.a $c a; as${c}df")
    assertState("${c}a.asdf. ${c}asdf.a  a; ${c}asdf")
  }

  @Test
  fun testMotionBigWordRightAction() {
    typeTextInFile(injector.parser.parseKeys("W"), "a$c.as${c}df. a${c}sdf.a $c a; as${c}df")
    assertState("a.asdf. ${c}asdf.a  ${c}a; asd${c}f")
  }

  @Test
  fun testMotionWordEndLeftAction() {
    typeTextInFile(injector.parser.parseKeys("ge"), "a.asdf. a${c}sdf$c.a a; as${c}df\n a${c}sdf")
    assertState("a.asdf$c. asd${c}f.a a$c; asd${c}f\n asdf")
  }

  @Test
  fun testMotionWordEndRightAction() {
    typeTextInFile(
      injector.parser.parseKeys("e"),
      "a$c.as${c}df. a${c}s${c}df.a $c a; as${c}df",
    )
    assertState("a.asd${c}f. asd${c}f.a  ${c}a; asd${c}f")
  }

  @Test
  fun testMotionWordLeftAction() {
    typeTextInFile(injector.parser.parseKeys("b"), "a$c.as${c}df. a${c}sdf.a $c a; as${c}df")
    assertState("${c}a.${c}asdf. ${c}asdf.${c}a  a; ${c}asdf")
  }

  @Test
  fun testMotionWordRightAction() {
    typeTextInFile(injector.parser.parseKeys("w"), "a$c.as${c}df. a${c}sdf.a $c a; as${c}df")
    assertState("a.${c}asdf$c. asdf$c.a  ${c}a; asd${c}f")
  }

  @Test
  fun testMotionCamelEndLeftAction() {
    typeTextInFile(
      injector.parser.parseKeys("2]b"),
      "ClassName.M${c}ethodN${c}ame(${c}Arg1Type arg1Na${c}me, Arg2Type arg2${c}Name) $c{",
    )
    assertState(
      "Clas${c}sNam${c}e.Metho${c}dName(Arg1Type ar${c}g1Name, Arg2Type ar${c}g${c}2Name) {",
    )
  }

  @Test
  fun testMotionCamelEndRightAction() {
    typeTextInFile(
      injector.parser.parseKeys("]w"),
      "Cl${c}assName.M${c}ethodN${c}ame(${c}Arg1Type arg1Na${c}me, Arg2Type ar${c}g2${c}Name) {",
    )
    assertState(
      "Clas${c}sName.Metho${c}dNam${c}e(Ar${c}g1Type arg1Nam${c}e, Arg2Type arg${c}2Nam${c}e) {",
    )
  }

  @Test
  fun testMotionCamelLeftAction() {
    typeTextInFile(
      injector.parser.parseKeys("2[b"),
      "ClassName.M${c}ethodN${c}ame(${c}Arg1Type arg1Na${c}me, Arg2Type arg2${c}Name) $c{",
    )
    assertState("Class${c}Name.${c}MethodName(Arg1Type arg${c}1Name, Arg2Type ${c}arg${c}2Name) {")
  }

  @Test
  fun testMotionCamelRightAction() {
    typeTextInFile(
      injector.parser.parseKeys("[w"),
      "Cl${c}assName.M${c}ethodN${c}ame(${c}Arg1Type arg1Na${c}me, Arg2Type ar${c}g2Name) {",
    )
    assertState(
      "Class${c}Name.Method${c}Name(${c}Arg${c}1Type arg1Name, ${c}Arg2Type arg${c}2Name) {",
    )
  }

  @Test
  fun testMotionNthCharacterAction() {
    typeTextInFile(
      injector.parser.parseKeys("5" + "go"),
      "${c}on${c}e two thr${c}ee four fiv${c}e six seven eigh${c}t ni${c}ne ten",
    )
    assertState("one ${c}two three four five six seven eight nine ten")
  }

  @Test
  fun testMotionParagraphNextAction() {
    typeTextInFile(injector.parser.parseKeys("2}"), "o${c}ne\n\n${c}two\n\nthree\nthree\n\nfour\n\nfive")
    assertState("one\n\ntwo\n${c}\nthree\nthree\n${c}\nfour\n\nfive")
  }

  @Test
  fun testMotionParagraphPreviousAction() {
    typeTextInFile(injector.parser.parseKeys("2{"), "one\n\ntwo\n\nthree\nthree\n\nfou${c}r\n\nfi${c}ve")
    assertState("one\n\ntwo\n${c}\nthree\nthree\n${c}\nfour\n\nfive")
  }

  @Test
  fun testMotionSectionBackwardEndAction() {
    typeTextInFile(
      injector.parser.parseKeys("[]"),
      """
     no${c}t_a_brace
     {
     ${c}not_a_brace
     }
     {
     n${c}ot_a_brace
     }
     not_a_${c}brace
      """.trimIndent(),
    )
    assertState(
      """
    ${c}not_a_brace
    {
    not_a_brace
    $c}
    {
    not_a_brace
    $c}
    not_a_brace
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionSectionBackwardStartAction() {
    typeTextInFile(
      injector.parser.parseKeys("[["),
      """
     n${c}ot_a_brace
     {
     not_a_${c}brace
     $c}
     {
     not_a_b${c}race
     $c}
     not_a_brace
      """.trimIndent(),
    )
    assertState(
      """
    ${c}not_a_brace
    $c{
    not_a_brace
    }
    $c{
    not_a_brace
    }
    not_a_brace
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionSectionForwardEndAction() {
    typeTextInFile(
      injector.parser.parseKeys("]["),
      """
     n${c}ot_a_brace
     {
     n${c}ot_a_brace
     $c}
     {
     not_${c}a_brace
     }
     not_a_brace
      """.trimIndent(),
    )
    assertState(
      """
    not_a_brace
    {
    not_a_brace
    $c}
    {
    not_a_brace
    $c}
    not_a_brace
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionSectionForwardStartAction() {
    typeTextInFile(
      injector.parser.parseKeys("]]"),
      """
     n${c}ot_a_brace
     {
     n${c}ot_a_brace
     $c}
     {
     not_a_brace
     }
     not_a_brace
      """.trimIndent(),
    )
    assertState(
      """
    not_a_brace
    $c{
    not_a_brace
    }
    $c{
    not_a_brace
    }
    not_a_brace
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionSentenceNextEndAction() {
    typeTextInFile(
      injector.parser.parseKeys("g)"),
      "a${c}sdf$c. a${c}sdf. a${c}sdf.$c asdf.$c asdf.",
    )
    assertState("asdf$c. asdf$c. asdf$c. asdf$c. asdf$c.")
  }

  @Test
  fun testMotionSentenceNextStartAction() {
    typeTextInFile(injector.parser.parseKeys(")"), "a${c}sdf. ${c}asdf.$c asdf. ${c}asdf. asdf.")
    assertState("asdf. ${c}asdf. ${c}asdf. asdf. ${c}asdf.")
  }

  @Test
  fun testMotionSentencePreviousEndAction() {
    typeTextInFile(
      injector.parser.parseKeys("g("),
      "asdf.$c a${c}sdf$c. as${c}df. asd${c}f. ${c}asdf.",
    )
    assertState("asdf$c. asdf$c. asdf$c. asdf$c. asdf.")
  }

  @Test
  fun testMotionSentencePreviousStartAction() {
    typeTextInFile(
      injector.parser.parseKeys("("),
      "asd${c}f. ${c}as${c}df. asdf$c. asdf$c. as${c}df.",
    )
    assertState("${c}asdf. ${c}asdf. ${c}asdf. ${c}asdf. ${c}asdf.")
  }

  @Test
  fun testMotionUnmatchedBraceCloseAction() {
    typeTextInFile(injector.parser.parseKeys("]}"), "{{}$c }$c }$c {}}$c{}}")
    assertState("{{} $c} $c} {}$c}{$c}}")
  }

  @Test
  fun testMotionUnmatchedBraceOpenAction() {
    typeTextInFile(injector.parser.parseKeys("[{"), "{$c {{}$c }{$c}{$c} ")
    assertState("$c{ $c{{} }$c{}$c{} ")
  }

  @Test
  fun testMotionUnmatchedParenCloseAction() {
    typeTextInFile(injector.parser.parseKeys("])"), "(()$c )$c )$c ())$c())")
    assertState("(() $c) $c) ()$c)($c))")
  }

  @Test
  fun testMotionUnmatchedParenOpenAction() {
    typeTextInFile(injector.parser.parseKeys("[("), "($c (()$c )($c)($c) ")
    assertState("$c( $c(() )$c()$c() ")
  }

  // com.maddyhome.idea.vim.action.visual.visual
  @Test
  fun testVisualSwapEndsAction() {
    typeTextInFile(injector.parser.parseKeys("v" + "iw" + "o"), "o${c}ne ${c}two th${c}ree\n")
    assertState(
      "<selection>${c}one</selection> <selection>${c}two</selection> <selection>${c}three</selection>\n",
    )
  }

  @Test
  fun testVisualToggleCharacterMode() {
    typeTextInFile(injector.parser.parseKeys("v" + "e"), "o${c}ne ${c}two th${c}ree")
    assertState("o<selection>ne</selection> <selection>two</selection> th<selection>ree</selection>")
  }

  @Test
  fun testVisualToggleLineMode() {
    typeTextInFile(
      injector.parser.parseKeys("V" + "2k"),
      """
     one two
     three four
     fi${c}ve six
     seven eight
     nine ten
     eleven twelve
     th${c}irteen fourteen
     
      """.trimIndent(),
    )
    assertState(
      """
    <selection>one two
    three four
    five six
    </selection>seven eight
    <selection>nine ten
    eleven twelve
    thirteen fourteen
    </selection>
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualModeMerging() {
    typeTextInFile(
      injector.parser.parseKeys("V" + "j"),
      """
     one$c two
     thr${c}ee four
     five six
     
      """.trimIndent(),
    )
    assertState(
      """
    ${s}one two
    three four
    five six
    $se
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualCharacterToVisualLineModeSwitch() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "k" + "V"),
      """
                one two
                three fo${c}ur
                five six
                seven eight
                nine t${c}en
                
      """.trimIndent(),
    )
    assertState(
      """
    <selection>one two
    three four
    </selection>five six
    <selection>seven eight
    nine ten
    </selection>
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualLineToVisualCharacterModeSwitch() {
    typeTextInFile(
      injector.parser.parseKeys("V" + "k" + "v"),
      """
                one two
                thre${c}e four
                five six
                seven eight
                n${c}ine ten
                
      """.trimIndent(),
    )
    assertState(
      """
    one <selection>two
    three</selection> four
    five six
    s<selection>even eight
    ni</selection>ne ten
    
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualBlockDownAfterLineEndMovement() {
    typeTextInFile(injector.parser.parseKeys("<C-V>\$j"), "abc\ndef\n")
    assertState(
      """
    <selection>abc</selection>
    <selection>def</selection>
    
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualBlockDownMovementAfterShorterLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("<C-V>" + "kkjj"),
      """
     one
     
     two three
     four fi${c}ve
     
      """.trimIndent(),
    )
    assertState(
      """
    one
    
    two three
    four fi<selection>${c}v</selection>e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualBlockDownMovementWithEmptyLineInMiddle() {
    typeTextInFile(
      injector.parser.parseKeys("<C-V>" + "3k" + "j"),
      """
     one
     
     two three
     four fi${c}ve
     
      """.trimIndent(),
    )
    assertState(
      """
    one
    
    <selection>two thre</selection>e
    <selection>four fiv</selection>e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testVisualBlockDownMovementWithManyEmptyLinesInMiddle() {
    typeTextInFile(
      injector.parser.parseKeys("<C-V>" + "4kjjj"),
      """
     one
     
     
     two three
     four fi${c}ve
     
      """.trimIndent(),
    )
    assertState(
      """
    one
    
    
    two thr<selection>e</selection>e
    four fi<selection>v</selection>e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testMergingSelections() {
    // Careful of the trailing whitespace. Without it, `vaW` will select leading whitespace, and the carets will merge
    // too soon. With it, the entire line is selected, but with two carets and the `l` and `h` merge into a single one
    typeTextInFile(
      injector.parser.parseKeys("v" + "aW" + "l" + "h"),
      "a${c}bcde.abcde.abcde  ab${c}cde.abcde \n",
    )
    assertState("<selection>abcde.abcde.abcde  abcde.abcde </selection>\n")
  }

  @Test
  fun testVisualMotionUp() {
    typeTextInFile(injector.parser.parseKeys("v" + "k" + "k"), "abcde\nabcde\nab${c}cde\n")
    assertState("ab<selection>cde\nabcde\nabc</selection>de\n")
  }

  @Test
  fun testVisualMotionDown() {
    typeTextInFile(injector.parser.parseKeys("v" + "2j" + "j"), "ab${c}cde\nabcde\n\nabcde\n")
    assertState("ab<selection>cde\nabcde\n\nabc</selection>de\n")
  }

  @Test
  fun testVisualLineMotionUp() {
    typeTextInFile(injector.parser.parseKeys("V" + "2k" + "k"), "abcde\nabcde\n\nab${c}cde\nabcde\n")
    assertState("<selection>ab${c}cde\nabcde\n\nabcde\n</selection>abcde\n")
  }

  @Test
  fun testVisualLineMotionDown() {
    typeTextInFile(injector.parser.parseKeys("V" + "2j" + "j"), "ab${c}cde\nabcde\n\nabcde\nabcde\n")
    assertState("<selection>abcde\nabcde\n\nab${c}cde\n</selection>abcde\n")
  }

  @Test
  fun testVisualCharacterUpMerging() {
    typeTextInFile(injector.parser.parseKeys("v" + "2k" + "k"), "abcde\nabcde\n\nabc${c}de\nab${c}cde\n")
    assertState("abc<selection>${c}de\nabcde\n\nabcde\nabc</selection>de\n")
  }

  @Test
  fun testVisualCharacterDownMerging() {
    typeTextInFile(injector.parser.parseKeys("v" + "2j" + "j"), "abc${c}de\nab${c}cde\n\nabcde\nabcde\n")
    assertState("abc<selection>de\nabcde\n\nabcde\nab${c}c</selection>de\n")
  }

  @Test
  fun testVisualLineUpMerging() {
    typeTextInFile(injector.parser.parseKeys("V" + "2k" + "k"), "abcde\nabcde\n\nabc${c}de\nab${c}cde\n")
    assertState("<selection>abc${c}de\nabcde\n\nabcde\nabcde\n</selection>")
  }

  @Test
  fun testVisualLineDownMerging() {
    typeTextInFile(injector.parser.parseKeys("V" + "2j" + "j"), "abc${c}de\nab${c}cde\n\nabcde\nabcde\n")
    assertState("<selection>abcde\nabcde\n\nabcde\nab${c}cde\n</selection>")
  }

  @Test
  fun testChangeCaseLowerMotionAction() {
    typeTextInFile(
      injector.parser.parseKeys("gu2w"),
      "O${c}NcE thIs ${c}TEXt wIlL n${c}Ot lOoK s${c}O rIdIcuLoUs\n",
    )
    assertState("O${c}nce this ${c}text will n${c}ot look s${c}o ridiculous\n")
  }

  @Test
  fun testChangeCaseLowerVisualAction() {
    typeTextInFile(
      injector.parser.parseKeys("v2wu"),
      "O${c}NcE thIs ${c}TEXt wIlL n${c}Ot lOoK s${c}O rIdIcuLoUs\n",
    )
    assertState("O${c}nce this text will n${c}ot look s${c}o ridiculous\n")
  }

  @Test
  fun testChangeCaseToggleCharacterAction() {
    typeTextInFile(injector.parser.parseKeys("5~"), "OnE t${c}Wo ${c}ThReE$c fOuR fIvE\n")
    assertState("OnE twO Th${c}rEe$c FoUr$c fIvE\n")
  }

  @Test
  fun testChangeCaseToggleMotionAction() {
    typeTextInFile(injector.parser.parseKeys("g~e"), "${c}capitalize ${c}UNCAPITALIZE$c ${c}sTaY\n")
    assertState("${c}CAPITALIZE ${c}uncapitalize$c ${c}sTaY\n")
  }

  @Test
  fun testChangeCaseToggleVisualAction() {
    typeTextInFile(injector.parser.parseKeys("ve~"), "${c}capitalize ${c}UNCAPITALIZE\n")
    assertState("${c}CAPITALIZE ${c}uncapitalize\n")
  }

  @Test
  fun testChangeCaseUpperMotionAction() {
    typeTextInFile(
      injector.parser.parseKeys("gU2w"),
      "O${c}NcE thIs ${c}TEXt wIlL ${c}nOt lOoK ${c}sO rIdIcuLoUs\n",
    )
    assertState("O${c}NCE THIS ${c}TEXT WILL ${c}NOT LOOK ${c}SO RIDICULOUS\n")
  }

  @Test
  fun testChangeCaseUpperVisualAction() {
    typeTextInFile(
      injector.parser.parseKeys("v2wU"),
      "O${c}NcE thIs ${c}TEXt wIlL N${c}Ot lOoK S${c}O rIdIcuLoUs\n",
    )
    assertState("O${c}NCE THIS TEXT WILL N${c}OT LOOK S${c}O RIDICULOUS\n")
  }

  @Test
  fun testChangeCharacterAction() {
    typeTextInFile(injector.parser.parseKeys("rz"), "on${c}e ${c}t${c}w${c}o th${c}r${c}ee")
    assertState("on${c}z ${c}z${c}z${c}z th${c}z${c}ze")
  }

  @Test
  fun testChangeCharacterActionWithCount() {
    typeTextInFile(injector.parser.parseKeys("2rz"), "on${c}e ${c}t${c}w${c}o th${c}r${c}ee")
    assertState("on${c}zz${c}z${c}z${c}zzth${c}z${c}zz")
  }

  @Test
  fun testChangeCharactersAction() {
    typeTextInFile(injector.parser.parseKeys("4s" + "<ESC>"), "on${c}e two ${c}th${c}ee four five\n")
    assertState("o${c}no$c r five\n")
  }

  @Test
  fun testChangeEndOfLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("Cabc" + "<ESC>"),
      """
                a${c}bcde
                abcde
                a${c}bcde
                a${c}bcd${c}e
                abcde
                
      """.trimIndent(),
    )
    assertState(
      """
    aab${c}c
    abcde
    aab${c}c
    aab${c}c
    abcde
    
      """.trimIndent(),
    )
  }

  @Test
  fun testChangeLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("c2ca" + "<ESC>"),
      """
     ab${c}cde
     abcde
     abcde
     abc${c}de
     abcde
     
      """.trimIndent(),
    )
    assertState(
      """
    ${c}a
    abcde
    ${c}a
    
      """.trimIndent(),
    )
  }

  @Test
  fun testOneCaretPositionAfterChangeLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("c2c" + "<ESC>"),
      """
     abcde
     ab${c}cde
     abcde
     abcde
     
      """.trimIndent(),
    )
    assertState(
      """
    abcde
    $c
    abcde
    
      """.trimIndent(),
    )
  }

  @Test
  fun testCaretPositionAfterChangeLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("c2c" + "<ESC>"),
      """
     abcd${c}e
     abcde
     abcde
     ab${c}cde
     abcde
     abcde
     ${c}abcde
     abcde
     
      """.trimIndent(),
    )
    assertState(
      """
    $c
    abcde
    $c
    abcde
    $c
    
      """.trimIndent(),
    )
  }

  @Test
  fun testChangeMotionAction() {
    typeTextInFile(
      injector.parser.parseKeys("ciw" + "correct" + "<ESC>"),
      "correct correct wron${c}g wr${c}ong correct\n",
    )
    assertState("correct correct correc${c}t correc${c}t correct\n")
  }

  @Test
  fun testChangeNumberIncAction() {
    typeTextInFile(injector.parser.parseKeys("<C-A>"), "1${c}7${c}7 2${c}38 ${c}999\n")
    assertState("17${c}9 23${c}9 100${c}0\n")
  }

  @Test
  fun testChangeNumberDecAction() {
    typeTextInFile(injector.parser.parseKeys("<C-X>"), "1${c}8${c}1 2${c}40 ${c}1001\n")
    assertState("17${c}9 23${c}9 100${c}0\n")
  }

  @Test
  fun testChangeReplaceAction() {
    typeTextInFile(
      injector.parser.parseKeys("Rz" + "<ESC>"),
      "on${c}e ${c}t${c}w${c}o th${c}r${c}ee",
    )
    assertState("on${c}z ${c}z${c}z${c}z th${c}z${c}ze")
  }

  @Test
  fun testChangeReplaceActionWithSeveralCharacters() {
    val before = """
            ${c}qwe
            asd ${c}zxc
            qwe${c}asdzxc
    """.trimIndent()
    typeTextInFile(injector.parser.parseKeys("Rrty" + "<Esc>"), before)
    val after = """
            rt${c}y
            asd rt${c}y
            qwert${c}yzxc
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testChangeVisualCharacterAction() {
    typeTextInFile(injector.parser.parseKeys("v2lra"), "abcd${c}ffffff${c}abcde${c}aaaa\n")
    assertState("abcdaa${c}afffaa${c}adeaa${c}aa\n")
  }

  @Test
  fun testChangeVisualLinesAction() {
    typeTextInFile(
      injector.parser.parseKeys("VjS" + "abcde" + "<ESC>"),
      """
                gh${c}ijk
                ghijk
                abcde
                ghi${c}jk
                ghijk
                
      """.trimIndent(),
    )
    assertState(
      """
    abcd${c}e
    abcde
    abcd${c}e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testChangeVisualLinesEndAction() {
    typeTextInFile(
      injector.parser.parseKeys("vjC" + "abcde" + "<ESC>"),
      """
                gh${c}ijk
                ghijk
                abcde
                ghi${c}jk
                ghijk
                
      """.trimIndent(),
    )
    assertState(
      """
    abcd${c}e
    abcde
    abcd${c}e
    
      """.trimIndent(),
    )
  }

  // com.maddyhome.idea.vim.action.change.delete
  @Test
  fun testDeleteCharacterAction() {
    typeTextInFile(
      injector.parser.parseKeys("<Del>"),
      """
     a${c}bcde
     ${c}abcde
     abcd${c}e
     
      """.trimIndent(),
    )
    assertState(
      """
    a${c}cde
    ${c}bcde
    abc${c}d
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteCharacterActionOrder() {
    typeTextInFile(injector.parser.parseKeys("<Del>"), "ab${c}c${c}d${c}e abcde\n")
    assertState("ab$c abcde\n")
  }

  @Test
  fun testDeleteCharacterLeftAction() {
    typeTextInFile(
      injector.parser.parseKeys("3X"),
      """
     a${c}bcde
     ${c}abcde
     abcd${c}e
     
      """.trimIndent(),
    )
    assertState(
      """
    ${c}bcde
    ${c}abcde
    a${c}e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteCharacterLeftCaretMerging() {
    typeTextInFile(injector.parser.parseKeys("3X"), "a${c}bc${c}def${c}ghij${c}klmn${c}op${c}q")
    assertState("gq")
  }

  @Test
  fun testDeleteCharacterRightAction() {
    typeTextInFile(
      injector.parser.parseKeys("3x"),
      """
     a${c}bcde
     ${c}abcde
     abcd${c}e
     
      """.trimIndent(),
    )
    assertState(
      """
    a${c}e
    ${c}de
    abc${c}d
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteCharacterRightCaretMerging() {
    typeTextInFile(injector.parser.parseKeys("4x"), "o${c}ne ${c}two ${c}three four")
    assertState("o$c four")
  }

  @Test
  fun testDeleteEndOfLineAction() {
    typeTextInFile(
      injector.parser.parseKeys("D"),
      """
     abcd${c}e
     abcde
     abc${c}de
     ${c}abcde
     ab${c}cde
     ab${c}cd${c}e
     
      """.trimIndent(),
    )
    assertState(
      """
    abc${c}d
    abcde
    ab${c}c
    $c
    a${c}b
    a${c}b
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteEndOfLineActionWithCount() {
    typeTextInFile(
      injector.parser.parseKeys("3D"),
      """
     ab${c}cde
     abcde
     abcde
     abcd${c}e
     a${c}bcd${c}e
     abc${c}de
     
      """.trimIndent(),
    )
    assertState(
      """
    ab
    abcd
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteJoinLinesAction() {
    typeTextInFile(
      injector.parser.parseKeys("gJ"),
      """ab${c}cde
abcde
ab${c}cde
abcd${c}e
abcde
abc${c}de
  abcde
""",
    )
    assertState(
      """
    abcde${c}abcde
    abcde${c}abcde${c}abcde
    abcde$c  abcde
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteJoinLinesSimpleAction() {
    typeTextInFile(
      injector.parser.parseKeys("gJ"),
      """
     a${c}bcde
     abcde
     
      """.trimIndent(),
    )
    assertState("abcde${c}abcde\n")
  }

  @Test
  fun testDeleteJoinLinesSpacesAction() {
    typeTextInFile(
      injector.parser.parseKeys("J"),
      """ab${c}cde
abcde
ab${c}cde
abcd${c}e
abcde
abc${c}de
  abcde
""",
    )
    assertState(
      """
    abcde$c abcde
    abcde$c abcde$c abcde
    abcde$c abcde
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteJoinVisualLinesAction() {
    typeTextInFile(
      injector.parser.parseKeys("VkgJ"),
      """
     one
     tw${c}o
     three
     fo${c}ur
     
      """.trimIndent(),
    )
    assertState(
      """
    one${c}two
    three${c}four
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteJoinVisualLinesSpacesAction() {
    typeTextInFile(
      injector.parser.parseKeys("VkJ"),
      """
     abcde
     abcd${c}e
     abcde
     ab${c}cde
     
      """.trimIndent(),
    )
    assertState(
      """
    abcde$c abcde
    abcde$c abcde
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteVisualAction() {
    typeTextInFile(
      injector.parser.parseKeys("vlj"),
      """
     abc${c}de
     ${c}abcde
     abc${c}de
     abcde
     
      """.trimIndent(),
    )
    assertState(
      """
                abc<selection>de
                abcde
                a${c}b</selection>c<selection>de
                abcd${c}e</selection>
                
      """.trimIndent(),
    )
    typeText(injector.parser.parseKeys("d"))
    assertState("abc${c}c\n")
  }

  @Test
  fun testDeleteVisualActionWithMultipleCaretsLeft() {
    typeTextInFile(
      injector.parser.parseKeys("v" + "fd" + "d"),
      """
     a${c}bcde
     abcde
     ${c}abcde
     ab${c}cde
     
      """.trimIndent(),
    )
    assertState(
      """
    a${c}e
    abcde
    ${c}e
    ab${c}e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testDeleteVisualLinesAction() {
    typeTextInFile(
      injector.parser.parseKeys("Vjd"),
      """
     abc${c}de
     abcde
     abcde
     a${c}bcde
     abcde
     
      """.trimIndent(),
    )
    assertState("${c}abcde\n$c")
  }

  // com.maddyhome.idea.vim.action.change.insert
  @Test
  fun testInsertEscape() {
    typeTextInFile(injector.parser.parseKeys("i" + "<ESC>" + "i" + "<ESC>"), "on${c}e tw${c}o th${c}ree")
    assertState("${c}one ${c}two ${c}three")
  }

  @Test
  fun testInsertAfterCursorActionMovement() {
    typeTextInFile(injector.parser.parseKeys("a" + "<ESC>"), "on${c}e two th${c}ree")
    assertState("on${c}e two th${c}ree")
  }

  @Test
  fun testInsertAfterCursorAction() {
    typeTextInFile(injector.parser.parseKeys("a" + "abcd" + "<ESC>"), "on${c}e two th${c}re${c}e")
    assertState("oneabc${c}d two thrabc${c}deeabc${c}d")
  }

  @Test
  fun testInsertBeforeCursorAction() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "four" + "<ESC>"),
      """
     one two three $c 
     seven six five $c 
     
      """.trimIndent(),
    )
    assertState(
      """
    one two three fou${c}r 
    seven six five fou${c}r 
    
      """.trimIndent(),
    )
  }

  @Test
  fun testInsertBeforeFirstNonBlankAction() {
    typeTextInFile(
      injector.parser.parseKeys("I" + "four " + "<ESC>"),
      """  three t${c}wo on${c}e
$c five six se${c}ven eight
""",
    )
    assertState(
      """  four$c three two one
 four$c five six seven eight
""",
    )
  }

  @Test
  fun testInsertCharacterAboveCursorAction() {
    typeTextInFile(
      injector.parser.parseKeys("a" + "<C-Y>" + "<C-Y>" + "<C-Y>" + "<ESC>"),
      """ one two three four
$c  two three four
four three two one
four three two$c 
""",
    )
    assertState(
      """ one two three four
 on${c}e two three four
four three two one
four three two on${c}e
""",
    )
  }

  @Test
  fun testInsertCharacterBelowCursorAction() {
    typeTextInFile(
      injector.parser.parseKeys("a" + "<C-E>" + "<C-E>" + "<C-E>" + "<ESC>"),
      """$c  two three four
 one two three four
four three two$c 
four three two one
""",
    )
    assertState(
      """ on${c}e two three four
 one two three four
four three two on${c}e
four three two one
""",
    )
  }

  @Test
  fun testInsertDeleteInsertedTextAction() {
    typeTextInFile(injector.parser.parseKeys("a" + "asdf" + "<C-U>" + "<ESC>"), "on${c}e two th${c}ree")
    assertState("on${c}e two th${c}ree")
  }

  @Test
  fun testInsertEnterAction() {
    typeTextInFile(injector.parser.parseKeys("i" + "<C-M>" + "<ESC>"), "one${c}two${c}three${c}four\n")
    assertState(
      """
    one
    ${c}two
    ${c}three
    ${c}four
    
      """.trimIndent(),
    )
  }

  @Test
  fun testInsertLineStartAction() {
    typeTextInFile(
      injector.parser.parseKeys("gI" + "four " + "<ESC>"),
      """  three t${c}wo on${c}e
$c five six se${c}ven eight
""",
    )
    assertState(
      """
    four$c   three two one
    four$c  five six seven eight
    
      """.trimIndent(),
    )
  }

  @Test
  fun testInsertNewLineAboveAction() {
    typeTextInFile(
      injector.parser.parseKeys("O" + "abcde" + "<ESC>"),
      """
                ab${c}cde
                ab${c}cde
                abcde
                abc${c}de
                
      """.trimIndent(),
    )
    assertState(
      """
    abcd${c}e
    abcde
    abcd${c}e
    abcde
    abcde
    abcd${c}e
    abcde
    
      """.trimIndent(),
    )
  }

  @VimBehaviorDiffers(originalVimAfter = "${c}\n${c}\nabcde\n${c}\n${c}\nabcde\n")
  @Test
  fun testInsertNewLineAboveActionWithMultipleCaretsInLine() {
    typeTextInFile(
      injector.parser.parseKeys("O" + "<ESC>"),
      """
     a${c}bcd${c}e
     abc${c}d${c}e
     
      """.trimIndent(),
    )
    assertState("${c}\nabcde\n${c}\nabcde\n")
  }

  @Test
  fun testInsertNewLineBelowAction() {
    typeTextInFile(
      injector.parser.parseKeys("o" + "abcde" + "<ESC>"),
      """
                ab${c}cde
                ab${c}cde
                abcde
                abc${c}de
                
      """.trimIndent(),
    )
    assertState(
      """
    abcde
    abcd${c}e
    abcde
    abcd${c}e
    abcde
    abcde
    abcd${c}e
    
      """.trimIndent(),
    )
  }

  @Test
  fun testInsertSingleCommandAction() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "<C-O>" + "2h" + "<ESC>"),
      "one ${c}two ${c}three ${c}four\n",
    )
    assertState("o${c}ne t${c}wo thr${c}ee four\n")
  }

  // com.maddyhome.idea.vim.action.change.shift
  @Test
  fun testShiftLeftLinesAction() {
    typeTextInFile(
      injector.parser.parseKeys("2<<"),
      """        ${c}abcde
        abcde
    abcde
    ${c}abcde
    abcde
""",
    )
    assertState(
      """    ${c}abcde
    abcde
    abcde
${c}abcde
abcde
""",
    )
  }

  @Test
  fun testShiftLeftMotionAction() {
    typeTextInFile(
      injector.parser.parseKeys("<j"),
      """        ${c}abcde
        abcde
    abcde
    ${c}abcde
    abcde
""",
    )
    assertState(
      """    ${c}abcde
    abcde
    abcde
${c}abcde
abcde
""",
    )
  }

  @Test
  fun testShiftLeftVisualAction() {
    typeTextInFile(
      injector.parser.parseKeys("Vj<"),
      """        ${c}abcde
        abcde
    abcde
    ${c}abcde
    abcde
""",
    )
    assertState(
      """    ${c}abcde
    abcde
    abcde
${c}abcde
abcde
""",
    )
  }

  @Test
  fun testShiftRightLinesAction() {
    typeTextInFile(
      injector.parser.parseKeys("2>>"),
      """    ${c}abcde
    abcde
    abcde
${c}abcde
abcde
""",
    )
    assertState(
      """        ${c}abcde
        abcde
    abcde
    ${c}abcde
    abcde
""",
    )
  }

  @Test
  fun testShiftRightMotionAction() {
    typeTextInFile(
      injector.parser.parseKeys(">j"),
      """    ${c}abcde
    abcde
    abcde
${c}abcde
abcde
""",
    )
    assertState(
      """        ${c}abcde
        abcde
    abcde
    ${c}abcde
    abcde
""",
    )
  }

  @Test
  fun testShiftRightVisualAction() {
    typeTextInFile(
      injector.parser.parseKeys("Vj>"),
      """    ${c}abcde
    abcde
    abcde
${c}abcde
abcde
""",
    )
    assertState(
      """        ${c}abcde
        abcde
    abcde
    ${c}abcde
    abcde
""",
    )
  }

  @Test
  fun testMotionGoToLineFirst() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "<C-Home>"),
      """    sdfdsfa${c}dsf fg dsfg sd${c}fjgkfdgl jsdfnflgj sd
 dflgj dfdsfg
 dfsgj sdf${c}klgj""",
    )
    assertState(
      """$c    sdfdsfadsf fg dsfg sdfjgkfdgl jsdfnflgj sd
 dflgj dfdsfg
 dfsgj sdfklgj""",
    )
  }

  @Test
  fun testMotionGotoLineLastEnd() {
    typeTextInFile(
      injector.parser.parseKeys("<C-End>"),
      """    sdfdsfa${c}dsf fg dsfg sd${c}fjgkfdgl jsdf
nflgj sd
 dflgj dfdsfg
 hdfsgj sdf${c}klgj
""",
    )
    assertState(
      """    sdfdsfadsf fg dsfg sdfjgkfdgl jsdf
nflgj sd
 dflgj dfdsfg
 hdfsgj sdfklgj
$c""",
    )
  }

  @Test
  fun testMotionGotoLineLastEndInsertMode() {
    typeTextInFile(
      injector.parser.parseKeys("i" + "<C-End>"),
      """    sdfdsfa${c}dsf fg dsfg sd${c}fjgkfdgl jsdf
nflgj sd
 dflgj dfdsfg
 hdfsgj sdf${c}klgj
""",
    )
    assertState(
      """    sdfdsfadsf fg dsfg sdfjgkfdgl jsdf
nflgj sd
 dflgj dfdsfg
 hdfsgj sdfklgj
$c""",
    )
  }

  @TestFor(classes = [SearchWholeWordForwardAction::class])
  @Test
  fun testSearchWholeWordForwardAction() {
    typeTextInFile(
      injector.parser.parseKeys("2*"),
      "q${c}we as${c}d zxc qwe asd zxc qwe asd zxc qwe asd zxc qwe asd zxc ",
    )
    assertState("qwe asd zxc qwe asd zxc ${c}qwe ${c}asd zxc qwe asd zxc qwe asd zxc ")
  }

  @Test
  fun testSearchWholeWordBackwardAction() {
    typeTextInFile(
      injector.parser.parseKeys("2#"),
      "qwe asd zxc qwe asd zxc ${c}qwe ${c}asd zxc qwe asd zxc qwe asd zxc ",
    )
    assertState("${c}qwe ${c}asd zxc qwe asd zxc qwe asd zxc qwe asd zxc qwe asd zxc ")
  }

  @Test
  fun testMotionPercentOrMatchAction() {
    typeTextInFile(
      injector.parser.parseKeys("%"),
      "fdgkh${c}sjh thsth[ sd${c}k er{}gha re${c}ghrjae (ghoefgh kjfgh)sdgfh dgfh]",
    )
    assertState("fdgkhsjh thsth[ sdk er{$c}gha reghrjae (ghoefgh kjfgh$c)sdgfh dgfh$c]")
  }

  @Test
  fun testMotionGotoLineLastAction() {
    typeTextInFile(
      injector.parser.parseKeys("G"),
      """
     dfgdfsg${c}gfdfgdfs dasgdfsk dfghsdfkj gh
     lsdjf lsj$c flk gjdlsadlsfj ${c}lksdgfj 
     dflgjdfsgk${c}d${c}flgjdfsklg
     
     
      """.trimIndent(),
    )
    assertState(
      """
    dfgdfsggfdfgdfs dasgdfsk dfghsdfkj gh
    lsdjf lsj flk gjdlsadlsfj lksdgfj 
    dflgjdfsgkdflgjdfsklg
    
    $c
      """.trimIndent(),
    )
  }

  @Test
  fun testMotionGotoLineLastWithArgumentAction() {
    typeTextInFile(
      injector.parser.parseKeys("1G"),
      """
     dfgdfsg${c}gfdfgdfs dasgdfsk dfghsdfkj gh
     lsdjf lsj$c flk gjdlsadlsfj ${c}lksdgfj 
     dflgjdfsgk${c}d${c}flgjdfsklg
     
     
      """.trimIndent(),
    )
    assertState(
      """
    ${c}dfgdfsggfdfgdfs dasgdfsk dfghsdfkj gh
    lsdjf lsj flk gjdlsadlsfj lksdgfj 
    dflgjdfsgkdflgjdfsklg
    
    
      """.trimIndent(),
    )
  }

  @Test
  fun testInsertAtPreviousInsert() {
    val before = """qw${c}e
  a${c}s${c}d
zx${c}c"""
    typeTextInFile(injector.parser.parseKeys("I" + "rty" + "<Esc>" + "2lj" + "gi" + "fgh" + "<Esc>"), before)
    val after = """rtyqwe
  rtyasd
rtyfg${c}hzxc"""
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursor() {
    val before = "${c}qwe asd ${c}zxc rty ${c}fgh vbn"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "fgh", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("\"*P" + "3l" + "\"*P"))
    val after = "fghqwfg${c}he asd fghzxfg${c}hc rty fghfgfg${c}hh vbn"
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorOverlapRange() {
    val before = "${c}q${c}we asd zxc rty ${c}fgh vbn"
    val editor = configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "fgh")
    ApplicationManager.getApplication().invokeAndWait {
      ApplicationManager.getApplication().runWriteAction {
        VimPlugin.getRegister()
          .storeText(
            IjVimEditor(editor),
            context,
            editor.vim.primaryCaret(),
            TextRange(16, 19),
            SelectionType.CHARACTER_WISE,
            false
          )
      }
    }
    typeText(injector.parser.parseKeys("\"*P"))
    val after = "fg${c}hqfg${c}hwe asd zxc rty fg${c}hfgh vbn"
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursor() {
    val before = "${c}qwe asd ${c}zxc rty ${c}fgh vbn"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "fgh", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("\"*p" + "3l" + "2\"*p"))
    val after = "qfghwe fghfg${c}hasd zfghxc fghfg${c}hrty ffghgh fghfg${c}hvbn"
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursorOverlapRange() {
    val before = "${c}q${c}we asd zxc rty ${c}fgh vbn"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "fgh", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("2\"*p"))
    val after = "qfghfg${c}hwfghfg${c}he asd zxc rty ffghfg${c}hgh vbn"
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorLinewise() {
    val before = """
            q${c}werty
            as${c}dfgh
            ${c}zxcvbn
            
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "zxcvbn\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*P"))
    val after = """
            ${c}zxcvbn
            qwerty
            ${c}zxcvbn
            asdfgh
            ${c}zxcvbn
            zxcvbn
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorLinewiseOverlapRange() {
    // Non-ide insert will produce double "${c}zxcvbn\n"
    testPutOverlapLine(
      """
    q${c}we${c}rty
    asdfgh
    ${c}zxcvbn
    
      """.trimIndent(),
      """
            ${c}zxcvbn
            ${c}zxcvbn
            qwerty
            asdfgh
            ${c}zxcvbn
            zxcvbn
            
      """.trimIndent(),
      true,
    )
    testPutOverlapLine(
      """
    qwerty
    a${c}sd${c}fgh
    ${c}zxcvbn
    
      """.trimIndent(),
      """
                qwerty
                ${c}zxcvbn
                ${c}zxcvbn
                asdfgh
                ${c}zxcvbn
                zxcvbn
                
      """.trimIndent(),
      true,
    )
    testPutOverlapLine(
      """
    qwerty
    asd${c}fgh
    ${c}zxcvb${c}n
    
      """.trimIndent(),
      """
                qwerty
                ${c}zxcvbn
                asdfgh
                ${c}zxcvbn
                ${c}zxcvbn
                zxcvbn
                
      """.trimIndent(),
      true,
    )
  }

  @Test
  fun testPutTextAfterCursorLinewiseOverlapRange() {
    // Non-ide insert will produce double "${c}zxcvbn\n"
    testPutOverlapLine(
      """
    q${c}wert${c}y
    asdfgh
    ${c}zxcvbn
    
      """.trimIndent(),
      """
            qwerty
            ${c}zxcvbn
            ${c}zxcvbn
            asdfgh
            zxcvbn
            ${c}zxcvbn
            
      """.trimIndent(),
      false,
    )
    testPutOverlapLine(
      """
    qwerty
    as${c}dfg${c}h
    ${c}zxcvbn
    
      """.trimIndent(),
      """
                qwerty
                asdfgh
                ${c}zxcvbn
                ${c}zxcvbn
                zxcvbn
                ${c}zxcvbn
                
      """.trimIndent(),
      false,
    )
    testPutOverlapLine(
      """
    qwerty
    asdfg${c}h
    ${c}zxcv${c}bn
    
      """.trimIndent(),
      """
                qwerty
                asdfgh
                ${c}zxcvbn
                zxcvbn
                ${c}zxcvbn
                ${c}zxcvbn
                
      """.trimIndent(),
      false,
    )
  }

  private fun testPutOverlapLine(before: String, after: String, beforeCursor: Boolean) {
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "zxcvbn\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*" + if (beforeCursor) "P" else "p"))
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursorLinewise() {
    val before = """
            q${c}werty
            as${c}dfgh
            ${c}zxcvbn
            
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "zxcvbn", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*p"))
    val after = """
            qwerty
            ${c}zxcvbn
            asdfgh
            ${c}zxcvbn
            zxcvbn
            ${c}zxcvbn
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorMoveCursor() {
    val before = "qw${c}e asd z${c}xc rty ${c}fgh vbn"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "fgh", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("l" + "\"*gP" + "b" + "\"*gP"))
    val after = "fgh${c}qwefgh asd fgh${c}zxfghc rty fgh${c}ffghgh vbn"
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursorMoveCursor() {
    val before = "qw${c}e asd z${c}xc rty ${c}fgh vbn"
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "fgh", SelectionType.CHARACTER_WISE)
    typeText(injector.parser.parseKeys("l" + "\"*gp" + "b" + "\"*gp"))
    val after = "qwe ffgh${c}ghasd zfgh${c}xcfgh rty ffgh${c}gfghh vbn"
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorMoveCursorLinewise() {
    val before = """
            qwert${c}y
            ${c}asdfgh
            zxc${c}vbn
            
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "zxcvbn\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*gP"))
    val after = """
            zxcvbn
            ${c}qwerty
            zxcvbn
            ${c}asdfgh
            zxcvbn
            ${c}zxcvbn
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursorMoveCursorLinewise() {
    val before = """
            qwert${c}y
            ${c}asdfgh
            zxc${c}vbn
            
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "zxcvbn", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*gp"))
    val after = """
            qwerty
            zxcvbn
            ${c}asdfgh
            zxcvbn
            ${c}zxcvbn
            zxcvbn
            $c
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testPutTextBeforeCursorBlockwise() {
    val before = """ *$c on${c}e
 * two
"""
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', " *\n *\n", SelectionType.BLOCK_WISE)
    typeText(injector.parser.parseKeys("\"*p"))
    val after = """ * $c *one$c *
 *  *two *
"""
    assertState(after)
  }

  @Test
  fun testPutTextAfterCursorBlockwise() {
    val before = """ *$c on${c}e
 * two
"""
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', " *\n \n", SelectionType.BLOCK_WISE)
    typeText(injector.parser.parseKeys("\"*P"))
    val after = """ *$c * on$c *e
 *   tw  o
"""
    assertState(after)
  }

  @Test
  fun testPutLinewiseWithoutLineSeparatorAtTheEndOfFile() {
    val before = """
      qwe
      asd
      z${c}xc
      rty
      fg${c}h
      vb${c}n
    """.trimIndent()
    configureByText(before)
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    injector.registerGroup.storeText(vimEditor, context, '*', "qwe\n", SelectionType.LINE_WISE)
    typeText(injector.parser.parseKeys("\"*p"))
    val after = """
      qwe
      asd
      zxc
      ${c}qwe
      rty
      fgh
      ${c}qwe
      vbn
      ${c}qwe
      
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testYankMotion() {
    val before = "qwe ${c}asd ${c}zxc"
    configureByText(before)
    typeText(injector.parser.parseKeys("ye"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val lastRegister = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)
    assertNotNull<Any>(lastRegister)
    val text = lastRegister.text
    assertNotNull<Any>(text)
    typeText(injector.parser.parseKeys("P"))
    val after = "qwe as${c}dasd zx${c}czxc"
    assertState(after)
  }

  @Test
  fun testYankMotionLineWise() {
    val before = """
            ${c}qwe
            rty
            asd
            ${c}fgh
            zxc
            vbn
            
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("yj"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val lastRegister = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)
    assertNotNull<Any>(lastRegister)
    val text = lastRegister.text
    assertNotNull<Any>(text)
    typeText(injector.parser.parseKeys("P"))
    val after = """
            ${c}qwe
            rty
            qwe
            rty
            asd
            ${c}fgh
            zxc
            fgh
            zxc
            vbn
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun testYankLine() {
    val before = """
            ${c}qwe
            asd
            zxc
            ${c}rty
            fgh
            vbn
            
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("2yy"))
    val vimEditor = fixture.editor.vim
    val context = injector.executionContextManager.getEditorExecutionContext(vimEditor)
    val registerService = injector.registerGroup
    val lastRegister = registerService.getRegister(vimEditor, context, registerService.lastRegisterChar)
    assertNotNull<Any>(lastRegister)
    val text = lastRegister.text
    assertNotNull<Any>(text)
    typeText(injector.parser.parseKeys("j" + "p"))
    val after = """
            qwe
            asd
            ${c}qwe
            asd
            zxc
            rty
            fgh
            ${c}rty
            fgh
            vbn
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `test multicaret with change`() {
    val before = """
            ${c}qwe
            asd
            zxc
            ${c}rty
            fgh
            vbn
            
    """.trimIndent()
    configureByText(before)
    typeText(injector.parser.parseKeys("cwblabla<Esc>p"))
    val after = """
            blablaqw${c}e
            asd
            zxc
            blablart${c}y
            fgh
            vbn
            
    """.trimIndent()
    assertState(after)
  }

  // VIM-2703
  @Test
  fun `test multicaret with unnamed clipboard`() {
    val before = """
            attach${c}Download(null)
            attach${c}Download(null)
            attach${c}Download(null)
            attach${c}Download(null)
            attach${c}Download(null)
            
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard+=unnamed")
    typeText(injector.parser.parseKeys("diw"))
    val after = """
            $c(null)
            $c(null)
            $c(null)
            $c(null)
            $c(null)
            
    """.trimIndent()
    assertState(after)
  }

  // VIM-2804
  @Test
  fun `test multicaret with unnamed clipboard2`() {
    val before = """
            attachDownload(${c}0)
            attachDownload(${c}1)
            attachDownload(${c}2)
            attachDownload(${c}3)
            attachDownload(${c}4)
            
    """.trimIndent()
    configureByText(before)
    enterCommand("set clipboard+=unnamed")
    typeText(injector.parser.parseKeys("yi(" + "A // <Esc>" + "p"))
    val after = """
            attachDownload(0) // ${c}0
            attachDownload(1) // ${c}1
            attachDownload(2) // ${c}2
            attachDownload(3) // ${c}3
            attachDownload(4) // ${c}4
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  fun `storing text to black hole register`() {
    val before = """
            attachD${c}ownload(0)
            attachD${c}ownload(1)
            attachD${c}ownload(2)
            attachD${c}ownload(3)
            attachD${c}ownload(4)
            
    """.trimIndent()
    configureByText(before)
    enterCommand("nnoremap c \"_c")
    typeText("ciw")
    val after = """
            (0)
            (1)
            (2)
            (3)
            (4)
            
    """.trimIndent()
    assertState(after)
  }

  @Test
  @TestFor(issues = ["VIM-2818"])
  fun `carets position after paste`() {
    val before = """
            ${c}word 0
            ${c}word 1
            ${c}word 2
            ${c}word 3
            ${c}word 4
    """.trimIndent()
    configureByText(before)
    typeText("vey\$p")
    val after = """
            word 0wor${c}d
            word 1wor${c}d
            word 2wor${c}d
            word 3wor${c}d
            word 4wor${c}d
    """.trimIndent()
    assertState(after)
  }
}
