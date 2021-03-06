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

package org.jetbrains.jet.plugin.versions;

import com.google.common.collect.ImmutableList;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

public class KotlinRuntimeLibraryCoreUtil {
    @Nullable
    public static PsiClass getKotlinRuntimeMarkerClass(@NotNull GlobalSearchScope scope) {
        FqName kotlinPackageFqName = FqName.topLevel(Name.identifier("kotlin"));
        String kotlinPackageClassFqName = PackageClassUtils.getPackageClassFqName(kotlinPackageFqName).asString();

        ImmutableList<String> candidateClassNames = ImmutableList.of(
                kotlinPackageClassFqName,
                "kotlin.Unit",
                // For older versions
                "kotlin.namespace",
                "jet.Unit"
        );

        for (String className : candidateClassNames) {
            PsiClass psiClass = JavaPsiFacade.getInstance(scope.getProject()).findClass(className, scope);
            if (psiClass != null) {
                return psiClass;
            }
        }
        return null;
    }
}
