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

package com.beust.jcommander;

import com.beust.jcommander.args.ArgsArgumentParameter1;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test the use of the Argument annotation
 *
 * @author cbeust
 */
public class ArgumentTest {

  @Test
  public void twoArgumentsInOrder() {
    ArgsArgumentParameter1 a = new ArgsArgumentParameter1();
    JCommander jc = new JCommander(a);
    jc.parse("foo", "bar");

    Assert.assertEquals(a.from, "foo");
    Assert.assertEquals(a.to, "bar");
    Assert.assertEquals(a.optional, "hey");
  }

  @Test
  public void twoArgumentsAndOptional() {
    ArgsArgumentParameter1 a = new ArgsArgumentParameter1();
    JCommander jc = new JCommander(a);
    jc.parse("foo", "bar", "another");

    Assert.assertEquals(a.from, "foo");
    Assert.assertEquals(a.to, "bar");
    Assert.assertEquals(a.optional, "another");
  }

  @Test(expectedExceptions = ParameterException.class)
  public void requiredArgumentsFail() {
    ArgsArgumentParameter1 a = new ArgsArgumentParameter1();
    String[] argv = {"foo"};
    new JCommander(a, argv);
  }

  @Test(expectedExceptions = ParameterException.class)
  public void requiredArgumentsFail2() {
    ArgsArgumentParameter1 a = new ArgsArgumentParameter1();
    String[] argv = {};
    new JCommander(a, argv);
  }

  public static void main(String[] args) {
    new ArgumentTest().requiredArgumentsFail();
  }
}

