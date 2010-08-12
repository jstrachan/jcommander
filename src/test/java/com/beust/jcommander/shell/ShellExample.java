/**
 * Copyright (C) 2010 the original author or authors.
 * See the notice.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.beust.jcommander.shell;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.command.CommandAdd;
import com.beust.jcommander.command.CommandCommit;

import java.util.HashMap;

/**
 * <p>
 * </p>
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class ShellExample {

  static class TestShell extends Shell {

    public TestShell() {
      prompt = "\u001B[1m"+getShellName()+">\u001B[0m ";
    }

    @Override
    public  String getShellName() {
      return "example";
    }

    @Override
    public  String[] getDisplayedCommands() {
      return new String[]{"commit","add"};
    }

    @Override
    public JCommander createSubCommand(String name) {
      if( "add".equals(name) ) {
        return new JCommander(new CommandAdd());
      }
      if( "commit".equals(name) ) {
        return new JCommander(new CommandCommit());
      }
      if( "?".equals(name) ) {
        return new JCommander(new Runnable(){
          public void run() {
            StringBuilder sb = new StringBuilder();
            usage(sb);
            System.out.println(sb);
          }
        });
      }
      return null;
    }

  }

  static public void main(String args[]) {
    Help help = new Help();
    TestShell shell = new TestShell();
    JCommander jc = new JCommander(new Object[]{shell,help});
    jc.parse(args);
    if( help.help) {
      jc.usage();
    } else {
      shell.run(jc);
    }
  }

}
