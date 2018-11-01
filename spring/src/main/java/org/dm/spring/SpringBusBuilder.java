package org.dm.spring;

import org.dm.core.*;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;

public class SpringBusBuilder {

    private BusBuilder busBuilder = new BusBuilder();
    private ApplicationContext applicationContext;

    public SpringBusBuilder(ApplicationContext context) {
        this.applicationContext = context;
    }

    public SpringBusBuilder registerCommandHandlers(String basePackage) {
        find(basePackage, Handler.class).forEach(busBuilder::registerCommandHandler);
        return this;
    }

    public SpringBusBuilder registerValueProviders(String basePackage) {
        find(basePackage, Provider.class).forEach(busBuilder::registerValueProvider);
        return this;
    }

    public SpringBusBuilder registerMiddleware(Middleware middleware) {
        busBuilder.registerMiddleware(middleware);
        return this;
    }

    public Bus build() {
        return busBuilder.build();
    }

    private List<Object> find(String basePackage, Class<? extends Annotation> annotationClass) {
        return new Reflections(basePackage, new MethodAnnotationsScanner())
                .getMethodsAnnotatedWith(annotationClass).stream()
                .map(Method::getDeclaringClass)
                .distinct()
                .map(this::findBean)
                .filter(Objects::nonNull)
                .collect(toList());
    }

    private Object findBean(Class beanClass) {
        try {
            //noinspection unchecked
            return applicationContext.getBean(beanClass);
        } catch (Exception e) {
            return null;
        }
    }
}
