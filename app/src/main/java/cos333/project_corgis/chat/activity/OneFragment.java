package cos333.project_corgis.chat.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cos333.project_corgis.R;
import cos333.project_corgis.chat.adapter.ChatRoomThreadAdapter;
import cos333.project_corgis.chat.app.Config;
import cos333.project_corgis.chat.app.MyApplication;
import cos333.project_corgis.chat.gcm.NotificationUtils;
import cos333.project_corgis.chat.model.Message;
import cos333.project_corgis.chat.model.User;


public class OneFragment extends Fragment{
    private String chatRoomId;
    private String title;
    private String fbid;
    private String TAG = ChatRoomActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private ChatRoomThreadAdapter mAdapter;
    private ArrayList<Message> messageArrayList;
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    private EditText inputMessage;
    private Button btnSend;

    public OneFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_one, container, false);

        chatRoomId = ((ChatRoomActivity)getActivity()).getChatRoomId();
        title = ((ChatRoomActivity)getActivity()).getChatTitle();
        fbid = ((ChatRoomActivity)getActivity()).getfbid();

        inputMessage = (EditText) rootView.findViewById(R.id.message);
        btnSend = (Button) rootView.findViewById(R.id.btn_send);

        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        messageArrayList = new ArrayList<>();

        // self user id is to identify the message owner
        String selfUserId = MyApplication.getInstance().getPrefManager().getUser().getId();

        mAdapter = new ChatRoomThreadAdapter(getActivity(), messageArrayList, selfUserId);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Config.PUSH_NOTIFICATION)) {
                    // new push message is received
                    handlePushNotification(intent);
                }
            }
        };

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        fetchChatThread();

        // Inflate the layout for this fragment
        return rootView;
    }

    public void onResume() {
        super.onResume();

        // registering the receiver for new notification
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(Config.PUSH_NOTIFICATION));

        NotificationUtils.clearNotifications();
    }

    public void onPause() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mRegistrationBroadcastReceiver);
        super.onPause();
    }


    /**
     * Handling new push message, will add the message to
     * recycler view and scroll it to bottom
     * */
    private void handlePushNotification(Intent intent) {
        Message message = (Message) intent.getSerializableExtra("message");
        String chatRoomId = intent.getStringExtra("chat_room_id");

        if (message != null && chatRoomId != null) {
            messageArrayList.add(message);
            mAdapter.notifyDataSetChanged();
            if (mAdapter.getItemCount() > 1) {
                recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
            }
        }
    }

    /**
     * Posting a new message in chat room
     * will make an http call to our server. Our server again sends the message
     * to all the devices as push notification
     * */
    // TODO: send put request to heroku
    private void sendMessage() {
        final String message = this.inputMessage.getText().toString().trim();

        if (TextUtils.isEmpty(message)) {
            Toast.makeText(getActivity().getApplicationContext(), "Enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        String endPoint = "http://holic-server.herokuapp.com/api/chats/" + chatRoomId;

        Log.e(TAG, "endpoint: " + endPoint);

        this.inputMessage.setText("");

        StringRequest strReq = new StringRequest(Request.Method.PUT,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);


                SharedPreferences pref = getActivity().getApplicationContext().getSharedPreferences("MyPref", 0);
                String userId = fbid;
                String userName = pref.getString("fname", "") + pref.getString("lname", "");
                User user = new User(userId, userName, null);

                Message nmessage = new Message();
                // TODO: add the actually comment id
                nmessage.setId("123");
                nmessage.setMessage(message);
                // TODO set the actualy created_at time
                nmessage.setCreatedAt("123");
                nmessage.setUser(user);

                messageArrayList.add(nmessage);

                mAdapter.notifyDataSetChanged();
                if (mAdapter.getItemCount() > 1) {
                    // scrolling to bottom of the recycler view
                    recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getActivity().getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                inputMessage.setText(message);
            }
        }) {

            @Override
            // TODO: set put params
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();

                params.put("type", "addmessage");
                params.put("fbid", fbid);
                params.put("message", message);

//                params.put("user_id", MyApplication.getInstance().getPrefManager().getUser().getId());
//                params.put("message", message);

                Log.e(TAG, "Params: " + params.toString());

                return params;
            };
        };


        // disabling retry policy so that it won't make
        // multiple http calls
        int socketTimeout = 0;
        RetryPolicy policy = new DefaultRetryPolicy(socketTimeout,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);

        strReq.setRetryPolicy(policy);

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);
    }


    /**
     * Fetching all the messages of a single chat room
     * */
    // TODO get the chat thread from heroku and populate
    private void fetchChatThread() {

        String endPoint = "http://holic-server.herokuapp.com/api/chats/" +chatRoomId;
        Log.e(TAG, "endPoint: " + endPoint);

        StringRequest strReq = new StringRequest(Request.Method.GET,
                endPoint, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "response: " + response);

                try {
                    JSONArray fObj = new JSONArray(response);
                    JSONObject obj = (JSONObject) fObj.get(0);

                    JSONArray commentsObj = obj.getJSONArray("messages");

                    for (int i = 0; i < commentsObj.length(); i++) {
                        JSONObject commentObj = (JSONObject) commentsObj.get(i);

                        String commentId = commentObj.getString("_id");
                        String commentText = commentObj.getString("message");
                        String createdAt = commentObj.getString("created_at");

                        JSONObject userObj = commentObj.getJSONObject("user");
                        String userId = userObj.getString("fbid");
                        String userName = userObj.getString("username");
                        User user = new User(userId, userName, null);

                        Message message = new Message();
                        message.setId(commentId);
                        message.setMessage(commentText);
                        message.setCreatedAt(createdAt);
                        message.setUser(user);

                        messageArrayList.add(message);
                    }

                    mAdapter.notifyDataSetChanged();
                    if (mAdapter.getItemCount() > 1) {
                        recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, mAdapter.getItemCount() - 1);
                    }

                } catch (JSONException e) {
                    Log.e(TAG, "json parsing error: " + e.getMessage());
                    Toast.makeText(getActivity().getApplicationContext(), "json parse error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                NetworkResponse networkResponse = error.networkResponse;
                Log.e(TAG, "Volley error: " + error.getMessage() + ", code: " + networkResponse);
                Toast.makeText(getActivity().getApplicationContext(), "Volley error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        //Adding request to request queue
        MyApplication.getInstance().addToRequestQueue(strReq);
    }

}