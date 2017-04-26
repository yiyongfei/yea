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
package ${tableWrapper.daoPackagePath};

import java.util.List;
<#list tableWrapper.listEntityImport?if_exists as className>
import ${className.fullyQualifiedNameWithoutTypeParameters};
</#list>

import org.springframework.stereotype.Repository;

import ${tableWrapper.entityPackagePath}.${tableWrapper.entityName};
import ${tableWrapper.pkPackagePath}.${tableWrapper.pkName};
import ${tableWrapper.aggregatePackagePath}.${tableWrapper.aggregateName};

@Repository
public interface ${tableWrapper.daoName}<T> {

    public int insert(${tableWrapper.entityName} ${tableWrapper.entityVar});
    
	public int insertSelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int update(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int updateSelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int delete(${tableWrapper.pkName} ${tableWrapper.pkVar});
	
	public int deleteBySelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public ${tableWrapper.aggregateName} load(${tableWrapper.pkName} ${tableWrapper.pkVar});
	
	public List<${tableWrapper.aggregateName}> selectBySelective(${tableWrapper.aggregateName} ${tableWrapper.aggregateVar});
	
	public int selectBySelectiveCount(${tableWrapper.aggregateName} ${tableWrapper.aggregateVar});
	
}