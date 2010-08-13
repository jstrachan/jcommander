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

package com.beust.jcommander.command;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.shell.CompletionResult;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

public class TabCompletionTest {

  @Test
  public void tabCompleteNoCommand() {
    assertTabCompletes(new String[]{}, 0, Arrays.asList("add", "commit"));
  }

  @Test
  public void tabCompleteStartOfCommand() {
    assertTabCompletes(new String[]{"c"}, 0, Arrays.asList("ommit"));
  }

  @Test
  public void tabCompleteCommandOption() {
    assertTabCompletes(new String[]{"commit", "--a"}, 1, Arrays.asList("mend", "uthor"));
  }

  /**
   * Asserts that when the given arguments are being typed on the console and the cursor is on the given argument
   * (or beyond the current arguments to indicate that the user is typing a new argument) then assert that
   * the expected tab completion matches are returned
   */
  protected void assertTabCompletes(String[] args, int cursorArgument, List<String> expected) {
    CommandMain cm = new CommandMain();
    JCommander jc = new JCommander(cm);
    CommandAdd add = new CommandAdd();
    jc.addCommand("add", add);
    CommandCommit commit = new CommandCommit();
    jc.addCommand("commit", commit);


    String prefix = null;
    if (args.length > 0 && cursorArgument < args.length) {
      prefix = args[cursorArgument];
    }
    CompletionResult results = new CompletionResult(prefix);
    jc.tabComplete(args, cursorArgument, results);
    Assert.assertEquals(results.getResults(), expected);
  }

  public static void main(String[] args) {
    new TabCompletionTest().tabCompleteNoCommand();
  }
}
