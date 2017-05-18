package com.yea.loadbalancer.util;

import java.util.Collection;

public class LoadBalanceUtils {
	public static boolean isPresentAsCause(Throwable throwableToSearchIn,
			Collection<Class<? extends Throwable>> throwableToSearchFor) {
		int infiniteLoopPreventionCounter = 10;
		while (throwableToSearchIn != null && infiniteLoopPreventionCounter > 0) {
			infiniteLoopPreventionCounter--;
			for (Class<? extends Throwable> c : throwableToSearchFor) {
				if (c.isAssignableFrom(throwableToSearchIn.getClass())) {
					return true;
				}
			}
			throwableToSearchIn = throwableToSearchIn.getCause();
		}
		return false;
	}
}
