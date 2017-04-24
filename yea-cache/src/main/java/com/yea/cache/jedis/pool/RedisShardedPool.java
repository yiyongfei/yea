/**
 * Copyright 2014 伊永飞
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
package com.yea.cache.jedis.pool;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.yea.cache.jedis.config.RedisPoolConfig;
import com.yea.core.cache.ICachePool;
import com.yea.core.cache.IPoolConfig;
import com.yea.core.cache.exception.CacheException;

import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

public class RedisShardedPool<K extends Serializable, V> implements ICachePool<K, V> {
	private ShardedJedisPool jedisPool;
	private IPoolConfig poolConfig;
	private List<JedisShardInfo> listServer;
	
	public RedisShardedPool(){
		listServer = new ArrayList<JedisShardInfo>();
		poolConfig = new RedisPoolConfig();
		//控制一个pool可分配多少个jedis实例，通过pool.getResource()来获取；
        //如果赋值为-1，则表示不限制；如果pool已经分配了maxActive个jedis实例，则此时pool的状态为exhausted(耗尽)。
		poolConfig.setMaxTotal(50);
		//控制一个pool最多有多少个状态为idle(空闲的)的jedis实例。
		poolConfig.setMaxIdle(10);
		//表示当borrow(引入)一个jedis实例时，最大的等待时间，如果超过等待时间，则直接抛出JedisConnectionException；
		poolConfig.setMaxWaitMillis(500);
		 //在borrow一个jedis实例时，是否提前进行validate操作；如果为true，则得到的jedis实例均是可用的；
		poolConfig.setTestOnBorrow(true);
		//在return给pool时，是否提前进行validate操作；
		poolConfig.setTestOnReturn(true);
		//borrowObject返回对象时，是采用DEFAULT_LIFO（last in first out，即类似cache的最频繁使用队列），如果为False，则表示FIFO队列；
		poolConfig.setLifo(true);
	}
	
	@Override
	public void setMaxTotal(int maxTotal){
		poolConfig.setMaxTotal(maxTotal);
	}
	
	@Override
	public void setMaxIdle(int maxIdle){
		poolConfig.setMaxIdle(maxIdle);
	}
	
	@Override
	public void setMaxWaitMillis(long maxWaitMillis){
		poolConfig.setMaxWaitMillis(maxWaitMillis);
	}
	
	@Override
	public void setTestOnBorrow(boolean testOnBorrow){
		poolConfig.setTestOnBorrow(testOnBorrow);
	}
	
	@Override
	public void setTestOnReturn(boolean testOnReturn){
		poolConfig.setTestOnReturn(testOnReturn);
	}
	
	@Override
	public void setLifo(boolean lifo) {
		poolConfig.setLifo(lifo);
	}
	
	@Override
	public void setServer(String address) {
		// TODO Auto-generated method stub
		String[] addrs = address.split(" ");
		for(String addr : addrs){
			String[] arg = addr.split(":");
			JedisShardInfo shardInfo = new JedisShardInfo(arg[0], Integer.valueOf(arg[1]));
			listServer.add(shardInfo);
			shardInfo = null;
		}
	}
	
	@Override
	public Object getResource(){
		return jedisPool.getResource();
	}
	
	@Override
	public void returnResource(Object cache){
		if(jedisPool == null){
			throw new CacheException("释放资源前请先获取资源");
		}
		if(cache != null) {
			ShardedJedis jedis = (ShardedJedis) cache;
			jedis.close();
			jedis = null;
		}
	}
	
	@Override
	public void initPool(){
		jedisPool = new ShardedJedisPool((JedisPoolConfig)poolConfig.getConfig(), listServer);
	}

	@Override
	public void destroyPool() {
		jedisPool.destroy();
	}
	
	@Override
	public void setMaxConnectMillis(long maxConnectMillis) {
	}

	@Override
	public void setEnableHealSession(boolean enableHealSession) {
	}

	@Override
	public void setHealSessionInterval(long healSessionInterval) {
	}

	@Override
	public void setFailureMode(boolean failureMode) {
	}
}
