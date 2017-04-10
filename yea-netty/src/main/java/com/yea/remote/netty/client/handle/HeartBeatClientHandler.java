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
package com.yea.remote.netty.client.handle;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.handle.AbstractHeartBeatServerHandler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;


/**
 * 心跳检测
 * @author yiyongfei
 * 
 */
public class HeartBeatClientHandler extends AbstractHeartBeatServerHandler {
	private final Logger LOGGER = LoggerFactory.getLogger(HeartBeatClientHandler.class);
	
	protected Logger getLogger() {
		return this.LOGGER;
	}
    /**
     * 
     * @param readerIdleTimeSeconds 读空闲时间：秒
     * @param writerIdleTimeSeconds 写空闲时间：秒
     */
    public HeartBeatClientHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, 0, TimeUnit.SECONDS);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        super.channelIdle(ctx, evt);
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        super.channelRead(ctx, msg);
        Message message = (Message) msg;
        // 返回心跳应答消息
        if (message.getHeader() != null && message.getHeader().getType() == RemoteConstants.MessageType.HEARTBEAT_RESP.value()) {
        	LOGGER.info("客户端" + ctx.channel().localAddress() + "从" + ctx.channel().remoteAddress() + "接收"+message.getHeader().getAttachment().get(EVT_STATE)+"心跳检测响应！");
        }
    }
    
}
