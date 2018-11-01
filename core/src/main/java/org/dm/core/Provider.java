package org.dm.core;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;

/**
 * Is used to mark method as <em>ValueProvider</em>
 * which then can be registered in BusBuilder with {@link BusBuilder#registerValueProvider(Object)}
 *
 * <p> Each method annotated with {@link Provider}
 * <b>should have exactly zero parameters</b>.
 * Return type and method name makes combination
 * that is used to find out, which <em>ValueProvider</em>
 * should be called to resolve <em>CommandHandler</em> params.
 *
 * <p>It is possible for a class to have more than one method
 * annotated with {@link Handler}. But the combination of
 * method return type and method name should be unique.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(value = METHOD)
public @interface Provider {

    String name() default "value";

}
