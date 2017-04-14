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

import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.crypto.hash.SimpleHash;

import com.yea.core.base.id.RandomIDGennerator;

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
	
	public EncrytPassword(String password) {
		this.salt = RandomIDGennerator.get().toHexString();
        SimpleHash simpleHash = new SimpleHash(PASSWORD_HASH.getAlgorithmName(), password, salt, HASH_ITERATIONS);  
        this.encrytPassword = simpleHash.toHex();
	}
	
	public final static SimpleHash PASSWORD_HASH = new Md5Hash();
	public final static int HASH_ITERATIONS = 2;
}
