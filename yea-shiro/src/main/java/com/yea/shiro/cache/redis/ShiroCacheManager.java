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
package com.yea.shiro.cache.redis;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;

import com.yea.cache.jedis.cache.RedisCache;
import com.yea.core.cache.ICachePool;
import com.yea.shiro.cache.serializer.ShiroSerializer;

/**
 * 
 * @author yiyongfei
 *
 */
@SuppressWarnings("rawtypes")
public class ShiroCacheManager implements CacheManager {
	
	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();
	private ICachePool cachePool;
	private Long expireTime = 1000L * 60 * 30;
	
	public void setCachePool(ICachePool cachePool) {
		this.cachePool = cachePool;
	}
	public void setExpireMilliseconds(Long milliseconds) {
		this.expireTime = milliseconds;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <K, V> Cache<K, V> getCache(String name) throws CacheException {
		if(!caches.containsKey(name)) {
			RedisCache cache = new RedisCache();
			cache.setCacheName(name);
			cache.setCachePool(cachePool);
			cache.setExpireTime(expireTime);
			cache.setSerializer(new ShiroSerializer());
			ShiroCache<Serializable, V> shiroCache = new ShiroCache<Serializable, V>();
			shiroCache.setCache(cache);
			caches.put(name, shiroCache);
		}
		return caches.get(name);
	}


}