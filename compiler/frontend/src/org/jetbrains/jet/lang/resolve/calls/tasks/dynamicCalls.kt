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

package org.jetbrains.jet.lang.resolve.calls.tasks

import org.jetbrains.jet.lang.psi.Call
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.jet.lang.descriptors.annotations.Annotations
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.SourceElement
import org.jetbrains.jet.lang.descriptors.impl.ReceiverParameterDescriptorImpl
import org.jetbrains.jet.lang.types.DynamicType
import org.jetbrains.jet.lang.descriptors.Modality
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ValueParameterDescriptorImpl
import org.jetbrains.jet.lang.types.JetType
import kotlin.platform.platformStatic
import org.jetbrains.jet.lang.resolve.scopes.receivers.TransientReceiver

object DynamicCallableDescriptors {

    platformStatic fun createCallableDescriptorForDynamicCall(call: Call, owner: DeclarationDescriptor): CallableDescriptor? {
        val callee = call.getCalleeExpression()
        if (callee !is JetSimpleNameExpression) return null
        val name = callee.getReferencedNameAsName()

        return if (call.getValueArgumentList() == null && call.getValueArguments().isEmpty()) {
            val propertyDescriptor = PropertyDescriptorImpl.create(
                    owner,
                    Annotations.EMPTY,
                    Modality.FINAL,
                    Visibilities.PUBLIC,
                    true,
                    name,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE
            )
            propertyDescriptor.setType(
                    DynamicType,
                    createTypeParameters(propertyDescriptor, call),
                    createDynamicDispatchReceiverParameter(propertyDescriptor),
                    null: JetType?
            )

            propertyDescriptor
        }
        else {
            val functionDescriptor = SimpleFunctionDescriptorImpl.create(
                    owner,
                    Annotations.EMPTY,
                    name,
                    CallableMemberDescriptor.Kind.DECLARATION,
                    SourceElement.NO_SOURCE
            )
            functionDescriptor.initialize(
                    null,
                    createDynamicDispatchReceiverParameter(functionDescriptor),
                    createTypeParameters(functionDescriptor, call),
                    createValueParameters(functionDescriptor, call),
                    DynamicType,
                    Modality.FINAL,
                    Visibilities.PUBLIC
            )
            functionDescriptor
        }
    }

    private fun createDynamicDispatchReceiverParameter(owner: CallableDescriptor): ReceiverParameterDescriptorImpl {
        return ReceiverParameterDescriptorImpl(
                owner,
                DynamicType,
                TransientReceiver(DynamicType)
        )
    }

    private fun createTypeParameters(owner: DeclarationDescriptor, call: Call): List<TypeParameterDescriptor> = call.getTypeArguments().indices.map {
        index
        ->
        TypeParameterDescriptorImpl.createWithDefaultBound(
                owner,
                Annotations.EMPTY,
                false,
                Variance.INVARIANT,
                Name.identifier("T$index"),
                index
        )
    }

    private fun createValueParameters(owner: DeclarationDescriptor, call: Call): List<ValueParameterDescriptor> =
            call.getValueArguments().indices.map {
                index
                ->
                ValueParameterDescriptorImpl(
                        owner,
                        null,
                        index,
                        Annotations.EMPTY,
                        Name.identifier("p$index"),
                        DynamicType,
                        false,
                        null,
                        SourceElement.NO_SOURCE
                )
            }
}