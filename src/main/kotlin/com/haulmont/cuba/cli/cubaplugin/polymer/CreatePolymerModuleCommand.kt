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

package com.haulmont.cuba.cli.cubaplugin.polymer

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.*
import com.haulmont.cuba.cli.commands.GeneratorCommand
import com.haulmont.cuba.cli.generation.Snippets
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import org.kodein.di.generic.instance
import java.io.PrintWriter

@Parameters(commandDescription = "Creates Polymer module")
class CreatePolymerModuleCommand : GeneratorCommand<PolymerModel>() {
    private val messages by localMessages()

    private val writer: PrintWriter by kodein.instance()

    private val resources by Resources.fromMyPlugin()

    private val snippets: Snippets by lazy {
        Snippets(resources, "polymer", projectModel.platformVersion)
    }

    private val printHelper: PrintHelper by kodein.instance()

    override fun getModelName(): String = "polymer"

    override fun preExecute() {
        checkProjectExistence()

        val alreadyContainsModule = projectStructure.settingsGradle.toFile()
                .readText().contains("polymer-client")

        !alreadyContainsModule || fail(messages["moduleExistsError"])
    }

    override fun QuestionsList.prompting() {
        confirmation("confirm", messages["confirmationCaption"]) {
            default(true)
        }
    }

    override fun createModel(answers: Answers): PolymerModel = if (answers["confirm"] as Boolean) {
        PolymerModel(projectModel)
    } else fail("rejected", true)


    override fun generate(bindings: Map<String, Any>) {
        val destinationDir = projectStructure.path.resolve("modules/polymer-client")

        val maybeHints = TemplateProcessor(resources.getTemplate("polymer"), bindings) {
            templatePath.walk(1).filter {
                it.fileName.toString() != "images" && it != templatePath
            }.forEach {
                transform(it, to = destinationDir)
            }
            copy("images", to = destinationDir)
        }

        projectStructure.buildGradle.toFile().apply {
            readText()
                    .replace(snippets["moduleVarSearch"], snippets["moduleVarReplace"])
                    .replace(snippets["moduleConfigureSearch"], snippets["moduleConfigureReplace"])
                    .replace(snippets["undeploySearch"], snippets["undeployReplace"])
                    .let {
                        writeText(it)
                    }
        }
        printHelper.fileModified(projectStructure.buildGradle)


        projectStructure.settingsGradle.toFile().apply {
            val lines = readLines().map {
                if (it.startsWith("include(")) {
                    it.replace(Regex("include\\((.*)\\)")) {
                        val modules = it.groupValues[1].split(",")
                        (modules + "\":\${modulePrefix}-polymer-client\"").joinToString(" ,", "include(", ")")
                    }
                } else it
            } + snippets["moduleRegistration"]
            writeText(lines.joinToString("\n"))
        }
        printHelper.fileModified(projectStructure.settingsGradle)

        maybeHints?.let { writer.println(it) }
    }
}