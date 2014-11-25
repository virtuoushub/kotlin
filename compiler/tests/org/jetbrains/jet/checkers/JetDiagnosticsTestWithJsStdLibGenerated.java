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

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.jet.JUnit3RunnerWithInners;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.test.InnerTestClasses;
import org.jetbrains.jet.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.jet.generators.tests.TestsPackage}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/diagnostics/testsWithJsStdLib")
@TestDataPath("$PROJECT_ROOT")
@InnerTestClasses({JetDiagnosticsTestWithJsStdLibGenerated.Native.class})
@RunWith(JUnit3RunnerWithInners.class)
public class JetDiagnosticsTestWithJsStdLibGenerated extends AbstractJetDiagnosticsTestWithJsStdLib {
    public void testAllFilesPresentInTestsWithJsStdLib() throws Exception {
        JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/diagnostics/testsWithJsStdLib"), Pattern.compile("^(.+)\\.kt$"), true);
    }

    @TestMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native")
    @TestDataPath("$PROJECT_ROOT")
    @InnerTestClasses({Native.NativeGetter.class, Native.NativeInvoke.class, Native.NativeSetter.class})
    @RunWith(JUnit3RunnerWithInners.class)
    public static class Native extends AbstractJetDiagnosticsTestWithJsStdLib {
        public void testAllFilesPresentInNative() throws Exception {
            JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/diagnostics/testsWithJsStdLib/native"), Pattern.compile("^(.+)\\.kt$"), true);
        }

        @TestMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class NativeGetter extends AbstractJetDiagnosticsTestWithJsStdLib {
            public void testAllFilesPresentInNativeGetter() throws Exception {
                JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter"), Pattern.compile("^(.+)\\.kt$"), true);
            }

            @TestMetadata("onLocalExtensionFun.kt")
            public void testOnLocalExtensionFun() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onLocalExtensionFun.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalNativeClassMembers.kt")
            public void testOnLocalNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onLocalNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalNonNativeClassMembers.kt")
            public void testOnLocalNonNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onLocalNonNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalOtherDeclarations.kt")
            public void testOnLocalOtherDeclarations() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onLocalOtherDeclarations.kt");
                doTest(fileName);
            }

            @TestMetadata("onNativeClassMembers.kt")
            public void testOnNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onNestedDeclarationsInsideNativeClass.kt")
            public void testOnNestedDeclarationsInsideNativeClass() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onNestedDeclarationsInsideNativeClass.kt");
                doTest(fileName);
            }

            @TestMetadata("onNestedDeclarationsInsideNonNativeClass.kt")
            public void testOnNestedDeclarationsInsideNonNativeClass() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onNestedDeclarationsInsideNonNativeClass.kt");
                doTest(fileName);
            }

            @TestMetadata("onNonNativeClassMembers.kt")
            public void testOnNonNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onNonNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onToplevelExtensionFun.kt")
            public void testOnToplevelExtensionFun() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onToplevelExtensionFun.kt");
                doTest(fileName);
            }

            @TestMetadata("onToplevelOtherDeclarations.kt")
            public void testOnToplevelOtherDeclarations() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeGetter/onToplevelOtherDeclarations.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class NativeInvoke extends AbstractJetDiagnosticsTestWithJsStdLib {
            public void testAllFilesPresentInNativeInvoke() throws Exception {
                JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke"), Pattern.compile("^(.+)\\.kt$"), true);
            }

            @TestMetadata("onLocalExtensionFun.kt")
            public void testOnLocalExtensionFun() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onLocalExtensionFun.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalNativeClassMembers.kt")
            public void testOnLocalNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onLocalNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalNonNativeClassMembers.kt")
            public void testOnLocalNonNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onLocalNonNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalOtherDeclarations.kt")
            public void testOnLocalOtherDeclarations() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onLocalOtherDeclarations.kt");
                doTest(fileName);
            }

            @TestMetadata("onNativeClassMembers.kt")
            public void testOnNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onNestedDeclarationsInsideNativeClass.kt")
            public void testOnNestedDeclarationsInsideNativeClass() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onNestedDeclarationsInsideNativeClass.kt");
                doTest(fileName);
            }

            @TestMetadata("onNestedDeclarationsInsideNonNativeClass.kt")
            public void testOnNestedDeclarationsInsideNonNativeClass() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onNestedDeclarationsInsideNonNativeClass.kt");
                doTest(fileName);
            }

            @TestMetadata("onNonNativeClassMembers.kt")
            public void testOnNonNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onNonNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onToplevelExtensionFun.kt")
            public void testOnToplevelExtensionFun() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onToplevelExtensionFun.kt");
                doTest(fileName);
            }

            @TestMetadata("onToplevelOtherDeclarations.kt")
            public void testOnToplevelOtherDeclarations() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeInvoke/onToplevelOtherDeclarations.kt");
                doTest(fileName);
            }
        }

        @TestMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter")
        @TestDataPath("$PROJECT_ROOT")
        @RunWith(JUnit3RunnerWithInners.class)
        public static class NativeSetter extends AbstractJetDiagnosticsTestWithJsStdLib {
            public void testAllFilesPresentInNativeSetter() throws Exception {
                JetTestUtils.assertAllTestsPresentByMetadata(this.getClass(), new File("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter"), Pattern.compile("^(.+)\\.kt$"), true);
            }

            @TestMetadata("onLocalExtensionFun.kt")
            public void testOnLocalExtensionFun() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onLocalExtensionFun.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalNativeClassMembers.kt")
            public void testOnLocalNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onLocalNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalNonNativeClassMembers.kt")
            public void testOnLocalNonNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onLocalNonNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onLocalOtherDeclarations.kt")
            public void testOnLocalOtherDeclarations() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onLocalOtherDeclarations.kt");
                doTest(fileName);
            }

            @TestMetadata("onNativeClassMembers.kt")
            public void testOnNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onNestedDeclarationsInsideNativeClass.kt")
            public void testOnNestedDeclarationsInsideNativeClass() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onNestedDeclarationsInsideNativeClass.kt");
                doTest(fileName);
            }

            @TestMetadata("onNestedDeclarationsInsideNonNativeClass.kt")
            public void testOnNestedDeclarationsInsideNonNativeClass() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onNestedDeclarationsInsideNonNativeClass.kt");
                doTest(fileName);
            }

            @TestMetadata("onNonNativeClassMembers.kt")
            public void testOnNonNativeClassMembers() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onNonNativeClassMembers.kt");
                doTest(fileName);
            }

            @TestMetadata("onToplevelExtensionFun.kt")
            public void testOnToplevelExtensionFun() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onToplevelExtensionFun.kt");
                doTest(fileName);
            }

            @TestMetadata("onToplevelOtherDeclarations.kt")
            public void testOnToplevelOtherDeclarations() throws Exception {
                String fileName = JetTestUtils.navigationMetadata("compiler/testData/diagnostics/testsWithJsStdLib/native/nativeSetter/onToplevelOtherDeclarations.kt");
                doTest(fileName);
            }
        }
    }
}
