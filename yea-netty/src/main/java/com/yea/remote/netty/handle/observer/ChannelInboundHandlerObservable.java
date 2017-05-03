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
package com.yea.remote.netty.handle.observer;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelInboundHandlerAdapter;

import com.yea.core.base.id.UUIDGenerator;
import com.yea.core.cache.ehcache.EhcacheInstance;
import com.yea.core.remote.observer.Observable;
import com.yea.core.remote.observer.Observer;
import com.yea.core.remote.struct.Header;

/**
 * 
 * @author yiyongfei
 */
public class ChannelInboundHandlerObservable extends ChannelInboundHandlerAdapter implements Observable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelInboundHandlerObservable.class);
    @SuppressWarnings("unchecked")
	protected Cache<String, Vector<Observer<?>>> cacheObserver = EhcacheInstance.getCacheInstance(EhcacheInstance.NETTY_CACHE);
    private Map<String, Vector<Observer<?>>> mapObserver;
  
    public ChannelInboundHandlerObservable(){
		mapObserver = new HashMap<String, Vector<Observer<?>>>();
    }
    
    /**
     * Adds an observer to the set of observers for this object, provided
     * that it is not the same as some observer already in the set.
     * The order in which notifications will be delivered to multiple
     * observers is not specified. See the class comment.
     *
     * @param   o   an observer to be added.
     * @throws NullPointerException   if the parameter o is null.
     */
    @SuppressWarnings({ "rawtypes" })
	public synchronized void addObserver(byte[] sessionID, Observer o)  {
        if (o == null)
            throw new NullPointerException();
        String key = toString(sessionID);
        
		if (cacheObserver != null) {
			if (!cacheObserver.containsKey(key)) {
				cacheObserver.put(key, new Vector<Observer<?>>());
			}
			if (!((Vector<Observer<?>>) cacheObserver.get(key)).contains(o)) {
				((Vector<Observer<?>>) cacheObserver.get(key)).addElement(o);
			}
		} else {
			if (!mapObserver.containsKey(key)) {
				mapObserver.put(key, new Vector<Observer<?>>());
			}
			if (!mapObserver.get(key).contains(o)) {
				mapObserver.get(key).addElement(o);
			}
		}
        
    }
    
    

    /**
     * Deletes an observer from the set of observers of this object.
     * Passing <CODE>null</CODE> to this method will have no effect.
     * @param   o   the observer to be deleted.
     */
    @SuppressWarnings({ "rawtypes" })
	public synchronized void deleteObserver(byte[] sessionID, Observer o) {
    	if (cacheObserver != null) {
    		if(cacheObserver.containsKey(toString(sessionID))){
    			((Vector<Observer<?>>)cacheObserver.get(toString(sessionID))).removeElement(o);
    		}
    	} else {
    		mapObserver.get(toString(sessionID)).removeElement(o);
    	}
    }

    /**
     * If this object has changed, as indicated by the
     * <code>hasChanged</code> method, then notify all of its observers
     * and then call the <code>clearChanged</code> method to
     * indicate that this object has no longer changed.
     * <p>
     * Each observer has its <code>update</code> method called with two
     * arguments: this observable object and <code>null</code>. In other
     * words, this method is equivalent to:
     * <blockquote><tt>
     * notifyObservers(null)</tt></blockquote>
     *
     * @see     java.util.Observable#clearChanged()
     * @see     java.util.Observable#hasChanged()
     * @see     java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    public void notifyObservers(byte[] sessionID, Header header) {
        notifyObservers(sessionID, header, null);
    }

    /**
     * If this object has changed, as indicated by the
     * <code>hasChanged</code> method, then notify all of its observers
     * and then call the <code>clearChanged</code> method to indicate
     * that this object has no longer changed.
     * <p>
     * Each observer has its <code>update</code> method called with two
     * arguments: this observable object and the <code>arg</code> argument.
     *
     * @param   arg   any object.
     * @see     java.util.Observable#clearChanged()
     * @see     java.util.Observable#hasChanged()
     * @see     java.util.Observer#update(java.util.Observable, java.lang.Object)
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
	public void notifyObservers(byte[] sessionID, Header header, Object arg) {
        /*
         * a temporary array buffer, used as a snapshot of the state of
         * current Observers.
         */
        Object[] arrLocal = null;

        synchronized (this) {
            /* We don't want the Observer doing callbacks into
             * arbitrary code while holding its own Monitor.
             * The code where we extract each Observable from
             * the Vector and store the state of the Observer
             * needs synchronization, but notifying observers
             * does not (should not).  The worst result of any
             * potential race-condition here is that:
             * 1) a newly-added Observer will miss a
             *   notification in progress
             * 2) a recently unregistered Observer will be
             *   wrongly notified when it doesn't care
             */
            
        	if (cacheObserver != null) {
        		try {
        			arrLocal = ((Vector<Observer<?>>)cacheObserver.get(toString(sessionID))).toArray();
        		} catch (Exception ex) {
        			LOGGER.error("更新["+toString(sessionID)+"]时出现异常", ex);
        			throw ex;
        		}
        	} else {
        		arrLocal = mapObserver.get(toString(sessionID)).toArray();
        	}
        }

		for (int i = arrLocal.length-1; i>=0; i--)
            ((Observer)arrLocal[i]).update(sessionID, header, this, arg);
	}

    /**
     * Clears the observer list so that this object no longer has any observers.
     */
    public synchronized void deleteObservers(byte[] sessionID) {
		if (cacheObserver != null) {
			if(cacheObserver.containsKey(toString(sessionID))){
				((Vector<Observer<?>>)cacheObserver.get(toString(sessionID))).removeAllElements();
			}
		} else {
			mapObserver.get(toString(sessionID)).removeAllElements();
		}
    	
    }

    /**
     * Returns the number of observers of this <tt>Observable</tt> object.
     *
     * @return  the number of observers of this object.
     */
    public synchronized int countObservers(byte[] sessionID) {
		if (cacheObserver != null) {
			if(cacheObserver.containsKey(toString(sessionID))){
	            return ((Vector<Observer<?>>)cacheObserver.get(toString(sessionID))).size();
	        } else {
	            return 0;
	        }
		} else {
			if(mapObserver.containsKey(toString(sessionID))){
	            return mapObserver.get(toString(sessionID)).size();
	        } else {
	            return 0;
	        }
		}
        
    }
    
	private String toString(byte[] ary) {
		return UUIDGenerator.restore(ary).toString();
	}
}
