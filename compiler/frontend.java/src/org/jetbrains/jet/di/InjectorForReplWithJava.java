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

package org.jetbrains.jet.di;

import com.intellij.openapi.project.Project;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.storage.StorageManager;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.ResolveSession;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.jet.lang.resolve.LazyTopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializationComponentsForJava;
import org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinder;
import org.jetbrains.jet.lang.resolve.java.JavaClassFinderImpl;
import org.jetbrains.jet.lang.resolve.java.resolver.TraceBasedExternalSignatureResolver;
import org.jetbrains.jet.lang.resolve.java.resolver.LazyResolveBasedCache;
import org.jetbrains.jet.lang.resolve.java.resolver.TraceBasedErrorReporter;
import org.jetbrains.jet.lang.resolve.java.resolver.PsiBasedMethodSignatureChecker;
import org.jetbrains.jet.lang.resolve.java.resolver.PsiBasedExternalAnnotationResolver;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaPropertyInitializerEvaluatorImpl;
import org.jetbrains.jet.lang.resolve.java.sam.SamConversionResolverImpl;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaSourceElementFactoryImpl;
import org.jetbrains.jet.lang.resolve.MutablePackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.lazy.SingleModuleClassResolver;
import org.jetbrains.jet.lang.resolve.java.JavaLazyAnalyzerPostConstruct;
import org.jetbrains.jet.lang.resolve.java.JavaFlexibleTypeCapabilitiesProvider;
import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider.AdditionalFileScopeProvider;
import org.jetbrains.jet.lang.resolve.AnnotationResolver;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.ArgumentTypeResolver;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingComponents;
import org.jetbrains.jet.lang.types.expressions.ControlStructureTypingUtils;
import org.jetbrains.jet.lang.types.DynamicTypesSettings;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.lang.types.expressions.ForLoopConventionsChecker;
import org.jetbrains.jet.lang.reflect.ReflectionTypes;
import org.jetbrains.jet.lang.resolve.calls.CallExpressionResolver;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.DelegatedPropertyResolver;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.QualifiedExpressionResolver;
import org.jetbrains.jet.context.LazinessToken;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtensionProvider;
import org.jetbrains.jet.lang.resolve.PartialBodyResolveProvider;
import org.jetbrains.jet.lang.resolve.calls.CallCompleter;
import org.jetbrains.jet.lang.resolve.calls.CandidateResolver;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.psi.JetImportsFactory;
import org.jetbrains.jet.lang.resolve.lazy.ScopeProvider;
import org.jetbrains.jet.lang.resolve.ScriptBodyResolver;
import org.jetbrains.jet.lang.resolve.BodyResolver;
import org.jetbrains.jet.lang.resolve.ControlFlowAnalyzer;
import org.jetbrains.jet.lang.resolve.DeclarationsChecker;
import org.jetbrains.jet.lang.resolve.ModifiersChecker;
import org.jetbrains.jet.lang.resolve.FunctionAnalyzerExtension;
import org.jetbrains.jet.lang.resolve.DeclarationResolver;
import org.jetbrains.jet.lang.resolve.ImportsResolver;
import org.jetbrains.jet.lang.resolve.OverloadResolver;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lang.resolve.TopDownAnalyzer;
import org.jetbrains.jet.lang.resolve.TypeHierarchyResolver;
import org.jetbrains.jet.lang.resolve.ScriptHeaderResolver;
import org.jetbrains.jet.lang.resolve.java.lazy.LazyJavaPackageFragmentProvider;
import org.jetbrains.jet.lang.resolve.java.lazy.GlobalJavaResolverContext;
import org.jetbrains.jet.lang.resolve.kotlin.DeserializedDescriptorResolver;
import org.jetbrains.jet.lang.resolve.kotlin.JavaClassDataFinder;
import org.jetbrains.jet.lang.resolve.kotlin.BinaryClassAnnotationAndConstantLoaderImpl;
import org.jetbrains.annotations.NotNull;
import javax.annotation.PreDestroy;

