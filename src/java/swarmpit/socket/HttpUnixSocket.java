package swarmpit.socket;

import com.google.common.collect.Queues;
import jnr.unixsocket.UnixSocketAddress;
import jnr.unixsocket.UnixSocketChannel;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;
import java.util.Queue;

import static jnr.unixsocket.UnixSocketOptions.SO_KEEPALIVE;
import static jnr.unixsocket.UnixSocketOptions.SO_SNDTIMEO;

public class HttpUnixSocket extends Socket {

    private final UnixSocketChannel channel;
    private SocketAddress address;
    private int lingerTime;

    private final Queue<SocketOptionSetter> optionsToSet = Queues.newArrayDeque();

    public HttpUnixSocket() throws IOException {
        this.channel = UnixSocketChannel.open();
        this.address = null;
    }

    @Override
    public void connect(final SocketAddress endpoint) throws IOException {
        if (endpoint instanceof UnixSocketAddress) {
            address = endpoint;
            channel.connect((UnixSocketAddress) endpoint);
            setAllSocketOptions();
        }
    }

    @Override
    public void connect(final SocketAddress endpoint, final int timeout) throws IOException {
        if (endpoint instanceof UnixSocketAddress) {
            address = endpoint;
            channel.connect((UnixSocketAddress) endpoint);
            setAllSocketOptions();
        }
    }

    @Override
    public void bind(final SocketAddress bindpoint) throws IOException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public InetAddress getInetAddress() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public InetAddress getLocalAddress() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public int getPort() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public int getLocalPort() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public SocketAddress getRemoteSocketAddress() {
        return address;
    }

    @Override
    public SocketAddress getLocalSocketAddress() {
        return null;
    }

    @Override
    public SocketChannel getChannel() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return Channels.newInputStream(channel);
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return Channels.newOutputStream(channel);
    }

    private void setSocketOption(final SocketOptionSetter optionSetter) throws SocketException {
        if (channel.isConnected()) {
            try {
                optionSetter.run();
            } catch (IOException e) {
                throw new SocketException(e.getMessage());
            }
        } else {
            if (!optionsToSet.offer(optionSetter)) {
                throw new SocketException("Failed to queue option");
            }
        }
    }

    private void setAllSocketOptions() throws IOException {
        for (final SocketOptionSetter setter : optionsToSet) {
            setter.run();
        }
    }

    @Override
    public void setTcpNoDelay(final boolean on) throws SocketException {
    }

    @Override
    public boolean getTcpNoDelay() throws SocketException {
        return false;
    }

    @Override
    public void setSoLinger(final boolean on, final int linger) throws SocketException {
        if (on) {
            lingerTime = linger;
        }
    }

    @Override
    public int getSoLinger() throws SocketException {
        return lingerTime;
    }

    @Override
    public void sendUrgentData(final int data) throws IOException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void setOOBInline(final boolean on) throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public boolean getOOBInline() throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public synchronized void setSoTimeout(final int timeout) throws SocketException {
        setSocketOption(() -> channel.setOption(SO_SNDTIMEO, timeout));
    }

    @Override
    public synchronized int getSoTimeout() throws SocketException {
        try {
            return channel.getOption(SO_SNDTIMEO);
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
    }

    @Override
    public synchronized void setSendBufferSize(final int size) throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public synchronized int getSendBufferSize() throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public synchronized void setReceiveBufferSize(final int size) throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public synchronized int getReceiveBufferSize() throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void setKeepAlive(final boolean on) throws SocketException {
        setSocketOption(() -> channel.setOption(SO_KEEPALIVE, on));
    }

    @Override
    public boolean getKeepAlive() throws SocketException {
        try {
            return channel.getOption(SO_KEEPALIVE);
        } catch (IOException e) {
            throw new SocketException(e.getMessage());
        }
    }

    @Override
    public void setTrafficClass(final int tc) throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public int getTrafficClass() throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void setReuseAddress(final boolean on) throws SocketException {
    }

    @Override
    public boolean getReuseAddress() throws SocketException {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public synchronized void close() throws IOException {
        if (lingerTime > 0) {
            boolean sleeping = true;
            while (sleeping) {
                try {
                    wait(lingerTime * (long) 1000);
                } catch (InterruptedException ignored) {
                }
                sleeping = false;
            }
        }

        try {
            shutdownInput();
        } finally {
            try {
                shutdownOutput();
            } finally {
                channel.close();
            }
        }
    }

    @Override
    public void shutdownInput() throws IOException {
        channel.shutdownInput();
    }

    @Override
    public void shutdownOutput() throws IOException {
        channel.shutdownOutput();
    }

    @Override
    public String toString() {
        if (address != null) {
            return address.toString();
        }
        return channel.toString();
    }

    @Override
    public boolean isConnected() {
        return channel.isConnected();
    }

    @Override
    public boolean isBound() {
        return false;
    }

    @Override
    public boolean isClosed() {
        return !channel.isOpen();
    }

    @Override
    public boolean isInputShutdown() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public boolean isOutputShutdown() {
        throw new UnsupportedOperationException("Unimplemented");
    }

    @Override
    public void setPerformancePreferences(final int connectionTime, final int latency,
                                          final int bandwidth) {
        throw new UnsupportedOperationException("Unimplemented");
    }

    interface SocketOptionSetter {

        void run() throws IOException;
    }
}