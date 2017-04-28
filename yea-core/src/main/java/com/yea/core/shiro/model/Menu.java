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
package com.yea.core.shiro.model;

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
