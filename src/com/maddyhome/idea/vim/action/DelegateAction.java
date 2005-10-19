/*
 * Created by IntelliJ IDEA.
 * User: rmaddy
 * Date: Dec 17, 2004
 * Time: 2:52:52 PM
 */
package com.maddyhome.idea.vim.action;

import com.intellij.openapi.actionSystem.AnAction;

public interface DelegateAction
{
    void setOrigAction(AnAction origAction);
    AnAction getOrigAction();
}