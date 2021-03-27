package com.djt.servlet;

import com.djt.base.FrontControllerServlet;
import com.djt.context.servlet.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
 * @author djt
 * @date 2021/3/27
 */
public class MyMvcServletInitializer implements WebApplicationInitializer {
    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.addServlet("front",FrontControllerServlet.class);
    }
}
