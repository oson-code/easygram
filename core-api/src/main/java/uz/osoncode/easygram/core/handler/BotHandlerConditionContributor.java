package uz.osoncode.easygram.core.handler;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Strategy for contributing additional {@link BotHandlerCondition}s to handler methods
 * discovered by the framework.
 *
 * <p>Implementations are collected from the Spring application context by
 * {@code BotHandlerLoader} and consulted for every handler method during the startup scan.
 * Each contributor can inspect the method and its owning bean and return zero or more
 * conditions that must be satisfied at runtime before the handler is eligible to process
 * an update.</p>
 *
 * <p>Use this SPI to add cross-cutting match conditions (such as permission checks,
 * feature-flag guards, or user-type restrictions) without modifying any framework class:</p>
 *
 * <pre>{@code
 * @Bean
 * public BotHandlerConditionContributor adminOnlyContributor() {
 *     return (method, bean) -> {
 *         if (!method.isAnnotationPresent(AdminOnly.class)) return List.of();
 *         return List.of(req -> req.getUser() != null && adminService.isAdmin(req.getUser().getId()));
 *     };
 * }
 * }</pre>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotHandlerCondition
 */
@FunctionalInterface
public interface BotHandlerConditionContributor {

    /**
     * Returns the conditions to apply to the given handler method.
     *
     * <p>Return an empty list when this contributor has no conditions to add for the
     * given method. Never return {@code null}.</p>
     *
     * @param method the handler method being registered; must not be {@code null}
     * @param bean   the controller bean that owns the method; must not be {@code null}
     * @return an immutable or mutable list of conditions; never {@code null}
     */
    List<BotHandlerCondition> contribute(Method method, Object bean);
}
