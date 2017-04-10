package com.yea.shiro.model;

import java.util.Set;

public class SystemMenu {

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
