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
package com.yea.achieve.shiro.service.impl;

import java.util.Iterator;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.yea.achieve.shiro.service.ShiroService;
import com.yea.core.base.dao.BaseDAO;

/**
 * 
 * @author yiyongfei
 *
 */
@Service("shiroService")
public class ShiroServiceImpl implements ShiroService, ApplicationContextAware {
	@SuppressWarnings("rawtypes")
	private BaseDAO shiroDao;
	
	public Object executeSQL(String sql, Object[] params) throws Exception {
		// TODO Auto-generated method stub
		return shiroDao.executeSQL(sql, params);
	}
	
	@SuppressWarnings("rawtypes")
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		// TODO Auto-generated method stub
		Map<String, BaseDAO> mapDao = arg0.getBeansOfType(BaseDAO.class);
		Iterator<BaseDAO> it = mapDao.values().iterator();
		while(it.hasNext()){
			shiroDao = it.next();
			break;
		}
	}

}
