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
package com.yea.core.hash;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;

import com.yea.core.util.NumberUtils;

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * @author yiyongfei
 * 
 */
public enum DefaultHashAlgorithm implements HashAlgorithm {

    /**
     * Native hash (String.hashCode()).
     */
    NATIVE_HASH,
    /**
     * CRC_HASH as used by the perl API. This will be more consistent both
     * across multiple API users as well as java versions, but is mostly likely
     * significantly slower.
     */
    CRC_HASH,
    /**
     * FNV hashes are designed to be fast while maintaining a low collision rate.
     * The FNV speed allows one to quickly hash lots of data while maintaining a
     * reasonable collision rate.
     *
     * @see <a href="http://www.isthe.com/chongo/tech/comp/fnv/">fnv
     *      comparisons</a>
     * @see <a href="http://en.wikipedia.org/wiki/Fowler_Noll_Vo_hash">fnv at
     *      wikipedia</a>
     */
    FNV1_64_HASH,
    /**
     * Variation of FNV.
     */
    FNV1A_64_HASH,
    /**
     * 32-bit FNV1.
     */
    FNV1_32_HASH,
    /**
     * 32-bit FNV1a.
     */
    FNV1A_32_HASH,
    /**
     * MD5-based hash algorithm used by ketama.
     */
    KETAMA_HASH,

    /**
     * 32-bit murmur.
     */
    MURMUR_32_HASH,
    
    /**
     * 64-bit murmur.
     */
    MURMUR_64_HASH;
    
    private static final long FNV_64_INIT = 0xcbf29ce484222325L;
    private static final long FNV_64_PRIME = 0x100000001b3L;

    private static final long FNV_32_INIT = 2166136261L;
    private static final long FNV_32_PRIME = 16777619;

    private static MessageDigest md5Digest = null;

    static {
        try {
            md5Digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not supported", e);
        }
    }

    /**
     * Compute the hash for the given key.
     *
     * @return a positive integer hash
     */
    public long hash(final String k) {
        long rv = 0;
        int len = k.length();
        switch (this) {
            case NATIVE_HASH:
                rv = k.hashCode();
                break;
            case CRC_HASH:
                // return (crc32(shift) >> 16) & 0x7fff;
                CRC32 crc32 = new CRC32();
                crc32.update(KeyUtil.getKeyBytes(k));
                rv = (crc32.getValue() >> 16) & 0x7fff;
                break;
            case FNV1_64_HASH:
                // Thanks to pierre@demartines.com for the pointer
                rv = FNV_64_INIT;
                for (int i = 0; i < len; i++) {
                    rv *= FNV_64_PRIME;
                    rv ^= k.charAt(i);
                }
                break;
            case FNV1A_64_HASH:
                rv = FNV_64_INIT;
                for (int i = 0; i < len; i++) {
                    rv ^= k.charAt(i);
                    rv *= FNV_64_PRIME;
                }
                break;
            case FNV1_32_HASH:
                rv = FNV_32_INIT;
                for (int i = 0; i < len; i++) {
                    rv *= FNV_32_PRIME;
                    rv ^= k.charAt(i);
                }
                break;
            case FNV1A_32_HASH:
                rv = FNV_32_INIT;
                for (int i = 0; i < len; i++) {
                    rv ^= k.charAt(i);
                    rv *= FNV_32_PRIME;
                }
                break;
            case KETAMA_HASH:
                byte[] bKey = computeMd5(k);
                rv = ((long) (bKey[3] & 0xFF) << 24) | ((long) (bKey[2] & 0xFF) << 16) | ((long) (bKey[1] & 0xFF) << 8) | (bKey[0] & 0xFF);
                break;
            case MURMUR_32_HASH:
                byte[] bM32 = KeyUtil.getKeyBytes(k);
                return murmurhash3_x86_32(bM32, 0, bM32.length, 40503);
            case MURMUR_64_HASH:
                byte[] bM64 = KeyUtil.getKeyBytes(k);
                return murmurhash3_x64_64(bM64, 0, bM64.length, 2654435769L);
            default:
                assert false;
        }
        return rv & 0xffffffffL; /* Truncate to 32-bits */
    }

