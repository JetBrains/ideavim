<html>
<head>
    <title>VIM Emulator Plugin for IntelliJ IDEA</title>
</head>
<body>
<?php include 'header.php'; ?>

<h1>VIM Emulator Plugin for IntelliJ IDEA</h1>

<h3>Introduction</h3>

<p>
<a href="http://www.intellij.com">IntelliJ IDEA</a> is an outstanding IDE for editing Java source code and other
related files. However, it lacks one
important feature - <code>vi</code> style commands for editing. I've been using <code>vi</code> and
<a href="http://www.vim.org">VIM</a>
for about 20 years. I'm used to it. I know the command keyboard shortcuts are cryptic and stange but I've been using
it so long I just know how to use them and I find I am very efficient with them.
</p>
<p>
IDEA makes my Java editing even more efficient. Now, if I could only combine the features of IDEA with the quick and
easy editing of VIM, life would be grand. A quick search revealed that there wasn't a feature filled VIM plugin so I
took it upon myself to write one. This project is the result of my desire to keep using my old keyboard habits.
</p>
<p>
The goal of this plugin is to support as much VIM functionality as makes sense within the scope of IDEA. The plugin
was actually written in IDEA using the VIM plugin once there was enough basic editing support. For the curious, the
plugin is being written without any reference to the VIM source code (except for the regular expression handling).
I'm basically using the excellent VIM documentation and VIM itself as a reference to verify correct behavior.
</p>
<p>
This plugin is meant for developers that already know, and probably love, vi/VIM. I make no attempt to teach users
how to use the VIM commands and you will not find any sort of help from within IDEA on what the key mappings are.
Within the pages of this website however, you will find a reference of all working commands.
</p>

<h3>Installation</h3>

<h4>Idea 3.x</h4>

Once you have downloaded the binary release or built it from source, you have three simple steps to perform to begin
using IDEA in VIM Emulation mode:

<ol>
<li>Copy <code>IdeaVIM.jar</code> to the plugins directory. This is at <code>&lt;IDEA_HOME&gt;/plugins</code> where
    <code>IDEA_HOME</code> is IDEA installation directory. Newer versions of the Aurora builds should use the
    plugin manager to download the latest version of the plugin.</li>
<li>Copy <code>vim.xml</code> to <code>&lt;HOME&gt;/.IntelliJIdea/config/keymaps</code> where <code>HOME</code>
    is your home directory on Unix or <code>C:\Documents&nbsp;and&nbsp;Settings\&lt;user&gt;</code> on Windows.
    Create the <code>keymaps</code> directory if it does not exist.</li>
<li>Restart IDEA and then select the <code>Options|Keymaps</code> menu. Select the <code>vim</code> keymap and make
    it the active keymap by pressing the <code>Set Active</code> button.</li>
</ol>

<h4>Idea Aurora and 4.x</h4>

<ol>
<li>Use the plugin manager built into Idea to download and install the IdeaVIM
plugin. This should be found under the Editor section.</li>
<li>If this is the first time you have ever installed this plugin you must
install the special VIM key mapping. Copy
<code>&lt;IDEA_HOME&gt;/plugins/IdeaVIM/vim.xml</code> to
<code>&lt;HOME&gt;/.IntelliJIdea/config/keymaps</code> where <code>HOME</code>
is your home directory.
Create the <code>keymaps</code> directory if it does not exist.
</li>
<li>Restart IDEA and then select the <code>Options|Keymaps</code> menu. Select the <code>vim</code> keymap and make
    it the active keymap by pressing the <code>Set Active</code> button.</li>
</ol>

You only need to do these steps the first time you install the plugin. You will find a new menu under
<code>Tools</code> named <code>VIM Emulator</code>. It should be checked by default. If this menu is not present you
did not copy the jar file to the plugins directory. If the <code>vim</code> keymap was not present then you did not
copy the <code>vim.xml</code> file to the correct directory.

<h3>Download</h3>

You may obtain the plugin by visiting the files page on
<a href="http://sourceforge.net/project/showfiles.php?group_id=79039">SourceForge</a>

<h3>Versions</h3>

Version 0.4.x was used with IDEA 3.x.
<br>
Version 0.6.x was used with Aurora.
<br>
Version 0.7.x is used with IDEA 4.x

<hr>

<a href="http://sourceforge.net">
    <img src="http://sourceforge.net/sflogo.php?group_id=79039&type=5" width="210" height="62" border="0"
        alt="SourceForge.net Logo">
</a>
</body>
</html>
