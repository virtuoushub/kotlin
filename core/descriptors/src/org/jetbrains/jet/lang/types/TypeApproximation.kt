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

package org.jetbrains.jet.lang.types.typesApproximation

import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.types.TypeProjection
import org.jetbrains.jet.lang.types.TypeProjectionImpl
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.Variance
import java.util.ArrayList
import org.jetbrains.jet.lang.types.JetTypeImpl
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.resolve.calls.inference.CapturedTypeConstructor
import org.jetbrains.jet.lang.types.TypeSubstitutor
import org.jetbrains.jet.lang.types.TypeSubstitution
import org.jetbrains.jet.lang.types.TypeConstructor
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor

public data class ApproximationBounds<T>(public val lower: T, public val upper: T)

data class EnhancedTypeProjection(
        val typeParameter: TypeParameterDescriptor, //todo????
        val inProjection: JetType,
        val outProjection: JetType
) {
    val isConsistent = JetTypeChecker.DEFAULT.isSubtypeOf(inProjection, outProjection)
}

val nullableAny = KotlinBuiltIns.getInstance().getNullableAnyType()
val nothing = KotlinBuiltIns.getInstance().getNothingType()

fun EnhancedTypeProjection.toTypeProjection(): TypeProjection {
    assert(isConsistent) { "Only consistent enhanced type propection can be converted to type projection" }
    //todo rename
    fun simplifyVariance(variance: Variance) = if (variance == typeParameter.getVariance()) Variance.INVARIANT else variance
    return when {

        inProjection == outProjection -> TypeProjectionImpl(inProjection)

        inProjection == nothing -> TypeProjectionImpl(simplifyVariance(Variance.OUT_VARIANCE), outProjection)

        outProjection == nullableAny -> TypeProjectionImpl(simplifyVariance(Variance.IN_VARIANCE), inProjection)

        else -> throw AssertionError("$this")//TODO
    }
}

fun TypeProjection.toEnhancedTypeProjection(typeParameter: TypeParameterDescriptor): EnhancedTypeProjection {

    return when (TypeSubstitutor.combine(typeParameter.getVariance(), getProjectionKind())) {

        Variance.INVARIANT -> EnhancedTypeProjection(typeParameter, getType(), getType())

        Variance.IN_VARIANCE -> EnhancedTypeProjection(typeParameter, getType(), nullableAny)

        Variance.OUT_VARIANCE -> EnhancedTypeProjection(typeParameter, nothing, getType())
    }
}

public fun approximateIfNecessary(jetType: JetType): ApproximationBounds<JetType> {
    if (!TypeUtils.containsSpecialType(jetType) { it.getConstructor() is CapturedTypeConstructor }) {
        return ApproximationBounds(jetType, jetType)
    }
    return approximate(jetType)
}

public fun substituteCapturedTypes(typeProjection: TypeProjection): TypeProjection? {
    val typeSubstitutor = TypeSubstitutor.create(object : TypeSubstitution {
        override fun get(typeConstructor: TypeConstructor?): TypeProjection? {
            return (typeConstructor as? CapturedTypeConstructor)?.typeProjection
        }
        override fun isEmpty() = false
    })
    return typeSubstitutor.substituteWithoutApproximation(typeProjection)
}

public fun approximate(jetType: JetType): ApproximationBounds<JetType> {
    val typeConstructor = jetType.getConstructor()
    if (typeConstructor is CapturedTypeConstructor) {
        val typeProjection = typeConstructor.typeProjection
        return when (typeProjection.getProjectionKind()) {
            //todo code duplication
            Variance.IN_VARIANCE -> ApproximationBounds(typeProjection.getType(), nullableAny)
            Variance.OUT_VARIANCE -> ApproximationBounds(nothing, typeProjection.getType())
            else -> throw AssertionError("$jetType")//todo
        }
    }
    if (jetType.getArguments().isEmpty()) {
        return ApproximationBounds(jetType, jetType)
    }
    val lowerBoundArguments = ArrayList<EnhancedTypeProjection>()
    val upperBoundArguments = ArrayList<EnhancedTypeProjection>()
    for ((typeProjection, typeParameter) in jetType.getArguments().zip(typeConstructor.getParameters())) {
        val (lower, upper) = approximateProjection(typeProjection.toEnhancedTypeProjection(typeParameter))
        lowerBoundArguments.add(lower)
        upperBoundArguments.add(upper)
    }
    val lowerBoundIsTrivial = lowerBoundArguments.any { !it.isConsistent }
    return ApproximationBounds(
            if (lowerBoundIsTrivial) KotlinBuiltIns.getInstance().getNothingType() else jetType.replaceTypeArguments(lowerBoundArguments),
            jetType.replaceTypeArguments(upperBoundArguments))
}

fun JetType.replaceTypeArguments(newTypeArguments: List<EnhancedTypeProjection>): JetType {
    assert(getArguments().size() == newTypeArguments.size()) { "Incorrect type arguments $newTypeArguments" }
    return JetTypeImpl(getAnnotations(), getConstructor(), isNullable(), newTypeArguments.map { it.toTypeProjection() }, getMemberScope())
}

fun approximateProjection(enhancedTypeProjection: EnhancedTypeProjection): ApproximationBounds<EnhancedTypeProjection> {
    val (inLower, inUpper) = approximate(enhancedTypeProjection.inProjection)
    val (outLower, outUpper) = approximate(enhancedTypeProjection.outProjection)
    return ApproximationBounds(
            lower = EnhancedTypeProjection(enhancedTypeProjection.typeParameter, inUpper, outLower),
            upper = EnhancedTypeProjection(enhancedTypeProjection.typeParameter, inLower, outUpper))
}