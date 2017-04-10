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
package com.yea.remote.netty.server.handle;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import com.yea.core.base.facade.IFacade;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.AbstractServiceHandler;
import com.yea.remote.netty.handle.NettyChannelHandler;

/**
 * 服务端对客户端发送过来的请求进行处理（只能处理实现IFacade接口的门面类）
 * @author yiyongfei
 * 
 */
public class ServiceServerHandler extends AbstractServiceHandler implements NettyChannelHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceServerHandler.class);
    
    private ExecutorService executor = Executors.newFixedThreadPool(NettyConstants.ThreadPool.SERVICE_SERVER_HANDLER.value());
    
    @Override
	protected void execute(ChannelHandlerContext ctx, Message message) throws Exception {
		// TODO Auto-generated method stub
    	if (message.getHeader().getType() == RemoteConstants.MessageType.SERVICE_REQ.value()) {
        	LOGGER.info(ctx.channel().localAddress() + "从" + ctx.channel().remoteAddress() + "接收到请求" + ",共用时：" + (new Date().getTime() - ((Date)message.getHeader().getAttachment().get(NettyConstants.HEADER_DATE)).getTime()));
            executor.submit(new FacadeRunnable(this.getApplicationContext(), ctx, message));
        } else {
            ctx.fireChannelRead(message);
        }
	}
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        ServiceServerHandler obj = null;
        obj = (ServiceServerHandler) super.clone();
        return obj;
    }

    class FacadeRunnable implements Callable<Message> {
        private ApplicationContext springCTX;
        private ChannelHandlerContext nettyCTX;
        private Message message;
        
        public FacadeRunnable(ApplicationContext springCTX, ChannelHandlerContext nettyCTX, Message message){
            this.springCTX = springCTX;
            this.nettyCTX = nettyCTX;
            this.message = message;
        }
        /** 
         * @see java.lang.Runnable#run()
         */
        public Message call() {
            byte[] sessionID = message.getHeader().getSessionID();
            Object[] messages = (Object[]) message.getBody();
            
            Message serviceResp = null;
            String facadeName = (String) message.getHeader().getAttachment().get(NettyConstants.CALL_FACADE);
            try {
                IFacade facade = (IFacade) springCTX.getBean(facadeName);
                Object result = facade.facade(messages);
                serviceResp = buildServiceResp(RemoteConstants.MessageResult.SUCCESS.value(), sessionID, result, (String)message.getHeader().getAttachment().get(NettyConstants.CALLBACK_FACADE));
            } catch (Exception ex) {
            	LOGGER.error("处理"+facadeName+"服务[标识:" + UUIDGenerator.restore(sessionID) + "]时出现异常：", ex);
                serviceResp = buildServiceResp(RemoteConstants.MessageResult.FAILURE.value(), sessionID, ex, (String)message.getHeader().getAttachment().get(NettyConstants.CALLBACK_FACADE));
            }
            nettyCTX.writeAndFlush(serviceResp);
            return serviceResp;
        }
        
        private Message buildServiceResp(byte result, byte[] sessionID, Object body, String facade) {
            Message message = new Message();
            Header header = new Header();
            header.setType(RemoteConstants.MessageType.SERVICE_RESP.value());
            header.setSessionID(sessionID);
            header.setResult(result);
            header.setAttachment(new HashMap<String, Object>());
            header.getAttachment().put(NettyConstants.CALL_FACADE, facade == null || facade.trim().length() == 0 ? null : facade);
            header.getAttachment().put(NettyConstants.HEADER_DATE, new Date());
            message.setHeader(header);
            message.setBody(body);
            return message;
        }   
    }
}

