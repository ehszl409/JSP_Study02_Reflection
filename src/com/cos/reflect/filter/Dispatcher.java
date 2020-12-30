package com.cos.reflect.filter;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Enumeration;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.cos.reflect.anno.RequestMapping;
import com.cos.reflect.controller.UserController;

public class Dispatcher implements Filter{
	
	private boolean isMaching = false;
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) req;
		HttpServletResponse response = (HttpServletResponse) resp;
		
		System.out.println("request.getContextPath(): " + request.getContextPath()); // 프로젝트 시작주소
		System.out.println("request.getRequestURI(): " + request.getRequestURI()); // 끝 주소
		System.out.println("request.getRequestURL(): " + request.getRequestURL()); // 전체 주소
		
		String endPoint = request.getRequestURI().replaceAll(request.getContextPath(), "");
		System.out.println("엔드포인트: " + endPoint);
		
		UserController userController = new UserController();
		Method[] methods = userController.getClass().getDeclaredMethods();
		for (Method method : methods) { // 4번 
			// .value를 담기 위해서.
			Annotation annotation = method.getDeclaredAnnotation(RequestMapping.class);
			RequestMapping requestMapping = (RequestMapping) annotation;
			System.out.println("requestMapping.value(): " + requestMapping.value());
			
			if(requestMapping.value().equals(endPoint)) {
				isMaching = true;
				try {
					// 메서드의 파라미터를 확인하는 코드 login(LoginDto dto)
					Parameter[] params = method.getParameters();
					String path = null;
					if(params.length != 0) { // 파람의 길이 login(LoginDto dto)
						System.out.println("params[0].getType(): " + params[0].getType());
						// /user/login => LoginDto, /user/join => JoinDto
						
						Object dtoInstance = params[0].getType().newInstance();
						String username = request.getParameter("username");
						String password = request.getParameter("password");
						String email = request.getParameter("email");
						System.out.println("username: " + username);
						System.out.println("password: " + password);
						System.out.println("email: " + email);
						
						setData(dtoInstance, request);
						
						path = (String) method.invoke(userController, dtoInstance);
//						Enumeration<String> keys = request.getParameterNames();
//						//keys 값을 변형 username -> 
//						
//						while(keys.hasMoreElements()) {
//							System.out.println("keys.nextElement(): " + keys.nextElement());
//						}
//						path = "/";
					} else {
						path = (String) method.invoke(userController);
					}
					RequestDispatcher dis = request.getRequestDispatcher(path);
					dis.forward(request, response);
					
				} catch (Exception e) {
					e.printStackTrace();
				} 
				break;
				
			}
		}
		
		if(isMaching == false) {
			response.setContentType("text/heml; charset = UTF-8");
			PrintWriter out = response.getWriter();
			out.println("잘못된 주소 요청입니다. 404에러");
			out.flush();
		}
		
	}
	
	private <T> void setData(T instance, HttpServletRequest request) {
		Enumeration<String> keys = request.getParameterNames();
		while(keys.hasMoreElements()) {
			String key = keys.nextElement();
			String methodkey = keyToMethodKey(key);
			
			// 들어온 instance(JoinDto, LoginDto 등등)
			Method[] methods = instance.getClass().getDeclaredMethods();
			
			for (Method method : methods) {
				if(method.getName().equals(methodkey)){
					try {
						method.invoke(instance, request.getParameter(key));
					} catch (Exception e) {
						e.printStackTrace();
					} 
				}
			}
		}
	}
	
	private String keyToMethodKey(String key) {
		String firstKey = "set";
		String upperKey = key.substring(0, 1).toUpperCase();
		String remainKey = key.substring(1);
		return firstKey+upperKey+remainKey;
	}
}
