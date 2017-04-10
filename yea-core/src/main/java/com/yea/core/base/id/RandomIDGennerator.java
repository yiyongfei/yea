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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicInteger;

import com.yea.core.balancing.hash.DefaultHashAlgorithm;
import com.yea.core.util.NumberUtils;

/**
 * 参照MongodbID生成器生成64位Long型数字。
 * 规则：前32位：存放时间，后32位：根据机器、进程、随机数所生成64位数字通过MURMUR算法哈希成32位数字
 * @author yiyongfei
 * 
 */
public final class RandomIDGennerator implements Comparable<RandomIDGennerator>, Serializable {

    private static final long serialVersionUID = 3670079982654483072L;

    private static final int LOW_ORDER_THREE_BYTES = 0x00ffffff;

    private static final int MACHINE_PROCESS_IDENTIFIER;
    private static final AtomicInteger NEXT_COUNTER = new AtomicInteger(new SecureRandom().nextInt());

    private static final char[] HEX_CHARS = new char[] {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private final int timestamp;
    private final int machineProcessIdentifier;
    private final int counter;

    /**
     * Gets a new object id.
     *
     * @return the new id
     */
    public static RandomIDGennerator get() {
        return new RandomIDGennerator();
    }

   
    /**
     * Gets the generated machine identifier.
     *
     * @return an int representing the machine identifier
     */
    public static int getGeneratedMachineProcessIdentifier() {
        return MACHINE_PROCESS_IDENTIFIER;
    }

    /**
     * Gets the current value of the auto-incrementing counter.
     *
     * @return the current counter value.
     */
    public static int getCurrentCounter() {
        return NEXT_COUNTER.get();
    }

    /**
     * Create a new object id.
     */
    public RandomIDGennerator() {
        this(new Date());
    }

    /**
     * Constructs a new instance using the given date.
     *
     * @param date the date
     */
    public RandomIDGennerator(final Date date) {
        this(dateToTimestampSeconds(date), MACHINE_PROCESS_IDENTIFIER, NEXT_COUNTER.getAndIncrement(), false);
    }

    private RandomIDGennerator(final int timestamp, final int machineProcessIdentifier, final int counter,
                     final boolean checkCounter) {
        if (checkCounter && ((counter & 0xff000000) != 0)) {
            throw new IllegalArgumentException("The counter must be between 0 and 16777215 (it must fit in three bytes).");
        }
        this.timestamp = timestamp;
        this.machineProcessIdentifier = machineProcessIdentifier;
        this.counter = counter & LOW_ORDER_THREE_BYTES;
    }

    
    /**
     * Convert to a byte array.  Note that the numbers are stored in big-endian order.
     *
     * @return the byte array
     * @throws Exception 
     */
    public byte[] toByteArray() {
    	byte[] bytes = new byte[8];
        System.arraycopy(NumberUtils.int2bytes(timestamp), 0, bytes, 0, 4);
        
        byte[] tmp = new byte[4+4];
        System.arraycopy(NumberUtils.int2bytes(machineProcessIdentifier), 0, tmp, 0, 4);
        System.arraycopy(NumberUtils.int2bytes(counter), 0, tmp, 4, 4);
        
        String str;
		try {
			str = new String(tmp, "ISO-8859-1");
		} catch (UnsupportedEncodingException e) {
			str = UUIDGenerator.generateString();
		}
        int hash = (int)DefaultHashAlgorithm.MURMUR_32_HASH.hash(str);
        System.arraycopy(NumberUtils.int2bytes(hash), 0, bytes, 4, 4);
        
        return bytes;
    }

    public long generate() {
        return NumberUtils.bytes2long(toByteArray())[0];
    }
    
    /**
     * Converts this instance into a 24-byte hexadecimal string representation.
     *
     * @return a string representation of the ObjectId in hexadecimal format
     * @throws Exception 
     */
	public String toHexString() {
		char[] chars = new char[16];
		int i = 0;
		for (byte b : toByteArray()) {
			chars[i++] = HEX_CHARS[b >> 4 & 0xF];
			chars[i++] = HEX_CHARS[b & 0xF];
		}
		return new String(chars);
	}
    
    /**
     * Gets the timestamp (number of seconds since the Unix epoch).
     *
     * @return the timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the machine identifier.
     *
     * @return the machine identifier
     */
    public long getMachineProcessIdentifier() {
        return machineProcessIdentifier;
    }

    /**
     * Gets the counter.
     *
     * @return the counter
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Gets the timestamp as a {@code Date} instance.
     *
     * @return the Date
     */
    public Date getDate() {
        return new Date(timestamp*1000);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        RandomIDGennerator objectId = (RandomIDGennerator) o;

        if (counter != objectId.counter) {
            return false;
        }
        if (machineProcessIdentifier != objectId.machineProcessIdentifier) {
            return false;
        }
        if (timestamp != objectId.timestamp) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = timestamp;
        result = 31 * result + (int)machineProcessIdentifier;
        result = 31 * result + counter;
        return result;
    }

    public int compareTo(final RandomIDGennerator other) {
        if (other == null) {
            throw new NullPointerException();
        }

        byte[] byteArray = toByteArray();
        byte[] otherByteArray = other.toByteArray();
        for (int i = 0; i < 12; i++) {
            if (byteArray[i] != otherByteArray[i]) {
                return ((byteArray[i] & 0xff) < (otherByteArray[i] & 0xff)) ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        try {
            return new String(toByteArray(), "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    static {
        try {
            MACHINE_PROCESS_IDENTIFIER = createMachineProcessIdentifier();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int createMachineProcessIdentifier() {
    	byte[] ary = new byte[4];
    	try {
    		Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                NetworkInterface ni = e.nextElement();
                byte[] mac = ni.getHardwareAddress();
                if (mac != null) {
                	ary[0] = (byte) (mac[0] + mac[1] + mac[2]);
                	ary[1] = (byte) (mac[3] + mac[4] + mac[5]);
                    break;
                }
            }
            
            short processId;
            try {
                String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
                if (processName.contains("@")) {
                    processId = (short) Integer.parseInt(processName.substring(0, processName.indexOf('@')));
                } else {
                    processId = (short) java.lang.management.ManagementFactory.getRuntimeMXBean().getName().hashCode();
                }
            } catch (Throwable t) {
                processId = (short) new SecureRandom().nextInt();
            }
            System.arraycopy(NumberUtils.short2bytes(processId), 0, ary, 2, 2);
            
            return NumberUtils.bytes2int(ary)[0];
        } catch (Throwable t) {
            // exception sometimes happens with IBM JVM, use random
        	return new SecureRandom().nextInt();
        }
    }

    private static int dateToTimestampSeconds(final Date time) {
        return (int) (time.getTime() / 1000);
    }

}

