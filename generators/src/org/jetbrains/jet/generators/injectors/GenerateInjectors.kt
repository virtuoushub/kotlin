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

package org.jetbrains.jet.generators.injectors

import com.intellij.openapi.project.Project
import org.jetbrains.jet.context.GlobalContext
import org.jetbrains.jet.context.GlobalContextImpl
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.resolve.java.*
import org.jetbrains.jet.lang.resolve.java.resolver.*
import org.jetbrains.jet.lang.resolve.java.sam.SamConversionResolverImpl
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices
import org.jetbrains.jet.di.*
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingComponents
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils
import org.jetbrains.jet.lang.resolve.calls.CallResolver
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaPropertyInitializerEvaluatorImpl
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.jet.lang.resolve.java.lazy.ModuleClassResolver
import org.jetbrains.jet.lang.resolve.kotlin.DeserializationComponentsForJava
import org.jetbrains.jet.lang.resolve.java.lazy.SingleModuleClassResolver
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinderFactory
import org.jetbrains.jet.lang.resolve.kotlin.JavaDeclarationCheckerProvider
import org.jetbrains.jet.lang.resolve.lazy.KotlinCodeAnalyzer
import org.jetbrains.jet.context.LazyResolveToken

// NOTE: After making changes, you need to re-generate the injectors.
//       To do that, you can run main in this file.
public fun main(args: Array<String>) {
    for (generator in createInjectorGenerators()) {
        try {
            generator.generate()
        }
        catch (e: Throwable) {
            System.err.println(generator.getOutputFile())
            throw e
        }
    }
}

public fun createInjectorGenerators(): List<DependencyInjectorGenerator> =
        listOf(
                generatorForTopDownAnalyzerBasic(),
                generatorForTopDownAnalyzerForJvm(),
                generatorForJavaDescriptorResolver(),
                generatorForLazyResolveWithJava(),
                generatorForTopDownAnalyzerForJs(),
                generatorForMacro(),
                generatorForTests(),
                generatorForLazyResolve(),
                generatorForBodyResolve(),
                generatorForLazyBodyResolve()
        )

private fun DependencyInjectorGenerator.commonForTopDownAnalyzer() {
    parameter<Project>()
    parameter<GlobalContext>(useAsContext = true)
    parameter<BindingTrace>()
    publicParameter<ModuleDescriptor>(useAsContext = true)

    publicFields(
            javaClass<TopDownAnalyzer>(),
            javaClass<LazyTopDownAnalyzer>()
    )

    field<MutablePackageFragmentProvider>()
}

private fun generatorForTopDownAnalyzerBasic() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerBasic") {
            commonForTopDownAnalyzer()
            parameter<AdditionalCheckerProvider>()
        }

private fun generatorForLazyBodyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyBodyResolve") {
            parameter(javaClass<Project>())
            parameter(javaClass<GlobalContext>(), useAsContext = true)
            parameter(javaClass<KotlinCodeAnalyzer>(), name = "analyzer")
            parameter(javaClass<BindingTrace>())
            parameter(javaClass<AdditionalCheckerProvider>())

            field(javaClass<ModuleDescriptor>(), init = GivenExpression("analyzer.getModuleDescriptor()"), useAsContext = true)

            publicFields(
                    javaClass<LazyTopDownAnalyzer>()
            )
        }

private fun generatorForTopDownAnalyzerForJs() =
        generator("js/js.frontend/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJs") {
            commonForTopDownAnalyzer()

            field<AdditionalCheckerProvider>(
                  init = GivenExpression(javaClass<AdditionalCheckerProvider.Empty>().getCanonicalName() + ".INSTANCE$"))
        }

private fun generatorForTopDownAnalyzerForJvm() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForTopDownAnalyzerForJvm") {
            commonForTopDownAnalyzer()

            publicField<JavaDescriptorResolver>()
            publicField<DeserializationComponentsForJava>()

            field<AdditionalCheckerProvider>(
                  init = GivenExpression(javaClass<JavaDeclarationCheckerProvider>().getName() + ".INSTANCE$"))

            field<GlobalSearchScope>(
                  init = GivenExpression(javaClass<GlobalSearchScope>().getName() + ".allScope(project)")
            )
            fields(
                    javaClass<JavaClassFinderImpl>(),
                    javaClass<TraceBasedExternalSignatureResolver>(),
                    javaClass<TraceBasedJavaResolverCache>(),
                    javaClass<TraceBasedErrorReporter>(),
                    javaClass<PsiBasedMethodSignatureChecker>(),
                    javaClass<PsiBasedExternalAnnotationResolver>(),
                    javaClass<MutablePackageFragmentProvider>(),
                    javaClass<JavaPropertyInitializerEvaluatorImpl>(),
                    javaClass<SamConversionResolverImpl>(),
                    javaClass<JavaSourceElementFactoryImpl>(),
                    javaClass<SingleModuleClassResolver>(),
                    javaClass<JavaFlexibleTypeCapabilitiesProvider>()
            )
            field<VirtualFileFinder>(init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
        }

