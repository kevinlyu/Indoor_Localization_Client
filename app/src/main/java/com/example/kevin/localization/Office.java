package com.example.kevin.localization;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import java.util.*; //hashmap
import java.lang.*;//parseInt
import java.io.*; //readLine

/**
 * Created by kevin on 2016/5/24.
 */
public class Office {

    private HashMap<Integer, Integer> officeMapping = new HashMap<Integer, Integer>(); //key: officeAddr, value: officeNum
    public HashMap<Integer, String> officeInformation = new HashMap<Integer, String>(); //key: officeNum, value:officeInformation

    public Office(Context cxt) {
        String inLine, temp = "";
        int officeNum = 0, mapNum;
        try {
            if (true) {
                //AssetFileDescriptor descriptor = cxt.getAssets().openFd("info.txt");
                //FileReader file = new FileReader (descriptor.getFileDescriptor());//("info.txt");
                AssetManager assetManager = cxt.getAssets();
                InputStream inputStream = assetManager.open("info.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                while ((inLine = reader.readLine()) != null) {
                    if (inLine.charAt(0) == '*') {
                        for (int i = 1; i < inLine.length(); i++)
                            temp += inLine.charAt(i);
                        officeNum = Integer.parseInt(temp);
                        temp = "";
                        System.out.println(officeNum);
                    } else //it is a map coordinate
                    {
                        officeMapping.put(Integer.parseInt(inLine), officeNum);
                    }

                    //System.out.println (inLine);
                }
                System.out.println("read: ");
            } else {
                Log.i("Office", "info.txt doesn't exist");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (true) {
                //AssetFileDescriptor descriptor = cxt.getAssets().openFd("officeInfo.txt");
                //FileReader file = new FileReader (descriptor.getFileDescriptor());
                //FileReader file = new FileReader("officeInfo.txt");
                //BufferedReader reader = new BufferedReader(file);
                AssetManager assetManager = cxt.getAssets();
                InputStream inputStream = assetManager.open("officeInfo.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String tempString;
                while ((inLine = reader.readLine()) != null) {
                    if (inLine.charAt(0) == '*') {
                        for (int i = 1; i < inLine.length(); i++)
                            temp += inLine.charAt(i);
                        Log.i("Office", "" + temp);
                        officeNum = Integer.parseInt(temp);
                        temp = "";
                        System.out.println(officeNum);
                        tempString = "";
                    } else {
                        officeInformation.put(officeNum, inLine);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOfficeInfo(int officeAddr) {
        int officeNum;
        if (officeMapping.containsKey(officeAddr)) {
            officeNum = officeMapping.get(officeAddr);
            if (officeInformation.containsKey(officeNum)) {
                return (officeInformation.get(officeNum));
            }
        }
        return (null);
    }
}
