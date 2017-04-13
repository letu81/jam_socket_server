package com.jam.socket;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.jam.socket.User;

//服务器类
public class SocketServer {

    public static void main(String[] args) throws Exception {

        // 实例化一个list,用于保存所有的User
        List<User> list = new ArrayList<User>();
        Map<String, String> gateways = new HashMap<>();
        Map<String, User> upThreads = new ConcurrentHashMap<>();
        Map<String, User> downThreads = new ConcurrentHashMap<>();
        int port = 6001;
        final int MEGABYTE = (1024*1024);
        
        // 创建绑定到特定端口的服务器套接字
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("服务端正在启动中。。。");
        // 循环监听客户端连接
        while (true) {
            Socket socket = serverSocket.accept();
            socket.setSoTimeout(20 * 1000); //20秒超时
            // 每接受一个线程，就随机生成一个一个新用户
            User user = new User("user " + socket.hashCode(), socket);
            
            System.out.println(user.getToken() + ", ip:" + user.getAddr().getHostAddress() + "正在登录。。。");
            list.add(user);
            // 创建一个新的线程，接收信息并转发
            ServerThread thread = new ServerThread(user, list, port, gateways, upThreads, downThreads);
            try {
            	thread.start();
            	user.setThread(thread);
            } catch (OutOfMemoryError e) {
            	MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            	MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
                long maxMemory = heapUsage.getMax() / MEGABYTE;
                long usedMemory = heapUsage.getUsed() / MEGABYTE;
                //TODO notify admin
                System.out.println(String.valueOf(list.size()) + " : Memory Use :" + usedMemory + "M/" + maxMemory + "M");
            	return;
            }
        }
    }
}