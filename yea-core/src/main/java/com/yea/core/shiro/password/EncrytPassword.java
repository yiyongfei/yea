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
package com.yea.core.shiro.password;

import com.yea.core.base.id.RandomIDGennerator;
import com.yea.core.util.HashUtil;
import com.yea.core.util.HexUtil;

/**
 * 密码加密
 * @author yiyongfei
 *
 */
public class EncrytPassword {
	private String encrytPassword;
	private String salt;
	
	public String getEncrytPassword(){
		return this.encrytPassword;
	}
	
	public String getSalt(){
		return this.salt;
	}
	
	/**
	 * 需与Shiro的密码加密方式保持一致
	 * @param password
	 */
	public EncrytPassword(String password) {
		this.salt = RandomIDGennerator.get().toHexString();
		byte[] hash = HashUtil.hash(PASSWORD_HASH, password, salt, HASH_ITERATIONS);
		this.encrytPassword = HexUtil.encodeToString(hash);
	}
	
	public final static String PASSWORD_HASH = "MD5";
	public final static int HASH_ITERATIONS = 2;
}
