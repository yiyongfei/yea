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
import java.util.Collection;
import java.util.Set;

/**
 * 基于Map的方法定义，定义通用缓存接口
 * @author yiyongfei
 *
 */
public interface IGeneralCache<K extends Serializable, V> {

	V put(K key, V value);
	
	V put(K key, V value, String putMode);
	
	V get(K key);
	
	V remove(K key);
	
	int size();
	
	boolean isEmpty();
	
	boolean containsKey(K key);
	
	void clear();
	
	Set<K> keySet();
	
	Collection<V> values();
	
}
