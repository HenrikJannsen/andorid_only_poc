package bisq.app;


import com.google.common.base.Joiner;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.nio.file.Path;
import java.security.KeyPair;
import java.security.Security;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatMessage;
import bisq.chat.ChatMessageType;
import bisq.chat.ChatService;
import bisq.chat.common.CommonPublicChatChannel;
import bisq.chat.common.CommonPublicChatChannelService;
import bisq.chat.common.CommonPublicChatMessage;
import bisq.chat.two_party.TwoPartyPrivateChatChannel;
import bisq.chat.two_party.TwoPartyPrivateChatMessage;
import bisq.common.currency.MarketRepository;
import bisq.common.encoding.Hex;
import bisq.common.locale.LanguageRepository;
import bisq.common.observable.Observable;
import bisq.common.observable.Pin;
import bisq.common.observable.collection.CollectionObserver;
import bisq.common.observable.collection.ObservableSet;
import bisq.common.timer.Scheduler;
import bisq.common.util.MathUtils;
import bisq.i18n.Res;
import bisq.network.common.Address;
import bisq.network.common.TransportType;
import bisq.network.p2p.ServiceNode;
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.node.Node;
import bisq.network.p2p.services.confidential.ConfidentialMessageService;
import bisq.network.p2p.services.peer_group.PeerGroupManager;
import bisq.security.DigestUtil;
import bisq.security.pow.ProofOfWork;
import bisq.user.UserService;
import bisq.user.identity.NymIdGenerator;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfile;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AndroidApp {
    private final AndroidApplicationService applicationService;
    public final List<String> logMessages = new ArrayList<>();
    public final Observable<String> logMessage = new Observable<>("");

    public AndroidApp(Path userDataDir, boolean isRunningInAndroidEmulator) {
        Address.setIsRunningInAndroidEmulator(isRunningInAndroidEmulator);

        // Androids default BC version does not support all algorithms we need, thus we remove
        // it and add our BC provider
        Security.removeProvider("BC");
        Security.addProvider(new BouncyCastleProvider());

        applicationService = AndroidApplicationService.getInstance(userDataDir);
        CompletableFuture.runAsync(() -> {
            observeAppState();
            printDefaultKey();
            printLanguageCode();
            applicationService.readAllPersisted().join();
            applicationService.initialize().join();

            observeNetworkState();
            observeNumConnections();
            observePrivateMessages();
            printMarketPrice();

            createUserIfNoneExist();
            printUserProfiles();

            publishRandomChatMessage();
            observeChatMessages(5);
        });
    }

    private void observeNetworkState() {
        Optional.ofNullable(applicationService.getNetworkService().getDefaultNodeStateByTransportType().get(TransportType.CLEAR))
                .orElseThrow()
                .addObserver(state -> appendLog("Network state", state));
    }

    private void observeNumConnections() {
        ServiceNode serviceNode = applicationService.getNetworkService().getServiceNodesByTransport().findServiceNode(TransportType.CLEAR).orElseThrow();
        Node defaultNode = serviceNode.getDefaultNode();
        PeerGroupManager peerGroupManager = serviceNode.getPeerGroupManager().orElseThrow();
        var peerGroupService = peerGroupManager.getPeerGroupService();
        AtomicLong numConnections = new AtomicLong();
        Scheduler.run(() -> {
            long currentNumConnections = peerGroupService.getAllConnectedPeers(defaultNode).count();
            if (numConnections.get() != currentNumConnections) {
                numConnections.set(currentNumConnections);
                appendLog("Number of connections", currentNumConnections);
            }
        }).periodically(100);
    }

    private void observePrivateMessages() {
        Map<String, Pin> pinByChannelId = new HashMap<>();
        applicationService.getChatService().getTwoPartyPrivateChatChannelService().getChannels()
                .addObserver(new CollectionObserver<>() {
                    @Override
                    public void add(TwoPartyPrivateChatChannel channel) {
                        appendLog("Private channel", channel.getDisplayString());
                        pinByChannelId.computeIfAbsent(channel.getId(),
                                k -> channel.getChatMessages().addObserver(new CollectionObserver<>() {
                                    @Override
                                    public void add(TwoPartyPrivateChatMessage message) {
                                        String text="";
                                        switch (message.getChatMessageType()) {
                                            case TEXT -> {
                                                text = message.getText();
                                            }
                                            case LEAVE -> {
                                                text = "PEER LEFT " + message.getText();
                                                // leave handling not working yet correctly
                                               /* Scheduler.run(()->applicationService.getChatService().getTwoPartyPrivateChatChannelService().leaveChannel(channel))
                                                        .after(500);*/
                                            }
                                            case TAKE_BISQ_EASY_OFFER -> {
                                                text = "TAKE_BISQ_EASY_OFFER " + message.getText();
                                            }
                                            case PROTOCOL_LOG_MESSAGE -> {
                                                text = "PROTOCOL_LOG_MESSAGE " + message.getText();
                                            }
                                        }
                                        String displayString = "[" + channel.getDisplayString() + "] " + text;
                                        appendLog("Private message", displayString);
                                    }

                                    @Override
                                    public void remove(Object o) {
                                        // We do not support remove of PM
                                    }

                                    @Override
                                    public void clear() {
                                    }
                                }));
                    }

                    @Override
                    public void remove(Object o) {
                        if (o instanceof TwoPartyPrivateChatChannel channel) {
                            String id = channel.getId();
                            if (pinByChannelId.containsKey(id)) {
                                pinByChannelId.get(id).unbind();
                                pinByChannelId.remove(id);
                            }
                            appendLog("Closed private channel", channel.getDisplayString());
                        }
                    }

                    @Override
                    public void clear() {
                    }
                });
    }

    private void observeAppState() {
        applicationService.getState().addObserver(state -> appendLog("Application state", Res.get("splash.applicationServiceState." + state.name())));
    }

    private void printDefaultKey() {
        appendLog("Default key ID", applicationService.getSecurityService().getKeyBundleService().getDefaultKeyId());
    }

    private void printLanguageCode() {
        appendLog("Language", LanguageRepository.getDisplayLanguage(applicationService.getSettingsService().getLanguageCode().get()));
    }

    private void printMarketPrice() {
        Optional<String> priceQuote = applicationService.getBondedRolesService().getMarketPriceService()
                .findMarketPrice(MarketRepository.getUSDBitcoinMarket())
                .map(e -> MathUtils.roundDouble(e.getPriceQuote().getValue() / 10000d, 2) + " BTC/USD");
        if (priceQuote.isEmpty()) {
            Scheduler.run(this::printMarketPrice).after(500);
        } else {
            appendLog("Market price", priceQuote.get());
        }
    }

    private void createUserIfNoneExist() {
        UserIdentityService userIdentityService = applicationService.getUserService().getUserIdentityService();
        ObservableSet<UserIdentity> userIdentities = userIdentityService.getUserIdentities();
        if (userIdentities.isEmpty()) {
            String nickName = "Android " + new Random().nextInt(100);
            KeyPair keyPair = applicationService.getSecurityService().getKeyBundleService().generateKeyPair();
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
                    statement).join();
        }
    }

    private void printUserProfiles() {
        applicationService.getUserService().getUserIdentityService().getUserIdentities().stream()
                .map(UserIdentity::getUserProfile)
                .map(userProfile -> userProfile.getUserName() + " [" + userProfile.getNym() + "]")
                .forEach(userName -> appendLog("My profile", userName));

    }

    private void publishRandomChatMessage() {
        UserService userService = applicationService.getUserService();
        UserIdentityService userIdentityService = userService.getUserIdentityService();
        ChatService chatService = applicationService.getChatService();
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        CommonPublicChatChannelService discussionChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        CommonPublicChatChannel channel = discussionChannelService.getChannels().stream().findFirst().orElseThrow();
        UserIdentity userIdentity = userIdentityService.getSelectedUserIdentity();
        discussionChannelService.publishChatMessage("Message " + new Random().nextInt(100),
                Optional.empty(),
                channel,
                userIdentity);
    }

    private void observeChatMessages(int numLastMessages) {
        UserService userService = applicationService.getUserService();
        ChatService chatService = applicationService.getChatService();
        ChatChannelDomain chatChannelDomain = ChatChannelDomain.DISCUSSION;
        CommonPublicChatChannelService discussionChannelService = chatService.getCommonPublicChatChannelServices().get(chatChannelDomain);
        CommonPublicChatChannel channel = discussionChannelService.getChannels().stream().findFirst().orElseThrow();
        int toSkip = Math.max(0, channel.getChatMessages().size() - numLastMessages);
        List<String> displayedMessages = new ArrayList<>();
        channel.getChatMessages().stream()
                .sorted(Comparator.comparingLong(ChatMessage::getDate))
                .map(message -> {
                    displayedMessages.add(message.getId());
                    String authorUserProfileId = message.getAuthorUserProfileId();
                    String userName = userService.getUserProfileService().findUserProfile(authorUserProfileId)
                            .map(UserProfile::getUserName)
                            .orElse("N/A");
                    return "{" + userName + "} " + message.getText();
                })
                .skip(toSkip)
                .forEach(e -> appendLog("Chat message", e));

        channel.getChatMessages().addObserver(new CollectionObserver<>() {
            @Override
            public void add(CommonPublicChatMessage message) {
                if (displayedMessages.contains(message.getId())) {
                    return;
                }
                displayedMessages.add(message.getId());
                String authorUserProfileId = message.getAuthorUserProfileId();
                String userName = userService.getUserProfileService().findUserProfile(authorUserProfileId)
                        .map(UserProfile::getUserName)
                        .orElse("N/A");
                String text = message.getText();
                String displayString = "{" + userName + "} " + text;
                appendLog("Chat message", displayString);
            }

            @Override
            public void remove(Object o) {
                if (o instanceof CommonPublicChatMessage message)
                    appendLog("Removed chat message", message.getText());
            }

            @Override
            public void clear() {
            }
        });
    }

    private void appendLog(String key, Object value) {
        String line = key + ": " + value;
        logMessages.add(line);
        if (logMessages.size() > 20) {
            logMessages.remove(0);
        }
        logMessage.set(Joiner.on("\n").join(logMessages));
    }
}
