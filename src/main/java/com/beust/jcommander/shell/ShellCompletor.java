/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.beust.jcommander.shell;

import com.beust.jcommander.internal.Lists;
import jline.ArgumentCompletor;
import jline.Completor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
*/
class ShellCompletor implements Completor {

  private final Shell shell;
  private final ArgumentCompletor.ArgumentDelimiter delim;

  public ShellCompletor(Shell shell) {
    this(shell, new ArgumentCompletor.WhitespaceArgumentDelimiter());
  }

  public ShellCompletor(Shell shell, ArgumentCompletor.ArgumentDelimiter delim) {
    this.delim = delim;
    this.shell = shell;
  }


  public int complete(String buffer, int cursor, List candidates) {
    // lets find the Pattern / Argument that the cursor is at right now
    // if it starts with a pattern, use those for candiates, otherwise
    ArgumentCompletor.ArgumentList argumentList = delim.delimit(buffer, cursor);
    String cursorArgument = argumentList.getCursorArgument();
    int index = argumentList.getCursorArgumentIndex();

    CompletionResult results = new CompletionResult(cursorArgument);

    String[] args = argumentList.getArguments();
    System.out.println("Args: " + Arrays.asList(args) + " cursor Argument: " + cursorArgument);
    Shell.getCurrentJCommander().tabComplete(args, index, results);
    results.getResults(candidates);
    
    //System.out.println("Cursor arg: " + cursorArgument + " index: " + argumentIndex + " arg Index: " + argumentList.getArgumentPosition());
    //System.out.println("Buffer: " + buffer + " cursor: " + cursor + " length: " + buffer.length() + " candidates: " + candidates);
    return candidates.size();
  }

}
