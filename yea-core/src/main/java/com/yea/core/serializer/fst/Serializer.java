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
package com.yea.core.serializer.fst;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;

import org.nustaq.serialization.FSTConfiguration;

import com.yea.core.serializer.AbstractSerializer;
import com.yea.core.serializer.fst.serializers.FSTInetSocketAddressSerializer;


/**
 * 序列化工具
 * 
 * @author yiyongfei
 *
 */
public class Serializer extends AbstractSerializer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private FSTConfiguration conf;
	public Serializer() {
		super();
		conf = FSTConfiguration.createDefaultConfiguration();
		conf.registerSerializer(InetSocketAddress.class, new FSTInetSocketAddressSerializer(), false);
	}
		
	protected ByteArrayOutputStream _Serialize(Object obj) throws Exception {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		conf.encodeToStream(outStream, obj);
		return outStream;
	}
	
	protected Object _Deserialize(ByteArrayInputStream inStream) throws Exception {
		return conf.decodeFromStream(inStream);
	}
	
}
