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
package com.yea.achieve.generator.dto;

import java.util.List;

import com.yea.achieve.generator.dto.table.IntrospectedColumn;
import com.yea.achieve.generator.dto.table.IntrospectedTable;
import com.yea.achieve.generator.util.StringUtility;

/**
 * 
 * @author yiyongfei
 *
 */
public class Wrapper {
	private String fileName;
	private String filePath;
	private IntrospectedTable tableInfo;
	private String entityPackagePath;
	private String entityName;
	private String entityVar;
	private String pkPackagePath;
	private String pkName;
	private String pkVar;
	private String aggregatePackagePath;
	private String aggregateName;
	private String aggregateVar;
	private String daoPackagePath;
	private String daoName;
	private String daoVar;
	private String ftlName;
	private boolean daoGenerateable = true;//是否生成DAO
	
	public Wrapper(IntrospectedTable arg0, String tablePrefixOverrides, String moduleName, String basePackagePath) {
		this.tableInfo = arg0;
		
		String tableName = this.getTableName(tablePrefixOverrides);
		entityName = StringUtility.getCamelCaseString(tableName, true) + "Entity";
		entityVar = StringUtility.getCamelCaseString(tableName, false) + "Entity";
		entityPackagePath = basePackagePath + "." + moduleName + ".model." + "entity";
		
		pkName = StringUtility.getCamelCaseString(tableName, true) + "PK";
		pkVar = StringUtility.getCamelCaseString(tableName, false) + "PK";
		pkPackagePath = basePackagePath + "." + moduleName + ".model." + "pk";
		
		aggregateName = StringUtility.getCamelCaseString(tableName, true);
		aggregateVar = StringUtility.getCamelCaseString(tableName, false);
		aggregatePackagePath = basePackagePath + "." + moduleName + ".model";
		
		daoName = StringUtility.getCamelCaseString(tableName, true) + "Dao";
		daoVar = StringUtility.getCamelCaseString(tableName, false) + "Dao";
		daoPackagePath = basePackagePath + "." + moduleName + "." + "dao";
		
	}
	
	protected String getTableName(String tablePrefixOverrides) {
		String tableName = tableInfo.getIntrospectedTableName();
		if(tableInfo.getIntrospectedTableName().startsWith(tablePrefixOverrides)) {
			tableName = tableName.substring(tablePrefixOverrides.length());
		}
		return tableName;
	}
	
	public String getIntrospectedTableName() {
		return tableInfo.getIntrospectedTableName();
	}
	
	public List<IntrospectedColumn> getPrimaryKeyColumns() {
        return tableInfo.getPrimaryKeyColumns();
    }
	
	public List<IntrospectedColumn> getAllColumns() {
        return tableInfo.getAllColumns();
    }
	
	public List<IntrospectedColumn> getNonPrimaryKeyColumns() {
        return tableInfo.getNonPrimaryKeyColumns();
    }

	public IntrospectedTable getTableInfo() {
		return tableInfo;
	}

	public void setTableInfo(IntrospectedTable tableInfo) {
		this.tableInfo = tableInfo;
	}

	public String getEntityPackagePath() {
		return entityPackagePath;
	}

	public void setEntityPackagePath(String entityPackagePath) {
		this.entityPackagePath = entityPackagePath;
	}

	public String getEntityName() {
		return entityName;
	}

	public String getPkPackagePath() {
		return pkPackagePath;
	}

	public void setPkPackagePath(String pkPackagePath) {
		this.pkPackagePath = pkPackagePath;
	}

	public String getPkName() {
		return pkName;
	}

	public void setPkName(String pkName) {
		this.pkName = pkName;
	}

	public String getAggregatePackagePath() {
		return aggregatePackagePath;
	}

	public void setAggregatePackagePath(String aggregatePackagePath) {
		this.aggregatePackagePath = aggregatePackagePath;
	}

	public String getAggregateName() {
		return aggregateName;
	}

	public void setAggregateName(String aggregateName) {
		this.aggregateName = aggregateName;
	}

	public String getPkVar() {
		return pkVar;
	}

	public String getAggregateVar() {
		return aggregateVar;
	}

	public String getDaoPackagePath() {
		return daoPackagePath;
	}

	public void setDaoPackagePath(String daoPackagePath) {
		this.daoPackagePath = daoPackagePath;
	}

	public String getDaoName() {
		return daoName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public void setDaoName(String daoName) {
		this.daoName = daoName;
		this.daoVar = daoName.substring(0, 1).toLowerCase() + daoName.substring(1);
	}

	public String getFtlName() {
		return ftlName;
	}

	public void setFtlName(String ftlName) {
		this.ftlName = ftlName;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public String getEntityVar() {
		return entityVar;
	}

	public String getDaoVar() {
		return daoVar;
	}
	
	public boolean getDaoGenerateable() {
		return daoGenerateable;
	}
	
	public void setDaoGenerateable(boolean isGenerateDao) {
		this.daoGenerateable = isGenerateDao;
	}
}