private fun generatorForJavaDescriptorResolver() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForJavaDescriptorResolver") {
            parameters(
                    javaClass<Project>(),
                    javaClass<BindingTrace>()
            )

            publicField<GlobalContextImpl>(useAsContext = true,
                        init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"))
            publicField<ModuleDescriptorImpl>(name = "module",
                        init = GivenExpression(javaClass<TopDownAnalyzerFacadeForJVM>().getName() + ".createJavaModule(\"<fake-jdr-module>\")"))
            publicField<JavaDescriptorResolver>()
            publicField<JavaClassFinderImpl>()

            field<GlobalSearchScope>(
                  init = GivenExpression(javaClass<GlobalSearchScope>().getName() + ".allScope(project)"))

            fields(
                    javaClass<TraceBasedExternalSignatureResolver>(),
                    javaClass<TraceBasedJavaResolverCache>(),
                    javaClass<TraceBasedErrorReporter>(),
                    javaClass<PsiBasedMethodSignatureChecker>(),
                    javaClass<PsiBasedExternalAnnotationResolver>(),
                    javaClass<JavaPropertyInitializerEvaluatorImpl>(),
                    javaClass<SamConversionResolverImpl>(),
                    javaClass<JavaSourceElementFactoryImpl>(),
                    javaClass<SingleModuleClassResolver>()
            )
            field<VirtualFileFinder>(
                  init = GivenExpression(javaClass<VirtualFileFinder>().getName() + ".SERVICE.getInstance(project)"))
        }

private fun generatorForLazyResolveWithJava() =
        generator("compiler/frontend.java/src", "org.jetbrains.jet.di", "InjectorForLazyResolveWithJava") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<ModuleDescriptorImpl>(name = "module", useAsContext = true)
            parameter<GlobalSearchScope>(name = "moduleContentScope")
            parameters(
                    javaClass<BindingTrace>(),
                    javaClass<DeclarationProviderFactory>(),
                    javaClass<ModuleClassResolver>()
            )

            publicFields(
                    javaClass<ResolveSession>(),
                    javaClass<JavaDescriptorResolver>()
            )

            field<VirtualFileFinder>(
                  init = GivenExpression(javaClass<VirtualFileFinderFactory>().getName()
                                         + ".SERVICE.getInstance(project).create(moduleContentScope)")
            )
            fields(
                    javaClass<JavaClassFinderImpl>(),
                    javaClass<TraceBasedExternalSignatureResolver>(),
                    javaClass<LazyResolveBasedCache>(),
                    javaClass<TraceBasedErrorReporter>(),
                    javaClass<PsiBasedMethodSignatureChecker>(),
                    javaClass<PsiBasedExternalAnnotationResolver>(),
                    javaClass<JavaPropertyInitializerEvaluatorImpl>(),
                    javaClass<SamConversionResolverImpl>(),
                    javaClass<JavaSourceElementFactoryImpl>(),
                    javaClass<JavaFlexibleTypeCapabilitiesProvider>(),
                    javaClass<LazyResolveToken>()
            )
            field<AdditionalCheckerProvider>(
                  init = GivenExpression(javaClass<JavaDeclarationCheckerProvider>().getName() + ".INSTANCE$"))
        }

private fun generatorForMacro() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForMacros") {
            parameter<Project>()
            parameter<ModuleDescriptor>(useAsContext = true)

            publicField<ExpressionTypingServices>()
            publicField<ExpressionTypingComponents>()
            publicField<CallResolver>()

            field<GlobalContext>(useAsContext = true,
                  init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"))

            field<AdditionalCheckerProvider>(
                  init = GivenExpression(javaClass<AdditionalCheckerProvider.Empty>().getCanonicalName() + ".INSTANCE$"))
        }

private fun generatorForTests() =
        generator("compiler/tests", "org.jetbrains.jet.di", "InjectorForTests") {
            parameter<Project>()
            parameter<ModuleDescriptor>(useAsContext = true)

            publicFields(
                    javaClass<DescriptorResolver>(),
                    javaClass<ExpressionTypingServices>(),
                    javaClass<ExpressionTypingUtils>(),
                    javaClass<TypeResolver>()
            )

            field<GlobalContext>(init = GivenExpression("org.jetbrains.jet.context.ContextPackage.GlobalContext()"),
                  useAsContext = true)

            field<AdditionalCheckerProvider>(
                  init = GivenExpression(javaClass<JavaDeclarationCheckerProvider>().getName() + ".INSTANCE$"))
        }

private fun generatorForBodyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForBodyResolve") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<BindingTrace>()
            parameter<ModuleDescriptor>(useAsContext = true)
            parameter<AdditionalCheckerProvider>()
            parameter<PartialBodyResolveProvider>()

            publicField<BodyResolver>()
        }

private fun generatorForLazyResolve() =
        generator("compiler/frontend/src", "org.jetbrains.jet.di", "InjectorForLazyResolve") {
            parameter<Project>()
            parameter<GlobalContext>(useAsContext = true)
            parameter<ModuleDescriptorImpl>(useAsContext = true)
            parameter<DeclarationProviderFactory>()
            parameter<BindingTrace>()
            parameter<AdditionalCheckerProvider>()

            publicField<ResolveSession>()

            field<LazyResolveToken>()
        }

private fun generator(
        targetSourceRoot: String,
        injectorPackageName: String,
        injectorClassName: String,
        body: DependencyInjectorGenerator.() -> Unit
) = generator(targetSourceRoot, injectorPackageName, injectorClassName, "org.jetbrains.jet.generators.injectors.InjectorsPackage", body)
