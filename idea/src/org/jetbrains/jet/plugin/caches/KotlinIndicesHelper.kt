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

package org.jetbrains.jet.plugin.caches

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.plugin.stubindex.*
import org.jetbrains.jet.lang.resolve.lazy.ResolveSessionUtils
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.psiUtil.getReceiverExpression
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import com.intellij.openapi.project.Project
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.jet.lang.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver.LookupMode
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.calls.smartcasts.DataFlowInfo
import com.intellij.psi.stubs.StringStubIndexExtension
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import org.jetbrains.jet.plugin.util.extensionsUtils.isExtensionCallable

public class KotlinIndicesHelper(
        private val project: Project,
        private val resolutionFacade: ResolutionFacade,
        private val bindingContext: BindingContext,
        private val scope: GlobalSearchScope,
        private val moduleDescriptor: ModuleDescriptor,
        private val visibilityFilter: (DeclarationDescriptor) -> Boolean
) {
    public fun getTopLevelCallablesByName(name: String, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return listOf()

        val result = HashSet<CallableDescriptor>()

        //TODO: this code is temporary and is to be dropped when compiled top level functions are indexed
        val identifier = Name.identifier(name)
        for (fqName in JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false)) {
            if (fqName.lastSegmentIs(identifier)) {
                result.addAll(findTopLevelCallables(fqName, context, jetScope))
            }
        }

        result.addTopLevelNonExtensionCallablesByName(JetFunctionShortNameIndex.getInstance(), name)
        result.addTopLevelNonExtensionCallablesByName(JetPropertyShortNameIndex.getInstance(), name)

        return result.filter(visibilityFilter)
    }

    private fun MutableSet<CallableDescriptor>.addTopLevelNonExtensionCallablesByName(
            index: StringStubIndexExtension<out JetCallableDeclaration>,
            name: String
    ) {
        index.get(name, project, scope)
                .filter { it.getParent() is JetFile && it.getReceiverTypeReference() == null }
                .mapTo(this) { resolutionFacade.resolveToDescriptor(it) as CallableDescriptor }
    }

    public fun getTopLevelCallables(nameFilter: (String) -> Boolean, context: JetExpression /*TODO: to be dropped*/): Collection<CallableDescriptor> {
        val sourceNames = JetTopLevelFunctionFqnNameIndex.getInstance().getAllKeys(project).stream() + JetTopLevelPropertyFqnNameIndex.getInstance().getAllKeys(project).stream()
        val allFqNames = sourceNames.map { FqName(it) } + JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, false).stream()

        val jetScope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return listOf()

        return allFqNames.filter { nameFilter(it.shortName().asString()) }
                .toSet()
                .flatMap { findTopLevelCallables(it, context, jetScope).filter(visibilityFilter) }
    }

    public fun getCallableExtensions(nameFilter: (String) -> Boolean, expression: JetSimpleNameExpression): Collection<CallableDescriptor> {
        val dataFlowInfo = bindingContext.getDataFlowInfo(expression)

        val functionsIndex = JetTopLevelFunctionFqnNameIndex.getInstance()
        val propertiesIndex = JetTopLevelPropertyFqnNameIndex.getInstance()

        val sourceFunctionNames = functionsIndex.getAllKeys(project).stream().map { FqName(it) }
        val sourcePropertyNames = propertiesIndex.getAllKeys(project).stream().map { FqName(it) }
        val compiledFqNames = JetFromJavaDescriptorHelper.getTopLevelCallableFqNames(project, scope, true).stream()

        val result = HashSet<CallableDescriptor>()
        result.fqNamesToSuitableExtensions(sourceFunctionNames, nameFilter, functionsIndex, expression, bindingContext, dataFlowInfo)
        result.fqNamesToSuitableExtensions(sourcePropertyNames, nameFilter, propertiesIndex, expression, bindingContext, dataFlowInfo)
        result.fqNamesToSuitableExtensions(compiledFqNames, nameFilter, null, expression, bindingContext, dataFlowInfo)
        return result
    }

    private fun MutableCollection<CallableDescriptor>.fqNamesToSuitableExtensions(
            fqNames: Stream<FqName>,
            nameFilter: (String) -> Boolean,
            index: StringStubIndexExtension<out JetCallableDeclaration>?,
            expression: JetSimpleNameExpression,
            bindingContext: BindingContext,
            dataFlowInfo: DataFlowInfo) {
        val matchingNames = fqNames.filter { nameFilter(it.shortName().asString()) }

        val receiverExpression = expression.getReceiverExpression()
        if (receiverExpression != null) {
            val isInfixCall = expression.getParent() is JetBinaryExpression

            val expressionType = bindingContext[BindingContext.EXPRESSION_TYPE, receiverExpression]
            if (expressionType == null || expressionType.isError()) return

            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, receiverExpression] ?: return

            val receiverValue = ExpressionReceiver(receiverExpression, expressionType)

            matchingNames.flatMapTo(this) {
                findSuitableExtensions(it, index, receiverValue, dataFlowInfo, isInfixCall, resolutionScope, moduleDescriptor, bindingContext).stream()
            }
        }
        else {
            val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, expression] ?: return

            for (receiver in resolutionScope.getImplicitReceiversHierarchy()) {
                matchingNames.flatMapTo(this) {
                    findSuitableExtensions(it, index, receiver.getValue(), dataFlowInfo, false, resolutionScope, moduleDescriptor, bindingContext).stream()
                }
            }
        }
    }

    /**
     * Check that function or property with the given qualified name can be resolved in given scope and called on given receiver
     */
    private fun findSuitableExtensions(callableFQN: FqName,
                                       index: StringStubIndexExtension<out JetCallableDeclaration>?,
                                       receiverValue: ReceiverValue,
                                       dataFlowInfo: DataFlowInfo,
                                       isInfixCall: Boolean,
                                       resolutionScope: JetScope,
                                       module: ModuleDescriptor,
                                       bindingContext: BindingContext): Collection<CallableDescriptor> {
        val fqnString = callableFQN.asString()
        val descriptors = if (index != null) {
            index.get(fqnString, project, scope)
                    .filter { it.getReceiverTypeReference() != null }
                    .map { resolutionFacade.resolveToDescriptor(it) as CallableDescriptor }
        }
        else {
            val importDirective = JetPsiFactory(project).createImportDirective(fqnString)
            analyzeImportReference(importDirective, resolutionScope, BindingTraceContext(), module)
                    .filterIsInstance<CallableDescriptor>()
                    .filter { it.getExtensionReceiverParameter() != null }
        }

        return descriptors.filter {
            visibilityFilter(it) && it.isExtensionCallable(receiverValue, isInfixCall, bindingContext, dataFlowInfo)
        }
    }

    public fun getClassDescriptors(nameFilter: (String) -> Boolean, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        return JetFullClassNameIndex.getInstance().getAllKeys(project).stream()
                .map { FqName(it) }
                .filter { nameFilter(it.shortName().asString()) }
                .toList()
                .flatMap { getClassDescriptorsByFQName(it, kindFilter) }
    }

    private fun getClassDescriptorsByFQName(classFQName: FqName, kindFilter: (ClassKind) -> Boolean): Collection<ClassDescriptor> {
        val declarations = JetFullClassNameIndex.getInstance()[classFQName.asString(), project, scope]

        if (declarations.isEmpty()) {
            // This fqn is absent in caches, dead or not in scope
            return listOf()
        }

        // Note: Can't search with psi element as analyzer could be built over temp files
        return ResolveSessionUtils.getClassOrObjectDescriptorsByFqName(moduleDescriptor, classFQName) { kindFilter(it.getKind()) }
                .filter(visibilityFilter)
    }

    private fun findTopLevelCallables(fqName: FqName, context: JetExpression, jetScope: JetScope): Collection<CallableDescriptor> {
        val importDirective = JetPsiFactory(context.getProject()).createImportDirective(ImportPath(fqName, false))
        val allDescriptors = analyzeImportReference(importDirective, jetScope, BindingTraceContext(), moduleDescriptor)
        return allDescriptors.filterIsInstance<CallableDescriptor>().filter { it.getExtensionReceiverParameter() == null }
    }

    private fun analyzeImportReference(
            importDirective: JetImportDirective, scope: JetScope, trace: BindingTrace, module: ModuleDescriptor
    ): Collection<DeclarationDescriptor> {
        return QualifiedExpressionResolver().processImportReference(importDirective, scope, scope, Importer.DO_NOTHING, trace,
                                                                    module, LookupMode.EVERYTHING)
    }
}