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

package org.jetbrains.jet.plugin.completion

import org.jetbrains.jet.lang.psi.JetValueArgument
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetCallElement
import org.jetbrains.jet.plugin.references.JetReference
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import com.intellij.codeInsight.lookup.LookupElementBuilder
import org.jetbrains.jet.plugin.JetIcons
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import org.jetbrains.jet.renderer.DescriptorRenderer
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.psi.filters.AndFilter
import org.jetbrains.jet.lang.psi.JetValueArgumentName
import com.intellij.psi.filters.position.ParentElementFilter
import com.intellij.psi.filters.OrFilter
import com.intellij.psi.filters.ClassFilter
import org.jetbrains.jet.plugin.util.FirstChildInParentFilter
import org.jetbrains.jet.lang.psi.psiUtil.getCallNameExpression
import com.intellij.psi.PsiElement
import com.intellij.codeInsight.completion.InsertHandler
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers
import org.jetbrains.jet.plugin.completion.handlers.WithTailInsertHandler
import org.jetbrains.jet.lang.psi.psiUtil.getStrictParentOfType

object NamedParametersCompletion {
    private val positionFilter = AndFilter(
            LeafElementFilter(JetTokens.IDENTIFIER),
            OrFilter(
                    AndFilter(
                            ParentElementFilter(ClassFilter(javaClass<JetValueArgument>()), 2),
                            FirstChildInParentFilter(2)
                    ),
                    ParentElementFilter(ClassFilter(javaClass<JetValueArgumentName>()), 2)
            )
    )

    public fun isOnlyNamedParameterExpected(position: PsiElement): Boolean {
        if (!positionFilter.isAcceptable(position, position)) return false

        val thisArgument = position.getStrictParentOfType<JetValueArgument>()!!

        val callElement = thisArgument.getStrictParentOfType<JetCallElement>() ?: return false

        for (argument in callElement.getValueArguments()) {
            if (argument == thisArgument) break
            if (argument.isNamed()) return true
        }

        return false
    }

    public fun complete(position: PsiElement, collector: LookupElementsCollector) {
        if (!positionFilter.isAcceptable(position, position)) return

        val valueArgument = position.getStrictParentOfType<JetValueArgument>()!!

        val callElement = valueArgument.getStrictParentOfType<JetCallElement>() ?: return
        val callSimpleName = callElement.getCallNameExpression() ?: return

        val callReference = callSimpleName.getReference() as JetReference

        val functionDescriptors = callReference.resolveToDescriptors().map { it as? FunctionDescriptor }.filterNotNull()

        for (funDescriptor in functionDescriptors) {
            if (!funDescriptor.hasStableParameterNames()) continue

            val usedArguments = QuickFixUtil.getUsedParameters(callElement, valueArgument, funDescriptor)

            for (parameter in funDescriptor.getValueParameters()) {
                val name = parameter.getName()
                val nameString = name.asString()
                if (nameString !in usedArguments) {
                    val lookupElement = LookupElementBuilder.create(nameString)
                            .withPresentableText("$nameString =")
                            .withTailText(" ${DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(parameter.getType())}")
                            .withIcon(JetIcons.PARAMETER)
                            .withInsertHandler(NamedParameterInsertHandler(name))
                            .assignPriority(ItemPriority.NAMED_PARAMETER)
                    collector.addElement(lookupElement)
                }
            }
        }
    }

    private class NamedParameterInsertHandler(val parameterName: Name) : InsertHandler<LookupElement> {
        override fun handleInsert(context: InsertionContext, item: LookupElement) {
            val editor = context.getEditor()
            val text = IdeDescriptorRenderers.SOURCE_CODE.renderName(parameterName)
            editor.getDocument().replaceString(context.getStartOffset(), context.getTailOffset(), text)
            editor.getCaretModel().moveToOffset(context.getStartOffset() + text.length)

            WithTailInsertHandler.eqTail().postHandleInsert(context, item)
        }
    }
}