package com.djt.context.support;

import com.djt.context.ComponentContext;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;

import java.io.IOException;
import java.util.List;

/**
 * @author djt
 * @date 2021/3/24
 */
public class ClassPathComponentReader extends ComponentReader {

    @Override
    public void instantiateComponents(ComponentContext componentContext) {
        ComponentScan componentScan = new ComponentScan();
        try {
            List<Class> classes = componentScan.doScan(getScanPackage(componentContext), componentContext.getClassLoader());
            classes.forEach(c->{
                try {
                    componentContext.getComponentsMap().put(getDefaultName(c), c.newInstance());
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getScanPackage(ComponentContext componentContext){
        ConfigProviderResolver configProviderResolver = componentContext.getConfigProviderResolver();
        Config config = configProviderResolver.getConfig(componentContext.getClassLoader());
        String value = config.getValue("scan-package", String.class);
        return value;
    }


    /**
     *
     * @param classType
     * @return
     */
    private String getDefaultName(Class classType){
        String simpleName = classType.getSimpleName();
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
