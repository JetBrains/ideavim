package org.jetbrains.plugins.ideavim.extension.repeat;

import org.jetbrains.plugins.ideavim.JavaVimTestCase;

import static com.maddyhome.idea.vim.helper.StringHelper.parseKeys;

/**
 * @author dhleong
 */
public class VimRepeatExtensionTest extends JavaVimTestCase {

  public void testRepeatSurround() {
    enableExtensions("surround", "repeat");

    configureByText("ffoo\n");
    typeText(parseKeys("x")); // set some initial repeat-able

    typeText(parseKeys("ysiwb"));
    myFixture.checkResult("<caret>(foo)\n");

    typeText(parseKeys("w."));
    myFixture.checkResult("(<caret>(foo))\n");

    typeText(parseKeys("hx"));
    myFixture.checkResult("<caret>(foo))\n");

    // this repeat should do the default
    typeText(parseKeys("."));
    myFixture.checkResult("<caret>foo))\n");
  }

  public void testRepeatDeleteSurround() {
    enableExtensions("surround", "repeat");

    configureByText("(((foo))\n");
    typeText(parseKeys("x")); // set some initial repeat-able

    typeText(parseKeys("dsb"));
    myFixture.checkResult("<caret>(foo)\n");

    typeText(parseKeys("."));
    myFixture.checkResult("<caret>foo\n");

    typeText(parseKeys("x"));
    myFixture.checkResult("<caret>oo\n");

    // this repeat should do the default
    typeText(parseKeys("."));
    myFixture.checkResult("<caret>o\n");
  }

  public void testRepeatChangeSurround() {
    enableExtensions("surround", "repeat");

    configureByText("((foo) (bar)\n");
    typeText(parseKeys("x")); // set some initial repeat-able

    typeText(parseKeys("csb'"));
    myFixture.checkResult("<caret>'foo' (bar)\n");

    typeText(parseKeys("W."));
    myFixture.checkResult("'foo' <caret>'bar'\n");

    typeText(parseKeys("x"));
    myFixture.checkResult("'foo' <caret>bar'\n");

    // this repeat should do the default
    typeText(parseKeys("."));
    myFixture.checkResult("'foo' <caret>ar'\n");
  }

  //public void testRepeatChangeSurroundTag() {
  //  enableExtensions("surround", "repeat");
  //
  //  configureByText("<<a>foo</a> <i>bar</i>\n");
  //  typeText(parseKeys("x")); // set some initial repeat-able
  //
  //  typeText(parseKeys("cst<div>"));
  //  myFixture.checkResult("<caret><div>foo</div> <i>bar</i>\n");
  //
  //  typeText(parseKeys("fb."));
  //  myFixture.checkResult("<div>foo</div> <caret><div>bar</div>\n");
  //
  //  typeText(parseKeys("x"));
  //  myFixture.checkResult("div>foo</div> <caret><div>bar</div>\n");
  //
  //  // this repeat should do the default
  //  typeText(parseKeys("."));
  //  myFixture.checkResult("iv>foo</div> <caret><div>bar</div>\n");
  //}

  /*
   * Repeating comments
   */

  public void testRepeatLineComment() {
    enableExtensions("commentary", "repeat");
    configureByJavaText("ffoo\n");
    typeText(parseKeys("x")); // set some initial repeat-able

    typeText(parseKeys("gcc"));
    myFixture.checkResult("<caret>//foo\n");

    // repeat the comment
    typeText(parseKeys("."));
    myFixture.checkResult("<caret>foo\n");

    typeText(parseKeys("x"));
    myFixture.checkResult("<caret>oo\n");

    // this repeat should do the default
    typeText(parseKeys("."));
    myFixture.checkResult("<caret>o\n");
  }

  public void testRepeatMotionComment() {
    enableExtensions("commentary", "repeat");
    configureByJavaText("ffoo\n");
    typeText(parseKeys("x")); // set some initial repeat-able

    typeText(parseKeys("gcap"));
    myFixture.checkResult("<caret>//foo\n");

    // repeat the comment
    typeText(parseKeys("."));
    myFixture.checkResult("<caret>foo\n");

    typeText(parseKeys("x"));
    myFixture.checkResult("<caret>oo\n");

    // this repeat should do the default
    typeText(parseKeys("."));
    myFixture.checkResult("<caret>o\n");
  }
}
