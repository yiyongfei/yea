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

import java.util.ArrayList;
import java.util.List;

import com.yea.achieve.generator.dto.java.FullyQualifiedJavaType;
import com.yea.achieve.generator.dto.table.IntrospectedColumn;
import com.yea.achieve.generator.dto.table.IntrospectedTable;
import com.yea.achieve.generator.util.StringUtility;

/**
 * （未使用）
 * @author yiyongfei
 *
 */
public class ServiceWrapper extends Wrapper{
	private List<FullyQualifiedJavaType> listEntityImport = new ArrayList<FullyQualifiedJavaType>();
	private String servicePackagePath;
	private String serviceName;
	
	public ServiceWrapper(IntrospectedTable arg0, GeneratorConfig config) {
		super(arg0, config.getTablePrefixOverrides(), config.getModuleName(), config.getBasePackagePath());
		
		for(IntrospectedColumn column : arg0.getAllColumns()) {
			if(!column.getFullyQualifiedJavaType().getFullyQualifiedNameWithoutTypeParameters().startsWith("java.lang.")) {
				if(!listEntityImport.contains(column.getFullyQualifiedJavaType())) {
					listEntityImport.add(column.getFullyQualifiedJavaType());
				}
			}
		}

		if(!config.getDaoGenerateable()) {
			this.setDaoPackagePath(config.getCommonDaoPackagePath());
			this.setDaoName(config.getCommonDaoName());
			this.setDaoGenerateable(config.getDaoGenerateable());
		}
		
		String tableName = this.getTableName(config.getTablePrefixOverrides());
		serviceName = StringUtility.getCamelCaseString(tableName, true) + "Service";
		servicePackagePath = config.getBasePackagePath() + "." + config.getModuleName() + "." + "service";
		
		String temp = this.servicePackagePath;
		temp = temp.replaceAll("\\.", "\\/");
		if(config.getBuildPath().endsWith("/") || config.getBuildPath().endsWith("\\")) {
			this.setFilePath(config.getBuildPath() + "src/main/java/" + temp + "/");
		} else {
			this.setFilePath(config.getBuildPath() + "/src/main/java/" + temp + "/");
		}
		this.setFileName(this.serviceName + ".java");
		
		this.setFtlName("service.ftl");
	}

	public String getServicePackagePath() {
		return servicePackagePath;
	}

	public String getServiceName() {
		return serviceName;
	}	
	
	public List<FullyQualifiedJavaType> getListEntityImport() {
		return listEntityImport;
	}	
}
