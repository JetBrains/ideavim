package com.maddyhome.idea.vim.group;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003-2008 Rick Maddy
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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.maddyhome.idea.vim.KeyHandler;
import com.maddyhome.idea.vim.VimPlugin;
import com.maddyhome.idea.vim.command.Command;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.common.TextRange;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.ExException;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.helper.RunnableHelper;
import com.maddyhome.idea.vim.key.KeyParser;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
import com.maddyhome.idea.vim.ui.MorePanel;

import javax.swing.*;
import java.io.*;

/**
 *
 */
public class ProcessGroup extends AbstractActionGroup {
  public ProcessGroup() {
  }

  public String getLastCommand() {
    return lastCommand;
  }

  public void startSearchCommand(Editor editor, DataContext context, int count, char leader) {
    if (editor.isOneLineMode()) // Don't allow searching in one line editors
    {
      return;
    }

    String initText = "";
    String label = "" + leader;

    //CommandState.getInstance(editor).pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(editor, context, label, initText, count);
  }

  public String endSearchCommand(final Editor editor, DataContext context) {
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate();

    final Project project = PlatformDataKeys.PROJECT.getData(context); // API change - don't merge
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        VirtualFile vf = EditorData.getVirtualFile(editor);
        if (vf != null) {
          FileEditorManager.getInstance(project).openFile(EditorData.getVirtualFile(editor), true);
        }
      }
    });

    record(editor, panel.getText());
    return panel.getText();
  }

  public void startExCommand(Editor editor, DataContext context, Command cmd) {
    if (editor.isOneLineMode()) // Don't allow ex commands in one line editors
    {
      return;
    }

    String initText = getRange(editor, cmd);
    CommandState.getInstance(editor).pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(editor, context, ":", initText, 1);
  }

  public boolean processExKey(Editor editor, DataContext context, KeyStroke stroke, boolean charOnly) {
    // This will only get called if somehow the key focus ended up in the editor while the ex entry window
    // is open. So I'll put focus back in the editor and process the key.

    ExEntryPanel panel = ExEntryPanel.getInstance();
    if (panel.isActive()) {
      panel.requestFocus();
      panel.handleKey(stroke);

      return true;
    }
    else {
      CommandState.getInstance(editor).popState();
      KeyHandler.getInstance().reset(editor);
      return false;
    }

    /*
    if (!charOnly || stroke.getKeyChar() != KeyEvent.CHAR_UNDEFINED && ExEntryPanel.getInstance().isActive())
    {
        ExEntryPanel panel = ExEntryPanel.getInstance();
        panel.handleKey(stroke);

        return true;
    }
    else
    {
        return false;
    }
    */
  }

  public boolean processExEntry(final Editor editor, final DataContext context) {
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate();
    boolean res = true;
    int flags = 0;
    try {
      CommandState.getInstance(editor).popState();
      logger.debug("processing command");
      final String text = panel.getText();
      record(editor, text);
      if (logger.isDebugEnabled()) logger.debug("swing=" + SwingUtilities.isEventDispatchThread());
      if (panel.getLabel().equals(":")) {
        flags = CommandParser.getInstance().processCommand(editor, context, text, 1);
        if (logger.isDebugEnabled()) logger.debug("flags=" + flags);
        if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
          CommandGroups.getInstance().getMotion().exitVisual(editor, true);
        }
      }
      else {
        int pos = CommandGroups.getInstance().getSearch().search(editor, context, text, panel.getCount(),
                                                                 panel.getLabel().equals("/")
                                                                 ? Command.FLAG_SEARCH_FWD
                                                                 : Command.FLAG_SEARCH_REV, true);
        if (pos == -1) {
          res = false;
        }
      }
    }
    catch (ExException ex) {
      //VimPlugin.showMessage(ex.getMessage());
      ProcessGroup.logger.info(ex.getMessage());
      VimPlugin.indicateError();
      res = false;
    }
    catch (Exception bad) {
      ProcessGroup.logger.error(bad);
      VimPlugin.indicateError();
      res = false;
    }
    finally {
      final int flg = flags;
      final Project project = PlatformDataKeys.PROJECT.getData(context); // API change - don't merge
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          //editor.getContentComponent().requestFocus();
          // Reopening the file was the only way I could solve the focus problem introduced in IDEA at
          // version 1050.
          if ((flg & CommandParser.RES_DONT_REOPEN) == 0) {
            VirtualFile vf = EditorData.getVirtualFile(editor);
            if (vf != null) {
              FileEditorManager.getInstance(project).openFile(EditorData.getVirtualFile(editor), true);
            }
          }

          // If the result of the ex command is to display the "more" panel, show it here.
          if ((flg & CommandParser.RES_MORE_PANEL) != 0 && MorePanel.getInstance(editor).hasText()) {
            RunnableHelper.runReadCommand(project, new Runnable() {
                public void run() {
                  MorePanel.getInstance(editor).activate();
                }
              }, "ShowMorePanel", "ExCommand");
          }
        }
      });
    }

    return res;
  }

  public boolean cancelExEntry(final Editor editor, final DataContext context) {
    CommandState.getInstance(editor).popState();
    KeyHandler.getInstance().reset(editor);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.deactivate();
    final Project project = PlatformDataKeys.PROJECT.getData(context); // API change - don't merge
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        //editor.getContentComponent().requestFocus();
        VirtualFile vf = EditorData.getVirtualFile(editor);
        if (vf != null) {
          FileEditorManager.getInstance(project).openFile(EditorData.getVirtualFile(editor), true);
        }
      }
    });

    return true;
  }

  private void record(Editor editor, String text) {
    if (CommandState.getInstance(editor).isRecording()) {
      CommandGroups.getInstance().getRegister().addText(text);
    }
  }

  public void startFilterCommand(Editor editor, DataContext context, Command cmd) {
    String initText = getRange(editor, cmd) + "!";
    CommandState.getInstance(editor).pushState(CommandState.MODE_EX_ENTRY, 0, KeyParser.MAPPING_CMD_LINE);
    ExEntryPanel panel = ExEntryPanel.getInstance();
    panel.activate(editor, context, ":", initText, 1);
  }

  private String getRange(Editor editor, Command cmd) {
    String initText = "";
    if (CommandState.getInstance(editor).getMode() == CommandState.MODE_VISUAL) {
      initText = "'<,'>";
    }
    else if (cmd.getRawCount() > 0) {
      if (cmd.getCount() == 1) {
        initText = ".";
      }
      else {
        initText = ".,.+" + (cmd.getCount() - 1);
      }
    }

    return initText;
  }

  public boolean executeFilter(Editor editor, DataContext context, TextRange range, String command) throws IOException {
    if (logger.isDebugEnabled()) logger.debug("command=" + command);
    CharSequence chars = editor.getDocument().getCharsSequence();
    StringReader car = new StringReader(chars.subSequence(range.getStartOffset(),
                                                          range.getEndOffset()).toString());
    StringWriter sw = new StringWriter();

    logger.debug("about to create filter");
    Process filter = Runtime.getRuntime().exec(command);
    logger.debug("filter created");
    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(filter.getOutputStream()));
    logger.debug("sending text");
    copy(car, writer);
    writer.close();
    logger.debug("sent");

    BufferedReader reader = new BufferedReader(new InputStreamReader(filter.getInputStream()));
    logger.debug("getting result");
    copy(reader, sw);
    sw.close();
    logger.debug("received");

    editor.getDocument().replaceString(range.getStartOffset(), range.getEndOffset(), sw.toString());

    lastCommand = command;

    return true;
  }

  private void copy(Reader from, Writer to) throws IOException {
    char[] buf = new char[2048];
    int cnt;
    while ((cnt = from.read(buf)) != -1) {
      if (logger.isDebugEnabled()) logger.debug("buf=" + buf);
      to.write(buf, 0, cnt);
    }
  }

  private String lastCommand;

  private static Logger logger = Logger.getInstance(ProcessGroup.class.getName());
}
