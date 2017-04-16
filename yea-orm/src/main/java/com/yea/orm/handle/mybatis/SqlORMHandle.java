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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.ibatis.session.SqlSession;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.util.StringUtils;

import com.yea.orm.handle.AbstractORMHandle;
import com.yea.orm.handle.ORMConstants;
import com.yea.orm.handle.dto.ORMParams;
import com.yea.orm.util.SqlMapper;

/**
 * SQL执行，通过Mybatis完成
 * @author yiyongfei
 *
 */
public class SqlORMHandle<T> extends AbstractORMHandle<T> {
	private static Map<Class<?>, String> jdbcType = new HashMap<Class<?>, String> ();
	static {
		jdbcType.put(String.class, "VARCHAR");
		jdbcType.put(Double.class, "DOUBLE");
		jdbcType.put(Float.class, "FLOAT");
		jdbcType.put(Long.class, "BIGINT");
		jdbcType.put(Integer.class, "INTEGER");
		jdbcType.put(Short.class, "SMALLINT");
		jdbcType.put(Byte.class, "TINYINT");
		jdbcType.put(Boolean.class, "BOOLEAN");
		
		jdbcType.put(java.util.Date.class, "DATE");
		jdbcType.put(java.sql.Date.class, "DATE");
		jdbcType.put(Timestamp.class, "TIMESTAMP");
		jdbcType.put(Time.class, "TIME");
		jdbcType.put(BigInteger.class, "DECIMAL");
		jdbcType.put(BigDecimal.class, "DECIMAL");
	}
	
	
	public SqlORMHandle() {
		super(ORMConstants.ORM_LEVEL.M_SQL.getCode());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected T execute(SqlSessionTemplate sessionTemplate, ORMParams dto) throws Exception {
		// TODO Auto-generated method stub
		SqlSession sqlSession = sessionTemplate.getSqlSessionFactory().openSession();
		SqlMapper sqlMapper = new SqlMapper(sqlSession);
		final ORMParams tmp = dto;
		try{
			if(tmp.getSqlid().trim().toUpperCase().startsWith("SELECT")) {
				if(tmp.getParam() != null){
					Object data = tmp.getParam();
					if(data instanceof Object[]){
						return (T) sqlMapper.selectList(parseSql(tmp.getSqlid(), (Object[]) data), parseParam((Object[]) data), HashMap.class);
					} else {
						throw new SQLException("通过SQL查询DB时，查询参数请以Object[]的方式提供!");
					}
				} else {
					return (T) sqlMapper.selectList(tmp.getSqlid(), HashMap.class);
				}
			} else {
				if (tmp.getParam() != null) {
					Object data = tmp.getParam();
					if (data instanceof Object[]) {
						sqlMapper.update(parseSql(tmp.getSqlid(), (Object[]) data), parseParam((Object[]) data));
					} else if (data instanceof Collection) {
						for (Object array : (Collection) data) {
							if (array instanceof Object[]) {
								sqlMapper.update(parseSql(tmp.getSqlid(), (Object[]) data), parseParam((Object[]) data));
							} else {
								throw new SQLException("执行SQL时，参数请以Object[]的方式提供!");
							}
						}
					} else {
						throw new SQLException(
								"ͨ执行SQL时，如果是单条记录操作，参数请以Object[]的方式提供，如果多条记录操作，请提供Collection实例，实例里存放Object[]!");
					}
				} else {
					sqlMapper.update(tmp.getSqlid());
				}
				return null;
			}
		} finally {
			sqlSession.close();
		}
		
	}

	@Override
	public void setNextHandle() {

	}
	
	private String parseSql(String sql, Object[] params) {
		if (sql.indexOf("?") > 0 && params != null) {
			StringBuffer sb = new StringBuffer();
			StringTokenizer st = new StringTokenizer(sql, "?", true);
			int index = 0;
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.equals("?")) {
					String type = null;
					if (params.length > index) {
						type = jdbcType.get(params[index].getClass());
					}
					if (StringUtils.isEmpty(type)) {
						sb.append("#{param").append(index++).append("}");
					} else {
						sb.append("#{param").append(index++).append(", jdbcType=").append(type).append("}");
					}
				} else {
					sb.append(token);
				}
			}
			return sb.toString();
		} else {
			return sql;
		}
	}

	private Map<String, Object> parseParam(Object[] params) {
		Map<String, Object> mapReturn = new HashMap<String, Object>();
		if (params != null && params.length > 0) {
			int index = 0;
			for (Object param : params) {
				mapReturn.put("param" + (index++), param);
			}
		}
		return mapReturn;
	}
	
}
