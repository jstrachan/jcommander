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


import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.ResourceBundles;
import com.beust.jcommander.shell.CompletionResult;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import static com.beust.jcommander.internal.Strings.isEmpty;

public class ArgumentDescription implements IDescription {
  private Object m_object;
  private Argument m_argumentAnnotation;
  private Field m_field;
  /** Keep track of whether a value was added to flag an error */
  private boolean m_assigned = false;
  private ResourceBundle m_bundle;
  private String m_description;
  private JCommander m_jCommander;

  public ArgumentDescription(Object object, Argument annotation, Field field,
      ResourceBundle bundle, JCommander jc) {
    init(object, annotation, field, bundle, jc);
  }

  private void init(Object object, Argument annotation, Field field, ResourceBundle bundle,
      JCommander jCommander) {
    m_object = object;
    m_argumentAnnotation = annotation;
    m_field = field;
    m_bundle = bundle;
    if (m_bundle == null) {
      m_bundle = ResourceBundles.findResourceBundle(object);
    }
    m_jCommander = jCommander;

    m_description = annotation.description();
    if (! "".equals(annotation.descriptionKey())) {
      if (m_bundle != null) {
        m_description = m_bundle.getString(annotation.descriptionKey());
      } else {
//        System.out.println("Warning: field " + object.getClass() + "." + field.getName()
//            + " has a descriptionKey but no bundle was defined with @ResourceBundle, using " +
//            "default description:'" + m_description + "'");
      }
    }
  }

  public String getDescription() {
    return m_description;
  }

  public Object getObject() {
    return m_object;
  }
  
  public Argument getArgument() {
    return m_argumentAnnotation;
  }

  public Field getField() {
    return m_field;
  }

  public boolean isRequired() {
    return m_argumentAnnotation.required();
  }
  
  private boolean isMultiOption() {
    Class<?> fieldType = m_field.getType();
    return fieldType.equals(List.class) || fieldType.equals(Set.class);
  }

  public void addValue(String value) {
    addValue(value, false /* not default */);
  }

  /**
   * @return true if this parameter received a value during the parsing phase.
   */
  public boolean wasAssigned() {
    return m_assigned;
  }

  /**
   * Add the specified value to the field. First look up any field converter, then
   * any type converter, and if we can't find any, throw an exception.
   */
  public void addValue(String value, boolean isDefault) {
    p("Adding " + (isDefault ? "default " : "") + "value:" + value
        + " to parameter:" + m_field.getName());
    if (m_assigned && ! isMultiOption()) {
      throw new ParameterException("Can only specify argument " + getName()
          + " once.");
    }

    Class<?> type = m_field.getType();

    if (! isDefault) m_assigned = true;
    Object convertedValue = m_jCommander.convertValue(m_field, m_field.getType(), value);
    boolean isCollection = Collection.class.isAssignableFrom(type);

    try {
      if (isCollection) {
        @SuppressWarnings("unchecked")
        List<Object> l = (List<Object>) m_field.get(m_object);
        if (l == null) {
          l = Lists.newArrayList();
          m_field.set(m_object, l);
        }
        if (convertedValue instanceof Collection) {
          l.addAll((Collection) convertedValue);
        } else { // if (isMainArgument || m_argumentAnnotation.arity() > 1) {
          l.add(convertedValue);
//        } else {
//          l.
        }
      } else {
        m_field.set(m_object, convertedValue);
      }
    }
    catch(IllegalAccessException ex) {
      ex.printStackTrace();
    }
  }

  public String getName() {
    String a = m_argumentAnnotation.name();
    if (isEmpty(a)) {
      return getField().getName();
    }
    return a;
  }

  private void p(String string) {
    if (System.getProperty(JCommander.DEBUG_PROPERTY) != null) {
      System.out.println("[ArgumentDescription] " + string);
    }
  }

  @Override
  public String toString() {
    return "[ArgumentDescription " + m_field.getName() + "]";
  }

  public void tabComplete(String[] args, int cursorPosition, CompletionResult candidates) {
    // TODO
    // Lets try to tab complete the current command based on this argument's possible set of values 

  }
}
