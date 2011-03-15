package com.maddyhome.idea.vim;

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

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.maddyhome.idea.vim.command.CommandState;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.helper.ApiHelper;
import com.maddyhome.idea.vim.helper.DelegateCommandListener;
import com.maddyhome.idea.vim.helper.DocumentManager;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.key.RegisterActions;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.undo.UndoManager;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;

/**
 * This plugin attempts to emulate the keybinding and general functionality of Vim and gVim. See the supplied
 * documentation for a complete list of supported and unsupported Vim emulation. The code base contains some debugging
 * output that can be enabled in necessary.
 * <p/>
 * This is an application level plugin meaning that all open projects will share a common instance of the plugin.
 * Registers and marks are shared across open projects so you can copy and paste between files of different projects.
 *
 * @version 0.1
 */
@State(
    name = "VimSettings",
    storages = {
        @Storage(
            id = "main",
            file = "$APP_CONFIG$/vim_settings.xml"
        )}
)
public class VimPlugin implements ApplicationComponent, PersistentStateComponent<Element>
{

  private static VimPlugin instance;
  private VimTypedActionHandler vimHandler;
  private RegisterActions actions;
  private boolean isBlockCursor = false;
  private boolean isSmoothScrolling = false;

  private boolean enabled = true;
  private static Logger LOG = Logger.getInstance(VimPlugin.class.getName());

  /**
   * Creates the Vim Plugin
   */
  public VimPlugin() {
    LOG.debug("VimPlugin ctr");
    instance = this;
  }

  public static VimPlugin getInstance() {
    return instance;
  }

  /**
   * Supplies the name of the plugin
   *
   * @return The plugin name
   */
  @NotNull
  public String getComponentName() {
    return "VimPlugin";
  }

  /**
   * Initialize the Vim Plugin. This plugs the vim key handler into the editor action mananger.
   */
  public void initComponent() {
    LOG.debug("initComponent");

    EditorActionManager manager = EditorActionManager.getInstance();
    TypedAction action = manager.getTypedAction();

    // Replace the default key handler with the Vim key handler
    vimHandler = new VimTypedActionHandler(action.getHandler());
    action.setupHandler(vimHandler);

    // Add some listeners so we can handle special events
    setupListeners();

    getActions();

    LOG.debug("done");
  }

