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

package org.jetbrains.jet.lang.resolve.calls.inference

import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.lang.types.TypeProjection
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.TypeUtils.DONT_CARE
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.types.ErrorUtils
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind
import org.jetbrains.jet.lang.types.checker.TypeCheckingProcedure
import org.jetbrains.jet.lang.types.checker.TypingConstraints
import org.jetbrains.jet.lang.types.TypeConstructor
import java.util.LinkedHashMap
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.calls.inference.TypeBounds.BoundKind.*
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintSystemImpl.ConstraintKind.*
import java.util.HashMap
import java.util.ArrayList
import org.jetbrains.kotlin.util.sure
import org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition.ConstraintPositionKind.*
import org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition.CompoundConstraintPosition
import org.jetbrains.jet.lang.resolve.calls.inference.constraintPosition.getCompoundConstraintPosition

public class ConstraintSystemImpl : ConstraintSystem {

    public enum class ConstraintKind {
        SUB_TYPE
        EQUAL
    }

    private val typeParameterBounds = LinkedHashMap<TypeParameterDescriptor, TypeBoundsImpl>()
    private val errorConstraintPositions = HashSet<ConstraintPosition>()
    private var hasErrorInConstrainingTypes: Boolean = false

    private val constraintSystemStatus = object : ConstraintSystemStatus {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        override fun isSuccessful() = !hasContradiction() && !hasUnknownParameters()

        override fun hasContradiction() = hasTypeConstructorMismatch() || hasConflictingConstraints()

        override fun hasViolatedUpperBound(): Boolean {
            if (isSuccessful()) return false
            return getSystemWithoutWeakConstraints().getStatus().isSuccessful()
        }

        override fun hasConflictingConstraints(): Boolean {
            for (typeBounds in typeParameterBounds.values()) {
                if (typeBounds.getValues().size() > 1) return true
            }
            return false
        }

        override fun hasUnknownParameters(): Boolean {
            for (typeBounds in typeParameterBounds.values()) {
                if (typeBounds.isEmpty()) {
                    return true
                }
            }
            return false
        }

        override fun hasTypeConstructorMismatch() = !errorConstraintPositions.isEmpty()

        override fun hasTypeConstructorMismatchAt(constraintPosition: ConstraintPosition) =
                errorConstraintPositions.contains(constraintPosition)

        override fun hasOnlyErrorsFromPosition(constraintPosition: ConstraintPosition): Boolean {
            if (isSuccessful()) return false
            val systemWithoutConstraintsFromPosition = filterConstraintsOut(constraintPosition)
            if (systemWithoutConstraintsFromPosition.getStatus().isSuccessful()) {
                return true
            }
            if (errorConstraintPositions.size() == 1 && errorConstraintPositions.contains(constraintPosition)) {
                // e.g. if systemWithoutConstraintsFromPosition has unknown type parameters, it's not successful
                return true
            }
            return false
        }

        override fun hasErrorInConstrainingTypes() = hasErrorInConstrainingTypes
    }

    private fun getParameterToInferredValueMap(typeParameterBounds: Map<TypeParameterDescriptor, TypeBoundsImpl>, getDefaultTypeProjection: Function1<TypeParameterDescriptor, TypeProjection>): Map<TypeParameterDescriptor, TypeProjection> {
        val substitutionContext = HashMap<TypeParameterDescriptor, TypeProjection>()
        for ((typeParameter, typeBounds) in typeParameterBounds) {
            val typeProjection: TypeProjection
            val value = typeBounds.getValue()
            if (value != null && !TypeUtils.containsSpecialType(value, DONT_CARE)) {
                typeProjection = TypeProjectionImpl(value)
            }
            else {
                typeProjection = getDefaultTypeProjection.invoke(typeParameter)
            }
            substitutionContext.put(typeParameter, typeProjection)
        }
        return substitutionContext
    }

    private fun replaceUninferredBy(getDefaultValue: (TypeParameterDescriptor) -> TypeProjection): TypeSubstitutor {
        return TypeUtils.makeSubstitutorForTypeParametersMap(getParameterToInferredValueMap(typeParameterBounds, getDefaultValue))
    }

    private fun replaceUninferredBy(defaultValue: JetType): TypeSubstitutor {
        return replaceUninferredBy { TypeProjectionImpl(defaultValue) }
    }

