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

package org.jetbrains.k2js.resolve

import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider
import org.jetbrains.jet.lang.resolve.AnnotationChecker
import org.jetbrains.jet.lang.psi.JetDeclaration
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.diagnostics.DiagnosticSink
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.jet.lang.psi.JetNamedFunction
import org.jetbrains.k2js.resolve.diagnostics.ErrorsJs
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.k2js.PredefinedAnnotation
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.k2js.translate.utils.AnnotationsUtils

public object KotlinJsDeclarationCheckerProvider : AdditionalCheckerProvider {
    override val annotationCheckers: List<AnnotationChecker> = listOf(NativeInvokeChecker(), NativeGetterChecker(), NativeSetterChecker())
}

abstract class NativeXAnnotationBaseChecker(requiredAnnotation: PredefinedAnnotation) : AnnotationChecker {
    private val requiredAnnotationFqName = FqName(requiredAnnotation.fqName)

    open fun additionalCheck(declaration: JetNamedFunction, descriptor: FunctionDescriptor, diagnosticHolder: DiagnosticSink) {}

    override fun check(declaration: JetDeclaration, descriptor: DeclarationDescriptor, diagnosticHolder: DiagnosticSink) {
        val annotationDescriptor = descriptor.getAnnotations().findAnnotation(requiredAnnotationFqName)
        if (annotationDescriptor == null) return

        if (declaration !is JetNamedFunction || descriptor !is FunctionDescriptor) {
            diagnosticHolder.report(ErrorsJs.NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN.on(declaration, annotationDescriptor.getType()))
            return
        }

        val isTopLevel = DescriptorUtils.isTopLevelDeclaration(descriptor)
        val isExtension = DescriptorUtils.isExtension(descriptor)
        if (!isTopLevel && isExtension ||
            isTopLevel && !isExtension ||
            !(isTopLevel && isExtension) && !AnnotationsUtils.isNativeObject(descriptor)
        ) {
            diagnosticHolder.report(ErrorsJs.NATIVE_X_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN.on(declaration, annotationDescriptor.getType()))
        }

        additionalCheck(declaration, descriptor, diagnosticHolder)
    }
}

public class NativeInvokeChecker : NativeXAnnotationBaseChecker(PredefinedAnnotation.NATIVE_INVOKE)

public open class NativeIndexerBaseChecker(
        requiredAnnotation: PredefinedAnnotation,
        private val indexerType: String,
        private val requiredParametersCount: Int
) : NativeXAnnotationBaseChecker(requiredAnnotation) {

    override fun additionalCheck(declaration: JetNamedFunction, descriptor: FunctionDescriptor, diagnosticHolder: DiagnosticSink) {
        val parameters = descriptor.getValueParameters()
        if (parameters.size() > 0) {
            val firstParamClassDescriptor = DescriptorUtils.getClassDescriptorForType(parameters.get(0).getType())
            if (firstParamClassDescriptor != KotlinBuiltIns.getInstance().getString() &&
                !DescriptorUtils.isSubclass(firstParamClassDescriptor, KotlinBuiltIns.getInstance().getNumber())
            ) {
                diagnosticHolder.report(ErrorsJs.NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER.on(declaration.getValueParameters().first, indexerType))
            }
        }

        if(parameters.size()!= requiredParametersCount) {
            diagnosticHolder.report(ErrorsJs.NATIVE_INDEXER_WRONG_PARAMETER_COUNT.on(declaration, requiredParametersCount, indexerType))
        }
    }
}

public class NativeGetterChecker : NativeIndexerBaseChecker(PredefinedAnnotation.NATIVE_GETTER, "Getter", requiredParametersCount = 1) {
    override fun additionalCheck(declaration: JetNamedFunction, descriptor: FunctionDescriptor, diagnosticHolder: DiagnosticSink) {
        super.additionalCheck(declaration, descriptor, diagnosticHolder)

        if (!TypeUtils.isNullableType(descriptor.getReturnType())) {
            diagnosticHolder.report(ErrorsJs.NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE.on(declaration))
        }
    }
}

public class NativeSetterChecker : NativeIndexerBaseChecker(PredefinedAnnotation.NATIVE_SETTER, "Setter", requiredParametersCount = 2)