/* This file is generated by org.jetbrains.jet.generators.injectors.InjectorsPackage. DO NOT EDIT! */
@SuppressWarnings("all")
public class InjectorForReplWithJava {

    private final Project project;
    private final GlobalContext globalContext;
    private final StorageManager storageManager;
    private final BindingTrace bindingTrace;
    private final ModuleDescriptorImpl module;
    private final PlatformToKotlinClassMap platformToKotlinClassMap;
    private final DeclarationProviderFactory declarationProviderFactory;
    private final ResolveSession resolveSession;
    private final GlobalSearchScope moduleContentScope;
    private final LazyTopDownAnalyzer lazyTopDownAnalyzer;
    private final JavaDescriptorResolver javaDescriptorResolver;
    private final DeserializationComponentsForJava deserializationComponentsForJava;
    private final VirtualFileFinder virtualFileFinder;
    private final JavaClassFinderImpl javaClassFinder;
    private final TraceBasedExternalSignatureResolver traceBasedExternalSignatureResolver;
    private final LazyResolveBasedCache lazyResolveBasedCache;
    private final TraceBasedErrorReporter traceBasedErrorReporter;
    private final PsiBasedMethodSignatureChecker psiBasedMethodSignatureChecker;
    private final PsiBasedExternalAnnotationResolver psiBasedExternalAnnotationResolver;
    private final JavaPropertyInitializerEvaluatorImpl javaPropertyInitializerEvaluator;
    private final SamConversionResolverImpl samConversionResolver;
    private final JavaSourceElementFactoryImpl javaSourceElementFactory;
    private final MutablePackageFragmentProvider mutablePackageFragmentProvider;
    private final SingleModuleClassResolver singleModuleClassResolver;
    private final JavaLazyAnalyzerPostConstruct javaLazyAnalyzerPostConstruct;
    private final JavaFlexibleTypeCapabilitiesProvider javaFlexibleTypeCapabilitiesProvider;
    private final AdditionalCheckerProvider additionalCheckerProvider;
    private final AdditionalFileScopeProvider additionalFileScopeProvider;
    private final AnnotationResolver annotationResolver;
    private final CallResolver callResolver;
    private final ArgumentTypeResolver argumentTypeResolver;
    private final ExpressionTypingServices expressionTypingServices;
    private final ExpressionTypingComponents expressionTypingComponents;
    private final ControlStructureTypingUtils controlStructureTypingUtils;
    private final DynamicTypesSettings dynamicTypesSettings;
    private final ExpressionTypingUtils expressionTypingUtils;
    private final ForLoopConventionsChecker forLoopConventionsChecker;
    private final ReflectionTypes reflectionTypes;
    private final CallExpressionResolver callExpressionResolver;
    private final DescriptorResolver descriptorResolver;
    private final DelegatedPropertyResolver delegatedPropertyResolver;
    private final TypeResolver typeResolver;
    private final QualifiedExpressionResolver qualifiedExpressionResolver;
    private final LazinessToken lazinessToken;
    private final CallResolverExtensionProvider callResolverExtensionProvider;
    private final PartialBodyResolveProvider partialBodyResolveProvider;
    private final CallCompleter callCompleter;
    private final CandidateResolver candidateResolver;
    private final TaskPrioritizer taskPrioritizer;
    private final JetImportsFactory jetImportsFactory;
    private final ScopeProvider scopeProvider;
    private final ScriptBodyResolver scriptBodyResolver;
    private final BodyResolver bodyResolver;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final DeclarationsChecker declarationsChecker;
    private final ModifiersChecker modifiersChecker;
    private final FunctionAnalyzerExtension functionAnalyzerExtension;
    private final DeclarationResolver declarationResolver;
    private final ImportsResolver importsResolver;
    private final OverloadResolver overloadResolver;
    private final OverrideResolver overrideResolver;
    private final TopDownAnalyzer topDownAnalyzer;
    private final TypeHierarchyResolver typeHierarchyResolver;
    private final ScriptHeaderResolver scriptHeaderResolver;
    private final LazyJavaPackageFragmentProvider lazyJavaPackageFragmentProvider;
    private final GlobalJavaResolverContext globalJavaResolverContext;
    private final DeserializedDescriptorResolver deserializedDescriptorResolver;
    private final JavaClassDataFinder javaClassDataFinder;
    private final BinaryClassAnnotationAndConstantLoaderImpl binaryClassAnnotationAndConstantLoader;

