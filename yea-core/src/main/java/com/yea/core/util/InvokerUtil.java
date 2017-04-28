package com.yea.core.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;

/**
 * 
 * 该工具类取自互联网，用于替代Java的反射调用
 *
 */
public class InvokerUtil {
	/**
	 * 调用器池
	 */
	private Map<Method, Invoker> INVOKER_MAP;

	private InvokerUtil() {
		INVOKER_MAP = new ConcurrentHashMap<Method, Invoker>();
	}
	
	public static InvokerUtil getInstance() {
		return Holder.SINGLETON;
	}

	private static class Holder {
		private static final InvokerUtil SINGLETON = new InvokerUtil();
	}
	
	/**
	 * 调用器接口
	 */
	public static interface Invoker {
		/**
		 * 获取方法本身
		 * 
		 * @return
		 */
		Method method();

		/**
		 * 调用方法
		 * 
		 * @param target
		 *            执行对象
		 * @param args
		 *            执行参数
		 * @return
		 */
		Object invoke(Object target, Object... args);
	}

	/**
	 * 根据传入的方法创建快速调用器。
	 * 
	 * @param method
	 *            方法对象
	 * @return 调用器
	 */
	public Invoker newInvoker(Method method) {
		Invoker invoker = INVOKER_MAP.get(method);
		if (invoker == null) {
			StringBuilder proxyClassNameBuilder = new StringBuilder();
			proxyClassNameBuilder.append("proxy.invoker.method$");
			proxyClassNameBuilder.append(method.hashCode());
			String proxyClassName = proxyClassNameBuilder.toString();
			try {
				Class<?> proxyClass;
				try {
					proxyClass = Class.forName(proxyClassName);
				} catch (Throwable e) {
					ClassPool cp = new ClassPool(true);
					CtClass cc = cp.makeClass(proxyClassName);
					cc.addField(CtField.make("private java.lang.reflect.Method m;", cc));
					CtConstructor ctConstructor = new CtConstructor(new CtClass[] { cp.get(Method.class.getName()) },
							cc);
					ctConstructor.setBody("{this.m=(java.lang.reflect.Method)$1;}");
					cc.addConstructor(ctConstructor);
					cc.addInterface(cp.get(Invoker.class.getName()));
					cc.addMethod(CtMethod.make("public java.lang.reflect.Method method(){return m;}", cc));
					StringBuilder invokeCode = new StringBuilder();
					invokeCode.append("public Object invoke(Object host, Object[] args){");
					StringBuilder parameterCode = new StringBuilder();
					for (int i = 0; i < method.getParameterTypes().length; i++) {
						if (i > 0) {
							parameterCode.append(",");
						}
						Class<?> parameterType = method.getParameterTypes()[i];
						parameterCode.append(generateCast("args[" + i + "]", Object.class, parameterType));
					}
					if (method.getParameterTypes().length > 0) {
						invokeCode.append("if(args==null||args.length!=");
						invokeCode.append(method.getParameterTypes().length);
						invokeCode.append(")throw new IllegalArgumentException(\"wrong number of arguments\");");
					}

					StringBuilder executeCode = new StringBuilder();

					executeCode.append("((");
					executeCode.append(method.getDeclaringClass().getCanonicalName());
					executeCode.append(")");
					String objCode = Modifier.isStatic(method.getModifiers()) ? "" : "host";
					executeCode.append(objCode);
					executeCode.append(").");
					executeCode.append(method.getName());
					executeCode.append("(");
					executeCode.append(parameterCode);
					executeCode.append(")");

					if (!method.getReturnType().equals(Void.TYPE)) {
						invokeCode.append("return ");
						invokeCode.append(generateCast(executeCode.toString(), method.getReturnType(), Object.class));
						invokeCode.append(";");
					} else {
						invokeCode.append(executeCode.toString());
						invokeCode.append(";return null;");
					}
					invokeCode.append("}");
					cc.addMethod(CtMethod.make(invokeCode.toString(), cc));
					proxyClass = cc.toClass();
				}
				invoker = (Invoker) proxyClass.getConstructor(Method.class).newInstance(method);
				INVOKER_MAP.put(method, invoker);
			} catch (Throwable e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				throw new RuntimeException(e);
			}
		}
		return invoker;
	}

