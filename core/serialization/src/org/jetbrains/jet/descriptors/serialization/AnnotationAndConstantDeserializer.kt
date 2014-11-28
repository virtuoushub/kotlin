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

package org.jetbrains.jet.descriptors.serialization

import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotatedCallableKind
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationAndConstantLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.ProtoContainer
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Annotation
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Annotation.Argument
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Annotation.Argument.Value
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Annotation.Argument.Value.Type
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.descriptors.ClassKind

public class AnnotationAndConstantDeserializer(
        private val module: ModuleDescriptor
) : AnnotationAndConstantLoader<AnnotationDescriptor, CompileTimeConstant<*>> {
    override fun loadClassAnnotations(
            classProto: ProtoBuf.Class,
            nameResolver: NameResolver
    ): List<AnnotationDescriptor> =
            classProto.getAnnotationList().map { proto -> resolveAnnotation(proto, nameResolver) }.filterNotNull()

    override fun loadCallableAnnotations(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind
    ): List<AnnotationDescriptor> =
            proto.getAnnotationList().map { proto -> resolveAnnotation(proto, nameResolver) }.filterNotNull()

    override fun loadValueParameterAnnotations(
            container: ProtoContainer,
            callable: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind,
            proto: ProtoBuf.Callable.ValueParameter
    ): List<AnnotationDescriptor> =
            proto.getAnnotationList().map { proto -> resolveAnnotation(proto, nameResolver) }.filterNotNull()

    override fun loadPropertyConstant(
            container: ProtoContainer,
            proto: ProtoBuf.Callable,
            nameResolver: NameResolver,
            kind: AnnotatedCallableKind
    ): CompileTimeConstant<*>? {
        // TODO: deserialize compile time constants
        throw UnsupportedOperationException()
    }


    private fun resolveAnnotation(proto: Annotation, nameResolver: NameResolver): AnnotationDescriptor? {
        val annotationClass = module.findClassAcrossModuleDependencies(nameResolver.getClassId(proto.getId())) ?: return null

        val arguments = if (proto.getArgumentCount() == 0) {
            mapOf()
        }
        else {
            val parameters = annotationClass.getConstructors().single().getValueParameters().toMap { it.getName() }
            val arguments = proto.getArgumentList().map { resolveArgument(it, parameters, nameResolver) }.filterNotNull()
            mapOf(*arguments.copyToArray())
        }

        return AnnotationDescriptorImpl(annotationClass.getDefaultType(), arguments)
    }

    private fun resolveArgument(
            proto: Argument,
            parameters: Map<Name, ValueParameterDescriptor>,
            nameResolver: NameResolver
    ): Pair<ValueParameterDescriptor, CompileTimeConstant<*>>? {
        val parameter = parameters[nameResolver.getName(proto.getNameId())] ?: return null
        return Pair(parameter, resolveValue(parameter.getType(), proto.getValue(), nameResolver) ?: return null)
    }

    private fun resolveValue(
            expectedType: JetType,
            value: ProtoBuf.Annotation.Argument.Value,
            nameResolver: NameResolver
    ): CompileTimeConstant<*>? {
        // TODO: maybe use createCompileTimeConstant somehow instead
        val result = when (value.getType()) {
            Type.BYTE -> ByteValue(value.getIntValue().toByte(), true, true, true)
            Type.CHAR -> CharValue(value.getIntValue().toChar(), true, true, true)
            Type.SHORT -> ShortValue(value.getIntValue().toShort(), true, true, true)
            Type.INT -> IntValue(value.getIntValue().toInt(), true, true, true)
            Type.LONG -> LongValue(value.getIntValue(), true, true, true)
            Type.FLOAT -> FloatValue(value.getFloatValue(), true, true)
            Type.DOUBLE -> DoubleValue(value.getDoubleValue(), true, true)
            Type.BOOLEAN -> BooleanValue(value.getIntValue() != 0L, true, true)
            Type.STRING -> {
                StringValue(nameResolver.getString(value.getStringValue()), true, true)
            }
            Type.CLASS -> {
                // TODO: support class literals
                error("Class literal annotation arguments are not supported yet (${nameResolver.getClassId(value.getClassId())})")
            }
            Type.ENUM -> {
                val enumClass = resolveClass(nameResolver.getClassId(value.getEnumClassId()))
                val entryName = nameResolver.getName(value.getEnumValueId())
                if (enumClass.getKind() != ClassKind.ENUM_CLASS) return null
                EnumValue(enumClass.getUnsubstitutedInnerClassesScope().getClassifier(entryName) as? ClassDescriptor ?: return null, true)
            }
            Type.ANNOTATION -> {
                AnnotationValue(resolveAnnotation(value.getAnnotation(), nameResolver))
            }
            Type.ARRAY -> {
                if (!KotlinBuiltIns.getInstance().isArray(expectedType) &&
                    !KotlinBuiltIns.getInstance().isPrimitiveArray(expectedType)) return null

                val arrayElements = value.getArrayElementList()
                val elementType =
                        if (arrayElements.isNotEmpty()) resolveArrayElementType(arrayElements.first(), nameResolver)
                        else {
                            // In the case of empty array, no element has the element type, so we recover it from the expected type.
                            // This is not very accurate, but should in fact not matter, because the value is empty anyway
                            KotlinBuiltIns.getInstance().getArrayElementType(expectedType)
                        }

                ArrayValue(
                        arrayElements.map { resolveValue(elementType, it, nameResolver) },
                        expectedType,
                        true, true
                )
            }
            else -> error("Unsupported annotation argument type: ${value.getType()} (expected $expectedType)")
        }

        if (result.getType(KotlinBuiltIns.getInstance()) != expectedType) {
            // This means that an annotation class has been changed incompatibly without recompiling clients
            return null
        }

        return result
    }

    private fun resolveArrayElementType(value: Value, nameResolver: NameResolver): JetType = when (value.getType()!!) {
        Type.BYTE -> KotlinBuiltIns.getInstance().getByteType()
        Type.CHAR -> KotlinBuiltIns.getInstance().getCharType()
        Type.SHORT -> KotlinBuiltIns.getInstance().getShortType()
        Type.INT -> KotlinBuiltIns.getInstance().getIntType()
        Type.LONG -> KotlinBuiltIns.getInstance().getLongType()
        Type.FLOAT -> KotlinBuiltIns.getInstance().getFloatType()
        Type.DOUBLE -> KotlinBuiltIns.getInstance().getDoubleType()
        Type.BOOLEAN -> KotlinBuiltIns.getInstance().getBooleanType()
        Type.STRING -> KotlinBuiltIns.getInstance().getStringType()
        // TODO: support arrays of class literals
        Type.CLASS -> error("Arrays of class literals are not supported yet")
        Type.ENUM -> resolveClass(nameResolver.getClassId(value.getEnumClassId())).getDefaultType()
        Type.ANNOTATION -> resolveClass(nameResolver.getClassId(value.getAnnotation().getId())).getDefaultType()
        Type.ARRAY -> error("Array of arrays is impossible")
    }

    private fun resolveClass(classId: ClassId): ClassDescriptor {
        return module.findClassAcrossModuleDependencies(classId)
               ?: ErrorUtils.createErrorClass(classId.asSingleFqName().asString())
    }
}
