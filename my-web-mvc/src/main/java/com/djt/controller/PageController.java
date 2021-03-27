package com.djt.controller;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface PageController extends MyController {

    /**
     * @param request  HTTP 请求
     * @param response HTTP 相应
     * @return 视图地址路径
     * @throws Throwable 异常发生时
     */
    String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable;
}