	private String generateCast(String arg, Class<?> fromClass, Class<?> toClass) {
		StringBuilder cast = new StringBuilder();
		if (fromClass.isPrimitive() && !toClass.isPrimitive()) {
			Class<?> wraperClass = toClass;
			if (!isWraper(toClass)) {
				wraperClass = getWraper(fromClass);
			}
			cast.append("(");
			cast.append(toClass.getCanonicalName());
			cast.append(")");
			cast.append(wraperClass.getCanonicalName());
			cast.append(".valueOf((");
			cast.append(getPrimitive(wraperClass).getCanonicalName());
			cast.append(")");
			cast.append(arg);
			cast.append(")");
		} else if (!fromClass.isPrimitive() && toClass.isPrimitive()) {
			cast.append("(");
			cast.append(toClass.getCanonicalName());
			cast.append(")");
			Class<?> wraperClass = fromClass;
			if (!isWraper(fromClass)) {
				wraperClass = getWraper(toClass);
				cast.append("((");
				if (Number.class.isAssignableFrom(wraperClass)) {
					cast.append(Number.class.getCanonicalName());
				} else {
					cast.append(wraperClass.getCanonicalName());
				}
				cast.append(")");
				cast.append(arg);
				cast.append(")");
			} else {
				cast.append(arg);
			}
			cast.append(".");
			cast.append(getPrimitive(wraperClass).getCanonicalName());
			cast.append("Value()");
		} else {
			cast.append("(");
			cast.append(toClass.getCanonicalName());
			cast.append(")");
			cast.append(arg);
		}
		return cast.toString();
	}

	private Class<?> getPrimitive(Class<?> wraperClass) {
		if (wraperClass.equals(Integer.class)) {
			return Integer.TYPE;
		}
		if (wraperClass.equals(Short.class)) {
			return Short.TYPE;
		}
		if (wraperClass.equals(Long.class)) {
			return Long.TYPE;
		}
		if (wraperClass.equals(Float.class)) {
			return Float.TYPE;
		}
		if (wraperClass.equals(Double.class)) {
			return Double.TYPE;
		}
		if (wraperClass.equals(Byte.class)) {
			return Byte.TYPE;
		}
		if (wraperClass.equals(Character.class)) {
			return Character.TYPE;
		}
		if (wraperClass.equals(Boolean.class)) {
			return Boolean.TYPE;
		}
		if (wraperClass.equals(Void.class)) {
			return Void.TYPE;
		}
		return wraperClass;
	}

	private Class<?> getWraper(Class<?> toClass) {
		if (toClass.equals(Integer.TYPE)) {
			return Integer.class;
		}
		if (toClass.equals(Short.TYPE)) {
			return Short.class;
		}
		if (toClass.equals(Long.TYPE)) {
			return Long.class;
		}
		if (toClass.equals(Float.TYPE)) {
			return Float.class;
		}
		if (toClass.equals(Double.TYPE)) {
			return Double.class;
		}
		if (toClass.equals(Byte.TYPE)) {
			return Byte.class;
		}
		if (toClass.equals(Character.TYPE)) {
			return Character.class;
		}
		if (toClass.equals(Boolean.TYPE)) {
			return Boolean.class;
		}
		if (toClass.equals(Void.TYPE)) {
			return Void.class;
		}
		return toClass;
	}

	private boolean isWraper(Class<?> toClass) {
		if (toClass.equals(Integer.class)) {
			return true;
		}
		if (toClass.equals(Short.class)) {
			return true;
		}
		if (toClass.equals(Long.class)) {
			return true;
		}
		if (toClass.equals(Float.class)) {
			return true;
		}
		if (toClass.equals(Double.class)) {
			return true;
		}
		if (toClass.equals(Byte.class)) {
			return true;
		}
		if (toClass.equals(Character.class)) {
			return true;
		}
		if (toClass.equals(Boolean.class)) {
			return true;
		}
		if (toClass.equals(Void.class)) {
			return true;
		}
		return false;
	}
}
