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

import org.jetbrains.jet.descriptors.serialization.PackageData
import org.jetbrains.jet.descriptors.serialization.ProtoBuf
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.psi.stubs.KotlinFileStub
import org.jetbrains.jet.descriptors.serialization.ClassDataFinder
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.descriptors.serialization.ClassData

public class PackageFacadeStubBuilder(
        packageData: PackageData,
        val packageFqName: FqName
) {
    //TODO: does it belong here
    private val throwingClassDataFinder = object : ClassDataFinder {
        override fun findClassData(classId: ClassId) = throw UnsupportedOperationException()
    }

    private val c = ClsStubBuilderContext(
            packageData.getNameResolver(),
            MemberFqNameProvider(packageFqName),
            TypeParameterContext.EMPTY,
            throwingClassDataFinder
    )

    private val memberStubBuilder = CallableStubBuilder(c)
    private val packageProto = packageData.getPackageProto()

    public fun createStub(): KotlinFileStub {
        val fileStub = createFileStub(packageFqName)
        for (callableProto in packageProto.getMemberList()) {
            memberStubBuilder.createCallableStub(fileStub, callableProto, isTopLevel = true)
        }
        return fileStub
    }
}
