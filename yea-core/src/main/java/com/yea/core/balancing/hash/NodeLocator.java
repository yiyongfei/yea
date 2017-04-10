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
import java.util.Collection;
import java.util.Iterator;

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * @author yiyongfei
 * 
 */
public interface NodeLocator {

    /**
     * Get the primary location for the given key.
     *
     * @param k the object key
     * @return the QueueAttachment containing the primary storage for a key
     */
    BalancingNode getPrimary(String k);

    /**
     * Get an iterator over the sequence of nodes that make up the backup
     * locations for a given key.
     *
     * @param k the object key
     * @return the sequence of backup nodes.
     */
    Iterator<BalancingNode> getSequence(String k);

    /**
     * Get all memcached nodes. This is useful for broadcasting messages.
     */
    Collection<BalancingNode> getAll();

    /**
     * Create a read-only copy of this NodeLocator.
     */
    NodeLocator getReadonlyCopy();

    void addLocator(BalancingNode node);
    void removeLocator(BalancingNode node);
    public boolean containsLocator(SocketAddress address);
    public Collection<BalancingNode> getLocator(SocketAddress address);
    
}
