# Commandbus
*Tiny, lightweight java command bus.*

## Introduction

The main idea is to route Command to corresponding CommandHandler, but there is something more. The interesting part lies in three components, that allow simple yet powerful and flexible configuration.

* [Command handler](#command-handler)
* [Value provider](#value-provider)
* [Middleware](#middleware)

## Usage

#### Command handler

First and most important component is Command handler. Command handler is responsible for handling specific Command and there is one-to-one mapping. Which means, that there should be **exactly one** Command handler, otherwise exception is thrown.
Simplest Command handler would be:

```java
public class MyCommand {
    
}

public class CommandHandler {
    @Handler
    public Integer handle(MyCommand cmd) {
        dosmth...
        return 5;
    }
}
```
First and most imporant thing is @Handler annotation, which marks method as a Command handler. **First parameter is always Command to be handled and any @Handler method should have at least 1 parameter**.
That means, that when MyCommand will be executed, this exact method will handle it.
MyCommand is just a POJO, which means it could be anything, there is no special requirments for Command to be Command. Typically it just contains some data.
Command handler may or may not return some result. In this case it returns **Integer**, but it also could be **void**.

It is also possible to have more than one @Handler method in one class. In that case, two Command handlers would be registered.
```java
public class CommandHandler {
    @Handler
    public Integer handle(MyCommand cmd) {
        dosmth...
        return 5;
    }
    
    @Handler
    public void handle2(MyCommand2 cmd) {
        dosmth...
    }
}
```
What makes it interesting, is that Command handler can have additional parameters.

```java
public class CommandHandler {
    @Handler
    public void handle(MyCommand cmd, Integer a, MyParam c) {
        dosmth...
    }
}
```
In that case, **Integer a** and **MyParam c** will be provided by Value provider.

#### Value provider

Value provider is very similiar to Command handler in terms of definition.

```java
public class ValueProvider {
    @Provider
    public Integer a() {
        return 5;
    }
}
```
@Provider annotation marks method as Value provider. But there are few important differences.
@Provider method should have return type different from void.
@Provider method name is also important, because it is possible to have multiple @Provider that returns the same type, in that case name must be different, otherwise exception is thrown. i.e.

```java
public class CommandHandler {
    @Handler
    public void handle(MyCommand cmd, Integer a, Integer b) {
        dosmth...
    }
    
    public static class ValueProvider {
        @Provider
        public Integer a() {
            return 5;
        }
        
        @Provider
        public Integer b() {
            return 6;
        }
    }
}
```
In that case, Command Handler MyCommand and two Integers. In order to resolve it correctly, @Provider method name is used.

#### Middleware

Middleware is used to provide pre/post processing, command/result transormation or to simply ignore command.

```java
public class TimerMiddleware implements Middleware {
    @Override
    public <R> R execute(Object command, Function<Object, R> next) {
        Timer.start();
        R result = next.apply(command);
        Timer.stop();
        return result;
    }
}
```
Measuring execution time would be one of common tasks for middleware. It could also be: Starting transactional context, Logging, etc.


#### Put it all together
To make it all happen Bus should be builded. In order to do that BusBuilder is used.
**It is imporant in what order middleware is registered**.

```java

public class M1 implements Middleware {
@Override
    public <R> R execute(Object command, Function<Object, R> next) {
        System.out.println("1");
        R res = next.apply(command);
        System.out.println("2");
        return res;
    }
}

public class M2 implements Middleware {
    public <R> R execute(Object command, Function<Object, R> next) {
        System.out.println("3");
        R res = next.apply(command);
        System.out.println("4");
        return res;
    }
}

Bus bus = new BusBuilder()
    .registerCommandHandler(new CommandHandler())
    .registerCommandHandler(new CommandHandler2())
    .registerValueProvider(new ValueProvider())
    .registerMiddleware(new M1())
    .registerMiddleware(new M2())
    .build();
    
Object res = bus.execute(new MyCommand());
System.out.println("Wohoo result of command is " + res);
```
In that case it the output would be following: 1->3->4->2'

Bus is the actual worker here. It has only one method - execute(Object), which accepts Command. It will then synchronously execute this Command which means it will first execute Middleware chain, then find appropriate Command Handler, if Command Handler has additional params it will resolve them with the help of Value Provider and finally result will be returned.

#### Spring support
Since Spring Framework is so popular, to make Command Handler and Value Provider registration easier - first add spring dependency and then create such configuration.

```java
@Configuration
public class BusConfig {

    @Bean
    Bus commandBus(ApplicationContext context) {

        return new SpringBusBuilder(context)
                .registerCommandHandlers("your.base.package.with.command.handlers")
                .registerCommandHandlers("your.base.package2.with.command.handlers")
                .registerValueProviders("your.base.package.with.value.providers")
                .registerValueProviders("your.base.package2.with.value.providers")
                .registerMiddleware(new TMiddleware())
                .build();
```

In that case registerCommandHandler and registerValueProviders accepts String which is base package. It is scanned and all @Handler and @Provider are automatically registered.

