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

package org.jetbrains.k2js.translate.reference;

import com.google.dart.compiler.backend.js.ast.*;
import com.google.dart.compiler.backend.js.ast.metadata.MetadataPackage;
import com.google.dart.compiler.common.SourceInfoImpl;
import com.google.gwt.dev.js.JsParser;
import com.google.gwt.dev.js.JsParserException;
import com.google.gwt.dev.js.rhino.ParserConfig;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetCallExpression;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.model.VariableAsFunctionResolvedCall;
import org.jetbrains.jet.lang.types.lang.InlineStrategy;
import org.jetbrains.jet.lang.types.lang.InlineUtil;
import org.jetbrains.jet.lang.psi.ValueArgument;
import org.jetbrains.k2js.translate.callTranslator.CallTranslator;
import org.jetbrains.k2js.translate.context.TranslationContext;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.DescriptorPredicate;
import org.jetbrains.k2js.translate.intrinsic.functions.patterns.PatternBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.calls.callUtil.CallUtilPackage.getFunctionResolvedCallWithAssert;

public final class CallExpressionTranslator extends AbstractCallExpressionTranslator {

    @NotNull
    private final static DescriptorPredicate JSCODE_PATTERN = PatternBuilder.pattern("kotlin.js", "jsCode");

    @NotNull
    private final static DescriptorPredicate JSEXPRESSION_PATTERN = PatternBuilder.pattern("kotlin.js", "jsExpression");

    @NotNull
    public static JsNode translate(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        if (matchesJsCode(expression, context)) {
            return (new CallExpressionTranslator(expression, receiver, context)).translateJsCode();
        }
        
        JsExpression callExpression = (new CallExpressionTranslator(expression, receiver, context)).translate();

        if (shouldBeInlined(expression, context)
            && callExpression instanceof JsInvocation) {

            MetadataPackage.setInlineStrategy((JsInvocation) callExpression, InlineStrategy.IN_PLACE);
        }

        return callExpression;
    }

    public static boolean shouldBeInlined(@NotNull JetCallExpression expression, @NotNull TranslationContext context) {
        if (!context.getConfig().isInlineEnabled()) return false;

        ResolvedCall<?> resolvedCall = CallUtilPackage.getResolvedCall(expression, context.bindingContext());
        assert resolvedCall != null;

        CallableDescriptor descriptor;

        if (resolvedCall instanceof VariableAsFunctionResolvedCall) {
            descriptor = ((VariableAsFunctionResolvedCall) resolvedCall).getVariableCall().getCandidateDescriptor();
        } else {
            descriptor = resolvedCall.getCandidateDescriptor();
        }

        if (descriptor instanceof SimpleFunctionDescriptor) {
            return ((SimpleFunctionDescriptor) descriptor).getInlineStrategy().isInline();
        }

        if (descriptor instanceof ValueParameterDescriptor) {
            DeclarationDescriptor containingDescriptor = descriptor.getContainingDeclaration();
            return InlineUtil.getInlineType(containingDescriptor).isInline()
                   && !InlineUtil.hasNoinlineAnnotation(descriptor);
        }

        return false;
    }

    private static boolean matchesJsCode(
            @NotNull JetCallExpression expression,
            @NotNull TranslationContext context
    ) {
        boolean matchesPattern = matchesPattern(expression, context, JSCODE_PATTERN)
                                 || matchesPattern(expression, context, JSEXPRESSION_PATTERN);

        return matchesPattern && expression.getValueArguments().size() == 1;
    }

    private static boolean matchesPattern(
            @NotNull JetCallExpression expression,
            @NotNull TranslationContext context,
            @NotNull DescriptorPredicate pattern
    ) {
        ResolvedCall<? extends FunctionDescriptor> resolvedCall =
                getFunctionResolvedCallWithAssert(expression, context.bindingContext());
        FunctionDescriptor descriptor = resolvedCall.getResultingDescriptor();

        return pattern.apply(descriptor);
    }

    @NotNull
    private static String removeQuotes(@NotNull String jsCode) {
        String singleQuote = "\"";
        String tripleQuote = "\"\"\"";

        if (endsMatchWith(jsCode, tripleQuote)) {
            jsCode = jsCode.substring(3, jsCode.length() - 3);
        } else if (endsMatchWith(jsCode, singleQuote)) {
            jsCode = jsCode.substring(1, jsCode.length() - 1);
        } else {
            throw new RuntimeException("String argument must have either 1 or 3 quotes");
        }

        return jsCode;
    }

    private static boolean endsMatchWith(String string, String pattern) {
        return string.startsWith(pattern) && string.endsWith(pattern);
    }

    private CallExpressionTranslator(
            @NotNull JetCallExpression expression,
            @Nullable JsExpression receiver,
            @NotNull TranslationContext context
    ) {
        super(expression, receiver, context);
    }

    @NotNull
    private JsExpression translate() {
        return CallTranslator.INSTANCE$.translate(context(), resolvedCall, receiver);
    }

    @NotNull
    private JsNode translateJsCode() {
        List<? extends ValueArgument> arguments = expression.getValueArguments();
        JetExpression argumentExpression = arguments.get(0).getArgumentExpression();
        assert argumentExpression != null;

        String jsCode = removeQuotes(argumentExpression.getText());
        List<JsStatement> statements = parseJsCode(jsCode);
        int size = statements.size();

        if (size == 0) {
            return program().getEmptyStatement();
        } else if (size > 1) {
            return new JsBlock(statements);
        } else {
            JsStatement resultStatement = statements.get(0);
            if (resultStatement instanceof JsExpressionStatement) {
                return ((JsExpressionStatement) resultStatement).getExpression();
            }

            return resultStatement;
        }
    }

    @NotNull
    private List<JsStatement> parseJsCode(@NotNull String jsCode) {
        List<JsStatement> statements = new ArrayList<JsStatement>();

        try {
            SourceInfoImpl info = new SourceInfoImpl(null, 0, 0, 0, 0);
            JsScope scope = context().scope();
            StringReader reader = new StringReader(jsCode);
            statements.addAll(JsParser.parse(info, scope, reader, getParserConfig()));
        } catch (JsParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return statements;
    }

    private ParserConfig getParserConfig() {
        /**
         * Always true, since any kotlin call
         * is translated to javascript call inside
         * some function (no top level calls)
         */
        boolean isInsideFunction = true;
        boolean isLiteral = matchesPattern(expression, context(), JSEXPRESSION_PATTERN);

        return new ParserConfig(isInsideFunction, isLiteral);
    }
}
