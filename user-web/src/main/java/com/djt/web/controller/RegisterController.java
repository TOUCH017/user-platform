package com.djt.web.controller;


import com.djt.context.annotation.Controller;
import com.djt.context.annotation.Value;
import com.djt.controller.PageController;
import com.djt.domain.User;
import com.djt.web.service.UserServiceImpl;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;


/**
 * @author djt
 * @date 2021/3/3
 */
@Controller
public class RegisterController implements PageController {


    @Resource(name="userServiceImpl")
    private UserServiceImpl userService;

    @Value(name="closeRegister")
    private  Boolean closeRegister;


    @GET
    @Path("/register")
    @Override
    public String execute(HttpServletRequest request, HttpServletResponse response) throws Throwable {
        if (closeRegister){
            request.setAttribute("failureMsg","注册系统已关闭");
            return "failure.jsp";
        }
        String name = request.getParameter("name");
        String phoneNumber = request.getParameter("phoneNumber");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        User user = new User();
        user.setName(name);
        user.setPhoneNumber(phoneNumber);
        user.setEmail(email);
        user.setPassword(password);
        user.setPhoneNumber(name);
        User register = userService.register(user);
        User loginUser = userService.queryUserById(register.getId());
        request.setAttribute("name",loginUser.getName());
        return "success.jsp" ;
    }

}
