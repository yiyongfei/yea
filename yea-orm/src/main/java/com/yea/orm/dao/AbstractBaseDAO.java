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
package com.yea.orm.dao;

import java.sql.Connection;
import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;

import com.yea.core.base.dao.BaseDAO;
import com.yea.orm.handle.ORMConstants;
import com.yea.orm.handle.ORMHandle;
import com.yea.orm.handle.ORMHandleFactory;
import com.yea.orm.handle.dto.ORMParams;

/**
 * 
 * @author yiyongfei
 *
 * @param <T>
 */
public abstract class AbstractBaseDAO<T> implements BaseDAO<T> {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ORMHandle<T> triggerORMHandle = ORMHandleFactory.getInstance(ORMConstants.ORM_LEVEL.M_TRIGGER);
	
	public abstract SqlSessionTemplate getReadSessionTemplate();
	public abstract SqlSessionTemplate getWriteSessionTemplate();
	
	public Connection getConnection() throws Exception {
		if(getWriteSessionTemplate() != null) {
			return getWriteSessionTemplate().getSqlSessionFactory().openSession().getConnection();
		} else {
			return getReadSessionTemplate().getSqlSessionFactory().openSession().getConnection();
		}
	}
	
	public final void insert(String sqlId, T params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setSqlid(sqlId);
		dto.setParam(params);
		triggerORMHandle.handle(getWriteSessionTemplate(), ORMConstants.ORM_LEVEL.M_INSERT.getCode(), dto);
	}
	
	public final void update(String sqlId, T params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setSqlid(sqlId);
		dto.setParam(params);
		triggerORMHandle.handle(getWriteSessionTemplate(), ORMConstants.ORM_LEVEL.M_UPDATE.getCode(), dto);
	}
	
	public final void delete(String sqlId, T params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setSqlid(sqlId);
		dto.setParam(params);
		triggerORMHandle.handle(getWriteSessionTemplate(), ORMConstants.ORM_LEVEL.M_DELETE.getCode(), dto);
	}
	
	public final T load(String sqlId, T pk) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setSqlid(sqlId);
		dto.setParam(pk);
		return triggerORMHandle.handle(getReadSessionTemplate(), ORMConstants.ORM_LEVEL.M_LOAD.getCode(), dto);
	}
	
	public final T queryOne(String sqlId) throws Exception{
		return queryOne(sqlId, null);
	}
	public final T queryOne(String sqlId, T params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setParam(params);
		dto.setSqlid(sqlId);
		return triggerORMHandle.handle(getReadSessionTemplate(), ORMConstants.ORM_LEVEL.M_QUERY.getCode(), dto);
	}
	
	public final List<T> queryMany(String sqlId) throws Exception{
		return queryMany(sqlId, null);
	}
	@SuppressWarnings("unchecked")
	public final List<T> queryMany(String sqlId, T params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setSqlid(sqlId);
		dto.setParam(params);
		return (List<T>)triggerORMHandle.handle(getReadSessionTemplate(), ORMConstants.ORM_LEVEL.M_QUERYLIST.getCode(), dto);
	}
	
	public final T execute(ORMConstants.ORM_LEVEL ormLevel, String sqlId, T params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setParam(params);
		dto.setSqlid(sqlId);
		if(ormLevel.getCode().equals(ORMConstants.ORM_LEVEL.M_LOAD.getCode()) 
				|| ormLevel.getCode().equals(ORMConstants.ORM_LEVEL.M_QUERY.getCode()) 
				|| ormLevel.getCode().equals(ORMConstants.ORM_LEVEL.M_QUERYLIST.getCode())) {
			return triggerORMHandle.handle(getReadSessionTemplate(), ormLevel.getCode(), dto);
		} else {
			return triggerORMHandle.handle(getWriteSessionTemplate(), ormLevel.getCode(), dto);
		}
	}
	
	public final T executeBatch(ORMConstants.ORM_LEVEL ormLevel, String sqlId, List<T> params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setParam(params);
		dto.setSqlid(sqlId);
		if(ormLevel.getCode().equals(ORMConstants.ORM_LEVEL.M_LOAD.getCode()) 
				|| ormLevel.getCode().equals(ORMConstants.ORM_LEVEL.M_QUERY.getCode()) 
				|| ormLevel.getCode().equals(ORMConstants.ORM_LEVEL.M_QUERYLIST.getCode())) {
			throw new Exception("批量执行不支持查询类操作！");
		} else {
			return triggerORMHandle.handle(getWriteSessionTemplate(), ormLevel.getCode(), dto);
		}
	}
	
	public final T executeSQL(String sql, Object[] params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setParam(params);
		dto.setSqlid(sql);
		return triggerORMHandle.handle(getWriteSessionTemplate(), ORMConstants.ORM_LEVEL.M_SQL.getCode(), dto);
	}
	
	public final void batchExecuteSQL(String sql, List<Object[]> params) throws Exception{
		ORMParams dto = new ORMParams();
		dto.setParam(params);
		dto.setSqlid(sql);
		triggerORMHandle.handle(getWriteSessionTemplate(), ORMConstants.ORM_LEVEL.M_SQL.getCode(), dto);
	}
}
