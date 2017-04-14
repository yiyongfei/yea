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
package com.yea.shiro.web.mgt;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;
import org.apache.shiro.authc.pam.AtLeastOneSuccessfulStrategy;
import org.apache.shiro.authc.pam.ModularRealmAuthenticator;
import org.apache.shiro.authz.ModularRealmAuthorizer;
import org.apache.shiro.authz.permission.WildcardPermissionResolver;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;

import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.shiro.password.EncrytPassword;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.credential.RetryLimitHashedCredentialsMatcher;
import com.yea.shiro.realm.netty.NettyRealm;

/**
 * 与Web相关，依赖shiro-web
 * @author yiyongfei
 *
 */
public class WebSecurityManager extends DefaultWebSecurityManager{
	private HashedCredentialsMatcher credentialsMatcher;
	private AbstractEndpoint endpoint;
	
	public WebSecurityManager() {
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
	
	@Override
	public Subject login(Subject subject, AuthenticationToken token) throws AuthenticationException {
		Subject userSubject = super.login(subject, token);
		userSubject.getSession().removeAttribute(ShiroConstants.SYSTEM_MENU);
		return userSubject;
	}
	
	public void setEndpoint(AbstractEndpoint endpoint){
		NettyRealm realm = new NettyRealm();
		realm.setNettyClient(endpoint);
		realm.setCredentialsMatcher(credentialsMatcher);
		realm.setPermissionsLookupEnabled(true);
		realm.setAuthenticationCachingEnabled(false);
		realm.setAuthorizationCachingEnabled(false);
		
		this.setRealm(realm);
		this.endpoint = endpoint;
	}
	
	public AbstractEndpoint getEndpoint(){
		return this.endpoint;
	}
	
}
