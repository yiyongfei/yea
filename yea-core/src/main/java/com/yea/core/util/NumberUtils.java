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
package com.yea.core.util;

/**
 * 
 * @author yiyongfei
 * 
 */
public class NumberUtils {
    
    public static byte[] long2bytes(long... numbers) {
        byte[] b = new byte[8 * numbers.length];
        for(int i = 0; i < numbers.length; i++){
            byte[] tmp = _long2bytes(numbers[i]);
            for(int j = 0; j < tmp.length; j++){
                b[i*8 + j] = tmp[j];
            }
        }
        return b;
    }
    
    public static long[] bytes2long(byte[] ary) {
        int longLen = ary.length / 8;
        long[] aryLong = new long[longLen];
        for(int i = 0; i < aryLong.length; i++){
            byte[] tmp = new byte[8];
            for(int j = 0; j < tmp.length; j++){
                tmp[j] = ary[i * 8 + j];
            }
            aryLong[i] = _bytes2long(tmp);
        }
        return aryLong;
    }
    
    public static byte[] int2bytes(int... numbers) {
        byte[] b = new byte[4 * numbers.length];
        for(int i = 0; i < numbers.length; i++){
            byte[] tmp = _int2bytes(numbers[i]);
            for(int j = 0; j < tmp.length; j++){
                b[i*8 + j] = tmp[j];
            }
        }
        return b;
    }
    
    public static int[] bytes2int(byte[] ary) {
        int intLen = ary.length / 4;
        int[] aryInt = new int[intLen];
        for(int i = 0; i < aryInt.length; i++){
            byte[] tmp = new byte[4];
            for(int j = 0; j < tmp.length; j++){
                tmp[j] = ary[i * 4 + j];
            }
            aryInt[i] = _bytes2int(tmp);
        }
        return aryInt;
    }
    
    public static byte[] short2bytes(short... numbers) {
        byte[] b = new byte[2 * numbers.length];
        for(int i = 0; i < numbers.length; i++){
            byte[] tmp = _short2bytes(numbers[i]);
            for(int j = 0; j < tmp.length; j++){
                b[i*8 + j] = tmp[j];
            }
        }
        return b;
    }
    
    public static short[] bytes2short(byte[] ary) {
        int shortLen = ary.length / 2;
        short[] aryShort = new short[shortLen];
        for(int i = 0; i < aryShort.length; i++){
            byte[] tmp = new byte[2];
            for(int j = 0; j < tmp.length; j++){
                tmp[j] = ary[i * 2 + j];
            }
            aryShort[i] = _bytes2short(tmp);
        }
        return aryShort;
    }
    
    private static long _bytes2long(byte[] ary) {
        long temp = 0;
        long res = 0;
        for (int i = 0; i < 8; i++) {
            res <<= 8;
            temp = ary[i] & 0xff;
            res |= temp;
        }
        return res;
    }
    private static byte[] _long2bytes(long number) {
        byte[] b = new byte[8];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (number >>> (56 - (i * 8)));
        }
        return b;
    }
    
    private static int _bytes2int(byte[] ary) {
        int temp = 0;
        int res = 0;
        for (int i = 0; i < 4; i++) {
            res <<= 8;
            temp = ary[i] & 0xff;
            res |= temp;
        }
        return res;
    }
    private static byte[] _int2bytes(int number) {
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (number >>> (24 - (i * 8)));
        }
        return b;
    }
    
    private static short _bytes2short(byte[] ary) {
        int temp = 0;
        short res = 0;
        for (int i = 0; i < 2; i++) {
            res <<= 8;
            temp = ary[i] & 0xff;
            res |= temp;
        }
        return res;
    }
    private static byte[] _short2bytes(short number) {
        byte[] b = new byte[2];
        for (int i = 0; i < b.length; i++) {
            b[i] = (byte) (number >>> (8 - (i * 8)));
        }
        return b;
    }
    
}
