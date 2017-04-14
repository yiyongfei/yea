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
package com.yea.shiro.realm;

import org.apache.shiro.authc.AccountException;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.yea.core.exception.YeaException;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.shiro.model.UserPrincipal;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.constants.ShiroConstants.ShiroColumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

/**
 * 
 * @author yiyongfei
 *
 */
public abstract class AbstractRealm extends AuthorizingRealm {
    private static final Logger log = LoggerFactory.getLogger(AbstractRealm.class);
    
    /**
     * Password hash salt configuration. <ul>
     *   <li>NO_SALT - password hashes are not salted.</li>
     *   <li>COLUMN - salt is in a separate column in the database.</li></ul>
     */
    public enum SaltStyle {NO_SALT, COLUMN};

    protected boolean permissionsLookupEnabled = false;
    
    private SaltStyle saltStyle = SaltStyle.COLUMN;

    /**
     * Enables lookup of permissions during authorization.  The default is "false" - meaning that only roles
     * are associated with a user.  Set this to true in order to lookup roles <b>and</b> permissions.
     *
     * @param permissionsLookupEnabled true if permissions should be looked up during authorization, or false if only
     *                                 roles should be looked up.
     */
    public void setPermissionsLookupEnabled(boolean permissionsLookupEnabled) {
        this.permissionsLookupEnabled = permissionsLookupEnabled;
    }
    
    public void setSaltStyle(SaltStyle saltStyle) {
        this.saltStyle = saltStyle;
    }

    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();
        if (username == null) {
            throw new AccountException("认证前请先提供欲认证用户的用户名");
        }

    	Map<String, Object> mapUser = getUser(username);
        String password = null;
        String salt = null;
        switch (saltStyle) {
        case NO_SALT:
            password = ((String)mapUser.get(ShiroConstants.ShiroColumn.LOGIN_PASSWORD.value())).trim();
            break;
        case COLUMN:
            password = ((String)mapUser.get(ShiroConstants.ShiroColumn.LOGIN_PASSWORD.value())).trim();
            salt = ((String)mapUser.get(ShiroConstants.ShiroColumn.LOGIN_SALT.value())).trim();
        }
        if(password == null) {
        	throw new UnknownAccountException("未发现欲认证用户[" + username + "]的账号密码");
        }
        if(ShiroConstants.LockTag.LOCK.value().equals(mapUser.get(ShiroConstants.ShiroColumn.LOGIN_LOCK_TAG.value()))) {
        	throw new LockedAccountException("认证用户[" + username + "]账号已被锁定");
        }
        UserPrincipal user = new UserPrincipal();
        user.setLoginName(username);
        user.setPartyId((Long)mapUser.get(ShiroConstants.ShiroColumn.LOGIN_ID.value()));
        user.setPersonName((String)mapUser.get(ShiroConstants.ShiroColumn.LOGIN_PERSON_NAME.value()));
        user.setIsLock((String)mapUser.get(ShiroConstants.ShiroColumn.LOGIN_LOCK_TAG.value()));
        
        SimpleAuthenticationInfo info = new SimpleAuthenticationInfo(user, password.toCharArray(), getName());
        if (salt != null) {
            info.setCredentialsSalt(ByteSource.Util.bytes(salt));
        }
        
