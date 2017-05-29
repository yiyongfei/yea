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
package com.yea.remote.netty.send;

import com.yea.core.loadbalancer.AbstractBalancingNode;
import com.yea.core.remote.struct.Message;
import com.yea.remote.netty.client.promise.AwaitPromise;

/**
 * 发送不成功的处理
 * @author yiyongfei
 *
 */
public interface IUnavailableSend {

	boolean send(Message nettyMessage, AwaitPromise<?> observer, AbstractBalancingNode node) ;
}