    private fun replaceUninferredBySpecialErrorType(): TypeSubstitutor {
        return replaceUninferredBy { TypeProjectionImpl(ErrorUtils.createUninferredParameterType(it)) }
    }

    override fun getStatus(): ConstraintSystemStatus = constraintSystemStatus

    override fun registerTypeVariables(typeVariables: Map<TypeParameterDescriptor, Variance>) {
        for ((typeVariable, positionVariance) in typeVariables) {
            typeParameterBounds.put(typeVariable, TypeBoundsImpl(typeVariable, positionVariance))
        }
        val constantSubstitutor = TypeUtils.makeConstantSubstitutor(typeParameterBounds.keySet(), DONT_CARE)
        for ((typeVariable, typeBounds) in typeParameterBounds) {
            for (declaredUpperBound in typeVariable.getUpperBounds()) {
                if (KotlinBuiltIns.getInstance().getNullableAnyType() == declaredUpperBound) continue //todo remove this line (?)
                val substitutedBound = constantSubstitutor?.substitute(declaredUpperBound, Variance.INVARIANT)
                if (substitutedBound != null) {
                    typeBounds.addBound(UPPER_BOUND, substitutedBound, TYPE_BOUND_POSITION.position(typeVariable.getIndex()))
                }
            }
        }
    }

    public fun copy(): ConstraintSystem = createNewConstraintSystemFromThis({ it }, { it.copy() }, { true })

    public fun substituteTypeVariables(typeVariablesMap: (TypeParameterDescriptor) -> TypeParameterDescriptor?): ConstraintSystem {
        // type bounds are proper types and don't contain other variables
        return createNewConstraintSystemFromThis(typeVariablesMap, { it }, { true })
    }

    public fun filterConstraintsOut(vararg excludePositions: ConstraintPosition): ConstraintSystem {
        val positions = excludePositions.toSet()
        return filterConstraints { !positions.contains(it) }
    }

    public fun filterConstraints(condition: (ConstraintPosition) -> Boolean): ConstraintSystem {
        return createNewConstraintSystemFromThis({ it }, { it.filter(condition) }, condition)
    }

    public fun getSystemWithoutWeakConstraints(): ConstraintSystem {
        return filterConstraints {
            constraintPosition ->
            // 'isStrong' for compound means 'has some strong constraints'
            // but for testing absence of weak constraints we need 'has only strong constraints' here
            if (constraintPosition is CompoundConstraintPosition) {
                constraintPosition.positions.all { it.isStrong() }
            }
            else {
                constraintPosition.isStrong()
            }
        }
    }

    private fun createNewConstraintSystemFromThis(
            substituteTypeVariable: (TypeParameterDescriptor) -> TypeParameterDescriptor?,
            replaceTypeBounds: (TypeBoundsImpl) -> TypeBoundsImpl,
            filterConstraintPosition: (ConstraintPosition) -> Boolean
    ): ConstraintSystem {
        val newSystem = ConstraintSystemImpl()
        for ((typeParameter, typeBounds) in typeParameterBounds) {
            val newTypeParameter = substituteTypeVariable(typeParameter)
            newSystem.typeParameterBounds.put(newTypeParameter!!, replaceTypeBounds(typeBounds))
        }
        newSystem.errorConstraintPositions.addAll(errorConstraintPositions.filter(filterConstraintPosition))
        //todo if 'filterConstraintPosition' is not trivial, it's incorrect to just copy 'hasErrorInConstrainingTypes'
        newSystem.hasErrorInConstrainingTypes = hasErrorInConstrainingTypes
        return newSystem
    }

