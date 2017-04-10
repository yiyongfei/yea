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
package com.yea.shiro.realm.jdbc;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UnknownAccountException;

import com.yea.core.base.dao.BaseDAO;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.realm.AbstractRealm;

import java.util.List;
import java.util.Map;

/**
 * 待废弃：对于在Web服务器读取Shiro配置数据，可以应用NettyRealm，但Netty客户端使用本地客户端
 * @author yiyongfei
 *
 */
public class JdbcRealm extends AbstractRealm {
    private BaseDAO<?> shiroDao;
    
    public void setShiroDao(BaseDAO<?> shiroDao) {
        this.shiroDao = shiroDao;
    }
    public BaseDAO<?> getShiroDao() {
        return this.shiroDao;
    }
    
	@SuppressWarnings("unchecked")
	@Override
	protected Map<String, Object> getUser(String username) throws AuthenticationException {
		List<Map<String, Object>> listUser;
		try {
			listUser = (List<Map<String, Object>>) shiroDao.executeSQL(ShiroConstants.ShiroSQL.AUTHENTICATION_QUERY.getSql(), new String[]{username});
			if(listUser == null || listUser.size() == 0) {
	    		throw new UnknownAccountException("未发现欲认证用户[" + username + "]的账号");
	    	} else if (listUser.size() > 1) {
	    		throw new AuthenticationException("认证用户[" + username + "]时发现该用户名有太多的账号，请确保用户名唯一");
	    	} else {
	    		return listUser.get(0);
	    	}
		} catch (Exception e) {
			final String message = "为用户[" + username + "]认证时发生了SQL error";
			throw new AuthenticationException(message, e);
		}
	}
	@SuppressWarnings("unchecked")
	@Override
	protected List<Map<String, Object>> getRoles(String username) throws Throwable {
		return (List<Map<String, Object>>) shiroDao.executeSQL(ShiroConstants.ShiroSQL.USER_ROLES_QUERY.getSql(), new String[]{username});
	}
	@SuppressWarnings("unchecked")
	@Override
	protected List<Map<String, String>> getPermissions(Object roleId) throws Throwable {
		return (List<Map<String, String>>) shiroDao.executeSQL(ShiroConstants.ShiroSQL.USER_PERMISSION_QUERY.getSql(), new Object[]{roleId});
	}
}
