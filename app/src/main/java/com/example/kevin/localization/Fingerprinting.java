package com.example.kevin.localization;

/**
 * Created by kevin on 2016/5/13.
 */
public class Fingerprinting implements Comparable<Fingerprinting> {

    int data[]; //record fingerprintings
    int x, y; //coordinate
    double bias; //distance between real user signal strength and database signal strength

    public Fingerprinting(int n) {
        data = new int[n];
        x = y = 0;
        bias = 0;
    }

    public void setData(int[] data) {

        for (int i = 0; i < data.length; i++) {
            this.data[i] = data[i];
        }
    }

    @Override
    public int compareTo(Fingerprinting another) {

        if (another.bias - this.bias > 0) return -1;
        else if (another.bias - this.bias < 0) return 1;
        else return 0;
    }
}
