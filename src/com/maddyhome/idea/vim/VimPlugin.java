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
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.maddyhome.idea.vim;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PermanentInstallationID;
import com.intellij.openapi.components.*;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.ex.KeymapManagerEx;
import com.intellij.openapi.keymap.impl.DefaultKeymap;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.io.HttpRequests;
import com.maddyhome.idea.vim.ex.CommandParser;
import com.maddyhome.idea.vim.ex.vimscript.VimScriptParser;
import com.maddyhome.idea.vim.extension.VimExtensionRegistrar;
import com.maddyhome.idea.vim.group.*;
import com.maddyhome.idea.vim.group.copy.PutGroup;
import com.maddyhome.idea.vim.group.copy.YankGroup;
import com.maddyhome.idea.vim.group.visual.VisualMotionGroup;
import com.maddyhome.idea.vim.helper.MacKeyRepeat;
import com.maddyhome.idea.vim.listener.VimListenerManager;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.ui.ExEntryPanel;
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
 */
@State(name = "VimSettings", storages = {@Storage("$APP_CONFIG$/vim_settings.xml")})
public class VimPlugin implements BaseComponent, PersistentStateComponent<Element>, Disposable {
  private static final String IDEAVIM_COMPONENT_NAME = "VimPlugin";
  private static final String IDEAVIM_PLUGIN_ID = "IdeaVIM";
  private static final String IDEAVIM_STATISTICS_TIMESTAMP_KEY = "ideavim.statistics.timestamp";
  public static final int STATE_VERSION = 5;

  private static long lastBeepTimeMillis;

  private boolean error = false;

  private int previousStateVersion = 0;
  private String previousKeyMap = "";

  // It is enabled by default to avoid any special configuration after plugin installation
  private boolean enabled = true;

  private static final Logger LOG = Logger.getInstance(VimPlugin.class);

  @NotNull
  @Override
  public String getComponentName() {
    return IDEAVIM_COMPONENT_NAME;
  }


  @NotNull private final VimState state = new VimState();

  // [VERSION UPDATE] 193+ replace with com.intellij.openapi.components.PersistentStateComponent.initializeComponent
  @Override
  public void initComponent() {
    LOG.debug("initComponent");

    if (enabled) turnOnPlugin();

    LOG.debug("done");
  }

  @Override
  public void dispose() {
    LOG.debug("disposeComponent");
    turnOffPlugin();
    LOG.debug("done");
  }

  /**
   * @return NotificationService as applicationService if project is null and projectService otherwise
   */
  @NotNull
  public static NotificationService getNotifications(@Nullable Project project) {
    if (project == null) {
      return ServiceManager.getService(NotificationService.class);
    } else {
      return ServiceManager.getService(project, NotificationService.class);
    }
  }

  @NotNull
  public static VimState getVimState() {
    return getInstance().state;
  }


  @NotNull
  public static MotionGroup getMotion() {
    return ServiceManager.getService(MotionGroup.class);
  }

