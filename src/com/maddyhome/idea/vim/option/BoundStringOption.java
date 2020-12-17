/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2020 The IdeaVim authors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.option;

import com.intellij.internal.statistic.eventLog.events.EventId2;
import com.maddyhome.idea.vim.statistic.OptionActivation;
import com.maddyhome.idea.vim.statistic.OptionsCollector;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BoundStringOption extends StringOption {
  BoundStringOption(@NonNls String name, @NonNls String abbrev, @NonNls String dflt, String[] values) {
    super(name, abbrev, dflt);

    this.values = values;
    this.statisticCollector = null;
  }

  BoundStringOption(@NonNls String name, @NonNls String abbrev, @NonNls String dflt, String[] values, @NotNull EventId2<@Nullable String, OptionActivation> statisticCollector) {
    super(name, abbrev, dflt);

    this.values = values;
    this.statisticCollector = statisticCollector;
  }

  @Override
  public boolean set(String val) {
    if (isValid(val)) {
      if (statisticCollector != null) {
        OptionActivation activation = OptionsCollector.Companion.getFileExecution()
                                      ? OptionActivation.IDEAVIMRC
                                      : OptionActivation.EX_COMMAND;
        OptionsManager.INSTANCE.getTrackedOptions().removeIf(o -> o.getName().equals(this.name));
        statisticCollector.log(val, activation);
      }
      return super.set(val);
    }

    return false;
  }

  @Override
  public boolean append(String val) {
    if (isValid(val) && getValue().length() == 0) {
      return super.set(val);
    }

    return false;
  }

  @Override
  public boolean prepend(String val) {
    if (isValid(val) && getValue().length() == 0) {
      return super.set(val);
    }

    return false;
  }

  @Override
  public boolean remove(@NotNull String val) {
    if (getValue().equals(val)) {
      return super.remove(val);
    }

    return false;
  }

  private boolean isValid(String val) {
    for (String value : values) {
      if (value.equals(val)) {
        return true;
      }
    }

    return false;
  }

  protected final String[] values;
  public final @Nullable EventId2<@Nullable String, OptionActivation> statisticCollector;
}
