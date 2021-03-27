package com.djt.context.support;

import com.djt.context.annotation.Component;
import com.djt.context.annotation.Controller;
import com.djt.context.annotation.Service;
import com.djt.context.annotation.Value;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author djt
 * @date 2021/3/23
 */
public class ComponentScan {

    private static Logger logger =Logger.getLogger(ComponentScan.class.getName());
    private static final Set<Class<? extends Annotation>> annotationList=new HashSet<>();

    static {
        annotationList.add(Controller.class);
        annotationList.add(Service.class);
        annotationList.add(Value.class);
        annotationList.add(Component.class);
    }

    public  List<Class>  doScan(String packageName,ClassLoader classLoader) throws IOException {
        if (packageName ==null || packageName ==""){
            throw new IllegalArgumentException("package can not be null");
        }
        List<Class> classes = new ArrayList<>();
        if (classLoader == null){
            classLoader=Thread.currentThread().getContextClassLoader();
        }
        List<Class<?>> candidateClassList = getClassesWithAnnotationFromPackage(packageName,classLoader);
        for (Class<?> candidateClass : candidateClassList) {
            Annotation[] annotations = candidateClass.getAnnotations();
            for (Annotation annotation : annotations) {
                if (annotationList.contains(annotation.annotationType())){
                    classes.add(candidateClass);
                }
            }
        }
        return classes;
    }

    /**
     *  根据包名扫描路径
     * @param packageName
     * @param classLoader
     * @return
     * @throws IOException
     */
    List<Class<?>> getClassesWithAnnotationFromPackage(String packageName,ClassLoader classLoader) throws IOException {
        String path = convertPackagePath(packageName);
        Enumeration<URL> resources=null;
        List<Class<?>> classes = new ArrayList<>();
        try {
             resources = classLoader.getResources(path);
        } catch (IOException e) {
            logger.log(Level.SEVERE,"扫描包目录发生异常",e);
            return null;
        }
        while (resources.hasMoreElements()){
            URL url = resources.nextElement();
            String protocol = url.getProtocol();
            if ("file".equals(protocol)){
                String filePath = null;
                try {
                    filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    logger.log(Level.SEVERE,"文件路径使用UTF-8编码失败",e);
                }
                filePath = filePath.substring(1);
                getClassesWithAnnotationFromFilePath(packageName,filePath,classes,classLoader);
            }else if ("jar".equals(protocol)){
                JarFile jar = null;
                try {
                    jar = ((JarURLConnection) url.openConnection()).getJarFile();
                    //扫描jar包文件 并添加到集合中
                }
                catch (Exception e) {
                    logger.log(Level.SEVERE,"Failed to decode class jar", e);
                }
                findClassesByJar(packageName,jar,classes,classLoader);
            }
        }
        return classes;

    }

    /**
     * 根据文件路径加载class
     * @param packageName
     * @param filePath
     * @param classes
     * @param classLoader
     * @throws IOException
     */
    public void getClassesWithAnnotationFromFilePath(String packageName,String filePath,List<Class<?>> classes,ClassLoader classLoader ) throws IOException {
        Path dir = Paths.get(filePath);
        try(DirectoryStream<Path> stream=  Files.newDirectoryStream(dir)){
            for (Path path : stream) {
                String fileName = String.valueOf(path.getFileName());
                if (Files.isDirectory(path)){
                    getClassesWithAnnotationFromFilePath(packageName + "." + fileName , path.toString(), classes,classLoader);
                }else{
                    String className = fileName.substring(0, fileName.length() - 6);
                    Class<?> targetClass = null;
                    String fullClassPath = packageName + "." + className;
                    try {
                        targetClass = classLoader.loadClass(fullClassPath);
                    }catch (ClassNotFoundException e){
                        logger.log(Level.SEVERE,fullClassPath+ "not found");
                    }
                    if (targetClass !=null){
                        classes.add(targetClass);
                    }
                }
            }
        }
    }

    /**
     * 扫描jar包下的
     * @param pkgName
     * @param jar
     * @param classes
     */
    public void findClassesByJar(String pkgName, JarFile jar, List<Class<?>> classes,ClassLoader classLoader){
        String pkgDir = pkgName.replace(".", "/");
        Enumeration<JarEntry> entry = jar.entries();
        while (entry.hasMoreElements()) {
            // 获取jar里的一个实体 可以是目录 和一些jar包里的其他文件 如META-INF等文
            JarEntry jarEntry = entry.nextElement();
            String name = jarEntry.getName();
            // 如果是以/开头的
            if (name.charAt(0) == '/') {
                // 获取后面的字符串
                name = name.substring(1);
            }

            if (jarEntry.isDirectory() || !name.startsWith(pkgDir) || !name.endsWith(".class")) {
                continue;
            }
            //如果是一个.class文件 而且不是目录
            // 去掉后面的".class" 获取真正的类名
            String className = name.substring(0, name.length() - 6);
            Class<?> tempClass = loadClass(className.replace("/", "."),classLoader);
            // 添加到集合中去
            if (tempClass != null) {
                classes.add(tempClass);
            }
        }
    }


    /**
     * 包路径转换位文件路径
     * @param packagePath
     * @return
     */
    public String convertPackagePath(String packagePath){
        return  packagePath.replace(".","/");
    }

    /**
     * 加载类
     * @param fullClsName 类全名
     * @return
     */
    private static Class<?> loadClass(String fullClsName,ClassLoader classLoader ) {
        try {
            return classLoader.loadClass(fullClsName );
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE,"PkgClsPath loadClass", e);
        }
        return null;
    }

}
