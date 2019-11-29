package com.maddyhome.idea.vim.extension;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
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

  private static Set<String> registeredExtensions = new HashSet<>();
  private static boolean extensionRegistered = false;

  public static void registerExtensions() {
    if (extensionRegistered) return;
    extensionRegistered = true;

    // TODO: [VERSION UPDATE] since 191 use
    //  ExtensionPoint.addExtensionPointListener(ExtensionPointListener<T>, boolean, Disposable)
    VimExtension.EP_NAME.getPoint(null).addExtensionPointListener(new ExtensionPointListener<VimExtension>() {
      @Override
      public void extensionAdded(@NotNull VimExtension extension, @NotNull PluginDescriptor pluginDescriptor) {
        registerExtension(extension);
      }
    });
  }

  synchronized private static void registerExtension(@NotNull VimExtension extension) {
    String name = extension.getName();

    if (registeredExtensions.contains(name)) return;

    registeredExtensions.add(name);
    ToggleOption option = new ToggleOption(name, name, false);
    option.addOptionChangeListener(event -> {
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

  private static Logger logger = Logger.getInstance(VimExtensionRegistrar.class);
}
