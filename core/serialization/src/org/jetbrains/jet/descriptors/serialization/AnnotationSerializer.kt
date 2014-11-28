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

import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.jet.lang.resolve.constants.*
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Annotation.Argument.Value
import org.jetbrains.jet.descriptors.serialization.ProtoBuf.Annotation.Argument.Value.Type
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns

public object AnnotationSerializer {
    public fun serializeAnnotation(annotation: AnnotationDescriptor, nameTable: NameTable): ProtoBuf.Annotation {
        return with(ProtoBuf.Annotation.newBuilder()) {
            val annotationClass = annotation.getType().getConstructor().getDeclarationDescriptor() as? ClassDescriptor
                                  ?: error("Annotation type is not a class: ${annotation.getType()}")

            setId(nameTable.getFqNameIndex(annotationClass))

            for ((parameter, value) in annotation.getAllValueArguments()) {
                val argument = ProtoBuf.Annotation.Argument.newBuilder()
                argument.setNameId(nameTable.getSimpleNameIndex(parameter.getName()))
                argument.setValue(valueProto(value, parameter.getType(), nameTable))
                addArgument(argument)
            }

            build()
        }
    }

    fun valueProto(constant: CompileTimeConstant<*>, type: JetType, nameTable: NameTable): Value.Builder = with(Value.newBuilder()) {
        constant.accept(object : AnnotationArgumentVisitor<Unit, Unit> {
            override fun visitAnnotationValue(value: AnnotationValue, data: Unit): Unit {
                setType(Type.ANNOTATION)
                setAnnotation(serializeAnnotation(value.getValue(), nameTable))
            }

            override fun visitArrayValue(value: ArrayValue, data: Unit): Unit {
                setType(Type.ARRAY)
                for (element in value.getValue()) {
                    addArrayElement(valueProto(element, KotlinBuiltIns.getInstance().getArrayElementType(type), nameTable).build())
                }
            }

            override fun visitBooleanValue(value: BooleanValue, data: Unit): Unit {
                setType(Type.BOOLEAN)
                setIntValue(if (value.getValue()) 1 else 0)
            }

            override fun visitByteValue(value: ByteValue, data: Unit): Unit {
                setType(Type.BYTE)
                setIntValue(value.getValue().toLong())
            }

            override fun visitCharValue(value: CharValue, data: Unit): Unit {
                setType(Type.CHAR)
                setIntValue(value.getValue().toLong())
            }

            override fun visitDoubleValue(value: DoubleValue, data: Unit): Unit {
                setType(Type.DOUBLE)
                setDoubleValue(value.getValue())
            }

            override fun visitEnumValue(value: EnumValue, data: Unit): Unit {
                setType(Type.ENUM)
                val enumEntry = value.getValue()
                setEnumClassId(nameTable.getFqNameIndex(enumEntry.getContainingDeclaration() as ClassDescriptor))
                setEnumValueId(nameTable.getSimpleNameIndex(enumEntry.getName()))
            }

            override fun visitErrorValue(value: ErrorValue, data: Unit): Unit {
                throw UnsupportedOperationException()
            }

            override fun visitFloatValue(value: FloatValue, data: Unit): Unit {
                setType(Type.FLOAT)
                setFloatValue(value.getValue())
            }

            override fun visitIntValue(value: IntValue, data: Unit): Unit {
                setType(Type.INT)
                setIntValue(value.getValue().toLong())
            }

            override fun visitJavaClassValue(value: JavaClassValue, data: Unit): Unit {
                throw UnsupportedOperationException()
            }

            override fun visitLongValue(value: LongValue, data: Unit): Unit {
                setType(Type.LONG)
                setIntValue(value.getValue())
            }

            override fun visitNullValue(value: NullValue, data: Unit): Unit {
                throw UnsupportedOperationException("Null should not appear in annotation arguments")
            }

            override fun visitNumberTypeValue(constant: IntegerValueTypeConstant, data: Unit): Unit {
                // TODO: IntegerValueTypeConstant should not occur in annotation arguments
                val number = constant.getValue(type)
                val specificConstant = with(KotlinBuiltIns.getInstance()) {
                    when (type) {
                        getLongType() -> LongValue(number.toLong(), true, true, true)
                        getIntType() -> IntValue(number.toInt(), true, true, true)
                        getShortType() -> ShortValue(number.toShort(), true, true, true)
                        getCharType() -> CharValue(number.toChar(), true, true, true)
                        getByteType() -> ByteValue(number.toByte(), true, true, true)
                        else -> throw IllegalStateException("Integer constant $constant has non-integer type $type")
                    }
                }

                specificConstant.accept(this, data)
            }

            override fun visitShortValue(value: ShortValue, data: Unit): Unit {
                setType(Type.SHORT)
                setIntValue(value.getValue().toLong())
            }

            override fun visitStringValue(value: StringValue, data: Unit): Unit {
                setType(Type.STRING)
                setStringValue(nameTable.getStringIndex(value.getValue()))
            }
        }, Unit)

        this
    }
}
