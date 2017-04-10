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

import java.util.HashMap;
import java.util.Map;

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * @author yiyongfei
 * 
 */
public class DefaultKetamaNodeLocatorConfiguration implements KetamaNodeLocatorConfiguration {

    private final int numReps = 160;

    protected Map<BalancingNode, String> socketAddresses = new HashMap<BalancingNode, String>();

    /**
    * Returns the socket address of a given MemcachedNode.
    *
    * @param node The node which we're interested in
    * @return String the socket address of that node.
    */
    protected String getSocketAddressForNode(BalancingNode node) {
        String result = socketAddresses.get(node);
        if (result == null) {
            result = String.valueOf(node.getSocketAddress());
            if (result.startsWith("/")) {
                result = result.substring(1);
            }
            socketAddresses.put(node, result);
        }
        return result;
    }

    /**
    * Returns the number of discrete hashes that should be defined for each node
    * in the continuum.
    *
    * @return NUM_REPS repetitions.
    */
    public int getNodeRepetitions() {
        return numReps;
    }

    /**
    * Returns a uniquely identifying key, suitable for hashing by the
    * KetamaNodeLocator algorithm.
    *
    * <p>
    * This default implementation uses the socket-address of the MemcachedNode
    * and concatenates it with a hyphen directly against the repetition number
    * for example a key for a particular server's first repetition may look like:
    * <p>
    *
    * <p>
    * <code>myhost/10.0.2.1-0</code>
    * </p>
    *
    * <p>
    * for the second repetition
    * </p>
    *
    * <p>
    * <code>myhost/10.0.2.1-1</code>
    * </p>
    *
    * <p>
    * for a server where reverse lookups are failing the returned keys may look
    * like
    * </p>
    *
    * <p>
    * <code>/10.0.2.1-0</code> and <code>/10.0.2.1-1</code>
    * </p>
    *
    * @param node The MemcachedNode to use to form the unique identifier
    * @param repetition The repetition number for the particular node in question
    *          (0 is the first repetition)
    * @return The key that represents the specific repetition of the node
    */
    public String getKeyForNode(BalancingNode node, int repetition) {
        return getSocketAddressForNode(node) + "-" + repetition;
    }
}
