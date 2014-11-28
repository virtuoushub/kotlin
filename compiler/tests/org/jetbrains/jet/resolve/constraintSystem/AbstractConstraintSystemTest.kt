/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.resolve.constraintSystem

import org.jetbrains.jet.ConfigurationKind
import org.jetbrains.jet.JetLiteFixture
import org.jetbrains.jet.JetTestUtils
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment
import org.jetbrains.jet.di.InjectorForTests
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.diagnostics.rendering.Renderers
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl
import org.jetbrains.jet.lang.types.Variance
import java.io.File
import java.util.regex.Pattern
import org.jetbrains.jet.resolve.constraintSystem.AbstractConstraintSystemTest.MyConstraintKind
import org.jetbrains.jet.resolve.constraintSystem.AbstractConstraintSystemTest.MyConstraint
import java.util.ArrayList
import java.util.LinkedHashMap
import org.jetbrains.jet.lang.resolve.lazy.JvmResolveUtil

abstract public class AbstractConstraintSystemTest() : JetLiteFixture() {
    private val typePattern = """([\w|<|>|\(|\)]+)"""
    val constraintPattern = Pattern.compile("""(SUBTYPE|SUPERTYPE)\s+${typePattern}\s+${typePattern}\s+(weak)?""", Pattern.MULTILINE)
    val variablesPattern = Pattern.compile("VARIABLES\\s+(.*)")

    private var _typeResolver: TypeResolver? = null
    private val typeResolver: TypeResolver
        get() = _typeResolver!!

    private var _myDeclarations: MyDeclarations? = null
    private val myDeclarations: MyDeclarations
        get() = _myDeclarations!!

    override fun createEnvironment(): JetCoreEnvironment {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL)
    }

    override fun setUp() {
        super.setUp()

        val injector = InjectorForTests(getProject(), JetTestUtils.createEmptyModule())
        _typeResolver = injector.getTypeResolver()!!
        _myDeclarations = analyzeDeclarations()
    }

    override fun tearDown() {
        _typeResolver = null
        _myDeclarations = null
        super<JetLiteFixture>.tearDown()
    }

    override fun getTestDataPath(): String {
        return super.getTestDataPath() + "/constraintSystem/"
    }

    private fun analyzeDeclarations(): MyDeclarations {
        val fileName = "declarations/declarations.kt"

        val psiFile = createPsiFile(null, fileName, loadFile(fileName))!!
        val bindingContext = JvmResolveUtil.analyzeOneFileWithJavaIntegrationAndCheckForErrors(psiFile).bindingContext
        return MyDeclarations(bindingContext, getProject(), typeResolver)
    }

    public fun doTest(filePath: String) {
        val file = File(filePath)
        val fileText = JetTestUtils.doLoadFile(file)!!

        val constraintSystem = ConstraintSystemImpl()

        val typeParameterDescriptors = LinkedHashMap<TypeParameterDescriptor, Variance>()
        val variables = parseVariables(fileText)
        for (variable in variables) {
            typeParameterDescriptors.put(myDeclarations.getParameterDescriptor(variable), Variance.INVARIANT)
        }
        constraintSystem.registerTypeVariables(typeParameterDescriptors)

        val constraints = parseConstraints(fileText)
        for (constraint in constraints) {
            val firstType = myDeclarations.getType(constraint.firstType)
            val secondType = myDeclarations.getType(constraint.secondType)
            val position = if (constraint.isWeak) ConstraintPosition.getTypeBoundPosition(0)!! else ConstraintPosition.SPECIAL
            when (constraint.kind) {
                MyConstraintKind.SUBTYPE -> constraintSystem.addSubtypeConstraint(firstType, secondType, position)
                MyConstraintKind.SUPERTYPE -> constraintSystem.addSupertypeConstraint(firstType, secondType, position)
            }
        }
        constraintSystem.processDeclaredBoundConstraints()

        val resultingStatus = Renderers.RENDER_CONSTRAINT_SYSTEM.render(constraintSystem)

        val resultingSubstitutor = constraintSystem.getResultingSubstitutor()
        val result = StringBuilder() append "result:\n"
        for ((typeParameter, variance) in typeParameterDescriptors) {
            val parameterType = myDeclarations.getType(typeParameter.getName().asString())
            val resultType = resultingSubstitutor.substitute(parameterType, variance)
            result append "${typeParameter.getName()}=${resultType?.let{ Renderers.RENDER_TYPE.render(it) }}\n"
        }

        JetTestUtils.assertEqualsToFile(file, "${getConstraintsText(fileText)}${resultingStatus}\n\n${result}\n")
    }

    class MyConstraint(val kind: MyConstraintKind, val firstType: String, val secondType: String, val isWeak: Boolean)
    enum class MyConstraintKind {
        SUBTYPE SUPERTYPE
    }

    private fun parseVariables(text: String): List<String> {
        val matcher = variablesPattern.matcher(text)
        if (matcher.find()) {
            val variablesText = matcher.group(1)!!
            val variables = variablesText.split(' ')
            return variables.toList()
        }
        throw AssertionError("Type variable names should be declared")
    }

    private fun parseConstraints(text: String): List<MyConstraint> {
        val constraints = ArrayList<MyConstraint>()
        val matcher = constraintPattern.matcher(text)
        while (matcher.find()) {
            val kind = MyConstraintKind.valueOf(matcher.group(1)!!)
            val firstType = matcher.group(2)!!
            val secondType = matcher.group(3)!!
            val isWeak = matcher.groupCount() == 4 && matcher.group(4) == "weak"
            constraints.add(MyConstraint(kind, firstType, secondType, isWeak))
        }
        return constraints
    }

    private fun getConstraintsText(text: String) = text.substring(0, text.indexOf("type parameter bounds"))
}



