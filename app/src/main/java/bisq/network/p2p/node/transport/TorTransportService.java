package bisq.network.p2p.node.transport;

import bisq.common.timer.Scheduler;
import bisq.network.common.Address;
import bisq.network.common.TransportConfig;
import bisq.network.common.TransportType;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.node.ConnectionException;
import bisq.security.keys.KeyBundle;
import com.runjva.sourceforge.jsocks.protocol.Socks5Proxy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@Slf4j
public class TorTransportService implements TransportService {

    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private Scheduler startBootstrapProgressUpdater;
    private int numSocketsCreated = 0;

    public TorTransportService(TransportConfig config) {
    }

    @Override
    public void initialize() {
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle) {
            throw new RuntimeException("Not supported");
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean isPeerOnline(Address address) {
        throw new RuntimeException("Not supported");
    }

    public Optional<Socks5Proxy> getSocksProxy() throws IOException {
        throw new RuntimeException("Not supported");
    }
}
