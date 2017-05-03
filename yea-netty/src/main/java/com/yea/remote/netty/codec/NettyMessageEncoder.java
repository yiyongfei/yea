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
package com.yea.remote.netty.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.yea.core.remote.struct.Message;
import com.yea.core.serializer.ISerializer;
import com.yea.core.serializer.pool.SerializePool;
import com.yea.remote.netty.handle.NettyChannelHandler;

/**
 * 消息处理，编码（FST）
 * @author yiyongfei
 * 
 */
public final class NettyMessageEncoder extends MessageToByteEncoder<Message> implements NettyChannelHandler {
	private static final byte[] LENGTH_PLACEHOLDER = new byte[4];
	private SerializePool serializePool;
    
    public NettyMessageEncoder() {
    	serializePool = new SerializePool();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf sendBuf) throws Exception {
    	if (msg == null || msg.getHeader() == null) {
            throw new Exception("The encode message is null");
        }
    	ISerializer serializer = serializePool.borrow();
    	try{
    		sendBuf.writeInt((msg.getHeader().getCrcCode()));//长度4字节
            sendBuf.writeInt((msg.getHeader().getLength()));//长度4字节，存放Header+Body的长度，其中Header固定为18字节
            sendBuf.writeBytes((msg.getHeader().getSessionID()));//长度16字节
            sendBuf.writeByte((msg.getHeader().getType()));//长度1字节
            sendBuf.writeByte((msg.getHeader().getPriority()));//长度1字节
            sendBuf.writeByte((msg.getHeader().getResult()));//长度1字节
            
            if (msg.getHeader().getAttachment() != null) {
                sendBuf.writeInt((msg.getHeader().getAttachment().size()));//长度4字节，附属内容的数目
                byte[] keyArray = null;
                byte[] valueArray = null;
                for (Map.Entry<String, Object> param : msg.getHeader().getAttachment().entrySet()) {
                    keyArray = param.getKey().getBytes("ISO-8859-1");
                    sendBuf.writeInt(keyArray.length);
                    sendBuf.writeBytes(keyArray);

                    valueArray = serializer.serialize(param.getValue());
                    sendBuf.writeInt(valueArray.length);
                    sendBuf.writeBytes(valueArray);
                }
                keyArray = null;
                valueArray = null;
            } else {
                sendBuf.writeInt(0);//长度4字节，附属内容的数目为0
            }
            
            if (msg.getBody() != null) {
                byte[] objArray = serializer.serialize(msg.getBody());
                int lengthPos = sendBuf.writerIndex();
                sendBuf.writeBytes(LENGTH_PLACEHOLDER);//长度4字节，存放Body的长度
                sendBuf.writeBytes(objArray);
                sendBuf.setInt(lengthPos, sendBuf.writerIndex() - lengthPos - 4);//设置Body长度
            } else {
                sendBuf.writeInt(0);
            }
            sendBuf.setInt(4, sendBuf.readableBytes() - 8);//设置总长度，Header+Body的长度
    	} finally {
    		serializePool.restore(serializer);
    	}
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        NettyMessageEncoder obj = null;
        obj = (NettyMessageEncoder) super.clone();
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
