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
package com.yea.core.base.dao;

import java.io.Serializable;
import java.sql.Connection;
import java.util.List;


/**
 * DAO层操作，默认单表的新增、修改、删除操作
 * 
 * @author yiyongfei
 * @param <T>
 *
 */
public interface BaseDAO<T> extends Serializable {
	
	public void insert(String sqlId, T params) throws Exception;
	
	public void update(String sqlId, T obj) throws Exception;
	
	public void delete(String sqlId, T obj) throws Exception;
	
	public T load(String sqlId, T pk) throws Exception;
	
	public T queryOne(String sqlId, T params) throws Exception;
	
	public List<T> queryMany(String sqlId, T params) throws Exception;
	
	public T executeSQL(String sql, Object[] params) throws Exception;
	
	public Connection getConnection() throws Exception;
}
