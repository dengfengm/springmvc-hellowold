package com.dengfeng.core;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dengfeng.annotation.DengFengController;
import com.dengfeng.annotation.DengFengRequestMapping;
import com.dengfeng.annotation.DengFengRequestParam;

@DengFengController
@DengFengRequestMapping("/test")
public class TestController {
 

 
	@DengFengRequestMapping("/doTest")
   public void test1(HttpServletRequest request, HttpServletResponse response,
       @DengFengRequestParam("param") String name){
     try {
           response.getWriter().write( "doTest method success! param:"+name);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
  
  
   @DengFengRequestMapping("/doTest2")
   public void test2(HttpServletRequest request, HttpServletResponse response){
       try {
           response.getWriter().println("doTest2 method success!");
       } catch (IOException e) {
           e.printStackTrace();
       }
   }
}

