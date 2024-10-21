package bisq.mobile;


import java.security.KeyPair;
import java.util.Optional;

import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.timer.Scheduler;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.security.keys.KeyBundleService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AndroidApp {
    private final AndroidApplicationService androidApplicationService;
    private final String defaultKeyId;
    private final KeyPair keyPair;
    public final Observable<String> logMessage = new Observable<>("");

    public AndroidApp(String userDataDir, boolean isRunningInAndroidEmulator) {
        Address.setIsRunningInAndroidEmulator(isRunningInAndroidEmulator);
        androidApplicationService = AndroidApplicationService.getInitializedInstance(userDataDir);
        KeyBundleService keyBundleService = androidApplicationService.getSecurityService().getKeyBundleService();
        defaultKeyId = keyBundleService.getDefaultKeyId();
        keyPair = keyBundleService.getOrCreateKeyBundle(defaultKeyId).getKeyPair();

        logMessage.set("defaultKeyId=" + defaultKeyId + "\n");
        logMessage.set(logMessage.get() + "default pub key as hex=" + Hex.encode(keyPair.getPublic().getEncoded()) + "\n");

        NetworkService networkService = androidApplicationService.getNetworkService();
        networkService.getDefaultNodeStateByTransportType().get(TransportType.CLEAR)
                .addObserver(state -> logMessage.set(logMessage.get() + "Network state: " + state.toString() + "\n"));

        ServiceNode serviceNode = networkService.getServiceNodesByTransport().findServiceNode(TransportType.CLEAR).orElseThrow();
        Node defaultNode = serviceNode.getDefaultNode();
        PeerGroupManager peerGroupManager = serviceNode.getPeerGroupManager().orElseThrow();
        var peerGroupService = peerGroupManager.getPeerGroupService();

        Scheduler.run(() -> {
            long numConnections = peerGroupService.getAllConnectedPeers(defaultNode).count();
            logMessage.set(logMessage.get() + "numConnections: " + numConnections + "\n");
        }).periodically(1000);
    }
}
