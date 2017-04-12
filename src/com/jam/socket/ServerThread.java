package com.jam.socket;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Date;

import java.text.SimpleDateFormat;
import java.net.Socket;
import java.net.SocketTimeoutException;

import org.json.*; 

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

/*
*   服务器线程的作用主要是:
*   1.接收来自客户端的信息
*   2.将接收到的信息解析，并转发给目标客户端
* */
public class ServerThread extends Thread {

    private User user;
    private static List<User> list;
    private String msg;
    private int port;
    private String apiUrl = "http://192.168.0.105:3000";
    private String[] cmdArray = {"card_open", "pwd_open", "finger_open", "low_power", 
    		"tamper", "door_bell", "finger_add", "finger_del", "pwd_add", "pwd_del",
            "card_add", "card_del", "illegal_key", "illegal_try", "lctch_bolt", "dead_bolt"};
    
    private Map<String, String> gateways = new HashMap<>();
    private Map<String, User> upThreads = new ConcurrentHashMap<>();
    private Map<String, User> downThreads = new ConcurrentHashMap<>();

    public ServerThread(User user, List<User> list, int port, Map<String, String> gateways,
    		Map<String, User> upThreads, Map<String, User> downThreads) {
        this.user = user;
        this.list = list;
        this.port = port;
        this.gateways = gateways;
        this.upThreads = upThreads;
        this.downThreads = downThreads;
    }

