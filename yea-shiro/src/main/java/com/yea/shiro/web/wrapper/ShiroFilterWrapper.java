package com.yea.shiro.web.wrapper;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.filter.authc.FormAuthenticationFilter;
import org.apache.shiro.web.filter.authc.LogoutFilter;
import org.apache.shiro.web.filter.authc.UserFilter;
import org.apache.shiro.web.filter.mgt.DefaultFilterChainManager;
import org.apache.shiro.web.filter.mgt.PathMatchingFilterChainResolver;
import org.apache.shiro.web.servlet.AbstractShiroFilter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import com.yea.core.exception.YeaException;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;
import com.yea.shiro.constants.ShiroConstants;
import com.yea.shiro.web.filter.authz.PermissionsAuthorizationFilter;

public class ShiroFilterWrapper implements ApplicationContextAware {
	private String usernameParam;
    private String passwordParam;
    private String rememberMeParam;
    private String logoutUrl;
    private String authenticedUrl;
    private AbstractEndpoint endpoint;
    private Boolean isReset = true;//是否需要重置
    
    public void setUsernameParam(String usernameParam) {
		this.usernameParam = usernameParam;
	}

    public void setPasswordParam(String passwordParam) {
		this.passwordParam = passwordParam;
	}

    public void setRememberMeParam(String rememberMeParam) {
		this.rememberMeParam = rememberMeParam;
	}

	public void setLogoutUrl(String redirectUrl) {
		this.logoutUrl = redirectUrl;
	}

	public void setAuthenticedUrl(String authenticedUrl) {
		this.authenticedUrl = authenticedUrl;
	}
	
	public void setEndpoint(AbstractEndpoint nettyClient) {
		endpoint = nettyClient;
	}
	
	public void setIsReset(Boolean isReset) {
		this.isReset = isReset;
	}

	private ShiroFilterFactoryBean shiroFilterBean;
	private Map<String, String> mapFilterChain;
    private Map<String, Filter> mapFilter;
    public void setShiroFilter(Filter shiroFilter) {}
    
	private Map<String, String> urlpermission = new LinkedHashMap<String, String>(); 
    @SuppressWarnings("unused")
	private long endResetTime;
	
    public void init() throws Exception {
		this.shiroFilterBean = context.getBean(ShiroFilterFactoryBean.class);
		this.mapFilterChain = this.shiroFilterBean.getFilterChainDefinitionMap();
		this.mapFilter=this.shiroFilterBean.getFilters();
		reset();
    }
    
    /**
     * 重新初始化过滤器，用于更改URL过滤器配置后对其的重置
     * @throws Exception
     */
    public void reset() throws Exception {
    	if(isReset && endpoint != null && endpoint.remoteConnects() > 0) {
    		DefaultFilterChainManager manager = (DefaultFilterChainManager) ((PathMatchingFilterChainResolver) ((AbstractShiroFilter)shiroFilterBean.getObject()).getFilterChainResolver()).getFilterChainManager();
    		// 清空初始权限配置
            manager.getFilterChains().clear();
            _resetFilterChainDefinition(endpoint);
            _resetFilter();
            manager.getFilters().putAll(shiroFilterBean.getFilters());
            Map<String, String> chains = shiroFilterBean.getFilterChainDefinitionMap();
            for (Map.Entry<String, String> entry : chains.entrySet()) {
                String url = entry.getKey();
                String chainDefinition = entry.getValue().trim().replace(" ", "");
                manager.createChain(url, chainDefinition);
            }
            endResetTime = new Date().getTime();
            isReset = false;
    	}
    }
	
	private void _resetFilterChainDefinition(AbstractEndpoint nettyClient) {
		shiroFilterBean.getFilterChainDefinitionMap().clear();
    	CallAct act = new CallAct();
		try {
			act.setActName("shiroAct");
			Promise<List<Map<String, String>>> futureRole = nettyClient.send(act, ShiroConstants.ShiroSQL.URL_FILTER_QUERY.getSql(), null);
			List<Map<String, String>> listUrlFilterMapper = futureRole.awaitObject(10000);
			for(Map<String, String> mapper : listUrlFilterMapper) {
				shiroFilterBean.getFilterChainDefinitionMap().put(mapper.get(ShiroConstants.ShiroColumn.URL_PATH.value()), mapper.get(ShiroConstants.ShiroColumn.URL_FILTER.value()));
				if (!StringUtils.isEmpty(mapper.get(ShiroConstants.ShiroColumn.PERMISSION_CONTENT.value()))) {
					urlpermission.put(mapper.get(ShiroConstants.ShiroColumn.URL_PATH.value()), mapper.get(ShiroConstants.ShiroColumn.PERMISSION_CONTENT.value()));
				}
			}
			if (!StringUtils.isEmpty(logoutUrl)) {
				shiroFilterBean.getFilterChainDefinitionMap().put(logoutUrl, "logout");
			}
			shiroFilterBean.getFilterChainDefinitionMap().put("/**", "anon");
			
			/*将配置文件里所定义的FilterChain覆盖DB里所定义的（相同URL）*/
			shiroFilterBean.getFilterChainDefinitionMap().putAll(mapFilterChain);
		} catch (Throwable e) {
			throw new YeaException(e);
		} finally {
			act = null;
		}
    }
    
    private void _resetFilter() {
    	/*重置过滤器*/
    	shiroFilterBean.getFilters().clear();
    	
		/*设置默认的认证Filter*/
		FormAuthenticationFilter authenticationFilter = new FormAuthenticationFilter();
		if (!StringUtils.isEmpty(authenticedUrl)) {
			authenticationFilter.setLoginUrl(authenticedUrl);
		}
		if (!StringUtils.isEmpty(usernameParam)) {
			authenticationFilter.setUsernameParam(usernameParam);
		}
		if (!StringUtils.isEmpty(passwordParam)) {
			authenticationFilter.setPasswordParam(passwordParam);
		}
		if (!StringUtils.isEmpty(rememberMeParam)) {
			authenticationFilter.setRememberMeParam(rememberMeParam);
		}
		shiroFilterBean.getFilters().put("authc", authenticationFilter);
    	
		UserFilter userFilter = new UserFilter();
		if (!StringUtils.isEmpty(authenticedUrl)) {
			userFilter.setLoginUrl(authenticedUrl);
		}
		shiroFilterBean.getFilters().put("user", userFilter);
		
		if (!StringUtils.isEmpty(logoutUrl)) {
			/* 设置默认的退出登录Filter */
			LogoutFilter LogoutFilter = new LogoutFilter();
			LogoutFilter.setRedirectUrl(shiroFilterBean.getLoginUrl());
			shiroFilterBean.getFilters().put("logout", LogoutFilter);
		}
		
		/*设置默认的授权Filter*/
		PermissionsAuthorizationFilter authorizationFilter = new PermissionsAuthorizationFilter();
		authorizationFilter.setLoginUrl(shiroFilterBean.getLoginUrl());
		authorizationFilter.setUnauthorizedUrl(shiroFilterBean.getUnauthorizedUrl());
		authorizationFilter.setPermissionSection(urlpermission);
		shiroFilterBean.getFilters().put("authz", authorizationFilter);
		
		shiroFilterBean.getFilters().putAll(mapFilter);
    }
    
    private ApplicationContext context;
    /** 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;
    }
}
