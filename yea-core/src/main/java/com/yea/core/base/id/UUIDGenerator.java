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
package com.yea.core.base.id;

import java.math.BigInteger;
import java.util.UUID;

import com.yea.core.util.NumberUtils;

/**
 * 
 * @author yiyongfei
 * 
 */
public class UUIDGenerator {
    public static byte[] generate(){
        UUID uuid = UUID.randomUUID();
        byte[] uuidBytes = NumberUtils.long2bytes(uuid.getMostSignificantBits(), uuid.getLeastSignificantBits());

        //高位符合位如果为负数，需要转成正数
        if (uuidBytes[0] < 0) {
            uuidBytes[0] = (byte) (uuidBytes[0] & 0xff >> 1);
        }

        try{
            return uuidBytes;
        } finally {
            uuid = null;
            uuidBytes = null;
        }
    }
    
    public static String generateString() {
        byte[] uuidBytes = generate();
        try{
            return new String(uuidBytes, "ISO-8859-1");
        } catch (Exception ex) {
            return null;
        }finally {
            uuidBytes = null;
        }
    }
    
    public static BigInteger generateNumber() {
        byte[] uuidBytes = generate();
        try {
            return new BigInteger(uuidBytes);
        } finally {
            uuidBytes = null;
        }
    }
    
    public static UUID restore(byte[] uuidBytes){
        byte[] mostSignificantBits = new byte[8];
        byte[] leastSignificantBits = new byte[8];
        for(int i = 0; i < uuidBytes.length; i++){
            if(i < 8){
                mostSignificantBits[i] = uuidBytes[i];
            } else {
                leastSignificantBits[i - 8] = uuidBytes[i];
            }
        }
        long[] tmp = NumberUtils.bytes2long(uuidBytes);
        UUID uuid = new UUID(tmp[0], tmp[1]);
        try{
            return uuid;
        } finally {
            uuid = null;
            mostSignificantBits = null;
            leastSignificantBits = null;
        }
    }
}
