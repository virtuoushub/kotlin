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

package org.jetbrains.jet.lang.resolve.kotlin.incremental

import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.psi.JetFile
import com.intellij.util.containers.MultiMap
import java.util.HashMap
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.storage.StorageManager
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.descriptors.serialization.*
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver
import org.jetbrains.jet.utils.addToStdlib.singletonOrEmptyList
import org.jetbrains.jet.storage.NotNullLazyValue
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils
import org.jetbrains.jet.lang.resolve.java.JvmClassName
import org.jetbrains.jet.lang.resolve.kotlin.incremental.cache.IncrementalCache
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.descriptors.serialization.context.DeserializationComponents

public class IncrementalPackageFragmentProvider(
        sourceFiles: Collection<JetFile>,
        val module: ModuleDescriptor,
        val storageManager: StorageManager,
        val deserializationComponents: DeserializationComponents,
        val incrementalCache: IncrementalCache,
        val moduleId: String,
        val javaDescriptorResolver: JavaDescriptorResolver
) : PackageFragmentProvider {

    val packagePartsToNotLoadFromCache = (
            sourceFiles.map { PackagePartClassUtils.getPackagePartInternalName(it) }
                    + incrementalCache.getRemovedPackageParts(sourceFiles).map { it.getInternalName() }
            ).toSet()
    val fqNameToSubFqNames = MultiMap<FqName, FqName>()
    val fqNameToPackageFragment = HashMap<FqName, PackageFragmentDescriptor>()
    val fqNamesToLoad: Set<FqName>

    ;{
        fun createPackageFragment(fqName: FqName) {
            if (fqNameToPackageFragment.containsKey(fqName)) {
                return
            }

            if (!fqName.isRoot()) {
                val parent = fqName.parent()
                createPackageFragment(parent)
                fqNameToSubFqNames.putValue(parent, fqName)
            }

            fqNameToPackageFragment[fqName] = IncrementalPackageFragment(fqName)
        }

        fqNamesToLoad = (
                PackagePartClassUtils.getPackageFilesWithCallables(sourceFiles).map { it.getPackageFqName() }
                + incrementalCache.getPackagesWithRemovedFiles(sourceFiles)
        ).toSet()

        fqNamesToLoad.forEach { createPackageFragment(it) }
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return fqNameToSubFqNames[fqName].orEmpty()
    }

    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return fqNameToPackageFragment[fqName].singletonOrEmptyList()
    }


    public inner class IncrementalPackageFragment(fqName: FqName) : PackageFragmentDescriptorImpl(module, fqName) {
        public val moduleId: String
            get() = this@IncrementalPackageFragmentProvider.moduleId

        val memberScope: NotNullLazyValue<JetScope> = storageManager.createLazyValue {
            if (fqName !in fqNamesToLoad) {
                JetScope.Empty
            }
            else {
                val packageDataBytes = incrementalCache.getPackageData(fqName)
                if (packageDataBytes == null) {
                    JetScope.Empty
                }
                else {
                    IncrementalPackageScope(JavaProtoBufUtil.readPackageDataFrom(packageDataBytes))
                }
            }
        }

        override fun getMemberScope(): JetScope = memberScope()

        private inner class IncrementalPackageScope(val packageData: PackageData) : DeserializedPackageMemberScope(
                this@IncrementalPackageFragment, packageData.getPackageProto(), packageData.getNameResolver(), deserializationComponents,
                { listOf() }
        ) {
            override fun filteredMemberProtos(allMemberProtos: Collection<ProtoBuf.Callable>): Collection<ProtoBuf.Callable> {
                return allMemberProtos
                        .filter {
                            member ->
                            if (member.hasExtension(JavaProtoBuf.implClassName)) {
                                val shortName = packageData.getNameResolver().getName(member.getExtension(JavaProtoBuf.implClassName)!!)
                                val internalName = JvmClassName.byFqNameWithoutInnerClasses(fqName.child(shortName)).getInternalName()
                                internalName !in packagePartsToNotLoadFromCache
                            }
                            else {
                                true
                            }
                        }
            }
        }
    }
}

