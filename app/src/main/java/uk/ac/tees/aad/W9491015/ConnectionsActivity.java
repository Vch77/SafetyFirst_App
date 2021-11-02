package uk.ac.tees.aad.W9491015;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.SnapshotParser;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.ac.tees.aad.W9491015.Notification.APIService;
import uk.ac.tees.aad.W9491015.Notification.Client;
import uk.ac.tees.aad.W9491015.Notification.Data;
import uk.ac.tees.aad.W9491015.Notification.MyResponse;
import uk.ac.tees.aad.W9491015.Notification.NotificationSender;

public class ConnectionsActivity extends AppCompatActivity {

    RecyclerView connectionlist;
    MaterialButton addconnection;

    private APIService apiService;

    //private List<ConnectionDetailsModal> connectionDetailsModalList;
    private FirebaseRecyclerAdapter adapter;

    DatabaseReference usersDatabase,connectionDatabase,notificationDatabase;
    String currentUser;

    Geocoder geocoder;

    int connectionCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connections);

        connectionlist = findViewById(R.id.connectionList);
        addconnection = findViewById(R.id.btnAddConnection);

        currentUser = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        usersDatabase = FirebaseDatabase.getInstance().getReference("Users");
        connectionDatabase = FirebaseDatabase.getInstance().getReference("Connections").child(currentUser);
        notificationDatabase = FirebaseDatabase.getInstance().getReference("Notifications");


        connectionlist.setLayoutManager(new LinearLayoutManager(this));
        connectionlist.setHasFixedSize(false);
        fetchConnections();

        apiService = Client.getClient("https://fcm.googleapis.com/").create(APIService.class);


        addconnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(ConnectionsActivity.this,R.style.CustomAlertDialog);
                ViewGroup viewGroup = findViewById(android.R.id.content);
                View dialogView = LayoutInflater.from(ConnectionsActivity.this).inflate(R.layout.add_connection_layout, viewGroup, false);

                final TextInputEditText connectionEmail = dialogView.findViewById(R.id.connectionEmailField);
                MaterialButton cancel = dialogView.findViewById(R.id.btnCancel);
                MaterialButton add = dialogView.findViewById(R.id.btnAdd);

                builder.setView(dialogView);
                final AlertDialog alertDialog = builder.create();

                alertDialog.setCanceledOnTouchOutside(false);

                alertDialog.setTitle("Enter email of connection");

                cancel.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        alertDialog.dismiss();

                    }
                });

                add.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if(!TextUtils.isEmpty(connectionEmail.getText().toString())){

                            usersDatabase.orderByChild("Email").equalTo(connectionEmail.getText().toString())
                                    .addValueEventListener(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull final DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.getValue() != null) {

                                                final ProgressDialog progressDialog = new ProgressDialog(ConnectionsActivity.this);
                                                progressDialog.setTitle("Adding Connection");
                                                progressDialog.setMessage("Please Wait...");
                                                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                                                progressDialog.setCancelable(false);

                                                progressDialog.show();

                                                connectionDatabase.addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot1) {

                                                        connectionCount = (int) dataSnapshot1.getChildrenCount();

                                                        usersDatabase.orderByChild("Email").equalTo(connectionEmail.getText().toString())
                                                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                                                    @Override
                                                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                                                        for (DataSnapshot childDataSnapshot : dataSnapshot.getChildren()) {

                                                                            HashMap<String, Object> connectionId = new HashMap<>();
                                                                            //connectionId.put("Connection"+ (connectionCount + 1),childDataSnapshot.getKey());
                                                                            connectionId.put("Name",childDataSnapshot.child("Name").getValue().toString());
                                                                            connectionId.put("Email",childDataSnapshot.child("Email").getValue().toString());
                                                                            connectionId.put("Lat",(double) childDataSnapshot.child("LastLat").getValue());
                                                                            connectionId.put("Lng",(double) childDataSnapshot.child("LastLng").getValue());

                                                                            connectionDatabase.child(childDataSnapshot.getKey()).updateChildren(connectionId).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                                                @Override
                                                                                public void onSuccess(Void aVoid) {

                                                                                    progressDialog.dismiss();
                                                                                    alertDialog.dismiss();

                                                                                }
                                                                            });

                                                                        }
                                                                    }

                                                                    @Override
                                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                                    }
                                                                });

                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                                    }
                                                });

                                            }

                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });

                        }

                    }
                });

                alertDialog.show();

            }
        });

    }

    private void fetchConnections(){

        Query query = FirebaseDatabase.getInstance().getReference().child("Connections").child(currentUser);

        FirebaseRecyclerOptions<ConnectionDetailsModal> options =
                new FirebaseRecyclerOptions.Builder<ConnectionDetailsModal>()
                        .setQuery(query, new SnapshotParser<ConnectionDetailsModal>() {
                            @NonNull
                            @Override
                            public ConnectionDetailsModal parseSnapshot(@NonNull DataSnapshot snapshot) {

                                return new ConnectionDetailsModal(snapshot.child("Name").getValue().toString(),
                                        snapshot.child("Email").getValue().toString(),
                                        Double.parseDouble(snapshot.child("Lat").getValue().toString()),
                                        Double.parseDouble(snapshot.child("Lng").getValue().toString()));
                            }
                        })
                        .build();

        adapter = new FirebaseRecyclerAdapter<ConnectionDetailsModal, ViewHolder>(options) {
            @NonNull
            @Override
            public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.connection_detail_layout, parent, false);

                return new ViewHolder(view);
            }


            @Override
            protected void onBindViewHolder(@NonNull final ViewHolder holder, @SuppressLint("RecyclerView") final int position, @NonNull final ConnectionDetailsModal model) {
                holder.setTxtName(model.getName());
                holder.setTxtEmail(model.getEmail());
                holder.setTxtLat(model.getLat());
                holder.setTxtLng(model.getLng());
                holder.setTxtAddress(model.getLat(),model.getLng());

                holder.remove.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String connectionToRemove = getRef(position).getKey().toString();

                        connectionDatabase.child(connectionToRemove).removeValue(new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {

                                adapter.notifyItemRemoved(position);

                            }
                        });

                    }
                });

                holder.send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        usersDatabase.child(currentUser).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                String connectionToSend = getRef(position).getKey();
                                String needieName = dataSnapshot.child("Name").getValue().toString();
                                String lastLat = dataSnapshot.child("LastLat").getValue().toString();
                                String lastLng = dataSnapshot.child("LastLng").getValue().toString();

                                usersDatabase.child(connectionToSend).child("token_id").addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                                        String usertoken=dataSnapshot.getValue(String.class);
                                        sendNotifications(usertoken,
                                                        getNotificationTitle(needieName,System.currentTimeMillis()),
                                                        "Geocordinates: Lat " + lastLat + " Lng " + lastLng);

                                        Toast.makeText(ConnectionsActivity.this,"Notification sent to "+model.getName(),Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError databaseError) {

                                    }
                                });

                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {

                            }
                        });
                    }
                });
            }
        };
        connectionlist.setAdapter(adapter);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        MaterialTextView connectionName,connectionEmail,connectionLat,connectionLng;
        AutoCompleteTextView addressUsingGeocoding;
        MaterialButton remove,send;

        public ViewHolder(View itemView) {
            super(itemView);
            remove = itemView.findViewById(R.id.btnRemove);
            send = itemView.findViewById(R.id.btnSend);
            connectionName = itemView.findViewById(R.id.tvConnectionName);
            connectionEmail = itemView.findViewById(R.id.tvConnectionEmail);
            connectionLat = itemView.findViewById(R.id.tvConnectionLat);
            connectionLng = itemView.findViewById(R.id.tvConnectionLng);
            addressUsingGeocoding = itemView.findViewById(R.id.addressResultText);
        }

        public void setTxtName(String string) {
            connectionName.setText(string);
        }

        public void setTxtEmail(String string) {
            connectionEmail.setText(string);
        }

        public void setTxtLat(double value) {
            connectionLat.setText(String.valueOf(value));
        }

        public void setTxtLng(double value) {
            connectionLng.setText(String.valueOf(value));
        }

        public void setTxtAddress(double lat, double lng){

            String errorMessage = "";

            geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

            List<Address> addresses = null;

            try {
                addresses = geocoder.getFromLocation(lat, lng, 1);
            } catch (IOException e) {
                errorMessage = getString(R.string.service_not_available);
                Log.e("AddressError", errorMessage, e);
            } catch (IllegalArgumentException illegalArgumentException) {
                // Catch invalid latitude or longitude values.
                errorMessage = getString(R.string.invalid_lat_long_used);
                Log.d("Locationmapview", errorMessage + ". " +"Latitude = " + lat +", Longitude = " +lng, illegalArgumentException);
            }

            // Handle case where no address was found.
            if (addresses == null || addresses.size() == 0) {
                if (errorMessage.isEmpty()) {
                    errorMessage = getString(R.string.no_address_found);
                    Log.e("AddressError", errorMessage);
                }

            } else {
                Address address = addresses.get(0);
                ArrayList<String> addressFragments = new ArrayList<>();

                // Fetch the address lines using getAddressLine,
                // join them, and send them to the thread.
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressFragments.add(address.getAddressLine(i));
                }
                // Log.i(TAG, getString(R.string.address_found));

                String geocodingAddress = TextUtils.join(System.getProperty("line.separator"),addressFragments);

                if(geocodingAddress != null)
                    addressUsingGeocoding.setText(geocodingAddress);
                else
                    addressUsingGeocoding.setText("Address fetching error. Use geo-coordinates.");

            }
        }
    }

    private String getNotificationTitle(String name, long time) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();//get your local time zone.
        //SimpleDateFormat sdf = new SimpleDateFormat("E dd-LLL-yyyy hh:mm a"); //day date-month-year hour:minute am/pm
        SimpleDateFormat sdf = new SimpleDateFormat("dd-LLL-yy hh:mm a");
        sdf.setTimeZone(tz);//set time zone.

        return name + " needs you | " + sdf.format(new Date(time));
    }

    public void sendNotifications(String usertoken, String title, String message) {
        Data data = new Data(title, message);
        NotificationSender sender = new NotificationSender(data, usertoken);
        apiService.sendNotifcation(sender).enqueue(new Callback<MyResponse>() {
            @Override
            public void onResponse(Call<MyResponse> call, Response<MyResponse> response) {
                if (response.code() == 200) {
                    if (response.body().success != 1) {
                        Toast.makeText(ConnectionsActivity.this, "Failed ", Toast.LENGTH_LONG);
                    }
                }
            }

            @Override
            public void onFailure(Call<MyResponse> call, Throwable t) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();

    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }

}


