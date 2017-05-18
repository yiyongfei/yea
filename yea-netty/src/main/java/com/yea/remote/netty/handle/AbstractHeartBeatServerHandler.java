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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
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
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;


/**
 * 心跳检测
 * @author yiyongfei
 * 
 */
public abstract class AbstractHeartBeatServerHandler extends IdleStateHandler implements NettyChannelHandler {
    private Map<String, Object> mapHeartBeat = new HashMap<String, Object>();
    
    private final String HEARTBEAT_READ_TIMEOUT = "READ_TIMEOUT";
    private final String HEARTBEAT_WRITE_TIMEOUT = "WRITE_TIMEOUT";
    private final String HEARTBEAT_READ_RETRY = "READ_RETRY";
    private final String HEARTBEAT_WRITE_RETRY = "WRITE_RETRY";
    
    protected final String IDLE_TIME = "IDLE_TIME";
    
    protected final String EVT_STATE = "evt";
    
    private enum Type { READER, WRITER }
    
    public AbstractHeartBeatServerHandler(int readerIdleTimeSeconds, int writerIdleTimeSeconds, int allIdleTimeSeconds, TimeUnit unit) {
        super(readerIdleTimeSeconds, writerIdleTimeSeconds, allIdleTimeSeconds, unit);
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception {
        super.channelIdle(ctx, evt);
        Message heatBeat = null;
        if (evt.state() == IdleState.READER_IDLE) {
            heatBeat = buildHeatBeatReq(evt.state());
            if(mapHeartBeat.get(HEARTBEAT_READ_TIMEOUT) == null) {
                mapHeartBeat.put(HEARTBEAT_READ_TIMEOUT, new Date().getTime());
                mapHeartBeat.put(HEARTBEAT_READ_RETRY, 0L);
            }
        } else if (evt.state() == IdleState.WRITER_IDLE) {
            heatBeat = buildHeatBeatReq(evt.state());
            if(mapHeartBeat.get(HEARTBEAT_WRITE_TIMEOUT) == null) {
                mapHeartBeat.put(HEARTBEAT_WRITE_TIMEOUT, new Date().getTime());
                mapHeartBeat.put(HEARTBEAT_WRITE_RETRY, 0L);
            }
        }
        if(heatBeat == null){
            return;
        }
        
        Long idleTime = (Long) mapHeartBeat.get(IDLE_TIME);
        if(idleTime != null && (new Date().getTime() - idleTime) < NettyConstants.Heartbeat.IDLETIME_LIMIT.value()){
          //空闲等待时间未超出上限时，心跳数据不发送
            return;
        }
        
        boolean isReadTimeout = checkTimeout(Type.READER);
        if(isReadTimeout){
            ctx.channel().close();
            getLogger().info("读超时，关闭当前Channel！");
            return;
        }
        boolean isWriteTimeout = checkTimeout(Type.WRITER);
        if(isWriteTimeout){
        	ctx.channel().close();
        	getLogger().info("写超时，关闭当前Channel！");
        	return;
        }
        
        ctx.writeAndFlush(heatBeat);
    }
    
    protected boolean checkTimeout(Type type) {
        Long endTime = (Long) (type.equals(Type.READER) ? mapHeartBeat.get(HEARTBEAT_READ_TIMEOUT) : mapHeartBeat.get(HEARTBEAT_WRITE_TIMEOUT));
        if(endTime != null && (new Date().getTime() - endTime) > NettyConstants.Heartbeat.TIMEOUT.value()){
            long retryTime = (Long) (type.equals(Type.READER) ? mapHeartBeat.get(HEARTBEAT_READ_RETRY) : mapHeartBeat.get(HEARTBEAT_WRITE_RETRY));;
            if(retryTime > NettyConstants.Heartbeat.RETRY.value()) {
                mapHeartBeat.clear();
                return true;
            } else {
                if(type.equals(Type.READER)){
                    mapHeartBeat.put(HEARTBEAT_READ_TIMEOUT, new Date().getTime());
                    mapHeartBeat.put(HEARTBEAT_READ_RETRY, retryTime + 1L);
                } else {
                    mapHeartBeat.put(HEARTBEAT_WRITE_TIMEOUT, new Date().getTime());
                    mapHeartBeat.put(HEARTBEAT_WRITE_RETRY, retryTime + 1L);
                }
            }
        }
        return false;
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Message message = (Message) msg;
        // 返回心跳应答消息
        if (message.getHeader() != null && message.getHeader().getType() == RemoteConstants.MessageType.HEARTBEAT_REQ.value()) {
            Message heartBeat = buildHeatBeatResp(message.getHeader().getSessionID(), (IdleState)message.getHeader().getAttachment().get(EVT_STATE));
            ctx.writeAndFlush(heartBeat);
        } else if (message.getHeader() != null && message.getHeader().getType() == RemoteConstants.MessageType.HEARTBEAT_RESP.value()) {
            if (((IdleState)message.getHeader().getAttachment().get(EVT_STATE)).equals(IdleState.READER_IDLE)) {
                mapHeartBeat.put(HEARTBEAT_READ_TIMEOUT, new Date().getTime());
                mapHeartBeat.put(HEARTBEAT_READ_RETRY, 0L);
            } else if (((IdleState)message.getHeader().getAttachment().get(EVT_STATE)).equals(IdleState.WRITER_IDLE)){
                mapHeartBeat.put(HEARTBEAT_WRITE_TIMEOUT, new Date().getTime());
                mapHeartBeat.put(HEARTBEAT_WRITE_RETRY, 0L);
            }
            
        } else {
            //记录接收数据请求的最新时间
            mapHeartBeat.put(IDLE_TIME, new Date().getTime());
            ctx.fireChannelRead(msg);
        }
        
    }
    
    protected abstract Logger getLogger();
    
    private Message buildHeatBeatReq(IdleState state) {
        Message message = new Message();
        Header header = new Header();
        header.setType(RemoteConstants.MessageType.HEARTBEAT_REQ.value());
        header.setSessionID(UUIDGenerator.generate());
        header.setAttachment(new HashMap<String, Object>());
        header.getAttachment().put(EVT_STATE, state);
        message.setHeader(header);
        return message;
    }
    
    private Message buildHeatBeatResp(byte[] sessionID, IdleState state) {
        Message message = new Message();
        Header header = new Header();
        header.setType(RemoteConstants.MessageType.HEARTBEAT_RESP.value());
        header.setSessionID(sessionID);
        header.setResult(RemoteConstants.MessageResult.SUCCESS.value());
        header.setAttachment(new HashMap<String, Object>());
        header.getAttachment().put(EVT_STATE, state);
        message.setHeader(header);
        return message;
    }

    public ChannelHandler clone() throws CloneNotSupportedException {
        ChannelHandler obj = null;
        obj = (ChannelHandler) super.clone();
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
