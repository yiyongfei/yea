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
package com.yea.core.loadbalancer;

import java.net.SocketAddress;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yea.core.remote.AbstractPoint;
import com.yea.core.remote.constants.RemoteConstants;

/**
 * 
 * @author yiyongfei
 *
 */
public abstract class AbstractBalancingNode extends BalancingNode {
	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBalancingNode.class);
	private AbstractPoint point = null;
	protected Double hystrix;
    protected Double unwrite;
    protected Double slow;
    private Date slowdownTime;

    public AbstractBalancingNode(AbstractPoint point, SocketAddress remoteAddress){
		super(remoteAddress.toString());
		this.hystrix = 1.0;
    	this.unwrite = 1.0;
    	this.slow = 1.0;
    	this.slowdownTime = null;
    	this.setZone(getHost());
		this.point = point;
    }
    
	public boolean resetServerHealth() {
		boolean _slow = false;
		boolean _send = false;
		boolean _hystrix = false;
		try {
			_slow = resetSlowHealth();
			_send = resetSendHealth();
			_hystrix = resetHystrixHealth();
			if (_slow || _send || _hystrix) {
				return true;
			} else {
				return false;
			}
		} finally {
			this.setSuspended((hystrix * unwrite * slow) < 1);
			LOGGER.info((_slow ? "重置慢权重," : "") + (_send ? "重置通道写权重," : "") + (_hystrix ? "重置熔断权重," : "")
					+ "此时节点状态[是否有效:" + isAlive() + ",临时失效:" + isSuspended() + "]");
		}
	}
	
	protected boolean resetHystrixHealth() {
		return false;
	}
	
	protected boolean resetSendHealth() {
		return false;
	}
	
	@Deprecated
	protected boolean resetSlowHealth() {
		if(this.slowdownTime != null) {
    		if(new Date().getTime() - this.slowdownTime.getTime() > (9 * 1000)) {
    			this.slow = 1.0;
    			this.slowdownTime = null;
    		}
    		return this.slow == 1.0;
    	}
		return false;
	}
	
	private final Lock lock = new ReentrantLock();
	
	public void renewServerHealth(RemoteConstants.ServerHealthType weithtType, Double weight) {
		lock.lock();
		try {
			if (weithtType.value() == RemoteConstants.ServerHealthType.HYSTRIX.value()) {
				renewHystrixHealth(weight);
			} else if (weithtType.value() == RemoteConstants.ServerHealthType.SEND.value()) {
				renewSendHealth(weight);
			} else if (weithtType.value() == RemoteConstants.ServerHealthType.SLOW.value()) {
				renewSlowHealth();
			}
		} finally {
			this.setSuspended((hystrix * unwrite * slow) < 1);
			lock.unlock();
		}
	}
	protected boolean renewHystrixHealth(Double weight) {
		if(hystrix != weight) {
			hystrix = weight;
			return true;
		} else {
			return false;
		}
	}
	protected boolean renewSendHealth(Double weight) {
		if(unwrite != weight) {
			unwrite = weight;
			return true;
		} else {
			return false;
		}
	}
	@Deprecated
	protected boolean renewSlowHealth() {
		slow = slow * 0.996;
		slowdownTime = new Date();
		if (slow < 0.6) {
			return true;
		} else {
			return false;
		}
	}
    
    public AbstractPoint getPoint() {
		return point;
	}
    
    public abstract SocketAddress getLocalAddress();
    
    @Override
    public String toString(){
    	return "Node[" + getLocalAddress().toString() + "->" + getSocketAddress().toString() + "]是否有效:"+this.isAlive()+",是否临时中断:"+this.isSuspended();
    }

    @Override
	public int hashCode() {
		return (getLocalAddress().toString() + "->" + getSocketAddress().toString()).hashCode();
	}

}
