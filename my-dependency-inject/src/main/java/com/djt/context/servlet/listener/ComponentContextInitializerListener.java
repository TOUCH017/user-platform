package com.djt.context.servlet.listener;

import com.djt.context.ComponentContext;

import javax.servlet.*;
import javax.servlet.annotation.WebListener;

/**
 * @author djt
 * @date 2021/3/26
 */

public class ComponentContextInitializerListener implements ServletRequestListener {

    private ServletContext servletContext;



    @Override
    public void requestDestroyed(ServletRequestEvent sre) {

    }

    @Override
    public void requestInitialized(ServletRequestEvent sre) {

    }
}
