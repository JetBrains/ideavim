package com.maddyhome.idea.vim.option;

/*
* IdeaVim - A Vim emulator plugin for IntelliJ Idea
* Copyright (C) 2003 Rick Maddy
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU General Public License
* as published by the Free Software Foundation; either version 2
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, write to the Free Software
* Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
*/

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.maddyhome.idea.vim.ui.MorePanel;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 *
 */
public class Options
{
    public synchronized static Options getInstance()
    {
        if (ourInstance == null)
        {
            ourInstance = new Options();
        }
        return ourInstance;
    }

    public Option getOption(String name)
    {
        Option res = (Option)options.get(name);
        if (res == null)
        {
            res = (Option)abbrevs.get(name);
        }

        return res;
    }
    
    Collection allOptions()
    {
        return options.values();
    }

    Collection changedOptions()
    {
        ArrayList res = new ArrayList();
        for (Iterator iterator = options.values().iterator(); iterator.hasNext();)
        {
            Option option = (Option)iterator.next();
            if (!option.isDefault())
            {
                res.add(option);
            }
        }

        return res;
    }

    public boolean parseOptionLine(Editor editor, String args, boolean failOnBad)
    {
        if (args.length() == 0)
        {
            showOptions(editor, changedOptions());

            return true;
        }
        else if (args.equals("all"))
        {
            showOptions(editor, allOptions());

            return true;
        }
        else if (args.equals("all&"))
        {
            resetAllOptions();
            
            return true;
        }

        int error = 0;
        String option = "";
        StringTokenizer tokenizer = new StringTokenizer(args);
        while (tokenizer.hasMoreTokens())
        {
            String token = tokenizer.nextToken();
            if (token.endsWith("?"))
            {
                option = token.substring(0, token.length() - 1);
                Option opt = getOption(option);
                if (opt != null)
                {
                    ArrayList list = new ArrayList();
                    list.add(opt);
                    showOptions(editor, list);
                }
                else
                {
                    error = UNKNOWN_OPTION;
                }
            }
            else if (token.startsWith("no"))
            {
                option = token.substring(2);
                Option opt = getOption(option);
                if (opt != null)
                {
                    if (opt instanceof ToggleOption)
                    {
                        ((ToggleOption)opt).reset();
                    }
                    else
                    {
                        error = INVALID_ARGUMENT;
                    }
                }
                else
                {
                    error = UNKNOWN_OPTION;
                }
            }
            else if (token.startsWith("inv"))
            {
                option = token.substring(3);
                Option opt = getOption(option);
                if (opt != null)
                {
                    if (opt instanceof ToggleOption)
                    {
                        ((ToggleOption)opt).toggle();
                    }
                    else
                    {
                        error = INVALID_ARGUMENT;
                    }
                }
                else
                {
                    error = UNKNOWN_OPTION;
                }
            }
            else if (token.endsWith("!"))
            {
                option = token.substring(0, token.length() - 1);
                Option opt = getOption(option);
                if (opt != null)
                {
                    if (opt instanceof ToggleOption)
                    {
                        ((ToggleOption)opt).toggle();
                    }
                    else
                    {
                        error = INVALID_ARGUMENT;
                    }
                }
                else
                {
                    error = UNKNOWN_OPTION;
                }
            }
            else if (token.endsWith("&"))
            {
                option = token.substring(0, token.length() - 1);
                Option opt = getOption(option);
                if (opt != null)
                {
                    opt.resetDefault();
                }
                else
                {
                    error = UNKNOWN_OPTION;
                }
            }
            else
            {
                int eq = token.indexOf('=');
                if (eq == -1)
                {
                    eq = token.indexOf(':');
                }
                if (eq == -1)
                {
                    option = token;
                    Option opt = getOption(option);
                    if (opt != null)
                    {
                        if (opt instanceof ToggleOption)
                        {
                            ((ToggleOption)opt).set();
                        }
                        else
                        {
                            ArrayList list = new ArrayList();
                            list.add(opt);
                            showOptions(editor, list);
                        }
                    }
                    else
                    {
                        error = UNKNOWN_OPTION;
                    }
                }
                else
                {
                    if (eq > 0)
                    {
                        char op = token.charAt(eq - 1);
                        int end = eq;
                        if ("+-^".indexOf(op) != -1)
                        {
                            end--;
                        }
                        option = token.substring(0, end);
                        String value = token.substring(eq + 1);
                        Option opt = getOption(option);
                        if (opt != null)
                        {
                            option = token;
                            if (opt instanceof TextOption)
                            {
                                TextOption to = (TextOption)opt;
                                boolean res = true;
                                switch (op)
                                {
                                    case '+':
                                        res = to.append(value);
                                        break;
                                    case '-':
                                        res = to.remove(value);
                                        break;
                                    case '^':
                                        res = to.prepend(value);
                                        break;
                                    default:
                                        res = to.set(value);
                                }
                                if (!res)
                                {
                                    error = INVALID_ARGUMENT;
                                }
                            }
                            else
                            {
                                error = INVALID_ARGUMENT;
                            }
                        }
                        else
                        {
                            error = UNKNOWN_OPTION;
                        }
                    }
                    else
                    {
                        error = UNKNOWN_OPTION;
                    }
                }
            }

            if (failOnBad && error != 0)
            {
                break;
            }
        }

        if (editor != null)
        {
            
        }

        return error == 0;
    }

