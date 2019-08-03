package ru.mail.condef.renderer
/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

import java.io.IOException
import java.io.StringWriter
import java.io.Writer
import java.util.Locale

/**
 * Escapes and unescapes `String`s for
 * Java, Java Script, HTML, XML, and SQL.
 *
 *
 * #ThreadSafe#
 *
 *
 * **Note that this version is a stripped down version from Commons Lang 2.6 with only necessary methods for
 * JSON builder**
 * @author Apache Software Foundation
 * @author Apache Jakarta Turbine
 * @author Purple Technology
 * @author [Alexander Day Chaffee](mailto:alex@purpletech.com)
 * @author Antony Riley
 * @author Helge Tesgaard
 * @author [Sean Brown](sean@boohai.com)
 * @author [Gary Gregory](mailto:ggregory@seagullsw.com)
 * @author Phil Steitz
 * @author Pete Gieser
 * @since 2.0
 */
/**
 *
 * `StringEscapeUtils` instances should NOT be constructed in
 * standard programming.
 *
 *
 * Instead, the class should be used as:
 * <pre>StringEscapeUtils.escapeJava("foo");</pre>
 *
 *
 * This constructor is public to permit tools that require a JavaBean
 * instance to operate.
 */
class StringEscapeUtils {
    companion object {

        /**
         * Escapes the characters in a `String` using Java String rules.
         *
         *
         * Deals correctly with quotes and control-chars (tab, backslash, cr, ff, etc.)
         *
         *
         * So a tab becomes the characters `'\\'` and `'t'`.
         *
         *
         * The only difference between Java strings and JavaScript strings
         * is that in JavaScript, a single quote must be escaped.
         *
         *
         * Example:
         * <pre>
         * input string: He didn't say, "Stop!"
         * output string: He didn't say, \"Stop!\"
        </pre> *
         *
         * @param str  String to escape values in, may be null
         * @return String with escaped values, `null` if null string input
         */
        fun escapeJava(str: String): String? {
            return escapeJavaStyleString(str, false, false)
        }

        /**
         * Worker method for the [.escapeJavaScript] method.
         *
         * @param str String to escape values in, may be null
         * @param escapeSingleQuotes escapes single quotes if `true`
         * @param escapeForwardSlash TODO
         * @return the escaped string
         */
        private fun escapeJavaStyleString(str: String?, escapeSingleQuotes: Boolean, escapeForwardSlash: Boolean): String? {
            if (str == null) {
                return null
            }
            try {
                val writer = StringWriter(str.length * 2)
                escapeJavaStyleString(writer, str, escapeSingleQuotes, escapeForwardSlash)
                return writer.toString()
            } catch (ioe: IOException) {
                // this should never ever happen while writing to a StringWriter
                throw RuntimeException(ioe)
            }

        }

        /**
         * Worker method for the [.escapeJavaScript] method.
         *
         * @param out write to receieve the escaped string
         * @param str String to escape values in, may be null
         * @param escapeSingleQuote escapes single quotes if `true`
         * @param escapeForwardSlash TODO
         * @throws IOException if an IOException occurs
         */
        @Throws(IOException::class)
        private fun escapeJavaStyleString(out: Writer?, str: String?, escapeSingleQuote: Boolean,
                                          escapeForwardSlash: Boolean) {
            if (out == null) {
                throw IllegalArgumentException("The Writer must not be null")
            }
            if (str == null) {
                return
            }
            val sz: Int = str.length
            for (i in 0 until sz) {
                val ch = str[i]

                // handle unicode
                when {
                    ch.toInt() > 0xfff -> out.write("\\u" + hex(ch))
                    ch.toInt() > 0xff -> out.write("\\u0" + hex(ch))
                    ch.toInt() > 0x7f -> out.write("\\u00" + hex(ch))
                    ch.toInt() < 32 -> when (ch) {
                        '\b' -> {
                            out.write('\\'.toInt())
                            out.write('b'.toInt())
                        }
                        '\n' -> {
                            out.write('\\'.toInt())
                            out.write('n'.toInt())
                        }
                        '\t' -> {
                            out.write('\\'.toInt())
                            out.write('t'.toInt())
                        }
                        '\r' -> {
                            out.write('\\'.toInt())
                            out.write('r'.toInt())
                        }
                        else -> if (ch.toInt() > 0xf) {
                            out.write("\\u00" + hex(ch))
                        } else {
                            out.write("\\u000" + hex(ch))
                        }
                    }
                    else -> when (ch) {
                        '\'' -> {
                            if (escapeSingleQuote) {
                                out.write('\\'.toInt())
                            }
                            out.write('\''.toInt())
                        }
                        '"' -> {
                            out.write('\\'.toInt())
                            out.write('"'.toInt())
                        }
                        '\\' -> {
                            out.write('\\'.toInt())
                            out.write('\\'.toInt())
                        }
                        '/' -> {
                            if (escapeForwardSlash) {
                                out.write('\\'.toInt())
                            }
                            out.write('/'.toInt())
                        }
                        else -> out.write(ch.toInt())
                    }
                }
            }
        }

        /**
         * Returns an upper case hexadecimal `String` for the given
         * character.
         *
         * @param ch The character to convert.
         * @return An upper case hexadecimal `String`
         */
        private fun hex(ch: Char): String {
            return Integer.toHexString(ch.toInt()).toUpperCase(Locale.ENGLISH)
        }
    }
}