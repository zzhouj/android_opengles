package com.nfg.sdk;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

public class NFGameServer extends WebSocketServer {

	public static final int DEFAULT_PORT = 8787;

	public NFGameServer() {
		super(new InetSocketAddress(DEFAULT_PORT));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		sendToAll(conn.getResourceDescriptor() + ": joined.");
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		sendToAll(conn.getResourceDescriptor() + ": left.");
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		sendToAll(conn.getResourceDescriptor() + ": " + message);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		ex.printStackTrace();
	}

	public void sendToAll(String text) {
		Collection<WebSocket> con = connections();
		synchronized (con) {
			for (WebSocket c : con) {
				c.send(text);
			}
		}
	}

}
