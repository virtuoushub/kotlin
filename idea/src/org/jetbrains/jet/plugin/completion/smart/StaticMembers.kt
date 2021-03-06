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

package org.jetbrains.jet.plugin.completion.smart

import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import com.intellij.codeInsight.lookup.LookupElementDecorator
import com.intellij.codeInsight.lookup.LookupElementPresentation
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.completion.InsertionContext
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.completion.ExpectedInfo
import org.jetbrains.jet.plugin.util.makeNotNullable
import org.jetbrains.jet.plugin.completion.qualifiedNameForSourceCode
import org.jetbrains.jet.lang.resolve.descriptorUtil.isExtension
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers
import org.jetbrains.jet.plugin.caches.resolve.ResolutionFacade
import org.jetbrains.jet.plugin.completion.LookupElementFactory

// adds java static members, enum members and members from class object
class StaticMembers(val bindingContext: BindingContext, val resolutionFacade: ResolutionFacade) {
    public fun addToCollection(collection: MutableCollection<LookupElement>,
                               expectedInfos: Collection<ExpectedInfo>,
                               context: JetExpression,
                               enumEntriesToSkip: Set<DeclarationDescriptor>) {

        val scope = bindingContext[BindingContext.RESOLUTION_SCOPE, context] ?: return

        val expectedInfosByClass = expectedInfos.groupBy { TypeUtils.getClassDescriptor(it.type) }
        for ((classDescriptor, expectedInfosForClass) in expectedInfosByClass) {
            if (classDescriptor != null && !classDescriptor.getName().isSpecial()) {
                addToCollection(collection, classDescriptor, expectedInfosForClass, scope, enumEntriesToSkip)
            }
        }
    }

    private fun addToCollection(
            collection: MutableCollection<LookupElement>,
            classDescriptor: ClassDescriptor,
            expectedInfos: Collection<ExpectedInfo>,
            scope: JetScope,
            enumEntriesToSkip: Set<DeclarationDescriptor>) {

        fun processMember(descriptor: DeclarationDescriptor) {
            if (descriptor is DeclarationDescriptorWithVisibility && !Visibilities.isVisible(descriptor, scope.getContainingDeclaration())) return

            val classifier: (ExpectedInfo) -> ExpectedInfoClassification
            if (descriptor is CallableDescriptor) {
                val returnType = descriptor.getReturnType() ?: return
                classifier = {
                    expectedInfo ->
                        when {
                            returnType.isSubtypeOf(expectedInfo.type) -> ExpectedInfoClassification.MATCHES
                            returnType.isNullable() && returnType.makeNotNullable().isSubtypeOf(expectedInfo.type) -> ExpectedInfoClassification.MAKE_NOT_NULLABLE
                            else -> ExpectedInfoClassification.NOT_MATCHES
                        }
                }
            }
            else if (DescriptorUtils.isEnumEntry(descriptor) && !enumEntriesToSkip.contains(descriptor)) {
                classifier = { ExpectedInfoClassification.MATCHES } /* we do not need to check type of enum entry because it's taken from proper enum */
            }
            else {
                return
            }

            collection.addLookupElements(expectedInfos, classifier, { createLookupElement(descriptor, classDescriptor) })
        }

        classDescriptor.getStaticScope().getAllDescriptors().forEach(::processMember)

        val classObject = classDescriptor.getClassObjectDescriptor()
        if (classObject != null) {
            classObject.getDefaultType().getMemberScope().getAllDescriptors()
                    .filter { !it.isExtension }
                    .forEach(::processMember)
        }

        var members = classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()
        if (classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
            members = members.filter { DescriptorUtils.isObject(it) }
        }
        members.forEach(::processMember)
    }

    private fun createLookupElement(memberDescriptor: DeclarationDescriptor, classDescriptor: ClassDescriptor): LookupElement {
        val lookupElement = LookupElementFactory.DEFAULT.createLookupElement(memberDescriptor, resolutionFacade, bindingContext)
        val qualifierPresentation = classDescriptor.getName().asString()
        val qualifierText = qualifiedNameForSourceCode(classDescriptor)

        return object: LookupElementDecorator<LookupElement>(lookupElement) {
            override fun getAllLookupStrings(): Set<String> {
                return setOf(lookupElement.getLookupString(), qualifierPresentation)
            }

            override fun renderElement(presentation: LookupElementPresentation) {
                getDelegate().renderElement(presentation)

                presentation.setItemText(qualifierPresentation + "." + presentation.getItemText())

                val tailText = " (" + DescriptorUtils.getFqName(classDescriptor.getContainingDeclaration()) + ")"
                if (memberDescriptor is FunctionDescriptor) {
                    presentation.appendTailText(tailText, true)
                }
                else {
                    presentation.setTailText(tailText, true)
                }

                if (presentation.getTypeText().isNullOrEmpty()) {
                    presentation.setTypeText(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(classDescriptor.getDefaultType()))
                }
            }

            override fun handleInsert(context: InsertionContext) {
                var text = qualifierText + "." + IdeDescriptorRenderers.SOURCE_CODE.renderName(memberDescriptor.getName())

                context.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), text)
                context.setTailOffset(context.getStartOffset() + text.length)

                if (memberDescriptor is FunctionDescriptor) {
                    getDelegate().handleInsert(context)
                }

                shortenReferences(context, context.getStartOffset(), context.getTailOffset())
            }
        }.assignSmartCompletionPriority(SmartCompletionItemPriority.STATIC_MEMBER)
    }
}
