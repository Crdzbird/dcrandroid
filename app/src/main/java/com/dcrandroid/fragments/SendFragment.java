package com.dcrandroid.fragments;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.ActionMode;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.dcrandroid.MainActivity;
import com.dcrandroid.activities.ReaderActivity;
import com.dcrandroid.R;
import com.dcrandroid.data.Constants;
import com.dcrandroid.util.AccountResponse;
import com.dcrandroid.util.BlockedSelectionEditText;
import com.dcrandroid.util.DcrConstants;
import com.dcrandroid.util.DecredInputFilter;
import com.dcrandroid.util.PreferenceUtil;
import com.dcrandroid.util.Utils;
import com.journeyapps.barcodescanner.Util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import mobilewallet.ConstructTxResponse;

import static android.app.Activity.RESULT_OK;

/**
 * Created by Macsleven on 28/11/2017.
 */

public class SendFragment extends android.support.v4.app.Fragment implements AdapterView.OnItemSelectedListener{

    public EditText address;
    public BlockedSelectionEditText amount;
    public TextView totalAmountSending,estimateFee,estimateSize,sendAll,error_label;
    public ImageView scanAddress;
    Button send;
    Spinner accountSpinner;
    private static final int SCANNER_ACTIVITY_RESULT_CODE = 0;
    List<String> categories;
    List<Integer> accountNumbers = new ArrayList<>();
    ArrayAdapter dataAdapter;
    ProgressDialog pd;
    PreferenceUtil util;
    private DcrConstants constants;
    private boolean isSendAll = false;
    private String addressError = "", amountError = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        constants = DcrConstants.getInstance();
        return inflater.inflate(R.layout.content_send, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //you can set the title for your toolbar here for different fragments different titles
        if (getActivity() == null) {
            System.out.println("Activity is null");
            return;
        }
        util = new PreferenceUtil(getActivity());
        getActivity().setTitle(getString(R.string.send));

        address = getActivity().findViewById(R.id.send_dcr_add);
        amount = getActivity().findViewById(R.id.send_dcr_amount);
        totalAmountSending = getActivity().findViewById(R.id.send_dcr_total_amt_sndng);
        scanAddress = getActivity().findViewById(R.id.send_dcr_scan);
        estimateSize = getActivity().findViewById(R.id.send_dcr_estimate_size);
        estimateFee = getActivity().findViewById(R.id.send_dcr_estimate_fee);
        sendAll = getActivity().findViewById(R.id.send_dcr_all);
        send = getActivity().findViewById(R.id.send_btn_tx);
        accountSpinner = view.findViewById(R.id.send_dropdown);
        error_label = getActivity().findViewById(R.id.send_error_label);
        accountSpinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        categories = new ArrayList<>();
        if (getContext() == null) {
            System.out.println("Context is null");
            return;
        }
        dataAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, categories);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_dropdown_item_1line);
        accountSpinner.setAdapter(dataAdapter);

        amount.setFilters(new InputFilter[]{new DecredInputFilter()});
        scanAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), ReaderActivity.class);
                startActivityForResult(intent, SCANNER_ACTIVITY_RESULT_CODE);
            }
        });
        address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().equals("")) {
                    addressError = "Destination Address can not be empty";
                    displayError();
                } else if (!constants.wallet.isAddressValid(s.toString())) {
                    addressError = "Destination Address is not valid";
                    displayError();
                }else{
                    addressError = "";
                    displayError();
                    constructTransaction();
                }
            }
        });

        amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                constructTransaction();
            }
        });

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (address.getText().toString().equals("")) {
                    addressError  = "Destination Address can not be empty";
                }
                if (amount.getText().toString().equals("")) {
                    amountError = "Amount cannot be empty";
                }
                if(addressError.length() > 0 || amountError.length() > 0){
                    displayError();
                    return;
                }
                final String destAddress = address.getText().toString();
                final long amt = Utils.decredToAtom(amount.getText().toString());
                if (!constants.wallet.isAddressValid(destAddress)) {
                    addressError = "Destination Address is not valid";
                }
                if (amt == 0 || !validateAmount()) {
                    amountError = "Amount is not valid";
                }
                if(addressError.length() > 0 || amountError.length() > 0){
                    displayError();
                    return;
                }
                amountError = "";
                addressError = "";
                displayError();
                showInputPassPhraseDialog(destAddress, amt);
            }
        });

        sendAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isSendAll) {
                    isSendAll = false;
                    amount.setEnabled(true);
                    sendAll.setTextColor(Color.parseColor("#000000"));
                    constructTransaction();
                } else {
                    isSendAll = true;
                    try {
                        amount.setText(Utils.formatDecred(constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS)));
                        amount.setEnabled(false);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    sendAll.setTextColor(Color.parseColor("#2970FF"));
                }
            }
        });
        prepareAccounts();
    }

    private void constructTransaction(){
        estimateSize.setText("");
        totalAmountSending.setText("");
        estimateFee.setText("");
        addressError = "";
        amountError = "";
        displayError();
        if(amount.getText().toString().length() == 0){
            return;
        }else if(!validateAmount()){
            return;
        }
        
        new Thread(){
            public void run(){
                if(getActivity() == null){
                    System.out.println("Activity is null");
                    return;
                }
                try {
                    String destAddress = address.getText().toString();

                    final long amt = Utils.decredToAtom(amount.getText().toString());
                    if (destAddress.equals("")){
                        destAddress = util.get(Constants.KEY_RECENT_ADDRESS);
                        if(destAddress.equals("")){
                            try {
                                destAddress = constants.wallet.addressForAccount(0);
                                util.set(Constants.KEY_RECENT_ADDRESS, destAddress);
                            }catch (Exception e){
                                e.printStackTrace();
                            }
                        }
                    }else if(amt <= 0){
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                estimateSize.setText("");
                                totalAmountSending.setText("");
                                estimateFee.setText("");
                            }
                        });
                        return;
                    }
                    final ConstructTxResponse response = constants.wallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS, isSendAll);
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            double estFee = 0.001 * response.getEstimatedSignedSize() / 1000;
                            estimateSize.setText(String.format(Locale.getDefault(),"%d bytes",response.getEstimatedSignedSize()));
                            totalAmountSending.setText(Utils.calculateTotalAmount(amt, response.getEstimatedSignedSize(), isSendAll).concat(" DCR"));
                            estimateFee.setText(Utils.formatDecred((float) estFee).concat(" DCR"));
                        }
                    });
                }catch (final Exception e){
                    e.printStackTrace();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            error_label.setText(e.getMessage().substring(0, 1).toUpperCase() + e.getMessage().substring(1));
                        }
                    });
                }
            }
        }.start();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == SCANNER_ACTIVITY_RESULT_CODE) {
            if(resultCode== RESULT_OK) {
                try {
                    String returnString = intent.getStringExtra("keyName");
                    System.out.println("Code: "+returnString);
                    if(returnString.startsWith("decred:"))
                        returnString = returnString.replace("decred:","");
                    if(returnString.length() < 25){
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_add_too_short, Toast.LENGTH_SHORT).show();
                        return;
                    }else if(returnString.length() > 36){
                        Toast.makeText(SendFragment.this.getContext(), R.string.wallet_addr_too_long, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    //TODO: Make available for mainnet
                    if(returnString.startsWith("T")){
                        address.setText(returnString);
                    }else{
                        Toast.makeText(SendFragment.this.getContext(), R.string.invalid_address_prefix, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), R.string.error_not_decred_address, Toast.LENGTH_LONG).show();
                    address.setText("");
                }
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(isSendAll){
            try {
                amount.setText(Utils.formatDecred(constants.wallet.spendableForAccount(accountNumbers.get(accountSpinner.getSelectedItemPosition()), 0)));
                amount.setEnabled(false);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        constructTransaction();
    }

    private void prepareAccounts(){
        new Thread(){
            public void run(){
                try{
                    final AccountResponse response = AccountResponse.parse(constants.wallet.getAccounts(util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS));
                    if(response.errorOccurred){
                        if(getActivity() == null){
                            System.out.println("Activity is null");
                            return;
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(SendFragment.this.getContext(),response.errorMessage,Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    accountNumbers.clear();
                    categories.clear();
                    for(int i = 0; i < response.items.size(); i++){
                        if(response.items.get(i).name.trim().equals("imported")){
                            continue;
                        }
                        categories.add(i, response.items.get(i).name + " " + Utils.formatDecred(response.items.get(i).balance.spendable));
                        accountNumbers.add(response.items.get(i).number);
                    }
                    if(getActivity() == null){
                        System.out.println("Activity is null");
                        return;
                    }
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dataAdapter.notifyDataSetChanged();
                        }
                    });
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }.start();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void startTransaction(final String passphrase, final String destAddress,final long amt){
        pd = Utils.getProgressDialog(getContext(),false,false,"Processing...");
        pd.show();
        new Thread(){
            public void run(){
                try {
                    final ConstructTxResponse response = constants.wallet.constructTransaction(destAddress, amt, accountNumbers.get(accountSpinner.getSelectedItemPosition()), util.getBoolean(Constants.KEY_SPEND_UNCONFIRMED_FUNDS) ? 0 : Constants.REQUIRED_CONFIRMATIONS, isSendAll);
                    byte[] tx = constants.wallet.signTransaction(response.getUnsignedTransaction(),passphrase.getBytes());
                    byte[] serializedTx = constants.wallet.publishTransaction(tx);
                    List<Byte> hashList = new ArrayList<>();
                    for (byte aSerializedTx : serializedTx) {
                        hashList.add(aSerializedTx);
                    }
                    Collections.reverse(hashList);
                    final StringBuilder sb = new StringBuilder();
                    for(byte b : hashList){
                        sb.append(String.format(Locale.getDefault(),"%02x", b));
                    }
                    System.out.println("Hash: "+sb.toString());
                    if(getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(pd.isShowing()){
                                    pd.dismiss();
                                }addressError = "";
                                showTxConfirmDialog(sb.toString());
                                send.setEnabled(true);
                            }
                        });
                    }
                }catch (final Exception e){
                    e.printStackTrace();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if(pd.isShowing()){
                                    pd.dismiss();
                                }
                                error_label.setText(e.getMessage().substring(0, 1).toUpperCase() + e.getMessage().substring(1));
                            }
                        });
                    }
                }
            }
        }.start();
    }

    public void showInputPassPhraseDialog(final String destAddress, final long amt) {
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.input_passphrase_box, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final EditText passphrase = dialogView.findViewById(R.id.passphrase_input);

        dialogBuilder.setMessage(getString(R.string.transaction_confirmation)+String.format(Locale.getDefault()," %f DCR", amt/1e8));
        dialogBuilder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String pass = passphrase.getText().toString();
                if(pass.length() > 0){
                    startTransaction(pass, destAddress, amt);
                }
            }
        });

        dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialogBuilder.setCancelable(true);
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(b.BUTTON_POSITIVE).setTextColor(Color.BLUE);
    }

    public void showTxConfirmDialog(final String txHash) {
        if(getActivity() == null){
            System.out.println("Activity is null");
            return;
        }
        if(getContext() == null){
            System.out.println("Context is null");
            return;
        }
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.tx_confrimation_display, null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.setView(dialogView);

        final TextView txHashtv = dialogView.findViewById(R.id.tx_hash_confirm_view);
        txHashtv.setText(txHash);
        txHashtv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyToClipboard(txHashtv.getText().toString());
            }
        });

        dialogBuilder.setTitle("Transaction was successful");
        dialogBuilder.setPositiveButton("OKAY", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialogBuilder.setCancelable(true);
                setMainA();
                //do something with edt.getText().toString();
            }
        });

        dialogBuilder.setNeutralButton("VIEW ON DCRDATA", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String url = "https://testnet.dcrdata.org/tx/"+txHash;
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        });

        AlertDialog b = dialogBuilder.create();
        b.show();
        b.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(Color.BLUE);
        amount.setText("0");
        address.setText("");
    }

    public void copyToClipboard(String copyText) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if (sdk < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if(clipboard != null) {
                clipboard.setText(copyText);
            }
        } else {
            if(getContext() == null){
                System.out.println("Context is null");
                return;
            }
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                    getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData
                    .newPlainText(getString(R.string.your_address), copyText);
            if(clipboard != null)
                clipboard.setPrimaryClip(clip);
        }
        Toast toast = Toast.makeText(getContext(),
                R.string.tx_hash_copy, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.BOTTOM | Gravity.END, 50, 50);
        toast.show();
    }

    private boolean validateAmount(){
        String s = amount.getText().toString();
        if(s.indexOf('.') != -1){
            String atoms = s.substring(s.indexOf('.'));
            if(atoms.length() > 9){
                addressError = "Amount is not valid";
                displayError();
                return false;
            }
        }
        addressError = "";
        displayError();
        return true;
    }

    private void displayError(){
        String error = addressError + "\n"+ amountError;
        error_label.setText(error.trim());
    }
    public void setMainA(){
        MainActivity mainActivity = (MainActivity) getActivity();
        mainActivity.displaySelectedScreen(R.id.nav_overview);
    }
}
