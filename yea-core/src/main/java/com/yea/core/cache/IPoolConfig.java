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
package com.yea.core.cache;

/**
 * 缓存池参数
 * 
 * @author yiyongfei
 *
 */
public interface IPoolConfig {
	//控制一个pool可分配多少个实例；
	public void setMaxTotal(int maxTotal);
	public int getMaxTotal();
	
	//控制一个pool最多有多少个状态为idle(空闲的)的实例；（只用于Redis）
	public void setMaxIdle(int maxIdle);
	public int getMaxIdle();
	
	//控制一个pool实例超时时间（milliseconds）；（只用于Memcached）
	public void setMaxConnectMillis(long maxConnectMillis);
	public long getMaxConnectMillis();
	
	//表示当引入一个实例时，最大的操作等待时间；
	public void setMaxWaitMillis(long maxWaitMillis);
	public long getMaxWaitMillis();
	
	//在引入一个实例时，是否提前进行validate操作；如果为true，则得到的实例均是可用的；（只用于Redis）
	public void setTestOnBorrow(boolean testOnBorrow);
	public boolean getTestOnBorrow();
	
	//在return给pool时，是否提前进行validate操作；（只用于Redis）
	public void setTestOnReturn(boolean testOnReturn);
	public boolean getTestOnReturn();
	
	//borrowObject返回对象时，是采用DEFAULT_LIFO（last in first out，即类似cache的最频繁使用队列），如果为False，则表示FIFO队列；（只用于Redis）
	public void setLifo(boolean lifo);
	public boolean getLifo();
	
	//实例修复开关（设成true时将自动修复失效的连接）；（只用于Memcached）
	public void setEnableHealSession(boolean enableHealSession);
	public boolean getEnableHealSession();
	
	//实例修复间隔时间（milliseconds）；（只用于Memcached）
	public void setHealSessionInterval(long healSessionInterval);
	public long getHealSessionInterval();
	
	//（只用于Memcached）
	public void setFailureMode(boolean failureMode);
	public boolean getFailureMode();
	
	public Object getConfig();
}
