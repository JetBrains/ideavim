<html>
<head>
<title>IdeaVIM - Frequently Asked Questions</title>
</head>
<body>
<?php include 'header.php'; ?>

<h1>Frequently Asked Questions</h1>

<a href="#noplugin">
The <code>Tools</code> menu doesn't have a new option called <code>VIM Emulator</code>
</a><br>
<a href="#nokeymap">
There is no "vim" keymap listed after selecting the <code>Options|Keymaps</code> menu
</a><br>
<a href="#pluginoff">
Everything is installed properly but I just can't seem to enter VIM commands
</a><br>
<a href="#turnoff">
How do I turn off the VIM plugin once it is installed?
</a><br>
<!--
<a href="#">
</a><br>
-->

<dl>
<dt>
<a name="noplugin"></a>
The <code>Tools</code> menu doesn't have a new option called <code>VIM Emulator</code>
</dt>
<dd>
This most likely means you did not properly copy the plugin jar file into the plugins directory of IDEA. Please make
sure you have copied the IdeaVIM.jar file from the installation download into the plugins directory.
</dd>
<dt>
<a name="nokeymap"></a>
There is no "vim" keymap listed after selecting the <code>Options|Keymaps</code> menu
</dt>
<dd>
Have you verified that you copied the vim.xml file from the installation download into the
&lt;HOME&gt;/.IntelliJIdea/config/keymaps directory?
</dd>
<dt>
<a name="pluginoff"></a>
Everything is installed properly but I just can't seem to enter VIM commands
</dt>
<dd>
Select the <code>Tools</code> menu and make sure the menu <code>VIM Emulator</code> has a checkmark next to it. Also
select the <code>Options|Keymaps</code> menu and make sure the "vim" keymap says "active" next to it. If not, select
the "vim" keymap and press the <code>Set Active</code> button.
</dd>
<dt>
<a name="turnoff"></a>
How do I turn off the VIM plugin once it is installed?
</dt>
<dd>
If you wish to temporarily turn off the VIM plugin you must select the <code>Tools</code> menu an uncheck the
<code>VIM Emulator</code> menu. You must also select the <code>Options|Keymaps</code> menu and make some other
keymap active other than "vim". Reverse this process to turn the VIM emulator back on. These settings will be saved
and the next time you start IDEA, it will be in the same state as you left it.
</dd>
<!--
<dt>
<a name=""></a>
</dt>
<dd>
</dd>
-->
</dl>

</body>
</html>