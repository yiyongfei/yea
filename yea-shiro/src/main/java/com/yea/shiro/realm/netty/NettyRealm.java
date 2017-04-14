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
package com.yea.shiro.realm.netty;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.UnknownAccountException;

import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.realm.AbstractRealm;

import java.util.List;
import java.util.Map;

/**
 * 通过Netty来获取用户数据，依赖com.yea.achieve.shiro.act.ShiroAct
 * @author yiyongfei
 *
 */
public class NettyRealm extends AbstractRealm {
    
    private AbstractEndpoint nettyClient;
    
    public void setNettyClient(AbstractEndpoint nettyClient) {
        this.nettyClient = nettyClient;
    }
    public AbstractEndpoint getNettyClient() {
        return this.nettyClient;
    }
    
	@Override
	protected Map<String, Object> getUser(String username) throws AuthenticationException {
		CallAct act = new CallAct();
		act.setActName("shiroAct");
		List<Map<String, Object>> listUser = null;
		try {
			Promise<List<Map<String, Object>>> future = nettyClient.send(act, ShiroConstants.ShiroSQL.AUTHENTICATION_QUERY.getSql(), new String[]{username});
			listUser = future.awaitObject(10000);
		} catch (Throwable e) {
            final String message = "认证用户[" + username + "]时发生了远程获取认证信息失败！";
            throw new AuthenticationException(message, e);
        }
    	if(listUser == null || listUser.size() == 0) {
    		throw new UnknownAccountException("未发现欲认证用户[" + username + "]的账号");
    	} else if (listUser.size() > 1) {
    		throw new UnknownAccountException("认证用户[" + username + "]时发现该用户名有太多的账号，请确保用户名唯一");
    	} else {
    		return listUser.get(0);
    	}
	}
	
	@Override
	protected List<Map<String, Object>> getRoles(String username) throws Throwable {
		CallAct act = new CallAct();
		act.setActName("shiroAct");
    	try {
    		Promise<List<Map<String, Object>>> futureRole = nettyClient.send(act, ShiroConstants.ShiroSQL.USER_ROLES_QUERY.getSql(), new String[]{username});
    		return futureRole.awaitObject(10000);
    	} finally {
    		act = null;
        }
	}
	
	@Override
	protected List<Map<String, String>> getPermissions(Object roleId) throws Throwable {
		CallAct act = new CallAct();
		act.setActName("shiroAct");
		try {
			Promise<List<Map<String, String>>> futurePermission = nettyClient.send(act, ShiroConstants.ShiroSQL.USER_PERMISSION_QUERY.getSql(), new Object[]{roleId});
	    	return futurePermission.awaitObject(10000);
	    } finally {
    		act = null;
        }
	}
}
