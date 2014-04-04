/**
 * IdeaVim command index.
 *
 *
 * 2. Normal mode
 *
 * tag                      action
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |i|                      {@link com.maddyhome.idea.vim.action.change.insert.InsertBeforeCursorAction}
 * |<Insert>|
 * |v|                      {@link com.maddyhome.idea.vim.action.motion.visual.VisualToggleCharacterModeAction}
 *
 *
 * 5. Ex commands
 *
 * tag                      handler
 * ---------------------------------------------------------------------------------------------------------------------
 *
 * |:map|                   {@link com.maddyhome.idea.vim.ex.handler.MapHandler}
 * |:nmap|
 * |:vmap|
 * |:omap|
 * |:map!|
 * |:imap|
 * |:cmap|
 * |:noremap|               {@link com.maddyhome.idea.vim.ex.handler.NonRecursiveMapHandler}
 * |:nnoremap|
 * |:vnoremap|
 * |:onoremap|
 * |:inoremap|
 * |:cnoremap|
 *
 * @see :help index.
 *
 * @author vlan
 */
package com.maddyhome.idea.vim;
