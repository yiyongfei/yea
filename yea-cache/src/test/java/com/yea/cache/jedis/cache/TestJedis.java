package com.yea.cache.jedis.cache;

import java.io.Serializable;

import com.yea.cache.jedis.pool.RedisGeneralPool;
import com.yea.cache.jedis.pool.RedisShardedPool;
import com.yea.core.base.id.RandomIDGennerator;

public class TestJedis {

	public static void main(String[] args) {
		RedisShardedPool<Serializable, Object> pool = new RedisShardedPool<Serializable, Object>();
		pool.setServer("127.0.0.1:6379");
		pool.setServer("127.0.0.1:6479");
		pool.initPool();
		
		RedisCache<Serializable, Object> cache = new RedisCache<Serializable, Object>();
		cache.setCachePool(pool);
		cache.setExpireTime(60 * 1000L);
		
		for(int i = 0; i < 100; i++) {
			cache.put(RandomIDGennerator.get().generate(), i);
		}
		
		System.out.println(cache.keySet());
		
		System.out.println(cache.size());
		
//		System.out.println(cache.isEmpty());
		
		System.out.println(cache.values());
		
//		cache.clear();
		
//		System.out.println(cache.keySet());
		
	}
}
