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
package com.yea.shiro.mgt;


import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.apache.shiro.mgt.DefaultSecurityManager;

import com.yea.core.remote.AbstractEndpoint;
import com.yea.shiro.credential.RetryLimitHashedCredentialsMatcher;
import com.yea.shiro.password.EncrytPassword;
import com.yea.shiro.realm.netty.NettyRealm;

/**
 * 
 * @author yiyongfei
 *
 */
public class ShiroSecurityManager extends DefaultSecurityManager{
	private HashedCredentialsMatcher credentialsMatcher;
	
	public ShiroSecurityManager() {
		super();
		// 设置authenticator
		ModularRealmAuthenticator authenticator = new ModularRealmAuthenticator();
		authenticator.setAuthenticationStrategy(new AtLeastOneSuccessfulStrategy());
		setAuthenticator(authenticator);

		// 设置authorizer
		ModularRealmAuthorizer authorizer = new ModularRealmAuthorizer();
		authorizer.setPermissionResolver(new WildcardPermissionResolver());
		setAuthorizer(authorizer);

		// 设置密码校验Matcher
		credentialsMatcher = new RetryLimitHashedCredentialsMatcher();
		credentialsMatcher.setHashAlgorithmName(EncrytPassword.PASSWORD_HASH.getAlgorithmName());
		credentialsMatcher.setHashIterations(EncrytPassword.HASH_ITERATIONS);
		credentialsMatcher.setStoredCredentialsHexEncoded(true);
		
	}
	
	public void setEndpoint(AbstractEndpoint endpoint){
		NettyRealm realm = new NettyRealm();
		//设置Netty的客户端
		realm.setNettyClient(endpoint);
		//设置密码校验Matcher
		realm.setCredentialsMatcher(credentialsMatcher);
		//设置是否查找权限，为true时，在获取授权时会查找所授予的权限
		realm.setPermissionsLookupEnabled(true);
		//认证信息是否缓存
		realm.setAuthenticationCachingEnabled(false);
		//授权信息是否缓存
		realm.setAuthorizationCachingEnabled(false);
		
		this.setRealm(realm);
	}
}
