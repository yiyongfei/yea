package com.yea.loadbalancer;

import java.util.ArrayList;
import java.util.List;
import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.DefaultClientConfigImpl;
import com.yea.loadbalancer.rx.LoadBalancerCommand;
import com.yea.loadbalancer.rx.NodeOperation;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;

public class LoadBalancerTest {
	private static ILoadBalancer loadBalancer;
	
	public static void main(String[] args) {
	
		
		loadBalancer = LoadBalancerBuilder.newBuilder()
				.withClientConfig(DefaultClientConfigImpl.getClientConfigWithDefaultValues()
						.set(CommonClientConfigKey.ListOfServers, "192.168.151.202:12020,192.168.151.202:14040,192.168.151.202:16060").setClientName("regist"))
				.buildDynamicServerListLoadBalancer();
		
		final List<Subscriber> list = new ArrayList<>();
		for(int i = 0; i < 10; i++) {
			String str = LoadBalancerCommand.<String>builder().withLoadBalancer(loadBalancer).withServerLocator(UUIDGenerator.generate()).build().submit(new NodeOperation<String>() {
	            @Override
	            public Observable<String> call(BalancingNode server) {
	            	Observable<String> observable = Observable.create(new Inner(server.getSocketAddress().toString()));
	                try {
	                	Observer ob = new Observer<String>() {
                            @Override
                            public void onCompleted() {
                            }
                            @Override
                            public void onError(Throwable e) {
                            }
                            @Override
                            public void onNext(String entity) {
                            }
                        };
	                	observable.subscribe(ob);
	                	
	                	return observable;
	                } catch (Exception e) {
	                    return Observable.error(e);
	                }
	            }
	        }).toBlocking().first();
			System.out.println("服务器：" + str);
		}
		
		System.out.println(loadBalancer);
		
		for(Subscriber s : list){
			s.onCompleted();
		}
		
		System.out.println(loadBalancer);
		try {
			Thread.sleep(1000 * 6);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

class Inner<String> implements Observable.OnSubscribe<String> {

	String xx;
	Inner(String xx) {
		this.xx = xx;
	}
	
	@Override
	public void call(Subscriber<? super String> t) {
		// TODO Auto-generated method stub
		t.onNext(xx);
	}


	
}
