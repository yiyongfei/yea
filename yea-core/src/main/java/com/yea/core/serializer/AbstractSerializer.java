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
package com.yea.core.serializer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import com.yea.core.compress.ICompress;
import com.yea.core.serializer.ISerializer;


/**
 * 序列化工具
 * 
 * @author yiyongfei
 *
 */
public abstract class AbstractSerializer implements ISerializer {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ICompress compress;
	
	public void setCompress(ICompress compress) {
		this.compress = compress;
	}
	
	public byte[] serialize(Object obj) throws Exception {
		// TODO Auto-generated method stub
		ByteArrayOutputStream outStream = _Serialize(obj);
		try {
			if (compress != null) {
				return compress.compress(outStream.toByteArray());
			} else {
				return outStream.toByteArray();
			}
		} finally {
			outStream.close();
		}
	}
		
	protected abstract ByteArrayOutputStream _Serialize(Object obj) throws Exception;
	
	public Object deserialize(byte[] aryByte) throws Exception {
		ByteArrayInputStream inStream = null;
		if (compress != null) {
			inStream = new ByteArrayInputStream(compress.decompress(aryByte));
		} else {
			inStream = new ByteArrayInputStream(aryByte);
		}
	    
        try {
            return _Deserialize(inStream);
        } finally {
            inStream.close(); 
        }
	}
	
	protected abstract Object _Deserialize(ByteArrayInputStream inStream) throws Exception;
}
