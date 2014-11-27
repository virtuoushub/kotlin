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

package org.jetbrains.jet.utils;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class LibraryUtils {
    private static final Logger LOG = Logger.getInstance(LibraryUtils.class);

    public static final String TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB;
    public static final String TITLE_KOTLIN_JAVASCRIPT_STDLIB;
    public static final String TITLE_KOTLIN_JAVASCRIPT_LIB;
    private static final String METAINF = "META-INF/";
    private static final String MANIFEST_PATH = METAINF + "MANIFEST.MF";
    private static final String METAINF_RESOURCES = METAINF + "resources/";
    private static final Attributes.Name KOTLIN_JS_MODULE_NAME = new Attributes.Name("Kotlin-JS-Module-Name");

    static {
        String jsStdLib = "";
        String jsLib = "";
        String jvmStdLib = "";

        InputStream manifestProperties = LibraryUtils.class.getResourceAsStream("/manifest.properties");
        if (manifestProperties != null) {
            try {
                Properties properties = new Properties();
                properties.load(manifestProperties);
                jvmStdLib = properties.getProperty("manifest.impl.title.kotlin.jvm.runtime");
                jsStdLib = properties.getProperty("manifest.impl.title.kotlin.javascript.stdlib");
                jsLib = properties.getProperty("manifest.spec.title.kotlin.javascript.lib");
            }
            catch (IOException e) {
                LOG.error(e);
            }
        }
        else {
            LOG.error("Resource 'manifest.properties' not found.");
        }

        TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB = jvmStdLib;
        TITLE_KOTLIN_JAVASCRIPT_STDLIB = jsStdLib;
        TITLE_KOTLIN_JAVASCRIPT_LIB = jsLib;
    }

    private LibraryUtils() {}

    @Nullable
    public static Manifest getManifestFromJar(@NotNull File library) {
        if (!library.canRead()) return null;

        try {
            JarFile jarFile = new JarFile(library);
            try {
                return jarFile.getManifest();
            }
            finally {
                jarFile.close();
            }
        }
        catch (IOException ignored) {
            return null;
        }
    }

    @Nullable
    public static Manifest getManifestFromDirectory(@NotNull File library) {
        if (!library.canRead() || !library.isDirectory()) return null;

        try {
            InputStream inputStream = new FileInputStream(new File(library, MANIFEST_PATH));
            try {
                return new Manifest(inputStream);
            }
            finally {
                inputStream.close();
            }
        }
        catch (IOException ignored) {
            LOG.warn("IOException " + ignored);
            return null;
        }
    }

    private static Manifest getManifestFromJarOrDirectory(@NotNull File library) {
        return library.isDirectory() ? getManifestFromDirectory(library) : getManifestFromJar(library);
    }

    @Nullable
    public static Attributes getManifestMainAttributesFromJarOrDirectory(@NotNull File library) {
        Manifest manifest = getManifestFromJarOrDirectory(library);
        return manifest != null ? manifest.getMainAttributes() : null;
    }

    private static boolean checkAttributeValue(@NotNull File library, String expected, @NotNull Attributes.Name attributeName) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        if (attributes == null) return false;

        String value = attributes.getValue(attributeName);
        return value != null && value.equals(expected);
    }

    @Nullable
    public static String getKotlinJsModuleName(@NotNull File library) {
        Attributes attributes = getManifestMainAttributesFromJarOrDirectory(library);
        return attributes != null ? attributes.getValue(KOTLIN_JS_MODULE_NAME) : null;
    }

    public static boolean isKotlinJavascriptLibrary(@NotNull File library) {
        return checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_LIB, Attributes.Name.SPECIFICATION_TITLE);
    }

    public static boolean isKotlinJavascriptStdLibrary(@NotNull File library) {
        return checkAttributeValue(library, TITLE_KOTLIN_JAVASCRIPT_STDLIB, Attributes.Name.IMPLEMENTATION_TITLE);
    }

    public static boolean isJvmRuntimeLibrary(@NotNull File library) {
        return checkAttributeValue(library, TITLE_KOTLIN_JVM_RUNTIME_AND_STDLIB, Attributes.Name.IMPLEMENTATION_TITLE);
    }


    private static void readZip(String file, @NotNull List<JsFile> jsFiles) throws IOException {
        ZipFile zipFile = new ZipFile(file);
        try {
            traverseArchive(zipFile, jsFiles);
        }
        finally {
            zipFile.close();
        }
    }

    private static void traverseArchive(@NotNull ZipFile zipFile, @NotNull List<JsFile> jsFiles) throws IOException {
        Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = zipEntries.nextElement();
            String entryName = entry.getName();
            if (!entry.isDirectory() && entryName.endsWith(".js")) {
                if (entryName.startsWith(METAINF)) {
                    if(entryName.startsWith(METAINF_RESOURCES)) {
                        entryName = entryName.substring(METAINF_RESOURCES.length());
                    }
                    else {
                        continue;
                    }
                }
                InputStream stream = zipFile.getInputStream(entry);
                String text = StringUtil.convertLineSeparators(FileUtil.loadTextAndClose(stream));
                JsFile jsFile = new JsFile(entryName, text);
                jsFiles.add(jsFile);
            }
        }
    }

    public static void loadJsFilesFromZipOrJar(@NotNull File file, @NotNull List<JsFile> jsFiles) {
        try {
            readZip(file.getAbsolutePath(), jsFiles);
        }
        catch (IOException ex) {
            LOG.warn(ex.toString());
        }
    }

    public static void loadJsFilesFromZipOrJar(@NotNull String filePath, @NotNull List<JsFile> jsFiles) {
        try {
            readZip(filePath, jsFiles);
        }
        catch (IOException ex) {
            LOG.warn(ex.toString());
        }
    }

    public static List<JsFile> loadJsFiles(@NotNull List<String> libraries) {
        List<JsFile> jsFiles = new ArrayList<JsFile>();
        for (String library : libraries) {
            loadJsFilesFromZipOrJar(library, jsFiles);
        }
        return jsFiles;
    }

    public static void copyJsFilesFromLibraries(@NotNull List<String> libraries, @NotNull String  outputLibraryJSPath) {
        List<JsFile> jsFiles = loadJsFiles(libraries);
        writeJsFiles(jsFiles, outputLibraryJSPath);
    }

    public static void writeJsFiles(@NotNull List<JsFile> jsFiles, @NotNull String  outputLibraryJSPath) {
        for(JsFile jsFile : jsFiles) {
            try {
                File outputFile = new File(outputLibraryJSPath, jsFile.relativePath);
                FileUtil.writeToFile(outputFile, jsFile.content.getBytes());
            }
            catch (IOException ex) {
                LOG.warn(ex.getMessage());
            }
        }
    }

    public static VirtualFile getJarFile(@NotNull List<VirtualFile> classesRoots, @NotNull String jarName) {
        for (VirtualFile root : classesRoots) {
            if (root.getName().equals(jarName)) {
                return root;
            }
        }

        return null;
    }

    public static class JsFile {
        public final String relativePath;
        public final String content;

        public JsFile(@NotNull String relativePath, @NotNull String content) {
            this.relativePath = relativePath;
            this.content = content;
        }

        @Override
        public String toString() {
            return "JsFile(" + relativePath + ")";
        }
    }

}
