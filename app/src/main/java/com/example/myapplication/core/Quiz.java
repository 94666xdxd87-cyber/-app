package com.example.myapplication.core;

public class Quiz {
    public int id;
    public String questionBody;
    public String correctAnswer;
    public String explanation;
    public String userAnswer;
    public int categoryType; // 1:名詞, 2:動詞, 3:副詞, 4:形容詞, 0:未知

    public Quiz(int id, String correctAnswer, String questionBody, String explanation, int categoryType) {
        this.id = id;
        this.correctAnswer = correctAnswer;
        this.questionBody = questionBody;
        this.explanation = explanation;
        this.categoryType = categoryType;
        this.userAnswer = "";
    }
}
