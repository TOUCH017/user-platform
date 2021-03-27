package com.djt.context.servlet;

import com.djt.context.ComponentContext;
import com.djt.context.servlet.listener.ComponentContextInitializerListener;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;
import java.lang.reflect.Modifier;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author djt
 * @date 2021/3/26
 */
@HandlesTypes(WebApplicationInitializer.class)
public class ComponentContextInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> webAppInitializerClasses, ServletContext servletContext) throws ServletException {
        ComponentContext context = new ComponentContext();
        context.init(servletContext);

        List<WebApplicationInitializer> initializers = new LinkedList<>();
        if (webAppInitializerClasses != null) {
            for (Class<?> waiClass : webAppInitializerClasses) {
                // Be defensive: Some servlet containers provide us with invalid classes,
                // no matter what @HandlesTypes says...
                if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&
                        WebApplicationInitializer.class.isAssignableFrom(waiClass)) {
                    try {
                        initializers.add((WebApplicationInitializer)
                                waiClass.newInstance());
                    }
                    catch (Throwable ex) {
                        throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);
                    }
                }
            }
        }

        if (initializers.isEmpty()) {
            servletContext.log("No  WebApplicationInitializer types detected on classpath");
            return;
        }

        servletContext.log(initializers.size() + "  WebApplicationInitializers detected on classpath");
        for (WebApplicationInitializer initializer : initializers) {
            initializer.onStartup(servletContext);
        }
    }



}
