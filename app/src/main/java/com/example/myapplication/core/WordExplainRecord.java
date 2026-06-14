package com.example.myapplication.core;

public class WordExplainRecord {
    public String word;
    public String chineseMeaning;
    public String kkPhonetic;
    public String partOfSpeech;
    public String exampleSentence;
    public String timestamp;
    public String audioPath;

    public WordExplainRecord(String word, String chineseMeaning, String kkPhonetic,
                              String partOfSpeech, String exampleSentence,
                              String timestamp, String audioPath) {
        this.word            = word;
        this.chineseMeaning  = chineseMeaning;
        this.kkPhonetic      = kkPhonetic;
        this.partOfSpeech    = partOfSpeech;
        this.exampleSentence = exampleSentence;
        this.timestamp       = timestamp;
        this.audioPath       = (audioPath != null) ? audioPath : "";
    }

    public WordExplainRecord(String word, String chineseMeaning, String kkPhonetic,
                              String partOfSpeech, String exampleSentence, String timestamp) {
        this(word, chineseMeaning, kkPhonetic, partOfSpeech, exampleSentence, timestamp, "");
    }

    public boolean hasAudio() { return audioPath != null && !audioPath.isEmpty(); }

    public String serialize() {
        return word + "§§" + chineseMeaning + "§§" + kkPhonetic + "§§"
                + partOfSpeech + "§§" + exampleSentence + "§§" + timestamp + "§§" + audioPath;
    }

    public static WordExplainRecord deserialize(String line) {
        String[] p = line.split("§§", -1);
        if (p.length < 6) return null;
        String audio = (p.length >= 7) ? p[6] : "";
        return new WordExplainRecord(p[0], p[1], p[2], p[3], p[4], p[5], audio);
    }
}
