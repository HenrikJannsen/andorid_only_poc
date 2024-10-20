package bisq.mobile;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.Nullable;

import java.security.KeyPair;
import java.security.Security;

import bisq.android.AndroidApplicationService;
import bisq.common.encoding.Hex;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AndroidApp {
    private final AndroidApplicationService androidApplicationService;
    private final String defaultKeyId;
    private final KeyPair keyPair;

    public AndroidApp() {
        androidApplicationService= AndroidApplicationService.getInitializedInstance();
        KeyBundleService keyBundleService = androidApplicationService.getSecurityService().getKeyBundleService();
        defaultKeyId = keyBundleService.getDefaultKeyId();
        keyPair = keyBundleService.getOrCreateKeyBundle(defaultKeyId).getKeyPair();
        log.info("defaultKeyId={}", defaultKeyId);
        log.info("default pub key as hex={}", Hex.encode(keyPair.getPublic().getEncoded()));
    }

    public String getInfo() {
        return "default key ID=" + defaultKeyId+
                "\n\ndefault pub key as hex=" + Hex.encode(keyPair.getPublic().getEncoded());
    }
}
