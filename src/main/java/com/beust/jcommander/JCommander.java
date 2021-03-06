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
import com.beust.jcommander.converters.StringConverter;
import com.beust.jcommander.internal.DefaultConverterFactory;
import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;
import com.beust.jcommander.shell.CompletionResult;

import java.io.BufferedReader;
import java.io.Console;
import java.io.FileReader;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * The main class for JCommander. It's responsible for parsing the object that contains
 * all the annotated fields, parse the command line and assign the fields with the correct
 * values and a few other helper methods, such as usage().
 * 
 * The object(s) you pass in the constructor are expected to have one or more
 * @Parameter annotations on them. You can pass either a single object, an array of objects 
 * or an instance of Iterable. In the case of an array or Iterable, JCommander will collect
 * the @Parameter annotations from all the objects passed in parameter.
 * 
 * @author cbeust
 */
public class JCommander {
  public static final String DEBUG_PROPERTY = "jcommander.debug";

  private Map<String, ParameterDescription> m_descriptions;

  private Map<Integer, ArgumentDescription> m_arguments;

  private List<ArgumentDescription> m_argumentList;

  /**
   * The objects that contain fields annotated with @Parameter.
   */
  private List<Object> m_objects;

  /**
   * This field will contain whatever command line parameter is not an option.
   * It is expected to be a List<String>.
   */
  private Field m_mainParameterField = null;

  /**
   * The object on which we found the main parameter field.
   */
  private Object m_mainParameterObject;

  /**
   * The annotation found on the main parameter field.
   */
  private Parameter m_mainParameterAnnotation;

  /**
   * A set of all the fields that are required. During the reflection phase,
   * this field receives all the fields that are annotated with required=true
   * and during the parsing phase, all the fields that are assigned a value
   * are removed from it. At the end of the parsing phase, if it's not empty,
   * then some required fields did not receive a value and an exception is
   * thrown.
   */
  private Map<Field, ParameterDescription> m_requiredFields = Maps.newHashMap();

  /**
   * A map of all the annotated fields.
   */
  private Map<Field, ParameterDescription> m_fields = Maps.newHashMap();

  private ResourceBundle m_bundle;

  /**
   * A default provider returns default values for the parameters.
   */
  private IDefaultProvider m_defaultProvider;

  /**
   * List of commands and their instance.
   */
  private Map<String, JCommander> m_commands = Maps.newHashMap();

  /**
   * The name of the command after the parsing has run.
   */
  private String m_parsedCommand;

  private String m_programName;

  /**
   * The factories used to look up string converters.
   */
  private static List<IStringConverterFactory> CONVERTER_FACTORIES = Lists.newArrayList();

  static {
    CONVERTER_FACTORIES.add(new DefaultConverterFactory());
  };

  /**
   * Returns a new commander with no command line arguments so that it can be configured first
   * such as to call {@link #setProgramName(String)} or {@link #addCommand(String, Object)}
   * then the {@link #parse(String...)} method can be called to parse the arguments.
   *
   * This method also avoids
   * <a href="http://stackoverflow.com/questions/3313929/how-do-i-disambiguate-in-scala-between-methods-with-vararg-and-without">this issue</a>
   * with Scala unable to differentiate the constructors since there are multiple matching var-args combinations if you allow zero sized arrays as a possible match
   *
   * @param object
   * @return
   */
  public static JCommander newInstance(Object object) {
    return new JCommander(object);
  }

  /**
   * Returns a new commander with no command line arguments so that it can be configured first
   * such as to call {@link #setProgramName(String)} or {@link #addCommand(String, Object)}
   * then the {@link #parse(String...)} method can be called to parse the arguments.
   *
   * This method also avoids
   * <a href="http://stackoverflow.com/questions/3313929/how-do-i-disambiguate-in-scala-between-methods-with-vararg-and-without">this issue</a>
   * with Scala unable to differentiate the constructors since there are multiple matching var-args combinations if you allow zero sized arrays as a possible match
   *
   * @param object
   * @return
   */
  public static JCommander newInstance(Object object, ResourceBundle bundle) {
    return new JCommander(object, bundle);
  }

  public JCommander(Object object) {
    init(object, null);
  }

  public JCommander(Object object, ResourceBundle bundle, String... args) {
    init(object, bundle);
    parse(args);
  }


