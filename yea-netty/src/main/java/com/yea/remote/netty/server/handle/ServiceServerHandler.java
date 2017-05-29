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
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

import com.yea.core.base.act.AbstractAct;
import com.yea.core.base.act.reflect.ReflectAct;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.struct.CallReflect;
import com.yea.core.remote.struct.Header;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.constants.NettyConstants;
import com.yea.remote.netty.handle.AbstractServiceHandler;
import com.yea.remote.netty.handle.NettyChannelHandler;
import com.yea.remote.netty.send.SendHelper;
import com.yea.remote.netty.send.SendHelperRegister;

/**
 * 服务端对客户端发送过来的请求进行处理
 * 
 * @author yiyongfei
 * 
 */
public class ServiceServerHandler extends AbstractServiceHandler implements NettyChannelHandler {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceServerHandler.class);
	private int availableProcessors = 0;
	private ForkJoinPool pool = null;

	public ServiceServerHandler() {
		availableProcessors = Runtime.getRuntime().availableProcessors();
		if (((int) Math.floor(availableProcessors * 1.5)) == availableProcessors) {
			//当CPU核心是一核时，workerGroup的线程数为2
			availableProcessors = availableProcessors + 1;
		} else {
			availableProcessors = (int) Math.floor(availableProcessors * 1.5);
		}
		pool = new ForkJoinPool(availableProcessors);
	}
	@Override
	protected void execute(ChannelHandlerContext ctx, Message message) throws Exception {
		// TODO Auto-generated method stub
		if (message.getHeader().getType() == RemoteConstants.MessageType.SERVICE_REQ.value()) {
			long times = new Date().getTime() - ((Date) message.getHeader().getAttachment()
					.get(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value())).getTime();
			if (times > 1500) {
				LOGGER.warn("{}从{}接收到请求[标识:{}]" + ",共用时:{}(传输用时:{})(耗时久),消息长度:{}", ctx.channel().localAddress(),
						ctx.channel().remoteAddress(), UUIDGenerator.restore(message.getHeader().getSessionID()), times,
						new Date().getTime() - ((Date) message.getHeader().getAttachment()
								.get(NettyConstants.MessageHeaderAttachment.SEND_DATE.value())).getTime(),
						message.getHeader().getLength());
			} else {
				LOGGER.info("{}从{}接收到请求[标识:{}]" + ",共用时:{}(传输用时:{}),消息长度:{}", ctx.channel().localAddress(),
						ctx.channel().remoteAddress(), UUIDGenerator.restore(message.getHeader().getSessionID()), times,
						new Date().getTime() - ((Date) message.getHeader().getAttachment()
								.get(NettyConstants.MessageHeaderAttachment.SEND_DATE.value())).getTime(),
						message.getHeader().getLength());
			}

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
			String actName = (String) message.getHeader().getAttachment()
					.get(NettyConstants.MessageHeaderAttachment.CALL_ACT.value());
			AbstractAct<?> cloneAct;
			Object act = null;
			if (!StringUtils.isEmpty(actName)) {
				act = springContext.getBean(actName);
			}
			try {
				if (act != null && act instanceof AbstractAct) {
					cloneAct = ((AbstractAct<?>) act).clone();
				} else {
					CallReflect reflect = (CallReflect) message.getHeader().getAttachment()
							.get(NettyConstants.MessageHeaderAttachment.CALL_REFLECT.value());
					if (reflect != null) {
						cloneAct = new ReflectAct();
						((ReflectAct) cloneAct).setMethodName(reflect.getMethodName());
						if (act != null) {
							((ReflectAct) cloneAct).setTarget(act);
						} else {
							((ReflectAct) cloneAct)
									.setTarget(springContext.getBean(Class.forName(reflect.getClassName())));
						}
					} else {
						throw new RemoteException("未发现Act[" + actName + "]被注册成Act类型，同时也未指定具体的Method，不能执行！");
					}
				}
				cloneAct.setApplicationContext(springContext);
				cloneAct.setMessages((Object[]) message.getBody());
				cloneAct.fork();
				Object result = cloneAct.join();
				serviceResp = buildServiceResp(RemoteConstants.MessageResult.SUCCESS.value(), message, result);
			} catch (Exception ex) {
				LOGGER.error("处理" + actName + "服务[标识:" + UUIDGenerator.restore(message.getHeader().getSessionID())
						+ "]时出现异常：", ex);
				serviceResp = buildServiceResp(RemoteConstants.MessageResult.FAILURE.value(), message, ex);
			} finally {
				cloneAct = null;
			}

			try {
				SendHelper nettySend = SendHelperRegister.getInstance(nettyContext.channel());
				nettySend.send(serviceResp, null);
			} catch (Exception ex) {
				LOGGER.error("批量发送出现异常，采取逐一发送策略，异常原因:" + ex);
				nettyContext.writeAndFlush(serviceResp);
			}
		}
		
		private Message buildServiceResp(byte result, Message serviceReq, Object body) {
			Message message = new Message();
			Header header = new Header();
			header.setType(RemoteConstants.MessageType.SERVICE_RESP.value());
			header.setSessionID(serviceReq.getHeader().getSessionID());
			header.setResult(result);
			if (!StringUtils.isEmpty(serviceReq.getHeader().getAttachment()
					.get(NettyConstants.MessageHeaderAttachment.CALLBACK_ACT.value()))) {
				header.addAttachment(NettyConstants.MessageHeaderAttachment.CALL_ACT.value(), serviceReq
						.getHeader().getAttachment().get(NettyConstants.MessageHeaderAttachment.CALLBACK_ACT.value()));
			}
			header.addAttachment(NettyConstants.MessageHeaderAttachment.REQUEST_DATE.value(), serviceReq
					.getHeader().getAttachment().get(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value()));
			header.addAttachment(NettyConstants.MessageHeaderAttachment.REQUEST_SEND_DATE.value(), serviceReq
					.getHeader().getAttachment().get(NettyConstants.MessageHeaderAttachment.SEND_DATE.value()));
			header.addAttachment(NettyConstants.MessageHeaderAttachment.REQUEST_RECIEVE_DATE.value(), serviceReq
					.getHeader().getAttachment().get(NettyConstants.MessageHeaderAttachment.RECIEVE_DATE.value()));
			header.addAttachment(NettyConstants.MessageHeaderAttachment.HEADER_DATE.value(), new Date());
			message.setHeader(header);
			message.setBody(body);
			return message;
		}
	}
}
