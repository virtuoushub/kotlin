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
import org.jetbrains.jet.descriptors.serialization.ClassData
import org.jetbrains.jet.descriptors.serialization.Flags
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.lang.psi.stubs.KotlinStubWithFqName
import org.jetbrains.jet.lang.psi.stubs.elements.JetClassElementType
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes
import com.intellij.util.io.StringRef
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.JetParameterList
import kotlin.properties.Delegates
import org.jetbrains.jet.lang.psi.JetTypeParameterList
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinTypeParameterStubImpl
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Type
import org.jetbrains.jet.lang.psi.JetTypeConstraintList
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinTypeConstraintStubImpl
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinNameReferenceExpressionStubImpl
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.TypeParameter.Variance
import org.jetbrains.jet.lexer.JetTokens
import org.jetbrains.jet.lexer.JetModifierKeywordToken

public class CompiledClassStubBuilder(
        classData: ClassData,
        private val classFqName: FqName,
        packageFqName: FqName,
        private val parent: StubElement<out PsiElement>,
        typeParameterContext: TypeParameterContext = TypeParameterContext.EMPTY
) {
    private val c = ClsStubBuilderContext(classData.getNameResolver(), MemberFqNameProvider(classFqName), typeParameterContext)
    private val classProto = classData.getClassProto()
    private var rootStub: KotlinStubWithFqName<out PsiNamedElement> by Delegates.notNull()
    private val memberStubBuilder = run {
        val typeParamNames = classProto.getTypeParameterList().map { c.nameResolver.getName(it.getName()) }
        val contextForClassMembers = c.withTypeParameters(typeParamNames)
        CompiledStubBuilderForMembers(contextForClassMembers)
    }

    public fun createStub() {
        createRootStub()
        createModifierListStubForDeclaration(rootStub, classProto.getFlags())
        //TODO: clearer login/naming
        val typeConstraintBuilder = createTypeParameterListStub()
        createConstructorStub()
        typeConstraintBuilder()
        createClassBodyAndMemberStubs()
    }

    private fun createTypeParameterListStub(): () -> Unit {
        val typeParameterProtoList = classProto.getTypeParameterList()

        if (typeParameterProtoList.isEmpty()) return {}

        val typeParameterListStub = KotlinPlaceHolderStubImpl<JetTypeParameterList>(rootStub, JetStubElementTypes.TYPE_PARAMETER_LIST)
        val protosForWhereClause = arrayListOf<Pair<Name, Type>>()
        for (typeParameterProto in typeParameterProtoList) {
            val name = c.nameResolver.getName(typeParameterProto.getName())
            val variance = typeParameterProto.getVariance()
            val typeParameterStub = KotlinTypeParameterStubImpl(
                    typeParameterListStub,
                    name = name.asString().ref(),
                    isInVariance = variance == Variance.IN,
                    isOutVariance = variance == Variance.OUT
            )
            val modifiers = when (variance) {
                Variance.IN -> listOf(JetTokens.IN_KEYWORD)
                Variance.OUT -> listOf(JetTokens.OUT_KEYWORD)
                else -> listOf<JetModifierKeywordToken>()
            }
            createModifierListStub(typeParameterStub, modifiers)
            val upperBoundsAsTypes = typeParameterProto.getUpperBoundList()
            val defaultBounds = upperBoundsAsTypes.singleOrNull()?.isNullableAny() ?: false
            if (defaultBounds) {
                continue
            }
            if (upperBoundsAsTypes.isNotEmpty()) {
                memberStubBuilder.createTypeReferenceStub(typeParameterStub, upperBoundsAsTypes.first())
                protosForWhereClause addAll upperBoundsAsTypes.drop(1).map { Pair(name, it) }
            }
        }
        val howToConstructTypeConstraintList = {
            val typeConstraintListStub = KotlinPlaceHolderStubImpl<JetTypeConstraintList>(rootStub, JetStubElementTypes.TYPE_CONSTRAINT_LIST)
            protosForWhereClause.forEach {
                val typeConstraintStub = KotlinTypeConstraintStubImpl(typeConstraintListStub, isClassObjectConstraint = false)
                //TODO: pair usage
                KotlinNameReferenceExpressionStubImpl(typeConstraintStub, it.first.asString().ref())
                memberStubBuilder.createTypeReferenceStub(typeConstraintStub, it.second)
            }
        }
        return howToConstructTypeConstraintList
    }

    private fun createClassBodyAndMemberStubs() {
        val classBody = KotlinPlaceHolderStubImpl<JetClassBody>(rootStub, JetStubElementTypes.CLASS_BODY)
        for (callableProto in classProto.getMemberList()) {
            memberStubBuilder.createCallableStub(classBody, callableProto, isTopLevel = false)
        }
    }

    private fun createRootStub() {
        val kind = Flags.CLASS_KIND.get(classProto.getFlags())
        val isEnumEntry = kind == ProtoBuf.Class.Kind.ENUM_ENTRY
        val shortName = classFqName.shortName().asString().ref()
        if (kind == ProtoBuf.Class.Kind.OBJECT) {
            rootStub = KotlinObjectStubImpl(
                    parent, shortName, classFqName, getSuperTypeRefs(),
                    isTopLevel = true,
                    isClassObject = false,
                    isLocal = false,
                    isObjectLiteral = false
            )
        }
        else {
            rootStub = KotlinClassStubImpl(
                    JetClassElementType.getStubType(isEnumEntry), parent, classFqName.asString().ref(), shortName,
                    getSuperTypeRefs(),
                    isTrait = kind == ProtoBuf.Class.Kind.TRAIT,
                    isEnumEntry = kind == ProtoBuf.Class.Kind.ENUM_ENTRY,
                    isLocal = false,
                    isTopLevel = true
            )
        }
    }

    private fun getSuperTypeRefs(): Array<StringRef> {
        val superTypeStrings = classProto.getSupertypeList().map {
            type ->
            assert(type.getConstructor().getKind() == ProtoBuf.Type.Constructor.Kind.CLASS)
            val superFqName = c.nameResolver.getFqName(type.getConstructor().getId())
            superFqName.asString()
        }
        return superTypeStrings.filter { it != "kotlin.Any" }.map { it.ref() }.copyToArray()
    }

    fun createConstructorStub() {
        KotlinPlaceHolderStubImpl<JetParameterList>(rootStub, JetStubElementTypes.VALUE_PARAMETER_LIST)
    }

    private fun Type.isNullableAny(): Boolean {
        val constructor = getConstructor()
        if (constructor.getKind() != ProtoBuf.Type.Constructor.Kind.CLASS) {
            return false
        }
        val fqName = c.nameResolver.getFqName(constructor.getId())
        return KotlinBuiltIns.getInstance().isAny(fqName.toUnsafe()) && this.hasNullable() && this.getNullable()
    }
}
