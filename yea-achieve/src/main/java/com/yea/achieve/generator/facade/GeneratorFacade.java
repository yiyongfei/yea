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
package com.yea.achieve.generator.facade;


import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.yea.achieve.generator.dto.GeneratorConfig;
import com.yea.achieve.generator.service.GeneratorService;
import com.yea.core.base.facade.AbstractFacade;


/**
 * 
 * @author yiyongfei
 * 
 */
@Component("generatorFacade")
public class GeneratorFacade extends AbstractFacade {
	
	@Autowired
	private GeneratorService generatorService;
    /** 
     * @throws Exception 
     * @see com.jbs.remote.facade.AbstractFacade#perform(java.lang.Object[])
     */
    @SuppressWarnings("unchecked")
	@Override
    protected Object perform(Object... messages) throws Exception {
    	Map<String, String> params = (Map<String, String>) messages[0];
    	GeneratorConfig config = new GeneratorConfig();
    	config.setModuleName(params.get("moduleName"));
    	config.setBasePackagePath(params.get("basePackagePath"));
    	config.setTableName(params.get("tableName"));
    	config.setTablePrefixOverrides(params.get("tablePrefixOverrides"));
    	if (!StringUtils.isEmpty(params.get("daoGenerateable")) && params.get("daoGenerateable").trim().equals("false")) {
    		config.setDaoGenerateable(false);
    		config.setCommonDaoPackagePath(params.get("commonDaoPackagePath"));
    		config.setCommonDaoName(params.get("commonDaoName"));
    	}
    	
    	return generatorService.generate(config);
    }
    
}
