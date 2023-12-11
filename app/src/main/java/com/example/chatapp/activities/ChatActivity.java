package com.example.chatapp.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import android.util.Base64;
import android.view.View;

import com.example.chatapp.adapter.ChatAdapter;
import com.example.chatapp.databinding.ActivityChatBinding;
import com.example.chatapp.models.ChatMessage;
import com.example.chatapp.models.User;
import com.example.chatapp.utilities.PreferenceManager;
import com.example.chatapp.utilities.constants;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceiverDetails();
        init();
        listenMessage();
    }
    private  void init(){
        preferenceManager =new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(constants.KEY_USER_ID)
        );
        binding.chatRecyclerviw.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }
    private void sendMessage(){
        HashMap<String , Object> message = new HashMap<>();
        message.put(constants.KEY_SENDER_ID,preferenceManager.getString(constants.KEY_USER_ID));
        message.put(constants.KEY_RECEIVER_ID,receiverUser.id);
        message.put(constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
        message.put(constants.KEY_TIMESTAMP , new Date());
        database.collection(constants.KEY_COLLECTION_CHAT).add(message);
        binding.inputMessage.setText(null);

    }
    private void listenMessage(){
        database.collection(constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(constants.KEY_SENDER_ID,preferenceManager.getString(constants.KEY_USER_ID))
                .whereEqualTo(constants.KEY_RECEIVER_ID ,receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(constants.KEY_SENDER_ID , receiverUser.id)
                .whereEqualTo(constants.KEY_RECEIVER_ID,preferenceManager.getString(constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);

    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if(value != null){
            int count = chatMessages.size();
            for(DocumentChange documentChange : value.getDocumentChanges()){
                if(documentChange.getType() == DocumentChange.Type.ADDED){
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);

                }


            }
            Collections.sort(chatMessages, (obj1 , obj2) ->obj1.dateObject.compareTo(obj2.dateObject));
            if(count ==0){
                chatAdapter.notifyDataSetChanged();
            }else{
                chatAdapter.notifyItemRangeInserted(chatMessages.size() ,chatMessages.size());
                binding.chatRecyclerviw.smoothScrollToPosition(chatMessages.size()-1);
            }
            binding.chatRecyclerviw.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
    } ;


    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage,Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes , 0,bytes.length);
    }

    private void loadReceiverDetails(){
        receiverUser = (User) getIntent().getSerializableExtra(constants.Key_USER);
        binding.textName.setText(receiverUser.name);

    }
    private void setListeners(){
        binding.imageBack.setOnClickListener(v ->onBackPressed());
        binding.layoutSend.setOnClickListener(v-> sendMessage());
    }
    private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMM dd,yyyy - hh:mm", Locale.getDefault()).format(date);
    }
}