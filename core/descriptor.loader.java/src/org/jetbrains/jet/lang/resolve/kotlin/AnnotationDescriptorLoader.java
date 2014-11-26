/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.SerializationPackage;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl;
import org.jetbrains.jet.lang.resolve.constants.*;
import org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames;
import org.jetbrains.jet.lang.resolve.java.resolver.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.kotlin.KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor;
import org.jetbrains.jet.lang.resolve.name.ClassId;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.ErrorUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.kotlin.DeserializedResolverUtils.javaClassIdToKotlinClassId;

public class AnnotationDescriptorLoader extends AbstractAnnotationLoader<AnnotationDescriptor> {
    private final ModuleDescriptor module;

    public AnnotationDescriptorLoader(
            @NotNull ModuleDescriptor module,
            @NotNull DescriptorLoadersStorage storage,
            @NotNull KotlinClassFinder kotlinClassFinder,
            @NotNull ErrorReporter errorReporter
    ) {
        super(storage, kotlinClassFinder, errorReporter);
        this.module = module;
    }

    @Override
    protected KotlinJvmBinaryClass.AnnotationArgumentVisitor loadAnnotation(
            @NotNull ClassId classId, @NotNull List<AnnotationDescriptor> result
    ) {
        return resolveAnnotation(classId, result, module);
    }

    @Nullable
    public static KotlinJvmBinaryClass.AnnotationArgumentVisitor resolveAnnotation(
            @NotNull ClassId classId,
            @NotNull final List<AnnotationDescriptor> result,
            @NotNull final ModuleDescriptor moduleDescriptor
    ) {
        if (JvmAnnotationNames.isSpecialAnnotation(classId, true)) return null;

        final ClassDescriptor annotationClass = resolveClass(classId, moduleDescriptor);

        return new KotlinJvmBinaryClass.AnnotationArgumentVisitor() {
            private final Map<ValueParameterDescriptor, CompileTimeConstant<?>> arguments =
                    new HashMap<ValueParameterDescriptor, CompileTimeConstant<?>>();

            @Override
            public void visit(@Nullable Name name, @Nullable Object value) {
                if (name != null) {
                    setArgumentValueByName(name, createConstant(name, value));
                }
            }

            @Override
            public void visitEnum(@NotNull Name name, @NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
                setArgumentValueByName(name, enumEntryValue(enumClassId, enumEntryName));
            }

            @Nullable
            @Override
            public AnnotationArrayArgumentVisitor visitArray(@NotNull final Name name) {
                return new KotlinJvmBinaryClass.AnnotationArrayArgumentVisitor() {
                    private final ArrayList<CompileTimeConstant<?>> elements = new ArrayList<CompileTimeConstant<?>>();

                    @Override
                    public void visit(@Nullable Object value) {
                        elements.add(createConstant(name, value));
                    }

                    @Override
                    public void visitEnum(@NotNull ClassId enumClassId, @NotNull Name enumEntryName) {
                        elements.add(enumEntryValue(enumClassId, enumEntryName));
                    }

                    @Override
                    public void visitEnd() {
                        ValueParameterDescriptor parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass);
                        if (parameter != null) {
                            elements.trimToSize();
                            arguments.put(parameter, new ArrayValue(elements, parameter.getType(), true, false));
                        }
                    }
                };
            }

            @NotNull
            private CompileTimeConstant<?> enumEntryValue(@NotNull ClassId enumClassId, @NotNull Name name) {
                ClassDescriptor enumClass = resolveClass(enumClassId, moduleDescriptor);
                if (enumClass.getKind() == ClassKind.ENUM_CLASS) {
                    ClassifierDescriptor classifier = enumClass.getUnsubstitutedInnerClassesScope().getClassifier(name);
                    if (classifier instanceof ClassDescriptor) {
                        return new EnumValue((ClassDescriptor) classifier, false);
                    }
                }
                return ErrorValue.create("Unresolved enum entry: " + enumClassId + "." + name);
            }

            @Override
            public void visitEnd() {
                result.add(new AnnotationDescriptorImpl(
                        annotationClass.getDefaultType(),
                        arguments
                ));
            }

            @NotNull
            private CompileTimeConstant<?> createConstant(@Nullable Name name, @Nullable Object value) {
                CompileTimeConstant<?> argument = ConstantsPackage.createCompileTimeConstant(value, true, false, false, null);
                return argument != null ? argument : ErrorValue.create("Unsupported annotation argument: " + name);
            }

            private void setArgumentValueByName(@NotNull Name name, @NotNull CompileTimeConstant<?> argumentValue) {
                ValueParameterDescriptor parameter = DescriptorResolverUtils.getAnnotationParameterByName(name, annotationClass);
                if (parameter != null) {
                    arguments.put(parameter, argumentValue);
                }
            }
        };
    }

    @NotNull
    private static ClassDescriptor resolveClass(@NotNull ClassId javaClassId, @NotNull ModuleDescriptor moduleDescriptor) {
        ClassId classId = javaClassIdToKotlinClassId(javaClassId);
        ClassDescriptor classDescriptor = SerializationPackage.findClassAcrossModuleDependencies(moduleDescriptor, classId);
        return classDescriptor != null ? classDescriptor : ErrorUtils.createErrorClass(classId.asSingleFqName().asString());
    }
}
