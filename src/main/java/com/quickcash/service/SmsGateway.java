
package com.quickcash.service;

public interface SmsGateway {

    void sendSms(String phoneNumber, String message);

}
