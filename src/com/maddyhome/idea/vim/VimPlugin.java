/*
 * IdeaVim - Vim emulator for IDEs based on the IntelliJ platform
 * Copyright (C) 2003-2019 The IdeaVim authors
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
package com.maddyhome.idea.vim;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.actionSystem.EditorActionManager;
import com.intellij.openapi.editor.actionSystem.TypedAction;
import com.intellij.openapi.editor.event.EditorFactoryAdapter;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.DefaultKeymap;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerAdapter;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.updateSettings.impl.UpdateChecker;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.io.HttpRequests;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.group.copy.PutGroup;
import com.maddyhome.idea.vim.group.copy.YankGroup;
import com.maddyhome.idea.vim.group.visual.VisualMotionGroup;
import com.maddyhome.idea.vim.helper.DocumentManager;
import com.maddyhome.idea.vim.helper.MacKeyRepeat;
import com.maddyhome.idea.vim.option.Options;
import com.maddyhome.idea.vim.ui.VimEmulationConfigurable;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.concurrent.TimeUnit;

/**
 * This plugin attempts to emulate the key binding and general functionality of Vim and gVim. See the supplied
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
  storages = {@Storage(file = "$APP_CONFIG$/vim_settings.xml")})
public class VimPlugin implements ApplicationComponent, PersistentStateComponent<Element> {
  private static final String IDEAVIM_COMPONENT_NAME = "VimPlugin";
  private static final String IDEAVIM_PLUGIN_ID = "IdeaVIM";
  private static final String IDEAVIM_STATISTICS_TIMESTAMP_KEY = "ideavim.statistics.timestamp";
  public static final String IDEAVIM_NOTIFICATION_ID = "ideavim";
  public static final String IDEAVIM_STICKY_NOTIFICATION_ID = "ideavim-sticky";
  public static final String IDEAVIM_NOTIFICATION_TITLE = "IdeaVim";
  public static final int STATE_VERSION = 4;

  private boolean error = false;

  private int previousStateVersion = 0;
  private String previousKeyMap = "";

  // It is enabled by default to avoid any special configuration after plugin installation
  private boolean enabled = true;

  private static final Logger LOG = Logger.getInstance(VimPlugin.class);

  @NotNull private final MotionGroup motion;
  @NotNull private final ChangeGroup change;
  @NotNull private final MarkGroup mark;
  @NotNull private final RegisterGroup register;
  @NotNull private final FileGroup file;
  @NotNull private final SearchGroup search;
  @NotNull private final ProcessGroup process;
  @NotNull private final MacroGroup macro;
  @NotNull private final DigraphGroup digraph;
  @NotNull private final HistoryGroup history;
  @NotNull private final KeyGroup key;
  @NotNull private final WindowGroup window;
  @NotNull private final EditorGroup editor;
  @NotNull private final VisualMotionGroup visualMotion;
  @NotNull private final YankGroup yank;
  @NotNull private final PutGroup put;

  public VimPlugin() {
    motion = new MotionGroup();
    change = new ChangeGroup();
    mark = new MarkGroup();
    register = new RegisterGroup();
    file = new FileGroup();
    search = new SearchGroup();
    process = new ProcessGroup();
    macro = new MacroGroup();
    digraph = new DigraphGroup();
    history = new HistoryGroup();
    key = new KeyGroup();
    window = new WindowGroup();
    editor = new EditorGroup();
    visualMotion = new VisualMotionGroup();
    yank = new YankGroup();
    put = new PutGroup();

    LOG.debug("VimPlugin ctr");
  }

  @NotNull
  @Override
  public String getComponentName() {
    return IDEAVIM_COMPONENT_NAME;
  }

  @Override
  public void initComponent() {
    LOG.debug("initComponent");

    Notifications.Bus.register(IDEAVIM_STICKY_NOTIFICATION_ID, NotificationDisplayType.STICKY_BALLOON);

    ApplicationManager.getApplication().invokeLater(new Runnable() {
      public void run() {
        updateState();
      }
    });

    final TypedAction typedAction = EditorActionManager.getInstance().getTypedAction();
    EventFacade.getInstance().setupTypedActionHandler(new VimTypedActionHandler(typedAction.getRawHandler()));

    // Register vim actions in command mode
    RegisterActions.registerActions();

    // Add some listeners so we can handle special events
    setupListeners();

    // Register ex handlers
    CommandParser.getInstance().registerHandlers();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      final File ideaVimRc = VimScriptParser.findIdeaVimRc();
      if (ideaVimRc != null) {
        VimScriptParser.executeFile(ideaVimRc);
      }
    }

    LOG.debug("done");
  }

  @Override
  public void disposeComponent() {
    LOG.debug("disposeComponent");
    turnOffPlugin();
    EventFacade.getInstance().restoreTypedActionHandler();
    LOG.debug("done");
  }

  @Override
  public Element getState() {
    LOG.debug("Saving state");

    final Element element = new Element("ideavim");
    // Save whether the plugin is enabled or not
    final Element state = new Element("state");
    state.setAttribute("version", Integer.toString(STATE_VERSION));
    state.setAttribute("enabled", Boolean.toString(enabled));
    element.addContent(state);

    mark.saveData(element);
    register.saveData(element);
    search.saveData(element);
    history.saveData(element);
    key.saveData(element);
    editor.saveData(element);

    return element;
  }

  @Override
  public void loadState(@NotNull final Element element) {
    LOG.debug("Loading state");

    // Restore whether the plugin is enabled or not
    Element state = element.getChild("state");
    if (state != null) {
      try {
        previousStateVersion = Integer.valueOf(state.getAttributeValue("version"));
      }
      catch (NumberFormatException ignored) {
      }
      enabled = Boolean.valueOf(state.getAttributeValue("enabled"));
      previousKeyMap = state.getAttributeValue("keymap");
    }

    mark.readData(element);
    register.readData(element);
    search.readData(element);
    history.readData(element);
    key.readData(element);
    editor.readData(element);
  }

  @NotNull
  public static MotionGroup getMotion() {
    return getInstance().motion;
  }

  @NotNull
  public static ChangeGroup getChange() {
    return getInstance().change;
  }

  @NotNull
  public static MarkGroup getMark() {
    return getInstance().mark;
  }

  @NotNull
  public static RegisterGroup getRegister() {
    return getInstance().register;
  }

  @NotNull
  public static FileGroup getFile() {
    return getInstance().file;
  }

  @NotNull
  public static SearchGroup getSearch() {
    return getInstance().search;
  }

  @NotNull
  public static ProcessGroup getProcess() {
    return getInstance().process;
  }

  @NotNull
  public static MacroGroup getMacro() {
    return getInstance().macro;
  }

  @NotNull
  public static DigraphGroup getDigraph() {
    return getInstance().digraph;
  }

  @NotNull
  public static HistoryGroup getHistory() {
    return getInstance().history;
  }

  @NotNull
  public static KeyGroup getKey() {
    return getInstance().key;
  }

  @NotNull
  public static WindowGroup getWindow() {
    return getInstance().window;
  }

  @NotNull
  public static EditorGroup getEditor() {
    return getInstance().editor;
  }

  @NotNull
  public static VisualMotionGroup getVisualMotion() {
    return getInstance().visualMotion;
  }

  @NotNull
  public static YankGroup getYank() {
    return getInstance().yank;
  }

  @NotNull
  public static PutGroup getPut() {
    return getInstance().put;
  }

  @NotNull
  public static PluginId getPluginId() {
    return PluginId.getId(IDEAVIM_PLUGIN_ID);
  }

  @NotNull
  public static String getVersion() {
    if (!ApplicationManager.getApplication().isInternal()) {
      final IdeaPluginDescriptor plugin = PluginManager.getPlugin(getPluginId());
      return plugin != null ? plugin.getVersion() : "SNAPSHOT";
    }
    else {
      return "INTERNAL";
    }
  }

  public static boolean isEnabled() {
    return getInstance().enabled;
  }

  public static void setEnabled(final boolean enabled) {
    if (!enabled) {
      getInstance().turnOffPlugin();
    }

    getInstance().enabled = enabled;

    if (enabled) {
      getInstance().turnOnPlugin();
    }
  }

  public static boolean isError() {
    return getInstance().error;
  }

  /**
   * Indicate to the user that an error has occurred. Just beep.
   */
  public static void indicateError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = true;
    }
    else if (!Options.getInstance().isSet("visualbell")) {
      Toolkit.getDefaultToolkit().beep();
    }
  }

  public static void clearError() {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      getInstance().error = false;
    }
  }

  public static void showMode(String msg) {
    showMessage(msg);
  }

  public static void showMessage(@Nullable String msg) {
    ProjectManager pm = ProjectManager.getInstance();
    Project[] projects = pm.getOpenProjects();
    for (Project project : projects) {
      StatusBar bar = WindowManager.getInstance().getStatusBar(project);
      if (bar != null) {
        if (msg == null || msg.length() == 0) {
          bar.setInfo("");
        }
        else {
          bar.setInfo("VIM - " + msg);
        }
      }
    }
  }

  @NotNull
  private static VimPlugin getInstance() {
    return (VimPlugin)ApplicationManager.getApplication().getComponent(IDEAVIM_COMPONENT_NAME);
  }

  private void turnOnPlugin() {
    KeyHandler.getInstance().fullReset(null);

    getEditor().turnOn();
    getMotion().turnOn();
  }

  private void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);

    getEditor().turnOff();
    getMotion().turnOff();
  }

  private void updateState() {
    if (isEnabled() && !ApplicationManager.getApplication().isUnitTestMode()) {
      if (SystemInfo.isMac) {
        final MacKeyRepeat keyRepeat = MacKeyRepeat.getInstance();
        final Boolean enabled = keyRepeat.isEnabled();
        final Boolean isKeyRepeat = editor.isKeyRepeat();
        if ((enabled == null || !enabled) && (isKeyRepeat == null || isKeyRepeat)) {
          if (Messages.showYesNoDialog("Do you want to enable repeating keys in Mac OS X on press and hold?\n\n" +
                                       "(You can do it manually by running 'defaults write -g " +
                                       "ApplePressAndHoldEnabled 0' in the console).", IDEAVIM_NOTIFICATION_TITLE,
                                       Messages.getQuestionIcon()) == Messages.YES) {
            editor.setKeyRepeat(true);
            keyRepeat.setEnabled(true);
          }
          else {
            editor.setKeyRepeat(false);
          }
        }
      }
      if (previousStateVersion > 0 && previousStateVersion < 3) {
        final KeymapManagerEx manager = KeymapManagerEx.getInstanceEx();
        Keymap keymap = null;
        if (previousKeyMap != null) {
          keymap = manager.getKeymap(previousKeyMap);
        }
        if (keymap == null) {
          keymap = manager.getKeymap(DefaultKeymap.getInstance().getDefaultKeymapName());
        }
        assert keymap != null : "Default keymap not found";
        new Notification(
          VimPlugin.IDEAVIM_STICKY_NOTIFICATION_ID,
          VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
          String.format("IdeaVim plugin doesn't use the special \"Vim\" keymap any longer. " +
                        "Switching to \"%s\" keymap.<br/><br/>" +
                        "Now it is possible to set up:<br/>" +
                        "<ul>" +
                        "<li>Vim keys in your ~/.ideavimrc file using key mapping commands</li>" +
                        "<li>IDE action shortcuts in \"File | Settings | Keymap\"</li>" +
                        "<li>Vim or IDE handlers for conflicting shortcuts in <a href='#settings'>Vim Emulation</a> settings</li>" +
                        "</ul>", keymap.getPresentableName()),
          NotificationType.INFORMATION,
          new NotificationListener.Adapter() {
            @Override
            protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
              ShowSettingsUtil.getInstance().editConfigurable((Project)null, new VimEmulationConfigurable());
            }
          }).notify(null);
        manager.setActiveKeymap(keymap);
      }
      if (previousStateVersion > 0 && previousStateVersion < 4) {
        new Notification(
          VimPlugin.IDEAVIM_STICKY_NOTIFICATION_ID,
          VimPlugin.IDEAVIM_NOTIFICATION_TITLE,
          "The ~/.vimrc file is no longer read by default, use ~/.ideavimrc instead. You can read it from your " +
          "~/.ideavimrc using this command:<br/><br/>" +
          "<code>source ~/.vimrc</code>",
          NotificationType.INFORMATION).notify(null);
      }
    }
  }

  /**
   * This sets up some listeners so we can handle various events that occur
   */
  private void setupListeners() {
    final EventFacade eventFacade = EventFacade.getInstance();

    setupStatisticsReporter(eventFacade);

    DocumentManager.getInstance().addDocumentListener(new MarkGroup.MarkUpdater());
    DocumentManager.getInstance().addDocumentListener(new SearchGroup.DocumentSearchListener());

    eventFacade.addProjectManagerListener(new ProjectManagerAdapter() {
      @Override
      public void projectOpened(@NotNull final Project project) {
        eventFacade.addFileEditorManagerListener(project, new MotionGroup.MotionEditorChange());
        eventFacade.addFileEditorManagerListener(project, new FileGroup.SelectionCheck());
        eventFacade.addFileEditorManagerListener(project, new SearchGroup.EditorSelectionCheck());
      }
    });
  }

  /**
   * Reports statistics about installed IdeaVim and enabled Vim emulation.
   *
   * See https://github.com/go-lang-plugin-org/go-lang-idea-plugin/commit/5182ab4a1d01ad37f6786268a2fe5e908575a217
   */
  private void setupStatisticsReporter(@NotNull EventFacade eventFacade) {
    final Application application = ApplicationManager.getApplication();
    eventFacade.addEditorFactoryListener(new EditorFactoryAdapter() {
      @Override
      public void editorCreated(@NotNull EditorFactoryEvent event) {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
        final long lastUpdate = propertiesComponent.getOrInitLong(IDEAVIM_STATISTICS_TIMESTAMP_KEY, 0);
        final boolean outOfDate = lastUpdate == 0 ||
                                  System.currentTimeMillis() - lastUpdate > TimeUnit.DAYS.toMillis(1);
        if (outOfDate && isEnabled()) {
          application.executeOnPooledThread(new Runnable() {
            @Override
            public void run() {
              try {
                final String buildNumber = ApplicationInfo.getInstance().getBuild().asString();
                final String pluginId = IDEAVIM_PLUGIN_ID;
                final String version = URLEncoder.encode(getVersion(), CharsetToolkit.UTF8);
                final String os = URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION,
                                                    CharsetToolkit.UTF8);
                final String uid = UpdateChecker.getInstallationUID(PropertiesComponent.getInstance());
                final String url =
                  "https://plugins.jetbrains.com/plugins/list" +
                  "?pluginId=" + pluginId +
                  "&build=" + buildNumber +
                  "&pluginVersion=" + version +
                  "&os=" + os +
                  "&uuid=" + uid;
                PropertiesComponent.getInstance().setValue(IDEAVIM_STATISTICS_TIMESTAMP_KEY,
                                                           String.valueOf(System.currentTimeMillis()));
                HttpRequests.request(url).connect(new HttpRequests.RequestProcessor<Object>() {
                    @Override
                    public Object process(@NotNull HttpRequests.Request request) throws IOException {
                      LOG.info("Sending statistics: " + url);
                      try {
                        JDOMUtil.load(request.getInputStream());
                      }
                      catch (JDOMException e) {
                        LOG.warn(e);
                      }
                      return null;
                    }
                  }
                );
              }
              catch (IOException e) {
                LOG.warn(e);
              }
            }
          });
        }
      }
    }, application);
  }
}
