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

package org.jetbrains.jet.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.jet.codegen.ExpressionCodegen
import org.jetbrains.jet.codegen.StackValue
import org.jetbrains.jet.lang.psi.JetExpression

public class StringGetChar : LazyIntrinsicMethod() {
    override fun generateImpl(
            codegen: ExpressionCodegen,
            returnType: Type,
            element: PsiElement?,
            arguments: List<JetExpression>,
            receiver: StackValue
    ): StackValue {
        return StackValue.operation(Type.CHAR_TYPE) {
            if (receiver != StackValue.none()) {
                receiver.put(receiver.type, it)
            }
            if (!arguments.isEmpty()) {
                codegen.gen(arguments.first()).put(Type.INT_TYPE, it)
            }
            it.invokevirtual("java/lang/String", "charAt", "(I)C", false)
        }
    }
}
