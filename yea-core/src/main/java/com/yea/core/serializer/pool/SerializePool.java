package com.yea.core.serializer.pool;

import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

import com.yea.core.exception.YeaException;
import com.yea.core.serializer.ISerializer;

public class SerializePool {
	private GenericObjectPool<ISerializer> serializerPool;
	
	public SerializePool() {
		this(new SerializeFactory());
	}

	public SerializePool(SerializeFactory serializeFactory) {
		this(serializeFactory, 10, 3, -1, 1800000);
	}
	
	public SerializePool(SerializeFactory serializeFactory, final int maxTotal, final int minIdle, final long maxWaitMillis, final long minEvictableIdleTimeMillis) {
		serializerPool = new GenericObjectPool<ISerializer>(serializeFactory);
		GenericObjectPoolConfig config = new GenericObjectPoolConfig();
		// 最大池对象总数
		config.setMaxTotal(maxTotal);
		// 最小空闲数
		config.setMinIdle(minIdle);
		// 最大等待时间， 默认的值为-1，表示无限等待
		config.setMaxWaitMillis(maxWaitMillis);
		// 退出连接的最小空闲时间 默认1800000毫秒
		config.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
		serializerPool.setConfig(config);
	}

	public ISerializer borrow() {
		try {
			return getPool().borrowObject();
		} catch (final Exception ex) {
			throw new YeaException("不能获取池对象");
		}
	}

	public void restore(final ISerializer object) {
		getPool().returnObject(object);
	}

	private GenericObjectPool<ISerializer> getPool() {
		return serializerPool;
	}
}
