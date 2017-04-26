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
package ${tableWrapper.pkPackagePath};

<#list tableWrapper.listEntityImport?if_exists as className>
import ${className.fullyQualifiedNameWithoutTypeParameters};
</#list>

import com.yea.core.base.entity.BasePK;
import com.yea.core.base.id.RandomIDGennerator;

public class ${tableWrapper.pkName} extends BasePK {

    private static final long serialVersionUID = 1L;
    
<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
   <#if field.remarks?? >
    /**
     * ${(field.remarks)!}
     */
   </#if>
    private ${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty};
    
</#list>
    public ${tableWrapper.pkName}() {
    }
    
    public ${tableWrapper.pkName}(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field_has_next>,</#if></#list>) {
    <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
	    this.${field.javaProperty} = ${field.javaProperty};
	</#list>
    }

<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
    public void set${field.javaMethod}(${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}) {
        this.${field.javaProperty} = ${field.javaProperty};
    }
    public ${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} get${field.javaMethod}() {
        return this.${field.javaProperty};
    }
    
</#list>
    public boolean isEmptyPK() {
        return
        <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
	       <#if field.jdbcType == 1 || field.jdbcType == 12 >
	          (${field.javaProperty} == null || ${field.javaProperty}.trim().length() == 0)
	       <#else>
	          (${field.javaProperty} == null)
	       </#if>
	       <#if field_has_next>||</#if>
	    </#list>
               ? true : false;
    }
    
    public ${tableWrapper.pkName} generatePK() {
        return new ${tableWrapper.pkName}(
        <#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>
	       <#if field.jdbcType == 4 || field.jdbcType == -5 >
	            RandomIDGennerator.get().generate()
	       <#else>
	            null
	       </#if>
	       <#if field_has_next>,</#if>
	    </#list>
                );
    }
}
