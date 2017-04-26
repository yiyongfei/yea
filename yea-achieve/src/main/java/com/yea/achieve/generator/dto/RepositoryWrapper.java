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
public class RepositoryWrapper extends Wrapper{
	private String repositoryPackagePath;
	private String repositoryName;
	
	public RepositoryWrapper(IntrospectedTable arg0, GeneratorConfig config) {
		super(arg0, config.getTablePrefixOverrides(), config.getModuleName(), config.getBasePackagePath());
		
		if(!config.getDaoGenerateable()) {
			this.setDaoPackagePath(config.getCommonDaoPackagePath());
			this.setDaoName(config.getCommonDaoName());
			this.setDaoGenerateable(config.getDaoGenerateable());
		}
		
		String tableName = this.getTableName(config.getTablePrefixOverrides());
		repositoryName = StringUtility.getCamelCaseString(tableName, true) + "Repository";
		repositoryPackagePath = config.getBasePackagePath() + "." + config.getModuleName() + "." + "repository";
		
		String temp = this.repositoryPackagePath;
		temp = temp.replaceAll("\\.", "\\/");
		if(config.getBuildPath().endsWith("/") || config.getBuildPath().endsWith("\\")) {
			this.setFilePath(config.getBuildPath() + "src/main/java/" + temp + "/");
		} else {
			this.setFilePath(config.getBuildPath() + "/src/main/java/" + temp + "/");
		}
		this.setFileName(this.repositoryName + ".java");
		
		this.setFtlName("repository.ftl");
	}

	public String getRepositoryPackagePath() {
		return repositoryPackagePath;
	}

	public String getRepositoryName() {
		return repositoryName;
	}	
	
}
