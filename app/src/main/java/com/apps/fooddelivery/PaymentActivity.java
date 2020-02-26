package com.apps.fooddelivery;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.apps.asyncTask.LoadAbout;
import com.apps.asyncTask.LoadCheckOut;
import com.apps.interfaces.LoginListener;
import com.apps.sharedPref.SharePref;
import com.apps.utils.Constant;
import com.apps.utils.Methods;
import com.squareup.picasso.Picasso;
import com.stripe.android.Stripe;
import com.stripe.android.TokenCallback;
import com.stripe.android.exception.AuthenticationException;
import com.stripe.android.model.Card;
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

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class PaymentActivity extends AppCompatActivity {

    Toolbar toolbar;
    ProgressDialog pbar;
    Methods methods;

    Stripe stripe;
    Integer amount;
    String name;
    Card card;
    Token tok;
    Button submitButton;
    LoadCheckOut loadCheckOut;
    ProgressDialog progressDialog;
    String address,comment,cart_ids;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        methods = new Methods(this);
        methods.forceRTLIfSupported(getWindow());

        toolbar = (Toolbar) this.findViewById(R.id.toolbar_payment);
        toolbar.setTitle("Payment");
        this.setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.loading));

        Intent intent = getIntent();
        address = intent.getStringExtra("address");
        comment = intent.getStringExtra("comment");
        cart_ids = intent.getStringExtra("cart_ids");



        amount = 4000;
        name = "xxx";
        stripe = new Stripe(this);
        //		toolbar.setBackgroundColor(Constant.color);

        pbar = new ProgressDialog(this);
        pbar.setMessage(getResources().getString(R.string.loading));
        pbar.setCancelable(false);

        submitButton = (Button) this.findViewById(R.id.submitButton);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                    loadCheckOutApi();
            }
        });

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public void submitCard(View view) {

        // TODO: replace with your own test key
        TextView cardNumberField = (TextView) findViewById(R.id.cardNumber);
        TextView monthField = (TextView) findViewById(R.id.month);
        TextView yearField = (TextView) findViewById(R.id.year);
        TextView cvcField = (TextView) findViewById(R.id.cvc);

        if(cardNumberField.getText().toString() != null && !cardNumberField.getText().toString().equals("")
                && monthField.getText().toString() != null && !monthField.getText().toString().equals("")
                && yearField.getText().toString() != null && !yearField.getText().toString().equals("")
                && cvcField.getText().toString() != null && !cvcField.getText().toString().equals("")) {

            pbar.show();

            card = new Card(
                    cardNumberField.getText().toString(),
                    Integer.valueOf(monthField.getText().toString()),
                    Integer.valueOf(yearField.getText().toString()),
                    cvcField.getText().toString()
            );

            SharePref pref = new SharePref(PaymentActivity.this);
            card.setCurrency("usd");
            card.setName(pref.getEmail()    );
            card.setAddressZip("1000");
        /*
        card.setNumber(4242424242424242);
        card.setExpMonth(12);
        card.setExpYear(19);
        card.setCVC("123");
        */


            stripe.createToken(card, "pk_test_EN40Bi6xuSqjSeVEJz4dP1LI", new TokenCallback() {
                public void onSuccess(Token token) {
                    //   Toast.makeText(getApplicationContext(), "Token created: " + token.getId(), Toast.LENGTH_LONG).show();
                    tok = token;
                    Log.d("Stripe", tok.toString());
                    new StripeCharge(token.getId()).execute();

                }

                public void onError(Exception error) {
                    Log.d("Stripe", error.getLocalizedMessage());
                    Toast.makeText(getApplicationContext(), "Unable to validate card", Toast.LENGTH_LONG).show();
                    pbar.dismiss();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), "Please Fill All Fields", Toast.LENGTH_LONG).show();
        }
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
            pbar.dismiss();
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

            List<NameValuePair> params = new ArrayList<NameValuePair>();
            params.add(new NameValuePair("method", "charge"));
            params.add(new NameValuePair("description", description));
            params.add(new NameValuePair("source", token));
            params.add(new NameValuePair("currency", "pkr"));
            params.add(new NameValuePair("amount", amount));

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


    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
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

    private void loadCheckOutApi() {
        loadCheckOut = new LoadCheckOut(PaymentActivity.this, new LoginListener() {
            @Override
            public void onStart() {
                progressDialog.show();
            }

            @Override
            public void onEnd(String success, String message) {
                if(progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }

                Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_SHORT).show();
                if(success.equals("0")) {
                    Toast.makeText(PaymentActivity.this, getString(R.string.error_order), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PaymentActivity.this, message, Toast.LENGTH_SHORT).show();
                    Constant.isCartRefresh = true;
                    Constant.menuCount = 0;
//                    finish();
                    Constant.isFromCheckOut = true;
                    Intent intent = new Intent(PaymentActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });
        loadCheckOut.execute(Constant.URL_CHECKOUT_1 + Constant.itemUser.getId() + Constant.URL_CHECKOUT_2 + address + Constant.URL_CHECKOUT_3 + comment + Constant.URL_CHECKOUT_4 + cart_ids);
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


