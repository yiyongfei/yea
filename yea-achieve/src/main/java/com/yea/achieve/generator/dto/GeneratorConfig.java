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

import com.yea.core.base.model.BaseModel;

public class GeneratorConfig extends BaseModel{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6905915447368712061L;
	
	private String buildPath;//文件创建的根路径（src的父目录）
	private String moduleName;//模块名
	private String basePackagePath;//根包名
	private boolean daoGenerateable = true;//是否生成DAO
	private String commonDaoPackagePath = "com.yea.achieve.common.dao";//公共DAO的包名
	private String commonDaoName = "CommonDao";//公共DAO的类名
	private String catalog;
	private String schema;
	private String tableName;
	private String tablePrefixOverrides;//表欲去除的前缀，用于生成Entity、Dao等
	private boolean isWildcardEscapingEnabled = true;//获取表结构时是否模糊匹配
	private String[] tableTypes = new String[] {"TABLE"};
	
	public String getBuildPath() {
		return buildPath;
	}
	public void setBuildPath(String buildPath) {
		this.buildPath = buildPath;
	}
	public String getModuleName() {
		return moduleName;
	}
	public void setModuleName(String moduleName) {
		this.moduleName = moduleName;
	}
	public String getBasePackagePath() {
		return basePackagePath;
	}
	public void setBasePackagePath(String basePackagePath) {
		this.basePackagePath = basePackagePath;
	}
	public boolean getDaoGenerateable() {
		return daoGenerateable;
	}
	public void setDaoGenerateable(boolean isGenerateDao) {
		this.daoGenerateable = isGenerateDao;
	}
	public String getCommonDaoPackagePath() {
		return commonDaoPackagePath;
	}
	public String getCommonDaoName() {
		return commonDaoName;
	}
	
	public void setCommonDaoPackagePath(String commonDaoPackagePath) {
		this.commonDaoPackagePath = commonDaoPackagePath;
	}
	public void setCommonDaoName(String commonDaoName) {
		this.commonDaoName = commonDaoName;
	}
	public String getCatalog() {
		return catalog;
	}
	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
	public String getTableName() {
		return tableName;
	}
	public void setTableName(String tableName) {
		this.tableName = tableName;
	}
	
	public String getTablePrefixOverrides() {
		return tablePrefixOverrides;
	}
	public void setTablePrefixOverrides(String tablePrefixOverrides) {
		this.tablePrefixOverrides = tablePrefixOverrides;
	}
	public boolean isWildcardEscapingEnabled() {
		return isWildcardEscapingEnabled;
	}
	public void setWildcardEscapingEnabled(boolean isWildcardEscapingEnabled) {
		this.isWildcardEscapingEnabled = isWildcardEscapingEnabled;
	}
	public String[] getTableTypes() {
		return tableTypes;
	}
	public void setTableTypes(String[] tableTypes) {
		this.tableTypes = tableTypes;
	}
	
}
