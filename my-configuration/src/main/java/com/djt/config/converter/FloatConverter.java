package com.djt.config.converter;

public class FloatConverter extends AbstractConverter<Float> {

    @Override
    protected Float doConvert(String value) {
        return Float.valueOf(value);
    }
}
