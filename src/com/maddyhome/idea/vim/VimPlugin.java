package com.maddyhome.idea.vim;

/*
 * IdeaVim - A Vim emulator plugin for IntelliJ Idea
 * Copyright (C) 2003 Rick Maddy
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
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.WindowManager;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.group.ChangeGroup;
import com.maddyhome.idea.vim.group.CommandGroups;
import com.maddyhome.idea.vim.group.FileGroup;
import com.maddyhome.idea.vim.group.MotionGroup;
import com.maddyhome.idea.vim.helper.EditorData;
import com.maddyhome.idea.vim.key.RegisterActions;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.MorePanel;
import java.awt.Toolkit;
import java.net.URL;
import javax.swing.ImageIcon;
import org.jdom.Element;

/**
 * This plugin attempts to emulate the keybinding and general functionality of Vim and gVim. See the supplied
 * documentation for a complete list of supported and unsupported Vim emulation. The code base contains some
 * debugging output that can be enabled in necessary.
 *
 * This is an application level plugin meaning that all open projects will share a common instance of the plugin.
 * Registers and marks are shared across open projects so you can copy and paste between files of different projects.
 *
 * @version 0.1
 */
public class VimPlugin implements ApplicationComponent, JDOMExternalizable
{
    /**
     * Creates the Vim Plugin
     */
    public VimPlugin()
    {
        logger.debug("VimPlugin ctr");
    }

    /**
     * Supplies the name of the plugin
     * @return The plugin name
     */
    public String getComponentName()
    {
        return "VimPlugin";
    }

    /**
     * Initialize the Vim Plugin. This plugs the vim key handler into the editor action mananger.
     */
    public void initComponent()
    {
        logger.debug("initComponent");
        // It appears that custom actions listed in plugin.xml are not registered with the ActionManager until
        // after this method returns :(

        EditorActionManager manager = EditorActionManager.getInstance();
        TypedAction action = manager.getTypedAction();

        // Replace the default key handler with the Vim key handler
        vimHandler = new VimTypedActionHandler(action.getHandler());
        action.setupHandler(vimHandler);

        // Add some listeners so we can handle special events
        setupListeners();

        logger.debug("done");
    }

    /**
     * This sets up some listeners so we can handle various events that occur
     */
    private void setupListeners()
    {
        EditorFactory.getInstance().addEditorFactoryListener(new EditorFactoryAdapter() {
            public void editorCreated(EditorFactoryEvent event)
            {
                EditorData.initializeEditor(event.getEditor());
            }

            public void editorReleased(EditorFactoryEvent event)
            {
                EditorData.uninitializeEditor(event.getEditor());
            }
        });

        // Since the Vim plugin custom actions aren't available to the call to <code>initComponent()</code>
        // we need to force the generation of the key map when the first project is opened.
        ProjectManager.getInstance().addProjectManagerListener(new ProjectManagerAdapter() {
            public void projectOpened(Project project)
            {
                // Make sure all the keys are registered before the user can interact with the first project
                actions = RegisterActions.getInstance();
                CommandParser.getInstance().registerHandlers();

                FileEditorManager.getInstance(project).addFileEditorManagerListener(new ChangeGroup.InsertCheck());
                FileEditorManager.getInstance(project).addFileEditorManagerListener(new MotionGroup.MotionEditorChange());
                FileEditorManager.getInstance(project).addFileEditorManagerListener(new MorePanel.MoreEditorChangeListener());
                FileEditorManager.getInstance(project).addFileEditorManagerListener(new FileGroup.SelectionCheck());

                /*
                ToolWindowManager mgr = ToolWindowManager.getInstance(project);
                ToolWindow win = mgr.registerToolWindow("VIM", VimToolWindow.getInstance(), ToolWindowAnchor.BOTTOM);
                setupToolWindow(win);
                toolWindows.put(project, win);
                */
            }

            public void projectClosed(Project project)
            {
                /*
                toolWindows.remove(project);
                ToolWindowManager mgr = ToolWindowManager.getInstance(project);
                mgr.unregisterToolWindow("VIM");
                */
            }
        });
    }

