package com.apps.interfaces;

public interface PayBillListener  {
    void onStart();
    void onEnd(String success, String message);
}
