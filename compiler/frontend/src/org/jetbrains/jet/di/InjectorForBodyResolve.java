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
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.PlatformToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.AdditionalCheckerProvider;
import org.jetbrains.jet.lang.resolve.PartialBodyResolveProvider;
import org.jetbrains.jet.lang.resolve.BodyResolver;
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
import org.jetbrains.jet.lang.resolve.TypeResolver.FlexibleTypeCapabilitiesProvider;
import org.jetbrains.jet.context.LazinessToken;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtensionProvider;
import org.jetbrains.jet.lang.resolve.calls.CallCompleter;
import org.jetbrains.jet.lang.resolve.calls.CandidateResolver;
import org.jetbrains.jet.lang.resolve.calls.tasks.TaskPrioritizer;
import org.jetbrains.jet.lang.resolve.ControlFlowAnalyzer;
import org.jetbrains.jet.lang.resolve.DeclarationsChecker;
import org.jetbrains.jet.lang.resolve.ModifiersChecker;
import org.jetbrains.jet.lang.resolve.FunctionAnalyzerExtension;
import org.jetbrains.jet.lang.resolve.ScriptBodyResolver;
import org.jetbrains.annotations.NotNull;
import javax.annotation.PreDestroy;

/* This file is generated by org.jetbrains.jet.generators.injectors.InjectorsPackage. DO NOT EDIT! */
@SuppressWarnings("all")
public class InjectorForBodyResolve {

    private final Project project;
    private final GlobalContext globalContext;
    private final StorageManager storageManager;
    private final BindingTrace bindingTrace;
    private final ModuleDescriptor moduleDescriptor;
    private final PlatformToKotlinClassMap platformToKotlinClassMap;
    private final AdditionalCheckerProvider additionalCheckerProvider;
    private final PartialBodyResolveProvider partialBodyResolveProvider;
    private final BodyResolver bodyResolver;
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
    private final FlexibleTypeCapabilitiesProvider flexibleTypeCapabilitiesProvider;
    private final LazinessToken lazinessToken;
    private final CallResolverExtensionProvider callResolverExtensionProvider;
    private final CallCompleter callCompleter;
    private final CandidateResolver candidateResolver;
    private final TaskPrioritizer taskPrioritizer;
    private final ControlFlowAnalyzer controlFlowAnalyzer;
    private final DeclarationsChecker declarationsChecker;
    private final ModifiersChecker modifiersChecker;
    private final FunctionAnalyzerExtension functionAnalyzerExtension;
    private final ScriptBodyResolver scriptBodyResolver;

    public InjectorForBodyResolve(
        @NotNull Project project,
        @NotNull GlobalContext globalContext,
        @NotNull BindingTrace bindingTrace,
        @NotNull ModuleDescriptor moduleDescriptor,
        @NotNull AdditionalCheckerProvider additionalCheckerProvider,
        @NotNull PartialBodyResolveProvider partialBodyResolveProvider
    ) {
        this.project = project;
        this.globalContext = globalContext;
        this.storageManager = globalContext.getStorageManager();
        this.bindingTrace = bindingTrace;
        this.moduleDescriptor = moduleDescriptor;
        this.platformToKotlinClassMap = moduleDescriptor.getPlatformToKotlinClassMap();
        this.additionalCheckerProvider = additionalCheckerProvider;
        this.partialBodyResolveProvider = partialBodyResolveProvider;
        this.bodyResolver = new BodyResolver();
        this.annotationResolver = new AnnotationResolver();
        this.callResolver = new CallResolver();
        this.argumentTypeResolver = new ArgumentTypeResolver();
        this.expressionTypingComponents = new ExpressionTypingComponents();
        this.expressionTypingServices = new ExpressionTypingServices(expressionTypingComponents);
        this.controlStructureTypingUtils = new ControlStructureTypingUtils(expressionTypingServices);
        this.dynamicTypesSettings = new DynamicTypesSettings();
        this.expressionTypingUtils = new ExpressionTypingUtils(expressionTypingServices, callResolver);
        this.forLoopConventionsChecker = new ForLoopConventionsChecker();
        this.reflectionTypes = new ReflectionTypes(moduleDescriptor);
        this.callExpressionResolver = new CallExpressionResolver();
        this.descriptorResolver = new DescriptorResolver();
        this.delegatedPropertyResolver = new DelegatedPropertyResolver();
        this.qualifiedExpressionResolver = new QualifiedExpressionResolver();
        this.flexibleTypeCapabilitiesProvider = new FlexibleTypeCapabilitiesProvider();
        this.lazinessToken = new LazinessToken();
        this.typeResolver = new TypeResolver(annotationResolver, qualifiedExpressionResolver, moduleDescriptor, flexibleTypeCapabilitiesProvider, storageManager, lazinessToken, dynamicTypesSettings);
        this.callResolverExtensionProvider = new CallResolverExtensionProvider();
        this.candidateResolver = new CandidateResolver();
        this.callCompleter = new CallCompleter(argumentTypeResolver, candidateResolver);
        this.taskPrioritizer = new TaskPrioritizer(storageManager);
        this.controlFlowAnalyzer = new ControlFlowAnalyzer();
        this.declarationsChecker = new DeclarationsChecker();
        this.modifiersChecker = new ModifiersChecker(bindingTrace, additionalCheckerProvider);
        this.functionAnalyzerExtension = new FunctionAnalyzerExtension();
        this.scriptBodyResolver = new ScriptBodyResolver();

        this.bodyResolver.setAnnotationResolver(annotationResolver);
        this.bodyResolver.setCallResolver(callResolver);
        this.bodyResolver.setControlFlowAnalyzer(controlFlowAnalyzer);
        this.bodyResolver.setDeclarationsChecker(declarationsChecker);
        this.bodyResolver.setDelegatedPropertyResolver(delegatedPropertyResolver);
        this.bodyResolver.setExpressionTypingServices(expressionTypingServices);
        this.bodyResolver.setFunctionAnalyzerExtension(functionAnalyzerExtension);
        this.bodyResolver.setScriptBodyResolverResolver(scriptBodyResolver);
        this.bodyResolver.setTrace(bindingTrace);

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

        controlFlowAnalyzer.setTrace(bindingTrace);

        declarationsChecker.setDescriptorResolver(descriptorResolver);
        declarationsChecker.setModifiersChecker(modifiersChecker);
        declarationsChecker.setTrace(bindingTrace);

        functionAnalyzerExtension.setTrace(bindingTrace);

        scriptBodyResolver.setExpressionTypingServices(expressionTypingServices);

    }

    @PreDestroy
    public void destroy() {
    }

    public BodyResolver getBodyResolver() {
        return this.bodyResolver;
    }

}
