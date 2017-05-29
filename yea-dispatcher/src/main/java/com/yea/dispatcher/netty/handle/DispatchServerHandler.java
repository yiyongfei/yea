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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.server.handle.ServiceServerHandler;

/**
 * 用于服务端的服务机制
 * @author yiyongfei
 */
public class DispatchServerHandler extends ChannelInboundHandlerAdapter implements NettyChannelHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceServerHandler.class);
    
	private Map<String, Map<String, List<SocketAddress>>> mapDispatch = new ConcurrentHashMap<String, Map<String, List<SocketAddress>>>();
    private Map<SocketAddress, Channel> mapChannel = new ConcurrentHashMap<SocketAddress, Channel>();
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
    	Message message = (Message) msg;
    	Object[] params = (Object[]) message.getBody();
		if (message.getHeader() != null
				&& (message.getHeader().getType() == RemoteConstants.MessageType.CONSUMER_REGISTER.value()
						|| message.getHeader().getType() == RemoteConstants.MessageType.PROVIDER_REGISTER.value())) {
			if (!mapDispatch.containsKey(params[0])) {
				// 当未发现待注册的系统服务时，需要设置服务
				Map<String, List<SocketAddress>> map = new ConcurrentHashMap<String, List<SocketAddress>>();
				map.put(RemoteConstants.DispatchType.CONSUMER.value(), new CopyOnWriteArrayList<SocketAddress>());
				map.put(RemoteConstants.DispatchType.PROVIDER.value(), new CopyOnWriteArrayList<SocketAddress>());
				mapDispatch.put((String) params[0], map);
			}
			if (message.getHeader().getType() == RemoteConstants.MessageType.CONSUMER_REGISTER.value()) {
				LOGGER.info("远程节点["+params[1]+"]注册消费者成功，注册服务["+params[0]+"]");
				mapChannel.put((SocketAddress) params[1], ctx.channel());
				mapDispatch.get(params[0]).get(RemoteConstants.DispatchType.CONSUMER.value()).add((SocketAddress) params[1]);
				//反馈远程节点注册服务成功
				Message resp = buildResp(RemoteConstants.MessageType.CONSUMER_REGISTER_RESULT, message.getHeader().getSessionID(), params[1]);
				ctx.writeAndFlush(resp);
			} else {
				LOGGER.info("远程节点["+params[1]+"]注册服务提供者成功，注册服务["+params[0]+"]");
				mapDispatch.get(params[0]).get(RemoteConstants.DispatchType.PROVIDER.value()).add((SocketAddress) params[1]);
				//通知所有相关服务的消费者，有新提供者增加，让消费者连接新服务提供者
				List<SocketAddress> listConsumer = mapDispatch.get(params[0]).get(RemoteConstants.DispatchType.CONSUMER.value());
				for (SocketAddress address : listConsumer) {
					Channel channel = mapChannel.get(address);
					Object[] obj = new Object[]{address, params[1]};
					Message resp = buildResp(RemoteConstants.MessageType.PROVIDER_REGISTER_NOTIFY, UUIDGenerator.generate(), obj);
					channel.writeAndFlush(resp);
				}
			}
		} else if (message.getHeader() != null
				&& (message.getHeader().getType() == RemoteConstants.MessageType.CONSUMER_LOGOUT.value()
						|| message.getHeader().getType() == RemoteConstants.MessageType.PROVIDER_LOGOUT.value())) {
			if (mapDispatch.containsKey(params[0])) {
				if (message.getHeader().getType() == RemoteConstants.MessageType.CONSUMER_LOGOUT.value()) {
					LOGGER.info("远程节点["+params[1]+"]注销消费者成功，注销服务["+params[0]+"]");
					mapChannel.remove((SocketAddress) params[1]);
					mapDispatch.get(params[0]).get(RemoteConstants.DispatchType.CONSUMER.value()).remove((SocketAddress) params[1]);
					//反馈远程节点注册服务成功
					Message resp = buildResp(RemoteConstants.MessageType.CONSUMER_LOGOUT_RESULT, message.getHeader().getSessionID(), params[1]);
					ctx.writeAndFlush(resp);
				} else {
					LOGGER.info("远程节点["+params[1]+"]注销服务提供者成功，注销服务["+params[0]+"]");
					mapDispatch.get(params[0]).get(RemoteConstants.DispatchType.PROVIDER.value()).remove((SocketAddress) params[1]);
				}
			}
		} else if (message.getHeader() != null
				&& message.getHeader().getType() == RemoteConstants.MessageType.PROVIDER_DISCOVER.value()) {
			if (mapDispatch.containsKey(params[0])) {
				List<SocketAddress> listProvider = mapDispatch.get(params[0]).get(RemoteConstants.DispatchType.PROVIDER.value());
				LOGGER.info("远程节点发现服务["+params[0]+"]的提供者共有"+listProvider.size()+"个，对应服务地址为"+listProvider+"");
				Message resp = buildResp(RemoteConstants.MessageType.PROVIDER_DISCOVER_RESULT, message.getHeader().getSessionID(), listProvider);
				ctx.writeAndFlush(resp);
			} else {
				LOGGER.info("远程节点发现服务["+params[0]+"]的提供者共有0个");
				Message resp = buildResp(RemoteConstants.MessageType.PROVIDER_DISCOVER_RESULT, message.getHeader().getSessionID(), new ArrayList<SocketAddress>());
				ctx.writeAndFlush(resp);
			}
		} else {
			ctx.fireChannelRead(msg);
		}
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        DispatchServerHandler obj = null;
        obj = (DispatchServerHandler) super.clone();
        return obj;
    }
    
    private Message buildResp(RemoteConstants.MessageType messageType, byte[] sessionID, Object body) {
        Message message = new Message();
        Header header = new Header();
        header.setType(messageType.value());
        header.setSessionID(sessionID);
        header.setResult(RemoteConstants.MessageResult.SUCCESS.value());
        header.addAttachment(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value(), new Date());
        message.setHeader(header);
        message.setBody(body);
        return message;
    }

	@Override
	public void setApplicationContext(ApplicationContext arg0) throws BeansException {
	}
}

