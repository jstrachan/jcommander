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

import jline.WindowsTerminal;
import org.fusesource.jansi.AnsiConsole;
import org.fusesource.jansi.WindowsAnsiOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AnsiWindowsTerminal extends WindowsTerminal {

    private boolean ansisupported = checkAnsiSupport();

    @Override
    public boolean isANSISupported() {
        return ansisupported;
    }

    @Override
    public int readCharacter(InputStream in) throws IOException {
        return in.read();
    }

    public int readDirectChar(InputStream in) throws IOException {
        return super.readCharacter(in);
    }

    public static boolean checkAnsiSupport() {
        OutputStream dummy = new ByteArrayOutputStream();
        OutputStream ansiout = AnsiConsole.wrapOutputStream(dummy);
        try {
            dummy.close();
            ansiout.close();
        } catch (IOException ignore) {
        }

        return (ansiout instanceof WindowsAnsiOutputStream);

    }

}
