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
package com.yea.core.compress;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.CompressorOutputStream;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorOutputStream;
import org.apache.commons.compress.compressors.deflate.DeflateParameters;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipParameters;
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lz4.FramedLZ4CompressorOutputStream;
import org.apache.commons.compress.compressors.lzma.LZMACompressorOutputStream;
import org.apache.commons.compress.compressors.pack200.Pack200CompressorOutputStream;
import org.apache.commons.compress.compressors.snappy.FramedSnappyCompressorOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;

import com.github.luben.zstd.Zstd;

/**
 * 数据压缩
 * @author yiyongfei
 *
 */
public class Compress implements ICompress {
	public static final int BUFFER = 1024; 
	private String compressionAlgorithm = CompressorStreamFactory.GZIP;
	private int compressionLevel = 3;//默认压缩等级3，只针对GZIP、DEFLATE
	
	private InnerCompressor getCompressor() throws Exception {
		if("zstd".equals(compressionAlgorithm)) {
			return new InnerZstdCompressor(compressionLevel);
		} else {
			return new InnerGeneralCompressor(compressionAlgorithm, compressionLevel);
		}
	}
	
	private InnerDecompressor getDecompressor() throws Exception {
		if("zstd".equals(compressionAlgorithm)) {
			return new InnerZstdDecompressor();
		} else {
			return new InnerGeneralDecompressor(compressionAlgorithm);
		}
	}
	
	@Override
	public byte[] compress(byte[] data) throws Exception {
		// TODO Auto-generated method stub
		InnerCompressor compressor = getCompressor();
		try{
			return compressor.compress(data);
		} finally {
			compressor.close();
		}
	}

	
	@Override
	public byte[] decompress(byte[] data) throws Exception {
		// TODO Auto-generated method stub
		InnerDecompressor decompressor = getDecompressor();
		try{
			return decompressor.decompress(data);
		} finally {
			decompressor.close();
		}
	}

	public Compress setCompressionAlgorithm(String compressionAlgorithm) {
		if(CompressorStreamFactory.getSingleton().getOutputStreamCompressorNames().contains(compressionAlgorithm) || "zstd".equals(compressionAlgorithm)) {
			this.compressionAlgorithm = compressionAlgorithm;
			return this;
		} else {
			throw new IllegalArgumentException("Invalid compression algorithm: " + compressionAlgorithm);
		}
	}
	
	public Compress setCompressionLevel(int compressionLevel) {
		this.compressionLevel = compressionLevel;
		return this;
	}

	
	/*内部压缩工具*/
	interface InnerCompressor {
		byte[] compress(byte[] data) throws Exception;
		void close() throws Exception;
	}
	interface InnerDecompressor {
		byte[] decompress(byte[] data) throws Exception;
		void close() throws Exception;
	}
	
	/**
	 * Facebook的Zstandard压缩
	 * @author yiyongfei
	 *
	 */
	class InnerZstdCompressor implements InnerCompressor {
		private int compressionLevel;

		InnerZstdCompressor(int compressionLevel) {
			this.compressionLevel = compressionLevel;
		}

		@Override
		public byte[] compress(byte[] data) throws Exception {
			return Zstd.compress(data, compressionLevel);
		}

		@Override
		public void close() throws Exception {
			// TODO Auto-generated method stub
			
		}
	}
	
	class InnerZstdDecompressor implements InnerDecompressor {
		InnerZstdDecompressor() {
		}

		@Override
		public byte[] decompress(byte[] data) throws Exception {
			int size = (int) Zstd.decompressedSize(data);
			if (size <= 0) {
				size = Integer.MAX_VALUE;
			}
			return Zstd.decompress(data, size);
		}

		@Override
		public void close() throws Exception {
			// TODO Auto-generated method stub
		}
	}
	
	class InnerGeneralCompressor implements InnerCompressor {
		private CompressorOutputStream compressorOutputStream;
		private ByteArrayOutputStream outStream;
		private String _compressionAlgorithm;

