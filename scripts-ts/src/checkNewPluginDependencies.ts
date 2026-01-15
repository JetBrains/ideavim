#!/usr/bin/env tsx
/**
 * Checks JetBrains Marketplace for new plugins that depend on IdeaVim.
 *
 * Marketplace has an API to get all plugins that depend on our plugin.
 * Here we have a list of dependent plugins at some moment, and we check if something changed.
 * If so, we need to update our list of plugins.
 *
 * This script makes no actions and is aimed to notify devs when they need to update the list.
 */

const knownPlugins = new Set([
  "IdeaVimExtension",
  "github.zgqq.intellij-enhance",
  "org.jetbrains.IdeaVim-EasyMotion",
  "io.github.mishkun.ideavimsneak",
  "eu.theblob42.idea.whichkey",
  "com.github.copilot",
  "com.github.dankinsoid.multicursor",
  "com.joshestein.ideavim-quickscope",

  "ca.alexgirard.HarpoonIJ",
  "me.kyren223.harpoonforjb", // https://plugins.jetbrains.com/plugin/23771-harpoonforjb
  "com.github.erotourtes.harpoon", // https://plugins.jetbrains.com/plugin/21796-harpooner
  "me.kyren223.trident", // https://plugins.jetbrains.com/plugin/23818-trident

  "com.protoseo.input-source-auto-converter",

  // "cc.implicated.intellij.plugins.bunny", // Not included - unclear purpose
  "com.julienphalip.ideavim.peekaboo", // https://plugins.jetbrains.com/plugin/25776-vim-peekaboo
  "com.julienphalip.ideavim.switch", // https://plugins.jetbrains.com/plugin/25899-vim-switch
  "com.julienphalip.ideavim.functiontextobj", // https://plugins.jetbrains.com/plugin/25897-vim-functiontextobj
  "com.miksuki.HighlightCursor", // https://plugins.jetbrains.com/plugin/26743-highlightcursor
  "com.ugarosa.idea.edgemotion", // https://plugins.jetbrains.com/plugin/27211-edgemotion

  "cn.mumukehao.plugin",

  "com.magidc.ideavim.anyObject",
  "dev.ghostflyby.ideavim.toggleIME",
  "com.magidc.ideavim.dial",

  "com.yelog.ideavim.cmdfloat", // https://plugins.jetbrains.com/plugin/28732-vim-cmdfloat
]);

async function getPluginLinkByXmlId(xmlId: string): Promise<string | null> {
  try {
    const response = await fetch(
      `https://plugins.jetbrains.com/api/plugins/intellij/${xmlId}`
    );
    if (!response.ok) {
      return null;
    }
    const data = await response.json();
    const link = data.link;
    return link ? `https://plugins.jetbrains.com${link}` : null;
  } catch {
    return null;
  }
}

async function main(): Promise<void> {
  console.log("Checking for new plugin dependencies on IdeaVim...");

  const response = await fetch(
    "https://plugins.jetbrains.com/api/plugins/?dependency=IdeaVIM&includeOptional=true"
  );

  if (!response.ok) {
    throw new Error(`Failed to fetch plugins: ${response.status}`);
  }

  const plugins: string[] = await response.json();
  const pluginSet = new Set(plugins);

  console.log(`Found ${plugins.length} plugins depending on IdeaVim`);
  console.log(plugins);

  // Find new plugins not in our known list
  const newPlugins: string[] = [];
  for (const plugin of pluginSet) {
    if (!knownPlugins.has(plugin)) {
      newPlugins.push(plugin);
    }
  }

  if (newPlugins.length > 0) {
    console.log("\nUnregistered plugins found:");

    const pluginDetails = await Promise.all(
      newPlugins.map(async (plugin) => {
        const link = await getPluginLinkByXmlId(plugin);
        return `${plugin} (${link ?? "Can't find plugin link"})`;
      })
    );

    for (const detail of pluginDetails) {
      console.log(`  - ${detail}`);
    }

    throw new Error(
      `Unregistered plugins:\n${pluginDetails.join("\n")}`
    );
  }

  console.log("\nNo new plugins found. All plugins are registered.");
}

main().catch((error) => {
  console.error("Error:", error.message);
  process.exit(1);
});
