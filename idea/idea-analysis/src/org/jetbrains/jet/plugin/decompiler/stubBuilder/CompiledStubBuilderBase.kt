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

import com.intellij.psi.stubs.StubElement
import org.jetbrains.jet.descriptors.serialization.Flags
import org.jetbrains.jet.descriptors.serialization.NameResolver
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinPropertyStubImpl
import org.jetbrains.jet.lang.resolve.name.FqName
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes
import org.jetbrains.jet.lang.psi.JetPackageDirective
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinUserTypeStubImpl
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinModifierListStubImpl
import org.jetbrains.jet.lang.psi.stubs.impl.ModifierMaskUtils
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Modality
import org.jetbrains.jet.lexer.JetModifierKeywordToken
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Visibility
import org.jetbrains.jet.lang.psi.JetParameterList
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinParameterStubImpl
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Callable.CallableKind
import com.intellij.util.io.StringRef
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.JetNullableType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.psi.JetTypeArgumentList
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinTypeProjectionStubImpl
import org.jetbrains.jet.lang.psi.JetProjectionKind
import org.jetbrains.jet.lang.psi.stubs.KotlinUserTypeStub
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Type.Argument.Projection
import org.jetbrains.jet.lang.psi.stubs.elements.JetPlaceHolderStubElementType
import org.jetbrains.jet.lang.psi.JetFunctionType
import org.jetbrains.jet.lang.psi.JetFunctionTypeReceiver

