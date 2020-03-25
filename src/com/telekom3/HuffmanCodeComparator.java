package com.telekom3;

import java.util.Comparator;
import java.util.HashMap;

class HuffmanCodeComparator implements Comparator<String> {
    public int compare(String x, String y) {
        return x.length() - y.length();
    }
}
