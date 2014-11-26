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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBuf;
import org.jetbrains.jet.descriptors.serialization.NameResolver;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotatedCallableKind;
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationLoader;
import org.jetbrains.jet.descriptors.serialization.descriptors.ProtoContainer;
import org.jetbrains.jet.lang.resolve.java.resolver.ErrorReporter;
import org.jetbrains.jet.lang.resolve.name.ClassId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractAnnotationLoader<A> extends BaseLoader implements AnnotationLoader<A> {
    private AbstractLoadersStorage<A, ?> storage;

    public AbstractAnnotationLoader(
            @NotNull AbstractLoadersStorage<A, ?> storage,
            @NotNull KotlinClassFinder kotlinClassFinder,
            @NotNull ErrorReporter errorReporter
    ) {
        super(kotlinClassFinder, errorReporter);
        this.storage = storage;
    }

    @NotNull
    @Override
    public List<A> loadClassAnnotations(
            @NotNull ProtoBuf.Class classProto,
            @NotNull NameResolver nameResolver
    ) {
        KotlinJvmBinaryClass kotlinClass = findKotlinClassByProto(classProto, nameResolver);
        if (kotlinClass == null) {
            // This means that the resource we're constructing the descriptor from is no longer present: KotlinClassFinder had found the
            // class earlier, but it can't now
            //TODO_R: message
            getErrorReporter().reportLoadingError("Kotlin class for loading class annotations is not found: " + classProto, null);
            return Collections.emptyList();
        }

        final List<A> result = new ArrayList<A>(1);

        kotlinClass.loadClassAnnotations(new KotlinJvmBinaryClass.AnnotationVisitor() {
            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitAnnotation(@NotNull ClassId classId) {
                return loadAnnotation(classId, result);
            }

            @Override
            public void visitEnd() {
            }
        });

        return result;
    }

    protected abstract KotlinJvmBinaryClass.AnnotationArgumentVisitor loadAnnotation(@NotNull ClassId classId, @NotNull List<A> result);

    @NotNull
    @Override
    public List<A> loadCallableAnnotations(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind
    ) {
        AbstractLoadersStorage.MemberSignature signature = getCallableSignature(proto, nameResolver, kind);
        if (signature == null) return Collections.emptyList();

        return findClassAndLoadMemberAnnotations(container, proto, nameResolver, kind, signature);
    }

    @NotNull
    private List<A> findClassAndLoadMemberAnnotations(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable proto,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind,
            @NotNull AbstractLoadersStorage.MemberSignature signature
    ) {
        KotlinJvmBinaryClass kotlinClass = findClassWithAnnotationsAndInitializers(container, proto, nameResolver, kind);
        if (kotlinClass == null) {
            getErrorReporter().reportLoadingError("Kotlin class for loading member annotations is not found: " + container, null);
            return Collections.emptyList();
        }

        List<A> descriptors = storage.getStorageForClass(kotlinClass).getMemberAnnotations().get(signature);
        return descriptors == null ? Collections.<A>emptyList() : descriptors;
    }

    @NotNull
    @Override
    public List<A> loadValueParameterAnnotations(
            @NotNull ProtoContainer container,
            @NotNull ProtoBuf.Callable callable,
            @NotNull NameResolver nameResolver,
            @NotNull AnnotatedCallableKind kind,
            @NotNull ProtoBuf.Callable.ValueParameter proto
    ) {
        AbstractLoadersStorage.MemberSignature methodSignature = getCallableSignature(callable, nameResolver, kind);
        if (methodSignature != null) {
            if (proto.hasExtension(JavaProtoBuf.index)) {
                AbstractLoadersStorage.MemberSignature paramSignature =
                        AbstractLoadersStorage.MemberSignature
                                .fromMethodSignatureAndParameterIndex(methodSignature, proto.getExtension(JavaProtoBuf.index));
                return findClassAndLoadMemberAnnotations(container, callable, nameResolver, kind, paramSignature);
            }
        }

        return Collections.emptyList();
    }
}
