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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import com.yea.core.base.act.AbstractAct;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.AbstractServiceHandler;
import com.yea.remote.netty.handle.NettyChannelHandler;

/**
 * 服务端对客户端发送过来的请求进行处理
 * @author yiyongfei
 * 
 */
public class ServiceServerHandler extends AbstractServiceHandler implements NettyChannelHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceServerHandler.class);
    private ForkJoinPool pool = new ForkJoinPool();
    
    @Override
	protected void execute(ChannelHandlerContext ctx, Message message) throws Exception {
		// TODO Auto-generated method stub
    	if (message.getHeader().getType() == RemoteConstants.MessageType.SERVICE_REQ.value()) {
        	LOGGER.info(ctx.channel().localAddress() + "从" + ctx.channel().remoteAddress() + "接收到请求" + ",共用时：" + (new Date().getTime() - ((Date)message.getHeader().getAttachment().get(NettyConstants.HEADER_DATE)).getTime()));
        	pool.execute(new InnerTask(this.getApplicationContext(), ctx, message));
        } else {
            ctx.fireChannelRead(message);
        }
	}
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        ServiceServerHandler obj = null;
        obj = (ServiceServerHandler) super.clone();
        return obj;
    }
    
    class InnerTask extends RecursiveAction {
    	private static final long serialVersionUID = 1L;
    	private ApplicationContext springContext;
    	private ChannelHandlerContext nettyContext;
    	private Message message;

    	public InnerTask(ApplicationContext springContext, ChannelHandlerContext nettyContext, Message message) {
    		this.springContext = springContext;
    		this.nettyContext = nettyContext;
    		this.message = message;
    	}

		@Override
		protected void compute() {
			// TODO Auto-generated method stub
			Message serviceResp = null;
            String actName = (String) message.getHeader().getAttachment().get(NettyConstants.CALL_ACT);
            try {
            	AbstractAct<?> act = (AbstractAct<?>) springContext.getBean(actName);
            	AbstractAct<?> cloneAct = act.clone();
            	cloneAct.setApplicationContext(springContext);
            	cloneAct.setMessages((Object[]) message.getBody());
            	cloneAct.fork();
            	Object result = cloneAct.join();
                serviceResp = buildServiceResp(RemoteConstants.MessageResult.SUCCESS.value(), message.getHeader().getSessionID(), result, (String)message.getHeader().getAttachment().get(NettyConstants.CALLBACK_ACT));
            } catch (Exception ex) {
            	LOGGER.error("处理"+actName+"服务[标识:" + UUIDGenerator.restore(message.getHeader().getSessionID()) + "]时出现异常：", ex);
                serviceResp = buildServiceResp(RemoteConstants.MessageResult.FAILURE.value(), message.getHeader().getSessionID(), ex, (String)message.getHeader().getAttachment().get(NettyConstants.CALLBACK_ACT));
            }
            nettyContext.writeAndFlush(serviceResp);
		}
		
	    private Message buildServiceResp(byte result, byte[] sessionID, Object body, String act) {
	        Message message = new Message();
	        Header header = new Header();
	        header.setType(RemoteConstants.MessageType.SERVICE_RESP.value());
	        header.setSessionID(sessionID);
	        header.setResult(result);
	        header.setAttachment(new HashMap<String, Object>());
	        header.getAttachment().put(NettyConstants.CALL_ACT, act == null || act.trim().length() == 0 ? null : act);
	        header.getAttachment().put(NettyConstants.HEADER_DATE, new Date());
	        message.setHeader(header);
	        message.setBody(body);
	        return message;
	    }
    }
}

