package com.apps.asyncTask;

import android.content.Context;
import android.os.AsyncTask;

import com.apps.interfaces.LoginListener;
import com.apps.interfaces.PayBillListener;
import com.apps.items.ItemUser;
import com.apps.utils.Constant;
import com.apps.utils.JsonUtils;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class PayBill extends AsyncTask<String,String,Boolean> {

    private Context context;
    private String suc = "";
    private String msg = "";
    private PayBillListener payBillListener;

    public PayBill(Context context, PayBillListener payBillListener) {
        this.context = context;
        this.payBillListener = payBillListener;
    }

    @Override
    protected void onPreExecute() {
        payBillListener.onStart();
        super.onPreExecute();
    }

    @Override
    protected Boolean doInBackground(String... strings) {
        String url = strings[0];
        String json = JsonUtils.getJSONString(url);
        try {
            JSONObject jsonObject = new JSONObject(json);
            JSONArray jsonArray = jsonObject.getJSONArray(Constant.TAG_ROOT);

            JSONObject object = jsonArray.getJSONObject(0);
            suc = object.getString(Constant.TAG_SUCCESS);
            if(suc.equals("0")) {
                msg = object.getString(Constant.TAG_MSG);
            } else {
                String user_id = object.getString(Constant.TAG_USER_ID);
                Constant.menuCount = Integer.parseInt(object.getString(Constant.TAG_CART_COUNT));
                Constant.itemUser = new ItemUser(user_id, object.getString(Constant.TAG_USER_NAME), object.getString(Constant.TAG_USER_NAME_MID), object.getString(Constant.TAG_USER_NAME_LAST), object.getString(Constant.TAG_USER_EMAIL), object.getString(Constant.TAG_USER_PHONE), "", "");
            }
            return true;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        } catch (Exception ee) {
            ee.printStackTrace();
            return false;
        }
    }

    @Override
    protected void onPostExecute(Boolean s) {
        payBillListener.onEnd(String.valueOf(s),suc);
        super.onPostExecute(s);
    }
}
