/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2021 The IdeaVim authors
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

package com.maddyhome.idea.vim.ui;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.KeyboardShortcut;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBoxTableRenderer;
import com.intellij.openapi.ui.StripeTable;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.UIUtil;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.helper.MessageHelper;
import com.maddyhome.idea.vim.key.ShortcutOwner;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * @author vlan
 */
public class VimEmulationConfigurable implements Configurable {
  private final @NotNull VimShortcutConflictsTable.Model myConflictsTableModel = new VimShortcutConflictsTable.Model();
  private final @NotNull VimSettingsPanel myPanel = new VimSettingsPanel(myConflictsTableModel);

  @Override
  public @NotNull String getDisplayName() {
    return MessageHelper.message("configurable.name.vim.emulation");
  }

  @Override
  public @Nullable String getHelpTopic() {
    return null;
  }

  @Override
  public @Nullable JComponent createComponent() {
    return myPanel;
  }

  @Override
  public boolean isModified() {
    return myConflictsTableModel.isModified();
  }

  @Override
  public void apply() throws ConfigurationException {
    myConflictsTableModel.apply();
  }

  @Override
  public void reset() {
    myConflictsTableModel.reset();
  }

  @Override
  public void disposeUIResources() {
  }

  private static final class VimSettingsPanel extends JPanel {

    public VimSettingsPanel(@NotNull VimShortcutConflictsTable.Model model) {
      VimShortcutConflictsTable shortcutConflictsTable = new VimShortcutConflictsTable(model);
      setLayout(new BorderLayout());
      final JScrollPane scrollPane = new JBScrollPane(shortcutConflictsTable);
      scrollPane.setBorder(new LineBorder(JBColor.border()));
      final JPanel conflictsPanel = new JPanel(new BorderLayout());
      final String title = MessageHelper.message("border.title.shortcut.conflicts.for.active.keymap");
      conflictsPanel.setBorder(IdeBorderFactory.createTitledBorder(title, false));
      conflictsPanel.add(scrollPane);
      add(conflictsPanel, BorderLayout.CENTER);
    }
  }

  private static final class VimShortcutConflictsTable extends StripeTable {
    public VimShortcutConflictsTable(@NotNull Model model) {
      super(model);
      getTableColumn(Column.KEYSTROKE).setPreferredWidth(100);
      getTableColumn(Column.IDE_ACTION).setPreferredWidth(400);
      final TableColumn ownerColumn = getTableColumn(Column.OWNER);
      final ComboBoxTableRenderer<ShortcutOwner> renderer = new ShortcutOwnerRenderer();
      ownerColumn.setPreferredWidth(150);
      ownerColumn.setCellRenderer(renderer);
      ownerColumn.setCellEditor(renderer);
    }

    @Override
    public @NotNull Dimension getMinimumSize() {
      return calcSize(super.getMinimumSize());
    }

    @Override
    public @NotNull Dimension getPreferredSize() {
      return calcSize(super.getPreferredSize());
    }

    private @NotNull Dimension calcSize(@NotNull Dimension dimension) {
      final Container container = getParent();
      if (container != null) {
        final Dimension size = container.getSize();
        return new Dimension(size.width, dimension.height);
      }
      return dimension;
    }

    private @NotNull TableColumn getTableColumn(@NotNull Column column) {
      return getColumnModel().getColumn(column.getIndex());
    }

    private static final class ShortcutOwnerRenderer extends ComboBoxTableRenderer<ShortcutOwner> {
      public ShortcutOwnerRenderer() {
        super(ShortcutOwner.values());
      }

      @Override
      protected void customizeComponent(ShortcutOwner owner, JTable table, boolean isSelected) {
        super.customizeComponent(owner, table, isSelected);
        if (owner == ShortcutOwner.UNDEFINED) {
          setForeground(UIUtil.getComboBoxDisabledForeground());
        }
      }

      @Override
      public boolean isCellEditable(EventObject event) {
        return true;
      }
    }

    private enum Column {
      KEYSTROKE(0, "Shortcut"),
      IDE_ACTION(1, "IDE Action"),
      OWNER(2, "Handler");

      private static final @NotNull Map<Integer, Column> ourMembers = new HashMap<>();

      static {
        for (Column column : values()) {
          ourMembers.put(column.myIndex, column);
        }
      }

