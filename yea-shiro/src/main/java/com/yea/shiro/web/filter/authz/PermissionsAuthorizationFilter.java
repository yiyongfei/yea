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
package com.yea.shiro.web.filter.authz;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.authz.AuthorizationFilter;
import org.apache.shiro.web.util.WebUtils;
import org.springframework.util.StringUtils;

/**
 * 
 * @author yiyongfei
 *
 */
public class PermissionsAuthorizationFilter extends AuthorizationFilter {
	private Map<String, String> permissionSection;
	
	public boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
			throws IOException {
		boolean isPermitted = true;
		if (isAuthorizedRequest(request, response)) {
			Subject subject = getSubject(request, response);
			if(subject.isAuthenticated() || subject.isRemembered()) {
				String requestURI = getPathWithinApplication(request);
				if(permissionSection == null || permissionSection.isEmpty()) {
					isPermitted = false;
				} else {
					String perms = permissionSection.get(requestURI);
					if (StringUtils.isEmpty(perms)) {
						isPermitted = false;
					}
					if (!subject.isPermitted(perms)) {
						isPermitted = false;
					}
				}
			}
		}

		return isPermitted;
	}
	
	protected String getPathWithinApplication(ServletRequest request) {
        return WebUtils.getPathWithinApplication(WebUtils.toHttp(request));
    }
	
	/**
	 * 非配置的unauthorizedUrl地址时，就需要进行授权认证
	 * @param request
	 * @param response
	 * @return
	 */
	protected boolean isAuthorizedRequest(ServletRequest request, ServletResponse response) {
        return !pathsMatch(getUnauthorizedUrl(), request);
    }

	public void setPermissionSection(Map<String, String> permissionSection) {
		this.permissionSection = permissionSection;
	}
}