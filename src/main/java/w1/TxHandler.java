package w1;
import java.util.*;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        ArrayList<UTXO> utxos = this.utxoPool.getAllUTXO();
        ArrayList<Transaction.Input> inputs = tx.getInputs();

        System.out.println("1");
        // IMPLEMENT THIS
        // (1) all outputs claimed by {@code tx} are in the current UTXO pool,
        for(int i = 0; i < tx.numInputs(); i++){
            Boolean txInUtxo = false;
            UTXO utxo = new UTXO(tx.getInput(i).prevTxHash,tx.getInput(i).outputIndex);
//            StringBuffer sb = new StringBuffer();
//            for (byte b : input.prevTxHash) {
//                sb.append(Integer.toHexString((int) (b & 0xff)));
//            }
//            System.out.println("input hash: " + sb);
//            for(UTXO utxo : utxos) {
//                StringBuffer sb2 = new StringBuffer();
//                for (byte b : utxo.getTxHash()) {
//                    sb2.append(Integer.toHexString((int) (b & 0xff)));
//                }
//                System.out.println("utxo hash: " + sb2);
//                if(Arrays.equals(utxo.getTxHash(), input.prevTxHash)) txInUtxo = true;
//            }
            if(!utxoPool.contains(utxo)) return false;
//            if(!txInUtxo) return false;
        }

        System.out.println("2");
        // (2) the signatures on each input of {@code tx} are valid,
        UTXOPool utxoSet = new UTXOPool();

        for(int i = 0; i < tx.numInputs(); i++){
            Transaction.Input input = tx.getInput(i);
            UTXO utxo = new UTXO(input.prevTxHash,input.outputIndex);
            if(!Crypto.verifySignature(utxoPool.getTxOutput(utxo).address,
                    tx.getRawDataToSign(i),
                    input.signature)) {
                return false;
            }
            if (utxoSet.contains(utxo)) return false;
            utxoSet.addUTXO(utxo, utxoPool.getTxOutput(utxo));
        }

        System.out.println("3");
        // (3) no UTXO is claimed multiple times by {@code tx}
//        List<byte[]> preHashes = new ArrayList<byte[]>();
//        for(Transaction.Input input : inputs){
//            byte[] preHash = input.prevTxHash;
//            if(preHashes.contains(preHash)) return false;
//            else preHashes.add(preHash);
//            }

        System.out.println("4");
        // (4) all of {@code tx}s output values are non-negative
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for(Transaction.Output output : outputs) {
            if(output.value < 0) return false;
        }

        System.out.println("5");
        // (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output value
        double inputSum = 0;
        for(Transaction.Input input : inputs){
            UTXO utxo = new UTXO(input.prevTxHash,input.outputIndex);
            inputSum += utxoPool.getTxOutput(utxo).value;
        }
        double outputSum = 0;
        for(Transaction.Output output : outputs){
            outputSum += output.value;
        }
        return (outputSum <= inputSum);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        Set<Transaction> validTxs = new HashSet<Transaction>();
        for(Transaction tx : possibleTxs) {
            if(isValidTx(tx)) {
                ArrayList<Transaction.Input> inputs = tx.getInputs();
                ArrayList<UTXO> utxos = this.utxoPool.getAllUTXO();

                for(Transaction.Input input : inputs){
//                    Boolean txInUtxo = false;
//                    StringBuffer sb = new StringBuffer();
//                    for (byte b : input.prevTxHash) {
//                        sb.append(Integer.toHexString((int) (b & 0xff)));
//                    }
//                    System.out.println("handleTxs-input hash: " + sb);
//                    for(UTXO utxo : utxos) {
//                        StringBuffer sb2 = new StringBuffer();
//                        for (byte b : utxo.getTxHash()) {
//                            sb2.append(Integer.toHexString((int) (b & 0xff)));
//                        }
//                        System.out.println("handleTxs-utxo hash: " + sb2);
//                        if(Arrays.equals(utxo.getTxHash(), input.prevTxHash)) this.utxoPool.removeUTXO(utxo);
//                    }
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    this.utxoPool.removeUTXO(utxo);
                }



                validTxs.add(tx);
//                for(Transaction.Input input : inputs){
//                    this.utxoPool.addUTXO(new UTXO(tx.getHash(), input.outputIndex), tx.getOutput(input.outputIndex));
//                }
                for(int i = 0 ; i < tx.numOutputs(); i++) this.utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
            }
        }
        return validTxs.toArray(new Transaction[0]);
    }

}
