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


/**
 * 
 * @author yiyongfei
 *
 */
public class EhcacheInstance {
//	public final static String LOGIN_RETRY_CACHE = "loginRetryCache";
//	public final static String NETTY_CACHE = "nettyCache";
	
//	private static CacheManager manager = null;
//	@SuppressWarnings("rawtypes")
//	private static Map<String, IGeneralCache> mapCache = new ConcurrentHashMap<String, IGeneralCache>();
//	static {
//		mapCache = new ConcurrentHashMap<String, Cache>();
//		Configuration configuration = new Configuration()//
//				.diskStore(new DiskStoreConfiguration().path("java.io.tmpdir"))// 临时文件目录
//		;
//		manager = CacheManager.create(configuration);
//		
//		/**
//		缓存配置
//		       name:缓存名称。 
//		       maxElementsInMemory：缓存最大个数。 
//		       eternal:对象是否永久有效，一但设置了，timeout将不起作用。
//		       timeToIdleSeconds：设置对象在失效前的允许闲置时间（单位：秒），也就是在一个元素消亡之前，两次访问时间的最大时间间隔值。仅当eternal=false对象不是永久有效时使用，可选属性，默认值是0，也就是可闲置时间无穷大。 
//		       timeToLiveSeconds：设置对象在失效前允许存活时间（单位：秒），也就是一个元素从构建到消亡的最大时间间隔值。最大时间介于创建时间和失效时间之间。仅当eternal=false对象不是永久有效时使用，默认是0.，也就是对象存活时间无穷大。 
//		       overflowToDisk：当内存中对象数量达到maxElementsInMemory时，Ehcache将会对象写到磁盘中。 
//		       diskSpoolBufferSizeMB：这个参数设置DiskStore（磁盘缓存）的缓存区大小。默认是30MB。每个Cache都应该有自己的一个缓冲区。 
//		       maxElementsOnDisk：硬盘最大缓存个数。 
//		       diskPersistent：是否缓存虚拟机重启期数据 Whether the disk store persists between restarts of the Virtual Machine. The default value is false. 
//		       diskExpiryThreadIntervalSeconds：磁盘失效线程运行时间间隔，默认是120秒。 
//		       memoryStoreEvictionPolicy：当达到maxElementsInMemory限制时，Ehcache将会根据指定的策略去清理内存。默认策略是LRU（最近最少使用）。你可以设置为FIFO（先进先出）或是LFU（较少使用）。 
//		       clearOnFlush：内存数量最大时是否清除。 
//		*/
//		/*登录重试次数缓存*/
//		if (!manager.cacheExists(LOGIN_RETRY_CACHE)) {
//			Cache loginRetryCache = new Cache(
//					 new CacheConfiguration(LOGIN_RETRY_CACHE, 20000)
//					 .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
//					 .eternal(false)
//					 .timeToLiveSeconds(3600*24)
//					 .timeToIdleSeconds(0)
//					 .diskExpiryThreadIntervalSeconds(120)
//					 .persistence(new PersistenceConfiguration().strategy(Strategy.NONE)));
//			manager.addCache(loginRetryCache);
//			mapCache.put(LOGIN_RETRY_CACHE, loginRetryCache);
//		}
//		/*Netty数据缓存*/
//		if (!manager.cacheExists(NETTY_CACHE)) {
//			Cache nettyCache = new Cache(
//					 new CacheConfiguration(NETTY_CACHE, 20000)
//					 .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
//					 .eternal(false)
//					 .timeToLiveSeconds(30)
//					 .timeToIdleSeconds(0)
//					 .diskExpiryThreadIntervalSeconds(120)
//					 .persistence(new PersistenceConfiguration().strategy(Strategy.NONE)));
//			manager.addCache(nettyCache);
//			mapCache.put(NETTY_CACHE, nettyCache);
//		}
//		
//	}
	
	private EhcacheInstance(){};
//	
//	@SuppressWarnings("rawtypes")
//	public static void setCacheInstance(String cacheName, IGeneralCache cache) {
//		mapCache.put(cacheName, cache);
//	}
//	
//	@SuppressWarnings("rawtypes")
//	public static IGeneralCache getCacheInstance(String cacheName) {
//		return mapCache.get(cacheName);
//	}
}
