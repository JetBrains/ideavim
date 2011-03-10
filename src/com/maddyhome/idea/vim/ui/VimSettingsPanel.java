package com.maddyhome.idea.vim.ui;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2005 Rick Maddy
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.keymap.KeymapUtil;
import com.maddyhome.idea.vim.VimSettings;
import com.maddyhome.idea.vim.key.KeyConflict;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.util.*;

public class VimSettingsPanel {
  public VimSettingsPanel() {
    setupControls();
  }

  public JComponent getMainComponent() {
    return mainPanel;
  }

  public void setOptions(VimSettings options, HashMap<KeyStroke, KeyConflict> conflicts) {
    pluginEnable.setSelected(options.isEnabled());

    keys.setModel(new ConflictTableModel(conflicts));
    sizeColumns();
    scrollPane.getVerticalScrollBar().setValue(0);
  }

  private void sizeColumns() {
    TableColumn choice = keys.getColumn(keys.getColumnName(0));
    choice.sizeWidthToFit();
    TableColumn key = keys.getColumn(keys.getColumnName(1));
    key.sizeWidthToFit();

    int width = getPreferredWidthForColumn(choice);
    choice.setMinWidth(width);
    choice.setMaxWidth(width);

    width = getPreferredWidthForColumn(key);
    key.setMinWidth(width);
    key.setMaxWidth(width);

    keys.sizeColumnsToFit(0);
  }

  private int getPreferredWidthForColumn(TableColumn column) {
    return Math.max(columnHeaderWidth(column), widestCellInColumn(column));
  }

  private int widestCellInColumn(TableColumn column) {
    int c = column.getModelIndex();
    int res = 0;

    TableCellRenderer renderer = keys.getDefaultRenderer(keys.getColumnClass(c));
    for (int r = 0; r < keys.getRowCount(); r++) {
      int width = renderer.getTableCellRendererComponent(
        keys, keys.getValueAt(r, c), false, false, r, c).getPreferredSize().width;
      res = Math.max(res, width);
    }

    return res;
  }

  private int columnHeaderWidth(TableColumn column) {
    TableCellRenderer renderer = keys.getTableHeader().getDefaultRenderer();

    return renderer.getTableCellRendererComponent(
      keys, column.getHeaderValue(), false, false, 0, 0).getPreferredSize().width + 2;
  }

  public VimSettings getOptions() {
    VimSettings res = new VimSettings();
    res.setEnabled(pluginEnable.isSelected());
    res.setChoices(((ConflictTableModel)keys.getModel()).getChoices());

    return res;
  }

  public boolean isModified(VimSettings options) {
    return !getOptions().equals(options);
  }

  private void setupControls() {
  }

  private static class ConflictTableModel extends AbstractTableModel {
    public ConflictTableModel(HashMap<KeyStroke, KeyConflict> conflicts) {
      TreeMap<String, KeyStroke> keys = new TreeMap<String, KeyStroke>();
      for (KeyStroke stroke : conflicts.keySet()) {
        KeyConflict conf = conflicts.get(stroke);
        if (conf.hasConflict()) {
          keys.put(KeymapUtil.getKeystrokeText(stroke), stroke);
        }
      }

      orig = new boolean[keys.size()];
      keystokes = new KeyStroke[keys.size()];
      data = new Object[keys.size()][4];

      ActionManager mgr = ActionManager.getInstance();
      Iterator iter = keys.keySet().iterator();
      for (int i = 0; iter.hasNext(); i++) {
        String keyLabel = (String)iter.next();
        KeyStroke stroke = keys.get(keyLabel);
        KeyConflict conf = conflicts.get(stroke);

        orig[i] = !conf.isPluginWins();
        keystokes[i] = stroke;
        data[i][0] = !conf.isPluginWins();
        data[i][1] = keyLabel;

        TreeSet<String> actions = new TreeSet<String>();
        for (String id : conf.getIdeaActions().keySet()) {
          String desc = null;
          AnAction act = mgr.getAction(id);
          if (act != null) {
            Presentation pres = act.getTemplatePresentation();
            if (pres != null) {
              desc = pres.getText();
            }
          }
          if (desc != null) {
            actions.add(desc);
          }
        }

        data[i][2] = actions.toString();

        actions = new TreeSet<String>();
        List<String> pacts = conf.getPluginActions();
        for (String id : pacts) {
          actions.add(mgr.getAction(id).getTemplatePresentation().getText());
        }

        data[i][3] = actions.toString();
      }
    }

    public HashSet getChoices() {
      HashSet<KeyStroke> res = new HashSet<KeyStroke>();
      for (int i = 0; i < getRowCount(); i++) {
        Boolean val = (Boolean)getValueAt(i, 0);
        if (val) {
          res.add(keystokes[i]);
        }
      }

      return res;
    }

    public int getColumnCount() {
      return labels.length;
    }

    public int getRowCount() {
      return data == null ? 0 : data.length;
    }

    public Object getValueAt(int row, int col) {
      return data[row][col];
    }

    public boolean isCellEditable(int row, int col) {
      return col == 0;
    }

    public Class getColumnClass(int col) {
      return col == 0 ? Boolean.class : String.class;
    }

    public void setValueAt(Object object, int row, int col) {
      data[row][col] = object;
    }

    public String getColumnName(int col) {
      return labels[col];
    }

    boolean[] orig;
    KeyStroke[] keystokes;
    Object[][] data;
    String[] labels = new String[]{"Use IDEA Action", "KeyStroke", "IDEA Actions", "VIM Actions"};
  }

  private JPanel mainPanel;
  private JTable keys;
  private JCheckBox pluginEnable;
  private JScrollPane scrollPane;
}