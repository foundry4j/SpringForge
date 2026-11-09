package io.github.foundry4j.forge.test;

import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Cleans every JPA entity before each test method of a Spring test class.
///
/// ```java
/// @SpringBootTest
/// @CleanDatabase
/// class UserRepositoryTest { ... }
/// ```
///
/// The extension issues a bulk `DELETE FROM Entity` for every entity of the metamodel,
/// which works on any database. Foreign key order is not handled; when entities reference
/// each other either write custom cleanup SQL or roll the test back with
/// `@Transactional`.
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(DatabaseCleanupExtension.class)
public @interface CleanDatabase {
}
