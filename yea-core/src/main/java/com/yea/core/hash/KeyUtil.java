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
package com.yea.core.hash;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * 
 * @author yiyongfei
 * 
 */
public final class KeyUtil {

	private KeyUtil() {
		// Empty
	}

	/**
	 * Get the bytes for a key.
	 *
	 * @param k
	 *            the key
	 * @return the bytes
	 */
	public static byte[] getKeyBytes(String k) {
		try {
			return k.getBytes("ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Get the keys in byte form for all of the string keys.
	 *
	 * @param keys
	 *            a collection of keys
	 * @return return a collection of the byte representations of keys
	 */
	public static Collection<byte[]> getKeyBytes(Collection<String> keys) {
		Collection<byte[]> rv = new ArrayList<byte[]>(keys.size());
		for (String s : keys) {
			rv.add(getKeyBytes(s));
		}
		return rv;
	}
}
