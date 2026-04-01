package uz.osoncode.easygram.core.handler.invocation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.executable.ExecutableValidator;
import lombok.RequiredArgsConstructor;
import uz.osoncode.easygram.core.argumentresolver.BotArgumentResolverFactory;
import uz.osoncode.easygram.core.exception.BotHandlerException;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

/**
 * {@link BotHandlerInvocationFilter} that resolves handler method arguments, optionally
 * validates them against Jakarta Bean Validation constraints, and invokes the controller
 * method reflectively.
 *
 * <p>This filter runs first in the invocation pipeline
 * (order = {@link BotHandlerInvocationFilterOrder#METHOD_INVOCATION}). It performs the
 * following steps in order:</p>
 * <ol>
 *   <li>Resolves each method parameter from the current request/response via
 *       {@link BotArgumentResolverFactory#resolveArguments}</li>
 *   <li>If a {@link Validator} is present, validates the resolved arguments using
 *       {@link ExecutableValidator#validateParameters}. If any constraint violations are
 *       found, a {@link ConstraintViolationException} is thrown <em>before</em> the
 *       handler method is invoked. This exception is <strong>not</strong> wrapped in a
 *       {@link BotHandlerException}, so it propagates directly to
 *       {@link uz.osoncode.easygram.core.filter.DefaultBotFilterChain} and can be caught
 *       by an {@code @BotExceptionHandler(ConstraintViolationException.class)} method.</li>
 *   <li>Invokes the controller method via reflection</li>
 *   <li>Stores the return value in {@link BotHandlerInvocationContext#setReturnValue}</li>
 *   <li>Calls {@code chain.proceed(context)} so subsequent filters can process the return value</li>
 * </ol>
 *
 * <p>Any {@link InvocationTargetException} thrown by the reflective call is unwrapped and
 * re-thrown as a {@link BotHandlerException}.</p>
 *
 * <h2>Usage example</h2>
 * <p>Annotate handler method parameters with Jakarta Validation constraints:</p>
 * <pre>{@code
 * @BotCommand("/register")
 * public String register(@BotCommandQueryParam("name") @NotBlank String name,
 *                        @BotCommandQueryParam("age")  @Min(18) int age) {
 *     return "Registered: " + name;
 * }
 *
 * @BotExceptionHandler(ConstraintViolationException.class)
 * public String handleValidation(ConstraintViolationException ex) {
 *     String msg = ex.getConstraintViolations().stream()
 *             .map(ConstraintViolation::getMessage)
 *             .collect(Collectors.joining(", "));
 *     return "Validation error: " + msg;
 * }
 * }</pre>
 *
 * <p>Validation is skipped entirely when no {@link Validator} bean is present in the
 * application context (i.e. when {@code spring-boot-starter-validation} or an equivalent
 * dependency is absent).</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@RequiredArgsConstructor
public class MethodInvocationFilter implements BotHandlerInvocationFilter {

    private final BotArgumentResolverFactory botArgumentResolverFactory;

    /**
     * Optional Jakarta {@link Validator} used to validate handler method parameters.
     * When empty, validation is skipped and the method is invoked without any constraint checks.
     */
    private final Optional<Validator> validator;

    /**
     * {@inheritDoc}
     *
     * @return {@link BotHandlerInvocationFilterOrder#METHOD_INVOCATION}
     */
    @Override
    public int getOrder() {
        return BotHandlerInvocationFilterOrder.METHOD_INVOCATION;
    }

    /**
     * Resolves arguments, validates them if a {@link Validator} is configured, invokes
     * the handler method, and stores the return value.
     *
     * <p>Argument resolution errors are wrapped in {@link BotHandlerException}. Constraint
     * violations throw {@link ConstraintViolationException} directly (unwrapped) so that
     * {@code @BotExceptionHandler} methods can catch them by their exact type. Method
     * invocation errors are also wrapped in {@link BotHandlerException}.</p>
     *
     * @param context the mutable invocation context
     * @param chain   the remaining filter chain
     * @throws ConstraintViolationException if any resolved argument violates a declared constraint
     * @throws BotHandlerException          if argument resolution or method invocation fails
     */
    @Override
    public void invoke(BotHandlerInvocationContext context, BotHandlerInvocationChain chain) throws InvocationTargetException, IllegalAccessException {
        Object[] args;
        try {
            args = botArgumentResolverFactory.resolveArguments(
                    context.getMethod().getParameters(),
                    context.getRequest(),
                    context.getResponse());
        } catch (Exception e) {
            throw new BotHandlerException("Failed to resolve arguments for: " + context.getMethod(), e);
        }

        // Validate resolved arguments against Jakarta Bean Validation constraints.
        // ConstraintViolationException propagates unwrapped so @BotExceptionHandler can catch it directly.
        validator.ifPresent(v -> {
            ExecutableValidator executableValidator = v.forExecutables();
            Set<ConstraintViolation<Object>> violations =
                    executableValidator.validateParameters(context.getBean(), context.getMethod(), args);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(violations);
            }
        });

        Object returnValue = null;
        try {
            returnValue = context.getMethod().invoke(context.getBean(), args);
            context.setReturnValue(returnValue);
            chain.proceed(context);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw e;
        }

    }
}
