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
package com.yea.shiro.credential;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.credential.HashedCredentialsMatcher;

import com.yea.cache.ehcache.instance.Instance;
import com.yea.core.cache.IGeneralCache;
import com.yea.shiro.constants.ShiroConstants;

/**
 * 登录重试次数限制
 * @author yiyongfei
 *
 */
public class RetryLimitHashedCredentialsMatcher extends HashedCredentialsMatcher {
	@SuppressWarnings("unchecked")
	private IGeneralCache<String, AtomicInteger> shiroCache = Instance.getCacheInstance(Instance.LOGIN_RETRY_CACHE);
	
	@Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
        String username = (String)token.getPrincipal();
		if (shiroCache.containsKey(username) && shiroCache.get(username) != null) {
			AtomicInteger retryCount = shiroCache.get(username);
			if (retryCount.get() > ShiroConstants.LOGIN_RETRY_LIMIT) {
				throw new ExcessiveAttemptsException("登录的失败次数过多，请稍候再登录");
			}
		}
		
		boolean matches = super.doCredentialsMatch(token, info);
		if (matches) {
			shiroCache.remove(username);
		} else {
			AtomicInteger retryCount = shiroCache.get(username);
			if(retryCount == null) {
				shiroCache.put(username, new AtomicInteger(1));
			} else {
				retryCount.incrementAndGet();
			}
		}
		
        return matches;
    }
}