    /**
     * Get the md5 of the given key.
     */
    public static byte[] computeMd5(String k) {
        MessageDigest md5;
        try {
            md5 = (MessageDigest) md5Digest.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("clone of MD5 not supported", e);
        }
        md5.update(KeyUtil.getKeyBytes(k));
        return md5.digest();
    }
    

    /** Returns the MurmurHash3_x86_32 hash. */
    public static int murmurhash3_x86_32(byte[] data, int offset, int len, int seed) {

        final int c1 = 0xcc9e2d51;
        final int c2 = 0x1b873593;

        int h1 = seed;
        int roundedEnd = offset + (len & 0xfffffffc); // round down to 4 byte block

        for (int i = offset; i < roundedEnd; i += 4) {
            // little endian load order
            int k1 = (data[i] & 0xff) | ((data[i + 1] & 0xff) << 8) | ((data[i + 2] & 0xff) << 16) | (data[i + 3] << 24);
            k1 *= c1;
            k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
            k1 *= c2;

            h1 ^= k1;
            h1 = (h1 << 13) | (h1 >>> 19); // ROTL32(h1,13);
            h1 = h1 * 5 + 0xe6546b64;
        }

        // tail
        int k1 = 0;

        switch (len & 0x03) {
            case 3:
                k1 = (data[roundedEnd + 2] & 0xff) << 16;
                // fallthrough
            case 2:
                k1 |= (data[roundedEnd + 1] & 0xff) << 8;
                // fallthrough
            case 1:
                k1 |= (data[roundedEnd] & 0xff);
                k1 *= c1;
                k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
                k1 *= c2;
                h1 ^= k1;
        }

        // finalization
        h1 ^= len;

        h1 = fmix32(h1);

        byte[] ary = NumberUtils.long2bytes(h1);
        //符合位如果为负数，需转成正数
        if (ary[0] < 0) {
            ary[0] = (byte) (ary[0] & 0xff >> 1);
        }
        return new BigInteger(ary).intValue();
    }
    

    /** Returns the MurmurHash3_x64_64 hash, placing the result in "out". */
    public static long murmurhash3_x64_64(byte[] key, int offset, int len, long seed) {
        // The original algorithm does have a 32 bit unsigned seed.
        // We have to mask to match the behavior of the unsigned types and prevent sign extension.
        long h1 = seed & 0x00000000FFFFFFFFL;

        final long c1 = 0x87c37b91114253d5L;
        final long c2 = 0x4cf5ad432745937fL;

        int roundedEnd = offset + (len & 0xFFFFFFF8); // round down to 8 byte block
        for (int i = offset; i < roundedEnd; i += 8) {
            long k1 = getLongLittleEndian(key, i);
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            
            h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27);
            h1 = h1 * 5 + 0x52dce729;
            
        }

        long k1 = 0;

        switch (len & 0x07) {
            case 7:
                k1 |= (key[roundedEnd + 6] & 0xffL) << 48;
            case 6:
                k1 |= (key[roundedEnd + 5] & 0xffL) << 40;
            case 5:
                k1 |= (key[roundedEnd + 4] & 0xffL) << 32;
            case 4:
                k1 |= (key[roundedEnd + 3] & 0xffL) << 24;
            case 3:
                k1 |= (key[roundedEnd + 2] & 0xffL) << 16;
            case 2:
                k1 |= (key[roundedEnd + 1] & 0xffL) << 8;
            case 1:
                k1 |= (key[roundedEnd] & 0xffL);
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
        }

        //----------
        // finalization

        h1 ^= len;


        h1 = fmix64(h1);

