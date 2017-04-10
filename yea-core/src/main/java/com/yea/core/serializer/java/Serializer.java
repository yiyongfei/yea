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
package com.yea.core.serializer.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.yea.core.serializer.AbstractSerializer;


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
		
	protected ByteArrayOutputStream _Serialize(Object obj) throws Exception {
		ObjectOutputStream objStream = null;
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try{
		    objStream = new ObjectOutputStream(outStream);
		    objStream.writeObject(obj);
		    objStream.flush();
	        return outStream;
		} finally {
		    objStream.close();
		}
	}
	
	protected Object _Deserialize(ByteArrayInputStream inStream) throws Exception {
	    ObjectInputStream objStream = null;
	    try {
	        objStream = new ObjectInputStream(inStream);
	        return objStream.readObject();
        } finally {
        	if(objStream != null){
        	    objStream.close(); 
        	}
        }
	}
}
