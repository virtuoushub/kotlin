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

package org.jetbrains.jet.lang.resolve.varianceChecker

import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.diagnostics.Errors
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure.EnrichedProjectionKind.*
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure.*
import org.jetbrains.jet.lang.descriptors.Visibilities
import org.jetbrains.jet.lang.resolve.BindingTrace
import org.jetbrains.jet.lang.resolve.typeBinding.TypeBinding
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.resolve.typeBinding.createTypeBinding
import org.jetbrains.jet.lang.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.jet.lang.psi.JetCallableDeclaration
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.resolve.TopDownAnalysisContext
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lang.types.Variance.*
import org.jetbrains.jet.lang.diagnostics.DiagnosticSink
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.jet.lang.descriptors.impl.PropertyAccessorDescriptorImpl
import org.jetbrains.jet.lang.resolve.source.getPsi
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.psi.JetTypeParameterListOwner
import org.jetbrains.jet.lang.resolve.BindingContext
import kotlin.platform.platformStatic


class VarianceChecker(private val trace: BindingTrace) {

    fun process(c: TopDownAnalysisContext) {
        for (member in c.getMembers().values()) {
            recordPrivateToThisIfNeeded(trace, member)
        }

        check(c)
    }

    fun check(c: TopDownAnalysisContext) {
        checkClasses(c)
        checkMembers(c)
    }

    private fun checkClasses(c: TopDownAnalysisContext) {
        for (jetClassOrObject in c.getDeclaredClasses()!!.keySet()) {
            if (jetClassOrObject is JetClass) {
                for (specifier in jetClassOrObject.getDelegationSpecifiers()) {
                    specifier.getTypeReference()?.checkTypePosition(trace.getBindingContext(), OUT_VARIANCE, trace)
                }
                jetClassOrObject.checkTypeParameters(trace.getBindingContext(), OUT_VARIANCE, trace)
            }
        }
    }

    private fun checkMembers(c: TopDownAnalysisContext) {
        for ((declaration, descriptor) in c.getMembers()) {
            if (!Visibilities.isPrivate(descriptor.getVisibility())) {
                checkCallableDeclaration(trace.getBindingContext(), declaration, descriptor, trace)
            }
        }
    }

    class VarianceConflictDiagnosticData(
            val containingType: JetType,
            val typeParameter: TypeParameterDescriptor,
            val occurrencePosition: Variance
    )

    class object {
        platformStatic fun recordPrivateToThisIfNeeded(trace: BindingTrace, descriptor: CallableMemberDescriptor) {
            if (descriptor.getVisibility() != Visibilities.PRIVATE) return

            val psiElement = descriptor.getSource().getPsi()
            if (psiElement !is JetCallableDeclaration) return

            if (!checkCallableDeclaration(trace.getBindingContext(), psiElement, descriptor, DiagnosticSink.DO_NOTHING)) {
                recordPrivateToThis(descriptor)
            }
        }

        private fun recordPrivateToThis(descriptor: CallableDescriptor) {
            if (descriptor is FunctionDescriptorImpl) {
                descriptor.setVisibility(Visibilities.PRIVATE_TO_THIS);
            }
            else if (descriptor is PropertyDescriptorImpl) {
                descriptor.setVisibility(Visibilities.PRIVATE_TO_THIS);
                for (accessor in descriptor.getAccessors()) {
                    (accessor as PropertyAccessorDescriptorImpl).setVisibility(Visibilities.PRIVATE_TO_THIS)
                }
            }
            else {
                throw IllegalStateException("Unexpected descriptor type: ${descriptor.javaClass.getName()}")
            }
        }

        private fun checkCallableDeclaration(
                trace: BindingContext,
                declaration: JetCallableDeclaration,
                descriptor: CallableDescriptor,
                diagnosticSink: DiagnosticSink
        ): Boolean {
            if (descriptor.getContainingDeclaration() !is ClassDescriptor) return true
            val noError = BooleanBuilder()

            noError and declaration.checkTypeParameters(trace, IN_VARIANCE, diagnosticSink)

            noError and declaration.getReceiverTypeReference()?.checkTypePosition(trace, IN_VARIANCE, diagnosticSink)

            for (parameter in declaration.getValueParameters()) {
                noError and parameter.getTypeReference()?.checkTypePosition(trace, IN_VARIANCE, diagnosticSink)
            }

            val returnTypePosition = if (descriptor is VariableDescriptor && descriptor.isVar()) INVARIANT else OUT_VARIANCE
            noError and declaration.createTypeBindingForReturnType(trace)?.checkTypePosition(returnTypePosition, diagnosticSink)

            return noError.value
        }

        private fun JetTypeParameterListOwner.checkTypeParameters(
                trace: BindingContext,
                typePosition: Variance,
                diagnosticSink: DiagnosticSink
        ): Boolean {
            val noError = BooleanBuilder()
            for (typeParameter in getTypeParameters()) {
                noError and typeParameter.getExtendsBound()?.checkTypePosition(trace, typePosition, diagnosticSink)
            }
            for (typeConstraint in getTypeConstraints()) {
                noError and typeConstraint.getBoundTypeReference()?.checkTypePosition(trace, typePosition, diagnosticSink)
            }
            return noError.value
        }

        private fun JetTypeReference.checkTypePosition(trace: BindingContext, position: Variance, diagnosticSink: DiagnosticSink)
                = createTypeBinding(trace)?.checkTypePosition(position, diagnosticSink)

        private fun TypeBinding<PsiElement>.checkTypePosition(position: Variance, diagnosticSink: DiagnosticSink)
                = checkTypePosition(jetType, position, diagnosticSink)

        private fun TypeBinding<PsiElement>.checkTypePosition(containingType: JetType, position: Variance, diagnosticSink: DiagnosticSink): Boolean {
            val classifierDescriptor = jetType.getConstructor().getDeclarationDescriptor()
            if (classifierDescriptor is TypeParameterDescriptor) {
                val declarationVariance = classifierDescriptor.getVariance()
                if (!declarationVariance.allowsPosition(position)) {
                    diagnosticSink.report(
                            Errors.TYPE_VARIANCE_CONFLICT.on(
                                    psiElement,
                                    VarianceConflictDiagnosticData(containingType, classifierDescriptor, position)
                            )
                    )
                }
                return declarationVariance.allowsPosition(position)
            }

            val noError = BooleanBuilder()
            for (argumentBinding in getArgumentBindings()) {
                if (argumentBinding == null || argumentBinding.typeParameterDescriptor == null) continue

                val projectionKind = getEffectiveProjectionKind(argumentBinding.typeParameterDescriptor, argumentBinding.typeProjection)!!
                val newPosition = when (projectionKind) {
                    OUT -> position
                    IN -> position.opposite()
                    INV -> INVARIANT
                    STAR -> null // CONFLICTING_PROJECTION error was reported
                }
                if (newPosition != null) {
                    noError and argumentBinding.typeBinding.checkTypePosition(containingType, newPosition, diagnosticSink)
                }
            }
            return noError.value
        }

        private class BooleanBuilder(var value: Boolean = true) {
            fun and(other: Boolean?) {
                if (other != null) {
                    value = value and other
                }
            }
        }

    }
}