  /**
   * This sets up some listeners so we can handle various events that occur
   */
  private void setupListeners() {
    DocumentManager.getInstance().addDocumentListener(new MarkGroup.MarkUpdater());
    DocumentManager.getInstance().addDocumentListener(new UndoManager.DocumentChangeListener());
    if (ApiHelper.supportsColorSchemes()) {
      DocumentManager.getInstance().addDocumentListener(new SearchGroup.DocumentSearchListener());
    }
    DocumentManager.getInstance().init();

    EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
      public void editorCreated(EditorFactoryEvent event) {
        isBlockCursor = event.getEditor().getSettings().isBlockCursor();
        isSmoothScrolling = event.getEditor().getSettings().isAnimatedScrolling();

        if (VimPlugin.isEnabled()) {
          event.getEditor().getSettings().setBlockCursor(!CommandState.inInsertMode(event.getEditor()));
          event.getEditor().getSettings().setAnimatedScrolling(false);
        }

        EditorData.initializeEditor(event.getEditor());
        DocumentManager.getInstance().addListeners(event.getEditor().getDocument());
      }

      public void editorReleased(EditorFactoryEvent event) {
        EditorData.uninitializeEditor(event.getEditor());
        event.getEditor().getSettings().setAnimatedScrolling(isSmoothScrolling);
        DocumentManager.getInstance().removeListeners(event.getEditor().getDocument());
      }
    });

    // Since the Vim plugin custom actions aren't available to the call to <code>initComponent()</code>
    // we need to force the generation of the key map when the first project is opened.
    ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
      public void projectOpened(Project project) {
        listeners.add(new MotionGroup.MotionEditorChange());
        listeners.add(new FileGroup.SelectionCheck());
        if (ApiHelper.supportsColorSchemes()) {
          listeners.add(new SearchGroup.EditorSelectionCheck());
        }

        for (FileEditorManagerListener listener : listeners) {
          FileEditorManager.getInstance(project).addFileEditorManagerListener(listener);
        }
      }

      public void projectClosed(Project project) {
        for (FileEditorManagerListener listener : listeners) {
          FileEditorManager.getInstance(project).removeFileEditorManagerListener(listener);
        }
        listeners.clear();
      }

      ArrayList<FileEditorManagerListener> listeners = new ArrayList<FileEditorManagerListener>();
    });

    CommandProcessor.getInstance().addCommandListener(DelegateCommandListener.getInstance());
  }

  /**
   * This shuts down the Vim plugin. All we need to do is reinstall the original key handler
   */
  public void disposeComponent() {
    LOG.debug("disposeComponent");
    setEnabled(false);
    EditorActionManager manager = EditorActionManager.getInstance();
    TypedAction action = manager.getTypedAction();
    action.setupHandler(vimHandler.getOriginalTypedHandler());
    LOG.debug("done");
  }

  @Override
  public void loadState(Element element) {
    LOG.debug("Loading state");

    // Restore whether the plugin is enabled or not
    Element state = element.getChild("state");
    if (state != null) {
      enabled = Boolean.valueOf(state.getAttributeValue("enabled"));
    }

    CommandGroups.getInstance().readData(element);
  }

  @Override
  public Element getState() {
    LOG.debug("Saving state");

    // Save whether the plugin is enabled or not
    Element element = new Element("state");
    element.setAttribute("enabled", Boolean.toString(enabled));
    element.addContent(element);

    CommandGroups.getInstance().saveData(element);
    return element;
  }

  /**
   * Indicates whether the user has enabled or disabled the plugin
   *
   * @return true if the Vim plugin is enabled, false if not
   */
  public static boolean isEnabled() {
    return getInstance().enabled;
  }

  public static void setEnabled(boolean set) {
    if (!set) {
      getInstance().turnOffPlugin();
    }

    getInstance().enabled = set;

    if (set) {
      getInstance().turnOnPlugin();
    }
  }

  /**
   * Inidicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    if (!Options.getInstance().isSet("visualbell")) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void showMode(String msg) {
    showMessage(msg);
  }

  public static void showMessage(String msg) {
    ProjectManager pm = ProjectManager.getInstance();
    Project[] projs = pm.getOpenProjects();
    for (Project proj : projs) {
      StatusBar bar = WindowManager.getInstance().getStatusBar(proj);
      if (msg == null || msg.length() == 0) {
        bar.setInfo("");
      }
      else {
        bar.setInfo("VIM - " + msg);
      }
    }
  }

  public void turnOnPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(true);
    setSmoothScrolling(false);

    CommandGroups.getInstance().getMotion().turnOn();
  }

  public void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);
    setCursors(isBlockCursor);
    setSmoothScrolling(isSmoothScrolling);

    CommandGroups.getInstance().getMotion().turnOff();
  }

  private void setCursors(boolean isBlock) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setBlockCursor(isBlock);
    }
  }

  private void setSmoothScrolling(boolean isOn) {
    Editor[] editors = EditorFactory.getInstance().getAllEditors();
    for (Editor editor : editors) {
      editor.getSettings().setAnimatedScrolling(isOn);
    }
  }


  private RegisterActions getActions() {
    if (actions == null) {
      actions = RegisterActions.getInstance();
      CommandParser.getInstance().registerHandlers();
    }

    return actions;
  }

  /**
   * This class is used to handle the Vim Plugin enabled/disabled toggle. This is most likely used as a menu option
   * but could also be used as a toolbar item.
   */
  public static class VimPluginToggleAction extends ToggleAction implements DumbAware {
    /**
     * Indicates if the toggle is on or off
     *
     * @param event The event that triggered the action
     * @return true if the toggle is on, false if off
     */
    public boolean isSelected(AnActionEvent event) {
      return VimPlugin.isEnabled();
    }

    /**
     * Specifies whether the toggle should be on or off
     *
     * @param event The event that triggered the action
     * @param b     The new state - true is on, false is off
     */
    public void setSelected(AnActionEvent event, boolean b) {
      VimPlugin.setEnabled(b);
    }
  }
}
