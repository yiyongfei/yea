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
package com.yea.core.serializer.fst.serializers;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.StringTokenizer;

/**
 * 
 * @author yiyongfei
 *
 */
public class FSTInetSocketAddressSerializer  extends FSTBasicObjectSerializer {

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy,
                            int streamPosition) throws IOException {
        byte[] value = ((InetSocketAddress) toWrite).toString().getBytes();
        out.writeInt(value.length);
        out.write(value);
    }

    @SuppressWarnings("rawtypes")
	@Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee,
                              int streamPosition) throws Exception {
        int len = in.readInt();
        byte[] buf = new byte[len];
        in.read(buf);
        StringTokenizer st = new StringTokenizer(new String(buf), "/:", false);
        InetSocketAddress socketAddress = new InetSocketAddress(st.nextToken(), Integer.parseInt(st.nextToken()));
        in.registerObject(socketAddress,streamPosition,serializationInfo,referencee);
        return socketAddress;
    }
}