    public InjectorForReplWithJava(
        @NotNull Project project,
        @NotNull GlobalContext globalContext,
        @NotNull BindingTrace bindingTrace,
        @NotNull ModuleDescriptorImpl module,
        @NotNull DeclarationProviderFactory declarationProviderFactory,
        @NotNull GlobalSearchScope moduleContentScope,
        @NotNull AdditionalFileScopeProvider additionalFileScopeProvider
    ) {
        this.project = project;
        this.globalContext = globalContext;
        this.storageManager = globalContext.getStorageManager();
        this.bindingTrace = bindingTrace;
        this.module = module;
        this.platformToKotlinClassMap = module.getPlatformToKotlinClassMap();
        this.declarationProviderFactory = declarationProviderFactory;
        this.resolveSession = new ResolveSession(project, globalContext, module, declarationProviderFactory, bindingTrace);
        this.moduleContentScope = moduleContentScope;
        this.lazyTopDownAnalyzer = new LazyTopDownAnalyzer();
        this.javaClassFinder = new JavaClassFinderImpl();
        this.virtualFileFinder = org.jetbrains.jet.lang.resolve.kotlin.VirtualFileFinderFactory.SERVICE.getInstance(project).create(moduleContentScope);
        this.traceBasedErrorReporter = new TraceBasedErrorReporter();
        this.deserializedDescriptorResolver = new DeserializedDescriptorResolver(traceBasedErrorReporter);
        this.psiBasedExternalAnnotationResolver = new PsiBasedExternalAnnotationResolver();
        this.traceBasedExternalSignatureResolver = new TraceBasedExternalSignatureResolver();
        this.psiBasedMethodSignatureChecker = new PsiBasedMethodSignatureChecker();
        this.lazyResolveBasedCache = new LazyResolveBasedCache();
        this.javaPropertyInitializerEvaluator = new JavaPropertyInitializerEvaluatorImpl();
        this.samConversionResolver = SamConversionResolverImpl.INSTANCE$;
        this.javaSourceElementFactory = new JavaSourceElementFactoryImpl();
        this.singleModuleClassResolver = new SingleModuleClassResolver();
        this.globalJavaResolverContext = new GlobalJavaResolverContext(storageManager, javaClassFinder, virtualFileFinder, deserializedDescriptorResolver, psiBasedExternalAnnotationResolver, traceBasedExternalSignatureResolver, traceBasedErrorReporter, psiBasedMethodSignatureChecker, lazyResolveBasedCache, javaPropertyInitializerEvaluator, samConversionResolver, javaSourceElementFactory, singleModuleClassResolver);
        this.lazyJavaPackageFragmentProvider = new LazyJavaPackageFragmentProvider(globalJavaResolverContext, module);
        this.javaDescriptorResolver = new JavaDescriptorResolver(lazyJavaPackageFragmentProvider, module);
        this.javaClassDataFinder = new JavaClassDataFinder(virtualFileFinder, deserializedDescriptorResolver);
        this.binaryClassAnnotationAndConstantLoader = new BinaryClassAnnotationAndConstantLoaderImpl(module, storageManager, virtualFileFinder, traceBasedErrorReporter);
        this.deserializationComponentsForJava = new DeserializationComponentsForJava(storageManager, module, javaClassDataFinder, binaryClassAnnotationAndConstantLoader, lazyJavaPackageFragmentProvider);
        this.mutablePackageFragmentProvider = new MutablePackageFragmentProvider(module);
        this.javaLazyAnalyzerPostConstruct = new JavaLazyAnalyzerPostConstruct();
        this.javaFlexibleTypeCapabilitiesProvider = new JavaFlexibleTypeCapabilitiesProvider();
        this.additionalCheckerProvider = org.jetbrains.jet.lang.resolve.kotlin.JavaDeclarationCheckerProvider.INSTANCE$;
        this.additionalFileScopeProvider = additionalFileScopeProvider;
        this.annotationResolver = new AnnotationResolver();
        this.callResolver = new CallResolver();
        this.argumentTypeResolver = new ArgumentTypeResolver();
        this.expressionTypingComponents = new ExpressionTypingComponents();
        this.expressionTypingServices = new ExpressionTypingServices(expressionTypingComponents);
        this.controlStructureTypingUtils = new ControlStructureTypingUtils(expressionTypingServices);
        this.dynamicTypesSettings = new DynamicTypesSettings();
        this.expressionTypingUtils = new ExpressionTypingUtils(expressionTypingServices, callResolver);
        this.forLoopConventionsChecker = new ForLoopConventionsChecker();
        this.reflectionTypes = new ReflectionTypes(module);
        this.callExpressionResolver = new CallExpressionResolver();
        this.descriptorResolver = new DescriptorResolver();
        this.delegatedPropertyResolver = new DelegatedPropertyResolver();
        this.qualifiedExpressionResolver = new QualifiedExpressionResolver();
        this.lazinessToken = new LazinessToken();
        this.typeResolver = new TypeResolver(annotationResolver, qualifiedExpressionResolver, module, javaFlexibleTypeCapabilitiesProvider, storageManager, lazinessToken, dynamicTypesSettings);
        this.callResolverExtensionProvider = new CallResolverExtensionProvider();
        this.partialBodyResolveProvider = new PartialBodyResolveProvider();
        this.candidateResolver = new CandidateResolver();
        this.callCompleter = new CallCompleter(argumentTypeResolver, candidateResolver);
        this.taskPrioritizer = new TaskPrioritizer(storageManager);
        this.jetImportsFactory = new JetImportsFactory();
        this.scopeProvider = new ScopeProvider(getResolveSession());
        this.scriptBodyResolver = new ScriptBodyResolver();
        this.bodyResolver = new BodyResolver();
        this.controlFlowAnalyzer = new ControlFlowAnalyzer();
        this.declarationsChecker = new DeclarationsChecker();
        this.modifiersChecker = new ModifiersChecker(bindingTrace, additionalCheckerProvider);
        this.functionAnalyzerExtension = new FunctionAnalyzerExtension();
        this.declarationResolver = new DeclarationResolver();
        this.importsResolver = new ImportsResolver();
        this.overloadResolver = new OverloadResolver();
        this.overrideResolver = new OverrideResolver();
        this.topDownAnalyzer = new TopDownAnalyzer();
        this.typeHierarchyResolver = new TypeHierarchyResolver();
        this.scriptHeaderResolver = new ScriptHeaderResolver();

        this.resolveSession.setAnnotationResolve(annotationResolver);
        this.resolveSession.setDescriptorResolver(descriptorResolver);
        this.resolveSession.setJetImportFactory(jetImportsFactory);
        this.resolveSession.setQualifiedExpressionResolver(qualifiedExpressionResolver);
        this.resolveSession.setScopeProvider(scopeProvider);
        this.resolveSession.setScriptBodyResolver(scriptBodyResolver);
        this.resolveSession.setTypeResolver(typeResolver);

        this.lazyTopDownAnalyzer.setBodyResolver(bodyResolver);
        this.lazyTopDownAnalyzer.setDeclarationResolver(declarationResolver);
        this.lazyTopDownAnalyzer.setKotlinCodeAnalyzer(resolveSession);
        this.lazyTopDownAnalyzer.setModuleDescriptor(module);
        this.lazyTopDownAnalyzer.setOverloadResolver(overloadResolver);
        this.lazyTopDownAnalyzer.setOverrideResolver(overrideResolver);
        this.lazyTopDownAnalyzer.setTopDownAnalyzer(topDownAnalyzer);
        this.lazyTopDownAnalyzer.setTrace(bindingTrace);

        javaClassFinder.setComponentPostConstruct(javaLazyAnalyzerPostConstruct);
        javaClassFinder.setProject(project);
        javaClassFinder.setScope(moduleContentScope);

        traceBasedExternalSignatureResolver.setExternalAnnotationResolver(psiBasedExternalAnnotationResolver);
        traceBasedExternalSignatureResolver.setProject(project);
        traceBasedExternalSignatureResolver.setTrace(bindingTrace);

        lazyResolveBasedCache.setSession(resolveSession);

        traceBasedErrorReporter.setTrace(bindingTrace);

        psiBasedMethodSignatureChecker.setExternalAnnotationResolver(psiBasedExternalAnnotationResolver);
        psiBasedMethodSignatureChecker.setExternalSignatureResolver(traceBasedExternalSignatureResolver);

        singleModuleClassResolver.setResolver(javaDescriptorResolver);

        javaLazyAnalyzerPostConstruct.setCodeAnalyzer(resolveSession);
        javaLazyAnalyzerPostConstruct.setProject(project);
        javaLazyAnalyzerPostConstruct.setTrace(bindingTrace);

        annotationResolver.setCallResolver(callResolver);
        annotationResolver.setStorageManager(storageManager);
        annotationResolver.setTypeResolver(typeResolver);

        callResolver.setArgumentTypeResolver(argumentTypeResolver);
        callResolver.setCallCompleter(callCompleter);
        callResolver.setCandidateResolver(candidateResolver);
        callResolver.setExpressionTypingServices(expressionTypingServices);
        callResolver.setTaskPrioritizer(taskPrioritizer);
        callResolver.setTypeResolver(typeResolver);

        argumentTypeResolver.setExpressionTypingServices(expressionTypingServices);
        argumentTypeResolver.setTypeResolver(typeResolver);

        expressionTypingServices.setAnnotationResolver(annotationResolver);
        expressionTypingServices.setCallExpressionResolver(callExpressionResolver);
        expressionTypingServices.setCallResolver(callResolver);
        expressionTypingServices.setDescriptorResolver(descriptorResolver);
        expressionTypingServices.setExtensionProvider(callResolverExtensionProvider);
        expressionTypingServices.setPartialBodyResolveProvider(partialBodyResolveProvider);
        expressionTypingServices.setProject(project);
        expressionTypingServices.setTypeResolver(typeResolver);

        expressionTypingComponents.setAdditionalCheckerProvider(additionalCheckerProvider);
        expressionTypingComponents.setCallResolver(callResolver);
        expressionTypingComponents.setControlStructureTypingUtils(controlStructureTypingUtils);
        expressionTypingComponents.setDynamicTypesSettings(dynamicTypesSettings);
        expressionTypingComponents.setExpressionTypingServices(expressionTypingServices);
        expressionTypingComponents.setExpressionTypingUtils(expressionTypingUtils);
        expressionTypingComponents.setForLoopConventionsChecker(forLoopConventionsChecker);
        expressionTypingComponents.setGlobalContext(globalContext);
        expressionTypingComponents.setPlatformToKotlinClassMap(platformToKotlinClassMap);
        expressionTypingComponents.setReflectionTypes(reflectionTypes);

        forLoopConventionsChecker.setExpressionTypingServices(expressionTypingServices);
        forLoopConventionsChecker.setExpressionTypingUtils(expressionTypingUtils);
        forLoopConventionsChecker.setProject(project);

        callExpressionResolver.setExpressionTypingServices(expressionTypingServices);

        descriptorResolver.setAnnotationResolver(annotationResolver);
        descriptorResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        descriptorResolver.setExpressionTypingServices(expressionTypingServices);
        descriptorResolver.setStorageManager(storageManager);
        descriptorResolver.setTypeResolver(typeResolver);

        delegatedPropertyResolver.setCallResolver(callResolver);
        delegatedPropertyResolver.setExpressionTypingServices(expressionTypingServices);

        candidateResolver.setArgumentTypeResolver(argumentTypeResolver);

        jetImportsFactory.setProject(project);

        scopeProvider.setAdditionalFileScopesProvider(additionalFileScopeProvider);

        scriptBodyResolver.setExpressionTypingServices(expressionTypingServices);

        bodyResolver.setAnnotationResolver(annotationResolver);
        bodyResolver.setCallResolver(callResolver);
        bodyResolver.setControlFlowAnalyzer(controlFlowAnalyzer);
        bodyResolver.setDeclarationsChecker(declarationsChecker);
        bodyResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        bodyResolver.setExpressionTypingServices(expressionTypingServices);
        bodyResolver.setFunctionAnalyzerExtension(functionAnalyzerExtension);
        bodyResolver.setScriptBodyResolverResolver(scriptBodyResolver);
        bodyResolver.setTrace(bindingTrace);

        controlFlowAnalyzer.setTrace(bindingTrace);

        declarationsChecker.setDescriptorResolver(descriptorResolver);
        declarationsChecker.setModifiersChecker(modifiersChecker);
        declarationsChecker.setTrace(bindingTrace);

        functionAnalyzerExtension.setTrace(bindingTrace);

        declarationResolver.setAnnotationResolver(annotationResolver);
        declarationResolver.setDescriptorResolver(descriptorResolver);
        declarationResolver.setImportsResolver(importsResolver);
        declarationResolver.setTrace(bindingTrace);

        importsResolver.setImportsFactory(jetImportsFactory);
        importsResolver.setModuleDescriptor(module);
        importsResolver.setQualifiedExpressionResolver(qualifiedExpressionResolver);
        importsResolver.setTrace(bindingTrace);

        overloadResolver.setTrace(bindingTrace);

        overrideResolver.setTrace(bindingTrace);

        topDownAnalyzer.setBodyResolver(bodyResolver);
        topDownAnalyzer.setDeclarationResolver(declarationResolver);
        topDownAnalyzer.setModuleDescriptor(module);
        topDownAnalyzer.setOverloadResolver(overloadResolver);
        topDownAnalyzer.setOverrideResolver(overrideResolver);
        topDownAnalyzer.setPackageFragmentProvider(mutablePackageFragmentProvider);
        topDownAnalyzer.setTypeHierarchyResolver(typeHierarchyResolver);

        typeHierarchyResolver.setDescriptorResolver(descriptorResolver);
        typeHierarchyResolver.setImportsResolver(importsResolver);
        typeHierarchyResolver.setPackageFragmentProvider(mutablePackageFragmentProvider);
        typeHierarchyResolver.setScriptHeaderResolver(scriptHeaderResolver);
        typeHierarchyResolver.setTrace(bindingTrace);

        scriptHeaderResolver.setPackageFragmentProvider(mutablePackageFragmentProvider);
        scriptHeaderResolver.setTrace(bindingTrace);

        deserializedDescriptorResolver.setComponents(deserializationComponentsForJava);

        javaClassFinder.initialize();

        javaLazyAnalyzerPostConstruct.postCreate();

    }

    @PreDestroy
    public void destroy() {
    }

    public ResolveSession getResolveSession() {
        return this.resolveSession;
    }

    public LazyTopDownAnalyzer getLazyTopDownAnalyzer() {
        return this.lazyTopDownAnalyzer;
    }

    public JavaDescriptorResolver getJavaDescriptorResolver() {
        return this.javaDescriptorResolver;
    }

    public DeserializationComponentsForJava getDeserializationComponentsForJava() {
        return this.deserializationComponentsForJava;
    }

}
