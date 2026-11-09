package io.github.foundry4j.forge.idempotency;

import io.github.foundry4j.forge.core.support.ExpressionEvaluator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/// Default [IdempotentKeyResolver].
///
/// When `key` is configured, each `#parameter.field` placeholder (or `#p0` by index) is
/// evaluated with SpEL. When it is blank, the JSON serialization of the arguments becomes
/// the key material, so identical calls are naturally deduplicated.
public class ExpressionIdempotentKeyResolver implements IdempotentKeyResolver {

    private static final Pattern PLACEHOLDER = Pattern.compile("#[A-Za-z_$][\\w$]*(?:\\.[\\w$]+)*");

    private final ExpressionEvaluator evaluator;
    private final ObjectMapper objectMapper;

    /// Creates the resolver.
    public ExpressionIdempotentKeyResolver(ExpressionEvaluator evaluator, ObjectMapper objectMapper) {
        this.evaluator = evaluator;
        this.objectMapper = objectMapper;
    }

    @Override
    public String resolve(Idempotent config, Method method, Object target, Object[] args) {
        String base = method.getDeclaringClass().getName() + "#" + method.getName();
        if (config.key() == null || config.key().isBlank()) {
            return base + ":" + serialize(args);
        }
        Matcher matcher = PLACEHOLDER.matcher(config.key());
        StringBuilder resolved = new StringBuilder();
        while (matcher.find()) {
            String value = evaluator.evaluate(matcher.group(), method, target, args);
            matcher.appendReplacement(resolved, Matcher.quoteReplacement(value == null ? "null" : value));
        }
        matcher.appendTail(resolved);
        return base + ":" + resolved;
    }

    private String serialize(Object[] args) {
        try {
            return objectMapper.writeValueAsString(args);
        } catch (JacksonException exception) {
            return Arrays.deepHashCode(args) + "";
        }
    }
}
