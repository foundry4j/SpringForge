package io.github.foundry4j.forge.web.version;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// Declares which API versions an endpoint supports.
///
/// The annotation can be placed on a controller, where it applies to every handler,
/// or on a single handler, where it overrides the class level declaration. Endpoints
/// without the annotation are not checked, which keeps existing APIs compatible.
///
/// Callers select a version with the `X-API-Version` header or a `vN` first path
/// segment; the default version is used when neither is present.
///
/// ```java
/// @RestController
/// @RequestMapping("/orders")
/// @ApiVersion("v1")
/// public class OrderController {
///
///     @GetMapping("/legacy")
///     @ApiVersion(value = "v1", deprecated = true, sunset = "2027-01-01")
///     public List<OrderView> legacy() { ... }
/// }
/// ```
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ApiVersion {

    /// Supported versions, for example `{"v1", "v2"}`.
    String[] value();

    /// Marks the endpoint as deprecated; responses carry `Deprecation: true`.
    boolean deprecated() default false;

    /// Planned removal date in ISO-8601 format, written to the `Sunset` header.
    String sunset() default "";

    /// Replacement endpoint, written to the `Link` header as `successor-version`.
    String successor() default "";
}
