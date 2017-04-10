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

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.NettyChannelHandler;

/**
 * 
 * @author yiyongfei
 * 
 */
public class LoginAuthServerHandler extends ChannelInboundHandlerAdapter implements NettyChannelHandler {
    
    private Map<String, Boolean> nodeCheck = new ConcurrentHashMap<String, Boolean>();
    private String[] ipList = { "127.0.0.1" };
    private Map<String, String> systemUser = new HashMap<String, String>();
    
    public LoginAuthServerHandler(){
        super();
        systemUser.put("admin", "password");
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

        // 如果是握手请求消息，处理，其它消息透传
        if (message.getHeader() != null && message.getHeader().getType() == RemoteConstants.MessageType.LOGIN_REQ.value()) {
            String nodeIndex = ctx.channel().remoteAddress().toString();
            Message loginResp = null;
            // 重复登陆，拒绝
            if (nodeCheck.containsKey(nodeIndex)) {
                loginResp = buildLoginResp(RemoteConstants.MessageResult.FAILURE.value());
            } else {
                InetSocketAddress address = (InetSocketAddress) ctx.channel().remoteAddress();
                String ip = address.getAddress().getHostAddress();
                boolean isOK = false;
                for (String WIP : ipList) {
                    if (WIP.equals(ip)) {
                        isOK = true;
                        break;
                    }
                }
                if (isOK) {
                    if(!systemUser.containsKey(message.getHeader().getAttachment().get(NettyConstants.LoginAuth.USERNAME.value()))){
                        isOK = false;
                    }
                    if (!systemUser.containsValue(message.getHeader().getAttachment().get(NettyConstants.LoginAuth.PASSWORD.value()))) {
                        isOK = false;
                    }
                }
                loginResp = isOK ? buildLoginResp(RemoteConstants.MessageResult.SUCCESS.value()) : buildLoginResp(RemoteConstants.MessageResult.FAILURE.value());
                if (isOK)
                    nodeCheck.put(nodeIndex, true);
            }
            ctx.writeAndFlush(loginResp);
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    private Message buildLoginResp(byte result) {
        Message message = new Message();
        Header header = new Header();
        header.setType(RemoteConstants.MessageType.LOGIN_RESP.value());
        header.setSessionID(UUIDGenerator.generate());
        header.setResult(result);
        message.setHeader(header);
        return message;
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        nodeCheck.remove(ctx.channel().remoteAddress().toString());// 删除缓存
        ctx.close();
        ctx.fireExceptionCaught(cause);
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        LoginAuthServerHandler obj = null;
        obj = (LoginAuthServerHandler) super.clone();
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
