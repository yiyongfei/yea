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
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.core.serializer.ISerializer;
import com.yea.core.serializer.pool.SerializePool;
import com.yea.remote.netty.handle.NettyChannelHandler;

/**
 * 消息处理，解码（FST）
 * @author yiyongfei
 * 
 */
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder implements NettyChannelHandler {
	private SerializePool serializePool;
	
    public NettyMessageDecoder() {
        super(1024*1024, 4, 4, 0, 0);
        serializePool = new SerializePool();
    }

    @Override
    public Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        if (frame == null) {
            return null;
        }
        ISerializer serializer = serializePool.borrow();
        try {
            Message message = new Message();
            byte[] sessionID = new byte[16];
            Header header = new Header();
            header.setCrcCode(frame.readInt());
            header.setLength(frame.readInt());
            frame.readBytes(sessionID);
            header.setSessionID(sessionID);
            header.setType(frame.readByte());
            header.setPriority(frame.readByte());
            header.setResult(frame.readByte());
            
            int attachmentSize = frame.readInt();
            if (attachmentSize > 0) {
                Map<String, Object> attachment = new HashMap<String, Object>();
                byte[] keyArray = null;
                byte[] valueArray = null;
                for (int i = 0; i < attachmentSize; i++) {
                    keyArray = new byte[frame.readInt()];
                    frame.readBytes(keyArray);
                    valueArray = new byte[frame.readInt()];
                    frame.readBytes(valueArray);

                    attachment.put(new String(keyArray, "ISO-8859-1"), serializer.deserialize(valueArray));
                }
                keyArray = null;
                valueArray = null;
                header.setAttachment(attachment);
            }
            message.setHeader(header);
            
            if (frame.readableBytes() > 4) {
                int length = frame.readInt();//Body的长度
                byte[] objArray = new byte[length];
                frame.readBytes(objArray);
                try{
                	Object body = serializer.deserialize(objArray);
                    message.setBody(body);
                } catch (Exception ex) {
					if (RemoteConstants.MessageType.SERVICE_RESP.value() == message.getHeader().getType()) {
						message.getHeader().setType(RemoteConstants.MessageType.SUSPEND_RESP.value());
	                	message.getHeader().setResult(RemoteConstants.MessageResult.FAILURE.value());
	                	message.setBody(ex);
					} else if (RemoteConstants.MessageType.SERVICE_REQ.value() == message.getHeader().getType()) {
						message.getHeader().setType(RemoteConstants.MessageType.SUSPEND_REQ.value());
	                	message.getHeader().setResult(RemoteConstants.MessageResult.FAILURE.value());
	                	message.setBody(ex);
					} else {
						throw ex;
					}
                }
            }

            return message;
        } finally {
        	serializePool.restore(serializer);
            frame.release();
        }
    }
    
    public ChannelHandler clone() throws CloneNotSupportedException {
        NettyMessageDecoder obj = null;
        obj = (NettyMessageDecoder) super.clone();
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