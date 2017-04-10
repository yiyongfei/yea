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
package com.yea.remote.netty.handle;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.handle.observer.ChannelInboundHandlerObservable;

import io.netty.channel.ChannelHandlerContext;

/**
 * 服务处理的基类
 * @author yiyongfei
 * 
 */
public abstract class AbstractServiceHandler extends ChannelInboundHandlerObservable {
    private ApplicationContext context;
    /** 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;
    }
    public ApplicationContext getApplicationContext() throws BeansException {
        return context;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	Message message = (Message) msg;
		if (message.getHeader() != null
				&& (message.getHeader().getType() == RemoteConstants.MessageType.SERVICE_REQ.value()
						|| message.getHeader().getType() == RemoteConstants.MessageType.SERVICE_RESP.value())) {
			try{
				execute(ctx, message);
			} catch (Exception ex) {
				if (RemoteConstants.MessageType.SERVICE_RESP.value() == message.getHeader().getType()) {
					message.getHeader().setType(RemoteConstants.MessageType.SUSPEND_RESP.value());
                	message.getHeader().setResult(RemoteConstants.MessageResult.FAILURE.value());
                	message.setBody(ex);
				} else {
					message.getHeader().setType(RemoteConstants.MessageType.SUSPEND_REQ.value());
                	message.getHeader().setResult(RemoteConstants.MessageResult.FAILURE.value());
                	message.setBody(ex);
				}
				ctx.fireChannelRead(msg);
			}
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    
    protected abstract void execute(ChannelHandlerContext ctx, Message message) throws Exception;
}
