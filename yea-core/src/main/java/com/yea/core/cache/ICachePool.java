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

import java.io.Serializable;


/**
 * 缓存池，具体池参数参看IPoolConfig的说明
 * 
 * @author yiyongfei
 *
 */
public interface ICachePool<K extends Serializable, V> {
	
	void setMaxTotal(int maxTotal);
	
	void setMaxIdle(int maxIdle);
	
	void setMaxWaitMillis(long maxWaitMillis);
	
	void setTestOnBorrow(boolean testOnBorrow);
	
	void setTestOnReturn(boolean testOnReturn);
	
	void setLifo(boolean lifo);
		
	void setMaxConnectMillis(long maxConnectMillis);

	void setEnableHealSession(boolean enableHealSession);

	void setHealSessionInterval(long healSessionInterval);

	void setFailureMode(boolean failureMode);
	
	void setServer(String address);
	
	void initPool();
	
	void destroyPool();
	
	Object getResource();
	
	void returnResource(Object cache);

}
