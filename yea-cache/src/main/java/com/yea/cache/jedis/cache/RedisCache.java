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
package com.yea.cache.jedis.cache;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.BinaryJedisCommands;
import redis.clients.jedis.Builder;
import redis.clients.jedis.BuilderFactory;
import redis.clients.jedis.Client;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCommands;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.MultiKeyBinaryCommands;
import redis.clients.jedis.MultiKeyPipelineBase;
import redis.clients.jedis.Response;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.PipelineBase;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;
import redis.clients.util.ShardInfo;

import com.yea.cache.jedis.pool.RedisShardedPool;
import com.yea.core.base.id.RandomIDGennerator;
import com.yea.core.cache.ICachePool;
import com.yea.core.cache.IGeneralCache;
import com.yea.core.cache.IPipelineCache;
import com.yea.core.cache.constants.CacheConstants;
import com.yea.core.cache.exception.CacheException;
import com.yea.core.serializer.ISerializer;
import com.yea.core.serializer.fst.Serializer;

public class RedisCache<K extends Serializable, V> implements IGeneralCache<K, V>, IPipelineCache<K, V> {
	private ISerializer serializer;
	private ICachePool<K, V> cachePool;
	private byte[] cacheName;
	private TimeUnit timeUnit;//按秒来计算超时时间
	private Long expireTime;
	private Long expireSeconds;
	private Map<Long, Object> innerMap = null;
	
	public RedisCache() {
		serializer = new Serializer();
		setTimeUnit(TimeUnit.SECONDS);
		cacheName = SafeEncoder.encode("DEFAULT");
		innerMap = new ConcurrentHashMap<Long, Object>();
	}
	
	public void setSerializer(ISerializer serializer) {
		this.serializer = serializer;
	}
	
	public void setTimeUnit(TimeUnit timeUnit) {
		this.timeUnit = timeUnit;
		if(expireTime != null) {
			if(TimeUnit.MILLISECONDS.equals(timeUnit)) {
				expireSeconds = expireTime / 1000;
			} else if (TimeUnit.MINUTES.equals(timeUnit)) {
				expireSeconds = expireTime * 60;
			} else if (TimeUnit.HOURS.equals(timeUnit)) {
				expireSeconds = expireTime * 60 * 60;
			} else if (TimeUnit.DAYS.equals(timeUnit)) {
				expireSeconds = expireTime * 24 * 60 * 60;
			} else {
				expireSeconds = expireTime;
			} 
		}
	}
	
	public void setCachePool(ICachePool<K, V> cachePool) {
		this.cachePool = cachePool;
	}

	public void setCacheName(String cacheName) {
		this.cacheName = SafeEncoder.encode(cacheName);
	}
	
	public void setExpireTime(Long expireTime) {
		this.expireTime = expireTime;
		if (TimeUnit.MILLISECONDS.equals(timeUnit)) {
			expireSeconds = expireTime / 1000;
		} else if (TimeUnit.MINUTES.equals(timeUnit)) {
			expireSeconds = expireTime * 60;
		} else if (TimeUnit.HOURS.equals(timeUnit)) {
			expireSeconds = expireTime * 60 * 60;
		} else if (TimeUnit.DAYS.equals(timeUnit)) {
			expireSeconds = expireTime * 24 * 60 * 60;
		} else {
			expireSeconds = expireTime;
		}
	}

	@Override
	public V get(K key){
		BinaryJedisCommands commands = null;
		try {
			commands = (BinaryJedisCommands) cachePool.getResource();
			return parseValue(commands.get(toKey(key)));
		} finally {
			cachePool.returnResource(commands);
		}
	}
	@Override
	public V put(K key, V value){
		return put(key, value, CacheConstants.PutMode.UN_LIMIT.value());
	}
	
