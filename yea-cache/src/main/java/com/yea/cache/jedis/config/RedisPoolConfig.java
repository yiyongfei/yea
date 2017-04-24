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
package com.yea.cache.jedis.config;

import com.yea.core.cache.IPoolConfig;

import redis.clients.jedis.JedisPoolConfig;


public class RedisPoolConfig implements IPoolConfig {
	private JedisPoolConfig config;
	
	public RedisPoolConfig(){
		config = new JedisPoolConfig();
	}
	
	public void setMaxTotal(int maxTotal){
		config.setMaxTotal(maxTotal);
	}
	public int getMaxTotal(){
		return config.getMaxTotal();
	}
	
	public void setMaxIdle(int maxIdle){
		config.setMaxIdle(maxIdle);
	}
	public int getMaxIdle(){
		return config.getMaxIdle();
	}
	
	public void setMaxConnectMillis(long maxConnectMillis){
		
	}
	public long getMaxConnectMillis(){
		return 0L;
	}
	
	public void setMaxWaitMillis(long maxWaitMillis){
		config.setMaxWaitMillis(maxWaitMillis);
	}
	public long getMaxWaitMillis(){
		return config.getMaxWaitMillis();
	}
	
	public void setTestOnBorrow(boolean testOnBorrow){
		config.setTestOnBorrow(testOnBorrow);
	}
	public boolean getTestOnBorrow(){
		return config.getTestOnBorrow();
	}
	
	public void setTestOnReturn(boolean testOnReturn){
		config.setTestOnReturn(testOnReturn);
	}
	public boolean getTestOnReturn(){
		return config.getTestOnReturn();
	}
	
	public void setLifo(boolean lifo) {
		config.setLifo(lifo);
	}
	public boolean getLifo() {
		return config.getLifo();
	}
	
	public Object getConfig(){
		return config;
	}

	public void setEnableHealSession(boolean enableHealSession) {
	}

	public boolean getEnableHealSession() {
		return false;
	}

	public void setHealSessionInterval(long healSessionInterval) {
	}

	public long getHealSessionInterval() {
		return 0;
	}

	public void setFailureMode(boolean failureMode) {
	}

	public boolean getFailureMode() {
		return false;
	}
}
