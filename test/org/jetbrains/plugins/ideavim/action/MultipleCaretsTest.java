package org.jetbrains.plugins.ideavim.action;

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.editor.Editor;
import org.jetbrains.plugins.ideavim.VimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author Vasily Alferov
 */
public class MultipleCaretsTest extends VimTestCase {
  // com.maddyhome.idea.vim.action.motion.leftright

  public void testMotionLeftAction() {
    typeTextInFile(parseKeys("3h"),
                   "abc<caret>de<caret>");
    myFixture.checkResult("<caret>ab<caret>cde");
  }

  public void testMotionRightAction() {
    typeTextInFile(parseKeys("l"),
                   "ab<caret>cd<caret>ef");
    myFixture.checkResult("abc<caret>de<caret>f");
  }

  public void testMovementMerging() {
    Editor editor = typeTextInFile(parseKeys("2h"),
                                   "o<caret>n<caret>e");
    assertEquals(1, editor.getCaretModel().getCaretCount());
    myFixture.checkResult("<caret>one");
  }

  public void testMotionColumnAction() {
    typeTextInFile(parseKeys("4|"),
                   "one<caret> two\n" +
                   "three four fiv<caret>e\n" +
                   "si<caret>x seven<caret>\n" +
                   "<caret>eig<caret>ht nine ten<caret>");
    myFixture.checkResult("one<caret> two\n" +
                          "thr<caret>ee four five\n" +
                          "six<caret> seven\n" +
                          "eig<caret>ht nine ten");
  }

  public void testMotionFirstColumnAction() {
    typeTextInFile(parseKeys("0"),
                   "one<caret> two\n" +
                   "three four fiv<caret>e\n" +
                   "si<caret>x seven<caret>\n" +
                   "<caret>eig<caret>ht nine te<caret>n");
    myFixture.checkResult("<caret>one two\n" +
                          "<caret>three four five\n" +
                          "<caret>six seven\n" +
                          "<caret>eight nine ten");
  }

  public void testMotionFirstNonSpaceAction() {
    typeTextInFile(parseKeys("^"),
                   "     one<caret> two\n" +
                   "three<caret> four\n" +
                   "  five<caret> six\n" +
                   " <caret>  seven eight");
    myFixture.checkResult("     <caret>one two\n" +
                          "<caret>three four\n" +
                          "  <caret>five six\n" +
                          "   <caret>seven eight");
  }

  public void testMotionLastNonSpaceAction() {
    typeTextInFile(parseKeys("g_"),
                   "one<caret> two   \n" +
                   "three<caret> four      \n" +
                   " five si<caret>x\n" +
                   "seven eight    <caret>  \n");
    myFixture.checkResult("one tw<caret>o   \n" +
                          "three fou<caret>r      \n" +
                          " five si<caret>x\n" +
                          "seven eigh<caret>t      \n");
  }

  public void testMotionLastColumnAction() {
    typeTextInFile(parseKeys("$"),
                   "one <caret>two\n" +
                   "three fou<caret>r");
    myFixture.checkResult("one tw<caret>o\n" +
                          "three fou<caret>r");
  }

