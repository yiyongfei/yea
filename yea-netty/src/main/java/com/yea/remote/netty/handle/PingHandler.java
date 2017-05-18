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
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.handle.observer.ChannelInboundHandlerObservable;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * 用于节点双方的Ping操作
 * @author yiyongfei
 * 
 */
public class PingHandler extends ChannelInboundHandlerObservable implements NettyChannelHandler {
    
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
				&& (message.getHeader().getType() == RemoteConstants.MessageType.PING_REQ.value()
						|| message.getHeader().getType() == RemoteConstants.MessageType.PING_RESP.value())) {
			try{
				execute(ctx, message);
			} catch (Exception ex) {
				if (RemoteConstants.MessageType.PING_RESP.value() == message.getHeader().getType()) {
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
    
	protected void execute(ChannelHandlerContext ctx, Message message) throws Exception {
		// TODO Auto-generated method stub
    	if (message.getHeader().getType() == RemoteConstants.MessageType.PING_REQ.value()) {
    		Message respMessage = new Message();
			Header header = new Header();
			header.setType(RemoteConstants.MessageType.PING_RESP.value());
			header.setSessionID(message.getHeader().getSessionID());
			header.setResult(RemoteConstants.MessageResult.SUCCESS.value());
			respMessage.setHeader(header);
			ctx.writeAndFlush(respMessage);
        } else if (message.getHeader().getType() == RemoteConstants.MessageType.PING_RESP.value()) {
			notifyObservers(message.getHeader().getSessionID(), message.getHeader(), Boolean.valueOf(true));
        } else {
            ctx.fireChannelRead(message);
        }
	}
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        PingHandler obj = null;
        obj = (PingHandler) super.clone();
        return obj;
    }
}
