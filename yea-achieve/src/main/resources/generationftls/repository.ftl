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
package ${tableWrapper.repositoryPackagePath};

<#if tableWrapper.daoGenerateable > 
    <#assign sqlidPrefix=""/>
<#else>
<#assign sqlidPrefix=tableWrapper.aggregateName + ".Sqlid." + tableWrapper.aggregateName?upper_case + "_" />
import java.util.ArrayList;
</#if>
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.yea.core.base.model.BaseModel;
import ${tableWrapper.daoPackagePath}.${tableWrapper.daoName};
import ${tableWrapper.aggregatePackagePath}.${tableWrapper.aggregateName};
import ${tableWrapper.pkPackagePath}.${tableWrapper.pkName};

@Repository
public class ${tableWrapper.repositoryName} {
	
	@Autowired
	private ${tableWrapper.daoName}<BaseModel> ${tableWrapper.daoVar};
	
	public ${tableWrapper.pkName} save${tableWrapper.aggregateName} (${tableWrapper.aggregateName} ${tableWrapper.aggregateVar}) throws Exception {
		if(${tableWrapper.aggregateVar}.get${tableWrapper.pkName}() != null && !${tableWrapper.aggregateVar}.get${tableWrapper.pkName}().isEmptyPK()) {
			${tableWrapper.aggregateVar}.get${tableWrapper.entityName}().setPk(${tableWrapper.aggregateVar}.get${tableWrapper.pkName}());
		  <#if tableWrapper.daoGenerateable > 
	        ${tableWrapper.daoVar}.update(${tableWrapper.aggregateVar}.get${tableWrapper.entityName}());
	      <#else>
	        ${tableWrapper.daoVar}.update(${sqlidPrefix}UPDATE.value(), ${tableWrapper.aggregateVar}.get${tableWrapper.entityName}());
	      </#if>
		} else {
			${tableWrapper.aggregateVar}.generatePK();
		  <#if tableWrapper.daoGenerateable > 
	        ${tableWrapper.daoVar}.insert(${tableWrapper.aggregateVar}.get${tableWrapper.entityName}());
	      <#else>
	        ${tableWrapper.daoVar}.insert(${sqlidPrefix}INSERT.value(), ${tableWrapper.aggregateVar}.get${tableWrapper.entityName}());
	      </#if>
		}
		return ${tableWrapper.aggregateVar}.get${tableWrapper.pkName}();
	}
	
	public ${tableWrapper.aggregateName} load${tableWrapper.aggregateName}(${tableWrapper.pkName} ${tableWrapper.pkVar}) throws Exception {
	  <#if tableWrapper.daoGenerateable >
        return ${tableWrapper.daoVar}.load(${tableWrapper.pkVar});
      <#else>
        return (${tableWrapper.aggregateName}) ${tableWrapper.daoVar}.queryOne(${sqlidPrefix}LOAD.value(), ${tableWrapper.pkVar});
      </#if>
	}
	
	public List<${tableWrapper.aggregateName}> query${tableWrapper.aggregateName}() throws Exception {
	  <#if tableWrapper.daoGenerateable >
        return ${tableWrapper.daoVar}.selectBySelective(null);
      <#else>
        List<${tableWrapper.aggregateName}> listReturn = new ArrayList<${tableWrapper.aggregateName}>();
        List<BaseModel> list = ${tableWrapper.daoVar}.queryMany(${sqlidPrefix}SELECT_SELECTIVE.value());
        for(BaseModel model : list) {
			listReturn.add((${tableWrapper.aggregateName})model);
		}
		return listReturn;
      </#if>
	}
	
}