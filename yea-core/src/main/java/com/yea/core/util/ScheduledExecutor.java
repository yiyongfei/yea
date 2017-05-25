package com.yea.core.util;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ScheduledExecutor {

	private static class LazyHolder {
		private static Thread _ShutdownThread;
		static ScheduledThreadPoolExecutor _ScheduledExecutor = null;
		static {
			_ScheduledExecutor = new ScheduledThreadPoolExecutor(2, new YeaThreadFactory("ScheduledExecutor"));
			_ShutdownThread = new Thread(new Runnable() {
				public void run() {
					shutdownExecutorPool();
				}
			});
			Runtime.getRuntime().addShutdownHook(_ShutdownThread);
		}

		private static void shutdownExecutorPool() {
			if (_ScheduledExecutor != null) {
				_ScheduledExecutor.shutdown();
				if (_ShutdownThread != null) {
					try {
						Runtime.getRuntime().removeShutdownHook(_ShutdownThread);
					} catch (IllegalStateException ise) {
					}
				}
			}
		}
	}

	public static ScheduledThreadPoolExecutor getScheduledExecutor() {
		return LazyHolder._ScheduledExecutor;
	}
	
    public static ThreadFactory getThreadFactory(String name) {
    	return new YeaThreadFactory(name);
    }
    
}

class YeaThreadFactory implements ThreadFactory {
	private static final AtomicInteger poolNumber = new AtomicInteger(1);
	private final ThreadGroup group;
	private final AtomicInteger threadNumber = new AtomicInteger(1);
	private final String namePrefix;

	YeaThreadFactory(String threadName) {
		SecurityManager s = System.getSecurityManager();
		group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
		namePrefix = "YEAPool-" + poolNumber.getAndIncrement() + "-" + threadName + "-";
	}

	public Thread newThread(Runnable r) {
		Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
		if (t.isDaemon())
			t.setDaemon(false);
		if (t.getPriority() != Thread.NORM_PRIORITY)
			t.setPriority(Thread.NORM_PRIORITY);
		return t;
	}
}