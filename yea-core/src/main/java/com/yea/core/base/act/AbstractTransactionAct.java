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
package com.yea.core.base.act;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import com.yea.core.base.act.exception.ActException;

/**
 * Act类，事务控制，对于增删改的操作，Act将继承该类
 * 在Act层，子类只要继承该类，该子类的事务就交由Spring管理
 * 
 * @author yiyongfei
 *
 */
public abstract class AbstractTransactionAct<T> extends AbstractAct<T> {

	protected static final long serialVersionUID = 1L;

	@Override
	protected T compute() {
		if (txManager == null) {
			return _compute();
		} else {
			TransactionTemplate transactionTemplate = new TransactionTemplate(txManager);
			transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
			transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
			return transactionTemplate.execute(new TransactionCallback<T>() {
				@Override
				public T doInTransaction(TransactionStatus status) {
					try {
						return _compute();
					} catch (ActException e) {
						status.setRollbackOnly();
						throw e;
					}
				}
			});
		}
	}
}
