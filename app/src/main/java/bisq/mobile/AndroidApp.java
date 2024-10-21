package bisq.mobile;


import com.google.common.base.Joiner;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;

import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.timer.Scheduler;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
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
    public final List<String> logMessages = new ArrayList<>();
    public final Observable<String> logMessage = new Observable<>("");

    public AndroidApp(String userDataDir, boolean isRunningInAndroidEmulator) {
        Address.setIsRunningInAndroidEmulator(isRunningInAndroidEmulator);
        androidApplicationService = AndroidApplicationService.getInitializedInstance(userDataDir);

        //i18n
        androidApplicationService.getState().addObserver(state -> appendLog("App state", Res.get("splash.applicationServiceState."+state.name())));

        // security
        KeyBundleService keyBundleService = androidApplicationService.getSecurityService().getKeyBundleService();
        defaultKeyId = keyBundleService.getDefaultKeyId();
        keyPair = keyBundleService.getOrCreateKeyBundle(defaultKeyId).getKeyPair();

        appendLog("defaultKeyId", defaultKeyId);
        appendLog("default pub key as hex", Hex.encode(keyPair.getPublic().getEncoded()));

        // network
        NetworkService networkService = androidApplicationService.getNetworkService();
        networkService.getDefaultNodeStateByTransportType().get(TransportType.CLEAR)
                .addObserver(state -> appendLog("Network state", state));

        ServiceNode serviceNode = networkService.getServiceNodesByTransport().findServiceNode(TransportType.CLEAR).orElseThrow();
        Node defaultNode = serviceNode.getDefaultNode();
        PeerGroupManager peerGroupManager = serviceNode.getPeerGroupManager().orElseThrow();
        var peerGroupService = peerGroupManager.getPeerGroupService();

        Scheduler.run(() -> {
            long numConnections = peerGroupService.getAllConnectedPeers(defaultNode).count();
            appendLog("numConnections", numConnections);
        }).periodically(1000);

        // identity
        IdentityService identityService = androidApplicationService.getIdentityService();


    }

    private void appendLog(String key, Object value) {
        String line = key + ": " + value;
        logMessages.add(line);
        if (logMessages.size() > 30) {
            logMessages.remove(0);
        }
        logMessage.set(Joiner.on("\n").join(logMessages));
    }
}
