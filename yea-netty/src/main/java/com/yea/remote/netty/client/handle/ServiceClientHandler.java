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

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import com.yea.core.base.facade.IFacade;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.AbstractServiceHandler;
import com.yea.remote.netty.handle.NettyChannelHandler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * 
 * 客户端接到服务端响应后的处理：
 * 1、如果需回调Facade，则新起线程执行回调
 * 2、通知Promise已收到响应
 * @author yiyongfei
 * 
 */
public class ServiceClientHandler extends AbstractServiceHandler implements NettyChannelHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceClientHandler.class);
    private ExecutorService executor = Executors.newFixedThreadPool(NettyConstants.ThreadPool.SERVICE_CLIENT_HANDLER.value());
    
    @Override
	protected void execute(ChannelHandlerContext ctx, Message message) throws Exception {
		// TODO Auto-generated method stub
		if (message.getHeader().getType() == RemoteConstants.MessageType.SERVICE_RESP.value()) {
            LOGGER.info(ctx.channel().localAddress() + "从" + ctx.channel().remoteAddress() + "接收到响应" + ",共用时：" + (new Date().getTime() - ((Date)message.getHeader().getAttachment().get(NettyConstants.HEADER_DATE)).getTime()));
            if(message.getHeader().getAttachment().get(NettyConstants.CALL_FACADE) != null) {
            	executor.submit(new FacadeRunnable(this.getApplicationContext(), message));
            }
            
            notifyObservers(message.getHeader().getSessionID(), message.getHeader(), message.getBody());
        } else {
            ctx.fireChannelRead(message);
        }
	}
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        ServiceClientHandler obj = null;
        obj = (ServiceClientHandler) super.clone();
        return obj;
    }

}

class FacadeRunnable implements Callable<Boolean> {
    private ApplicationContext springCTX;
    private Message message;
    
    public FacadeRunnable(ApplicationContext springCTX, Message message){
        this.springCTX = springCTX;
        this.message = message;
    }
    /** 
     * @see java.lang.Runnable#run()
     */
    public Boolean call() {
    	
    	String facadeName = (String) message.getHeader().getAttachment().get(NettyConstants.CALL_FACADE);
    	IFacade facade = (IFacade) springCTX.getBean(facadeName);
    	
    	if(RemoteConstants.MessageResult.SUCCESS.value() == message.getHeader().getResult()) {
    		Object[] messages = new Object[]{ message.getBody() };
    		facade.facade(messages);
    	} else {
    		Exception ex = (Exception) message.getBody();
    		facade.facade(ex);
    	}
        
        return true;
    }
    
}
