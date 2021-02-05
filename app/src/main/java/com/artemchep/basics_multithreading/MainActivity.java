package com.artemchep.basics_multithreading;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.UiThread;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.artemchep.basics_multithreading.cipher.CipherUtil;
import com.artemchep.basics_multithreading.domain.Message;
import com.artemchep.basics_multithreading.domain.WithMillis;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {

    private static final int CIPHER_ELEMENT_DONE_MESSAGE = 1;
    private static final int CIPHER_QUEUE_START_MESSAGE = 2;
    private static final int CIPHER_QUEUE_DONE_MESSAGE = 3;

    private List<WithMillis<Message>> mList = new ArrayList<>();

    private Queue<Long> mAddTimeQueue = new LinkedList<>();

    private MessageAdapter mAdapter = new MessageAdapter(mList);

    private Handler mHandler;

    private boolean canEncrypt = true;
    private int mQueueEndPointer = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(mAdapter);

        showWelcomeDialog();

        mHandler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case CIPHER_QUEUE_START_MESSAGE: {
                        canEncrypt = false;
                        return true;
                    }
                    case CIPHER_ELEMENT_DONE_MESSAGE: {
                        update((WithMillis<Message>) msg.obj);
                        return true;
                    }
                    case CIPHER_QUEUE_DONE_MESSAGE: {
                        canEncrypt = true;
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void showWelcomeDialog() {
        new AlertDialog.Builder(this)
                .setMessage("What are you going to need for this task: Thread, Handler.\n" +
                        "\n" +
                        "1. The main thread should never be blocked.\n" +
                        "2. Messages ;should be processed sequentially.\n" +
                        "3. The elapsed time SHOULD include the time message spent in the queue.")
                .show();
    }

    public void onPushBtnClick(View view) {
        Message message = Message.generate();
        insert(new WithMillis<>(message));
    }

    @UiThread
    public void insert(final WithMillis<Message> message) {
        mList.add(message);
        mAddTimeQueue.add(System.currentTimeMillis());
        mAdapter.notifyItemInserted(mList.size() - 1);

        // TODO: Start processing the message (please use CipherUtil#encrypt(...)) here.
        //       After it has been processed, send it to the #update(...) method.
        if (canEncrypt) {
            mHandler.sendEmptyMessage(CIPHER_QUEUE_START_MESSAGE);
            new Thread(new Runnable() {
                public void run() {
                    while (mQueueEndPointer < mList.size()) {
                        WithMillis<Message> message = mList.get(mQueueEndPointer);
                        Long startTime = mAddTimeQueue.poll();
                        final Message messageNew = message.value.copy(CipherUtil.encrypt(message.value.plainText));
                        long threadDuration = (System.currentTimeMillis() - startTime);
                        final WithMillis<Message> messageNewWithMillis = new WithMillis<>(messageNew, threadDuration);
                        mHandler.sendMessage(mHandler.obtainMessage(CIPHER_ELEMENT_DONE_MESSAGE, messageNewWithMillis));
                        mQueueEndPointer++;
                    }
                    mHandler.sendEmptyMessage(CIPHER_QUEUE_DONE_MESSAGE);
                }
            }).start();
        }
        // How it should look for the end user? Uncomment if you want to see. Please note that
        // you should not use poor decor view to send messages to UI thread.
//        getWindow().getDecorView().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                final Message messageNew = message.value.copy("sample :)");
//                final WithMillis<Message> messageNewWithMillis = new WithMillis<>(messageNew, CipherUtil.WORK_MILLIS);
//                update(messageNewWithMillis);
//            }
//        }, CipherUtil.WORK_MILLIS);
    }

    @UiThread
    public void update(final WithMillis<Message> message) {
        for (int i = 0; i < mList.size(); i++) {
            if (mList.get(i).value.key.equals(message.value.key)) {
                mList.set(i, message);
                mAdapter.notifyItemChanged(i);
                return;
            }
        }
        throw new IllegalStateException();
    }
}
