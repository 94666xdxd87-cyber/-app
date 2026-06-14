package com.example.myapplication.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WordTrie {

    private static class TrieNode {
        final Map<Character, TrieNode> children = new HashMap<>();
        boolean isEnd = false;
    }

    private final TrieNode root = new TrieNode();
    private int wordCount = 0;

    public void insert(String word) {
        if (word == null || word.isEmpty()) return;
        word = word.toLowerCase();
        TrieNode cur = root;
        for (char c : word.toCharArray()) {
            cur.children.putIfAbsent(c, new TrieNode());
            cur = cur.children.get(c);
        }
        if (!cur.isEnd) { cur.isEnd = true; wordCount++; }
    }

    public void delete(String word) {
        if (word == null || word.isEmpty()) return;
        word = word.toLowerCase();
        if (deleteHelper(root, word, 0)) wordCount--;
    }

    private boolean deleteHelper(TrieNode node, String word, int depth) {
        if (depth == word.length()) {
            if (!node.isEnd) return false;
            node.isEnd = false;
            return node.children.isEmpty();
        }
        char c = word.charAt(depth);
        TrieNode child = node.children.get(c);
        if (child == null) return false;
        boolean shouldDelete = deleteHelper(child, word, depth + 1);
        if (shouldDelete) {
            node.children.remove(c);
            return !node.isEnd && node.children.isEmpty();
        }
        return false;
    }

    public boolean contains(String word) {
        if (word == null || word.isEmpty()) return false;
        TrieNode node = getNode(word.toLowerCase());
        return node != null && node.isEnd;
    }

    public boolean hasPrefix(String prefix) {
        return getNode(prefix.toLowerCase()) != null;
    }

    public List<String> getWordsWithPrefix(String prefix) {
        List<String> result = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) {
            collectAll(root, new StringBuilder(), result);
            return result;
        }
        prefix = prefix.toLowerCase();
        TrieNode node = getNode(prefix);
        if (node == null) return result;
        collectAll(node, new StringBuilder(prefix), result);
        return result;
    }

    public List<String> getAllWords() { return getWordsWithPrefix(""); }

    private void collectAll(TrieNode node, StringBuilder current, List<String> result) {
        if (node.isEnd) result.add(current.toString());
        node.children.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    current.append(entry.getKey());
                    collectAll(entry.getValue(), current, result);
                    current.deleteCharAt(current.length() - 1);
                });
    }

    public int size()     { return wordCount; }
    public boolean isEmpty() { return wordCount == 0; }
    public void clear()   { root.children.clear(); wordCount = 0; }

    public static WordTrie fromList(List<String> words) {
        WordTrie trie = new WordTrie();
        if (words != null) for (String w : words) trie.insert(w);
        return trie;
    }

    private TrieNode getNode(String prefix) {
        TrieNode cur = root;
        for (char c : prefix.toCharArray()) {
            cur = cur.children.get(c);
            if (cur == null) return null;
        }
        return cur;
    }
}
