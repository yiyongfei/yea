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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import com.yea.core.compress.Compress;
import com.yea.core.remote.constants.RemoteConstants;
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
	private RemoteConstants.CompressionAlgorithm compressionAlgorithm;
	
    public NettyMessageEncoder() {
    	this(null);
    }
    
    public NettyMessageEncoder(RemoteConstants.CompressionAlgorithm compressionAlgorithm) {
    	this.serializePool = new SerializePool();
    	this.compressionAlgorithm = compressionAlgorithm;
    }

    @Override
	protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf sendBuf) throws Exception {
		if (msg == null || msg.getHeader() == null) {
			throw new Exception("The encode message is null");
		}
		ISerializer serializer = serializePool.borrow();
		try {
			long basedate = new Date().getTime();
			sendBuf.writeInt((msg.getHeader().getCrcCode()));// 长度4字节
			sendBuf.writeInt((msg.getHeader().getLength()));// 长度4字节，存放Header+Body的长度，其中Header固定为18字节
			sendBuf.writeBytes((msg.getHeader().getSessionID()));// 长度16字节
			sendBuf.writeByte((msg.getHeader().getType()));// 长度1字节
			sendBuf.writeByte((msg.getHeader().getPriority()));// 长度1字节
			sendBuf.writeByte((msg.getHeader().getResult()));// 长度1字节
			if (compressionAlgorithm != null && compressionAlgorithm.code() > 0) {
				// 设置压缩方法
				serializer.setCompress(new Compress().setCompressionAlgorithm(compressionAlgorithm.algorithm()));
				sendBuf.writeByte(compressionAlgorithm.ordinal());
			} else {
				sendBuf.writeByte(RemoteConstants.CompressionAlgorithm.NONE.ordinal());
			}
			sendBuf.writeLong(basedate);// 长度为8字节，发送时间
			if (msg.getHeader().getAttachment() != null && !msg.getHeader().getAttachment().isEmpty()) {
				sendBuf.writeByte(msg.getHeader().getAttachment().size());
				Map<String, Number> mapDateType = new HashMap<String, Number>();// 存放Date类型
				Map<String, String> mapStringType = new HashMap<String, String>();// 存放String类型

				int lengthPos = sendBuf.writerIndex();
				sendBuf.writeBytes(new byte[1]);// 长度1字节，存放非Date类型的参数个数
				byte[] keyArray = null;
				byte[] valueArray = null;
				for (Map.Entry<String, Object> param : msg.getHeader().getAttachment().entrySet()) {
					if (param.getValue() instanceof Date) {
						//Date类型的处理，按数字来存放，减少byte数
						long time = basedate - ((Date) param.getValue()).getTime();
						if (time > Integer.MAX_VALUE) {
							mapDateType.put(param.getKey(), new Long(time));
						} else if (time > Short.MAX_VALUE) {
							mapDateType.put(param.getKey(), new Integer((int) time));
						} else if (time > Byte.MAX_VALUE) {
							mapDateType.put(param.getKey(), new Short((short) time));
						} else {
							mapDateType.put(param.getKey(), new Byte((byte) time));
						}
					} else if (param.getValue() instanceof String) {
						//字符串类型的处理，一般情况，字符串的序列化会比getBytes()会占用更多空间
						mapStringType.put(param.getKey(), (String) param.getValue());
					} else {
						keyArray = param.getKey().getBytes("ISO-8859-1");
						sendBuf.writeShort(keyArray.length);
						sendBuf.writeBytes(keyArray);

						valueArray = serializer.serialize(param.getValue());
						sendBuf.writeShort(valueArray.length);
						sendBuf.writeBytes(valueArray);
					}
				}
				sendBuf.setByte(lengthPos,
						msg.getHeader().getAttachment().size() - mapDateType.size() - mapStringType.size());

				if (mapDateType.isEmpty()) {
					sendBuf.writeByte(0);// 长度1字节，存放Date类型的参数个数，个数为0
				} else {
					sendBuf.writeByte(mapDateType.size());// 长度1字节，存放Date类型的参数个数，个数为元素个数
					for (Map.Entry<String, Number> param : mapDateType.entrySet()) {
						keyArray = param.getKey().getBytes("ISO-8859-1");
						sendBuf.writeShort(keyArray.length);
						sendBuf.writeBytes(keyArray);

						if (param.getValue() instanceof Long) {
							sendBuf.writeByte(8);
							sendBuf.writeLong((Long) param.getValue());
						} else if (param.getValue() instanceof Integer) {
							sendBuf.writeByte(4);
							sendBuf.writeInt((Integer) param.getValue());
						} else if (param.getValue() instanceof Short) {
							sendBuf.writeByte(2);
							sendBuf.writeShort((Short) param.getValue());
						} else {
							sendBuf.writeByte(1);
							sendBuf.writeByte((Byte) param.getValue());
						}
					}
				}

				if (mapStringType.isEmpty()) {
					sendBuf.writeByte(0);// 长度1字节，存放String类型的参数个数，个数为0
				} else {
					sendBuf.writeByte(mapStringType.size());// 长度1字节，存放String类型的参数个数，个数为元素个数
					for (Map.Entry<String, String> param : mapStringType.entrySet()) {
						keyArray = param.getKey().getBytes("ISO-8859-1");
						sendBuf.writeShort(keyArray.length);
						sendBuf.writeBytes(keyArray);

						valueArray = param.getValue().getBytes("ISO-8859-1");
						sendBuf.writeShort(valueArray.length);
						sendBuf.writeBytes(valueArray);
					}
				}
			} else {
				sendBuf.writeByte(0);// 长度2字节，附属内容的数目为0
			}

			if (msg.getBody() != null) {
				byte[] objArray = serializer.serialize(msg.getBody());
				int lengthPos = sendBuf.writerIndex();
				sendBuf.writeBytes(LENGTH_PLACEHOLDER);// 长度4字节，存放Body的长度
				sendBuf.writeBytes(objArray);
				sendBuf.setInt(lengthPos, sendBuf.writerIndex() - lengthPos - 4);// 设置Body长度
			} else {
				sendBuf.writeInt(0);
			}
			sendBuf.setInt(4, sendBuf.readableBytes() - 8);// 设置总长度，Header+Body的长度
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
