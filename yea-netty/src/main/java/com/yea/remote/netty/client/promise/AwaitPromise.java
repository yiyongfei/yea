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
package com.yea.remote.netty.client.promise;

import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.yea.core.exception.constants.YeaErrorMessage;
import com.yea.core.remote.constants.RemoteConstants;
import com.yea.core.remote.exception.RemoteException;
import com.yea.core.remote.observer.Observable;
import com.yea.core.remote.observer.Observer;
import com.yea.core.remote.struct.Header;
import com.yea.remote.netty.promise.NettyChannelPromise;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPromise;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;


/**
 * 
 * @author yiyongfei
 * 
 */
public class AwaitPromise<T> implements NettyChannelPromise<T>, Observer<T> {
    private ChannelPromise promise;
    private volatile T object;
    private volatile Throwable throwable;
    private volatile boolean isChanged;
    private volatile boolean isSuccess;
    private volatile byte[] sessionID;
//    private volatile List<Class<?>> allowObservable;
    
    public AwaitPromise(ChannelPromise promise){
        this.promise = promise;
        this.isChanged = false;
        this.object = null;
//        this.allowObservable = new ArrayList<Class<?>>();
//        allowObservable.add(ServiceClientHandler.class);
//        allowObservable.add(ExceptionHandler.class);
//        allowObservable.add(DispatchClientHandler.class);
    }
    
//    public boolean isAllowObservable(Class<?> clazz) {
//        return allowObservable.contains(clazz);
//    }
    
    public T awaitObject() throws Throwable{
        return awaitObject(0L);
    }
    
    public T awaitObject(long timeout) throws Throwable{
        long startTime = new Date().getTime();
        return awaitObject(startTime, timeout);
    }
    
    private T awaitObject(long startTime, long timeout) throws Throwable{
        if(this.isDone()){
            if(this.isSuccess()){
                if(this.hasChanged(sessionID)){
					if (this.isSuccess) {
						return this.object;
					} else {
					    throw new RemoteException(YeaErrorMessage.ERR_APPLICATION, RemoteConstants.ExceptionType.BUSINESS.value() ,this.throwable.getMessage(), this.throwable);
					}
                    
                } else {
                    while(true){
                        TimeUnit.MILLISECONDS.sleep(25);
                        if(this.hasChanged(sessionID)){
                            break;
                        }
                        long endTime = new Date().getTime();
                        if(timeout > 0L && (endTime - startTime > timeout)) {
                            throw new RemoteException(YeaErrorMessage.ERR_FOUNDATION, RemoteConstants.ExceptionType.TIMEOUT.value() ,"获取数据超时！", null);
                        }
                    }
                    if (this.isSuccess) {
						return this.object;
					} else {
					    throw new RemoteException(YeaErrorMessage.ERR_APPLICATION, RemoteConstants.ExceptionType.BUSINESS.value() ,this.throwable.getMessage(), this.throwable);
					}
                }
            } else {
            	/*请求已完成，但结果是不成功*/
            	if(this.cause() != null) {
            	    throw new RemoteException(YeaErrorMessage.ERR_FOUNDATION, RemoteConstants.ExceptionType.OTHER.value() ,this.cause().getMessage(), this.cause());
            	} else {
            	    throw new RemoteException(YeaErrorMessage.ERR_FOUNDATION, RemoteConstants.ExceptionType.OTHER.value() ,"未知异常！", null);
            	}
            }
        } else {
            /*请求尚未完成，等待50毫秒后重新再获取*/
            TimeUnit.MILLISECONDS.sleep(50);
            long endTime = new Date().getTime();
            if(timeout > 0L && (endTime - startTime > timeout)) {
                throw new RemoteException(YeaErrorMessage.ERR_FOUNDATION, RemoteConstants.ExceptionType.TIMEOUT.value() ,"获取数据超时！", null);
            }
            return awaitObject(startTime, timeout);
        }
        
    }
    
    /** 
     * @see com.yea.core.remote.observer.Observer#update(com.yea.core.remote.observer.Observable, java.lang.Object)
     */
    public void update(byte[] sessionID, Header header, Observable o, T arg) {
        this.sessionID = sessionID;
        o.deleteObserver(sessionID, this);
		if (RemoteConstants.MessageResult.SUCCESS.value() == header.getResult()) {
			this.object = arg;
			this.isSuccess = true;
		} else {
			this.throwable = (Throwable)arg;
			this.isSuccess = false;
		}
		this.isChanged = true;
    }

