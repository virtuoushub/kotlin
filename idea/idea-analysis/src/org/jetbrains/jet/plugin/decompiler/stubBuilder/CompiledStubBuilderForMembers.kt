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

package org.jetbrains.jet.plugin.decompiler.stubBuilder

import org.jetbrains.jet.descriptors.serialization.NameResolver
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.PsiElement
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.descriptors.serialization.Flags
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Modality
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Callable.CallableKind
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.name.FqName

//TODO: name
public class CompiledStubBuilderForMembers(
        context: ClsStubBuilderContext
) : CompiledStubBuilderBase(context) {

    public fun createCallableStub(
            parentStub: StubElement<out PsiElement>,
            callableProto: ProtoBuf.Callable,
            isTopLevel: Boolean
    ) {
        val callableStub = doCreateCallableStub(callableProto, parentStub, isTopLevel)
        createModifierListStubForDeclaration(callableStub, callableProto.getFlags(), ignoreModality = isTopLevel)
        if (callableProto.hasReceiverType()) {
            createTypeReferenceStub(callableStub, callableProto.getReceiverType())
        }
        createValueParametersStub(callableStub, callableProto)
        createTypeReferenceStub(callableStub, callableProto.getReturnType())
    }

    private fun doCreateCallableStub(
            callableProto: ProtoBuf.Callable,
            parentStub: StubElement<out PsiElement>,
            isTopLevel: Boolean
    ): StubElement<out PsiElement> {
        val callableKind = Flags.CALLABLE_KIND.get(callableProto.getFlags())
        val callableName = c.nameResolver.getName(callableProto.getName())
        val callableFqName = c.memberFqNameProvider.getFqNameForMember(callableName)
        val hasReceiverType = callableProto.hasReceiverType()
        val callableNameRef = callableName.asString().ref()
        return when (callableKind) {
            ProtoBuf.Callable.CallableKind.FUN -> {
                val isAbstract = Flags.MODALITY.get(callableProto.getFlags()) == Modality.ABSTRACT
                KotlinFunctionStubImpl(
                        parentStub,
                        callableNameRef,
                        isTopLevel = isTopLevel,
                        fqName = callableFqName,
                        isExtension = hasReceiverType,
                        hasBlockBody = true,
                        hasBody = !isAbstract,
                        hasTypeParameterListBeforeFunctionName = false
                )
            }
            ProtoBuf.Callable.CallableKind.VAL, ProtoBuf.Callable.CallableKind.VAR -> {
                KotlinPropertyStubImpl(
                        parentStub, callableNameRef,
                        isVar = callableKind == CallableKind.VAR,
                        isTopLevel = isTopLevel,
                        hasDelegate = false,
                        hasDelegateExpression = false,
                        hasInitializer = false,
                        hasReceiverTypeRef = hasReceiverType,
                        hasReturnTypeRef = true,
                        fqName = callableFqName
                )
            }
            ProtoBuf.Callable.CallableKind.CONSTRUCTOR -> throw IllegalStateException("Stubs for constructors are not supported!")
            else -> throw IllegalStateException("Unknown callable kind $callableKind")
        }
    }
}