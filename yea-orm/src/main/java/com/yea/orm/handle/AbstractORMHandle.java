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
package com.yea.orm.handle;

import org.mybatis.spring.SqlSessionTemplate;

import com.yea.orm.handle.dto.ORMParams;

/**
 * 
 * @author yiyongfei
 *
 * @param <T>
 */
public abstract class AbstractORMHandle<T> implements ORMHandle<T> {
	private String level;
	private ORMHandle<T> nextHandle;
	
	public AbstractORMHandle(String level){
		this.level = level;
	}
	
	/**
	 * 符合当前Level，则执行DB操作，否则交由下个Handle去执行
	 */
	public final T handle(SqlSessionTemplate mybatisSessionTemplate, String level, ORMParams dto) throws Exception {
		if(this.level.equals(level)){
			return this.execute(mybatisSessionTemplate, dto);
		} else {
			this.setNextHandle();
			if(this.nextHandle != null){
				return this.nextHandle.handle(mybatisSessionTemplate, level, dto);
			} else {
				throw new Exception("必须设置ORMHandle处理当前请求");
			}
		}
	}
	
	/**
	 * 执行DB操作
	 * @param dto
	 * @return
	 * @throws Exception
	 */
	protected abstract T execute(SqlSessionTemplate mybatisSessionTemplate, ORMParams dto) throws Exception;
	
	/**
	 * 设置下个Handle，子类设置，如果是最后一个Handle，不设置
	 */
	public abstract void setNextHandle();

	protected void setNextHandle(ORMHandle<T> nextHandle){
		this.nextHandle = nextHandle;
	}

	public String getLevel(){
		return this.level;
	}
	
}
