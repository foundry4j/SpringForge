package io.github.foundry4j.forge.core.support;

import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import java.lang.reflect.Method;

/// Evaluates SpEL expressions against a method invocation.
///
/// Annotations such as `@DistributedLock`, `@RateLimit` and `@ForgeScheduled` reuse
/// this helper to turn expressions like `#order.id` into concrete keys.
public final class ExpressionEvaluator {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    /// Evaluates `expression` against `method` and `args` and returns the string form.
    ///
    /// @param expression SpEL expression, for example `#id` or `#request.orderNo`
    /// @param method     method that owns the annotation
    /// @param target     target instance, used for `#root.target` references
    /// @param args       invocation arguments
    /// @return the evaluated value as string, or `null` when the expression yields `null`
    public String evaluate(String expression, Method method, Object target, Object[] args) {
        var context = new MethodBasedEvaluationContext(target, method, args, discoverer);
        Object value = parser.parseExpression(expression).getValue(context);
        return value == null ? null : value.toString();
    }
}
