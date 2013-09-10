package org.jetbrains.plugins.ideavim.cucumber.glue;

import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import cucumber.api.java.After;
import cucumber.api.java.Before;

import static org.jetbrains.plugins.ideavim.cucumber.glue.IdeaVimWorld.myFixture;

/**
 * User: zolotov
 * Date: 3/8/13
 */
public class Hooks {
  private static final String ULTIMATE_MARKER_CLASS = "com.intellij.psi.css.CssFile";

  @Before
  public void init() throws Exception {
    // Only in IntelliJ IDEA Ultimate Edition
    //PlatformTestCase.initPlatformLangPrefix();
    // XXX: IntelliJ IDEA Community and Ultimate 12+
    PlatformTestCase.initPlatformPrefix(ULTIMATE_MARKER_CLASS, "PlatformLangXml");
    
    final IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
    final LightProjectDescriptor projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR;
    final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor);
    final IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
                                                                                    new LightTempDirTestFixtureImpl(true));
    myFixture.setUp();
    KeyHandler.getInstance().fullReset(myFixture.getEditor());
    Options.getInstance().resetAllOptions();
  }

  @After
  public void tearDown() throws Exception {
    myFixture.tearDown();
    myFixture = null;
    ExEntryPanel.getInstance().deactivate();
  }
}
