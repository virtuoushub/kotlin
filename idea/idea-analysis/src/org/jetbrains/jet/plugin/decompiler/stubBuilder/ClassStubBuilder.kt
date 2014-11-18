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
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.plugin.decompiler.textBuilder.LocalClassDataFinder

public fun ClassStubBuilderForTopLevelClass(
        classData: ClassData,
        classFqName: FqName,
        parent: StubElement<out PsiElement>,
        localClassDataFinder: LocalClassDataFinder
): ClassStubBuilder {
    val context = ClsStubBuilderContext(classData.getNameResolver(), MemberFqNameProvider(classFqName), TypeParameterContext.EMPTY, localClassDataFinder)
    return ClassStubBuilder(ClassId.topLevel(classFqName), classData.getClassProto(), parent, context)
}

public class ClassStubBuilder(
        private val classId: ClassId,
        private val classProto: ProtoBuf.Class,
        private val parentStub: StubElement<out PsiElement>,
        private val c: ClsStubBuilderContext
) {
    private var rootStub: KotlinStubWithFqName<out PsiNamedElement> by Delegates.notNull()
    private val contextWithTypeParameters = c.withTypeParams(classProto.getTypeParameterList())
    private val typeStubBuilder = TypeStubBuilder(contextWithTypeParameters)

    public fun createStub() {
        createRootStub()
        createModifierListStubForDeclaration(rootStub, classProto.getFlags(), FlagsToModifiers.VISIBILITY, FlagsToModifiers.MODALITY, FlagsToModifiers.INNER)
        //TODO: clearer logic/naming
        val typeConstraintBuilder = typeStubBuilder.createTypeParameterListStub(rootStub, classProto.getTypeParameterList())
        createConstructorStub()
        typeConstraintBuilder()
        createClassBodyAndMemberStubs()
    }

    private fun createClassBodyAndMemberStubs() {
        val classBody = KotlinPlaceHolderStubImpl<JetClassBody>(rootStub, JetStubElementTypes.CLASS_BODY)
        val memberStubBuilder = CallableStubBuilder(contextWithTypeParameters)
        for (callableProto in classProto.getMemberList()) {
            memberStubBuilder.createCallableStub(classBody, callableProto, isTopLevel = false)
        }

        createInnerAndNestedClasses(classBody)
    }

    private fun createRootStub() {
        val kind = Flags.CLASS_KIND.get(classProto.getFlags())
        val shortName = classId.getRelativeClassName().shortName().asString().ref()
        if (kind == ProtoBuf.Class.Kind.OBJECT) {
            rootStub = KotlinObjectStubImpl(
                    //TODO: to safe??
                    parentStub, shortName, classId.asSingleFqName().toSafe(), getSuperTypeRefs(),
                    //TODO_R: istoplevel
                    isTopLevel = true,
                    isClassObject = false,
                    isLocal = false,
                    isObjectLiteral = false
            )
        }
        else {
            rootStub = KotlinClassStubImpl(
                    //TODO_R: as singleFqname
                    JetClassElementType.getStubType(kind == ProtoBuf.Class.Kind.ENUM_ENTRY),
                    parentStub,
                    classId.asSingleFqName().asString().ref(),
                    shortName,
                    getSuperTypeRefs(),
                    isTrait = kind == ProtoBuf.Class.Kind.TRAIT,
                    isEnumEntry = kind == ProtoBuf.Class.Kind.ENUM_ENTRY,
                    isLocal = false,
                    isTopLevel = classId.getRelativeClassName().pathSegments().size == 1
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

    fun createInnerAndNestedClasses(classBody: KotlinPlaceHolderStubImpl<JetClassBody>) {
        classProto.getNestedClassNameList().forEach { id ->
            val nestedClassName = c.nameResolver.getName(id)
            val nestedClassID = classId.createNestedClassId(nestedClassName)
            //TODO_R: log if null
            val classData = c.classDataFinder.findClassData(nestedClassID)!!
            //TODO: refactor
            val innerContext = ClsStubBuilderContext(
                    classData.getNameResolver(),
                    MemberFqNameProvider(nestedClassID.asSingleFqName().toSafe()),
                    contextWithTypeParameters.typeParameters,
                    c.classDataFinder
            )
            ClassStubBuilder(nestedClassID, classData.getClassProto(), classBody, innerContext).createStub()
        }
    }
}

//TODO: obvious bad naming
private fun ClsStubBuilderContext.withTypeParams(typeParameterList: List<ProtoBuf.TypeParameter>): ClsStubBuilderContext {
    val typeParamNames = typeParameterList.map { nameResolver.getName(it.getName()) }
    return withTypeParameters(typeParamNames)
}
