package com.djt.base;

import com.djt.context.ComponentContext;
import com.djt.context.annotation.Controller;
import com.djt.controller.MyController;
import com.djt.controller.PageController;
import com.djt.controller.RestController;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Resource;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import static org.apache.commons.lang.StringUtils.substringAfter;

import static java.util.Arrays.asList;


/**
 * @author djt
 * @date 2021/3/3
 */
public class FrontControllerServlet extends HttpServlet {

    private Map<String, HandlerMethodInfo> handleMethodInfoMapping = new HashMap<>();

    private Map<String, MyController> controllersMapping = new HashMap<>();


    @Override
    public void init(ServletConfig servletConfig){
        ServletContext servletContext = servletConfig.getServletContext();
        initHandleMethods(servletContext);
    }


    private void initHandleMethods( ServletContext servletContext){
        ServiceLoader<MyController> load = ServiceLoader.load(MyController.class);
        for (MyController myController : load) {
            Class<? extends MyController> controllerClass = myController.getClass();
            Path pathAnnotation = controllerClass.getAnnotation(Path.class);
            Controller controller = controllerClass.getAnnotation(Controller.class);
            if (controller == null){
                return;
            }
            ComponentContext componentContext =(ComponentContext) servletContext.getAttribute(ComponentContext.class.getName());
            MyController myControllerComponent = (MyController)componentContext.lookupComponent(getDefaultName(controllerClass));
            String prefixRequestPath="";

            if (pathAnnotation != null){
                prefixRequestPath += pathAnnotation.value();
            }

            Method[] publicMethods = controllerClass.getMethods();
            for (Method method : publicMethods) {
                Set<String> supportedHttpMethods = findSupportedHttpMethods(method);
                Path methodPathAnnotation = method.getAnnotation(Path.class);
                if (methodPathAnnotation != null){
                   String methodRequestPath=prefixRequestPath+methodPathAnnotation.value();
                    handleMethodInfoMapping.put(methodRequestPath,
                            new HandlerMethodInfo(methodRequestPath, method, supportedHttpMethods));
                    controllersMapping.put(methodRequestPath, myControllerComponent);
                }
            }
        }
    }


    private Set<String> findSupportedHttpMethods(Method method){
        Set<String> supportedHttpMethods = new HashSet<>();
        Annotation[] methodAnnotations = method.getAnnotations();
        for (Annotation annotation : methodAnnotations) {
            HttpMethod httpMethod = annotation.annotationType().getAnnotation(HttpMethod.class);
            if (httpMethod != null){
                supportedHttpMethods.add(httpMethod.value());
            }
        }
        if (supportedHttpMethods.isEmpty()) {
            supportedHttpMethods.addAll(asList(HttpMethod.GET, HttpMethod.POST,
                    HttpMethod.PUT, HttpMethod.DELETE, HttpMethod.HEAD, HttpMethod.OPTIONS));
        }
        return supportedHttpMethods;
    }

    @Override
    public void service(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String requestURI = request.getRequestURI();
        String servletContextPath = request.getContextPath();
        String prefixPath = servletContextPath;
        String requestMappingPath = substringAfter(requestURI,
                StringUtils.replace(prefixPath, "//", "/"));
        MyController myController = controllersMapping.get(requestMappingPath);

        if (myController != null){

            HandlerMethodInfo handlerMethodInfo = handleMethodInfoMapping.get(requestMappingPath);

            try{
                if (handlerMethodInfo != null){

                    Set<String> supportedHttpMethods = handlerMethodInfo.getSupportedHttpMethods();
                    String httpMethod = request.getMethod();
                    Method handlerMethod = handlerMethodInfo.getHandlerMethod();

                    if (!supportedHttpMethods.contains(httpMethod)){
                        // HTTP 方法不支持
                        response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                        return;
                    }

                    if (myController instanceof PageController) {

                        PageController pageController = PageController.class.cast(myController);
                        String viewPath = pageController.execute(request, response);
                        ServletContext servletContext = request.getServletContext();

                        if (!viewPath.startsWith("/")) {
                            viewPath = "/" + viewPath;
                        }

                        RequestDispatcher requestDispatcher = servletContext.getRequestDispatcher(viewPath);
                        requestDispatcher.forward(request, response);

                        return;
                    }else if (myController instanceof RestController){
                        handlerMethod.invoke(myController,request,response);
                    }
                }
            } catch (Throwable throwable) {
                if (throwable.getCause() instanceof IOException) {
                    throw (IOException) throwable.getCause();
                } else {
                    throw new ServletException(throwable.getCause());
                }
            }

        }

    }

    private String getDefaultName(Class classType){
        String simpleName = classType.getSimpleName();
        char [] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

}
