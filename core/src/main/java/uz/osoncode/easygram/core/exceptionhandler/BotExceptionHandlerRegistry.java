package uz.osoncode.easygram.core.exceptionhandler;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Registry that stores {@link BotExceptionMethodHandler} instances and keeps them sorted
 * so that the most specific (deepest in the class hierarchy) exception type is matched first.
 *
 * <p>When an exception is thrown during bot-handler execution, the registry's sorted list
 * allows callers to iterate and pick the first handler whose declared exception type is
 * assignable from the thrown exception — ensuring that, e.g., a handler for
 * {@code IOException} is preferred over one for {@code Exception}.</p>
 *
 * <p>Specificity is measured by counting superclass hops from the exception type up to
 * {@code null} (end of hierarchy): a greater depth means a more specific type and therefore
 * higher priority in the sorted list.</p>
 *
 * @author Islom Mirsaburov
 * @since 0.0.1
 */
@Getter
public class BotExceptionHandlerRegistry {

    /** Sorted list of exception handlers, most-specific exception type first. */
    private final List<BotExceptionMethodHandler<?>> botHandlers = new ArrayList<>();

    /**
     * Registers an exception handler and re-sorts the handler list so that the most
     * specific exception types remain at the front.
     *
     * @param botHandler the exception handler to register; must not be {@code null}
     */
    public void register(BotExceptionMethodHandler<?> botHandler) {
        register(botHandler, botHandlers);
    }

    /**
     * Adds the given handler to the target list and re-sorts by exception-type specificity.
     *
     * @param botHandler  the exception handler to add; must not be {@code null}
     * @param handlerList the list into which the handler should be inserted
     */
    private void register(BotExceptionMethodHandler<?> botHandler, List<BotExceptionMethodHandler<?>> handlerList) {
        handlerList.add(botHandler);
        handlerList.sort(this::compareSpecificity);
    }

    /**
     * Compares two exception handlers by the depth of their declared exception type in the
     * class hierarchy, ordering more-specific types before less-specific ones.
     *
     * @param a the first handler to compare
     * @param b the second handler to compare
     * @return a negative value if {@code a}'s exception type is more specific than {@code b}'s,
     *         a positive value if less specific, or zero if equally specific
     */
    private int compareSpecificity(
            BotExceptionMethodHandler<?> a,
            BotExceptionMethodHandler<?> b
    ) {
        int depthCompare = getDepth(b.getExceptionType()) - getDepth(a.getExceptionType());
        if (depthCompare != 0) return depthCompare;
        return Integer.compare(a.getPriority(), b.getPriority());
    }

    /**
     * Calculates the inheritance depth of a class by counting the number of superclass
     * links from the given type up to the root ({@code null}).
     *
     * <p>A higher depth indicates a more specific (concrete) type in the hierarchy.</p>
     *
     * @param type the class whose depth should be measured; {@code null} terminates traversal
     * @return the number of superclass hops from {@code type} to the root of the hierarchy
     */
    private int getDepth(Class<?> type) {
        int depth = 0;
        while (type != null) {
            depth++;
            type = type.getSuperclass();
        }
        return depth;
    }
}
