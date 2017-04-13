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
package com.yea.core.base.facade;

import java.util.concurrent.RecursiveTask;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import com.yea.core.base.facade.exception.FacadeException;


/**
 * 
 * @author yiyongfei
 * @param <T>
 * 
 */
public abstract class AbstractFacade<T> extends RecursiveTask<T> implements Cloneable {
	protected static final long serialVersionUID = 1L;
	protected PlatformTransactionManager txManager;
	protected ApplicationContext context;
	protected Object[] messages;
	protected Throwable throwable;

	public void setMessages(Object[] messages) {
		this.messages = messages;
	}

	public void setThrowable(Throwable throwable) {
		this.throwable = throwable;
	}

	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		context = arg0;
		txManager = context.getBean(PlatformTransactionManager.class);
	}

	@Override
	protected T compute() {
		return _compute();
	}

	protected T _compute() {
		try {
			if (throwable != null) {
				return perform(new Throwable[] { throwable });
			} else {
				return perform(messages);
			}
		} catch (Throwable e) {
			throw new FacadeException(e.getMessage(), e);
		}
	}

	protected abstract T perform(Object[] messages) throws Throwable;

	@Override
	public AbstractFacade<?> clone() throws CloneNotSupportedException {
		AbstractFacade<?> obj = (AbstractFacade<?>) super.clone();
		return obj;
	}

}
