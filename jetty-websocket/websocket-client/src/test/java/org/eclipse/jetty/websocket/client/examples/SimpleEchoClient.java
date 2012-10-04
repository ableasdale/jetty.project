//
//  ========================================================================
//  Copyright (c) 1995-2012 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.client.examples;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.util.FutureCallback;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.eclipse.jetty.websocket.client.WebSocketClientFactory;
import org.eclipse.jetty.websocket.core.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.core.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.core.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.core.annotations.WebSocket;
import org.eclipse.jetty.websocket.core.api.StatusCode;
import org.eclipse.jetty.websocket.core.api.WebSocketConnection;

/**
 * Example of a simple Echo Client.
 */
public class SimpleEchoClient
{
    @WebSocket
    public static class SimpleEchoSocket
    {
        private final CountDownLatch closeLatch;
        @SuppressWarnings("unused")
        private WebSocketConnection conn;

        public SimpleEchoSocket()
        {
            this.closeLatch = new CountDownLatch(1);
        }

        public boolean awaitClose(int duration, TimeUnit unit) throws InterruptedException
        {
            return this.closeLatch.await(duration,unit);
        }

        @OnWebSocketClose
        public void onClose(int statusCode, String reason)
        {
            System.out.printf("Connection closed: %d - %s%n",statusCode,reason);
            this.conn = null;
            this.closeLatch.countDown(); // trigger latch
        }

        @OnWebSocketConnect
        public void onConnect(WebSocketConnection conn)
        {
            System.out.printf("Got connect: %s%n",conn);
            this.conn = conn;
            try
            {
                FutureCallback<Void> callback = new FutureCallback<>();
                conn.write(null,callback,"Echo Me!");
                callback.get(2,TimeUnit.SECONDS); // wait for send to complete.

                callback = new FutureCallback<>();
                conn.write(null,callback,"Echo Another One, please.");
                callback.get(2,TimeUnit.SECONDS); // wait for send to complete.

                conn.close(StatusCode.NORMAL,"I'm done");
            }
            catch (Throwable t)
            {
                t.printStackTrace();
            }
        }

        @OnWebSocketMessage
        public void onMessage(String msg)
        {
            System.out.printf("Got msg: %s%n",msg);
        }
    }

    public static void main(String[] args)
    {

        WebSocketClientFactory factory = new WebSocketClientFactory();
        SimpleEchoSocket socket = new SimpleEchoSocket();
        try
        {
            factory.start();
            WebSocketClient client = factory.newWebSocketClient(socket);
            URI echoUri = new URI("ws://echo.websocket.org");
            System.out.printf("Connecting to : %s%n",echoUri);
            client.connect(echoUri);

            // wait for closed socket connection.
            socket.awaitClose(5,TimeUnit.SECONDS);
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
        finally
        {
            try
            {
                factory.stop();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
