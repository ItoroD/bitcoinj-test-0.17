package org.example;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;
import org.bitcoinj.base.Address;
import org.bitcoinj.base.Coin;
import org.bitcoinj.base.ScriptType;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.ECKey;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.DeterministicKeyChain;
import org.bitcoinj.wallet.DeterministicSeed;
import org.bitcoinj.wallet.SendRequest;
import org.bitcoinj.wallet.Wallet;
import org.bitcoinj.wallet.listeners.WalletCoinsReceivedEventListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {
    //String[] args = null;
    private NetworkParameters connectToBitcoinNode(String[] args) {
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

    private Address getSegAddress(Wallet wallet) {
        // Get the DeterministicSeed from the wallet
        DeterministicSeed seed = wallet.getKeyChainSeed();

        // Create a new DeterministicKeyChain with the P2WPKH script type
        DeterministicKeyChain keyChain = DeterministicKeyChain.builder().seed(seed).outputScriptType(ScriptType.P2WPKH).build();

        // Add the key chain to the wallet
        wallet.addAndActivateHDChain(keyChain);

        // Generate a fresh SegWit address
        Address segwitAddress = wallet.freshReceiveAddress();

// Print the SegWit address
        System.out.println("Segwit address: " + segwitAddress.toString());
        return segwitAddress;
    }

    private void connectToPeer() {
        //            if (params == RegTestParams.get()) {
//                //kit.connectToLocalHost();
//                int port = 18444;
//                String ipAddress = "192.168.100.130";
//                //String ipAddress = "127.0.0.1";
//                try {
//                    InetAddress address = InetAddress.getByName(ipAddress);
//                    if (address.isReachable(5000)) {
//                        InetSocketAddress nodeAddress = new InetSocketAddress(ipAddress, port);
//                        PeerAddress peerAddress = PeerAddress.simple(nodeAddress);
//                        kit.setPeerNodes(peerAddress);
//                        System.out.println("Connected to peer");
//                    } else {
//                        System.out.println("IP address is not reachable");
//                    }
//                } catch (UnknownHostException e) {
//                    System.out.println("Invalid IP address");
//                } catch (IOException e) {
//                    System.out.println("Error checking IP address");
//                }
//            }

//       kit.wallet().addChangeEventListener(new WalletChangeEventListener() {
//           @Override
//           public void onWalletChanged(Wallet wallet) {
//               System.out.println("changed");
//           }
//       });
    }

    private void listenForPayment(WalletAppKit kit) {
        //Address recieveAddress = kit.wallet().freshReceiveAddress();
        //System.out.println("Send BTC to: " + recieveAddress);
        Executor executor = MoreExecutors.directExecutor();
        Address myaddress = kit.wallet().currentReceiveAddress();
        System.out.println(myaddress);
        // Get the balance
        Coin balance = kit.wallet().getBalance();

        // Print the balance
        System.out.println("Wallet balance: " + balance.toFriendlyString());

        //Address mySegAddress = app.getSegAddress(kit.wallet());
        //System.out.println(mySegAddress);


        //boolean stayRunning = true;
        kit.wallet().addCoinsReceivedEventListener(new WalletCoinsReceivedEventListener() {
            @Override
            public void onCoinsReceived(Wallet w, Transaction tx, Coin prevBalance, Coin newBalance) {
                Coin value = tx.getValueSentToMe(w);
                System.out.println("Received tx for " + value.toFriendlyString() + ": " + tx);
                System.out.println("Transaction will be forwarded after it confirms.");
                Futures.addCallback(tx.getConfidence().getDepthFuture(1), new FutureCallback<TransactionConfidence>() {
                    @Override
                    public void onSuccess(TransactionConfidence result) {
                        // "result" here is the same as "tx" above, but we use it anyway for clarity.
                        //forwardCoins(result);
                        System.out.println("Transaction confirmed: " + result.getTransactionHash());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                    }
                }, executor);
            }
        });
    }

    private void sendPayment(WalletAppKit kit, String amount) {
        Coin value = Coin.parseCoin(amount);
        final Coin amountToSend = value.subtract(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
        //CompletableFuture<Coin> balanceFuture = kit.wallet().getBalanceFuture(amountToSend, Wallet.BalanceType.AVAILABLE);


        Address to = kit.wallet().parseAddress("tb1qerzrlxcfu24davlur5sqmgzzgsal6wusda40er");
        try {
            //Wallet.SendResult result = kit.wallet().sendCoins(kit.peerGroup(), to, value);
            //System.out.println("coins sent. transaction hash: " + result.transaction().getTxId());

            // Create a SendRequest
            SendRequest req = SendRequest.to(to, value);

            // Set the fee per kilobyte (replace 'customFee' with the fee you want to set)
            req.feePerKb = Coin.valueOf(100);

            // Ensure the transaction will meet the required fee
            //kit.wallet().completeTx(req);

            // Broadcast the transaction to the network
            Wallet.SendResult result = kit.wallet().sendCoins(kit.peerGroup(),req);
            System.out.println("coins sent. transaction hash: " + result.transaction().getTxId());
            CompletableFuture<TransactionConfidence> confirmationFuture = result.transaction().getConfidence().getDepthFuture(1);

            confirmationFuture.thenAccept(confidence -> {
                System.out.println("Transaction Confidence: " + confidence.getConfidenceType());
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });


            //Wallet.SendResult result = kit.wallet().sendCoins(kit.peerGroup(), to, value);
            CompletableFuture<Transaction> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // This will block until the transaction is broadcasted
                    return result.broadcastComplete.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

            future.thenAccept(transaction -> {
                System.out.println("Sent coins onwards! Transaction hash is " + transaction.getTxId().toString());
            }).exceptionally(e -> {
                e.printStackTrace();
                return null;
            });
        } catch (InsufficientMoneyException e) {
            System.out.println("Not enough coins in your wallet. Missing " + e.missing.getValue() + " satoshis are missing (including fees)");
            System.out.println("Send money to: " + kit.wallet().currentReceiveAddress().toString());
            System.out.println("Your balance is: " + kit.wallet().getBalance());
        }catch (Exception e){
            System.out.println("Error " + e.getMessage());
        }
    }

    private void attachStatusListners(WalletAppKit kit) {
        kit.addListener(new Service.Listener() {
            @Override
            public void starting() {
                System.out.println("Starting...");
            }

            @Override
            public void running() {
                System.out.println("Running!");
            }

            @Override
            public void failed(Service.State from, Throwable failure) {
                System.err.println("Failed to start WalletAppKit: " + failure);
            }
        }, MoreExecutors.directExecutor());
    }

    private void getTransactions(WalletAppKit kit){
        // Get the transactions from the wallet
        List<Transaction> transactions = kit.wallet().getTransactionsByTime();

        for (Transaction tx : transactions) {
            // Check if the transaction is sent from the wallet
            if (tx.getValue(kit.wallet()).signum() < 0) {
                System.out.println("Sent transaction: " + tx.getTxId());
                System.out.println("Sent to: " + tx.getOutput(0).getSpentBy());
                System.out.println("Amount: " + tx.getValue(kit.wallet()));
            }
        }
    }

    public static void main(String[] args) throws IOException {

        BriefLogFormatter.init();
        try {
            NetworkParameters params = null; // use TestNet3Params.get() for testnet
            Main app = new Main();
            params = app.connectToBitcoinNode(new String[]{"bcrt1qg4u6746cf0geu3ysrdn2dmpn5vtm3ye2dmsmhw", "testnet"});
            System.out.println(params);
            Context context = new Context(params);

            // Propagate the Context object to the current thread
            Context.propagate(context);
            WalletAppKit kit = new WalletAppKit(params, new File("C:\\Users\\itoro\\Downloads\\bitcoinj"), "Wallet-A") {
                @Override
                protected void onSetupCompleted() {
                    if (wallet().getKeyChainGroupSize() < 1)
                        wallet().importKey(new ECKey());
                }
            };

            System.out.println(params.getPort());
            System.out.println(params.getAddrSeeds());

            kit.startAsync();
            kit.awaitRunning();

            app.attachStatusListners(kit);
            app.listenForPayment(kit);
            //app.sendPayment(kit, "0.000005");
            app.getTransactions(kit);

            while (true) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            // kit.stopAsync();
            //kit.awaitTerminated();
            //kit.peerGroup().startBlockChainDownload(bListener);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


    }
}