    private void resetAllOptions()
    {
        Collection opts = allOptions();
        for (Iterator iterator = opts.iterator(); iterator.hasNext();)
        {
            Option option = (Option)iterator.next();
            option.resetDefault();
        }
    }

    private void showOptions(Editor editor, Collection opts)
    {
        if (editor == null)
        {
            return;
        }

        ArrayList cols = new ArrayList();
        ArrayList extra = new ArrayList();
        for (Iterator iterator = opts.iterator(); iterator.hasNext();)
        {
            Option option = (Option)iterator.next();
            if (option.toString().length() > 19)
            {
                extra.add(option);
            }
            else
            {
                cols.add(option);
            }
        }

        Collections.sort(cols, new Option.NameSorter());
        Collections.sort(extra, new Option.NameSorter());

        String pad = "                    ";
        MorePanel panel = new MorePanel(editor);
        int width = panel.getDisplayWidth();
        if (width < 20)
        {
            width = 80;
        }
        int colCount = width / 20;
        int height = (int)Math.ceil((double)cols.size() / (double)colCount);
        int empty = cols.size() % colCount;
        empty = empty == 0 ? colCount : empty;

        logger.debug("width=" + width);
        logger.debug("colCount=" + colCount);
        logger.debug("height=" + height);

        StringBuffer res = new StringBuffer();
        res.append("--- Options ---\n");
        for (int h = 0; h < height; h++)
        {
            for (int c = 0; c < colCount; c++)
            {
                if (h == height - 1 && c >= empty)
                {
                    break;
                }

                int pos = c * height + h;
                if (c > empty)
                {
                    pos -= c - empty;
                }

                Option opt = (Option)cols.get(pos);
                String val = opt.toString();
                res.append(val);
                res.append(pad.substring(0, 20 - val.length()));
            }
            res.append("\n");
        }

        for (int i = 0; i < extra.size(); i++)
        {
            Option opt = (Option)extra.get(i);
            String val = opt.toString();
            int seg = (val.length() - 1) / width;
            for (int j = 0; j <= seg; j++)
            {
                res.append(val.substring(j * width, Math.min(j * width + width, val.length())));
                res.append("\n");
            }
        }

        panel.setText(res.toString());
        panel.setVisible(true);
    }

    private Options()
    {
        createDefaultOptions();
        loadVimrc();
    }

    private void loadVimrc()
    {
        String home = System.getProperty("user.home");
        if (home != null)
        {
            File rc = new File(home, ".vimrc");
            if (!rc.exists())
            {
                rc = new File(home, "_vimrc");
                if (!rc.exists())
                {
                    return;
                }
            }

            logger.debug("found vimrc at " + rc);

            try
            {
                BufferedReader br = new BufferedReader(new FileReader(rc));
                String line;
                while ((line = br.readLine()) != null)
                {
                    if (line.startsWith(":set") || line.startsWith("set"))
                    {
                        int pos = line.indexOf(' ');
                        parseOptionLine(null, line.substring(pos).trim(), false);
                    }
                }
            }
            catch (Exception e)
            {
            }
        }
    }

    private void createDefaultOptions()
    {
        addOption(new ToggleOption("gdefault", "gd", false));
        addOption(new ToggleOption("hlsearch", "hls", false));
        addOption(new ToggleOption("ignorecase", "ic", false));
        addOption(new ToggleOption("incsearch", "is", false));
        addOption(new ListOption("matchpairs", "mps", new String[] { "(:)", "{:}", "[:]" }));
        addOption(new ToggleOption("more", "more", true));
        addOption(new NumberOption("scroll", "scr", 0));
        addOption(new BoundStringOption("selection", "sel", "inclusive", new String[] { "old", "inclusive", "exclusive" }));
        addOption(new ToggleOption("smartcase", "scs", false));
        addOption(new NumberOption("undolevels", "ul", 1000, -1, Integer.MAX_VALUE));
        addOption(new ToggleOption("visualbell", "vb", false));
        addOption(new ToggleOption("wrapscan", "ws", true));
    }

    private void addOption(Option option)
    {
        options.put(option.getName(), option);
        abbrevs.put(option.getAbbreviation(), option);
    }

    private HashMap options = new HashMap();
    private HashMap abbrevs = new HashMap();

    private static Options ourInstance;

    private static final int UNKNOWN_OPTION = 1;
    private static final int INVALID_ARGUMENT = 2;

    private static Logger logger = Logger.getInstance(Options.class.getName());
}

