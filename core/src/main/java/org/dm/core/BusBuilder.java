package org.dm.core;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.String.format;
/**
 * Bus builder is responsible for <i>CommandHandlers/ValueProvider/Middleware</i> registration.
 *
 * <p> <i>Command:CommandHandler</i> is <b>oneToOne</b> mapping which means,
 * that there could be only one <em>CommandHandler</em> for Command.
 * If more than one <em>CommandHandler</em> is resolved for Command - exception is thrown.
 * CommandHandler is just a class, having one or more methods
 * annotated with {@link Handler}
 *
 * <p> It is possible to register more than one <em>ValueProvider</em>
 * of same type. In that case appropriate <em>ValueProvider</em> for CommandHandler's
 * param is resolved by <em>ValueProvider</em> method's name.
 * If more than one <em>ValueProvider</em> with same return type and name
 * is resolved - exception is thrown.
 * <em>ValueProvider</em> is just a class, having one or more methods
 * annotated with {@link Provider}
 *
 * <p><em>Middleware</em> should implement interface {@link Middleware}
 * <em>Middleware</em> are executed in order. So having
 * BusBuilder.registerMiddleware(m1).registerMiddleware(m2)
 * on <em>Command</em> execution there could be such sequence:
 * m1.pre -> m2.pre -> m2.post -> m1.post
 *
 *
 */
public class BusBuilder {

    private LinkedList<Middleware> middlewareList = new LinkedList<>();

    private List<Object> commandHandlerCandidates = new ArrayList<>();
    private List<Object> valueProviderCandidates = new ArrayList<>();

    public BusBuilder registerCommandHandler(Object commandHandler) {
        commandHandlerCandidates.add(commandHandler);
        return this;
    }


    public BusBuilder registerValueProvider(Object valueProvider) {
        valueProviderCandidates.add(valueProvider);
        return this;
    }

    public BusBuilder registerMiddleware(Middleware middleware) {
        middlewareList.addFirst(middleware);
        return this;
    }

    public Bus build() {
        return new Bus(middlewareList.stream()
                .reduce((Function<Object, Object>) new CommandExecutor(buildHandlersMapping(this.commandHandlerCandidates, buildProviderMapping(this.valueProviderCandidates))),
                        (f, m) -> command -> m.execute(command, f),
                        (f1, f2) -> f2));
    }

    private Map<Class, CommandHandler> buildHandlersMapping(List<Object> commandHandlerCandidates, Map<Class, Map<String, ValueProvider>> valueProvidersMapping) {
        Map<Class, CommandHandler> handlerMap = new HashMap<>();

        commandHandlerCandidates.stream()
                .flatMap(candidate -> CommandHandlerFactory.create(candidate, valueProvidersMapping).stream())
                .forEach(handlerTuple -> {
                    Class CommandClass = handlerTuple.getFirst();
                    CommandHandler commandHandler = handlerTuple.getSecond();
                    CommandHandler registeredCommandHandler = handlerMap.get(CommandClass);
                    if (registeredCommandHandler != null) {
                        throw new IllegalStateException(format("Trying to register Command Handler %s for command %s, but Command handler %s already registered for the same command",
                                commandHandler.getClass().getName(),
                                CommandClass.getName(),
                                registeredCommandHandler.getClass().getName()));
                    }

                    handlerMap.put(CommandClass, commandHandler);
                });

        return handlerMap;
    }

    private Map<Class, Map<String, ValueProvider>> buildProviderMapping(List<Object> valueProviderCandidates) {
        Map<Class, Map<String, ValueProvider>> providerMap = new HashMap<>();

        valueProviderCandidates.stream()
                .flatMap(candidate -> ValueProviderFactory.create(candidate).stream())
                .forEach(provider -> {
                    Class valueType = provider.providedValueDescription.type;
                    String valueName = provider.providedValueDescription.name;
                    ValueProvider registerdValueProvider = providerMap.computeIfAbsent(valueType, k -> new HashMap<>()).get(valueName);
                    if (registerdValueProvider != null) {
                        throw new IllegalStateException(format("Trying to register Value Provider %s for type %s and name %s, but such Value provider already registered %s",
                                provider.getClass().getName(),
                                valueType.getName(),
                                valueName,
                                registerdValueProvider.getClass().getName()));
                    }

                    providerMap
                            .get(valueType)
                            .put(valueName, provider);
                });

        return providerMap;
    }

    private static final class CommandExecutor implements Function<Object, Object> {
        private Map<Class, CommandHandler> commandHandlers;

        CommandExecutor(Map<Class, CommandHandler> commandHandlers) {
            this.commandHandlers = commandHandlers;
        }

        @Override
        public Object apply(Object command) {
            CommandHandler commandHandler = commandHandlers.get(command.getClass());
            if (commandHandler == null) {
                throw new IllegalStateException(format("Command handler for command %s not found.", command.getClass().getName()));
            }

            return commandHandler.invoke(command);
        }
    }

    static final class CommandHandler {
        private Object target;
        private Method method;
        private List<ValueProvider> providers;

        CommandHandler(Object target, Method method, List<ValueProvider> providers) {
            this.target = target;
            this.method = method;
            this.providers = providers;
        }

        Object invoke(Object cmd) {
            try {
                return method.invoke(target, buildParams(cmd));
            } catch (InvocationTargetException e) {
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                }
                throw new IllegalStateException(e);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }

        private Object[] buildParams(Object cmd) {
            return Stream.concat(Arrays.stream(new Object[]{cmd}), providers.stream().map(ValueProvider::invoke))
                    .toArray(Object[]::new);
        }

    }

    static final class TypeDescription {
        Class type;
        String name;

        TypeDescription(Class type, String name) {
            this.type = type;
            this.name = name;
        }
    }

    static final class ValueProvider {
        private Object target;
        private Method method;
        private TypeDescription providedValueDescription;

        ValueProvider(Object target, Method method, TypeDescription providedValueDescription) {
            this.target = target;
            this.method = method;
            this.providedValueDescription = providedValueDescription;
        }

        Object invoke() {
            try {
                return method.invoke(target);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Failed to execute value provider", e);
            }
        }

    }

}
