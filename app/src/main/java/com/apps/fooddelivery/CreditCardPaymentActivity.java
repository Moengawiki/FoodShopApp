package com.apps.fooddelivery;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.asyncTask.LoadCheckOut;
import com.apps.asyncTask.LoadLoginLocal;
import com.apps.asyncTask.PayBill;
import com.apps.interfaces.LoginListener;
import com.apps.interfaces.PayBillListener;
import com.apps.sharedPref.SharePref;
import com.apps.utils.Constant;
import com.apps.utils.Methods;
import com.craftman.cardform.Card;
import com.craftman.cardform.CardForm;
import com.craftman.cardform.OnPayBtnClickListner;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.model.Token;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class CreditCardPaymentActivity extends AppCompatActivity {

    public CardForm cardForm;
    public TextView amountCard;
    public Button paybtn;
    public PayBill payBill;
    ProgressDialog progressDialog;
    com.stripe.android.model.Card card;
    Token tok;
    Methods methods;
    ProgressDialog pbar;

    Stripe stripe;
    Integer amount;
    String name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credit_card_payment);
        cardForm = (CardForm) findViewById(R.id.cardForm);
        amountCard =(TextView) findViewById(R.id.payment_amount);
        cardForm.setAmount(Constant.itemOrderList.getTotalBill());
        amountCard.setText(Constant.itemOrderList.getTotalBill());

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.login_in));
        progressDialog.setCancelable(false);


        amount = (int) Math.round(Double.valueOf(Constant.itemOrderList.getTotalBill()));
        name = "xxx";
        stripe = new Stripe(this);

        pbar = new ProgressDialog(this);
        pbar.setMessage(getResources().getString(R.string.loading));
        pbar.setCancelable(false);

        paybtn = (Button) findViewById(R.id.btn_pay);

        paybtn.setText("Pay Now!");

        cardForm.setPayBtnClickListner(new OnPayBtnClickListner() {
            @Override
            public void onClick(Card card) {
                PayBill();

            }
        });

    }


    private void PayBill() {
        payBill = new PayBill(this, new PayBillListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String message) {
                progressDialog.dismiss();
                if(success.equals("true")) {
                    Toast.makeText(CreditCardPaymentActivity.this,message,Toast.LENGTH_LONG).show();
                    submitCard();
                    CreditCardPaymentActivity.this.finish();
                    Intent intent = new Intent(CreditCardPaymentActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);

                } else {
                    Toast.makeText(CreditCardPaymentActivity.this,message,Toast.LENGTH_LONG).show();
                }
            }
        });

        payBill.execute(Constant.Pay_Bill_URL+"?orderId=" + Constant.itemOrderList.getUniqueId());
    }



    public void submitCard() {




        pbar.show();

        card = new com.stripe.android.model.Card(
                cardForm.getCard().getNumber(),
                Integer.valueOf(cardForm.getCard().getExpMonth()),
                Integer.valueOf(cardForm.getCard().getExpYear()),
                cardForm.getCard().getCVC()
        );

        SharePref pref = new SharePref(CreditCardPaymentActivity.this);
        card.setCurrency("pkr");
        card.setName(pref.getEmail()    );
        card.setAddressZip("1000");



        stripe.createToken(card, "pk_test_EN40Bi6xuSqjSeVEJz4dP1LI", new TokenCallback() {
            public void onSuccess(Token token) {
                //   Toast.makeText(getApplicationContext(), "Token created: " + token.getId(), Toast.LENGTH_LONG).show();
                tok = token;
                Log.d("Stripe", tok.toString());
                new CreditCardPaymentActivity.StripeCharge(token.getId()).execute();

            }

            public void onError(Exception error) {
                Log.d("Stripe", error.getLocalizedMessage());
                Toast.makeText(getApplicationContext(), "Unable to validate card", Toast.LENGTH_LONG).show();
                pbar.dismiss();
            }
        });

    }

    public class StripeCharge extends AsyncTask<String, Void, String> {
        String token;

        public StripeCharge(String token) {
            this.token = token;
        }

        @Override
        protected String doInBackground(String... params) {
            new Thread() {
                @Override
                public void run() {
                    postData(name,token,""+amount);
                }
            }.start();
            return "Done";
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.e("Result",s);
//            pbar.dismiss();
            Constant.paymentComplete = 0;
            finish();
        }
    }

    public void postData(String description, String token,String amount) {
        // Create a new HttpClient and Post Header
        try {
            URL url = new URL("https://bonappetitte.com/apis/mobile_payment.php");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            List<CreditCardPaymentActivity.NameValuePair> params = new ArrayList<CreditCardPaymentActivity.NameValuePair>();
            params.add(new CreditCardPaymentActivity.NameValuePair("method", "charge"));
            params.add(new CreditCardPaymentActivity.NameValuePair("description", description));
            params.add(new CreditCardPaymentActivity.NameValuePair("source", token));
            params.add(new CreditCardPaymentActivity.NameValuePair("currency", "pkr"));
            params.add(new CreditCardPaymentActivity.NameValuePair("amount", amount));

            OutputStream os = null;

            os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(params));
            writer.flush();
            writer.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String getQuery(List<CreditCardPaymentActivity.NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (CreditCardPaymentActivity.NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }
        Log.e("Query",result.toString());
        return result.toString();
    }



    public class NameValuePair{
        String name,value;

        public NameValuePair(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}