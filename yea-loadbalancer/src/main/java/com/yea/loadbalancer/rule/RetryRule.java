package com.yea.loadbalancer.rule;

import java.util.Timer;
import java.util.TimerTask;

import com.yea.core.loadbalancer.BalancingNode;
import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.core.loadbalancer.IRule;
import com.yea.loadbalancer.AbstractLoadBalancerRule;
import com.yea.loadbalancer.config.IClientConfig;

public class RetryRule extends AbstractLoadBalancerRule {
	IRule subRule = new RoundRobinRule();
	long maxRetryMillis = 500;

	public RetryRule() {
	}

	public RetryRule(IRule subRule) {
		this.subRule = (subRule != null) ? subRule : new RoundRobinRule();
	}

	public RetryRule(IRule subRule, long maxRetryMillis) {
		this.subRule = (subRule != null) ? subRule : new RoundRobinRule();
		this.maxRetryMillis = (maxRetryMillis > 0) ? maxRetryMillis : 500;
	}

	public void setRule(IRule subRule) {
		this.subRule = (subRule != null) ? subRule : new RoundRobinRule();
	}

	public IRule getRule() {
		return subRule;
	}

	public void setMaxRetryMillis(long maxRetryMillis) {
		if (maxRetryMillis > 0) {
			this.maxRetryMillis = maxRetryMillis;
		} else {
			this.maxRetryMillis = 500;
		}
	}

	public long getMaxRetryMillis() {
		return maxRetryMillis;
	}

	
	
	@Override
	public void setLoadBalancer(ILoadBalancer lb) {		
		super.setLoadBalancer(lb);
		subRule.setLoadBalancer(lb);
	}

	/*
	 * Loop if necessary. Note that the time CAN be exceeded depending on the
	 * subRule, because we're not spawning additional threads and returning
	 * early.
	 */
	public BalancingNode choose(ILoadBalancer lb, Object key) {
		long requestTime = System.currentTimeMillis();
		long deadline = requestTime + maxRetryMillis;

		BalancingNode answer = null;

		answer = subRule.choose(key);

		if (((answer == null) || (!answer.isAlive()) || (answer.isSuspended()))
				&& (System.currentTimeMillis() < deadline)) {

			InterruptTask task = new InterruptTask(deadline
					- System.currentTimeMillis());

			while (!Thread.interrupted()) {
				answer = subRule.choose(key);

				if (((answer == null) || (!answer.isAlive()) || (answer.isSuspended()))
						&& (System.currentTimeMillis() < deadline)) {
					/* pause and retry hoping it's transient */
					Thread.yield();
				} else {
					break;
				}
			}

			task.cancel();
		}

		if ((answer == null) || (!answer.isAlive()) || (answer.isSuspended())) {
			return null;
		} else {
			return answer;
		}
	}

	@Override
	public BalancingNode choose(Object key) {
		return choose(getLoadBalancer(), key);
	}

	@Override
	public void initWithNiwsConfig(IClientConfig clientConfig) {
	}
}

class InterruptTask extends TimerTask {
	
	static Timer timer = new Timer("InterruptTimer", true); 
	
	protected Thread target = null;

	public InterruptTask(long millis) {
			target = Thread.currentThread();
			timer.schedule(this, millis);
	}


	/* Auto-scheduling constructor */
	public InterruptTask(Thread target, long millis) {
			this.target = target;
			timer.schedule(this, millis);
	}


	public boolean cancel() {
			try {
					/* This shouldn't throw exceptions, but... */
					return super.cancel();
			} catch (Exception e) {
					return false;
			}
	}

	public void run() {
			if ((target != null) && (target.isAlive())) {
					target.interrupt();
			}
	}
} 