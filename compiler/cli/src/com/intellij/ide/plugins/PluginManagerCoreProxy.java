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

package com.intellij.ide.plugins;

import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.io.ZipFileCache;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.XmlSerializationException;
import org.jdom.Document;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

// TODO drop this temporary hack to access to PluginManagerCore methods when CoreApplicationEnvironment got similar features.
public class PluginManagerCoreProxy {
    private PluginManagerCoreProxy() {}

    @Nullable
    public static IdeaPluginDescriptorImpl loadDescriptorFromDir(@NotNull File file, @NotNull String fileName) {
        return PluginManagerCore.loadDescriptorFromDir(file, fileName);
    }

    @Nullable
    public static IdeaPluginDescriptorImpl loadDescriptorFromJar(@NotNull File file, @NotNull String fileName) {
        //return PluginManagerCore.loadDescriptorFromJar(file, fileName);
        try {
            String fileURL = StringUtil.replace(file.toURI().toASCIIString(), "!", "%21");
            URL jarURL = new URL("jar:" + fileURL + "!/META-INF/" + fileName);

            ZipFile zipFile = ZipFileCache.acquire(file.getPath());
            try {
                ZipEntry entry = zipFile.getEntry("META-INF/" + fileName);
                if (entry != null) {
                    Document document = JDOMUtil.loadDocument(zipFile.getInputStream(entry));
                    IdeaPluginDescriptorImpl descriptor = new IdeaPluginDescriptorImpl(file);
                    descriptor.readExternal(document, jarURL);
                    System.err.println("$$$ I: Load from" + file);
                    return descriptor;
                }
            }
            finally {
                ZipFileCache.release(zipFile);
            }
        }
        catch (XmlSerializationException e) {
            System.err.println("$$ I: Cannot load " + file + "\n" + e);
            System.err.println("$$ I: Plugin file " + file.getName() + " contains invalid plugin descriptor file.");

            PluginManagerCore.getLogger().info("Cannot load " + file, e);
            PluginManagerCore.prepareLoadingPluginsErrorMessage(
                    "Plugin file " + file.getName() + " contains invalid plugin descriptor file.");
        }
        catch (Throwable e) {
            System.err.println("$$$ I: Cannot load " + file + "\n" + e);
            PluginManagerCore.getLogger().info("Cannot load " + file, e);
        }

        return null;
    }

    // copied as is from PluginManagerCore#registerExtensionPointsAndExtensions
    public static void registerExtensionPointsAndExtensions(ExtensionsArea area, List<IdeaPluginDescriptorImpl> loadedPlugins) {
        for (IdeaPluginDescriptorImpl descriptor : loadedPlugins) {
            descriptor.registerExtensionPoints(area);
        }

        Set<String> epNames = ContainerUtil.newHashSet();
        for (ExtensionPoint point : area.getExtensionPoints()) {
            epNames.add(point.getName());
        }

        for (IdeaPluginDescriptorImpl descriptor : loadedPlugins) {
            for (String epName : epNames) {
                descriptor.registerExtensions(area, epName);
            }
        }
    }
}
