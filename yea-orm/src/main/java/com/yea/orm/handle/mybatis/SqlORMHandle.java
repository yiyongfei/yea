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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;

import com.yea.orm.handle.AbstractORMHandle;
import com.yea.orm.handle.ORMConstants;
import com.yea.orm.handle.dto.ORMParams;

/**
 * SQL执行，通过Mybatis完成
 * @author yiyongfei
 *
 */
public class SqlORMHandle<T> extends AbstractORMHandle<T> {
    
	public SqlORMHandle() {
		super(ORMConstants.ORM_LEVEL.M_SQL.getCode());
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected T execute(SqlSessionTemplate sessionTemplate, ORMParams dto) throws Exception {
		// TODO Auto-generated method stub
		SqlSession sqlSession = sessionTemplate.getSqlSessionFactory().openSession();
		Connection connection = sqlSession.getConnection();;
		final ORMParams tmp = dto;
		ResultSet rs = null;
		PreparedStatement ps = connection.prepareStatement(tmp.getSqlid());
		try{
			if(tmp.getSqlid().trim().toUpperCase().startsWith("SELECT")) {
				//表示本次SQL用于查询
				if(tmp.getParam() != null){
					Object data = tmp.getParam();
					if(data instanceof Object[]){
						Object[] array = (Object[])data;
						int index = 1;
						for(Object obj : array){
							setParam(ps, index++, obj);
						}
					} else {
						throw new SQLException("通过SQL查询DB时，查询参数请以Object[]的方式提供!");
					}
				}
				rs = ps.executeQuery();
				
				ResultSetMetaData md = rs.getMetaData();
				List<Map<String, Object>> listResult = new ArrayList<Map<String,Object>>();
				int columnCount = md.getColumnCount();
				while (rs.next()) {
					Map<String,Object> rowData = new HashMap<String,Object>();
					for (int i = 1; i <= columnCount; i++) {
						rowData.put(md.getColumnLabel(i), rs.getObject(i));
					}
					listResult.add(rowData);
				}
				return (T) listResult;
			} else {
				if(tmp.getParam() != null){
					Object data = tmp.getParam();
					if(data instanceof Object[]){
						Object[] array = (Object[])data;
						int index = 1;
						for(Object obj : array){
							setParam(ps, index++, obj);
						}
						return (T) Boolean.valueOf(ps.execute());
					} else if (data instanceof Collection) {
						for(Object array : (Collection)data){
							if(array instanceof Object[]){
								int index = 1;
								for(Object obj : (Object[])array){
									setParam(ps, index++, obj);
								}
								ps.addBatch();
							} else {
								throw new SQLException("执行SQL时，参数请以Object[]的方式提供!");
							}
							
						}
						return (T) ps.executeBatch();
					} else {
						throw new SQLException("ͨ执行SQL时，如果是单条记录操作，参数请以Object[]的方式提供，如果多条记录操作，请提供Collection实例，实例里存放Object[]!");
					}
				}
				return (T) Boolean.valueOf(ps.execute());
			}
		} finally {
			if(rs != null){
				rs.close();
			}
			if(ps != null){
				ps.close();
			}
			connection.close();
			sqlSession.close();
		}
		
	}

	@Override
	public void setNextHandle() {

	}
	
	private void setParam(PreparedStatement ps, int index, Object param) throws SQLException{
		ps.setObject(index, param);
	}
}
