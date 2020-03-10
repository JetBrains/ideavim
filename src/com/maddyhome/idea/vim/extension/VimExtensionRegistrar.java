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

package com.maddyhome.idea.vim.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.maddyhome.idea.vim.key.MappingOwner;
import com.maddyhome.idea.vim.option.OptionsManager;
import com.maddyhome.idea.vim.option.ToggleOption;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * TODO [VERSION UPDATE] this file cannot be converted to kt before 192 because of nullabilities problems in
 * [ExtensionPointListener]. (In previous versions of IJ pluginDescriptor was nullable)
 */
public class VimExtensionRegistrar {

  private static final Set<String> registeredExtensions = new HashSet<>();
  private static boolean extensionRegistered = false;

  private static final Logger logger = Logger.getInstance(VimExtensionRegistrar.class);

  public static void registerExtensions() {
    if (extensionRegistered) return;
    extensionRegistered = true;

    // TODO: [VERSION UPDATE] since 191 use
    //  ExtensionPoint.addExtensionPointListener(ExtensionPointListener<T>, boolean, Disposable)
    //noinspection deprecation
    VimExtension.EP_NAME.getPoint(null).addExtensionPointListener(new ExtensionPointListener<VimExtension>() {
      @Override
      public void extensionAdded(@NotNull VimExtension extension, PluginDescriptor pluginDescriptor) {
        registerExtension(extension);
      }

      @Override
      public void extensionRemoved(@NotNull VimExtension extension, PluginDescriptor pluginDescriptor) {
        unregisterExtension(extension);
      }
    });
  }

  private static synchronized void registerExtension(@NotNull VimExtension extension) {
    String name = extension.getName();

    if (registeredExtensions.contains(name)) return;

    registeredExtensions.add(name);
    ToggleOption option = new ToggleOption(name, name, false);
    option.addOptionChangeListener((oldValue, newValue) -> {
      for (VimExtension extensionInListener : VimExtension.EP_NAME.getExtensionList()) {
        if (name.equals(extensionInListener.getName())) {
          if (OptionsManager.INSTANCE.isSet(name)) {
            extensionInListener.init();
            logger.info("IdeaVim extension '" + name + "' initialized");
          }
          else {
            extensionInListener.dispose();
          }
        }
      }
    });
    OptionsManager.INSTANCE.addOption(option);
  }

  private static synchronized void unregisterExtension(@NotNull VimExtension extension) {
    String name = extension.getName();

    if (!registeredExtensions.contains(name)) return;

    registeredExtensions.remove(name);
    extension.dispose();
    OptionsManager.INSTANCE.removeOption(name);
    MappingOwner.Plugin.Companion.remove(name);
    logger.info("IdeaVim extension '" + name + "' disposed");
  }
}
