/*
 * Copyright (c) 2008-2018 Haulmont.
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

package com.haulmont.cuba.cli.cubaplugin.screen

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.commands.AbstractCommand
import com.haulmont.cuba.cli.kodein
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Screen related commands")
object ScreenCommandsGroup : AbstractCommand() {

    private val printWriter: PrintWriter by kodein.instance()

    override fun run() {
        printWriter.println("Use 'custom','extend','browse' or 'edit' subcommands.")
    }

}