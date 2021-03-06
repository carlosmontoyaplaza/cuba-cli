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

package com.haulmont.cuba.cli.cubaplugin.editscreen

import com.beust.jcommander.Parameters
import com.haulmont.cuba.cli.ModuleStructure
import com.haulmont.cuba.cli.Resources
import com.haulmont.cuba.cli.commands.NonInteractiveInfo
import com.haulmont.cuba.cli.cubaplugin.ScreenCommandBase
import com.haulmont.cuba.cli.generation.Properties
import com.haulmont.cuba.cli.generation.TemplateProcessor
import com.haulmont.cuba.cli.generation.getChildElements
import com.haulmont.cuba.cli.generation.parse
import com.haulmont.cuba.cli.prompting.Answers
import com.haulmont.cuba.cli.prompting.QuestionsList
import net.sf.practicalxml.DomUtil

@Parameters(commandDescription = "Creates new edit screen")
class CreateEditScreenCommand : ScreenCommandBase<EditScreenModel>(), NonInteractiveInfo {

    private val resources by Resources.fromMyPlugin()

    override fun getModelName(): String = EditScreenModel.MODEL_NAME

    override fun preExecute() {
        checkProjectExistence()
    }

    override fun getNonInteractiveParameters(): Map<String, String> = mapOf(
            "entityName" to "Fully qualified entity name",
            "packageName" to "Package name",
            "screenId" to "Screen id",
            "descriptorName" to "Descriptor name",
            "controllerName" to "Controller name"
    )

    override fun QuestionsList.prompting() {
        val persistenceXml = projectStructure.getModule(ModuleStructure.GLOBAL_MODULE).persistenceXml
        val entitiesList = parse(persistenceXml).documentElement
                .let { DomUtil.getChild(it, "persistence-unit") }
                .getChildElements()
                .filter { element -> element.tagName == "class" }
                .map { it.textContent.trim() }

        if (entitiesList.isEmpty())
            fail("Project does not have any entities.")

        options("entityName", "Choose entity", entitiesList)

        question("packageName", "Package name") {
            validate {
                checkIsPackage()
            }

            default { answers ->
                val entityName: String by answers

                val packageParts = entityName.split('.').filter { it != "entity" }
                packageParts.take(packageParts.lastIndex).joinToString(".") + ".web." + packageParts.last().toLowerCase()
            }
        }

        question("screenId", "Screen id") {
            default { answers ->
                val entityName: String by answers

                projectModel.namespace + "$" + entityName.split('.').last() + ".edit"
            }

            validate {
                screenIdDoesNotExists(value)
            }
        }

        question("descriptorName", "Descriptor name") {
            default { answers ->
                val entityName: String by answers

                entityName.split('.').last().toLowerCase() + "-edit"
            }

            validate {
                checkIsScreenDescriptor()

                screenDescriptorDoesNotExists(value)
            }
        }

        question("controllerName", "Controller name") {
            default { answers -> (answers["entityName"] as String).split('.').last() + "Edit" }

            validate {
                checkIsClass()

                screenControllerDoesNotExists(value)
            }
        }
    }

    override fun createModel(answers: Answers): EditScreenModel = EditScreenModel(answers)

    override fun beforeGeneration() {
        checkScreenId(model.screenId)
        checkExistence(model.packageName, descriptor = model.descriptorName, controller = model.controllerName)
    }

    override fun generate(bindings: Map<String, Any>) {
        TemplateProcessor(resources.getTemplate("editScreen"), bindings) {
            transformWhole()
        }

        val webModule = projectStructure.getModule(ModuleStructure.WEB_MODULE)

        addToScreensXml(model.screenId, model.packageName, model.descriptorName)

        val screenMessages = webModule.resolvePackagePath(model.packageName).resolve("messages.properties")
        Properties.modify(screenMessages) {
            set("editorCaption", model.entityName + " editor")
        }
    }
}