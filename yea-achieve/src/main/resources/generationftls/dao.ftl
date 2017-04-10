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

@Repository
public interface ${tableWrapper.daoName} {

    public int insert(${tableWrapper.entityName} ${tableWrapper.entityVar});
    
	public int insertSelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int update(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int updateSelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int delete(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int deleteBySelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public ${tableWrapper.entityName} load(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public List<${tableWrapper.entityName}> selectBySelective(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
	public int selectBySelectiveCount(${tableWrapper.entityName} ${tableWrapper.entityVar});
	
}