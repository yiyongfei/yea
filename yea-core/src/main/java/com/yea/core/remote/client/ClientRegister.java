package com.yea.core.remote.client;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import com.yea.core.remote.AbstractEndpoint;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.promise.Promise;
import com.yea.core.remote.struct.CallAct;

@SuppressWarnings("rawtypes")
public class ClientRegister<T> {
	private Map<String, AbstractEndpoint> mapEndpoint = null;
	private Map<String, Set<String>> mapActName = null;

	private ClientRegister() {
		mapEndpoint = new ConcurrentHashMap<String, AbstractEndpoint>();
		mapActName = new ConcurrentHashMap<String, Set<String>>();
	}

	public Promise<T> send(CallAct act, Object... messages) throws Throwable {
		if (mapActName.containsKey(act.getActName())) {
			Set<String> actset = mapActName.get(act.getActName());
			if (actset.size() == 0) {
				throw new RemoteException("未发现Act[" + act.getActName() + "]对应的注册服务，请检查！");
			} else if (actset.size() > 1) {
				throw new RemoteException("发现Act[" + act.getActName() + "]对应的注册服务超出一个，请指定注册服务发送！");
			} else {
				return send((String) actset.toArray()[0], act, messages);
			}
		} else {
			throw new RemoteException("未发现Act[" + act.getActName() + "]被注册，请检查！");
		}
	}

	public Promise<T> send(String registerName, CallAct act, Object... messages) throws Throwable {
		if (mapEndpoint.containsKey(registerName)) {
			return mapEndpoint.get(registerName).send(act, messages);
		} else {
			throw new RemoteException("未发现注册服务[" + registerName + "]的存在，请检查！");
		}
	}

	/**
	 * 注意：如果endpoint已经定义，但未被注册到ClientRegister内，可能原因是延迟加载引发的，请修改成立即加载
	 * 
	 * @param registerName
	 * @param endpoint
	 */
	public void registerEndpoint(String registerName, AbstractEndpoint endpoint) {
		if (endpoint != null) {
			mapEndpoint.put(registerName, endpoint);
		}
	}

	public void registerAct(String registerName, String[] actnames) {
		if (actnames != null && actnames.length > 0) {
			for (String actname : actnames) {
				if (!mapActName.containsKey(actname)) {
					mapActName.put(actname, new ConcurrentSkipListSet<String>());
				}
				mapActName.get(actname).add(registerName);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> ClientRegister<T> getInstance() {
		return Holder.SINGLETON;
	}

	private static class Holder {
		private static final ClientRegister SINGLETON = new ClientRegister();
	}
}
