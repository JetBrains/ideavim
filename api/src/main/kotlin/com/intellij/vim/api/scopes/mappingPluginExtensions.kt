/*
 * Copyright 2003-2025 The IdeaVim authors
 *
 * Use of this source code is governed by an MIT-style
 * license that can be found in the LICENSE.txt file or at
 * https://opensource.org/licenses/MIT.
 */

package com.intellij.vim.api.scopes

import com.intellij.vim.api.VimApi

/**
 * Create a mapping for the plugin in normal, visual, select, and operator-pending modes.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * map from intermediateMappingLabel
 * noremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `map from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.mapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    // Check each mode individually and only add mapping if it doesn't exist for that mode
    if (!nhasmapto(intermediateMappingLabel)) {
      nmap(from, intermediateMappingLabel)
    }
    if (!xhasmapto(intermediateMappingLabel)) {
      xmap(from, intermediateMappingLabel)
    }
    if (!shasmapto(intermediateMappingLabel)) {
      smap(from, intermediateMappingLabel)
    }
    if (!ohasmapto(intermediateMappingLabel)) {
      omap(from, intermediateMappingLabel)
    }
  }

  noremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in normal mode.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * nmap from intermediateMappingLabel
 * nnoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `nmap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.nmapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    if (!nhasmapto(intermediateMappingLabel)) {
      nmap(from, intermediateMappingLabel)
    }
  }

  nnoremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in visual and select modes.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * vmap from intermediateMappingLabel
 * vnoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `vmap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.vmapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    // Check each mode individually and only add mapping if it doesn't exist for that mode
    if (!xhasmapto(intermediateMappingLabel)) {
      xmap(from, intermediateMappingLabel)
    }
    if (!shasmapto(intermediateMappingLabel)) {
      smap(from, intermediateMappingLabel)
    }
  }

  vnoremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in visual mode.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * xmap from intermediateMappingLabel
 * xnoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `xmap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.xmapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    if (!xhasmapto(intermediateMappingLabel)) {
      xmap(from, intermediateMappingLabel)
    }
  }

  xnoremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in select mode.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * smap from intermediateMappingLabel
 * snoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `smap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.smapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    if (!shasmapto(intermediateMappingLabel)) {
      smap(from, intermediateMappingLabel)
    }
  }

  snoremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in operator-pending mode.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * omap from intermediateMappingLabel
 * onoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `omap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.omapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    if (!ohasmapto(intermediateMappingLabel)) {
      omap(from, intermediateMappingLabel)
    }
  }

  onoremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in insert mode.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * imap from intermediateMappingLabel
 * inoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `imap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.imapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    if (!ihasmapto(intermediateMappingLabel)) {
      imap(from, intermediateMappingLabel)
    }
  }

  inoremap(intermediateMappingLabel, action)
}

/**
 * Create a mapping for the plugin in command-line mode.
 *
 * Generally, we make a mapping from the [from] keys to the [action]. But following the practices of Vim mappings
 *   for plugins.
 * See the documentation about mappings for details.
 *
 * [intermediateMappingLabel] is an intermediate mapping. It must start with `<Plug>`. So, two mappings will be created:
 * ```
 * cmap from intermediateMappingLabel
 * cnoremap intermediateMappingLabel action
 * ```
 *
 * If [keepDefaultMapping] is false the `cmap from intermediateMappingLabel` part of the mapping will not be registered.
 */
fun MappingScope.cmapPluginAction(
  from: String,
  intermediateMappingLabel: String,
  keepDefaultMapping: Boolean,
  action: suspend VimApi.() -> Unit,
) {
  require(intermediateMappingLabel.startsWith("<Plug>")) { "Intermediate mapping label must start with <Plug>" }

  if (keepDefaultMapping) {
    if (!chasmapto(intermediateMappingLabel)) {
      cmap(from, intermediateMappingLabel)
    }
  }

  cnoremap(intermediateMappingLabel, action)
}
