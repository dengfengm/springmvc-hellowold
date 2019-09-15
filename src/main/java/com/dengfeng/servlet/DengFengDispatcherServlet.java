package com.dengfeng.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dengfeng.annotation.DengFengController;
import com.dengfeng.annotation.DengFengRequestMapping;
import com.dengfeng.annotation.DengFengRequestParam;

/**
 * Servlet implementation class DengFengDispatcherServlet
 */
public class DengFengDispatcherServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	
	private Properties properties = new Properties();
	 
	 private List<String> classNames = new ArrayList<>();
	 
	 private Map<String, Object> ioc = new HashMap<>();
	 
	 private Map<String, Method> handlerMapping = new  HashMap<>();
	 
	 private Map<String, Object> controllerMap  =new HashMap<>();
    /**
     * @see HttpServlet#HttpServlet()
     */
    public DengFengDispatcherServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    	   //1.加载配置文件
    	   doLoadConfig(config.getInitParameter("dengfengContextLocation"));
    	   
    	   //2.初始化所有相关联的类,扫描用户设定的包下面所有的类
    	   doScanner(properties.getProperty("scanPackage"));
    	   
    	   //3.拿到扫描到的类,通过反射机制,实例化,并且放到ioc容器中(k-v  beanName-bean) beanName默认是首字母小写
    	    doInstance();
    	   
    	   //4.初始化HandlerMapping(将url和method对应上)
    	   initHandlerMapping();
    	   
    	   
    	 }
    
	private void initHandlerMapping() {
		if(ioc.isEmpty()){
		     return;
		   }
		try{
			for(Entry<String,Object> entry:ioc.entrySet()){
				Class<? extends Object> clazz = entry.getValue().getClass();
				if(!clazz.isAnnotationPresent(DengFengController.class)){
					continue;
				}
				//拼url时,是controller头的url拼上方法上的url
			    String baseUrl ="";
				if(clazz.isAnnotationPresent(DengFengRequestMapping.class)){
					DengFengRequestMapping annnotation = clazz.getAnnotation(DengFengRequestMapping.class);
					baseUrl=annnotation.value();
				}
				
				Method[] methods = clazz.getMethods();
				for(Method method:methods){
					if(method.isAnnotationPresent(DengFengRequestMapping.class)){
						DengFengRequestMapping annotation = method.getAnnotation(DengFengRequestMapping.class);
						String url = annotation.value();
				         url =(baseUrl+"/"+url).replaceAll("/+", "/");
				         handlerMapping.put(url,method);
				         controllerMap.put(url,clazz.newInstance());
					}
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		
	}

	private void doInstance() {
		if(classNames.isEmpty())return;
		for(String className:classNames){
			try {
				//把类搞出来,反射来实例化(只有加@DengFengController需要实例化)
				Class<?> clazz = Class.forName(className);
				if(clazz.isAnnotationPresent(DengFengController.class)){
					ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
				}
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private String toLowerFirstWord(String name) {
		char[] charArray = name.toCharArray();
		   charArray[0] += 32;
		   return String.valueOf(charArray);
	}

	private void doScanner(String packageName) {
		//把所有的.替换成/
		   URL url  =this.getClass().getClassLoader().getResource("/"+packageName.replaceAll("\\.", "/"));
		   File dir = new File(url.getFile());
		   if(!dir.exists()){
			   return;
		   }
		   for (File file : dir.listFiles()) {
			     if(file.isDirectory()){
			       //递归读取包
			       doScanner(packageName+"."+file.getName());
			     }else{
			       String className =packageName +"." +file.getName().replace(".class", "");
			       classNames.add(className);
			     }
			   }
		   
	}

	private void doLoadConfig(String location) {
			//把web.xml中的contextConfigLocation对应value值的文件加载到流里面
		   InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
		    try {
		     //用Properties文件加载文件里的内容
		     properties.load(resourceAsStream);
		   } catch (IOException e) {
		     e.printStackTrace();
		   }finally {
		     //关流
		     if(null!=resourceAsStream){
		       try {
		         resourceAsStream.close();
		       } catch (IOException e) {
		         e.printStackTrace();
		       }
		     }
		   }		
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doPost(request,response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.doDispatch(request,response);
	}

	private void doDispatch(HttpServletRequest request, HttpServletResponse response) {
		if(handlerMapping.isEmpty()){
		     return;
		}
		
		String url = request.getRequestURI();
		String contextPath = request.getContextPath();
		url=url.replace(contextPath, "").replaceAll("/+", "/");
		
		
		try {
			if(!this.handlerMapping.containsKey(url)){
				response.getWriter().write("404 NOT FOUND!");
			     return;
			}
			Method method = handlerMapping.get(url);
			
			Class<?>[] parameterTypes =method.getParameterTypes();
			Parameter[] parameters = method.getParameters();
			//获取请求的参数
			   Map<String, String[]> parameterMap = request.getParameterMap();
			   
			   //保存参数值
			   Object [] paramValues= new Object[parameterTypes.length];
			   
			   //方法的参数列表
			       for (int i = 0; i<parameterTypes.length; i++){  
			           //根据参数名称，做某些处理  
			           String requestParam = parameterTypes[i].getSimpleName();  
			           String paramName = "";
			           if(parameters[i].isAnnotationPresent(DengFengRequestParam.class)){
//			        	   DengFengRequestParam[] dengs = parameters[i].getDeclaredAnnotationsByType(DengFengRequestParam.class);
			        	   DengFengRequestParam deng = parameters[i].getAnnotation(DengFengRequestParam.class);
//			        	   for(DengFengRequestParam deng :dengs){
			        		   paramName = deng.value();
//			        	  }
			           }
			           if (requestParam.equals("HttpServletRequest")){  
			               //参数类型已明确，这边强转类型  
			             paramValues[i]=request;
			               continue;  
			           }  
			           if (requestParam.equals("HttpServletResponse")){  
			             paramValues[i]=response;
			               continue;  
			           }
			           if(requestParam.equals("String")){
			             for (Entry<String, String[]> param : parameterMap.entrySet()) {
			            	if(param.getKey().equals(paramName)){
			            		String value =Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
					               paramValues[i]=value;
					               System.out.println("annotation");
			            	}
			            	
			            	if(param.getKey().equals(parameters[i].getName())){
			            		String value =Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
					             paramValues[i]=value;
					             System.out.println("paramter");
			            	}
			            }
			           }
			       } 
			method.invoke(controllerMap.get(url), paramValues);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
