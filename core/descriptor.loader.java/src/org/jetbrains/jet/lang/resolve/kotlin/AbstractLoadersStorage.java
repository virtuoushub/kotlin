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
import org.jetbrains.jet.lang.resolve.name.ClassId;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractLoadersStorage<A, C> {
    @NotNull
    protected abstract Storage<A, C> getStorageForClass(@NotNull KotlinJvmBinaryClass kotlinClass);

    @NotNull
    protected Storage<A, C> loadAnnotationsAndInitializers(@NotNull KotlinJvmBinaryClass kotlinClass) {
        final Map<MemberSignature, List<A>> memberAnnotations =
                new HashMap<MemberSignature, List<A>>();
        final Map<MemberSignature, C> propertyConstants = new HashMap<MemberSignature, C>();

        kotlinClass.visitMembers(new KotlinJvmBinaryClass.MemberVisitor() {
            @Nullable
            @Override
            public KotlinJvmBinaryClass.MethodAnnotationVisitor visitMethod(@NotNull Name name, @NotNull String desc) {
                return new AnnotationVisitorForMethod(MemberSignature.fromMethodNameAndDesc(name.asString() + desc));
            }

            @Nullable
            @Override
            public KotlinJvmBinaryClass.AnnotationVisitor visitField(
                    @NotNull Name name,
                    @NotNull String desc,
                    @Nullable Object initializer
            ) {
                MemberSignature signature = MemberSignature.fromFieldNameAndDesc(name, desc);

                if (initializer != null) {
                    C constant = loadConstant(desc, initializer);
                    if (constant != null) {
                        propertyConstants.put(signature, constant);
                    }
                }
                return new MemberAnnotationVisitor(signature);
            }

            class AnnotationVisitorForMethod extends MemberAnnotationVisitor implements KotlinJvmBinaryClass.MethodAnnotationVisitor {
                public AnnotationVisitorForMethod(@NotNull MemberSignature signature) {
                    super(signature);
                }

                @Nullable
                @Override
                public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitParameterAnnotation(int index, @NotNull ClassId classId) {
                    MemberSignature paramSignature = MemberSignature.fromMethodSignatureAndParameterIndex(signature, index);
                    List<A> result = memberAnnotations.get(paramSignature);
                    if (result == null) {
                        result = new ArrayList<A>();
                        memberAnnotations.put(paramSignature, result);
                    }
                    return loadAnnotation(classId, result);
                }
            }

            class MemberAnnotationVisitor implements KotlinJvmBinaryClass.AnnotationVisitor {
                private final List<A> result = new ArrayList<A>();
                protected final MemberSignature signature;

                public MemberAnnotationVisitor(@NotNull MemberSignature signature) {
                    this.signature = signature;
                }

                @Nullable
                @Override
                public KotlinJvmBinaryClass.AnnotationArgumentVisitor visitAnnotation(@NotNull ClassId classId) {
                    return loadAnnotation(classId, result);
                }

                @Override
                public void visitEnd() {
                    if (!result.isEmpty()) {
                        memberAnnotations.put(signature, result);
                    }
                }
            }
        });

        return new Storage<A, C>(memberAnnotations, propertyConstants);
    }

    protected abstract KotlinJvmBinaryClass.AnnotationArgumentVisitor loadAnnotation(ClassId classId, List<A> result);

    protected abstract C loadConstant(String desc, Object initializer);

    // The purpose of this class is to hold a unique signature of either a method or a field, so that annotations on a member can be put
    // into a map indexed by these signatures
    public static final class MemberSignature {
        private final String signature;

        private MemberSignature(@NotNull String signature) {
            this.signature = signature;
        }

        @NotNull
        public static MemberSignature fromMethodNameAndDesc(@NotNull String nameAndDesc) {
            return new MemberSignature(nameAndDesc);
        }

        @NotNull
        public static MemberSignature fromFieldNameAndDesc(@NotNull Name name, @NotNull String desc) {
            return new MemberSignature(name.asString() + "#" + desc);
        }

        @NotNull
        public static MemberSignature fromMethodSignatureAndParameterIndex(@NotNull MemberSignature signature, int index) {
            return new MemberSignature(signature.signature + "@" + index);
        }

        @Override
        public int hashCode() {
            return signature.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MemberSignature && signature.equals(((MemberSignature) o).signature);
        }

        @Override
        public String toString() {
            return signature;
        }
    }

    protected static class Storage<A, C> {
        private final Map<MemberSignature, List<A>> memberAnnotations;
        private final Map<MemberSignature, C> propertyConstants;

        public Storage(
                @NotNull Map<MemberSignature, List<A>> annotations,
                @NotNull Map<MemberSignature, C> constants
        ) {
            this.memberAnnotations = annotations;
            this.propertyConstants = constants;
        }

        public Map<MemberSignature, List<A>> getMemberAnnotations() {
            return memberAnnotations;
        }

        public Map<MemberSignature, C> getPropertyConstants() {
            return propertyConstants;
        }
    }
}
