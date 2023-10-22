/*
 * Copyright 2003-2023 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package ui.pages;

import com.intellij.remoterobot.RemoteRobot;
import com.intellij.remoterobot.data.RemoteComponent;
import com.intellij.remoterobot.fixtures.ComponentFixture;
import com.intellij.remoterobot.fixtures.ContainerFixture;
import com.intellij.remoterobot.fixtures.DefaultXpath;
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;

import static com.intellij.remoterobot.search.locators.Locators.byXpath;


@DefaultXpath(by = "FlatWelcomeFrame type", xpath = "//div[@class='FlatWelcomeFrame']")
@FixtureName(name = "Welcome Frame")
public class WelcomeFrameFixture extends ContainerFixture {
  public WelcomeFrameFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
    super(remoteRobot, remoteComponent);
  }

  public ActionLinkFixture createNewProjectLink() {
    return find(ActionLinkFixture.class, byXpath(
      "//div[(@accessiblename='New Project' and @class='MainButton') or (@accessiblename='New Project' and @class='JButton')]"));
  }

  public ComponentFixture importProjectLink() {
    return find(ComponentFixture.class,
      byXpath("//div[@accessiblename='Get from Version Control...' and @class='JButton']"));
  }
}
