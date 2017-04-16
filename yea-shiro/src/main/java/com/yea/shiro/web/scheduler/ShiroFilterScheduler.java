package com.yea.shiro.web.scheduler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.yea.shiro.web.wrapper.ShiroFilterWrapper;

/**
 * 定时器，每隔一定时间尝试重置ShiroFilter的权限内容
 * 是否重置由ShiroFilterWrapper的重置标识决定
 * @author yiyongfei
 *
 */
@Component
@Lazy(false)
public class ShiroFilterScheduler implements ApplicationContextAware {
	private ApplicationContext context;
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;
    }
    
    @Scheduled(cron="0/30 * * * * ?") //每30秒执行一次
	public void scheduler() {
		ShiroFilterWrapper shiroFilterWrapper = (ShiroFilterWrapper) context.getBean(ShiroFilterWrapper.class);
		try {
			shiroFilterWrapper.reset();
		} catch (Exception e) {
		}
	}
}
