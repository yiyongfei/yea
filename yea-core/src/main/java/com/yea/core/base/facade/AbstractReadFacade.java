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

import org.springframework.transaction.annotation.Transactional;

import com.yea.core.base.facade.exception.FacadeException;

/**
 * Facade类，事务控制，对于查的操作，Facade将继承该类
 * 在Facade层，子类只要继承该类，该子类的事务就交由Spring管理
 * @author yiyongfei
 * 
 */
public abstract class AbstractReadFacade implements IFacade {

	protected static final long serialVersionUID = 1L;

	@Transactional(readOnly=true, rollbackFor={Throwable.class})
	public Object facade(Object... messages) {
		try {
            return perform(messages);
        } catch (Throwable e) {
            throw new FacadeException(e.getMessage(), e);
        }
	}
	
	@Transactional(readOnly=true, rollbackFor={Throwable.class})
	public void facade(Exception ex) {
		try {
            perform(ex);
        } catch (Throwable e) {
            throw new FacadeException(e.getMessage(), e);
        }
	}
	
	protected abstract Object perform(Object... messages) throws Throwable ;
}
