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
package com.yea.core.balancing.hash;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * @author yiyongfei
 * 
 */
public final class KetamaNodeLocator implements NodeLocator {
	private ReadWriteLock lock = new ReentrantReadWriteLock();
    private volatile TreeMap<Long, BalancingNode> ketamaNodes;
    private volatile Collection<BalancingNode> allNodes;

    private final HashAlgorithm hashAlg;
    private final KetamaNodeLocatorConfiguration config;

    /**
     * Create a new KetamaNodeLocator using specified nodes and the specifed hash
     * algorithm.
     *
     * @param nodes The List of nodes to use in the Ketama consistent hash
     *          continuum
     * @param alg The hash algorithm to use when choosing a node in the Ketama
     *          consistent hash continuum
     */
    public KetamaNodeLocator() {
        this(DefaultHashAlgorithm.KETAMA_HASH);
    }

    public KetamaNodeLocator(HashAlgorithm alg) {
        this(alg, new DefaultKetamaNodeLocatorConfiguration());
    }
    
    /**
     * Create a new KetamaNodeLocator using specified nodes and the specifed hash
     * algorithm and configuration.
     *
     * @param nodes The List of nodes to use in the Ketama consistent hash
     *          continuum
     * @param alg The hash algorithm to use when choosing a node in the Ketama
     *          consistent hash continuum
     * @param conf
     */
    public KetamaNodeLocator(HashAlgorithm alg, KetamaNodeLocatorConfiguration conf) {
        super();
        allNodes = new CopyOnWriteArrayList<BalancingNode>();
        hashAlg = alg;
        config = conf;
    }

    private KetamaNodeLocator(TreeMap<Long, BalancingNode> smn, Collection<BalancingNode> an, HashAlgorithm alg, KetamaNodeLocatorConfiguration conf) {
        super();
        ketamaNodes = smn;
        allNodes = an;
        hashAlg = alg;
        config = conf;
    }

    public Collection<BalancingNode> getAll() {
        return allNodes;
    }

    public BalancingNode getPrimary(final String k) {
        BalancingNode rv = getNodeForKey(hashAlg.hash(k));
        assert rv != null : "Found no node for key " + k;
        return rv;
    }

    long getMaxKey() {
        return getKetamaNodes().lastKey();
    }

    BalancingNode getNodeForKey(long hash) {
        final BalancingNode rv;
        if (!ketamaNodes.containsKey(hash)) {
            // Java 1.6 adds a ceilingKey method, but I'm still stuck in 1.5
            // in a lot of places, so I'm doing this myself.
            SortedMap<Long, BalancingNode> tailMap = getKetamaNodes().tailMap(hash);
            if (tailMap.isEmpty()) {
                hash = getKetamaNodes().firstKey();
            } else {
                hash = tailMap.firstKey();
            }
        }
        rv = getKetamaNodes().get(hash);
        return rv;
    }
    
    

    public Iterator<BalancingNode> getSequence(String k) {
        // Seven searches gives us a 1 in 2^7 chance of hitting the
        // same dead node all of the time.
        return new KetamaIterator(k, 7, getKetamaNodes(), hashAlg);
    }

    public NodeLocator getReadonlyCopy() {
        TreeMap<Long, BalancingNode> smn = new TreeMap<Long, BalancingNode>(getKetamaNodes());
        Collection<BalancingNode> an = new ArrayList<BalancingNode>(allNodes.size());

        // Rewrite the values a copy of the map.
        for (Map.Entry<Long, BalancingNode> me : smn.entrySet()) {
            smn.put(me.getKey(), new NodeROImpl(me.getValue()));
        }

        // Copy the allNodes collection.
        for (BalancingNode n : allNodes) {
            an.add(new NodeROImpl(n));
        }

        return new KetamaNodeLocator(smn, an, hashAlg, config);
    }

    public void addLocator(BalancingNode node) {
    	lock.writeLock().lock();
    	try {
    		allNodes.add(node);
            setKetamaNodes(allNodes);
    	} finally {
    		lock.writeLock().unlock();
    	}
    	
    }
    public void removeLocator(BalancingNode node) {
    	lock.writeLock().lock();
    	try {
    		allNodes.remove(node);
            setKetamaNodes(allNodes);
    	} finally {
    		lock.writeLock().unlock();
    	}
    	
    }
    public boolean containsLocator(SocketAddress address) {
    	Iterator<BalancingNode> it = allNodes.iterator();
		while (it.hasNext()) {
			BalancingNode node = it.next();
			if(node.getSocketAddress().equals(address)){
    			return true;
    		}
		}
        return false;
    }
    public Collection<BalancingNode> getLocator(SocketAddress address) {
    	Collection<BalancingNode> tmp = new ArrayList<BalancingNode>();
    	Iterator<BalancingNode> it = allNodes.iterator();
		while (it.hasNext()) {
			BalancingNode node = it.next();
			if(node.getSocketAddress().equals(address)){
    			tmp.add(node);
    		}
		}
    	return tmp;
    }

    /**
     * @return the ketamaNodes
     */
    protected TreeMap<Long, BalancingNode> getKetamaNodes() {
        return ketamaNodes;
    }

    /**
     * Setup the KetamaNodeLocator with the list of nodes it should use.
     *
     * @param nodes a List of MemcachedNodes for this KetamaNodeLocator to use in
     *          its continuum
     */
    protected void setKetamaNodes(Collection<BalancingNode> nodes) {
        TreeMap<Long, BalancingNode> newNodeMap = new TreeMap<Long, BalancingNode>();
        int numReps = config.getNodeRepetitions();
        for (BalancingNode node : nodes) {
            // Ketama does some special work with md5 where it reuses chunks.
            if (hashAlg == DefaultHashAlgorithm.KETAMA_HASH) {
                for (int i = 0; i < numReps / 4; i++) {
                    byte[] digest = DefaultHashAlgorithm.computeMd5(config.getKeyForNode(node, i));
                    for (int h = 0; h < 4; h++) {
                        Long k = ((long) (digest[3 + h * 4] & 0xFF) << 24) | ((long) (digest[2 + h * 4] & 0xFF) << 16) | ((long) (digest[1 + h * 4] & 0xFF) << 8) | (digest[h * 4] & 0xFF);
                        newNodeMap.put(k, node);
                    }
                }
            } else {
                for (int i = 0; i < numReps; i++) {
                    newNodeMap.put(hashAlg.hash(config.getKeyForNode(node, i)), node);
                }
            }
        }
        assert newNodeMap.size() == numReps * nodes.size();
        ketamaNodes = newNodeMap;
    }
}
