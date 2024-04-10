package org.example;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Peer;
import org.bitcoinj.core.PeerAddress;
import org.bitcoinj.core.listeners.PeerConnectedEventListener;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    //String[] args = null;
    private NetworkParameters connectToBitcoinNode(String[] args){
        if (args.length < 2) {
            System.err.println("Usage: address-to-send-back-to [regtest|testnet]");
            //return;
        }
        NetworkParameters params;
        String filePrefix;
        if (args[1].equals("testnet")) {
            params = TestNet3Params.get();
            filePrefix = "forwarding-service-testnet"; //use of this file?
        } else if (args[1].equals("regtest")) {
            params = RegTestParams.get();
            filePrefix = "forwarding-service-regtest";
        } else {
            params = MainNetParams.get();
            filePrefix = "forwarding-service";
        }
        return params;
    }
    public static void main(String[] args) throws UnknownHostException {

        BriefLogFormatter.init();

        NetworkParameters params = null; // use TestNet3Params.get() for testnet
        Main app = new Main();
        params = app.connectToBitcoinNode(new String[]{"2oiosidmos09494", "regtest"});
        System.out.println(params);

        WalletAppKit kit = new WalletAppKit(params, new File("C:\\Users\\itoro\\Downloads\\bitcoinj"), "Wallet-A") {
            @Override
            protected void onSetupCompleted() {
                if (wallet().getKeyChainGroupSize() < 1)
                    wallet().importKey(new ECKey());
            }
        };

        System.out.println(params.getPort());
        System.out.println(params.getAddrSeeds());

        if (params == RegTestParams.get()) {
            //kit.connectToLocalHost();
            int port = 18424;
            String ipAddress = "172.30.145.30";
            InetSocketAddress nodeAddress = new InetSocketAddress(ipAddress, port);
            kit.setPeerNodes(PeerAddress.simple(nodeAddress));
        }

        kit.addListener(new Service.Listener() {
            @Override
            public void starting() {
                System.out.println("Starting...");
            }

            @Override
            public void running() {
                System.out.println("Running!");

                // Now that WalletAppKit is running, it's safe to add the PeerConnectedEventListener.
                kit.peerGroup().addConnectedEventListener(new PeerConnectedEventListener() {
                    @Override
                    public void onPeerConnected(Peer peer, int peerCount) {
                        System.out.println("Connected to: " + peer.getAddress() + ", peer count: " + peerCount);
                    }
                });
            }

            @Override
            public void failed(Service.State from, Throwable failure) {
                System.err.println("Failed to start WalletAppKit: " + failure);
            }
        }, MoreExecutors.directExecutor());

        kit.startAsync();
        kit.awaitRunning();

        kit.peerGroup().addConnectedEventListener(new PeerConnectedEventListener() {
            @Override
            public void onPeerConnected(Peer peer, int peerCount) {
                System.out.println("Connected to: " + peer.getAddress() + ", peer count: " + peerCount);
            }
        });
        System.out.println(params.getAddrSeeds());

    }
}