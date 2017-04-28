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
package com.yea.core.base.act.reflect;

import java.lang.reflect.Method;

import com.yea.core.base.act.AbstractTransactionAct;
import com.yea.core.base.act.exception.ActException;
import com.yea.core.util.InvokerUtil;

@SuppressWarnings("rawtypes")
public class ReflectAct extends AbstractTransactionAct {
	private static final long serialVersionUID = 1L;
	
	private Object target;
	private String methodName;
	
	public void setTarget(Object target) {
		this.target = target;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}


	@Override
	protected Object perform(Object[] messages) throws Throwable {
		Method method;
		if (messages.length > 0) {
			boolean existNullParam = false;
			Class<?>[] parameterTypes = new Class[messages.length];
			for (int i = 0; i < messages.length; i++) {
				if (messages[i] == null) {
					parameterTypes[i] = null;
					existNullParam = true;
				} else {
					parameterTypes[i] = messages[i].getClass();
				}
			}
			if(existNullParam) {
				method = null;
				Method[] methods = target.getClass().getMethods();
				for(Method _method : methods) {
					if(_method.getName().equals(methodName) && _method.getParameterTypes().length == parameterTypes.length) {
						int i = 0;
						boolean isMatch = true;
						for(Class parameterType : _method.getParameterTypes()) {
							if(parameterTypes[i] != null) {
								if(!parameterTypes[i].equals(parameterType)) {
									isMatch = false;
									break;
								} else {
									i++;
								}
							}
						}
						if(isMatch) {
							method = _method;
							break;
						}
					}
				}
				if(method == null) {
					throw new ActException("对象["+target.getClass()+"]未能找到匹配的Method["+methodName+"]");
				}
			} else {
				method = target.getClass().getMethod(methodName, parameterTypes);
			}
		} else {
			method = target.getClass().getMethod(methodName);
		}
		
		return InvokerUtil.getInstance().newInvoker(method).invoke(target, messages);
	}

}
