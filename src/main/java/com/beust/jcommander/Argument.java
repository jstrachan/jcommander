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

import com.beust.jcommander.converters.NoConverter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * Represents a positional argument which is not named but appears at a specific order in the list of
 * arguments after all the options are removed.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ FIELD })
public @interface Argument {

  /**
   * The position index starting at 0 for the argument so that multiple arguments can be ordered
   */
  int index();
  
  /**
   * The name of the argument which is used in the usage printing. If no name is specified then the name
   * of the field is used.
   */
  String name() default "";

  /**
   * A description of this argument.
   */
  String description() default "";

  /**
   * Whether this argument is required.
   */
  boolean required() default true;

  /**
   * The key used to find the string in the message bundle.
   */
  String descriptionKey() default "";

  /**
   * The string converter to use for this field.
   */
  Class<? extends IStringConverter<?>> converter() default NoConverter.class;
}