	@Override
	public V put(K key, V value, String putMode) {
		if (!(CacheConstants.PutMode.NOT_EXIST.value().equals(putMode)
				|| CacheConstants.PutMode.EXIST.value().equals(putMode)
				|| CacheConstants.PutMode.UN_LIMIT.value().equals(putMode))) {
			throw new CacheException("提供的赋值模式不正确，请设置NX|XX|UX");
		}
		BinaryJedisCommands commands = null;
		byte[] _key = null;
		V _value = null;
		try {
			_key = toKey(key);
			commands = (BinaryJedisCommands) cachePool.getResource();
			if (CacheConstants.PutMode.UN_LIMIT.value().equals(putMode)) {
				_value = this.parseValue(commands.getSet(_key, toValue(value)));
				if (expireSeconds != null) {
					commands.expire(_key, expireSeconds.intValue());
				}
			} else {
				_value = get(key);
				if (expireSeconds != null) {
					commands.set(_key, toValue(value), SafeEncoder.encode(putMode), SafeEncoder.encode("EX"), expireSeconds);
				} else {
					commands.set(_key, toValue(value), SafeEncoder.encode(putMode));
				}
			}
			return _value;
		} finally {
			cachePool.returnResource(commands);
			commands = null;
			_key = null;
			_value = null;
		}
	}
	
	public void expire(K key, TimeUnit timeUnit, Long expireTime) {
		BinaryJedisCommands commands = null;
		try {
			commands = (BinaryJedisCommands) cachePool.getResource();
			Long seconds = 0L;
			if (TimeUnit.MILLISECONDS.equals(timeUnit)) {
				seconds = expireTime / 1000;
			} else if(TimeUnit.SECONDS.equals(timeUnit)) {
				seconds = expireTime;
			} else if (TimeUnit.MINUTES.equals(timeUnit)) {
				seconds = expireTime * 60;
			} else if (TimeUnit.HOURS.equals(timeUnit)) {
				seconds = expireTime * 60 * 60;
			} else if (TimeUnit.DAYS.equals(timeUnit)) {
				seconds = expireTime * 24 * 60 * 60;
			} else {
				throw new CacheException("设置的时间单位不正确，请重新设置");
			} 
			commands.expire(toKey(key), seconds.intValue());
		} finally {
			cachePool.returnResource(commands);
			commands = null;
		}
	}
	
	public void expireAt(K key, long unixTime) {
		BinaryJedisCommands commands = null;
		try {
			commands = (BinaryJedisCommands) cachePool.getResource();
			commands.expireAt(toKey(key), unixTime);
		} finally {
			cachePool.returnResource(commands);
			commands = null;
		}
	}
	
	public Long ttl(K key) {
		BinaryJedisCommands commands = null;
		try {
			commands = (BinaryJedisCommands) cachePool.getResource();
			return commands.ttl(toKey(key));
		} finally {
			cachePool.returnResource(commands);
			commands = null;
		}
	}
	
	@Override
	public V remove(K key) {
		BinaryJedisCommands commands = null;
		V _value = null;
		try {
			_value = get(key);
			commands = (BinaryJedisCommands) cachePool.getResource();
			commands.del(toKey(key));
			return _value;
		} finally {
			cachePool.returnResource(commands);
			commands = null;
			_value = null;
		}
	}

