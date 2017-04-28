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