    private void setupToolWindow(ToolWindow win)
    {
        win.setTitle("");
        URL url = getClass().getClassLoader().getResource("icons/logo.png");
        ImageIcon icon = new ImageIcon(url);
        win.setIcon(icon);

        /*
        win.setType(ToolWindowType.DOCKED, null);
        if (isEnabled())
        {
            win.setAutoHide(false);
            win.show(null);
        }
        else
        {
            win.setAutoHide(true);
            win.hide(null);
        }
        */
    }

    /**
     * This shuts down the Vim plugin. All we need to do is reinstall the original key handler
     */
    public void disposeComponent()
    {
        logger.debug("disposeComponent");
        EditorActionManager manager = EditorActionManager.getInstance();
        TypedAction action = manager.getTypedAction();
        action.setupHandler(vimHandler.getOriginalTypedHandler());
        logger.debug("done");
    }

    /**
     * This is called by the framework to load custom configuration data. The data is stored in
     * <code>$HOME/.IntelliJIdea/config/options/other.xml</code> though this is handled by the openAPI.
     * @param element The element specific to the Vim Plugin. All the plugin's custom state information is
     *                children of this element.
     * @throws InvalidDataException if any of the configuration data is invalid
     */
    public void readExternal(Element element) throws InvalidDataException
    {
        logger.debug("readExternal");

        // Restore whether the plugin is enabled or not
        Element state = element.getChild("state");
        if (state != null)
        {
            enabled = Boolean.valueOf(state.getAttributeValue("enabled")).booleanValue();
        }

        CommandGroups.getInstance().readData(element);
    }

    /**
     * This is called by the framework to store custom configuration data. The data is stored in
     * <code>$HOME/.IntelliJIdea/config/options/other.xml</code> though this is handled by the openAPI.
     * @param element The element specific to the Vim Plugin. All the plugin's custom state information is
     *                children of this element.
     * @throws WriteExternalException if unable to save and of the configuration data
     */
    public void writeExternal(Element element) throws WriteExternalException
    {
        // Save whether the plugin is enabled or not
        Element elem = new Element("state");
        elem.setAttribute("enabled", Boolean.toString(enabled));
        element.addContent(elem);

        CommandGroups.getInstance().saveData(element);
    }

    /**
     * Indicates whether the user has enabled or disabled the plugin
     * @return true if the Vim plugin is enabled, false if not
     */
    public static boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Inidicate to the user that an error has occurred. Just beep.
     */
    public static void indicateError()
    {
        if (!Options.getInstance().isSet("visualbell"))
        {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    public static void showMode(String msg)
    {
        showMessage(msg);
    }

    public static void showMessage(String msg)
    {
        /*
        for (Iterator iterator = toolWindows.values().iterator(); iterator.hasNext();)
        {
            ToolWindow window = (ToolWindow)iterator.next();
            window.setTitle(msg);
        }
        */
        ProjectManager pm = ProjectManager.getInstance();
        Project[] projs = pm.getOpenProjects();
        for (int i = 0; i < projs.length; i++)
        {
            StatusBar bar = WindowManager.getInstance().getStatusBar(projs[i]);
            if (msg == null || msg.length() == 0)
            {
                bar.setInfo("");
            }
            else
            {
                bar.setInfo("VIM - " + msg);
            }
        }
    }

    /**
     * This class is used to handle the Vim Plugin enabled/disabled toggle. This is most likely used as a menu
     * option but could also be used as a toolbar item.
     */
    public static class VimPluginToggleAction extends ToggleAction
    {
        /**
         * Indicates if the toggle is on or off
         * @param event The event that triggered the action
         * @return true if the toggle is on, false if off
         */
        public boolean isSelected(AnActionEvent event)
        {
            return enabled;
        }

        /**
         * Specifies whether the toggle should be on or off
         * @param event The event that triggered the action
         * @param b The new state - true is on, false is off
         */
        public void setSelected(AnActionEvent event, boolean b)
        {
            enabled = b;
        }
    }

    private VimTypedActionHandler vimHandler;
    private RegisterActions actions;
    //private static HashMap toolWindows = new HashMap();

    private static boolean enabled = true;
    private static Logger logger = Logger.getInstance(VimPlugin.class.getName());
}
