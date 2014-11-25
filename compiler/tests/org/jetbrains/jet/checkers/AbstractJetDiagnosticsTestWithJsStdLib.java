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

package org.jetbrains.jet.checkers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.cli.jvm.compiler.CliLightClassGenerationSupport;
import org.jetbrains.jet.context.GlobalContext;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.k2js.analyze.TopDownAnalyzerFacadeForJS;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.config.LibrarySourcesConfig;
import org.jetbrains.k2js.config.LibrarySourcesConfigWithCaching;

import java.util.List;

public abstract class AbstractJetDiagnosticsTestWithJsStdLib extends AbstractJetDiagnosticsTest {

    private LibrarySourcesConfigWithCaching config;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        config = new LibrarySourcesConfigWithCaching(getProject(), "module", EcmaVersion.defaultVersion(), false, true, false);
    }

    @Override
    protected void tearDown() throws Exception {
        config = null;
        super.tearDown();
    }

    @Override
    protected void analyzeModuleContents(
            GlobalContext context,
            List<JetFile> jetFiles,
            ModuleDescriptorImpl module,
            BindingTrace moduleTrace,
            TestModule testModule
    ) {
        BindingContext libraryContext = config.getLibraryContext();
        DelegatingBindingTrace trace = new DelegatingBindingTrace(libraryContext, "trace with preanalyzed library");
        super.analyzeModuleContents(context, jetFiles, module, trace, testModule);
        trace.addAllMyDataTo(moduleTrace);
    }

    @Override
    protected LibrarySourcesConfig getConfigForJsAnalyzer() {
        return config;
    }

    @NotNull
    @Override
    protected ModuleDescriptorImpl createModuleWith(@NotNull CliLightClassGenerationSupport support) {
        //It's JVM specific thing, so for JS we just create and setup module.

        ModuleDescriptorImpl module = TopDownAnalyzerFacadeForJS.createJsModule("<kotlin-js-test-module>");

        module.addDependencyOnModule(module);
        module.addDependencyOnModule(KotlinBuiltIns.getInstance().getBuiltInsModule());

        ModuleDescriptor libraryModule = config.getLibraryModule();
        assert libraryModule instanceof ModuleDescriptorImpl;
        module.addDependencyOnModule((ModuleDescriptorImpl) libraryModule);

        module.seal();

        return module;
    }

    @Override
    protected String getDefaultPlatform() {
        return "js";
    }
}
