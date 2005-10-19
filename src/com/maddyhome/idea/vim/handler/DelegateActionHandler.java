/*
 * Created by IntelliJ IDEA.
 * User: rmaddy
 * Date: Dec 16, 2004
 * Time: 4:15:26 PM
 */
package com.maddyhome.idea.vim.handler;

import com.intellij.openapi.actionSystem.AnAction;

public interface DelegateActionHandler
{
    void setOrigAction(AnAction origAction);
    AnAction getOrigAction();
}