package com.djt.context.annotation;

import java.lang.annotation.*;

/**
 * @author djt
 * @date 2021/3/24
 */

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Component {

}
