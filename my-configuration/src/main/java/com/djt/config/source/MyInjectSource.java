package com.djt.config.source;

import com.djt.config.source.MapBasedConfigSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.Properties;

/**
 * @author djt
 * @date 2021/3/27
 */
public class MyInjectSource extends MapBasedConfigSource {

    public MyInjectSource() {
        super("Java System Properties", 700);
    }

    @Override
    protected void prepareConfigData(Map configData) throws Throwable {
        InputStream in = getClass().getClassLoader().getResourceAsStream("META-INF/my-inject.properties");
        Properties properties = new Properties();
        properties.load(new InputStreamReader(in,"UTF-8"));
        configData.putAll(properties);
    }
}
