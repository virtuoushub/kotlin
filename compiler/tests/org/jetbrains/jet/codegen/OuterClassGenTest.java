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

package org.jetbrains.jet.codegen;

import com.intellij.util.lang.UrlClassLoader;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.OutputFile;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.InputStream;

import static org.jetbrains.jet.codegen.CodegenTestUtil.compileJava;
import static org.jetbrains.jet.utils.UtilsPackage.join;

public class OuterClassGenTest extends CodegenTestCase {

    private static final String TEST_FOLDER = "outerClassInfo";

    public void testClass() throws Exception {
        doTest("foo.Foo", "outerClassInfo");
    }

    public void testClassObject() throws Exception {
        doTest("foo.Foo$object", "outerClassInfo");
    }

    public void testInnerClass() throws Exception {
        doTest("foo.Foo$InnerClass", "outerClassInfo");
    }

    public void testInnerObject() throws Exception {
        doTest("foo.Foo$InnerObject", "outerClassInfo");
    }

    public void testLocalClassInFunction() throws Exception {
        doTest("foo.Foo$foo$LocalClass", "foo.Foo$1LocalClass", "outerClassInfo");
    }

    public void testLocalObjectInFunction() throws Exception {
        doTest("foo.Foo$foo$LocalObject", "foo.Foo$1LocalObject", "outerClassInfo");
    }

    public void testObjectInPackageClass() throws Exception {
        doTest("foo.PackageInnerObject", "outerClassInfo");
    }

    public void testLambdaInNoInlineFun() throws Exception {
        doTest("foo.Foo$foo$1", "foo.Foo$1Lambda", "outerClassInfo");
    }

    public void testLambdaInConstructor() throws Exception {
        doTest("foo.Foo$s$1", "foo.Foo$1LambdaInConstructor", "outerClassInfo");
    }

