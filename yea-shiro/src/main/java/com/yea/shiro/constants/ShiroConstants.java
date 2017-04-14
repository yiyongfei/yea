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
package com.yea.shiro.constants;

/**
 * SQL语句
 * @author yiyongfei
 *
 */
public class ShiroConstants {
	
	public enum ShiroSQL {
		AUTHENTICATION_QUERY("select a.party_id as " + ShiroColumn.LOGIN_ID.value + ", b.login_name as " + ShiroColumn.LOGIN_NAME.value + ", a.login_password as " + ShiroColumn.LOGIN_PASSWORD.value + ", a.login_salt as " + ShiroColumn.LOGIN_SALT.value 
				           + "     , c.person_name as " + ShiroColumn.LOGIN_PERSON_NAME.value + ", a.is_lock as " + ShiroColumn.LOGIN_LOCK_TAG.value
				           + "  from t_user_info a, t_login_info b, t_person_info c"
				           + " where a.party_id = b.party_id and a.party_id = c.party_id and b.login_name = ?"), 
		USER_ROLES_QUERY("select c.role_id as " + ShiroColumn.ROLE_ID.value + ", c.role_type as "+ ShiroColumn.ROLE_TYPE.value + ", c.role_name as " + ShiroColumn.ROLE_NAME.value
					  + "   from t_login_info a, t_party_role_rela b, t_role_info c"
					  + "  where a.party_id = b.party_id and b.role_id = c.role_id"
					  + "    and c.is_valid = '01' and c.is_delete = '01'"
					  + "    and a.login_name = ?"),
		USER_PERMISSION_QUERY("select b.permission_id as " + ShiroColumn.PERMISSION_ID.value + ", b.permission_name as " + ShiroColumn.PERMISSION_NAME.value 
				           + "      , case when a.permission_id is null then a.permission_wildcards else b.permission_wildcards end as " + ShiroColumn.PERMISSION_CONTENT.value
				           + "   from t_role_permission_rela a "
				           + " left outer join (select d1.permission_id, d1.permission_name, d2.resource_content || ':' || d3.operation_name as permission_wildcards "
				           + "                    from t_permission_info d1, t_resource_info d2, t_operation_info d3 "
				           + "                   where d1.resource_id = d2.resource_id and d1.operation_id = d3.operation_id and d1.is_valid = '01' and d1.is_delete = '01') b "
				           + "              on a.permission_id = b.permission_id"
				           + "  where a.role_id = ?"),
		URL_FILTER_QUERY("select a.identifier_path as " + ShiroColumn.URL_PATH 
				        + "        , case when access_type = '01' then 'anon' "
				        + "                           when access_type = '02' then 'user' "
				        + "                           when access_type = '03' then 'authc' "
				        + "                           when access_type = '04' then 'authc,authz' end " + ShiroColumn.URL_FILTER.value
				        + "        , b.permission_wildcards as " + ShiroColumn.PERMISSION_CONTENT.value
                        + "     from t_resource_identifier a"
                        + "     left outer join (select d1.permission_id, d2.resource_content || ':' || d3.operation_name as permission_wildcards "
                        + "                        from t_permission_info d1, t_resource_info d2, t_operation_info d3 "
                        + "                       where d1.resource_id = d2.resource_id and d1.operation_id = d3.operation_id and d1.is_valid = '01' and d1.is_delete = '01') b on a.permission_id = b.permission_id"
                        +"     where a.identifier_type = '01'"),
		
		PERMISSION_MENU_QUERY("select a.menu_id as " + ShiroColumn.MENU_ID.value + ", a.menu_name as " + ShiroColumn.MENU_NAME.value + ", a.parent_menu_id as " + ShiroColumn.MENU_PARENT.value + ", a.menu_sequence as " + ShiroColumn.MENU_SEQ.value + ", c.identifier_path as " + ShiroColumn.URL_PATH.value 
				        + "      from t_menu_info a, t_resource_identifier c "
						+ "     where a.identifier_id = c.identifier_id "
						+ "       and exists (select identifier_id from "
						+ "          (select b.* from (select distinct a.permission_id from "
						+ "              (select t1.permission_id, t2.resource_content || ':' || t3.operation_name as permission_wildcards from t_permission_info t1, t_resource_info t2, t_operation_info t3"
						+ "                where t1.resource_id = t2.resource_id and t1.operation_id = t3.operation_id) a, "
						+ "              (select t2.permission_id, replace(t2.permission_wildcards,'*','%') as permission_wildcards from t_party_role_rela t1, t_role_permission_rela t2"
						+ "                where t1.role_id = t2.role_id and t1.party_id = ?) b where (a.permission_wildcards like b.permission_wildcards or a.permission_id = b.permission_id)"
						+ "          ) a, t_resource_identifier b where a.permission_id = b.permission_id"
						+ "          union "
						+ "          select * from t_resource_identifier where permission_id is null) b"
						+ "            where a.identifier_id = b.identifier_id)"),
		PARENT_MENU_QUERY("select a.menu_id as " + ShiroColumn.MENU_ID.value + ", a.menu_name as " + ShiroColumn.MENU_NAME.value + ", a.parent_menu_id as " + ShiroColumn.MENU_PARENT.value + ", a.menu_sequence as " + ShiroColumn.MENU_SEQ.value + ", b.identifier_path as " + ShiroColumn.URL_PATH.value 
		        + "      from t_menu_info a"
		        + "      left outer join t_resource_identifier b on a.identifier_id = b.identifier_id"
				+ "     where a.menu_id in ")
		;

		private String sql;

		private ShiroSQL(String value) {
			this.sql = value;
		}

		public String getSql() {
			return this.sql;
		}
	}
   
    public enum ShiroColumn {
    	LOGIN_ID("partyid"),
    	LOGIN_NAME("loginname"),
    	LOGIN_PASSWORD("loginpassword"),
    	LOGIN_SALT("loginsalt"),
    	LOGIN_PERSON_NAME("personname"),
    	LOGIN_LOCK_TAG("islock"),
    	ROLE_ID("roleid"),
    	ROLE_NAME("rolename"),
    	ROLE_TYPE("roletype"),
    	PERMISSION_ID("permissionid"),
    	PERMISSION_NAME("permissionname"),
    	PERMISSION_CONTENT("permissioncontent"),
    	URL_PATH("url_path"),
    	URL_FILTER("url_filter"),
    	MENU_ID("menu_id"),
    	MENU_NAME("menu_name"),
    	MENU_SEQ("menu_sequence"),
    	MENU_PARENT("parent_menu_id");
        
        private String value;
        private ShiroColumn(String value) {
            this.value = value;
        }
        public String value() {
            return this.value;
        }
    }
    
    public enum LockTag {
    	LOCK("09"),
    	UN_LOCK("01");
        
        private String value;
        private LockTag(String value) {
            this.value = value;
        }
        public String value() {
            return this.value;
        }
    }
    
    public final static int LOGIN_RETRY_LIMIT = 5;
    
    public final static String SYSTEM_MENU = "SYSTEM_MENU";
}