        return info;
    }
    
    protected abstract Map<String, Object> getUser(String username) throws AuthenticationException;

    /**
     * This implementation of the interface expects the principals collection to return a String username keyed off of
     * this realm's {@link #getName() name}
     *
     * @see #getAuthorizationInfo(org.apache.shiro.subject.PrincipalCollection)
     */
    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
    	//null usernames are invalid
    	if (principals == null) {
        	//PrincipalCollection method argument cannot be null.
            throw new AuthorizationException("认证结果不能为NULL！");
        }

    	UserPrincipal user = (UserPrincipal) getAvailablePrincipal(principals);

        Set<String> roleNames = null;
        Set<String> permissions = null;
        try {
        	Set<String>[] rolepermission = getRolePermissionsForUser(user.getLoginName());
        	roleNames = rolepermission[0];
        	permissions = rolepermission[1];
        } catch (Throwable e) {
            final String message = "为用户[" + user.getLoginName() + "]授权时发生了远程获取授权信息失败！";
            if (log.isErrorEnabled()) {
                log.error(message, e);
            }
            throw new AuthorizationException(message, e);
        }

        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo(roleNames);
        info.setStringPermissions(permissions);
        return info;
    }
    
	@SuppressWarnings("unchecked")
	protected Set<String>[] getRolePermissionsForUser(String username) throws Throwable {
    	Set<String> roleNames = new LinkedHashSet<String>();
    	Set<String> permissions = null;
    	if (permissionsLookupEnabled) {
    		permissions = new LinkedHashSet<String>();
    	}
    	
    	CallAct act = new CallAct();
		act.setActName("shiroAct");
    	List<Map<String, Object>> listRole = getRoles(username);
		for(Map<String, Object> mapRole : listRole) {
    		roleNames.add(String.valueOf(mapRole.get(ShiroColumn.ROLE_NAME.value())));
    		if (permissions != null) {
    	    	List<Map<String, String>> listPermission = getPermissions(mapRole.get(ShiroColumn.ROLE_ID.value()));
    			for(Map<String, String> mapPermission : listPermission) {
    				permissions.add(mapPermission.get(ShiroColumn.PERMISSION_CONTENT.value()));
    				if(!StringUtils.isEmpty(mapPermission.get(ShiroColumn.PERMISSION_NAME.value()))) {
    					permissions.add(mapPermission.get(ShiroColumn.PERMISSION_NAME.value()));
    				}
    			}
    		}
    	}
    	return new Set[]{roleNames, permissions};
    }
    
    protected abstract List<Map<String, Object>> getRoles(String username) throws Throwable;
    protected abstract List<Map<String, String>> getPermissions(Object roleId) throws Throwable;
    
    //支持的运算符和运算符优先级
    private static final Map<String, Integer> expMap = new HashMap<String, Integer>(){
    	private static final long serialVersionUID = 1L;
	{
        put("not",0);
        put("!"  ,0);

        put("and",0);
        put("&&" ,0);

        put("or" ,0);
        put("||" ,0);

        put("("  ,1);
        put(")"  ,1);
    }};
    private static final Set<String> expList = expMap.keySet();
    
    @Override
    public boolean isPermitted(PrincipalCollection principals, String permission) {
        Stack<String> exp = getExp(expList, permission);
        if (exp.size() == 1){
            return super.isPermitted(principals, exp.pop());
        }
        List<String> expTemp = new ArrayList<>();
        //将其中的权限字符串解析成true , false
        for(String temp : exp){
            if (expList.contains(temp)){
                expTemp.add(temp);
            }else{
                expTemp.add(Boolean.toString(super.isPermitted(principals, temp)) );
            }
        }
        //计算逆波兰
        return computeRpn(expList, expTemp);
    }
    
    private static boolean computeRpn(Collection<String> expList,Collection<String> exp) {
    	log.debug("RPN  exp :{}", exp);
        Stack<Boolean> stack = new Stack<>();
        for(String temp : exp){
            if (expList.contains(temp)){
                if ("!".equals(temp) || "not".equals(temp)){
                    stack.push( !stack.pop() );
                }else if ("and".equals(temp) || "&&".equals(temp)){
                    Boolean s1 = stack.pop();
                    Boolean s2 = stack.pop();
                    stack.push(s1 && s2);
                }else{
                    Boolean s1 = stack.pop();
                    Boolean s2 = stack.pop();
                    stack.push(s1 || s2);
                }
            }else{
                stack.push(Boolean.parseBoolean(temp));
            }
        }
        if (stack.size() > 1){
            throw new YeaException("compute error！ stack: "+ exp.toString());
        }else{
            return stack.pop();
        }
    }

    //获得逆波兰表达式
    private static Stack<String> getExp(Collection<String> expList, String exp) {
        Stack<String> s1 = new Stack<>();
        Stack<String> s2 = new Stack<>();
        for (String str : exp.split(" ")){
            str = str.trim();
            String strL = str.toLowerCase();
            if ("".equals(str)){
                continue;
            }
            if ("(".equals(str)){
                //左括号
                s1.push(str);
            }else if (")".equals(str)){
                //右括号
                while(!s1.empty()){
                    String temp = s1.pop();
                    if ("(".equals(temp)){
                        break;
                    }else{
                        s2.push(temp);
                    }
                }
            }else if(expList.contains(strL)){
                //操作符
                if (s1.empty()){
                    s1.push(strL);
                }else {
                    String temp = s1.peek();
                    if ("(".equals(temp) || ")".equals(temp)){
                        s1.push(strL);
                    }else if(expMap.get(strL) >= expMap.get(temp)){
                        s1.push(strL);
                    }else{
                        s2.push(s1.pop());
                        s1.push(strL);
                    }
                }
            }else{
                //运算数
                s2.push(str);
            }
        }
        while(!s1.empty()){
            s2.push(s1.pop());
        }
        return s2;
    }
}