    override fun addSupertypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition) {
        if (constrainingType != null && TypeUtils.noExpectedType(constrainingType)) return

        addConstraint(SUB_TYPE, subjectType, constrainingType, constraintPosition)
    }

    override fun addSubtypeConstraint(constrainingType: JetType?, subjectType: JetType, constraintPosition: ConstraintPosition) {
        addConstraint(SUB_TYPE, constrainingType, subjectType, constraintPosition)
    }

    private fun addConstraint(constraintKind: ConstraintKind, subType: JetType?, superType: JetType?, constraintPosition: ConstraintPosition) {
        val typeCheckingProcedure = TypeCheckingProcedure(object : TypingConstraints {
            override fun assertEqualTypes(a: JetType, b: JetType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                doAddConstraint(EQUAL, a, b, constraintPosition, typeCheckingProcedure)
                return true

            }

            override fun assertEqualTypeConstructors(a: TypeConstructor, b: TypeConstructor): Boolean {
                return a == b
            }

            override fun assertSubtype(subtype: JetType, supertype: JetType, typeCheckingProcedure: TypeCheckingProcedure): Boolean {
                doAddConstraint(SUB_TYPE, subtype, supertype, constraintPosition, typeCheckingProcedure)
                return true
            }

            override fun noCorrespondingSupertype(subtype: JetType, supertype: JetType): Boolean {
                errorConstraintPositions.add(constraintPosition)
                return true
            }
        })
        doAddConstraint(constraintKind, subType, superType, constraintPosition, typeCheckingProcedure)
    }

    private fun isErrorOrSpecialType(jetType: JetType?): Boolean {
        if (jetType == DONT_CARE || ErrorUtils.isUninferredParameter(jetType)) {
            return true
        }

        if (jetType == null || (jetType.isError() && jetType != TypeUtils.PLACEHOLDER_FUNCTION_TYPE)) {
            hasErrorInConstrainingTypes = true
            return true
        }
        return false
    }

    private fun doAddConstraint(
            constraintKind: ConstraintKind,
            subType: JetType?,
            superType: JetType?,
            constraintPosition: ConstraintPosition,
            typeCheckingProcedure: TypeCheckingProcedure
    ) {
        if (isErrorOrSpecialType(subType) || isErrorOrSpecialType(superType)) return
        if (subType == null || superType == null) return

        assert(superType != TypeUtils.PLACEHOLDER_FUNCTION_TYPE) {
            "The type for " + constraintPosition + " shouldn't be a placeholder for function type"
        }

        val kotlinBuiltIns = KotlinBuiltIns.getInstance()
        if (subType == TypeUtils.PLACEHOLDER_FUNCTION_TYPE) {
            if (!kotlinBuiltIns.isFunctionOrExtensionFunctionType(superType)) {
                if (isMyTypeVariable(superType)) {
                    // a constraint binds type parameter and any function type, so there is no new info and no error
                    return
                }
                errorConstraintPositions.add(constraintPosition)
            }
            return
        }

        // todo temporary hack
        // function literal without declaring receiver type { x -> ... }
        // can be considered as extension function if one is expected
        // (special type constructor for function/ extension function should be introduced like PLACEHOLDER_FUNCTION_TYPE)
        val newSubType = if (constraintKind == SUB_TYPE
                && kotlinBuiltIns.isFunctionType(subType)
                && kotlinBuiltIns.isExtensionFunctionType(superType)) {
            createCorrespondingExtensionFunctionType(subType, DONT_CARE)
        }
        else {
            subType : JetType
        }

        fun simplifyConstraint(subType: JetType, superType: JetType) {
            // can be equal for the recursive invocations:
            // fun <T> foo(i: Int) : T { ... return foo(i); } => T <: T
            if (subType == superType) return

            assert(!isMyTypeVariable(subType) || !isMyTypeVariable(superType)) {
                "The constraint shouldn't contain different type variables on both sides: " + subType + " <: " + superType
            }

            if (isMyTypeVariable(subType)) {
                val boundKind = if (constraintKind == SUB_TYPE) UPPER_BOUND else EXACT_BOUND
                generateTypeParameterConstraint(subType, superType, boundKind, constraintPosition)
                return
            }
            if (isMyTypeVariable(superType)) {
                val boundKind = if (constraintKind == SUB_TYPE) LOWER_BOUND else EXACT_BOUND
                generateTypeParameterConstraint(superType, subType, boundKind, constraintPosition)
                return
            }
            // if superType is nullable and subType is not nullable, unsafe call error will be generated later,
            // but constraint system should be solved anyway
            typeCheckingProcedure.isSubtypeOf(TypeUtils.makeNotNullable(subType), TypeUtils.makeNotNullable(superType))
        }
        simplifyConstraint(newSubType, superType)
    }

    private fun generateTypeParameterConstraint(
            parameterType: JetType,
            constrainingType: JetType,
            boundKind: TypeBounds.BoundKind,
            constraintPosition: ConstraintPosition
    ) {
        val typeBounds = getTypeBounds(parameterType)

        if (!parameterType.isNullable() || !constrainingType.isNullable()) {
            typeBounds.addBound(boundKind, constrainingType, constraintPosition)
            return
        }
        // For parameter type T:
        // constraint T? =  Int? should transform to T >: Int and T <: Int?
        // constraint T? >: Int? should transform to T >: Int
        val notNullConstrainingType = TypeUtils.makeNotNullable(constrainingType)
        if (boundKind == EXACT_BOUND || boundKind == LOWER_BOUND) {
            typeBounds.addBound(LOWER_BOUND, notNullConstrainingType, constraintPosition)
        }
        // constraint T? <: Int? should transform to T <: Int?
        if (boundKind == EXACT_BOUND || boundKind == UPPER_BOUND) {
            typeBounds.addBound(UPPER_BOUND, constrainingType, constraintPosition)
        }
    }

    public fun processDeclaredBoundConstraints() {
        for ((typeParameterDescriptor, typeBounds) in typeParameterBounds) {
            for (declaredUpperBound in typeParameterDescriptor.getUpperBounds()) {
                //todo order matters here
                val bounds = ArrayList(typeBounds.bounds)
                for (bound in bounds) {
                    if (bound.kind == LOWER_BOUND || bound.kind == EXACT_BOUND) {
                        val position = getCompoundConstraintPosition(
                                TYPE_BOUND_POSITION.position(typeParameterDescriptor.getIndex()), bound.position)
                        addSubtypeConstraint(bound.constrainingType, declaredUpperBound, position)
                    }
                }
                if (isMyTypeVariable(declaredUpperBound)) {
                    val typeBoundsForUpperBound = getTypeBounds(declaredUpperBound)
                    for (bound in typeBoundsForUpperBound.bounds) {
                        if (bound.kind == UPPER_BOUND || bound.kind == EXACT_BOUND) {
                            val position = getCompoundConstraintPosition(
                                    TYPE_BOUND_POSITION.position(typeParameterDescriptor.getIndex()), bound.position)
                            typeBounds.addBound(UPPER_BOUND, bound.constrainingType, position)
                        }
                    }
                }
            }
        }
    }

    override fun getTypeVariables() = typeParameterBounds.keySet()

    override fun getTypeBounds(typeVariable: TypeParameterDescriptor): TypeBoundsImpl {
        if (!isMyTypeVariable(typeVariable)) {
            throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $typeVariable")
        }
        return typeParameterBounds[typeVariable]!!
    }

    private fun getTypeBounds(parameterType: JetType): TypeBoundsImpl {
        assert (isMyTypeVariable(parameterType)) { "Type is not a type variable for constraint system: $parameterType" }
        return getTypeBounds(getMyTypeVariable(parameterType)!!)
    }

    private fun isMyTypeVariable(typeVariable: TypeParameterDescriptor) = typeParameterBounds.contains(typeVariable)

    private fun isMyTypeVariable(jetType: JetType): Boolean = getMyTypeVariable(jetType) != null

    private fun getMyTypeVariable(jetType: JetType): TypeParameterDescriptor? {
        val typeParameterDescriptor = jetType.getConstructor().getDeclarationDescriptor() as? TypeParameterDescriptor
        return if (typeParameterDescriptor != null && isMyTypeVariable(typeParameterDescriptor)) typeParameterDescriptor else null
    }

    override fun getResultingSubstitutor() = replaceUninferredBySpecialErrorType()

    override fun getCurrentSubstitutor() = replaceUninferredBy(TypeUtils.DONT_CARE)

    private fun createCorrespondingExtensionFunctionType(functionType: JetType, receiverType: JetType): JetType {
        assert(KotlinBuiltIns.getInstance().isFunctionType(functionType))

        val typeArguments = functionType.getArguments()
        assert(!typeArguments.isEmpty())

        val arguments = ArrayList<JetType>()
        // excluding the last type argument of the function type, which is the return type
        var index = 0
        val lastIndex = typeArguments.size() - 1
        for (typeArgument in typeArguments) {
            if (index < lastIndex) {
                arguments.add(typeArgument.getType())
            }
            index++
        }
        val returnType = typeArguments.get(lastIndex).getType()
        return KotlinBuiltIns.getInstance().getFunctionType(functionType.getAnnotations(), receiverType, arguments, returnType)
    }
}