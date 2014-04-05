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
 * |:imap|
 * |:cmap|
 * |:noremap|
 * |:nnoremap|
 * |:vnoremap|
 * |:onoremap|
 * |:inoremap|
 * |:cnoremap|
 * |:source|                {@link com.maddyhome.idea.vim.ex.handler.SourceHandler}
 *
 * @see :help index.
 *
 * @author vlan
 */
package com.maddyhome.idea.vim;
