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
package com.yea.shiro.session.mgt.redis;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.shiro.cache.AbstractCacheManager;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.cache.MapCache;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;

import com.yea.cache.jedis.cache.RedisCache;
import com.yea.core.cache.ICachePool;
import com.yea.shiro.cache.serializer.ShiroSerializer;
import com.yea.shiro.session.mgt.eis.ShiroSessionIdGenerator;

public class ShiroSessionDAO extends CachingSessionDAO {
	
	private ICachePool<Serializable, Session> cachePool;
	
	public ShiroSessionDAO() {
		super.setCacheManager(new AbstractCacheManager() {
			@Override
			protected Cache<Serializable, Session> createCache(String name) throws CacheException {
				return new MapCache<Serializable, Session>(name, new ConcurrentHashMap<Serializable, Session>());
			}
		});
		
		this.setSessionIdGenerator(new ShiroSessionIdGenerator());
	}

	public void setCachePool(ICachePool<Serializable, Session> cachePool) {
		this.cachePool = cachePool;
		cache = new RedisCache<Serializable, Session>();
		cache.setCachePool(cachePool);
		cache.setCacheName(getActiveSessionsCacheName());
		cache.setSerializer(new ShiroSerializer());
	}

	@Override
	public void setActiveSessionsCacheName(String activeSessionsCacheName) {
		super.setActiveSessionsCacheName(activeSessionsCacheName);
		if(this.cachePool != null) {
			cache = null;
			cache = new RedisCache<Serializable, Session>();
			cache.setCachePool(this.cachePool);
			cache.setCacheName(activeSessionsCacheName);
		}
	}
	
	@Override
	public void setCacheManager(CacheManager cacheManager) {
		if(this.getCacheManager() == null) {
			super.setCacheManager(cacheManager);
		}
    }
	
	private RedisCache<Serializable, Session> cache;
	
	@Override
	protected Serializable doCreate(Session session) {
		// TODO Auto-generated method stub
		Serializable sessionId = generateSessionId(session);
        assignSessionId(session, sessionId);
        cache.put(sessionId, session);
        return sessionId;
	}
	
	@Override
	protected void doUpdate(Session session) {
		// TODO Auto-generated method stub
		if(session == null || session.getId() == null){
			return;
		}
		cache.put(session.getId(), session);
	}

	@Override
	protected void doDelete(Session session) {
		// TODO Auto-generated method stub
		cache.remove(session.getId());
	}

	@Override
	protected Session doReadSession(Serializable sessionId) {
		// TODO Auto-generated method stub
		return cache.get(sessionId);
	}

	@Override
	public Collection<Session> getActiveSessions() {
		return cache.values();
	}
	
}
	
	

