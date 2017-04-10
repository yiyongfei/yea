package com.yea.shiro.web.interceptor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.ui.ModelMap;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.WebRequestInterceptor;

import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallFacadeDef;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.model.Menu;
import com.yea.shiro.model.SystemMenu;
import com.yea.shiro.model.UserPrincipal;
import com.yea.shiro.web.mgt.WebSecurityManager;

public class ShiroInterceptor implements WebRequestInterceptor {

	@Override
	public void preHandle(WebRequest request) throws Exception {
		ServletWebRequest servletRequest = (ServletWebRequest) request;
		Iterator<String> it = servletRequest.getParameterNames();
		while(it.hasNext()) {
			if(it.next().equals("menu")) {
				Subject subject = SecurityUtils.getSubject();
				if(subject.isAuthenticated() || subject.isRemembered()) {
					String requestURI = WebUtils.getPathWithinApplication(servletRequest.getRequest());
					SystemMenu menu = (SystemMenu) subject.getSession().getAttribute(ShiroConstants.SYSTEM_MENU);
					if(menu != null) {
						menu.setCurrMenu(requestURI);
					}
				}
			}
		}
	}

	@Override
	public void postHandle(WebRequest request, ModelMap model) throws Exception {
		Subject subject = SecurityUtils.getSubject();
		if((subject.isAuthenticated() || subject.isRemembered()) && model != null) {
			model.put("loginuser", subject.getPrincipal());
			if (subject.getSession().getAttribute(ShiroConstants.SYSTEM_MENU) == null) {
				_InnerMenu menu = new _InnerMenu();
				menu.menu(((WebSecurityManager)SecurityUtils.getSecurityManager()).getEndpoint(), subject);
			}
			model.put("systemMenu", subject.getSession().getAttribute(ShiroConstants.SYSTEM_MENU));
		}
	}

	@Override
	public void afterCompletion(WebRequest request, Exception ex) throws Exception {	
	}

	@SuppressWarnings("hiding")
	class MemuComparator<Menu> implements Comparator<Menu>{
		public int compare(Menu menu1, Menu menu2) {
			if (((com.yea.shiro.model.Menu) menu1).getMenuSequence() == null) {
				return -1;
			} else if (((com.yea.shiro.model.Menu) menu2).getMenuSequence() == null) {
				return 1;
			} else if (((com.yea.shiro.model.Menu) menu1).getMenuSequence() > ((com.yea.shiro.model.Menu) menu2)
					.getMenuSequence()) {
				return 1;
			} else {
				return -1;
			}
		}
	}
	
	class _InnerMenu {
		private Set<Menu> topMenu = new TreeSet<Menu>(new MemuComparator<Menu>());
		private Map<Long, Menu> mapMenu = new HashMap<Long, Menu>();
		
		public void menu(AbstractEndpoint endpoint, Subject subject) throws AuthenticationException {
			UserPrincipal user = (UserPrincipal) subject.getPrincipal();
			
			CallFacadeDef facade = new CallFacadeDef();
			facade.setCallFacadeName("shiroFacade");
			List<Map<String, Object>> listMenu = null;
			try {
				Promise<List<Map<String, Object>>> future = endpoint.send(facade, ShiroConstants.ShiroSQL.PERMISSION_MENU_QUERY.getSql(), new Long[]{user.getPartyId()});
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
					menu.setMenuPath("#");
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
			
//			user.getUserMenu().setMenus(topMenu);
			SystemMenu sysMenu = new SystemMenu();
			sysMenu.setMenus(topMenu);
			subject.getSession().setAttribute(ShiroConstants.SYSTEM_MENU, sysMenu);
		}
		
		private void parentMenu(AbstractEndpoint endpoint, List<Menu> arg) {
			StringBuffer sb = new StringBuffer();
			for(Menu menu : arg) {
				sb.append(menu.getParentMenuId()).append(",");
			}
			
			CallFacadeDef facade = new CallFacadeDef();
			facade.setCallFacadeName("shiroFacade");
			List<Map<String, Object>> listMenu = null;
			try {
				String sql = ShiroConstants.ShiroSQL.PARENT_MENU_QUERY.getSql() + "(" + sb.substring(0, sb.length() - 1) + ")";
				Promise<List<Map<String, Object>>> future = endpoint.send(facade, sql, new Object[] {});
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
