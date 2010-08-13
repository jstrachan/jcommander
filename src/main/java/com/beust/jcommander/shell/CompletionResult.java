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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A helper class to help collect completion results
 */
public class CompletionResult {
  private SortedSet<String> results = new TreeSet<String>();
  private final String prefix;

  /**
   * @param prefix the command prefix is either null (for a new command) or the prefix typed so far which acts as a filter
   * on all the possible values we can collect
   */
  public CompletionResult(String prefix) {
    this.prefix = prefix;
  }

  public void addCandidates(Collection<String> names) {
    for (String name : names) {
      addCandidate(name);
    }
  }

  public void addCandidate(String name) {
    if (prefix == null || name.startsWith(prefix)) {
      String remaining = (prefix == null) ? name : name.substring(prefix.length());
      if (remaining.length() > 0) {
        results.add(remaining);
      }
    }
  }

  /**
   * Copies the completion results to the given list of candidates
   */
  public void getResults(List candidates) {
    candidates.addAll(results);
  }

  public List<String> getResults() {
    List list = Lists.newArrayList();
    getResults(list);
    return list;    
  }
}
