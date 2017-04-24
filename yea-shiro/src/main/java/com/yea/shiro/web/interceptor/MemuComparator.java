package com.yea.shiro.web.interceptor;

import java.util.Comparator;

import com.yea.core.base.model.BaseModel;

class MemuComparator<Menu> extends BaseModel implements Comparator<Menu>{
	private static final long serialVersionUID = 1L;

	public int compare(Menu menu1, Menu menu2) {
		if (((com.yea.core.shiro.model.Menu) menu1).getMenuSequence() == null) {
			return -1;
		} else if (((com.yea.core.shiro.model.Menu) menu2).getMenuSequence() == null) {
			return 1;
		} else if (((com.yea.core.shiro.model.Menu) menu1).getMenuSequence() > ((com.yea.core.shiro.model.Menu) menu2)
				.getMenuSequence()) {
			return 1;
		} else {
			return -1;
		}
	}
}
