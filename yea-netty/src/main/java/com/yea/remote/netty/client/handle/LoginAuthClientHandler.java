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

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.NettyChannelHandler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

/**
 * 
 * （登录验证，未完成）
 * @author yiyongfei
 * 
 */
public class LoginAuthClientHandler extends ChannelInboundHandlerAdapter implements NettyChannelHandler {
    
    /**
     * Calls {@link ChannelHandlerContext#fireChannelActive()} to forward to the
     * next {@link ChannelHandler} in the {@link ChannelPipeline}.
     * 
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush(buildLoginReq());
    }

    /**
     * Calls {@link ChannelHandlerContext#fireChannelRead(Object)} to forward to
     * the next {@link ChannelHandler} in the {@link ChannelPipeline}.
     * 
     * Sub-classes may override this method to change behavior.
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;

        // 如果是握手应答消息，需要判断是否认证成功
        if (message.getHeader() != null && message.getHeader().getType() == RemoteConstants.MessageType.LOGIN_RESP.value()) {
            Byte loginResult = (Byte) message.getHeader().getResult();
            if (loginResult.byteValue() != RemoteConstants.MessageResult.SUCCESS.value()) {
                // 握手失败，关闭连接
                ctx.channel().close();
                ctx.close();
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.channel().close();
        ctx.close();
        ctx.fireExceptionCaught(cause);
    }

    private Message buildLoginReq() {
        Message message = new Message();
        Header header = new Header();
        header.setType(RemoteConstants.MessageType.LOGIN_REQ.value());
        header.setSessionID(UUIDGenerator.generate());
        header.addAttachment(NettyConstants.LoginAuth.USERNAME.value(), "admin1");
        header.addAttachment(NettyConstants.LoginAuth.PASSWORD.value(), "password");
        message.setHeader(header);
        return message;
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        LoginAuthClientHandler obj = null;
        obj = (LoginAuthClientHandler) super.clone();
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
}
