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
package com.yea.remote.netty;

import java.util.List;

import com.yea.core.base.id.MongodbIDGennerator;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.struct.CallAct;
import com.yea.loadbalancer.ClientRegisterServerList;
import com.yea.loadbalancer.LoadBalancerBuilder;
import com.yea.loadbalancer.NettyPing;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.DefaultClientConfigImpl;
import com.yea.loadbalancer.rule.WeightedHashRule;
import com.yea.loadbalancer.rx.LoadBalancerCommand;
import com.yea.loadbalancer.rx.NodeOperation;
import com.yea.remote.netty.balancing.RemoteClient;
import com.yea.remote.netty.promise.NettyChannelPromise;

import io.netty.util.concurrent.GenericFutureListener;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.functions.Action1;

/**
 * 
 * @author yiyongfei
 */
public abstract class AbstractNettyEndpoint extends AbstractEndpoint {
	// 负载均衡（哈希）
	protected ILoadBalancer loadBalancer = null;

	@Override
	public void setRegisterName(String registerName) {
		super.setRegisterName(registerName);
		initLoadBalancer();
	}

	protected void initLoadBalancer() {
		loadBalancer = LoadBalancerBuilder.newBuilder().withRule(new WeightedHashRule()).withPing(new NettyPing())
				.withClientConfig(DefaultClientConfigImpl.getClientConfigWithDefaultValues()
						.setClientName(this.getRegisterName()).set(CommonClientConfigKey.NIWSServerListClassName,
								ClientRegisterServerList.class.getName()))
				.buildDynamicServerListLoadBalancer();
	}
	
	public <T> NettyChannelPromise<T> send(CallAct act, Object... messages) throws Exception {
        return send(act, null, messages);
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> NettyChannelPromise<T> send(final CallAct act, final List<GenericFutureListener> listeners, final Object... messages) throws Exception {
    	if(loadBalancer.getAllNodes().size() == 0) {
    	    throw new RemoteException(YeaErrorMessage.ERR_APPLICATION, RemoteConstants.ExceptionType.CONNECT.value() ,"未发现连接，请先连接服务器后发送！", null);
    	}
		NettyChannelPromise<T> promise = LoadBalancerCommand.<NettyChannelPromise> builder()
				.withLoadBalancer(loadBalancer).withServerLocator(MongodbIDGennerator.get().toHexString()).build()
				.submit(new ClientOperation<NettyChannelPromise>(act, listeners, messages)).toBlocking().first();
		return promise;
    }
    
    public String useStatistics() {
    	return loadBalancer.toString();
    }

	@SuppressWarnings({ "unchecked", "rawtypes", "hiding" })
	public class ClientOperation<NettyChannelPromise> implements NodeOperation<NettyChannelPromise> {
		CallAct act;
		List listeners;
		Object[] messages;

		public ClientOperation(CallAct act, List listeners, Object[] messages) {
			this.act = act;
			this.listeners = listeners;
			this.messages = messages;
		}

		@Override
		public Observable<NettyChannelPromise> call(BalancingNode server) {
			// TODO Auto-generated method stub
			RemoteClient client = (RemoteClient) server;
			rx.Observable.OnSubscribe future = null;
			Observable<NettyChannelPromise> observable = null;
			try {
				future = (OnSubscribe) client.send(act, listeners, RemoteConstants.MessageType.SERVICE_REQ,
						UUIDGenerator.generate(), messages);
				observable = Observable.create(future);
				observable.subscribe(new Action1() {
					@Override
					public void call(Object t) {
					}
				});
				return observable;
			} catch (Exception e) {
				return Observable.error(e);
			}
		}
	}
}
