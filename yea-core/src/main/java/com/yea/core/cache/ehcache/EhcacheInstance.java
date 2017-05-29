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
package com.yea.core.cache.ehcache;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;
import org.springframework.util.StringUtils;

/**
 * 
 * @author yiyongfei
 *
 */
@SuppressWarnings("rawtypes")
public class EhcacheInstance {
	public final static String NETTY_CACHE = "nettyCache";
	private static Map<String, Cache> mapCache = new ConcurrentHashMap<String, Cache>();
	static {
		int port = 38080;
		if(!StringUtils.isEmpty(System.getProperty("server.port"))){
			port = Integer.valueOf(System.getProperty("server.port"));
		}
		
		PersistentCacheManager persistentCacheManager = CacheManagerBuilder.newCacheManagerBuilder()
				.with(CacheManagerBuilder.persistence("NETTY.EHCACHE." + port)).build(true);

		/* Netty缓冲区缓存 */
		CacheConfigurationBuilder<Serializable, Serializable> nettyConfiguration = CacheConfigurationBuilder
				.newCacheConfigurationBuilder(Serializable.class, Serializable.class,
						ResourcePoolsBuilder.newResourcePoolsBuilder().heap(60, MemoryUnit.MB).disk(200, MemoryUnit.MB))
				.withExpiry(Expirations.timeToLiveExpiration(Duration.of(60, TimeUnit.SECONDS)));

		persistentCacheManager.createCache(NETTY_CACHE, nettyConfiguration);
		Cache cache = persistentCacheManager.getCache(NETTY_CACHE, Serializable.class, Serializable.class);
		mapCache.put(NETTY_CACHE, cache);
	}

	private EhcacheInstance() {
	};

	public static Cache getCacheInstance(String cacheName) {
		return mapCache.get(cacheName);
	}
}
