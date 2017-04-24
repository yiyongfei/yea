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
package com.yea.cache.ehcache.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.ehcache.Cache;
import org.ehcache.Cache.Entry;
import org.springframework.util.StringUtils;
import org.ehcache.CacheManager;

import com.yea.core.cache.IGeneralCache;
import com.yea.core.cache.constants.CacheConstants;
import com.yea.core.cache.exception.CacheException;


public class EhcacheCache<K extends Serializable, V extends Serializable> implements IGeneralCache<K, V> {
	private CacheManager cacheManager;
	private String cacheName;
	
	@SuppressWarnings("unchecked")
	public void setCacheManager(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
		if (!StringUtils.isEmpty(cacheName)) {
			cache = (Cache<K, V>) cacheManager.getCache(cacheName, Serializable.class, Serializable.class);
		}
	}

	@SuppressWarnings("unchecked")
	public void setCacheName(String cacheName) {
		this.cacheName = cacheName;
		if(this.cacheManager != null) {
			cache = (Cache<K, V>) cacheManager.getCache(cacheName, Serializable.class, Serializable.class);
		}
	}

	private Cache<K, V> cache;
	
	@Override
	public V get(K key){
		return (V) cache.get(key);
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
		V _value = get(key);
		if (CacheConstants.PutMode.UN_LIMIT.value().equals(putMode)) {
			cache.put(key, value);
		} else if (CacheConstants.PutMode.NOT_EXIST.value().equals(putMode)) {
			if(_value == null) {
				cache.put(key, value);
			}
		} else {
			if(_value != null) {
				cache.put(key, value);
			}
		}
		return _value; 
	}
	
	@Override
	public V remove(K key) {
		V _value = get(key);
		cache.remove(key);
		return _value;
	}

	@SuppressWarnings("unchecked")
	public void remove(K... keys) throws Exception {
		Set<K> _keys = new HashSet<K>();
		for(K key : keys) {
			_keys.add(key);
		}
		cache.removeAll(_keys);
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
		return cache.containsKey(key);
	}

	@Override
	public void clear() {
		cache.clear();
	}

	@Override
	public Set<K> keySet() {
		Set<K> _keys = new HashSet<K>();
		Iterator<Entry<K, V>> it = cache.iterator();
		while(it.hasNext()) {
			_keys.add((K) it.next().getKey());
		}
		return _keys;
	}

	@Override
	public Collection<V> values() {
		Collection<V> values = new ArrayList<V>();
		Iterator<Entry<K, V>> it = cache.iterator();
		while(it.hasNext()) {
			values.add((V) it.next().getValue());
		}
		return values;
	}
	
}
