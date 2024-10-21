package bisq.mobile;


import java.security.KeyPair;

import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
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
        androidApplicationService= AndroidApplicationService.getInitializedInstance(userDataDir);
        KeyBundleService keyBundleService = androidApplicationService.getSecurityService().getKeyBundleService();
        defaultKeyId = keyBundleService.getDefaultKeyId();
        keyPair = keyBundleService.getOrCreateKeyBundle(defaultKeyId).getKeyPair();

        logMessage.set("defaultKeyId=" + defaultKeyId + "\n");
        logMessage.set(logMessage.get() + "default pub key as hex=" + Hex.encode(keyPair.getPublic().getEncoded()) + "\n");

        NetworkService networkService = androidApplicationService.getNetworkService();
        networkService.getDefaultNodeStateByTransportType().get(TransportType.CLEAR)
                .addObserver(state-> logMessage.set(logMessage.get() + "Network state: " + state.toString() + "\n"));
    }
}
