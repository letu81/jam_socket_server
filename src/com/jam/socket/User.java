package com.jam.socket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

public class User {

    private String token;
    private String device_mac;
    private String device_ip;
    private String device_req;
    private String device_type;
    private String device_id;
    
    private Socket socket;
    private BufferedReader br;
    private PrintWriter pw;
    private InetAddress addr;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getDeviceMac() {
        return device_mac;
    }

    public void setDeviceMac(String device_mac) {
        this.device_mac = device_mac;
    }
    
    public String getDeviceIp() {
        return device_ip;
    }

    public void setDeviceIp(String device_ip) {
        this.device_ip = device_ip;
    }
    
    public String getDeviceReq() {
        return device_req;
    }

    public void setDeviceReq(String device_req) {
        this.device_req = device_req;
    }
    
    public String getDeviceType() {
        return device_type;
    }

    public void setDeviceType(String device_type) {
        this.device_type = device_type;
    }
    
    public String getDeviceId() {
        return device_id;
    }

    public void setDeviceId(String device_id) {
        this.device_id = device_id;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(final Socket socket) {
        this.socket = socket;
    }

    public BufferedReader getBr() {
        return br;
    }

    public void setBr(BufferedReader br) {
        this.br = br;
    }
    
    public PrintWriter getPw() {
        return pw;
    }

    public void setPw(PrintWriter pw) {
        this.pw = pw;
    }
    
    public InetAddress getAddr() {
        return addr;
    }

    public void setAddr(InetAddress addr) {
        this.addr = addr;
    }

    public User(String token, final Socket socket) throws IOException {
        this.token = token;
        this.socket = socket;
        this.addr = socket.getInetAddress();
        this.device_ip = socket.getInetAddress().getHostAddress();
        this.br = new BufferedReader(new InputStreamReader(
                socket.getInputStream()));
        this.pw = new PrintWriter(socket.getOutputStream());
    }
    
	public InetAddress getInetAddress() {
		return addr;
	}

    @Override
    public String toString() {
        return "User [token=" + token + ", Device mac=" + device_mac + ", socket="
                + socket + "]";
    }
}