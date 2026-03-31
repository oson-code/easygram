package uz.osoncode.easygram.core.markup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import uz.osoncode.easygram.core.annotation.BotConfiguration;
import uz.osoncode.easygram.core.annotation.BotMarkup;
import uz.osoncode.easygram.core.chatstate.BotChatState;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/**
 * {@link ApplicationRunner} that scans all {@link BotConfiguration} beans for methods
 * annotated with {@link BotMarkup} and registers them in the {@link BotMarkupRegistry}.
 *
 * <p>Markup factory creation — including parameter resolution and reflective invocation —
 * is fully delegated to {@link BotMarkupFactory}. The default implementation,
 * {@link DefaultBotMarkupFactory}, resolves method parameters via the framework's
 * {@code BotArgumentResolverFactory}, enabling arbitrary parameter types in
 * {@code @BotMarkup} methods.</p>
 *
 * <h2>State-bound keyboards</h2>
 * <p>If a {@code @BotMarkup} method is also annotated with
 * {@link BotChatState @BotChatState("STATE")}, the same factory is additionally
 * registered via {@link BotMarkupRegistry#registerForState} for each declared state.
 * This allows handlers that forward to those states to receive the keyboard
 * automatically, without an explicit {@code @BotReplyMarkup} annotation.</p>
 *
 * <p>Override {@link BotMarkupFactory} to customise how markup methods are invoked,
 * or override this bean to customise how they are scanned and registered.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 * @see BotMarkupFactory
 * @see DefaultBotMarkupFactory
 */
@Slf4j
@RequiredArgsConstructor
public class BotMarkupLoader implements ApplicationRunner {

    private final ApplicationContext applicationContext;
    private final BotMarkupRegistry markupRegistry;
    private final BotMarkupFactory botMarkupFactory;

    @Override
    public void run(ApplicationArguments args) {
        Map<String, Object> configBeans =
                applicationContext.getBeansWithAnnotation(BotConfiguration.class);

        for (Object bean : configBeans.values()) {
            Class<?> targetClass = AopUtils.getTargetClass(bean);
            for (Method method : targetClass.getDeclaredMethods()) {
                BotMarkup annotation = AnnotationUtils.findAnnotation(method, BotMarkup.class);
                if (Objects.isNull(annotation)) {
                    continue;
                }
                var factory = botMarkupFactory.create(bean, method);
                markupRegistry.register(annotation.value(), factory);

                BotChatState chatState = AnnotationUtils.findAnnotation(method, BotChatState.class);
                if (Objects.nonNull(chatState)) {
                    if (chatState.value().length == 0) {
                        log.warn("@BotChatState on @BotMarkup method '{}' has an empty value array — " +
                                "no state-bound keyboard will be registered. " +
                                "Specify at least one state name to enable auto-attachment.",
                                method.getName());
                    } else {
                        for (String state : chatState.value()) {
                            markupRegistry.registerForState(state, factory);
                        }
                    }
                }
            }
        }
    }
}

