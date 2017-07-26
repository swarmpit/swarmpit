package swarmpit.socket;

import jnr.unixsocket.UnixSocketAddress;
import org.apache.http.HttpHost;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.protocol.HttpContext;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class UnixSocketFactory implements ConnectionSocketFactory {

    private File socketFile;

    private UnixSocketFactory(String file) {
        this.socketFile = new File(file);
    }

    public static UnixSocketFactory createUnixSocketFactory(String socket) {
        return new UnixSocketFactory(socket);
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
        return new HttpUnixSocket();
    }

    @Override
    public Socket connectSocket(int connectTimeout, Socket sock, HttpHost host,
                                InetSocketAddress remoteAddress, InetSocketAddress localAddress, HttpContext context) throws IOException {
        try {
            sock.connect(new UnixSocketAddress(socketFile), connectTimeout);
        } catch (SocketTimeoutException e) {
            throw new ConnectTimeoutException(e, null, remoteAddress.getAddress());
        }

        return sock;
    }
}
