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

package org.jetbrains.jet.lang.resolve.kotlin;

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.ConstantsPackage;
import org.jetbrains.jet.lang.resolve.name.ClassId;
import org.jetbrains.jet.storage.MemoizedFunctionToNotNull;
import org.jetbrains.jet.storage.StorageManager;

import java.util.List;

public class DescriptorLoadersStorage extends AbstractLoadersStorage<AnnotationDescriptor, CompileTimeConstant<?>> {
    protected final MemoizedFunctionToNotNull<KotlinJvmBinaryClass, Storage<AnnotationDescriptor, CompileTimeConstant<?>>> storage;
    private final ModuleDescriptor module;

    public DescriptorLoadersStorage(@NotNull StorageManager storageManager, @NotNull ModuleDescriptor module) {
        super();
        this.storage = storageManager.createMemoizedFunction(
                new Function1<KotlinJvmBinaryClass, Storage<AnnotationDescriptor, CompileTimeConstant<?>>>() {
                    @NotNull
                    @Override
                    public Storage<AnnotationDescriptor, CompileTimeConstant<?>> invoke(@NotNull KotlinJvmBinaryClass kotlinClass) {
                        return loadAnnotationsAndInitializers(kotlinClass);
                    }
                }
        );
        this.module = module;
    }

    @NotNull
    @Override
    protected Storage<AnnotationDescriptor, CompileTimeConstant<?>> getStorageForClass(@NotNull KotlinJvmBinaryClass kotlinClass) {
        return storage.invoke(kotlinClass);
    }

    @Override
    protected KotlinJvmBinaryClass.AnnotationArgumentVisitor loadAnnotation(ClassId classId, List<AnnotationDescriptor> result) {
        return AnnotationDescriptorLoader.resolveAnnotation(classId, result, module);
    }

    @Override
    protected CompileTimeConstant<?> loadConstant(String desc, Object initializer) {
        Object normalizedValue;
        if ("ZBCS".contains(desc)) {
            int intValue = ((Integer) initializer).intValue();
            if ("Z".equals(desc)) {
                normalizedValue = intValue != 0;
            }
            else if ("B".equals(desc)) {
                normalizedValue = ((byte) intValue);
            }
            else if ("C".equals(desc)) {
                normalizedValue = ((char) intValue);
            }
            else if ("S".equals(desc)) {
                normalizedValue = ((short) intValue);
            }
            else {
                throw new AssertionError(desc);
            }
        }
        else {
            normalizedValue = initializer;
        }

        return ConstantsPackage.createCompileTimeConstant(
                normalizedValue,
                /* canBeUsedInAnnotation */ true,
                /* isPureIntConstant */ true,
                /* usesVariableAsConstant */ true,
                /* expectedType */ null
        );
    }
}
