package com.djt.component;

import com.djt.context.ComponentContext;
import com.djt.context.function.ThrowableAction;
import com.djt.context.function.ThrowableFunction;
import com.djt.context.support.ComponentReader;


import javax.naming.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author djt
 * @date 2021/3/24
 */
public class JNDIComponentReader extends ComponentReader {

    private static final Logger logger = Logger.getLogger(JNDIComponentReader.class.getName());

    private static final String COMPONENT_ENV_CONTEXT_NAME = "java:comp/env";

    private Context envContext;


    @Override
    public void instantiateComponents(ComponentContext componentContext ) {
        initEnvContext();
        List<String> componentNames = listAllComponentNames(componentContext.getClassLoader());
        componentNames.forEach(name -> componentContext.getComponentsMap().put(name, lookupComponent(name)));
    }

    public <C> C lookupComponent(String name) {
        return executeInContext(context -> (C) context.lookup(name));
    }


    private void initEnvContext() throws RuntimeException {
        if (this.envContext != null) {
            return;
        }
        Context context = null;
        try {
            context = new InitialContext();
            this.envContext = (Context) context.lookup(COMPONENT_ENV_CONTEXT_NAME);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        } finally {
            close(context);
        }
    }



    private List<String> listAllComponentNames(ClassLoader classLoader) {
        return listComponentNames("/",classLoader);
    }


    protected List<String> listComponentNames(String name,ClassLoader classLoader) {
        return executeInContext(context -> {
            NamingEnumeration<NameClassPair> e = executeInContext(context, ctx -> ctx.list(name), true);

            // 目录 - Context
            // 节点 -
            if (e == null) { // 当前 JNDI 名称下没有子节点
                return Collections.emptyList();
            }

            List<String> fullNames = new LinkedList<>();
            while (e.hasMoreElements()) {
                NameClassPair element = e.nextElement();
                String className = element.getClassName();
                Class<?> targetClass = classLoader.loadClass(className);
                if (Context.class.isAssignableFrom(targetClass)) {
                    // 如果当前名称是目录（Context 实现类）的话，递归查找
                    fullNames.addAll(listComponentNames(element.getName(),classLoader));
                } else {
                    // 否则，当前名称绑定目标类型的话话，添加该名称到集合中
                    String fullName = name.startsWith("/") ?
                            element.getName() : name + "/" + element.getName();
                    fullNames.add(fullName);
                }
            }
            return fullNames;
        });
    }


    /**
     * 在 Context 中执行，通过指定 ThrowableFunction 返回计算结果
     *
     * @param function ThrowableFunction
     * @param <R>      返回结果类型
     * @return 返回
     * @see ThrowableFunction#apply(Object)
     */
    protected <R> R executeInContext(ThrowableFunction<Context, R> function) {
        return executeInContext(function, false);
    }

    /**
     * 在 Context 中执行，通过指定 ThrowableFunction 返回计算结果
     *
     * @param function         ThrowableFunction
     * @param ignoredException 是否忽略异常
     * @param <R>              返回结果类型
     * @return 返回
     * @see ThrowableFunction#apply(Object)
     */
    protected <R> R executeInContext(ThrowableFunction<Context, R> function, boolean ignoredException) {
        return executeInContext(this.envContext, function, ignoredException);
    }



    private <R> R executeInContext(Context context, ThrowableFunction<Context, R> function,
                                   boolean ignoredException) {
        R result = null;
        try {
            result = ThrowableFunction.execute(context, function);
        } catch (Throwable e) {
            if (ignoredException) {
                logger.warning(e.getMessage());
            } else {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    private static void close(Context context) {
        if (context != null) {
            ThrowableAction.execute(context::close);
        }
    }
}
