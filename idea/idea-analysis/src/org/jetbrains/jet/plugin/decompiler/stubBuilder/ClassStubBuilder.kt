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
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.psi.JetClassBody
import org.jetbrains.jet.lang.psi.stubs.impl.KotlinPlaceHolderStubImpl
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes
import com.intellij.util.io.StringRef
import com.intellij.psi.PsiNamedElement
import org.jetbrains.jet.lang.psi.JetParameterList
import kotlin.properties.Delegates

public class ClassStubBuilder(
        classData: ClassData,
        private val classFqName: FqName,
        packageFqName: FqName,
        private val parent: StubElement<out PsiElement>,
        typeParameterContext: TypeParameterContext = TypeParameterContext.EMPTY
) {
    private val c = ClsStubBuilderContext(classData.getNameResolver(), MemberFqNameProvider(classFqName), typeParameterContext)
    private val classProto = classData.getClassProto()
    private var rootStub: KotlinStubWithFqName<out PsiNamedElement> by Delegates.notNull()
    private val contextWithTypeParameters = c.withTypeParams(classProto.getTypeParameterList())
    private val typeStubBuilder = TypeStubBuilder(contextWithTypeParameters)

    public fun createStub() {
        createRootStub()
        createModifierListStubForDeclaration(rootStub, classProto.getFlags())
        //TODO: clearer logic/naming
        val typeConstraintBuilder = typeStubBuilder.createTypeParameterListStub(rootStub, classProto.getTypeParameterList())
        createConstructorStub()
        typeConstraintBuilder()
        createClassBodyAndMemberStubs()
    }

    private fun createClassBodyAndMemberStubs() {
        val memberStubBuilder = CallableStubBuilder(contextWithTypeParameters)
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
}

//TODO: obvious bad naming
private fun ClsStubBuilderContext.withTypeParams(typeParameterList: List<ProtoBuf.TypeParameter>): ClsStubBuilderContext {
    val typeParamNames = typeParameterList.map { nameResolver.getName(it.getName()) }
    return withTypeParameters(typeParamNames)
}
