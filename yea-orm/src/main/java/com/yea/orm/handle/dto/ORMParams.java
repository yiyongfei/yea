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
package com.yea.orm.handle.dto;

import com.yea.core.base.model.BaseModel;
import com.yea.core.cache.ICacheable;

public class ORMParams extends BaseModel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Object param;
	private String sqlid;
	
	public Object getParam() {
		return param;
	}
	public void setParam(Object param) {
		this.param = param;
	}
	public String getSqlid() {
		return sqlid;
	}
	public void setSqlid(String sqlid) {
		this.sqlid = sqlid;
	}
	
	public String generatorCacheKey() {
		if (param != null && param instanceof ICacheable) {
			return ((ICacheable)param).generatorCacheKey();
		} else {
			return null;
		}
	}
}
