package com.yea.shiro.model;

import java.util.Set;

import com.yea.core.base.model.BaseModel;

public class Menu extends BaseModel {
	private static final long serialVersionUID = 1L;

	private Long menuId;
	private String menuName;
	private String menuPath;
	private Long menuSequence;
	private Long parentMenuId;
	private Set<Menu> childMenu;
	
	
	public Long getMenuId() {
		return menuId;
	}
	public void setMenuId(Long menuId) {
		this.menuId = menuId;
	}
	public Long getParentMenuId() {
		return parentMenuId;
	}
	public void setParentMenuId(Long parentMenuId) {
		this.parentMenuId = parentMenuId;
	}
	public String getMenuName() {
		return menuName;
	}
	public void setMenuName(String menuName) {
		this.menuName = menuName;
	}
	public String getMenuPath() {
		return menuPath;
	}
	public void setMenuPath(String menuPath) {
		this.menuPath = menuPath;
	}
	public Long getMenuSequence() {
		return menuSequence;
	}
	public void setMenuSequence(Long menuSequence) {
		this.menuSequence = menuSequence;
	}
	public Set<Menu> getChildMenu() {
		return childMenu;
	}
	public void setChildMenu(Set<Menu> childMenu) {
		this.childMenu = childMenu;
	}
	
}
