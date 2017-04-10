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
package ${tableWrapper.servicePackagePath};

import java.util.List;
<#list tableWrapper.listEntityImport?if_exists as className>
import ${className.fullyQualifiedNameWithoutTypeParameters};
</#list>

import ${tableWrapper.entityPackagePath}.${tableWrapper.entityName};

<#list importList?if_exists as importPackage>  
import ${importPackage.importPackage};
</#list>

public interface ${tableWrapper.serviceName} {

	public void insert(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception;
	
	public void update(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception;
	
	public void delete(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field_has_next>,</#if></#list>) throws Exception;
	
	public ${tableWrapper.entityName} load(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field_has_next>,</#if></#list>) throws Exception;
	
	public List<${tableWrapper.entityName}> selectBySelective(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception;
	
	public int selectBySelectiveCount(${tableWrapper.entityName} ${tableWrapper.entityVar}) throws Exception;
    
	
}