        byte[] ary = NumberUtils.long2bytes(h1);
        //符合位如果为负数，需转成正数
        if (ary[0] < 0) {
            ary[0] = (byte) (ary[0] & 0xff >> 1);
        }
        return new BigInteger(ary).longValue();
    }
    
    /** Returns the MurmurHash3_x64_128 hash, placing the result in "out". */
    public static BigInteger murmurhash3_x64_128(byte[] key, int offset, int len, long seed) {
        // The original algorithm does have a 32 bit unsigned seed.
        // We have to mask to match the behavior of the unsigned types and prevent sign extension.
        long h1 = seed & 0x00000000FFFFFFFFL;
        long h2 = seed & 0x00000000FFFFFFFFL;

        final long c1 = 0x87c37b91114253d5L;
        final long c2 = 0x4cf5ad432745937fL;

        int roundedEnd = offset + (len & 0xFFFFFFF0); // round down to 16 byte block
        for (int i = offset; i < roundedEnd; i += 16) {
            long k1 = getLongLittleEndian(key, i);
            long k2 = getLongLittleEndian(key, i + 8);
            k1 *= c1;
            k1 = Long.rotateLeft(k1, 31);
            k1 *= c2;
            h1 ^= k1;
            h1 = Long.rotateLeft(h1, 27);
            h1 += h2;
            h1 = h1 * 5 + 0x52dce729;
            k2 *= c2;
            k2 = Long.rotateLeft(k2, 33);
            k2 *= c1;
            h2 ^= k2;
            h2 = Long.rotateLeft(h2, 31);
            h2 += h1;
            h2 = h2 * 5 + 0x38495ab5;
        }

        long k1 = 0;
        long k2 = 0;

        switch (len & 0x0F) {
            case 15:
                k2 = (key[roundedEnd + 14] & 0xffL) << 48;
            case 14:
                k2 |= (key[roundedEnd + 13] & 0xffL) << 40;
            case 13:
                k2 |= (key[roundedEnd + 12] & 0xffL) << 32;
            case 12:
                k2 |= (key[roundedEnd + 11] & 0xffL) << 24;
            case 11:
                k2 |= (key[roundedEnd + 10] & 0xffL) << 16;
            case 10:
                k2 |= (key[roundedEnd + 9] & 0xffL) << 8;
            case 9:
                k2 |= (key[roundedEnd + 8] & 0xffL);
                k2 *= c2;
                k2 = Long.rotateLeft(k2, 33);
                k2 *= c1;
                h2 ^= k2;
            case 8:
                k1 = ((long) key[roundedEnd + 7]) << 56;
            case 7:
                k1 |= (key[roundedEnd + 6] & 0xffL) << 48;
            case 6:
                k1 |= (key[roundedEnd + 5] & 0xffL) << 40;
            case 5:
                k1 |= (key[roundedEnd + 4] & 0xffL) << 32;
            case 4:
                k1 |= (key[roundedEnd + 3] & 0xffL) << 24;
            case 3:
                k1 |= (key[roundedEnd + 2] & 0xffL) << 16;
            case 2:
                k1 |= (key[roundedEnd + 1] & 0xffL) << 8;
            case 1:
                k1 |= (key[roundedEnd] & 0xffL);
                k1 *= c1;
                k1 = Long.rotateLeft(k1, 31);
                k1 *= c2;
                h1 ^= k1;
        }

        //----------
        // finalization

        h1 ^= len;
        h2 ^= len;

        h1 += h2;
        h2 += h1;

        h1 = fmix64(h1);
        h2 = fmix64(h2);

        h1 += h2;
        h2 += h1;

        byte[] ary = NumberUtils.long2bytes(h1, h2);
        //符合位如果为负数，需转成正数
        if (ary[0] < 0) {
            ary[0] = (byte) (ary[0] & 0xff >> 1);
        }
        return new BigInteger(ary);
    }
    

    private static final int fmix32(int h) {
        h ^= h >>> 16;
        h *= 0x85ebca6b;
        h ^= h >>> 13;
        h *= 0xc2b2ae35;
        h ^= h >>> 16;
        return h;
    }

    private static final long fmix64(long k) {
        k ^= k >>> 33;
        k *= 0xff51afd7ed558ccdL;
        k ^= k >>> 33;
        k *= 0xc4ceb9fe1a85ec53L;
        k ^= k >>> 33;
        return k;
    }

    /** Gets a long from a byte buffer in little endian byte order. */
    private static long getLongLittleEndian(byte[] buf, int offset) {
        return ((long) buf[offset + 7] << 56) // no mask needed
               | ((buf[offset + 6] & 0xffL) << 48) | ((buf[offset + 5] & 0xffL) << 40) | ((buf[offset + 4] & 0xffL) << 32) | ((buf[offset + 3] & 0xffL) << 24) | ((buf[offset + 2] & 0xffL) << 16) | ((buf[offset + 1] & 0xffL) << 8) | ((buf[offset] & 0xffL)); // no shift needed
    }
}
