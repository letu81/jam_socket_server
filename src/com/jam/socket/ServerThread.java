package com.jam.socket;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.ArrayList;
import java.net.SocketTimeoutException;

import org.json.*; 

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair; 

/*
*   服务器线程的作用主要是:
*   1.接收来自客户端的信息
*   2.将接收到的信息解析，并转发给目标客户端
* */
public class ServerThread extends Thread {

    private User user;
    private List<User> list;

    public ServerThread(User user, List<User> list) {
        this.user = user;
        this.list = list;
    }

    public void run() {
        try {
            while (true) {
                // 信息的格式：{req:'up', mac:'mac', device_type:'gateway', 
                // device_id:'1234', cmd:'heartbeat', status: 1,
                // data: '12345678'}
                // 不断地读取客户端发过来的信息
                String msg = user.getBr().readLine();
                System.out.println(msg);
                System.out.println("=====before=====");
                if (msg == null) {
                	return;
                }
                msg = msg.replaceAll("[\\w+]\\{", "\\{").replaceAll("\"", "'").replaceAll(" ", "");
                msg = msg.toString().trim();
                
                System.out.println(msg);

                String client_ip = user.getAddr().getHostAddress();
                JSONObject res = new JSONObject(msg);
                String req = (String) res.get("req");
                String mac = (String) res.get("mac");
                String cmd = (String) res.get("cmd");
                String device_type = (String) res.get("device_type");
                String device_id = (String) res.get("device_id");
                String status = (String) res.get("status");
                String data = (String) res.get("data");

                System.out.println("receive from client ip:" + client_ip + ", msg:" + msg);
                
                if ( user.getDeviceMac() == null ) {
        			setDeviceMacAndReq(user, mac, req, client_ip);
        		}
                
                System.out.println("ip:" + user.getDeviceIp());
                System.out.println("mac:" + user.getDeviceMac());
                System.out.println("req:" + user.getDeviceReq());
                
                if ( req.equals("down") ) {
                	System.out.println("send msg to gateway:" + msg);
                	if ( cmd.equals("hearbeat") ) {
                		sendString(mac, client_ip, "up", msg);
                	} else {
                		sendString(mac, client_ip, "up", msg);
                	}
                	//sleep(5000);
                	//remove(user);
                } else {
                	if ( cmd.equals("hearbeat") ) {
                		System.out.println("cmd hearbeat");
                		if ( user.getDeviceReq().equals("up") && user.getDeviceMac().equals(mac) 
                        		&& user.getDeviceIp().equals(client_ip) ) {
                			sendStringToGateway(user, "server receive msg:" + msg);
                		}
                	} else {
                		if ( user.getDeviceMac().equals(mac) && user.getDeviceReq().equals("up") ) {
                			sendString(mac, client_ip, "down", msg);
                			
                			String url = "http://183.62.232.142:3009/api/v1/devices/listen";
                			CloseableHttpClient httpclient = HttpClients.createDefault();
                			HttpPost post = new HttpPost(url);
                			
                			try {
                				
                				List<NameValuePair> params = new ArrayList<>();
                	            params.add(new BasicNameValuePair("device_mac", mac));
                	            params.add(new BasicNameValuePair("device_token", device_id));
                	            params.add(new BasicNameValuePair("device_cmd", cmd));
                	            UrlEncodedFormEntity e = new UrlEncodedFormEntity(params, "UTF-8");
                	            post.setEntity(e);

                	            CloseableHttpResponse response = httpclient.execute(post);
                	            try {
                	                System.out.println("返回status:" + response.getStatusLine());
                	                
                	            } finally {
                	                response.close();
                	            }
                			} finally {
                	            httpclient.close();
                	        }
                		}
                	}
                }
            }
        } catch (JSONException e) {
        	e.printStackTrace();
        	System.out.println("无效的数据格式");
        } catch (SocketTimeoutException e) {
        	e.printStackTrace();
        	System.out.println("socket超时");
        } catch (Exception e) {
        	e.printStackTrace();
            System.out.println("socket异常");
        } finally {
        	//remove(user);
        	System.out.println("close socket");
        }
    }
    
    private void sendString(String mac, String ip, String req, String msg) {
        for (User user : list) {
        	if ( user.getDeviceReq().equals(req) && user.getDeviceMac().equals(mac)
        			&& user.getDeviceIp().equals(ip) ) {
                try {
                    PrintWriter pw = user.getPw();
                    pw.println(msg);
                    pw.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void sendStringToGateway(User user2, String msg) {
    	try {
            PrintWriter pw = user.getPw();
            pw.println(msg);
            pw.flush();
        } catch (Exception e) {
        	e.printStackTrace();
        }
    }
    
    private void remove(User user2) {
	    try {
	    	user2.getBr().close();
	    	user2.getPw().close();
			user2.getSocket().close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			list.remove(user2);
		}
    }

    private void setDeviceMacAndReq(User user2, String mac, String req, String ip) {
        user2.setDeviceMac(mac);
        user2.setDeviceReq(req);
        user2.setDeviceIp(ip);
    }
}