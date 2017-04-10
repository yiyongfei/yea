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

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * @author yiyongfei
 * 
 */
public class NodeROImpl implements BalancingNode {

    private final BalancingNode root;

    public NodeROImpl(BalancingNode n) {
        super();
        root = n;
    }

    @Override
    public String toString() {
        return root.toString();
    }

    public SocketAddress getSocketAddress() {
        return root.getSocketAddress();
    }
}
