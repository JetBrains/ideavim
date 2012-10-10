package org.jetbrains.plugins.ideavim;

import com.intellij.openapi.application.PathManager;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestCase;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CodeInsightTestFixture;
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture;
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory;
import com.intellij.testFramework.fixtures.TestFixtureBuilder;
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author vlan
 */
public abstract class VimTestCase extends UsefulTestCase {
  protected CodeInsightTestFixture myFixture;

  public VimTestCase() {
    PlatformTestCase.initPlatformLangPrefix();
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    final IdeaTestFixtureFactory factory = IdeaTestFixtureFactory.getFixtureFactory();
    final LightProjectDescriptor projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR;
    final TestFixtureBuilder<IdeaProjectTestFixture> fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor);
    final IdeaProjectTestFixture fixture = fixtureBuilder.getFixture();
    myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
                                                                                    new LightTempDirTestFixtureImpl(true));
    myFixture.setUp();
    myFixture.setTestDataPath(getTestDataPath());
  }

  protected String getTestDataPath() {
    return PathManager.getHomePath() + "/community/plugins/ideavim/testData";
  }

  @Override
  protected void tearDown() throws Exception {
    myFixture.tearDown();
    myFixture = null;
    super.tearDown();
  }

  protected static List<KeyStroke> toKeyStrokes(String input) {
    final List<KeyStroke> results = new ArrayList<KeyStroke>();
    for (int i = 0; i < input.length(); i++) {
      results.add(KeyStroke.getKeyStroke(input.charAt(i)));
    }
    return results;
  }
}
