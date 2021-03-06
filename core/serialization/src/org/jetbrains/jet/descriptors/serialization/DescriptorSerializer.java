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

package org.jetbrains.jet.descriptors.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.resolve.DescriptorFactory;
import org.jetbrains.jet.lang.resolve.MemberComparator;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.DescriptorUtils.*;

public class DescriptorSerializer {

    private final NameTable nameTable;
    private final Interner<TypeParameterDescriptor> typeParameters;
    private final SerializerExtension extension;

    private DescriptorSerializer(NameTable nameTable, Interner<TypeParameterDescriptor> typeParameters, SerializerExtension extension) {
        this.nameTable = nameTable;
        this.typeParameters = typeParameters;
        this.extension = extension;
    }

    @NotNull
    public static DescriptorSerializer createTopLevel(@NotNull SerializerExtension extension) {
        return new DescriptorSerializer(new NameTable(), new Interner<TypeParameterDescriptor>(), extension);
    }

    @NotNull
    public static DescriptorSerializer create(@NotNull ClassDescriptor descriptor, @NotNull SerializerExtension extension) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        DescriptorSerializer parentSerializer =
                container instanceof ClassDescriptor
                ? create((ClassDescriptor) container, extension)
                : createTopLevel(extension);

        // Calculate type parameter ids for the outer class beforehand, as it would've had happened if we were always
        // serializing outer classes before nested classes.
        // Otherwise our interner can get wrong ids because we may serialize classes in any order.
        DescriptorSerializer serializer = parentSerializer.createChildSerializer();
        for (TypeParameterDescriptor typeParameter : descriptor.getTypeConstructor().getParameters()) {
            serializer.typeParameters.intern(typeParameter);
        }
        return serializer;
    }

    private DescriptorSerializer createChildSerializer() {
        return new DescriptorSerializer(nameTable, new Interner<TypeParameterDescriptor>(typeParameters), extension);
    }

    @NotNull
    public NameTable getNameTable() {
        return nameTable;
    }

    @NotNull
    public ProtoBuf.Class.Builder classProto(@NotNull ClassDescriptor classDescriptor) {
        ProtoBuf.Class.Builder builder = ProtoBuf.Class.newBuilder();

        int flags = Flags.getClassFlags(hasAnnotations(classDescriptor), classDescriptor.getVisibility(),
                                        classDescriptor.getModality(), classDescriptor.getKind(), classDescriptor.isInner());
        builder.setFlags(flags);

        // TODO extra visibility

        builder.setFqName(getClassId(classDescriptor));

        for (TypeParameterDescriptor typeParameterDescriptor : classDescriptor.getTypeConstructor().getParameters()) {
            builder.addTypeParameter(typeParameter(typeParameterDescriptor));
        }

        if (!KotlinBuiltIns.getInstance().isSpecialClassWithNoSupertypes(classDescriptor)) {
            // Special classes (Any, Nothing) have no supertypes
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                builder.addSupertype(type(supertype));
            }
        }

        ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
        if (primaryConstructor != null) {
            if (DescriptorFactory.isDefaultPrimaryConstructor(primaryConstructor)) {
                builder.setPrimaryConstructor(ProtoBuf.Class.PrimaryConstructor.getDefaultInstance());
            }
            else {
                ProtoBuf.Class.PrimaryConstructor.Builder constructorBuilder = ProtoBuf.Class.PrimaryConstructor.newBuilder();
                constructorBuilder.setData(callableProto(primaryConstructor));
                builder.setPrimaryConstructor(constructorBuilder);
            }
        }

        // TODO: other constructors

        for (DeclarationDescriptor descriptor : sort(classDescriptor.getDefaultType().getMemberScope().getAllDescriptors())) {
            if (descriptor instanceof CallableMemberDescriptor) {
                CallableMemberDescriptor member = (CallableMemberDescriptor) descriptor;
                if (member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE) continue;
                builder.addMember(callableProto(member));
            }
        }

        for (DeclarationDescriptor descriptor : sort(classDescriptor.getUnsubstitutedInnerClassesScope().getAllDescriptors())) {
            int name = nameTable.getSimpleNameIndex(descriptor.getName());
            if (isEnumEntry(descriptor)) {
                builder.addEnumEntry(name);
            }
            else {
                builder.addNestedClassName(name);
            }
        }

        ClassDescriptor classObject = classDescriptor.getClassObjectDescriptor();
        if (classObject != null) {
            builder.setClassObject(classObjectProto(classObject));
        }

        return builder;
    }

    @NotNull
    private ProtoBuf.Class.ClassObject classObjectProto(@NotNull ClassDescriptor classObject) {
        if (isObject(classObject.getContainingDeclaration())) {
            return ProtoBuf.Class.ClassObject.newBuilder().setData(classProto(classObject)).build();
        }

        return ProtoBuf.Class.ClassObject.getDefaultInstance();
    }

    @NotNull
    public ProtoBuf.Callable.Builder callableProto(@NotNull CallableMemberDescriptor descriptor) {
        ProtoBuf.Callable.Builder builder = ProtoBuf.Callable.newBuilder();

        DescriptorSerializer local = createChildSerializer();

        boolean hasGetter = false;
        boolean hasSetter = false;
        boolean hasConstant = false;
        if (descriptor instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

            int propertyFlags = Flags.getAccessorFlags(
                    hasAnnotations(propertyDescriptor),
                    propertyDescriptor.getVisibility(),
                    propertyDescriptor.getModality(),
                    false
            );

            PropertyGetterDescriptor getter = propertyDescriptor.getGetter();
            if (getter != null) {
                hasGetter = true;
                int accessorFlags = getAccessorFlags(getter);
                if (accessorFlags != propertyFlags) {
                    builder.setGetterFlags(accessorFlags);
                }
            }

            PropertySetterDescriptor setter = propertyDescriptor.getSetter();
            if (setter != null) {
                hasSetter = true;
                int accessorFlags = getAccessorFlags(setter);
                if (accessorFlags != propertyFlags) {
                    builder.setSetterFlags(accessorFlags);
                }

                if (!setter.isDefault()) {
                    for (ValueParameterDescriptor valueParameterDescriptor : setter.getValueParameters()) {
                        builder.addValueParameter(local.valueParameter(valueParameterDescriptor));
                    }
                }
            }

            hasConstant = propertyDescriptor.getCompileTimeInitializer() != null;
        }

        builder.setFlags(Flags.getCallableFlags(
                hasAnnotations(descriptor),
                descriptor.getVisibility(),
                descriptor.getModality(),
                descriptor.getKind(),
                callableKind(descriptor),
                hasGetter,
                hasSetter,
                hasConstant
        ));
        //TODO builder.setExtraVisibility()

        for (TypeParameterDescriptor typeParameterDescriptor : descriptor.getTypeParameters()) {
            builder.addTypeParameter(local.typeParameter(typeParameterDescriptor));
        }

        ReceiverParameterDescriptor receiverParameter = descriptor.getExtensionReceiverParameter();
        if (receiverParameter != null) {
            builder.setReceiverType(local.type(receiverParameter.getType()));
        }

        builder.setName(nameTable.getSimpleNameIndex(descriptor.getName()));

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            builder.addValueParameter(local.valueParameter(valueParameterDescriptor));
        }

        builder.setReturnType(local.type(getSerializableReturnType(descriptor.getReturnType())));

        extension.serializeCallable(descriptor, builder, nameTable);

        return builder;
    }

    @NotNull
    private static JetType getSerializableReturnType(@NotNull JetType type) {
        return isSerializableType(type) ? type : KotlinBuiltIns.getInstance().getAnyType();
    }

    /**
     * @return true iff this type can be serialized. Types which correspond to type parameters, top-level classes, inner classes, and
     * generic classes with serializable arguments are serializable. For other types (local classes, inner of local, etc.) it may be
     * problematical to construct a FQ name for serialization
     */
    private static boolean isSerializableType(@NotNull JetType type) {
        ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
        if (descriptor instanceof TypeParameterDescriptor) {
            return true;
        }
        else if (descriptor instanceof ClassDescriptor) {
            for (TypeProjection projection : type.getArguments()) {
                if (!isSerializableType(projection.getType())) {
                    return false;
                }
            }

            return isTopLevelOrInnerClass((ClassDescriptor) descriptor);
        }
        else {
            throw new IllegalStateException("Unknown type constructor: " + type);
        }
    }

    private static int getAccessorFlags(@NotNull PropertyAccessorDescriptor accessor) {
        return Flags.getAccessorFlags(
                hasAnnotations(accessor),
                accessor.getVisibility(),
                accessor.getModality(),
                !accessor.isDefault()
        );
    }

    @NotNull
    private static ProtoBuf.Callable.CallableKind callableKind(@NotNull CallableMemberDescriptor descriptor) {
        if (descriptor instanceof PropertyDescriptor) {
            return ((PropertyDescriptor) descriptor).isVar() ? ProtoBuf.Callable.CallableKind.VAR : ProtoBuf.Callable.CallableKind.VAL;
        }
        if (descriptor instanceof ConstructorDescriptor) {
            return ProtoBuf.Callable.CallableKind.CONSTRUCTOR;
        }
        assert descriptor instanceof FunctionDescriptor : "Unknown descriptor class: " + descriptor.getClass();
        return ProtoBuf.Callable.CallableKind.FUN;
    }

    @NotNull
    private ProtoBuf.Callable.ValueParameter.Builder valueParameter(@NotNull ValueParameterDescriptor descriptor) {
        ProtoBuf.Callable.ValueParameter.Builder builder = ProtoBuf.Callable.ValueParameter.newBuilder();

        builder.setFlags(Flags.getValueParameterFlags(hasAnnotations(descriptor), descriptor.declaresDefaultValue()));

        builder.setName(nameTable.getSimpleNameIndex(descriptor.getName()));

        builder.setType(type(descriptor.getType()));

        JetType varargElementType = descriptor.getVarargElementType();
        if (varargElementType != null) {
            builder.setVarargElementType(type(varargElementType));
        }

        extension.serializeValueParameter(descriptor, builder, nameTable);

        return builder;
    }

    private ProtoBuf.TypeParameter.Builder typeParameter(TypeParameterDescriptor typeParameter) {
        ProtoBuf.TypeParameter.Builder builder = ProtoBuf.TypeParameter.newBuilder();

        builder.setId(getTypeParameterId(typeParameter));

        builder.setName(nameTable.getSimpleNameIndex(typeParameter.getName()));

        // to avoid storing a default
        if (typeParameter.isReified()) {
            builder.setReified(true);
        }

        // to avoid storing a default
        ProtoBuf.TypeParameter.Variance variance = variance(typeParameter.getVariance());
        if (variance != ProtoBuf.TypeParameter.Variance.INV) {
            builder.setVariance(variance);
        }

        for (JetType upperBound : typeParameter.getUpperBounds()) {
            builder.addUpperBound(type(upperBound));
        }

        return builder;
    }

    private static ProtoBuf.TypeParameter.Variance variance(Variance variance) {
        switch (variance) {
            case INVARIANT:
                return ProtoBuf.TypeParameter.Variance.INV;
            case IN_VARIANCE:
                return ProtoBuf.TypeParameter.Variance.IN;
            case OUT_VARIANCE:
                return  ProtoBuf.TypeParameter.Variance.OUT;
        }
        throw new IllegalStateException("Unknown variance: " + variance);
    }

    @NotNull
    public ProtoBuf.Type.Builder type(@NotNull JetType type) {
        assert !type.isError() : "Can't serialize error types: " + type; // TODO

        if (TypesPackage.isFlexible(type)) return flexibleType(type);

        ProtoBuf.Type.Builder builder = ProtoBuf.Type.newBuilder();

        builder.setConstructor(typeConstructor(type.getConstructor()));

        for (TypeProjection projection : type.getArguments()) {
            builder.addArgument(typeArgument(projection));
        }

        // to avoid storing a default
        if (type.isNullable()) {
            builder.setNullable(true);
        }

        return builder;
    }

    private ProtoBuf.Type.Builder flexibleType(@NotNull JetType type) {
        Flexibility flexibility = TypesPackage.flexibility(type);

        ProtoBuf.Type.Builder builder = type(flexibility.getLowerBound());

        builder.setFlexibleTypeCapabilitiesId(nameTable.getStringIndex(flexibility.getExtraCapabilities().getId()));

        builder.setFlexibleUpperBound(type(flexibility.getUpperBound()));

        return builder;
    }

    @NotNull
    private ProtoBuf.Type.Argument.Builder typeArgument(@NotNull TypeProjection typeProjection) {
        ProtoBuf.Type.Argument.Builder builder = ProtoBuf.Type.Argument.newBuilder();
        ProtoBuf.Type.Argument.Projection projection = projection(typeProjection.getProjectionKind());

        // to avoid storing a default
        if (projection != ProtoBuf.Type.Argument.Projection.INV) {
            builder.setProjection(projection);
        }

        builder.setType(type(typeProjection.getType()));
        return builder;
    }

    @NotNull
    private ProtoBuf.Type.Constructor.Builder typeConstructor(@NotNull TypeConstructor typeConstructor) {
        ProtoBuf.Type.Constructor.Builder builder = ProtoBuf.Type.Constructor.newBuilder();

        ClassifierDescriptor declarationDescriptor = typeConstructor.getDeclarationDescriptor();

        assert declarationDescriptor instanceof TypeParameterDescriptor || declarationDescriptor instanceof ClassDescriptor
                : "Unknown declaration descriptor: " + typeConstructor;
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) declarationDescriptor;
            builder.setKind(ProtoBuf.Type.Constructor.Kind.TYPE_PARAMETER);
            builder.setId(getTypeParameterId(typeParameterDescriptor));
        }
        else {
            ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
            //default: builder.setKind(Type.Constructor.Kind.CLASS);
            builder.setId(getClassId(classDescriptor));
        }
        return builder;
    }

    @NotNull
    public ProtoBuf.Package.Builder packageProto(@NotNull Collection<PackageFragmentDescriptor> fragments) {
        ProtoBuf.Package.Builder builder = ProtoBuf.Package.newBuilder();

        Collection<DeclarationDescriptor> members = new ArrayList<DeclarationDescriptor>();
        for (PackageFragmentDescriptor fragment : fragments) {
            members.addAll(fragment.getMemberScope().getAllDescriptors());
        }

        for (DeclarationDescriptor declaration : sort(members)) {
            if (declaration instanceof PropertyDescriptor || declaration instanceof FunctionDescriptor) {
                builder.addMember(callableProto((CallableMemberDescriptor) declaration));
            }
        }

        return builder;
    }

    @NotNull
    private static ProtoBuf.Type.Argument.Projection projection(@NotNull Variance projectionKind) {
        switch (projectionKind) {
            case INVARIANT:
                return ProtoBuf.Type.Argument.Projection.INV;
            case IN_VARIANCE:
                return ProtoBuf.Type.Argument.Projection.IN;
            case OUT_VARIANCE:
                return ProtoBuf.Type.Argument.Projection.OUT;
        }
        throw new IllegalStateException("Unknown projectionKind: " + projectionKind);
    }

    private int getClassId(@NotNull ClassDescriptor descriptor) {
        return nameTable.getFqNameIndex(descriptor);
    }

    private int getTypeParameterId(@NotNull TypeParameterDescriptor descriptor) {
        return typeParameters.intern(descriptor);
    }

    private static boolean hasAnnotations(Annotated descriptor) {
        return !descriptor.getAnnotations().isEmpty();
    }

    @NotNull
    public static <T extends DeclarationDescriptor> List<T> sort(@NotNull Collection<T> descriptors) {
        List<T> result = new ArrayList<T>(descriptors);
        //NOTE: the exact comparator does matter here
        Collections.sort(result, MemberComparator.INSTANCE);
        return result;

    }
}
