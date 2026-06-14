package com.example.myapplication.core;

public class WrongWord {
    public String word;
    public int errorCount;

    public WrongWord(String word) {
        this.word = word;
        this.errorCount = 0;
    }

    public void incrementCount() {
        this.errorCount++;
    }

    @Override
    public String toString() {
        return String.format("%s (強度: %d)", word, errorCount);
    }
}
