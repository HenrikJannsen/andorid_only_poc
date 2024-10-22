package bisq.network.p2p.node.transport;

import bisq.common.timer.Scheduler;
import bisq.common.network.Address;
import bisq.common.network.TransportConfig;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyBundle;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class I2PTransportService implements TransportService {
    @Getter
    @ToString
    @EqualsAndHashCode
    public static final class Config implements TransportConfig {
        public static Config from(Path dataDir, com.typesafe.config.Config config) {
            return new Config(dataDir,
                    config.hasPath("defaultNodePort") ? config.getInt("defaultNodePort") : -1,
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("defaultNodeSocketTimeout")),
                    (int) TimeUnit.SECONDS.toMillis(config.getInt("userNodeSocketTimeout")),
                    config.getInt("inboundKBytesPerSecond"),
                    config.getInt("outboundKBytesPerSecond"),
                    config.getInt("bandwidthSharePercentage"),
                    config.getString("i2cpHost"),
                    config.getInt("i2cpPort"),
                    config.getBoolean("embeddedRouter"),
                    config.getBoolean("extendedI2pLogging"),
                    config.getInt("sendMessageThrottleTime"),
                    config.getInt("receiveMessageThrottleTime"));
        }

        private final int defaultNodePort;
        private final int defaultNodeSocketTimeout;
        private final int userNodeSocketTimeout;
        private final int inboundKBytesPerSecond;
        private final int outboundKBytesPerSecond;
        private final int bandwidthSharePercentage;
        private final int i2cpPort;
        private final String i2cpHost;
        private final boolean embeddedRouter;
        private final Path dataDir;
        private final boolean extendedI2pLogging;
        private final int sendMessageThrottleTime;
        private final int receiveMessageThrottleTime;

        public Config(Path dataDir,
                      int defaultNodePort,
                      int defaultNodeSocketTimeout,
                      int userNodeSocketTimeout,
                      int inboundKBytesPerSecond,
                      int outboundKBytesPerSecond,
                      int bandwidthSharePercentage,
                      String i2cpHost,
                      int i2cpPort,
                      boolean embeddedRouter,
                      boolean extendedI2pLogging,
                      int sendMessageThrottleTime,
                      int receiveMessageThrottleTime) {
            this.dataDir = dataDir;
            this.defaultNodePort = defaultNodePort;
            this.defaultNodeSocketTimeout = defaultNodeSocketTimeout;
            this.userNodeSocketTimeout = userNodeSocketTimeout;
            this.inboundKBytesPerSecond = inboundKBytesPerSecond;
            this.outboundKBytesPerSecond = outboundKBytesPerSecond;
            this.bandwidthSharePercentage = bandwidthSharePercentage;
            this.i2cpHost = i2cpHost;
            this.i2cpPort = i2cpPort;
            this.embeddedRouter = embeddedRouter;
            this.extendedI2pLogging = extendedI2pLogging;
            this.sendMessageThrottleTime = sendMessageThrottleTime;
            this.receiveMessageThrottleTime = receiveMessageThrottleTime;
        }
    }

    private final String i2pDirPath;
    private boolean initializeCalled;
    private String sessionId;
    @Getter
    private final BootstrapInfo bootstrapInfo = new BootstrapInfo();
    private int numSocketsCreated = 0;
    private final Config config;
    private Scheduler startBootstrapProgressUpdater;

    public I2PTransportService(TransportConfig config) {
        // Demonstrate potential usage of specific config.
        // Would be likely passed to i2p router not handled here...

        // Failed to get config generic...
        this.config = (Config) config;

        i2pDirPath = config.getDataDir().toAbsolutePath().toString();
        log.info("I2PTransport using i2pDirPath: {}", i2pDirPath);
    }

    @Override
    public void initialize() {
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }


    public ServerSocketResult getServerSocket(NetworkId networkId, KeyBundle keyBundle) {
        throw new RuntimeException("Not supported");
    }

    @Override
    public Socket getSocket(Address address) throws IOException {
        throw new RuntimeException("Not supported");
    }

    @Override
    public boolean isPeerOnline(Address address) {
        throw new UnsupportedOperationException("isPeerOnline needs to be implemented for I2P.");
    }
}
