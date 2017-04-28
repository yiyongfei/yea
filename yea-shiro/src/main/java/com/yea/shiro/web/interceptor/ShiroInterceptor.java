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
package com.yea.shiro.web.interceptor;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.core.shiro.model.Menu;
import com.yea.core.shiro.model.SystemMenu;
import com.yea.core.shiro.model.UserPrincipal;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.web.mgt.WebSecurityManager;

public class ShiroInterceptor implements HandlerInterceptor {

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		// TODO Auto-generated method stub
		Enumeration<String> names = request.getParameterNames();
		while(names.hasMoreElements()) {
			if(names.nextElement().equals("menu")) {
				Subject subject = SecurityUtils.getSubject();
				if(subject.isAuthenticated() || subject.isRemembered()) {
					String requestURI = WebUtils.getPathWithinApplication(request);
					SystemMenu menu = (SystemMenu) subject.getSession().getAttribute(ShiroConstants.SYSTEM_MENU);
					if(menu != null) {
						menu.setCurrMenu(requestURI);
					}
				}
			}
		}
		return true;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// TODO Auto-generated method stub
		Subject subject = SecurityUtils.getSubject();
		if((subject.isAuthenticated() || subject.isRemembered()) && modelAndView != null && modelAndView.getModel() != null) {
			modelAndView.getModel().put("loginuser", subject.getPrincipal());
			if (subject.getSession().getAttribute(ShiroConstants.SYSTEM_MENU) == null) {
				_InnerMenu menu = new _InnerMenu();
				menu.menu(((WebSecurityManager)SecurityUtils.getSecurityManager()).getEndpoint(), subject);
			}
			modelAndView.getModel().put("systemMenu", subject.getSession().getAttribute(ShiroConstants.SYSTEM_MENU));
		}
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	class _InnerMenu {
		private Set<Menu> topMenu = new TreeSet<Menu>(new MemuComparator<Menu>());
		private Map<Long, Menu> mapMenu = new HashMap<Long, Menu>();
		
		public void menu(AbstractEndpoint endpoint, Subject subject) throws AuthenticationException {
			UserPrincipal user = (UserPrincipal) subject.getPrincipal();
			
			CallAct act = new CallAct();
			act.setActName("shiroAct");
			List<Map<String, Object>> listMenu = null;
			try {
				Promise<List<Map<String, Object>>> future = endpoint.send(act, ShiroConstants.ShiroSQL.PERMISSION_MENU_QUERY.getSql(), new Long[]{user.getPartyId()});
				listMenu = future.awaitObject(10000);
			} catch (Throwable e) {
	            final String message = "获取用户[" + user.getLoginName() + "]时发生了远程获取用户菜单信息失败！";
	            throw new AuthenticationException(message, e);
	        }
			List<Menu> parentMenu = new ArrayList<Menu>();
			for(Map<String, Object> map : listMenu) {
				Menu menu = new Menu();
				menu.setMenuId((Long) map.get(ShiroConstants.ShiroColumn.MENU_ID.value()));
				menu.setMenuName((String) map.get(ShiroConstants.ShiroColumn.MENU_NAME.value()));
				if (map.get(ShiroConstants.ShiroColumn.URL_PATH.value()) != null) {
					if(map.get(ShiroConstants.ShiroColumn.URL_PATH.value()).toString().indexOf("?") > 0) {
						menu.setMenuPath((String) map.get(ShiroConstants.ShiroColumn.URL_PATH.value()) + "&menu");
					} else {
						menu.setMenuPath((String) map.get(ShiroConstants.ShiroColumn.URL_PATH.value()) + "?menu");
					}
				} else {
					menu.setMenuPath("/#");
				}
				
				if (map.get(ShiroConstants.ShiroColumn.MENU_SEQ.value()) != null) {
					menu.setMenuSequence(Long.parseLong(map.get(ShiroConstants.ShiroColumn.MENU_SEQ.value()).toString()));
				}
				menu.setParentMenuId((Long) map.get(ShiroConstants.ShiroColumn.MENU_PARENT.value()));
				menu.setChildMenu(new TreeSet<Menu>(new MemuComparator<Menu>()));
				if(menu.getParentMenuId() == null) {
					mapMenu.put(menu.getMenuId(), menu);
					topMenu.add(menu);
				} else {
					parentMenu.add(menu);
				}
			}
			if(parentMenu.size() > 0) {
				parentMenu(endpoint, parentMenu);
			}
			
			SystemMenu sysMenu = new SystemMenu();
			sysMenu.setMenus(topMenu);
			subject.getSession().setAttribute(ShiroConstants.SYSTEM_MENU, sysMenu);
		}
		
		private void parentMenu(AbstractEndpoint endpoint, List<Menu> arg) {
			StringBuffer sb = new StringBuffer();
			for(Menu menu : arg) {
				sb.append(menu.getParentMenuId()).append(",");
			}
			
			CallAct act = new CallAct();
			act.setActName("shiroAct");
			List<Map<String, Object>> listMenu = null;
			try {
				String sql = ShiroConstants.ShiroSQL.PARENT_MENU_QUERY.getSql() + "(" + sb.substring(0, sb.length() - 1) + ")";
				Promise<List<Map<String, Object>>> future = endpoint.send(act, sql, new Object[] {});
				listMenu = future.awaitObject(10000);
			} catch (Throwable e) {
	            final String message = "获取用户时发生了远程获取用户菜单信息失败！";
	            throw new AuthenticationException(message, e);
	        }
			List<Menu> parentMenu = new ArrayList<Menu>();
			for(Map<String, Object> map : listMenu) {
				if (mapMenu.containsKey(map.get(ShiroConstants.ShiroColumn.MENU_ID.value()))) {
					continue;
				}
				Menu menu = new Menu();
				menu.setMenuId((Long) map.get(ShiroConstants.ShiroColumn.MENU_ID.value()));
				menu.setMenuName((String) map.get(ShiroConstants.ShiroColumn.MENU_NAME.value()));
				menu.setMenuPath(map.get(ShiroConstants.ShiroColumn.URL_PATH.value()) != null ? (String) map.get(ShiroConstants.ShiroColumn.URL_PATH.value()) : "#");
				if (map.get(ShiroConstants.ShiroColumn.MENU_SEQ.value()) != null) {
					menu.setMenuSequence(Long.parseLong(map.get(ShiroConstants.ShiroColumn.MENU_SEQ.value()).toString()));
				}
				menu.setParentMenuId((Long) map.get(ShiroConstants.ShiroColumn.MENU_PARENT.value()));
				menu.setChildMenu(new TreeSet<Menu>(new MemuComparator<Menu>()));
				if(menu.getParentMenuId() == null) {
					mapMenu.put(menu.getMenuId(), menu);
					topMenu.add(menu);
				} else {
					parentMenu.add(menu);
				}
			}
			for(Menu menu : arg) {
				mapMenu.get(menu.getParentMenuId()).getChildMenu().add(menu);
			}
			
			if(parentMenu.size() > 0) {
				parentMenu(endpoint, parentMenu);
			}
			
		}
	}

}
