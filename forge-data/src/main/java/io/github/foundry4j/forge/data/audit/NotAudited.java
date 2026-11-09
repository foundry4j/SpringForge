package io.github.foundry4j.forge.data.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Excludes an entity type from auditing.
///
/// Audit records themselves must carry this annotation, otherwise writing an audit
/// row would trigger another audit event and recurse endlessly.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface NotAudited {
}
