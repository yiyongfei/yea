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
package com.yea.dispatcher.netty.handle;

import java.net.SocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.handle.observer.ChannelInboundHandlerObservable;

/**
 * 用于客户端的服务机制
 * @author yiyongfei
 */
public class DispatchClientHandler extends ChannelInboundHandlerObservable implements NettyChannelHandler {
	private Map<SocketAddress, byte[]> mapConsumer = new ConcurrentHashMap<SocketAddress, byte[]>();
	
	public DispatchClientHandler () {
		super();
		cacheObserver = null;
	}
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	Message message = (Message) msg;
    	if (message.getHeader() != null
				&& message.getHeader().getType() == RemoteConstants.MessageType.CONSUMER_REGISTER_RESULT.value()) {
    		SocketAddress address = (SocketAddress) message.getBody();
			mapConsumer.put(address, message.getHeader().getSessionID());
    	} else if (message.getHeader() != null
				&& message.getHeader().getType() == RemoteConstants.MessageType.CONSUMER_LOGOUT_RESULT.value()) {
    		SocketAddress address = (SocketAddress) message.getBody();
    		byte[] sessionID = mapConsumer.get(address);
			mapConsumer.remove(address);
			notifyObservers(sessionID, message.getHeader(), address);
    	} else if (message.getHeader() != null
				&& message.getHeader().getType() == RemoteConstants.MessageType.PROVIDER_DISCOVER_RESULT.value()) {
			notifyObservers(message.getHeader().getSessionID(), message.getHeader(), message.getBody());
		} else if (message.getHeader() != null
				&& message.getHeader().getType() == RemoteConstants.MessageType.PROVIDER_REGISTER_NOTIFY.value()) {
			Object[] params = (Object[]) message.getBody();
			byte[] sessionID = mapConsumer.get(params[0]);
			if(sessionID != null) {
				notifyObservers(sessionID, message.getHeader(), params[1]);
			}
		} else {
			ctx.fireChannelRead(msg);
		}
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        DispatchClientHandler obj = null;
        obj = (DispatchClientHandler) super.clone();
        return obj;
    }

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
	}
  
}

