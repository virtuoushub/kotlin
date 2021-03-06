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
import org.jetbrains.jet.lang.types.isDynamic
import org.jetbrains.jet.lang.resolve.calls.tasks.collectors.CallableDescriptorCollector
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.calls.tasks.collectors.CallableDescriptorCollectors
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl
import org.jetbrains.jet.utils.Printer
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lang.types.expressions.OperatorConventions
import org.jetbrains.jet.lang.psi.JetOperationReferenceExpression

object DynamicCallableDescriptors {

    platformStatic fun createDynamicDescriptorScope(call: Call, owner: DeclarationDescriptor) = object : JetScopeImpl() {
        override fun getContainingDeclaration() = owner

        override fun printScopeStructure(p: Printer) {
            p.println(javaClass.getSimpleName(), ": dynamic candidates for " + call)
        }

        override fun getFunctions(name: Name): Collection<FunctionDescriptor> {
            if (isAugmentedAssignmentConvention(name)) return listOf()
            if (call.getCallType() == Call.CallType.INVOKE
                && call.getValueArgumentList() == null && call.getFunctionLiteralArguments().isEmpty()) {
                // this means that we are looking for "imaginary" invokes,
                // e.g. in `+d` we are looking for property "plus" with member "invoke"
                return listOf()
            }
            return listOf(createDynamicFunction(owner, name, call))
        }

        /*
         * Detects the case when name "plusAssign" is requested for "+=" call,
         * since both "plus" and "plusAssign" are resolvable on dynamic receivers,
         * we have to prefer ne of them, and prefer "plusAssign" for generality:
         * it may be called even on a val
         */
        private fun isAugmentedAssignmentConvention(name: Name): Boolean {
            val callee = call.getCalleeExpression()
            if (callee is JetOperationReferenceExpression) {
                val token = callee.getReferencedNameElementType()
                if (token in JetTokens.AUGMENTED_ASSIGNMENTS && OperatorConventions.ASSIGNMENT_OPERATIONS[token] != name) {
                    return true
                }
            }
            return false
        }

        override fun getProperties(name: Name): Collection<VariableDescriptor> {
            return if (call.getValueArgumentList() == null && call.getValueArguments().isEmpty()) {
                listOf(createDynamicProperty(owner, name, call))
            }
            else listOf()
        }
    }

    private fun createDynamicProperty(owner: DeclarationDescriptor, name: Name, call: Call): PropertyDescriptorImpl {
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

        return propertyDescriptor
    }

    private fun createDynamicFunction(owner: DeclarationDescriptor, name: Name, call: Call): SimpleFunctionDescriptorImpl {
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
        return functionDescriptor
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
            call.getValueArguments().withIndices().map { p ->
                val (index, arg) = p
                ValueParameterDescriptorImpl(
                        owner,
                        null,
                        index,
                        Annotations.EMPTY,
                        arg.getArgumentName()?.getReferenceExpression()?.getReferencedNameAsName() ?: Name.identifier("p$index"),
                        DynamicType,
                        false,
                        null,
                        SourceElement.NO_SOURCE
                )
            }
}

fun DeclarationDescriptor.isDynamic(): Boolean {
    if (this !is CallableDescriptor) return false
    val dispatchReceiverParameter = getDispatchReceiverParameter()
    return dispatchReceiverParameter != null && dispatchReceiverParameter.getType().isDynamic()
}

class CollectorForDynamicReceivers<D: CallableDescriptor>(val delegate: CallableDescriptorCollector<D>) : CallableDescriptorCollector<D> by delegate {
    override fun getExtensionsByName(scope: JetScope, name: Name, bindingTrace: BindingTrace): Collection<D> {
        return delegate.getExtensionsByName(scope, name, bindingTrace).filter {
            it.getExtensionReceiverParameter()?.getType()?.isDynamic() ?: false
        }
    }
}

fun <D: CallableDescriptor> CallableDescriptorCollectors<D>.onlyDynamicReceivers(): CallableDescriptorCollectors<D> {
    return CallableDescriptorCollectors(* this.map { CollectorForDynamicReceivers(it) }.copyToArray())
}
