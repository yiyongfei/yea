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
package ${tableWrapper.serviceimplPackagePath};

import java.util.List;
<#list tableWrapper.listEntityImport?if_exists as className>
import ${className.fullyQualifiedNameWithoutTypeParameters};
</#list>

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ${tableWrapper.servicePackagePath}.${tableWrapper.serviceName};
import ${tableWrapper.daoPackagePath}.${tableWrapper.daoName};
import ${tableWrapper.entityPackagePath}.${tableWrapper.entityName};

  <#if tableWrapper.daoGenerateable > 
    <#assign sqlidPrefix=""/>
  <#else>
    <#assign sqlidPrefix=tableWrapper.entityVar + "_" />
  </#if>
@Service
public class ${tableWrapper.serviceimplName} implements ${tableWrapper.serviceName}{
	
	@Autowired
	private ${tableWrapper.daoName} ${tableWrapper.daoVar};
	
    @Override
    public void insert(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception {
      <#if tableWrapper.daoGenerateable > 
        ${tableWrapper.daoVar}.insert(${tableWrapper.entityVar});
      <#else>
        ${tableWrapper.daoVar}.save("${sqlidPrefix}insert", ${tableWrapper.entityVar});
      </#if>
    }
	
	@Override
	public void update(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception {
	  <#if tableWrapper.daoGenerateable > 
        ${tableWrapper.daoVar}.update(${tableWrapper.entityVar});
      <#else>
        ${tableWrapper.daoVar}.update("${sqlidPrefix}update", ${tableWrapper.entityVar});
      </#if>
	}
	
	@Override
	public void delete(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field_has_next>,</#if></#list>) throws Exception {
	    ${tableWrapper.entityName} ${tableWrapper.entityVar} = new ${tableWrapper.entityName}();
	    <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
	    ${tableWrapper.entityVar}.set${field.javaMethod}(${field.javaProperty});
	    </#list>
	  <#if tableWrapper.daoGenerateable > 
        ${tableWrapper.daoVar}.delete(${tableWrapper.entityVar});
      <#else>
        ${tableWrapper.daoVar}.delete("${sqlidPrefix}delete", ${tableWrapper.entityVar});
      </#if>
	}
	
	@Override
	public ${tableWrapper.entityName} load(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field_has_next>,</#if></#list>) throws Exception {
	    ${tableWrapper.entityName} ${tableWrapper.entityVar} = new ${tableWrapper.entityName}();
	    <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
	    ${tableWrapper.entityVar}.set${field.javaMethod}(${field.javaProperty});
	    </#list>
	  <#if tableWrapper.daoGenerateable > 
        return ${tableWrapper.daoVar}.load(${tableWrapper.entityVar});
      <#else>
        return (${tableWrapper.entityName}) ${tableWrapper.daoVar}.load("${sqlidPrefix}load", ${tableWrapper.entityVar});
      </#if>
	}
	
	@Override
	public List<${tableWrapper.entityName}> selectBySelective(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception {
	  <#if tableWrapper.daoGenerateable > 
        return ${tableWrapper.daoVar}.selectBySelective(${tableWrapper.entityVar});
      <#else>
        return ${tableWrapper.daoVar}.queryMany("${sqlidPrefix}selectBySelective", ${tableWrapper.entityVar});
      </#if>
	}
	
	@Override
	public int selectBySelectiveCount(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception {
	  <#if tableWrapper.daoGenerateable > 
        return ${tableWrapper.daoVar}.selectBySelectiveCount(${tableWrapper.entityVar});
      <#else>
        return (int) ${tableWrapper.daoVar}.queryOne("${sqlidPrefix}selectBySelectiveCount", ${tableWrapper.entityVar});
      </#if>
	}
    

}