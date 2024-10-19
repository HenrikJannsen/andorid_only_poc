package bisq.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import bisq.common.platform.Version
import bisq.common.util.ExceptionUtil
import bisq.i18n.Res
import bisq.mobile.ui.theme.MobileTheme
import bisq.network.common.Address
import bisq.network.common.AddressByTransportTypeMap
import bisq.network.identity.NetworkId
import bisq.persistence.PersistenceService
import bisq.persistence.protobuf.Persistence
import bisq.security.SecurityService
import bisq.security.keys.PubKey
import com.typesafe.config.ConfigFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        var version = Version("2.1.2")
        ExceptionUtil.getCauseStack(RuntimeException("test"))
        ExceptionUtil.getCauseStack(RuntimeException("test"))

        Res.setLanguage("en")
        Res.get("test")

        val configString = """
        application {
            appName = "Bisq2"
            devMode = false
            keyIds = "E222AA02,387C8307"
            ignoreSigningKeyInResourcesCheck = false
            ignoreSignatureVerification = false
            memoryReportIntervalSec = 600
            includeThreadListInMemoryReport = true
        
            security = {
                keyBundle = {
                    defaultTorPrivateKey = ""
                    writeDefaultTorPrivateKeyToFile = false
                }
            }
        
        
            bondedRoles = { 
                ignoreSecurityManager = false
        
                marketPrice = {
                    interval = 180 // in seconds
                    timeoutInSeconds = 60
                    providers = [
                                {
                                    // Production node, bonded role
                                    url = "http://runbtcpn7gmbj5rgqeyfyvepqokrijem6rbw7o5wgqbguimuoxrmcdyd.onion"
                                    operator = "runbtc",
                                }
                            ]
                    fallbackProviders = [
                                {
                                   url = "https://price.bisq.wiz.biz"
                                   operator = "wiz",
                                },
                                {
                                    url = "http://emzypricpidesmyqg2hc6dkwitqzaxrqnpkdg3ae2wef5znncu2ambqd.onion"
                                    operator = "emzy",
                                },
                                {
                                    url = "http://devinpndvdwll4wiqcyq5e7itezmarg7rzicrvf6brzkwxdm374kmmyd.onion"
                                    operator = "devinbileck",
                                },
                                {
                                    url = "http://ro7nv73awqs3ga2qtqeqawrjpbxwarsazznszvr6whv7tes5ehffopid.onion"
                                    operator = "alexej996",
                                },
                            ]
                }
        
                blockchainExplorer = {
                    timeoutInSeconds = 60
                    providers = [
                                {
                                     // Production node, bonded role
                                    url = "http://runbtcx3wfygbq2wdde6qzjnpyrqn3gvbks7t5jdymmunxttdvvttpyd.onion"
                                    operator = "RunBTC",
                                }
                            ]
                    fallbackProviders = [
                                {
                                    url = "https://mempool.emzy.de"
                                    operator = "emzy",
                                },
                                {
                                    url = "http://mempool4t6mypeemozyterviq3i5de4kpoua65r3qkn5i3kknu5l2cad.onion"
                                    operator = "emzy",
                                },
                                {
                                    url = "https://mempool.bisq.services"
                                    operator = "devinbileck",
                                },
                                {
                                    url = "http://mempoolcutehjtynu4k4rd746acmssvj2vz4jbz4setb72clbpx2dfqd.onion"
                                    operator = "devinbileck",
                                },
                                {
                                    url = "https://blockstream.info"
                                    operator = "blockstream",
                                },
                                {
                                    url = "http://explorerzydxu5ecjrkwceayqybizmpjjznk5izmitf2modhcusuqlid.onion"
                                    operator = "blockstream",
                                },
                            ]
                }
            }
            
            support = {
                securityManager ={
                    staticPublicKeysProvided = false        
                }
                releaseManager ={
                    staticPublicKeysProvided = false        
                }
                moderator ={
                    staticPublicKeysProvided = false        
                }
            }
            
            network = {
                version = 1
        
                supportedTransportTypes = ["TOR"]
                features = ["INVENTORY_HASH_SET","AUTHORIZATION_HASH_CASH"]
        
                serviceNode {
                    p2pServiceNode=["PEER_GROUP","DATA","CONFIDENTIAL","ACK","MONITOR"]
                }
        
                inventory {
                    maxSizeInKb = 2000
                    repeatRequestIntervalInSeconds = 600
                    maxSeedsForRequest = 2
                    maxPeersForRequest = 4
                    maxPendingRequestsAtStartup = 5
                    maxPendingRequestsAtPeriodicRequests = 2
                    myPreferredFilterTypes=["HASH_SET"]
                }
        
                authorization {
                    myPreferredAuthorizationTokenTypes=["HASH_CASH"]
                }
        
                clearNetPeerGroup {
                    bootstrapTimeInSeconds = 5
                    houseKeepingIntervalInSeconds = 60
                    timeoutInSeconds = 120
                    maxAgeInHours = 2
                    maxPersisted = 100
                    maxReported = 500
                    maxSeeds = 4
                }
        
                defaultPeerGroup {
                    bootstrapTimeInSeconds = 20
                    houseKeepingIntervalInSeconds = 60
                    timeoutInSeconds = 120
                    maxAgeInHours = 2
                    maxPersisted = 100
                    maxReported = 500
                    maxSeeds = 4
                }
        
                peerGroup {
                    minNumConnectedPeers=8
                    minNumOutboundConnectedPeers=3
                    maxNumConnectedPeers=12
                    minNumReportedPeers=1
                }
        
                peerExchangeStrategy {
                    numSeedNodesAtBoostrap=3
                    numPersistedPeersAtBoostrap=10
                    numReportedPeersAtBoostrap=10
                    supportPeerReporting = true
                }
        
                keepAlive {
                    maxIdleTimeInSeconds=90
                    intervalInSeconds=60
                    timeoutInSeconds = 120
                }
        
                // For now there are no public seed nodes set up. Devs have to run their local ones.    
                seedAddressByTransportType {
                    "clear" : [
                        "127.0.0.1:8000",
                        "127.0.0.1:8001"
                    ]
                    "tor" : [
                        "hj2kj43oyq4mhd5gx4vokalnci3vlbwzclv7usocfwuj5f5iks3eivqd.onion:1000",
                        "plur5t7zhcf45bltdhtb4o43p726dqhibc6xo2lhjfxctjcoinlclaid.onion:1000",
                        "ljtn3q7fkb7nqo6umkz36qip5fb46y43hajlkjl5zncctzii5wcsthyd.onion:1000"
                    ]
                    "i2p" : [
                        "kglZCQYj~nyK3YlXCD5FjxOY2ggH8yosII0rqc7oqFhFfjKWy-89WYw-~mtTUqzCaN6LGd17XzheKG44XJnKrM-WvP732V8lbJcoMBIKeeHPlcfwpsTNbMJyWeXIlJByYNlw1HPVRMpBtzfJ9IznyQdwQWDkzA72pLreqpzJrgIoVYzP9OTXVLdROXnTP9RdmnzZ0h1B8XhQM-8LjHB7cE9o9VT9IXIFScICM8VZ8I1sp02rn26McTM~~XO5Zs1Df3IMV0eqteAe6TvH~Rc-6Hh3YhPrjEcv-YvV6RUlsoj605mmSO0Sj5oeacH3Cec73BlNJEGfQkmbTrXVNLqt2S4smqmkAhMq~sdCJCRKP8CFeBk6r-qVREucTeW3AmwXuGS~-8s7pAm99SlpTSepp75a2WNTIsWw~rWiHlM6faTJrkjcO5wJM7~G0tdYgVGk4zrt4VJ02AakUdh8wG1Y5sAX-daTUum~0YTk-fIAVBJSEiNc93XgZkwuTcc4J2BqAAAA:5000",
                        "u~EXMqCbYcdPHvb7nl-Y3eHxSUbaFhwQLycOtA0c45mhrieMaEbRVSRxaUEtjhgk8nVBpKYiDn4Za6X82aPokSFqURJx09bfKTExTklI~1u~0PJk6Wt3~Jpg4TLCYxql0WEphbEs5oEIR1d4myIm4ng3Iz9TM3dZUBMf4B~oRUiMGRxO-U7Vwxb3Qh1J0ZiqvQZmKzk9~ShEpk-FDR1-j0hlICQ2~RHNM7z4CdWReZLiyY8UboOxkakSIYasVEL2xs2Vgt7t4o078X5AcVtEJu6H31WXvUZSffFrt1BXZNTIoYs1FCCuhS1jMLh8N96eR3AqZ43Nr4Ljp78iqbLdikeVhb53Nzr0rDSYcfh57d2YVitjhfz2ant~6~SGSPxdJRdmsmDkTn5VAZwJhHGM5nh2BQbEwuEeeoufw6s7FNEoWMcv86h6ODmKTO0xyk8oMBT81zjdT8Xg5UkaHMSqJ0DnGcrVN4RQ6kOEbT5wtshVjpHgwWiJvOyEcj8XLJLqAAAA:5001"
                    ]
                }
                
                configByTransportType {
                    clear { 
                        defaultNodeSocketTimeout = 120
                        userNodeSocketTimeout = 120
                        devModeDelayInMs = 300
                        sendMessageThrottleTime = 200
                        receiveMessageThrottleTime = 200
                    }
                    tor {
                        bootstrapTimeout = 240
                        hsUploadTimeout = 120
                        defaultNodeSocketTimeout = 120
                        userNodeSocketTimeout = 120
                        testNetwork = false
                        directoryAuthorities = []
                        torrcOverrides = {}
                        sendMessageThrottleTime = 200
                        receiveMessageThrottleTime = 200
                    }
                    i2p {
                        defaultNodeSocketTimeout = 120
                        userNodeSocketTimeout = 120
                        i2cpHost = "127.0.0.1"
                        i2cpPort = 7654
                        inboundKBytesPerSecond = 1024
                        outboundKBytesPerSecond = 512
                        bandwidthSharePercentage = 50
                        embeddedRouter = true
                        extendedI2pLogging = false
                        sendMessageThrottleTime = 200
                        receiveMessageThrottleTime = 200
                    }
                }
            }
        
            bitcoinWallet = {
                // BitcoinWalletSelection enum values: BITCOIND, ELECTRUM, NONE
                // BITCOIND currently not supported
                
                bitcoinWalletSelection = NONE
                bitcoind = {
                    network = regtest
                }
                electrum = {
                    network = regtest
                    electrumXServerHost = 127.0.0.1
                    electrumXServerPort = 50001
                }
            }
        }
    """.trimIndent()

        // Parse the configuration string into a Config object
        val baseConfig: com.typesafe.config.Config = ConfigFactory.parseString(configString)

        var persistenceService = PersistenceService("bisq_mobile")
        val applicationConfig = baseConfig.getConfig("application")
        var securityServiceConfig= SecurityService.Config.from(applicationConfig.getConfig("security"))
        var securityService = SecurityService(persistenceService, securityServiceConfig)

        var address = Address("test.com",80)
        //var networkId = NetworkId(AddressByTransportTypeMap(), PubKey(null,""))

        setContent {
            MobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Version: " + version,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MobileTheme {
        Greeting("Version: " + Version("1.0.0"))
    }
}