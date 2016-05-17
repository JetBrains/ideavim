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
