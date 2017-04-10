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
package ${tableWrapper.entityPackagePath};

<#list tableWrapper.listEntityImport?if_exists as className>
import ${className.fullyQualifiedNameWithoutTypeParameters};
</#list>

import com.yea.core.base.entity.BaseEntity;
import ${tableWrapper.pkPackagePath}.${tableWrapper.pkName};

public class ${tableWrapper.entityName} extends BaseEntity {

    private static final long serialVersionUID = 1L;
    
<#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as field>
   <#if field.remarks?? >
    /**
     * ${(field.remarks)!}
     */
   </#if>
   <#--数据库字段类型是Date类型-->
   <#if field.jdbcType == 91 >
    @DateTimeFormat(iso=DateTimeFormat.ISO.DATE)
   </#if>
   <#--数据库字段类型是Time类型-->
   <#if field.jdbcType == 92 >
    @DateTimeFormat(pattern = "HH:mm:ss")
   </#if>
   <#--数据库字段类型是Timestamp类型-->
   <#if field.jdbcType == 93 >
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
   </#if>
    private ${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field.defaultValue?? > = "${field.defaultValue}"</#if>;
    
</#list>
    public ${tableWrapper.entityName}() {
        super(new ${tableWrapper.pkName}());
    }
    
    public ${tableWrapper.entityName}(${tableWrapper.pkName} pk) {
        super(pk);
    }

<#list tableWrapper.tableInfo.nonPrimaryKeyColumns?if_exists as field>
    public void set${field.javaMethod}(${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}) {
        this.${field.javaProperty} = ${field.javaProperty};
    }
    public ${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} get${field.javaMethod}() {
        return this.${field.javaProperty};
    }
    
</#list>  
}
