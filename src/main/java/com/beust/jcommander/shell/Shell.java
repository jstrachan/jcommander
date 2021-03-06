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
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.UsageReporter;
import jline.Completor;
import jline.ConsoleReader;
import jline.Terminal;
import jline.UnsupportedTerminal;
import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;

import java.io.*;
import java.lang.reflect.Method;
import java.util.List;

/**
 * <p>Implements a jline base shell for executing JCommander commands.</p>
 * <p/>
 * This is code is original based on the Karaf Console code.
 *
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
abstract public class Shell implements Runnable, UsageReporter {

  final private InputStream in = System.in;
  final private PrintStream out = System.out;
  final private PrintStream err = System.err;

  protected String prompt = "\u001B[1m>\u001B[0m ";
  protected Throwable lastException;
  protected boolean printStackTraces;
  protected boolean bellEnabled = true;
  protected File history;

  @Parameter(description = "a sub command to execute, if not specified, you will be placed into an interactive shell.")
  public List<String> cliArgs;
  private Completor completer = createCompleter();

  public static class CloseShellException extends RuntimeException {
  }

  static final private ThreadLocal<JCommander> CURRENT_JCOMMANDER = new ThreadLocal<JCommander>();
  static final private ThreadLocal<Session> CURRENT_SESSION = new ThreadLocal<Session>();

  /**
   * @return the currently executing shell session.
   */
  static public Session getCurrentSession() {
    return CURRENT_SESSION.get();
  }

  /**
   * @return the currently executing JCommander 
   */
  static public JCommander getCurrentJCommander() {
    return CURRENT_JCOMMANDER.get();
  }

  public class Session {

    public Shell getShell() {
      return Shell.this;
    }

    private final JCommander jcommander = getCurrentJCommander();

    private ConsoleReader reader;

    public JCommander getJCommander() {
      return jcommander;
    }

    public ConsoleReader getConsoleReader() {
      return reader;
    }

    private void execute() {
      Session original = CURRENT_SESSION.get();
      CURRENT_SESSION.set(this);
      Terminal terminal = null;
      try {
        terminal = openTerminal();
        try {
          this.reader = new ConsoleReader(System.in, new PrintWriter(out), getClass().getResourceAsStream("keybinding.properties"), terminal);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

        if (history != null) {
          try { // to setup history file support
            history.getParentFile().mkdirs();
            reader.getHistory().setHistoryFile(history);
          } catch (IOException ignore) {
          }
        }

        reader.setBellEnabled(bellEnabled);
        if (completer != null) {
          reader.addCompletor(completer);
        }

        try {
          while (true) {
            String line = null;
            try {
              line = reader.readLine(prompt);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
            if (line == null) {
              break;
            }
            executeLine(line);
          }
        } catch (CloseShellException e) {
        }

      } finally {
        reader = null;
        closeTerminal(terminal);
        CURRENT_SESSION.set(original);
      }
    }

    private Terminal openTerminal() {
      if ("jline.UnsupportedTerminal".equals(System.getProperty("jline.terminal"))) {
        return new UnsupportedTerminal();
      }
      boolean windows = System.getProperty("os.name").toLowerCase().contains("windows");
      try {
        if (windows) {
          AnsiWindowsTerminal t = new AnsiWindowsTerminal();
          t.setDirectConsole(true);
          t.initializeTerminal();
          return t;
        }

        NoInterruptUnixTerminal t = new NoInterruptUnixTerminal();
        t.initializeTerminal();
        return t;

      } catch (Throwable e) {
        return new UnsupportedTerminal();
      }
    }

    private void closeTerminal(Terminal term) {
      if (term != null) {
        try {
          term.restoreTerminal();
        } catch (Exception ignore) {
        }
      }
    }

  }


  /**
   * Override to customize command line parsing, should call {@link #executeCommand(String, String[])}
   */
  public void executeLine(String line) {
    // implementing a really simple command line parser..
    // TODO: replace with something that can handle quoted args etc.
    String[] args = line.split(" +");
    if (args.length > 0) {
      String[] commandArgs = new String[args.length - 1];
      System.arraycopy(args, 1, commandArgs, 0, args.length - 1);
      executeCommand(args[0], commandArgs);
    }
  }

  public abstract String getShellName();

  public abstract String[] getDisplayedCommands();

  public abstract JCommander createSubCommand(String name);

  public void usage(StringBuilder out) {
    out.append("\n");
    out.append("  Commands:\n");
    int ln = longestName(getDisplayedCommands()) + 3;
    for (String name : getDisplayedCommands()) {
      int spaceCount = ln - name.length();
      JCommander jc = createSubCommand(name);
      if( jc!=null ) {
        String description = jc.getCommandDescription();
        if (description == null) {
          description = jc.getMainParameterDescription();
        }
        out.append("    " + name + s(spaceCount) + description + "\n");
      }
    }
  }

  private String s(int count) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < count; i++) {
      result.append(" ");
    }

    return result.toString();
  }

  private int longestName(String[] objects) {
    int result = 0;
    for (Object o : objects) {
      int l = o.toString().length();
      if (l > result) result = l;
    }

    return result;
  }

  /**
   * Override if you want to change how the command is looked up and executed.  It reports
   * an error if the command is not found.
   * This ends up calling {@link #executeCommand(String, com.beust.jcommander.JCommander)}
   */
  protected void executeCommand(String command, String[] args) {
    JCommander jc = createSubCommand(command);
    if (jc == null) {
      displayNotFound(command);
      return;
    }
    try {
      jc.parse(args);
    } catch (ParameterException e) {
      err.print(Ansi.ansi().fg(Ansi.Color.RED));
      err.println(command + ": invalid usage: " + e.getMessage());
      err.print(Ansi.ansi().reset());
      err.flush();
      out.println();
      jc.usage();
      return;
    }
    try {
      executeCommand(command, jc);
    } catch (CloseShellException e) {
      throw e;
    } catch (Throwable t) {
      displayFailure(command, t);
    }
  }

  public void displayFailure(String command, Throwable t) {
    lastException = t;
    err.print(Ansi.ansi().fg(Ansi.Color.RED).toString());
    if (printStackTraces) {
      t.printStackTrace(err);
    } else {
      if( command==null ) {
        err.println(getShellName() + ": " + t);
      } else {
        err.println(command + ": " + t);
      }
    }
    err.print(Ansi.ansi().fg(Ansi.Color.DEFAULT).toString());
  }

  public void displayNotFound(String command) {
    err.print(Ansi.ansi().fg(Ansi.Color.RED));
    err.println(getShellName() + ": " + command + ": command not found");
    err.print(Ansi.ansi().reset());
    return;
  }

  /**
   * This executes the command by looking for the first Runnable object
   * added to the JCommander object.  Override if you want to execute
   * commands using a different strategy.
   */
  protected void executeCommand(String name, JCommander command) {
    for (Object o : command.getObjects()) {
      if (o instanceof Runnable) {

        JCommander original = CURRENT_JCOMMANDER.get();
        CURRENT_JCOMMANDER.set(command);
        try {
          ((Runnable) o).run();
        } finally {
          CURRENT_JCOMMANDER.set(original);
        }

        return;
      }
    }
    throw new IllegalArgumentException("The command " + name + " is not a Runnable command.");
  }

  public void run(JCommander jc) {
    JCommander original = CURRENT_JCOMMANDER.get();
    CURRENT_JCOMMANDER.set(jc);
    try {
      run();
    } finally {
      CURRENT_JCOMMANDER.set(original);
    }
  }

  /**
   * Sub classes can override to implement before/after actions like
   * showing a banner etc.
   *
   * @throws IOException
   */
  public void run() {
    if (cliArgs == null || cliArgs.isEmpty()) {
      new Session().execute();
    } else {
      String command = cliArgs.remove(0);
      String args[] = cliArgs.toArray(new String[cliArgs.size()]);
      executeCommand(command, args);
    }
  }


  private static PrintStream wrap(PrintStream stream) {
    OutputStream o = AnsiConsole.wrapOutputStream(stream);
    if (o instanceof PrintStream) {
      return ((PrintStream) o);
    } else {
      return new PrintStream(o);
    }
  }

  private static <T> T unwrap(T stream) {
    try {
      Method mth = stream.getClass().getMethod("getRoot");
      return (T) mth.invoke(stream);
    } catch (Throwable t) {
      return stream;
    }
  }

  protected Completor createCompleter() {
    return new ShellCompletor(this);
  }

}
