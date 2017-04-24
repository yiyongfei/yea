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
package com.yea.cache.ehcache.instance;

import java.io.Serializable;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;

import com.yea.cache.ehcache.cache.EhcacheCache;
import com.yea.core.cache.IGeneralCache;
import com.yea.core.remote.observer.Observer;


/**
 * 
 * @author yiyongfei
 *
 */
@SuppressWarnings("rawtypes")
public class Instance {
	public final static String LOGIN_RETRY_CACHE = "loginRetryCache";
	public final static String NETTY_CACHE = "nettyCache";
	
	private static Map<String, IGeneralCache> mapCache = new ConcurrentHashMap<String, IGeneralCache>();
	static {
		EhcacheCache cacheInstance = null;
		CacheManager cacheManager = CacheManagerBuilder.newCacheManagerBuilder().build(true);
		
		/*登录重试缓存*/
		CacheConfigurationBuilder<Serializable, Serializable> shiroConfiguration = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(Serializable.class, Serializable.class,
						ResourcePoolsBuilder.newResourcePoolsBuilder().heap(128, MemoryUnit.KB))
				.withExpiry(Expirations.timeToLiveExpiration(Duration.of(3600 * 24, TimeUnit.SECONDS)));
		
		cacheManager.createCache(LOGIN_RETRY_CACHE, shiroConfiguration);
		cacheInstance = new EhcacheCache<String, AtomicInteger>();
		cacheInstance.setCacheName(LOGIN_RETRY_CACHE);
		cacheInstance.setCacheManager(cacheManager);
		mapCache.put(LOGIN_RETRY_CACHE, cacheInstance);
		
		/*Netty缓冲区缓存*/
		CacheConfigurationBuilder<Serializable, Serializable> nettyConfiguration = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(Serializable.class, Serializable.class,
						ResourcePoolsBuilder.newResourcePoolsBuilder().heap(1, MemoryUnit.MB))
				.withExpiry(Expirations.timeToLiveExpiration(Duration.of(30, TimeUnit.SECONDS)));
		
		cacheManager.createCache(NETTY_CACHE, nettyConfiguration);
		cacheInstance = new EhcacheCache<String, Vector<Observer>>();
		cacheInstance.setCacheName(NETTY_CACHE);
		cacheInstance.setCacheManager(cacheManager);
		mapCache.put(NETTY_CACHE, cacheInstance);
	}
	
	private Instance(){};
	
	public static IGeneralCache getCacheInstance(String cacheName) {
		return mapCache.get(cacheName);
	}
}
