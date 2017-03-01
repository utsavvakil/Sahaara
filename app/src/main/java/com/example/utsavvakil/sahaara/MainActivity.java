package com.example.utsavvakil.sahaara;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Handler;
import android.os.SystemClock;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity
{
    TextView myLabel;

    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    TextView metSpeed;
    TextView timeElapsed;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;


    volatile boolean stopWorker;
    SoundPool twenty, thirty, forty;
    int twentybpm, thirtybpm,fortybpm;

    MediaPlayer _twenty, _thirty, _forty;
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        _twenty = MediaPlayer.create(this,R.raw.twenty_bpm);
        _thirty = MediaPlayer.create(this,R.raw.thirty_bpm);
        _forty = MediaPlayer.create(this,R.raw.forty_bpm);
        twenty = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        twentybpm = twenty.load(this,R.raw.twenty_bpm,1);
        thirty = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        thirtybpm = thirty.load(this,R.raw.thirty_bpm,1);
        forty = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        fortybpm = forty.load(this,R.raw.forty_bpm,1);
        Button openButton = (Button)findViewById(R.id.open);
        //Button sendButton = (Button)findViewById(R.id.send);
        Button closeButton = (Button)findViewById(R.id.close);
        metSpeed = (TextView)findViewById(R.id.tvMetSpeed);
        myLabel = (TextView)findViewById(R.id.label);
        timeElapsed = (TextView)findViewById(R.id.timeElapsed);
        //Open Button
        openButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    findBT();
                    openBT();
                }
                catch (IOException ex) { }
            }
        });

       /* //Send Button
        sendButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    sendData();
                }
                catch (IOException ex) { }
            }
        });*/

        //Close button
        closeButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                try
                {
                    closeBT();
                }
                catch (IOException ex) { }
            }
        });
    }

    void findBT()
    {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null)
        {
            myLabel.setText("No bluetooth adapter available");
            Toast.makeText(this,"No bluetooth adapter available",Toast.LENGTH_SHORT).show();
        }

        if(!mBluetoothAdapter.isEnabled())
        {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if(pairedDevices.size() > 0)
        {
            for(BluetoothDevice device : pairedDevices)
            {
                if(device.getName().equals("HC-05"))
                {
                    mmDevice = device;
                    break;
                }
            }
        }
        Toast.makeText(this,"Bluetooth Device Found",Toast.LENGTH_LONG).show();
        myLabel.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException
    {
        myLabel.setText("Entered openBT");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        myLabel.setText("Socket Connected");
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();
        myLabel.setText("Streams Initialized");
        beginListenForData();

        myLabel.setText("Bluetooth Opened");
    }

    void beginListenForData()
    {
        final Handler handler = new Handler();
        final byte delimiter = 10; //This is the ASCII code for a newline character
        final long[] times = new long[10];
        final int[] count = {0};

        final long[] time_elapsed1 = new long[1];
        final long[] time_elapsed2 = new long[1];
        final long[] diff = new long[1];
        time_elapsed1[0]=0;
        time_elapsed2[0]=0;

        diff[0]=0;
        //Toast.makeText(getApplicationContext(),"Received data",Toast.LENGTH_SHORT).show();

        stopWorker = false;
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        workerThread = new Thread(new Runnable()
        {
            public void run()
            {
                while(!Thread.currentThread().isInterrupted() && !stopWorker)
                {
                    try
                    {
                        int bytesAvailable = mmInputStream.available();
                        if(bytesAvailable > 0)
                        {
                            byte[] packetBytes = new byte[bytesAvailable];
                            mmInputStream.read(packetBytes);
                            for(int i=0;i<bytesAvailable;i++)
                            {
                                byte b = packetBytes[i];


                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    final char c=(char)b;

                                    readBufferPosition = 0;

                                boolean post = handler.post(new Runnable() {
                                    public void run() {
                                        myLabel.setText("Data Received is: "+c);
                                        if (time_elapsed1[0] == 0) {
                                            time_elapsed1[0] = SystemClock.elapsedRealtime();
                                            diff[0] = time_elapsed1[0];
                                        } else if (time_elapsed1[0] != 0 && time_elapsed2[0] == 0) {
                                            time_elapsed2[0] = SystemClock.elapsedRealtime();
                                            diff[0] = time_elapsed2[0] - time_elapsed1[0];
                                        } else if (time_elapsed2[0] != 0) {
                                            time_elapsed1[0] = time_elapsed2[0];
                                            time_elapsed2[0] = SystemClock.elapsedRealtime();
                                            diff[0] = time_elapsed2[0] - time_elapsed1[0];
                                        }
                                        System.out.print("Time elapsed" + time_elapsed1[0]);
                                        timeElapsed.setText("Time Difference: "+(diff[0]/1000));

                                       // Toast.makeText(getApplicationContext(), "Time elapsed" + (diff[0]), Toast.LENGTH_SHORT).show();
                                       // myLabel.setText("" + c);
                                        if ((diff[0]/1000) < 2) {
                                            //twenty.play(twentybpm, 1, 1, 1, -1, 1f);
                                            if(_thirty.isPlaying())
                                            {
                                                _thirty.pause();
                                                _thirty.seekTo(0);
                                            }
                                            if(_twenty.isPlaying())
                                            {
                                                _twenty.pause();
                                                _twenty.seekTo(0);
                                            }

                                            if(!_forty.isPlaying()) {


                                                _forty.start();
                                                _forty.setLooping(true);
                                                metSpeed.setText("40");
                                            }

                                            myLabel.setText("Twenty bpm" + diff[0]);
                                            //Toast.makeText(getApplicationContext(), "Twenty bpm" + (diff[0]), Toast.LENGTH_SHORT).show();
                                        } else if ((diff[0]/1000) >= 2 && (diff[0]/1000) < 4) {
                                            //thirty.play(thirtybpm, 1, 1, 1, -1, 1f);

                                            if(_twenty.isPlaying())
                                            {
                                                _twenty.pause();
                                                _twenty.seekTo(0);
                                            }
                                            if(_forty.isPlaying())
                                            {
                                                _forty.pause();
                                                _forty.seekTo(0);
                                            }

                                            if(!_thirty.isPlaying()) {


                                                _thirty.start();
                                                _thirty.setLooping(true);
                                                metSpeed.setText("30");
                                            }


                                            myLabel.setText("Thirty bpm" + diff[0]);
                                            //Toast.makeText(getApplicationContext(), "Thirty bpm" + (diff[0]), Toast.LENGTH_SHORT).show();
                                        } else if ((diff[0]/1000) >= 4 && (diff[0]/1000) < 7) {
                                           // forty.play(fortybpm, 1, 1, 1, -1, 1f);

                                            if(_forty.isPlaying())
                                            {
                                                _forty.pause();
                                                _forty.seekTo(0);
                                            }
                                            if(_twenty.isPlaying())
                                            {
                                                _twenty.pause();
                                                _twenty.seekTo(0);
                                            }

                                            if(!_twenty.isPlaying()) {


                                                _twenty.start();
                                                _twenty.setLooping(true);
                                                metSpeed.setText("20");
                                            }

                                            myLabel.setText("Forty bpm" + diff[0]);
                                            //Toast.makeText(getApplicationContext(), "Forty bpm" + (diff[0]), Toast.LENGTH_SHORT).show();
                                        }


                                    }
                                });


                            }
                        }
                    }
                    catch (IOException ex)
                    {
                        stopWorker = true;
                    }
                }
            }
        });

        workerThread.start();
    }

   /* void sendData() throws IOException
    {
        String msg = myTextbox.getText().toString();
        msg += "\n";
        mmOutputStream.write(msg.getBytes());
        myLabel.setText("Data Sent");
    }
*/
    void closeBT() throws IOException
    {
        stopWorker = true;
        if(_twenty.isPlaying())
        {
            _twenty.stop();
        }
        if(_thirty.isPlaying())
        {
            _thirty.stop();
        }
        if(_forty.isPlaying())
        {
            _forty.stop();
        }
        mmOutputStream.close();
        mmInputStream.close();
        mmSocket.close();
        myLabel.setText("Bluetooth Closed");
        Toast.makeText(this,"Bluetooth Closed",Toast.LENGTH_SHORT).show();
    }
}
