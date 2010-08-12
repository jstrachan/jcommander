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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;

public class CommandTest {
  @Test
  public void commandTest1() {
    CommandMain cm = new CommandMain();
    JCommander jc = new JCommander(cm);
    CommandAdd add = new CommandAdd();
    jc.addCommand("add", add);
    CommandCommit commit = new CommandCommit();
    jc.addCommand("commit", commit);
    jc.parse("add", "-i", "A.java");

    Assert.assertEquals(jc.getParsedCommand(), "add");
    Assert.assertEquals(add.interactive.booleanValue(), true);
    Assert.assertEquals(add.patterns, Arrays.asList("A.java"));
  }

  @Test
  public void commandTest2() {
    CommandMain cm = new CommandMain();
    JCommander jc = new JCommander(cm);
    CommandAdd add = new CommandAdd();
    jc.addCommand("add", add);
    CommandCommit commit = new CommandCommit();
    jc.addCommand("commit", commit);
    jc.parse("-v", "commit", "--amend", "--author=cbeust", "A.java", "B.java");

    jc.setProgramName("TestCommander");
    jc.usage();
    jc.usage("add");
    jc.usage("commit");

    Assert.assertTrue(cm.verbose);
    Assert.assertEquals(jc.getParsedCommand(), "commit");
    Assert.assertTrue(commit.amend);
    Assert.assertEquals(commit.author, "cbeust");
    Assert.assertEquals(commit.files, Arrays.asList("A.java", "B.java"));
  }

  public static void main(String[] args) {
    new CommandTest().commandTest2();
  }
}
