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

/**
 * 
 * @author yiyongfei
 *
 */
public class ORMConstants {

	public enum ORM_LEVEL {
		M_INSERT("M_INSERT", "保存"), 
		M_UPDATE("M_UPDATE", "更新"), 
		M_DELETE("M_DELETE", "删除"), 
		M_LOAD("M_LOAD", "根据PK查看"), 
		M_QUERY("M_QUERY", "查找"), 
		M_QUERYLIST("M_QUERYLIST", "查找一批数据"), 
		M_TRIGGER("M_TRIGGER", "ֻ触发DB操作"), 
		M_SQL("M_SQL", "使用Mybitis进行SQL操作");
		
		private String code;
		private String desc;

		private ORM_LEVEL(String code, String desc) {
			this.code = code;
			this.desc = desc;
		}

		public String getCode() {
			return code;
		}
		public String getDesc() {
			return desc;
		}

	};
}
