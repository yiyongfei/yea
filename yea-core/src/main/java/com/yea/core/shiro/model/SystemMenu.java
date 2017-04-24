package com.yea.core.shiro.model;

import java.util.Set;

import com.yea.core.base.model.BaseModel;

public class SystemMenu extends BaseModel {
	private static final long serialVersionUID = 1L;

	private String currMenu;
	private Set<Menu> menus;
	public String getCurrMenu() {
		return currMenu;
	}
	public void setCurrMenu(String currMenu) {
		this.currMenu = currMenu;
	}
	public Set<Menu> getMenus() {
		return menus;
	}
	public void setMenus(Set<Menu> menus) {
		this.menus = menus;
	}
	
}
