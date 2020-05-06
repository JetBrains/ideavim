package icons;

import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public final class VimIcons {
  private static @NotNull Icon load(@NotNull String path) {
    return IconManager.getInstance().getIcon(path, VimIcons.class);
  }

  public static final @NotNull Icon IDEAVIM = load("/icons/ideavim.svg");
  public static final @NotNull Icon IDEAVIM_DISABLED = load("/icons/ideavim_disabled.svg");
  public static final @NotNull Icon GITHUB = load("/icons/github.svg");
  public static final @NotNull Icon TWITTER = load("/icons/twitter.svg");
  public static final @NotNull Icon YOUTRACK = load("/icons/youtrack.svg");
}
