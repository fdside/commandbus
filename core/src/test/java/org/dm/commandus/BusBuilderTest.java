package org.dm.commandus;

import org.dm.core.*;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.function.Function;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class BusBuilderTest {

    @Test
    public void handlerWithoutAdditionalParams() {
        assertThat(5, equalTo(new BusBuilder()
                .registerCommandHandler(new CommandHandler())
                .build()
                .execute(new Command())));
    }

    @Test
    public void allHandlerMethodsAreRegistered() {
        Bus bus = new BusBuilder()
                .registerCommandHandler(new CommandHandlerWith2Handlers())
                .build();
        assertThat(5, equalTo(bus.execute(new Command())));
        assertThat(6, equalTo(bus.execute(new Command2())));
    }

    @Test(expected = IllegalStateException.class)
    public void errorIfMoreThanOneCommandHandlerForCommandFound() {
        new BusBuilder()
                .registerCommandHandler(new CommandHandler())
                .registerCommandHandler(new CommandHandler())
                .build()
                .execute(new Command());
    }

    @Test
    public void handlerWithOneAdditionalParamResolvedByType() {
        assertThat(6, equalTo(new BusBuilder()
                .registerCommandHandler(new CommandHandlerWithOneParam())
                .registerValueProvider(new IIntegerValueProvider())
                .build()
                .execute(new Command())));
    }

    @Test
    public void handlerWithOneAdditionalParamResolvedByTypeAndName() {
        assertThat(7, equalTo(new BusBuilder()
                .registerCommandHandler(new CommandHandlerWithOneParam())
                .registerValueProvider(new JIntegerValueProvider())
                .build()
                .execute(new Command())));
    }

    @Test
    public void handlerWithTwoAdditionalParamResolvedByTypeAndName() {
        assertThat(8, equalTo(new BusBuilder()
                .registerCommandHandler(new CommandHandlerWithTwoParam())
                .registerValueProvider(new JIntegerValueProvider())
                .build()
                .execute(new Command())));
    }

    @Test
    public void middleWareExecutedInCorrectOrder() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        new BusBuilder()
                .registerMiddleware(new MW1())
                .registerMiddleware(new MW2())
                .registerCommandHandler(new CommandHandler())
                .build()
                .execute(new Command());

        assertThat("1342", equalTo(baos.toString()));
    }

    @Test(expected = IllegalStateException.class)
    public void errorIfCommandWithoutCommandHandler() {
        new BusBuilder()
                .build()
                .execute(new Command());
    }

    @Test(expected = IllegalStateException.class)
    public void errorIfValueProviderForCommandHandlerNotFound() {
        new BusBuilder()
                .registerCommandHandler(new CommandHandlerWithOneParam())
                .build()
                .execute(new Command());
    }

    public static final class CommandHandler {
        @Handler
        public Integer handle(Command c) {
            return 5;
        }
    }

    public static final class CommandHandlerWith2Handlers {
        @Handler
        public Integer handle(Command c) {
            return 5;
        }

        @Handler
        public Integer handle(Command2 c) {
            return 6;
        }
    }

    public static final class CommandHandlerWithOneParam {
        @Handler
        public Integer handle(Command c, Integer i) {
            return 5 + i;
        }
    }

    public static final class CommandHandlerWithTwoParam {
        @Handler
        public Integer handle(Command c, Integer j, Integer i) {
            return 5 + i + j;
        }
    }

    public static final class IIntegerValueProvider {
        @Provider
        public Integer i() {
            return 1;
        }
    }

    public static final class JIntegerValueProvider {
        @Provider
        public Integer i() {
            return 2;
        }

        @Provider
        public Integer j() {
            return 1;
        }
    }

    private static final class Command {
    }

    private static final class Command2 {
    }

    public static final class MW1 implements Middleware {

        @Override
        public <R> R execute(Object command, Function<Object, R> next) {
            System.out.print(1);
            R result = next.apply(command);
            System.out.print(2);

            return result;
        }
    }

    public static final class MW2 implements Middleware {

        @Override
        public <R> R execute(Object command, Function<Object, R> next) {
            System.out.print(3);
            R result = next.apply(command);
            System.out.print(4);

            return result;
        }
    }
}
