package uk.ac.tees.aad.W9491015.Notification;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService {
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAAeuV2e4s:APA91bEMo_fJc3zelDLhxm2axK3g0RbDTCPTxPDs4gzl6nm5dFpnfXTEa3fAeC5FQ2NAQZB-ZLpkQ0OV2aGRhZC9uWDHEwm0q-KI9hIkZ95pUf7X2aQ6nrUcg7TbfVJXVbXWnHxJS6Us" // Your server key refer to video for finding your server key
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotifcation(@Body NotificationSender body);
}
