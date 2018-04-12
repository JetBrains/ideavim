package com.maddyhome.idea.vim.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

public class ShellCommandHelper {
    public static String getResultFromShell(StringBuilder script) {
        String result = "";
        try {
            ProcessBuilder pb = new ProcessBuilder("/bin/bash");
            Process bash = pb.start();

            PrintStream ps = new PrintStream(bash.getOutputStream());
            ps.println(script);
            ps.close();

            BufferedReader br = new BufferedReader(new InputStreamReader(bash.getInputStream()));

            String line;
            while (null != (line = br.readLine())) {
                result += "> " + line + "\n";
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