      private final int myIndex;
      private final @NotNull String myTitle;

      Column(int index, @NotNull @Nls(capitalization = Nls.Capitalization.Title) String title) {
        myIndex = index;
        myTitle = title;
      }

      public static @Nullable Column fromIndex(int index) {
        return ourMembers.get(index);
      }

      public int getIndex() {
        return myIndex;
      }

      public @NotNull String getTitle() {
        return myTitle;
      }
    }

    private static final class Row implements Comparable<Row> {
      private final @NotNull KeyStroke myKeyStroke;
      private final @NotNull AnAction myAction;
      private @NotNull ShortcutOwner myOwner;

      private Row(@NotNull KeyStroke keyStroke, @NotNull AnAction action, @NotNull ShortcutOwner owner) {
        myKeyStroke = keyStroke;
        myAction = action;
        myOwner = owner;
      }

      public @NotNull KeyStroke getKeyStroke() {
        return myKeyStroke;
      }

      public @NotNull AnAction getAction() {
        return myAction;
      }

      public @NotNull ShortcutOwner getOwner() {
        return myOwner;
      }

      @Override
      public int compareTo(@NotNull Row row) {
        final KeyStroke otherKeyStroke = row.getKeyStroke();
        final int keyCodeDiff = myKeyStroke.getKeyCode() - otherKeyStroke.getKeyCode();
        return keyCodeDiff != 0 ? keyCodeDiff : myKeyStroke.getModifiers() - otherKeyStroke.getModifiers();
      }

      public void setOwner(@NotNull ShortcutOwner owner) {
        myOwner = owner;
      }
    }

    private static final class Model extends AbstractTableModel {
      private final @NotNull List<Row> myRows = new ArrayList<>();

      public Model() {
        reset();
      }

      @Override
      public int getRowCount() {
        return myRows.size();
      }

      @Override
      public int getColumnCount() {
        return Column.values().length;
      }

      @Override
      public @Nullable Object getValueAt(int rowIndex, int columnIndex) {
        final Column column = Column.fromIndex(columnIndex);
        if (column != null && rowIndex >= 0 && rowIndex < myRows.size()) {
          final Row row = myRows.get(rowIndex);
          switch (column) {
            case KEYSTROKE:
              return KeymapUtil.getShortcutText(new KeyboardShortcut(row.getKeyStroke(), null));
            case IDE_ACTION:
              return row.getAction().getTemplatePresentation().getText();
            case OWNER:
              return row.getOwner();
          }
        }
        return null;
      }

      @Override
      public void setValueAt(Object object, int rowIndex, int columnIndex) {
        final Column column = Column.fromIndex(columnIndex);
        if (column != null && rowIndex >= 0 && rowIndex < myRows.size() && object instanceof ShortcutOwner) {
          final Row row = myRows.get(rowIndex);
          row.setOwner((ShortcutOwner)object);
        }
      }

      @Override
      public boolean isCellEditable(int rowIndex, int columnIndex) {
        return Column.fromIndex(columnIndex) == Column.OWNER;
      }

      @Override
      public @Nullable String getColumnName(int index) {
        final Column column = Column.fromIndex(index);
        return column != null ? column.getTitle() : null;
      }

      public boolean isModified() {
        return !VimPlugin.getKey().getShortcutConflicts().equals(getCurrentData());
      }

      public void apply() {
        VimPlugin.getKey().getSavedShortcutConflicts().putAll(getCurrentData());
      }

      public void reset() {
        myRows.clear();
        for (Map.Entry<KeyStroke, ShortcutOwner> entry : VimPlugin.getKey().getShortcutConflicts().entrySet()) {
          final KeyStroke keyStroke = entry.getKey();
          final List<AnAction> actions = VimPlugin.getKey().getKeymapConflicts(keyStroke);
          if (!actions.isEmpty()) {
            myRows.add(new Row(keyStroke, actions.get(0), entry.getValue()));
          }
        }
        Collections.sort(myRows);
      }

      private @NotNull Map<KeyStroke, ShortcutOwner> getCurrentData() {
        final Map<KeyStroke, ShortcutOwner> result = new HashMap<>();
        for (Row row : myRows) {
          result.put(row.getKeyStroke(), row.getOwner());
        }
        return result;
      }
    }
  }
}
