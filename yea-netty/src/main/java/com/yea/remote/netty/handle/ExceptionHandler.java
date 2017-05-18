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

import java.util.Date;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.handle.observer.ChannelInboundHandlerObservable;

/**
 * 错误处理，兜底Handle
 * @author yiyongfei
 * 
 */
public class ExceptionHandler extends ChannelInboundHandlerObservable implements NettyChannelHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionHandler.class);
    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward to
     * the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     * 
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;
        if(message.getHeader() == null) {
        	ctx.fireChannelRead(msg);
        	return;
        }
        if (message.getHeader().getType() == RemoteConstants.MessageType.SUSPEND_REQ.value()) {
        	LOGGER.info(ctx.channel().localAddress() + "从" + ctx.channel().remoteAddress() + "接收请求后处理时出现异常！异常内容：" + message.getBody());
        	ctx.writeAndFlush(buildResp(message.getHeader().getResult(), message.getHeader().getSessionID(), (Date)message.getHeader().getAttachment().get(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value()), new Date(), message.getBody(), (String)message.getHeader().getAttachment().get(NettyConstants.MessageHeaderAttachment.CALLBACK_ACT.value())));
        } else if (message.getHeader().getType() == RemoteConstants.MessageType.SUSPEND_RESP.value()) {
            LOGGER.info(ctx.channel().localAddress() + "从" + ctx.channel().remoteAddress() + "接收响应后处理时出现异常！异常内容：" + message.getBody());
            notifyObservers(message.getHeader().getSessionID(), message.getHeader(), message.getBody());
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        ExceptionHandler obj = null;
        obj = (ExceptionHandler) super.clone();
        return obj;
    }
    
    @SuppressWarnings("unused")
	private ApplicationContext context;
    /** 
     * @see org.springframework.context.ApplicationContextAware#setApplicationContext(org.springframework.context.ApplicationContext)
     */
    public void setApplicationContext(ApplicationContext arg0) throws BeansException {
        context = arg0;
    }
    
    private Message buildResp(byte result, byte[] sessionID, Date reqDate, Date reqRecieveDate, Object body, String act) {
        Message message = new Message();
        Header header = new Header();
        header.setType(RemoteConstants.MessageType.SERVICE_RESP.value());
        header.setSessionID(sessionID);
        header.setResult(result);
        header.setAttachment(new HashMap<String, Object>());
        header.getAttachment().put(NettyConstants.MessageHeaderAttachment.CALL_ACT.value(), act == null || act.trim().length() == 0 ? null : act);
        header.getAttachment().put(NettyConstants.MessageHeaderAttachment.REQUEST_DATE.value(), reqDate);
        header.getAttachment().put(NettyConstants.MessageHeaderAttachment.REQUEST_RECIEVE_DATE.value(), reqRecieveDate);
        header.getAttachment().put(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value(), new Date());
        message.setHeader(header);
        message.setBody(body);
        return message;
    }
    
}