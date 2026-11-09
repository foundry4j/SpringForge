package io.github.foundry4j.forge.security.aspect;

import io.github.foundry4j.forge.core.error.CommonErrorCode;
import io.github.foundry4j.forge.core.exception.BusinessException;
import io.github.foundry4j.forge.security.AuthContext;
import io.github.foundry4j.forge.security.Logical;
import io.github.foundry4j.forge.security.LoginUser;
import io.github.foundry4j.forge.security.annotation.RequiresPermission;
import io.github.foundry4j.forge.security.annotation.RequiresRole;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.Order;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/// Enforces [RequiresRole] and [RequiresPermission] on beans and methods.
///
/// The aspect reads the annotation from the most specific implementation method
/// first, so an interface declaration also works when the implementation overrides
/// the method without repeating the annotation.
@Aspect
@Order(100)
public class RbacAspect {

    private final AuthContext authContext;

    /// Creates the aspect.
    public RbacAspect(AuthContext authContext) {
        this.authContext = authContext;
    }

    /// Checks role requirements.
    @Around("@annotation(io.github.foundry4j.forge.security.annotation.RequiresRole)"
            + " || @within(io.github.foundry4j.forge.security.annotation.RequiresRole)")
    public Object checkRole(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresRole requirement = find(joinPoint, RequiresRole.class);
        if (requirement != null) {
            LoginUser user = authContext.requireUser();
            assertGranted(user.roles(), requirement.value(), requirement.logical(), "role");
        }
        return joinPoint.proceed();
    }

    /// Checks permission requirements.
    @Around("@annotation(io.github.foundry4j.forge.security.annotation.RequiresPermission)"
            + " || @within(io.github.foundry4j.forge.security.annotation.RequiresPermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequiresPermission requirement = find(joinPoint, RequiresPermission.class);
        if (requirement != null) {
            LoginUser user = authContext.requireUser();
            assertGranted(user.permissions(), requirement.value(), requirement.logical(), "permission");
        }
        return joinPoint.proceed();
    }

    private void assertGranted(Set<String> granted, String[] required, Logical logical, String kind) {
        if (required.length == 0) {
            return;
        }
        boolean satisfied = logical == Logical.ALL
                ? Arrays.stream(required).allMatch(value -> has(granted, value))
                : Arrays.stream(required).anyMatch(value -> has(granted, value));
        if (!satisfied) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN,
                    "Missing required %s: %s".formatted(kind, String.join(", ", required)));
        }
    }

    private boolean has(Collection<String> granted, String required) {
        return granted.contains(required) || granted.contains("*")
                || granted.stream().anyMatch(value -> value.endsWith("*")
                && required.startsWith(value.substring(0, value.length() - 1)));
    }

    private <A extends Annotation> A find(ProceedingJoinPoint joinPoint, Class<A> type) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> targetClass = joinPoint.getTarget() == null
                ? signature.getDeclaringType()
                : joinPoint.getTarget().getClass();
        Method method = AopUtils.getMostSpecificMethod(signature.getMethod(), targetClass);
        A annotation = AnnotatedElementUtils.findMergedAnnotation(method, type);
        return annotation != null ? annotation : AnnotatedElementUtils.findMergedAnnotation(targetClass, type);
    }
}