  /**
   * Reports statistics about installed IdeaVim and enabled Vim emulation.
   * <p>
   * See https://github.com/go-lang-plugin-org/go-lang-idea-plugin/commit/5182ab4a1d01ad37f6786268a2fe5e908575a217
   */
  public static void statisticReport() {
    final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance();
    final long lastUpdate = propertiesComponent.getOrInitLong(IDEAVIM_STATISTICS_TIMESTAMP_KEY, 0);
    final boolean outOfDate =
      lastUpdate == 0 || System.currentTimeMillis() - lastUpdate > TimeUnit.DAYS.toMillis(1);
    if (outOfDate && isEnabled()) {
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        try {
          final String buildNumber = ApplicationInfo.getInstance().getBuild().asString();
          final String version = URLEncoder.encode(getVersion(), CharsetToolkit.UTF8);
          final String os =
            URLEncoder.encode(SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION, CharsetToolkit.UTF8);
          final String uid = PermanentInstallationID.get();
          final String url = "https://plugins.jetbrains.com/plugins/list" +
                             "?pluginId=" + IDEAVIM_PLUGIN_ID +
                             "&build=" +
                             buildNumber +
                             "&pluginVersion=" +
                             version +
                             "&os=" +
                             os +
                             "&uuid=" +
                             uid;
          PropertiesComponent.getInstance()
            .setValue(IDEAVIM_STATISTICS_TIMESTAMP_KEY, String.valueOf(System.currentTimeMillis()));
          HttpRequests.request(url).connect(request -> {
            LOG.info("Sending statistics: " + url);
            try {
              JDOMUtil.load(request.getInputStream());
            }
            catch (JDOMException e) {
              LOG.warn(e);
            }
            return null;
          });
        }
        catch (IOException e) {
          LOG.warn(e);
        }
      });
    }
  }

  @NotNull
  public static ChangeGroup getChange() {
    return ServiceManager.getService(ChangeGroup.class);
  }

  @NotNull
  public static CommandGroup getCommand() {
    return ServiceManager.getService(CommandGroup.class);
  }

  @NotNull
  public static MarkGroup getMark() {
    return ServiceManager.getService(MarkGroup.class);
  }

  @NotNull
  public static RegisterGroup getRegister() {
    return ServiceManager.getService(RegisterGroup.class);
  }

  @NotNull
  public static FileGroup getFile() {
    return ServiceManager.getService(FileGroup.class);
  }

  @NotNull
  public static SearchGroup getSearch() {
    return ServiceManager.getService(SearchGroup.class);
  }

  @NotNull
  public static ProcessGroup getProcess() {
    return ServiceManager.getService(ProcessGroup.class);
  }

  @NotNull
  public static MacroGroup getMacro() {
    return ServiceManager.getService(MacroGroup.class);
  }

  @NotNull
  public static DigraphGroup getDigraph() {
    return ServiceManager.getService(DigraphGroup.class);
  }

  @NotNull
  public static HistoryGroup getHistory() {
    return ServiceManager.getService(HistoryGroup.class);
  }

  @NotNull
  public static KeyGroup getKey() {
    return ServiceManager.getService(KeyGroup.class);
  }

  @NotNull
  public static WindowGroup getWindow() {
    return ServiceManager.getService(WindowGroup.class);
  }

  @NotNull
  public static EditorGroup getEditor() {
    return ServiceManager.getService(EditorGroup.class);
  }

  @NotNull
  public static VisualMotionGroup getVisualMotion() {
    return ServiceManager.getService(VisualMotionGroup.class);
  }

  @NotNull
  public static YankGroup getYank() {
    return ServiceManager.getService(YankGroup.class);
  }

  @NotNull
  public static PutGroup getPut() {
    return ServiceManager.getService(PutGroup.class);
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

    getKey().saveData(element);
    getEditor().saveData(element);
    this.state.saveData(element);

    return element;
  }

  @NotNull
  private static NotificationService getNotifications() {
    return getNotifications(null);
  }

  private boolean ideavimrcRegistered = false;

  private void registerIdeavimrc() {
    if (ideavimrcRegistered) return;
    ideavimrcRegistered = true;

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      final File ideaVimRc = VimScriptParser.findIdeaVimRc();
      if (ideaVimRc != null) {
        VimScriptParser.executeFile(ideaVimRc);
      }
    }
  }

  @NotNull
  public static PluginId getPluginId() {
    return PluginId.getId(IDEAVIM_PLUGIN_ID);
  }

  // [VERSION UPDATE] 193+ remove suppress
  @SuppressWarnings({"MissingRecentApi", "UnstableApiUsage"})
  @NotNull
  public static String getVersion() {
    final IdeaPluginDescriptor plugin = PluginManager.getPlugin(getPluginId());
    if (!ApplicationManager.getApplication().isInternal()) {
      return plugin != null ? plugin.getVersion() : "SNAPSHOT";
    }
    else {
      return "INTERNAL" + (plugin != null ? " - " + plugin.getVersion() : "");
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

    VimStatusBar.INSTANCE.update();
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
    else if (!OptionsManager.INSTANCE.getVisualbell().isSet()) {
      // Vim only allows a beep once every half second - :help 'visualbell'
      final long currentTimeMillis = System.currentTimeMillis();
      if (currentTimeMillis - lastBeepTimeMillis > 500) {
        Toolkit.getDefaultToolkit().beep();
        lastBeepTimeMillis = currentTimeMillis;
      }
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
    return ApplicationManager.getApplication().getComponent(VimPlugin.class);
  }

  private void turnOnPlugin() {
    ApplicationManager.getApplication().invokeLater(this::updateState);

    getEditor().turnOn();
    getSearch().turnOn();
    VimListenerManager.INSTANCE.turnOn();

    // Register vim actions in command mode
    RegisterActions.registerActions();

    // Register ex handlers
    CommandParser.getInstance().registerHandlers();

    // Register extensions
    VimExtensionRegistrar.registerExtensions();

    // Execute ~/.ideavimrc
    registerIdeavimrc();
  }

  private void turnOffPlugin() {
    KeyHandler.getInstance().fullReset(null);

    getEditor().turnOff();
    getSearch().turnOff();
    VimListenerManager.INSTANCE.turnOff();
    ExEntryPanel.fullReset();
  }

  private boolean stateUpdated = false;

  private void updateState() {
    if (stateUpdated) return;
    if (isEnabled() && !ApplicationManager.getApplication().isUnitTestMode()) {
      stateUpdated = true;
      if (SystemInfo.isMac) {
        final MacKeyRepeat keyRepeat = MacKeyRepeat.getInstance();
        final Boolean enabled = keyRepeat.isEnabled();
        final Boolean isKeyRepeat = getEditor().isKeyRepeat();
        if ((enabled == null || !enabled) && (isKeyRepeat == null || isKeyRepeat)) {
          if (VimPlugin.getNotifications().enableRepeatingMode() == Messages.YES) {
            getEditor().setKeyRepeat(true);
            keyRepeat.setEnabled(true);
          }
          else {
            getEditor().setKeyRepeat(false);
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
        VimPlugin.getNotifications().specialKeymap(keymap, new NotificationListener.Adapter() {
          @Override
          protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
            ShowSettingsUtil.getInstance().editConfigurable((Project)null, new VimEmulationConfigurable());
          }
        });
        manager.setActiveKeymap(keymap);
      }
      if (previousStateVersion > 0 && previousStateVersion < 4) {
        VimPlugin.getNotifications().noVimrcAsDefault();
      }
    }
  }

  @Override
  public void loadState(@NotNull final Element element) {
    LOG.debug("Loading state");

    // Restore whether the plugin is enabled or not
    Element state = element.getChild("state");
    if (state != null) {
      try {
        previousStateVersion = Integer.parseInt(state.getAttributeValue("version"));
      }
      catch (NumberFormatException ignored) {
      }
      enabled = Boolean.parseBoolean(state.getAttributeValue("enabled"));
      previousKeyMap = state.getAttributeValue("keymap");
    }

    if (previousStateVersion > 0 && previousStateVersion < 5) {
      // Migrate settings from 4 to 5 version
      getMark().readData(element);
      getRegister().readData(element);
      getSearch().readData(element);
      getHistory().readData(element);
    }
    getKey().readData(element);
    getEditor().readData(element);
    this.state.readData(element);
  }
}
