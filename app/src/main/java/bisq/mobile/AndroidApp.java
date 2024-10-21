package bisq.mobile;


import com.google.common.base.Joiner;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

import bisq.account.AccountService;
import bisq.bonded_roles.BondedRolesService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatService;
import bisq.chat.Citation;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.common.currency.MarketRepository;
import bisq.common.encoding.Hex;
import bisq.common.observable.Observable;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.i18n.Res;
import bisq.identity.IdentityService;
import bisq.network.NetworkService;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.security.DigestUtil;
import bisq.security.keys.KeyBundleService;
import bisq.security.pow.ProofOfWork;
import bisq.settings.SettingsService;
import bisq.user.UserService;
import bisq.user.identity.NymIdGenerator;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
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
        androidApplicationService.getState().addObserver(state -> appendLog("App state", Res.get("splash.applicationServiceState." + state.name())));

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
        }).periodically(5000);

        // identity
        IdentityService identityService = androidApplicationService.getIdentityService();

        // account
        AccountService accountService = androidApplicationService.getAccountService();

        // settings
        SettingsService settingsService = androidApplicationService.getSettingsService();
        appendLog("LanguageCode", settingsService.getLanguageCode().get());

        // bonded roles
        BondedRolesService bondedRolesService = androidApplicationService.getBondedRolesService();
        Scheduler.run(() -> {
            String priceQuote = bondedRolesService.getMarketPriceService().findMarketPrice(MarketRepository.getUSDBitcoinMarket())
                    .map(e -> MathUtils.roundDouble(e.getPriceQuote().getValue() / 10000d, 2) + " BTC/USD")
                    .orElse("N/A");
            appendLog("USD market price", priceQuote);
        }).periodically(5000);

        // User
        UserService userService = androidApplicationService.getUserService();
        UserIdentityService userIdentityService = userService.getUserIdentityService();

        ObservableSet<UserIdentity> userIdentities = userIdentityService.getUserIdentities();
        if (userIdentities.isEmpty()) {
            String nickName = "Android nick name";
            KeyPair keyPair = keyBundleService.generateKeyPair();
            byte[] pubKeyHash = DigestUtil.hash(keyPair.getPublic().getEncoded());
            ProofOfWork proofOfWork = userIdentityService.mintNymProofOfWork(pubKeyHash);
            byte[] powSolution = proofOfWork.getSolution();
            String nym = NymIdGenerator.generate(pubKeyHash, powSolution); // nym will be created on demand from pubKeyHash and pow
            // CatHash is in desktop, needs to be reimplemented or the javafx part extracted and refactored into a non javafx lib
            //  Image image = CatHash.getImage(pubKeyHash,
            //                                powSolution,
            //                                CURRENT_AVATARS_VERSION,
            //                                CreateProfileModel.CAT_HASH_IMAGE_SIZE);
            int avatarVersion = 0;
            String terms = "";
            String statement = "";
            appendLog("Create new user with", "");
            appendLog("nickName", nickName);
            appendLog("pubKeyHash", Hex.encode(pubKeyHash));
            appendLog("nym", nym);
            userIdentityService.createAndPublishNewUserProfile(nickName,
                    keyPair,
                    pubKeyHash,
                    proofOfWork,
                    avatarVersion,
                    terms,
                    statement);
        } else {
            userIdentities.stream()
                    .map(userIdentity -> userIdentity.getUserProfile())
                    .map(userProfile -> userProfile.getUserName() + " [" + userProfile.getNym() + "]")
                    .forEach(userName -> appendLog("Existing user name", userName));
        }

        // chat
        ChatService chatService = androidApplicationService.getChatService();
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        CommonPublicChatChannelService discussionChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        CommonPublicChatChannel channel = discussionChannelService.getChannels().stream().findFirst().orElseThrow();
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        discussionChannelService.publishChatMessage("my random message " + new Random().nextInt(100),
                Optional.empty(),
                channel,
                userIdentity);
        Scheduler.run(() -> {
            channel.getChatMessages().stream()
                    .map(message -> {
                        String authorUserProfileId = message.getAuthorUserProfileId();
                        String userName = userService.getUserProfileService().findUserProfile(authorUserProfileId)
                                .map(UserProfile::getUserName)
                                .orElse("N/A");
                        String text = message.getText();
                        return userName + ": " + text;
                    })
                    .forEach(e -> appendLog("Chat message:", e));
        }).after(3000);
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
