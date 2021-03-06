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

package com.beust.jcommander.args;

import com.beust.jcommander.Argument;
import com.beust.jcommander.HostPort;
import com.beust.jcommander.Parameter;
import org.testng.collections.Lists;

import java.util.List;

/**
 * A class with some arguments
 * 
 * @author cbeust
 */
public class ArgsArgumentParameter1 {
  @Parameter(names = "--debug", description = "Turns on debug mode")
  public boolean debug;

  @Argument(index = 0, description="From file")
  public String from;

  @Argument(index = 1, description="To file")
  public String to;

  @Argument(index = 2, description="Optional thingy", required = false)
  public String optional = "hey";
}
