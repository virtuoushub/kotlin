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

package org.jetbrains.jet.lang.resolve.java.sam;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorImpl;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaMethodDescriptor;
import org.jetbrains.jet.lang.resolve.java.descriptor.SamAdapterDescriptor;

/* package */ class SamAdapterFunctionDescriptor extends JavaMethodDescriptor implements SamAdapterDescriptor<JavaMethodDescriptor> {
    private final JavaMethodDescriptor declaration;

    private SamAdapterFunctionDescriptor(
            @NotNull DeclarationDescriptor owner,
            @NotNull JavaMethodDescriptor declaration,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Kind kind
    ) {
        super(owner, original, declaration.getAnnotations(), declaration.getName(), kind, declaration.getSource());
        this.declaration = declaration;
        setHasStableParameterNames(declaration.hasStableParameterNames());
        setHasSynthesizedParameterNames(declaration.hasSynthesizedParameterNames());
    }

    public SamAdapterFunctionDescriptor(@NotNull JavaMethodDescriptor declaration) {
        this(declaration.getContainingDeclaration(), declaration, null, Kind.SYNTHESIZED);
    }

    @NotNull
    @Override
    public JavaMethodDescriptor getOriginForSam() {
        return declaration;
    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner, @Nullable FunctionDescriptor original, @NotNull Kind kind
    ) {
        return new SamAdapterFunctionDescriptor(newOwner, declaration, (SimpleFunctionDescriptor) original, kind);
    }
}
