package com.daytripper.app.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class TrieNode<K, V> {

	private Map<K, TrieNode<K, V>> children = null;
	private V value = null;

	public TrieNode() {
		children = new HashMap<K, TrieNode<K, V>>();
	}

	public void addPattern(K[] pattern, V value) {
		Map<K, TrieNode<K, V>> currentChildren = children;
		TrieNode<K, V> currentTrie = null;
		for (K element : pattern) {
			if (currentChildren.containsKey(element)) {
				currentTrie = currentChildren.get(element);
				currentChildren = currentChildren.get(element).children;
			} else {
				TrieNode<K, V> trie = new TrieNode<K, V>();
				currentTrie = trie;
				currentChildren.put(element, trie);
				currentChildren = currentTrie.children;
			}
		}
		currentTrie.value = value;
	}

	public V lookup(Collection<K> pattern) {
		Map<K, TrieNode<K, V>> currentChildren = children;
		TrieNode<K, V> currentTrie = null;
		Iterator<K> iterator = pattern.iterator();
		while (iterator.hasNext()) {
			K nextItem = iterator.next();
			if (currentChildren.containsKey(nextItem)) {
				currentTrie = currentChildren.get(nextItem);
				currentChildren = currentTrie.children;
			}
		}
		if (currentTrie != null) {
			return currentTrie.value;
		}
		return null;
	}

	public V lookup(K[] pattern) {
		return lookup(Arrays.asList(pattern));
	}
}