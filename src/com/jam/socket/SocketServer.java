package com.jam.socket;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import com.jam.socket.User;

//服务器类
public class SocketServer {

    public static void main(String[] args) throws Exception {

        // 实例化一个list,用于保存所有的User
        List<User> list = new ArrayList<User>();
        // 创建绑定到特定端口的服务器套接字
        @SuppressWarnings("resource")
        ServerSocket serverSocket = new ServerSocket(6001);
        System.out.println("服务端正在启动中。。。");
        // 循环监听客户端连接
        while (true) {
            Socket socket = serverSocket.accept();
            socket.setSoTimeout(10 * 60 * 1000);
            // 每接受一个线程，就随机生成一个一个新用户
            User user = new User("user" + Math.round(Math.random() * 100),socket);

            System.out.println(user.getToken() + ", ip:" + user.getAddr().getHostAddress() + "正在登录。。。");
            list.add(user);
            // 创建一个新的线程，接收信息并转发
            ServerThread thread = new ServerThread(user, list);
            thread.start();
        }
    }
}