  public JCommander(Object object, String... args) {
    init(object, null);
    parse(args);
  }

  private void init(Object object, ResourceBundle bundle) {
    m_bundle = bundle;
    m_objects = Lists.newArrayList();
    if (object instanceof Iterable) {
      // Iterable
      for (Object o : (Iterable<?>) object) {
        m_objects.add(o);
      }
    } else if (object.getClass().isArray()) {
      // Array
      for (Object o : (Object[]) object) {
        m_objects.add(o);
      }
    } else {
      // Single object
      m_objects.add(object);
    }

  }

  /**
   * Parse the command line parameters.
   */
  public void parse(String... args) {
    StringBuilder sb = new StringBuilder("Parsing \"");
    sb.append(join(args).append("\"\n  with:").append(join(m_objects.toArray())));
    p(sb.toString());

    m_descriptions = Maps.newHashMap();

    // Create the ParameterDescriptions for all the @Parameter found.
    for (Object object : m_objects) {
      addDescription(object);
    }
    initializeDefaultValues();
    parseValues(expandArgs(args));
    validateOptions();
  }

  private StringBuilder join(Object[] args) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < args.length; i++) {
      if (i > 0) result.append(" ");
      result.append(args[i]);
    }
    return result;
  }

  private void initializeDefaultValues() {
    if (m_defaultProvider != null) {
      for (ParameterDescription pd : getDescriptions().values()) {
        initializeDefaultValue(pd);
      }
    }
  }

  /**
   * Make sure that all the required parameters have received a value.
   */
  private void validateOptions() {
    if (! m_requiredFields.isEmpty()) {
      StringBuilder missingFields = new StringBuilder();
      for (ParameterDescription pd : m_requiredFields.values()) {
        missingFields.append(pd.getNames()).append(" ");
      }
      throw new ParameterException("The following options are required: " + missingFields);
    }
    
  }
  
  /**
   * Expand the command line parameters to take @ parameters into account.
   * When @ is encountered, the content of the file that follows is inserted
   * in the command line.
   * 
   * @param originalArgv the original command line parameters
   * @return the new and enriched command line parameters
   */
  private String[] expandArgs(String[] originalArgv) {
    List<String> vResult1 = Lists.newArrayList();

    //
    // Expand @
    //
    for (String arg : originalArgv) {

      if (arg.startsWith("@")) {
        String fileName = arg.substring(1);
        vResult1.addAll(readFile(fileName));
      }
      else {
        vResult1.add(arg);
      }
    }

    //
    // Expand separators
    //
    List<String> vResult2 = Lists.newArrayList();
    for (int i = 0; i < vResult1.size(); i++) {
      String arg = vResult1.get(i);
      String[] v1 = vResult1.toArray(new String[0]);
      if (isOption(v1, arg)) {
        String sep = getSeparatorFor(v1, arg);
        if (! " ".equals(sep)) {
          String[] sp = arg.split("[" + sep + "]");
          for (String ssp : sp) {
            vResult2.add(ssp);
          }
        } else {
          vResult2.add(arg);
        }
      } else {
        vResult2.add(arg);
      }
    }

    return vResult2.toArray(new String[vResult2.size()]);
  }

  private boolean isOption(String[] args, String arg) {
    String prefixes = getOptionPrefixes(args, arg);
    return prefixes.indexOf(arg.charAt(0)) >= 0;
  }

  private ParameterDescription getPrefixDescriptionFor(String arg) {
    for (Map.Entry<String, ParameterDescription> es : getDescriptions().entrySet()) {
      if (arg.startsWith(es.getKey())) return es.getValue();
    }

    return null;
  }

  /**
   * If arg is an option, we can look it up directly, but if it's a value,
   * we need to find the description for the option that precedes it.
   */
  private ParameterDescription getDescriptionFor(String[] args, String arg) {
    ParameterDescription result = getPrefixDescriptionFor(arg);
    if (result != null) return result;

    for (String a : args) {
      ParameterDescription pd = getPrefixDescriptionFor(arg);
      if (pd != null) result = pd;
      if (a.equals(arg)) return result;
    }

    throw new ParameterException("Unknown parameter: " + arg);
  }

  private String getSeparatorFor(String[] args, String arg) {
    ParameterDescription pd = getDescriptionFor(args, arg);

    // Could be null if only main parameters were passed
    if (pd != null) {
      Parameters p = pd.getObject().getClass().getAnnotation(Parameters.class);
      if (p != null) return p.separators();
    }

    return " ";
  }

  private String getOptionPrefixes(String[] args, String arg) {
    ParameterDescription pd = getDescriptionFor(args, arg);

    // Could be null if only main parameters were passed
    if (pd != null) {
      Parameters p = pd.getObject().getClass()
          .getAnnotation(Parameters.class);
      if (p != null) return p.optionPrefixes();
    }

    return Parameters.DEFAULT_OPTION_PREFIXES;
  }

  /**
   * Reads the file specified by filename and returns the file content as a string.
   * End of lines are replaced by a space.
   * 
   * @param fileName the command line filename
   * @return the file content as a string.
   */
  private static List<String> readFile(String fileName) {
    List<String> result = Lists.newArrayList();

    try {
      BufferedReader bufRead = new BufferedReader(new FileReader(fileName));

      String line;

      // Read through file one line at time. Print line # and line
      while ((line = bufRead.readLine()) != null) {
        result.add(line);
      }

      bufRead.close();
    }
    catch (IOException e) {
      throw new ParameterException("Could not read file " + fileName + ": " + e);
    }

    return result;
  }

  /**
   * Remove spaces at both ends and handle double quotes.
   */
  private static String trim(String string) {
    String result = string.trim();
    if (result.startsWith("\"")) {
      if (result.endsWith("\"")) {
          return result.substring(1, result.length() - 1);
      }
      return result.substring(1);
    }
    return result;
  }

  private void addDescription(Object object) {
    Class<?> cls = object.getClass();

    while (!Object.class.equals(cls)) {
      for (Field f : cls.getDeclaredFields()) {
        p("Field:" + cls.getSimpleName() + "." + f.getName());
        f.setAccessible(true);
        Annotation annotation = f.getAnnotation(Parameter.class);
        if (annotation != null) {
          Parameter p = (Parameter) annotation;
          if (p.names().length == 0) {
            p("Found main parameter:" + f);
            if (m_mainParameterField != null) {
              throw new ParameterException("Only one @Parameter with no names attribute is"
                  + " allowed, found:" + m_mainParameterField + " and " + f);
            }
            m_mainParameterField = f;
            m_mainParameterObject = object;
            m_mainParameterAnnotation = p;
          } else {
            for (String name : p.names()) {
              if (getDescriptions().containsKey(name)) {
                throw new ParameterException("Found the option " + name + " multiple times");
              }
              p("Adding description for " + name);
              ParameterDescription pd = new ParameterDescription(object, p, f, m_bundle, this);
              m_fields.put(f, pd);
              getDescriptions().put(name, pd);

              if (p.required()) m_requiredFields.put(f, pd);
            }
          }
        }
        Argument a = f.getAnnotation(Argument.class);
        if (a != null) {
          Integer index = a.index();
          if (getArguments().containsKey(index)) {
            throw new ParameterException("Found the argument at index " + index + " multiple times");
          }
          p("Adding argument for " + index);
          ArgumentDescription pd = new ArgumentDescription(object, a, f, m_bundle, this);
          getArguments().put(index, pd);
        }
      }
      // Traverse the super class until we find Object.class
      cls = cls.getSuperclass();
    }
  }

  private void initializeDefaultValue(ParameterDescription pd) {
    String optionName = pd.getParameter().names()[0];
    String def = m_defaultProvider.getDefaultValueFor(optionName);
    if (def != null) {
      p("Initializing " + optionName + " with default value:" + def);
      pd.addValue(def, true /* default */);
    }
  }

  /**
   * Main method that parses the values and initializes the fields accordingly.
   */
  private void parseValues(String[] args) {
    // This boolean becomes true if we encounter a command, which indicates we need
    // to stop parsing (the parsing of the command will be done in a sub JCommander
    // object)
    boolean commandParsed = false;
    int i = 0;
    int argIndex = 0;
    while (i < args.length && ! commandParsed) {
      String arg = args[i];
      String a = trim(arg);
      p("Parsing arg:" + a);

      if (isOption(args, a)) {
        //
        // Option
        //
        ParameterDescription pd = getDescriptions().get(a);

        if (pd != null) {
          if (pd.getParameter().password()) {
            //
            // Password option, use the Console to retrieve the password
            //
            Console console = System.console();
            if (console == null) {
              throw new ParameterException("No console is available to get parameter " + a);
            }
            System.out.print("Value for " + a + " (" + pd.getDescription() + "):");
            char[] password = console.readPassword();
            pd.addValue(new String(password));
          } else {
            //
            // Regular option
            //
            Class<?> fieldType = pd.getField().getType();
            
            // Boolean, set to true as soon as we see it, unless it specified
            // an arity of 1, in which case we need to read the next value
            if ((fieldType == boolean.class || fieldType == Boolean.class)
                && pd.getParameter().arity() == -1) {
              pd.addValue("true");
              m_requiredFields.remove(pd.getField());
            } else {
              // Regular parameter, use the arity to tell use how many values
              // we need to consume
              int arity = pd.getParameter().arity();
              int n = (arity != -1 ? arity : 1);

              int offset = "--".equals(args[i + 1]) ? 1 : 0;

              if (i + n < args.length) {
                for (int j = 1; j <= n; j++) {
                  pd.addValue(trim(args[i + j + offset]));
                  m_requiredFields.remove(pd.getField());
                }
                i += n + offset;
              } else {
                throw new ParameterException(n + " parameters expected after " + arg);
              }
            }
          }
        } else {
          throw new ParameterException("Unknown option: " + a);
        }
      }
      else {
        //
        // Main parameter
        //
        if (! isStringEmpty(arg)) {
          if (m_commands.isEmpty()) {
            //
            // Regular (non-command) parsing
            //

            // Do we have any arguments to eat up arguments
            // TODO
            if (getArguments().size() > argIndex) {
              ArgumentDescription ad = getArgument(argIndex);
              ad.addValue(arg);
              argIndex++;
            }
            else {
              // lets pass any remaining arguments into the main parameter
              List mp = getMainParameter(arg);
              String value = arg;
              Object convertedValue = value;

              if (m_mainParameterField.getGenericType() instanceof ParameterizedType) {
                ParameterizedType p = (ParameterizedType) m_mainParameterField.getGenericType();
                Type cls = p.getActualTypeArguments()[0];
                if (cls instanceof Class) {
                  convertedValue = convertValue(m_mainParameterField, (Class) cls, value);
                }
              }

              mp.add(convertedValue);
            }
          }
          else {
            //
            // Command parsing
            //
            JCommander jc = m_commands.get(arg);
            if (jc == null) throw new ParameterException("Expected a command, got " + arg);
            m_parsedCommand = arg;

            // Found a valid command, ask it to parse the remainder of the arguments.
            // Setting the boolean commandParsed to true will force the current
            // loop to end.
            jc.parse(subArray(args, i + 1));
            commandParsed = true;
          }
        }
      }
      i++;
    }
    if (getArguments().size() > argIndex) {
      ArgumentDescription ad = getArgument(argIndex);
      if (ad.isRequired()) {
        throw new ParameterException("Missing " + ad.getName() + " argument");
      }
    }
  }


  /**
   * Attempts to perform tab completion in a shell
   * or maybe using some kind of bash completion mechanism.
   *
   * Note that the cursorPosition can be beyond the end of the list
   * when we are about to type a new argument.
   *
   * Basically we need to try figure out which pattern or argument the cursor is on, then
   * figure out a list of all possible values we can deduce for that position and feed them
   * into the result object which will filter and record the results
   */
  public void tabComplete(String[] args, int cursorPosition, CompletionResult results) {
    int i = 0;
    int argIndex = 0;
    while (i <= cursorPosition) {
      // TODO what if we start with an option???
      // e.g. current arg is "-"

      boolean option = false;
      String a = null;
      if (i < args.length) {
        String arg = args[i];
        a = trim(arg);
        option = isOption(args, a);
      }
      if (option) {
        //
        // Option
        //
        ParameterDescription pd = getDescriptions().get(a);
        if (i == cursorPosition) {
          if (pd != null) {
            // nothing to complete
            break;
          } else {
            // find all the options which start with the arg
            for (ParameterDescription d : getDescriptions().values()) {
              String[] names = d.getParameter().names();
              for (String name : names) {
                results.addCandidate(name);
              }
            }
          }
        }
        if (pd != null) {
          // TODO need to figure out arity here to detect values after a parameter
        }
      } else {
        //
        // Main parameter
        //
        if (m_commands.isEmpty()) {
          //
          // Regular (non-command) parsing
          //

          if (getArguments().size() > argIndex) {
            ArgumentDescription ad = getArgument(argIndex);
            argIndex++;
            if (i == cursorPosition) {
              ad.tabComplete(args, cursorPosition, results);
              break;
            }
          } else {
            // TODO deal with main parameter
            // lets pass any remaining arguments into the main parameter
          }
        } else {
          if (i == cursorPosition) {
            // Lets complete on all command names
            results.addCandidates(m_commands.keySet());
            break;
          }
          if (a != null) {
            //
            // Command parsing
            //
            JCommander jc = m_commands.get(a);
            if (jc == null) throw new ParameterException("Expected a command, got " + a);

            // Found a valid command, ask it to parse the remainder of the arguments.
            // Setting the boolean commandParsed to true will force the current
            // loop to end.
            jc.tabComplete(subArray(args, i + 1), cursorPosition - 1, results);
            break;
          }
        }
      }
      i++;
    }
  }

  protected ArgumentDescription getArgument(int argIndex) {
    ArgumentDescription ad = getArguments().get(argIndex);
    if (ad == null) {
      throw new ParameterException("Missing @Argument annotation for index " + argIndex);
    }
    return ad;
  }

  private String[] subArray(String[] args, int index) {
    int l = args.length - index;
    String[] result = new String[l];
    System.arraycopy(args, index, result, 0, l);

    return result;
  }

  private static boolean isStringEmpty(String s) {
    return s == null || "".equals(s);
  }

  /**
   * @return the field that's meant to receive all the parameters that are not options.
   * 
   * @param arg the arg that we're about to add (only passed here to ouput a meaningful
   * error message).
   */
  private List<?> getMainParameter(String arg) {
    if (m_mainParameterField == null) {
      throw new ParameterException(
          "Was passed main parameter '" + arg + "' but no main parameter was defined");
    }

    try {
      @SuppressWarnings("unchecked")
      List<?> result = (List<?>) m_mainParameterField.get(m_mainParameterObject);
      if (result == null) {
        result = Lists.newArrayList();
        m_mainParameterField.set(m_mainParameterObject, result);
      }
      return result;
    }
    catch(IllegalAccessException ex) {
      throw new ParameterException("Couldn't access main parameter: " + ex.getMessage());
    }
  }

  public String getMainParameterDescription() {
    getDescriptions(); // force lazy create
    return m_mainParameterAnnotation != null ? m_mainParameterAnnotation.description()
        : null;
  }

  private int longestName(Collection<?> objects) {
    int result = 0;
    for (Object o : objects) {
      int l = o.toString().length();
      if (l > result) result = l;
    }

    return result;
  }

  /**
   * Set the program name (used only in the usage).
   */
  public void setProgramName(String name) {
    m_programName = name;
  }

  /**
   * Display the usage for this command.
   */
  public void usage(String commandName) {
    StringBuilder sb = new StringBuilder();
    usage(commandName, sb);
    System.out.println(sb.toString());
  }

  /**
   * Store the help for the command in the passed string builder.
   */
  public void usage(String commandName, StringBuilder out) {
    JCommander jc = m_commands.get(commandName);
    String description = jc.getCommandDescription();
    if (description != null) {
      out.append(description);
      out.append("\n");
    }
    jc.usage(out);
  }

  /**
   * Display a the help on System.out.
   */
  public void usage() {
    StringBuilder sb = new StringBuilder();
    usage(sb);
    System.out.println(sb.toString());
  }

  /**
   * Store the help in the passed string builder.
   */
  public void usage(StringBuilder out) {
    if (getDescriptions() == null) {
      m_descriptions = Maps.newHashMap();

      // Create the ParameterDescriptions for all the @Parameter found.
      for (Object object : m_objects) {
        addDescription(object);
      }
    }
    boolean hasCommands = ! m_commands.isEmpty();

    //
    // First line of the usage
    //
    String programName = m_programName != null ? m_programName : "<main class>";
    out.append("Usage: " + programName + " [options]");
    if (hasCommands) out.append(" [command] [command options]");
    List<ArgumentDescription> adList = getArgumentList();
    int longestArg = 0;
    for (ArgumentDescription ad : adList) {
      String name = ad.getName();
      if (name.length() > longestArg) {
        longestArg = name.length();
      }
      out.append(" " + ad.getName());
    }
    longestArg += 2; // use 2 space at least
    if (m_mainParameterAnnotation != null) {
      out.append(" " + m_mainParameterAnnotation.description());
    }
    for (ArgumentDescription ad : adList) {
      String name = ad.getName();
      int spaceCount = longestArg - name.length();
      out.append("\n    " + name + s(spaceCount) + ad.getDescription());
    }
    if (!adList.isEmpty()) {
      out.append("\n");
    }
    out.append("\n  Options:\n");

    // 
    // Align the descriptions at the "longestName" column
    //
    int longestName = 0;
    List<ParameterDescription> sorted = Lists.newArrayList();
    for (ParameterDescription pd : m_fields.values()) {
      if (! pd.getParameter().hidden()) {
        sorted.add(pd);
        // + to have an extra space between the name and the description
        int length = pd.getNames().length() + 2;
        if (length > longestName) {
          longestName = length;
        }
      }
    }

    //
    // Sort the options
    //
    Collections.sort(sorted, new Comparator<ParameterDescription>() {
      public int compare(ParameterDescription arg0, ParameterDescription arg1) {
        return arg0.getNames().toLowerCase().compareTo(arg1.getNames().toLowerCase());
      }
    });

    //
    // Display all the names and descriptions
    //
    for (ParameterDescription pd : sorted) {
      int l = pd.getNames().length();
      int spaceCount = longestName - l;
      out.append("  "
          + (pd.getParameter().required() ? "* " : "  ")
          + pd.getNames() + s(spaceCount) + pd.getDescription());
      Object def = pd.getDefault();
      if (def != null) out.append(" (default: " + def + ")");
      out.append("\n");
    }

    //
    // If commands were specified, show them as well
    //
    if (hasCommands) {
      out.append("  Commands:\n");
      int ln = longestName(m_commands.keySet()) + 3;
      for (Map.Entry<String, JCommander> commands : m_commands.entrySet()) {
        String name = commands.getKey();
        int spaceCount  = ln - name.length();
        JCommander jc = commands.getValue();
        String description = jc.getCommandDescription();
        if (description == null) {
          description = jc.getMainParameterDescription();
        }
        out.append("    " + name + s(spaceCount) + description + "\n");
      }
    }

    //
    // If a UsageReporter is being used, include it's usage as well.
    //
    for(Object o:m_objects) {
      if( o instanceof UsageReporter) {
        ((UsageReporter) o).usage(out);
      }
    }

  }

  public String getCommandDescription() {
    for (Object object : m_objects) {
      Command command = object.getClass().getAnnotation(Command.class);
      if (command != null) {
        return command.description();
      }
    }
    return null;
  }

  /**
   * @return a Collection of all the @Parameter annotations found on the
   * target class. This can be used to display the usage() in a different
   * format (e.g. HTML).
   */
  public List<ParameterDescription> getParameters() {
    return new ArrayList<ParameterDescription>(m_fields.values());
  }

  private void p(String string) {
    if (System.getProperty(JCommander.DEBUG_PROPERTY) != null) {
      System.out.println("[JCommander] " + string);
    }
  }

  /**
   * Define the default provider for this instance.
   */
  public void setDefaultProvider(IDefaultProvider defaultProvider) {
    m_defaultProvider = defaultProvider;
  }

  public void addConverterFactory(IStringConverterFactory converterFactory) {
    CONVERTER_FACTORIES.add(converterFactory);
  }

  public <T> Class<? extends IStringConverter<T>> findConverter(Class<T> cls) {
    for (IStringConverterFactory f : CONVERTER_FACTORIES) {
      Class<? extends IStringConverter<T>> result = f.getConverter(cls);
      if (result != null) return result;
    }

    return null;
  }

  public Object convertValue(ParameterDescription pd, String value) {
    return convertValue(pd.getField(), pd.getField().getType(), value);
  }

  /**
   * @param type The class of the field
   * @param value The value to convert
   */
  public Object convertValue(Field field, Class type, String value) {
    Class<? extends IStringConverter<?>> converterClass;
    String optionName;
    Parameter annotation = field.getAnnotation(Parameter.class);
    if (annotation != null) {
      converterClass = annotation.converter();
      String[] names = annotation.names();
      optionName = names.length > 0 ? names[0] : "[Main class]";
    }
    else {
      Argument argAnn = field.getAnnotation(Argument.class);
      converterClass = argAnn.converter();
      optionName = field.getName();
    }

    //
    // Try to find a converter on the annotation
    //
    if (converterClass == null || converterClass == NoConverter.class) {
      converterClass = findConverter(type);
    }
    if (converterClass == null) {
      converterClass = StringConverter.class;
    }
    if (converterClass == null && Collection.class.isAssignableFrom(type)) {
      converterClass = StringConverter.class;
    }

    //
//    //
//    // Try to find a converter in the factory
//    //
//    IStringConverter<?> converter = null;
//    if (converterClass == null && m_converterFactories != null) {
//      // Mmmh, javac requires a cast here
//      converter = (IStringConverter) m_converterFactories.getConverter(type);
//    }

    if (converterClass == null) {
      throw new ParameterException("Don't know how to convert " + value
          + " to type " + type + " (field: " + field.getName() + ")");
    }

    IStringConverter<?> converter;
    Object result = null;
    try {
      converter = instantiateConverter(optionName, converterClass);
      result = converter.convert(value);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }

    return result;
  }

  private IStringConverter<?> instantiateConverter(String optionName,
      Class<? extends IStringConverter<?>> converterClass)
      throws IllegalArgumentException, InstantiationException, IllegalAccessException,
      InvocationTargetException {
    Constructor<IStringConverter<?>> ctor = null;
    Constructor<IStringConverter<?>> stringCtor = null;
    Constructor<IStringConverter<?>>[] ctors
        = (Constructor<IStringConverter<?>>[]) converterClass.getDeclaredConstructors();
    for (Constructor<IStringConverter<?>> c : ctors) {
      Class<?>[] types = c.getParameterTypes();
      if (types.length == 1 && types[0].equals(String.class)) {
        stringCtor = c;
      } else if (types.length == 0) {
        ctor = c;
      }
    }

    IStringConverter<?> result = stringCtor != null
        ? stringCtor.newInstance(optionName)
        : ctor.newInstance();

        return result;
  }

  /**
   * Add a command object.
   */
  public void addCommand(String name, Object object) {
    JCommander jc = new JCommander(object);
    jc.setProgramName(name);
    m_commands.put(name, jc);
  }

  public String getParsedCommand() {
    return m_parsedCommand;
  }

  /**
   * @return n spaces
   */
  private String s(int count) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < count; i++) {
      result.append(" ");
    }

    return result.toString();
  }

  public List<Object> getObjects() {
    return Collections.unmodifiableList(m_objects);
  }

  /**
   * A map to look up argument description per index position
   */
  protected Map<Integer, ArgumentDescription> getArguments() {
    if (m_arguments == null) {
      m_arguments = Maps.newHashMap();
    }
    return m_arguments;
  }

  protected List<ArgumentDescription> getArgumentList() {
    if (m_argumentList == null) {
      Map<Integer, ArgumentDescription> map = getArguments();
      int s = map.size();
      m_argumentList = new ArrayList<ArgumentDescription>(s);
      ArgumentDescription previous = null;
      for (int i = 0; i < s; i++) {
        ArgumentDescription ad = getArgument(i);
        if (previous != null && !previous.isRequired() && ad.isRequired()) {
          throw new ParameterException("Argument " + ad.getName() + " at index " + i +
              " is required when argument " + previous.getName() + " before it is optional");
        }
        previous = ad;
        m_argumentList.add(ad);
      }
    }
    return m_argumentList;
  }

  /**
   * A map to look up parameter description per option name.
   */
  protected Map<String, ParameterDescription> getDescriptions() {
    if (m_descriptions == null) {
      m_descriptions = Maps.newHashMap();

      // Create the ParameterDescriptions for all the @Parameter found.
      for (Object object : m_objects) {
        addDescription(object);
      }
    }
    return m_descriptions;
  }
}

