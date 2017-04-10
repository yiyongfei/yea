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
package com.yea.achieve.generator.service.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import com.yea.achieve.generator.dto.AggregateWrapper;
import com.yea.achieve.generator.dto.DaoWrapper;
import com.yea.achieve.generator.dto.EntityWrapper;
import com.yea.achieve.generator.dto.GeneratorConfig;
import com.yea.achieve.generator.dto.PKWrapper;
import com.yea.achieve.generator.dto.SqlmapWrapper;
import com.yea.achieve.generator.dto.Wrapper;
import com.yea.achieve.generator.dto.java.FullyQualifiedJavaType;
import com.yea.achieve.generator.dto.java.JavaTypeResolver;
import com.yea.achieve.generator.dto.java.JavaTypeResolverDefaultImpl;
import com.yea.achieve.generator.dto.table.IntrospectedColumn;
import com.yea.achieve.generator.dto.table.IntrospectedTable;
import com.yea.achieve.generator.dto.table.JdbcTypeNameTranslator;
import com.yea.achieve.generator.service.GeneratorService;
import com.yea.achieve.generator.util.StringUtility;
import com.yea.core.base.dao.BaseDAO;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * 
 * @author yiyongfei
 *
 */
@Service
public class GeneratorServiceImpl implements GeneratorService, ApplicationContextAware {
	@SuppressWarnings("rawtypes")
	private BaseDAO baseDao;
	
