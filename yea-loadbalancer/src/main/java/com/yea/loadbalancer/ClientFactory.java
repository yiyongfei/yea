package com.yea.loadbalancer;

import com.yea.core.loadbalancer.ILoadBalancer;
import com.yea.loadbalancer.config.CommonClientConfigKey;
import com.yea.loadbalancer.config.DefaultClientConfigImpl;
import com.yea.loadbalancer.config.IClientConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A factory that creates client, load balancer and client configuration instances from properties. It also keeps mappings of client names to 
 * the  created instances.
 * 
 * @author awang
 *
 */
public class ClientFactory {
    
    private static Map<String, ILoadBalancer> namedLBMap = new ConcurrentHashMap<String, ILoadBalancer>();
    private static ConcurrentHashMap<String, IClientConfig> namedConfig = new ConcurrentHashMap<String, IClientConfig>();
    
    private static Logger logger = LoggerFactory.getLogger(ClientFactory.class);
    
    /**
     * Get the load balancer associated with the name, or create one with an instance {@link DefaultClientConfigImpl} if does not exist
     * 
     * @throws RuntimeException if any error occurs
     */
    public static synchronized ILoadBalancer getNamedLoadBalancer(String name) {
    	return getNamedLoadBalancer(name, DefaultClientConfigImpl.class);
    }
    
    /**
     * Get the load balancer associated with the name, or create one with an instance of configClass if does not exist
     * 
     * @throws RuntimeException if any error occurs
     * @see #registerNamedLoadBalancerFromProperties(String, Class)
     */
    public static synchronized ILoadBalancer getNamedLoadBalancer(String name, Class<? extends IClientConfig> configClass) {
        ILoadBalancer lb = namedLBMap.get(name);
        if (lb != null) {
            return lb;
        } else {
            try {
                lb = registerNamedLoadBalancerFromProperties(name, configClass);
            } catch (ClientException e) {
                throw new RuntimeException("Unable to create load balancer", e);
            }
            return lb;
        }
    }

    /**
     * Create and register a load balancer with the name and given the class of configClass.
     *
     * @throws ClientException if load balancer with the same name already exists or any error occurs
     * @see #instantiateInstanceWithClientConfig(String, IClientConfig)
     */
    public static ILoadBalancer registerNamedLoadBalancerFromclientConfig(String name, IClientConfig clientConfig) throws ClientException {
        if (namedLBMap.get(name) != null) {
            throw new ClientException("LoadBalancer for name " + name + " already exists");
        }
    	ILoadBalancer lb = null;
        try {
            String loadBalancerClassName = (String) clientConfig.getProperty(CommonClientConfigKey.NFLoadBalancerClassName);
            lb = (ILoadBalancer) ClientFactory.instantiateInstanceWithClientConfig(loadBalancerClassName, clientConfig);                                    
            namedLBMap.put(name, lb);            
            logger.info("Client: {} instantiated a LoadBalancer: {}", name, lb);
            return lb;
        } catch (Throwable e) {           
           throw new ClientException("Unable to instantiate/associate LoadBalancer with Client:" + name, e);
        }    	
    }
    
    /**
     * Create and register a load balancer with the name and given the class of configClass.
     *
     * @throws ClientException if load balancer with the same name already exists or any error occurs
     * @see #instantiateInstanceWithClientConfig(String, IClientConfig)
     */
    public static synchronized ILoadBalancer registerNamedLoadBalancerFromProperties(String name, Class<? extends IClientConfig> configClass) throws ClientException {
        if (namedLBMap.get(name) != null) {
            throw new ClientException("LoadBalancer for name " + name + " already exists");
        }
        IClientConfig clientConfig = getNamedConfig(name, configClass);
        return registerNamedLoadBalancerFromclientConfig(name, clientConfig);
    }    

    /**
     * Creates instance related to client framework using reflection. It first checks if the object is an instance of 
     * {@link IClientConfigAware} and if so invoke {@link IClientConfigAware#initWithNiwsConfig(IClientConfig)}. If that does not
     * apply, it tries to find if there is a constructor with {@link IClientConfig} as a parameter and if so invoke that constructor. If neither applies,
     * it simply invokes the no-arg constructor and ignores the clientConfig parameter. 
     *  
     * @param className Class name of the object
     * @param clientConfig IClientConfig object used for initialization.
     */
    @SuppressWarnings("unchecked")
	public static Object instantiateInstanceWithClientConfig(String className, IClientConfig clientConfig) 
    		throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    	Class clazz = Class.forName(className);
    	if (IClientConfigAware.class.isAssignableFrom(clazz)) {
    		IClientConfigAware obj = (IClientConfigAware) clazz.newInstance();
    		obj.initWithNiwsConfig(clientConfig);
    		return obj;
    	} else {
    		try {
    		    if (clazz.getConstructor(IClientConfig.class) != null) {
    		    	return clazz.getConstructor(IClientConfig.class).newInstance(clientConfig);
    		    }
    		} catch (NoSuchMethodException ignored) {
    		    // OK for a class to not take an IClientConfig
    		} catch (SecurityException | IllegalArgumentException | InvocationTargetException e) { 
    		    logger.warn("Error getting/invoking IClientConfig constructor of {}", className, e);
    		}    		
    	}
    	logger.warn("Class " + className + " neither implements IClientConfigAware nor provides a constructor with IClientConfig as the parameter. Only default constructor will be used.");
    	return clazz.newInstance();
    }
    
    /**
     * Get the client configuration given the name or create one with {@link DefaultClientConfigImpl} if it does not exist.
     * 
     * @see #getNamedConfig(String, Class)
     */
    public static IClientConfig getNamedConfig(String name) {
        return 	getNamedConfig(name, DefaultClientConfigImpl.class);
    }
    
    /**
     * Get the client configuration given the name or create one with clientConfigClass if it does not exist. An instance of IClientConfig
     * is created and {@link IClientConfig#loadProperties(String)} will be called.
     */
    public static IClientConfig getNamedConfig(String name, Class<? extends IClientConfig> clientConfigClass) {
        IClientConfig config = namedConfig.get(name);
        if (config != null) {
            return config;
        } else {
            try {
                config = (IClientConfig) clientConfigClass.newInstance();
                config.loadProperties(name);
            } catch (InstantiationException | IllegalAccessException e) {
                return null;
            }
            config.loadProperties(name);
            IClientConfig old = namedConfig.putIfAbsent(name, config);
            if (old != null) {
                config = old;
            }
            return config;
        }
    }
}
