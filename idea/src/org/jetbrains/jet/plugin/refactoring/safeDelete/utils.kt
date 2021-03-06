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

package org.jetbrains.jet.plugin.refactoring.safeDelete

import org.jetbrains.jet.lang.psi.JetNamedFunction
import com.intellij.psi.PsiMethod
import org.jetbrains.jet.lang.psi.JetModifierListOwner
import org.jetbrains.jet.lang.psi.JetProperty
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.psi.JetPsiUtil
import com.intellij.psi.search.searches.OverridingMethodsSearch
import org.jetbrains.jet.asJava.unwrapped
import java.util.ArrayList
import org.jetbrains.jet.lang.psi.JetParameter
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.psi.JetPropertyAccessor
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.psi.JetTypeParameter
import org.jetbrains.jet.lang.psi.psiUtil.*

public fun PsiElement.canDeleteElement(): Boolean {
    if (isObjectLiteral()) return false

    if (this is JetParameter) {
        val declaration = getStrictParentOfType<JetDeclaration>()
        return declaration != null && !(declaration is JetPropertyAccessor && declaration.isSetter())
    }

    return this is JetClassOrObject
        || this is JetNamedFunction
        || this is PsiMethod
        || this is JetProperty
        || this is JetTypeParameter
}

fun PsiElement.removeOverrideModifier() {
    when (this) {
        is JetNamedFunction, is JetProperty -> {
            (this as JetModifierListOwner).getModifierList()?.getModifier(JetTokens.OVERRIDE_KEYWORD)?.delete()
        }
        is PsiMethod -> {
            getModifierList().getAnnotations().find {
                annotation -> annotation.getQualifiedName() == "java.lang.Override"
            }?.delete()
        }
    }
}

fun PsiMethod.cleanUpOverrides() {
    val superMethods = findSuperMethods(true)
    for (overridingMethod in OverridingMethodsSearch.search(this, true).findAll()) {
        val currentSuperMethods = overridingMethod.findSuperMethods(true).stream() + superMethods.stream()
        if (currentSuperMethods.all { superMethod -> superMethod.unwrapped == unwrapped }) {
            overridingMethod.unwrapped?.removeOverrideModifier()
        }
    }
}