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
package com.yea.orm.handle;

import java.util.HashMap;
import java.util.Map;

import com.yea.orm.handle.mybatis.DeleteORMHandle;
import com.yea.orm.handle.mybatis.LoadORMHandle;
import com.yea.orm.handle.mybatis.QueryListORMHandle;
import com.yea.orm.handle.mybatis.QueryORMHandle;
import com.yea.orm.handle.mybatis.SaveORMHandle;
import com.yea.orm.handle.mybatis.SqlORMHandle;
import com.yea.orm.handle.mybatis.TriggerORMHandle;
import com.yea.orm.handle.mybatis.UpdateORMHandle;

/**
 * 
 * @author yiyongfei
 *
 */
public class ORMHandleFactory {
	private ORMHandleFactory(){};
	
	public static <T> ORMHandle<T> getInstance(ORMConstants.ORM_LEVEL ormLevel){
		return Holder.map.get(ormLevel.getCode());
	}
	
	private static class Holder{
		private static Map<String, ORMHandle> map;
		static{
			map = new HashMap<String, ORMHandle>();
			map.put(ORMConstants.ORM_LEVEL.M_TRIGGER.getCode(), new TriggerORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_INSERT.getCode(), new SaveORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_DELETE.getCode(), new DeleteORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_UPDATE.getCode(), new UpdateORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_LOAD.getCode(), new LoadORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_QUERY.getCode(), new QueryORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_QUERYLIST.getCode(), new QueryListORMHandle());
			map.put(ORMConstants.ORM_LEVEL.M_SQL.getCode(), new SqlORMHandle());
		}
	}
}


