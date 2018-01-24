
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import w1.Transaction;
import w1.TxHandler;
import w1.UTXO;
import w1.UTXOPool;

import static com.sun.xml.internal.ws.dump.LoggingDumpTube.Position.Before;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class TxHandlerTest {

    private final KeyPair ALICE = signatureKeyPair();
    private final KeyPair BOB = signatureKeyPair();
    private final KeyPair HAPPY = signatureKeyPair();

    private Map<PublicKey, PrivateKey> publicToPrivate = new HashMap<>();

    private TxHandler txHandler;
    private UTXOPool utxoPool;

    @Before
    public void setUp() throws Exception {
        Stream.of(ALICE, BOB, HAPPY)
                .forEach(keyPair -> publicToPrivate.put(keyPair.getPublic(), keyPair.getPrivate()));
    }

    @Test
    public void simpleAndValidTransactions() throws Exception {
        createHandler();
        Transaction tx = generateNewTransaction(0, 1);

        Transaction[] txs = txHandler.handleTxs(new Transaction[] { tx });

        assertTrue(txHandler.isValidTx(tx));
        assertArrayEquals(txs, new Transaction[] { tx });
    }

    private Transaction generateNewTransaction(Integer... outputIdxs) {
        Transaction tx = new Transaction();
        ArrayList<UTXO> utxos = utxoPool.getAllUTXO();
        double totalInput = Stream.of(outputIdxs)
                .map(utxos::get)
                .mapToDouble(utxo -> utxoPool.getTxOutput(utxo).value)
                .sum();
        tx.addOutput(totalInput, HAPPY.getPublic());
        Stream.of(outputIdxs).forEach(idx -> {
            UTXO utxo = utxos.get(idx);
            tx.addInput(utxo.getTxHash(), utxo.getIndex());
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            try {
                Signature rsa = Signature.getInstance("SHA256withRSA");
                rsa.initSign(publicToPrivate.get(output.address));
                rsa.update(tx.getRawDataToSign(tx.getInputs().size() - 1));
                tx.addSignature(rsa.sign(), tx.getInputs().size() - 1);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                e.printStackTrace();
            }
        });
        setHash(tx);
        return tx;
    }

    private void createHandler() {
        this.utxoPool = populatePool(generateUtx(), generateUtx());
        this.txHandler = new TxHandler(utxoPool);
    }

    private Transaction generateUtx() {
        Random random = new Random();
        Transaction tx = new Transaction();
        tx.addOutput(random.nextDouble(), ALICE.getPublic());
        tx.addOutput(random.nextDouble(), BOB.getPublic());
        setHash(tx);
        return tx;
    }

    private void setHash(Transaction tx) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return;
        }
        md.update(tx.getRawTx());
        tx.setHash(md.digest());
    }

    private UTXOPool populatePool(Transaction... txs) {
        UTXOPool pool = new UTXOPool();
        Arrays.stream(txs).forEach(tx -> {
            pool.addUTXO(new UTXO(tx.getHash(), 0), tx.getOutput(0));
            pool.addUTXO(new UTXO(tx.getHash(), 1), tx.getOutput(1));
        });
        return pool;
    }

    public static KeyPair signatureKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(1024, random);
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            return null;
        }
    }
}