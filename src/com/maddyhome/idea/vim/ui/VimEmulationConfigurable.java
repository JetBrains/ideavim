/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2014 The IdeaVim authors
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.StripeTable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import java.awt.*;

/**
 * @author vlan
 */
public class VimEmulationConfigurable implements Configurable {
  @NotNull private final VimSettingsPanel myPanel = new VimSettingsPanel();

  @Nls
  @Override
  public String getDisplayName() {
    return "Vim Emulation";
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return null;
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
  }

  @Override
  public void reset() {
  }

  @Override
  public void disposeUIResources() {
  }

  private static final class VimSettingsPanel extends JPanel {
    @NotNull private final VimShortcutConflictsTable myShortcutConflictsTable = new VimShortcutConflictsTable();

    public VimSettingsPanel() {
      setLayout(new BorderLayout());
      final JScrollPane scrollPane = new JBScrollPane(myShortcutConflictsTable);
      scrollPane.setBorder(new LineBorder(UIUtil.getBorderColor()));
      final JPanel conflictsPanel = new JPanel(new BorderLayout());
      conflictsPanel.setBorder(IdeBorderFactory.createTitledBorder("Shortcut Conflicts", false));
      conflictsPanel.add(scrollPane);
      add(conflictsPanel, BorderLayout.CENTER);
    }
  }

  private static final class VimShortcutConflictsTable extends StripeTable {
    public VimShortcutConflictsTable() {
      super(new VimShortcutConflictsTableModel());
    }
  }

  private static final class VimShortcutConflictsTableModel extends AbstractTableModel {
    @Override
    public int getRowCount() {
      return 0;
    }

    @Override
    public int getColumnCount() {
      return 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      return null;
    }
  }
}
