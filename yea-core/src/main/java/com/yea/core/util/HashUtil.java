package com.yea.core.util;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.yea.core.exception.YeaException;

public class HashUtil {
	
	public static byte[] hash(String algorithmName, Object source, Object salt, int hashIterations) {
		byte[] saltBytes = CodecSupport.toBytes(salt);
		byte[] sourceBytes = CodecSupport.toBytes(source);
		hashIterations = Math.max(1, hashIterations);
		
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(algorithmName);
		} catch (NoSuchAlgorithmException e) {
			throw new YeaException(e.getMessage(), e);
		}
		if (salt != null) {
			digest.reset();
			digest.update(saltBytes);
		}
		byte[] hashed = digest.digest(sourceBytes);
		int iterations = hashIterations - 1; // already hashed once above
		for (int i = 0; i < iterations; i++) {
			digest.reset();
			hashed = digest.digest(hashed);
		}
		return hashed;
	}
	
	
	/**
	 * 复制自shiro
	 * @author yiyongfei
	 *
	 */
	static class CodecSupport {
	    static final String PREFERRED_ENCODING = "UTF-8";

	    /**
	     * Converts the specified character array to a byte array using the Shiro's preferred encoding (UTF-8).
	     * <p/>
	     * This is a convenience method equivalent to calling the {@link #toBytes(String,String)} method with a
	     * a wrapping String and {@link CodecSupport#PREFERRED_ENCODING PREFERRED_ENCODING}, i.e.
	     * <p/>
	     * <code>toBytes( new String(chars), {@link CodecSupport#PREFERRED_ENCODING PREFERRED_ENCODING} );</code>
	     *
	     * @param chars the character array to be converted to a byte array.
	     * @return the byte array of the UTF-8 encoded character array.
	     */
	    static byte[] toBytes(char[] chars) {
	        return toBytes(new String(chars), PREFERRED_ENCODING);
	    }

	    /**
	     * Converts the specified source argument to a byte array with Shiro's
	     * {@link CodecSupport#PREFERRED_ENCODING PREFERRED_ENCODING}.
	     *
	     * @param source the string to convert to a byte array.
	     * @return the bytes representing the specified string under the {@link CodecSupport#PREFERRED_ENCODING PREFERRED_ENCODING}.
	     * @see #toBytes(String, String)
	     */
	    static byte[] toBytes(String source) {
	        return toBytes(source, PREFERRED_ENCODING);
	    }

	    /**
	     * Converts the specified source to a byte array via the specified encoding, throwing a
	     * {@link CodecException CodecException} if the encoding fails.
	     *
	     * @param source   the source string to convert to a byte array.
	     * @param encoding the encoding to use to use.
	     * @return the byte array of the specified source with the given encoding.
	     * @throws CodecException if the JVM does not support the specified encoding.
	     */
	    static byte[] toBytes(String source, String encoding) {
	        try {
	            return source.getBytes(encoding);
	        } catch (UnsupportedEncodingException e) {
	            String msg = "Unable to convert source [" + source + "] to byte array using " +
	                    "encoding '" + encoding + "'";
	            throw new YeaException(msg, e);
	        }
	    }

	    /**
	     * Converts the specified Object into a byte array.
	     * <p/>
	     * If the argument is a {@code byte[]}, {@code char[]}, {@link ByteSource}, {@link String}, {@link File}, or
	     * {@link InputStream}, it will be converted automatically and returned.}
	     * <p/>
	     * If the argument is anything other than these types, it is passed to the
	     * {@link #objectToBytes(Object) objectToBytes} method which must be overridden by subclasses.
	     *
	     * @param o the Object to convert into a byte array
	     * @return a byte array representation of the Object argument.
	     */
	    static byte[] toBytes(Object o) {
	        if (o == null) {
	            String msg = "Argument for byte conversion cannot be null.";
	            throw new IllegalArgumentException(msg);
	        }
	        if (o instanceof byte[]) {
	            return (byte[]) o;
	        } else if (o instanceof char[]) {
	            return toBytes((char[]) o);
	        } else if (o instanceof String) {
	            return toBytes((String) o);
	        } else {
	        	String msg = "Argument for byte conversion cannot be "+o.getClass()+".";
	            throw new IllegalArgumentException(msg);
	        }
	    }
	}
}
