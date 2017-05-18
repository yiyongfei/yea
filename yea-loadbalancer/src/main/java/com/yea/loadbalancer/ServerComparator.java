package com.yea.loadbalancer;

import java.io.Serializable;
import java.util.Comparator;

import com.yea.core.loadbalancer.BalancingNode;

/**
 * Class to help establishing equality for Hash/Key operations.
 * 
 * @author stonse
 * 
 */
public class ServerComparator implements Comparator<BalancingNode>, Serializable {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int compare(BalancingNode s1, BalancingNode s2) {
        return s1.getId().compareTo(s2.getId());
    }
}
