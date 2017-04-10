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
package ${tableWrapper.aggregatePackagePath};

<#list tableWrapper.listEntityImport?if_exists as className>
import ${className.fullyQualifiedNameWithoutTypeParameters};
</#list>

import com.yea.core.base.entity.BaseEntity;
import ${tableWrapper.entityPackagePath}.${tableWrapper.entityName};
import ${tableWrapper.pkPackagePath}.${tableWrapper.pkName};

public class ${tableWrapper.aggregateName} extends BaseEntity {

    private static final long serialVersionUID = 1L;
    
    private ${tableWrapper.pkName} ${tableWrapper.pkVar};
	private ${tableWrapper.entityName} ${tableWrapper.entityVar};
	
	public ${tableWrapper.aggregateName}() {
		this.${tableWrapper.pkVar} = new ${tableWrapper.pkName}();
		this.${tableWrapper.entityVar} = new ${tableWrapper.entityName}();
	}
	
	public ${tableWrapper.aggregateName}(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.fullyQualifiedJavaType.shortNameWithoutTypeArguments} ${field.javaProperty}<#if field_has_next>, </#if></#list>) {
		this.${tableWrapper.pkVar} = new ${tableWrapper.pkName}(<#list tableWrapper.tableInfo.primaryKeyColumns?if_exists as field>${field.javaProperty}<#if field_has_next>, </#if></#list>);
		this.${tableWrapper.entityVar} = new ${tableWrapper.entityName}();
		this.${tableWrapper.entityVar}.setPk(this.${tableWrapper.pkVar});
	}
	
	public ${tableWrapper.aggregateName}(${tableWrapper.entityName} ${tableWrapper.entityVar}) {
		this.${tableWrapper.entityVar} = ${tableWrapper.entityVar};
		this.${tableWrapper.pkVar} = (${tableWrapper.pkName}) this.${tableWrapper.entityVar}.getPk();
	}
	
	public ${tableWrapper.pkName} get${tableWrapper.pkName}() {
		return ${tableWrapper.pkVar};
	}
	
	public ${tableWrapper.entityName} get${tableWrapper.entityName}() {
		return ${tableWrapper.entityVar};
	}
	
	@Override
	public void generatePK() {
		this.${tableWrapper.pkVar} = ${tableWrapper.pkVar}.generatePK();
		this.${tableWrapper.entityVar}.setPk(this.${tableWrapper.pkVar});
	}
	
  <#if tableWrapper.daoGenerateable > 
    
  <#else>
    <#assign sqlidPrefix=tableWrapper.aggregateVar + "_" />
    public enum Sqlid {
        ${sqlidPrefix?upper_case}INSERT("${sqlidPrefix}insert"),
        ${sqlidPrefix?upper_case}INSERT_SELECTIVE("${sqlidPrefix}insertSelective"),
        ${sqlidPrefix?upper_case}UPDATE("${sqlidPrefix}update"),
        ${sqlidPrefix?upper_case}UPDATE_SELECTIVE("${sqlidPrefix}updateSelective"),
        ${sqlidPrefix?upper_case}DELETE("${sqlidPrefix}delete"),
        ${sqlidPrefix?upper_case}DELETE_SELECTIVE("${sqlidPrefix}deleteBySelective"),
        ${sqlidPrefix?upper_case}LOAD("${sqlidPrefix}load"),
        ${sqlidPrefix?upper_case}SELECT_SELECTIVE("${sqlidPrefix}selectBySelective"),
        ${sqlidPrefix?upper_case}COUNT_SELECTIVE("${sqlidPrefix}selectBySelectiveCount");
        
        private String value;
        private Sqlid(String value) {
            this.value = value;
        }
        public String value() {
            return this.value;
        }
    }
  </#if>
}