    public void testObjectLiteralInPackageClass() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/FooPackage$outerClassInfo$", null, null);
        doCustomTest("foo/FooPackage\\$.+\\$packageObjectLiteral\\$1", expectedInfo, "outerClassInfo");
    }

    public void testLocalClassInTopLevelFunction() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/FooPackage$outerClassInfo$", "packageMethod", "(Lfoo/Foo;)V");
        doCustomTest("foo/FooPackage\\$.+\\$packageMethod\\$PackageLocalClass", expectedInfo, "outerClassInfo");
    }

    public void testLocalObjectInTopLevelFunction() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/FooPackage$outerClassInfo$", "packageMethod", "(Lfoo/Foo;)V");
        doCustomTest("foo/FooPackage\\$.+\\$packageMethod\\$PackageLocalObject", expectedInfo, "outerClassInfo");
    }

    public void testLocalObjectInInlineFunction() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/Foo", "inlineFoo", "(Lkotlin/Function0;)V");
        doCustomTest("foo/Foo\\$inlineFoo\\$localObject\\$1", expectedInfo, "inlineObject");
    }

    public void testLocalObjectInlined() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/Bar", null, null);
        doCustomTest("foo/Bar\\$callToInline\\$\\$inlined\\$inlineFoo\\$1", expectedInfo, "inlineObject");
    }

    public void testLocalObjectInInlineLambda() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/Bar", null, null);
        doCustomTest("foo/Bar\\$objectInInlineLambda\\$\\$inlined\\$simpleFoo\\$lambda\\$1", expectedInfo, "inlineObject");
    }

    public void testLocalObjectInLambdaInlinedIntoObject() throws Exception {
        OuterClassInfo intoObjectInfo = new OuterClassInfo("foo/Bar", null, null);

        doCustomTest("foo/Bar\\$objectInLambdaInlinedIntoObject\\$\\$inlined\\$inlineFoo\\$1", intoObjectInfo, "inlineObject");

        OuterClassInfo objectInLambda = new OuterClassInfo("foo/Bar$objectInLambdaInlinedIntoObject$$inlined$inlineFoo$1", null, null);
        doCustomTest("foo/Bar\\$objectInLambdaInlinedIntoObject\\$\\$inlined\\$inlineFoo\\$lambda\\$lambda\\$1",
                     objectInLambda, "inlineObject");
    }

    public void testLambdaInInlineFunction() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/Foo", "inlineFoo", "(Lkotlin/Function0;)V");
        doCustomTest("foo/Foo\\$inlineFoo\\$1", expectedInfo, "inlineLambda");
    }

    public void testLambdaInlined() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/Bar", null, null);
        doCustomTest("foo/Bar\\$callToInline\\$\\$inlined\\$inlineFoo\\$1", expectedInfo, "inlineLambda");
    }

    public void testLambdaInInlineLambda() throws Exception {
        OuterClassInfo expectedInfo = new OuterClassInfo("foo/Bar", null, null);
        doCustomTest("foo/Bar\\$objectInInlineLambda\\$\\$inlined\\$simpleFoo\\$lambda\\$1", expectedInfo, "inlineLambda");
    }

    public void testLambdaInLambdaInlinedIntoObject() throws Exception {
        OuterClassInfo intoObjectInfo = new OuterClassInfo("foo/Bar", null, null);

        doCustomTest("foo/Bar\\$objectInLambdaInlinedIntoObject\\$\\$inlined\\$inlineFoo\\$1", intoObjectInfo, "inlineLambda");

        OuterClassInfo objectInLambda = new OuterClassInfo("foo/Bar$objectInLambdaInlinedIntoObject$$inlined$inlineFoo$1", null, null);
        doCustomTest("foo/Bar\\$objectInLambdaInlinedIntoObject\\$\\$inlined\\$inlineFoo\\$lambda\\$lambda\\$1",
                     objectInLambda, "inlineLambda");
    }

    private void doTest(@NotNull String classFqName, @NotNull String testDataFile) throws Exception {
        doTest(classFqName, classFqName, testDataFile);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithMockJdkAndIdeaAnnotations(ConfigurationKind.JDK_ONLY);
    }

    private void doTest(@NotNull String classFqName, @NotNull String javaClassName, @NotNull String testDataFile) throws Exception {
        File javaClassesTempDirectory = compileJava(TEST_FOLDER + "/" + testDataFile + ".java");

        UrlClassLoader javaClassLoader = UrlClassLoader.build().urls(javaClassesTempDirectory.toURI().toURL()).get();

        String javaClassPath = javaClassName.replace('.', File.separatorChar) + ".class";
        InputStream javaClassStream = javaClassLoader.getResourceAsStream(javaClassPath);
        assert javaClassStream != null : "Couldn't find class bytecode " + javaClassPath;

        ClassReader javaReader =  new ClassReader(javaClassStream);
        ClassReader kotlinReader = getKotlinClassReader(classFqName.replace('.', '/').replace("$", "\\$"), testDataFile);

        checkInfo(kotlinReader, javaReader);
    }

    private void doCustomTest(
            @Language("RegExp") @NotNull String internalNameRegexp,
            @NotNull OuterClassInfo expectedInfo,
            @NotNull String testDataFile
    ) {
        ClassReader kotlinReader = getKotlinClassReader(internalNameRegexp, testDataFile);
        OuterClassInfo kotlinInfo = getOuterClassInfo(kotlinReader);
        String message = "Error in enclosingMethodInfo info for class: " + kotlinReader.getClassName();
        if (kotlinInfo.owner == null) {
            assertNull(expectedInfo.owner);
        }
        else {
            assertTrue(message + "\n" + kotlinInfo.owner + " doesn't start with " + expectedInfo.owner,
                       kotlinInfo.owner.startsWith(expectedInfo.owner));
        }
        assertEquals(message, expectedInfo.method, kotlinInfo.method);
        assertEquals(message, expectedInfo.descriptor, kotlinInfo.descriptor);
    }

    @NotNull
    private ClassReader getKotlinClassReader(@Language("RegExp") @NotNull String internalNameRegexp, @NotNull String testDataFile) {
        loadFile(TEST_FOLDER + "/" + testDataFile + ".kt");
        OutputFileCollection outputFiles = generateClassesInFile();
        for (OutputFile file : outputFiles.asList()) {
            if (file.getRelativePath().matches(internalNameRegexp + "\\.class")) {
                return new ClassReader(file.asByteArray());
            }
        }
        throw new AssertionError("Couldn't find class by regexp: " + internalNameRegexp + " in:\n" + join(outputFiles.asList(), "\n"));
    }

    private static void checkInfo(@NotNull ClassReader kotlinReader, @NotNull ClassReader javaReader) {
        OuterClassInfo kotlinInfo = getOuterClassInfo(kotlinReader);
        OuterClassInfo javaInfo = getOuterClassInfo(javaReader);
        //noinspection ConstantConditions
        compareInfo(kotlinReader.getClassName(), kotlinInfo, javaInfo);
    }

    private static void compareInfo(
            @NotNull String kotlinClassName,
            @NotNull OuterClassInfo kotlinInfo,
            @NotNull OuterClassInfo expectedJavaInfo
    ) {
        assertEquals("Error in enclosingMethodInfo info for: " + kotlinClassName + " class", expectedJavaInfo, kotlinInfo);
    }

    public static OuterClassInfo getOuterClassInfo(ClassReader reader) {
        final OuterClassInfo info = new OuterClassInfo();
        reader.accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public void visitOuterClass(@NotNull String owner, String name, String desc) {
                info.owner = owner;
                info.method = name;
                info.descriptor = desc;
            }
        }, 0);
        return info;
    }

    private static class OuterClassInfo {
        private String owner;
        private String method;
        private String descriptor;

        private OuterClassInfo(String owner, String method, String descriptor) {
            this.owner = owner;
            this.method = method;
            this.descriptor = descriptor;
        }

        private OuterClassInfo() {
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof OuterClassInfo)) return false;

            OuterClassInfo info = (OuterClassInfo) o;

            if (descriptor != null ? !descriptor.equals(info.descriptor) : info.descriptor != null) return false;
            if (method != null ? !method.equals(info.method) : info.method != null) return false;
            if (owner != null ? !owner.equals(info.owner) : info.owner != null) return false;

            return true;
        }

        @Override
        public String toString() {
            return "[owner=" + owner + ", method=" + method + ", descriptor="+ descriptor + "]";
        }
    }
}