	@SuppressWarnings("rawtypes")
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
		// TODO Auto-generated method stub
		Map<String, BaseDAO> mapDao = arg0.getBeansOfType(BaseDAO.class);
		Iterator<BaseDAO> it = mapDao.values().iterator();
		while(it.hasNext()){
			baseDao = it.next();
			break;
		}
	}
	
	public Set<String> generate(GeneratorConfig config) throws Exception{
		Set<String> tableNames = new HashSet<String>();
		config.setBuildPath(getBuildPath());
		Connection conn = baseDao.getConnection();
		try {
			Collection<String> tablenames = new HashSet<String>();
			if(config.getTableName().indexOf(";") > 0) {
				StringTokenizer st = new StringTokenizer(config.getTableName().trim(), ";");
				while(st.hasMoreTokens()) {
					tablenames.add(st.nextToken());
				}
			} else {
				tablenames.add(config.getTableName());
			}
			Collection<IntrospectedTable> tables = new HashSet<IntrospectedTable>();
			for(String tablename : tablenames) {
				tables.addAll(calculateIntrospectedTables(conn.getMetaData(), config.getCatalog(), config.getSchema(), tablename, config.isWildcardEscapingEnabled(), config.getTableTypes()));
			}
			for(IntrospectedTable table : tables) {
				tableNames.add(table.getIntrospectedTableName());
				Wrapper wrapper = new SqlmapWrapper(table, config);
				_generate(wrapper);
				
				wrapper = new PKWrapper(table, config);
				_generate(wrapper);
				
				wrapper = new EntityWrapper(table, config);
				_generate(wrapper);
				
				wrapper = new AggregateWrapper(table, config);
				_generate(wrapper);
				
				wrapper = new DaoWrapper(table, config);
				_generate(wrapper);
				
//				wrapper = new ServiceWrapper(table, config);
//				_generate(wrapper);
//				
//				wrapper = new ServiceImplWrapper(table, config);
//				_generate(wrapper);
			}
			return tableNames;
		} finally {
			conn.close();
			conn = null;
		}
	}
	
	private void _generate(Wrapper wrapper) throws IOException, TemplateException{
		if(wrapper instanceof DaoWrapper) {
			if(!wrapper.getDaoGenerateable()) {
				return;
			}
		}
		
		File file = createFile(wrapper.getFilePath() + wrapper.getFileName());
		@SuppressWarnings("deprecation")
		Configuration configuration = new Configuration();
		configuration.setClassForTemplateLoading(GeneratorServiceImpl.class, "/generationftls");
		Template template = configuration.getTemplate(wrapper.getFtlName(), Locale.CHINA, "UTF-8");
		
		Map<String, Object> rootMap = new HashMap<String, Object>();
		rootMap.put("tableWrapper", wrapper);
		
		StringWriter bw = new StringWriter();
		template.process(rootMap, bw);
		
		writeFile(bw.toString(), file);
	}
	
	private Collection<IntrospectedTable> calculateIntrospectedTables(DatabaseMetaData databaseMetaData, String catalog, String schema, String tableName, boolean isWildcardEscapingEnabled, String[] tableTypes) throws SQLException {
    	Collection<IntrospectedTable> tables = getTables(databaseMetaData, catalog, schema, tableName, isWildcardEscapingEnabled, tableTypes);  

        for (IntrospectedTable table : tables) {
        	setColumn(databaseMetaData, table);
            setPrimaryKey(databaseMetaData, table);
            setExtraColumnInfo(table);
        }
        return tables;
    }
	
	private Collection<IntrospectedTable> getTables(DatabaseMetaData databaseMetaData, String catalog, String schema, String tableName, boolean isWildcardEscapingEnabled, String[] tableTypes) throws SQLException {
        String localCatalog;
        String localSchema;
        String localTableName;

		if (databaseMetaData.storesLowerCaseIdentifiers()) {
			localCatalog = catalog == null ? null : catalog.toLowerCase();
			localSchema = schema == null ? null : schema.toLowerCase();
			localTableName = tableName == null ? null : tableName.toLowerCase();
		} else if (databaseMetaData.storesUpperCaseIdentifiers()) {
			localCatalog = catalog == null ? null : catalog.toUpperCase();
			localSchema = schema == null ? null : schema.toUpperCase();
			localTableName = tableName == null ? null : tableName.toUpperCase();
		} else {
			localCatalog = catalog;
			localSchema = schema;
			localTableName = tableName;
		}

		if (isWildcardEscapingEnabled) {
			String escapeString = databaseMetaData.getSearchStringEscape();
			StringBuilder sb = new StringBuilder();
			StringTokenizer st;
			if (localSchema != null) {
				st = new StringTokenizer(localSchema, "_", true); //$NON-NLS-1$
				while (st.hasMoreTokens()) {
					String token = st.nextToken();
					if (token.equals("_")) { //$NON-NLS-1$
						sb.append(escapeString);
					}
					sb.append(token);
				}
				localSchema = sb.toString();
			}

			sb.setLength(0);
			st = new StringTokenizer(localTableName, "_", true); //$NON-NLS-1$
			while (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.equals("_")) {
					sb.append(escapeString);
				}
				sb.append(token);
			}
			localTableName = sb.toString();
		}

		Map<String, IntrospectedTable> result = new HashMap<String, IntrospectedTable>();
        
        ResultSet rs = null;
		try {
			rs = databaseMetaData.getTables(localCatalog, localSchema, localTableName, tableTypes);
	        while (rs.next()) {
	        	String _catalog = rs.getString("TABLE_CAT");//表类别（可能为空）
	            String _schema = rs.getString("TABLE_SCHEM");//表模式（可能为空）,在oracle中获取的是命名空间
	            String _tablename = rs.getString("TABLE_NAME");//表名
	            
	            String fullTablename = StringUtility.composeFullyQualifiedTableName(_catalog, _schema, _tablename, '.');
	            
	            if(!result.containsKey(fullTablename)) {
	            	IntrospectedTable introspectedTable = new IntrospectedTable(_catalog, 
	            			_schema, 
	            			_tablename);
	            	introspectedTable.setRemarks(rs.getString("REMARKS"));
	                introspectedTable.setTableType(rs.getString("TABLE_TYPE"));
	            	result.put(fullTablename, introspectedTable);
	            }
	        	
	        }
		} finally {
			closeResultSet(rs);
		}
        return result.values();
    }
	
    private void setPrimaryKey(DatabaseMetaData databaseMetaData, IntrospectedTable introspectedTable) {
        ResultSet rs = null;
        try {
            rs = databaseMetaData.getPrimaryKeys(
            		introspectedTable.getIntrospectedCatalog(), introspectedTable.getIntrospectedSchema(), introspectedTable.getIntrospectedTableName());
        } catch (SQLException e) {
            closeResultSet(rs);
            return;
        }
        try {
            // keep primary columns in key sequence order
            Map<Short, String> keyColumns = new TreeMap<Short, String>();
            while (rs.next()) {
                String columnName = rs.getString("COLUMN_NAME"); //$NON-NLS-1$
                short keySeq = rs.getShort("KEY_SEQ"); //$NON-NLS-1$
                keyColumns.put(keySeq, columnName);
            }
            
            for (String columnName : keyColumns.values()) {
                introspectedTable.addPrimaryKeyColumn(columnName);
            }
        } catch (SQLException e) {
            // ignore the primary key if there's any error
        } finally {
            closeResultSet(rs);
        }
    }
    
	private void setExtraColumnInfo(IntrospectedTable introspectedTable) {
		JavaTypeResolver javaTypeResolver = new JavaTypeResolverDefaultImpl();
		for (IntrospectedColumn introspectedColumn : introspectedTable.getAllColumns()) {
			// 设置Java的属性名，驼峰命名法
			introspectedColumn
					.setJavaProperty(StringUtility.getCamelCaseString(introspectedColumn.getActualColumnName(), false));
			introspectedColumn.setJavaMethod(StringUtility.getCamelCaseString(introspectedColumn.getActualColumnName(), true));
			FullyQualifiedJavaType fullyQualifiedJavaType = javaTypeResolver.calculateJavaType(introspectedColumn);
			introspectedColumn.setFullyQualifiedJavaType(fullyQualifiedJavaType);
			introspectedColumn.setJdbcTypeName(javaTypeResolver.calculateJdbcTypeName(introspectedColumn));
		}
	}
    
	private void setColumn(DatabaseMetaData databaseMetaData, IntrospectedTable tableinfo) throws SQLException {
        ResultSet rs = null;
		try {
			/**
			 * 获取可在指定类别中使用的表列的描述。
			 * 方法原型:ResultSet getColumns(String catalog,String schemaPattern,String tableNamePattern,String columnNamePattern)
			 * catalog - 表所在的类别名称;""表示获取没有类别的列,null表示获取所有类别的列。
			 * schema - 表所在的模式名称(oracle中对应于Tablespace);""表示获取没有模式的列,null标识获取所有模式的列; 可包含单字符通配符("_"),或多字符通配符("%");
			 * tableNamePattern - 表名称;可包含单字符通配符("_"),或多字符通配符("%");
			 * columnNamePattern - 列名称; ""表示获取列名为""的列(当然获取不到);null表示获取所有的列;可包含单字符通配符("_"),或多字符通配符("%");
			 */
	        rs = databaseMetaData.getColumns(tableinfo.getIntrospectedCatalog(), tableinfo.getIntrospectedSchema(), tableinfo.getIntrospectedTableName(), "%");
	        while (rs.next()) {
	            IntrospectedColumn introspectedColumn = new IntrospectedColumn();

	            introspectedColumn.setActualColumnName(rs.getString("COLUMN_NAME")); //列名
	            introspectedColumn.setJdbcType(rs.getInt("DATA_TYPE")); //对应的java.sql.Types的SQL类型(列类型ID)
	            introspectedColumn.setJdbcTypeName(JdbcTypeNameTranslator.getJdbcTypeName(introspectedColumn.getJdbcType()));
	            /**
	             *  0 (columnNoNulls) - 该列不允许为空
	             *  1 (columnNullable) - 该列允许为空
	             *  2 (columnNullableUnknown) - 不确定该列是否为空
	             */
	            introspectedColumn.setNullable(rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable); //是否允许为null
	            introspectedColumn.setLength(rs.getInt("COLUMN_SIZE")); //列大小
	            introspectedColumn.setScale(rs.getInt("DECIMAL_DIGITS")); //小数位数
	            introspectedColumn.setRemarks(rs.getString("REMARKS")); //列描述
	            String defalutValue = rs.getString("COLUMN_DEF");
	            if(defalutValue != null && defalutValue.trim().length() > 0) {
	            	if(introspectedColumn.getJdbcType() == 1 || introspectedColumn.getJdbcType() == 12) {
	            		if(defalutValue.indexOf("'") != -1) {
	            			StringTokenizer st = new StringTokenizer(defalutValue, "'", false);
	            			introspectedColumn.setDefaultValue(st.nextToken()); //默认值
	            		}
	            	}
	            }
	            tableinfo.addColumn(introspectedColumn);
	        }
		} finally {
			closeResultSet(rs);
		}
    }
	
	private void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // ignore
            }
        }
    }
	
	private void writeFile(String fileContent, File file) {
		FileOutputStream fileOut = null;
        OutputStreamWriter outStream = null;
        BufferedWriter bw = null;
		try {
			fileOut = new FileOutputStream(file);
			outStream = new OutputStreamWriter(fileOut, "UTF-8");
			bw = new BufferedWriter(outStream);
			bw.write(fileContent);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			
			try {
				if(bw != null) {
					bw.close();
					bw = null;
				}
				if(outStream != null) {
					outStream.close();
					outStream = null;
				}
				if(fileOut != null) {
					fileOut.close();
					fileOut = null;
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private File createFile(String path) {
		File file = new File(path);
		if(file.exists()) {
			file.delete();
		}
        if(!file.getParentFile().exists()) {  
            if(!file.getParentFile().mkdirs()) {  
                System.err.println("文件创建不成功");  
            }  
        }
        return file;
	}
	
	private String getBuildPath() {
		java.net.URL url = GeneratorServiceImpl.class.getProtectionDomain().getCodeSource().getLocation();
		String filePath = null;
		try {
			filePath = java.net.URLDecoder.decode(url.getPath(), "utf-8");
			if (filePath.endsWith(".jar"))
				filePath = filePath.substring(0, filePath.lastIndexOf("/") + 1);
			File file = new File(filePath);
			filePath = _buildPath(file);
		} catch (Exception e) {
		}
		return filePath;
	}
	
	private String _buildPath(File file) {
		if (file.getAbsolutePath().indexOf("/target") > 0) {
			return _buildPath(file.getParentFile());
		} else {
			return file.getAbsolutePath();
		}
	}
	
}
