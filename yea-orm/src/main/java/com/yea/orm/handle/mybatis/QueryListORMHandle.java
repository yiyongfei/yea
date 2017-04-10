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
package com.yea.orm.handle.mybatis;

import org.mybatis.spring.SqlSessionTemplate;

import com.yea.orm.handle.AbstractORMHandle;
import com.yea.orm.handle.ORMConstants;
import com.yea.orm.handle.ORMHandle;
import com.yea.orm.handle.ORMHandleFactory;
import com.yea.orm.handle.dto.ORMParams;

/**
 * 查询一批符合条件的数据操作
 * 
 * @author yiyongfei
 *
 */
public class QueryListORMHandle<T> extends AbstractORMHandle<T> {
    
	public QueryListORMHandle() {
		super(ORMConstants.ORM_LEVEL.M_QUERYLIST.getCode());
	}

	@Override
	protected T execute(SqlSessionTemplate sessionTemplate, ORMParams dto) {
		// TODO Auto-generated method stub
		if(dto.getParam() == null){
			return (T) sessionTemplate.selectList(dto.getSqlid());
		}
		return (T) sessionTemplate.selectList(dto.getSqlid(), dto.getParam());
	}

	@Override
	public void setNextHandle() {
		ORMHandle<T> nextHandle = ORMHandleFactory.getInstance(ORMConstants.ORM_LEVEL.M_SQL);
		this.setNextHandle(nextHandle);
	}
}
