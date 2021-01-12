/*
 ******************************************************************************
 * Copyright (c) 2013, 2015 EclipseSource.
 * Copyright (c) 2015-2017 Christian Zangl
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package org.hjson;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

class NewHjsonParser {

    /** The raw string itself. */
    private final String buffer;

    /** The source reader for the entire string. */
    private final Reader reader;

    /** Stores the entire string up to this point. */
    private final StringBuilder captureBuffer;

    /** Stores any values not yet being captured. */
    private final StringBuilder peek;

    /** Whether to ignore root braces. */
    private final boolean legacyRoot;

    /** Domain specific formats being supported. */
    private final IHjsonDsfProvider[] dsfProviders;

    /** Current position in the reader. */
    private int index;

    /** Current line number. */
    private int line;

    /** Current position on this line. */
    private int lineOffset;

    /** Current character being parsed. */
    private int current;

    /** Whether to capture the current character at this moment. */
    private boolean capture;

    /** The buffer capacity used in reading strings. */
    private static final int BUFFER_SIZE = 8 * 1024;

    NewHjsonParser(String string, HjsonOptions options) {
        if (options != null) {
            this.dsfProviders = options.getDsfProviders();
            this.legacyRoot = options.getParseLegacyRoot();
        } else {
            this.dsfProviders = new IHjsonDsfProvider[0];
            this.legacyRoot = true;
        }
        this.buffer = string;
        this.peek = new StringBuilder();
        this.captureBuffer = new StringBuilder();
        this.reader = new StringReader(string);
        this.index = 0;
        this.lineOffset = 0;
        this.current = 0;
        this.line = 1;
        this.capture = false;
    }

    NewHjsonParser(Reader reader, HjsonOptions options) throws IOException {
        this(readToEnd(reader), options);
    }

    private static String readToEnd(Reader reader) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final char[] buffer = new char[BUFFER_SIZE];

        int n;
        while ((n = reader.read(buffer, 0, BUFFER_SIZE)) != -1) {
            sb.append(buffer, 0, n);
        }
        return sb.toString();
    }

    private static class ContainerData {
        private int lineLength = 1;
        private int sumLineLength = 0;
        private int numLines = 0;
        private boolean condensed;

        private ContainerData(boolean condensed) {
            this.condensed = condensed;
        }

        private void incrLineLength() {
            lineLength++;
        }

        private void overrideCondensed() {
            condensed=false;
        }

        private void nl() {
            sumLineLength += lineLength;
            lineLength = 1;
            numLines++;
        }

        private int finalLineLength(int size) {
            return sumLineLength > 0 ? avgLineLength() : condensed ? size : 1;
        }

        private int avgLineLength() {
            int avgLineLength = sumLineLength / numLines;
            if (avgLineLength <= 0) {
                avgLineLength = 1;
            }
            return avgLineLength;
        }

        private JsonArray into(JsonArray array) {
            return array
                .setLineLength(finalLineLength(array.size()))
                .setCondensed(condensed);
        }

        private JsonObject into(JsonObject object) {
            return object
                .setLineLength(finalLineLength(object.size()))
                .setCondensed(condensed);
        }
    }
}