		InnerGeneralCompressor(String compressionAlgorithm, int compressionLevel) throws Exception {
			this._compressionAlgorithm = compressionAlgorithm;
			this.outStream = new ByteArrayOutputStream();
			if(CompressorStreamFactory.DEFLATE.equals(compressionAlgorithm)) {
				DeflateParameters parameters = new DeflateParameters();
				parameters.setCompressionLevel(compressionLevel);
				this.compressorOutputStream = new DeflateCompressorOutputStream(outStream, parameters);
			} else if(CompressorStreamFactory.GZIP.equals(compressionAlgorithm)) {
				GzipParameters parameters = new GzipParameters();
				parameters.setCompressionLevel(compressionLevel);
				this.compressorOutputStream = new GzipCompressorOutputStream(outStream, parameters);
			} else if(CompressorStreamFactory.BZIP2.equals(compressionAlgorithm)) {
				this.compressorOutputStream = new BZip2CompressorOutputStream(outStream, compressionLevel);
			} else if(CompressorStreamFactory.XZ.equals(compressionAlgorithm)) {
				this.compressorOutputStream = new XZCompressorOutputStream(outStream, compressionLevel);
			} else {
				this.compressorOutputStream = CompressorStreamFactory.getSingleton().createCompressorOutputStream(compressionAlgorithm, outStream); 
			}
		}
		
		@Override
		public byte[] compress(byte[] data) throws Exception {
			compressorOutputStream.write(data);
			finish();
			compressorOutputStream.flush();
			return outStream.toByteArray();
		}
		@Override
		public void close() throws Exception {
			compressorOutputStream.close();
			outStream.close();
		}
		
		private void finish() throws IOException {
			if (CompressorStreamFactory.GZIP.equalsIgnoreCase(_compressionAlgorithm)) {
				((GzipCompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.BZIP2.equalsIgnoreCase(_compressionAlgorithm)) {
				((BZip2CompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.XZ.equalsIgnoreCase(_compressionAlgorithm)) {
				((XZCompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.PACK200.equalsIgnoreCase(_compressionAlgorithm)) {
				((Pack200CompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.LZMA.equalsIgnoreCase(_compressionAlgorithm)) {
				((LZMACompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.DEFLATE.equalsIgnoreCase(_compressionAlgorithm)) {
				((DeflateCompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.SNAPPY_FRAMED.equalsIgnoreCase(_compressionAlgorithm)) {
				((FramedSnappyCompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.LZ4_BLOCK.equalsIgnoreCase(_compressionAlgorithm)) {
				((BlockLZ4CompressorOutputStream) compressorOutputStream).finish();
			}
			if (CompressorStreamFactory.LZ4_FRAMED.equalsIgnoreCase(_compressionAlgorithm)) {
				((FramedLZ4CompressorOutputStream) compressorOutputStream).finish();
			}
		}
	}
	
	class InnerGeneralDecompressor implements InnerDecompressor {
		private String _compressionAlgorithm;

		InnerGeneralDecompressor(String compressionAlgorithm) {
			this._compressionAlgorithm = compressionAlgorithm;
		}

		@Override
		public byte[] decompress(byte[] data) throws Exception {
			ByteArrayInputStream inStream = new ByteArrayInputStream(data);
			ByteArrayOutputStream outStream = new ByteArrayOutputStream();
			CompressorInputStream compressorInputStream = CompressorStreamFactory.getSingleton()
					.createCompressorInputStream(_compressionAlgorithm, inStream);
			try {
				int count;
				byte[] tmp = new byte[BUFFER];
				while ((count = compressorInputStream.read(tmp, 0, BUFFER)) != -1) {
					outStream.write(tmp, 0, count);
				}
				return outStream.toByteArray();
			} finally {
				compressorInputStream.close();
				inStream.close();
				outStream.close();
			}
		}

		@Override
		public void close() throws Exception {
			// TODO Auto-generated method stub
		}
	}
	
}
