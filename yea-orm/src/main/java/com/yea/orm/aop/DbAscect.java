/**
 * Copyright 2017 伊永飞
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yea.orm.aop;

//import java.lang.reflect.Method;

//import org.aspectj.lang.ProceedingJoinPoint;
//import org.aspectj.lang.annotation.Around;
//import org.aspectj.lang.annotation.Aspect;
//import org.aspectj.lang.annotation.Pointcut;
//import org.springframework.context.ApplicationContext;
//
//import com.yea.orm.handle.dto.ORMParams;

//@Aspect
public class DbAscect {
	public DbAscect() {}
	//缓存切点  
//    @Pointcut("@annotation(com.ea.core.cache.annotation.Cache)")  
	public void executePointcut() {}
  
    /** 
     * 前置通知 用于拦截DB操作 
     * 
     * @param joinPoint 切点 
     * @throws Throwable 
     */  
//	@Around("executePointcut()")
//	public Object doAround(ProceedingJoinPoint joinPoint) throws Throwable {
//		System.out.println("abcde");
//		Object[] params = joinPoint.getArgs();
//		
//		if(params[0] instanceof ORMParamsDTO) {
//			ORMParamsDTO ormParamsDTO = (ORMParamsDTO)params[0];
//			String cacheKey = ormParamsDTO.generatorCacheKey();
//			if(cacheKey != null){
//				String signatureName = joinPoint.getSignature().getName();
//				Object target = joinPoint.getTarget();
//				Cache cache = null;
//				for (Method method : target.getClass().getDeclaredMethods()) {
//					if (signatureName.equals(method.getName())) {
//						cache = method.getAnnotation(Cache.class);
//						break;
//					}
//				}
//				ApplicationContext context = null;
//				if(joinPoint.getTarget() instanceof ApplicationContextAware) {
//					context = ((ApplicationContextAware)joinPoint.getTarget()).getApplicationContext();
//				}
//				if(cache != null && context != null){
//					CacheContext cacheContext = (CacheContext) context.getBean("cacheContext");
//					CacheConstants.CommandType command = cache.value();
//					if(CacheConstants.CommandType.GET.equals(command)){
//						Object value = cacheContext.get(cacheKey);
//						if(value != null) {
//							return value;
//						} else {
//							return joinPoint.proceed();
//						}
//					} else {
//						if(CacheConstants.CommandType.SET.equals(command)){
//							cacheContext.set(cacheKey, ormParamsDTO.getParam(), 0);
//						}
//						if (CacheConstants.CommandType.DEL.equals(command)) {
//							cacheContext.delete(cacheKey);
//						}
//						return joinPoint.proceed();
//					}
//				} else {
//					return joinPoint.proceed();
//				}
//			} else {
//				return joinPoint.proceed();
//			}
//		} else {
//			return joinPoint.proceed();
//		}
//	}
//  
    /** 
     * 异常通知 用于拦截service层记录异常日志 
     * 
     * @param joinPoint 
     * @param e 
     */  
//    @AfterThrowing(pointcut = "serviceAspect()", throwing = "e")  
//     public  void doAfterThrowing(JoinPoint joinPoint, Throwable e) {  
//        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();  
//        HttpSession session = request.getSession();  
//        //读取session中的用户  
//        User user = (User) session.getAttribute(WebConstants.CURRENT_USER);  
//        //获取请求ip  
//        String ip = request.getRemoteAddr();  
//        //获取用户请求方法的参数并序列化为JSON格式字符串  
//        String params = "";  
//         if (joinPoint.getArgs() !=  null && joinPoint.getArgs().length > 0) {  
//             for ( int i = 0; i < joinPoint.getArgs().length; i++) {  
//                params += JSONUtil.toJsonString(joinPoint.getArgs()[i]) + ";";  
//            }  
//        }  
//         try {  
//              /*========控制台输出=========*/  
//            System.out.println("=====异常通知开始=====");  
//            System.out.println("异常代码:" + e.getClass().getName());  
//            System.out.println("异常信息:" + e.getMessage());  
//            System.out.println("异常方法:" + (joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName() + "()"));  
//            System.out.println("方法描述:" + getServiceMthodDescription(joinPoint));  
//            System.out.println("请求人:" + user.getName());  
//            System.out.println("请求IP:" + ip);  
//            System.out.println("请求参数:" + params);  
//               /*==========数据库日志=========*/  
//            Log log = SpringContextHolder.getBean("logxx");  
//            log.setDescription(getServiceMthodDescription(joinPoint));  
//            log.setExceptionCode(e.getClass().getName());  
//            log.setType("1");  
//            log.setExceptionDetail(e.getMessage());  
//            log.setMethod((joinPoint.getTarget().getClass().getName() + "." + joinPoint.getSignature().getName() + "()"));  
//            log.setParams(params);  
//            log.setCreateBy(user);  
//            log.setCreateDate(DateUtil.getCurrentDate());  
//            log.setRequestIp(ip);  
//            //保存数据库  
//            logService.add(log);  
//            System.out.println("=====异常通知结束=====");  
//        }  catch (Exception ex) {  
//            //记录本地异常日志  
//            logger.error("==异常通知异常==");  
//            logger.error("异常信息:{}", ex.getMessage());  
//        }  
//         /*==========记录本地异常日志==========*/  
//        logger.error("异常方法:{}异常代码:{}异常信息:{}参数:{}", joinPoint.getTarget().getClass().getName() + joinPoint.getSignature().getName(), e.getClass().getName(), e.getMessage(), params);  
//  
//    }  
//  
//  
//    /** 
//     * 获取注解中对方法的描述信息 用于service层注解 
//     * 
//     * @param joinPoint 切点 
//     * @return 方法描述 
//     * @throws Exception 
//     */  
//     public  static String getServiceMthodDescription(JoinPoint joinPoint)  
//             throws Exception {  
//        String targetName = joinPoint.getTarget().getClass().getName();  
//        String methodName = joinPoint.getSignature().getName();  
//        Object[] arguments = joinPoint.getArgs();  
//        Class targetClass = Class.forName(targetName);  
//        Method[] methods = targetClass.getMethods();  
//        String description = "";  
//         for (Method method : methods) {  
//             if (method.getName().equals(methodName)) {  
//                Class[] clazzs = method.getParameterTypes();  
//                 if (clazzs.length == arguments.length) {  
//                    description = method.getAnnotation(SystemServiceLog. class).description();  
//                     break;  
//                }  
//            }  
//        }  
//         return description;  
//    }  
//  
//    /** 
//     * 获取注解中对方法的描述信息 用于Controller层注解 
//     * 
//     * @param joinPoint 切点 
//     * @return 方法描述 
//     * @throws Exception 
//     */  
//     public  static String getControllerMethodDescription(JoinPoint joinPoint)  throws Exception {  
//        String targetName = joinPoint.getTarget().getClass().getName();  
//        String methodName = joinPoint.getSignature().getName();  
//        Object[] arguments = joinPoint.getArgs();  
//        Class targetClass = Class.forName(targetName);  
//        Method[] methods = targetClass.getMethods();  
//        String description = "";  
//         for (Method method : methods) {  
//             if (method.getName().equals(methodName)) {  
//                Class[] clazzs = method.getParameterTypes();  
//                 if (clazzs.length == arguments.length) {  
//                    description = method.getAnnotation(SystemControllerLog. class).description();  
//                     break;  
//                }  
//            }  
//        }  
//         return description;  
//    }  
}
