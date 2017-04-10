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

import com.yea.achieve.generator.dto.table.IntrospectedTable;
import com.yea.achieve.generator.util.StringUtility;

/**
 * （未使用）
 * @author yiyongfei
 *
 */
public class ServiceImplWrapper extends ServiceWrapper{
	private String serviceimplPackagePath;
	private String serviceimplName;
	
	public ServiceImplWrapper(IntrospectedTable arg0, GeneratorConfig config) {
		super(arg0, config);
		
		String tableName = this.getTableName(config.getTablePrefixOverrides());
		serviceimplName = StringUtility.getCamelCaseString(tableName, true) + "ServiceImpl";
		serviceimplPackagePath = this.getServicePackagePath() + ".impl";
		
		String temp = this.serviceimplPackagePath;
		temp = temp.replaceAll("\\.", "\\/");
		if(config.getBuildPath().endsWith("/") || config.getBuildPath().endsWith("\\")) {
			this.setFilePath(config.getBuildPath() + "src/main/java/" + temp + "/");
		} else {
			this.setFilePath(config.getBuildPath() + "/src/main/java/" + temp + "/");
		}
		this.setFileName(this.serviceimplName + ".java");
		
		this.setFtlName("serviceimpl.ftl");
	}

	public String getServiceimplPackagePath() {
		return serviceimplPackagePath;
	}

	public String getServiceimplName() {
		return serviceimplName;
	}	
	
}