  public void testMotionLeftMatchCharAction() {
    typeTextInFile(parseKeys("2Fa"),
                   "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("<caret>a<caret>a<caret>ab<caret>ab<caret>ababbx");
  }

  public void testMotionRightMatchCharAction() {
    typeTextInFile(parseKeys("2fb"),
                   "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("aaaba<caret>baba<caret>b<caret>b<caret>x");
  }

  public void testMotionLeftTillMatchCharAction() {
    typeTextInFile(parseKeys("2Ta"),
                   "b<caret>a<caret>ba<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("b<caret>a<caret>ba<caret>a<caret>a<caret>bab");
  }

  public void testMotionRightTillMatchCharAction() {
    typeTextInFile(parseKeys("2ta"),
                   "<caret>b<caret>a<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("ba<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
  }

  public void testMotionLastLeftMatchChar() {
    typeTextInFile(parseKeys("Fa;"),
                   "a<caret>a<caret>abab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("<caret>aa<caret>ab<caret>ab<caret>ababbx");
  }

  public void testMotionLastRightMatchChar() {
    typeTextInFile(parseKeys("fb;"),
                   "<caret>a<caret>aabab<caret>ab<caret>ab<caret>b<caret>x");
    myFixture.checkResult("aaaba<caret>baba<caret>b<caret>b<caret>x");
  }

  public void testMotionLastLeftTillMatchChar() {
    typeTextInFile(parseKeys("Ta,"),
                   "b<caret>aba<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("ba<caret>baa<caret>a<caret>ba<caret>b");
  }

  public void testMotionLastRightTillMatchChar() {
    typeTextInFile(parseKeys("ta;"),
                   "<caret>b<caret>a<caret>b<caret>a<caret>a<caret>a<caret>ba<caret>b");
    myFixture.checkResult("ba<caret>b<caret>a<caret>aa<caret>ba<caret>b");
  }

  public void testMotionLastMatchCharReverse() {
    typeTextInFile(parseKeys("fa", "2;", "3,"),
                   "abaab<caret>a<caret>baaa<caret>abaaba");
    myFixture.checkResult("abaab<caret>abaaa<caret>abaaba");
  }

  public void testMotionLeftWrap() {
    typeTextInFile(parseKeys("5<BS>"),
                   "one\n" +
                   "t<caret>wo three\n" +
                   "fo<caret>ur\n");
    myFixture.checkResult("<caret>one\ntwo thr<caret>ee\nfour\n");
  }

  public void testMotionRightWrap() {
    typeTextInFile(parseKeys("5<Space>"),
                   "<caret>one\n" +
                   "two thr<caret>ee\n" +
                   "four\n");
    myFixture.checkResult("one\nt<caret>wo three\nfo<caret>ur\n");
  }

  // com.maddyhome.idea.vim.action.motion.updown

  public void testMotionUpAction() {
    typeTextInFile(parseKeys("k"),
                   "o<caret>ne\n" +
                   "t<caret>wo<caret> \n" +
                   "t<caret>hree<caret> ");
    myFixture.checkResult("o<caret>n<caret>e\n" +
                          "t<caret>wo<caret> \n" +
                          "three ");
  }

  public void testMotionDownAction() {
    typeTextInFile(parseKeys("2j"),
                   "o<caret>n<caret>e\n" +
                   "<caret>tw<caret>o          <caret> \n" +
                   "three\n" +
                   "four");
    myFixture.checkResult("one\n" +
                          "two           \n" +
                          "t<caret>h<caret>ree\n" +
                          "<caret>fo<caret>u<caret>r");
  }

  public void testMotionDownFirstNonSpaceAction() {
    typeTextInFile(parseKeys("+"),
                   " <caret> on<caret>e<caret> two\n" +
                   "<caret>   three<caret> four\n" +
                   " five six\n");
    myFixture.checkResult("  one two\n" +
                          "   <caret>three four\n" +
                          " <caret>five six\n");
  }

  public void testMotionDownLess1FirstNonSpaceActionWithNoCount() {
    typeTextInFile(parseKeys("_"),
                   "     one<caret> two\n" +
                   "three<caret> four\n" +
                   "  five<caret> six\n" +
                   " <caret>  seven eight");
    myFixture.checkResult("     <caret>one two\n" +
                          "<caret>three four\n" +
                          "  <caret>five six\n" +
                          "   <caret>seven eight");
  }

  public void testMotionDownLess1FirstNonSpaceActionWithCount() {
    typeTextInFile(parseKeys("3_"),
                   "x<caret>y<caret>z\n" +
                   "  skip this <caret>line\n" +
                   "   don't skip this line\n" +
                   "    stop there\n");
    myFixture.checkResult("xyz\n" +
                          "  skip this line\n" +
                          "   <caret>don't skip this line\n" +
                          "    <caret>stop there\n");
  }

  public void testMotionUpFirstNonSpaceAction() {
    typeTextInFile(parseKeys("-"),
                   " one\n" +
                   "<caret>  tw<caret>o\n");
    myFixture.checkResult(" <caret>one\n" +
                          "  two\n");
  }

  // com.maddyhome.idea.vim.action.motion.object

  public void testMotionInnerBigWordAction() {
    typeTextInFile(parseKeys("v", "iW"),
                   "a,<caret>bc<caret>d,e f,g<caret>hi,j");
    myFixture.checkResult("<selection>a,bcd,e</selection> <selection>f,ghi,j</selection>");
  }

  public void testMotionInnerWordAction() {
    typeTextInFile(parseKeys("v", "iw"),
                   "a,<caret>bc<caret>d,e f,g<caret>hi,j");
    myFixture.checkResult("a,<selection>bcd</selection>,e f,<selection>ghi</selection>,j");
  }

  public void testMotionInnerBlockAngleAction() {
    typeTextInFile(parseKeys("v", "2i<"),
                   "<asdf<asdf<a<caret>sdf>a<caret>sdf>asdf> <asdf<as<caret>df>asdf>");
    myFixture.checkResult("<<selection>asdf<asdf<asdf>asdf>asdf</selection>> <<selection>asdf<asdf>asdf</selection>>");
  }

  public void testMotionInnerBlockBackQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i`"),
                   "`as<caret>d<caret>f`asdf `a<caret>sdf`a<caret>sdf`a<caret>sdf`");
    myFixture.checkResult("`<selection>asdf</selection>`asdf `<selection>asdf</selection>`<selection>asdf</selection>`<selection>asdf</selection>`");
  }

  public void testMotionInnerBlockBraceAction() {
    typeTextInFile(parseKeys("v", "2i{"),
                   "{asdf{asdf{a<caret>sdf}a<caret>sdf}asdf} {asdf{as<caret>df}asdf}");
    myFixture.checkResult("{<selection>asdf{asdf{asdf}asdf}asdf</selection>} {<selection>asdf{asdf}asdf</selection>}");
  }

  public void testMotionInnerBlockBracketAction() {
    typeTextInFile(parseKeys("v", "2i["),
                   "[asdf[asdf[a<caret>sdf]a<caret>sdf]asdf] [asdf[as<caret>df]asdf]");
    myFixture.checkResult("[<selection>asdf[asdf[asdf]asdf]asdf</selection>] [<selection>asdf[asdf]asdf</selection>]");
  }

  public void testMotionInnerBlockDoubleQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i\""),
                   "\"as<caret>d<caret>f\"asdf \"a<caret>sdf\"a<caret>sdf\"a<caret>sdf\"");
    myFixture.checkResult("\"<selection>asdf</selection>\"asdf \"<selection>asdf</selection>\"<selection>asdf</selection>\"<selection>asdf</selection>\"");
  }

  public void testMotionInnerBlockParenAction() {
    typeTextInFile(parseKeys("v", "2i("),
                   "(asdf(asdf(a<caret>sdf)a<caret>sdf)asdf) (asdf(as<caret>df)asdf)");
    myFixture.checkResult("(<selection>asdf(asdf(asdf)asdf)asdf</selection>) (<selection>asdf(asdf)asdf</selection>)");
  }

  public void testMotionInnerBlockSingleQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i'"),
                   "'as<caret>d<caret>f'asdf 'a<caret>sdf'a<caret>sdf'a<caret>sdf'");
    myFixture.checkResult("'<selection>asdf</selection>'asdf '<selection>asdf</selection>'<selection>asdf</selection>'<selection>asdf</selection>'");
  }

  public void testMotionInnerBlockTagAction() {
    typeTextInFile(parseKeys("v", "it"),
                   "<asdf1>qwer<asdf2>qwer<asdf3>qw<caret>er</asdf3>qw<caret>er</asdf2>qwer</asdf1>\n" +
                   "<asdf1>qwer<asdf2>qw<caret>er</asdf2>qwer</asdf1>");
    myFixture.checkResult("<asdf1>qwer<asdf2><selection>qwer<asdf3>qwer</asdf3>qwer</selection></asdf2>qwer</asdf1>\n" +
                          "<asdf1>qwer<asdf2><selection>qwer</selection></asdf2>qwer</asdf1>");
  }

  public void testMotionInnerParagraphAction() {
    typeTextInFile(parseKeys("v", "3ip"),
                   "a<caret>bcd\na<caret>bcd\n\nabcd\nabcd\n\na<caret>bcd\nabcd\n\nabcd\nabcd\n");
    myFixture.checkResult("<selection>abcd\nabcd\n\nabcd\nabcd</selection>\n\n<selection>abcd\nabcd\n\nabcd\nabcd</selection>\n");
  }

  public void testMotionInnerSentenceAction() {
    typeTextInFile(parseKeys("v", "3is"),
                   "a<caret>bcd a<caret>bcd. abcd abcd. a<caret>bcd abcd.");
    myFixture.checkResult("<selection>abcd abcd. abcd abcd.</selection><selection> abcd abcd.</selection>");
  }

  public void testMotionOuterBigWordAction() {
    typeTextInFile(parseKeys("v", "aW"),
                   " a<caret>bcd<caret>e.abcde.a<caret>bcde  a<caret>bcde.abcde\n");
    myFixture.checkResult(" <selection>abcde.abcde.abcde  </selection><selection>abcde.abcde</selection>\n");
  }

  public void testMotionOuterWordAction() {
    typeTextInFile(parseKeys("v", "aw"),
                  " a<caret>bcd<caret>e.abcde.a<caret>bcde  a<caret>bcde.abcde");
    myFixture.checkResult(" <selection>abcde</selection>.abcde.<selection>abcde  abcde</selection>.abcde");
  }

  public void testMotionOuterBlockAngleAction() {
    typeTextInFile(parseKeys("v", "2a<"),
                   "<asdf<asdf<a<caret>sdf>a<caret>sdf>asdf> <asdf<a<caret>sdf>asdf>");
    myFixture.checkResult("<selection><asdf<asdf<asdf>asdf>asdf></selection> <selection><asdf<asdf>asdf></selection>");
  }

  public void testMotionOuterBlockBackQuoteAction() {
    typeTextInFile(parseKeys("v", "a`"),
                   "`asdf`asdf`a<caret>sdf`a<caret>sdf`asdf` `asdf`a<caret>sdf`asdf`");
    myFixture.checkResult("`asdf`asdf<selection>`asdf`asdf`</selection>asdf` `asdf<selection>`asdf`</selection>asdf`");
  }

  public void testMotionOuterBraceAction() {
    typeTextInFile(parseKeys("v", "2a{"),
                   "{asdf{asdf{a<caret>sdf}a<caret>sdf}asdf} {asdf{a<caret>sdf}asdf}");
    myFixture.checkResult("<selection>{asdf{asdf{asdf}asdf}asdf}</selection> <selection>{asdf{asdf}asdf}</selection>");
  }

  public void testMotionOuterBlockBracketAction() {
    typeTextInFile(parseKeys("v", "2a["),
                   "[asdf[asdf[a<caret>sdf]a<caret>sdf]asdf] [asdf[a<caret>sdf]asdf]");
    myFixture.checkResult("<selection>[asdf[asdf[asdf]asdf]asdf]</selection> <selection>[asdf[asdf]asdf]</selection>");
  }

  public void testMotionOuterBlockDoubleQuoteAction() {
    typeTextInFile(parseKeys("v", "a\""),
                   "\"asdf\"asdf\"a<caret>sdf\"a<caret>sdf\"asdf\" \"asdf\"a<caret>sdf\"asdf\"");
    myFixture.checkResult("\"asdf\"asdf<selection>\"asdf\"asdf\"</selection>asdf\" \"asdf<selection>\"asdf\"</selection>asdf\"");
  }

  public void testMotionOuterBlockParenAction() {
    typeTextInFile(parseKeys("v", "2a("), "(asdf(asdf(a<caret>sdf)a<caret>sdf)asdf) (asdf(a<caret>sdf)asdf)");
    myFixture.checkResult("<selection>(asdf(asdf(asdf)asdf)asdf)</selection> <selection>(asdf(asdf)asdf)</selection>");
  }

  public void testMotionOuterBlockSingleQuoteAction() {
    typeTextInFile(parseKeys("v", "a'"),
                   "'asdf'asdf'a<caret>sdf'a<caret>sdf'asdf' 'asdf'a<caret>sdf'asdf'");
    myFixture.checkResult("'asdf'asdf<selection>'asdf'asdf'</selection>asdf' 'asdf<selection>'asdf'</selection>asdf'");
  }

  public void testMotionOuterBlockTagAction() {
    typeTextInFile(parseKeys("v", "at"),
                   "<asdf1>qwer<asdf2>qwer<asdf3>qw<caret>er</asdf3>qw<caret>er</asdf2>qwer</asdf1>\n" +
                   "<asdf1>qwer<asdf2>qw<caret>er</asdf2>qwer</asdf1>");
    myFixture.checkResult("<asdf1>qwer<selection><asdf2>qwer<asdf3>qwer</asdf3>qwer</asdf2></selection>qwer</asdf1>\n" +
                          "<asdf1>qwer<selection><asdf2>qwer</asdf2></selection>qwer</asdf1>");
  }

  public void testMotionOuterParagraphAction() {
    typeTextInFile(parseKeys("v", "2ap"),
                   "a<caret>sdf\n\na<caret>sdf\n\nasdf\n\n");
    myFixture.checkResult("<selection>asdf\n\nasdf\n\nasdf\n</selection>\n");
  }

  public void testMotionOuterSentenceAction() {
    typeTextInFile(parseKeys("v", "2as"),
                   "a<caret>sdf. a<caret>sdf. asdf.");
    myFixture.checkResult("<selection>asdf. asdf. asdf.</selection>");
  }

  // com.maddyhime.idea.vim.action.motion.text

  public void testMotionBigWordEndLeftAction() {
    typeTextInFile(parseKeys("gE"), "a.asdf. a<caret>sdf<caret>.a a; as<caret>df\n a<caret>sdf");
    myFixture.checkResult("a.asdf<caret>. asdf.a a<caret>; asd<caret>f\n asdf");
  }

  public void testMotionBigWordEndRightAction() {
    typeTextInFile(parseKeys("E"), "a<caret>.as<caret>df. a<caret>s<caret>df.a <caret> a; as<caret>df");
    myFixture.checkResult("a.asdf<caret>. asdf.<caret>a  a<caret>; asd<caret>f");
  }

  public void testMotionBigWordLeftAction() {
    typeTextInFile(parseKeys("B"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    myFixture.checkResult("<caret>a.asdf. <caret>asdf.a  a; <caret>asdf");
  }

  public void testMotionBigWordRightAction() {
    typeTextInFile(parseKeys("W"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    myFixture.checkResult("a.asdf. <caret>asdf.a  <caret>a; asd<caret>f");
  }

  public void testMotionWordEndLeftAction() {
    typeTextInFile(parseKeys("ge"), "a.asdf. a<caret>sdf<caret>.a a; as<caret>df\n a<caret>sdf");
    myFixture.checkResult("a.asdf<caret>. asd<caret>f.a a<caret>; asd<caret>f\n asdf");
  }

  public void testMotionWordEndRightAction() {
    typeTextInFile(parseKeys("e"), "a<caret>.as<caret>df. a<caret>s<caret>df.a <caret> a; as<caret>df");
    myFixture.checkResult("a.asd<caret>f. asd<caret>f.a  <caret>a; asd<caret>f");
  }

  public void testMotionWordLeftAction() {
    typeTextInFile(parseKeys("b"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    myFixture.checkResult("<caret>a.<caret>asdf. <caret>asdf.<caret>a  a; <caret>asdf");
  }

  public void testMotionWordRightAction() {
    typeTextInFile(parseKeys("w"), "a<caret>.as<caret>df. a<caret>sdf.a <caret> a; as<caret>df");
    myFixture.checkResult("a.<caret>asdf<caret>. asdf<caret>.a  <caret>a; asd<caret>f");
  }

  public void testMotionCamelEndLeftAction() {
    typeTextInFile(parseKeys("2]b"), "ClassName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type arg2<caret>Name) <caret>{");
    myFixture.checkResult("Clas<caret>sNam<caret>e.Metho<caret>dName(Arg1Type ar<caret>g1Name, Arg2Type ar<caret>g<caret>2Name) {");
  }

  public void testMotionCamelEndRightAction() {
    typeTextInFile(parseKeys("]w"), "Cl<caret>assName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type ar<caret>g2<caret>Name) {");
    myFixture.checkResult("Clas<caret>sName.Metho<caret>dNam<caret>e(Ar<caret>g1Type arg1Nam<caret>e, Arg2Type arg<caret>2Nam<caret>e) {");
  }

  public void testMotionCamelLeftAction() {
    typeTextInFile(parseKeys("2[b"), "ClassName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type arg2<caret>Name) <caret>{");
    myFixture.checkResult("Class<caret>Name.<caret>MethodName(Arg1Type arg<caret>1Name, Arg2Type <caret>arg<caret>2Name) {");
  }

  public void testMotionCamelRightAction() {
    typeTextInFile(parseKeys("[w"),
                   "Cl<caret>assName.M<caret>ethodN<caret>ame(<caret>Arg1Type arg1Na<caret>me, Arg2Type ar<caret>g2Name) {");
    myFixture.checkResult("Class<caret>Name.Method<caret>Name(<caret>Arg<caret>1Type arg1Name, <caret>Arg2Type arg<caret>2Name) {");
  }

  public void testMotionMethodNextEndAction() {
    myFixture.configureByText(JavaFileType.INSTANCE,
                              "public class Foo {\n" +
                              "    private static void firstMethod(int argument) {\n" +
                              "        // Do som<caret>ething...\n" +
                              "    }\n" +
                              "    <caret>private static int x<caret>;" +
                              "    private static void secondMethod(String argument) {\n" +
                              "        // Do something.<caret>..\n" +
                              "    }\n" +
                              "}");
    typeText(parseKeys("]M"));
    myFixture.checkResult("public class Foo {\n" +
                          "    private static void firstMethod(int argument) {\n" +
                          "        // Do something...\n" +
                          "    <caret>}\n" +
                          "    private static int x<caret>;" +
                          "    private static void secondMethod(String argument) {\n" +
                          "        // Do something...\n" +
                          "    <caret>}\n" +
                          "}");
  }

  public void testMotionMethodNextStartAction() {
    myFixture.configureByText(JavaFileType.INSTANCE,
                              "public class Foo {\n" +
                              " <caret>   private static void firstMethod(int argument) {\n" +
                              "        // Do som<caret>ething...\n" +
                              "    }\n" +
                              "    <caret>private static int x<caret>;" +
                              "    private static void secondMethod(String argument) {\n" +
                              "        // Do something.<caret>..\n" +
                              "    }\n" +
                              "}");
    typeText(parseKeys("]m"));
    myFixture.checkResult("public class Foo {\n" +
                          "    private static void firstMethod(int argument) <caret>{\n" +
                          "        // Do something...\n" +
                          "    }\n" +
                          "    <caret>private static int x;" +
                          "    private static void secondMethod(String argument) <caret>{\n" +
                          "        // Do something...\n" +
                          "    }\n" +
                          "}");
  }

  public void testMotionMethodPreviousEndAction() {
    myFixture.configureByText(JavaFileType.INSTANCE,
                              "public class Foo {\n" +
                              " <caret>   private static void firstMethod(int argument) {\n" +
                              "        // Do som<caret>ething...\n" +
                              "    }\n" +
                              "    <caret>private static int x<caret>;" +
                              "    private static void secondMethod(String argument) {\n" +
                              "        // Do something.<caret>..\n" +
                              "    }\n" +
                              "}");
    typeText(parseKeys("[M"));
    myFixture.checkResult("public class Foo {\n" +
                          "    private static void firstMethod(int argument) {\n" +
                          "        // Do something...\n" +
                          "    <caret>}\n" +
                          "    private static int x<caret>;" +
                          "    private static void secondMethod(String argument) {\n" +
                          "        // Do something...\n" +
                          "    }\n" +
                          "}");
  }

  public void testMotionMethodPreviousStartAction() {
    myFixture.configureByText(JavaFileType.INSTANCE,
                              "public class Foo {\n" +
                              " <caret>   private static void firstMethod(int argument) {\n" +
                              "        // Do som<caret>ething...\n" +
                              "    }\n" +
                              "    <caret>private static int x<caret>;" +
                              "    private static void secondMethod(String argument) {\n" +
                              "        // Do something.<caret>..\n" +
                              "    }\n" +
                              "}");
    typeText(parseKeys("[m"));
    myFixture.checkResult("public class Foo <caret>{\n" +
                          "    private static void firstMethod(int argument) <caret>{\n" +
                          "        // Do something...\n" +
                          "    }\n" +
                          "    <caret>private static int x;" +
                          "    private static void secondMethod(String argument) <caret>{\n" +
                          "        // Do something...\n" +
                          "    }\n" +
                          "}");
  }

  public void testMotionNthCharacterAction() {
    typeTextInFile(parseKeys("5", "go"),
                   "<caret>on<caret>e two thr<caret>ee four fiv<caret>e six seven eigh<caret>t ni<caret>ne ten");
    myFixture.checkResult("one <caret>two three four five six seven eight nine ten");
  }

  public void testMotionParagraphNextAction() {
    typeTextInFile(parseKeys("2}"),
                   "o<caret>ne\n\n<caret>two\n\nthree\nthree\n\nfour\n\nfive");
    myFixture.checkResult("one\n\ntwo\n<caret>\nthree\nthree\n<caret>\nfour\n\nfive");
  }

  public void testMotionParagraphPreviousAction() {
    typeTextInFile(parseKeys("2{"),
                   "one\n\ntwo\n\nthree\nthree\n\nfou<caret>r\n\nfi<caret>ve");
    myFixture.checkResult("one\n\ntwo\n<caret>\nthree\nthree\n<caret>\nfour\n\nfive");
  }

  public void testMotionSectionBackwardEndAction() {
    typeTextInFile(parseKeys("[]"),
                   "no<caret>t_a_brace\n" +
                   "{\n" +
                   "<caret>not_a_brace\n" +
                   "}\n" +
                   "{\n" +
                   "n<caret>ot_a_brace\n" +
                   "}\n" +
                   "not_a_<caret>brace");
    myFixture.checkResult("<caret>not_a_brace\n" +
                          "{\n" +
                          "not_a_brace\n" +
                          "<caret>}\n" +
                          "{\n" +
                          "not_a_brace\n" +
                          "<caret>}\n" +
                          "not_a_brace");
  }

  public void testMotionSectionBackwardStartAction() {
    typeTextInFile(parseKeys("[["),
                   "n<caret>ot_a_brace\n" +
                   "{\n" +
                   "not_a_<caret>brace\n" +
                   "<caret>}\n" +
                   "{\n" +
                   "not_a_b<caret>race\n" +
                   "<caret>}\n" +
                   "not_a_brace");
    myFixture.checkResult("<caret>not_a_brace\n" +
                          "<caret>{\n" +
                          "not_a_brace\n" +
                          "}\n" +
                          "<caret>{\n" +
                          "not_a_brace\n" +
                          "}\n" +
                          "not_a_brace");
  }

  public void testMotionSectionForwardEndAction() {
    typeTextInFile(parseKeys("]]"),
                   "n<caret>ot_a_brace\n" +
                   "{\n" +
                   "n<caret>ot_a_brace\n" +
                   "<caret>}\n" +
                   "{\n" +
                   "not_<caret>a_brace\n" +
                   "}\n" +
                   "not_a_brace");
    myFixture.checkResult("not_a_brace\n" +
                          "{\n" +
                          "not_a_brace\n" +
                          "<caret>}\n" +
                          "{\n" +
                          "not_a_brace\n" +
                          "<caret>}\n" +
                          "not_a_brace");
  }

  public void testMotionSectionForwardStartAction() {
    typeTextInFile(parseKeys("]["),
                   "n<caret>ot_a_brace\n" +
                   "{\n" +
                   "n<caret>ot_a_brace\n" +
                   "<caret>}\n" +
                   "{\n" +
                   "not_a_brace\n" +
                   "}\n" +
                   "not_a_brace");
    myFixture.checkResult("not_a_brace\n" +
                          "<caret>{\n" +
                          "not_a_brace\n" +
                          "}\n" +
                          "<caret>{\n" +
                          "not_a_brace\n" +
                          "}\n" +
                          "not_a_brace");
  }

  public void testMotionSentenceNextEndAction() {
    typeTextInFile(parseKeys("g)"),
                   "a<caret>sdf<caret>. a<caret>sdf. a<caret>sdf.<caret> asdf.<caret> asdf.");
    myFixture.checkResult("asdf<caret>. asdf<caret>. asdf<caret>. asdf<caret>. asdf<caret>.");
  }

  public void testMotionSentenceNextStartAction() {
    typeTextInFile(parseKeys(")"),
                   "a<caret>sdf. <caret>asdf.<caret> asdf. <caret>asdf. asdf.");
    myFixture.checkResult("asdf. <caret>asdf. <caret>asdf. asdf. <caret>asdf.");
  }

  public void testMotionSentencePreviousEndAction() {
    typeTextInFile(parseKeys("g("),
                   "asdf.<caret> a<caret>sdf<caret>. as<caret>df. asd<caret>f. <caret>asdf.");
    myFixture.checkResult("asdf<caret>. asdf<caret>. asdf<caret>. asdf<caret>. asdf.");
  }

  public void testMotionSentencePreviousStartAction() {
    typeTextInFile(parseKeys("("),
                   "asd<caret>f. <caret>as<caret>df. asdf<caret>. asdf<caret>. as<caret>df.");
    myFixture.checkResult("<caret>asdf. <caret>asdf. <caret>asdf. <caret>asdf. <caret>asdf.");
  }

  public void testMotionUnmatchedBraceCloseAction() {
    typeTextInFile(parseKeys("]}"),
                   "{{}<caret> }<caret> }<caret> {}}<caret>{}}");
    myFixture.checkResult("{{} <caret>} <caret>} {}<caret>}{<caret>}}");
  }

  public void testMotionUnmatchedBraceOpenAction() {
    typeTextInFile(parseKeys("[{"),
                   "{<caret> {{}<caret> }{<caret>}{<caret>} ");
    myFixture.checkResult("<caret>{ <caret>{{} }<caret>{}<caret>{} ");
  }


  public void testMotionUnmatchedParenCloseAction() {
    typeTextInFile(parseKeys("])"),
                   "(()<caret> )<caret> )<caret> ())<caret>())");
    myFixture.checkResult("(() <caret>) <caret>) ()<caret>)(<caret>))");
  }

  public void testMotionUnmatchedParenOpenAction() {
    typeTextInFile(parseKeys("[("),
                   "(<caret> (()<caret> )(<caret>)(<caret>) ");
    myFixture.checkResult("<caret>( <caret>(() )<caret>()<caret>() ");
  }

  // com.maddyhome.idea.vim.action.motion.visual

  public void testVisualSwapEndsAction() {
    typeTextInFile(parseKeys("v", "iw", "o"), "o<caret>ne <caret>two th<caret>ree\n");
    myFixture.checkResult("<selection><caret>one</selection> <selection><caret>two</selection> <selection><caret>three</selection>\n");
  }

  public void testVisualToggleCharacterMode() {
    typeTextInFile(parseKeys("v", "e"), "o<caret>ne <caret>two th<caret>ree");
    myFixture.checkResult("o<selection>ne</selection> <selection>two</selection> th<selection>ree</selection>");
  }

  public void testVisualToggleLineMode() {
    typeTextInFile(parseKeys("V", "2k"),
                   "one two\n" +
                   "three four\n" +
                   "fi<caret>ve six\n" +
                   "seven eight\n" +
                   "nine ten\n" +
                   "eleven twelve\n" +
                   "th<caret>irteen fourteen\n");
    myFixture.checkResult("<selection>one two\n" +
                          "three four\n" +
                          "five six</selection>\n" +
                          "seven eight\n" +
                          "<selection>nine ten\n" +
                          "eleven twelve\n" +
                          "thirteen fourteen</selection>\n");
  }

  public void testVisualModeMerging() {
    typeTextInFile(parseKeys("V", "j"),
                   "one<caret> two\n" +
                   "thr<caret>ee four\n" +
                   "five six\n");
    myFixture.checkResult("<selection>one two\n" +
                          "three four\n" +
                          "five six</selection>\n");
  }

  public void testVisualCharacterToVisualLineModeSwitch() {
    typeTextInFile(parseKeys("v", "k", "V"),
                   "one two\n" +
                   "three fo<caret>ur\n" +
                   "five six\n" +
                   "seven eight\n" +
                   "nine t<caret>en\n");
    myFixture.checkResult("<selection>one two\n" +
                          "three four</selection>\n" +
                          "five six\n" +
                          "<selection>seven eight\n" +
                          "nine ten</selection>\n");
  }

  public void testVisualLineToVisualCharacterModeSwitch() {
    typeTextInFile(parseKeys("V", "k", "v"),
                   "one two\n" +
                   "thre<caret>e four\n" +
                   "five six\n" +
                   "seven eight\n" +
                   "n<caret>ine ten\n");
    myFixture.checkResult("one <selection>two\n" +
                          "three</selection> four\n" +
                          "five six\n" +
                          "s<selection>even eight\n" +
                          "ni</selection>ne ten\n");
  }

  public void testVisualBlockDownAfterLineEndMovement() {
    typeTextInFile(parseKeys("<C-V>$j"),
                   "abc\ndef\n");
    myFixture.checkResult("<selection>abc</selection>\n" +
                          "<selection>def</selection>\n");
  }

  public void testVisualSwapEndsBlockActionInBlockMode() {
    typeTextInFile(parseKeys("<C-V>", "2l", "j", "O"),
                   "a<caret>abcc\n" +
                   "ddeff\n");
    myFixture.checkResult("a<selection><caret>abc</selection>c\n" +
                          "d<selection>de<caret>f</selection>f\n");
  }

  public void testVisualBlockMovementAfterSwapEndsBlockAction() {
    typeTextInFile(parseKeys("<C-V>", "2l", "j", "O", "k", "h", "j"),
                   "aabcc\n" +
                   "d<caret>deff\n" +
                   "gghii\n" +
                   "jjkll\n");
    myFixture.checkResult("aabcc\n" +
                          "<selection><caret>ddef</selection>f\n" +
                          "<selection>ggh<caret>i</selection>i\n" +
                          "jjkll\n");
    typeText(parseKeys("j"));
    myFixture.checkResult("aabcc\n" +
                          "ddeff\n" +
                          "<selection><caret>gghi</selection>i\n" +
                          "jjkll\n");
  }

  public void testMergingSelections() {
    typeTextInFile(parseKeys("v", "aW", "l", "h"),
      "a<caret>bcde.abcde.abcde  ab<caret>cde.abcde\n");
    myFixture.checkResult("<selection>abcde.abcde.abcde  abcde.abcde</selection>\n");

  }

  public void testVisualMotionUp() {
    typeTextInFile(parseKeys("v", "k", "k"),
                   "abcde\nabcde\nab<caret>cde\n");
    myFixture.checkResult("ab<selection>cde\nabcde\nabc</selection>de\n");
  }

  public void testVisualMotionDown() {
    typeTextInFile(parseKeys("v", "2j", "j"),
                   "ab<caret>cde\nabcde\n\nabcde\n");
    myFixture.checkResult("ab<selection>cde\nabcde\n\nabc</selection>de\n");
  }

  public void testVisualLineMotionUp() {
    typeTextInFile(parseKeys("V", "2k", "k"),
                   "abcde\nabcde\n\nab<caret>cde\nabcde\n");
    myFixture.checkResult("<selection>ab<caret>cde\nabcde\n\nabcde</selection>\nabcde\n");
  }

  public void testVisualLineMotionDown() {
    typeTextInFile(parseKeys("V", "2j", "j"),
                   "ab<caret>cde\nabcde\n\nabcde\nabcde\n");
    myFixture.checkResult("<selection>abcde\nabcde\n\nab<caret>cde</selection>\nabcde\n");
  }

  public void testVisualCharacterUpMerging() {
    typeTextInFile(parseKeys("v", "2k", "k"),
                   "abcde\nabcde\n\nabc<caret>de\nab<caret>cde\n");
    myFixture.checkResult("abc<selection><caret>de\nabcde\n\nabcde\nabc</selection>de\n");
  }

  public void testVisualCharacterDownMerging() {
    typeTextInFile(parseKeys("v", "2j", "j"),
                   "abc<caret>de\nab<caret>cde\n\nabcde\nabcde\n");
    myFixture.checkResult("abc<selection>de\nabcde\n\nabcde\nab<caret>c</selection>de\n");
  }

  public void testVisualLineUpMerging() {
    typeTextInFile(parseKeys("V", "2k", "k"),
                   "abcde\nabcde\n\nabc<caret>de\nab<caret>cde\n");
    myFixture.checkResult("<selection>abc<caret>de\nabcde\n\nabcde\nabcde</selection>\n");
  }

  public void testVisualLineDownMerging() {
    typeTextInFile(parseKeys("V", "2j", "j"),
                   "abc<caret>de\nab<caret>cde\n\nabcde\nabcde\n");
    myFixture.checkResult("<selection>abcde\nabcde\n\nabcde\nab<caret>cde</selection>\n");
  }

  // com.maddyhome.idea.vim.action.change.delete

  public void testDeleteCharacterAction() {
    typeTextInFile(parseKeys( "<Del>"),
                   "a<caret>bcde\n" +
                   "<caret>abcde\n" +
                   "abcd<caret>e\n");
    myFixture.checkResult("a<caret>cde\n" +
                          "<caret>bcde\n" +
                          "abc<caret>d\n");
  }

  public void testDeleteCharacterActionOrder() {
    typeTextInFile(parseKeys("<Del>"),
                   "ab<caret>c<caret>d<caret>e abcde\n");
    myFixture.checkResult("ab<caret> abcde\n");
  }

  public void testDeleteCharacterLeftAction() {
    typeTextInFile(parseKeys("3X"),
                   "a<caret>bcde\n" +
                   "<caret>abcde\n" +
                   "abcd<caret>e\n");
    myFixture.checkResult("<caret>bcde\n" +
                          "<caret>abcde\n" +
                          "a<caret>e\n");
  }

  public void testDeleteCharacterLeftCaretMerging() {
    typeTextInFile(parseKeys("3X"),
                   "a<caret>bc<caret>def<caret>ghij<caret>klmn<caret>op<caret>q");
    myFixture.checkResult("gq");
  }

  public void testDeleteCharacterRightAction() {
    typeTextInFile(parseKeys("3x"),
                   "a<caret>bcde\n" +
                   "<caret>abcde\n" +
                   "abcd<caret>e\n");
    myFixture.checkResult("a<caret>e\n" +
                          "<caret>de\n" +
                          "abc<caret>d\n");
  }

  public void testDeleteCharacterRightCaretMerging() {
    typeTextInFile(parseKeys("4x"),
                   "o<caret>ne <caret>two <caret>three four");
    myFixture.checkResult("o<caret> four");
  }

  public void testDeleteEndOfLineAction() {
    typeTextInFile(parseKeys("D"),
                   "abcd<caret>e\n" +
                   "abcde\n" +
                   "abc<caret>de\n" +
                   "<caret>abcde\n" +
                   "ab<caret>cde\n" +
                   "ab<caret>cd<caret>e\n");
    myFixture.checkResult("abc<caret>d\n" +
                          "abcde\n" +
                          "ab<caret>c\n" +
                          "<caret>\n" +
                          "a<caret>b\n" +
                          "a<caret>b\n");
  }

  public void testDeleteEndOfLineActionWithCount() {
    typeTextInFile(parseKeys("3D"),
                   "ab<caret>cde\n" +
                   "abcde\n" +
                   "abcde\n" +
                   "abcd<caret>e\n" +
                   "a<caret>bcd<caret>e\n" +
                   "abc<caret>de\n");
    myFixture.checkResult("ab\n" +
                          "abcd\n");
  }

  public void testDeleteJoinLinesAction() {
    typeTextInFile(parseKeys("gJ"),
                   "ab<caret>cde\n" +
                   "abcde\n" +
                   "ab<caret>cde\n" +
                   "abcd<caret>e\n" +
                   "abcde\n" +
                   "abc<caret>de\n" +
                   "  abcde\n");
    myFixture.checkResult("abcde<caret>abcde\n" +
                          "abcde<caret>abcde<caret>abcde\n" +
                          "abcde<caret>  abcde\n");
  }

  public void testDeleteJoinLinesSimpleAction() {
    typeTextInFile(parseKeys("gJ"),
                   "a<caret>bcde\n" +
                   "abcde\n");
    myFixture.checkResult("abcde<caret>abcde\n");
  }

  public void testDeleteJoinLinesSpacesAction() {
    typeTextInFile(parseKeys("J"),
                   "ab<caret>cde\n" +
                   "abcde\n" +
                   "ab<caret>cde\n" +
                   "abcd<caret>e\n" +
                   "abcde\n" +
                   "abc<caret>de\n" +
                   "  abcde\n");
    myFixture.checkResult("abcde<caret> abcde\n" +
                          "abcde<caret> abcde<caret> abcde\n" +
                          "abcde<caret> abcde\n");
  }

  public void testDeleteJoinVisualLinesAction() {
    typeTextInFile(parseKeys("VkgJ"),
                   "abcde\n" +
                   "abcd<caret>e\n" +
                   "abcde\n" +
                   "ab<caret>cde\n");
    myFixture.checkResult("<selection>abcdeabcde</selection>\n" +
                          "<selection>abcdeabcde</selection>\n");
  }

  public void testDeleteJoinVisualLinesSpacesAction() {
    typeTextInFile(parseKeys("VkJ"),
                   "abcde\n" +
                   "abcd<caret>e\n" +
                   "abcde\n" +
                   "ab<caret>cde\n");
    myFixture.checkResult("<selection>abcde abcde</selection>\n" +
                          "<selection>abcde abcde</selection>\n");
  }

  public void testDeleteLineAction() {
    typeTextInFile(parseKeys("d3d"),
                   "abc<caret>de\n" +
                   "abcde\n" +
                   "abcde\n" +
                   "abcde\n" +
                   "ab<caret>cde\n" +
                   "abcde\n" +
                   "abcde\n");
    myFixture.checkResult("<caret>abcde\n");
  }

  public void testDeleteMotionAction() {
    typeTextInFile(parseKeys("dt)"),
                   "public class Foo {\n" +
                   "  int foo(int a, int b) {\n" +
                   "    boolean bar = (a < 0 && (b < 0 || a > 0)<caret> || b != 0);\n" +
                   "    if (bar<caret> || b != 0) {\n" +
                   "      return a;\n" +
                   "    }\n" +
                   "    else {\n" +
                   "      return b;\n" +
                   "    }\n" +
                   "  }\n" +
                   "}\n");
    myFixture.checkResult("public class Foo {\n" +
                   "  int foo(int a, int b) {\n" +
                   "    boolean bar = (a < 0 && (b < 0 || a > 0)<caret>);\n" +
                   "    if (bar<caret>) {\n" +
                   "      return a;\n" +
                   "    }\n" +
                   "    else {\n" +
                   "      return b;\n" +
                   "    }\n" +
                   "  }\n" +
                   "}\n");
  }

  public void testDeleteVisualAction() {
    typeTextInFile(parseKeys("vlj"),
                   "abc<caret>de\n" +
                   "<caret>abcde\n" +
                   "abc<caret>de\n" +
                   "abcde\n");
    myFixture.checkResult("abc<selection>de\n" +
                          "abcde\n" +
                          "a<caret>b</selection>c<selection>de\n" +
                          "abcd<caret>e</selection>\n");
    typeText(parseKeys("d"));
    myFixture.checkResult("abc<caret>c\n");
  }

  public void testDeleteVisualActionWithMultipleCaretsLeft() {
    typeTextInFile(parseKeys("v", "fd", "d"),
                   "a<caret>bcde\n" +
                   "abcde\n" +
                   "<caret>abcde\n" +
                   "ab<caret>cde\n");
    myFixture.checkResult("a<caret>e\n" +
                          "abcde\n" +
                          "<caret>e\n" +
                          "ab<caret>e\n");
  }

  public void testDeleteVisualLinesAction() {
    typeTextInFile(parseKeys("Vjd"),
                   "abc<caret>de\n" +
                   "abcde\n" +
                   "abcde\n" +
                   "a<caret>bcde\n" +
                   "abcde\n");
    myFixture.checkResult("<caret>abcde\n");
  }

  public void testDeleteVisualLinesEndAction() {
    typeTextInFile(parseKeys("v", "2j", "D"),
                   "a<caret>bcde\n" +
                   "abcde\n" +
                   "abcde\n" +
                   "abcde\n" +
                   "abcd<caret>e\n" +
                   "abcde\n" +
                   "abcde\n");
    myFixture.checkResult("<caret>a\n" +
                          "abc<caret>d\n");
  }

  // com.maddyhome.idea.vim.action.change.insert

  public void testInsertEscape() {
    typeTextInFile(parseKeys("i", "<ESC>", "i", "<ESC>"),
                   "on<caret>e tw<caret>o th<caret>ree");
    myFixture.checkResult("<caret>one <caret>two <caret>three");
  }
}
