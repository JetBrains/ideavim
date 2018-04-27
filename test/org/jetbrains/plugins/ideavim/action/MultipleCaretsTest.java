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
    myFixture.checkResult("<selection><asdf<asdf<asdf>asdf>asdf></selection> <selection><asdf<asdf>asdf></selection>");
  }

  public void testMotionInnerBlockBackQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i`"),
                   "`as<caret>d<caret>f`asdf `a<caret>sdf`a<caret>sdf`a<caret>sdf`");
    myFixture.checkResult("`<selection>asdf</selection>`asdf `<selection>asdf</selection>`<selection>asdf</selection>`<selection>asdf</selection>`");
  }

  public void testMotionInnerBlockBackQuoteActionWithCount() {
    typeTextInFile(parseKeys("v", "2i`"),
                   "`as<caret>d<caret>f`asdf `a<caret>sdf`a<caret>sdf`a<caret>sdf`");
    myFixture.checkResult("<selection>`asdf</selection>`asdf <selection>`asdf`asdf`asdf`</selection>");
  }

  public void testMotionInnerBlockBraceAction() {
    typeTextInFile(parseKeys("v", "2i{"),
                   "{asdf{asdf{a<caret>sdf}a<caret>sdf}asdf} {asdf{as<caret>df}asdf}");
    myFixture.checkResult("<selection>{asdf{asdf{asdf}asdf}asdf}</selection> <selection>{asdf{asdf}asdf}</selection>");
  }

  public void testMotionInnerBlockBracketAction() {
    typeTextInFile(parseKeys("v", "2i["),
                   "[asdf[asdf[a<caret>sdf]a<caret>sdf]asdf] [asdf[as<caret>df]asdf]");
    myFixture.checkResult("<selection>[asdf[asdf[asdf]asdf]asdf]</selection> <selection>[asdf[asdf]asdf]</selection>");
  }

  public void testMotionInnerBlockDoubleQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i\""),
                   "\"as<caret>d<caret>f\"asdf \"a<caret>sdf\"a<caret>sdf\"a<caret>sdf\"");
    myFixture.checkResult("\"<selection>asdf</selection>\"asdf \"<selection>asdf</selection>\"<selection>asdf</selection>\"<selection>asdf</selection>\"");
  }

  public void testMotionInnerBlockDoubleQuoteActionWithCount() {
    typeTextInFile(parseKeys("v", "2i\""),
                   "\"as<caret>d<caret>f\"asdf \"a<caret>sdf\"a<caret>sdf\"a<caret>sdf\"");
    myFixture.checkResult("<selection>\"asdf</selection>\"asdf <selection>\"asdf\"asdf\"asdf\"</selection>");
  }

  public void testMotionInnerBlockParenAction() {
    typeTextInFile(parseKeys("v", "2i("),
                   "(asdf(asdf(a<caret>sdf)a<caret>sdf)asdf) (asdf(as<caret>df)asdf)");
    myFixture.checkResult("<selection>(asdf(asdf(asdf)asdf)asdf)</selection> <selection>(asdf(asdf)asdf)</selection>");
  }

  public void testMotionInnerBlockSingleQuoteActionWithNoCount() {
    typeTextInFile(parseKeys("v", "i'"),
                   "'as<caret>d<caret>f'asdf 'a<caret>sdf'a<caret>sdf'a<caret>sdf'");
    myFixture.checkResult("'<selection>asdf</selection>'asdf '<selection>asdf</selection>'<selection>asdf</selection>'<selection>asdf</selection>'");
  }

  public void testMotionInnerBlockSingleQuoteActionWithCount() {
    typeTextInFile(parseKeys("v", "2i'"),
                   "'as<caret>d<caret>f'asdf 'a<caret>sdf'a<caret>sdf'a<caret>sdf'");
    myFixture.checkResult("<selection>'asdf</selection>'asdf <selection>'asdf'asdf'asdf'</selection>");
  }

  public void testMotionInnerBlockTagAction() {
    typeTextInFile(parseKeys("v", "2it"),
                   "<asdf>qwer<asdf>qwer<asdf>qw<caret>er</asdf>qw<caret>er</asdf>qwer</asdf>\n" +
                   "<asdf>qwer<asdf>qwer</asdf>qwer</asdf>");
    myFixture.checkResult("<selection><asdf>qwer<asdf>qwer<asdf>qwer</asdf>qwer</asdf>qwer</asdf></selection>\n" +
                          "<selection><asdf>qwer<asdf>qwer</asdf>qwer</asdf></selection>");
  }

  public void testMotionInnerParagraphAction() {
    typeTextInFile(parseKeys("v", "3ip"),
                   "a<caret>bcd\na<caret>bcd\n\nabcd\nabcd\n\na<caret>bcd\nabcd\n");
    myFixture.checkResult("<selection>abcd\nabcd\n\nabcd\nabcd</selection>\n\n<selection>abcd\nabcd\n</selection>");
  }

  public void testMotionInnerSentenseAction() {
    typeTextInFile(parseKeys("v", "3is"),
                   "a<caret>bcd a<caret>bcd. abcd abcd. a<caret>bcd abcd.");
    myFixture.checkResult("<selection>abcd abcd. abcd abcd</selection>. <selection>abcd abcd.</selection>");
  }

  public void testMotionOuterBigWordAction() {
    typeTextInFile(parseKeys("v", "aW"),
                   " a<caret>bcd<caret>e.abcde.a<caret>bcde  a<caret>bcde.abcde\n");
    myFixture.checkResult("<selection> abcde.abcde.abcde  abcde.abcde</selection>\n");
  }

  public void testMotionOuterWordAction() {
    typeTextInFile(parseKeys("v", "aw"),
                  " a<caret>bcd<caret>e.abcde.a<caret>bcde  a<caret>bcde.abcde");
    myFixture.checkResult("<selection> abcde</selection>.abcde.<selection>abcde  abcde</selection>.abcde");
  }

  public void testMotionOuterBlockAngleAction() {
    typeTextInFile(parseKeys("v", "2a["),
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
    typeTextInFile(parseKeys("v", "2at"),
                   "<asdf>qwer<asdf>qwer<asdf>q<caret>wer</asdf>q<caret>wer</asdf>qwer</asdf>\n" +
                   "<asdf>qwer<asdf>q<caret>wer</asdf>qwer</asdf>");
    myFixture.checkResult("<selection><asdf>qwer<asdf>qwer<asdf>qwer</asdf>qwer</asdf>qwer</asdf></selection>\n" +
                          "<selection><asdf>qwer<asdf>qwer</asdf>qwer</asdf></selection>");
  }

  public void testMotionOuterParagraphAction() {
    typeTextInFile(parseKeys("v", "2ap"),
                   "a<caret>sdf\n\na<caret>sdf\n\nasdf");
    myFixture.checkResult("<selection>asdf\n\nasdf\n\nasdf</selection>");
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

  public void testVisualExitModeAction() {
    typeTextInFile(parseKeys("<ESC>"), "<selection>abcd</selection>  <selection>efgh</selection>");
    myFixture.checkResult("abc<caret>d  efg<caret>h");
  }
}
