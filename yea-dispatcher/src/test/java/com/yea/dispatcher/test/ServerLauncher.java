package com.yea.dispatcher.test;


import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import com.yea.remote.netty.server.NettyServer;

@SuppressWarnings("resource")
public class ServerLauncher {
	private static NettyServer nettyServer;
	static{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:application-bean.xml");
		context.registerShutdownHook();
		nettyServer = (NettyServer) context.getBean("nettyServer");
	}
	
	public static void main(String[] args) throws Throwable {
		if(!StringUtils.isEmpty(System.getProperty("server.port"))){
			int port = Integer.valueOf(System.getProperty("server.port"));
			nettyServer.setPort(port);
		}
		nettyServer.bind();
	}
}
