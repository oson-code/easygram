package uz.osoncode.easygram.core.stereotype;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * Marks a class as a global bot exception handler advice, applied across all
 * {@link BotController} beans. Analogous to Spring Web's {@code @ControllerAdvice}.
 *
 * <p>Methods annotated with
 * {@link uz.osoncode.easygram.core.bind.annotation.BotExceptionHandler}
 * inside an advice class act as global fallbacks. Controller-local exception handlers
 * (inside {@link BotController} classes) always take priority over advice handlers for
 * the same exception type.</p>
 *
 * <p>Optionally restrict which {@link BotController} beans this advice applies to using
 * {@link #basePackages()}, {@link #assignableTypes()}, or {@link #annotations()}. Empty
 * arrays (the default) mean the advice applies globally to all controllers.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface BotControllerAdvice {

    /**
     * Alias for the Spring bean name of this advice component.
     * Aliased to {@link Component#value()} so that the name is honoured
     * by Spring's component-scan.
     *
     * @return the bean name; empty string means no explicit name
     */
    @AliasFor(annotation = Component.class, attribute = "value")
    String value() default "";

    /**
     * Restrict this advice to {@link BotController} beans in these base packages.
     * Empty (default) means all packages.
     *
     * @return the base packages to restrict this advice to
     */
    String[] basePackages() default {};

    /**
     * Restrict this advice to {@link BotController} beans that are assignable to
     * one of these types. Empty (default) means all types.
     *
     * @return the assignable types to restrict this advice to
     */
    Class<?>[] assignableTypes() default {};

    /**
     * Restrict this advice to {@link BotController} beans annotated with one of
     * these annotations. Empty (default) means all controllers.
     *
     * @return the annotations to restrict this advice to
     */
    Class<? extends Annotation>[] annotations() default {};
}