    /** 
     * @see com.yea.core.remote.observer.Observer#hasChanged()
     */
    public boolean hasChanged(byte[] sessionID) {
        return this.isChanged;
    }
    
    /** 
     * @see io.netty.channel.ChannelFuture#channel()
     */
    public Channel channel() {
        return promise.channel();
    }

    

    /** 
     * @see io.netty.util.concurrent.Future#isSuccess()
     */
    public boolean isSuccess() {
        return promise.isSuccess();
    }

    /** 
     * @see io.netty.util.concurrent.Future#isCancellable()
     */
    public boolean isCancellable() {
        return promise.isCancellable();
    }

    /** 
     * @see io.netty.util.concurrent.Future#cause()
     */
    public Throwable cause() {
        return promise.cause();
    }

    /** 
     * @see io.netty.util.concurrent.Future#await(long, java.util.concurrent.TimeUnit)
     */
    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return promise.await(timeout, unit);
    }

    /** 
     * @see io.netty.util.concurrent.Future#await(long)
     */
    public boolean await(long timeoutMillis) throws InterruptedException {
        return promise.await(timeoutMillis);
    }

    /** 
     * @see io.netty.util.concurrent.Future#awaitUninterruptibly(long, java.util.concurrent.TimeUnit)
     */
    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return promise.awaitUninterruptibly(timeout, unit);
    }

    /** 
     * @see io.netty.util.concurrent.Future#awaitUninterruptibly(long)
     */
    public boolean awaitUninterruptibly(long timeoutMillis) {
        return promise.awaitUninterruptibly(timeoutMillis);
    }

    /** 
     * @see io.netty.util.concurrent.Future#getNow()
     */
    public Void getNow() {
        return promise.getNow();
    }

    /** 
     * @see io.netty.util.concurrent.Future#cancel(boolean)
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        return promise.cancel(mayInterruptIfRunning);
    }


	@Override
	public ChannelPromise setSuccess(Void result) {
		return promise.setSuccess(result);
	}

	@Override
	public ChannelPromise setSuccess() {
		return promise.setSuccess();
	}

	@Override
	public boolean trySuccess() {
		return promise.trySuccess();
	}

	@Override
	public ChannelPromise setFailure(Throwable cause) {
		return promise.setFailure(cause);
	}

	@Override
	public ChannelPromise addListener(GenericFutureListener<? extends Future<? super Void>> listener) {
		return promise.addListener(listener);
	}

	@Override
	public ChannelPromise addListeners(@SuppressWarnings("unchecked") GenericFutureListener<? extends Future<? super Void>>... listeners) {
		return promise.addListeners(listeners);
	}

	@Override
	public ChannelPromise removeListener(GenericFutureListener<? extends Future<? super Void>> listener) {
		return promise.removeListener(listener);
	}

	@Override
	public ChannelPromise removeListeners(@SuppressWarnings("unchecked") GenericFutureListener<? extends Future<? super Void>>... listeners) {
		return promise.removeListeners(listeners);
	}

	@Override
	public ChannelPromise sync() throws InterruptedException {
		return promise.sync();
	}

	@Override
	public ChannelPromise syncUninterruptibly() {
		return promise.syncUninterruptibly();
	}

	@Override
	public ChannelPromise await() throws InterruptedException {
		return promise.await();
	}

	@Override
	public ChannelPromise awaitUninterruptibly() {
		return promise.awaitUninterruptibly();
	}

	@Override
	public boolean trySuccess(Void result) {
		return promise.trySuccess(result);
	}

	@Override
	public boolean tryFailure(Throwable cause) {
		return promise.tryFailure(cause);
	}

	@Override
	public boolean setUncancellable() {
		return promise.setUncancellable();
	}
	
    /** 
     * @see java.util.concurrent.Future#isCancelled()
     */
    public boolean isCancelled() {
        return promise.isCancelled();
    }

    /** 
     * @see java.util.concurrent.Future#isDone()
     */
    public boolean isDone() {
        return promise.isDone();
    }

    /** 
     * @see java.util.concurrent.Future#get()
     */
    public Void get() throws InterruptedException, ExecutionException {
        return promise.get();
    }

    /** 
     * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
     */
    public Void get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return promise.get(timeout, unit);
    }

}