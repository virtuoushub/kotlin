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

package org.jetbrains.jet.plugin.refactoring.changeSignature

import com.google.common.collect.Sets
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.OverridingMethodsSearch
import com.intellij.refactoring.changeSignature.MethodDescriptor
import com.intellij.refactoring.changeSignature.OverriderUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.util.Function
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.jet.asJava.KotlinLightMethod
import org.jetbrains.jet.asJava.LightClassUtil
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import org.jetbrains.jet.plugin.refactoring.changeSignature.usages.JetFunctionDefinitionUsage
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers

import java.util.*
import kotlin.properties.Delegates

public class JetChangeSignatureData(
        private val baseDescriptor: FunctionDescriptor,
        private val baseDeclaration: PsiElement,
        private val descriptorsForSignatureChange: Collection<FunctionDescriptor>
) : JetMethodDescriptor {
    val _parameters: MutableList<JetParameterInfo> by Delegates.lazy {
        val valueParameters = when {
            baseDeclaration is JetFunction -> baseDeclaration.getValueParameters()
            baseDeclaration is JetClass -> baseDeclaration.getPrimaryConstructorParameters()
            else -> null
        }
        ArrayList(ContainerUtil.map<ValueParameterDescriptor, JetParameterInfo>(this.baseDescriptor.getValueParameters(), object : Function<ValueParameterDescriptor, JetParameterInfo> {
            override fun `fun`(param: ValueParameterDescriptor): JetParameterInfo {
                val parameter = if (valueParameters != null) valueParameters.get(param.getIndex()) else null
                return JetParameterInfo(param.getIndex(), param.getName().asString(), param.getType(), if (parameter != null) parameter.getDefaultValue() else null, if (parameter != null) parameter.getValOrVarNode() else null)
            }
        }))
    }

    override val affectedFunctions: Collection<UsageInfo> by Delegates.lazy {
        descriptorsForSignatureChange.flatMapTo(HashSet<UsageInfo>()) { descriptor -> computeOverrides(descriptor) }
    }

    private fun computeOverrides(descriptor: FunctionDescriptor): HashSet<UsageInfo> {
        val declaration = DescriptorToDeclarationUtil.getDeclaration(baseDeclaration.getProject(), descriptor)
        assert(declaration != null) { "No declaration found for " + descriptor }

        val result = Sets.newHashSet<UsageInfo>()
        result.add(JetFunctionDefinitionUsage(declaration, descriptor, false))

        if (declaration !is JetNamedFunction) return result

        // there are valid situations when light method is null: local functions and literals
        val baseLightMethod = LightClassUtil.getLightClassMethod(declaration) ?: return result

        return OverridingMethodsSearch.search(baseLightMethod).map { method ->
            when (method) {
                is KotlinLightMethod -> method.origin?.let { JetFunctionDefinitionUsage(it, descriptor, true) }
                else -> OverriderUsageInfo(method, baseLightMethod, true, true, true)
            }
        }.filterNotNullTo(result)
    }

    override fun getParameters(): MutableList<JetParameterInfo>? = _parameters

    public fun addParameter(parameter: JetParameterInfo) {
        _parameters.add(parameter)
    }

    public fun removeParameter(index: Int) {
        _parameters.remove(index)
    }

    public fun clearParameters() {
        _parameters.clear()
    }

    override fun getName(): String {
        return when (baseDescriptor) {
            is ConstructorDescriptor -> baseDescriptor.getContainingDeclaration().getName().asString()
            is AnonymousFunctionDescriptor -> ""
            else -> baseDescriptor.getName().asString()
        }
    }

    override fun getParametersCount(): Int = baseDescriptor.getValueParameters().size()

    override fun getVisibility(): Visibility = baseDescriptor.getVisibility()

    override fun getMethod(): PsiElement = baseDeclaration

    override fun canChangeVisibility(): Boolean {
        val parent = baseDescriptor.getContainingDeclaration()
        return !(baseDescriptor is AnonymousFunctionDescriptor || parent is ClassDescriptor && parent.getKind() == ClassKind.TRAIT)
    }

    override fun canChangeParameters(): Boolean {
        return true
    }

    override fun canChangeName(): Boolean {
        return !(baseDescriptor is ConstructorDescriptor || baseDescriptor is AnonymousFunctionDescriptor)
    }

    override fun canChangeReturnType(): MethodDescriptor.ReadWriteOption {
        return if (baseDescriptor is ConstructorDescriptor) MethodDescriptor.ReadWriteOption.None else MethodDescriptor.ReadWriteOption.ReadWrite
    }

    override val isConstructor: Boolean get() = baseDescriptor is ConstructorDescriptor

    override val context: PsiElement get() = baseDeclaration

    override val descriptor: FunctionDescriptor? get() = baseDescriptor

    override fun getReturnTypeText(): String? =
            baseDescriptor.getReturnType()?.let { IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_IN_TYPES.renderType(it) }
}
