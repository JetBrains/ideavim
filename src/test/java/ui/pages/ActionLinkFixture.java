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
import com.intellij.remoterobot.fixtures.FixtureName;
import org.jetbrains.annotations.NotNull;


@FixtureName(name = "Action Link")
public class ActionLinkFixture extends ComponentFixture {
  public ActionLinkFixture(@NotNull RemoteRobot remoteRobot, @NotNull RemoteComponent remoteComponent) {
    super(remoteRobot, remoteComponent);
  }

  public void click() {
    runJs("const offset = component.getHeight()/2;\n" +
      "robot.click(" +
      "component, " +
      "new Point(offset, offset), " +
      "MouseButton.LEFT_BUTTON, 1);");
  }
}