public abstract class CompiledStubBuilderBase(
        protected val nameResolver: NameResolver,
        protected val packageFqName: FqName
) {
    protected fun createCallableStub(
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

    private fun createTypeReferenceStub(parent: StubElement<out PsiElement>, typeProto: ProtoBuf.Type) {
        val typeReference = KotlinPlaceHolderStubImpl<JetTypeReference>(parent, JetStubElementTypes.TYPE_REFERENCE)
        createTypeStub(typeProto, typeReference)
    }

    private fun createValueParametersStub(callableStub: StubElement<out PsiElement>, callableProto: ProtoBuf.Callable) {
        if (Flags.CALLABLE_KIND.get(callableProto.getFlags()) != CallableKind.FUN) {
            return
        }
        val parameterListStub = KotlinPlaceHolderStubImpl<JetParameterList>(callableStub, JetStubElementTypes.VALUE_PARAMETER_LIST)
        for (valueParameter in callableProto.getValueParameterList()) {
            val name = nameResolver.getName(valueParameter.getName())
            val isVararg = valueParameter.hasVarargElementType()
            val parameterStub = KotlinParameterStubImpl(
                    parameterListStub,
                    name = name.asString().ref(),
                    fqName = null,
                    hasDefaultValue = false,
                    hasValOrValNode = false,
                    isMutable = false
            )
            if (isVararg) {
                createModifierListStub(parameterStub, listOf(JetTokens.VARARG_KEYWORD))
            }
            val typeProto = if (isVararg) valueParameter.getVarargElementType() else valueParameter.getType()
            createTypeReferenceStub(parameterStub, typeProto)
        }
    }

    private fun doCreateCallableStub(
            callableProto: ProtoBuf.Callable,
            parentStub: StubElement<out PsiElement>,
            isTopLevel: Boolean
    ): StubElement<out PsiElement> {
        val callableKind = Flags.CALLABLE_KIND.get(callableProto.getFlags())
        val callableName = nameResolver.getName(callableProto.getName())
        val callableFqName = getInternalFqName(callableName)
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

    protected abstract fun getInternalFqName(name: Name): FqName?

    //TODO_R: parameter order inconsistent
    private fun createTypeStub(type: ProtoBuf.Type, parent: StubElement<out PsiElement>) {
        val id = type.getConstructor().getId()
        //TODO_R: really?
        val isNullable = type.hasNullable() && type.getNullable()
        val realParent = if (isNullable) KotlinPlaceHolderStubImpl<JetNullableType>(parent, JetStubElementTypes.NULLABLE_TYPE) else parent
        when (type.getConstructor().getKind()) {
            ProtoBuf.Type.Constructor.Kind.CLASS -> {
                //TODO_r: add proto/stub specifiers to this code
                val fqName = nameResolver.getFqName(id)
                val isFunctionType = KotlinBuiltIns.getInstance().isExactFunctionType(fqName)
                val isExtensionFunctionType = KotlinBuiltIns.getInstance().isExactExtensionFunctionType(fqName)
                val typeArgumentList = type.getArgumentList()
                if (isFunctionType || isExtensionFunctionType) {
                    val functionType = KotlinPlaceHolderStubImpl<JetFunctionType>(realParent, JetStubElementTypes.FUNCTION_TYPE)
                    if (isExtensionFunctionType) {
                        val functionTypeReceiverStub
                                = KotlinPlaceHolderStubImpl<JetFunctionTypeReceiver>(functionType, JetStubElementTypes.FUNCTION_TYPE_RECEIVER)
                        val receiverTypeProto = typeArgumentList.first().getType()
                        createTypeReferenceStub(functionTypeReceiverStub, receiverTypeProto)
                    }

                    val parameterList = KotlinPlaceHolderStubImpl<JetParameterList>(functionType, JetStubElementTypes.VALUE_PARAMETER_LIST)
                    //TODO_R: assertion that sub list makes sense
                    val typeArgumentsWithoutReceiverAndReturnType
                            = typeArgumentList.subList(if (isExtensionFunctionType) 1 else 0, typeArgumentList.size - 1)
                    typeArgumentsWithoutReceiverAndReturnType.forEach { argument ->
                        val parameter = KotlinParameterStubImpl(parameterList, fqName = null, name = null, isMutable = false, hasValOrValNode = false, hasDefaultValue = false)
                        createTypeReferenceStub(parameter, argument.getType())
                    }

                    val returnType = typeArgumentList.last().getType()
                    createTypeReferenceStub(functionType, returnType)
                    return
                }
                val typeStub = createStubForType(fqName, realParent)
                val argumentList = typeArgumentList
                if (argumentList.isNotEmpty()) {
                    val typeArgList = KotlinPlaceHolderStubImpl<JetTypeArgumentList>(typeStub, JetStubElementTypes.TYPE_ARGUMENT_LIST)
                    argumentList.forEach { typeArgument ->
                        val projectionKind = typeArgument.getProjection().toProjectionKind()
                        val typeProjection = KotlinTypeProjectionStubImpl(typeArgList, projectionKind.ordinal())
                        val token = projectionKind.getToken() as? JetModifierKeywordToken
                        if (token != null) {
                            createModifierListStub(typeProjection, listOf(token))
                        }
                        val typeReference = KotlinPlaceHolderStubImpl<JetTypeReference>(typeProjection, JetStubElementTypes.TYPE_REFERENCE)
                        createTypeStub(typeArgument.getType(), typeReference)
                    }
                }
            }
            ProtoBuf.Type.Constructor.Kind.TYPE_PARAMETER -> {
                //TODO: rocket science goes here
                throw IllegalStateException("Unexpected $type")
            }
        }
    }
}

private fun Projection.toProjectionKind() = when (this) {
    Projection.IN -> JetProjectionKind.IN
    Projection.OUT -> JetProjectionKind.OUT
    Projection.INV -> JetProjectionKind.NONE
}


public fun createFileStub(packageFqName: FqName): KotlinFileStubImpl {
    val fileStub = KotlinFileStubImpl(null, packageFqName.asString(), packageFqName.isRoot())
    val packageDirectiveStub = KotlinPlaceHolderStubImpl<JetPackageDirective>(fileStub, JetStubElementTypes.PACKAGE_DIRECTIVE)
    createStubForPackageName(packageDirectiveStub, packageFqName)
    return fileStub
}

private fun createStubForPackageName(packageDirectiveStub: KotlinPlaceHolderStubImpl<JetPackageDirective>, packageFqName: FqName) {
    val segments = packageFqName.pathSegments().toArrayList()
    var current: StubElement<out JetElement> = packageDirectiveStub
    while (segments.notEmpty) {
        val head = segments.popFirst()
        if (segments.empty) {
            current = KotlinNameReferenceExpressionStubImpl(current, head.asString().ref())
        }
        else {
            current = KotlinPlaceHolderStubImpl<JetDotQualifiedExpression>(current, JetStubElementTypes.DOT_QUALIFIED_EXPRESSION)
            KotlinNameReferenceExpressionStubImpl(current, head.asString().ref())
        }
    }
}

//TODO_r: naming HELL
private fun createStubForType(typeFqName: FqName, parent: StubElement<out PsiElement>): KotlinUserTypeStub? {
    val segments = typeFqName.pathSegments().toArrayList()
    assert(segments.notEmpty)
    var current: StubElement<out PsiElement> = parent
    var next: StubElement<out PsiElement>? = null
    var result: KotlinUserTypeStub? = null
    //TODO_R: really?
    while (true) {
        val lastSegment = segments.popLast()
        if (next != null) {
            current = next!!
        }
        else {
            result = KotlinUserTypeStubImpl(current, isAbsoluteInRootPackage = false)
            current = result!!
        }
        if (segments.notEmpty) {
            next = KotlinUserTypeStubImpl(current, isAbsoluteInRootPackage = false)
        }
        KotlinNameReferenceExpressionStubImpl(current, lastSegment.asString().ref())
        if (segments.isEmpty()) {
            break
        }
    }
    return result!!
}

private fun <T> MutableList<T>.popLast(): T {
    val last = this.last
    this.remove(lastIndex)
    return last!!
}

private fun <T> MutableList<T>.popFirst(): T {
    val first = this.head
    this.remove(0)
    return first!!
}

//TODO_r: merge utilities
fun createModifierListStubForDeclaration(
        parent: StubElement<out PsiElement>,
        flags: Int,
        ignoreModality: Boolean = false
) {
    val modifiers = arrayListOf(visibilityToModifier(Flags.VISIBILITY.get(flags)))
    if (!ignoreModality) {
        modifiers.add(modalityToModifier(Flags.MODALITY.get(flags)))
    }

    KotlinModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { it in modifiers },
            JetStubElementTypes.MODIFIER_LIST
    )
}

fun createModifierListStub(
        parent: StubElement<out PsiElement>,
        modifiers: Collection<JetModifierKeywordToken>
) {
    KotlinModifierListStubImpl(
            parent,
            ModifierMaskUtils.computeMask { it in modifiers },
            JetStubElementTypes.MODIFIER_LIST
    )
}


private fun modalityToModifier(modality: Modality): JetModifierKeywordToken {
    return when (modality) {
        ProtoBuf.Modality.ABSTRACT -> JetTokens.ABSTRACT_KEYWORD
        ProtoBuf.Modality.FINAL -> JetTokens.FINAL_KEYWORD
        ProtoBuf.Modality.OPEN -> JetTokens.OPEN_KEYWORD
        else -> throw IllegalStateException("Unexpected modality: $modality")
    }
}

private fun visibilityToModifier(visibility: Visibility): JetModifierKeywordToken {
    return when (visibility) {
        ProtoBuf.Visibility.PRIVATE -> JetTokens.PRIVATE_KEYWORD
        ProtoBuf.Visibility.INTERNAL -> JetTokens.INTERNAL_KEYWORD
        ProtoBuf.Visibility.PROTECTED -> JetTokens.PROTECTED_KEYWORD
        ProtoBuf.Visibility.PUBLIC -> JetTokens.PUBLIC_KEYWORD
    //TODO: support extra visibility
        else -> throw IllegalStateException("Unexpected visibility: $visibility")
    }
}


//TODO_r: eliminate toString().ref
//TODO_r: move to some util?
fun String.ref() = StringRef.fromString(this)