	@SuppressWarnings("unchecked")
	public void remove(K... keys) {
		if(cachePool instanceof RedisShardedPool) {
			if(keys.length > 0) {
				Long pipeid = null;
				try {
					pipeid = startPipeline();
					for(K key : keys) {
						removePipeline(pipeid, key);
					}
					syncPipeline(pipeid);
				} finally {
					cleanPipeline(pipeid);
					pipeid = null;
				}
			}
		} else {
			byte[][] _keys = new byte[keys.length][];
			MultiKeyBinaryCommands commands = null;
			try {
				commands = (MultiKeyBinaryCommands) cachePool.getResource();
				for (int i = 0; i < keys.length; i++) {
			    	_keys[i] = toKey(keys[i]);
			    }
				commands.del(_keys);
			} finally {
				_keys = null;
				cachePool.returnResource(commands);
			}
		}
	}
	
	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return size() == 0 ? true : false;
	}

	@Override
	public boolean containsKey(K key) {
		BinaryJedisCommands commands = null;
		try {
			commands = (BinaryJedisCommands) cachePool.getResource();
			return commands.exists(toKey(key));
		} finally {
			cachePool.returnResource(commands);
		}
	}

	@Override
	public void clear() {
		Set<K> keys = keySet();
		if(keys.size() > 0) {
			Long pipeid = null;
			try {
				pipeid = startPipeline();
				for(K key : keys) {
					removePipeline(pipeid, key);
				}
				syncPipeline(pipeid);
			} finally {
				cleanPipeline(pipeid);
				pipeid = null;
				keys = null;
			}
		}
	}

	@Override
	public Set<K> keySet() {
		return keys("*");
	}
	
	public Set<K> keys(String pattern) {
		if(cachePool instanceof RedisShardedPool) {
			Set<K> _keys = new HashSet<K>();
			ShardedJedis shardedJedis = null;
			try {
				shardedJedis = (ShardedJedis) cachePool.getResource();
				for(Jedis jedis : shardedJedis.getAllShards()) {
					_keys.addAll(_keys(jedis, pattern));
				}
				return _keys;
			} finally {
				cachePool.returnResource(shardedJedis);
			}
		} else {
			MultiKeyBinaryCommands commands = null;
			try {
				commands = (MultiKeyBinaryCommands) cachePool.getResource();
				return _keys(commands, pattern);
			} finally {
				cachePool.returnResource(commands);
			}
		}
	}
	
	private Set<K> _keys(MultiKeyBinaryCommands commands, String pattern) {
		String _cacheName = SafeEncoder.encode(cacheName);
		Set<byte[]> aryKeys = null;
		Set<K> keys = new HashSet<K>();
		try {
			aryKeys = commands.keys(SafeEncoder.encode(_cacheName + pattern));
			for(byte[] aryKey : aryKeys) {
				keys.add(parseKey(aryKey));
			}
			return keys;
		} finally {
			_cacheName = null;
			aryKeys = null;
			keys = null;
		}
	}

	@Override
	public Collection<V> values() {
		Collection<V> values = new ArrayList<V>();
		Set<K> keys = keySet();
		if(keys.size() > 0) {
			Long pipeid = null;
			try {
				pipeid = startPipeline();
				for(K key : keys) {
					getPipeline(pipeid, key);
				}
				values = syncPipeline(pipeid);
			} finally {
				cleanPipeline(pipeid);
				pipeid = null;
				keys = null;
			}
		}
		return values;
	}
	
	@Override
	public Long startPipeline() {
		InnerPipeline pipeline = new InnerPipeline();
		pipeline.setJedis((JedisCommands)cachePool.getResource());
		Long pipelineid = RandomIDGennerator.get().generate();
		innerMap.put(pipelineid, pipeline);
	    try {
	    	return pipelineid;
	    } finally {
	    	pipeline = null;
			pipelineid = null;
	    }
	}
	
	@Override
	public void putPipeline(Long pipelineid, K key, V value) {
		putPipeline(pipelineid, key, value, CacheConstants.PutMode.UN_LIMIT.value());
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void putPipeline(Long pipelineid, K key, V value, String putMode) {
		PipelineBase commands = (InnerPipeline) innerMap.get(pipelineid);
		if(commands == null) {
			throw new CacheException("使用管道前请先startPipeline");
		}
		if(!(CacheConstants.PutMode.NOT_EXIST.value().equals(putMode) || CacheConstants.PutMode.EXIST.value().equals(putMode) || CacheConstants.PutMode.UN_LIMIT.value().equals(putMode))) {
			throw new CacheException("提供的赋值模式不正确，请设置NX|XX|UX");
		}
		byte[] _key = toKey(key);
		try {
			if (CacheConstants.PutMode.UN_LIMIT.value().equals(putMode)) {
				if (expireSeconds != null) {
					commands.setex(_key, expireSeconds.intValue(), toValue(value));
				} else {
					commands.set(_key, toValue(value));
				}
			} else {
				if (expireSeconds != null) {
					commands.set(_key, toValue(value), SafeEncoder.encode(putMode), SafeEncoder.encode("EX"), expireSeconds.intValue());
				} else {
					commands.set(_key, toValue(value), SafeEncoder.encode(putMode));
				}
			}
		} finally {
			_key = null;
			commands = null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void getPipeline(Long pipelineid, K key){
		PipelineBase commands = (InnerPipeline) innerMap.get(pipelineid);
		if(commands == null) {
			throw new CacheException("使用管道前请先startPipeline");
		}
		try {
			commands.get(toKey(key));
		} finally {
			commands = null;
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void removePipeline(Long pipelineid, K key) {
		PipelineBase commands = (InnerPipeline) innerMap.get(pipelineid);
		if(commands == null) {
			throw new CacheException("使用管道前请先startPipeline");
		}
		try {
			commands.del(toKey(key));
		} finally {
			commands = null;
		}
	}
	
	@SuppressWarnings({ "unchecked" })
	@Override
	public List<V> syncPipeline(Long pipelineid) {
		InnerPipeline commands = (InnerPipeline) innerMap.get(pipelineid);
		if(commands == null) {
			throw new CacheException("使用管道前请先startPipeline");
		}
		List<Object> listTemp = null;
		List<V> listReturn = null;
		try {
			listTemp = commands.syncAndReturnAll();
	    	listReturn = new ArrayList<V>();
			for (Object temp : listTemp) {
				if (temp instanceof byte[]) {
					listReturn.add(this.parseValue((byte[]) temp));
				} else {
					listReturn.add((V)temp);
				}
			}
	    	return listReturn;
	    } finally {
	    	listTemp = null;
			listReturn = null;
	    	commands = null;
	    }
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void cleanPipeline(Long pipelineid) {
		InnerPipeline commands = (InnerPipeline) innerMap.get(pipelineid);
		if(commands == null) {
			throw new CacheException("使用管道前请先startPipeline");
		}
	    try {
	    	innerMap.remove(pipelineid);
	    	commands.clear();
	    	cachePool.returnResource(commands.getJedis());
	    } finally {
	    	commands = null;
	    }
	}
	
	private byte[] toKey(K obj) {
		byte[] tmp = null;
		byte[] key = null;
		try {
			tmp = serializer.serialize(obj);
			key = new byte[cacheName.length + tmp.length];
			System.arraycopy(cacheName, 0, key, 0, cacheName.length);
			System.arraycopy(tmp, 0, key, cacheName.length, tmp.length);
			return key;
		} catch (Exception ex) {
			throw new CacheException(ex);
		} finally {
			tmp = null;
			key = null;
		}
	}
	private byte[] toValue(V obj) {
		try {
			return serializer.serialize(obj);
		} catch (Exception ex) {
			throw new CacheException(ex);
		} 
	}

	@SuppressWarnings("unchecked")
	private K parseKey(byte[] key) {
		byte[] tmp = null;
		try {
			if(key == null) {
				return null;
			}
			tmp = new byte[key.length - cacheName.length];
			System.arraycopy(key, cacheName.length, tmp, 0, tmp.length);
			return (K) serializer.deserialize(tmp);
		} catch (Exception ex) {
			throw new CacheException(ex);
		} finally {
			tmp = null;
		}
	}
	@SuppressWarnings("unchecked")
	private V parseValue(byte[] value) {
		try {
			if(value == null) {
				return null;
			}
			return (V) serializer.deserialize(value);
		} catch (Exception ex) {
			throw new CacheException(ex);
		} 
	}

	
	class InnerPipeline extends MultiKeyPipelineBase implements Closeable {
		private JedisCommands jedis;
		private Map<ShardInfo<?>, Client> clientResources;

		InnerPipeline() {
			super();
			clientResources = new LinkedHashMap<ShardInfo<?>, Client>();
		}

		void setJedis(JedisCommands jedis) {
			this.jedis = jedis;
			if (jedis instanceof ShardedJedis) {
				Collection<JedisShardInfo> shardinfos = ((ShardedJedis) jedis).getAllShardInfo();
				Collection<Jedis> shards = ((ShardedJedis) jedis).getAllShards();
				for (JedisShardInfo shardinfo : shardinfos) {
					for(Jedis shard : shards) {
						if(shard.getClient().getHost().equals(shardinfo.getHost()) && shard.getClient().getPort() == shardinfo.getPort()) {
							clientResources.put(shardinfo, shard.getClient());
							break;
						}
					}
				}
			} else {
				this.client = ((Jedis) jedis).getClient();
			}
		}

		JedisCommands getJedis() {
			return this.jedis;
		}
		
		@Override
		protected Client getClient(byte[] key) {
			if (clientResources.isEmpty()) {
				return client;
			} else {
				JedisShardInfo shardinfo = ((ShardedJedis) jedis).getShardInfo(key);
				return clientResources.get(shardinfo);
			}
		}

		@Override
		protected Client getClient(String key) {
			if (clientResources.isEmpty()) {
				return client;
			} else {
				JedisShardInfo shardinfo = ((ShardedJedis) jedis).getShardInfo(key);
				return clientResources.get(shardinfo);
			}
		}

		@Override
		public void close() throws IOException {
			clear();
		}

		private MultiResponseBuilder currentMulti;

		@Override
		protected <T> Response<T> getResponse(Builder<T> builder) {
			if (currentMulti != null) {
				super.getResponse(BuilderFactory.STRING); // Expected QUEUED

				Response<T> lr = new Response<T>(builder);
				currentMulti.addResponse(lr);
				return lr;
			} else {
				return super.getResponse(builder);
			}
		}


		public void clear() {
			if (isInMulti()) {
				discard();
			}

			sync();
		}

		public boolean isInMulti() {
			return currentMulti != null;
		}

		/**
		 * Synchronize pipeline by reading all responses. This operation close
		 * the pipeline. In order to get return values from pipelined commands,
		 * capture the different Response&lt;?&gt; of the commands you execute.
		 */
		public void sync() {
			if (getPipelinedResponseLength() > 0) {
				List<Object> unformatted = new ArrayList<Object>();
				if (clientResources.isEmpty()) {
					unformatted.addAll(client.getAll());
				} else {
					Collection<Client> shardClients = clientResources.values();
					for (Client shardClient : shardClients) {
						unformatted.addAll(shardClient.getAll());
					}
				}
				for (Object o : unformatted) {
					generateResponse(o);
				}
			}
		}

		/**
		 * Synchronize pipeline by reading all responses. This operation close
		 * the pipeline. Whenever possible try to avoid using this version and
		 * use Pipeline.sync() as it won't go through all the responses and
		 * generate the right response type (usually it is a waste of time).
		 * 
		 * @return A list of all the responses in the order you executed them.
		 */
		public List<Object> syncAndReturnAll() {
			if (getPipelinedResponseLength() > 0) {
				List<Object> unformatted = new ArrayList<Object>();
				if (clientResources.isEmpty()) {
					unformatted.addAll(client.getAll());
				} else {
					Collection<Client> shardClients = clientResources.values();
					for (Client shardClient : shardClients) {
						unformatted.addAll(shardClient.getAll());
					}
				}
				
				List<Object> formatted = new ArrayList<Object>();

				for (Object o : unformatted) {
					try {
						formatted.add(generateResponse(o).get());
					} catch (JedisDataException e) {
						formatted.add(e);
					}
				}
				return formatted;
			} else {
				return java.util.Collections.<Object> emptyList();
			}
		}

		public Response<String> discard() {
			if (currentMulti == null)
				throw new JedisDataException("DISCARD without MULTI");

			if (clientResources.isEmpty()) {
				client.discard();
			} else {
				Collection<Client> shardClients = clientResources.values();
				for (Client shardClient : shardClients) {
					shardClient.discard();
				}
			}
			
			currentMulti = null;
			return getResponse(BuilderFactory.STRING);
		}

		public Response<List<Object>> exec() {
			if (currentMulti == null)
				throw new JedisDataException("EXEC without MULTI");

			if (clientResources.isEmpty()) {
				client.exec();
			} else {
				Collection<Client> shardClients = clientResources.values();
				for (Client shardClient : shardClients) {
					shardClient.exec();
				}
			}
			
			Response<List<Object>> response = super.getResponse(currentMulti);
			currentMulti.setResponseDependency(response);
			currentMulti = null;
			return response;
		}

		public Response<String> multi() {
			if (currentMulti != null)
				throw new JedisDataException("MULTI calls can not be nested");

			if (clientResources.isEmpty()) {
				client.multi();
			} else {
				Collection<Client> shardClients = clientResources.values();
				for (Client shardClient : shardClients) {
					shardClient.multi();
				}
			}
			
			Response<String> response = getResponse(BuilderFactory.STRING); // Expecting
			// OK
			currentMulti = new MultiResponseBuilder();
			return response;
		}

	}

	private class MultiResponseBuilder extends Builder<List<Object>> {
		private List<Response<?>> responses = new ArrayList<Response<?>>();

		@Override
		public List<Object> build(Object data) {
			@SuppressWarnings("unchecked")
			List<Object> list = (List<Object>) data;
			List<Object> values = new ArrayList<Object>();

			if (list.size() != responses.size()) {
				throw new JedisDataException("Expected data size " + responses.size() + " but was " + list.size());
			}

			for (int i = 0; i < list.size(); i++) {
				Response<?> response = responses.get(i);
				response.set(list.get(i));
				Object builtResponse;
				try {
					builtResponse = response.get();
				} catch (JedisDataException e) {
					builtResponse = e;
				}
				values.add(builtResponse);
			}
			return values;
		}

		public void setResponseDependency(Response<?> dependency) {
			for (Response<?> response : responses) {
				response.setDependency(dependency);
			}
		}

		public void addResponse(Response<?> response) {
			responses.add(response);
		}
	}
}
