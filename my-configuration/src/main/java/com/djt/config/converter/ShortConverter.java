package com.djt.config.converter;

public class ShortConverter extends AbstractConverter<Short> {

    @Override
    protected Short doConvert(String value) {
        return Short.valueOf(value);
    }
}
