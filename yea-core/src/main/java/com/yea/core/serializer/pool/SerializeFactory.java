package com.yea.core.serializer.pool;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import com.yea.core.serializer.ISerializer;

public class SerializeFactory extends BasePooledObjectFactory<ISerializer> {
	public enum SERIALIZE {
		SERIALIZE_FST(1), SERIALIZE_HESSION(2), SERIALIZE_JAVA(3);
        
        private int value;
        private SERIALIZE(int value) {
            this.value = value;
        }
        public int value() {
            return this.value;
        }
    }
	
	private SerializeFactory.SERIALIZE serialize;
	
	public SerializeFactory() {
		this(SERIALIZE.SERIALIZE_FST);
	}
	
	public SerializeFactory(SerializeFactory.SERIALIZE serialize) {
		this.serialize = serialize;
	}

	public ISerializer create() throws Exception {
		return createSerialize();
	}

	public PooledObject<ISerializer> wrap(ISerializer hessian) {
		return new DefaultPooledObject<ISerializer>(hessian);
	}

	private ISerializer createSerialize() {
		if(serialize.value() == SERIALIZE.SERIALIZE_FST.value()) {
			return new com.yea.core.serializer.fst.Serializer();
		} else if (serialize.value() == SERIALIZE.SERIALIZE_HESSION.value()) {
			return new com.yea.core.serializer.hessian.Serializer();
		} else {
			return new com.yea.core.serializer.java.Serializer();
		}
	}
}
