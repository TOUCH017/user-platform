package com.djt.context;

import com.djt.context.annotation.Value;
import com.djt.context.support.ComponentReader;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigBuilder;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.servlet.ServletContext;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * @author djt
 * @date 2021/3/23
 */
public  class ComponentContext {


    public static final String CONTEXT_NAME = ComponentContext.class.getName();

    private static ServletContext servletContext;

    private ClassLoader classLoader;

    private ConfigProviderResolver configProviderResolver;

    private Map<String, Object> componentsMap = new LinkedHashMap<>();

    public void init(ServletContext servletContext){
        ComponentContext.servletContext = servletContext;
        servletContext.setAttribute(CONTEXT_NAME, this);
        this.classLoader = servletContext.getClassLoader();
        initConfigProviderResolver();
        initinstantiateComponents();
        initializeComponents();

    }


    public void initinstantiateComponents(){
        ServiceLoader<ComponentReader> load = ServiceLoader.load(ComponentReader.class, Thread.currentThread().getContextClassLoader());
        Iterator<ComponentReader> iterator = load.iterator();
        while (iterator.hasNext()){
            ComponentReader componentReader = iterator.next();
            componentReader.instantiateComponents(this);
        }
    }

    public static ComponentContext getInstance() {
        return (ComponentContext) servletContext.getAttribute(CONTEXT_NAME);
    }

    protected void initializeComponents() {
        componentsMap.values().forEach(component -> {
            Class<?> componentClass = component.getClass();
            // 注入阶段 - {@link Resource}
            injectComponents(component, componentClass);
            // 初始阶段 - {@link PostConstruct}
            processPostConstruct(component, componentClass);
            // TODO 实现销毁阶段 - {@link PreDestroy}
            processPreDestroy();
        });
    }


    private void injectComponents(Object component, Class<?> componentClass) {
        Stream.of(componentClass.getDeclaredFields())
                .filter(field -> {
                    int mods = field.getModifiers();
                    return !Modifier.isStatic(mods);
                }).forEach(field -> {
            Object injectedObject =null;
            if (field.isAnnotationPresent(Resource.class)){
                Resource resource = field.getAnnotation(Resource.class);
                String resourceName = resource.name();
                injectedObject = lookupComponent(resourceName);
            }else if (field.isAnnotationPresent(Value.class)){
                Value value = field.getAnnotation(Value.class);
                String name = value.name();
                Config config = configProviderResolver.getConfig(this.getClassLoader());
                injectedObject = config.getValue(name, field.getType());
            }else{
                return;
            }
            field.setAccessible(true);
            try {
                // 注入目标对象
                field.set(component, injectedObject);
            } catch (IllegalAccessException e) {
            }
        });
    }



    private void initConfigProviderResolver(){
        // 获取当前 ClassLoader
        ConfigProviderResolver configProviderResolver = ConfigProviderResolver.instance();
        ConfigBuilder configBuilder = configProviderResolver.getBuilder();
        // 配置 ClassLoader
        configBuilder.forClassLoader(this.classLoader);
        configBuilder.addDefaultSources();
        configBuilder.addDiscoveredConverters();
        // 获取 Config
        Config config = configBuilder.build();
        // 注册 Config 关联到当前 ClassLoader
        configProviderResolver.registerConfig(config, classLoader);
        this.configProviderResolver=configProviderResolver;
    }

    private void processPostConstruct(Object component, Class<?> componentClass) {
        Stream.of(componentClass.getMethods())
                .filter(method ->
                        !Modifier.isStatic(method.getModifiers()) &&      // 非 static
                                method.getParameterCount() == 0 &&        // 没有参数
                                method.isAnnotationPresent(PostConstruct.class) // 标注 @PostConstruct
                ).forEach(method -> {
            // 执行目标方法
            try {
                method.invoke(component);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void processPreDestroy() {
        // TODO: 通过 ShutdownHook 实现
        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            // 逐一调用这
            componentsMap.values();
        }));
    }

    public Object lookupComponent(String resourceName){
        return    componentsMap.get(resourceName);
    }




    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public Map<String, Object> getComponentsMap() {
        return componentsMap;
    }

    public ConfigProviderResolver getConfigProviderResolver() {
        return configProviderResolver;
    }
}