	public void run() {
        try {
        	Socket mSocket = user.getSocket();
        	if ( mSocket == null || mSocket.isClosed() || user.getBr() == null ) {
        		return;
        	}
            while ( (msg = user.getBr().readLine()) != null ) {
                // 信息的格式：{req:'up', mac:'mac', device_type:'gateway', 
                // device_id:'1234', cmd:'heartbeat', status: 1,
                // data: '12345678'}
                // 不断地读取客户端发过来的信息
                msg = msg.replaceAll("\"", "\'");
                parseData(msg);
                if ( msg.length() == 0 ) {
                	return;
                }
                
                msg = StringFilter(msg).replaceAll("[(.)+]\\{", "\\{").trim();
                
                String client_ip = user.getAddr().getHostAddress();
                JSONObject res = new JSONObject(msg);
                String req = (String) res.get("req");
                String mac = (String) res.get("mac");
                String cmd = (String) res.get("cmd");
                String device_type = (String) res.get("device_type");
                String device_id = (String) res.get("device_id");
                String mobile_mac = (String) res.get("mobile_mac");
                String status = (String) res.get("status");
                String data = (String) res.get("data");
                
                System.out.println("data is :" +  data);
                
                String send_msg = "";
                String default_mobile_mac = "11-22-33-44-55-66";
                
                String mac_and_mobile_mac = String.valueOf(mac + "-" + mobile_mac);
                System.out.println("mac_and_mobile_mac is:" +  mac_and_mobile_mac);
                System.out.println("receive from client ip:" + client_ip
                		+ ", mobile_mac:" + mobile_mac + ", msg:" + msg);
                
                if ( user.getDeviceMac() == null || user.getMobileMac() == null || user.getMobileMac() != default_mobile_mac ) {
                	System.out.println("set mac: " + mac + ", req: " +  req + ", ip:" + client_ip + ", mobile_mac: "  + mobile_mac);
                	setDeviceMacAndReq(user, mac, req, client_ip, mobile_mac);
        		}
                
                System.out.println("ip:" + user.getDeviceIp());
                System.out.println("mac:" + user.getDeviceMac());
                System.out.println("req:" + user.getDeviceReq());
                
                if ( req.equals("down") ) {
                	System.out.println("[down]mac_and_mobile_mac is :" +  mac_and_mobile_mac);
                    downThreads.put(mac_and_mobile_mac, user);
                	send_msg = msg.replaceAll("\'", "\"");
                	
                	System.out.println("是否有对应的上报线程:" + String.valueOf(upThreads.containsKey(mac)));
                	if ( upThreads.containsKey(mac) ) {
                		if ( cmd.equals("hearbeat") ) {
                			send_msg = msg.replaceAll("'req':'down'", "'req':'up'");
                			sendStringToUser(downThreads.get(mac_and_mobile_mac), send_msg);
                		} else {
                	 		if ( cmd.equals("sync_time") ) {
                	 			res.put("data", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                	 			send_msg = res.toString();
                	 		}
                	 		System.out.println("[cmd]send msg to gateway:" + send_msg);
                	 		sendStringToUser(upThreads.get(mac), send_msg);
                		}
                	} else {
                		System.out.println("gateway not online, mac is:" + mac);
                		send_msg = msg.replaceAll("'req':'down'", "'req':'up'").replaceAll("'status':'1'", "'status':'2'");
                		sendStringToUser(downThreads.get(mac_and_mobile_mac), send_msg);
                	}
                } else {
                	upThreads.put(mac, user);
                	System.out.println("是否有对应的下发线程:" + String.valueOf(downThreads.containsKey(mac_and_mobile_mac)));
                	System.out.println("是否有设置过对应的网关端口:" + String.valueOf(gateways.containsKey(mac)));
                	
                	if ( cmd.equals("hearbeat") || cmd.length() == 0 ) {
                		System.out.println("[heartbeat]send reply to gateway, mac is: " + mac);
                		send_msg = msg.replaceAll("\'", "\"");
                		sendStringToUser(upThreads.get(mac), send_msg);
                		
                		if ( !gateways.containsKey(mac) ) {
                    		gateways.put(mac, String.valueOf(port));

                    		String url = apiUrl + "/api/v1/devices/port/update";
                			CloseableHttpClient httpClient = HttpClients.createDefault();
                			HttpPost postPort = new HttpPost(url);
                			List<NameValuePair> params = new ArrayList<>();
            	            params.add(new BasicNameValuePair("device_mac", mac));
            	            params.add(new BasicNameValuePair("gateway_port", String.valueOf(port)));
            	            params.add(new BasicNameValuePair("gateway_version", data));
            	            
            	            try {
                	            UrlEncodedFormEntity e = new UrlEncodedFormEntity(params, "UTF-8");
                	            postPort.setEntity(e);
                	            CloseableHttpResponse response = httpClient.execute(postPort);
                	            try {
                	                System.out.println("port_update返回status:" + response.getStatusLine());
                	            } finally {
                	                response.close();
                	            }
                			} finally {
                				httpClient.close();
                	        }
                    	}
                	} else {
                		if ( downThreads.containsKey(mac_and_mobile_mac) ) {
                			sendStringToUser(downThreads.get(mac_and_mobile_mac), msg);
                		}
                		if ( status.equals("1") && Arrays.asList(cmdArray).contains(cmd) ) {
                			String url = apiUrl + "/api/v1/devices/listen";
                			CloseableHttpClient httpclient = HttpClients.createDefault();
                			HttpPost post = new HttpPost(url);
                			List<NameValuePair> params = new ArrayList<>();
            	            params.add(new BasicNameValuePair("device_mac", mac));
            	            params.add(new BasicNameValuePair("device_token", device_id));
            	            params.add(new BasicNameValuePair("device_cmd", cmd));
            	            
            	            String device_num = ""; // TODO get device ID
                			if ( data.trim().length() > 0 ) {
                				if ( cmd.contains("finger") ) {
                					params.add(new BasicNameValuePair("device_num", device_num));
                					params.add(new BasicNameValuePair("lock_type", "1"));
                				} else if ( cmd.contains("pwd") ) {
                					params.add(new BasicNameValuePair("device_num", device_num));
                					params.add(new BasicNameValuePair("lock_type", "2"));
                				} else if ( cmd.contains("card") ) {
                					params.add(new BasicNameValuePair("device_num", device_num));
                					params.add(new BasicNameValuePair("lock_type", "3"));
                				}
                			}
                			
                			try {
                	            UrlEncodedFormEntity e = new UrlEncodedFormEntity(params, "UTF-8");
                	            post.setEntity(e);

                	            CloseableHttpResponse response = httpclient.execute(post);
                	            try {
                	                System.out.println("devices_listen返回status:" + response.getStatusLine());
                	            } finally {
                	                response.close();
                	            }
                			} finally {
                	            httpclient.close();
                	        }
                	    }
                	}
                }
                sleep(50);
            }
        } catch (JSONException e) {
        	remove(user);
        	e.printStackTrace();
        	System.out.println("无效的数据格式");
        } catch (SocketTimeoutException e) {
        	remove(user);
        	e.printStackTrace();
        	System.out.println("socket超时");
        } catch (Exception e) {
        	remove(user);
        	e.printStackTrace();
            System.out.println("socket异常");
        }
    }
    
    private void sendStringToUser(User user, String msg) {
        System.out.println("[sendString]send msg to gateway:" + msg);
        try {
            PrintWriter pw = user.getPw();
            pw.println(msg);
            //pw.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void remove(User user2) {
	    try {
	    	if ( user2.getDeviceReq().equals("up") ) {
	    		if ( upThreads.containsKey( user2.getDeviceMac() ) ) {
	    			try {
	    				upThreads.remove( user2.getDeviceMac() );
	    			} catch (Exception e) {
	    				e.printStackTrace();
	    			}
	    		}
	    	}
	    	user2.getBr().close();
	    	user2.getPw().close();
			user2.getSocket().close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			list.remove(user2);
		}
    }

    private void setDeviceMacAndReq(User user2, String mac, String req, String ip, String mobile_mac) {
    	if ( list.size() == 1 ) {
    		user2.setDeviceMac(mac);
		    user2.setDeviceReq(req);
		    user2.setDeviceIp(ip);
		    user2.setMobileMac(mobile_mac);
    	} else {
    		if ( req.equals("up") ) {
    			if ( !upThreads.containsKey(mac) ) {
    				user2.setDeviceMac(mac);
    			    user2.setDeviceReq(req);
    			    user2.setDeviceIp(ip);
    			    user2.setMobileMac(mobile_mac);
    			} else {
    				user2.setDeviceIp(ip);
    			    user2.setMobileMac(mobile_mac);
    			}
    		} else {
    			user2.setDeviceMac(mac);
			    user2.setDeviceReq(req);
			    user2.setDeviceIp(ip);
			    user2.setMobileMac(mobile_mac);
    		}
    		System.out.println("total users is " + String.valueOf(list.size()));
    	}
    }
    
    private static String StringFilter(String str) {
    	String regEx = "[^a-zA-Z0-9-:.',{}_|]";
    	Pattern p = Pattern.compile(regEx);     
        Matcher m = p.matcher(str);     
        return m.replaceAll("").trim();  
    }
    
    private void parseData(String msg) throws DecoderException, UnsupportedEncodingException {
    	String[] temp = msg.split("data");
        String regEx = "[a-zA-Z0-9-:.',{}_|]";
    	Pattern p = Pattern.compile(regEx);     
        Matcher m = p.matcher(temp[1]);
        String data = m.replaceAll("").trim();
        if ( data.length() > 0 ) {
        	System.out.println(data.getBytes("UTF-8") );
        }
    }
}