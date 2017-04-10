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

/**
 * 基于Spy Memcached client的Ketama的Hash算法
 * @author yiyongfei
 * 
 */
public interface KetamaNodeLocatorConfiguration {

    /**
     * Returns a uniquely identifying key, suitable for hashing by the
     * KetamaNodeLocator algorithm.
     *
     * @param node The MemcachedNode to use to form the unique identifier
     * @param repetition The repetition number for the particular node in question
     *          (0 is the first repetition)
     * @return The key that represents the specific repetition of the node
     */
    String getKeyForNode(BalancingNode node, int repetition);

    /**
     * Returns the number of discrete hashes that should be defined for each node
     * in the continuum.
     *
     * @return a value greater than 0
     */
    int getNodeRepetitions();
}
